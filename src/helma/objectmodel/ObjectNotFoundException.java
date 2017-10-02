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

package helma.objectmodel;


/**
 * Thrown when an object could not found in the database where
 * it was expected.
 */
public class ObjectNotFoundException extends Exception {
    private static final long serialVersionUID = -5368941052804232094L;

    /**
     * Creates a new ObjectNotFoundException object.
     *
     * @param msg ...
     */
    public ObjectNotFoundException(String msg) {
        super(msg);
    }
}
