package com.zutubi.pulse.core.commands.maven2;

import com.zutubi.pulse.core.FileLoaderTestBase;
import com.zutubi.pulse.core.api.PulseException;
import com.zutubi.pulse.core.commands.core.RegexPostProcessor;

/**
 */
public class Maven2PostProcessorLoadTest extends FileLoaderTestBase
{
    public void setUp() throws Exception
    {
        super.setUp();

        loader.register("maven2.pp", Maven2PostProcessor.class);
    }

    public void testEmpty() throws PulseException
    {
        Maven2PostProcessor pp = referenceHelper("empty");
        assertEquals(3, pp.size());
    }

    public void testFailOnError() throws PulseException
    {
        Maven2PostProcessor pp = referenceHelper("fail");
        assertEquals(3, pp.size());
        assertTrue(((RegexPostProcessor)pp.get(1)).isFailOnError());
        assertTrue(((RegexPostProcessor)pp.get(2)).isFailOnError());
    }
}
