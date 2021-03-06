package com.zutubi.pulse.master.tove.velocity;

import com.zutubi.i18n.Messages;
import com.zutubi.pulse.core.spring.SpringComponentContext;
import com.zutubi.util.logging.Logger;
import org.apache.velocity.context.InternalContextAdapter;
import org.apache.velocity.exception.ParseErrorException;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.apache.velocity.runtime.parser.node.Node;

import java.io.IOException;
import java.io.Writer;
import java.util.Map;

/**
 * The I18N directive needs to be used in the context of a Type. It is the type
 * that provides the class context needed to retrieve the correct i18n bundle.
 *
 */
public class I18NDirective extends AbstractI18NDirective
{
    private static final Logger LOG = Logger.getLogger(I18NDirective.class);

    private static final boolean DEBUG_MODE = false;

    public I18NDirective()
    {
        SpringComponentContext.autowire(this);
    }

    /**
     * @see org.apache.velocity.runtime.directive.Directive#getName() 
     */
    public String getName()
    {
        return "i18n";
    }

    /**
     * @see org.apache.velocity.runtime.directive.Directive#getType() 
     * @see org.apache.velocity.runtime.directive.DirectiveConstants#LINE 
     */
    public int getType()
    {
        return LINE;
    }

    public boolean render(InternalContextAdapter context, Writer writer, Node node) throws IOException, ResourceNotFoundException, ParseErrorException
    {
        // validation: key field is required.

        try
        {
            Map params = createPropertyMap(context, node);
            wireParams(params);

            Messages messages = getMessages();

            String value = DEBUG_MODE ? "unresolved: " + key : "";
            if (messages.isKeyDefined(this.key))
            {
                value = messages.format(this.key);
            }
            writer.write(value);

            return true;
        }
        catch (Exception e)
        {
            writer.write(renderError("Failed to render. Unexpected " + e.getClass() + ": " + e.getMessage()));
            LOG.severe(e);
            return true;
        }
    }

}
