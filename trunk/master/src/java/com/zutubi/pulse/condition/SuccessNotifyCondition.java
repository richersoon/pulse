package com.zutubi.pulse.condition;

import com.zutubi.pulse.condition.NotifyCondition;
import com.zutubi.pulse.model.BuildResult;
import com.zutubi.pulse.model.User;

/**
 * 
 *
 */
public class SuccessNotifyCondition implements NotifyCondition
{
    public boolean satisfied(BuildResult result, User user)
    {
        return result.succeeded();
    }
}
