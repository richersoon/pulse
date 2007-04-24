package com.zutubi.prototype;

import com.zutubi.config.annotations.Handler;
import com.zutubi.prototype.handler.AnnotationHandler;
import com.zutubi.prototype.model.SelectFieldDescriptor;
import com.zutubi.prototype.type.CollectionType;
import com.zutubi.prototype.type.CompositeType;
import com.zutubi.prototype.type.EnumType;
import com.zutubi.prototype.type.PrimitiveType;
import com.zutubi.prototype.type.ReferenceType;
import com.zutubi.prototype.type.SimpleType;
import com.zutubi.prototype.type.Type;
import com.zutubi.prototype.type.TypeProperty;
import com.zutubi.prototype.type.TypeRegistry;
import com.zutubi.prototype.type.record.PathUtils;
import com.zutubi.pulse.prototype.config.EnumOptionProvider;
import com.zutubi.util.bean.DefaultObjectFactory;
import com.zutubi.util.bean.ObjectFactory;
import com.zutubi.util.logging.Logger;
import com.zutubi.validation.annotations.Constraint;
import com.zutubi.validation.validators.RequiredValidator;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 *
 *
 */
public class FormDescriptorFactory
{
    /**
     * The object factory is required for the instantiation of objects that occurs within the form descriptor.
     * To ensure that this always works, we default to a base implementation of the Object factory, which simply
     * instantiated objects.  When deployed, this should be replaced by the auto wiring object factory.
     */
    private ObjectFactory objectFactory = new DefaultObjectFactory();

    private static final Logger LOG = Logger.getLogger(FormDescriptorFactory.class);

    // FIXME: extract these mappings to make them extendable.
    private static final Map<Class, String> defaultFieldTypeMapping = new HashMap<Class, String>();
    static
    {
        defaultFieldTypeMapping.put(String.class, "text");
        defaultFieldTypeMapping.put(Boolean.class, "checkbox");
        defaultFieldTypeMapping.put(Boolean.TYPE, "checkbox");
        defaultFieldTypeMapping.put(Integer.class, "text");
        defaultFieldTypeMapping.put(Integer.TYPE, "text");
        defaultFieldTypeMapping.put(Long.class, "text");
        defaultFieldTypeMapping.put(Long.TYPE, "text");
    }
/*
    private static final Map<Class, Class<? extends AnnotationHandler>> defaultHandlerMapping = new HashMap<Class, Class<? extends AnnotationHandler>>();
    static
    {
        defaultHandlerMapping.put(Field.class, FieldAnnotationHandler.class);
        defaultHandlerMapping.put(Form.class, FormAnnotationHandler.class);
        defaultHandlerMapping.put(Password.class, FieldAnnotationHandler.class);
        defaultHandlerMapping.put(Reference.class, ReferenceAnnotationHandler.class);
        defaultHandlerMapping.put(Select.class, SelectAnnotationHandler.class);
        defaultHandlerMapping.put(Password.class, FieldAnnotationHandler.class);
        defaultHandlerMapping.put(Text.class, FieldAnnotationHandler.class);
        defaultHandlerMapping.put(TextArea.class, FieldAnnotationHandler.class);
        // typeselect
    }
*/

    private TypeRegistry typeRegistry;

    public FormDescriptor createDescriptor(String path, Class clazz)
    {
        return createDescriptor(path, typeRegistry.getType(clazz));
    }

    public FormDescriptor createDescriptor(String path, String symbolicName)
    {
        return createDescriptor(path, typeRegistry.getType(symbolicName));
    }

    public FormDescriptor createDescriptor(String path, CompositeType type)
    {
        FormDescriptor descriptor = new FormDescriptor();

        // The symbolic name uniquely identifies the type, and so will uniquely identify this form.
        // (we are not planning to have multiple forms on a single page at this stage...) 
        descriptor.setId(type.getClazz().getName());

        // Process the annotations at apply to the type / form.
        List<Annotation> annotations = type.getAnnotations();
        handleAnnotations(type, descriptor, annotations);

        descriptor.setFieldDescriptors(buildFieldDescriptors(path, type));
        descriptor.setActions(Arrays.asList("save", "cancel"));

        return descriptor;
    }

    private List<FieldDescriptor> buildFieldDescriptors(String path, CompositeType type)
    {
        List<FieldDescriptor> fieldDescriptors = new LinkedList<FieldDescriptor>();

        for (TypeProperty property : type.getProperties(SimpleType.class))
        {
            FieldDescriptor fd = createField(path, property);
            addFieldParameters(type, path, property, fd);
            fieldDescriptors.add(fd);
        }

        for(TypeProperty property: type.getProperties(CollectionType.class))
        {
            CollectionType propertyType = (CollectionType) property.getType();
            Type targetType = propertyType.getCollectionType();
            if(targetType instanceof EnumType || targetType instanceof ReferenceType)
            {
                SelectFieldDescriptor fd = new SelectFieldDescriptor();
                fd.setPath(PathUtils.getPath(path, property.getName()));
                fd.setProperty(property);
                fd.setName(property.getName());
                fd.setType("select");
                fd.setMultiple(true);
                addFieldParameters(type, path, property, fd);
                fieldDescriptors.add(fd);
            }
        }

        return fieldDescriptors;
    }

    private void addFieldParameters(CompositeType type, String path, TypeProperty property, FieldDescriptor fd)
    {
        handleAnnotations(type, fd, property.getAnnotations());
        if("select".equals(fd.getType()) && !fd.hasParameter("list"))
        {
            addDefaultOptions(path, property, (SelectFieldDescriptor)fd);
        }
    }

    private FieldDescriptor createField(String path, TypeProperty property)
    {
        SimpleType propertyType = (SimpleType) property.getType();
        String fieldType = "select";
        if(propertyType instanceof PrimitiveType)
        {
            // some little bit of magic, take a guess at any property called password. If we come up with any
            // other magical cases, then we can refactor this a bit.
            if (property.getName().equals("password"))
            {
                fieldType = "password";
            }
            else
            {
                fieldType = defaultFieldTypeMapping.get(propertyType.getClazz());
            }
        }

        FieldDescriptor fd = new FieldDescriptor();
        if (fieldType.equals("select"))
        {
            fd = new SelectFieldDescriptor();            
        }

        fd.setType(fieldType);
        fd.setPath(PathUtils.getPath(path, property.getName()));
        fd.setProperty(property);
        fd.setName(property.getName());
        return fd;
    }

    private void addDefaultOptions(String path, TypeProperty typeProperty, SelectFieldDescriptor fd)
    {
        // FIXME this dups code in the OptionAnnotationHandler
        // FIXME i have not fixed now because this class itself seems to
        // FIXME a prime candidate for refactoring when we understand
        // FIXME things better
        if(typeProperty.getType().getTargetType() instanceof EnumType)
        {
            OptionProvider optionProvider = new EnumOptionProvider();
            fd.setList(optionProvider.getOptions(path, typeProperty));
            if (optionProvider.getOptionKey() != null)
            {
                fd.setListKey(optionProvider.getOptionKey());
            }
            if (optionProvider.getOptionValue() != null)
            {
                fd.setListValue(optionProvider.getOptionValue());
            }
        }
        else
        {
            fd.setList(Collections.EMPTY_LIST);
        }
    }

    /**
     * This handle annotation method will serach through the annotaion
     * hierarchy, looking for annotations that have a handler mapped to them.
     * When found, the handler is run in the context of the annotation and
     * the descriptor.
     *
     * Note: This method will process all of the annotation's annotations as well.
     *
     * @param type       the composite type that has been annotated (or meta-annotated)
     * @param descriptor the target that will be modified by these annotations.
     * @param annotations the annotations that need to be processed.
     */
    private void handleAnnotations(CompositeType type, Descriptor descriptor, List<Annotation> annotations)
    {
        for (Annotation annotation : annotations)
        {
            if (annotation.annotationType().getName().startsWith("java.lang"))
            {
                // ignore standard annotations.
                continue;
            }

            // recurse up the annotation hierarchy.
            handleAnnotations(type, descriptor, Arrays.asList(annotation.annotationType().getAnnotations()));

            Handler handlerAnnotation = annotation.annotationType().getAnnotation(Handler.class);
            if (handlerAnnotation != null)
            {
                try
                {
                    AnnotationHandler handler = objectFactory.buildBean(handlerAnnotation.className());
                    handler.process(type, annotation, descriptor);
                }
                catch (InstantiationException e)
                {
                    LOG.warning("Failed to instantiate annotation handler.", e); // failed to instantiate the annotation handler.
                }
                catch (IllegalAccessException e)
                {
                    LOG.warning("Failed to instantiate annotation handler.", e); // failed to instantiate the annotation handler.
                }
                catch (Exception e)
                {
                    LOG.warning("Unexpected exception processing the annotation handler.", e);
                }
            }
            // This needs to be handled by a handler somehow - remembering that the annotations themselves
            // are not part of the form package.  ...
            if (annotation instanceof Constraint)
            {
                FieldDescriptor fieldDescriptor = (FieldDescriptor) descriptor;
                fieldDescriptor.setConstrained(true);
                List<String> constraints = Arrays.asList(((Constraint)annotation).value());
                if (constraints.contains(RequiredValidator.class.getName()))
                {
                    fieldDescriptor.setRequired(true);
                }
            }
        }
    }

    /**
     * Required resource
     *
     * @param typeRegistry instance.
     */
    public void setTypeRegistry(TypeRegistry typeRegistry)
    {
        this.typeRegistry = typeRegistry;
    }

    public void setObjectFactory(ObjectFactory objectFactory)
    {
        this.objectFactory = objectFactory;
    }
}
