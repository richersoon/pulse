package com.zutubi.pulse;

import com.zutubi.pulse.core.BuildException;
import com.zutubi.pulse.core.model.CommandResult;
import com.zutubi.pulse.core.model.RecipeResult;
import com.zutubi.pulse.events.build.*;
import com.zutubi.util.IOUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.util.Date;

/**
 * A logger that writes the details out to a log file.
 */
public class DefaultRecipeLogger implements RecipeLogger
{
    private static final String PRE_RULE = "============================[ command output below ]============================";
    private static final String POST_RULE = "============================[ command output above ]============================";
    private static final DateFormat FORMAT = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.LONG);

    private File logFile = null;
    private PrintWriter writer = null;

    public DefaultRecipeLogger(File logFile)
    {
        this.logFile = logFile;
    }

    public void prepare()
    {
        try
        {
            writer = new PrintWriter(logFile);
        }
        catch (FileNotFoundException e)
        {
            throw new BuildException("Unable to create recipe log file '" + logFile.getAbsolutePath() + "': " + e.getMessage(), e);
        }
    }

    public void log(RecipeDispatchedEvent event)
    {
        logMarker("Recipe dispatched to agent " + event.getAgent().getAgentConfig().getName(), System.currentTimeMillis());
    }

    public void log(RecipeCommencedEvent event, RecipeResult result)
    {
        logMarker("Recipe '" + result.getRecipeNameSafe() + "' commenced", result.getStamps().getStartTime());
    }

    public void log(CommandCommencedEvent event, CommandResult result)
    {
        logMarker("Command '" + result.getCommandName() + "' commenced", result.getStamps().getStartTime());
        if (writer != null)
        {
            writer.println(PRE_RULE);
            writer.flush();
        }
    }

    public void log(CommandOutputEvent event)
    {
        if (writer != null)
        {
            writer.print(new String(event.getData()));
            writer.flush();
        }
    }

    public void log(CommandCompletedEvent event, CommandResult result)
    {
        if (writer != null)
        {
            writer.println(POST_RULE);
            writer.flush();
        }

        logMarker("Command '" + result.getCommandName() + "' completed with status " + result.getState().getPrettyString(), result.getStamps().getEndTime());
    }

    public void log(RecipeCompletedEvent event, RecipeResult result)
    {
        // Do nothing: completion message comes from complete
    }

    public void log(RecipeErrorEvent event, RecipeResult result)
    {
        logMarker("Recipe terminated with an error: " + event.getErrorMessage(), System.currentTimeMillis());
    }

    public void complete(RecipeResult result)
    {
        logMarker("Recipe '" + result.getRecipeNameSafe() + "' completed with status " + result.getState().getPrettyString(), result.getStamps().getEndTime());
    }

    public void collecting(RecipeResult recipeResult, boolean collectWorkingCopy)
    {
        logMarker("Collecting recipe artifacts" + (collectWorkingCopy ? " and working copy..." : "..."));
    }

    public void collectionComplete()
    {
        logMarker("Collection complete");
        IOUtils.close(writer);
    }

    private void logMarker(String message)
    {
        logMarker(message, System.currentTimeMillis());
    }

    private void logMarker(String message, long time)
    {
        if (writer != null)
        {
            writer.print(FORMAT.format(new Date(time)));
            writer.print(": ");
            writer.println(message);
            writer.flush();
        }
    }
}
