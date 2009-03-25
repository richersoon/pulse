package com.zutubi.pulse.acceptance.pages.admin;

import com.thoughtworks.selenium.Selenium;
import com.zutubi.pulse.acceptance.IDs;
import com.zutubi.pulse.acceptance.SeleniumUtils;
import com.zutubi.pulse.master.webwork.Urls;
import com.zutubi.tove.type.record.PathUtils;

/**
 * Base for config pages that appear when the "configuration" tab is selected
 * in the tree pane.
 */
public abstract class ConfigPage extends ConfigurationPanePage
{
    public ConfigPage(Selenium selenium, Urls urls, String id)
    {
        super(selenium, urls, id);
    }

    public ConfigPage(Selenium selenium, Urls urls, String id, String title)
    {
        super(selenium, urls, id, title);
    }

    public abstract String getPath();

    public CompositePage clickComposite(String relativePath, String displayName)
    {
        selenium.click(getTreeLinkLocator(displayName));
        return new CompositePage(selenium, urls, PathUtils.getPath(getPath(), relativePath));
    }

    public ListPage clickCollection(String baseName, String displayName)
    {
        selenium.click(getTreeLinkLocator(displayName));
        return new ListPage(selenium, urls, PathUtils.getPath(getId(), baseName));
    }

    public void expandTreeNode(String path)
    {
        String nodeExpression = "selenium.browserbot.getCurrentWindow().configTree.getNodeByConfigPath('" + path + "')";
        selenium.getEval(nodeExpression + ".expand(false, false);");
        SeleniumUtils.waitForCondition(selenium, nodeExpression + ".isExpanded();", SeleniumUtils.DEFAULT_TIMEOUT);
    }

    public String getTreeLinkLocator(String displayName)
    {
        return "//div[@id='config-tree']//a[span='" + displayName + "']";
    }

    public boolean isTreeLinkPresent(String displayName)
    {
        return selenium.isElementPresent(getTreeLinkLocator(displayName));
    }

    protected String getHierarchyLocator()
    {
        return "//span[text()='hierarchy']";
    }

    public boolean isCollapsedCollectionPresent()
    {
        return selenium.isElementPresent(IDs.COLLECTION_TABLE);
    }

    public boolean isLinksBoxPresent()
    {
        return selenium.isElementPresent(IDs.LINKS_BOX);
    }
}
