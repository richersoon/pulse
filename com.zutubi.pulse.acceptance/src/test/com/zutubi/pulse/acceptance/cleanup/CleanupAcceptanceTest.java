package com.zutubi.pulse.acceptance.cleanup;

import com.zutubi.pulse.acceptance.AcceptanceTestUtils;
import com.zutubi.pulse.acceptance.SeleniumTestBase;
import com.zutubi.pulse.acceptance.pages.SeleniumPage;
import com.zutubi.pulse.acceptance.pages.browse.*;
import com.zutubi.pulse.master.cleanup.config.CleanupWhat;
import com.zutubi.pulse.master.model.ProjectManager;
import com.zutubi.util.Condition;
import com.zutubi.util.RandomUtils;
import static com.zutubi.util.Constants.SECOND;

import java.util.Vector;
import java.util.Hashtable;

/**
 * The set of acceptance tests for the projects cleanup configuration.
 */
public class CleanupAcceptanceTest extends SeleniumTestBase
{
    private static final long CLEANUP_TIMEOUT = SECOND * 10;

    private CleanupTestUtils utils;

    @Override
    protected void setUp() throws Exception
    {
        super.setUp();

        loginAsAdmin();

        xmlRpcHelper.loginAsAdmin();

        utils = new CleanupTestUtils(xmlRpcHelper);
    }

    @Override
    protected void tearDown() throws Exception
    {
        xmlRpcHelper.logout();

        logout();

        super.tearDown();
    }

    public void testCleanupWorkingDirectories() throws Exception
    {
        final String projectName = random;
        xmlRpcHelper.insertSimpleProject(projectName, ProjectManager.GLOBAL_PROJECT_NAME, false);

        utils.setRetainWorkingCopy(projectName, true);
        utils.addCleanupRule(projectName, "working_directory", CleanupWhat.WORKING_COPY_SNAPSHOT);
        utils.deleteCleanupRule(projectName, "default");

        xmlRpcHelper.runBuild(projectName);
        waitForCleanupToRunAsynchronously();

        assertTrue(utils.hasBuildWorkingCopy(projectName, 1));
        assertTrue(isBuildPresentViaUI(projectName, 1));

        xmlRpcHelper.runBuild(projectName);
        waitForCleanupToRunAsynchronously(new InvertedCondition()
        {
            public boolean notSatisfied() throws Exception
            {
                return utils.hasBuildWorkingCopy(projectName, 1);
            }
        });

        assertTrue(isBuildPresentViaUI(projectName, 2));
        assertTrue(utils.hasBuildWorkingCopy(projectName, 2));

        assertFalse(utils.hasBuildWorkingCopy(projectName, 1));

        // verify that the UI is as expected - the working copy tab exists and displays the
        // appropriate messages.

        assertFalse(isWorkingCopyPresentViaUI(projectName, 1));
        assertTrue(isWorkingCopyPresentViaUI(projectName, 2));
    }

    public void testCleanupBuildArtifacts() throws Exception
    {
        final String projectName = random;
        xmlRpcHelper.insertSimpleProject(projectName, ProjectManager.GLOBAL_PROJECT_NAME, false);

        utils.setRetainWorkingCopy(projectName, true);
        utils.addCleanupRule(projectName, "build_artifacts", CleanupWhat.BUILD_ARTIFACTS);
        utils.deleteCleanupRule(projectName, "default");

        xmlRpcHelper.runBuild(projectName);
        waitForCleanupToRunAsynchronously();

        assertTrue(utils.hasBuildDirectory(projectName, 1));
        assertTrue(utils.hasBuildWorkingCopy(projectName, 1));

        xmlRpcHelper.runBuild(projectName);
        waitForCleanupToRunAsynchronously(new InvertedCondition()
        {
            public boolean notSatisfied() throws Exception
            {
                return utils.hasBuildOutputDirectory(projectName, 1);
            }
        }, new InvertedCondition()
        {
            public boolean notSatisfied() throws Exception
            {
                return utils.hasBuildFeaturesDirectory(projectName, 1);
            }
        });

        assertTrue(utils.hasBuildDirectory(projectName, 2));

        assertTrue(utils.hasBuildDirectory(projectName, 1));
        assertTrue(utils.hasBuildWorkingCopy(projectName, 1));
        assertFalse(utils.hasBuildOutputDirectory(projectName, 1));
        assertFalse(utils.hasBuildFeaturesDirectory(projectName, 1));

        assertTrue(isBuildPulseFilePresentViaUI(projectName, 1));
        assertTrue(isBuildLogsPresentViaUI(projectName, 1));
        assertFalse(isBuildArtifactsPresentViaUI(projectName, 1));

        // the remote api returns artifacts based on what is in the database.
        Vector artifactsInBuild = xmlRpcHelper.getArtifactsInBuild(projectName, 1);
        assertEquals(3, artifactsInBuild.size());
    }

    public void testCleanupAll() throws Exception
    {
        final String projectName = random;
        xmlRpcHelper.insertSimpleProject(projectName, ProjectManager.GLOBAL_PROJECT_NAME, false);

        utils.setRetainWorkingCopy(projectName, true);
        utils.addCleanupRule(projectName, "everything");
        utils.deleteCleanupRule(projectName, "default");

        xmlRpcHelper.runBuild(projectName);
        waitForCleanupToRunAsynchronously();

        assertTrue(utils.hasBuild(projectName, 1));
        assertTrue(isBuildPresentViaUI(projectName, 1));

        xmlRpcHelper.runBuild(projectName);
        waitForCleanupToRunAsynchronously(new InvertedCondition()
        {
            public boolean notSatisfied() throws Exception
            {
                return utils.hasBuild(projectName, 1);
            }
        });

        assertTrue(utils.hasBuild(projectName, 2));
        assertTrue(isBuildPresentViaUI(projectName, 2));

        assertFalse(utils.hasBuild(projectName, 1));
        assertFalse(isBuildPresentViaUI(projectName, 1));

        // Unknown build '1' for project 'testCleanupAll-8KHqy3jjGJ'
        try
        {
            xmlRpcHelper.getArtifactsInBuild(projectName, 1);
        }
        catch(Exception e)
        {
            assertTrue(e.getMessage().contains("Unknown build '1' for project '"+projectName+"'"));
        }
    }

    public void testCleanupRepositoryArtifacts() throws Exception
    {
        final String projectName = random;
        xmlRpcHelper.insertSimpleProject(projectName, ProjectManager.GLOBAL_PROJECT_NAME, false);

        utils.addCleanupRule(projectName, "repository_artifacts", CleanupWhat.REPOSITORY_ARTIFACTS);
        utils.deleteCleanupRule(projectName, "default");

        xmlRpcHelper.runBuild(projectName);
        waitForCleanupToRunAsynchronously();

        assertTrue(utils.hasIvyFile(projectName, 1));

        xmlRpcHelper.runBuild(projectName);
        waitForCleanupToRunAsynchronously(new InvertedCondition()
        {
            public boolean notSatisfied() throws Exception
            {
                return utils.hasIvyFile(projectName, 1);
            }
        });

        assertTrue(utils.hasIvyFile(projectName, 2));
        assertFalse(utils.hasIvyFile(projectName, 1));
    }

    public void testCleanupRepositoryArtifactsAfterProjectRename() throws Exception
    {
        final String projectName = random;
        xmlRpcHelper.insertSimpleProject(projectName, ProjectManager.GLOBAL_PROJECT_NAME, false);

        utils.addCleanupRule(projectName, "repository_artifacts", CleanupWhat.REPOSITORY_ARTIFACTS);
        utils.deleteCleanupRule(projectName, "default");

        xmlRpcHelper.runBuild(projectName);
        waitForCleanupToRunAsynchronously();

        assertTrue(utils.hasIvyFile(projectName, 1));

        // rename the project.
        String newProjectName = renameProject(projectName);

        xmlRpcHelper.runBuild(newProjectName);
        waitForCleanupToRunAsynchronously(new InvertedCondition()
        {
            public boolean notSatisfied() throws Exception
            {
                return utils.hasIvyFile(projectName, 1);
            }
        });

        assertTrue(utils.hasIvyFile(newProjectName, 2));
        assertFalse(utils.hasIvyFile(projectName, 1));
    }

    private String renameProject(String projectName) throws Exception
    {
        String newProjectName = getName() + "-" + RandomUtils.randomString(10);
        Hashtable<String, Object> projectConfig = xmlRpcHelper.getConfig("projects/" + projectName);
        projectConfig.put("name", newProjectName);
        xmlRpcHelper.saveConfig("projects/" + projectName, projectConfig, false);
        return newProjectName;
    }

    private void waitForCleanupToRunAsynchronously(Condition... conditions)
    {
        if (conditions.length > 0)
        {
            int i = 0;
            for (Condition c : conditions)
            {
                i++;
                AcceptanceTestUtils.waitForCondition(c, CLEANUP_TIMEOUT, "condition("+i+") to be satisfied.");
            }
        }
        else
        {
            try
            {
                Thread.sleep(CLEANUP_TIMEOUT);
            }
            catch (InterruptedException e)
            {
                // noop.
            }
        }
    }

    public boolean isWorkingCopyPresentViaUI(String projectName, long buildNumber)
    {
        BuildWorkingCopyPage page = browser.openAndWaitFor(BuildWorkingCopyPage.class, projectName, buildNumber);
        return page.isWorkingCopyPresent();
    }

    public boolean isBuildPresentViaUI(String projectName, long buildNumber)
    {
        BuildSummaryPage page = browser.createPage(BuildSummaryPage.class, projectName, buildNumber);
        return canOpenPage(page);
    }

    public boolean isBuildPulseFilePresentViaUI(String projectName, long buildNumber)
    {
        BuildFilePage page = browser.createPage(BuildFilePage.class, projectName, buildNumber);
        return canOpenPage(page);
    }

    public boolean isBuildLogsPresentViaUI(String projectName, long buildNumber)
    {
        BuildDetailedViewPage page = browser.createPage(BuildDetailedViewPage.class, projectName, buildNumber);
        if (!canOpenPage(page))
        {
            return false;
        }
        if (!page.isBuildLogLinkPresent())
        {
            return false;
        }
        page.clickBuildLogLink();
        return browser.isTextPresent("tail of build log");
    }

    public boolean isBuildArtifactsPresentViaUI(String projectName, long buildNumber)
    {
        BuildArtifactsPage page = browser.createPage(BuildArtifactsPage.class, projectName, buildNumber);
        if (!canOpenPage(page))
        {
            return false;
        }

        // if artifacts are available, we should have the build command open in the tree.
        browser.waitForLocator(page.getArtifactLocator("environment"));
        return page.isArtifactAvailable("environment");
    }

    private boolean canOpenPage(SeleniumPage page)
    {
        page.open();
        browser.waitForPageToLoad();
        return page.isPresent();
    }

    private abstract class InvertedCondition implements Condition
    {
        public boolean satisfied()
        {
            try
            {
                return !notSatisfied();
            }
            catch(Exception e)
            {
                throw new RuntimeException(e);
            }
        }

        public abstract boolean notSatisfied() throws Exception;
    }
}
