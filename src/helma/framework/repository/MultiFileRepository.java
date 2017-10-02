/*
 * Helma License Notice
 *
 * The contents of this file are subject to the Helma License
 * Version 2.0 (the "License"). You may not use this file except in
 * compliance with the License. A copy of the License is available at
 * http://adele.helma.org/download/helma/license.txt
 *
 * Copyright 2005 Helma Software. All Rights Reserved.
 */

package helma.framework.repository;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * RepositoryInterface implementation that provides all of its subdirectories
 * as top-level FileRepositories
 *
 * @author Barbara Ondrisek
 */
public class MultiFileRepository extends FileRepository {

    /**
     * Constructs a MultiFileRepository using the given argument
     * @param initArgs absolute path to the directory
     */
    public MultiFileRepository(String initArgs) {
        this(new File(initArgs));
    }

    /**
     * Constructs a MultiFileRepository using the given directory as top-level
     * repository
     * @param dir directory
     */
    public MultiFileRepository(File dir) {
        super(dir, null);
    }

    /**
     * Updates the content cache of the repository. We override this
     * to create child repositories that act as top-level script repositories
     * rather than prototype repositories. Zip files are handled as top-level
     * script repositories like in FileRepository, while resources are ignored.
     */
    @Override
    public synchronized void update() {
        if (!this.directory.exists()) {
            this.repositories = emptyRepositories;
            if (this.resources != null)
                this.resources.clear();
            this.lastModified = 0;
            return;
        }

        if (this.directory.lastModified() != this.lastModified) {
            this.lastModified = this.directory.lastModified();

            File[] list = this.directory.listFiles();

            ArrayList newRepositories = new ArrayList(list.length);
            HashMap newResources = new HashMap(list.length);

            for (int i = 0; i < list.length; i++) {
                // create both directories and zip files as top-level repositories,
                // while resources (files) are ignored.
                if (list[i].isDirectory()) {
                    // a nested directory aka child file repository
                    newRepositories.add(new FileRepository(list[i], this));
                } else if (list[i].getName().endsWith(".zip")) { //$NON-NLS-1$
                    // a nested zip repository
                    newRepositories.add(new ZipRepository(list[i], this));
                }
            }

            this.repositories = (RepositoryInterface[])
                    newRepositories.toArray(new RepositoryInterface[newRepositories.size()]);
            this.resources = newResources;
        }
    }

    /**
     * get hashcode
     * @return int
     */
    @Override
    public int hashCode() {
        return 37 + (37 * this.directory.hashCode());
    }

    /**
     * equals object
     * @param obj Object
     * @return boolean
     */
    @Override
    public boolean equals(Object obj) {
        return obj instanceof MultiFileRepository &&
               this.directory.equals(((MultiFileRepository) obj).directory);
    }

    /**
     * get object serialized as string
     * @return String
     */
    @Override
    public String toString() {
        return new StringBuffer("MultiFileRepository[").append(this.name).append("]").toString();  //$NON-NLS-1$//$NON-NLS-2$
    }
}
