package com.zutubi.pulse.master.security;

import com.zutubi.pulse.core.test.api.PulseTestCase;
import com.zutubi.pulse.master.model.UserManager;
import com.zutubi.pulse.master.security.ldap.LdapManager;
import com.zutubi.pulse.master.tove.config.user.UserConfiguration;
import static org.mockito.Mockito.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetails;

import javax.servlet.http.HttpServletRequest;

public class CustomAuthenticationProviderTest extends PulseTestCase
{
    private LdapManager ldapManager;
    private UserManager userManager;
    private CustomAuthenticationProvider authenticationProvider;

    protected void setUp() throws Exception
    {
        super.setUp();

        ldapManager = mock(LdapManager.class);
        userManager = mock(UserManager.class);

        authenticationProvider = new CustomAuthenticationProvider();
        authenticationProvider.setLdapManager(ldapManager);
        authenticationProvider.setUserManager(userManager);
    }

    public void testUnknownUser()
    {
        String username = "unknown-user";
        stub(ldapManager.canAutoAdd()).toReturn(true);
        stub(ldapManager.authenticate(username, "pass", true)).toReturn(null);
        stub(userManager.loadUserByUsername(username)).toThrow(new UsernameNotFoundException(username));

        try
        {
            UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken(username, "pass");
            authenticationProvider.authenticate(token);
            fail();
        }
        catch (AuthenticationException e)
        {
            assertEquals("Bad credentials", e.getMessage());
        }
    }

    public void testNewKnownUser()
    {
        UserConfiguration user = new UserConfiguration();
        user.setLogin("user");

        stub(ldapManager.canAutoAdd()).toReturn(true);
        stub(ldapManager.authenticate(same(user.getLogin()), same("pass"), anyBoolean())).toReturn(user);
        stub(userManager.getUserConfig(user.getLogin())).toReturn(null);
        stub(userManager.insert(same(user))).toReturn(user);
        stub(userManager.loadUserByUsername(user.getLogin())).toReturn(new Principle(1, "user", "pass"));

        UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken("user", "pass");
        authenticationProvider.authenticate(token);

        verify(userManager, times(1)).insert(same(user));
        verify(userManager, times(1)).setPassword(same(user), anyString());
    }

    public void testResetPasswordOnKnownUserWebLogin()
    {
        UserConfiguration user = new UserConfiguration();
        user.setLogin("user");
        user.setAuthenticatedViaLdap(true);

        stub(ldapManager.canAutoAdd()).toReturn(true);
        stub(ldapManager.authenticate(same(user.getLogin()), same("pass"), anyBoolean())).toReturn(user);
        stub(userManager.getUserConfig(user.getLogin())).toReturn(user);
        stub(userManager.loadUserByUsername(user.getLogin())).toReturn(new Principle(1, user.getLogin(), "pass"));

        UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken("user", "pass");
        token.setDetails(new WebAuthenticationDetails(mock(HttpServletRequest.class)));
        authenticationProvider.authenticate(token);

        verify(userManager, never()).insert((UserConfiguration)anyObject());
        verify(userManager, times(1)).setPassword(same(user), anyString());
    }

    public void testNoResetPasswordOnKnownUserNonWebLogin()
    {
        UserConfiguration user = new UserConfiguration();
        user.setLogin("user");
        user.setAuthenticatedViaLdap(true);

        stub(ldapManager.canAutoAdd()).toReturn(true);
        stub(ldapManager.authenticate(same(user.getLogin()), same("pass"), anyBoolean())).toReturn(user);
        stub(userManager.getUserConfig(user.getLogin())).toReturn(user);
        stub(userManager.loadUserByUsername(user.getLogin())).toReturn(new Principle(1, user.getLogin(), "pass"));

        UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken("user", "pass");
        authenticationProvider.authenticate(token);

        verify(userManager, never()).insert((UserConfiguration)anyObject());
        verify(userManager, never()).setPassword(same(user), anyString());
    }
}
