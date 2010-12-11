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

package helma.objectmodel;

import java.io.*;
import java.text.*;
import java.util.Date;

/**
 * A property implementation for Nodes stored inside a database.
 */
public final class TransientProperty implements PropertyInterface, Serializable {
    private static final long serialVersionUID = 3128899239601365229L;

    protected String propname;
    protected TransientNode node;
    public String svalue;
    public boolean bvalue;
    public long lvalue;
    public double dvalue;
    public NodeInterface nvalue;
    public Object jvalue;
    public int type;

    /**
     * Creates a new Property object.
     *
     * @param node ...
     */
    public TransientProperty(TransientNode node) {
        this.node = node;
    }

    /**
     * Creates a new Property object.
     *
     * @param propname ...
     * @param node ...
     */
    public TransientProperty(String propname, TransientNode node) {
        this.propname = propname;
        this.node = node;
    }

    /**
     *
     *
     * @return ...
     */
    public String getName() {
        return this.propname;
    }

    /**
     *
     *
     * @return ...
     */
    public Object getValue() {
        switch (this.type) {
            case STRING:
                return this.svalue;

            case BOOLEAN:
                return new Boolean(this.bvalue);

            case INTEGER:
                return new Long(this.lvalue);

            case FLOAT:
                return new Double(this.dvalue);

            case DATE:
                return new Date(this.lvalue);

            case NODE:
                return this.nvalue;

            case JAVAOBJECT:
                return this.jvalue;
        }

        return null;
    }

    /**
     *
     *
     * @param value ...
     */
    public void setStringValue(String value) {
        if (this.type == NODE) {
            this.nvalue = null;
        }

        if (this.type == JAVAOBJECT) {
            this.jvalue = null;
        }

        this.type = STRING;
        this.svalue = value;
    }

    /**
     *
     *
     * @param value ...
     */
    public void setIntegerValue(long value) {
        if (this.type == NODE) {
            this.nvalue = null;
        }

        if (this.type == JAVAOBJECT) {
            this.jvalue = null;
        }

        this.type = INTEGER;
        this.lvalue = value;
    }

    /**
     *
     *
     * @param value ...
     */
    public void setFloatValue(double value) {
        if (this.type == NODE) {
            this.nvalue = null;
        }

        if (this.type == JAVAOBJECT) {
            this.jvalue = null;
        }

        this.type = FLOAT;
        this.dvalue = value;
    }

    /**
     *
     *
     * @param value ...
     */
    public void setDateValue(Date value) {
        if (this.type == NODE) {
            this.nvalue = null;
        }

        if (this.type == JAVAOBJECT) {
            this.jvalue = null;
        }

        this.type = DATE;
        this.lvalue = value.getTime();
    }

    /**
     *
     *
     * @param value ...
     */
    public void setBooleanValue(boolean value) {
        if (this.type == NODE) {
            this.nvalue = null;
        }

        if (this.type == JAVAOBJECT) {
            this.jvalue = null;
        }

        this.type = BOOLEAN;
        this.bvalue = value;
    }

    /**
     *
     *
     * @param value ...
     */
    public void setNodeValue(NodeInterface value) {
        if (this.type == JAVAOBJECT) {
            this.jvalue = null;
        }

        this.type = NODE;
        this.nvalue = value;
    }

    /**
     *
     *
     * @param value ...
     */
    public void setJavaObjectValue(Object value) {
        if (this.type == NODE) {
            this.nvalue = null;
        }

        this.type = JAVAOBJECT;
        this.jvalue = value;
    }

    /**
     *
     *
     * @return ...
     */
    public String getStringValue() {
        switch (this.type) {
            case STRING:
                return this.svalue;

            case BOOLEAN:
                return "" + this.bvalue; //$NON-NLS-1$

            case DATE:

                SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"); //$NON-NLS-1$

                return format.format(new Date(this.lvalue));

            case INTEGER:
                return Long.toString(this.lvalue);

            case FLOAT:
                return Double.toString(this.dvalue);

            case NODE:
                return this.nvalue.getName();

            case JAVAOBJECT:
                return (this.jvalue == null) ? null : this.jvalue.toString();
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
            return this.lvalue;
        }

        return 0;
    }

    /**
     *
     *
     * @return ...
     */
    public double getFloatValue() {
        if (this.type == FLOAT) {
            return this.dvalue;
        }

        return 0.0;
    }

    /**
     *
     *
     * @return ...
     */
    public Date getDateValue() {
        if (this.type == DATE) {
            return new Date(this.lvalue);
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
            return this.bvalue;
        }

        return false;
    }

    /**
     *
     *
     * @return ...
     */
    public NodeInterface getNodeValue() {
        if (this.type == NODE) {
            return this.nvalue;
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
            return this.jvalue;
        }

        return null;
    }

    /**
     *
     *
     * @return ...
     */
    public int getType() {
        return this.type;
    }
}
