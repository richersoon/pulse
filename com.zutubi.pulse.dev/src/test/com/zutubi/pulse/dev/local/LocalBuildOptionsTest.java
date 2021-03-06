package com.zutubi.pulse.dev.local;

import com.zutubi.pulse.core.api.PulseException;
import com.zutubi.pulse.core.resources.ResourceRequirement;
import com.zutubi.pulse.core.test.api.PulseTestCase;

import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

public class LocalBuildOptionsTest extends PulseTestCase
{
    public void testSimpleRequirement() throws PulseException
    {
        resourceRequirementTest(new ResourceRequirement("simple", false, false), "-q", "simple");
    }

    public void testRequirementLongOpt() throws PulseException
    {
        resourceRequirementTest(new ResourceRequirement("myresource", false, false), "--require", "myresource");
    }

    public void testRequirementInverted() throws PulseException
    {
        resourceRequirementTest(new ResourceRequirement("myresource", true, false), "--require", "!myresource");
    }

    public void testMultipleRequirements() throws PulseException
    {
        LocalBuildOptions options = new LocalBuildOptions("-q", "r1", "-q", "r2");
        assertEquals(asList(new ResourceRequirement("r1", false, false), new ResourceRequirement("r2", false, false)), options.getResourceRequirements());
    }

    public void testRequirementWithVersion() throws PulseException
    {
        resourceRequirementTest(new ResourceRequirement("myresource", "myversion", false, false), "--require", "myresource/myversion");
    }

    public void testRequirementWithEmptyVersion() throws PulseException
    {
        resourceRequirementTest(new ResourceRequirement("myresource", false, false), "--require", "myresource/");
    }

    public void testRequirementWithEmptyName() throws PulseException
    {
        failedParseTest("Resource requirement '/version' has empty resource name", "-q", "/version");
    }

    public void testRequirementWithNoArg() throws PulseException
    {
        failedParseTest("no argument for:q", "-q");
    }

    private void failedParseTest(String expectedMessage, String... argv)
    {
        try
        {
            new LocalBuildOptions(argv);
            fail("Invalid options should not parse");
        }
        catch (PulseException e)
        {
            assertThat(e.getMessage(), containsString(expectedMessage));
        }
    }

    private void resourceRequirementTest(ResourceRequirement expected, String... argv) throws PulseException
    {
        LocalBuildOptions options = new LocalBuildOptions(argv);
        assertEquals(asList(expected), options.getResourceRequirements());
    }
}
