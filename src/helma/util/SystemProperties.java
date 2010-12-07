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
 * Contributions:
 *   Daniel Ruthardt
 *   Copyright 2010 dowee it solutions GmbH. All rights reserved.
 * 
 * $RCSfile$
 * $Author$
 * $Revision$
 * $Date$
 */

package helma.util;

import java.io.*;
import java.util.*;

/**
 *  A property dictionary that is updated from a property file each time the
 *  file is modified. It is also case insensitive.
 */
public final class SystemProperties extends Properties {

    private static final long serialVersionUID = -6994562125444162183L;

    final static long cacheTime = 1500L;
    private SystemProperties defaultProps; // the default/fallback properties.
    private File file; // the underlying properties file from which we read.
    private long lastread; // time we last read the underlying properties file
    private long lastcheck; // time we last checked the underlying properties file
    private long lastadd; // time we last added or removed additional props

    // map of additional properties
    private HashMap additionalProps = null;

    // are keys case sensitive? 
    private boolean ignoreCase = true;

    /**
     *  Construct an empty properties object.
     */
    public SystemProperties() {
        this(null, null);
    }

    /**
     *  Construct a properties object from a properties file.
     */
    public SystemProperties(String filename) {
        this(filename, null);
    }

    /**
     *  Contstruct a properties object with the given default properties.
     */
    public SystemProperties(SystemProperties defaultProps) {
        this(null, defaultProps);
    }

    /**
     *  Construct a properties object from a file name with the given default properties (ignoring case)
     */
    public SystemProperties(String filename, SystemProperties defaultProps) {
        // System.err.println ("building sysprops with file "+filename+" and node "+node);
        super(defaultProps);
        this.defaultProps = defaultProps;
        file = (filename == null) ? null : new File(filename);
        lastcheck = lastread = lastadd = 0;
    }

    /**
     *  Return the modify-time of the underlying properties file.
     */
    public long lastModified() {
        if ((file == null) || !file.exists()) {
            return lastadd;
        }

        return Math.max(file.lastModified(), lastadd);
    }

    /**
     *  Update/re-read the properties from file if necessary.
     */
    public void update () {
        checkFile();
    }

    /**
     *  Return a checksum that changes when something in the properties changes.
     */
    public long getChecksum() {
        if (defaultProps == null) {
            return lastModified();
        }

        return lastModified() + defaultProps.lastModified();
    }

    /**
     *  Private method to read file if it has been changed since the last time we did
     */
    private void checkFile() {
        if ((file != null) && (file.lastModified() > lastread)) {
            reload();
        }

        lastcheck = System.currentTimeMillis();
    }

    /**
     * Get the properties file
     *
     * @return the properties file
     */
    public File getFile() {
        return file;
    }

    /**
     * Reload properties. This clears out the existing entries,
     * loads the main properties file and then adds any additional
     * properties there may be (usually from zip files). This is used
     * internally by addProps() and removeProps().
     */
    private synchronized void reload() {
        // clear out old entries
        clear();

        // read from the primary file
        if (file != null && file.exists()) {
			FileReader reader = null;

			try {
				reader = new FileReader(file);
				load(reader);
			} catch (Exception x) {
				System.err.println(Messages.getString("SystemProperties.0") + file //$NON-NLS-1$
						+ ": " + x); //$NON-NLS-1$
			} finally {
				try {
					reader.close();
				} catch (Exception ignore) {
					// ignored
				}
			}
        }

        // read additional properties from zip files, if available
        if (additionalProps != null) {
            for (Iterator i = additionalProps.values().iterator(); i.hasNext();)
                putAll((Properties) i.next());
        }

        lastread = System.currentTimeMillis();
    }

    /**
     * Similar to load(), but adds to the existing properties instead
     * of discarding them.
     */
    public synchronized void addProps(String key, InputStream in)
                               throws IOException {
        Properties newProps = new SystemProperties();
        newProps.load(in);
        in.close();

        if (additionalProps == null) {
            additionalProps = new HashMap();
        }
        additionalProps.put(key, newProps);

        // fully reload properties and mark as updated
        reload();
        lastadd = System.currentTimeMillis();
    }

    /**
     *  Remove an additional properties dictionary.
     */
    public synchronized void removeProps(String key) {
        if (additionalProps != null) {
            // remove added properties for this key. If we had
            // properties associated with the key, mark props as updated.
            Object p = additionalProps.remove(key);

            if (p != null) {
                // fully reload properties and mark as updated
                reload();
                lastadd = System.currentTimeMillis();
            }
        }
    }

    /*
     * This should not be used directly if properties are read from file,
     *  otherwise changes will be lost whe the file is next modified.
     */
    public synchronized Object put(Object key, Object value) {
        // cut off trailing whitespace
        if (value != null) {
            value = value.toString().trim();
        }

        return super.put(ignoreCase ? key.toString().toLowerCase() : key, value);
    }

    /**
     *  Overrides method to act on the wrapped properties object.
     */
    public synchronized Object get(Object key) {
        if ((System.currentTimeMillis() - lastcheck) > cacheTime) {
            checkFile();
        }

        return super.get(ignoreCase ? key.toString().toLowerCase() : key);
    }

    /**
     *  Overrides method to act on the wrapped properties object.
     */
    public synchronized Object remove(Object key) {
        return super.remove(ignoreCase ? key.toString().toLowerCase() : key);
    }

    /**
     *  Overrides method to act on the wrapped properties object.
     */
    public synchronized boolean contains(Object obj) {
        if ((System.currentTimeMillis() - lastcheck) > cacheTime) {
            checkFile();
        }

        return super.contains(obj);
    }

    /**
     *  Overrides method to act on the wrapped properties object.
     */
    public synchronized boolean containsKey(Object key) {
        if ((System.currentTimeMillis() - lastcheck) > cacheTime) {
            checkFile();
        }

        return super.containsKey(ignoreCase ? key.toString().toLowerCase() : key);
    }

    /**
     *  Overrides method to act on the wrapped properties object.
     */
    public synchronized boolean isEmpty() {
        if ((System.currentTimeMillis() - lastcheck) > cacheTime) {
            checkFile();
        }

        return super.isEmpty();
    }

    /**
     *  Overrides method to act on the wrapped properties object.
     */
    public String getProperty(String name) {
        if ((System.currentTimeMillis() - lastcheck) > cacheTime) {
            checkFile();
        }

        return super.getProperty(ignoreCase ? name.toLowerCase() : name);
    }

    /**
     *  Overrides method to act on the wrapped properties object.
     */
    public String getProperty(String name, String defaultValue) {
        if ((System.currentTimeMillis() - lastcheck) > cacheTime) {
            checkFile();
        }

        return super.getProperty(ignoreCase ?
                name.toLowerCase() : name.toLowerCase(), defaultValue);
    }

    /**
     *  Overrides method to act on the wrapped properties object.
     */
    public synchronized Enumeration keys() {
        if ((System.currentTimeMillis() - lastcheck) > cacheTime) {
            checkFile();
        }

        return super.keys();
    }

    /**
     *  Overrides method to act on the wrapped properties object.
     */
    public Set keySet() {
        if ((System.currentTimeMillis() - lastcheck) > cacheTime) {
            checkFile();
        }

        return super.keySet();
    }

    /**
     *  Overrides method to act on the wrapped properties object.
     */
    public synchronized Enumeration elements() {
        if ((System.currentTimeMillis() - lastcheck) > cacheTime) {
            checkFile();
        }

        return super.elements();
    }

    /**
     *  Overrides method to act on the wrapped properties object.
     */
    public synchronized int size() {
        if ((System.currentTimeMillis() - lastcheck) > cacheTime) {
            checkFile();
        }

        return super.size();
    }

    /**
     *  Overrides method to act on the wrapped properties object.
     */
    public synchronized String toString() {
        return super.toString();
    }

    /**
     *  Turns case sensitivity for keys in this Map on or off.
     */
    public void setIgnoreCase(boolean ignore) {
        if (!super.isEmpty()) {
            throw new RuntimeException(Messages.getString("SystemProperties.1")); //$NON-NLS-1$
        }
        ignoreCase = ignore;
    }

    /**
     *  Returns true if this property map ignores key case
     */
    public boolean isIgnoreCase() {
        return ignoreCase;
    }

}
