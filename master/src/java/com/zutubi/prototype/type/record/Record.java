package com.zutubi.prototype.type.record;

import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.Collection;

/**
 * Simple record that holds key:value data, along with meta data.
 *
 */
public class Record implements Map<String, Object>, Cloneable
{
    private Map<String, String> meta = new HashMap<String, String>();
    
    private Map<String, Object> data = new HashMap<String, Object>();

    public Record()
    {
    }

    public void setSymbolicName(String name)
    {
        meta.put("symbolicName", name);
    }

    public String getSymbolicName()
    {
        return meta.get("symbolicName");
    }

    public void putMeta(String key, String value)
    {
        meta.put(key, value);
    }

    public String getMeta(String key)
    {
        return meta.get(key);
    }

    public void putAll(Record newRecord)
    {
        meta.putAll(newRecord.meta);
        data.putAll(newRecord.data);
    }

    public Object put(String key, Object value)
    {
        return data.put(key, value);
    }

    public int size()
    {
        return data.size();
    }

    public Set<String> keySet()
    {
        return data.keySet();
    }

    public boolean isEmpty()
    {
        return data.isEmpty();
    }

    public boolean containsKey(Object key)
    {
        return data.containsKey(key);
    }

    public boolean containsValue(Object value)
    {
        return data.containsValue(value);
    }

    public Object get(Object key)
    {
        return data.get(key);
    }

    public Object remove(Object key)
    {
        return data.remove(key);
    }

    public void putAll(Map<? extends String, ? extends Object> t)
    {
        this.data.putAll(t);
    }

    public void clear()
    {
        data.clear();
    }

    public Collection<Object> values()
    {
        return data.values();
    }

    public Set<Entry<String, Object>> entrySet()
    {
        return data.entrySet();
    }

    public Record clone() throws CloneNotSupportedException
    {
        super.clone();

        Record clone = new Record();

        for (Map.Entry<String, String> entry : meta.entrySet())
        {
            String key = new String(entry.getKey());
            String value = new String(entry.getValue());
            clone.putMeta(key, value);
        }

        for (Map.Entry<String, Object> entry : data.entrySet())
        {
            String key = new String(entry.getKey());
            Object value = entry.getValue();
            if (value instanceof String)
            {
                value = new String((String)value);
            }
            else if (value instanceof Record)
            {
                value = ((Record)value).clone();
            }
            clone.put(key, value);
        }
        return clone;
    }
}
