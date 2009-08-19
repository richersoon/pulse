package com.zutubi.pulse.master;

import com.zutubi.events.Event;
import com.zutubi.events.EventListener;
import com.zutubi.events.EventManager;
import com.zutubi.i18n.Messages;
import com.zutubi.pulse.core.dependency.DependencyManager;
import com.zutubi.pulse.master.events.build.BuildCompletedEvent;
import com.zutubi.pulse.master.events.build.BuildRequestEvent;
import com.zutubi.pulse.master.events.build.RebuildRequestEvent;
import com.zutubi.pulse.master.events.build.SingleBuildRequestEvent;
import com.zutubi.pulse.master.model.BuildManager;
import com.zutubi.pulse.master.model.BuildResult;
import com.zutubi.pulse.master.model.Project;
import com.zutubi.pulse.master.model.ProjectManager;
import com.zutubi.pulse.master.tove.config.project.DependencyConfiguration;
import com.zutubi.pulse.master.tove.config.project.ProjectConfiguration;
import com.zutubi.util.CollectionUtils;
import com.zutubi.util.Predicate;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * The rebuild schedule handler is responsible for coordinating an
 * extended build of the project identified in the request and all
 * of that projects dependencies.  The order of the building will
 * be such that a dependency is built before the project that depends
 * upon it.
 */
public class DependencyRebuildScheduleHandler extends BaseScheduleHandler
{
    private static final Messages I18N = Messages.getInstance(DependencyRebuildScheduleHandler.class);

    private FatController scheduler;
    private EventManager eventManager;

    private EventListener eventListener;
    private ProjectManager projectManager;
    private BuildManager buildManager;
    private DependencyManager dependencyManager;
    private SingleBuildRequestEvent originalRequest;

    private final Map<ProjectConfiguration, List<ProjectConfiguration>> buildOrder= new HashMap<ProjectConfiguration, List<ProjectConfiguration>>();
    private final Map<ProjectConfiguration, BuildResult> resultCache = new HashMap<ProjectConfiguration, BuildResult>();

    public DependencyRebuildScheduleHandler()
    {
    }

    public void init()
    {
        super.init();

        eventListener = new EventListener()
        {
            public void handleEvent(Event event)
            {
                BuildCompletedEvent evt = (BuildCompletedEvent) event;
                if (evt.getBuildResult().getBuildId() == getMetaBuildId())
                {
                    buildFinished(evt);
                }
            }

            public Class[] getHandledEvents()
            {
                return new Class[]{BuildCompletedEvent.class};
            }
        };

        eventManager.register(eventListener);
    }

    public synchronized void handle(BuildRequestEvent request)
    {
        // Any requests that are triggered as a side effect of one of the builds
        // generated by this scheduler will be ignored.
        if (request.getBuildId() == getMetaBuildId())
        {
            return;
        }

        if (originalRequest != null)
        {
            // Coding error.
            throw new RuntimeException("Can not reuse this instance.");
        }

        originalRequest = (SingleBuildRequestEvent) request;

        ProjectConfiguration projectToRebuild = request.getProjectConfig();
        recordDependencies(projectToRebuild, buildOrder);

        // Build those projects that do not depend on any other projects to be built.
        List<ProjectConfiguration> canBuildNow = CollectionUtils.filter(buildOrder.keySet(), new Predicate<ProjectConfiguration>()
        {
            public boolean satisfied(ProjectConfiguration project)
            {
                return buildOrder.get(project).size() == 0;
            }
        });

        for (ProjectConfiguration p : canBuildNow)
        {
            buildOrder.remove(p);

            RebuildRequestEvent buildRequest = createRequest(p);
            scheduler.enqueueBuildRequest(buildRequest);
        }
    }

    public boolean isMultiBuild()
    {
        return true;
    }

    public synchronized void buildFinished(BuildCompletedEvent evt)
    {
        BuildResult buildResult = evt.getBuildResult();
        ProjectConfiguration builtProject = buildResult.getProject().getConfig();

        if (buildOrder.containsKey(builtProject))
        {
            buildOrder.remove(builtProject);
        }

        if (buildOrder.size() == 0)
        {
            // our work here is finished.
            eventManager.unregister(eventListener);
            scheduler.multiBuildComplete(getMetaBuildId());
        }
        else
        {
            for (ProjectConfiguration p : buildOrder.keySet())
            {
                List<ProjectConfiguration> d = buildOrder.get(p);
                if (d.contains(builtProject))
                {
                    d.remove(builtProject);
                }
            }
            if (buildResult.succeeded())
            {
                // if build successful, keep going
                for (ProjectConfiguration p : buildOrder.keySet())
                {
                    List<ProjectConfiguration> d = buildOrder.get(p);
                    if (d.size() == 0)
                    {
                        RebuildRequestEvent buildRequest = createRequest(p);
                        scheduler.enqueueBuildRequest(buildRequest);
                    }
                }
            }
            else
            {
                final String failedProjectName = buildResult.getProject().getConfig().getName();
                
                // else mark the rest as failed - except the active ones, just leave them.
                final List<ProjectConfiguration> toRemove = new LinkedList<ProjectConfiguration>();
                for (ProjectConfiguration project : buildOrder.keySet())
                {
                    List<ProjectConfiguration> d = buildOrder.get(project);
                    if (d.size() != 0)
                    {
                        toRemove.add(project);
                        for (ProjectConfiguration pendingProject : d)
                        {
                            BuildResult pendingResult = resultCache.get(pendingProject);
                            pendingResult.error(I18N.format("skip.build", new Object[]{failedProjectName}));
                            buildManager.save(pendingResult);
                        }
                    }
                    else
                    {
                        BuildResult result = resultCache.get(project);
                        if (!result.inProgress())
                        {
                            result.error(I18N.format("skip.build", new Object[]{failedProjectName}));
                            buildManager.save(result);
                            toRemove.add(project);
                        }
                    }
                }
                for (ProjectConfiguration p : toRemove)
                {
                    buildOrder.remove(p);
                }
            }
        }
    }

    private RebuildRequestEvent createRequest(ProjectConfiguration project)
    {
        Project p = projectManager.getProject(project.getProjectId(), false);
        RebuildRequestEvent request = new RebuildRequestEvent(this, originalRequest.getRevision(), p, originalRequest.getOptions(), originalRequest.getProjectConfig(), resultCache);
        request.setBuildId(getMetaBuildId());
        return request;
    }

    private void recordDependencies(ProjectConfiguration project, Map<ProjectConfiguration, List<ProjectConfiguration>> dependencies)
    {
        List<ProjectConfiguration> projects = new LinkedList<ProjectConfiguration>();
        List<DependencyConfiguration> traversableDependencies = CollectionUtils.filter(
                project.getDependencies().getDependencies(),
                getTraverseDependencyPredicate()
        );
        for (DependencyConfiguration dependency : traversableDependencies)
        {
            ProjectConfiguration p = dependency.getProject();
            projects.add(p);
            recordDependencies(p, dependencies);
        }
        dependencies.put(project, projects);
    }

    public Predicate<DependencyConfiguration> getTraverseDependencyPredicate()
    {
        final String buildStatus = originalRequest.getStatus();

        return new Predicate<DependencyConfiguration>()
        {
            public boolean satisfied(DependencyConfiguration dependency)
            {
                String revision = dependency.getRevision();
                if (revision.startsWith(DependencyConfiguration.LATEST))
                {
                    // We have a variable revision that works with the latest build of dependency.
                    // Check the status to see if a new build of the dependency will be picked up.

                    String dependencyStatus = revision.substring(DependencyConfiguration.LATEST.length());
                    if (dependencyManager.getPriority(buildStatus) <= dependencyManager.getPriority(dependencyStatus))
                    {
                        return dependency.isTransitive();
                    }
                }
                return false;
            }
        };
    }

    public void setFatController(FatController scheduler)
    {
        this.scheduler = scheduler;
    }

    public void setEventManager(EventManager eventManager)
    {
        this.eventManager = eventManager;
    }

    public void setProjectManager(ProjectManager projectManager)
    {
        this.projectManager = projectManager;
    }

    public void setDependencyManager(DependencyManager dependencyManager)
    {
        this.dependencyManager = dependencyManager;
    }

    public void setBuildManager(BuildManager buildManager)
    {
        this.buildManager = buildManager;
    }
}