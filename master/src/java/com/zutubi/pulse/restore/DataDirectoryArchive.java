package com.zutubi.pulse.restore;

import com.zutubi.pulse.bootstrap.MasterUserPaths;
import com.zutubi.pulse.util.FileSystemUtils;

import java.io.File;
import java.io.IOException;

/**
 *
 *
 */
public class DataDirectoryArchive extends AbstractArchivableComponent
{
    private MasterUserPaths paths;

    public String getName()
    {
        return "config";
    }

    public String getDescription()
    {
        return "The configuration restoration takes the appropriate 1.2.x system files and restores " +
                "them to there 2.0 locations.  Note, the one system file excluded from this process is the" +
                "database.properties file.  The database configured for your 2.0 installation will be retained.";
    }

    public void backup(File archive) throws ArchiveException
    {
        try
        {
            FileSystemUtils.copy(new File(archive, "config"), paths.getUserConfigRoot());
            FileSystemUtils.copy(archive, new File(paths.getData(), "pulse.config.properties"));
        }
        catch (IOException e)
        {
            throw new ArchiveException(e);
        }
    }

    public void restore(File archive) throws ArchiveException
    {
        // replace the existing files with the archived files.
        try
        {
            cleanup(paths.getUserConfigRoot());
            FileSystemUtils.delete(new File(paths.getData(), "pulse.config.properties"));
            
            FileSystemUtils.copy(paths.getUserConfigRoot(), new File(archive, "config"));
            FileSystemUtils.copy(paths.getData(), new File(archive, "pulse.config.properties"));
        }
        catch (IOException e)
        {
            throw new ArchiveException(e);
        }
    }

    private void cleanup(File base) throws IOException
    {
        for (File file : base.listFiles())
        {
            if (file.isDirectory())
            {
                cleanup(file);
            }
            if (!file.getName().equals("database.properties"))
            {
                FileSystemUtils.delete(file);   
            }
        }
    }

    public void setUserPaths(MasterUserPaths paths)
    {
        this.paths = paths;
    }
}
