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

package helma.framework.demo;

import helma.framework.IPathElement;

/**
 * This is an example implementation for the helma.framework.IPathElement interface.
 * It creates any child element which is requested on the fly without ever asking.
 */
public class SimplePathElement implements IPathElement {
    String name;
    String prototype;
    IPathElement parent;

    /**
     *  Constructor for the root element.
     */
    public SimplePathElement() {
        this.name = "root"; //$NON-NLS-1$
        this.prototype = "root"; //$NON-NLS-1$
        this.parent = null;
    }

    /**
     * Constructor for non-root elements.
     */
    public SimplePathElement(String n, IPathElement p) {
        this.name = n;
        this.prototype = "hopobject"; //$NON-NLS-1$
        this.parent = p;
    }

    /**
     * Returns a child element for this object, creating it on the fly.
     */
    public IPathElement getChildElement(String n) {
        return new SimplePathElement(n, this);
    }

    /**
     * Returns this object's parent element
     */
    public IPathElement getParentElement() {
        return this.parent;
    }

    /**
     * Returns the element name to be used for this object.
     */
    public String getElementName() {
        return this.name;
    }

    /**
     * Returns the name of the scripting prototype to be used for this object.
     * This will be "root" for the root element and "hopobject for everything else.
     */
    public String getPrototype() {
        return this.prototype;
    }

    /**
     * Returns a string representation of this element.
     */
    @Override
    public String toString() {
        return "SimplePathElement " + this.name; //$NON-NLS-1$
    }
}
