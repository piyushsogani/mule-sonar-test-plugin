package org.mule.sonar.testtime.analysis;

import org.mule.sonar.testtime.TestTimeMetrics;

import java.io.File;
import java.io.FilenameFilter;
import java.util.List;
import java.util.Map;

import javax.xml.stream.XMLStreamException;

import org.apache.commons.lang.StringUtils;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.measures.Metric;
import org.sonar.api.resources.JavaFile;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Resource;
import org.sonar.api.utils.SonarException;
import org.sonar.api.utils.StaxParser;
import org.sonar.plugins.surefire.data.UnitTestClassReport;
import org.sonar.plugins.surefire.data.UnitTestIndex;
import org.sonar.plugins.surefire.data.UnitTestResult;

public class SurefireParser
{

    private static final int MAX_TEST_TIME_OK = 5;
    private static final int MAX_TEST_TIME_WARNING = 50;

    public void collect(Project project, SensorContext context, File reportsDir)
    {
        File[] xmlFiles = getReports(reportsDir);

        if (xmlFiles.length > 0)
        {
            parseFiles(context, xmlFiles);
        }
    }

    private File[] getReports(File dir)
    {
        if (dir == null || !dir.isDirectory() || !dir.exists())
        {
            return new File[0];
        }
        return dir.listFiles(new FilenameFilter()
        {
            public boolean accept(File dir, String name)
            {
                return name.startsWith("TEST") && name.endsWith(".xml");
            }
        });
    }

    private void parseFiles(SensorContext context, File[] reports)
    {
        UnitTestIndex index = new UnitTestIndex();
        parseFiles(reports, index);
        sanitize(index);
        save(index, context);
    }

    private void parseFiles(File[] reports, UnitTestIndex index)
    {
        SurefireStaxHandler staxParser = new SurefireStaxHandler(index);
        StaxParser parser = new StaxParser(staxParser, false);
        for (File report : reports)
        {
            try
            {
                parser.parse(report);
            }
            catch (XMLStreamException e)
            {
                throw new SonarException("Fail to parse the Surefire report: " + report, e);
            }
        }
    }

    private void sanitize(UnitTestIndex index)
    {
        for (String classname : index.getClassnames())
        {
            if (StringUtils.contains(classname, "$"))
            {
                // Surefire reports classes whereas sonar supports files
                String parentClassName = StringUtils.substringBefore(classname, "$");
                index.merge(classname, parentClassName);
            }
        }
    }

    private void save(UnitTestIndex index, SensorContext context)
    {
        for (Map.Entry<String, UnitTestClassReport> entry : index.getIndexByClassname().entrySet())
        {
            UnitTestClassReport report = entry.getValue();
            List<UnitTestResult> results = report.getResults();

            long testCount = report.getTests();

            int testTimeOK = 0;
            int testTimeWarning = 0;
            int testTimeError = 0;

            if (testCount > 0)
            {
                for (UnitTestResult result : results)
                {
                    if (isTestTimeOK(result.getDurationMilliseconds()))
                    {
                        testTimeOK++;
                    }
                    else if (isTestTimeWarning(result.getDurationMilliseconds()))
                    {
                        testTimeWarning++;
                    }
                    else
                    {
                        testTimeError++;
                    }
                }

                Resource resource = getUnitTestResource(entry.getKey());

                saveMeasure(context, resource, TestTimeMetrics.GOOD_TEST_TIME_COUNT, testTimeOK);
                saveMeasure(context, resource, TestTimeMetrics.UGLY_TEST_TIME_COUNT, testTimeWarning);
                saveMeasure(context, resource, TestTimeMetrics.BAD_TEST_TIME_COUNT, testTimeError);
            }
        }
    }

    private boolean isTestTimeWarning(long durationMilliseconds)
    {
        return durationMilliseconds > MAX_TEST_TIME_OK && durationMilliseconds <= MAX_TEST_TIME_WARNING;
    }

    private boolean isTestTimeOK(long durationMilliseconds)
    {
        return durationMilliseconds <= MAX_TEST_TIME_OK;
    }

    private void saveMeasure(SensorContext context, Resource resource, Metric metric, double value)
    {
        if (!Double.isNaN(value))
        {
            context.saveMeasure(resource, metric, value);
        }
    }

    protected Resource<?> getUnitTestResource(String classKey)
    {
        if (!StringUtils.contains(classKey, "$"))
        {
            // temporary hack waiting for http://jira.codehaus.org/browse/SONAR-1865
            return new JavaFile(classKey, true);
        }

        return null;
    }
}
