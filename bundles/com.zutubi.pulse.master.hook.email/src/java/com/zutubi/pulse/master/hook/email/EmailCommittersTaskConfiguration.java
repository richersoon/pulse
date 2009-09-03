package com.zutubi.pulse.master.hook.email;

import com.zutubi.pulse.core.api.PulseException;
import com.zutubi.pulse.core.engine.api.ExecutionContext;
import com.zutubi.pulse.core.engine.api.ResultState;
import com.zutubi.pulse.core.model.PersistentChangelist;
import com.zutubi.pulse.core.scm.api.ScmCapability;
import com.zutubi.pulse.core.scm.api.ScmClient;
import com.zutubi.pulse.core.scm.api.ScmContext;
import com.zutubi.pulse.core.scm.api.ScmException;
import com.zutubi.pulse.core.scm.config.api.CommitterMappingConfiguration;
import com.zutubi.pulse.master.model.BuildManager;
import com.zutubi.pulse.master.model.BuildResult;
import com.zutubi.pulse.master.model.RecipeResultNode;
import com.zutubi.pulse.master.model.UserManager;
import com.zutubi.pulse.master.notifications.ResultNotifier;
import com.zutubi.pulse.master.notifications.renderer.BuildResultRenderer;
import com.zutubi.pulse.master.scm.ScmClientUtils;
import com.zutubi.pulse.master.scm.ScmManager;
import com.zutubi.pulse.master.tove.config.admin.EmailConfiguration;
import com.zutubi.pulse.master.tove.config.admin.GlobalConfiguration;
import com.zutubi.pulse.master.tove.config.project.ProjectConfiguration;
import com.zutubi.pulse.master.tove.config.project.hooks.BuildHookTaskConfiguration;
import com.zutubi.pulse.master.tove.config.project.hooks.CompatibleHooks;
import com.zutubi.pulse.master.tove.config.project.hooks.ManualBuildHookConfiguration;
import com.zutubi.pulse.master.tove.config.project.hooks.PostBuildHookConfiguration;
import com.zutubi.pulse.master.tove.config.user.contacts.EmailContactConfiguration;
import com.zutubi.tove.annotations.Form;
import com.zutubi.tove.annotations.Select;
import com.zutubi.tove.annotations.SymbolicName;
import com.zutubi.tove.annotations.Wire;
import com.zutubi.tove.config.ConfigurationProvider;
import com.zutubi.tove.config.api.AbstractConfiguration;
import com.zutubi.util.CollectionUtils;
import com.zutubi.util.Predicate;
import com.zutubi.util.StringUtils;
import com.zutubi.validation.annotations.Required;

import static java.util.Arrays.asList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * A build hook task that emails users that committed changes that affected
 * the build.
 */
@SymbolicName("zutubi.emailCommittersTaskConfig")
@Form(fieldOrder = {"emailDomain", "template", "sinceLastSuccess", "ignorePulseUsers", "useScmEmails"})
@CompatibleHooks({ManualBuildHookConfiguration.class, PostBuildHookConfiguration.class})
@Wire
public class EmailCommittersTaskConfiguration extends AbstractConfiguration implements BuildHookTaskConfiguration
{
    @Required
    private String emailDomain;
    @Select(optionProvider = "com.zutubi.pulse.master.tove.config.user.SubscriptionTemplateOptionProvider")
    private String template;
    private boolean sinceLastSuccess = false;
    private boolean ignorePulseUsers = false;
    private boolean useScmEmails = false;

    private BuildResultRenderer buildResultRenderer;
    private ConfigurationProvider configurationProvider;
    private BuildManager buildManager;
    private UserManager userManager;
    private ScmManager scmManager;

    public String getEmailDomain()
    {
        return emailDomain;
    }

    public void setEmailDomain(String emailDomain)
    {
        this.emailDomain = emailDomain;
    }

    public String getTemplate()
    {
        return template;
    }

    public void setTemplate(String template)
    {
        this.template = template;
    }

    public boolean isSinceLastSuccess()
    {
        return sinceLastSuccess;
    }

    public void setSinceLastSuccess(boolean sinceLastSuccess)
    {
        this.sinceLastSuccess = sinceLastSuccess;
    }

    public boolean isUseScmEmails()
    {
        return useScmEmails;
    }

    public void setUseScmEmails(boolean useScmEmails)
    {
        this.useScmEmails = useScmEmails;
    }

    public boolean isIgnorePulseUsers()
    {
        return ignorePulseUsers;
    }

    public void setIgnorePulseUsers(boolean ignorePulseUsers)
    {
        this.ignorePulseUsers = ignorePulseUsers;
    }

    public void execute(ExecutionContext context, BuildResult buildResult, RecipeResultNode resultNode) throws Exception
    {
        if (!buildResult.completed())
        {
            throw new PulseException("Email build hook task can only be applied post-build");
        }

        EmailConfiguration emailConfiguration = configurationProvider.get(EmailConfiguration.class);
        if (!StringUtils.stringSet(emailConfiguration.getHost()))
        {
            throw new PulseException("Cannot execute email build hook task as no SMTP host is configured.");
        }

        GlobalConfiguration globalConfiguration = configurationProvider.get(GlobalConfiguration.class);
        List<String> emails = getEmails(getBuilds(buildResult));
        if (emails.size() > 0)
        {
            ResultNotifier.RenderedResult rendered = ResultNotifier.renderResult(buildResult, globalConfiguration.getBaseUrl(), buildManager, buildResultRenderer, template);
            String mimeType = buildResultRenderer.getTemplateInfo(template, buildResult.isPersonal()).getMimeType();
            String subject = rendered.getSubject();

            EmailContactConfiguration.sendMail(emails, emailConfiguration, subject, mimeType, rendered.getContent());
        }
    }

    private List<BuildResult> getBuilds(BuildResult result)
    {
        if (sinceLastSuccess)
        {
            BuildResult previousSuccess = buildManager.getPreviousBuildResultWithRevision(result, new ResultState[]{ResultState.SUCCESS});
            long lowestNumber = previousSuccess == null ? 1 : previousSuccess.getNumber() + 1;
            List<BuildResult> builds = buildManager.queryBuilds(result.getProject(), ResultState.getCompletedStates(), lowestNumber, result.getNumber() - 1, 0, -1, false, false);
            builds.add(result);
            return builds;
        }
        else
        {
            return asList(result);
        }
    }

    private List<String> getEmails(List<BuildResult> builds)
    {
        List<String> emails = new LinkedList<String>();
        Set<String> seenLogins = new HashSet<String>();
        for (BuildResult build: builds)
        {
            for (PersistentChangelist change : buildManager.getChangesForBuild(build))
            {
                String scmLogin = change.getAuthor();
                // Only bother to map and add if we haven't already done so.
                if (seenLogins.add(scmLogin))
                {
                    if (StringUtils.stringSet(scmLogin) && (!ignorePulseUsers || userManager.getUser(scmLogin) == null))
                    {
                        emails.add(getEmail(scmLogin));
                    }
                }
            }
        }
        return emails;
    }

    private String getEmail(final String scmLogin)
    {
        ProjectConfiguration projectConfig = configurationProvider.getAncestorOfType(this, ProjectConfiguration.class);
        CommitterMappingConfiguration mapping = CollectionUtils.find(projectConfig.getScm().getCommitterMappings(), new Predicate<CommitterMappingConfiguration>()
        {
            public boolean satisfied(CommitterMappingConfiguration committerMappingConfiguration)
            {
                return committerMappingConfiguration.getScmLogin().equals(scmLogin);
            }
        });

        String email = null;
        if (mapping == null)
        {
            if (useScmEmails)
            {
                try
                {
                    email = ScmClientUtils.withScmClient(projectConfig, scmManager, new ScmClientUtils.ScmContextualAction<String>()
                    {
                        public String process(ScmClient client, ScmContext context) throws ScmException
                        {
                            if (client.getCapabilities(context).contains(ScmCapability.EMAIL))
                            {
                                return client.getEmailAddress(context, scmLogin);
                            }
                            else
                            {
                                return null;
                            }
                        }
                    });
                }
                catch (ScmException e)
                {
                    // Oh well, fall back to guess.
                }
            }

            if (email == null)
            {
                email = scmLogin;
            }
        }
        else
        {
            email = mapping.getEmail();
        }

        if (email.contains("@"))
        {
            return email;
        }
        else
        {
            return StringUtils.join("@", true, email, emailDomain);
        }
    }

    public void setBuildResultRenderer(BuildResultRenderer buildResultRenderer)
    {
        this.buildResultRenderer = buildResultRenderer;
    }

    public void setConfigurationProvider(ConfigurationProvider configurationProvider)
    {
        this.configurationProvider = configurationProvider;
    }

    public void setBuildManager(BuildManager buildManager)
    {
        this.buildManager = buildManager;
    }

    public void setUserManager(UserManager userManager)
    {
        this.userManager = userManager;
    }

    public void setScmManager(ScmManager scmManager)
    {
        this.scmManager = scmManager;
    }
}

