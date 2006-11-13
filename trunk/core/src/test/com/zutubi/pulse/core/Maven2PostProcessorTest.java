package com.zutubi.pulse.core;

import com.zutubi.pulse.core.model.CommandResult;
import com.zutubi.pulse.util.SystemUtils;

import java.io.IOException;

/**
 * Tests for RegexPostProcessor.
 */
public class Maven2PostProcessorTest extends PostProcessorTestBase
{
    private Maven2PostProcessor pp;

    public void setUp() throws IOException
    {
        pp = new Maven2PostProcessor();
        super.setUp();
    }

    public void tearDown()
    {
        pp = null;
        super.tearDown();
    }

    public void testSuccess() throws Exception
    {
        CommandResult result = createAndProcessArtifact("success", pp);
        assertTrue(result.succeeded());
        assertEquals(0, artifact.getFeatures().size());
    }

    public void testNoPOM() throws Exception
    {
        createAndProcessArtifact("nopom", pp);
        assertErrors("[INFO] ----------------------------------------------------------------------------\n" +
                "[ERROR] BUILD ERROR\n" +
                "[INFO] ----------------------------------------------------------------------------\n" +
                "[INFO] Cannot execute mojo: resources. It requires a project, but the build is not using one.\n" +
                "[INFO] ----------------------------------------------------------------------------\n" +
                "[INFO] For more information, run Maven with the -e switch\n" +
                "[INFO] ----------------------------------------------------------------------------\n" +
                "[INFO] ----------------------------------------------------------------------------");
    }

    public void testNoGoal() throws Exception
    {
        createAndProcessArtifact("nogoal", pp);
        assertErrors("[INFO] ----------------------------------------------------------------------------\n" +
                "[ERROR] BUILD FAILURE\n" +
                "[INFO] ----------------------------------------------------------------------------\n" +
                "[INFO] You must specify at least one goal. Try 'install'\n" +
                "[INFO] ----------------------------------------------------------------------------\n" +
                "[INFO] For more information, run Maven with the -e switch\n" +
                "[INFO] ----------------------------------------------------------------------------\n" +
                "[INFO] ----------------------------------------------------------------------------");
    }

    public void testCompilerError() throws Exception
    {
        CommandResult result = createAndProcessArtifact("compilererror", pp);
        assertErrors("[INFO] ----------------------------------------------------------------------------\n" +
                "[ERROR] BUILD FAILURE\n" +
                "[INFO] ----------------------------------------------------------------------------\n" +
                "[INFO] Compilation failure\n" +
                "\n" +
                "base.dir/src/main/java/com/zutubi/maven2/test/App.java:[12,4] ';' expected\n" +
                "\n" +
                "");

        if(SystemUtils.IS_WINDOWS)
        {
            assertTrue(result.failed());
        }
        else
        {
            assertTrue(result.succeeded());
        }
    }

    public void testTestFailure() throws Exception
    {
        CommandResult result = createAndProcessArtifact("testfailure", pp);
        assertErrors("[surefire] Running com.zutubi.maven2.test.AppTest\n" +
                "[surefire] Tests run: 1, Failures: 1, Errors: 0, Time elapsed: x sec <<<<<<<< FAILURE !! ",

                "[INFO] ----------------------------------------------------------------------------\n" +
                "[ERROR] BUILD ERROR\n" +
                "[INFO] ----------------------------------------------------------------------------\n" +
                "[INFO] There are test failures.\n" +
                "[INFO] ----------------------------------------------------------------------------\n" +
                "[INFO] For more information, run Maven with the -e switch\n" +
                "[INFO] ----------------------------------------------------------------------------\n" +
                "[INFO] ----------------------------------------------------------------------------");

        if(SystemUtils.IS_WINDOWS)
        {
            assertTrue(result.failed());
        }
        else
        {
            assertTrue(result.succeeded());
        }
    }

    public void testSuccessfulError() throws Exception
    {
        CommandResult result = createAndProcessArtifact("successfulerror", pp);
        assertErrors("[INFO] Generate \"Continuous Integration\" report.\n" +
                "[ERROR] VM #displayTree: error : too few arguments to macro. Wanted 2 got 0\n" +
                "[ERROR] VM #menuItem: error : too few arguments to macro. Wanted 1 got 0\n" +
                "[INFO] Generate \"Dependencies\" report.");
        assertTrue(result.succeeded());
    }
}
