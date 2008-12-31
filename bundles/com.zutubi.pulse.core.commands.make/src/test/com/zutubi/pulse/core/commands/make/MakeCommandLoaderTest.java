package com.zutubi.pulse.core.commands.make;

import com.zutubi.pulse.core.Command;
import com.zutubi.pulse.core.FileLoaderTestBase;
import com.zutubi.pulse.core.PulseFile;
import com.zutubi.pulse.core.Recipe;

import java.util.List;

/**
 * <class comment/>
 */
public class MakeCommandLoaderTest extends FileLoaderTestBase
{
    public void setUp() throws Exception
    {
        super.setUp();

        loader.register("make", MakeCommand.class);
    }

    private MakeCommand makeCommandHelper(int commandIndex) throws Exception
    {
        PulseFile bf = new PulseFile();
        loader.load(getInput("testMakeCommand", "xml"), bf);

        List<Recipe> recipes = bf.getRecipes();
        assertEquals(recipes.size(), 1);

        Recipe recipe = recipes.get(0);
        List<Command> commands = recipe.getCommands();
        assertTrue(commands.get(commandIndex) instanceof MakeCommand);

        return (MakeCommand) commands.get(commandIndex);
    }

    public void testMakeCommandCustomExe() throws Exception
    {
        MakeCommand command = makeCommandHelper(1);
        assertEquals("mymake", command.getExe());
    }

    public void testMakeCommandTargets() throws Exception
    {
        MakeCommand command = makeCommandHelper(2);
        assertEquals("build test", command.getTargets());
    }

    public void testMakeCommandMakefile() throws Exception
    {
        MakeCommand command = makeCommandHelper(3);
        assertEquals("mymakefile", command.getMakefile());
    }
}
