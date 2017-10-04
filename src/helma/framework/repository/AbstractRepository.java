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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

/**
 * Provides common methods and fields for the default implementations of the
 * repository interface
 */
public abstract class AbstractRepository implements RepositoryInterface {


    /**
     * Parent repository this repository is contained in.
     */
    RepositoryInterface parent;

    /**
     * Holds direct child repositories
     */
    RepositoryInterface[] repositories;

    /**
     * Holds direct resources
     */
    HashMap resources;

    /**
     * Cached name for faster access
     */
    String name;

    /**
     * Cached short name for faster access
     */
    String shortName;

    /*
     * empty repository array for convenience
     */
    final static RepositoryInterface[] emptyRepositories = new RepositoryInterface[0]; 

    /**
     * Called to check the repository's content.
     */
    public abstract void update();

    /**
     * Called to create a child resource for this repository
     */
    protected abstract ResourceInterface createResource(String name);

    /**
     * Get the full name that identifies this repository globally
     */
    public String getName() {
        return this.name;
    }

    /**
     * Get the local name that identifies this repository locally within its
     * parent repository
     */
    public String getShortName() {
        return this.shortName;
    }

    /**
     * Get this repository's logical script root repository.
     *
     *@see {isScriptRoot()}
     */
    public RepositoryInterface getRootRepository() {
        if (this.parent == null || isScriptRoot()) {
            return this;
        }
        return this.parent.getRootRepository();
    }

    /**
     * Get a resource contained in this repository identified by the given local name.
     * If the name can't be resolved to a resource, a resource object is returned
     * for which {@link ResourceInterface exists()} returns <code>false<code>.
     */
    public synchronized ResourceInterface getResource(String name) {
        update();

        ResourceInterface res = (ResourceInterface) this.resources.get(name);
        // if resource does not exist, create it
        if (res == null) {
            res = createResource(name);
            this.resources.put(name, res);
        }
        return res;
    }

    /**
     * Get an iterator over the resources contained in this repository.
     */
    public synchronized Iterator getResources() {
        update();

        return this.resources.values().iterator();
    }

    /**
     * Get an iterator over the sub-repositories contained in this repository.
     */
    public synchronized RepositoryInterface[] getRepositories() {
        update();

        return this.repositories;
    }

    /**
     * Get this repository's parent repository.
     */
    public RepositoryInterface getParentRepository() {
        return this.parent;
    }

    /**
     * Get a deep list of this repository's resources, including all resources
     * contained in sub-reposotories.
     */
    public synchronized List getAllResources() throws IOException {
        update();

        ArrayList allResources = new ArrayList();
        allResources.addAll(this.resources.values());

        for (int i = 0; i < this.repositories.length; i++) {
            allResources.addAll(this.repositories[i].getAllResources());
        }

        return allResources;
    }

    /**
     * Returns the repositories full name as string representation.
     * @see {getName()}
     */
    @Override
    public String toString() {
        return getName();
    }

}
