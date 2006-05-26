package com.zutubi.pulse.slave;

import com.zutubi.pulse.bootstrap.ComponentContext;
import com.zutubi.pulse.bootstrap.Startup;
import com.zutubi.pulse.bootstrap.StartupException;
import com.zutubi.pulse.core.ObjectFactory;
import com.zutubi.pulse.core.Stoppable;
import com.zutubi.pulse.util.logging.Logger;
import org.mortbay.http.SocketListener;
import org.mortbay.jetty.Server;
import org.mortbay.util.InetAddrPort;

import java.util.List;

/**
 */
public class SlaveStartupManager implements Startup, Stoppable
{
    private static final Logger LOG = Logger.getLogger(SlaveStartupManager.class);

    private List<String> systemContexts;
    private List<String> startupRunnables;
    private ObjectFactory objectFactory;
    private Server jettyServer;
    private SlaveConfigurationManager configurationManager;

    public void init() throws StartupException
    {
        ComponentContext.addClassPathContextDefinitions(systemContexts.toArray(new String[systemContexts.size()]));

        runStartupRunnables();

        jettyServer = new Server();
        SocketListener listener = new SocketListener(new InetAddrPort(configurationManager.getAppConfig().getServerPort()));
        jettyServer.addListener(listener);
        try
        {
            jettyServer.addWebApplication("/", configurationManager.getSystemPaths().getContentRoot().getAbsolutePath());
            jettyServer.start();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    private void runStartupRunnables()
    {
        for (String name : startupRunnables)
        {
            try
            {
                Runnable instance = objectFactory.buildBean(name);
                instance.run();
            }
            catch (Exception e)
            {
                LOG.warning("Failed to run startup task " + name + ". Reason: " + e.getMessage(), e);
            }
        }
    }

    public void setSystemContexts(List<String> systemContexts)
    {
        this.systemContexts = systemContexts;
    }

    public void setStartupRunnables(List<String> startupRunnables)
    {
        this.startupRunnables = startupRunnables;
    }

    public void setObjectFactory(ObjectFactory objectFactory)
    {
        this.objectFactory = objectFactory;
    }

    public void setConfigurationManager(SlaveConfigurationManager configurationManager)
    {
        this.configurationManager = configurationManager;
    }

    public void stop(boolean force)
    {
        try
        {
            jettyServer.stop(true);
        }
        catch (InterruptedException e)
        {
            // Ignore.
        }
    }
}
