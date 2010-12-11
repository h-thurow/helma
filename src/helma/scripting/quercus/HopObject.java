/*
 * Helma License Notice
 *
 * The contents of this file are subject to the Helma License
 * Version 2.0 (the "License"). You may not use this file except in
 * compliance with the License. A copy of the License is available at
 * http://adele.helma.org/download/helma/license.txt
 *
 * Copyright 2010 dowee it solutions GmbH. All rights reserved.
 */

package helma.scripting.quercus;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;

import helma.framework.RedirectException;
import helma.framework.ResponseTrans;
import helma.framework.core.Prototype;
import helma.framework.core.Skin;
import helma.framework.repository.ResourceInterface;
import helma.objectmodel.NodeInterface;
import helma.objectmodel.NodeStateInterface;
import helma.objectmodel.db.DbKey;
import helma.objectmodel.db.DbMapping;
import helma.objectmodel.db.Node;
import helma.scripting.ScriptingException;

import com.caucho.quercus.annotation.Construct;
import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.NullValue;
import com.caucho.quercus.env.NumberValue;
import com.caucho.quercus.env.ObjectExtJavaValue;
import com.caucho.quercus.env.StringValue;
import com.caucho.quercus.env.Value;
import com.caucho.quercus.env.Var;
import com.caucho.quercus.program.AbstractFunction;

/**
 * The HopObject is used to wrap NodeInterface objects for use in the Quercus
 * PHP interpreter.<br/>
 * Quercus does not automatically make use of the generic getter (__get) and
 * setter (__set), if defined in the PHP context (extending a Java object). This
 * functionality is also provided by this class. It first checks, if a generic
 * getter or setter is defined in the PHP context, and if so does a callback to
 * PHP. If no generic getter or setter is defined in the PHP context, the
 * HopObject's generic getter or setter method is called.
 */
public class HopObject extends ObjectExtJavaValue {

    private static final long   serialVersionUID = -5555069166328227883L;

    /**
     * The underlying NodeInterface that is wrapped
     */
    private final NodeInterface         _node;

    /**
     * The quercus engine we belong to
     */
    private final QuercusEngine _engine;

    /**
     * Constructor for situations where there is no direct reference available
     * to the quercus engine
     * 
     * @param node
     *            The NodeInterface to wrap
     */
    private HopObject(final NodeInterface node) {
        super(
                QuercusEngine.ENGINE.get().getEnvironment().getClass(
                        "HopObject"), new Object(), QuercusEngine.ENGINE.get() //$NON-NLS-1$
                        .getEnvironment().getClass("HopObject") //$NON-NLS-1$
                        .getJavaClassDef());
        this._node = node;
        this._engine = QuercusEngine.ENGINE.get();
    }

    /**
     * Default constructor
     * 
     * @param node
     *            The NodeInterface to wrap
     * @param engine
     *            The quercus engine
     */
    protected HopObject(final NodeInterface node, final QuercusEngine engine) {
        super(engine.getEnvironment()
                .getClass(
                        node.getPrototype() != null ? node.getPrototype()
                                : "HopObject"), new HopObject(node), engine //$NON-NLS-1$
                .getEnvironment().getClass(
                        node.getPrototype() != null ? node.getPrototype()
                                : "HopObject").getJavaClassDef()); //$NON-NLS-1$
        this._node = node;
        this._engine = engine;
    }

    /**
     * Default constructor for the PHP context.<br/>
     * Creates a HopObject that wraps a Node
     * 
     * @param prototype The prototype for the new object
     */
    @Construct
    public HopObject(final StringValue prototype) {
        this(new Node(null, prototype.toJavaString(), QuercusEngine.ENGINE
                .get().getApplication().getWrappedNodeManager()));

    }

    /**
     * Returns a property's value by name.<br/>
     * This method implements the magic properties known from the JS scripting
     * engine. All other properties are directly taken from the underlying
     * NodeInterface.
     * 
     * @param name
     *            The name of the property to return
     * @return The value of the property
     */
    public Object __get(final String name) {
        // check what we are looking for
        if (name.startsWith("_")) { //$NON-NLS-1$
            // we are looking for an internal property

            // TODO: make naming more consistent
            // will break backwards compatibility to HopObject however
            if (name.equals("_prototype") || name.equals("__prototype__")) {  //$NON-NLS-1$//$NON-NLS-2$
                return this._node.getPrototype();
            } else if (name.equals("_name") || name.equals("__name__")) { //$NON-NLS-1$ //$NON-NLS-2$
                return this._node.getName();
            } else if (name.equals("_parent") || name.equals("__parent__")) {  //$NON-NLS-1$//$NON-NLS-2$
                return new HopObject(this._node.getParent(), this._engine);
            } else if (name.equals("cache")) { //$NON-NLS-1$
                return new HopObject(this._node.getCacheNode(), this._engine);
            } else if (name.equals("_id") || name.equals("__id__")) {  //$NON-NLS-1$//$NON-NLS-2$
                return this._node.getID();
            } else if (name.equals("__proto__")) { //$NON-NLS-1$
                return this._engine.getApplication().getPrototypeByName(
                        this._node.getPrototype());
            } else if (name.equals("__hash__") && this._node instanceof Node) { //$NON-NLS-1$
                return Integer.valueOf(((Node) this._node).hashCode());
            } else if (name.equals("__node__")) { //$NON-NLS-1$
                return this._node;
            } else if (name.equalsIgnoreCase("__created__")) { //$NON-NLS-1$
                return Long.valueOf(this._node.created());
            } else if (name.equalsIgnoreCase("__lastmodified__")) { //$NON-NLS-1$
                return Long.valueOf(this._node.lastModified());
            }
        } else if (name.equals("subnodeRelation")) { //$NON-NLS-1$
            // we are looking for a special property

            // TODO: make naming more consistent
            // will break backwards compatibility to HopObject however
            return this._node.getSubnodeRelation();
        }

        // we are looking for a normal property
        return this._node.get(name);
    }

    /**
     * Sets a property given by name to the given value
     * 
     * @param name
     *            The name of the property to set
     * @param value
     *            The value to set for the given property
     */
    public void __set(final String name, final Object value) {
        // check which property or which value to set, as we might need to user
        // special methods depending on the
        // property or the value

        // TODO: make naming more consistent
        // will break backwards compatibility to HopObject however
        if (name.equals("subnodeRelation")) { //$NON-NLS-1$
            this._node.setSubnodeRelation(value.toString());
        } else if (value == null) {
            this._node.unset(name);
        } else if (name.equals("_prototype")) { //$NON-NLS-1$
            this._node.setPrototype(value.toString());
        } else if (name.equals("_name")) { //$NON-NLS-1$
            this._node.setName(value.toString());
        } else if (value instanceof Boolean) {
            this._node.setBoolean(name, ((Boolean) value).booleanValue());
        } else if (value instanceof Date) {
            this._node.setDate(name, (Date) value);
        } else if (value instanceof Float) {
            this._node.setFloat(name, ((Float) value).floatValue());
        } else if (value instanceof Boolean) {
            this._node.setInteger(name, ((Integer) value).intValue());
        } else if (value instanceof HopObject) {
            this._node.setNode(name, ((HopObject) value).getNode());
        } else if (value instanceof String) {
            this._node.setString(name, (String) value);
        } else {
            this._node.setJavaObject(name, value);
        }
    }

    /**
     * No idea what the orignal overridden method does, but it conflicts with
     * add(HopObject), so we need to either delegate to add(HopObject) or to
     * super.add(Value).
     * 
     * @see com.caucho.quercus.env.Value#add(com.caucho.quercus.env.Value)
     */
    @Override
    public Value add(final Value value) {
        if (value.toJavaObject() instanceof HopObject) {
            add((HopObject) value.toJavaObject());
            return NullValue.create();
        }

        return super.add(value);
    }

    /**
     * Adds the given node to the end of this node's child collection.
     * 
     * @param node
     *            The node to add
     * @return True, if the node was added (although one can never be sure if
     *         the operation really was successful internally), false if this
     *         node's child collection already contained the node to add
     */
    public boolean add(final HopObject node) {
        if (this._node.contains(node.getNode()) >= 0) {
            return false;
        }

        this._node.addNode(node.getNode());
        return true;
    }

    /**
     * Adds or moves the given node to the given index position.
     * 
     * @param index
     *            The index position to add at or to move the geiven node to
     * @param node
     *            The node to add or or to move
     * @return Always true, one can not be sure if the operation was really
     *         successful internally
     */
    public boolean addAt(final int index, final HopObject node) {
        this._node.addNode(node.getNode(), index);
        return true;
    }

    /*
     * (non-Javadoc)
     * @see
     * com.caucho.quercus.env.ObjectExtValue#callMethod(com.caucho.quercus.env
     * .Env, int, char[], int, com.caucho.quercus.env.Value[])
     */
    @Override
    public Value callMethod(final Env env, final int hash, final char[] name,
            final int nameLen, final Value[] args) {
        // TODO: implement the generic callee (__call)
        return super.callMethod(env, hash, name, nameLen, args);
    }

    /**
     * Clears the cache node
     */
    public void clearCache() {
        this._node.clearCacheNode();
    }

    /**
     * Checks if the given node is in this node's child collection
     * 
     * @param node
     *            The node to look for
     * @return True, if the given node is in this node's child collection
     */
    public boolean contains(final HopObject node) {
        return this._node.contains(node.getNode()) >= 0;
    }

    /**
     * Returns the number of nodes in this node's child collection
     * 
     * @return The number of subnodes
     * @deprecated use size() instead
     * @see size()
     */
    @Deprecated
    public int count() {
        return this._node.numberOfNodes();
    }

    /**
     * Returns a child node at the given index in this node's child collection
     * 
     * @param index
     *            The index position in this node's child collection
     * @return The child node or null, if the index was out of range
     */
    public HopObject get(final int index) {
        return new HopObject(this._node.getSubnodeAt(index), this._engine);
    }

    /**
     * Returns a child node by name<br/>
     * Mapped object properties are considered child nodes too.
     * 
     * @param name
     *            The name of the child node
     * @return The child node or null, if no child node could be found by the
     *         given name
     * @see type.properties
     */
    public Object get(final String name) {
        final Object node = this._node.getChildElement(name);

        if (node instanceof NodeInterface) {
            return new HopObject((NodeInterface) node, this._engine);
        }

        return node;
    }

    /**
     * No idea what the orignal overridden method does, but it conflicts with
     * get(int) and get(name), so we need to either delegate to get(int),
     * get(name) or to super.get(Value).
     * 
     * @see com.caucho.quercus.env.Value#get(com.caucho.quercus.env.Value)
     */
    @Override
    public Value get(Value key) {
        // check if key is a variable
        if (key instanceof Var) {
            // key is a variable, get the value
            key = ((Var) key).toValue();
        }

        // check type of key
        if (key instanceof NumberValue) {
            // key is a number, we want to call get(int)
            return get(((NumberValue) key).toInt());
        } else if (key instanceof StringValue) {
            // key is a string, we want to call get(String)
            final Object value = get(((StringValue) key).toJavaString());
            if (value instanceof NodeInterface) {
                // don't forget to wrap the value if it is an NodeInterface
                return new HopObject((NodeInterface) value);
            }

            return this._engine.getEnvironment().wrapJava(value);
        }

        return super.get(key);
    }

    /**
     * Returns a child node by id
     * 
     * @param id
     *            The id of the child node
     * @return The child node or null, if no child node could be found by the
     *         given id
     */
    public HopObject getById(final String id) {
        return new HopObject(this._node.getSubnode(id), this._engine);
    }

    /**
     * Returns a node by id and prototype.<br/>
     * The node specified by id and prototype is not needed to be a child of of
     * this node, it can be any node.
     * 
     * @param prototype
     *            The prototype of the node to get
     * @param id
     *            The id of the node to get
     * @return The node as specified by prototype and id or null, if the node
     *         was not found
     * @throws ScriptingException 
     */
    public HopObject getById(final String prototype, final String id)
            throws ScriptingException {
        final DbMapping dbMapping = this._engine.getApplication().getDbMapping(
                prototype);
        if (dbMapping == null) {
            return null;
        }

        final DbKey dbKey = new DbKey(dbMapping, id);
        try {
            final NodeInterface node = this._engine.getApplication().getNodeManager()
                    .getNode(dbKey);
            if (node != null) {
                return new HopObject(node, this._engine);
            }
        } catch (final Exception e) {
            throw new ScriptingException(Messages.getString("HopObject.0"), e); //$NON-NLS-1$
        }

        return null;
    }

    /*
     * (non-Javadoc)
     * @see
     * com.caucho.quercus.env.ObjectExtValue#getFieldArg(com.caucho.quercus.
     * env.Env, com.caucho.quercus.env.StringValue)
     */
    @Override
    public Value getFieldArg(final Env env, final StringValue name) {
        return getFieldExt(env, name);
    }

    /**
     * Implements the generic getter (__get)
     * 
     * @see com.caucho.quercus.env.ObjectExtJavaValue#getFieldExt(com.caucho.quercus.env.Env,
     *      com.caucho.quercus.env.StringValue)
     */
    @Override
    protected Value getFieldExt(final Env environment, final StringValue name) {
        final AbstractFunction function = findFunction("__get"); //$NON-NLS-1$
        if (function != null) {
            return callMethod(environment, StringValue.create("__get") //$NON-NLS-1$
                    .toStringValue(), new Value[] {name});
        }

        final Object value = __get(name.toJavaString());
        if (value != null) {
            return environment.wrapJava(value);
        }

        return super.getFieldExt(environment, name);
    }

    /**
     * Returns the underlying NodeInterface
     * 
     * @return The underlying NodeInterface
     */
    protected NodeInterface getNode() {
        return this._node;
    }

    /**
     * @param name
     * @return
     * @throws ScriptingException
     * 
     * TODO: implement
     */
    public HopObject getOrderedView(
            final String name)
            throws ScriptingException {
        // TODO: implement
        throw new ScriptingException(Messages.getString("HopObject.1"), null); //$NON-NLS-1$
    }

    /**
     * Returns a resource by the given name
     * 
     * @param name
     *            The resource to get
     * @return The resource by the given name or null, if no resource could be
     *         found by the given name
     */
    public ResourceInterface getResource(final String name) {
        Prototype prototype = this._engine.getApplication().getPrototypeByName(
                this._node.getPrototype());
        while (prototype != null) {
            final ResourceInterface[] resources = prototype.getResources();
            for (int i = resources.length - 1; i >= 0; i--) {
                final ResourceInterface resource = resources[i];
                if (resource.exists() && resource.getShortName().equals(name)) {
                    return resource;
                }
            }

            prototype = prototype.getParentPrototype();
        }

        return null;
    }

    /**
     * Returns all resources of this node by the given name
     * 
     * @param name
     *            The name of the resources to get
     * @return All resources by the given name as array or an empty arry, if no
     *         resources could be found by the given name
     */
    public ResourceInterface[] getResources(final String name) {
        Prototype prototype = this._engine.getApplication().getPrototypeByName(
                this._node.getPrototype());
        final ArrayList<ResourceInterface> foundResources = new ArrayList<ResourceInterface>();
        while (prototype != null) {
            final ResourceInterface[] resources = prototype.getResources();
            for (int i = resources.length - 1; i >= 0; i--) {
                final ResourceInterface resource = resources[i];
                if (resource.exists() && resource.getShortName().equals(name)) {
                    foundResources.add(resource);
                }
            }

            prototype = prototype.getParentPrototype();
        }

        return foundResources.toArray(new ResourceInterface[0]);
    }

    /**
     * Returns the appropriate skin for the given name.<br/>
     * The returned skin might be a subskin, a cached skin or a newly loaded
     * skin.
     * 
     * @param name
     *            The name of the skin to get
     * @return A skin with the given name or null, if no skin by the given name
     *         could be found
     * @throws ScriptingException
     */
    private Skin getSkin(final String name) throws ScriptingException {
        Skin skin;
        final ResponseTrans response = this._engine.getRequestEvaluator()
                .getResponse();
        if (name.startsWith("#")) { //$NON-NLS-1$
            // evaluate relative subskin name against currently rendering skin
            skin = response.getActiveSkin();
            return skin == null ? null : skin.getSubskin(name.substring(1));
        }

        final Integer hashCode = Integer.valueOf(this._node.getPrototype()
                .hashCode()
                + name.hashCode());
        skin = response.getCachedSkin(hashCode);

        if (skin == null) {
            // retrieve res.skinpath, an array of objects that tell us where to
            // look for skins
            // (strings for directory names and INodes for internal, db-stored
            // skinsets)
            final Object[] skinpath = response.getSkinpath();
            try {
                skin = this._engine.getApplication().getSkin(this._node.getPrototype(),
                        name, skinpath);
            } catch (final IOException e) {
                throw new ScriptingException(
                        Messages.getString("HopObject.2"), //$NON-NLS-1$
                        e);
            }
            response.cacheSkin(hashCode, skin);
        }
        return skin;
    }

    /**
     * Implements the generic getter (__get)
     * 
     * @see com.caucho.quercus.env.ObjectExtValue#getThisFieldArg(com.caucho.quercus.env.Env,
     *      com.caucho.quercus.env.StringValue)
     */
    @Override
    public Value getThisFieldArg(final Env env, final StringValue name) {
        return getFieldExt(env, name);
    }

    /**
     * Returns the url to this node
     * 
     * @param action
     *            The action to append to the url
     * @return The url to this node, optionally appended by the provided action
     * @throws ScriptingException 
     */
    public String href(final String action) throws ScriptingException {
        try {
            return this._engine.getApplication().getNodeHref(this._node, action, null);
        } catch (final UnsupportedEncodingException e) {
            throw new ScriptingException(e.getMessage(), e);
        }
    }

    /**
     * Returns the index position of the given child node in this node's child
     * collection
     * 
     * @param node
     *            The child node to determine the index position for
     * @return The index position of the given child node, or -1 if the child
     *         node is not contained in this node's child collection
     */
    public int indexOf(final HopObject node) {
        return this._node.contains(node.getNode());
    }

    /**
     * Invalidates this node
     */
    public void invalidate() {
        if (this._node instanceof Node) {
            if (this._node.getState() == NodeStateInterface.INVALID) {
                return;
            }

            ((Node) this._node).invalidate();
        }
    }

    /**
     * Invalidates a child node given by id
     * 
     * @param id
     *            The child node to invalidate
     * @return True, if the child node was found and thus could be invalidated
     * @deprecated use get(id).invalidate() instead
     */
    @Deprecated
    public boolean invalidate(final String id) {
        if (id != null && this._node instanceof Node) {
            ((Node) this._node).invalidateNode(id);
            return true;
        }

        return false;
    }

    /**
     * Checks if this node is persisted in some way
     * 
     * @return True, if this node is persistent
     */
    public boolean isPersistent() {
        return this._node instanceof Node && this._node.getState() != NodeStateInterface.TRANSIENT;
    }

    /**
     * Checks if this node is transient
     * 
     * @return True, if this node is transient
     */
    public boolean isTransient() {
        return !(this._node instanceof Node) || this._node.getState() == NodeStateInterface.TRANSIENT;
    }

    /**
     * Returns this node's child collection as array
     * 
     * @return This node's child collection as array
     * @deprecated use a loop, size and get instead to build it yourself in PHP
     */
    @SuppressWarnings("unchecked")
    @Deprecated
    public HopObject[] list() {
        final ArrayList<HopObject> children = new ArrayList<HopObject>();
        final Enumeration subnodes = this._node.getSubnodes();
        while (subnodes.hasMoreElements()) {
            children
                    .add(new HopObject((NodeInterface) subnodes.nextElement(), this._engine));
        }

        return children.toArray(new HopObject[0]);
    }

    /**
     * Persist the node
     * 
     * @return The id after persisting the node or null, if the node was not
     *         persisted
     */
    public String persist() {
        if (this._node instanceof Node) {
            ((Node) this._node).persist();
            return this._node.getID();
        }

        return null;
    }

    /**
     * Prefetches some child nodes to the node-manager's cache. Otherwise they
     * would be fetched dynamically as soon as they are accessed. Prefetching
     * has the advantage that a bigger number of child nodes can be fetched with
     * one single SQL statement, where as dynamically fetching child nodes would
     * lead to one SQL statement per child node being fetched.
     * 
     * @param startIndex
     *            The index position in this node's child collection to start
     *            prefetching from
     * @param length
     *            The number of child nodes to prefetch
     * @throws ScriptingException 
     */
    public void prefetchChildren(final int startIndex, final int length)
            throws ScriptingException {
        if (this._node instanceof Node) {
            try {
                ((Node) this._node).prefetchChildren(startIndex, length);
            } catch (final Exception e) {
                throw new ScriptingException(Messages.getString("HopObject.3"), e); //$NON-NLS-1$
            }
        }
    }

    /**
     * Implements the generic setter (__set)
     * 
     * @see com.caucho.quercus.env.ObjectExtJavaValue#putFieldExt(com.caucho.quercus.env.Env,
     *      com.caucho.quercus.env.StringValue, com.caucho.quercus.env.Value)
     */
    @Override
    protected Value putFieldExt(final Env environment, final StringValue name,
            final Value value) {
        final Value oldValue = getFieldExt(environment, name);

        final AbstractFunction function = findFunction("__set"); //$NON-NLS-1$
        if (function != null) {
            callMethod(environment,
                    StringValue.create("__set").toStringValue(), new Value[] { //$NON-NLS-1$
                            name, value});
        } else {
            __set(name.toJavaString(), value.toJavaObject());
        }

        return oldValue;
    }

    /**
     * Unpersists / removes / deletes this node
     */
    public void remove() {
        this._node.remove();
    }

    /**
     * Removes the given node from this node's child collection without
     * unpersisting neither of both
     * 
     * @param node
     *            The node to remove from this node's child collection
     * @deprecated use removeChild() instead
     * @see removeChild()
     */
    @Deprecated
    public void remove(final HopObject node) {
        removeChild(node);
    }

    /**
     * Removes the given node from this node's child collection without
     * unpersisting neither of both
     * 
     * @param node
     *            The node to remove from this node's child collection
     */
    public void removeChild(final HopObject node) {
        this._node.removeNode(node.getNode());
    }

    /**
     * Renders a skin
     * 
     * @param skin
     *            The skin to render
     * @param parameters
     *            The parameters to provide to the skin
     * @throws ScriptingException
     */
    private void renderSkin(final Skin skin, final Object[] parameters) {
        try {
            skin.render(this._engine.getRequestEvaluator(), this._node, parameters);
        } catch (final RedirectException e) {
            // ignored by intention
        }
    }

    /**
     * Renders a skin given by name
     * 
     * @param name
     *            The name of the skin to render
     * @param parameters
     *            The parameters to provide to the skin
     * @throws ScriptingException
     */
    public void renderSkin(final String name, final Object[] parameters)
            throws ScriptingException {
        final Skin skin = getSkin(name);
        if (skin != null) {
            renderSkin(skin, parameters);
        }
    }

    /**
     * Returns the result of rendering the given skin
     * 
     * @param skin
     *            The skin to render
     * @param parameters
     *            The parameters to provide to the skin
     * @return The result of rendering the given skin with the given parameters
     * @throws ScriptingException
     */
    private String renderSkinAsString(final Skin skin, final Object[] parameters) {
        try {
            return skin.renderAsString(this._engine.getRequestEvaluator(), this._node,
                    parameters);
        } catch (final RedirectException e) {
            // ignored by intention
        }

        return ""; //$NON-NLS-1$
    }

    /**
     * Returns the result of rendering a skin given by name
     * 
     * @param name
     *            The skin to render
     * @param parameters
     *            The parameters to provide to the skin
     * @return The result of rendering the given skin with the given parameters
     *         or null, if no skin by the given name could be found
     * @throws ScriptingException
     */
    public String renderSkinAsString(final String name,
            final Object[] parameters) throws ScriptingException {
        final Skin skin = getSkin(name);
        if (skin != null) {
            return renderSkinAsString(skin, parameters);
        }

        return ""; //$NON-NLS-1$
    }

    /**
     * Returns the number of nodes in this node's child collection
     * 
     * @return The number of nodes in this node's child collection
     */

    public int size() {
        return this._node.numberOfNodes();
    }

    /**
     * Simply returns this. If not overridden, this method would return an
     * Object which would be the wrong runtime class for various reflection
     * actions.
     * 
     * @see com.caucho.quercus.env.ObjectExtJavaValue#toJavaObject()
     */
    @Override
    public Object toJavaObject() {
        return this;
    }

    /**
     * Updates this node's child collection by reloading it
     * 
     * @return The number of child nodes updated
     */
    public int update() {
        if (this._node instanceof Node) {
            ((Node) this._node).markSubnodesChanged();
            // FIXME
            return 0;
        }

        return 0;
    }

}