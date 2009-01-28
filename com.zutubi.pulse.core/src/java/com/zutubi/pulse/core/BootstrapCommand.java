package com.zutubi.pulse.core;

import com.zutubi.pulse.core.commands.api.CommandContext;

/**
 * An adaptation between the command and Bootstrap interfaces that allows
 * any bootstrapper to be run like a command.  This further allows the result
 * of bootstrapping to be stored as part of the recipe result.
 *
 */
public class BootstrapCommand implements Command
{
    public static final String OUTPUT_NAME = "bootstrap output";
    public static final String FILES_FILE = "files.txt";

    private BootstrapCommandConfiguration config;

    public BootstrapCommand(BootstrapCommandConfiguration config)
    {
        this.config = config;
    }

    public void execute(CommandContext commandContext)
    {
        commandContext.registerOutput(OUTPUT_NAME, null);
        config.getBootstrapper().bootstrap((PulseExecutionContext) commandContext.getExecutionContext());
    }

    public void terminate()
    {
        config.getBootstrapper().terminate();
    }
}
