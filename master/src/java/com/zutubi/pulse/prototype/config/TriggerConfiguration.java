package com.zutubi.pulse.prototype.config;

import com.zutubi.prototype.annotation.ID;

/**
 */
public class TriggerConfiguration
{
    @ID
    private String name;

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }
}
