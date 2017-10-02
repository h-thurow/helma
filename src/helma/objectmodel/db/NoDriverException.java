/*
 * Helma License Notice
 *
 * The contents of this file are subject to the Helma License
 * Version 2.0 (the "License"). You may not use this file except in
 * compliance with the License. A copy of the License is available at
 * http://adele.helma.org/download/helma/license.txt
 *
 * Copyright 2017 Daniel Ruthardt. All Rights Reserved.
 */

package helma.objectmodel.db;

/**
 * Represents the information, that some database operation didn't succeed, because of the corresponding JDBC driver
 * not being present.
 */
public class NoDriverException extends Exception {

    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = -4498614734005646616L;

    /**
     * @param cause
     *  The cause for the no driver exception.
     */
    public NoDriverException(Throwable cause) {
        // delegate
        super(cause);
    }

}
