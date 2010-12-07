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

package helma.objectmodel.db;

import helma.objectmodel.ObjectNotFoundException;

import java.util.Vector;
import java.util.List;

/**
 * A wrapper around NodeManager that catches most Exceptions, or rethrows them as RuntimeExceptions.
 * The idea behind this is that we don't care a lot about Exception classes, since Hop programming is done
 * in JavaScript which doesn't know about them (except for the exception message).
 */
public final class WrappedNodeManager {
    NodeManager nmgr;

    /**
     * Creates a new WrappedNodeManager object.
     *
     * @param nmgr ...
     */
    public WrappedNodeManager(NodeManager nmgr) {
        this.nmgr = nmgr;
    }

    /**
     * Get a node given its id and DbMapping
     *
     * @param id
     * @param dbmap
     * @return
     */
    public Node getNode(String id, DbMapping dbmap) {
        return getNode(new DbKey(dbmap, id));
    }

    /**
     * Get a node given its key
     *
     * @param key
     * @return
     */
    public Node getNode(Key key) {
        Transactor tx = checkLocalTransactor();
        try {
            beginLocalTransaction(tx, "getNode"); //$NON-NLS-1$
            Node node = this.nmgr.getNode(key);
            commitLocalTransaction(tx);
            return node;
        } catch (ObjectNotFoundException x) {
            abortLocalTransaction(tx);
            return null;
        } catch (Exception x) {
            abortLocalTransaction(tx);
            this.nmgr.app.logError(Messages.getString("WrappedNodeManager.0") + key, x); //$NON-NLS-1$
            throw new RuntimeException(Messages.getString("WrappedNodeManager.1"), x); //$NON-NLS-1$
        }
    }

    /**
     * Get the node specified by the given id and Relation.
     *
     * @param home
     * @param id
     * @param rel
     * @return
     */
    public Node getNode(Node home, String id, Relation rel) {
        Transactor tx = checkLocalTransactor();
        try {
            beginLocalTransaction(tx, "getNode"); //$NON-NLS-1$
            Node node = this.nmgr.getNode(home, id, rel);
            commitLocalTransaction(tx);
            return node;
        } catch (ObjectNotFoundException x) {
            abortLocalTransaction(tx);
            return null;
        } catch (Exception x) {
            abortLocalTransaction(tx);
            this.nmgr.app.logError(Messages.getString("WrappedNodeManager.2") + id + Messages.getString("WrappedNodeManager.3") + home, x); //$NON-NLS-1$ //$NON-NLS-2$
            throw new RuntimeException(Messages.getString("WrappedNodeManager.4"), x); //$NON-NLS-1$
        }
    }

    /**
     * Get the list of nodes contained in the collection of the given
     * Node specified by the given Relation.
     *
     * @param home
     * @param rel
     * @return
     */
    public List getNodes(Node home, Relation rel) {
        Transactor tx = checkLocalTransactor();
        try {
            beginLocalTransaction(tx, "getNodes"); //$NON-NLS-1$
            List list = this.nmgr.getNodes(home, rel);
            commitLocalTransaction(tx);
            return list;
        } catch (Exception x) {
            abortLocalTransaction(tx);
            throw new RuntimeException(Messages.getString("WrappedNodeManager.5"), x); //$NON-NLS-1$
        }
    }

    /**
     * Get a list of IDs of nodes contained in the given Node's
     * collection specified by the given Relation.
     *
     * @param home
     * @param rel
     * @return
     */
    public List getNodeIDs(Node home, Relation rel) {
        try {
            return this.nmgr.getNodeIDs(home, rel);
        } catch (Exception x) {
            throw new RuntimeException(Messages.getString("WrappedNodeManager.6"), x); //$NON-NLS-1$
        }
    }

    /**
     * @see helma.objectmodel.db.NodeManager#updateSubnodeList(Node, Relation)
     */
    /* public int updateSubnodeList (Node home, Relation rel) {
        try {
            return nmgr.updateSubnodeList(home, rel);
        } catch (Exception x) {
            throw new RuntimeException("Error retrieving NodeIDs", x);
        }
    } */

    /**
     * Count the nodes contained in the given Node's collection
     * specified by the given Relation.
     *
     * @param home
     * @param rel
     * @return
     */
    public int countNodes(Node home, Relation rel) {
        try {
            return this.nmgr.countNodes(home, rel);
        } catch (Exception x) {
            throw new RuntimeException(Messages.getString("WrappedNodeManager.7"), x); //$NON-NLS-1$
        }
    }

    public void prefetchNodes(Node node, Relation rel, SubnodeList list,
                              int start, int length) {
        try {
            this.nmgr.prefetchNodes(node, rel, list, start, length);
        } catch (Exception x) {
            throw new RuntimeException(Messages.getString("WrappedNodeManager.8"), x); //$NON-NLS-1$
        }
    }

    /**
     * Delete a node from the database
     *
     * @param node
     */
    public void deleteNode(Node node) {
        Transactor tx = checkLocalTransactor();
        try {
            beginLocalTransaction(tx, "deleteNode"); //$NON-NLS-1$
            this.nmgr.deleteNode(node);
            commitLocalTransaction(tx);
        } catch (Exception x) {
            abortLocalTransaction(tx);
            throw new RuntimeException(Messages.getString("WrappedNodeManager.9"), x); //$NON-NLS-1$
        }
    }

    /**
     * Get a list of property names from the given node.
     * TODO: this retrieves access names of child nodes, not property names
     *
     * @param home
     * @param rel
     * @return
     */
    public Vector getPropertyNames(Node home, Relation rel) {
        try {
            return this.nmgr.getPropertyNames(home, rel);
        } catch (Exception x) {
            throw new RuntimeException(Messages.getString("WrappedNodeManager.10"), x); //$NON-NLS-1$
        }
    }

    /**
     * Register a node with the object cache using its primary key.
     *
     * @param node
     */
    public void registerNode(Node node) {
        this.nmgr.registerNode(node);
    }

    /**
     * Register a node with the object cache using the given key.
     *
     * @param node
     */
    public void registerNode(Node node, Key key) {
        this.nmgr.registerNode(node, key);
    }

    /**
     * Evict a node from the object cache
     *
     * @param node
     */
    public void evictNode(Node node) {
        this.nmgr.evictNode(node);
    }

    /**
     * Completely evict the object with the given key from the object cache
     *
     * @param key
     */
    public void evictNodeByKey(Key key) {
        this.nmgr.evictNodeByKey(key);
    }

    /**
     * Evict the object with the given key from the object cache
     *
     * @param key
     */
    public void evictKey(Key key) {
        this.nmgr.evictKey(key);
    }

    /**
     * Generate a new id for an object specified by the DbMapping
     *
     * @param map the DbMapping to generate an id for
     * @return a new unique id
     */
    public String generateID(DbMapping map) {
        try {
            return this.nmgr.generateID(map);
        } catch (Exception x) {
            throw new RuntimeException(x.toString(), x);
        }
    }

    /**
     * Gets the application's root node.
     */
    public Node getRootNode() {
        Transactor tx = checkLocalTransactor();
        try {
            beginLocalTransaction(tx, "getRootNode"); //$NON-NLS-1$
            Node node = this.nmgr.getRootNode();
            commitLocalTransaction(tx);
            return node;
        } catch (Exception x) {
            abortLocalTransaction(tx);
            throw new RuntimeException(x.toString(), x);
        }
    }

    /**
     * Checks if the given node is the application's root node.
     */
    public boolean isRootNode(Node node) {
        return this.nmgr.isRootNode(node);
    }

    /**
     * Get an array of all objects in the object cache
     */
    public Object[] getCacheEntries() {
        return this.nmgr.getCacheEntries();
    }

    /**
     * Write an entry to the application's event log
     *
     * @param msg event message
     */
    public void logEvent(String msg) {
        this.nmgr.app.logEvent(msg);
    }

    /**
     * Get the DbMapping corresponding to a type name
     *
     * @param name a type name
     * @return the corresponding DbMapping
     */
    public DbMapping getDbMapping(String name) {
        return this.nmgr.app.getDbMapping(name);
    }

    // helper methods to wrap execution inside local transactions

    private Transactor checkLocalTransactor() {
        Transactor tx = Transactor.getInstance();
        if (tx != null) {
            // transactor already associated with current thread - return null
            return null;
        }
        return Transactor.getInstance(this.nmgr);
    }

    private void beginLocalTransaction(Transactor tx, String name) {
        if (tx != null) {
            try {
                tx.begin(name);
            } catch (Exception x) {
                this.nmgr.app.logError(Messages.getString("WrappedNodeManager.11"), x); //$NON-NLS-1$
            }
        }
    }

    private void commitLocalTransaction(Transactor tx) {
        if (tx != null) {
            try {
                tx.commit();
            } catch (Exception x) {
                this.nmgr.app.logError(Messages.getString("WrappedNodeManager.12"), x);                 //$NON-NLS-1$
            }
        }
    }

    private void abortLocalTransaction(Transactor tx) {
        if (tx != null) {
            try {
                tx.abort();
            } catch (Exception x) {
                this.nmgr.app.logError(Messages.getString("WrappedNodeManager.13"), x); //$NON-NLS-1$
            }
        }
    }
}
