package com.zutubi.pulse.master.tove.config.agent;

import com.zutubi.pulse.core.resources.api.ResourceConfiguration;
import com.zutubi.pulse.core.resources.api.ResourcePropertyConfiguration;
import com.zutubi.pulse.master.agent.HostLocation;
import com.zutubi.pulse.master.agent.SlaveProxyFactory;
import com.zutubi.tove.annotations.*;
import com.zutubi.tove.config.api.AbstractConfiguration;
import com.zutubi.tove.config.api.NamedConfiguration;
import com.zutubi.validation.Validateable;
import com.zutubi.validation.ValidationContext;
import com.zutubi.validation.annotations.Numeric;
import com.zutubi.validation.annotations.Required;

import java.net.MalformedURLException;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Configuration for an agent: a service that can execute recipes.
 */
@Form(fieldOrder = {"name", "remote", "host", "port", "ssl", "allowPersonalBuilds", "priority"})
@Table(columns = {"name", "location", "status"})
@SymbolicName("zutubi.agentConfig")
@Wire
public class AgentConfiguration extends AbstractConfiguration implements NamedConfiguration, Validateable, HostLocation
{
    @ExternalState
    private long agentStateId;
    @ControllingCheckbox(checkedFields = {"host", "port", "ssl"})
    private boolean remote = true;
    @NoInherit
    private String name;
    @Required @Select(optionProvider = "AgentHostOptionProvider", editable = true)
    private String host;
    @Numeric(min = 1)
    private int port = 8090;
    private boolean ssl;
    private boolean allowPersonalBuilds = true;
    @Numeric
    private int priority = 0;
    private AgentStorageConfiguration storage = new AgentStorageConfiguration();
    private Map<String, ResourceConfiguration> resources;
    @Ordered
    private Map<String, ResourcePropertyConfiguration> properties = new LinkedHashMap<String, ResourcePropertyConfiguration>();
    private List<AgentAclConfiguration> permissions = new LinkedList<AgentAclConfiguration>();

    @Transient
    private SlaveProxyFactory slaveProxyFactory;

    public AgentConfiguration()
    {
    }

    public AgentConfiguration(String name)
    {
        this.name = name;
    }

    public AgentConfiguration(String name, String host, int port)
    {
        remote = true;
        this.name = name;
        this.host = host;
        this.port = port;
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public boolean isRemote()
    {
        return remote;
    }

    public void setRemote(boolean remote)
    {
        this.remote = remote;
    }

    @Transient
    public String getHostName()
    {
        return getHost();
    }

    public String getHost()
    {
        return host;
    }

    public void setHost(String host)
    {
        this.host = host;
    }

    public int getPort()
    {
        return port;
    }

    public void setPort(int port)
    {
        this.port = port;
    }

    public boolean isSsl()
    {
        return ssl;
    }

    public void setSsl(boolean ssl)
    {
        this.ssl = ssl;
    }

    public boolean getAllowPersonalBuilds()
    {
        return allowPersonalBuilds;
    }

    public void setAllowPersonalBuilds(boolean allowPersonalBuilds)
    {
        this.allowPersonalBuilds = allowPersonalBuilds;
    }

    public int getPriority()
    {
        return priority;
    }

    public void setPriority(int priority)
    {
        this.priority = priority;
    }

    public AgentStorageConfiguration getStorage()
    {
        return storage;
    }

    public void setStorage(AgentStorageConfiguration storage)
    {
        this.storage = storage;
    }

    public long getAgentStateId()
    {
        return agentStateId;
    }

    public void setAgentStateId(long agentStateId)
    {
        this.agentStateId = agentStateId;
    }

    public Map<String, ResourcePropertyConfiguration> getProperties()
    {
        return properties;
    }

    public void setProperties(Map<String, ResourcePropertyConfiguration> properties)
    {
        this.properties = properties;
    }

    public Map<String, ResourceConfiguration> getResources()
    {
        return resources;
    }

    public void setResources(Map<String, ResourceConfiguration> resources)
    {
        this.resources = resources;
    }

    public List<AgentAclConfiguration> getPermissions()
    {
        return permissions;
    }

    public void setPermissions(List<AgentAclConfiguration> permissions)
    {
        this.permissions = permissions;
    }

    public void addPermission(AgentAclConfiguration permission)
    {
        permissions.add(permission);
    }

    public void validate(ValidationContext context)
    {
        if (remote && slaveProxyFactory != null)
        {
            try
            {
                slaveProxyFactory.unsafeCreateProxy(this);
            }
            catch (MalformedURLException e)
            {
                context.addFieldError("host", e.getMessage());
            }
        }
    }

    public void setSlaveProxyFactory(SlaveProxyFactory slaveProxyFactory)
    {
        this.slaveProxyFactory = slaveProxyFactory;
    }
}
