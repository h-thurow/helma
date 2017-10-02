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

package helma.scripting.rhino;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.Wrapper;
import org.mozilla.javascript.Undefined;
import java.util.HashMap;
import java.util.Map;

/**
 *  A class that wraps a Java Map as a native JavaScript object. This is
 *  used by the RhinoCore Wrapper for instances of helma.util.SystemMap
 *  and helma.util.WrappedMap.
 */
public class MapWrapper extends ScriptableObject implements Wrapper {
    private static final long serialVersionUID = -8802538795495729410L;

    Map map;
    RhinoCore core;

    /**
     * Creates a new MapWrapper object.
     */
    public MapWrapper() {
        this.map = null;
    }

    /**
     * Creates a new MapWrapper object.
     *
     * @param map the Map
     * @param core the RhinoCore instance
     */
    public MapWrapper(Map map, RhinoCore core) {
        this.map = map;
        this.core = core;
        setParentScope(core.global);
        setPrototype(ScriptableObject.getObjectPrototype(core.global));
    }

    /**
     *
     *
     * @param name ...
     * @param start ...
     * @param value ...
     */
    @Override
    public void put(String name, Scriptable start, Object value) {
        if (this.map == null) {
            this.map = new HashMap();
        }

        if (value == null || value == Undefined.instance) {
            this.map.remove(name);
        } else if (value instanceof Wrapper) {
            this.map.put(name, ((Wrapper) value).unwrap());
        } else {
            this.map.put(name, value);
        }
    }

    /**
     *
     *
     * @param name ...
     * @param start ...
     *
     * @return ...
     */
    @Override
    public Object get(String name, Scriptable start) {
        if (this.map == null) {
            return null;
        }

        Object obj = this.map.get(name);

        if (obj != null && !(obj instanceof Scriptable)) {
            // do NOT wrap primitives - otherwise they'll be wrapped as Objects,
            // which makes them unusable for many purposes (e.g. ==)
            if (obj instanceof String ||
                    obj instanceof Number ||
                    obj instanceof Boolean) {
                return obj;
            }

            return Context.toObject(obj, this.core.global);
        }

        if (obj != null)
            return obj;
        if (map.containsKey(name))
            return null;
        return Scriptable.NOT_FOUND;
    }

    /**
     *
     *
     * @param name ...
     * @param start ...
     *
     * @return ...
     */
    @Override
    public boolean has(String name, Scriptable start) {
        return (this.map != null) && this.map.containsKey(name);
    }

    /**
     *
     *
     * @param name ...
     */
    @Override
    public void delete(String name) {
        if (this.map != null) {
            this.map.remove(name);
        }
    }

    /**
     *
     *
     * @param idx ...
     * @param start ...
     * @param value ...
     */
    @Override
    public void put(int idx, Scriptable start, Object value) {
        if (this.map == null) {
            this.map = new HashMap();
        }

        if (value == null || value == Undefined.instance) {
            this.map.remove(Integer.toString(idx));
        } else if (value instanceof Wrapper) {
            this.map.put(Integer.toString(idx), ((Wrapper) value).unwrap());
        } else {
            this.map.put(Integer.toString(idx), value);
        }
    }

    /**
     *
     *
     * @param idx ...
     * @param start ...
     *
     * @return ...
     */
    @Override
    public Object get(int idx, Scriptable start) {
        if (this.map == null) {
            return null;
        }

        String name = Integer.toString(idx);
        Object obj = this.map.get(name);

        if (obj != null && !(obj instanceof Scriptable)) {
            // do NOT wrap primitives - otherwise they'll be wrapped as Objects,
            // which makes them unusable for many purposes (e.g. ==)
            if (obj instanceof String ||
                    obj instanceof Number ||
                    obj instanceof Boolean) {
                return obj;
            }

            return Context.toObject(obj, this.core.global);
        }

        if (obj != null)
            return obj;
        if (map.containsKey(name))
            return null;
        return Scriptable.NOT_FOUND;
    }

    /**
     *
     *
     * @param idx ...
     * @param start ...
     *
     * @return ...
     */
    @Override
    public boolean has(int idx, Scriptable start) {
        return (this.map != null) && this.map.containsKey(Integer.toString(idx));
    }

    /**
     *
     *
     * @param idx ...
     */
    @Override
    public void delete(int idx) {
        if (this.map != null) {
            this.map.remove(Integer.toString(idx));
        }
    }


    /**
     * Return an array containing the property key values of this map.
     */
    @Override
    public Object[] getIds() {
        if (this.map == null) {
            return new Object[0];
        }

        return this.map.keySet().toArray();
    }

    @Override
    public Object getDefaultValue(Class hint) {
        if (hint == null || hint == String.class) {
            return this.map == null ? "{}" : this.map.toString(); //$NON-NLS-1$
        }
        return super.getDefaultValue(hint);
    }

    /**
     * Return the wrapped Map object.
     */
    public Object unwrap() {
        if (this.map == null) {
            this.map = new HashMap();
        }
        return this.map;
    }

    /**
     * Return the class name for wrapped maps.
     */
    @Override
    public String getClassName() {
        return "[MapWrapper]"; //$NON-NLS-1$
    }

    /**
     * Return a string representation for this wrapped map. This calls
     * Map.toString(), so usually the contents of the map will be listed.
     */
    @Override
    public String toString() {
        if (this.map == null) {
            return "[MapWrapper{}]"; //$NON-NLS-1$
        }
        return "[MapWrapper"+this.map.toString()+"]";  //$NON-NLS-1$//$NON-NLS-2$
    }
}
