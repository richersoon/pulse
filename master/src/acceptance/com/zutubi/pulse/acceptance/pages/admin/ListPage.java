package com.zutubi.pulse.acceptance.pages.admin;

import com.thoughtworks.selenium.Selenium;
import com.zutubi.prototype.type.record.PathUtils;
import com.zutubi.pulse.acceptance.IDs;
import com.zutubi.pulse.acceptance.SeleniumUtils;
import com.zutubi.pulse.webwork.mapping.Urls;

/**
 * A page in the admin UI that displays a list of composites.  The list is
 * shown in a table with edit and delete links, and there are no child nodes.
 */
public class ListPage extends ConfigPage
{
    public static final String ANNOTATION_INHERITED  = "inherited";
    public static final String ANNOTATION_OVERRIDDEN = "overridden";
    public static final String ANNOTATION_HIDDEN     = "hidden";

    private static final String ADD_LINK = "map:add";
    private String path;

    public ListPage(Selenium selenium, Urls urls, String path)
    {
        super(selenium, urls, "map:path:" + path);
        this.path = path;
    }

    public String getPath()
    {
        return path;
    }

    public String getUrl()
    {
        return urls.admin() + path + "/";
    }

    public void assertItemPresent(String baseName, String annotation, String... actions)
    {
        SeleniumUtils.assertElementPresent(selenium, getItemId(baseName));

        if(annotation == null)
        {
            annotation = "noan";
        }
        SeleniumUtils.assertElementPresent(selenium, annotation + ":" + baseName);

        for(String action: actions)
        {
            SeleniumUtils.assertLinkPresent(selenium, getActionId(action, baseName));
        }
    }

    public void assertItemNotPresent(String baseName)
    {
        SeleniumUtils.assertElementNotPresent(selenium, getItemId(baseName));
    }

    public void assertCellContent(int itemIndex, int columnIndex, String text)
    {
        SeleniumUtils.assertCellContents(selenium, IDs.COLLECTION_TABLE, itemIndex + 2, columnIndex, text);
    }
    
    private String getItemId(String baseName)
    {
        return "item:" + baseName;
    }

    public void assertActionsNotPresent(String baseName, String... actions)
    {
        for(String action: actions)
        {
            SeleniumUtils.assertLinkNotPresent(selenium, getActionId(action, baseName));
        }
    }

    private String getActionId(String action, String baseName)
    {
        return action + ":" + baseName;
    }

    public void clickAdd()
    {
        selenium.click(ADD_LINK);
    }

    public void clickView(String baseName)
    {
        selenium.click(getActionId("view", baseName));
    }

    public DeleteConfirmPage clickDelete(String baseName)
    {
        String actionId = getActionId("delete", baseName);
        boolean isHide = "hide".equals(selenium.getText(actionId));
        selenium.click(actionId);
        return new DeleteConfirmPage(selenium, urls, PathUtils.getPath(path, baseName), isHide);
    }

    public void clickRestore(String baseName)
    {
        selenium.click(getActionId("restore", baseName));
        SeleniumUtils.waitForVariable(selenium, "actionInProgress", SeleniumUtils.DEFAULT_TIMEOUT, true);
    }
}
