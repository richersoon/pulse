package com.zutubi.pulse.core.dependency.ivy;

import com.zutubi.tove.type.record.PathUtils;
import com.zutubi.util.junit.ZutubiTestCase;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;

public class IvyClientTest extends ZutubiTestCase
{
    private IvyClient client;

    private File tmp;
    private File repositoryBase;
    private File workBase;
    private File cacheBase;

    private IvyModuleDescriptor descriptor;
    private IvyConfiguration configuration;

    @Override
    protected void setUp() throws Exception
    {
        super.setUp();

        tmp = createTempDirectory();
        repositoryBase = new File(tmp, "repository");
        workBase = new File(tmp, "work");
        cacheBase = new File(tmp, "cache");

        configuration = new IvyConfiguration(repositoryBase.toURI().toString());
        configuration.setCacheBase(cacheBase);
        client = new IvyClient(configuration);
//        client.pushMessageLogger(new DefaultMessageLogger(Message.MSG_VERBOSE));

        descriptor = new IvyModuleDescriptor("org", "module", "revision", configuration);
    }

    @Override
    protected void tearDown() throws Exception
    {
        removeDirectory(tmp);

        super.tearDown();
    }

    public void testPublishNoArtifacts() throws IOException
    {
        client.publishArtifacts(descriptor);

        assertEquals(null, repositoryBase.list());
    }

    public void testPublishSingleArtifact() throws IOException
    {
        descriptor.addArtifact(createArtifact("artifact.jar"), "build");

        client.publishArtifacts(descriptor);

        assertExists(repositoryBase, "org/module/build/jars/artifact-revision.jar");
    }

    public void testPublishMultipleArtifacts() throws IOException
    {
        descriptor.addArtifact(createArtifact("artifactA.jar"), "build");
        descriptor.addArtifact(createArtifact("artifactB.jar"), "build");

        client.publishArtifacts(descriptor);

        assertExists(repositoryBase, "org/module/build/jars/artifactA-revision.jar");
        assertExists(repositoryBase, "org/module/build/jars/artifactB-revision.jar");
    }

    public void testPublishArtifactByConf() throws IOException
    {
        descriptor.addArtifact(createArtifact("artifactA.jar"), "buildA");
        descriptor.addArtifact(createArtifact("artifactB.jar"), "buildB");

        client.publishArtifacts(descriptor, "buildA");

        assertExists(repositoryBase, "org/module/buildA/jars/artifactA-revision.jar");
        assertNotExists(repositoryBase, "org/module/buildB/jars/artifactB-revision.jar");
    }

    public void testPublishDescriptor() throws IOException, ParseException
    {
        client.publishDescriptor(descriptor);
        
        assertExists(repositoryBase, "org/module/ivy-revision.xml");
    }

    public void testRetrieveArtifact() throws IOException, ParseException
    {
        descriptor.addArtifact(createArtifact("artifact.jar"), "build");
        client.publishArtifacts(descriptor);
        client.publishDescriptor(descriptor);

        IvyModuleDescriptor retrievalDescriptor = new IvyModuleDescriptor("org", "moduleB", "revision", configuration);
        retrievalDescriptor.addDependency(descriptor.getDescriptor().getModuleRevisionId());

        IvyRetrievalReport report = client.retrieveArtifacts(retrievalDescriptor.getDescriptor(), workBase.getCanonicalPath() + "/[artifact]-[revision].[ext]");

        assertExists(workBase, "artifact-revision.jar");
        assertEquals(1, report.getRetrievedArtifacts().size());
    }

    public void testRetrieveMultipleArtifacts() throws IOException, ParseException
    {
        descriptor.addArtifact(createArtifact("artifactA.jar"), "build");
        descriptor.addArtifact(createArtifact("artifactB.jar"), "build");
        client.publishArtifacts(descriptor);
        client.publishDescriptor(descriptor);

        IvyModuleDescriptor retrievalDescriptor = new IvyModuleDescriptor("org", "moduleB", "revision", configuration);
        retrievalDescriptor.addDependency(descriptor.getDescriptor().getModuleRevisionId());

        String retrievalPattern = workBase.getCanonicalPath() + "/[artifact]-[revision].[ext]";
        IvyRetrievalReport report = client.retrieveArtifacts(retrievalDescriptor.getDescriptor(), retrievalPattern);

        assertExists(workBase, "artifactA-revision.jar");
        assertExists(workBase, "artifactB-revision.jar");
        assertEquals(2, report.getRetrievedArtifacts().size());
    }

    public void testTransitiveDependency() throws IOException, ParseException
    {
        descriptor.addArtifact(createArtifact("artifact.jar"), "build");
        client.publishArtifacts(descriptor);
        client.publishDescriptor(descriptor);

        IvyModuleDescriptor descriptorB = new IvyModuleDescriptor("org", "moduleB", "revision", configuration);
        descriptorB.addDependency(descriptor.getDescriptor().getModuleRevisionId(), true);
        client.publishDescriptor(descriptorB);

        IvyModuleDescriptor descriptorC = new IvyModuleDescriptor("org", "moduleC", "revision", configuration);
        descriptorC.addDependency(descriptor.getDescriptor().getModuleRevisionId(), true);

        String retrievalPattern = workBase.getCanonicalPath() + "/[artifact]-[revision].[ext]";
        IvyRetrievalReport report = client.retrieveArtifacts(descriptorC.getDescriptor(), retrievalPattern);

        assertExists(workBase, "artifact-revision.jar");
        assertEquals(1, report.getRetrievedArtifacts().size());
    }

    public void testRetrievalFailure() throws IOException, ParseException
    {
        descriptor.addArtifact(createArtifact("artifact.jar"), "build");
        client.publishDescriptor(descriptor);

        IvyModuleDescriptor retrievalDescriptor = new IvyModuleDescriptor("org", "moduleB", "revision", configuration);
        retrievalDescriptor.addDependency(descriptor.getDescriptor().getModuleRevisionId());

        String retrievalPattern = workBase.getCanonicalPath() + "/[artifact]-[revision].[ext]";
        IvyRetrievalReport report = client.retrieveArtifacts(retrievalDescriptor.getDescriptor(), retrievalPattern);
        assertTrue(report.hasFailures());
    }

    public void testResolveFailure() throws IOException, ParseException
    {
        descriptor.addArtifact(createArtifact("artifact.jar"), "build");

        IvyModuleDescriptor retrievalDescriptor = new IvyModuleDescriptor("org", "moduleB", "revision", configuration);
        retrievalDescriptor.addDependency(descriptor.getDescriptor().getModuleRevisionId());

        String retrievalPattern = workBase.getCanonicalPath() + "/[artifact]-[revision].[ext]";
        IvyRetrievalReport report = client.retrieveArtifacts(retrievalDescriptor.getDescriptor(), retrievalPattern);
        assertTrue(report.hasFailures());
    }

    public void testUnresolvedTransiantDependency() throws IOException, ParseException
    {
        descriptor.addArtifact(createArtifact("artifact.jar"), "build");

        IvyModuleDescriptor descriptorB = new IvyModuleDescriptor("org", "moduleB", "revision", configuration);
        descriptorB.addDependency(descriptor.getDescriptor().getModuleRevisionId(), true);
        client.publishDescriptor(descriptorB);

        IvyModuleDescriptor descriptorC = new IvyModuleDescriptor("org", "moduleC", "revision", configuration);
        descriptorC.addArtifact(createArtifact("artifactC.jar"), "build");
        descriptorC.addDependency(descriptorB.getDescriptor().getModuleRevisionId(), true);
        client.publishArtifacts(descriptorC);
        client.publishDescriptor(descriptorC);

        IvyModuleDescriptor descriptorD = new IvyModuleDescriptor("org", "moduleD", "revision", configuration);
        descriptorD.addDependency(descriptorC.getDescriptor().getModuleRevisionId(), true);

        String retrievalPattern = workBase.getCanonicalPath() + "/[artifact]-[revision].[ext]";
        IvyRetrievalReport report = client.retrieveArtifacts(descriptorD.getDescriptor(), retrievalPattern);
        assertTrue(report.hasFailures());
    }

    public void testNonTransitiveDependencyIsNotResolved() throws IOException, ParseException
    {
        descriptor.addArtifact(createArtifact("artifactA.jar"), "build");

        IvyModuleDescriptor descriptorB = new IvyModuleDescriptor("org", "moduleB", "revision", configuration);
        descriptorB.addDependency(descriptor.getDescriptor().getModuleRevisionId(), false);
        descriptorB.addArtifact(createArtifact("artifactB.jar"), "build");
        client.publishArtifacts(descriptorB);
        client.publishDescriptor(descriptorB);

        IvyModuleDescriptor descriptorC = new IvyModuleDescriptor("org", "moduleC", "revision", configuration);
        descriptorC.addDependency(descriptorB.getDescriptor().getModuleRevisionId(), false);

        String retrievalPattern = workBase.getCanonicalPath() + "/[artifact]-[revision].[ext]";
        IvyRetrievalReport report = client.retrieveArtifacts(descriptorC.getDescriptor(), retrievalPattern);
        assertFalse(report.hasFailures());
    }

    public void testRetrieveWhereNoDownloadIsRequired() throws IOException, ParseException
    {
        descriptor.addArtifact(createArtifact("artifactB.jar"), "build");
        client.publishArtifacts(descriptor);
        client.publishDescriptor(descriptor);

        IvyModuleDescriptor retrievalDescriptor = new IvyModuleDescriptor("org", "moduleB", "revision", configuration);
        retrievalDescriptor.addDependency(descriptor.getDescriptor().getModuleRevisionId());

        String retrievalPattern = workBase.getCanonicalPath() + "/[artifact]-[revision].[ext]";
        IvyRetrievalReport report = client.retrieveArtifacts(retrievalDescriptor.getDescriptor(), retrievalPattern);

        assertExists(workBase, "artifactB-revision.jar");
        assertEquals(1, report.getRetrievedArtifacts().size());

        report = client.retrieveArtifacts(retrievalDescriptor.getDescriptor(), retrievalPattern);
        assertEquals(1, report.getRetrievedArtifacts().size());
    }

    private File createArtifact(String path) throws IOException
    {
        String parentPath = PathUtils.getParentPath(path);
        File parentDir = (parentPath != null) ? new File(workBase, parentPath) : workBase;
        if (!parentDir.isDirectory() && !parentDir.mkdirs())
        {
            throw new RuntimeException();
        }

        String name = PathUtils.getBaseName(path);
        File artifact = new File(parentDir, name);
        if (!artifact.createNewFile())
        {
            throw new RuntimeException();
        }
        return artifact;
    }

    private void assertExists(File base, String path)
    {
        assertTrue(new File(base, path).isFile());
    }

    private void assertNotExists(File base, String path)
    {
        assertFalse(new File(base, path).isFile());
    }
}
