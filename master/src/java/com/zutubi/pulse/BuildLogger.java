package com.zutubi.pulse;

import com.zutubi.pulse.events.build.BuildEvent;

/**
 *
 *
 */
public interface BuildLogger
{
    /**
     * Initialise any required resources.  This method will be called before any logging
     * requestes are made.
     */
    void prepare();

    void log(BuildEvent event);

    /**
     * Close any held resources.  This method will be called after the final logging
     * request is made.
     */
    void done();
}
