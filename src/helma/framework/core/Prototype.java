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
import helma.util.ResourceProperties;
import helma.util.WrappedMap;
import helma.framework.repository.ResourceInterface;
import helma.framework.repository.RepositoryInterface;
import helma.framework.repository.ResourceTracker;
import helma.framework.repository.FileResource;
import helma.scripting.ScriptingEngineInterface;

import java.io.*;
import java.util.*;

/**
 * The Prototype class represents Script prototypes/type defined in a Helma
 * application. This class manages a prototypes templates, functions and actions
 * as well as optional information about the mapping of this type to a
 * relational database table.
 */
public final class Prototype {
    // the app this prototype belongs to
    Application app;

    // this prototype's name in natural and lower case
    String name;
    String lowerCaseName;

    // this prototype's resources
    ResourceInterface[] resources;

    // tells us the checksum of the repositories at the time we last updated them
    long lastChecksum = -1;

    // the time at which any of the prototype's files were found updated the last time
    volatile long lastCodeUpdate = 0;

    TreeSet code;
    TreeSet skins;

    HashMap trackers;

    TreeSet repositories;

    // a map of this prototype's skins as raw strings
    // used for exposing skins to application (script) code (via app.skinfiles).
    SkinMap skinMap;

    DbMapping dbmap;

    private Prototype parent;

    ResourceProperties props;

    /**
     * Creates a new Prototype object.
     *
     * @param name the prototype's name
     * @param repository the first prototype's repository
     * @param app the application this prototype is a part of
     * @param typeProps custom type mapping properties
     */
    public Prototype(String name, RepositoryInterface repository, Application app, Map typeProps) {
        // app.logEvent ("Constructing Prototype "+app.getName()+"/"+name);
        this.app = app;
        this.name = name;
        this.repositories = new TreeSet(app.getResourceComparator());
        if (repository != null) {
            this.repositories.add(repository);
        }
        this.lowerCaseName = name.toLowerCase();

        // Create and register type properties file
        this.props = new ResourceProperties(app);
        if (typeProps != null) {
            this.props.putAll(typeProps);
        } else if (repository != null) {
            this.props.addResource(repository.getResource("type.properties")); //$NON-NLS-1$
            this.props.addResource(repository.getResource(name + ".properties")); //$NON-NLS-1$
        }

        this.dbmap = new DbMapping(app, name, this.props);
        // we don't need to put the DbMapping into proto.updatables, because
        // dbmappings are checked separately in TypeManager.checkFiles() for
        // each request

        this.code = new TreeSet(app.getResourceComparator());
        this.skins = new TreeSet(app.getResourceComparator());

        this.trackers = new HashMap();

        this.skinMap = new SkinMap();
    }

    /**
     *  Return the application this prototype is a part of
     */
    public Application getApplication() {
        return this.app;
    }

    /**
     * Adds an repository to the list of repositories
     * @param repository repository to add
     * @param update indicates whether to immediately update the prototype with the new code
     * @throws IOException if reading/updating from the repository fails
     */
    public void addRepository(RepositoryInterface repository, boolean update) throws IOException {
        if (!this.repositories.contains(repository)) {
            this.repositories.add(repository);
            this.props.addResource(repository.getResource("type.properties")); //$NON-NLS-1$
            this.props.addResource(repository.getResource(this.name + ".properties")); //$NON-NLS-1$
            if (update) {
                RequestEvaluator eval = this.app.getCurrentRequestEvaluator();
                ScriptingEngineInterface engine = eval == null ? null : eval.scriptingEngine;
                Iterator it = repository.getAllResources().iterator();
                while (it.hasNext()) {
                    checkResource((ResourceInterface) it.next(), engine);
                }
            }
        }
    }

    /**
     * Check a prototype for new or updated resources. After this has been
     * called the code and skins collections of this prototype should be
     * up-to-date and the lastCodeUpdate be set if there has been any changes.
     */
    public synchronized void checkForUpdates() {
        boolean updatedResources = false;

        // check if any resource the prototype knows about has changed or gone
        for (Iterator i = this.trackers.values().iterator(); i.hasNext();) {
            ResourceTracker tracker = (ResourceTracker) i.next();

            if (tracker.hasChanged()) {
                updatedResources = true;
                // let tracker know we've seen the update
                tracker.markClean();
                // if resource has gone remove it
                if (!tracker.getResource().exists()) {
                    i.remove();
                    String name = tracker.getResource().getName();
                    if (name.endsWith(TypeManager.skinExtension)) {
                        this.skins.remove(tracker.getResource());
                    } else {
                        this.code.remove(tracker.getResource());
                    }
                }
            }
        }

        // next we check if resources have been created or removed
        ResourceInterface[] resources = getResources();

        for (int i = 0; i < resources.length; i++) {
            updatedResources |= checkResource(resources[i], null);
        }

        if (updatedResources) {
            // mark prototype as dirty and the code as updated
            this.lastCodeUpdate = System.currentTimeMillis();
            this.app.typemgr.setLastCodeUpdate(this.lastCodeUpdate);
        }
    }

    private boolean checkResource(ResourceInterface res, ScriptingEngineInterface engine) {
        String name = res.getName();
        boolean updated = false;
        if (!this.trackers.containsKey(name)) {
            if (name.endsWith(TypeManager.templateExtension) ||
                    name.endsWith(TypeManager.scriptExtension) ||
                    name.endsWith(TypeManager.actionExtension) ||
                    name.endsWith(TypeManager.skinExtension)) {
                updated = true;
                if (name.endsWith(TypeManager.skinExtension)) {
                    this.skins.add(res);
                } else {
                    if (engine != null) {
                        engine.injectCodeResource(this.lowerCaseName, res);
                    }
                    this.code.add(res);
                }
                this.trackers.put(res.getName(), new ResourceTracker(res));
            }
        }
        return updated;
    }

    /**
     *  Returns the list of resources in this prototype's repositories. Used
     *  by checkForUpdates() to see whether there is anything new.
     */
    public ResourceInterface[] getResources() {
        long checksum = getRepositoryChecksum();
        // reload resources if the repositories checksum has changed
        if (checksum != this.lastChecksum) {
            ArrayList list = new ArrayList();
            Iterator iterator = this.repositories.iterator();

            while (iterator.hasNext()) {
                try {
                    list.addAll(((RepositoryInterface) iterator.next()).getAllResources());
                } catch (IOException iox) {
                    iox.printStackTrace();
                }
            }

            this.resources = (ResourceInterface[]) list.toArray(new ResourceInterface[list.size()]);
            this.lastChecksum = checksum;
        }
        return this.resources;
    }

    /**
     * Returns an array of repositories containing code for this prototype.
     */
    public RepositoryInterface[] getRepositories() {
        return (RepositoryInterface[]) this.repositories.toArray(new RepositoryInterface[this.repositories.size()]);
    }

    /**
     *  Get a checksum over this prototype's repositories. This tells us
     *  if any resources were added or removed.
     */
    long getRepositoryChecksum() {
        long checksum = 0;
        Iterator iterator = this.repositories.iterator();

        while (iterator.hasNext()) {
            try {
                checksum += ((RepositoryInterface) iterator.next()).getChecksum();
            } catch (IOException iox) {
                iox.printStackTrace();
            }
        }

        return checksum;
    }

    /**
     *  Set the parent prototype of this prototype, i.e. the prototype this
     *  prototype inherits from.
     */
    public void setParentPrototype(Prototype parent) {
        // this is not allowed for the hopobject and global prototypes
        if ("hopobject".equals(this.lowerCaseName) || "global".equals(this.lowerCaseName)) { //$NON-NLS-1$ //$NON-NLS-2$
            return;
        }

        this.parent = parent;
    }

    /**
     *  Get the parent prototype from which we inherit, or null
     *  if we are top of the line.
     */
    public Prototype getParentPrototype() {
        return this.parent;
    }

    /**
     * Check if the given prototype is within this prototype's parent chain.
     */
    public final boolean isInstanceOf(String pname) {
        if (this.name.equalsIgnoreCase(pname)) {
            return true;
        }

        if (this.parent != null) {
            return this.parent.isInstanceOf(pname);
        }

        return false;
    }

    /**
     * Register an object as handler for all our parent prototypes, but only if
     * a handler by that prototype name isn't registered yet. This is used to
     * implement direct over indirect prototype precedence and child over parent
     *  precedence.
     */
    public final void registerParents(Map handlers, Object obj) {

        Prototype p = this.parent;

        while ((p != null) && !"hopobject".equals(p.getLowerCaseName())) { //$NON-NLS-1$
            Object old = handlers.put(p.name, obj);
            // if an object was already registered by this name, put it back in again.
            if (old != null) {
                handlers.put(p.name, old);
            }
            // same with lower case name
            old = handlers.put(p.lowerCaseName, obj);
            if (old != null) {
                handlers.put(p.lowerCaseName, old);
            }

            p = p.parent;
        }
    }

    /**
     * Get the DbMapping associated with this prototype
     */
    public DbMapping getDbMapping() {
        return this.dbmap;
    }

    /**
     *  Get a skin for this prototype. This only works for skins
     *  residing in the prototype directory, not for skins files in
     *  other locations or database stored skins. If parentName and
     *  subName are defined, the skin may be a subskin of another skin.
     */
    public Skin getSkin(Prototype proto, String skinname, String subskin, Object[] skinpath)
            throws IOException {
        ResourceInterface res = this.skinMap.getResource(skinname);
        while (res != null) {
            Skin skin = Skin.getSkin(res, this.app);
            if (subskin == null && skin.hasMainskin()) {
                return skin;
            } else if (subskin != null && skin.hasSubskin(subskin)) {
                return skin.getSubskin(subskin);
            }
            String baseskin = skin.getExtends();
            if (baseskin != null && !baseskin.equalsIgnoreCase(skinname)) {
                // we need to call SkinManager.getSkin() to fetch overwritten
                // base skins from skinpath
                return this.app.skinmgr.getSkin(proto, baseskin, subskin, skinpath);
            }
            res = res.getOverloadedResource();
        }
        return null;
    }

    /**
     * Return this prototype's name
     *
     * @return ...
     */
    public String getName() {
        return this.name;
    }

    /**
     * Return this prototype's name in lower case letters
     *
     * @return ...
     */
    public String getLowerCaseName() {
        return this.lowerCaseName;
    }

    /**
     *  Get the last time any script has been re-read for this prototype.
     */
    public long lastCodeUpdate() {
        return this.lastCodeUpdate;
    }

    /**
     *  Signal that some script in this prototype has been
     *  re-read from disk and needs to be re-compiled by
     *  the evaluators.
     */
    public void markUpdated() {
        this.lastCodeUpdate = System.currentTimeMillis();
    }

    /**
     * Set the custom type properties for this prototype and update the database mapping.
     * @param map the custom type mapping properties.
     */
    public void setTypeProperties(Map map) {
        this.props.clear();
        this.props.putAll(map);
        this.dbmap.update();
    }

    /**
     * Get the prototype's aggregated type.properties
     *
     * @return type.properties
     */
    public ResourceProperties getTypeProperties() {
        return this.props;
    }

    /**
     *  Return an iterator over this prototype's code resoruces. Synchronized
     *  to not return a collection in a transient state where it is just being
     *  updated by the type manager.
     *
     *  @return an iterator of this prototype's code resources
     */
    public synchronized Iterator getCodeResources() {
    	// if code has never been updated, do so now before returning an empty or incomplete list
    	if (this.lastCodeUpdate == 0) {
    		checkForUpdates();
    	}
    	
        // we copy over to a new list, because the underlying set may grow
        // during compilation through use of app.addRepository()
        return new ArrayList(this.code).iterator();
    }

    /**
     *  Return an iterator over this prototype's skin resoruces. Synchronized
     *  to not return a collection in a transient state where it is just being
     *  updated by the type manager.
     *
     *  @return an iterator over this prototype's skin resources
     */
    public Iterator getSkinResources() {
        return this.skins.iterator();
    }

    /**
     *  Return a string representing this prototype.
     */
    @Override
    public String toString() {
        return "[Prototype " + this.app.getName() + "/" + this.name + "]"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
    }

    /**
     * Get a map containing this prototype's skins as strings
     *
     * @return a scriptable skin map
     */
    public Map getScriptableSkinMap() {
        return new ScriptableSkinMap(new SkinMap());
    }

    /**
     * Get a map containing this prototype's skins as strings, overloaded by the
     * skins found in the given skinpath.
     *
     * @return a scriptable skin map
     */
    public Map getScriptableSkinMap(Object[] skinpath) {
        return new ScriptableSkinMap(new SkinMap(skinpath));
    }

    /**
     * A map of this prototype's skins that acts as a native JavaScript object in
     * rhino and returns the skins as strings. This is used to expose the skins
     * to JavaScript in app.skinfiles[prototypeName][skinName].
     */
    class ScriptableSkinMap extends WrappedMap {

        public ScriptableSkinMap(Map wrapped) {
            super(wrapped);
        }

        @Override
        public Object get(Object key) {
            ResourceInterface res = (ResourceInterface) super.get(key);

            if (res == null || !res.exists()) {
                return null;
            }

            try {
                return res.getContent();
            } catch (IOException iox) {
                return null;
            }
        }
    }

    /**
     * A Map that dynamically expands to all skins in this prototype.
     */
    class SkinMap extends HashMap {
        private static final long serialVersionUID = -8855785541204100909L;

        volatile long lastSkinmapLoad = -1;
        Object[] skinpath;

        SkinMap() {
            this.skinpath = null;
        }

        SkinMap(Object[] path) {
            this.skinpath = path;
        }

        @Override
        public boolean containsKey(Object key) {
            checkForUpdates();
            return super.containsKey(key);
        }

        @Override
        public boolean containsValue(Object value) {
            checkForUpdates();
            return super.containsValue(value);
        }

        @Override
        public Set entrySet() {
            checkForUpdates();
            return super.entrySet();
        }

        @Override
        public boolean equals(Object obj) {
            checkForUpdates();
            return super.equals(obj);
        }

        public Skin getSkin(Object key) throws IOException {
            ResourceInterface res = (ResourceInterface) get(key);

            if (res != null) {
                return Skin.getSkin(res, Prototype.this.app);
            }
            return null;
        }

        public ResourceInterface getResource(Object key) {
            return (ResourceInterface) get(key);
        }

        @Override
        public Object get(Object key) {
            checkForUpdates();
            return super.get(key);
        }

        @Override
        public int hashCode() {
            checkForUpdates();
            return super.hashCode();
        }

        @Override
        public boolean isEmpty() {
            checkForUpdates();
            return super.isEmpty();
        }

        @Override
        public Set keySet() {
            checkForUpdates();
            return super.keySet();
        }

        @Override
        public Object put(Object key, Object value) {
            // checkForUpdates ();
            return super.put(key, value);
        }

        @Override
        public void putAll(Map t) {
            // checkForUpdates ();
            super.putAll(t);
        }

        @Override
        public Object remove(Object key) {
            checkForUpdates();
            return super.remove(key);
        }

        @Override
        public int size() {
            checkForUpdates();
            return super.size();
        }

        @Override
        public Collection values() {
            checkForUpdates();
            return super.values();
        }

        private void checkForUpdates() {
            if (Prototype.this.lastCodeUpdate > this.lastSkinmapLoad) {
                if (Prototype.this.lastCodeUpdate == 0) {
                    // if prototype resources haven't been checked yet, check them now
                    Prototype.this.checkForUpdates();
                }
                loadSkins();
            }
        }

        private synchronized void loadSkins() {
            if (Prototype.this.lastCodeUpdate == this.lastSkinmapLoad) {
                return;
            }

            super.clear();

            // load Skins
            for (Iterator i = Prototype.this.skins.iterator(); i.hasNext();) {
                ResourceInterface res = (ResourceInterface) i.next();
                ResourceInterface prev = (ResourceInterface) super.put(res.getBaseName(), res);
                res.setOverloadedResource(prev);
            }

            // if skinpath is not null, overload/add skins from there
            if (this.skinpath != null) {
                for (int i = this.skinpath.length - 1; i >= 0; i--) {
                    if ((this.skinpath[i] != null) && this.skinpath[i] instanceof String) {
                        loadSkinFiles((String) this.skinpath[i]);
                    }
                }
            }

            this.lastSkinmapLoad = Prototype.this.lastCodeUpdate;
        }

        private void loadSkinFiles(String skinDir) {
            File dir = new File(skinDir, Prototype.this.getName());
            // if directory does not exist use lower case property name
            if (!dir.isDirectory()) {
                dir = new File(skinDir, Prototype.this.getLowerCaseName());
                if (!dir.isDirectory()) {
                    return;
                }
            }

            String[] skinNames = dir.list(Prototype.this.app.skinmgr);

            if ((skinNames == null) || (skinNames.length == 0)) {
                return;
            }

            for (int i = 0; i < skinNames.length; i++) {
                String name = skinNames[i].substring(0, skinNames[i].length() - 5);
                File file = new File(dir, skinNames[i]);

                ResourceInterface res = new FileResource(file);
                ResourceInterface prev = (ResourceInterface) super.put(name, res);
                res.setOverloadedResource(prev);
            }

        }

        @Override
        public String toString() {
            return "[SkinMap " + Prototype.this.name + "]";  //$NON-NLS-1$//$NON-NLS-2$
        }
    }
}