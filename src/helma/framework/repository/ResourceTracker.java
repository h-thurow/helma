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

import java.io.IOException;

/**
 * A utility class that allows Resource consumers to track changes
 * on resources.
 */
public class ResourceTracker {

    Resource resource;
    long lastModified;

    public ResourceTracker(Resource resource) {
        this.resource = resource;
        markClean();
    }

    public boolean hasChanged() throws IOException {
        return lastModified != resource.lastModified();
    }

    public void markClean() {
        lastModified = resource.lastModified();
    }

    public Resource getResource() {
        return resource;
    }
}
