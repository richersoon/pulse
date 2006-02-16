package com.cinnamonbob.model;

import com.cinnamonbob.core.util.FileSystemUtils;

import java.io.IOException;

/**
 */
public class MakeBobFileDetailsTest extends TemplateBobFileDetailsTest
{
    private MakeBobFileDetails details;

    protected void setUp() throws Exception
    {
        details = new MakeBobFileDetails();
        super.setUp();
    }

    protected void tearDown() throws Exception
    {
        super.tearDown();
        FileSystemUtils.removeDirectory(tmpDir);
    }

    public TemplateBobFileDetails getDetails()
    {
        return details;
    }

    public void testBasic() throws IOException
    {
        createAndVerify("basic");
    }

    public void testExplicitBuildFile() throws IOException
    {
        details.setMakefile("test.makefile");
        createAndVerify("explicitMakefile");
    }

    public void testEnvironment() throws IOException
    {
        details.addEnvironmentalVariable("var", "value");
        details.addEnvironmentalVariable("var2", "value2");
        createAndVerify("environment");
    }

    public void testExplicitTargets() throws IOException
    {
        details.setTargets("build test");
        createAndVerify("explicitTargets");
    }
}
