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
 * Contributions:
 *   Daniel Ruthardt
 *   Copyright 2010 dowee Limited. All rights reserved.
 *
 * $RCSfile$
 * $Author$
 * $Revision$
 * $Date$
 */

package helma.objectmodel.db;

import helma.framework.PathElementInterface;
import helma.framework.core.RequestEvaluator;
import helma.framework.core.Application;
import helma.objectmodel.ConcurrencyException;
import helma.objectmodel.NodeInterface;
import helma.objectmodel.PropertyInterface;
import helma.objectmodel.TransientNode;

import java.util.*;

/**
 * An implementation of NodeInterface that can be stored in the internal database or
 * an external relational database.
 */
public final class Node implements NodeInterface {

    // The handle to the node's parent
    protected NodeHandle parentHandle;

    // Ordered list of subnodes of this node
    private SubnodeList subnodes;

    // Named subnodes (properties) of this node
    private Hashtable propMap;

    protected long created;
    protected long lastmodified;
    private String id;
    private String name;

    // is this node's main identity as a named property or an
    // anonymous node in a subnode collection?
    protected boolean anonymous = false;

    // the serialization version this object was read from (see readObject())
    protected short version = 0;
    private String prototype;
    private NodeHandle handle;
    private NodeInterface cacheNode;
    final WrappedNodeManager nmgr;
    DbMapping dbmap;
    KeyInterface primaryKey = null;
    String subnodeRelation = null;
    long lastNameCheck = 0;
    long lastParentSet = 0;
    private volatile Transactor lock;
    private volatile int state;
    private static long idgen = 0;

    /**
     * Creates an empty, uninitialized Node with the given create and modify time.
     * This is used for null-node references in the node cache.
     * @param timestamp
     */
    protected Node(long timestamp) {
        this.created = this.lastmodified = timestamp;
        this.nmgr = null;
    }

    /**
     * Creates an empty, uninitialized Node. The init() method must be called on the
     * Node before it can do anything useful.
     */
    protected Node(WrappedNodeManager nmgr) {
        if (nmgr == null) {
            throw new NullPointerException("nmgr"); //$NON-NLS-1$
        }
        this.nmgr = nmgr;
        this.created = this.lastmodified = System.currentTimeMillis();
    }

    /**
     * Creates a new Node with the given name. Used by NodeManager for creating "root nodes"
     * outside of a Transaction context, which is why we can immediately mark it as CLEAN.
     * Also used by embedded database to re-create an existing Node.
     */
    public Node(String name, String id, String prototype, WrappedNodeManager nmgr) {
        if (nmgr == null) {
            throw new NullPointerException("nmgr"); //$NON-NLS-1$
        }
        this.nmgr = nmgr;
        if (prototype == null) {
            prototype = "HopObject"; //$NON-NLS-1$
        }
        init(nmgr.getDbMapping(prototype), id, name, prototype, null);
    }

    /**
     * Constructor used to create a Node with a given name from a embedded database.
     */
    public Node(String name, String id, String prototype, WrappedNodeManager nmgr,
                long created, long lastmodified) {
        this(name, id, prototype, nmgr);
        this.created = created;
        this.lastmodified = lastmodified;
    }

    /**
     * Constructor used for virtual nodes.
     */
    public Node(Node home, String propname, WrappedNodeManager nmgr, String prototype) {
        if (nmgr == null) {
            throw new NullPointerException("nmgr"); //$NON-NLS-1$
        }
        this.nmgr = nmgr;
        setParent(home);
        // generate a key for the virtual node that can't be mistaken for a Database KeyInterface
        this.primaryKey = new SyntheticKey(home.getKey(), propname);
        this.id = this.primaryKey.getID();
        this.name = propname;
        this.prototype = prototype;
        this.anonymous = false;

        // set the collection's state according to the home node's state
        if (home.state == NEW || home.state == TRANSIENT) {
            this.state = TRANSIENT;
        } else {
            this.state = VIRTUAL;
        }
    }

    /**
     * Creates a new Node with the given name. This is used for ordinary transient nodes.
     */
    public Node(String name, String prototype, WrappedNodeManager nmgr) {
        if (nmgr == null) {
            throw new NullPointerException("nmgr"); //$NON-NLS-1$
        }
        this.nmgr = nmgr;
        this.prototype = prototype;
        this.dbmap = nmgr.getDbMapping(prototype);

        // the id is only generated when the node is actually checked into db,
        // or when it's explicitly requested.
        this.id = null;
        this.name = (name == null) ? "" : name; //$NON-NLS-1$
        this.created = this.lastmodified = System.currentTimeMillis();
        this.state = TRANSIENT;

        if (prototype != null && this.dbmap != null) {
            String protoProperty = this.dbmap.columnNameToProperty(this.dbmap.getPrototypeField());
            if (protoProperty != null) {
                setString(protoProperty, this.dbmap.getExtensionId());
            }
        }
    }

    /**
     * Initializer used for nodes being instanced from an embedded or relational database.
     */
    public synchronized void init(DbMapping dbm, String id, String name,
                                  String prototype, Hashtable propMap) {
        this.dbmap = dbm;
        this.prototype = prototype;
        this.id = id;
        this.name = name;
        // If name was not set from resultset, create a synthetical name now.
        if ((name == null) || (name.length() == 0)) {
            this.name = prototype + " " + id; //$NON-NLS-1$
        }

        this.propMap = propMap;

        // set lastmodified and created timestamps and mark as clean
        this.created = this.lastmodified = System.currentTimeMillis();

        if (this.state != CLEAN) {
            markAs(CLEAN);
        }
    }

    /**
     * used by Xml deserialization
     */
    public synchronized void setPropMap(Hashtable propMap) {
        this.propMap = propMap;
    }

    /**
     * Get the write lock on this node, throwing a ConcurrencyException if the
     * lock is already held by another thread.
     */
    synchronized void checkWriteLock() {
        if (this.state == TRANSIENT) {
            return; // no need to lock transient node
        }

        Transactor tx = Transactor.getInstanceOrFail();

        if (!tx.isActive()) {
            throw new helma.framework.TimeoutException();
        }

        if (this.state == INVALID) {
            this.nmgr.logEvent(Messages.getString("Node.0") + this); //$NON-NLS-1$
            Thread.dumpStack();
            throw new ConcurrencyException(Messages.getString("Node.1") + this + //$NON-NLS-1$
                                           Messages.getString("Node.2")); //$NON-NLS-1$
        }

        if ((this.lock != null) && (this.lock != tx) && this.lock.isAlive() && this.lock.isActive()) {
            // nmgr.logEvent("Concurrency conflict for " + this + ", lock held by " + lock);
            throw new ConcurrencyException(Messages.getString("Node.3") + this + //$NON-NLS-1$
                                           Messages.getString("Node.4")); //$NON-NLS-1$
        }

        tx.visitDirtyNode(this);
        this.lock = tx;
    }

    /**
     * Clear the write lock on this node.
     */
    synchronized void clearWriteLock() {
        this.lock = null;
    }

    /**
     *  Set this node's state, registering it with the transactor if necessary.
     */
    void markAs(int s) {
        if (s == this.state || this.state == INVALID || this.state == VIRTUAL || this.state == TRANSIENT) {
            return;
        }

        this.state = s;

        Transactor tx = Transactor.getInstance();
        if (tx != null) {
            if (s == CLEAN) {
                clearWriteLock();
                tx.dropDirtyNode(this);
            } else {
                tx.visitDirtyNode(this);

                if (s == NEW) {
                    clearWriteLock();
                    tx.visitCleanNode(this);
                }
            }
        }
    }

    /**
     * Register this node as parent node with the transactor so that
     * setLastSubnodeChange is called when the transaction completes.
     */
    void registerSubnodeChange() {
        // we do not fetch subnodes for nodes that haven't been persisted yet or are in
        // the process of being persistified - except if "manual" subnoderelation is set.
        if ((this.state == TRANSIENT || this.state == NEW) && this.subnodeRelation == null) {
            return;
        }
        Transactor tx = Transactor.getInstance();
        if (tx != null) {
            tx.visitParentNode(this);
        }
    }

    /**
     * Notify the node's parent that its child collection needs to be reloaded
     * in case the changed property has an affect on collection order or content.
     *
     * @param propname the name of the property being changed
     */
    void notifyPropertyChange(String propname) {
        Node parent = (this.parentHandle == null) ? null : (Node) getParent();

        if ((parent != null) && (parent.getDbMapping() != null)) {
            // check if this node is already registered with the old name; if so, remove it.
            // then set parent's property to this node for the new name value
            DbMapping parentmap = parent.getDbMapping();
            Relation subrel = parentmap.getSubnodeRelation();
            String dbcolumn = this.dbmap.propertyToColumnName(propname);
            if (subrel == null || dbcolumn == null)
                return;

            if (subrel.order != null && subrel.order.indexOf(dbcolumn) > -1) {
                parent.registerSubnodeChange();
            }
        }
    }

    /**
     * Called by the transactor on registered parent nodes to mark the
     * child index as changed
     */
    public void markSubnodesChanged() {
        if (this.subnodes != null) {
            this.subnodes.markAsChanged();
        }
    }

    /**
     *  Gets this node's stateas defined in the NodeInterface interface
     *
     * @return this node's state
     */
    public int getState() {
        return this.state;
    }

    /**
     * Sets this node's state as defined in the NodeInterface interface
     *
     * @param s this node's new state
     */
    public void setState(int s) {
        this.state = s;
    }

    /**
     *  Mark node as invalid so it is re-fetched from the database
     */
    public void invalidate() {
        // This doesn't make sense for transient nodes
        if ((this.state == TRANSIENT) || (this.state == NEW)) {
            return;
        }

        checkWriteLock();
        this.nmgr.evictNode(this);
    }

    /**
     *  Check for a child mapping and evict the object specified by key from the cache
     */
    public void invalidateNode(String key) {
        // This doesn't make sense for transient nodes
        if ((this.state == TRANSIENT) || (this.state == NEW)) {
            return;
        }

        Relation rel = getDbMapping().getSubnodeRelation();

        if (rel != null) {
            if (rel.usesPrimaryKey()) {
                this.nmgr.evictNodeByKey(new DbKey(getDbMapping().getSubnodeMapping(), key));
            } else {
                this.nmgr.evictNodeByKey(new SyntheticKey(getKey(), key));
            }
        }
    }

    /**
     *  Get the ID of this Node. This is the primary database key and used as part of the
     *  key for the internal node cache.
     */
    public String getID() {
        // if we are transient, we generate an id on demand. It's possible that we'll never need
        // it, but if we do it's important to keep the one we have.
        if (this.state == TRANSIENT && this.id == null) {
            this.id = generateTransientID();
        }
        return this.id;
    }

    /**
     * Returns true if this node is accessed by id from its aprent, false if it
     * is accessed by name
     */
    public boolean isAnonymous() {
        return this.anonymous;
    }

    /**
     * Return this node' name, which may or may not have some meaning
     */
    public String getName() {
        return this.name;
    }

    /**
     * Get something to identify this node within a URL. This is the ID for anonymous nodes
     * and a property value for named properties.
     */
    public String getElementName() {
        // check element name - this is either the Node's id or name.
        long lastmod = this.lastmodified;

        if (this.dbmap != null) {
            lastmod = Math.max(lastmod, this.dbmap.getLastTypeChange());
        }

        if ((this.parentHandle != null) && (this.lastNameCheck <= lastmod)) {
            try {
                Node p = this.parentHandle.getNode(this.nmgr);
                DbMapping parentmap = p.getDbMapping();
                Relation prel = parentmap.getSubnodeRelation();

                if (prel != null) {
                    if (prel.groupby != null) {
                        setName(getString("groupname")); //$NON-NLS-1$
                        this.anonymous = false;
                    } else if (prel.accessName != null) {
                        String propname = this.dbmap.columnNameToProperty(prel.accessName);
                        String propvalue = getString(propname);

                        if ((propvalue != null) && (propvalue.length() > 0)) {
                            setName(propvalue);
                            this.anonymous = false;
                        } else if (!this.anonymous && p.isParentOf(this)) {
                            this.anonymous = true;
                        }
                    } else if (!this.anonymous && p.isParentOf(this)) {
                        this.anonymous = true;
                    }
                } else if (!this.anonymous && p.isParentOf(this)) {
                    this.anonymous = true;
                }
            } catch (Exception ignore) {
                // FIXME: add proper NullPointer checks in try statement
                // just fall back to default method
            }

            this.lastNameCheck = System.currentTimeMillis();
        }

        return (this.anonymous || (this.name == null) || (this.name.length() == 0)) ? this.id : this.name;
    }

    /**
     * Get the node's path
     */
    public String getPath() {
        String divider = null;
        StringBuffer b = new StringBuffer();
        NodeInterface p = this;
        int loopWatch = 0;

        while (p != null && p.getParent() != null) {
            if (divider != null) {
                b.insert(0, divider);
            } else {
                divider = "/"; //$NON-NLS-1$
            }

            b.insert(0, p.getElementName());
            p = p.getParent();

            loopWatch++;

            if (loopWatch > 10) {
                b.insert(0, "..."); //$NON-NLS-1$

                break;
            }
        }
        return b.toString();
    }

    /**
     * Return the node's prototype name.
     */
    public String getPrototype() {
        // if prototype is null, it's a vanilla HopObject.
        if (this.prototype == null) {
            return "HopObject"; //$NON-NLS-1$
        }

        return this.prototype;
    }

    /**
     * Set the node's prototype name.
     */
    public void setPrototype(String proto) {
        this.prototype = proto;
        // Note: we mustn't set the DbMapping according to the prototype,
        // because some nodes have custom dbmappings, e.g. the groupby
        // dbmappings created in DbMapping.initGroupbyMapping().
    }

    /**
     * Set the node's {@link DbMapping}.
     */
    public void setDbMapping(DbMapping dbmap) {
        this.dbmap = dbmap;
    }

    /**
     * Get the node's {@link DbMapping}.
     */
    public DbMapping getDbMapping() {
        return this.dbmap;
    }

    /**
     * Get the node's key.
     */
    public KeyInterface getKey() {
        if (this.primaryKey == null && this.state == TRANSIENT) {
            throw new RuntimeException(Messages.getString("Node.5") + this); //$NON-NLS-1$
        }

        if ((this.dbmap == null) && (this.prototype != null) && (this.nmgr != null)) {
            this.dbmap = this.nmgr.getDbMapping(this.prototype);
        }

        if (this.primaryKey == null) {
            this.primaryKey = new DbKey(this.dbmap, this.id);
        }

        return this.primaryKey;
    }

    /**
     * Get the node's handle.
     */
    public NodeHandle getHandle() {
        if (this.handle == null) {
            this.handle = new NodeHandle(this);
        }
        return this.handle;
    }

    /**
     * Set an explicit select clause for the node's subnodes
     */
    public synchronized void setSubnodeRelation(String rel) {
        if ((rel == null && this.subnodeRelation == null) ||
                (rel != null && rel.equalsIgnoreCase(this.subnodeRelation))) {
            return;
        }

        checkWriteLock();
        this.subnodeRelation = rel;

        DbMapping smap = (this.dbmap == null) ? null : this.dbmap.getSubnodeMapping();

        if (this.subnodes != null && smap != null && smap.isRelational()) {
            this.subnodes = null;
        }
    }

    /**
     * Get the node's explicit subnode select clause if one was set, or null
     */
    public synchronized String getSubnodeRelation() {
        return this.subnodeRelation;
    }

    /**
     * Set the node's name.
     */
    public void setName(String name) {
        if ((name == null) || (name.length() == 0)) {
            // use id as name
            this.name = this.id;
        } else if (name.indexOf('/') > -1) {
            // "/" is used as delimiter, so it's not a legal char
            return;
        } else {
            this.name = name;
        }
    }

    /**
     * Set this node's parent node.
     */
    public void setParent(Node parent) {
        this.parentHandle = (parent == null) ? null : parent.getHandle();
    }

    /**
     *  Set this node's parent node to the node referred to by the NodeHandle.
     */
    public void setParentHandle(NodeHandle parent) {
        this.parentHandle = parent;
    }

    /**
     * Get parent, retrieving it if necessary.
     */
    public NodeInterface getParent() {
        // check what's specified in the type.properties for this node.
        ParentInfo[] parentInfo = null;

        if (isRelational() && this.lastParentSet <= Math.max(this.dbmap.getLastTypeChange(), this.lastmodified)) {
            parentInfo = this.dbmap.getParentInfo();
        }

        // check if current parent candidate matches presciption,
        // if not, try to get one that does.
        if (this.nmgr.isRootNode(this)) {
            this.parentHandle = null;
            this.lastParentSet =  System.currentTimeMillis();
            return null;
        } else if (parentInfo != null) {

            Node parentFallback = null;

            for (int i = 0; i < parentInfo.length; i++) {

                ParentInfo pinfo = parentInfo[i];
                Node parentNode = null;

                // see if there is an explicit relation defined for this parent info
                // we only try to fetch a node if an explicit relation is specified for the prop name
                Relation rel = this.dbmap.propertyToRelation(pinfo.propname);
                if ((rel != null) && (rel.isReference() || rel.isComplexReference())) {
                    parentNode = (Node) getNode(pinfo.propname);
                }

                // the parent of this node is the app's root node...
                if ((parentNode == null) && pinfo.isroot) {
                    parentNode = this.nmgr.getRootNode();
                }

                // if we found a parent node, check if we ought to use a virtual or groupby node as parent
                if (parentNode != null) {
                    // see if dbmapping specifies anonymity for this node
                    if (pinfo.virtualnames.length > 0) {
                    	Node newParentNode = parentNode;
                    	for (int j = 0; j < pinfo.virtualnames.length; j++) {
                    		newParentNode = (Node) newParentNode.getNode(pinfo.virtualnames[j]);
                    		if (newParentNode == null) {
                                getApp().logError(Messages.getString("Node.6") + //$NON-NLS-1$
                                                       pinfo + Messages.getString("Node.7") + this); //$NON-NLS-1$
                            }
                    	}

                        parentNode = newParentNode;
                    }

                    if (parentNode != null && pinfo.collectionname != null) {
            			parentNode = (Node) parentNode.getNode(pinfo.collectionname);
                    }

                    DbMapping dbm = (parentNode == null) ? null : parentNode.getDbMapping();

                    try {
                        if ((dbm != null) && (dbm.getSubnodeGroupby() != null)) {
                            // check for groupby
                            rel = this.dbmap.columnNameToRelation(dbm.getSubnodeGroupby());
                            parentNode = (Node) parentNode.getChildElement(getString(rel.propName));
                        }

                        // check if parent actually contains this node. If it does,
                        // accept it immediately, otherwise, keep it as fallback in case
                        // no other parent matches. See http://helma.org/bugs/show_bug.cgi?id=593
                        if (parentNode != null) {
                            if (parentNode.isParentOf(this)) {
                                this.parentHandle = parentNode.getHandle();
                                this.lastParentSet = System.currentTimeMillis();
                                return parentNode;
                            } else if (parentFallback == null) {
                                parentFallback = parentNode;
                            }
                        }
                    } catch (Exception x) {
                        getApp().logError(Messages.getString("Node.8") + //$NON-NLS-1$
                                                   pinfo + Messages.getString("Node.9") + this, x); //$NON-NLS-1$
                    }
                }
            }
            this.lastParentSet = System.currentTimeMillis();
            // if we came till here and we didn't find a parent.
            // set parent to null unless we have a fallback.
            if (parentFallback != null) {
                this.parentHandle = parentFallback.getHandle();
                return parentFallback;
            }
            this.parentHandle = null;
            if (this.state != TRANSIENT) {
                getApp().logEvent(Messages.getString("Node.10") + this + //$NON-NLS-1$
                        Messages.getString("Node.11")); //$NON-NLS-1$
            }
            return null;
        }

        return this.parentHandle == null ? null : this.parentHandle.getNode(this.nmgr);
    }

    /**
     * Get parent, using cached info if it exists.
     */
    public Node getCachedParent() {
        if (this.parentHandle == null) {
            return null;
        }

        return this.parentHandle.getNode(this.nmgr);
    }

    /**
     *  NodeInterface-related
     */
    public NodeInterface addNode(NodeInterface elem) {
        return addNode(elem, -1);
    }

    /**
     * Add a node to this Node's subnodes, making the added node persistent if it
     * hasn't been before and this Node is already persistent.
     *
     * @param elem the node to add to this Nodes subnode-list
     * @param where the index-position where this node has to be added
     *
     * @return the added node itselve
     */
    public NodeInterface addNode(NodeInterface elem, int where) {
        Node node = null;

        if (elem instanceof Node) {
            node = (Node) elem;
        } else {
            throw new RuntimeException(Messages.getString("Node.12")); //$NON-NLS-1$
        }

        // only lock nodes if parent node is not transient
        if (this.state != TRANSIENT) {
            // only lock parent if it has to be modified for a change in subnodes
            if (!ignoreSubnodeChange()) {
                checkWriteLock();
            }
            node.checkWriteLock();
        }

        Relation subrel = this.dbmap == null ? null : this.dbmap.getSubnodeRelation();
        // if subnodes are defined via relation, make sure its constraints are enforced.
        if (subrel != null && (subrel.countConstraints() < 2 || this.state != TRANSIENT)) {
            subrel.setConstraints(this, node);
        }

        // if the new node is marked as TRANSIENT and this node is not, mark new node as NEW
        if (this.state != TRANSIENT && node.state == TRANSIENT) {
            node.makePersistable();
        }

        // only mark this node as modified if subnodes are not in relational db
        // pointing to this node.
        if (!ignoreSubnodeChange() && (this.state == CLEAN || this.state == DELETED)) {
            markAs(MODIFIED);
        }

        // TODO this is a rather minimal fix for bug http://helma.org/bugs/show_bug.cgi?id=554
        // - eventually we want to get rid of this code as a whole.
        if (this.state != TRANSIENT && (node.state == CLEAN || node.state == DELETED)) {
            node.markAs(MODIFIED);
        }

        loadNodes();

        if (subrel != null && subrel.groupby != null) {
            // check if this node has a group-by subnode-relation
            Node groupbyNode = getGroupbySubnode(node, true);
            if (groupbyNode != null) {
                groupbyNode.addNode(node);
                return node;
            }
        }

        NodeHandle nhandle = node.getHandle();

        if (this.subnodes != null && this.subnodes.contains(nhandle)) {
            // Node is already subnode of this - just move to new position
            synchronized (this.subnodes) {
                this.subnodes.remove(nhandle);
                // check if index is out of bounds when adding
                if (where < 0 || where > this.subnodes.size()) {
                    this.subnodes.add(nhandle);
                } else {
                    this.subnodes.add(where, nhandle);
                }
            }
        } else {
            // create subnode list if necessary
            if (this.subnodes == null) {
                this.subnodes = createSubnodeList();
            }

            // check if subnode accessname is set. If so, check if another node
            // uses the same access name, throwing an exception if so.
            if (this.dbmap != null && node.dbmap != null) {
                Relation prel = this.dbmap.getSubnodeRelation();

                if (prel != null && prel.accessName != null) {
                    Relation localrel = node.dbmap.columnNameToRelation(prel.accessName);

                    // if no relation from db column to prop name is found,
                    // assume that both are equal
                    String propname = (localrel == null) ? prel.accessName
                                                         : localrel.propName;
                    String prop = node.getString(propname);

                    if (prop != null && prop.length() > 0) {
                        NodeInterface old = (NodeInterface) getChildElement(prop);

                        if (old != null && old != node) {
                            // A node with this name already exists. This is a
                            // programming error, throw an exception.
                            throw new RuntimeException(Messages.getString("Node.13") + prop + //$NON-NLS-1$
                                Messages.getString("Node.14")); //$NON-NLS-1$
                        }

                        if (this.state != TRANSIENT) {
                            Transactor tx = Transactor.getInstanceOrFail();
                            SyntheticKey key = new SyntheticKey(this.getKey(), prop);
                            tx.visitCleanNode(key, node);
                            this.nmgr.registerNode(node, key);
                        }
                    }
                }
            }

            // actually add the new child to the subnode list
            synchronized (this.subnodes) {
                // check if index is out of bounds when adding
                if (where < 0 || where > this.subnodes.size()) {
                    this.subnodes.add(nhandle);
                } else {
                    this.subnodes.add(where, nhandle);
                }
            }

            if (node != this && !this.nmgr.isRootNode(node)) {
                // avoid calling getParent() because it would return bogus results
                // for the not-anymore transient node
                Node nparent = (node.parentHandle == null) ? null
                                                           : node.parentHandle.getNode(this.nmgr);

                // if the node doesn't have a parent yet, or it has one but it's
                // transient while we are persistent, make this the nodes new parent.
                if ((nparent == null) ||
                        ((this.state != TRANSIENT) && (nparent.getState() == TRANSIENT))) {
                    node.setParent(this);
                    node.anonymous = true;
                }
            }
        }

        this.lastmodified = System.currentTimeMillis();
        // we want the element name to be recomputed on the child node
        node.lastNameCheck = 0;
        registerSubnodeChange();

        return node;
    }

    /**
     *
     *
     * @return ...
     */
    public NodeInterface createNode() {
        // create new node at end of subnode array
        return createNode(null, -1);
    }

    /**
     *
     *
     * @param where ...
     *
     * @return ...
     */
    public NodeInterface createNode(int where) {
        return createNode(null, where);
    }

    /**
     *
     *
     * @param nm ...
     *
     * @return ...
     */
    public NodeInterface createNode(String nm) {
        // parameter where is  ignored if nm != null so we try to avoid calling numberOfNodes()
        return createNode(nm, -1);
    }

    /**
     *
     *
     * @param nm ...
     * @param where ...
     *
     * @return ...
     */
    public NodeInterface createNode(String nm, int where) {
        // checkWriteLock();

        boolean anon = false;

        if ((nm == null) || "".equals(nm.trim())) { //$NON-NLS-1$
            anon = true;
        }

        String proto = null;

        // try to get proper prototype for new node
        if (this.dbmap != null) {
            DbMapping childmap = anon ?
                this.dbmap.getSubnodeMapping() :
                this.dbmap.getPropertyMapping(nm);
            if (childmap != null) {
                proto = childmap.getTypeName();
            }
        }

        Node n = new Node(nm, proto, this.nmgr);

        if (anon) {
            addNode(n, where);
        } else {
            setNode(nm, n);
        }

        return n;
    }


    /**
     * This implements the getChildElement() method of the PathElementInterface interface
     */
    public PathElementInterface getChildElement(String name) {
        if (this.dbmap != null) {
            // if a dbmapping is provided, check what it tells us about
            // getting this specific child element
            Relation rel = this.dbmap.getExactPropertyRelation(name);

            if (rel != null && !rel.isPrimitive()) {
                return getNode(name);
            }

            rel = this.dbmap.getSubnodeRelation();

            if ((rel != null) && (rel.groupby != null || rel.accessName != null)) {
                if (this.state != TRANSIENT && rel.otherType != null && rel.otherType.isRelational()) {
                    return this.nmgr.getNode(this, name, rel);
                }
                // Do what we have to do: loop through subnodes and
                // check if any one matches
                String propname = rel.groupby != null ? "groupname" : rel.accessName; //$NON-NLS-1$
                NodeInterface node = null;
                Enumeration e = getSubnodes();
                while (e.hasMoreElements()) {
                    Node n = (Node) e.nextElement();
                    if (name.equalsIgnoreCase(n.getString(propname))) {
                        node = n;
                        break;
                    }
                }
                // set DbMapping for embedded db group nodes
                if (node != null && rel.groupby != null) {
                     node.setDbMapping(this.dbmap.getGroupbyMapping());
                }
                return node;
            }

            return getSubnode(name);
        }
        // no dbmapping - just try child collection first, then named property.
        NodeInterface child = getSubnode(name);

        if (child == null) {
            child = getNode(name);
        }

        return child;
    }

    /**
     * This implements the getParentElement() method of the PathElementInterface interface
     */
    public PathElementInterface getParentElement() {
        return getParent();
    }

    /**
     * Get a named child node with the given id.
     */
    public NodeInterface getSubnode(String subid) {
        if (subid == null || subid.length() == 0) {
            return null;
        }

        Node retval = null;
        loadNodes();
        if (this.subnodes == null || this.subnodes.size() == 0) {
            return null;
        }

        NodeHandle nhandle = null;
        int l = this.subnodes.size();

        for (int i = 0; i < l; i++) {
            try {
                NodeHandle shandle = this.subnodes.get(i);
                if (subid.equals(shandle.getID())) {
                    nhandle = shandle;
                    break;
                }
            } catch (Exception x) {
                break;
            }
        }

        if (nhandle != null) {
            retval = nhandle.getNode(this.nmgr);
        }

        // This would be a better way to do it, without loading the subnodes,
        // but it currently isn't supported by NodeManager.
        //    if (dbmap != null && dbmap.getSubnodeRelation () != null)
        //         retval = nmgr.getNode (this, subid, dbmap.getSubnodeRelation ());

        if (retval != null && retval.parentHandle == null && !this.nmgr.isRootNode(retval)) {
            retval.setParent(this);
            retval.anonymous = true;
        }

        return retval;
    }

    /**
     * Get a node at a given position. This causes the subnode list to be loaded in case
     * it isn't up to date.
     * @param index the subnode index
     * @return the node at the given index
     */
    public NodeInterface getSubnodeAt(int index) {
        loadNodes();
        if (this.subnodes == null) {
            return null;
        }
        return this.subnodes.getNode(index);
    }

    /**
     * Get or create a group name for a given content node.
     *
     * @param node the content node
     * @param create whether the node should be created if it doesn't exist
     * @return the group node, or null
     */
    protected Node getGroupbySubnode(Node node, boolean create) {
        if (node.dbmap != null && node.dbmap.isGroup()) {
            return null;
        }

        if (this.dbmap != null) {
            Relation subrel = this.dbmap.getSubnodeRelation();

            if (subrel != null && subrel.groupby != null) {
                // use actual child mapping to resolve group property name,
                // otherwise the subnode mapping defined for the collection.
                DbMapping childmap = node.dbmap == null ? subrel.otherType : node.dbmap;
                Relation grouprel = childmap.columnNameToRelation(subrel.groupby);
                // If group name can't be resolved to a property name use the group name itself
                String groupprop = (grouprel != null) ? grouprel.propName : subrel.groupby;
                String groupname = node.getString(groupprop);
                Node groupbyNode = (Node) getChildElement(groupname);

                // if group-by node doesn't exist, we'll create it
                if (groupbyNode == null) {
                    groupbyNode = getGroupbySubnode(groupname, create);
                    // mark subnodes as changed as we have a new group node
                    if (create && groupbyNode != null) {
                        Transactor.getInstance().visitParentNode(this);
                    }
                } else {
                    groupbyNode.setDbMapping(this.dbmap.getGroupbyMapping());
                }

                return groupbyNode;
            }
        }
        return null;
    }

    /**
     * Get or create a group name for a given group name.
     *
     * @param groupname the group name
     * @param create whether the node should be created if it doesn't exist
     * @return the group node, or null
     */
    protected Node getGroupbySubnode(String groupname, boolean create) {
        if (groupname == null) {
            throw new IllegalArgumentException(Messages.getString("Node.15")); //$NON-NLS-1$
        }

        boolean persistent = this.state != TRANSIENT;

        loadNodes();

        if (this.subnodes == null) {
            this.subnodes = new SubnodeList(this);
        }

        if (create || this.subnodes.contains(new NodeHandle(new SyntheticKey(getKey(), groupname)))) {
            try {
                DbMapping groupbyMapping = this.dbmap.getGroupbyMapping();
                boolean relational = groupbyMapping.getSubnodeMapping().isRelational();

                if (relational || create) {
                    Node node;
                    if (relational && persistent) {
                        node = new Node(this, groupname, this.nmgr, null);
                    } else {
                        node = new Node(groupname, null, this.nmgr);
                        node.setParent(this);
                    }

                    // set "groupname" property to value of groupby field
                    node.setString("groupname", groupname); //$NON-NLS-1$
                    // Set the dbmapping on the group node
                    node.setDbMapping(groupbyMapping);
                    node.setPrototype(groupbyMapping.getTypeName());

                    // if we're relational and persistent, make new node persistable
                    if (!relational && persistent) {
                        node.makePersistable();
                        node.checkWriteLock();
                    }

                    // if we created a new node, check if we need to add it to subnodes
                    if (create) {
                        NodeHandle handle = node.getHandle();
                        if (!this.subnodes.contains(handle))
                            this.subnodes.add(handle);
                    }

                    // If we created the group node, we register it with the
                    // nodemanager. Otherwise, we just evict whatever was there before
                    if (persistent) {
                        if (create) {
                            // register group node with transactor
                            Transactor tx = Transactor.getInstanceOrFail();
                            tx.visitCleanNode(node);
                            this.nmgr.registerNode(node);
                        } else {
                            this.nmgr.evictKey(node.getKey());
                        }
                    }

                    return node;
                }
            } catch (Exception noluck) {
                this.nmgr.nmgr.app.logError(Messages.getString("Node.16") + groupname, noluck); //$NON-NLS-1$
            }
        }

        return null;
    }

    /**
     *
     *
     * @return ...
     */
    public boolean remove() {
        NodeInterface parent = getParent();
        if (parent != null) {
            try {
                parent.removeNode(this);
            } catch (Exception x) {
                // couldn't remove from parent. Log and continue
                getApp().logError(Messages.getString("Node.17") + x); //$NON-NLS-1$
            }
        }
        deepRemoveNode();
        return true;
    }

    /**
     *
     *
     * @param node ...
     */
    public void removeNode(NodeInterface node) {
        Node n = (Node) node;
        releaseNode(n);
    }

    /**
     * "Locally" remove a subnode from the subnodes table.
     * The logical stuff necessary for keeping data consistent is done in
     * {@link #removeNode(NodeInterface)}.
     */
    protected void releaseNode(Node node) {

        Node groupNode = getGroupbySubnode(node, false);
        if (groupNode != null) {
            groupNode.releaseNode(node);
            return;
        }

        NodeInterface parent = node.getParent();

        checkWriteLock();
        node.checkWriteLock();

        // load subnodes in case they haven't been loaded.
        // this is to prevent subsequent access to reload the
        // index which would potentially still contain the removed child
        loadNodes();

        if (this.subnodes != null) {
            boolean removed = false;
            synchronized (this.subnodes) {
                removed = this.subnodes.remove(node.getHandle());
            }
            if (this.dbmap != null && this.dbmap.isGroup() && this.subnodes.size() == 0) {
                // clean up ourself if we're an empty group node
                remove();
            } else if (removed) {
                registerSubnodeChange();
            }
        }


        // check if subnodes are also accessed as properties. If so, also unset the property
        if (this.dbmap != null && node.dbmap != null) {
            Relation prel = this.dbmap.getSubnodeRelation();

            if (prel != null) {
                if (prel.accessName != null) {
                    Relation localrel = node.dbmap.columnNameToRelation(prel.accessName);

                    // if no relation from db column to prop name is found, assume that both are equal
                    String propname = (localrel == null) ? prel.accessName : localrel.propName;
                    String prop = node.getString(propname);

                    if (prop != null) {
                        if (getNode(prop) == node) {
                            unset(prop);
                        }
                        // let the node cache know this key's not for this node anymore.
                        if (this.state != TRANSIENT) {
                            this.nmgr.evictKey(new SyntheticKey(getKey(), prop));
                        }
                    }
                } else if (prel.groupby != null) {
                    String prop = node.getString("groupname"); //$NON-NLS-1$
                    if (prop != null && this.state != TRANSIENT) {
                        this.nmgr.evictKey(new SyntheticKey(getKey(), prop));
                    }

                }
                // TODO: We should unset constraints to actually remove subnodes here,
                // but omit it by convention and to keep backwards compatible.
                // if (prel.countConstraints() > 1) {
                //    prel.unsetConstraints(this, node);
                // }
            }
        }

        if (parent == this) {
            // node.markAs(MODIFIED);
            node.setParentHandle(null);
        }

        // If subnodes are relational no need to mark this node as modified
        if (ignoreSubnodeChange()) {
            return;
        }

        this.lastmodified = System.currentTimeMillis();

        if (this.state == CLEAN) {
            markAs(MODIFIED);
        }
    }

    /**
     * Delete the node from the db. This mainly tries to notify all nodes referring to this that
     * it's going away. For nodes from the embedded db it also does a cascading delete, since
     * it can tell which nodes are actual children and which are just linked in.
     */
    protected void deepRemoveNode() {

        // tell all nodes that are properties of n that they are no longer used as such
        if (this.propMap != null) {
            for (Enumeration en = this.propMap.elements(); en.hasMoreElements();) {
                Property p = (Property) en.nextElement();

                if ((p != null) && (p.getType() == PropertyInterface.NODE)) {
                    Node n = (Node) p.getNodeValue();
                    if (n != null && !n.isRelational() && n.getParent() == this) {
                        n.deepRemoveNode();
                    }
                }
            }
        }

        // cascading delete of all subnodes. This is never done for relational subnodes, because
        // the parent info is not 100% accurate for them.
        if (this.subnodes != null) {
            Vector v = new Vector();

            // remove modifies the Vector we are enumerating, so we are extra careful.
            for (Enumeration en = getSubnodes(); en.hasMoreElements();) {
                v.add(en.nextElement());
            }

            int m = v.size();

            for (int i = 0; i < m; i++) {
                // getParent() is heuristical/implicit for relational nodes, so we don't base
                // a cascading delete on that criterium for relational nodes.
                Node n = (Node) v.get(i);

                if (!n.isRelational() && n.getParent() == this) {
                    n.deepRemoveNode();
                }
            }
        }

        // mark the node as deleted and evict its primary key
        setParent(null);
        if (this.primaryKey != null || this.state != TRANSIENT) {
            this.nmgr.evictKey(getKey());
        }
        markAs(DELETED);
    }

    /**
     * Check if the given node is contained in this node's child list.
     * If it is contained return its index in the list, otherwise return -1.
     *
     * @param n a node
     *
     * @return the node's index position in the child list, or -1
     */
    public int contains(NodeInterface n) {
        if (n == null) {
            return -1;
        }

        loadNodes();

        if (this.subnodes == null) {
            return -1;
        }

        // if the node contains relational groupby subnodes, the subnodes vector
        // contains the names instead of ids.
        if (!(n instanceof Node)) {
            return -1;
        }

        Node node = (Node) n;

        return this.subnodes.indexOf(node.getHandle());
    }

    /**
     * Check if the given node is contained in this node's child list. This
     * is similar to <code>contains(NodeInterface)</code> but does not load the
     * child index for relational nodes.
     *
     * @param n a node
     * @return true if the given node is contained in this node's child list
     */
    public boolean isParentOf(Node n) {
        if (this.dbmap != null) {
            Relation subrel = this.dbmap.getSubnodeRelation();
            // if we're dealing with relational child nodes use
            // Relation.checkConstraints to avoid loading the child index.
            // Note that we only do that if no filter is set, since
            // Relation.checkConstraints() would always return false
            // if there was a filter property.
            if (subrel != null && subrel.otherType != null
                               && subrel.otherType.isRelational()
                               && subrel.filter == null) {
                // first check if types are stored in same table
                if (!subrel.otherType.isStorageCompatible(n.getDbMapping())) {
                    return false;
                }
                // if they are, check if constraints are met
                return subrel.checkConstraints(this, n);
            }
        }
        // just fall back to contains() for non-relational nodes
        return contains(n) > -1;
    }

    /**
     * Count the subnodes of this node. If they're stored in a relational data source, we
     * may actually load their IDs in order to do this.
     */
    public int numberOfNodes() {
        loadNodes();
        return (this.subnodes == null) ? 0 : this.subnodes.size();
    }

    /**
     * Make sure the subnode index is loaded for subnodes stored in a relational data source.
     *  Depending on the subnode.loadmode specified in the type.properties, we'll load just the
     *  ID index or the actual nodes.
     */
    public void loadNodes() {
        // Don't do this for transient nodes which don't have an explicit subnode relation set
        if ((this.state == TRANSIENT || this.state == NEW) && this.subnodeRelation == null) {
            return;
        }

        DbMapping subMap = (this.dbmap == null) ? null : this.dbmap.getSubnodeMapping();

        if (subMap != null && subMap.isRelational()) {
            // check if subnodes need to be reloaded
            synchronized (this) {
                if (this.subnodes == null) {
                    createSubnodeList();
                }
                this.subnodes.update();
            }
        }
    }

    /**
     * Create an empty subnode list.
     * @return List an empty List of the type used by this Node
     */
    public SubnodeList createSubnodeList() {
        Relation subrel = this.dbmap == null ? null : this.dbmap.getSubnodeRelation();
        this.subnodes = subrel == null || !subrel.lazyLoading ?
                new SubnodeList(this) : new SegmentedSubnodeList(this);
        return this.subnodes;
    }

    /**
     * Compute a serial number indicating the last change in subnode collection
     * @return a serial number that increases with each subnode change
     */
    long getLastSubnodeChange() {
        // TODO check if we should compute this on demand
        if (this.subnodes == null) {
            createSubnodeList();
        }
        return this.subnodes.getLastSubnodeChange();
    }

    /**
     *
     *
     * @param startIndex ...
     * @param length ...
     *
     * @throws Exception ...
     */
    public void prefetchChildren(int startIndex, int length) {
        if (startIndex < 0) {
            return;
        }

        loadNodes();

        if (this.subnodes == null || startIndex >= this.subnodes.size()) {
            return;
        }

        this.subnodes.prefetch(startIndex, length);
    }

    /**
     * Enumerate through the subnodes of this node.
     * @return an enumeration of this node's subnodes
     */
    public Enumeration getSubnodes() {
        loadNodes();
        return getLoadedSubnodes();
    }

    private Enumeration getLoadedSubnodes() {
        final SubnodeList list = this.subnodes;
        if (list == null) {
            return Collections.enumeration(Collections.EMPTY_LIST);
        }

        return new Enumeration() {
            int pos = 0;

            public boolean hasMoreElements() {
                return this.pos < list.size();
            }

            public Object nextElement() {
                // prefetch in batches of 100
                // if (pos % 100 == 0)
                //     list.prefetch(pos, 100);
                return list.getNode(this.pos++);
            }
        };
    }

    /**
     * Return this Node's subnode list
     *
     * @return the subnode list
     */
    public SubnodeList getSubnodeList() {
        return this.subnodes;
    }

   /**
    * Return true if a change in subnodes can be ignored because it is
    * stored in the subnodes themselves.
    */
    private boolean ignoreSubnodeChange() {
        Relation rel = (this.dbmap == null) ? null : this.dbmap.getSubnodeRelation();

        return ((rel != null) && (rel.otherType != null) && rel.otherType.isRelational());
    }

    /**
     *  Get all properties of this node.
     */
    public Enumeration properties() {
        if ((this.dbmap != null) && this.dbmap.isRelational()) {
            // return the properties defined in type.properties, if there are any
            return this.dbmap.getPropertyEnumeration();
        }

        Relation prel = (this.dbmap == null) ? null : this.dbmap.getSubnodeRelation();

        if (this.state != TRANSIENT && prel != null && prel.hasAccessName() &&
                prel.otherType != null && prel.otherType.isRelational()) {
            // return names of objects from a relational db table
            return this.nmgr.getPropertyNames(this, prel).elements();
        } else if (this.propMap != null) {
            // return the actually explicitly stored properties
            return this.propMap.keys();
        }

        // sorry, no properties for this Node
        return Collections.enumeration(Collections.EMPTY_LIST);
    }

    /**
     *
     *
     * @return ...
     */
    public Hashtable getPropMap() {
        return this.propMap;
    }

    /**
     *
     *
     * @param propname ...
     *
     * @return ...
     */
    public PropertyInterface get(String propname) {
        return getProperty(propname);
    }

    /**
     *
     *
     * @return ...
     */
    public String getParentInfo() {
        return "anonymous:" + this.anonymous + ",parentHandle" + this.parentHandle + ",parent:" +  //$NON-NLS-1$ //$NON-NLS-2$//$NON-NLS-3$
               getParent();
    }

    /**
     *
     *
     * @param propname ...
     *
     * @return ...
     */
    protected Property getProperty(String propname) {
        if (propname == null) {
            return null;
        }

        Relation rel = this.dbmap == null ?
                null : this.dbmap.getExactPropertyRelation(propname);

        // 1) check if the property is contained in the propMap
        Property prop = this.propMap == null ? null : (Property) this.propMap.get(this.correctPropertyName(propname));

        if (prop != null) {
            if (rel != null) {
                // Is a relational node stored by id but things it's a string or int. Fix it.
                if (rel.otherType != null && prop.getType() != PropertyInterface.NODE) {
                    prop.convertToNodeReference(rel);
                }
                if (rel.isVirtual()) {
                    // property was found in propMap and is a collection - this is
                    // a collection holding non-relational objects. set DbMapping and
                    // NodeManager
                    Node n = (Node) prop.getNodeValue();
                    if (n != null) {
                        // do set DbMapping for embedded db collection nodes
                        n.setDbMapping(rel.getVirtualMapping());
                    }
                }
            }
            return prop;
        } else if (this.state == TRANSIENT && rel != null && rel.isVirtual()) {
            // When we get a collection from a transient node for the first time, or when
            // we get a collection whose content objects are stored in the embedded
            // XML data storage, we just want to create and set a generic node without
            // consulting the NodeManager about it.
            Node n = new Node(propname, rel.getPrototype(), this.nmgr);
            n.setDbMapping(rel.getVirtualMapping());
            n.setParent(this);
            setNode(propname, n);
            return (Property) this.propMap.get(this.correctPropertyName(propname));
        }

        // 2) check if this is a create-on-demand node property
        if (rel != null && (rel.isVirtual() || rel.isComplexReference())) {
            if (this.state != TRANSIENT) {
                Node n = this.nmgr.getNode(this, propname, rel);

                if (n != null) {
                    if ((n.parentHandle == null) &&
                            !this.nmgr.isRootNode(n)) {
                        n.setParent(this);
                        n.name = propname;
                        n.anonymous = false;
                    }
                    return new Property(propname, this, n);
                }
            }
        }

        // 4) nothing to be found - return null
        return null;
    }

    /**
     *
     *
     * @param propname ...
     *
     * @return ...
     */
    public String getString(String propname) {
        Property prop = getProperty(propname);

        try {
            return prop.getStringValue();
        } catch (Exception ignore) {
        }

        return null;
    }

    /**
     *
     *
     * @param propname ...
     *
     * @return ...
     */
    public long getInteger(String propname) {
        Property prop = getProperty(propname);

        try {
            return prop.getIntegerValue();
        } catch (Exception ignore) {
        }

        return 0;
    }

    /**
     *
     *
     * @param propname ...
     *
     * @return ...
     */
    public double getFloat(String propname) {
        Property prop = getProperty(propname);

        try {
            return prop.getFloatValue();
        } catch (Exception ignore) {
        }

        return 0.0;
    }

    /**
     *
     *
     * @param propname ...
     *
     * @return ...
     */
    public Date getDate(String propname) {
        Property prop = getProperty(propname);

        try {
            return prop.getDateValue();
        } catch (Exception ignore) {
        }

        return null;
    }

    /**
     *
     *
     * @param propname ...
     *
     * @return ...
     */
    public boolean getBoolean(String propname) {
        Property prop = getProperty(propname);

        try {
            return prop.getBooleanValue();
        } catch (Exception ignore) {
        }

        return false;
    }

    /**
     *
     *
     * @param propname ...
     *
     * @return ...
     */
    public NodeInterface getNode(String propname) {
        Property prop = getProperty(propname);

        try {
            return prop.getNodeValue();
        } catch (Exception ignore) {
        }

        return null;
    }

    /**
     *
     *
     * @param propname ...
     *
     * @return ...
     */
    public Object getJavaObject(String propname) {
        Property prop = getProperty(propname);

        try {
            return prop.getJavaObjectValue();
        } catch (Exception ignore) {
        }

        return null;
    }

    /**
     * Directly set a property on this node
     *
     * @param propname ...
     * @param value ...
     */
    protected void set(String propname, Object value, int type) {
        boolean isPersistable = this.state != TRANSIENT && isPersistableProperty(propname);
        if (isPersistable) {
            checkWriteLock();
        }

        if (this.propMap == null) {
            this.propMap = new Hashtable();
        }

        propname = propname.trim();
        String p2 = this.correctPropertyName(propname);
        Property prop = (Property) this.propMap.get(p2);

        if (prop != null) {
            prop.setValue(value, type);
        } else {
            prop = new Property(propname, this);
            prop.setValue(value, type);
            this.propMap.put(p2, prop);
        }

        this.lastmodified = System.currentTimeMillis();

        if (this.state == CLEAN && isPersistable) {
            markAs(MODIFIED);
        }
    }

    /**
     *
     *
     * @param propname ...
     * @param value ...
     */
    public void setString(String propname, String value) {
        // nmgr.logEvent ("setting String prop");
        boolean isPersistable = this.state != TRANSIENT && isPersistableProperty(propname);
        if (isPersistable) {
            checkWriteLock();
        }

        if (this.propMap == null) {
            this.propMap = new Hashtable();
        }

        propname = propname.trim();
        String p2 = this.correctPropertyName(propname);
        Property prop = (Property) this.propMap.get(p2);
        String oldvalue = null;

        if (prop != null) {
            oldvalue = prop.getStringValue();

            // check if the value has changed
            if ((value != null) && value.equals(oldvalue)) {
                return;
            }

            prop.setStringValue(value);
        } else {
            prop = new Property(propname, this);
            prop.setStringValue(value);
            this.propMap.put(p2, prop);
        }

        if (this.dbmap != null) {

            // check if this may have an effect on the node's parerent's child collection
            // in combination with the accessname or order field.
            Node parent = (this.parentHandle == null) ? null : (Node) getParent();

            if ((parent != null) && (parent.getDbMapping() != null)) {
                DbMapping parentmap = parent.getDbMapping();
                Relation subrel = parentmap.getSubnodeRelation();
                String dbcolumn = this.dbmap.propertyToColumnName(propname);

                if (subrel != null && dbcolumn != null) {
                    // inlined version of notifyPropertyChange();
                    if (subrel.order != null && subrel.order.indexOf(dbcolumn) > -1) {
                        parent.registerSubnodeChange();
                    }
                    // check if accessname has changed
                    if (subrel.accessName != null &&
                            subrel.accessName.equals(dbcolumn)) {
                        // if any other node is contained with the new value, remove it
                        NodeInterface n = (NodeInterface) parent.getChildElement(value);

                        if ((n != null) && (n != this)) {
                            throw new RuntimeException(this +
                                    Messages.getString("Node.18") + value); //$NON-NLS-1$
                        }

                        // check if this node is already registered with the old name;
                        // if so, remove it, then add again with the new acessname
                        if (oldvalue != null) {
                            n = (NodeInterface) parent.getChildElement(oldvalue);

                            if (n == this) {
                                parent.unset(oldvalue);
                                parent.addNode(this);

                                // let the node cache know this key's not for this node anymore.
                                this.nmgr.evictKey(new SyntheticKey(parent.getKey(), oldvalue));
                            }
                        }

                        setName(value);
                    }
                }
            }

            // check if the property we're setting specifies the prototype of this object.
            if (this.state != TRANSIENT &&
                    propname.equals(this.dbmap.columnNameToProperty(this.dbmap.getPrototypeField()))) {
                DbMapping newmap = this.nmgr.getDbMapping(value);

                if (newmap != null) {
                    // see if old and new prototypes have same storage - otherwise type change is ignored
                    String oldStorage = this.dbmap.getStorageTypeName();
                    String newStorage = newmap.getStorageTypeName();

                    if (((oldStorage == null) && (newStorage == null)) ||
                            ((oldStorage != null) && oldStorage.equals(newStorage))) {
                        // long now = System.currentTimeMillis();
                        this.dbmap.setLastDataChange();
                        newmap.setLastDataChange();
                        this.dbmap = newmap;
                        this.prototype = value;
                    }
                }
            }
        }

        this.lastmodified = System.currentTimeMillis();

        if (this.state == CLEAN && isPersistable) {
            markAs(MODIFIED);
        }
    }

    /**
     *
     *
     * @param propname ...
     * @param value ...
     */
    public void setInteger(String propname, long value) {
        // nmgr.logEvent ("setting bool prop");
        boolean isPersistable = this.state != TRANSIENT && isPersistableProperty(propname);
        if (isPersistable) {
            checkWriteLock();
        }

        if (this.propMap == null) {
            this.propMap = new Hashtable();
        }

        propname = propname.trim();
        String p2 = this.correctPropertyName(propname);
        Property prop = (Property) this.propMap.get(p2);

        if (prop != null) {
            prop.setIntegerValue(value);
        } else {
            prop = new Property(propname, this);
            prop.setIntegerValue(value);
            this.propMap.put(p2, prop);
        }

        notifyPropertyChange(propname);

        this.lastmodified = System.currentTimeMillis();

        if (this.state == CLEAN && isPersistable) {
            markAs(MODIFIED);
        }
    }

    /**
     *
     *
     * @param propname ...
     * @param value ...
     */
    public void setFloat(String propname, double value) {
        // nmgr.logEvent ("setting bool prop");
        boolean isPersistable = this.state != TRANSIENT && isPersistableProperty(propname);
        if (isPersistable) {
            checkWriteLock();
        }

        if (this.propMap == null) {
            this.propMap = new Hashtable();
        }

        propname = propname.trim();
        String p2 = this.correctPropertyName(propname);
        Property prop = (Property) this.propMap.get(p2);

        if (prop != null) {
            prop.setFloatValue(value);
        } else {
            prop = new Property(propname, this);
            prop.setFloatValue(value);
            this.propMap.put(p2, prop);
        }

        notifyPropertyChange(propname);

        this.lastmodified = System.currentTimeMillis();

        if (this.state == CLEAN && isPersistable) {
            markAs(MODIFIED);
        }
    }

    /**
     *
     *
     * @param propname ...
     * @param value ...
     */
    public void setBoolean(String propname, boolean value) {
        // nmgr.logEvent ("setting bool prop");
        boolean isPersistable = this.state != TRANSIENT && isPersistableProperty(propname);
        if (isPersistable) {
            checkWriteLock();
        }

        if (this.propMap == null) {
            this.propMap = new Hashtable();
        }

        propname = propname.trim();
        String p2 = this.correctPropertyName(propname);
        Property prop = (Property) this.propMap.get(p2);

        if (prop != null) {
            prop.setBooleanValue(value);
        } else {
            prop = new Property(propname, this);
            prop.setBooleanValue(value);
            this.propMap.put(p2, prop);
        }

        notifyPropertyChange(propname);

        this.lastmodified = System.currentTimeMillis();

        if (this.state == CLEAN && isPersistable) {
            markAs(MODIFIED);
        }
    }

    /**
     *
     *
     * @param propname ...
     * @param value ...
     */
    public void setDate(String propname, Date value) {
        // nmgr.logEvent ("setting date prop");
        boolean isPersistable = this.state != TRANSIENT && isPersistableProperty(propname);
        if (isPersistable) {
            checkWriteLock();
        }

        if (this.propMap == null) {
            this.propMap = new Hashtable();
        }

        propname = propname.trim();
        String p2 = this.correctPropertyName(propname);
        Property prop = (Property) this.propMap.get(p2);

        if (prop != null) {
            prop.setDateValue(value);
        } else {
            prop = new Property(propname, this);
            prop.setDateValue(value);
            this.propMap.put(p2, prop);
        }

        notifyPropertyChange(propname);

        this.lastmodified = System.currentTimeMillis();

        if (this.state == CLEAN && isPersistable) {
            markAs(MODIFIED);
        }
    }

    /**
     *
     *
     * @param propname ...
     * @param value ...
     */
    public void setJavaObject(String propname, Object value) {
        // nmgr.logEvent ("setting jobject prop");
        boolean isPersistable = this.state != TRANSIENT && isPersistableProperty(propname);
        if (isPersistable) {
            checkWriteLock();
        }

        if (this.propMap == null) {
            this.propMap = new Hashtable();
        }

        propname = propname.trim();
        String p2 = this.correctPropertyName(propname);
        Property prop = (Property) this.propMap.get(p2);

        if (prop != null) {
            prop.setJavaObjectValue(value);
        } else {
            prop = new Property(propname, this);
            prop.setJavaObjectValue(value);
            this.propMap.put(p2, prop);
        }

        notifyPropertyChange(propname);

        this.lastmodified = System.currentTimeMillis();

        if (this.state == CLEAN && isPersistable) {
            markAs(MODIFIED);
        }
    }

    /**
     *
     *
     * @param propname ...
     * @param value ...
     */
    public void setNode(String propname, NodeInterface value) {
        // nmgr.logEvent ("setting node prop");
        // check if types match, otherwise throw exception
        Relation rel = (this.dbmap == null) ?
                null : this.dbmap.getExactPropertyRelation(propname);
        DbMapping nmap = (rel == null) ? null : rel.getPropertyMapping();
        DbMapping vmap = value.getDbMapping();

        if ((nmap != null) && (nmap != vmap)) {
            if (vmap == null) {
                value.setDbMapping(nmap);
            } else if (!nmap.isStorageCompatible(vmap) && !rel.isComplexReference()) {
                throw new RuntimeException(Messages.getString("Node.19") + propname + //$NON-NLS-1$
                                           Messages.getString("Node.20") + //$NON-NLS-1$
                                           value.getPrototype() + Messages.getString("Node.21") + //$NON-NLS-1$
                                           nmap.getTypeName());
            }
        }

        if (this.state != TRANSIENT) {
            checkWriteLock();
        }

        Node n = null;

        if (value instanceof Node) {
            n = (Node) value;
        } else {
            throw new RuntimeException(Messages.getString("Node.22")); //$NON-NLS-1$
        }

        boolean isPersistable = isPersistableProperty(propname);
        // if the new node is marked as TRANSIENT and this node is not, mark new node as NEW
        if (this.state != TRANSIENT && n.state == TRANSIENT && isPersistable) {
            n.makePersistable();
        }

        if (this.state != TRANSIENT) {
            n.checkWriteLock();
        }

        // check if the main identity of this node is as a named property
        // or as an anonymous node in a collection
        if (n != this && !this.nmgr.isRootNode(n) && isPersistable) {
            // avoid calling getParent() because it would return bogus results
            // for the not-anymore transient node
            Node nparent = (n.parentHandle == null) ? null
                                                    : n.parentHandle.getNode(this.nmgr);

            // if the node doesn't have a parent yet, or it has one but it's
            // transient while we are persistent, make this the nodes new parent.
            if ((nparent == null) ||
               ((this.state != TRANSIENT) && (nparent.getState() == TRANSIENT))) {
                n.setParent(this);
                n.name = propname;
                n.anonymous = false;
            }
        }

        propname = propname.trim();
        String p2 = correctPropertyName(propname);
        if (rel == null && this.dbmap != null) {
            // widen relation to non-exact (collection) mapping
            rel = this.dbmap.getPropertyRelation(propname);
        }

        if (rel != null && this.state != TRANSIENT && (rel.countConstraints() > 1 || rel.isComplexReference())) {
            rel.setConstraints(this, n);
            if (rel.isComplexReference()) {
                KeyInterface key = new MultiKey(n.getDbMapping(), rel.getKeyParts(this));
                this.nmgr.nmgr.registerNode(n, key);
                return;
            }
        }

        Property prop = (this.propMap == null) ? null : (Property) this.propMap.get(p2);

        if (prop != null) {
            if ((prop.getType() == PropertyInterface.NODE) &&
                    n.getHandle().equals(prop.getNodeHandle())) {
                // nothing to do, just clean up locks and return
                if (this.state == CLEAN) {
                    clearWriteLock();
                }

                if (n.state == CLEAN) {
                    n.clearWriteLock();
                }

                return;
            }
        } else {
            prop = new Property(propname, this);
        }

        prop.setNodeValue(n);

        if ((rel == null) ||
                rel.isReference() ||
                this.state == TRANSIENT ||
                rel.otherType == null ||
                !rel.otherType.isRelational()) {
            // the node must be stored as explicit property
            if (this.propMap == null) {
                this.propMap = new Hashtable();
            }

            this.propMap.put(p2, prop);

            if (this.state == CLEAN && isPersistable) {
                markAs(MODIFIED);
            }
        }

        // don't check node in transactor cache if node is transient -
        // this is done anyway when the node becomes persistent.
        if (n.state != TRANSIENT) {
            // check node in with transactor cache
            Transactor tx = Transactor.getInstanceOrFail();

            // tx.visitCleanNode (new DbKey (dbm, nID), n);
            // UPDATE: using n.getKey() instead of manually constructing key. HW 2002/09/13
            tx.visitCleanNode(n.getKey(), n);

            // if the field is not the primary key of the property, also register it
            if ((rel != null) && (rel.accessName != null) && (this.state != TRANSIENT)) {
                KeyInterface secKey = new SyntheticKey(getKey(), propname);
                this.nmgr.registerNode(n, secKey);
                tx.visitCleanNode(secKey, n);
            }
        }

        this.lastmodified = System.currentTimeMillis();

        if (n.state == DELETED) {
            n.markAs(MODIFIED);
        }
    }

    private boolean isPersistableProperty(String propname) {
        return propname.length() > 0 && propname.charAt(0) != '_';
    }

    /**
     * Remove a property. Note that this works only for explicitly set properties, not for those
     * specified via property relation.
     */
    public void unset(String propname) {

        try {
            // if node is relational, leave a null property so that it is
            // updated in the DB. Otherwise, remove the property.
            Property p = null;
            boolean relational = (this.dbmap != null) && this.dbmap.isRelational();

            if (this.propMap != null) {
                if (relational) {
                    p = (Property) this.propMap.get(this.correctPropertyName(propname));
                } else {
                    p = (Property) this.propMap.remove(this.correctPropertyName(propname));
                }
            }

            if (p != null) {
                boolean isPersistable = this.state != TRANSIENT && isPersistableProperty(propname);
                if (isPersistable) {
                    checkWriteLock();
                }

                if (relational) {
                    p.setStringValue(null);
                    notifyPropertyChange(propname);
                }

                this.lastmodified = System.currentTimeMillis();

                if (this.state == CLEAN && isPersistable) {
                    markAs(MODIFIED);
                }
            } else if (this.dbmap != null) {
                // check if this is a complex constraint and we have to
                // unset constraints.
                Relation rel = this.dbmap.getExactPropertyRelation(propname);

                if (rel != null && (rel.isComplexReference())) {
                    p = getProperty(propname);
                    rel.unsetConstraints(this, p.getNodeValue());
                }
            }
        } catch (Exception x) {
            getApp().logError(Messages.getString("Node.23"), x); //$NON-NLS-1$
        }
    }

    /**
     *
     *
     * @return ...
     */
    public long lastModified() {
        return this.lastmodified;
    }

    /**
     *
     *
     * @return ...
     */
    public long created() {
        return this.created;
    }

    /**
     * Return a string representation for this node. This tries to call the
     * javascript implemented toString() if it is defined.
     * @return a string representing this node.
     */
    @Override
    public String toString() {
        try {
            // We need to reach deap into helma.framework.core to invoke toString(),
            // but the functionality is really worth it.
            RequestEvaluator reval = getApp().getCurrentRequestEvaluator();
            if (reval != null) {
                Object str = reval.invokeDirectFunction(this, "toString", RequestEvaluator.EMPTY_ARGS); //$NON-NLS-1$
                if (str instanceof String)
                    return (String) str;
            }
        } catch (Exception x) {
            // fall back to default representation
        }
        return "HopObject " + this.name; //$NON-NLS-1$
    }

    /**
     * Tell whether this node is stored inside a relational db. This doesn't mean
     * it actually is stored in a relational db, just that it would be, if the node was
     * persistent
     */
    public boolean isRelational() {
        return (this.dbmap != null) && this.dbmap.isRelational();
    }

    /**
     * Public method to make a node persistent.
     */
    public void persist() {
        if (this.state == TRANSIENT) {
            makePersistable();
        } else if (this.state == CLEAN) {
            markAs(MODIFIED);
        }

    }

    /**
     * Turn node status from TRANSIENT to NEW so that the Transactor will
     * know it has to insert this node. Recursively persistifies all child nodes
     * and references. This method will immediately cause the node it is called upon to
     * be stored in db when the transaction is committed, so it should be called
     * with care.
     */
    private void makePersistable() {
        // if this isn't a transient node, do nothing.
        if (this.state != TRANSIENT) {
            return;
        }

        // mark as new
        setState(NEW);

        // generate a real, persistent ID for this object
        this.id = this.nmgr.generateID(this.dbmap);
        getHandle().becomePersistent();

        // register node with the transactor
        Transactor tx = Transactor.getInstanceOrFail();
        tx.visitDirtyNode(this);
        tx.visitCleanNode(this);

        // recursively make children persistable
        makeChildrenPersistable();
    }

    /**
     * Recursively turn node status from TRANSIENT to NEW on child nodes
     * so that the Transactor knows they are to be persistified. This method
     * can be called on TRANSIENT nodes that have just been made perstable
     * using makePersistable() or converted to virtual using convertToVirtual().
     */
    private void makeChildrenPersistable() {
        Relation subrel = this.dbmap == null ? null : this.dbmap.getSubnodeRelation();
        for (Enumeration e = getLoadedSubnodes(); e.hasMoreElements();) {
            Node node = (Node) e.nextElement();

            if (node.state == TRANSIENT) {
                DbMapping submap = node.getDbMapping();
                if (submap != null && submap.isVirtual() && !submap.needsPersistence()) {
                    convertToVirtual(node);
                } else {
                    node.makePersistable();
                    if (subrel != null && subrel.countConstraints() > 1) {
                        subrel.setConstraints(this, node);
                    }
                }
            }
        }

        // no need to make properties of virtual nodes persistable
        if (this.state == VIRTUAL) return;

        for (Enumeration e = properties(); e.hasMoreElements();) {
            String propname = (String) e.nextElement();
            PropertyInterface next = get(propname);

            if (next == null || next.getType() != PropertyInterface.NODE) {
                continue;
            }

            // check if this property actually needs to be persisted.
            Node node = (Node) next.getNodeValue();
            Relation rel = null;

            if (node == null || node == this) {
                continue;
            }

            rel = this.dbmap == null ? null : this.dbmap.getExactPropertyRelation(next.getName());
            if (rel != null && rel.isVirtual() && !rel.needsPersistence()) {
                convertToVirtual(node);
            } else {
                node.makePersistable();
                if (rel != null && rel.isComplexReference()) {
                    // if this is a complex reference, make binding properties are set
                    rel.setConstraints(this, node);
                }
            }
        }
    }

    /**
     * Convert a node to a virtual (collection or group ) node. This is used when we
     * encounter a node that is defined as virtual from within the  makePeristable() and
     * makeChildrenPersistable() methods. It will first mark the node as virtual and then
     * call makeChildrenPersistable() on it.
     * @param node a previously transient node to be converted to a virtual node.
     */
    private void convertToVirtual(Node node) {
        // Make node a virtual node with this as parent node. what we do is
        // basically to replay the things done in the constructor for virtual nodes.
        node.setState(VIRTUAL);
        node.primaryKey = new SyntheticKey(getKey(), node.name);
        node.id = node.name;
        node.makeChildrenPersistable();
    }

    /**
     * Get the cache node for this node. This can be
     * used to store transient cache data per node from Javascript.
     */
    public synchronized NodeInterface getCacheNode() {
        if (this.cacheNode == null) {
            this.cacheNode = new TransientNode();
        }

        return this.cacheNode;
    }

    /**
     * Reset the cache node for this node.
     */
    public synchronized void clearCacheNode() {
        this.cacheNode = null;
    }

    /**
     * This method walks down node path to the first non-virtual node and return it.
     *  limit max depth to 5, since there shouldn't be more then 2 layers of virtual nodes.
     */
    public Node getNonVirtualParent() {
        Node node = this;

        for (int i = 0; i < 5; i++) {
            if (node == null) {
                break;
            }

            if (node.getState() == NodeInterface.TRANSIENT) {
                DbMapping map = node.getDbMapping();
                if (map == null || !map.isVirtual())
                    return node;
            } else if (node.getState() != NodeInterface.VIRTUAL) {
                return node;
            }

            node = (Node) node.getParent();
        }

        return null;
    }

    /**
     *  Instances of this class may be used to mark an entry in the object cache as null.
     *  This method tells the caller whether this is the case.
     */
    public boolean isNullNode() {
        return this.nmgr == null;
    }

    String generateTransientID() {
        // make transient ids differ from persistent ones
        // and are unique within on runtime session
        return "t" + idgen++; //$NON-NLS-1$
    }

    /**
     * We overwrite hashCode to make it dependant from the prototype. That way, when the prototype
     * changes, the node will automatically get a new ESNode wrapper, since they're cached in a hashtable.
     * You gotta love these hash code tricks ;-)
     */
    @Override
    public int hashCode() {
        if (this.prototype == null) {
            return super.hashCode();
        }
        return super.hashCode() + this.prototype.hashCode();
    }

    /**
     *
     */
    public void dump() {
        System.err.println(Messages.getString("Node.24") + this.subnodes); //$NON-NLS-1$
        System.err.println(Messages.getString("Node.25") + this.propMap); //$NON-NLS-1$
    }

    /**
     * Get the application this node belongs to.
     * @return the app we belong to
     */
    private Application getApp() {
        return this.nmgr.nmgr.app;
    }

    private String correctPropertyName(String propname) {
        return getApp().correctPropertyName(propname);
    }
}
