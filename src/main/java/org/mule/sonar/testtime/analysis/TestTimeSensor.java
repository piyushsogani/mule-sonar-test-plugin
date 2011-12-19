package org.mule.sonar.testtime.analysis;

import java.io.File;

import org.sonar.api.batch.DependsUpon;
import org.sonar.api.batch.Sensor;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.resources.Java;
import org.sonar.api.resources.Project;
import org.sonar.plugins.surefire.api.SurefireUtils;

@DependsUpon("SurefireSensor")
public class TestTimeSensor implements Sensor
{

    public boolean shouldExecuteOnProject(Project project)
    {
        return project.getAnalysisType().isDynamic(true) && Java.KEY.equals(project.getLanguageKey());
    }

    public void analyse(Project project, SensorContext sensorContext)
    {
        File dir = SurefireUtils.getReportsDirectory(project);
        collect(project, sensorContext, dir);
    }

    private void collect(Project project, SensorContext context, File reportsDir)
    {
        new SurefireParser().collect(project, context, reportsDir);
    }

    @Override
    public String toString()
    {
        return getClass().getSimpleName();
    }
}
