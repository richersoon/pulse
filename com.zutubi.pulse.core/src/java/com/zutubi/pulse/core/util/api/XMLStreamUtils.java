package com.zutubi.pulse.core.util.api;

import com.zutubi.util.logging.Logger;

import javax.xml.stream.XMLStreamConstants;
import static javax.xml.stream.XMLStreamConstants.END_ELEMENT;
import static javax.xml.stream.XMLStreamConstants.START_ELEMENT;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

/**
 * XML Utilities used for working with a pull parser.
 */
public class XMLStreamUtils
{
    private static final Logger LOG = Logger.getLogger(XMLStreamUtils.class);

    /**
     * Get a map containing the attributes of the current tag.
     *
     * @param reader    the reader whose state references the tag for which the
     *                  attributes will be retrieved.
     * @return a map of attribute keys to attribute values.
     */
    public static Map<String, String> getAttributes(XMLStreamReader reader)
    {
        Map<String, String> attributes = new HashMap<String, String>();
        for (int i = 0; i < reader.getAttributeCount(); i++)
        {
            String name = reader.getAttributeLocalName(i);
            String value = reader.getAttributeValue(i);
            attributes.put(name, value);
        }
        return attributes;
    }

    /**
     * Free any resources currently held by the XMLStreamReader instance.
     *
     * Note: This does not close the underlying input source.
     *
     * @param reader    the reader being closed.
     *
     * @see javax.xml.stream.XMLStreamReader#close() 
     */
    public static void close(XMLStreamReader reader)
    {
        if (reader != null)
        {
            try
            {
                reader.close();
            }
            catch (XMLStreamException e)
            {
                LOG.finest(e);
            }
        }
    }

    /**
     * Move the xml stream readers cursor to the end tag that matches the
     * current start tag, skipping the contents of the element.
     *
     * This requires that the reader is currently at a start element. That is,
     * reader.getEventType() == XMLStreamConstants.START_ELEMENT
     *
     * @param reader            the reader
     * @throws javax.xml.stream.XMLStreamException on error
     */
    public static void skipElement(XMLStreamReader reader) throws XMLStreamException
    {
        int eventType = reader.getEventType();
        if (eventType != START_ELEMENT)
        {
            throw new IllegalStateException("Expected reader to be at a start element, but instead found " + reader.getEventType());
        }

        Stack<String> tags = new Stack<String>();
        tags.push(reader.getLocalName());

        while (!tags.isEmpty())
        {
            // Unless the xml document is invalid, we can safely assume that tags.isEmpty()
            // will be true before reader.hasNext is false.
            eventType = reader.next();

            if (eventType == START_ELEMENT)
            {
                tags.push(reader.getLocalName());
            }
            else if (eventType == END_ELEMENT)
            {
                tags.pop();
            }
        }
    }

    /**
     * Convert the specified integer into the name of the associated XMLStreamConstants constant.
     *
     * @param xmlStreamConstant the integer to be mapped to the constants in the XMLStreamConstants object.
     * @return  the name of the constant, or "<unknown>" if it is not located.
     */
    public static String toString(int xmlStreamConstant)
    {
        try
        {
            Field[] fields = XMLStreamConstants.class.getDeclaredFields();
            for (Field field : fields)
            {
                if (xmlStreamConstant == (Integer)field.get(XMLStreamConstants.class))
                {
                    return field.getName();
                }
            }
        }
        catch (Exception e)
        {
            // noop
        }
        return "<unknown>";
    }

    /**
     * Move the readers cursor to the start tag of the next element, returning true if
     * a new element is encountered, false otherwise.
     *
     * Note that this operation will skip over any nested elements, but not 'skip out of'
     * the current element depth within the xml document.
     *
     * @param reader    the reader being shifted.
     * @return true if a new element is encountered, false otherwise.
     *
     * @throws XMLStreamException on error.
     */
    public static boolean nextElement(XMLStreamReader reader) throws XMLStreamException
    {
        skipElement(reader);

        reader.nextTag();

        return (reader.isStartElement());
    }

    /**
     * Throws an XMLStreamException if the tag at the current cursor location
     * of the stream reader is not a start tag and if the name does not match
     * the specified name.
     *
     * @param localName     the expected element name
     * @param reader        the xml stream reader whose cursor location is to be checked.
     *
     * @throws XMLStreamException if either of the expected conditions are not satisfied.
     */
    public static void expectStartTag(String localName, XMLStreamReader reader) throws XMLStreamException
    {
        if (!reader.getLocalName().equals(localName) || !reader.isStartElement())
        {
            throw new XMLStreamException("Expected " + toString(XMLStreamConstants.START_ELEMENT) + ":" +
                    localName + ", instead found " + toString(reader.getEventType()) + ":" +
                    reader.getLocalName() + " at " + reader.getLocation());
        }
    }

    /**
     * Throws an XMLStreamException if the tag at the current cursor location
     * of the stream reader is not an end tag and if the name does not match
     * the specified name.
     *
     * @param localName     the expected element name
     * @param reader        the xml stream reader whose cursor location is to be checked.
     *
     * @throws XMLStreamException if either of the expected conditions are not satisfied.
     */
    public static void expectEndTag(String localName, XMLStreamReader reader) throws XMLStreamException
    {
        if (!isElement(localName, reader) || !reader.isEndElement())
        {
            throw new XMLStreamException("Expected " + toString(XMLStreamConstants.END_ELEMENT) + ":" +
                    localName + ", instead found " + toString(reader.getEventType()) + ":" +
                    reader.getLocalName() + " at " + reader.getLocation());
        }
    }

    /**
     * This method reads through all of the elements at the current level of the xml document,
     * converting them into a map by using the elements localname as the key, and the elementText
     * as the value.
     *
     * This method requires that all of the elements encountered as plain text elements.  No nested
     * elements are supported.
     *
     * @param reader    the xml stream reader that provides the element data.
     *
     * @return a map of keys that represent the element names, and values that represent
     * the elements text value.
     *
     * @throws XMLStreamException on error or if a nested element is encountered.
     */
    public static Map<String, String> readElements(XMLStreamReader reader) throws XMLStreamException
    {
        Map<String, String> elements = new HashMap<String, String>();

        while (reader.isStartElement())
        {
            String name = reader.getLocalName();
            String text = reader.getElementText();

            elements.put(name, text);

            reader.nextTag();
        }
        return elements;
    }

    /**
     * Returns true if the name of the element at the readers cursor matches the
     * specified name.
     *
     * @param elementName   the element name to match
     * @param reader        the reader whose cursor location is being evaluated.
     * @return  true if the specified element name matches the current cursor location
     * in the reader, false otherwise.
     */
    public static boolean isElement(String elementName, XMLStreamReader reader)
    {
        return reader.getLocalName().equals(elementName);
    }
}