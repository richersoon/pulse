package com.zutubi.pulse.events;

import com.zutubi.util.logging.Logger;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * <class-comment/>
 */
public class AsynchronousDispatcher implements EventDispatcher
{
    private static final Logger LOG = Logger.getLogger(AsynchronousDispatcher.class);

    private final ExecutorService executor;

    public AsynchronousDispatcher()
    {
        executor = Executors.newSingleThreadExecutor();
    }

    public void dispatch(final Event evt, final List<EventListener> listeners)
    {
        executor.execute(new Runnable()
        {
            public void run()
            {
                for (EventListener listener: listeners)
                {
                    try
                    {
                        listener.handleEvent(evt);
                    }
                    catch (Exception e)
                    {
                        // isolate the exceptions generated by the event handling.
                        LOG.warning("Exception generated by "+listener+".handleEvent("+evt+")", e);
                    }
                }
            }
        });
    }
}
