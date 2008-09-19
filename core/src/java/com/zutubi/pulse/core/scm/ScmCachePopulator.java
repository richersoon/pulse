package com.zutubi.pulse.core.scm;

import com.zutubi.pulse.core.model.Revision;
import com.zutubi.pulse.core.scm.ScmException;

/**
 */
public interface ScmCachePopulator
{
    String getUniqueLocation() throws ScmException;

    boolean requiresRefresh(ScmContext context, Revision revision) throws ScmException;

    void populate(ScmFileCache.CacheItem item) throws ScmException;
}
