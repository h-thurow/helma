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
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * A Map that wraps another map and creates a new copy of the 
 * wrapped map if we try to modify it. This class is wrapped 
 * as a native scripted object in JavaScript rather than exposing 
 * them through Java reflection.
 *
 * All methods in this class are synchronized in order not 
 * to miss the switch between original and copied map.
 */
public class CopyOnWriteMap extends WrappedMap {

    boolean modified = false;

    /**
     * Constructor
     */
    public CopyOnWriteMap(Map map) {
        super(map);
    }

    public synchronized boolean wasModified() {
        return this.modified;
    }

    @Override
    public synchronized int size() {
        return this.wrapped.size();
    }

    @Override
    public synchronized boolean isEmpty() {
        return this.wrapped.isEmpty();
    }

    @Override
    public synchronized boolean containsKey(Object key) {
        return this.wrapped.containsKey(key);
    }

    @Override
    public synchronized boolean containsValue(Object value) {
        return this.wrapped.containsValue(value);
    }

    @Override
    public synchronized Object get(Object key) {
        return this.wrapped.get(key);
    }

    // Modification Operations - check for readonly

    @Override
    public synchronized Object put(Object key, Object value) {
        if (!this.modified) {
            this.wrapped = new HashMap(this.wrapped);
            this.modified = true;
        }
        return this.wrapped.put(key, value);
    }

    @Override
    public synchronized Object remove(Object key) {
        if (!this.modified) {
            this.wrapped = new HashMap(this.wrapped);
            this.modified = true;
        }
        return this.wrapped.remove(key);
    }

    @Override
    public synchronized void putAll(Map t) {
        if (!this.modified) {
            this.wrapped = new HashMap(this.wrapped);
            this.modified = true;
        }
        this.wrapped.putAll(t);
    }

    @Override
    public synchronized void clear() {
        if (!this.modified) {
            this.wrapped = new HashMap(this.wrapped);
            this.modified = true;
        }
        this.wrapped.clear();
    }

    // Views

    @Override
    public synchronized Set keySet() {
        return this.wrapped.keySet();
    }

    @Override
    public synchronized Collection values() {
        return this.wrapped.values();
    }

    @Override
    public synchronized Set entrySet() {
        return this.wrapped.entrySet();
    }

    // Comparison and hashing

    @Override
    public synchronized boolean equals(Object o) {
        return this.wrapped.equals(o);
    }

    @Override
    public synchronized int hashCode() {
        return this.wrapped.hashCode();
    }

    // toString

    @Override
    public synchronized String toString() {
        return this.wrapped.toString();
    }

}
