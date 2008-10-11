package com.zutubi.pulse.core.scm.svn.config;

import com.zutubi.config.annotations.SymbolicName;
import com.zutubi.config.annotations.Wire;
import com.zutubi.pulse.core.config.ConfigurationCheckHandlerSupport;
import com.zutubi.pulse.core.scm.api.ScmClientFactory;
import com.zutubi.pulse.core.scm.api.ScmException;
import com.zutubi.pulse.core.scm.svn.SubversionClient;

/**
 *
 *
 */
@SymbolicName("zutubi.subversionConfigurationCheckHandler")
@Wire
public class SubversionConfigurationCheckHandler extends ConfigurationCheckHandlerSupport<SubversionConfiguration>
{
    private ScmClientFactory scmClientFactory;

    public void test(SubversionConfiguration configuration) throws ScmException
    {
        SubversionClient client = (SubversionClient) scmClientFactory.createClient(configuration);
        try
        {
            client.testConnection();
        }
        finally
        {
            client.close();
        }
    }

    public void setScmClientFactory(ScmClientFactory scmClientFactory)
    {
        this.scmClientFactory = scmClientFactory;
    }
}
