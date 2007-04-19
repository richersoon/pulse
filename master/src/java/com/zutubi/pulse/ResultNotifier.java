package com.zutubi.pulse;

import com.zutubi.pulse.bootstrap.ComponentContext;
import com.zutubi.pulse.bootstrap.MasterConfigurationManager;
import com.zutubi.pulse.condition.UnsuccessfulCountBuildsValue;
import com.zutubi.pulse.condition.UnsuccessfulCountDaysValue;
import com.zutubi.pulse.core.model.Feature;
import com.zutubi.pulse.events.Event;
import com.zutubi.pulse.events.EventListener;
import com.zutubi.pulse.events.EventManager;
import com.zutubi.pulse.events.build.BuildCompletedEvent;
import com.zutubi.pulse.model.*;
import com.zutubi.pulse.renderer.BuildResultRenderer;
import com.zutubi.util.logging.Logger;
import com.zutubi.pulse.prototype.config.admin.GeneralAdminConfiguration;
import com.zutubi.prototype.config.ConfigurationProvider;

import java.io.StringWriter;
import java.util.*;

/**
 *
 */
public class ResultNotifier implements EventListener
{
    public static final String FAILURE_LIMIT_PROPERTY = "pulse.notification.test.failure.limit";
    public static final int DEFAULT_FAILURE_LIMIT = 20;

    private static final Logger LOG = Logger.getLogger(ResultNotifier.class);

    private SubscriptionManager subscriptionManager;
    private UserManager userManager;
    private MasterConfigurationManager configurationManager;
    private ConfigurationProvider configurationProvider;
    private BuildResultRenderer buildResultRenderer;
    private BuildManager buildManager;

    public static int getFailureLimit()
    {
        int limit = DEFAULT_FAILURE_LIMIT;
        String property = System.getProperty(FAILURE_LIMIT_PROPERTY);
        if(property != null)
        {
            try
            {
                limit = Integer.parseInt(property);
            }
            catch(NumberFormatException e)
            {
                LOG.warning(e);
            }
        }

        return limit;
    }

    public void handleEvent(Event evt)
    {
        BuildCompletedEvent event = (BuildCompletedEvent) evt;
        BuildResult buildResult = event.getResult();

        buildResult.loadFailedTestResults(configurationManager.getDataDirectory(), getFailureLimit());

        Set<Long> notifiedContactPoints = new HashSet<Long>();
        Map<String, RenderedResult> renderCache = new HashMap<String, RenderedResult>();
        Map<String, Object> dataMap = getDataMap(buildResult, configurationProvider.get(GeneralAdminConfiguration.class).getBaseUrl(), buildManager, buildResultRenderer);

        // Retrieve all of the subscriptions indicating an interest in the project
        // associated with the build result.
        List<Subscription> subscriptions = subscriptionManager.getSubscriptions(buildResult.getProject());
        for (Subscription subscription : subscriptions)
        {
            // filter out contact points that we have already notified.
            ContactPoint contactPoint = subscription.getContactPoint();
            if (notifiedContactPoints.contains(contactPoint.getId()))
            {
                continue;
            }

            // subscriptions are generated by hibernate, so we will need to
            // manually wire them to ensure they have access to the necessary resources.
            ComponentContext.autowire(subscription);

            // determine which of these subscriptions should be notified.
            if (subscription.conditionSatisfied(buildResult))
            {
                String templateName = subscription.getTemplate();
                RenderedResult rendered = renderResult(buildResult, dataMap, templateName, renderCache);
                notifiedContactPoints.add(contactPoint.getId());
                contactPoint.notify(buildResult, rendered.subject, rendered.content, buildResultRenderer.getTemplateInfo(templateName, buildResult.isPersonal()).getMimeType());
                
                // Contact point may be modified: e.g. error may be set.
                userManager.save(contactPoint);
            }
        }
    }

    public static Map<String, Object> getDataMap(BuildResult result, String baseUrl, BuildManager buildManager, BuildResultRenderer renderer)
    {
        Project project = result.getProject();

        Map<String, Object> dataMap = new HashMap<String, Object>();
        dataMap.put("renderer", renderer);
        dataMap.put("baseUrl", baseUrl);
        dataMap.put("project", project);
        dataMap.put("status", result.succeeded() ? "healthy" : "broken");
        dataMap.put("result", result);
        dataMap.put("model", result);
        dataMap.put("changelists", buildManager.getChangesForBuild(result));
        dataMap.put("errorLevel", Feature.Level.ERROR);
        dataMap.put("warningLevel", Feature.Level.WARNING);

        if(!result.succeeded())
        {
            BuildResult lastSuccess = buildManager.getLatestSuccessfulBuildResult();
            if (lastSuccess != null)
            {
                dataMap.put("lastSuccess", lastSuccess);
            }
            
            dataMap.put("unsuccessfulBuilds", UnsuccessfulCountBuildsValue.getValueForBuild(result, buildManager));
            dataMap.put("unsuccessfulDays", UnsuccessfulCountDaysValue.getValueForBuild(result, buildManager));
        }

        return dataMap;
    }

    private RenderedResult renderResult(BuildResult result, Map<String, Object> dataMap, String template, Map<String, RenderedResult> cache)
    {
        RenderedResult rendered = cache.get(template);
        if(rendered == null)
        {
            StringWriter w = new StringWriter();
            buildResultRenderer.render(result, dataMap, template, w);
            String content = w.toString();

            String subject;
            String subjectTemplate = template + "-subject";
            if(buildResultRenderer.hasTemplate(subjectTemplate, result.isPersonal()))
            {
                w = new StringWriter();
                buildResultRenderer.render(result, dataMap, subjectTemplate, w);
                subject = w.toString().trim();
            }
            else
            {
                subject = getDefaultSubject(result);
            }

            rendered = new RenderedResult(subject, content);
            cache.put(template, rendered);
        }

        return rendered;
    }

    private String getDefaultSubject(BuildResult result)
    {
        String prelude = result.isPersonal() ? "personal build " : (result.getProject().getName() + ": build ");
        return prelude + Long.toString(result.getNumber()) + ": " + result.getState().getPrettyString();
    }

    public Class[] getHandledEvents()
    {
        return new Class[]{BuildCompletedEvent.class};
    }

    public void setEventManager(EventManager eventManager)
    {
        eventManager.register(this);
    }

    public void setSubscriptionManager(SubscriptionManager subscriptionManager)
    {
        this.subscriptionManager = subscriptionManager;
    }

    public void setUserManager(UserManager userManager)
    {
        this.userManager = userManager;
    }

    public void setConfigurationManager(MasterConfigurationManager configurationManager)
    {
        this.configurationManager = configurationManager;
    }

    public void setBuildResultRenderer(BuildResultRenderer buildResultRenderer)
    {
        this.buildResultRenderer = buildResultRenderer;
    }

    public void setBuildManager(BuildManager buildManager)
    {
        this.buildManager = buildManager;
    }

    public void setConfigurationProvider(ConfigurationProvider configurationProvider)
    {
        this.configurationProvider = configurationProvider;
    }

    private class RenderedResult
    {
        String subject;
        String content;

        public RenderedResult(String subject, String content)
        {
            this.subject = subject;
            this.content = content;
        }
    }
}
