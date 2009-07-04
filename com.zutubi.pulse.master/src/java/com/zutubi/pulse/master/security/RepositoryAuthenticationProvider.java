package com.zutubi.pulse.master.security;

import org.acegisecurity.Authentication;
import org.acegisecurity.AuthenticationException;
import org.acegisecurity.providers.AuthenticationProvider;
import org.acegisecurity.providers.UsernamePasswordAuthenticationToken;

import java.util.HashSet;
import java.util.Set;

/**
 * An authentication provider implementation that uses a token (a shared token
 * between the pulse master and agents) to provide an authentication.
 */
public class RepositoryAuthenticationProvider implements AuthenticationProvider
{
    private static final String USERNAME = "pulse";

    private Set<Object> activeTokens = new HashSet<Object>();

    public Authentication authenticate(Authentication authentication) throws AuthenticationException
    {
        UsernamePasswordAuthenticationToken auth = (UsernamePasswordAuthenticationToken) authentication;
        String username = (authentication.getPrincipal() == null) ? "NONE_PROVIDED" : authentication.getName();
        if (accept(username))
        {
            return validateToken(auth);
        }
        // pass authentication on to another provider
        return null;
    }

    private Authentication validateToken(Authentication auth)
    {
        if (activeTokens.contains(auth.getCredentials()))
        {
            return getRepositoryAuthentication();
        }
        return null;
    }

    private Authentication getRepositoryAuthentication()
    {
        AcegiUser repositoryUser = AcegiUtils.getRepositoryUser();
        return new UsernamePasswordAuthenticationToken(repositoryUser, null, repositoryUser.getAuthorities());
    }

    public void activate(String token)
    {
        activeTokens.add(token);
    }

    public void deactivate(String token)
    {
        activeTokens.remove(token);
    }

    private boolean accept(String name)
    {
        return name != null && name.compareTo(USERNAME) == 0;
    }

    public boolean supports(Class authentication)
    {
        return (UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication));
    }
}