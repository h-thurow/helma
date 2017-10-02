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

package helma.objectmodel.db;

import helma.objectmodel.NodeInterface;
import java.util.ArrayList;
import java.util.List;
import java.io.Serializable;

/**
 * Container implementation for subnode collections.
 */
public class SubnodeList implements Serializable {

    private static final long serialVersionUID = 711208015232333566L;

    protected Node node;
    protected List list;

    transient protected long lastSubnodeFetch = 0;
    transient protected long lastSubnodeChange = 0;


    /**
     * Hide/disable zero argument constructor for subclasses
     */
    @SuppressWarnings("unused")
    private SubnodeList()  {}

    /**
     * Creates a new subnode list
     * @param node the node we belong to
     */
    public SubnodeList(Node node) {
        this.node = node;
        this.list = new ArrayList();
    }

    /**
     * Adds the specified object to this list performing
     * custom ordering
     *
     * @param handle element to be inserted.
     */
    public boolean add(NodeHandle handle) {
        return this.list.add(handle);
    }
    /**
     * Adds the specified object to the list at the given position
     * @param idx the index to insert the element at
     * @param handle the object to add
     */
    public void add(int idx, NodeHandle handle) {
        this.list.add(idx, handle);
    }

    public NodeHandle get(int index) {
        if (index < 0 || index >= this.list.size()) {
            return null;
        }
        return (NodeHandle) this.list.get(index);
    }

    public Node getNode(int index) {
        Node retval = null;
        NodeHandle handle = get(index);

        if (handle != null) {
            retval = handle.getNode(this.node.nmgr);
            // Legacy alarm!
            if ((retval != null) && (retval.parentHandle == null) &&
                    !this.node.nmgr.isRootNode(retval)) {
                retval.setParent(this.node);
                retval.anonymous = true;
            }
        }

        return retval;
    }

    public boolean contains(Object object) {
        return this.list.contains(object);
    }

    public int indexOf(Object object) {
        return this.list.indexOf(object);
    }

    /**
     * remove the object specified by the given index-position
     * @param idx the index-position of the NodeHandle to remove
     */
    public Object remove (int idx) {
        return this.list.remove(idx);
    }

    /**
     * remove the given Object from this List
     * @param obj the NodeHandle to remove
     */
    public boolean remove (Object obj) {
        return this.list.remove(obj);
    }

    public Object[] toArray() {
        return this.list.toArray();
    }

    /**
     * Return the size of the list.
     * @return the list size
     */
    public int size() {
        return this.list.size();
    }

    protected void update() {
        // also reload if the type mapping has changed.
        long lastChange = getLastSubnodeChange();
        if (lastChange != this.lastSubnodeFetch) {
            Relation rel = getSubnodeRelation();
            if (rel != null && rel.aggressiveLoading && rel.groupby == null) {
                this.list = this.node.nmgr.getNodes(this.node, rel);
            } else {
                this.list = this.node.nmgr.getNodeIDs(this.node, rel);
            }
            this.lastSubnodeFetch = lastChange;
        }
    }

    protected void prefetch(int start, int length) {
        if (start < 0 || start >= size()) {
            return;
        }
        length =  (length < 0) ?
                size() - start : Math.min(length, size() - start);
        if (length < 0) {
            return;
        }

        DbMapping dbmap = getSubnodeMapping();

        if (dbmap.isRelational()) {
            Relation rel = getSubnodeRelation();
            this.node.nmgr.prefetchNodes(this.node, rel, this, start, length);
        }
    }

    /**
     * Compute a serial number indicating the last change in subnode collection
     * @return a serial number that increases with each subnode change
     */
    protected long getLastSubnodeChange() {
        // include dbmap.getLastTypeChange to also reload if the type mapping has changed.
        long checkSum = this.lastSubnodeChange + this.node.dbmap.getLastTypeChange();
        Relation rel = getSubnodeRelation();
        return rel == null || rel.aggressiveCaching ?
                checkSum : checkSum + rel.otherType.getLastDataChange();
    }

    protected synchronized void markAsChanged() {
        this.lastSubnodeChange += 1;
    }

    protected boolean hasRelationalNodes() {
        DbMapping dbmap = getSubnodeMapping();
        return (dbmap != null && dbmap.isRelational()
                && ((this.node.getState() != NodeInterface.TRANSIENT &&  this.node.getState() != NodeInterface.NEW)
                    || this.node.getSubnodeRelation() != null));
    }

    protected DbMapping getSubnodeMapping() {
        return this.node.dbmap == null ? null : this.node.dbmap.getSubnodeMapping();
    }

    protected Relation getSubnodeRelation() {
        return this.node.dbmap == null ? null : this.node.dbmap.getSubnodeRelation();
    }
}
