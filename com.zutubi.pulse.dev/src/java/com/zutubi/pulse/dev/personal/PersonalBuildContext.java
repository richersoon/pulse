package com.zutubi.pulse.dev.personal;

import com.zutubi.pulse.core.scm.api.WorkingCopy;
import com.zutubi.pulse.core.scm.api.WorkingCopyContext;
import com.zutubi.pulse.core.scm.patch.api.PatchFormat;
import com.zutubi.pulse.dev.client.ClientException;

/**
 * Simple holder class for objects used during a personal build.
 */
public class PersonalBuildContext
{
    private WorkingCopy workingCopy;
    private WorkingCopyContext workingCopyContext;
    private String patchFormatType;
    private PatchFormat patchFormat;

    public PersonalBuildContext(WorkingCopy workingCopy, WorkingCopyContext workingCopyContext, String patchFormatType, PatchFormat patchFormat)
    {
        this.workingCopy = workingCopy;
        this.workingCopyContext = workingCopyContext;
        this.patchFormatType = patchFormatType;
        this.patchFormat = patchFormat;
    }

    public WorkingCopy getWorkingCopy()
    {
        return workingCopy;
    }

    public WorkingCopy getRequiredWorkingCopy() throws ClientException
    {
        if (workingCopy == null)
        {
            throw new ClientException("Personal builds are not supported for this SCM.");
        }

        return workingCopy;
    }

    public WorkingCopyContext getWorkingCopyContext()
    {
        return workingCopyContext;
    }

    public String getPatchFormatType()
    {
        return patchFormatType;
    }

    public PatchFormat getPatchFormat()
    {
        return patchFormat;
    }
}
