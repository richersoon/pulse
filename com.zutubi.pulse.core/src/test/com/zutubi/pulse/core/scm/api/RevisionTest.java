package com.zutubi.pulse.core.scm.api;

import com.zutubi.pulse.core.test.api.PulseTestCase;

public class RevisionTest extends PulseTestCase
{
    private static final String SHORT_STRING = "short";
    private static final String LONG_STRING = "this one is long";

    public void testIsNumericNumber()
    {
        assertTrue(new Revision(12).isNumeric());
    }

    public void testIsNumericLargeNumber()
    {
        assertTrue(new Revision(Long.MAX_VALUE).isNumeric());
    }

    public void testIsNumericAlpha()
    {
        assertFalse(new Revision("abcd").isNumeric());
    }
    
    public void testIsNumericAlphaNumeric()
    {
        assertFalse(new Revision("123abcd").isNumeric());
    }

    public void testIsNumericFreakSha()
    {
        // If unlucky, a 40-character SHA could be entirely numeric.
        // Happily, this would almost certainly make it too large to
        // parse as a long.
        assertFalse(new Revision("0000000000000000000100000000000000000000").isNumeric());
    }

    public void testIsAbbreviatedNumeric()
    {
        assertFalse(new Revision(111).isAbbreviated());
    }

    public void testIsAbbreviatedLargeNumber()
    {
        assertFalse(new Revision(Long.MAX_VALUE).isAbbreviated());
    }

    public void testIsAbbreviatedShortString()
    {
        assertFalse(new Revision(SHORT_STRING).isAbbreviated());
    }

    public void testIsAbbreviatedLongString()
    {
        assertTrue(new Revision(LONG_STRING).isAbbreviated());
    }

    public void tesGetAbbreviatedNumeric()
    {
        assertEquals("111", new Revision(111).getAbbreviatedRevisionString());
    }

    public void testGetAbbreviatedLargeNumber()
    {
        assertEquals(Long.toString(Long.MAX_VALUE), new Revision(Long.MAX_VALUE).getAbbreviatedRevisionString());
    }

    public void testGetAbbreviatedShortString()
    {
        assertEquals(SHORT_STRING, new Revision(SHORT_STRING).getAbbreviatedRevisionString());
    }

    public void testGetAbbreviatedLongString()
    {
        String expected = LONG_STRING.substring(0, Revision.ABBREVIATION_LIMIT - Revision.ELLIPSIS.length()) + Revision.ELLIPSIS;
        assertEquals(expected, new Revision(LONG_STRING).getAbbreviatedRevisionString());
    }
}
