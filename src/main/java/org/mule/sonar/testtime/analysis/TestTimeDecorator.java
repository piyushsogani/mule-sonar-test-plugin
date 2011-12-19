package org.mule.sonar.testtime.analysis;

import org.mule.sonar.testtime.TestTimeMetrics;

import java.util.Collection;

import org.sonar.api.batch.Decorator;
import org.sonar.api.batch.DecoratorContext;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Measure;
import org.sonar.api.measures.MeasureUtils;
import org.sonar.api.measures.Metric;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Resource;
import org.sonar.api.resources.ResourceUtils;
import org.sonar.api.utils.ParsingUtils;

public class TestTimeDecorator implements Decorator
{

    public boolean shouldExecuteOnProject(Project project)
    {
        return project.isLatestAnalysis();
    }

    public void decorate(Resource resource, DecoratorContext context)
    {
        if (!shouldDecorateResource(resource, context))
        {
            return;
        }

        Double numberOfTests = MeasureUtils.sum(true, context.getChildrenMeasures(CoreMetrics.TESTS));

        if (numberOfTests > 0)
        {
            calculateAverageTestExecutionTest(context, numberOfTests);

            double totalTestTime = calculateTestTimeCount(context, TestTimeMetrics.GOOD_TEST_TIME_COUNT);
            calculateDensityMetric(context, numberOfTests, totalTestTime, TestTimeMetrics.GOOD_TEST_TIME_DENSITY);

            totalTestTime = calculateTestTimeCount(context, TestTimeMetrics.UGLY_TEST_TIME_COUNT);
            calculateDensityMetric(context, numberOfTests, totalTestTime, TestTimeMetrics.UGLY_TEST_TIME_DENSITY);

            totalTestTime = calculateTestTimeCount(context, TestTimeMetrics.BAD_TEST_TIME_COUNT);
            calculateDensityMetric(context, numberOfTests, totalTestTime, TestTimeMetrics.BAD_TEST_TIME_DENSITY);
        }
    }

    private void calculateDensityMetric(DecoratorContext context, Double numberOfTests, double testTimeOkCount, Metric metric)
    {
        Double percentage = testTimeOkCount * 100d / numberOfTests;

        context.saveMeasure(metric, percentage);
    }

    private double calculateTestTimeCount(DecoratorContext context, Metric metric)
    {
        Collection<Measure> childrenMeasures = context.getChildrenMeasures(metric);
        double testTimeOkCount = 0;

        if (childrenMeasures.size() > 0)
        {
            testTimeOkCount = MeasureUtils.sum(true, childrenMeasures);
        }

        context.saveMeasure(metric, testTimeOkCount);
        return testTimeOkCount;
    }

    private void calculateAverageTestExecutionTest(DecoratorContext context, Double numberOfTests)
    {
        Double testExecutionTime = MeasureUtils.sum(true, context.getChildrenMeasures(CoreMetrics.TEST_EXECUTION_TIME));

        context.saveMeasure(TestTimeMetrics.AVERAGE_TEST_TIME, ParsingUtils.scaleValue(testExecutionTime / numberOfTests));
    }

    private boolean shouldDecorateResource(Resource resource, DecoratorContext context)
    {
        return context.getMeasure(TestTimeMetrics.GOOD_TEST_TIME_COUNT) == null && (ResourceUtils.isUnitTestClass(resource) || !ResourceUtils.isEntity(resource));
    }
}
