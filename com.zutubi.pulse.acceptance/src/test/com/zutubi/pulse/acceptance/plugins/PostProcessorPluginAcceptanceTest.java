package com.zutubi.pulse.acceptance.plugins;

import com.zutubi.pulse.core.PulseFile;
import com.zutubi.pulse.core.PulseFileLoader;
import com.zutubi.pulse.core.PulseFileLoaderFactory;
import com.zutubi.pulse.core.api.PulseException;
import com.zutubi.pulse.core.engine.api.Reference;
import com.zutubi.pulse.core.plugins.Plugin;
import com.zutubi.pulse.core.plugins.PostProcessorExtensionManager;
import com.zutubi.pulse.core.test.PulseTestCase;
import com.zutubi.util.FileSystemUtils;
import com.zutubi.util.bean.DefaultObjectFactory;

import java.io.File;

public class PostProcessorPluginAcceptanceTest extends PulseTestCase
{
    private PostProcessorExtensionManager extensionManager;

    private PulseFileLoaderFactory loaderFactory;
    private File tmpDir;
    private PluginSystem pluginSystem;
    private File samplePostProcessorPlugin;

    protected void setUp() throws Exception
    {
        super.setUp();

        // base directory will be cleaned up at the end of the test.
        tmpDir = FileSystemUtils.createTempDir();

        File pkgFile = getPulsePackage();
        if (!pkgFile.exists())
        {
            fail("Pulse package file '" + pkgFile.getAbsolutePath() + "'does not exist.");
        }

        File dataDir = new File(getPulseRoot(), FileSystemUtils.join("com.zutubi.pulse.acceptance", "src", "test", "data"));
        samplePostProcessorPlugin = new File(dataDir, "com.zutubi.bundles.postprocessor.sample_1.0.0.jar");

        pluginSystem = new PluginSystem(pkgFile, tmpDir);
        pluginSystem.startup();

        loaderFactory = new PulseFileLoaderFactory();
        loaderFactory.setObjectFactory(new DefaultObjectFactory());

        extensionManager = new PostProcessorExtensionManager();
        extensionManager.setPluginManager(pluginSystem.getPluginManager());
        extensionManager.setFileLoaderFactory(loaderFactory);
        extensionManager.init();
        extensionManager.initialiseExtensions();
    }

    protected void tearDown() throws Exception
    {
        pluginSystem.shutdown();
        removeDirectory(tmpDir);

        super.tearDown();
    }

    public void testPostProcessorPlugins() throws PulseException, InterruptedException
    {
        // install test plugin.
        Plugin plugin = pluginSystem.install(samplePostProcessorPlugin);
        assertEquals(Plugin.State.ENABLED, plugin.getState());

        // need to yield since the extension manager is notified of the new plugin asynchronously.
        Thread.yield();

        // ensure that we are picking up the expected post processors from the installed plugin.
        assertNotNull(extensionManager.getPostProcessor("sample.pp"));

        PulseFile pf = new PulseFile();

        PulseFileLoader loader = loaderFactory.createLoader();
        loader.load(getInput("testPostProcessorPlugin"), pf);

        Reference ref = pf.getReference("sample.pp");

        // verify that the reference is to the expected instance.
        assertNotNull(ref.getValue());
    }
}
