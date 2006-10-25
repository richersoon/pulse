package com.zutubi.pulse.acceptance;

import com.zutubi.pulse.acceptance.forms.CreateUserForm;
import com.zutubi.pulse.acceptance.forms.LicenseEditForm;
import com.zutubi.pulse.acceptance.forms.LoginForm;
import com.zutubi.pulse.license.License;
import com.zutubi.pulse.license.LicenseType;
import com.zutubi.pulse.test.LicenseHelper;
import junit.framework.Test;
import junit.framework.TestSuite;
import net.sourceforge.jwebunit.WebTester;

/**
 * <class-comment/>
 */
public class LicenseAuthorisationAcceptanceTest extends BaseAcceptanceTestCase
{
    public static Test suite()
    {
        TestSuite testSuite = new TestSuite(LicenseAuthorisationAcceptanceTest.class);
        return new LicenseAuthorisationSetup(testSuite);
    }

    protected void setUp() throws Exception
    {
        super.setUp();

        beginAt("/");
        loginAsAdmin();
    }

    public void testAddProjectLinkOnlyAvailableWhenLicensed() throws Exception
    {
        // verify that the link is initially available.
        clickLink(Navigation.TAB_PROJECTS);
        assertAndClick(Navigation.Projects.LINK_ADD_PROJECT);

        // configure a license that supports 0 projects.
        installLicense(tester, new License(LicenseType.CUSTOM, "holder").setSupportedProjects(0));

        // verify that add project wizard link is not available.
        clickLink(Navigation.TAB_PROJECTS);
        assertLinkNotPresent(Navigation.Projects.LINK_ADD_PROJECT);

        // verify that add project wizard action can not be triggered directly.
        goTo("/addProject!input.action");
        assertTextPresent("not licensed to execute the requested action");
    }

    public void testCreateUserFormOnlyAvailableWhenLicensed() throws Exception
    {
        // verify that the form is available.
        CreateUserForm form = new CreateUserForm(tester);

        navigateToUserAdministration();
        form.assertFormPresent();

        // configure a license that supports 0 users.
        installLicense(tester, new License(LicenseType.CUSTOM, "holder").setSupportedUsers(0));

        // verify create user form is not available.
        navigateToUserAdministration();
        form.assertFormNotPresent();

        // verify that we can not post directly to the create user action.
        goTo("/admin/createUser.action");
        assertTextPresent("not licensed to execute the requested action");
    }

    public void testAddAgentLinkOnlyAvailableWhenLicensed() throws Exception
    {
        // firstly, verify that the add agent link exists.
        clickLink(Navigation.TAB_AGENTS);
        assertLinkPresent(Navigation.Agents.LINK_ADD_AGENTS);

        // configure a license that supports 0 users.
        installLicense(tester, new License(LicenseType.CUSTOM, "holder").setSupportedAgents(0));

        // verify that the link is no longer available.
        clickLink(Navigation.TAB_AGENTS);
        assertLinkNotPresent(Navigation.Agents.LINK_ADD_AGENTS);

        // verify that we can not post directly to the add agent action.
        goTo("/admin/addAgent.action");
        assertTextPresent("not licensed to execute the requested action");
    }
/*
    public void testAddAgentLimitedByLicense() throws Exception
    {
        // install a license that supports 4 agents.
        installLicense(tester, new License(LicenseType.CUSTOM, "holder").setSupportedAgents(4));

        // create and ensure that 4 and only 4 agents can be created.
        for (int i = 0; i < 10; i++)
        {
            String agentName = "agent-" + RandomUtils.randomString(5);
            addAgent(agentName);
        }
    }

    public void testAddUserLimitedByLicense() throws Exception
    {
        // install a license that supports a limited number of users.
        installLicense(tester, new License(LicenseType.CUSTOM, "holder").setSupportedUsers(17));

        // create and ensure that 5 and only 5 users can be created.
        for (int i = 0; i < 10; i++)
        {
            String userName = "user-" + RandomUtils.randomString(5);
            addUser(userName);
        }
    }

    private void addUser(String login)
    {
        clickLink(Navigation.Tabs.TAB_ADMINISTRATION);
        clickLinkWithText("users");
        CreateUserForm form = new CreateUserForm(tester);
        form.assertFormPresent();
        form.saveFormElements(login, login, Boolean.toString(false), login, login);
    }

    // reusable item of work.
    private void addAgent(String name)
    {
        clickLink(Navigation.Tabs.TAB_AGENTS);
        assertAndClick(Navigation.Agents.LINK_ADD_AGENTS);
        SlaveForm form = new SlaveForm(tester, true);
        form.assertFormPresent();
        form.saveFormElements(name, "host", "80");
    }
*/

    private static void installLicense(WebTester tester, License l) throws Exception
    {
        String licenseKey = LicenseHelper.newLicenseKey(l);

        // navigate to admin license update.
        tester.beginAt("/");
        tester.clickLink(Navigation.TAB_ADMINISTRATION);
        tester.clickLink("license.edit");

        LicenseEditForm form = new LicenseEditForm(tester);
        form.assertFormPresent();
        form.saveFormElements(licenseKey);
        form.assertFormNotPresent();
    }

    /**
     *
     *
     */
    public static class LicenseAuthorisationSetup extends WebTestSetup
    {
        public LicenseAuthorisationSetup(Test test)
        {
            super(test);
        }

        protected void setUp() throws Exception
        {
            License l = new License(LicenseType.CUSTOM, "tester");
            loginAsAdmin();
            installLicense(tester, l);
            logout();
        }

        protected void tearDown() throws Exception
        {
            License l = new License(LicenseType.CUSTOM, "tester");
            loginAsAdmin();
            installLicense(tester, l);
            logout();
        }

        protected void loginAsAdmin()
        {
            beginAt("/login.action");
            LoginForm loginForm = new LoginForm(tester);
            loginForm.loginFormElements("admin", "admin", "false");
        }

        protected void logout()
        {
            clickLink(Navigation.LINK_LOGOUT);
        }
    }
}
