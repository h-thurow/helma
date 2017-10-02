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

package helma.framework.repository;

/**
 * Abstract resource base class that implents get/setOverloadedResource.
 */
public abstract class AbstractResource implements ResourceInterface {

    protected ResourceInterface overloaded = null;

    /**
     * Method for registering a ResourceInterface this ResourceInterface is overloading
     *
     * @param res the overloaded resource
     */
    public void setOverloadedResource(ResourceInterface res) {
        this.overloaded = res;
    }

    /**
     * Get a ResourceInterface this ResourceInterface is overloading
     *
     * @return the overloaded resource
     */
    public ResourceInterface getOverloadedResource() {
        return this.overloaded;
    }
}
