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
 * A utility class that allows ResourceInterface consumers to track changes
 * on resources.
 */
public class ResourceTracker {

    ResourceInterface resource;
    long lastModified;

    public ResourceTracker(ResourceInterface resource) {
        this.resource = resource;
        markClean();
    }

    public boolean hasChanged() {
        return this.lastModified != this.resource.lastModified();
    }

    public void markClean() {
        this.lastModified = this.resource.lastModified();
    }

    public ResourceInterface getResource() {
        return this.resource;
    }
}
