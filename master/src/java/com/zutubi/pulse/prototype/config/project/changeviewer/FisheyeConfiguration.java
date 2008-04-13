package com.zutubi.pulse.prototype.config.project.changeviewer;

import com.zutubi.config.annotations.Form;
import com.zutubi.config.annotations.SymbolicName;
import com.zutubi.config.annotations.Wire;
import com.zutubi.pulse.core.model.Revision;
import com.zutubi.pulse.core.scm.config.ScmConfiguration;
import com.zutubi.util.StringUtils;
import com.zutubi.util.TextUtils;

/**
 * A ChangeViewer for linking to a Fisheye instance.
 */
@Form(fieldOrder = {"baseURL", "projectPath", "pathStripPrefix"})
@SymbolicName("zutubi.fisheyeChangeViewerConfig")
@Wire
public class FisheyeConfiguration extends BasePathChangeViewer
{
    /**
     * Useful when configured against Perforce.  In this case the paths in
     * Pulse are full depot paths, but Fisheye expects a shorter form:
     * starting after a path configured in Fisheye itself.  This will be at
     * least //depot, but could be deeper, and needs to be stripped from the
     * front of all paths before using them to construct URLs.
     */
    private String pathStripPrefix;

    public FisheyeConfiguration()
    {
        super(null, null);
    }

    public FisheyeConfiguration(String baseURL, String projectPath)
    {
        super(baseURL, projectPath);
    }

    public String getPathStripPrefix()
    {
        return pathStripPrefix;
    }

    public void setPathStripPrefix(String pathStripPrefix)
    {
        this.pathStripPrefix = pathStripPrefix;
    }

    public String getDetails()
    {
        return "Fisheye [" + getBaseURL() + "]";
    }

    public String getChangesetURL(Revision revision)
    {
        return StringUtils.join("/", true, true, getBaseURL(), "changelog", getProjectPath(), "?cs=" + getChangesetString(revision));
    }

    public String getFileViewURL(String path, String revision)
    {
        return StringUtils.join("/", true, true, getBaseURL(), "browse", getProjectPath(), StringUtils.urlEncodePath(stripPathPrefix(path)) + "?r=" + revision);
    }

    public String getFileDownloadURL(String path, String revision)
    {
        return StringUtils.join("/", true, true, getBaseURL(), "browse", "~raw,r=" + revision, getProjectPath(), StringUtils.urlEncodePath(stripPathPrefix(path)));
    }

    public String getFileDiffURL(String path, String revision)
    {
        ScmConfiguration scm = lookupScmConfiguration();
        String previousRevision = scm.getPreviousRevision(revision);
        if(previousRevision == null)
        {
            return null;
        }

        return StringUtils.join("/", true, true, getBaseURL(), "browse", getProjectPath(), StringUtils.urlEncodePath(stripPathPrefix(path)) + "?r1=" + previousRevision + "&r2=" + revision);
    }

    private String stripPathPrefix(String path)
    {
        if(TextUtils.stringSet(pathStripPrefix) && path.startsWith(pathStripPrefix))
        {
            path = path.substring(pathStripPrefix.length());
        }

        return path;
    }

    private String getChangesetString(Revision revision)
    {
        ScmConfiguration scm = lookupScmConfiguration();
        if (scm.getType().equals("cvs"))
        {
            return String.format("%s:%s:%s", revision.getBranch(), revision.getAuthor(), CustomChangeViewerConfiguration.FISHEYE_DATE_FORMAT.format(revision.getDate()));
        }

        return revision.getRevisionString();
    }
}
