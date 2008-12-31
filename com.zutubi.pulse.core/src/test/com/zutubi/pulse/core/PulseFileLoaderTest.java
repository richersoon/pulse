package com.zutubi.pulse.core;

import com.zutubi.pulse.core.config.ResourceRequirement;
import com.zutubi.util.io.IOUtils;

import java.io.IOException;
import java.util.List;

/**
 */
public class PulseFileLoaderTest extends FileLoaderTestBase
{
    public void setUp() throws Exception
    {
        super.setUp();

        loader.register("resource", ResourceReference.class);
    }

    public void testLoaderRequireResources() throws Exception
    {
        List<ResourceRequirement> requirements = loader.loadRequiredResources(getPulseFile("requiredResources"), null);

        assertEquals(3, requirements.size());
        assertEquals("noversion", requirements.get(0).getResource());
        assertNull(requirements.get(0).getVersion());
        assertEquals("withversion", requirements.get(1).getResource());
        assertEquals("1", requirements.get(1).getVersion());
        assertEquals("explicitlyrequired", requirements.get(2).getResource());
        assertNull(requirements.get(2).getVersion());
    }

    public void testLoaderRequiredResourceNoName() throws Exception
    {
        try
        {
            loader.loadRequiredResources(getPulseFile("requiredResources"), "noname");
            fail();
        }
        catch (Exception e)
        {
            assertTrue(e.getMessage().contains("Required attribute name not specified"));
        }
    }

    private String getPulseFile(String name) throws IOException
    {
        return IOUtils.inputStreamToString(getInput(name, "xml"));
    }
}
