package com.zutubi.pulse.acceptance.pages.browse;

import com.zutubi.pulse.acceptance.AcceptanceTestUtils;
import com.zutubi.pulse.acceptance.SeleniumBrowser;
import com.zutubi.pulse.acceptance.pages.SeleniumPage;
import com.zutubi.pulse.master.vfs.provider.pulse.ArtifactFileObject;
import com.zutubi.pulse.master.webwork.Urls;
import com.zutubi.util.StringUtils;

/**
 * The artifacts tab for a build result.
 */
public class BuildArtifactsPage extends SeleniumPage
{
    private static final String EXPRESSION_COMBO = "var combo = selenium.browserbot.getCurrentWindow().Ext.getCmp('filter-combo');";

    private String projectName;
    private long buildId;

    public BuildArtifactsPage(SeleniumBrowser browser, Urls urls, String projectName, long buildId)
    {
        super(browser, urls, "artifacts-tree", "build " + buildId);
        this.projectName = projectName;
        this.buildId = buildId;
    }

    public String getUrl()
    {
        return urls.buildArtifacts(projectName, Long.toString(buildId));
    }

    @Override
    public void waitFor()
    {
        super.waitFor();
        waitForLoad();
    }

    private void waitForLoad()
    {
        browser.waitForCondition("selenium.browserbot.getCurrentWindow().Ext.getCmp('artifacts-tree').loading === false");
    }

    /**
     * Returns the current value of the filter combo.
     *
     * @return the current filter
     */
    public String getCurrentFilter()
    {
        return browser.evalExpression(EXPRESSION_COMBO + "combo.getValue();");
    }

    /**
     * Sets the current filter to the given value, then waits for the tree to
     * reload.
     *
     * @param filter new filter value
     */
    public void setFilterAndWait(String filter)
    {
        String indexExpression;
        // Annoyingly ext stores can't find the empty strnig value...
        if (StringUtils.stringSet(filter))
        {
            indexExpression = "store.find('filter', '" + filter + "')";
        }
        else
        {
            indexExpression ="0";
        }

        browser.evalExpression(EXPRESSION_COMBO +
                "combo.setValue('" + filter + "');" +
                "var store = combo.getStore();" +
                "combo.fireEvent('select', combo, store.getAt(" + indexExpression + "));"
        );

        waitForLoad();
    }

    /**
     * Clicks the save filter link, and waits for it to be saved.
     */
    public void clickSaveFilterAndWait()
    {
        browser.click("save-filter-link");
        AcceptanceTestUtils.waitForStatus(browser, "Filter saved.");
    }

    /**
     * Generate the selenium locator for the named command node on the build artifacts page.
     *
     * @param command   name of the command in question.
     * @return the selenium locator for the command node
     */
    public String getCommandLocator(String command)
    {
        return "link=*command*::*" + command + "*";
    }

    /**
     * Generate the selenium locator for a named artifact node on the build artifacts page.
     *
     * @param artifact  name of the artifact in question
     *
     * @return  the selenium locator for the artifact node.
     */
    public String getArtifactLocator(String artifact)
    {
        return "//a[contains(@class, 'x-tree-node-anchor')]/span[text()='" + artifact + "']";
    }

    /**
     * Indicates whether or not an artifact of the given name exists in this
     * build.  Note, this does not imply that the artifact is available for
     * download.
     *
     * @param artifactName  name of the artifact being tested.
     * @return  true if the artifact exists, false otherwise.
     *
     * @see #isArtifactAvailable(String)
     */
    public boolean isArtifactListed(String artifactName)
    {
        return browser.isElementPresent(getArtifactLocator(artifactName));
    }

    /**
     * Indicates whether or not an artifact of the given name exists and was captured
     * / is available for download.
     *
     * @param artifactName  name of the artifact being tested.
     * @return  true if the artifact is available, false otherwise.
     */
    public boolean isArtifactAvailable(String artifactName)
    {
        String iconCls = browser.getAttribute(getArtifactLocator(artifactName) + "/preceding::img[position()=1]/@class");
        return !iconCls.contains(ArtifactFileObject.CLASS_PREFIX + ArtifactFileObject.CLASS_SUFFIX_BROKEN);
    }

    /**
     * Indicates if a file with the given name is listed under the artifact
     * with the given name.
     * 
     * @param artifactName name of the artifact to look under
     * @param fileName     name of the file to look for
     * @return true if a file of the given name is listed under the given
     *         artifact
     */
    public boolean isArtifactFileListed(String artifactName, String fileName)
    {
        String locator = getArtifactLocator(artifactName) + "/ancestor::tbody[position()=1]" + getArtifactLocator(fileName);
        return browser.isElementPresent(locator);
    }
}
