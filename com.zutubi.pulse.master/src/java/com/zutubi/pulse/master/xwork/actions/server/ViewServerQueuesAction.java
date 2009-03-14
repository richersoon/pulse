package com.zutubi.pulse.master.xwork.actions.server;

import com.zutubi.pulse.core.model.Entity;
import com.zutubi.pulse.master.*;
import com.zutubi.pulse.master.events.build.AbstractBuildRequestEvent;
import com.zutubi.pulse.master.model.BuildManager;
import com.zutubi.pulse.master.model.BuildResult;
import com.zutubi.pulse.master.security.AcegiUtils;
import com.zutubi.pulse.master.tove.config.project.ProjectConfigurationActions;
import com.zutubi.pulse.master.xwork.actions.ActionSupport;
import static com.zutubi.tove.security.AccessManager.ACTION_VIEW;

import java.util.*;

/**
 * Action to show the build and recipe queues.
 */
public class ViewServerQueuesAction extends ActionSupport
{
    private List<AbstractBuildRequestEvent> buildQueue;
    private List<BuildResult> executingBuilds;
    private List<RecipeAssignmentRequest> recipeQueueSnapshot;

    private FatController fatController;
    private RecipeQueue recipeQueue;
    private BuildManager buildManager;

    public List<AbstractBuildRequestEvent> getBuildQueue()
    {
        return buildQueue;
    }

    public List<BuildResult> getExecutingBuilds()
    {
        return executingBuilds;
    }

    public boolean getRecipeQueueRunning()
    {
        return recipeQueue.isRunning();
    }

    public List<RecipeAssignmentRequest> getRecipeQueueSnapshot()
    {
        return recipeQueueSnapshot;
    }

    public boolean canCancel(Object resource)
    {
        return accessManager.hasPermission(ProjectConfigurationActions.ACTION_CANCEL_BUILD, resource);
    }

    public String execute() throws Exception
    {
        // We snapshot the queues with full privileges so we can show a more
        // complete picture to the user by replacing entries they don't have
        // the authority to view with placeholders.
        snapshotQueuesAsSystem();
        sortBuilds();
        nullOutUnviewableEntries();

        return SUCCESS;
    }

    private void snapshotQueuesAsSystem()
    {
        AcegiUtils.runAsSystem(new Runnable()
        {
            public void run()
            {
                recipeQueueSnapshot = recipeQueue.takeSnapshot();

                buildQueue = new LinkedList<AbstractBuildRequestEvent>();
                executingBuilds = new LinkedList<BuildResult>();

                BuildQueue.Snapshot snapshot = fatController.snapshotBuildQueue();
                for (Map.Entry<Entity, List<AbstractBuildRequestEvent>>  entityQueue: snapshot.getQueuedBuilds().entrySet())
                {
                    buildQueue.addAll( entityQueue.getValue());
                }

                for (List<EntityBuildQueue.ActiveBuild> activeForEntity: snapshot.getActiveBuilds().values())
                {
                    for (EntityBuildQueue.ActiveBuild activeBuild: activeForEntity)
                    {
                        BuildController buildController = activeBuild.getController();
                        BuildResult buildResult = buildManager.getBuildResult(buildController.getBuildId());
                        if (buildResult != null && !buildResult.completed())
                        {
                            executingBuilds.add(buildResult);
                        }
                    }
                }
            }
        });
    }

    private void nullOutUnviewableEntries()
    {
        for (int i = 0; i < buildQueue.size(); i++)
        {
            if (!accessManager.hasPermission(ACTION_VIEW, buildQueue.get(i).getOwner()))
            {
                buildQueue.set(i, null);
            }
        }

        for (int i = 0; i < executingBuilds.size(); i++)
        {
            if (!accessManager.hasPermission(ACTION_VIEW, executingBuilds.get(i).getOwner()))
            {
                executingBuilds.set(i, null);
            }
        }

        for (int i = 0; i < recipeQueueSnapshot.size(); i++)
        {
            if (!accessManager.hasPermission(ACTION_VIEW, recipeQueueSnapshot.get(i).getProject()))
            {
                recipeQueueSnapshot.set(i, null);
            }

        }
    }

    private void sortBuilds()
    {
        Collections.sort(buildQueue, new Comparator<AbstractBuildRequestEvent>()
        {
            public int compare(AbstractBuildRequestEvent o1, AbstractBuildRequestEvent o2)
            {
                return (int) (o1.getQueued() - o2.getQueued());
            }
        });

        Collections.sort(executingBuilds, new Comparator<BuildResult>()
        {
            public int compare(BuildResult o1, BuildResult o2)
            {
                return (int) (o1.getStamps().getStartTime() - o2.getStamps().getStartTime());
            }
        });
    }

    public void setFatController(FatController fatController)
    {
        this.fatController = fatController;
    }

    public void setRecipeQueue(RecipeQueue queue)
    {
        this.recipeQueue = queue;
    }

    public void setBuildManager(BuildManager buildManager)
    {
        this.buildManager = buildManager;
    }
}
