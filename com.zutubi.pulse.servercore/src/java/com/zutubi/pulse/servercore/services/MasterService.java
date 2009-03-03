package com.zutubi.pulse.servercore.services;

import com.zutubi.events.Event;
import com.zutubi.pulse.core.config.ResourceConfiguration;

import java.util.List;

/**
 */
public interface MasterService
{
    void pong();

    void upgradeStatus(String token, UpgradeStatus upgradeStatus);

    void handleEvent(String token, Event event) throws InvalidTokenException;

    ResourceConfiguration getResource(String token, long handle, String name) throws InvalidTokenException;
    List<String> getResourceNames(String token, long handle) throws InvalidTokenException;
}
