/*
 * Helma License Notice
 *
 * The contents of this file are subject to the Helma License
 * Version 2.0 (the "License"). You may not use this file except in
 * compliance with the License. A copy of the License is available at
 * http://adele.helma.org/download/helma/license.txt
 *
 * Copyright 1998-2006 Helma Software. All Rights Reserved.
 *
 * $RCSfile$
 * $Author$
 * $Revision$
 * $Date$
 */

package helma.framework.repository;

import java.io.File;
import java.util.List;
import java.util.Iterator;
import java.util.LinkedList;

public class SingleFileRepository implements RepositoryInterface {

    final ResourceInterface res;
    final RepositoryInterface parent;
    final RepositoryInterface[] repositories;
    final LinkedList resources = new LinkedList();
    final LinkedList allResources = new LinkedList();
    final boolean isScriptFile;

    /**
     * Constructs a SingleFileRepository using the given argument
     * @param initArgs absolute path to the script file
     */
    public SingleFileRepository(String initArgs) {
        this(new File(initArgs), null);
    }

    /**
     * Constructs a SingleFileRepository using the given argument
     * @param file the script file
     */
    public SingleFileRepository(File file) {
        this(file, null);
    }

    /**
     * Constructs a SingleFileRepository using the given argument
     * @param file the script file
     * @param parent the parent repository, or null
     */
    public SingleFileRepository(File file, RepositoryInterface parent) {
        this.parent = parent;
        this.res = new FileResource(file, this);
        this.allResources.add(this.res);
        this.isScriptFile = file.getName().endsWith(".js"); //$NON-NLS-1$
        if (this.isScriptFile) {
            this.repositories = new RepositoryInterface[] { new FakeGlobal() };
        } else {
            this.repositories = AbstractRepository.emptyRepositories;
            this.resources.add(this.res);
        }
    }

    /**
     * Checksum of the repository and all its content. Implementations
     * should make sure
     *
     * @return checksum
     * @throws java.io.IOException
     */
    public long getChecksum() {
        return this.res.lastModified();
    }

    /**
     * Returns the name of the repository.
     *
     * @return name of the repository
     */
    public String getShortName() {
        return this.res.getShortName();
    }

    /**
     * Returns the name of the repository; this is a full name including all
     * parent repositories.
     *
     * @return full name of the repository
     */
    public String getName() {
        return this.res.getName();
    }

    /**
     * Get this repository's logical script root repository.
     *
     * @return top-level repository
     * @see {isScriptRoot()}
     */
    public RepositoryInterface getRootRepository() {
        return this;
    }

    /**
     * Returns this repository's parent repository.
     * Returns null if this repository already is the top-level repository
     *
     * @return the parent repository
     */
    public RepositoryInterface getParentRepository() {
        return this.parent;
    }

    /**
     * Checks wether the repository is to be considered a top-level
     * repository from a scripting point of view. For example, a zip
     * file within a file repository is not a root repository from
     * a physical point of view, but from the scripting point of view it is.
     *
     * @return true if the repository is to be considered a top-level script repository
     */
    public boolean isScriptRoot() {
        return false;
    }

    /**
     * Creates the repository if does not exist yet
     *
     * @throws java.io.IOException
     */
    public void create() {
        // noop
    }

    /**
     * Checks wether the repository actually (or still) exists
     *
     * @return true if the repository exists
     * @throws java.io.IOException
     */
    public boolean exists() {
        return this.res.exists();
    }

    /**
     * Returns this repository's direct child repositories
     *
     * @return direct repositories
     * @throws java.io.IOException
     */
    public RepositoryInterface[] getRepositories() {
        return this.repositories;
    }

    /**
     * Returns all direct and indirect resources
     *
     * @return resources recursive
     * @throws java.io.IOException
     */
    public List getAllResources() {
        return this.resources;
    }

    /**
     * Returns all direct resources
     *
     * @return direct resources
     * @throws java.io.IOException
     */
    public Iterator getResources() {
        return this.resources.iterator();
    }

    /**
     * Returns a specific direct resource of the repository
     *
     * @param resourceName name of the child resource to return
     * @return specified child resource
     */
    public ResourceInterface getResource(String resourceName) {
        if (!this.isScriptFile && this.res.getName().equals(resourceName)) {
            return this.res;
        }
        return null;
    }

    /**
     * Returns the date the repository was last modified.
     *
     * @return last modified date
     * @throws java.io.IOException
     */
    public long lastModified() {
        return this.res.lastModified();
    }

    /**
     * Return our single resource.
     * @return the wrapped resource
     */
    protected ResourceInterface getResource() {
        return this.res;
    }

    /**
     * Indicates whether some other object is "equal to" this one.
     */
    @Override
    public boolean equals(Object obj) {
        return (obj instanceof SingleFileRepository &&
                this.res.equals(((SingleFileRepository) obj).res));
    }

    /**
     * Returns a hash code value for the object.
     */
    @Override
    public int hashCode() {
        return this.res.hashCode();
    }

    /**
     * Returns a string representation of the object.
     */
    @Override
    public String toString() {
        return new StringBuffer("SingleFileRepository[") //$NON-NLS-1$
                .append(this.res.getName()).append("]").toString(); //$NON-NLS-1$
    }

    class FakeGlobal implements RepositoryInterface {

        /**
         * Checksum of the repository and all its content. Implementations
         * should make sure
         *
         * @return checksum
         * @throws java.io.IOException
         */
        public long getChecksum() {
            return SingleFileRepository.this.res.lastModified();
        }

        /**
         * Returns the name of the repository.
         *
         * @return name of the repository
         */
        public String getShortName() {
            // we need to return "Global" here in order to be recognized as
            // global code folder - that's the whole purpose of this class
            return "Global"; //$NON-NLS-1$
        }

        /**
         * Returns the name of the repository; this is a full name including all
         * parent repositories.
         *
         * @return full name of the repository
         */
        public String getName() {
            return SingleFileRepository.this.res.getName();
        }

        /**
         * Get this repository's logical script root repository.
         *
         * @return top-level repository
         * @see {isScriptRoot()}
         */
        public RepositoryInterface getRootRepository() {
            return SingleFileRepository.this;
        }

        /**
         * Returns this repository's parent repository.
         * Returns null if this repository already is the top-level repository
         *
         * @return the parent repository
         */
        public RepositoryInterface getParentRepository() {
            return SingleFileRepository.this;
        }

        /**
         * Checks wether the repository is to be considered a top-level
         * repository from a scripting point of view. For example, a zip
         * file within a file repository is not a root repository from
         * a physical point of view, but from the scripting point of view it is.
         *
         * @return true if the repository is to be considered a top-level script repository
         */
        public boolean isScriptRoot() {
            return false;
        }

        /**
         * Creates the repository if does not exist yet
         *
         * @throws java.io.IOException
         */
        public void create() {
        }

        /**
         * Checks wether the repository actually (or still) exists
         *
         * @return true if the repository exists
         * @throws java.io.IOException
         */
        public boolean exists() {
            return SingleFileRepository.this.res.exists();
        }

        /**
         * Returns this repository's direct child repositories
         *
         * @return direct repositories
         * @throws java.io.IOException
         */
        public RepositoryInterface[] getRepositories() {
            return AbstractRepository.emptyRepositories;
        }

        /**
         * Returns all direct and indirect resources
         *
         * @return resources recursive
         * @throws java.io.IOException
         */
        public List getAllResources() {
            return SingleFileRepository.this.allResources;
        }

        /**
         * Returns all direct resources
         *
         * @return direct resources
         * @throws java.io.IOException
         */
        public Iterator getResources() {
            return SingleFileRepository.this.allResources.iterator();
        }

        /**
         * Returns a specific direct resource of the repository
         *
         * @param resourceName name of the child resource to return
         * @return specified child resource
         */
        public ResourceInterface getResource(String resourceName) {
            if (SingleFileRepository.this.res.getName().equals(resourceName)) {
                return SingleFileRepository.this.res;
            }
            return null;
        }

        /**
         * Returns the date the repository was last modified.
         *
         * @return last modified date
         * @throws java.io.IOException
         */
        public long lastModified() {
            return SingleFileRepository.this.res.lastModified();
        }
    }


}

