package com.zutubi.pulse.tove.config.project.hooks;

import com.zutubi.pulse.MasterBuildProperties;
import com.zutubi.pulse.agent.MasterLocationProvider;
import com.zutubi.pulse.bootstrap.MasterConfigurationManager;
import com.zutubi.pulse.core.CommandEventOutputStream;
import com.zutubi.pulse.core.ExecutionContext;
import com.zutubi.pulse.core.BuildEventOutputStream;
import com.zutubi.pulse.core.model.Feature;
import com.zutubi.pulse.core.model.Result;
import com.zutubi.pulse.core.model.ResultState;
import com.zutubi.pulse.events.Event;
import com.zutubi.pulse.events.EventListener;
import com.zutubi.pulse.events.EventManager;
import com.zutubi.pulse.events.build.BuildEvent;
import com.zutubi.pulse.events.build.StageEvent;
import com.zutubi.pulse.model.BuildResult;
import com.zutubi.pulse.model.Project;
import com.zutubi.pulse.model.RecipeResultNode;
import com.zutubi.pulse.model.persistence.hibernate.HibernateBuildResultDao;
import com.zutubi.util.IOUtils;
import com.zutubi.util.UnaryProcedure;

import java.io.OutputStream;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Manages the execution of build hooks at various points in the build
 * process.
 */
public class BuildHookManager implements EventListener
{
    private ExecutorService executor = Executors.newSingleThreadExecutor();

    private MasterConfigurationManager configurationManager;
    private MasterLocationProvider masterLocationProvider;

    private EventManager eventManager;

    public void handleEvent(Event event)
    {
        final BuildEvent be = (BuildEvent) event;
        BuildResult buildResult = be.getBuildResult();
        if (!buildResult.isPersonal())
        {
            // generate the execution context.
            ExecutionContext context = new ExecutionContext(be.getContext());
            Project project = buildResult.getProject();
            for (BuildHookConfiguration hook : project.getConfig().getBuildHooks().values())
            {
                if (hook.enabled() && hook.triggeredBy(be))
                {
                    RecipeResultNode resultNode = null;
                    if (be instanceof StageEvent)
                    {
                        resultNode = ((StageEvent) be).getStageNode();
                    }

                    OutputStream out = null;
                    try
                    {
                        // stream the output to whoever is listening.
                        out = new BuildEventOutputStream(eventManager, true);
                        context.setOutputStream(out);

                        executeTask(hook, context, be.getBuildResult(), resultNode, false);
                    }
                    finally
                    {
                        IOUtils.close(out);
                    }
                }
            }
        }
    }

    public void manualTrigger(final BuildHookConfiguration hook, final BuildResult result)
    {
        HibernateBuildResultDao.intialise(result);
        executor.execute(new Runnable()
        {
            public void run()
            {
                final ExecutionContext context = new ExecutionContext();
                MasterBuildProperties.addAllBuildProperties(context, result, masterLocationProvider, configurationManager);
                if (hook.appliesTo(result))
                {
                    executeTask(hook, context, result, null, true);
                }

                result.getRoot().forEachNode(new UnaryProcedure<RecipeResultNode>()
                {
                    public void process(RecipeResultNode recipeResultNode)
                    {
                        if (hook.appliesTo(recipeResultNode))
                        {
                            context.push();
                            try
                            {
                                MasterBuildProperties.addStageProperties(context, result, recipeResultNode, configurationManager, false);
                                executeTask(hook, context, result, recipeResultNode, true);
                            }
                            finally
                            {
                                context.pop();
                            }
                        }
                    }
                });
            }
        });
    }

    private void executeTask(BuildHookConfiguration hook, ExecutionContext context, BuildResult buildResult, RecipeResultNode resultNode, boolean manual)
    {
        BuildHookTaskConfiguration task = hook.getTask();
        try
        {
            task.execute(context, buildResult, resultNode);
        }
        catch (Exception e)
        {
            if (!manual)
            {
                Result result = resultNode == null ? buildResult : resultNode.getResult();
                result.addFeature(Feature.Level.ERROR, "Error executing task for hook '" + hook.getName() + "': " + e.getMessage());
                if (hook.failOnError())
                {
                    result.setState(ResultState.ERROR);
                }
            }
        }
    }

    public Class[] getHandledEvents()
    {
        return new Class[]{BuildEvent.class};
    }

    public void setEventManager(EventManager eventManager)
    {
        this.eventManager = eventManager;
        eventManager.register(this);
    }

    public void setConfigurationManager(MasterConfigurationManager configurationManager)
    {
        this.configurationManager = configurationManager;
    }

    public void setMasterLocationProvider(MasterLocationProvider masterLocationProvider)
    {
        this.masterLocationProvider = masterLocationProvider;
    }
}
