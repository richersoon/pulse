package com.zutubi.pulse.webwork.velocity;

import com.opensymphony.webwork.views.velocity.components.AbstractDirective;
import com.opensymphony.webwork.components.Component;
import com.opensymphony.xwork.util.OgnlValueStack;
import com.zutubi.pulse.webwork.components.CustomCheckbox;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * <class-comment/>
 */
public class CustomCheckboxDirective extends AbstractDirective
{
    public String getBeanName()
    {
        return "ccheckbox";
    }

    protected Component getBean(OgnlValueStack stack, HttpServletRequest req, HttpServletResponse res)
    {
        return new CustomCheckbox(stack, req, res);
    }

    public int getType()
    {
        return LINE;
    }
}
