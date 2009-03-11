package com.zutubi.pulse.core.commands.xcode;

import com.zutubi.pulse.core.FileLoaderTestBase;
import com.zutubi.pulse.core.api.PulseException;
import com.zutubi.pulse.core.test.EqualityAssertions;

import java.util.Arrays;
import java.util.Collections;

/**
 */
public class XCodeCommandLoadTest extends FileLoaderTestBase
{
    public void setUp() throws Exception
    {
        super.setUp();

        loader.register("xcodebuild", XCodeCommand.class);
    }

    public void testEmpty() throws PulseException
    {
        XCodeCommand command = commandHelper("empty");
        assertNull(command.getProject());
        assertNull(command.getConfig());
        assertNull(command.getTarget());
        assertNull(command.getBuildaction());
        assertNull(command.getSettings());
    }

    public void testAll() throws PulseException
    {
        XCodeCommand command = commandHelper("all");
        assertEquals("testproject", command.getProject());
        assertEquals("testconfig", command.getConfig());
        assertEquals("testtarget", command.getTarget());
        assertEquals("testbuildaction", command.getBuildaction());
        EqualityAssertions.assertEquals(Arrays.asList("test=value", "test2=value2"), command.getSettings());
    }

    public void testEmptySettings() throws PulseException
    {
        XCodeCommand command = commandHelper("emptysettings");
        EqualityAssertions.assertEquals(Collections.emptyList(), command.getSettings());
    }

    public void testQuotedSettings() throws PulseException
    {
        XCodeCommand command = commandHelper("quotedsettings");
        EqualityAssertions.assertEquals(Arrays.asList("test=value", "test2=value with spaces"), command.getSettings());
    }

    public void testSlashedSettings() throws PulseException
    {
        XCodeCommand command = commandHelper("slashedsettings");
        EqualityAssertions.assertEquals(Arrays.asList("setting=value with spaces"), command.getSettings());
    }
}