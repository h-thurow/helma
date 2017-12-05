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

package helma.framework.repository;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

/**
 * An empty file repository not having any resources or sub-repositories.
 * This repository is useful for applications, which don't use the application's main directory for
 * Helma-resources, but nevertheless want to use the application's main directory for non Helma-resources
 * like CommonJS or NPM-style modules.
 */
public class EmptyFileRepository extends FileRepository {

    /**
     * Creates an empty file repository with the given directory path.
     * 
     * @param initArgs
     *  The directory path.
     * @throws IOException
     *  When the directory does not exist yet, but can't be created neither.
     */
    public EmptyFileRepository(String initArgs) throws IOException {
        // delegate
        this(new File(initArgs), null);
    }

    /**
     * Creates an empty file repository with the given directory.
     * 
     * @param dir
     *  The directory.
     * @throws IOException
     *  When the directory does not exist yet, but can't be created neither.
     */
    public EmptyFileRepository(File dir) throws IOException {
        // delegate
        this(dir, null);
    }

    /**
     * Creates an empty file repository with the given directory.
     * 
     * @param dir
     *  The directory.
     * @param parent
     *  The parent directory.
     * @throws IOException
     *  When the directory does not exist yet, but can't be created neither.
     */
    public EmptyFileRepository(File dir, RepositoryInterface parent) {
        // do what would have been done anyways
        super(dir, parent);

        // there are no sub-repositories
        this.repositories = this.emptyRepositories;
        // there are no resources
        this.resources = new HashMap();
    }

    @Override
    public synchronized long getChecksum() throws IOException {
       // return the directorie's last modified time 
       return this.lastModified;
    }

    @Override
    public boolean isScriptRoot() {
        // this repository is always a script root repository
        return true;
    }

    @Override
    public synchronized void update() {
        // check if the directory doesn't exist yet
        if (!this.directory.exists()) {
            // the directory was not modified yet, as such the repository was not modified yet
            this.lastModified = 0;
        } else {
            // the repository was modified when the directory was modified
            this.lastModified = this.directory.lastModified();
        }
    }

    @Override
    protected ResourceInterface createResource(String name) {
        // no resources can be created in this repository
        return null;
    }

    /**
     * Returns the repositorie's directory.
     * 
     * @return
     *  The repositorie's directory.
     */
    public File getDirectory() {
        // return the repositorie's directory
        return this.directory;
    }

    @Override
    public int hashCode() {
        // return the directorie's hash code
        return this.directory.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        // the repository equals, if it is an empty file repository for the same directory
        return obj instanceof EmptyFileRepository && 
                this.directory.equals(((EmptyFileRepository) obj).directory);
    }

    @Override
    public String toString() {
        // return the repositorie's name
        return new StringBuffer("EmptyFileRepository[").append(this.name).append("]").toString(); //$NON-NLS-1$ //$NON-NLS-2$
    }

    @Override
    public synchronized ResourceInterface getResource(String name) {
        // there are no resources in this repository
        return null;
    }

}