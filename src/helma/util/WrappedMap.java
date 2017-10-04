/*
 * Helma License Notice
 *
 * The contents of this file are subject to the Helma License
 * Version 2.0 (the "License"). You may not use this file except in
 * compliance with the License. A copy of the License is available at
 * http://adele.helma.org/download/helma/license.txt
 *
 * Copyright 1998-2003 Helma Software. All Rights Reserved.
 */

package helma.util;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 *  A Map that wraps another map. We use this class to be able to
 *  wrap maps as native objects within a scripting engine rather
 *  than exposing them through Java reflection.
 *  Additionally, instances of this class can be set to readonly
 *  so that the original map can't be modified.
 */
public class WrappedMap implements Map {

    // the wrapped map
    protected Map wrapped = null;

    // is this map readonly?
    protected boolean readonly = false;

    /**
     *  Constructor
     */
    public WrappedMap(Map map) {
        this(map, false);
    }

    /**
     *  Constructor
     */
    public WrappedMap(Map map, boolean readonly) {
        if (map == null) {
            throw new NullPointerException(
                Messages.getString("WrappedMap.0")); //$NON-NLS-1$
        }
        this.wrapped = map;
        this.readonly = readonly;
    }

    /**
     *  Set the readonly flag on or off
     */
    public void setReadonly(boolean readonly) {
        this.readonly = readonly;
    }

    /**
     *  Is this map readonly?
     */
    public boolean isReadonly() {
        return this.readonly;
    }

    // Methods from interface java.util.Map -
    // these are just proxies to the wrapped map, except for
    // readonly checks on modifiers.

    public int size() {
        return this.wrapped.size();
    }

    public boolean isEmpty() {
        return this.wrapped.isEmpty();
    }

    public boolean containsKey(Object key) {
        return this.wrapped.containsKey(key);
    }

    public boolean containsValue(Object value) {
        return this.wrapped.containsValue(value);
    }

    public Object get(Object key) {
        return this.wrapped.get(key);
    }

    // Modification Operations - check for readonly

    public Object put(Object key, Object value) {
        if (this.readonly) {
            throw new RuntimeException(Messages.getString("WrappedMap.1")); //$NON-NLS-1$
        }
        return this.wrapped.put(key, value);
    }

    public Object remove(Object key) {
        if (this.readonly) {
            throw new RuntimeException(Messages.getString("WrappedMap.2")); //$NON-NLS-1$
        }
        return this.wrapped.remove(key);
    }

    public void putAll(Map t) {
        if (this.readonly) {
            throw new RuntimeException(Messages.getString("WrappedMap.3")); //$NON-NLS-1$
        }
        this.wrapped.putAll(t);
    }

    public void clear() {
        if (this.readonly) {
            throw new RuntimeException(Messages.getString("WrappedMap.4")); //$NON-NLS-1$
        }
        this.wrapped.clear();
    }


    // Views

    public Set keySet() {
        return this.wrapped.keySet();
    }

    public Collection values() {
        return this.wrapped.values();
    }

    public Set entrySet() {
        return this.wrapped.entrySet();
    }


    // Comparison and hashing

    @Override
    public boolean equals(Object o) {
        return this.wrapped.equals(o);
    }

    @Override
    public int hashCode() {
        return this.wrapped.hashCode();
    }

    // toString

    @Override
    public String toString() {
        return this.wrapped.toString();
    }

}
