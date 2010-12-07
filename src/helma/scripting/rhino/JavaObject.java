/*
 * Helma License Notice
 *
 * The contents of this file are subject to the Helma License
 * Version 2.0 (the "License"). You may not use this file except in
 * compliance with the License. A copy of the License is available at
 * http://adele.helma.org/download/helma/license.txt
 *
 * Copyright 1998-2003 Helma Software. All Rights Reserved.
 *
 * $RCSfile$
 * $Author$
 * $Revision$
 * $Date$
 */

package helma.scripting.rhino;

import helma.framework.core.*;
import helma.framework.ResponseTrans;
import helma.framework.repository.Resource;
import org.mozilla.javascript.*;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import java.io.UnsupportedEncodingException;
import java.io.IOException;

/**
 *
 */
public class JavaObject extends NativeJavaObject {

    private static final long serialVersionUID = 6348440950512377606L;

    RhinoCore core;
    String protoName;
    NativeJavaObject unscriptedJavaObj;

    static HashMap overload;

    static {
        overload = new HashMap();
        Method[] m = JavaObject.class.getMethods();
        for (int i=0; i<m.length; i++) {
            if ("href".equals(m[i].getName()) || //$NON-NLS-1$
                "renderSkin".equals(m[i].getName()) || //$NON-NLS-1$
                "renderSkinAsString".equals(m[i].getName()) || //$NON-NLS-1$
                "getResource".equals(m[i].getName()) || //$NON-NLS-1$
                "getResources".equals(m[i].getName())) { //$NON-NLS-1$
                overload.put(m[i].getName(), m[i]);
            }
        }
    }

    /**
     *  Creates a new JavaObject wrapper.
     */
    public JavaObject(Scriptable scope, Object obj,
            String protoName, Scriptable prototype, RhinoCore core) {
        this.parent = scope;
        this.javaObject = obj;
        this.protoName = protoName;
        this.core = core;
        staticType = obj.getClass();
        unscriptedJavaObj = new NativeJavaObject(scope, obj, staticType);
        setPrototype(prototype);
        initMembers();
    }

    /**
     *
     *
     * @param skinobj ...
     * @param paramobj ...
     *
     * @return ...
     */
    public boolean renderSkin(Object skinobj, Object paramobj)
            throws UnsupportedEncodingException, IOException {
        RhinoEngine engine = RhinoEngine.getRhinoEngine();
        Skin skin = engine.toSkin(skinobj, protoName);

        if (skin != null) {
            skin.render(engine.reval, javaObject, 
                    (paramobj == Undefined.instance) ? null : paramobj);
        }

        return true;
    }

    /**
     *
     *
     * @param skinobj ...
     * @param paramobj ...
     *
     * @return ...
     */
    public String renderSkinAsString(Object skinobj, Object paramobj)
            throws UnsupportedEncodingException, IOException {
        RhinoEngine engine = RhinoEngine.getRhinoEngine();
        Skin skin = engine.toSkin(skinobj, protoName);

        if (skin != null) {
            return skin.renderAsString(engine.reval, javaObject,
                    (paramobj == Undefined.instance) ? null : paramobj);
        }

        return ""; //$NON-NLS-1$
    }

    /**
     * Get the URL for this object with the application
     * @param action optional action name
     * @param params optional query parameters
     * @return the URL for the object
     * @throws UnsupportedEncodingException if the application's charset property
     *         is not a valid encoding name
     */
    public Object href(Object action, Object params)
            throws UnsupportedEncodingException, IOException {
        if (javaObject == null) {
            return null;
        }

        String actionName = null;
        Map queryParams = params instanceof Scriptable ?
                core.scriptableToProperties((Scriptable) params) : null;

        if (action != null) {
            if (action instanceof Wrapper) {
                actionName = ((Wrapper) action).unwrap().toString();
            } else if (!(action instanceof Undefined)) {
                actionName = action.toString();
            }
        }

        String basicHref = core.app.getNodeHref(javaObject, actionName, queryParams);
        return core.postProcessHref(javaObject, protoName, basicHref);
    }

    /**
     * Checks whether the given property is defined in this object.
     */
    public boolean has(String name, Scriptable start) {
        return overload.containsKey(name) || super.has(name, start);
    }

    /** 
     * Get a named property from this object.
     */
    public Object get(String name, Scriptable start) {
        Object value;

        // we really are not supposed to walk down the prototype chain in get(),
        // but we break the rule in order to be able to override java methods,
        // which are looked up by super.get() below
        Scriptable proto = getPrototype();
        while (proto != null) {
            value = proto.get(name, start);
            // Skip FunctionObject properties, which represent native wrapped
            // java host methods. The prototype chain is made of HopObjects, and
            // we can't invoked these on our wrapped java object.
            if (value != NOT_FOUND && !(value instanceof FunctionObject)) {
                return value;
            }
            proto = proto.getPrototype();
        }

        value = overload.get(name);
        if (value != null) {
            return new FunctionObject(name, (Method) value, this);
        }

        if ("_prototype".equals(name) || "__prototype__".equals(name)) { //$NON-NLS-1$ //$NON-NLS-2$
            return protoName;
        }

        if ("__proto__".equals(name)) { //$NON-NLS-1$
            return getPrototype();
        }

        if ("__javaObject__".equals(name)) { //$NON-NLS-1$
            return unscriptedJavaObj;
        }

        return super.get(name, start);
    }

    /**
     * Returns a prototype's resource of a given name. Walks up the prototype's
     * inheritance chain if the resource is not found
     *
     * @param resourceName the name of the resource, e.g. "type.properties",
     *                     "messages.properties", "script.js", etc.
     * @return the resource, if found, null otherwise
     */
    public Object getResource(String resourceName) {
        RhinoEngine engine = RhinoEngine.getRhinoEngine();
        Prototype prototype = engine.core.app.getPrototypeByName(protoName);
        while (prototype != null) {
            Resource[] resources = prototype.getResources();
            for (int i = resources.length - 1; i >= 0; i--) {
                Resource resource = resources[i];
                if (resource.exists() && resource.getShortName().equals(resourceName))
                    return Context.toObject(resource, core.global);
            }
            prototype =  prototype.getParentPrototype();
        }
        return null;
    }


    /**
     * Returns an array containing the prototype's resource with a given name.
     *
     * @param resourceName the name of the resource, e.g. "type.properties",
     *                     "messages.properties", "script.js", etc.
     * @return an array of resources with the given name
     */
    public Object getResources(String resourceName) {
        RhinoEngine engine = RhinoEngine.getRhinoEngine();
        Prototype prototype = engine.core.app.getPrototypeByName(protoName);
        ArrayList a = new ArrayList();
        while (prototype != null) {
            Resource[] resources = prototype.getResources();
            for (int i = resources.length - 1; i >= 0; i--) {
                Resource resource = resources[i];
                if (resource.exists() && resource.getShortName().equals(resourceName))
                    a.add(Context.toObject(resource, core.global));
            }
            prototype =  prototype.getParentPrototype();
        }
        return Context.getCurrentContext().newArray(core.global, a.toArray());
    }

}
