package com.zutubi.prototype.type.record;

import java.util.Set;

/**
 * A record defines a simple map for storing data.  
 *
 */
public interface Record
{

    /**
     * Get the symbolic name associated with the data for this record.  The symbolic name defined the type of the
     * records data.
     * 
     * @return the symbolic name
     *
     * @see com.zutubi.prototype.type.TypeRegistry
     */
    String getSymbolicName();

    /**
     * Retrieve meta data associated with this record.
     *
     * @param key of the meta data to be retrieved.
     *
     * @return value of the meta data, or null if the meta data does not exist.
     */
    String getMeta(String key);

    Object get(String key);

    int size();

    boolean containsKey(String key);

    /**
     * Create a copy of this record.
     *
     * @param deep if true, the copy is deep: child records are also copied
     * @return a value copy of this record
     */
    MutableRecord copy(boolean deep);

    Set<String> keySet();

    Set<String> metaKeySet();
}
