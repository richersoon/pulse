package com.zutubi.pulse.core.engine;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.zutubi.pulse.core.InMemoryResourceRepository;
import com.zutubi.pulse.core.resources.ResourceRequirement;
import com.zutubi.pulse.core.resources.api.ResourceConfiguration;
import com.zutubi.tove.annotations.SymbolicName;
import com.zutubi.tove.config.api.AbstractConfiguration;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static com.google.common.collect.Lists.transform;

/**
 * Configuration type used specifically for loading resources from a file.
 */
@SymbolicName("zutubi.resourcesConfig")
public class ResourcesConfiguration extends AbstractConfiguration
{
    private Map<String, ResourceConfiguration> resources = new HashMap<String, ResourceConfiguration>();
    private List<SimpleResourceRequirementConfiguration> requirements = new LinkedList<SimpleResourceRequirementConfiguration>();

    public Map<String, ResourceConfiguration> getResources()
    {
        return resources;
    }

    public void setResources(Map<String, ResourceConfiguration> resources)
    {
        this.resources = resources;
    }

    public List<SimpleResourceRequirementConfiguration> getRequirements()
    {
        return requirements;
    }

    public void setRequirements(List<SimpleResourceRequirementConfiguration> requirements)
    {
        this.requirements = requirements;
    }

    public InMemoryResourceRepository createRepository()
    {
        InMemoryResourceRepository repository = new InMemoryResourceRepository();
        for (ResourceConfiguration resourceConfiguration: resources.values())
        {
            repository.addResource(resourceConfiguration);
        }

        return repository;
    }

    public List<ResourceRequirement> createRequirements()
    {
        return Lists.newArrayList(transform(requirements, new Function<SimpleResourceRequirementConfiguration, ResourceRequirement>()
        {
            public ResourceRequirement apply(SimpleResourceRequirementConfiguration requirementConfiguration)
            {
                return requirementConfiguration.asResourceRequirement();
            }
        }));
    }
}
