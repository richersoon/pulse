package com.zutubi.pulse.master.tove.config.project;

import com.zutubi.pulse.master.tove.handler.MapOption;
import com.zutubi.pulse.master.tove.handler.MapOptionProvider;
import com.zutubi.tove.type.TypeProperty;
import com.zutubi.tove.config.ConfigurationProvider;
import com.zutubi.tove.config.api.Configuration;
import com.zutubi.i18n.Messages;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * An option provider that gives a list of stage names along with the option to select [all stages].
 */
public class DependencyStagesOptionProvider extends MapOptionProvider
{
    private static final Messages I18N = Messages.getInstance(DependencyStagesOptionProvider.class);

    private ConfigurationProvider configurationProvider;

    protected Map<String, String> getMap(Object instance, String parentPath, TypeProperty property)
    {
        Configuration c = configurationProvider.get(parentPath, Configuration.class);
        ProjectConfiguration projectConfig = configurationProvider.getAncestorOfType(c, ProjectConfiguration.class);

        Map<String, String> options = new LinkedHashMap<String, String>();
        options.put(DependencyConfiguration.ALL_STAGES, allStages());

        Map<String, BuildStageConfiguration> stages = projectConfig.getStages();
        for (BuildStageConfiguration stage : stages.values())
        {
            options.put(stage.getName(), stage.getName());
        }

        return options;
    }

    public MapOption getEmptyOption(Object instance, String parentPath, TypeProperty property)
    {
        return null;
    }

    private String allStages()
    {
        return "["+I18N.format("all.stages")+"]";
    }

    public void setConfigurationProvider(ConfigurationProvider configurationProvider)
    {
        this.configurationProvider = configurationProvider;
    }
}
