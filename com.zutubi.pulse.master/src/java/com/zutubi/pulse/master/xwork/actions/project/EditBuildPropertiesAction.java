package com.zutubi.pulse.master.xwork.actions.project;

import com.opensymphony.xwork.ActionContext;
import static com.zutubi.config.annotations.FieldParameter.ACTIONS;
import static com.zutubi.config.annotations.FieldParameter.SCRIPTS;
import com.zutubi.config.annotations.FieldType;
import com.zutubi.pulse.core.config.NamedConfigurationComparator;
import com.zutubi.pulse.core.config.ResourceProperty;
import com.zutubi.pulse.core.scm.api.ScmClientFactory;
import com.zutubi.pulse.core.scm.ScmClientUtils;
import com.zutubi.pulse.core.scm.api.*;
import com.zutubi.pulse.core.scm.config.ScmConfiguration;
import com.zutubi.pulse.master.model.ManualTriggerBuildReason;
import com.zutubi.pulse.master.model.Project;
import com.zutubi.pulse.master.model.ProjectManager;
import com.zutubi.pulse.master.tove.config.project.ProjectConfiguration;
import com.zutubi.pulse.master.tove.config.project.types.TypeConfiguration;
import com.zutubi.tove.config.ConfigurationProvider;
import com.zutubi.tove.config.ConfigurationRegistry;
import com.zutubi.tove.model.Field;
import com.zutubi.tove.model.Form;
import com.zutubi.tove.type.record.PathUtils;
import com.zutubi.tove.webwork.ConfigurationPanel;
import com.zutubi.tove.webwork.ConfigurationResponse;
import com.zutubi.tove.webwork.ToveUtils;
import com.zutubi.util.TextUtils;
import com.zutubi.util.logging.Logger;
import freemarker.template.Configuration;
import freemarker.template.TemplateException;

import java.io.IOException;
import java.io.StringWriter;
import java.util.*;

public class EditBuildPropertiesAction extends ProjectActionBase
{
    private static final Logger LOG = Logger.getLogger(EditBuildPropertiesAction.class);

    private static final String PROPERTY_PREFIX = "property.";

    private String formSource;
    private String revision;
    private List<ResourceProperty> properties;
    private boolean ajax;
    private ConfigurationPanel newPanel;
    private ConfigurationResponse configurationResponse;
    private String submitField;

    private ScmClientFactory<ScmConfiguration> scmClientFactory;
    private ConfigurationProvider configurationProvider;
    private Configuration configuration;

    public boolean isCancelled()
    {
        return "cancel".equals(submitField);
    }

    public String getFormSource()
    {
        return formSource;
    }

    public List<ResourceProperty> getProperties()
    {
        return properties;
    }

    public String getRevision()
    {
        return revision;
    }

    public void setRevision(String revision)
    {
        this.revision = revision;
    }

    public void setPath(String path)
    {
        String[] elements = PathUtils.getPathElements(path);
        if(elements.length == 2 && elements[0].equals(ConfigurationRegistry.PROJECTS_SCOPE))
        {
            setProjectName(elements[1]);
        }
    }

    public void setAjax(boolean ajax)
    {
        this.ajax = ajax;
    }

    public void setSubmitField(String submitField)
    {
        this.submitField = submitField;
    }

    public ConfigurationPanel getNewPanel()
    {
        return newPanel;
    }

    public ConfigurationResponse getConfigurationResponse()
    {
        return configurationResponse;
    }

    private void renderForm() throws IOException, TemplateException
    {
        Project project = getRequiredProject();
        properties = new ArrayList<ResourceProperty>(project.getConfig().getProperties().values());
        Collections.sort(properties, new NamedConfigurationComparator());

        Form form = new Form("form", "edit.build.properties", (ajax ? "aaction/" : "") + "editBuildProperties.action");
        form.setAjax(ajax);

        Field field = new Field(FieldType.HIDDEN, "projectName");
        field.setValue(getProjectName());
        form.add(field);

        field = new Field(FieldType.TEXT, "revision");
        field.setLabel("revision");
        field.setValue(revision);
        addLatestAction(field, project.getConfig().getScm());

        form.add(field);

        for(ResourceProperty property: properties)
        {
            field = new Field(FieldType.TEXT, PROPERTY_PREFIX + property.getName());
            field.setLabel(property.getName());
            field.setValue(property.getValue());
            form.add(field);
        }

        addSubmit(form, "trigger");
        addSubmit(form, "cancel");

        Map<String, Object> context = new HashMap<String, Object>();
        context.put("projectId", project.getId());

        StringWriter writer = new StringWriter();
        ToveUtils.renderForm(context, form, getClass(), writer, configuration);
        formSource = writer.toString();
        newPanel = new ConfigurationPanel("aaction/edit-build-properties.vm");
    }

    private void addLatestAction(Field field, ScmConfiguration scm)
    {
        try
        {
            if(ScmClientUtils.getCapabilities(scm, scmClientFactory).contains(ScmCapability.REVISIONS))
            {
                field.addParameter(ACTIONS, Arrays.asList("getlatest"));
                field.addParameter(SCRIPTS, Arrays.asList("EditBuildPropertiesAction.getlatest"));
            }
        }
        catch (ScmException e)
        {
            // Just don't add the action.
        }
    }

    private void addSubmit(Form form, String name)
    {
        Field field = new Field(FieldType.SUBMIT, name);
        field.setValue(name);
        form.add(field);
    }

    public String doInput() throws Exception
    {
        renderForm();
        return INPUT;
    }

    private String getPath()
    {
        return PathUtils.getPath(ConfigurationRegistry.PROJECTS_SCOPE, getProjectName());
    }

    private void setupResponse()
    {
        String newPath = getPath();
        configurationResponse = new ConfigurationResponse(newPath, configurationTemplateManager.getTemplatePath(newPath));
    }

    public void doCancel()
    {
        setupResponse();
    }

    public String execute() throws IOException, TemplateException
    {
        Project project = getRequiredProject();

        // Ensure we are allowed to change the project configuration.
        getProjectManager().checkWrite(project);
        ProjectConfiguration projectConfig = configurationProvider.deepClone(project.getConfig());
        mapProperties(projectConfig);
        String path = configurationProvider.save(projectConfig);

        // Look up the config again with the change, and also rewire it to
        // the state.
        projectConfig = configurationProvider.get(path, ProjectConfiguration.class);
        project.setConfig(projectConfig);

        Revision r = null;
        if(TextUtils.stringSet(revision))
        {
            ScmClient client = null;
            try
            {
                client = scmClientFactory.createClient(projectConfig.getScm());
                r = client.parseRevision(revision);
            }
            catch (ScmException e)
            {
                addFieldError("revision", "Unable to verify revision: " + e.getMessage());
                LOG.severe(e);
                renderForm();
                return INPUT;
            }
            finally
            {
                ScmClientUtils.close(client);
            }
            
            // CIB-1162: Make sure we can get a pulse file at this revision
            try
            {
                TypeConfiguration projectType = projectConfig.getType();
                projectType.getPulseFile(projectConfig, r, null);
            }
            catch (Exception e)
            {
                addFieldError("revision", "Unable to get pulse file for revision: " + e.getMessage());
                LOG.severe(e);
                renderForm();
                return INPUT;
            }
        }

        try
        {
            projectManager.triggerBuild(projectConfig, new ManualTriggerBuildReason((String)getPrinciple()), r, ProjectManager.TRIGGER_CATEGORY_MANUAL, false, true);
        }
        catch (Exception e)
        {
            addActionError(e.getMessage());
            return ERROR;
        }

        try
        {
            // Pause for dramatic effect
            Thread.sleep(1000);
        }
        catch (InterruptedException e)
        {
            // Empty
        }

        setupResponse();
        return SUCCESS;
    }

    private void mapProperties(ProjectConfiguration projectConfig)
    {
        Map parameters = ActionContext.getContext().getParameters();
        for(Object n: parameters.keySet())
        {
            String name = (String) n;
            if(name.startsWith(PROPERTY_PREFIX))
            {
                String propertyName = name.substring(PROPERTY_PREFIX.length());
                ResourceProperty property = projectConfig.getProperty(propertyName);
                if(property != null)
                {
                    Object value = parameters.get(name);
                    if(value instanceof String)
                    {
                        property.setValue((String) value);
                    }
                    else if(value instanceof String[])
                    {
                        property.setValue(((String[])value)[0]);
                    }
                }
            }
        }
    }

    public void setScmClientFactory(ScmClientFactory<ScmConfiguration> scmClientFactory)
    {
        this.scmClientFactory = scmClientFactory;
    }

    public void setConfigurationProvider(ConfigurationProvider configurationProvider)
    {
        this.configurationProvider = configurationProvider;
    }

    public void setFreemarkerConfiguration(Configuration configuration)
    {
        this.configuration = configuration;
    }
}
