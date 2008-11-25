package com.zutubi.pulse.core.postprocessors;

import com.zutubi.pulse.core.PulseExecutionContext;
import static com.zutubi.pulse.core.engine.api.BuildProperties.*;
import com.zutubi.pulse.core.engine.api.ExecutionContext;
import com.zutubi.pulse.core.model.*;
import com.zutubi.pulse.core.test.PulseTestCase;

import java.io.File;

/**
 */
public abstract class XMLTestReportPostProcessorTestBase extends PulseTestCase
{
    protected XMLTestReportPostProcessorSupport pp;

    protected XMLTestReportPostProcessorTestBase(XMLTestReportPostProcessorSupport pp)
    {
        this.pp = pp;
    }

    protected abstract File getOutputDir() throws Exception;

    protected StoredFileArtifact getArtifact(String name)
    {
        return new StoredFileArtifact(getClass().getSimpleName() + "." + name + ".xml");
    }

    protected PersistentTestSuiteResult runProcessor(String... names) throws Exception
    {
        File outputDir = getOutputDir();
        PersistentTestSuiteResult testResults = new PersistentTestSuiteResult();
        ExecutionContext context = new PulseExecutionContext();
        context.addValue(NAMESPACE_INTERNAL, PROPERTY_TEST_RESULTS, testResults);
        context.addString(NAMESPACE_INTERNAL, PROPERTY_OUTPUT_DIR, outputDir.getAbsolutePath());

        for(String name: names)
        {
            StoredFileArtifact artifact = getArtifact(name);

            File artifactFile = new File(outputDir.getAbsolutePath(), artifact.getPath());
            // not much point running a test if the artifact being processed does not exist.
            assertTrue("File " + artifactFile.getAbsolutePath() + " does not exist.", artifactFile.exists());

            pp.process(artifact, new CommandResult("test"), context);
        }
        
        return testResults;
    }

    protected void checkCase(PersistentTestCaseResult caseResult, String name, PersistentTestCaseResult.Status status, long duration, String message)
    {
        assertEquals(name, caseResult.getName());
        assertEquals(status, caseResult.getStatus());
        assertEquals(duration, caseResult.getDuration());
        assertEquals(message, caseResult.getMessage());
    }

    protected void checkCase(PersistentTestCaseResult caseResult, String name, PersistentTestCaseResult.Status status, String message)
    {
        checkCase(caseResult, name, status, PersistentTestResult.UNKNOWN_DURATION, message);
    }

    protected void checkSuite(PersistentTestSuiteResult suite, String name, int total, int failures, int errors)
    {
        assertEquals(name, suite.getName());
        assertEquals(total, suite.getTotal());
        assertEquals(failures, suite.getFailures());
        assertEquals(errors, suite.getErrors());
    }

    protected void checkPassCase(PersistentTestSuiteResult suite, String name)
    {
        PersistentTestCaseResult caseResult = suite.getCase(name);
        assertNotNull(caseResult);
        checkPassCase(caseResult, name);
    }

    protected void checkPassCase(PersistentTestCaseResult caseResult, String name)
    {
        checkCase(caseResult, name, PersistentTestCaseResult.Status.PASS, null);
    }

    protected void checkFailureCase(PersistentTestSuiteResult suite, String name, String message)
    {
        PersistentTestCaseResult caseResult = suite.getCase(name);
        assertNotNull(caseResult);
        checkFailureCase(caseResult, name, message);
    }

    protected void checkFailureCase(PersistentTestCaseResult caseResult, String name, String message)
    {
        checkCase(caseResult, name, PersistentTestCaseResult.Status.FAILURE, message);
    }

    protected void checkErrorCase(PersistentTestSuiteResult suite, String name, String message)
    {
        PersistentTestCaseResult caseResult = suite.getCase(name);
        assertNotNull(caseResult);
        checkErrorCase(caseResult, name, message);
    }

    protected void checkErrorCase(PersistentTestCaseResult caseResult, String name, String message)
    {
        checkCase(caseResult, name, PersistentTestCaseResult.Status.ERROR, message);
    }
}

