package com.zutubi.pulse.master.bootstrap.velocity;

import com.zutubi.util.logging.Logger;
import org.apache.velocity.runtime.RuntimeServices;
import org.apache.velocity.runtime.log.LogSystem;

/**
 * Implementation of the velocity log system interface that maps velocity log events
 * to the Pulse logging system.
 */
public class CustomLogSystem implements LogSystem
{
    private static final Logger LOG = Logger.getLogger(CustomLogSystem.class);

    private boolean enabled = false;

    public void init(RuntimeServices runtimeServices) throws Exception
    {
        // Do nothing
    }

    public void logVelocityMessage(int level, String message)
    {
        // Ignore it all because Velocity's loggin is b0rked!
        if (enabled)
        {
            switch (level)
            {
                case DEBUG_ID:
                    LOG.debug(message);
                    break;
                case INFO_ID:
                    LOG.info(message);
                    break;
                case WARN_ID:
                    LOG.warning(message);
                    break;
                case ERROR_ID:
                    LOG.error(message);
                    break;
                default:
                    LOG.fine(message);
                    break;
            }
        }
    }
}
