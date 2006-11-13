package com.zutubi.pulse.renderer;

import com.zutubi.pulse.core.model.Changelist;
import com.zutubi.pulse.model.BuildResult;

import java.io.Writer;
import java.util.List;

/**
 * A BuildResultRenderer converts a build model into a displayable form, based
 * on a specified template.
 *
 * @author jsankey
 */
public interface BuildResultRenderer
{
    public void render(String baseUrl, BuildResult result, List<Changelist> changelists, String templateName, Writer writer);
    public List<TemplateInfo> getAvailableTemplates(boolean personal);
    public TemplateInfo getTemplateInfo(String templateName, boolean personal);   
}