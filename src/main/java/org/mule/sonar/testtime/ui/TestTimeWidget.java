package org.mule.sonar.testtime.ui;

import org.sonar.api.web.AbstractRubyTemplate;
import org.sonar.api.web.RubyRailsWidget;

public class TestTimeWidget extends AbstractRubyTemplate implements RubyRailsWidget
{

    public String getId()
    {
        return "testTime";
    }

    public String getTitle()
    {
        return "Test Execution Time";
    }

    @Override
    protected String getTemplatePath()
    {
        return "/widget/test_time_widget.html.erb";
    }
}