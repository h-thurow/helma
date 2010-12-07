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

import helma.objectmodel.INode;
import helma.objectmodel.IProperty;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * A property implementation for Nodes stored inside a database. Basically
 * the same as for transient nodes, with a few hooks added.
 */
public final class Property implements IProperty, Serializable, Cloneable, Comparable {
    static final long serialVersionUID = -1022221688349192379L;
    private String propname;
    private Node node;
    private Object value;
    private int type;
    transient boolean dirty;

    /**
     * Creates a new Property object.
     *
     * @param node ...
     */
    public Property(Node node) {
        this.node = node;
        this.dirty = true;
    }

    /**
     * Creates a new Property object.
     *
     * @param propname ...
     * @param node ...
     */
    public Property(String propname, Node node) {
        this.propname = propname;
        this.node = node;
        this.dirty = true;
    }

    /**
     * Creates a new Property object.
     *
     * @param propname ...
     * @param node ...
     * @param valueNode ...
     */
    public Property(String propname, Node node, Node valueNode) {
        this(propname, node);
        this.type = NODE;
        this.value = (valueNode == null) ? null : valueNode.getHandle();
        this.dirty = true;
    }

    private void readObject(ObjectInputStream in) throws IOException {
        try {
            this.propname = in.readUTF();
            this.node = (Node) in.readObject();
            this.type = in.readInt();

            switch (this.type) {
                case STRING:
                    this.value = in.readObject();

                    break;

                case BOOLEAN:
                    this.value = in.readBoolean() ? Boolean.TRUE : Boolean.FALSE;

                    break;

                case INTEGER:
                    this.value = new Long(in.readLong());

                    break;

                case DATE:
                    this.value = new Date(in.readLong());

                    break;

                case FLOAT:
                    this.value = new Double(in.readDouble());

                    break;

                case NODE:
                    this.value = in.readObject();

                    break;

                case JAVAOBJECT:
                    this.value = in.readObject();

                    break;
            }
        } catch (ClassNotFoundException x) {
            throw new IOException(x.toString());
        }
    }

    private void writeObject(ObjectOutputStream out) throws IOException {
        out.writeUTF(this.propname);
        out.writeObject(this.node);
        out.writeInt(this.type);

        switch (this.type) {
            case STRING:
                out.writeObject(this.value);

                break;

            case BOOLEAN:
                out.writeBoolean(((Boolean) this.value).booleanValue());

                break;

            case INTEGER:
                out.writeLong(((Long) this.value).longValue());

                break;

            case DATE:
                out.writeLong(((Date) this.value).getTime());

                break;

            case FLOAT:
                out.writeDouble(((Double) this.value).doubleValue());

                break;

            case NODE:
                out.writeObject(this.value);

                break;

            case JAVAOBJECT:

                if ((this.value != null) && !(this.value instanceof Serializable)) {
                    out.writeObject(null);
                } else {
                    out.writeObject(this.value);
                }

                break;
        }
    }

    /**
     *  Get the name of the property
     *
     * @return this property's name
     */
    public String getName() {
        return this.propname;
    }

    /**
     *  Set the name of the property
     */
    protected void setName(String name) {
        this.propname = name;
    }

    /**
     *
     *
     * @return the property's value in its native class
     */
    public Object getValue() {
        return this.value;
    }

    /**
     *
     *
     * @return the property's type as defined in helma.objectmodel.IProperty.java
     */
    public int getType() {
        return this.type;
    }

    /**
     * Directly set the value of this property.
     */
    protected void setValue(Object value, int type) {
        this.value = value;
        this.type = type;
        this.dirty = true;
    }

    /**
     *
     *
     * @param str ...
     */
    public void setStringValue(String str) {
        this.type = STRING;
        this.value = str;
        this.dirty = true;
    }

    /**
     *
     *
     * @param l ...
     */
    public void setIntegerValue(long l) {
        this.type = INTEGER;
        this.value = new Long(l);
        this.dirty = true;
    }

    /**
     *
     *
     * @param d ...
     */
    public void setFloatValue(double d) {
        this.type = FLOAT;
        this.value = new Double(d);
        this.dirty = true;
    }

    /**
     *
     *
     * @param date ...
     */
    public void setDateValue(Date date) {
        this.type = DATE;
        // normalize from java.sql.* Date subclasses
        if (date != null && date.getClass() != Date.class) {
            this.value = new Date(date.getTime());
        } else {
            this.value = date;
        }
        this.dirty = true;
    }

    /**
     *
     *
     * @param bool ...
     */
    public void setBooleanValue(boolean bool) {
        this.type = BOOLEAN;
        this.value = bool ? Boolean.TRUE : Boolean.FALSE;
        this.dirty = true;
    }

    /**
     *
     *
     * @param node ...
     */
    public void setNodeValue(Node node) {
        this.type = NODE;
        this.value = (node == null) ? null : node.getHandle();
        this.dirty = true;
    }

    /**
     *
     *
     * @param handle ...
     */
    public void setNodeHandle(NodeHandle handle) {
        this.type = NODE;
        this.value = handle;
        this.dirty = true;
    }

    /**
     *
     *
     * @return ...
     */
    public NodeHandle getNodeHandle() {
        if (this.type == NODE) {
            return (NodeHandle) this.value;
        }

        return null;
    }

    /**
     *
     *
     * @param rel the Relation
     */
    public void convertToNodeReference(Relation rel) {
        if ((this.value != null) && !(this.value instanceof NodeHandle)) {
            if (rel.usesPrimaryKey()) {
                this.value = new NodeHandle(new DbKey(rel.otherType, this.value.toString()));
            } else {
                this.value = new NodeHandle(new MultiKey(rel.otherType, rel.getKeyParts(this.node)));
            }
        }

        this.type = NODE;
    }

    /**
     *
     *
     * @param obj ...
     */
    public void setJavaObjectValue(Object obj) {
        this.type = JAVAOBJECT;
        this.value = obj;
        this.dirty = true;
    }


    /**
     *
     *
     * @return ...
     */
    public String getStringValue() {
        if (this.value == null) {
            return null;
        }

        switch (this.type) {
            case STRING:
            case BOOLEAN:
            case INTEGER:
            case FLOAT:
            case JAVAOBJECT:
                return this.value.toString();

            case DATE:

                SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"); //$NON-NLS-1$

                return format.format((Date) this.value);

            case NODE:
                return ((NodeHandle) this.value).getID();
        }

        return ""; //$NON-NLS-1$
    }

    /**
     *
     *
     * @return ...
     */
    @Override
    public String toString() {
        return getStringValue();
    }

    /**
     *
     *
     * @return ...
     */
    public long getIntegerValue() {
        if (this.type == INTEGER) {
            return ((Long) this.value).longValue();
        }

        if (this.type == FLOAT) {
            return ((Double) this.value).longValue();
        }

        if (this.type == BOOLEAN) {
            return ((Boolean) this.value).booleanValue() ? 1 : 0;
        }

        try {
            return Long.parseLong(getStringValue());
        } catch (Exception x) {
            return 0;
        }
    }

    /**
     *
     *
     * @return ...
     */
    public double getFloatValue() {
        if (this.type == FLOAT) {
            return ((Double) this.value).doubleValue();
        }

        if (this.type == INTEGER) {
            return ((Long) this.value).doubleValue();
        }

        try {
            return Double.parseDouble(getStringValue());
        } catch (Exception x) {
            return 0.0;
        }
    }

    /**
     *
     *
     * @return ...
     */
    public Date getDateValue() {
        if (this.type == DATE) {
            return (Date) this.value;
        }

        return null;
    }

    /**
     *
     *
     * @return ...
     */
    public Timestamp getTimestampValue() {
        if ((this.type == DATE) && (this.value != null)) {
            return new Timestamp(((Date) this.value).getTime());
        }

        return null;
    }

    /**
     *
     *
     * @return ...
     */
    public boolean getBooleanValue() {
        if (this.type == BOOLEAN) {
            return ((Boolean) this.value).booleanValue();
        }

        return 0 != getIntegerValue();
    }

    /**
     *
     *
     * @return ...
     */
    public INode getNodeValue() {
        if ((this.type == NODE) && (this.value != null)) {
            NodeHandle nhandle = (NodeHandle) this.value;

            return nhandle.getNode(this.node.nmgr);
        }

        return null;
    }

    /**
     *
     *
     * @return ...
     */
    public Object getJavaObjectValue() {
        if (this.type == JAVAOBJECT) {
            return this.value;
        }

        return null;
    }

    /**
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     *
     * The following cases throw a ClassCastException
     * - Properties of a different type
     * - Properties of boolean or node type
     */
    public int compareTo(Object obj) {
        Property p = (Property) obj;
        int ptype = p.getType();
        Object pvalue = p.getValue();

        if (this.type==NODE || ptype==NODE ||
                this.type == BOOLEAN || ptype == BOOLEAN) {
            throw new ClassCastException(Messages.getString("Property.0") + this + Messages.getString("Property.1") + this.type + Messages.getString("Property.2") + p + Messages.getString("Property.3") + ptype + Messages.getString("Property.4")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
        }
        if (this.value==null && pvalue == null) {
            return 0;
        } else if (this.value == null) {
            return 1;
        } if (pvalue == null) {
            return -1;
        }
        if (this.type != ptype) {
            // float/integer sometimes get mixed up in Rhino
            if ((this.type == FLOAT && ptype == INTEGER) || (this.type == INTEGER && ptype == FLOAT))
                return Double.compare(((Number) this.value).doubleValue(), ((Number) pvalue).doubleValue());
            throw new ClassCastException(Messages.getString("Property.5") + this + Messages.getString("Property.6") + this.type + Messages.getString("Property.7") + p + Messages.getString("Property.8") + ptype + Messages.getString("Property.9")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$

        }
        if (!(this.value instanceof Comparable)) {
            throw new ClassCastException(Messages.getString("Property.10") + this.value + Messages.getString("Property.11") + this.value.getClass() + Messages.getString("Property.12")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        }
        // System.err.println("COMPARING: " + value.getClass() + " TO " + pvalue.getClass());
        return ((Comparable) this.value).compareTo(pvalue);
    }

    /**
     * Return true if object o is equal to this property.
     *
     * @param obj the object to compare to
     * @return true if this equals obj
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == this)
            return true;
        if (obj == null)
            return false;
        if (!(obj instanceof Property))
            return false;
        Property p = (Property) obj;
        return this.value == null ? p.value == null : this.value.equals(p.value);
    }
}