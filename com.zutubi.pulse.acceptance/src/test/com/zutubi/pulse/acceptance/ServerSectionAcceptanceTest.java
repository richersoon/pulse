package com.zutubi.pulse.acceptance;

import com.zutubi.pulse.acceptance.pages.server.ServerMessagesPage;

/**
 * Acceptance tests for the server section of the reporting UI.
 */
public class ServerSectionAcceptanceTest extends AcceptanceTestBase
{
    public void testServerMessages() throws Exception
    {
        getBrowser().loginAsAdmin();
        getBrowser().openAndWaitFor(ServerMessagesPage.class);
        assertTrue(getBrowser().isTextPresent("messages found"));
    }

    public void testServerMessagesPaging() throws Exception
    {
        rpcClient.loginAsAdmin();
        try
        {
            for (int i = 0; i < 100; i++)
            {
                rpcClient.TestApi.logError("Test error message " + i);
            }
        }
        finally
        {
            rpcClient.logout();
        }

        getBrowser().loginAsAdmin();
        ServerMessagesPage page = getBrowser().openAndWaitFor(ServerMessagesPage.class);

        assertEquals("100 messages found", page.getMessagesCountText());
        assertPagingLinks(page, 10);
        page = page.clickPage(5);
        page.waitFor();
        assertPagingLinks(page, 10);
        assertTrue(getBrowser().isTextPresent("Test error message 59"));
    }

    private void assertPagingLinks(ServerMessagesPage page, int pageCount)
    {
        for(int i = 1; i <= pageCount; i++)
        {
            String pageId = page.getPageId(i);
            if(i == page.getPageNumber())
            {
                assertFalse(getBrowser().isElementIdPresent(pageId));
            }
            else
            {
                assertTrue(getBrowser().isElementIdPresent(pageId));
            }
        }

        if(page.getPageNumber() == 1)
        {
            assertFalse(getBrowser().isElementIdPresent("page.latest"));
            assertFalse(getBrowser().isElementIdPresent("page.previous"));
        }
        else
        {
            assertTrue(getBrowser().isElementIdPresent("page.latest"));
            assertTrue(getBrowser().isElementIdPresent("page.previous"));
        }

        if(page.getPageNumber() == pageCount)
        {
            assertFalse(getBrowser().isElementIdPresent("page.oldest"));
            assertFalse(getBrowser().isElementIdPresent("page.next"));
        }
        else
        {
            assertTrue(getBrowser().isElementIdPresent("page.oldest"));
            assertTrue(getBrowser().isElementIdPresent("page.next"));
        }
    }
}
