package com.zutubi.prototype;

import com.zutubi.prototype.type.TypeException;
import com.zutubi.prototype.type.TypeRegistry;
import junit.framework.TestCase;

import java.util.List;

/**
 *
 *
 */
public class FormDescriptorFactoryTest extends TestCase
{
    private TypeRegistry typeRegistry;
    private FormDescriptorFactory descriptorFactory;

    protected void setUp() throws Exception
    {
        super.setUp();

        typeRegistry = new TypeRegistry();
        descriptorFactory = new FormDescriptorFactory();
        descriptorFactory.setTypeRegistry(typeRegistry);
    }

    protected void tearDown() throws Exception
    {
        typeRegistry = null;
        descriptorFactory = null;

        super.tearDown();
    }

    public void testPasswordField() throws TypeException
    {
        typeRegistry.register("mock", MockPassword.class);
        FormDescriptor formDescriptor = descriptorFactory.createDescriptor("path", "mock");

        List<FieldDescriptor> fieldDescriptors = formDescriptor.getFieldDescriptors();
        assertEquals(1, fieldDescriptors.size());
        assertEquals("password", fieldDescriptors.get(0).getType());
    }

    public void testTextField() throws TypeException
    {
        typeRegistry.register("mock", MockText.class);
        FormDescriptor formDescriptor = descriptorFactory.createDescriptor("path", "mock");

        List<FieldDescriptor> fieldDescriptors = formDescriptor.getFieldDescriptors();
        assertEquals(1, fieldDescriptors.size());
        assertEquals("text", fieldDescriptors.get(0).getType());
    }

    private class MockText
    {
        private String text;

        public String getText()
        {
            return text;
        }

        public void setText(String text)
        {
            this.text = text;
        }
    }

    private class MockPassword
    {
        private String password;

        public String getPassword()
        {
            return password;
        }

        public void setPassword(String password)
        {
            this.password = password;
        }
    }
}
