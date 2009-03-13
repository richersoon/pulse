package com.zutubi.pulse.master.tove.config.user;

import com.zutubi.pulse.master.model.BuildManager;
import com.zutubi.pulse.master.model.UserManager;
import com.zutubi.pulse.master.util.TransactionContext;
import com.zutubi.tove.config.cleanup.RecordCleanupTask;

import java.util.Arrays;
import java.util.List;

/**
 * Adds a custom cleanup task for user configuration that deletes the user
 * state.
 */
public class UserConfigurationCleanupTasks
{
    private UserManager userManager;
    private BuildManager buildManager;
    private TransactionContext transactionContext;

    public List<RecordCleanupTask> getTasks(UserConfiguration instance)
    {
        return Arrays.<RecordCleanupTask>asList(new UserStateCleanupTask(instance, userManager, buildManager, transactionContext));
    }

    public void setUserManager(UserManager userManager)
    {
        this.userManager = userManager;
    }

    public void setBuildManager(BuildManager buildManager)
    {
        this.buildManager = buildManager;
    }

    public void setTransactionContext(TransactionContext transactionContext)
    {
        this.transactionContext = transactionContext;
    }
}
