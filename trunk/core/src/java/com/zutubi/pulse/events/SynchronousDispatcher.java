package com.zutubi.pulse.events;

import com.zutubi.pulse.util.logging.Logger;

import java.util.List;

/**
 * <class-comment/>
 */
public class SynchronousDispatcher implements EventDispatcher
{
    private static final Logger LOG = Logger.getLogger(SynchronousDispatcher.class);

    public void dispatch(Event evt, List<EventListener> listeners)
    {
        for (EventListener listener : listeners)
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
}
