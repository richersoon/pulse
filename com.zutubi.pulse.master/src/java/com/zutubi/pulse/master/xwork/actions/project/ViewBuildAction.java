package com.zutubi.pulse.master.xwork.actions.project;

import com.zutubi.i18n.Messages;
import com.zutubi.pulse.core.model.PersistentChangelist;
import com.zutubi.pulse.core.model.PersistentTestSuiteResult;
import com.zutubi.pulse.core.model.Result;
import com.zutubi.pulse.core.model.ResultCustomFields;
import com.zutubi.pulse.master.bootstrap.MasterConfigurationManager;
import com.zutubi.pulse.master.model.*;
import com.zutubi.pulse.master.tove.config.project.ProjectConfiguration;
import com.zutubi.pulse.master.tove.config.project.ProjectConfigurationActions;
import com.zutubi.pulse.master.tove.config.project.hooks.BuildHookConfiguration;
import com.zutubi.pulse.master.tove.config.user.UserPreferencesConfiguration;
import com.zutubi.pulse.master.tove.model.ActionLink;
import com.zutubi.pulse.master.tove.webwork.ToveUtils;
import com.zutubi.pulse.servercore.bootstrap.SystemPaths;
import com.zutubi.tove.config.NamedConfigurationComparator;
import com.zutubi.tove.security.AccessManager;
import com.zutubi.util.CollectionUtils;
import com.zutubi.util.Predicate;
import com.zutubi.util.logging.Logger;

import java.io.File;
import java.util.*;

/**
 *
 *
 */
public class ViewBuildAction extends CommandActionBase
{
    public static final String FAILURE_LIMIT_PROPERTY = "pulse.test.failure.limit";
    public static final int DEFAULT_FAILURE_LIMIT = 100;

    private static final Logger LOG = Logger.getLogger(ViewBuildAction.class);

    private List<PersistentChangelist> changelists;
    private BuildColumns summaryColumns;
    /**
     * Insanity to work around lack of locals in velocity.
     */
    private Stack<String> pathStack = new Stack<String>();
    private String responsibleOwner;
    private String responsibleComment;
    private boolean canClearResponsible = false;
    private List<ActionLink> actions = new LinkedList<ActionLink>();
    private List<BuildHookConfiguration> hooks;

    private MasterConfigurationManager configurationManager;
    private SystemPaths systemPaths;

    public boolean haveRecipeResultNode()
    {
        return getRecipeResultNode() != null;
    }

    public boolean haveCommandResult()
    {
        return getCommandResult() != null;
    }

    public BuildResult getResult()
    {
        return getBuildResult();
    }

    public BuildColumns getSummaryColumns()
    {
        // Lazy init: not always used.
        if(summaryColumns == null)
        {
            User u = getLoggedInUser();
            summaryColumns = new BuildColumns(u == null ? UserPreferencesConfiguration.defaultProjectColumns() : u.getPreferences().getProjectSummaryColumns(), accessManager);
        }
        return summaryColumns;
    }

    public Map<String, String> getCustomFields(Result result)
    {
        ResultCustomFields customFields = new ResultCustomFields(result.getAbsoluteOutputDir(configurationManager.getDataDirectory()));
        return customFields.load();
    }

    public static int getFailureLimit()
    {
        int limit = DEFAULT_FAILURE_LIMIT;
        String property = System.getProperty(FAILURE_LIMIT_PROPERTY);
        if(property != null)
        {
            try
            {
                limit = Integer.parseInt(property);
            }
            catch(NumberFormatException e)
            {
                LOG.warning(e);
            }
        }

        return limit;
    }

    public String getResponsibleOwner()
    {
        return responsibleOwner;
    }

    public String getResponsibleComment()
    {
        return responsibleComment;
    }

    public boolean isCanClearResponsible()
    {
        return canClearResponsible;
    }

    public List<ActionLink> getActions()
    {
        return actions;
    }

    public List<BuildHookConfiguration> getHooks()
    {
        return hooks;
    }

    public String execute()
    {
        final BuildResult result = getRequiredBuildResult();
        Project project = result.getProject();
        boolean canWrite = accessManager.hasPermission(AccessManager.ACTION_WRITE, project);
        if (canWrite)
        {
            ProjectConfiguration projectConfig = getRequiredProject().getConfig();
            hooks = CollectionUtils.filter(projectConfig.getBuildHooks().values(), new Predicate<BuildHookConfiguration>()
            {
                public boolean satisfied(BuildHookConfiguration hookConfiguration)
                {
                    return hookConfiguration.canManuallyTriggerFor(result);
                }
            });
            Collections.sort(hooks, new NamedConfigurationComparator());
        }
        else
        {
            hooks = Collections.emptyList();
        }

        Messages messages = Messages.getInstance(BuildResult.class);
        File contentRoot = systemPaths.getContentRoot();
        if (result.completed())
        {
            if (canWrite)
            {
                actions.add(ToveUtils.getActionLink(AccessManager.ACTION_DELETE, messages, contentRoot));
            }
        }
        else
        {
            if (accessManager.hasPermission(ProjectConfigurationActions.ACTION_CANCEL_BUILD, result))
            {
                actions.add(ToveUtils.getActionLink(BuildResult.ACTION_CANCEL, messages, contentRoot));
            }
        }

        ProjectResponsibility projectResponsibility = project.getResponsibility();
        if (projectResponsibility == null && accessManager.hasPermission(ProjectConfigurationActions.ACTION_TAKE_RESPONSIBILITY, project))
        {
            actions.add(ToveUtils.getActionLink(ProjectConfigurationActions.ACTION_TAKE_RESPONSIBILITY, messages, contentRoot));
        }

        if (projectResponsibility != null)
        {
            responsibleOwner = projectResponsibility.getMessage(getLoggedInUser());
            responsibleComment = projectResponsibility.getComment();

            if (accessManager.hasPermission(ProjectConfigurationActions.ACTION_CLEAR_RESPONSIBILITY, project))
            {
                canClearResponsible = true;
                actions.add(ToveUtils.getActionLink(ProjectConfigurationActions.ACTION_CLEAR_RESPONSIBILITY, messages, contentRoot));
            }
        }

        // Initialise detail down to the command level (optional)
        getCommandResult();

        File dataDir = configurationManager.getDataDirectory();
        result.loadFeatures(dataDir);

        if(result.completed())
        {
            result.loadFailedTestResults(dataDir, getFailureLimit());
        }

        return SUCCESS;
    }

    public String pushSuite(PersistentTestSuiteResult suite)
    {
        if(pathStack.empty())
        {
            return pathStack.push(uriComponentEncode(suite.getName()));
        }
        else
        {
            return pathStack.push(pathStack.peek() + "/" + uriComponentEncode(suite.getName()));
        }
    }

    public void popSuite()
    {
        pathStack.pop();
    }

    public List<PersistentChangelist> getChangelists()
    {
        if(changelists == null)
        {
            changelists = buildManager.getChangesForBuild(getResult());
        }
        return changelists;
    }

    public void setConfigurationManager(MasterConfigurationManager configurationManager)
    {
        this.configurationManager = configurationManager;
    }

    public void setSystemPaths(SystemPaths systemPaths)
    {
        this.systemPaths = systemPaths;
    }
}
