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

import helma.util.StringUtils;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public final class ZipRepository extends AbstractRepository {

    // zip file serving sub-repositories and zip file resources
    private File file;

    // the nested directory depth of this repository
    private int depth;

    String entryPath;

    private long lastModified = -1;

    /**
     * Constructs a ZipRespository using the given argument
     * @param initArgs absolute path to the zip file
     */
    public ZipRepository(String initArgs) {
        this(new File(initArgs), null, null);
    }

    /**
     * Constructs a ZipRespository using the given argument
     * @param file zip file
     */
    public ZipRepository(File file) {
        this(file, null, null);
    }

    /**
     * Constructs a ZipRepository using the given zip file as top-level
     * repository
     * @param file a zip file
     * @param parent the parent repository, or null
     */
    public ZipRepository(File file, Repository parent) {
        this(file, parent, null);
    }

    /**
     * Constructs a ZipRepository using the zip entryName belonging to the given
     * zip file and top-level repository
     * @param file a zip file
     * @param zipentry zip entryName
     * @param parent repository
     */
    private ZipRepository(File file, Repository parent, ZipEntry zipentry) {
        // make sure our file has an absolute path,
        // see http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4117557
        if (file.isAbsolute()) {
            this.file = file;
        } else {
            this.file = file.getAbsoluteFile();
        }
        this.parent = parent;

        if (zipentry == null) {
            name = shortName = file.getName();
            depth = 0;
            entryPath = "";
        } else {
            String[] pathArray = StringUtils.split(zipentry.getName(), "/");
            depth = pathArray.length;
            shortName = pathArray[depth - 1];
            entryPath = zipentry.getName();
            name = new StringBuffer(parent.getName())
                                   .append('/').append(shortName).toString();
        }
    }

    /**
     * Returns a java.util.zip.ZipFile for this repository. It is the caller's
     * responsability to call close() in it when it is no longer needed.
     * @return a ZipFile for reading
     * @throws IOException
     */
    protected ZipFile getZipFile() throws IOException {
        return new ZipFile(file);
    }

    public synchronized void update() {
        if (file.lastModified() != lastModified ||
                repositories == null ||
                resources == null) {
            lastModified = file.lastModified();
            ZipFile zipfile = null;

            try {
                zipfile = getZipFile();
                Enumeration en = zipfile.entries();
                HashMap newRepositories = new HashMap();
                HashMap newResources = new HashMap();

                while (en.hasMoreElements()) {
                    ZipEntry entry = (ZipEntry) en.nextElement();
                    String eName = entry.getName();

                    if (!eName.regionMatches(0, entryPath, 0, entryPath.length())) {
                        // names don't match - not a child of ours
                        continue;
                    }
                    String[] entrypath = StringUtils.split(eName, "/");
                    if (depth > 0 && !shortName.equals(entrypath[depth-1])) {
                        // catch case where our name is Foo and other's is FooBar
                        continue;
                    }

                    // create new repositories and resources for all entries with a
                    // path depth of this.depth + 1
                    if (entrypath.length == depth + 1 && !entry.isDirectory()) {
                        // create a new child resource
                        ZipResource resource = new ZipResource(entry.getName(), this);
                        newResources.put(resource.getShortName(), resource);
                    } else if (entrypath.length > depth) {
                        // create a new child repository
                        if (!newRepositories.containsKey(entrypath[depth])) {
                            ZipEntry child = composeChildEntry(entrypath[depth]);
                            ZipRepository rep = new ZipRepository(file, this, child);
                            newRepositories.put(entrypath[depth], rep);
                        }
                    }
                }

                repositories = (Repository[]) newRepositories.values()
                        .toArray(new Repository[newRepositories.size()]);
                resources = newResources;

            } catch (Exception ex) {
                ex.printStackTrace();
                repositories = emptyRepositories;
                if (resources == null) {
                    resources = new HashMap();
                } else {
                    resources.clear();
                }

            } finally {
                try {
                    // unlocks the zip file in the underlying filesystem
                    zipfile.close();
                } catch (Exception ex) {}
            }
        }
    }

    private ZipEntry composeChildEntry(String name) {
        if (entryPath == null || entryPath.length() == 0) {
            return new ZipEntry(name);
        } else if (entryPath.endsWith("/")) {
            return new ZipEntry(entryPath + name);
        } else {
            return new ZipEntry(entryPath + "/" + name);
        }
    }

    /**
     * Called to create a child resource for this repository
     */
    protected Resource createResource(String name) {
        return new ZipResource(entryPath + "/" + name, this);
    }

    public long getChecksum() {
        return file.lastModified();
    }

    public boolean exists() {
        ZipFile zipfile = null;
        try {
            /* a ZipFile needs to be created to see if the zip file actually
             exists; this is not cached to provide blocking the zip file in
             the underlying filesystem */
            zipfile = getZipFile();
            return true;
        } catch (IOException ex) {
            return false;
        }
        finally {
            try {
                // unlocks the zip file in the underlying filesystem
                zipfile.close();
            } catch (Exception ex) {
                return false;
            }
        }
    }

    public void create() {
        // we do not create zip files as it makes no sense
        throw new UnsupportedOperationException("create() not implemented for ZipRepository");
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
        return depth == 0;
    }

    public long lastModified() {
        return file.lastModified();
    }

    public int hashCode() {
        return 17 + (37 * file.hashCode()) + (37 * name.hashCode());
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof ZipRepository)) {
            return false;
        }

        ZipRepository rep = (ZipRepository) obj;
        return (file.equals(rep.file) && name.equals(rep.name));
    }

    public String toString() {
        return new StringBuffer("ZipRepository[").append(name).append("]").toString();
    }

}
