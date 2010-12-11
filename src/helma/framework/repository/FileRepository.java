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

package helma.framework.repository;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * RepositoryInterface implementation for directories providing file resources
 */
public class FileRepository extends AbstractRepository {

    // Directory serving sub-repositories and file resources
    protected File directory;

    protected long lastModified = -1;
    protected long lastChecksum = 0;
    protected long lastChecksumTime = 0;

    /**
     * Defines how long the checksum of the repository will be cached
     */
    final long cacheTime = 1000L;

    /**
     * Constructs a FileRepository using the given argument
     * @param initArgs absolute path to the directory
     */
    public FileRepository(String initArgs) {
        this(new File(initArgs), null);
    }

    /**
     * Constructs a FileRepository using the given directory as top-level
     * repository
     * @param dir directory
     */
    public FileRepository(File dir) {
        this(dir, null);
    }

    /**
     * Constructs a FileRepository using the given directory and top-level
     * repository
     * @param dir directory
     * @param parent the parent repository, or null
     */
    public FileRepository(File dir, RepositoryInterface parent) {
        // make sure our directory has an absolute path,
        // see http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4117557
        if (dir.isAbsolute()) {
            this.directory = dir;
        } else {
            this.directory = dir.getAbsoluteFile();
        }
        if (!this.directory.exists()) {
            create();
        } else if (!this.directory.isDirectory()) {
            throw new IllegalArgumentException(Messages.getString("FileRepository.0") + this.directory + Messages.getString("FileRepository.1")); //$NON-NLS-1$ //$NON-NLS-2$
        }

        if (parent == null) {
            this.name = this.shortName = this.directory.getAbsolutePath();
        } else {
            this.parent = parent;
            this.shortName = this.directory.getName();
            this.name = this.directory.getAbsolutePath();
        }
    }

    public boolean exists() {
        return this.directory.exists() && this.directory.isDirectory();
    }

    public void create() {
        if (!this.directory.exists() || !this.directory.isDirectory()) {
            this.directory.mkdirs();
        }
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
        return this.parent == null || this.parent instanceof MultiFileRepository;
    }

    public long lastModified() {
        return this.directory.lastModified();
    }

    public synchronized long getChecksum() throws IOException {
        // delay checksum check if already checked recently
        if (System.currentTimeMillis() > this.lastChecksumTime + this.cacheTime) {

            update();
            long checksum = this.lastModified;

            for (int i = 0; i < this.repositories.length; i++) {
                checksum += this.repositories[i].getChecksum();
            }

            this.lastChecksum = checksum;
            this.lastChecksumTime = System.currentTimeMillis();
        }

        return this.lastChecksum;
    }

    /**
     * Updates the content cache of the repository
     * Gets called from within all methods returning sub-repositories or
     * resources
     */
    @Override
    public synchronized void update() {
        if (!this.directory.exists()) {
            this.repositories = emptyRepositories;
            if (this.resources == null) {
                this.resources = new HashMap();
            } else {
                this.resources.clear();
            }
            this.lastModified = 0;
            return;
        }

        if (this.directory.lastModified() != this.lastModified) {
            this.lastModified = this.directory.lastModified();

            File[] list = this.directory.listFiles();

            ArrayList newRepositories = new ArrayList(list.length);
            HashMap newResources = new HashMap(list.length);

            for (int i = 0; i < list.length; i++) {
                if (list[i].isDirectory()) {
                    // a nested directory aka child file repository
                    newRepositories.add(new FileRepository(list[i], this));
                } else if (list[i].getName().endsWith(".zip")) { //$NON-NLS-1$
                    // a nested zip repository
                    newRepositories.add(new ZipRepository(list[i], this));
                } else if (list[i].isFile()) {
                    // a file resource
                    FileResource resource = new FileResource(list[i], this);
                    newResources.put(resource.getShortName(), resource);
                }
            }

            this.repositories = (RepositoryInterface[])
                    newRepositories.toArray(new RepositoryInterface[newRepositories.size()]);
            this.resources = newResources;
        }
    }

    /**
     * Called to create a child resource for this repository
     */
    @Override
    protected ResourceInterface createResource(String name) {
        return new FileResource(new File(this.directory, name), this);
    }

    /**
     * Get the repository's directory
     */
    public File getDirectory() {
        return this.directory;
    }

    @Override
    public int hashCode() {
        return 17 + (37 * this.directory.hashCode());
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof FileRepository &&
               this.directory.equals(((FileRepository) obj).directory);
    }

    @Override
    public String toString() {
        return new StringBuffer("FileRepository[").append(this.name).append("]").toString();  //$NON-NLS-1$//$NON-NLS-2$
    }
}
