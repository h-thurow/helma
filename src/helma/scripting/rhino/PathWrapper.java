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

import java.io.UnsupportedEncodingException;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.Undefined;
import org.mozilla.javascript.Wrapper;

import helma.framework.core.RequestPath;

/**
 * This class wraps around instances of helma.framework.core.RequestPath and
 * exposes them in an array-like fashion to the JavaScript runtime.
 *
 * @see helma.framework.core.RequestPath
 */
public class PathWrapper extends ScriptableObject {

    private static final long serialVersionUID = 514381479839863014L;

    RequestPath path;
    RhinoCore core;

    /**
     * Zero arg constructor for creating the PathWrapper prototype.
     */
    public PathWrapper (RhinoCore core) throws NoSuchMethodException {
        this.core = core;
        // create a dummy path object
        this.path = new RequestPath(core.app);

        // initialize properties and functions
        setParentScope(core.getScope());
        setPrototype(null);
        defineProperty("length", PathWrapper.class, DONTENUM | READONLY | PERMANENT); //$NON-NLS-1$
        defineFunctionProperties(new String[] {"href", "contains"},  //$NON-NLS-1$ //$NON-NLS-2$
                                 PathWrapper.class, DONTENUM | PERMANENT);
    }

    /**
     * Creates a new PathWrapper around a RequestPath.
     */
    PathWrapper(RequestPath path, RhinoCore core) {
        this.path = path;
        this.core = core;
    }


    /**
     * Returns a path object in the wrapped path by property name.
     */
    @Override
    public Object get(String name, Scriptable start) {
        Object obj = this.path.getByPrototypeName(name);

        if (obj != null) {
            return Context.toObject(obj, this.core.getScope());
        }

        return super.get(name, start);
    }

    /**
     * Returns a path object in the wrapped path by property name.
     */
    @Override
    public Object get(int idx, Scriptable start) {
        Object obj = this.path.get(idx);

        if (obj != null) {
            return Context.toObject(obj, this.core.getScope());
        }

        return null;
    }

    /**
     * Checks if an object with the given name is contained in the path.
     */
    @Override
    public boolean has(String name, Scriptable start) {
        return this.path.getByPrototypeName(name) != null;
    }

    /**
     * Checks if an object with the given index is contained in the path.
     */
    @Override
    public boolean has(int index, Scriptable start) {
        return index >= 0 && index < this.path.size();
    }

    /**
     * Returns a list of array indices 0..length-1.
     */
    @Override
    public Object[] getIds() {
        Object[] ids = new Object[this.path.size()];

        for (int i=0; i<ids.length; i++) {
            ids[i] = new Integer(i);
        }

        return ids;
    }

    /**
     * Getter for length property.
     */
    public long getLength() {
        return this.path.size();
    }

    /**
     * Returns the wrapped path rendered as URL path.
     */
    public String href(Object action) throws UnsupportedEncodingException {
        if (action != null && action != Undefined.instance) {
            return this.path.href(action.toString());
        }

        return this.path.href(null);
    }

    /**
     * Checks if the given object is contained in the request path
     *
     * @param obj the element to check
     * @return the index of the element, or -1 if it isn't contained
     */
    public int contains(Object obj) {
        if (obj instanceof Wrapper)
            obj = ((Wrapper) obj).unwrap();
        return this.path.indexOf(obj);
    }

    @Override
    public String getClassName() {
        return "[PathWrapper]"; //$NON-NLS-1$
    }

    @Override
    public String toString() {
        return "PathWrapper["+this.path.toString()+"]";  //$NON-NLS-1$//$NON-NLS-2$
    }

    /**
     * Return a primitive representation for this object.
     * FIXME: We always return a string representation.
     *
     * @param hint the type hint
     * @return the default value for the object
     */
    @Override
    public Object getDefaultValue(Class hint) {
        return toString();
    }
}
