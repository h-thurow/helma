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

import helma.framework.core.*;
import helma.framework.repository.ResourceInterface;
import helma.objectmodel.*;
import helma.objectmodel.db.*;
import helma.objectmodel.db.Node;
import org.mozilla.javascript.*;

import java.lang.reflect.Method;
import java.util.*;
import java.io.UnsupportedEncodingException;
import java.io.IOException;

/**
 *
 */
public class HopObject extends ScriptableObject implements Wrapper, PropertyRecorderInterface {

    private static final long serialVersionUID = 1329862822101428427L;

    String className;
    final NodeProxy proxy;
    final RhinoCore core;

    // fields to implement PropertyRecorderInterface
    private boolean isRecording = false;
    private HashSet changedProperties;

    /**
     * Creates a new HopObject prototype.
     *
     * @param className the prototype name
     * @param core the RhinoCore
     */
    protected HopObject(String className, RhinoCore core) {
        this.className = className;
        this.core = core;
        this.proxy = null;
        setParentScope(core.global);
    }

    /**
     * Creates a new HopObject.
     *
     * @param className the className
     * @param core the RhinoCore
     * @param node the wrapped node
     * @param proto the object's prototype
     */
    protected HopObject(String className, RhinoCore core,
                        NodeInterface node, Scriptable proto) {
        this.className = className;
        this.core = core;
        this.proxy = new NodeProxy(node);
        if (proto != null)
            setPrototype(proto);
        setParentScope(core.global);
    }

    /**
     * Creates a new HopObject.
     *
     * @param className the className
     * @param core the RhinoCore
     * @param handle the handle for the wrapped node
     * @param proto the object's prototype
     */
    protected HopObject(String className, RhinoCore core,
                        NodeHandle handle, Scriptable proto) {
        this.className = className;
        this.core = core;
        this.proxy = new NodeProxy(handle);
        if (proto != null)
            setPrototype(proto);
        setParentScope(core.global);
    }

    /**
     * Initialize HopObject prototype for Rhino scope.
     *
     * @param core the RhinoCore
     * @return the HopObject prototype
     */
    public static HopObject init(RhinoCore core) {
        int attributes = DONTENUM | PERMANENT;

        // create prototype object
        HopObject proto = new HopObject("HopObject", core); //$NON-NLS-1$
        proto.setPrototype(getObjectPrototype(core.global));

        // install JavaScript methods and properties
        Method[] methods = HopObject.class.getDeclaredMethods();
        for (int i=0; i<methods.length; i++) {
            String methodName = methods[i].getName();

            if (methodName.startsWith("jsFunction_")) { //$NON-NLS-1$
                methodName = methodName.substring(11);
                FunctionObject func = new FunctionObject(methodName,
                                                         methods[i], proto);
                proto.defineProperty(methodName, func, attributes);

            } else if (methodName.startsWith("jsGet_")) { //$NON-NLS-1$
                methodName = methodName.substring(6);
                proto.defineProperty(methodName, null, methods[i],
                                         null, attributes);
            }
        }
        return proto;
    }

    /**
     * Get the class/prototype name for this HopObject
     *
     * @return The object's class or prototype name
     */
    @Override
    public String getClassName() {
        return this.className;
    }

    /**
     * Overwritten to not define constructor property as constant -
     * we need to have the constructor property resettable in Helma.
     * @param propertyName the property name
     * @param value the property value
     * @param attributes the property attributes
     */
    @Override
    public void defineProperty(String propertyName, Object value,
                               int attributes)
    {
        if ("constructor".equals(propertyName)) //$NON-NLS-1$
            put(propertyName, this, value);
        else
            super.defineProperty(propertyName, value, attributes);
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
        return this.proxy == null ? toString() : this.proxy.getNode().toString();
    }

    /**
     *  Return the NodeInterface wrapped by this HopObject.
     *
     * @return the wrapped NodeInterface instance
     */
    public NodeInterface getNode() {
        if (this.proxy != null) {
            return this.proxy.getNode();
        }
        return null;
    }

    /**
     * Returns the wrapped Node. Implements unwrap() in interface Wrapper.
     *
     */
    public Object unwrap() {
        if (this.proxy != null) {
            return this.proxy.getNode();
        }
        return this;
    }

    /**
     *
     *
     * @return ...
     */
    public Object jsGet_cache() {
        if (this.proxy != null) {
            NodeInterface cache = this.proxy.getNode().getCacheNode();
            if (cache != null) {
                return Context.toObject(cache, this.core.global);
            }
        }
        return null;
    }

    /**
     * Render a skin to the response buffer.
     *
     * @param skinobj The skin object or name
     * @param paramobj An optional parameter object
     *
     * @return ...
     */
    public boolean jsFunction_renderSkin(Object skinobj, Object paramobj)
            throws UnsupportedEncodingException, IOException {
        RhinoEngine engine = RhinoEngine.getRhinoEngine();
        Skin skin = engine.toSkin(skinobj, this.className);

        NodeInterface node = getNode();

        if (skin != null) {
            skin.render(engine.reval, node,
                    (paramobj == Undefined.instance) ? null : paramobj);
        }

        return true;
    }

    /**
     * Returns a prototype's resource of a given name. Walks up the prototype's
     * inheritance chain if the resource is not found
     *
     * @param resourceName the name of the resource, e.g. "type.properties",
     *                     "messages.properties", "script.js", etc.
     * @return the resource, if found, null otherwise
     */
    public Object jsFunction_getResource(String resourceName) {
        RhinoEngine engine = RhinoEngine.getRhinoEngine();
        Prototype prototype = engine.app.getPrototypeByName(this.className);
        while (prototype != null) {
            ResourceInterface[] resources = prototype.getResources();
            for (int i = resources.length - 1; i >= 0; i--) {
                ResourceInterface resource = resources[i];
                if (resource.exists() && resource.getShortName().equals(resourceName))
                    return Context.toObject(resource, this.core.global);
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
    public Object jsFunction_getResources(String resourceName) {
        RhinoEngine engine = RhinoEngine.getRhinoEngine();
        Prototype prototype = engine.core.app.getPrototypeByName(this.className);
        ArrayList a = new ArrayList();
        while (prototype != null) {
            ResourceInterface[] resources = prototype.getResources();
            for (int i = resources.length - 1; i >= 0; i--) {
                ResourceInterface resource = resources[i];
                if (resource.exists() && resource.getShortName().equals(resourceName))
                    a.add(Context.toObject(resource, this.core.global));
            }
            prototype =  prototype.getParentPrototype();
        }
        return Context.getCurrentContext().newArray(this.core.global, a.toArray());
    }

    /**
     *  Render a skin and return its output as string.
     *
     * @param skinobj The skin object or name
     * @param paramobj An optional parameter object
     *
     * @return ...
     */
    public String jsFunction_renderSkinAsString(Object skinobj, Object paramobj)
            throws UnsupportedEncodingException, IOException {
        RhinoEngine engine = RhinoEngine.getRhinoEngine();
        Skin skin = engine.toSkin(skinobj, this.className);
        NodeInterface node = getNode();

        if (skin != null) {
            return skin.renderAsString(engine.reval, node,
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
    public Object jsFunction_href(Object action, Object params)
            throws UnsupportedEncodingException, IOException {
        if (this.proxy == null) {
            return null;
        }

        String actionName = null;
        Map queryParams = params instanceof Scriptable ?
                this.core.scriptableToProperties((Scriptable) params) : null;
        NodeInterface node = getNode();

        if (action != null) {
            if (action instanceof Wrapper) {
                actionName = ((Wrapper) action).unwrap().toString();
            } else if (!(action instanceof Undefined)) {
                actionName = action.toString();
            }
        }

        String basicHref = this.core.app.getNodeHref(node, actionName, queryParams);

        return this.core.postProcessHref(node, this.className, basicHref);
    }

    /**
     * Get a childObject by name/id or index
     *
     * @param id The name/id or index, depending if the argument is a String or Number.
     *
     * @return ...
     */
    public Object jsFunction_get(Object id) {
        if (this.proxy == null || id == null || id == Undefined.instance) {
            return null;
        }

        Object child;
        NodeInterface node = getNode();

        if (id instanceof Number) {
            child = node.getSubnodeAt(((Number) id).intValue());
        } else {
            child = node.getChildElement(id.toString());
        }

        if (child != null) {
            return Context.toObject(child, this.core.global);
        }
        return null;
    }

    /**
     * Get a child object by ID
     *
     * @param id the child id.
     *
     * @return ...
     */
    public Object jsFunction_getById(Object id) {
        if (this.proxy == null || id == null || id == Undefined.instance) {
            return null;
        }

        NodeInterface node = getNode();
        String idString = (id instanceof Double) ?
                          Long.toString(((Double) id).longValue()) :
                          id.toString();
        Object n = node.getSubnode(idString);

        if (n == null) {
            return null;
        }
        return Context.toObject(n, this.core.global);
    }

    /**
     * Set a property on this HopObject
     *
     * @param id The name/id or index, depending if the argument is a String or Number.
     *
     * @return ...
     */
    public boolean jsFunction_set(Object id, Object value) {
        if (id == Undefined.instance || value == Undefined.instance) {
            throw new EvaluatorException(Messages.getString("HopObject.0")); //$NON-NLS-1$
        }
        if (this.proxy == null) {
            return false;
        }

        if (id instanceof Number) {
             if (!(value instanceof HopObject)) {
                throw new EvaluatorException(Messages.getString("HopObject.1")); //$NON-NLS-1$
            }

            NodeInterface node = getNode();

            int idx = (((Number) id).intValue());
            NodeInterface n = ((HopObject) value).getNode();

            node.addNode(n, idx);

        } else if (id != null) {
            put(id.toString(), this, value);
        }

        return true;
    }

    /**
     *
     *
     * @return ...
     */
    public int jsFunction_count() {
        if (this.proxy == null) {
            return 0;
        }
        NodeInterface node = getNode();
        return node.numberOfNodes();
    }

    /**
     *
     *
     * @return ...
     */
    public int jsFunction_size() {
        return jsFunction_count();
    }

    /**
     *  Prefetch child objects from (relational) database.
     */
    public void jsFunction_prefetchChildren(Object startArg, Object lengthArg) {
        // check if we were called with no arguments
        if (startArg == Undefined.instance && lengthArg == Undefined.instance) {
            prefetchChildren(0, 1000);
        } else {
            int start = (int) ScriptRuntime.toNumber(startArg);
            int length = (int) ScriptRuntime.toNumber(lengthArg);
            prefetchChildren(start, length);
        }
    }

    private void prefetchChildren(int start, int length) {
        if (this.proxy != null) {
            NodeInterface node = getNode();
            if (node instanceof Node) {
                Node n = (Node) node;
                if (n.getState() != NodeInterface.TRANSIENT && n.getState() != NodeInterface.NEW) {
                    n.prefetchChildren(start, length);
                }
            }
        }
    }

    /**
     *  Clear the node's cache node.
     */
    public void jsFunction_clearCache() {
        if (this.proxy != null) {
            NodeInterface node = getNode();
            node.clearCacheNode();
        }
    }

    /**
     * Return the full list of child objects in a JavaScript Array.
     * This is called by jsFunction_list() if called with no arguments.
     *
     * @return A JavaScript Array containing all child objects
     */
    private Scriptable list() {
        Node node = (Node) getNode();
        node.loadNodes();
        SubnodeList list = node.getSubnodeList();
        if (list == null) {
            return Context.getCurrentContext().newArray(this.core.global, 0);
        }
        Object[] array = list.toArray();
        for (int i = 0; i < array.length; i++) {
            array[i] = this.core.getNodeWrapper((NodeHandle) array[i]);
        }
        return Context.getCurrentContext().newArray(this.core.global, array);
    }

    /**
     *  Return a JS array of child objects with the given start and length.
     *
     * @return A JavaScript Array containing the specified child objects
     */
    public Scriptable jsFunction_list(Object startArg, Object lengthArg) {
        if (startArg == Undefined.instance && lengthArg == Undefined.instance) {
            return list();
        }

        int start = (int) ScriptRuntime.toNumber(startArg);
        int length = (int) ScriptRuntime.toNumber(lengthArg);

        if (start < 0 || length < 0) {
            throw new EvaluatorException(Messages.getString("HopObject.2")); //$NON-NLS-1$
        }

        Node node = (Node) getNode();
        prefetchChildren(start, length);
        SubnodeList list = node.getSubnodeList();
        length = list == null ? 0 : Math.min(list.size() - start, length);
        if (length <= 0) {
            return Context.getCurrentContext().newArray(this.core.global, 0);
        }
        Object[] array = new Object[length];

        for (int i = 0; i < length; i++) {
            Object obj = list.get(start + i);
            if (obj != null) {
                array[i] = Context.toObject(obj, this.core.global);
            }
        }

        return Context.getCurrentContext().newArray(this.core.global, array);
    }

    /**
     *
     *
     * @param child ...
     *
     * @return ...
     */
    public boolean jsFunction_add(Object child) {
        if (this.proxy == null || child == null) {
            return false;
        }

        NodeInterface node = getNode();

        if (child instanceof HopObject) {
            node.addNode(((HopObject) child).getNode());
            return true;
        } else if (child instanceof NodeInterface) {
            node.addNode((NodeInterface) child);
            return true;
        }

        return false;
    }

    /**
     *
     *
     * @param index ...
     * @param child ...
     *
     * @return ...
     */
    public boolean jsFunction_addAt(int index, Object child) {
        if (child == null) {
            return false;
        }

        NodeInterface node = getNode();

        if (child instanceof HopObject) {
            node.addNode(((HopObject) child).getNode(), index);

            return true;
        } else if (child instanceof NodeInterface) {
            node.addNode((NodeInterface) child, index);

            return true;
        }

        return false;
    }

    /**
     *  Remove this object from the database.
     */
    public boolean jsFunction_remove(Object arg) {
        // shield off usage of old deprecated version taking an argument
        if (arg != Undefined.instance) {
            System.err.println(Messages.getString("HopObject.3")); //$NON-NLS-1$
            System.err.println(Messages.getString("HopObject.4")); //$NON-NLS-1$
            System.err.println(Messages.getString("HopObject.5")); //$NON-NLS-1$
            System.err.println(Messages.getString("HopObject.6")); //$NON-NLS-1$
            System.err.println(Messages.getString("HopObject.7")); //$NON-NLS-1$
            System.err.println(Messages.getString("HopObject.8")); //$NON-NLS-1$
            System.err.println(Messages.getString("HopObject.9")); //$NON-NLS-1$
            System.err.println(Messages.getString("HopObject.10")); //$NON-NLS-1$
            System.err.println(Messages.getString("HopObject.11")); //$NON-NLS-1$
            throw new RuntimeException(Messages.getString("HopObject.12")); //$NON-NLS-1$
        }

        NodeInterface node = getNode();

        return node.remove();
    }

    /**
     *  Remove a child node from this node's collection without deleting
     *  it from the database.
     */
    public boolean jsFunction_removeChild(Object child) {

        NodeInterface node = getNode();

        if (child instanceof HopObject) {
            HopObject hobj = (HopObject) child;

            if (hobj.proxy != null) {
                node.removeNode(hobj.getNode());
                return true;
            }
        }

        return false;
    }

    /**
     * Makes the HopObject and all its reachable descendants persistent.
     *
     * @return the ID of the newly persisted HopObject or null if operation failed
     */
    public Object jsFunction_persist() {

        NodeInterface node = getNode();

        if (node instanceof Node) {
            ((Node) node).persist();
            return node.getID();
        }
        return null;
    }

    /**
     *  Invalidate the node itself or a subnode
     */
    public boolean jsFunction_invalidate(Object childId) {
        if (childId != null && this.proxy != null) {
            NodeInterface node = getNode();
            if (!(node instanceof Node)) {
                return true;
            }
            if (childId == Undefined.instance) {
                if (node.getState() == NodeInterface.INVALID) {
                    return true;
                }
                ((Node) node).invalidate();
            } else {
                ((Node) node).invalidateNode(childId.toString());
            }
        }

        return true;
    }

    /**
     * Check whether the wrapped Node is persistent.
     * This also returns true if the Node is being inserted in the database,
     * or it has been in database and is in the proces of being deleted.
     * @return true if the the wrapped Node has a valid database id.
     */
    public boolean jsFunction_isPersistent() {
        if (this.proxy == null) {
            return false;
        }
        NodeInterface node = getNode();
        int nodeState = node.getState();
        return nodeState != NodeInterface.TRANSIENT;
    }

    /**
     * Check whether the wrapped Node is transient.
     * This also returns false if the Node is being inserted in the database,
     * or it has been in database and is in the proces of being deleted.
     * @return true if the the wrapped Node is not stored in a database.
     */
    public boolean jsFunction_isTransient() {
        if (this.proxy == null) {
            return true;
        }
        NodeInterface node = getNode();
        int nodeState = node.getState();
        return nodeState == NodeInterface.TRANSIENT;
    }

    /**
     * Check if node is contained in the subnode collection.
     * Return its index position if it is, and -1 otherwise.
     */
    public int jsFunction_indexOf(Object obj) {

        if (this.proxy != null && obj instanceof HopObject) {
            NodeInterface node = getNode();
            return node.contains(((HopObject) obj).getNode());
        }
        return -1;
    }

    /**
     * Check if node is contained in the subnode collection.
     * Return its index position if it is, and -1 otherwise.
     * @deprecated use indexOf(Object) instead.
     */
    @Deprecated
    public int jsFunction_contains(Object obj) {
        return jsFunction_indexOf(obj);
    }

    /**
     * Set a property in this HopObject
     *
     * @param name property name
     * @param start
     * @param value ...
     */
    @Override
    public void put(String name, Scriptable start, Object value) {
        if (this.proxy == null) {
            // redirect the scripted constructor to __constructor__,
            // constructor is set to the native constructor method.
            if ("constructor".equals(name) && value instanceof NativeFunction) { //$NON-NLS-1$
                name = "__constructor__"; //$NON-NLS-1$
            }
            propertyChanged(name, value, null, null);
            // register property for PropertyRecorder interface
           super.put(name, start, value);
        } else if (super.has(name, start)) {
            // if property is defined as ScriptableObject slot
            // (e.g. via __defineGetter__/__defineSetter__)
            // use ScriptableObject.put to set it
            super.put(name, start, value);
        } else {
            NodeInterface node = getNode();

            if ("subnodeRelation".equals(name)) { //$NON-NLS-1$
                node.setSubnodeRelation(value == null ? null : value.toString());
            }

            if (value instanceof Wrapper) {
                value = ((Wrapper) value).unwrap();
            }

            if ((value == null) || (value == Undefined.instance)) {
                node.unset(name);
            } else if (value instanceof Scriptable) {
                Scriptable s = (Scriptable) value;
                String className = s.getClassName();
                if ("Date".equals(className)) { //$NON-NLS-1$
                    node.setDate(name, new Date((long) ScriptRuntime.toNumber(s)));
                } else if ("String".equals(className)) { //$NON-NLS-1$
                    node.setString(name, ScriptRuntime.toString(s));
                } else if ("Number".equals(className)) { //$NON-NLS-1$
                    node.setFloat(name, ScriptRuntime.toNumber(s));
                } else if ("Boolean".equals(className)) { //$NON-NLS-1$
                    node.setBoolean(name, ScriptRuntime.toBoolean(s));
                } else {
                    node.setJavaObject(name, s);
                }
            } else if (value instanceof String) {
                node.setString(name, value.toString());
            } else if (value instanceof Boolean) {
                node.setBoolean(name, ((Boolean) value).booleanValue());
            } else if (value instanceof Number) {
                node.setFloat(name, ((Number) value).doubleValue());
            } else if (value instanceof Date) {
                node.setDate(name, (Date) value);
            } else if (value instanceof NodeInterface) {
                node.setNode(name, (NodeInterface) value);
            } else {
                node.setJavaObject(name, value);
            }
        }
    }

    protected void propertyChanged(Object id, Object value, Object get,
            Object set) {
        if (isRecording) {
            changedProperties.add(id.toString());
        }
        if (value != null)
            correctScope(value);
        if (get != null)
            correctScope(get);
        if (set != null)
            correctScope(set);
    }

    protected void correctScope(Object object) {
        if (object instanceof Function) {
            // reset function's parent scope, needed because of the way we compile
            // prototype code, using the prototype objects as scope
            Scriptable scriptable = (Scriptable) object;
            while (scriptable != null) {
                Scriptable scope = scriptable.getParentScope();
                // do not just support switching of this HopObject prototype to
                // global, but all prototypes, e.g. when compiling into
                // another prototype from one prototype folder (bad practise!)
                if (scope instanceof HopObject) {
                    scriptable.setParentScope(core.global);
                    break;
                }
                scriptable = scope;
            }
        }
    }

    /**
     * Check if a property is set in this HopObject
     *
     * @param name the property name
     * @param start the object in which the lookup began
     * @return true if the property was found
     */
    @Override
    public boolean has(String name, Scriptable start) {
        if (this.proxy != null) {
            NodeInterface node = getNode();
            if (node.get(name) != null) {
            	return true;
            }
        }
        return super.has(name, start);
    }

    /**
     *
     *
     * @param name ...
     */
    @Override
    public void delete(String name) {
        if ((this.proxy != null)) {
            NodeInterface node = getNode();
            node.unset(name);
        }
        super.delete(name);
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
    	Object obj = super.get(name, start);
    	if (obj == Scriptable.NOT_FOUND && this.proxy != null) {
            obj = getFromNode(name);
        }
        return obj;
    }

    /**
     *  Retrieve a property only from the node itself, not the underlying prototype object.
     *  This is called directly when we call get(x) on a JS HopObject, since we don't want
     *  to return the prototype functions in that case.
     */
    private Object getFromNode(String name) {
        if (this.proxy != null && name != null && name.length() > 0) {

            NodeInterface node = getNode();

            // Property names starting with an underscore is interpreted
            // as internal properties
            if (name.charAt(0) == '_') {
                Object value = getInternalProperty(node, name);
                if (value != NOT_FOUND)
                    return value;
            }

            if ("subnodeRelation".equals(name)) { //$NON-NLS-1$
                return node.getSubnodeRelation();
            }

            PropertyInterface p = node.get(name);

            if (p != null) {
                switch (p.getType()) {
                    case PropertyInterface.STRING:
                    case PropertyInterface.INTEGER:
                    case PropertyInterface.FLOAT:
                    case PropertyInterface.BOOLEAN:
                        return p.getValue();
                }

                Context cx = Context.getCurrentContext();

                if (p.getType() == PropertyInterface.DATE) {
                    Date d = p.getDateValue();

                    if (d == null) {
                        return null;
                    }
                    Object[] args = { new Long(d.getTime()) };
                    try {
                        return cx.newObject(this.core.global, "Date", args); //$NON-NLS-1$
                    } catch (JavaScriptException nafx) {
                        return null;
                    }
                }

                if (p.getType() == PropertyInterface.NODE) {
                    NodeInterface n = p.getNodeValue();

                    if (n == null) {
                        return null;
                    }
                    return Context.toObject(n, this.core.global);
                }

                if (p.getType() == PropertyInterface.JAVAOBJECT) {
                    Object obj = p.getJavaObjectValue();

                    if (obj == null) {
                        return null;
                    }
                    return Context.toObject(obj, this.core.global);
                }
            }

            DbMapping dbmap = node.getDbMapping();
            if (dbmap != null && dbmap.propertyToRelation(name) != null) {
                return null;
            }
        }

        return NOT_FOUND;
    }

    private Object getInternalProperty(NodeInterface node, String name) {
        if ("__id__".equals(name) || "_id".equals(name)) { //$NON-NLS-1$ //$NON-NLS-2$
            return node.getID();
        }

        if ("__proto__".equals(name)) { //$NON-NLS-1$
            return getPrototype(); // prototype object
        }

        if ("__prototype__".equals(name) || "_prototype".equals(name)) {  //$NON-NLS-1$//$NON-NLS-2$
            return node.getPrototype(); // prototype name
        }

        if ("__parent__".equals(name) || "_parent".equals(name)) {  //$NON-NLS-1$//$NON-NLS-2$
            return this.core.getNodeWrapper(node.getParent());
        }

        // some more internal properties
        if ("__name__".equals(name)) { //$NON-NLS-1$
            return node.getName();
        }

        if ("__path__".equals(name)) { //$NON-NLS-1$
            return node.getPath();
        }

        if ("__hash__".equals(name)) { //$NON-NLS-1$
            return Integer.toString(node.hashCode());
        }

        if ("__node__".equals(name)) { //$NON-NLS-1$
            return new NativeJavaObject(this.core.global, node, null);
        }

        if ("__created__".equalsIgnoreCase(name)) { //$NON-NLS-1$
            return new Date(node.created());
        }

        if ("__lastmodified__".equalsIgnoreCase(name)) { //$NON-NLS-1$
            return new Date(node.lastModified());
        }

        return NOT_FOUND;
    }

    /**
     * Return all property names of this object. This method is used by debugger.
     * @return array containing the names of all properties defined in this object
     */
    @Override
    public Object[] getAllIds() {
        if (this.proxy == null) {
            return super.getAllIds();
        }
        return getIds();
    }

    /**
     * Return all "ordinary" property ids of this object. This "hides" the prototype methods.
     * @return array containing the names of this object's data properties
     */
    @Override
    public Object[] getIds() {
        if (this.proxy == null) {
            // HopObject prototypes always return an empty array in order not to
            // pollute actual HopObjects properties. Call getAllIds() to get
            // a list of properties from a HopObject prototype.
            return new Object[0];
        }

        NodeInterface node = getNode();

        Enumeration en = node.properties();
        ArrayList list = new ArrayList();

        while (en.hasMoreElements())
            list.add(en.nextElement());

        return list.toArray();
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
        if (this.proxy != null) {
            NodeInterface node = getNode();

            return (0 <= idx && idx < node.numberOfNodes());
        }

        return false;
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
        if (this.proxy != null) {
            NodeInterface node = getNode();

            NodeInterface n = node.getSubnodeAt(idx);

            if (n != null) {
                return Context.toObject(n, this.core.global);
            }
        }

        return NOT_FOUND;
    }

    /**
     * Custom <tt>==</tt> operator.
     * Must return {@link org.mozilla.javascript.Scriptable#NOT_FOUND} if this object does not
     * have custom equality operator for the given value,
     * <tt>Boolean.TRUE</tt> if this object is equivalent to <tt>value</tt>,
     * <tt>Boolean.FALSE</tt> if this object is not equivalent to
     * <tt>value</tt>.
     */
    @Override
    protected Object equivalentValues(Object value) {
        if (value == this) {
            return Boolean.TRUE;
        }
        if (value instanceof HopObject && this.proxy != null
                && this.proxy.equivalentValues(((HopObject) value).proxy)) {
            return Boolean.TRUE;
        }
        return Scriptable.NOT_FOUND;
    }

    /**
     * Return a string representation of this HopObject.
     * @return a string representing this HopObject
     */
    @Override
    public String toString() {
        if (this.proxy == null) {
            return "[HopObject prototype " + this.className + "]"; //$NON-NLS-1$ //$NON-NLS-2$
        }
        return "[HopObject " + this.proxy.getNode().getName() + "]";  //$NON-NLS-1$//$NON-NLS-2$
    }

    /**
     * Tell this PropertyRecorderInterface to start recording changes to properties
     */
    public void startRecording() {
        this.changedProperties = new HashSet();
        this.isRecording = true;
    }

    /**
     * Tell this PropertyRecorderInterface to stop recording changes to properties
     */
    public void stopRecording() {
        this.isRecording = false;
    }

    /**
     * Returns a set containing the names of properties changed since
     * the last time startRecording() was called.
     *
     * @return a Set containing the names of changed properties
     */
    public Set getChangeSet() {
        return this.changedProperties;
    }

    /**
     * Clear the set of changed properties.
     */
    public void clearChangeSet() {
        this.changedProperties = null;
    }

    class NodeProxy {
        private NodeInterface node;
        private NodeHandle handle;

        NodeProxy(NodeInterface node) {
            this.node = node;
            if (node instanceof Node) {
                this.handle = ((Node) node).getHandle();
            }
        }

        NodeProxy(NodeHandle handle) {
            this.handle = handle;
        }

        synchronized NodeInterface getNode() {
            if (this.node == null || this.node.getState() == NodeInterface.INVALID) {
                if (this.handle != null) {
                    this.node = this.handle.getNode(HopObject.this.core.app.getWrappedNodeManager());
                    if (this.node != null) {
                        String protoname = this.node.getPrototype();
                        // the actual prototype name may vary from the node handle's prototype name
                        if (HopObject.this.className == null || !HopObject.this.className.equals(protoname)) {
                            Scriptable proto = HopObject.this.core.getValidPrototype(protoname);
                            if (proto == null) {
                                protoname = "HopObject"; //$NON-NLS-1$
                                proto = HopObject.this.core.getValidPrototype("HopObject"); //$NON-NLS-1$
                            }
                            HopObject.this.className = protoname;
                            setPrototype(proto);
                        }
                    }
                }
                if (this.node == null || this.node.getState() == NodeInterface.INVALID) {
                    // We probably have a deleted node.
                    // Replace with empty transient node to avoid throwing an exception.
                    this.node = new TransientNode();
                }
            }
            return this.node;
        }

        public boolean equivalentValues(NodeProxy other) {
            if (this.handle == null) {
                return other.node == this.node;
            }
            return this.handle == other.handle || this.handle.equals(other.handle);
        }
    }
}
