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

package helma.objectmodel;

import helma.framework.PathElementInterface;
import helma.objectmodel.db.DbMapping;
import helma.objectmodel.db.Relation;
import helma.objectmodel.db.Node;
import java.io.*;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.StringTokenizer;
import java.util.Vector;

/**
 * A transient implementation of NodeInterface. An instance of this class can't be
 * made persistent by reachability from a persistent node. To make a persistent-capable
 * object, class helma.objectmodel.db.Node has to be used.
 */
public class TransientNode implements NodeInterface, Serializable {
    private static final long serialVersionUID = -4599844796152072979L;

    private static long idgen = 0;
    protected Hashtable propMap;
    protected Hashtable nodeMap;
    protected Vector nodes;
    protected TransientNode parent;
    transient String prototype;
    protected long created;
    protected long lastmodified;
    protected String id;
    protected String name;

    // is the main identity a named property or an anonymous node in a collection?
    protected boolean anonymous = false;
    transient DbMapping dbmap;
    NodeInterface cacheNode;

    /**
     * Creates a new TransientNode object.
     */
    public TransientNode() {
        this.id = generateID();
        this.name = this.id;
        this.created = this.lastmodified = System.currentTimeMillis();
    }

    /**
     *  Make a new TransientNode object with a given name
     */
    public TransientNode(String n) {
        this.id = generateID();
        this.name = (n == null || n.length() == 0) ? this.id : n;
        // HACK - decrease creation and last-modified timestamp by 1 so we notice
        // modifications that take place immediately after object creation
        this.created = this.lastmodified = System.currentTimeMillis() - 1;
    }

    public static String generateID() {
        // make transient ids differ from persistent ones
        // and are unique within on runtime session
        return "t" + idgen++; //$NON-NLS-1$
    }

    public void setDbMapping(DbMapping dbmap) {
        this.dbmap = dbmap;
    }

    public DbMapping getDbMapping() {
        return this.dbmap;
    }

    public String getID() {
        return this.id;
    }

    public boolean isAnonymous() {
        return this.anonymous;
    }

    public String getName() {
        return this.name;
    }

    public String getElementName() {
        return this.anonymous ? this.id : this.name;
    }

    public int getState() {
        return TRANSIENT;
    }

    public void setState(int s) {
        // state always is TRANSIENT on this kind of node
    }

    public String getPath() {
        return getFullName(null);
    }

    public String getFullName(NodeInterface root) {
        String divider = null;
        StringBuffer b = new StringBuffer();
        TransientNode p = this;

        while ((p != null) && (p.parent != null) && (p != root)) {
            if (divider != null) {
                b.insert(0, divider);
            } else {
                divider = "/"; //$NON-NLS-1$
            }

            b.insert(0, p.getElementName());
            p = p.parent;
        }

        return b.toString();
    }

    public void setName(String name) {
        // if (name.indexOf('/') > -1)
        //     throw new RuntimeException ("The name of the node must not contain \"/\".");
        if ((name == null) || (name.trim().length() == 0)) {
            this.name = this.id;
        } else {
            this.name = name;
        }
    }

    public String getPrototype() {
        // if prototype is null, it's a vanilla HopObject.
        if (this.prototype == null) {
            return "HopObject"; //$NON-NLS-1$
        }

        return this.prototype;
    }

    public void setPrototype(String proto) {
        this.prototype = proto;
    }

    public NodeInterface getParent() {
        return this.parent;
    }

    public void setSubnodeRelation(String rel) {
        throw new UnsupportedOperationException(Messages.getString("TransientNode.0")); //$NON-NLS-1$
    }

    public String getSubnodeRelation() {
        return null;
    }

    public int numberOfNodes() {
        return (this.nodes == null) ? 0 : this.nodes.size();
    }

    public NodeInterface addNode(NodeInterface elem) {
        return addNode(elem, numberOfNodes());
    }

    public NodeInterface addNode(NodeInterface elem, int where) {
        if ((where < 0) || (where > numberOfNodes())) {
            where = numberOfNodes();
        }

        String n = elem.getName();

        if (n.indexOf('/') > -1) {
            throw new RuntimeException(Messages.getString("TransientNode.1")); //$NON-NLS-1$
        }

        if ((this.nodeMap != null) && (this.nodeMap.get(elem.getID()) != null)) {
            this.nodes.removeElement(elem);
            where = Math.min(where, numberOfNodes());
            this.nodes.insertElementAt(elem, where);

            return elem;
        }

        if (this.nodeMap == null) {
            this.nodeMap = new Hashtable();
        }

        if (this.nodes == null) {
            this.nodes = new Vector();
        }

        this.nodeMap.put(elem.getID(), elem);
        this.nodes.insertElementAt(elem, where);

        if (elem instanceof TransientNode) {
            TransientNode node = (TransientNode) elem;

            if (node.parent == null) {
                node.parent = this;
                node.anonymous = true;
            }
        }

        this.lastmodified = System.currentTimeMillis();
        return elem;
    }

    public NodeInterface createNode() {
        return createNode(null, 0); // where is ignored since this is an anonymous node
    }

    public NodeInterface createNode(int where) {
        return createNode(null, where);
    }

    public NodeInterface createNode(String nm) {
        return createNode(nm, numberOfNodes()); // where is usually ignored (if nm != null)
    }

    public NodeInterface createNode(String nm, int where) {
        boolean anon = false;

        if ((nm == null) || "".equals(nm.trim())) { //$NON-NLS-1$
            anon = true;
        }

        NodeInterface n = new TransientNode(nm);

        if (anon) {
            addNode(n, where);
        } else {
            setNode(nm, n);
        }

        return n;
    }


    public PathElementInterface getParentElement() {
        return getParent();
    }

    public PathElementInterface getChildElement(String name) {
        return getNode(name);
    }

    public NodeInterface getSubnode(String name) {
        StringTokenizer st = new StringTokenizer(name, "/"); //$NON-NLS-1$
        TransientNode retval = this;
        TransientNode runner;

        while (st.hasMoreTokens() && (retval != null)) {
            runner = retval;

            String next = st.nextToken().trim().toLowerCase();

            if ("".equals(next)) { //$NON-NLS-1$
                retval = this;
            } else {
                retval = (runner.nodeMap == null) ? null
                                                  : (TransientNode) runner.nodeMap.get(next);
            }

            if (retval == null) {
                retval = (TransientNode) runner.getNode(next);
            }
        }

        return retval;
    }

    public NodeInterface getSubnodeAt(int index) {
        return (this.nodes == null) ? null : (NodeInterface) this.nodes.elementAt(index);
    }

    public int contains(NodeInterface n) {
        if ((n == null) || (this.nodes == null)) {
            return -1;
        }

        return this.nodes.indexOf(n);
    }

    public boolean remove() {
        if (this.anonymous) {
            this.parent.unset(this.name);
        } else {
            this.parent.removeNode(this);
        }

        return true;
    }

    public void removeNode(NodeInterface node) {
        // IServer.getLogger().log ("removing: "+ node);
        releaseNode(node);

        TransientNode n = (TransientNode) node;

        if ((n.getParent() == this) && n.anonymous) {

            // remove all subnodes, giving them a chance to destroy themselves.
            Vector v = new Vector(); // removeElement modifies the Vector we are enumerating, so we are extra careful.

            for (Enumeration e3 = n.getSubnodes(); e3.hasMoreElements();) {
                v.addElement(e3.nextElement());
            }

            int m = v.size();

            for (int i = 0; i < m; i++) {
                n.removeNode((TransientNode) v.elementAt(i));
            }
        }
    }

    /**
     * "Physically" remove a subnode from the subnodes table.
     * the logical stuff necessary for keeping data consistent is done elsewhere (in removeNode).
     */
    protected void releaseNode(NodeInterface node) {
        if ((this.nodes == null) || (this.nodeMap == null)) {

            return;
        }

        int runner = this.nodes.indexOf(node);

        // this is due to difference between .equals() and ==
        while ((runner > -1) && (this.nodes.elementAt(runner) != node))
            runner = this.nodes.indexOf(node, Math.min(this.nodes.size() - 1, runner + 1));

        if (runner > -1) {
            this.nodes.removeElementAt(runner);
        }

        this.nodeMap.remove(node.getName().toLowerCase());
        this.lastmodified = System.currentTimeMillis();
    }

    /**
     *
     *
     * @return ...
     */
    public Enumeration getSubnodes() {
        return (this.nodes == null) ? new Vector().elements() : this.nodes.elements();
    }

    /**
     *  property-related
     */
    public Enumeration properties() {
        return (this.propMap == null) ? Collections.enumeration(Collections.EMPTY_LIST) : this.propMap.keys();
    }

    private TransientProperty getProperty(String propname) {
        TransientProperty prop = (this.propMap == null) ? null : (TransientProperty) this.propMap.get(propname);

        // check if we have to create a virtual node
        if ((prop == null) && (this.dbmap != null)) {
            Relation rel = this.dbmap.getPropertyRelation(propname);

            if ((rel != null) && rel.isVirtual()) {
                prop = makeVirtualNode(propname, rel);
            }
        }

        return prop;
    }

    private TransientProperty makeVirtualNode(String propname, Relation rel) {
        NodeInterface node = new Node(rel.getPropName(), rel.getPrototype(),
                                                   this.dbmap.getWrappedNodeManager());

        node.setDbMapping(rel.getVirtualMapping());
        setNode(propname, node);

        return (TransientProperty) this.propMap.get(propname);
    }

    public PropertyInterface get(String propname) {
        return getProperty(propname);
    }

    public String getString(String propname, String defaultValue) {
        String propValue = getString(propname);

        return (propValue == null) ? defaultValue : propValue;
    }

    public String getString(String propname) {
        TransientProperty prop = getProperty(propname);

        try {
            return prop.getStringValue();
        } catch (Exception ignore) {
        }

        return null;
    }

    public long getInteger(String propname) {
        TransientProperty prop = getProperty(propname);

        try {
            return prop.getIntegerValue();
        } catch (Exception ignore) {
        }

        return 0;
    }

    public double getFloat(String propname) {
        TransientProperty prop = getProperty(propname);

        try {
            return prop.getFloatValue();
        } catch (Exception ignore) {
        }

        return 0.0;
    }

    public Date getDate(String propname) {
        TransientProperty prop = getProperty(propname);

        try {
            return prop.getDateValue();
        } catch (Exception ignore) {
        }

        return null;
    }

    public boolean getBoolean(String propname) {
        TransientProperty prop = getProperty(propname);

        try {
            return prop.getBooleanValue();
        } catch (Exception ignore) {
        }

        return false;
    }

    public NodeInterface getNode(String propname) {
        TransientProperty prop = getProperty(propname);

        try {
            return prop.getNodeValue();
        } catch (Exception ignore) {
        }

        return null;
    }

    public Object getJavaObject(String propname) {
        TransientProperty prop = getProperty(propname);

        try {
            return prop.getJavaObjectValue();
        } catch (Exception ignore) {
        }

        return null;
    }

    // create a property if it doesn't exist for this name
    private TransientProperty initProperty(String propname) {
        if (this.propMap == null) {
            this.propMap = new Hashtable();
        }

        propname = propname.trim();
        TransientProperty prop = (TransientProperty) this.propMap.get(propname);

        if (prop == null) {
            prop = new TransientProperty(propname, this);
            this.propMap.put(propname, prop);
        }

        return prop;
    }

    public void setString(String propname, String value) {
        TransientProperty prop = initProperty(propname);
        prop.setStringValue(value);
        this.lastmodified = System.currentTimeMillis();
    }

    public void setInteger(String propname, long value) {
        TransientProperty prop = initProperty(propname);
        prop.setIntegerValue(value);
        this.lastmodified = System.currentTimeMillis();
    }

    public void setFloat(String propname, double value) {
        TransientProperty prop = initProperty(propname);
        prop.setFloatValue(value);
        this.lastmodified = System.currentTimeMillis();
    }

    public void setBoolean(String propname, boolean value) {
        TransientProperty prop = initProperty(propname);
        prop.setBooleanValue(value);
        this.lastmodified = System.currentTimeMillis();
    }

    public void setDate(String propname, Date value) {
        TransientProperty prop = initProperty(propname);
        prop.setDateValue(value);
        this.lastmodified = System.currentTimeMillis();
    }

    public void setJavaObject(String propname, Object value) {
        TransientProperty prop = initProperty(propname);
        prop.setJavaObjectValue(value);
        this.lastmodified = System.currentTimeMillis();
    }

    public void setNode(String propname, NodeInterface value) {
        TransientProperty prop = initProperty(propname);
        prop.setNodeValue(value);

        // check if the main identity of this node is as a named property
        // or as an anonymous node in a collection
        if (value instanceof TransientNode) {
            TransientNode n = (TransientNode) value;

            if (n.parent == null) {
                n.name = propname;
                n.parent = this;
                n.anonymous = false;
            }
        }

        this.lastmodified = System.currentTimeMillis();
    }

    public void unset(String propname) {
        if (this.propMap != null && propname != null) {
            this.propMap.remove(propname);
            this.lastmodified = System.currentTimeMillis();
        }
    }

    public long lastModified() {
        return this.lastmodified;
    }

    public long created() {
        return this.created;
    }

    @Override
    public String toString() {
        return Messages.getString("TransientNode.2") + this.name; //$NON-NLS-1$
    }

    /**
     * Get the cache node for this node. This can
     * be used to store transient cache data per node
     * from Javascript.
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

    private String correctPropertyName(String propname) {
        return correctPropertyName(propname);
    }
}
