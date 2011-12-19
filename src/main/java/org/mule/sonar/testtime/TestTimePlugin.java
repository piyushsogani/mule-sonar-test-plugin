package org.mule.sonar.testtime;

import org.mule.sonar.testtime.analysis.TestTimeDecorator;
import org.mule.sonar.testtime.analysis.TestTimeSensor;
import org.mule.sonar.testtime.ui.TestTimeWidget;

import java.util.Arrays;
import java.util.List;

import org.sonar.api.Properties;
import org.sonar.api.Property;
import org.sonar.api.SonarPlugin;

/**
 * Exports all the entry points provided by the plugin.
 */
//TODO(pablo.kraan): remove this property???
@Properties({
                    @Property(
                            key = TestTimePlugin.MY_PROPERTY,
                            name = "Plugin Property",
                            description = "A property for the plugin")})
public final class TestTimePlugin extends SonarPlugin
{

    public static final String MY_PROPERTY = "sonar.example.myproperty";

    // This is where you're going to declare all your Sonar extensions
    public List getExtensions()
    {
        return Arrays.asList(
                // Definitions
                TestTimeMetrics.class,

                // Batch
                TestTimeSensor.class, TestTimeDecorator.class,

                // UI
                TestTimeWidget.class);
    }
}