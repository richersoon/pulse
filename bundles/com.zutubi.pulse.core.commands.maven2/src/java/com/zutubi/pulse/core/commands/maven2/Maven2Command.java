package com.zutubi.pulse.core.commands.maven2;

import com.zutubi.pulse.core.MavenUtils;
import com.zutubi.pulse.core.PulseExecutionContext;
import com.zutubi.pulse.core.api.PulseException;
import com.zutubi.pulse.core.commands.api.CommandContext;
import com.zutubi.pulse.core.commands.core.NamedArgumentCommand;
import com.zutubi.pulse.core.postprocessors.api.Feature;
import com.zutubi.pulse.core.postprocessors.api.PostProcessorConfiguration;

import java.io.File;
import java.util.Arrays;
import java.util.List;

/**
 * Support for running Maven 2 - adds automatic version capturing.
 */
public class Maven2Command extends NamedArgumentCommand
{
    public Maven2Command(Maven2CommandConfiguration configuration)
    {
        super(configuration);
    }

    @Override
    protected List<Class<? extends PostProcessorConfiguration>> getDefaultPostProcessorTypes()
    {
        return Arrays.<Class<? extends PostProcessorConfiguration>>asList(Maven2PostProcessorConfiguration.class);
    }

    @Override
    public void execute(CommandContext commandContext)
    {
        super.execute(commandContext);

        try
        {
            //TODO: use the context's variables to transfer this maven specific information around.
            PulseExecutionContext pec = (PulseExecutionContext) commandContext.getExecutionContext();
            pec.setVersion(MavenUtils.extractVersion(new File(getWorkingDir(pec.getWorkingDir()), "pom.xml"), "version"));
        }
        catch (PulseException e)
        {
            commandContext.addFeature(new Feature(Feature.Level.WARNING, e.getMessage()));
        }
    }
}