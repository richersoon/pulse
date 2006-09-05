package com.zutubi.pulse.web.project;

import com.zutubi.pulse.model.BuildResult;
import com.zutubi.pulse.model.BuildSpecification;
import com.zutubi.pulse.model.Project;

import java.io.File;

/**
 */
public class BrowseProjectDirAction extends ProjectActionSupport
{
    private long buildId;
    private BuildResult buildResult;
    private String separator;
    private BuildSpecification buildSpecification;

    public long getBuildId()
    {
        return buildId;
    }

    public void setBuildId(long buildId)
    {
        this.buildId = buildId;
    }

    public BuildResult getBuildResult()
    {
        return buildResult;
    }

    public BuildSpecification getBuildSpecification()
    {
        return buildSpecification;
    }

    public String getSeparator()
    {
        return separator;
    }

    public Project getProject()
    {
        if(buildResult != null)
        {
            return buildResult.getProject();
        }

        return null;
    }

    public String execute() throws Exception
    {
        buildResult = getBuildManager().getBuildResult(buildId);
        if (buildResult == null)
        {
            addActionError("Unknown build [" + buildId + "]");
            return ERROR;
        }

        checkPermissions(buildResult);
        getProjectManager().checkWrite(buildResult.getProject());

        // this value is going to be written to the vm template and evaluated by javascript, so
        // we need to ensure that we escape the escape char.
        separator = File.separator.replace("\\", "\\\\");

        // provide some useful feedback on why the working directory is not available.

        // a) the working copy is not being retained.
        buildSpecification = getProject().getBuildSpecification(buildResult.getBuildSpecification());
        // b) else, the working directory has been cleaned up by a the projects "cleanup rules" or
        //    it has been manually deleted or the working directory capture has failed.

        return SUCCESS;
    }
}
