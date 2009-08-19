package com.zutubi.pulse.master.events.build;

import com.zutubi.pulse.core.BuildRevision;
import com.zutubi.pulse.core.model.NamedEntity;
import com.zutubi.pulse.master.model.*;

/**
 * A request for a project build.
 */
public class SingleBuildRequestEvent extends BuildRequestEvent
{
    private Project owner;

    public SingleBuildRequestEvent(Object source, Project project, BuildRevision buildRevision, TriggerOptions options)
    {
        super(source, buildRevision, project.getConfig(), options);
        this.owner = project;
    }

    public NamedEntity getOwner()
    {
        return owner;
    }

    public boolean isPersonal()
    {
        return false;
    }

    public String getStatus()
    {
        String status = getProjectConfig().getDependencies().getStatus();
        if (getOptions().hasStatus())
        {
            status = options.getStatus();
        }
        return status;
    }

    public BuildResult createResult(ProjectManager projectManager, BuildManager buildManger)
    {
        Project project = projectManager.getProject(getProjectConfig().getProjectId(), false); // can we use the 'owner' project instance instead of loading here?
        BuildResult result = new BuildResult(options.getReason(), project, projectManager.getNextBuildNumber(project, true), getRevision().isUser());
        result.setStatus(getStatus());
        result.setBuildId(getBuildId());

        buildManger.save(result);

        return result;
    }

    public String toString()
    {
        StringBuffer buff = new StringBuffer("Build Request Event");
        // should never be null, but then again, toString must never fail either.
        if (getProjectConfig() != null)
        {
            buff.append(": name: ").append(getProjectConfig().getName());
        }
        if (options.getReason() != null)
        {
            buff.append(": summary: ").append(options.getReason().getSummary());
        }
        if (options.getSource() != null)
        {
            buff.append(": source: ").append(options.getSource());
        }
        if(options.isReplaceable())
        {
            buff.append(" (replaceable)");
        }
        return buff.toString();
    }
}