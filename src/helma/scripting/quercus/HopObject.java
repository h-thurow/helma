/*
 * Helma License Notice
 *
 * The contents of this file are subject to the Helma License
 * Version 2.0 (the "License"). You may not use this file except in
 * compliance with the License. A copy of the License is available at
 * http://adele.helma.org/download/helma/license.txt
 *
 * Copyright 2010-2017 Daniel Ruthardt. All rights reserved.
 */

package helma.scripting.quercus;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;

import com.caucho.quercus.annotation.Construct;
import com.caucho.quercus.annotation.Optional;
import com.caucho.quercus.env.ArrayValueImpl;
import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.NullValue;
import com.caucho.quercus.env.NumberValue;
import com.caucho.quercus.env.ObjectExtValue;
import com.caucho.quercus.env.StringValue;
import com.caucho.quercus.env.Value;
import com.caucho.quercus.env.Var;

import helma.framework.RedirectException;
import helma.framework.core.Prototype;
import helma.framework.core.Skin;
import helma.framework.repository.ResourceInterface;
import helma.objectmodel.NodeInterface;
import helma.objectmodel.db.DbKey;
import helma.objectmodel.db.DbMapping;
import helma.objectmodel.db.Node;
import helma.scripting.ScriptingException;

/**
 * A wrapper for NodeInterface objects.
 * Quercus does not automatically make use of the generic getter (__get) and setter (__set), if defined in 
 * the PHP context (extending a Java object). This functionality is also provided by this class.
 */
public class HopObject extends ObjectExtValue {

    // serialization UID
    private static final long serialVersionUID = -5555069166328227883L;

    /**
     * The wrapped object.
     */
    private final NodeInterface _node;

    /**
     * Wrap the given NodeInterface object.
     * 
     * @param node
     *  The object to wrap.
     */
    public HopObject(final NodeInterface node) {
        // do what would have been done anyways
        super(Env.getCurrent(), Env.getCurrent().getClass(node.getPrototype()));

        // remember the wrapped object
        this._node = node;
    }

    /**
     * PHP constructor wrapping a new Node of the given prototype.
     * 
     * @param prototype
     *  The prototype to use for creating the wrapped Node.
     */
    @Construct
    public HopObject(@Optional final String prototype) {
        // create a new Node of the given prototype and wrap it
        this(new Node(null, prototype, 
                QuercusEngine.ENGINE.get().getRequestEvaluator().app.getWrappedNodeManager()));
    }

    /**
     * No idea what the orignal overridden method does, but it conflicts with add(HopObject), so we need to 
     * either delegate to HopObject::add(HopObject) or to super::add(Value).
     * 
     * @see com.caucho.quercus.env.Value#add(com.caucho.quercus.env.Value)
     */
    @Override
    public Value add(final Value value) {
        // check if the un-marshalled value is a HopObject
        if (value.toJavaObject() instanceof HopObject) {
            // delegate
            this.add((HopObject) value.toJavaObject());
            // nothing to return
            return NullValue.NULL;
        }

        // do what would have been done anyways
        return super.add(value);
    }

    /**
     * Attaches a HopObject as an additional subnode. 
     * 
     * Adds a HopObject as new subnode to another HopObject. The new subnode is added after the last subnode 
     * already contained in the parent HopObject. 
     * 
     * If the subnode is already attached, it is moved to the last index position.
     * 
     * @param subnode
     *  The subnode to add to this node.
     * @return
     *  True, if the addition or move was successful.
     */
    public boolean add(final HopObject subnode) {
        // delegate
        this._node.addNode(subnode._node);

        // addition or move was successful
        return true;
    }

    /**
     * Attaches an additional subnode at the given index. 
     * 
     * Adds a HopObject to a collection at a certain position, and shifts the index of all succeeding 
     * objects in that collection by one. Index positions start with 0. Any out of range moves will move the 
     * subnode to the last index position.
     * 
     * Just makes sense for HopObjects, that are not mapped on a relational DB, since the sort order of the 
     * collection would otherwise be defined by type.properties, resp. by the database itself. Returns true 
     * in case of success. If the subnode is already attached, it will be moved to the specified index 
     * position.
     * 
     * @param position
     *  The index position where the subnode is to be inserted.
     * @param subnode
     *  The subnode to add to this node.
     * @return
     *  True, if the addition or move was successful.
     */
    public boolean addAt(final int position, final HopObject subnode) {
        // delegate
        this._node.addNode(subnode._node, position);

        // addition or move was successful
        return true;
    }

    /**
     * Clears this HopObject's cache property. 
     * 
     * Removes all information stored in the cache object. Doing this by just calling 'obj.cache = null' is 
     * not possible, since the property itself can not be set.
     */
    public void clearCache() {
        // delegate
        this._node.clearCacheNode();
    }

    /**
     * Determines if this node contains a certain subnode.
     * 
     * @param node
     *  The node to look for.
     * @return
     *  True, if a this node contains the given subnode.
     */
    public boolean contains(final HopObject node) {
        // check if this node contains the given subnode
        return this._node.contains(node._node) >= 0;
    }

    /**
     * Retrieves a persisted HopObject that is a subnode or a mapped property of this HopObject.
     * 
     * If the argument is a number, this method returns the subnode of this HopObject at the corresponding 
     * index position. 
     * 
     * If the argument is a string and matches a property name mapped in this prototype's type.properties 
     * file to a mountpoint, object, or collection, this function returns the corresponding HopObject. 
     * 
     * If the string argument produces no such match, the behavior depends on whether this HopObject's 
     * _children have an accessname defined in the prototype's type.properties. 
     * 
     * If an accessname is defined, this function first attempts to return the subnode with the 
     * corresponding name. Otherwise, or if that attempt fails, a string argument will result in a null 
     * return unless the string argument is numeric, in which case this function will return the child with 
     * an _id matching the numeric value of the argument. However, retrieving a HopObject based on its _id 
     * value is better achieved using the getById() HopObject method.
     * 
     * @param index
     *  The index position.
     * @return
     *  The subnode at the specified index position.
     */
    public HopObject get(final int index) {
        // get the subnode the specified index position
        NodeInterface node = this._node.getSubnodeAt(index);
        // return the wrapped subnode or null
        return node != null ? new HopObject(node) : null;
    }

    /**
     * Retrieves a persisted HopObject that is a subnode or a mapped property of this HopObject.
     * 
     * If the argument is a number, this method returns the subnode of this HopObject at the corresponding 
     * index position. 
     * 
     * If the argument is a string and matches a property name mapped in this prototype's type.properties 
     * file to a mountpoint, object, or collection, this function returns the corresponding HopObject. 
     * 
     * If the string argument produces no such match, the behavior depends on whether this HopObject's 
     * _children have an accessname defined in the prototype's type.properties. 
     * 
     * If an accessname is defined, this function first attempts to return the subnode with the 
     * corresponding name. Otherwise, or if that attempt fails, a string argument will result in a null 
     * return unless the string argument is numeric, in which case this function will return the child with 
     * an _id matching the numeric value of the argument. However, retrieving a HopObject based on its _id 
     * value is better achieved using the getById() HopObject method.
     * 
     * @param name
     *  The name of a property name, the accessname of a subnode or the id of a subnode.
     * @return
     *  The property or the subnode with the given accessname or id.
     */
    public Object get(final String name) {
        // get the property or subnode
        final Object node = this._node.getChildElement(name);

        // check if the property or subnode is a Node
        if (node instanceof NodeInterface) {
            // return the wrapped Node
            return new HopObject((NodeInterface) node);
        }

        // return the wrapped property or subnode
        return Env.getCurrent().wrapJava(node);
    }

    /**
     * No idea what the orignal overridden method does, but it conflicts with HopObject::get(int) and 
     * HopObject::get(name), so we need to either delegate to HopObject::get(int), HopObject::get(name) or 
     * to super::get(Value).
     * 
     * @see com.caucho.quercus.env.Value#get(com.caucho.quercus.env.Value)
     */
    @Override
    public Value get(Value key) {
        // check if key is a variable
        if (key instanceof Var) {
            // get the variable's value
            key = ((Var) key).toValue();
        }

        // check if the key is a number
        if (key instanceof NumberValue) {
            // key is a number, we want to call HopObject::get(int)
            return get(((NumberValue) key).toInt());
        }
        // check if the key is a string
        else if (key instanceof StringValue) {
            // key is a string, we want to call get(String)
            final Object value = get(((StringValue) key).toJavaString());

            // check if the value is a Node
            if (value instanceof NodeInterface) {
                // return the wrapped Node
                return new HopObject((NodeInterface) value);
            }

            // return the wrapped value
            return Env.getCurrent().wrapJava(value);
        }

        // do what would have been done anyways
        return super.get(key);
    }

    /**
     * Retrieves the specified HopObject.
     * 
     * If called on a HopObject instance, this getById() retrieves a child object by ID. If called on a 
     * HopObject constructor, it retrieves the persisted HopObject of that prototype and with the specified 
     * ID.
     * 
     * Fetches a HopObject of a certain prototype through its ID and its prototype name. The prototype name 
     * can either be passed as a second argument, or alternatively the function can also be called on the 
     * prototype itself with a single argument (e.g. Story.getById(123)).
     * 
     * In case of multiple prototypes being mapped on the same table (which is for instance the case with 
     * inherited prototypes) Helma will not check whether the prototype of the fetched object actually 
     * matches the specified prototype.
     * 
     * Note, that this refers to the static method 'getById', not to be mixed up with the method getById 
     * called on a specific HopObject. 
     * 
     * @param id
     *  The id of a child node.
     * @return
     *  The child node that was retrieved.
     */
    public HopObject getById(final String id) {
     // get the subnode the specified index position
        NodeInterface node = this._node.getSubnode(id);
        // return the wrapped child node or null
        return node != null ? new HopObject(node) : null;
    }

    /**
     * Retrieves the specified HopObject.
     * 
     * If called on a HopObject instance, this getById() retrieves a child object by ID. If called on a 
     * HopObject constructor, it retrieves the persisted HopObject of that prototype and with the specified 
     * ID.
     * 
     * Fetches a HopObject of a certain prototype through its ID and its prototype name. The prototype name 
     * can either be passed as a second argument, or alternatively the function can also be called on the 
     * prototype itself with a single argument (e.g. Story.getById(123)).
     * 
     * In case of multiple prototypes being mapped on the same table (which is for instance the case with 
     * inherited prototypes) Helma will not check whether the prototype of the fetched object actually 
     * matches the specified prototype.
     * 
     * Note, that this refers to the static method 'getById', not to be mixed up with the method getById 
     * called on a specific HopObject. 
     * 
     * @param id
     *  The id of the child node.
     * @param prototype
     *  The name of the prototype.
     * @return
     *  The child node that was retrieved.
     * 
     * @throws ScriptingException 
     */
    public static Value getById(final String id, final String prototype) throws ScriptingException {
        // get the db mapping for the given prototype
        final DbMapping dbMapping = QuercusEngine.ENGINE.get().getApplication().getDbMapping(prototype);
        // check if there is no db mapping for the given prototype
        if (dbMapping == null) {
            // no child node can be returned
            return NullValue.NULL;
        }

        // create a db kex for the given id
        final DbKey dbKey = new DbKey(dbMapping, id);
        try {
            // get the child node
            final NodeInterface node = QuercusEngine.ENGINE.get().getApplication().getNodeManager()
                    .getNode(dbKey);
            
            // check if a child node was retrieved
            if (node != null) {
                // return the wrapped childe node
                return new HopObject(node);
            }
        } catch (final Exception e) {
            // return an exception as PHP error
            return Env.getCurrent().error(new ScriptingException(Messages.getString(Messages.getString("HopObject.0")), e)); //$NON-NLS-1$
        }

        // no child node was retrieved
        return NullValue.NULL;
    }

    /**
     * The magic getter.
     * 
     * @param name
     *  The name of the property.
     * @return
     *  The value of the property.
     */
    public Value __getField(StringValue name) {
        // delegate
        return this.getFieldExt(Env.getCurrent(), name);
    }

    /**
     * The actual magic getter.
     * 
     * @see com.caucho.quercus.env.ObjectExtJavaValue#getFieldExt(com.caucho.quercus.env.Env,
     *      com.caucho.quercus.env.StringValue)
     */
    @Override
    protected Value getFieldExt(final Env environment, final StringValue name) {
        // check if we are looking for an internal property
        if (name.startsWith("_")) { //$NON-NLS-1$
            // swirtch the property we are looking for
            switch (name.toJavaString()) {
                case "_prototype": //$NON-NLS-1$
                case "__prototype__": //$NON-NLS-1$
                    // return the Node's prototype name
                    return Env.getCurrent().createString(this._node.getPrototype());
                case "_name": //$NON-NLS-1$
                case "__name__": //$NON-NLS-1$
                    // return the Node's name
                    return Env.getCurrent().createString(this._node.getName());
                case "_parent": //$NON-NLS-1$
                case "__parent__": //$NON-NLS-1$
                    // return the wrapped Node's parent
                    return new HopObject(this._node.getParent());
                case "cache": //$NON-NLS-1$
                    // return the wrapped Node's cache Node
                    return new HopObject(this._node.getCacheNode());
                case "_id": //$NON-NLS-1$
                case "__id__": //$NON-NLS-1$
                    // return the Node's id
                    return Env.getCurrent().createString(this._node.getID());
                case "__proto__": //$NON-NLS-1$
                    // return the wrapped Node's prototype
                    return Env.getCurrent().wrapJava(QuercusEngine.ENGINE.get().getApplication()
                            .getPrototypeByName(this._node.getPrototype()));
                case "__hash__": //$NON-NLS-1$
                    // return the Node's hash
                    return Env.getCurrent().wrapJava(new Integer(this._node.hashCode()));
                case "__node__": //$NON-NLS-1$
                    // return the Node
                    return Env.getCurrent().wrapJava(this._node);
                case "__created__": //$NON-NLS-1$
                    // return the Node's creation date
                    return Env.getCurrent().wrapJava(new Long(this._node.created()));
                case "__lastmodified__": //$NON-NLS-1$
                    // return the Node's last modified date
                    return Env.getCurrent().wrapJava(new Long(this._node.lastModified()));
            }            
        }
        // check if we are looking for a special, but not internal, property
        else if (name.toJavaString().equals("subnodeRelation")) { //$NON-NLS-1$
            // return the subnode relation configuration
            return Env.getCurrent().createString(this._node.getSubnodeRelation());
        }

        // delegate
        return Env.getCurrent().wrapJava(this._node.get(name.toJavaString()));
    }

    /**
     * Returns the wrapped Node.
     * 
     * @return
     *  The wrapped Node.
     */
    protected NodeInterface getNode() {
        // return the wrapped node
        return this._node;
    }

    /**
     * Returns a helma.framework.repository.Resource object defined for the prototype.
     * 
     * Returns a resource referenced by its name for the current HopObject's prototype - getResource() walks 
     * up the inheritance chain and through all defined repositories to find the resource and returns null 
     * if unsuccessful. 
     * 
     * @param name
     *  The name of the requested resource.
     * @return
     *  The requested resource.
     */
    public ResourceInterface getResource(final String name) {
        // get the prototype
        Prototype prototype = QuercusEngine.ENGINE.get().getApplication().getPrototypeByName(
                this._node.getPrototype());
        // loop up the prototype chain
        while (prototype != null) {
            // get the current prototype's resources
            final ResourceInterface[] resources = prototype.getResources();
            // loop the current prototype's resources
            for (int i = resources.length - 1; i >= 0; i--) {
                // get the current resource
                final ResourceInterface resource = resources[i];
                // check if the resource actually exists and matches the given name
                if (resource.exists() && resource.getShortName().equals(name)) {
                    // return the found resource
                    return resource;
                }
            }

            // move on to the parent prototype
            prototype = prototype.getParentPrototype();
        }

         // the resource was not found
        return null;
    }

    /**
     * Returns an Array of helma.framework.repository.Resource objects defined for the prototype. 
     * 
     * Returns an array of resources by the specified name for the current HopObject's prototype - 
     * getResources() walks up the inheritance chain and through all defined repositories to collect all the 
     * resources by that name and returns null if unsuccessful.
     * 
     * @param name
     *  The name of the requested resource.
     * @return
     *  The requested resources.
     */
    public ResourceInterface[] getResources(final String name) {
        // get the prototype
        Prototype prototype =  QuercusEngine.ENGINE.get().getApplication().getPrototypeByName(
                this._node.getPrototype());
        // the found resources
        final ArrayList<ResourceInterface> foundResources = new ArrayList<ResourceInterface>();
        // loop up the prototype chain
        while (prototype != null) {
            // get the current prototype's resources
            final ResourceInterface[] resources = prototype.getResources();
            // loop the current prototype's resources
            for (int i = resources.length - 1; i >= 0; i--) {
                // get the current resource
                final ResourceInterface resource = resources[i];
                // check if the resource actually exists and matches the given name
                if (resource.exists() && resource.getShortName().equals(name)) {
                    // add the current resource to the found resources
                    foundResources.add(resource);
                }
            }

            // move on to the parent prototype
            prototype = prototype.getParentPrototype();
        }

        // return the found resources
        return foundResources.toArray(new ResourceInterface[0]);
    }

    /**
     * Implements the generic getter (__get)
     * 
     * @see com.caucho.quercus.env.ObjectExtValue#getThisFieldArg(com.caucho.quercus.env.Env,
     *      com.caucho.quercus.env.StringValue)
     */
    @Override
    public Value getThisFieldArg(final Env env, final StringValue name) {
        // delegate
        return getFieldExt(env, name);
    }

    /**
     * Returns the absoulte URL path of a HopObject relative to the application's root.
     * 
     * This function is useful when referring to a HopObject in a markup tag (e.g. with a href attribute in 
     * an HTML <a>-tag). An optional string argument is appended to the return value.
     * 
     * @param action
     *  Optional part to be attached to the URL of this HopObject
     * @return
     *  The URL path for this HopObject.
     */
    public Value href(@Optional final String action) throws ScriptingException {
        try {
            // delegate
            return Env.getCurrent().createString(QuercusEngine.ENGINE.get().getApplication()
                    .getNodeHref(this._node, action, null));
        } catch (final UnsupportedEncodingException e) {
            // return an exception as PHP error
            return Env.getCurrent().error(new ScriptingException(e.getMessage(), e));
        }
    }

    /**
     * Determines if a HopObject contains a certain subnode.
     * 
     * Returns the index position of a Subnode contained by a HopObject (as usual for JavaScript, 0 refers 
     * to the first position).
     * 
     * The index position is a relative value inside a HopObject (not to be confused with a Hop ID which is 
     * unique for each HopObject).
     * 
     * If there is no appropriate subnode inside the HopObject the returned value equals -1. 
     * 
     * @param node
     *  The node to look for.
     * @return
     *  The index position of the subnode.
     */
    public int indexOf(final HopObject node) {
        // return the subnode's index position
        return this._node.contains(node._node);
    }

    /**
     * Marks a HopObject as invalid so that it is fetched again from the database.
     * 
     * Helma will overwrite the HopObject's node cache with the database contents the next time the 
     * HopObject is accessed.
     * 
     * In other words, use this function to kick out an HopObject of Helma's node cache and force a database 
     * retrieval of the HopObject data. 
     */
    public void invalidate() {
        // check if the wrapped Node can be invalidated
        if (this._node instanceof Node && this._node.getState() != NodeInterface.INVALID) {
            // invalidate the wraped Node
            ((Node) this._node).invalidate();
        }
    }

    /**
     * Returns true if the HopObject is in persistent state, meaning that it is stored in the database, and 
     * false if it is transient.
     * 
     * Persistent state is also assumed if the object is currently in the process of being inserted into or 
     * deleted from the database.
     * 
     * @return
     *  True, if the HopObject is in persistent state.
     */
    public boolean isPersistent() {
        // check and return, if the wrapped Node is persistent
        return this._node.getState() != NodeInterface.TRANSIENT;
    }

    /**
     * Returns true if the HopObject is in transient state, meaning that it is not stored in the database.
     * 
     * This method returns false if the object is stored in the database, or is in the process of being 
     * inserted into or deleted from the database.
     * 
     * @return
     *  True if the HopObject is in transient state.
     */
    public boolean isTransient() {
        // check and return, if the wrapped Node is transient
        return !(this._node instanceof Node) || this._node.getState() == NodeInterface.TRANSIENT;
    }

    /**
     * Returns an array including all subnodes of a HopObject.
     * 
     * The startIndex and length parameters are optional, if omitted, an array of the entire collection of 
     * subnodes is returned, otherwise only the specified range.
     * 
     * @return
     *  All subnodes of a HopObject.
     */
    public HopObject[] list() {
        // all subnodes of this wrapped Node
        final ArrayList<HopObject> children = new ArrayList<HopObject>();
        // get all subnodes
        final Enumeration subnodes = this._node.getSubnodes();
        // loop all subnodes
        while (subnodes.hasMoreElements()) {
            // add the current subnode as wrapped HopObject
            children.add(new HopObject((NodeInterface) subnodes.nextElement()));
        }

        // return all subnodes
        return children.toArray(new HopObject[0]);
    }

    /**
     * Stores a transient HopObject and all HopObjects reachable from it to database.
     * 
     * The function returns the id (primary key) of the newly stored HopObject as string, or null if the 
     * HopObject couldn't be stored for some reason. 
     * 
     * @return
     *  The id (primary key) of the newly stored HopObject. 
     */
    public String persist() {
        // check if wrapped Node can be persisted
        if (this._node instanceof Node) {
            // persist the wrapped Node
            ((Node) this._node).persist();
            // return the id
            return this._node.getID();
        }

        // wrapped Node is not persistable, no id to return
        return null;
    }

    /**
     * Manually retrieving a particular set of subnodes.
     * 
     * This function provides some control of how many subnodes Helma should retrieve from the database and 
     * hold prepared in the node cache for further processing. 
     * 
     * This means that for large collections Helma does not need to retrieve neither the subset of subnodes 
     * via one SQL statement for each subnode nor the whole collection at once via one statement. 
     * 
     * Moreover, only subnodes are retrieved that are not in the node cache already which leads to a maximum 
     * of caching efficiency and loading performance. 
     * 
     * @param startIndex
     *  The first subnode to retrieve.
     * @param length
     *  The number of subnodes to retrieve. 
     */
    public void prefetchChildren(final int startIndex, final int length) {
        // check if subnodes can be prefetched for the wrapped Node
        if (this._node instanceof Node) {
            try {
                // delegate
                ((Node) this._node).prefetchChildren(startIndex, length);
            } catch (final Exception e) {
                // throw an exception as PHP error
                Env.getCurrent().error(new ScriptingException(Messages.getString(Messages.getString("HopObject.15")), e)); //$NON-NLS-1$
            }
        }
    }

    /**
     * The generic setter.
     * 
     * @param name
     * @param value
     */
    public void __setField(String name, Value value) {
        // delegate
        this.putFieldExt(Env.getCurrent(), Env.getCurrent().createString(name), value);
    }

    /**
     * The actual generic setter.
     * 
     * @see com.caucho.quercus.env.ObjectExtJavaValue#putFieldExt(com.caucho.quercus.env.Env,
     *      com.caucho.quercus.env.StringValue, com.caucho.quercus.env.Value)
     */
    @Override
    protected Value putFieldExt(final Env environment, final StringValue name, final Value value) {
        // check if to set the subnode relation configuaration
        if (name.toJavaString().equals("subnodeRelation")) { //$NON-NLS-1$
            // set the subnode relation configuration
            this._node.setSubnodeRelation(value.toJavaString());
        }
        // check if to unset a value
        else if (value.isNull()) {
            // unset the value
            this._node.unset(name.toJavaString());
        }
        // check if to set the prototype
        else if (name.toJavaString().equals("_prototype")) { //$NON-NLS-1$
            // set the prototype
            this._node.setPrototype(value.toJavaString());
        }
        // check if to set the name
        else if (name.toJavaString().equals("_name")) { //$NON-NLS-1$
            // set the name
            this._node.setName(value.toJavaString());
        }
        // check if to set a boolean value
        else if (value.isBoolean()) {
            // set boolean value
            this._node.setBoolean(name.toJavaString(), value.toBoolean());
        }
        // check if to set a Date
        else if (value.toJavaObject() instanceof Date) {
            // set the Date
            this._node.setDate(name.toJavaString(), value.toJavaDate());
        }
        // check if to set a float value
        else if (value.isNumeric() && value.toJavaObject() instanceof Float) {
            // set the float value
            this._node.setFloat(name.toJavaString(), ((Float) value.toJavaObject()).floatValue());
        }
        // check if to set an integer value
        else if (value.isNumeric() && value.toJavaObject() instanceof Integer) {
            // set the integer value
            this._node.setInteger(name.toJavaString(), ((Integer) value.toJavaObject()).intValue());
        }
        // check if to set a HopObject
        else if (value.toJavaObject() instanceof HopObject) {
            // set the HopObject's wrapped Node
            this._node.setNode(name.toJavaString(), ((HopObject) value.toJavaObject())._node);
        }
        // check if to set a string value
        else if (value.isString()) {
            // set the string value
            this._node.setString(name.toJavaString(), value.toJavaString());
        } else {
            // set the value as Java value
            this._node.setJavaObject(name.toJavaString(), value.toJavaObject());
        }

        // FIXME
        return NullValue.NULL;
    }

    /**
     * Deletes a HopObject from the database.
     * 
     * The remove() function deletes a persistent HopObject from the database.
     * 
     * Note that additionally you may want to call the removeChild() function on any object holding the 
     * deleted object in its child collection in order to notify it that the child object has been removed.
     */
    public void remove() {
        // delegate
        this._node.remove();
    }

    /**
     * Notifies a parent object that a child object has been removed.
     * 
     * The removeChild() function lets a parent object know that a child object has been removed. Note that 
     * calling removeChild() will not actually delete the child object. Directly call remove() on the child 
     * object in order to delete it from the database. 
     * 
     * @param node
     *  The removed child object
     */
    public void removeChild(final HopObject node) {
        // delegate
        this._node.removeNode(node._node);
    }

    /**
     * Renders the passed skin object to the response buffer.
     * The properties of the optional parameter object are accessible within the skin through the 'param' 
     * macro handler.
     * 
     * @param skin
     *  The skin object.
     * @param parameters
     *  Optional properties to be passed to the skin.
     */
    public void renderSkin(final Skin skin, @Optional final ArrayValueImpl parameters) {
        try {
            // render the skin
            skin.render(QuercusEngine.ENGINE.get().getRequestEvaluator(), this._node, parameters);
        } catch (final RedirectException e) {
            // ignored by intention
        }
    }

    /**
     * Renders the skin matching the passed name to the response buffer.
     * The properties of the optional parameter object are accessible within the skin through the 'param' 
     * macro handler.
     * 
     * @param skin
     *  The name of the skin.
     * @param parameters
     *  Optional properties to be passed to the skin.
     */
    public Value renderSkin(final String name, @Optional final ArrayValueImpl parameters) {
        try {
            // get the skin
            Skin skin = QuercusEngine.ENGINE.get().getSkin(this._node.getPrototype(), name);
            // render the skin
            this.renderSkin(skin, parameters);
        } catch (ScriptingException e) {
            // return the exception as PHP error
            return Env.getCurrent().error(e);
        }

        // nothing to return
        return NullValue.NULL;
    }

    /**
     * Returns the result of the rendered skin object.
     * The properties of the optional parameter object are accessible within the skin through the 'param' 
     * macro handler.
     * 
     * @param skin
     *  The skin object.
     * @param parameters
     *  Optional properties to be passed to the skin.
     * @return
     *  The rendered skin.
     */
    public StringValue renderSkinAsString(final Skin skin, @Optional final ArrayValueImpl parameters) {
        try {
            // delegate
            return Env.getCurrent().createString(skin.renderAsString(QuercusEngine.ENGINE.get()
                    .getRequestEvaluator(), this._node, parameters));
        } catch (final RedirectException e) {
            // ignored by intention
        }

        // rendering failed
        return StringValue.EMPTY;
    }

    /**
    * Returns the result of a rendered skin matching the passed name.
    * The properties of the optional parameter object are accessible within the skin through the 'param' 
    * macro handler.
    * 
    * @param skin
    *  The name of the skin.
    * @param parameters
    *  Optional properties to be passed to the skin.
    * @return
    *  The rendered skin.
    */
    public Value renderSkinAsString(final String name, @Optional final ArrayValueImpl parameters) { 
        try {
            // get the skin
            Skin skin = QuercusEngine.ENGINE.get().getSkin(this._node.getPrototype(), name);
            // return the result of rendering the skin
            return this.renderSkinAsString(skin, parameters);
        } catch (ScriptingException e) {
            // return the exception as PHP errpr
            return Env.getCurrent().error(e);
        }
    }

    /**
     * Get the number of subnodes attached to this HopObject. 
     * 
     * @return
     *  The number of subnodes.
     */
    public int size() {
        // return the number of subnodes
        return this._node.numberOfNodes();
    }

    /**
     * Simply return the wrapped Node as is.
     * If not overridden, this method would return an object which would be the wrong runtime class for 
     * various reflection actions.
     * 
     * @see com.caucho.quercus.env.ObjectExtJavaValue#toJavaObject()
     */
    @Override
    public Object toJavaObject() {
        // simply return the wrapped Node as is
        return this;
    }

    /**
     * Refetches updateable Subnode-Collections from the database.
     * 
     * The following conditions must be met to make a subnodecollection updateable:
     * 1) the collection must be specified with collection.updateable=true
     * 2) the id's of this collection must be in ascending order, meaning, that new records do have a higher 
     * id than the last record loaded by this collection.
     * 
     * @return
     *  The number of updated nodes
     */
    public int update() {
        // check if the wrapped Node can be updated
        if (this._node instanceof Node) {
            // delegate
            ((Node) this._node).markSubnodesChanged();
            // FIXME
            return 0;
        }

        // wrapped Node can not be updated
        return 0;
    }

    /**
     * @see Object::equals()
     */
    public boolean equals(Object object) {
        // check if objects equal
        return object instanceof HopObject && this._node.equals(((HopObject) object)._node);
    }

}