package com.cinnamonbob.local;

import com.cinnamonbob.test.BobTestCase;
import com.cinnamonbob.core.util.FileSystemUtils;
import com.cinnamonbob.core.util.IOUtils;
import com.cinnamonbob.core.BobException;

import java.io.*;
import java.net.URL;

/**
 */
public class LocalBuildTest extends BobTestCase
{
    File tmpDir;
    boolean generateMode = false;
    LocalBuild builder;

    @Override
    protected void setUp() throws Exception
    {
        super.setUp();
        // Create a temporary working directory
        tmpDir = FileSystemUtils.createTempDirectory(LocalBuildTest.class.getName(), "");
        builder = new LocalBuild();
    }

    @Override
    protected void tearDown() throws Exception
    {
        super.tearDown();
        FileSystemUtils.removeDirectory(tmpDir);
    }

    private File getExpectedOutput(String name)
    {
        URL url = getClass().getResource(getClass().getSimpleName() + ".basic.xml");
        File xmlFile = new File(url.getFile());
        File dataDir = new File(xmlFile.getParentFile(), "data");

        return new File(dataDir, name);
    }

    private String copyFile(String name) throws IOException
    {
        URL bobURL = getInputURL(name);
        File srcFile = new File(bobURL.getFile());
        File destFile = new File(tmpDir, srcFile.getName());

        IOUtils.copyFile(srcFile, destFile);
        return srcFile.getName();
    }

    public void testBasicBuild() throws Exception
    {
        String bobFile = copyFile("basic");

        builder.runBuild(tmpDir, bobFile, "my-default", null, "out");
        compareOutput("basic", "out");
    }

    public void testInvalidWorkDir()
    {
        File workDir = new File("/no/such/dir");
        try
        {
            builder.runBuild(workDir, "bob.xml", "my-default", null, "out");
        }
        catch (BobException e)
        {
            assertEquals("Working directory '" + workDir.getAbsolutePath() + "' does not exist", e.getMessage());
            return;
        }

        assertTrue("Expected exception", false);
    }

    public void testInvalidBobFile() throws BobException, IOException
    {
        builder.runBuild(tmpDir, "no-such-bob.xml", "my-default", null, "out");
        File expectedDir = getExpectedOutput("invalidBobFile");
        File actualDir = new File(tmpDir, "out");

        cleanBuildLog(new File(actualDir, "build.log"));

        if (generateMode)
        {
            tmpDir.renameTo(expectedDir);
        }
        else
        {
            assertFilesEqual(new File(expectedDir, "build.log.cleaned"), new File(actualDir, "build.log.cleaned"));
            // Just verify exception file exists, content is too difficult...
            assertTrue((new File(actualDir, "exception")).isFile());
        }
    }

    public void testLoadResources() throws IOException, BobException
    {
        String bobFile = copyFile("resourceload");
        String resourceFile = getInputURL("resources").getFile();

        builder.runBuild(tmpDir, bobFile, null, resourceFile, "out");
        compareOutput("resourceload", "out");
    }

    private void cleanBuildLog(File log) throws IOException
    {
        File output = new File(log.getAbsolutePath() + ".cleaned");

        BufferedReader reader = null;
        BufferedWriter writer = null;

        try
        {
            reader = new BufferedReader(new FileReader(log));
            writer = new BufferedWriter(new FileWriter(output));
            String line;

            while ((line = reader.readLine()) != null)
            {
                line = line.replaceFirst("commenced:.*", "commenced:");
                line = line.replaceFirst("completed:.*", "completed:");
                line = line.replaceFirst("elapsed  :.*", "elapsed  :");
                line = line.replaceFirst("The system cannot find the file specified", "No such file or directory");
                line = line.replace(tmpDir.getAbsolutePath(), "tmpDir");
                line = line.replaceAll("\\\\", "/");
                writer.write(line);
                writer.newLine();
            }
        }
        finally
        {
            IOUtils.close(reader);
            IOUtils.close(writer);
        }

        log.delete();
    }

    private void compareOutput(String expectedName, String actualName) throws IOException
    {
        File expectedDir = getExpectedOutput(expectedName);
        File actualDir = new File(tmpDir, actualName);

        cleanBuildLog(new File(actualDir, "build.log"));

        if (generateMode)
        {
            actualDir.renameTo(expectedDir);
        }
        else
        {
            assertDirectoriesEqual(expectedDir, actualDir);
        }
    }
}
