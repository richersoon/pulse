package com.zutubi.pulse.core;

/**
 */
public class RegexTestPostProcessorLoadTest extends FileLoaderTestBase
{
    public void testStandard() throws PulseException
    {
        RegexTestPostProcessor pp = referenceHelper("tests.pp");
        assertEquals("tests.pp", pp.getName());
        assertEquals("\\[(.*)\\].*E[S|D]T:(.*)", pp.getRegex().trim());
        assertEquals(1, pp.getStatusGroup());
        assertEquals(2, pp.getNameGroup());
        assertEquals("PASS", pp.getPassStatus());
        assertEquals("FAIL", pp.getFailureStatus());
    }
}
