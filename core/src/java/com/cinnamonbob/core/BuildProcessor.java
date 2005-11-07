package com.cinnamonbob.core;

import com.cinnamonbob.core.event.EventManager;
import com.cinnamonbob.core.model.BuildResult;
import com.cinnamonbob.core.model.CommandResult;

import java.io.File;
import java.io.FileInputStream;
import java.util.LinkedList;
import java.util.List;

/**
 */
public class BuildProcessor
{
    private EventManager eventManager;
    private FileLoader fileLoader;

    public BuildProcessor()
    {
        // For use with Spring
    }

    public BuildProcessor(EventManager eventManager, FileLoader fileLoader)
    {
        setEventManager(eventManager);
        setFileLoader(fileLoader);
    }

    public static String getCommandDirName(int i, CommandResult result)
    {
        // Use the command name because:
        // a) we do not have an id for the command model
        // b) for local builds, this is a lot friendlier for the developer
        return String.format("%08d-%s", i, result.getCommandName());
    }

    public void build(File workDir, String bobFileName, String recipeName, BuildResult buildResult, File outputDir) throws BuildException
    {
        BobFile bobFile = loadBobFile(workDir, bobFileName);
        Recipe recipe;

        if(recipeName == null)
        {
            recipeName = bobFile.getDefaultRecipe();
        }

        recipe = bobFile.getRecipe(recipeName);
        if (recipe == null)
        {
            throw new BuildException("Undefined recipe '" + recipeName + "'");
        }

        // TODO: support continuing build when errors occur. Take care: exceptions.
        int i = 0;
        for (Command command : recipe.getCommands())
        {
            CommandResult result = new CommandResult(command.getName());

            buildResult.add(result);

            File commandOutput = new File(outputDir, getCommandDirName(i, result));
            result.commence(commandOutput);
            eventManager.publish(new CommandCommencedEvent(this, buildResult));

            try
            {
                if(!commandOutput.mkdir())
                {
                    throw new BuildException("Could not create command output directory '" + commandOutput.getAbsolutePath() + "'");
                }

                command.execute(commandOutput, result);
            }
            catch(BuildException e)
            {
                result.error(e);
            }
            catch(Exception e)
            {
                //LOG.log(Level.SEVERE, "Unhandled exception during build", e);
                result.error(new BuildException(e));
            }
            finally
            {
                result.complete();
                eventManager.publish(new CommandCompletedEvent(this, buildResult));
            }

            switch(result.getState())
            {
                case FAILURE:
                    buildResult.failure();
                    return;
                case ERROR:
                    buildResult.commandError();
                    return;
            }

            i++;
        }
    }

    private BobFile loadBobFile(File workDir, String bobFileName) throws BuildException
    {
        List<Reference> properties = new LinkedList<Reference>();

        Property property = new Property("work.dir", workDir.getAbsolutePath());
        properties.add(property);

        try
        {
            BobFile         result = new BobFile();
            File            bob    = new File(workDir, bobFileName);
            FileInputStream stream = new FileInputStream(bob);

            fileLoader.load(stream, result, properties);
            return result;
        }
        catch (Exception e)
        {
            throw new BuildException(e);
        }
    }

    public void setFileLoader(FileLoader fileLoader)
    {
        this.fileLoader = fileLoader;

        // TODO: move config into file.
        fileLoader.register("property", Property.class);
        fileLoader.register("recipe", Recipe.class);
        fileLoader.register("def", ComponentDefinition.class);
        fileLoader.register("post-processor", PostProcessorGroup.class);
        fileLoader.register("command", CommandGroup.class);
        fileLoader.register("regex", RegexPostProcessor.class);
        fileLoader.register("executable", ExecutableCommand.class);
        fileLoader.register("resource", ResourceReference.class);
    }

    public void setEventManager(EventManager eventManager)
    {
        this.eventManager = eventManager;
    }
}
