/*
 * Helma License Notice
 *
 * The contents of this file are subject to the Helma License
 * Version 2.0 (the "License"). You may not use this file except in
 * compliance with the License. A copy of the License is available at
 * http://adele.helma.org/download/helma/license.txt
 *
 * Copyright 2009 Helma Project. All Rights Reserved.
 */

package helma.objectmodel.db;

import java.util.*;

public class SegmentedSubnodeList extends SubnodeList {

    private static final long serialVersionUID = -4947752577517584610L;

    transient Segment[] segments = null;
    static int SEGLENGTH = 1000;

    transient private int subnodeCount = -1;

    /**
     * Creates a new subnode list
     * @param node the node we belong to
     */
    public SegmentedSubnodeList(Node node) {
        super(node);
    }

    /**
     * Adds the specified object to this list performing
     * custom ordering
     *
     * @param handle element to be inserted.
     */
    @Override
    public synchronized boolean add(NodeHandle handle) {
        if (!hasRelationalNodes() || this.segments == null) {
            return super.add(handle);
        }
        if (this.subnodeCount == -1) {
            update();
        }
        this.subnodeCount++;
        this.segments[this.segments.length - 1].length += 1;
        return this.list.add(handle);
    }
    /**
     * Adds the specified object to the list at the given position
     * @param index the index to insert the element at
     * @param handle the object to add
     */
    @Override
    public synchronized void add(int index, NodeHandle handle) {
        if (!hasRelationalNodes() || this.segments == null) {
            super.add(index, handle);
            return;
        }
        if (this.subnodeCount == -1) {
            update();
        }
        this.subnodeCount++;
        this.list.add(index, handle);
        // shift segment indices by one
        int s = getSegment(index);
        this.segments[s].length += 1;
        for (int i = s + 1; i < this.segments.length; i++) {
            this.segments[i].startIndex += 1;
        }
    }

    @Override
    public NodeHandle get(int index) {
        if (!hasRelationalNodes() || this.segments == null) {
            return super.get(index);
        }
        if (index < 0 || index >= this.subnodeCount) {
            return null;
        }
        loadSegment(getSegment(index), false);
        return (NodeHandle) this.list.get(index);
    }

    @Override
    public synchronized boolean contains(Object object) {
        if (!hasRelationalNodes() || this.segments == null) {
            return super.contains(object);
        }
        if (this.list.contains(object)) {
            return true;
        }
        for (int i = 0; i < this.segments.length; i++) {
            if (loadSegment(i, false).contains(object)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public synchronized int indexOf(Object object) {
        if (!hasRelationalNodes() || this.segments == null) {
            return super.indexOf(object);
        }
        int index;
        if ((index = this.list.indexOf(object)) > -1) {
            return index;
        }
        for (int i = 0; i < this.segments.length; i++) {
            if ((index = loadSegment(i, false).indexOf(object)) > -1) {
                return this.segments[i].startIndex + index;
            }
        }
        return -1;
    }

    /**
     * remove the object specified by the given index-position
     * @param index the index-position of the NodeHandle to remove
     */
    @Override
    public synchronized Object remove(int index) {
        if (!hasRelationalNodes() || this.segments == null) {
            return super.remove(index);
        }
        if (this.subnodeCount == -1) {
            update();
        }
        Object removed = this.list.remove(index);
        int s = getSegment(index);
        this.segments[s].length -= 1;
        for (int i = s + 1; i < this.segments.length; i++) {
            this.segments[i].startIndex -= 1;
        }
        this.subnodeCount--;
        return removed;
    }

    /**
     * remove the given Object from this List
     * @param object the NodeHandle to remove
     */
    @Override
    public synchronized boolean remove(Object object) {
        if (!hasRelationalNodes() || this.segments == null) {
            return super.remove(object);
        }
        if (this.subnodeCount == -1) {
            update();
        }
        int index = indexOf(object);
        if (index > -1) {
            this.list.remove(object);
            int s = getSegment(index);
            this.segments[s].length -= 1;
            for (int i = s + 1; i < this.segments.length; i++) {
                this.segments[i].startIndex -= 1;
            }
            this.subnodeCount--;
            return true;
        }
        return false;
    }

    @Override
    public synchronized Object[] toArray() {
        if (!hasRelationalNodes() || this.segments == null) {
            return super.toArray();
        }
        this.node.nmgr.logEvent(Messages.getString("SegmentedSubnodeList.0") + this.node); //$NON-NLS-1$
        for (int i = 0; i < this.segments.length; i++) {
            loadSegment(i, false);
        }
        return this.list.toArray();
    }

    private int getSegment(int index) {
        for (int i = 1; i < this.segments.length; i++) {
            if (index < this.segments[i].startIndex) {
                return i - 1;
            }
        }
        return this.segments.length - 1;
    }

    private List loadSegment(int seg, boolean deep) {
        Segment segment = this.segments[seg];
        if (segment != null && !segment.loaded) {
            Relation rel = getSubnodeRelation().getClone();
            rel.offset = segment.startIndex;
            int expectedSize = rel.maxSize = segment.length;
            List seglist =  deep ?
                    this.node.nmgr.getNodes(this.node, rel) :
                    this.node.nmgr.getNodeIDs(this.node, rel);
            int actualSize = seglist.size();
            if (actualSize != expectedSize) {
                this.node.nmgr.logEvent(Messages.getString("SegmentedSubnodeList.1") + this.node + Messages.getString("SegmentedSubnodeList.2") + segment); //$NON-NLS-1$ //$NON-NLS-2$
            }
            int listSize = this.list.size();
            for (int i = 0; i < actualSize; i++) {
                if (segment.startIndex + i < listSize) {
                    this.list.set(segment.startIndex + i, seglist.get(i));
                } else {
                    this.list.add(seglist.get(i));
                }
                // FIXME how to handle inconsistencies?
            }
            segment.loaded = true;
            return seglist;
        }
        return Collections.EMPTY_LIST;
    }

    @Override
    protected synchronized void update() {
        if (!hasRelationalNodes()) {
            this.segments = null;
            super.update();
            return;
        }
        // also reload if the type mapping has changed.
        long lastChange = getLastSubnodeChange();
        if (lastChange != this.lastSubnodeFetch) {
            // count nodes in db without fetching anything
            this.subnodeCount = this.node.nmgr.countNodes(this.node, getSubnodeRelation());
            if (this.subnodeCount > SEGLENGTH) {
                float size = this.subnodeCount;
                int nsegments = (int) Math.ceil(size / SEGLENGTH);
                int remainder = (int) size % SEGLENGTH;
                this.segments = new Segment[nsegments];
                for (int s = 0; s < nsegments; s++) {
                    int length = (s == nsegments - 1 && remainder > 0) ?
                        remainder : SEGLENGTH;
                    this.segments[s] = new Segment(s * SEGLENGTH, length);
                }
                this.list = new ArrayList((int) size + 5);
                for (int i = 0; i < size; i++) {
                    this.list.add(null);
                }
            } else {
                this.segments = null;
                super.update();
            }
            this.lastSubnodeFetch = lastChange;
        }
    }

    @Override
    public int size() {
        if (!hasRelationalNodes() || this.segments == null) {
            return super.size();
        }
        return this.subnodeCount;
    }

    class Segment {

        int startIndex, length;
        boolean loaded;

        Segment(int startIndex, int length) {
            this.startIndex = startIndex;
            this.length = length;
            this.loaded = false;
        }

        int endIndex() {
            return this.startIndex + this.length;
        }

        @Override
        public String toString() {
            return "Segment{startIndex: " + this.startIndex + ", length: " + this.length + "}";  //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$
        }
    }

}

