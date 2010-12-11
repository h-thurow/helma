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

package helma.framework.core;

import helma.objectmodel.db.DbMapping;
import helma.framework.repository.ResourceInterface;
import helma.framework.repository.RepositoryInterface;
import helma.util.StringUtils;

import java.io.*;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;

/**
 * The type manager periodically checks the prototype definitions for its
 * applications and updates the evaluators if anything has changed.
 */
public final class TypeManager {
    final static String[] standardTypes = { "User", "Global", "Root", "HopObject" }; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
    final static String templateExtension = ".hsp"; //$NON-NLS-1$
    final static String scriptExtension = ".js"; //$NON-NLS-1$
    final static String actionExtension = ".hac"; //$NON-NLS-1$
    final static String skinExtension = ".skin"; //$NON-NLS-1$

    private Application app;
    // map of prototypes
    private HashMap prototypes;

    // set of Java archives
    private HashSet jarfiles;

    // set of directory names to ignore
    private HashSet ignoreDirs;

    private long lastCheck = 0;
    private long lastCodeUpdate;
    private HashMap lastRepoScan;

    // app specific class loader, includes jar files in the app directory
    private AppClassLoader loader;

    /**
     * Creates a new TypeManager object.
     *
     * @param app ...
     *
     * @throws RuntimeException ...
     */
    public TypeManager(Application app, String ignore) {
        this.app = app;
        this.prototypes = new HashMap();
        this.jarfiles = new HashSet();
        this.ignoreDirs = new HashSet();
        this.lastRepoScan = new HashMap();
        // split ignore dirs list and add to hash set
        if (ignore != null) {
            String[] arr = StringUtils.split(ignore, ","); //$NON-NLS-1$
            for (int i=0; i<arr.length; i++)
                this.ignoreDirs.add(arr[i].trim());
        }

        URL helmajar = TypeManager.class.getResource("/"); //$NON-NLS-1$

        if (helmajar == null) {
            // Helma classes are in jar file, get helma.jar URL
            URL[] urls = ((URLClassLoader) TypeManager.class.getClassLoader()).getURLs();

            for (int i = 0; i < urls.length; i++) {
                String url = urls[i].toString().toLowerCase();
                if (url.endsWith("helma.jar")) { //$NON-NLS-1$
                    helmajar = urls[i];
                    break;
                }
            }
        }

        if (helmajar == null) {
            // throw new RuntimeException("helma.jar not found in embedding classpath");
            this.loader = new AppClassLoader(app.getName(), new URL[0]);
        } else {
            this.loader = new AppClassLoader(app.getName(), new URL[] { helmajar });
        }
    }

    /**
     * Run through application's prototype directories and create prototypes, but don't
     * compile or evaluate any scripts.
     */
    public synchronized void createPrototypes() throws IOException {
        // create standard prototypes.
        for (int i = 0; i < standardTypes.length; i++) {
            createPrototype(standardTypes[i], null, null);
        }

        // loop through directories and create prototypes
        checkRepositories();
    }

    /**
     * Run through application's prototype directories and check if anything
     * has been updated.
     * If so, update prototypes and scripts.
     */
    public synchronized void checkPrototypes() throws IOException {
        if ((System.currentTimeMillis() - this.lastCheck) < 1000L) {
            return;
        }

        checkRepositories();

        this.lastCheck = System.currentTimeMillis();
    }

    protected synchronized void checkRepository(RepositoryInterface repository, boolean update) throws IOException {
        RepositoryInterface[] list = repository.getRepositories();
        for (int i = 0; i < list.length; i++) {
 
            // ignore dir name found - compare to shortname (= Prototype name)
            if (this.ignoreDirs.contains(list[i].getShortName())) {
                // jump this repository
                if (this.app.debug) {
                    this.app.logEvent(Messages.getString("TypeManager.0") + list[i].getName() + Messages.getString("TypeManager.1")); //$NON-NLS-1$ //$NON-NLS-2$
                }
                continue;
            }

            if (list[i].isScriptRoot()) {
                // this is an embedded top-level script repository 
                if (this.app.addRepository(list[i], list[i].getParentRepository())) {
                    // repository is new, check it
                    checkRepository(list[i], update);
                }
            } else {
                // it's an prototype
                String name = list[i].getShortName();
                Prototype proto = getPrototype(name);

                // if prototype doesn't exist, create it
                if (proto == null) {
                    // create new prototype if type name is valid
                    if (isValidTypeName(name)) 
                        createPrototype(name, list[i], null);
                } else {
                    proto.addRepository(list[i], update);
                }
            }
        }

        Iterator resources = repository.getResources();
        while (resources.hasNext()) {
            // check for jar files to add to class loader
            ResourceInterface resource = (ResourceInterface) resources.next();
            String name = resource.getName();
            if (name.endsWith(".jar")) { //$NON-NLS-1$
                if (!this.jarfiles.contains(name)) {
                    this.jarfiles.add(name);
                    try {
                        this.loader.addURL(resource.getUrl());
                    } catch (UnsupportedOperationException x) {
                        // not implemented by all kinds of resources
                    }
                }
            }
        }
    }

    /**
     * Run through application's prototype sources and check if
     * there are any prototypes to be created.
     */
    private synchronized void checkRepositories() throws IOException {
        List list = this.app.getRepositories();

        // walk through repositories and check if any of them have changed.
        for (int i = 0; i < list.size(); i++) {
            RepositoryInterface repository = (RepositoryInterface) list.get(i);
            long lastScan = this.lastRepoScan.containsKey(repository) ?
                    ((Long) this.lastRepoScan.get(repository)).longValue() : 0;
            if (repository.lastModified() != lastScan) {
                this.lastRepoScan.put(repository, new Long(repository.lastModified()));
                checkRepository(repository, false);
            }
        }

        boolean debug = "true".equalsIgnoreCase(this.app.getProperty("helma.debugTypeManager")); //$NON-NLS-1$ //$NON-NLS-2$
        if (debug) {
            System.err.println(Messages.getString("TypeManager.2") + Thread.currentThread()); //$NON-NLS-1$
        }

        // loop through prototypes and check if type.properties needs updates
        // it's important that we do this _after_ potentially new prototypes
        // have been created in the previous loop.
        for (Iterator i = this.prototypes.values().iterator(); i.hasNext();) {
            Prototype proto = (Prototype) i.next();

            if (debug) {
                System.err.println(Messages.getString("TypeManager.3") + proto.getName() + Messages.getString("TypeManager.4") + Thread.currentThread()); //$NON-NLS-1$ //$NON-NLS-2$
            }            

            // update prototype's type mapping
            DbMapping dbmap = proto.getDbMapping();

            if ((dbmap != null) && dbmap.needsUpdate()) {
                // call dbmap.update(). This also checks the
                // parent prototype for prototypes other than
                // global and HopObject, which is a bit awkward...
                // I mean we're the type manager, so this should
                // be part of our job, right?
                proto.props.update();
                dbmap.update();
            }
        }
        if (debug) {
            System.err.println(Messages.getString("TypeManager.5") + Thread.currentThread()); //$NON-NLS-1$
        }
    }

    private boolean isValidTypeName(String str) {
        if (str == null) {
            return false;
        }

        char[] c = str.toCharArray();

        for (int i = 0; i < c.length; i++)
            if (!Character.isJavaIdentifierPart(c[i])) {
                return false;
            }

        return true;
    }

    /**
     *  Returns the last time any resource in this app was modified.
     *  This can be used to find out quickly if any file has changed.
     */
    public long getLastCodeUpdate() {
        return this.lastCodeUpdate;
    }

    /**
     *  Set the last time any resource in this app was modified.
     */
    public void setLastCodeUpdate(long update) {
        this.lastCodeUpdate = update;
    }

    /**
     * Return the class loader used by this application.
     *
     * @return the ClassLoader
     */
    public ClassLoader getClassLoader() {
        return this.loader;
    }

    /**
     * Return a collection containing the prototypes defined for this type
     * manager.
     *
     * @return a collection containing the prototypes
     */
    public synchronized Collection getPrototypes() {
        return Collections.unmodifiableCollection(this.prototypes.values());
    }

    /**
     *   Get a prototype defined for this application
     */
    public synchronized Prototype getPrototype(String typename) {
        if (typename == null) {
            return null;
        }
        return (Prototype) this.prototypes.get(typename.toLowerCase());
    }

    /**
     * Create and register a new Prototype.
     *
     * @param typename the name of the prototype
     * @param repository the first prototype source
     * @param typeProps custom type mapping properties
     * @return the newly created prototype
     */
    public synchronized Prototype createPrototype(String typename, RepositoryInterface repository, Map typeProps) {
        if ("true".equalsIgnoreCase(this.app.getProperty("helma.debugTypeManager"))) { //$NON-NLS-1$ //$NON-NLS-2$
            System.err.println(Messages.getString("TypeManager.6") + typename + Messages.getString("TypeManager.7") + repository + Messages.getString("TypeManager.8") + Thread.currentThread()); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            // Thread.dumpStack();
        }
        Prototype proto = new Prototype(typename, repository, this.app, typeProps);
        // put the prototype into our map
        this.prototypes.put(proto.getLowerCaseName(), proto);
        return proto;
    }

}
