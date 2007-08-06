package com.zutubi.prototype.type;

import com.zutubi.config.annotations.ID;
import com.zutubi.prototype.config.ConfigurationReferenceManager;
import com.zutubi.pulse.core.config.Configuration;
import com.zutubi.util.AnnotationUtils;
import com.zutubi.util.logging.Logger;

import java.beans.IntrospectionException;

/**
 * A type that represents a reference to some composite type.  The reference
 * value itself is just a path, which resolves to a record stored at that
 * location.
 */
public class ReferenceType extends SimpleType implements Type
{
    private static final Logger LOG = Logger.getLogger(ReferenceType.class);

    private CompositeType referencedType;
    private ConfigurationReferenceManager configurationReferenceManager;
    private String idProperty;

    public ReferenceType(CompositeType referencedType, ConfigurationReferenceManager configurationReferenceManager) throws TypeException
    {
        super(referencedType.getClazz());
        this.referencedType = referencedType;
        this.configurationReferenceManager = configurationReferenceManager;

        try
        {
            idProperty = AnnotationUtils.getPropertyAnnotatedWith(referencedType.getClazz(), ID.class);
        }
        catch (IntrospectionException e)
        {
            LOG.severe(e);
        }

        if(idProperty == null)
        {
            throw new TypeException("Referenced types must have an ID property");
        }
    }

    public CompositeType getReferencedType()
    {
        return referencedType;
    }

    public String getIdProperty()
    {
        return idProperty;
    }

    public Configuration instantiate(Object data, Instantiator instantiator) throws TypeException
    {
        long handle = getHandle(data);
        if(handle > 0)
        {
            return instantiator.resolveReference(handle);
        }
        else
        {
            // Zero handle == null reference.
            return null;
        }
    }

    private long getHandle(Object data) throws TypeException
    {
        String referenceHandle = (String) data;
        try
        {
            return Long.parseLong(referenceHandle);
        }
        catch (NumberFormatException e)
        {
            throw new TypeException("Illegal reference '" + referenceHandle + "'");
        }
    }

    public Object unstantiate(Object instance) throws TypeException
    {
        return instance == null ? 0 : Long.toString(((Configuration)instance).getHandle());
    }

    public Object toXmlRpc(Object data) throws TypeException
    {
        // We return references via the remote api as paths so that the
        // caller can use the value in subsequent calls.
        long handle = getHandle(data);
        if(handle > 0)
        {
            return configurationReferenceManager.getPathForHandle(handle);
        }
        else
        {
            return null;
        }
    }

    public String fromXmlRpc(Object data) throws TypeException
    {
        if(data == null)
        {
            return "0";
        }
        else
        {
            typeCheck(data, String.class);
            String path = (String) data;
            long handle = configurationReferenceManager.getHandleForPath(path);
            if(handle == 0)
            {
                throw new TypeException("Reference to unknown path '" + path + "'");
            }

            return Long.toString(handle);
        }
    }
}
