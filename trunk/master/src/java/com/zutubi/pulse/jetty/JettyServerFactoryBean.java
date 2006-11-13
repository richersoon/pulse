package com.zutubi.pulse.jetty;

import com.zutubi.pulse.bootstrap.MasterConfigurationManager;
import com.zutubi.pulse.bootstrap.SystemConfiguration;
import com.zutubi.pulse.util.logging.Logger;
import org.mortbay.http.SocketListener;
import org.mortbay.jetty.Server;
import org.springframework.beans.factory.FactoryBean;

/**
 * 
 *
 */
public class JettyServerFactoryBean implements FactoryBean
{
    private static final Logger LOG = Logger.getLogger(JettyServerFactoryBean.class);

    private Server instance;

    private MasterConfigurationManager configManager = null;

    public Object getObject() throws Exception
    {
        if (instance == null)
        {
            synchronized(this)
            {
                if (instance == null)
                {
                    instance = new Server();

                    // configuration of the server depends upon the configmanager.
                    SocketListener listener = new SocketListener();
                    SystemConfiguration systemConfiguration = configManager.getSystemConfig();
                    listener.setHost(systemConfiguration.getBindAddress());
                    listener.setPort(systemConfiguration.getServerPort());
                    instance.addListener(listener);
                }
            }
        }
        return instance;
    }

    public Class getObjectType()
    {
        return Server.class;
    }

    public boolean isSingleton()
    {
        return true;
    }

    public void setConfigurationManager(MasterConfigurationManager configManager)
    {
        this.configManager = configManager;
    }

    public void shutdown()
    {
        try
        {
            if (instance.isStarted())
            {
                instance.stop();
            }
        } 
        catch (InterruptedException e)
        {
            LOG.error(e);
        }
    }
}

