package com.zutubi.pulse.vfs.pulse;

import com.zutubi.pulse.model.BuildResult;
import com.zutubi.pulse.model.Project;
import com.zutubi.util.logging.Logger;
import org.apache.commons.vfs.FileName;
import org.apache.commons.vfs.FileSystemException;
import org.apache.commons.vfs.FileType;
import org.apache.commons.vfs.provider.AbstractFileSystem;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * <class comment/>
 */
public class LatestBuildFileObject extends AbstractPulseFileObject implements AddressableFileObject, BuildResultProvider
{
    private static final Map<String, Class> nodesDefinitions = new HashMap<String, Class>();
    {
        // setup the default root node definitions.
        nodesDefinitions.put("artifacts", NamedArtifactsFileObject.class);
    }

    private static final Logger LOG = Logger.getLogger(LatestBuildFileObject.class);

    public LatestBuildFileObject(final FileName name, final AbstractFileSystem fs)
    {
        super(name, fs);
    }

    public AbstractPulseFileObject createFile(final FileName fileName) throws Exception
    {
        String name = fileName.getBaseName();
        if (nodesDefinitions.containsKey(name))
        {
            Class clazz = nodesDefinitions.get(name);
            return objectFactory.buildBean(clazz,
                    new Class[]{FileName.class, AbstractFileSystem.class},
                    new Object[]{fileName, pfs}
            );
        }

        return objectFactory.buildBean(NamedStageFileObject.class,
                new Class[]{FileName.class, String.class, AbstractFileSystem.class},
                new Object[]{fileName, name, pfs}
        );
    }

    protected FileType doGetType() throws Exception
    {
        return FileType.IMAGINARY;
    }

    protected String[] doListChildren() throws Exception
    {
        Set<String> rootPaths = nodesDefinitions.keySet();
        return rootPaths.toArray(new String[rootPaths.size()]);
    }

    public boolean isLocal()
    {
        return true;
    }

    public String getUrlPath()
    {
        return "/viewBuild.action?id=" + getBuildResultId();
    }

    public BuildResult getBuildResult()
    {
        try
        {
            ProjectProvider provider = getAncestor(ProjectProvider.class);
            if (provider != null)
            {
                Project project = provider.getProject();

                BuildSpecificationProvider buildSpecProvider = getAncestor(BuildSpecificationProvider.class);
                if (buildSpecProvider != null)
                {
                    return buildManager.getLatestBuildResult(buildSpecProvider.getBuildSpecification());
                }
                else
                {
                    return buildManager.getLatestBuildResult(project);
                }
            }
            else
            {
                return buildManager.getLatestBuildResult();
            }
        }
        catch (FileSystemException e)
        {
            LOG.error(e);
            return null;
        }
    }

    public long getBuildResultId()
    {
        BuildResult result = getBuildResult();
        if (result != null)
        {
            return result.getId();
        }
        return -1;
    }
}
