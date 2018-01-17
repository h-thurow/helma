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

package helma.main.launcher;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLDecoder;
import java.util.ArrayList;

/**
 *  Helma bootstrap class. Basically this is a convenience wrapper that takes over
 *  the job of setting the class path and helma install directory before launching
 *  the static main(String[]) method in <code>helma.main.Server</code>. This class
 *  should be invoked from a jar file in the Helma install directory in order to
 *  be able to set up class and install paths.
 */
public class Main {
    private Class serverClass;
    private Object server;

    private static final Class[] EMPTY_CLASS_ARRAY = new Class[0];
    private static final Object[] EMPTY_OBJECT_ARRAY = new Object[0];

    /**
     * Helma boot method. This retrieves the Helma home directory, creates the
     * classpath and invokes main() in helma.main.Server.
     *
     * @param args command line arguments
     *
     */
    public static void main(String[] args) {
        Main main = new Main();
        main.init(args);
        main.start();
    }

    public void init(String[] args) {
        try {
            String installDir = getInstallDir(args);
            ClassLoader loader = createClassLoader(installDir);
            // get the main server class
            this.serverClass = loader.loadClass("helma.main.Server"); //$NON-NLS-1$
            Class[] cargs = new Class[]{args.getClass()};
            Method loadServer = this.serverClass.getMethod("loadServer", cargs); //$NON-NLS-1$
            Object[] nargs = new Object[]{args};
            // and invoke the static loadServer(String[]) method
            this.server = loadServer.invoke(null, nargs);
            Method init = this.serverClass.getMethod("init", EMPTY_CLASS_ARRAY); //$NON-NLS-1$
            init.invoke(this.server, EMPTY_OBJECT_ARRAY);
        } catch (Exception x) {
            // unable to get Helma installation dir from launcher jar
            System.err.println(Messages.getString("Main.0")); //$NON-NLS-1$
            x.printStackTrace();
            System.exit(2);
        }
    }

    public void start() {
        try {
            Method start = this.serverClass.getMethod("start", EMPTY_CLASS_ARRAY); //$NON-NLS-1$
            start.invoke(this.server, EMPTY_OBJECT_ARRAY);
        } catch (Exception x) {
            // unable to get Helma installation dir from launcher jar
            System.err.println(Messages.getString("Main.1")); //$NON-NLS-1$
            x.printStackTrace();
            System.exit(2);
        }
    }

    public void stop() {
        try {
            Method start = this.serverClass.getMethod("stop", EMPTY_CLASS_ARRAY); //$NON-NLS-1$
            start.invoke(this.server, EMPTY_OBJECT_ARRAY);
        } catch (Exception x) {
            // unable to get Helma installation dir from launcher jar
            System.err.println(Messages.getString("Main.2")); //$NON-NLS-1$
            x.printStackTrace();
            System.exit(2);
        }
    }

    public void destroy() {
        try {
            Method start = this.serverClass.getMethod("shutdown", EMPTY_CLASS_ARRAY); //$NON-NLS-1$
            start.invoke(this.server, EMPTY_OBJECT_ARRAY);
        } catch (Exception x) {
            // unable to get Helma installation dir from launcher jar
            System.err.println(Messages.getString("Main.3")); //$NON-NLS-1$
            x.printStackTrace();
            System.exit(2);
        }
    }

    static void addJars(ArrayList jarlist, File dir) throws MalformedURLException {
        File[] files = dir.listFiles(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                String n = name.toLowerCase();
                return n.endsWith(".jar") || n.endsWith(".zip");  //$NON-NLS-1$//$NON-NLS-2$
            }
        });

        if (files != null) {
            for (int i = 0; i < files.length; i++) {
                jarlist.add(new URL("file:" + files[i].getAbsolutePath())); //$NON-NLS-1$
                System.err.println(Messages.getString("Main.8") + ": " + files[i].getAbsolutePath()); //$NON-NLS-1$ //$NON-NLS-2$
            }
        }
    }

    /**
     * Create a server-wide ClassLoader from our install directory.
     * This will be used as parent ClassLoader for all application
     * ClassLoaders.
     *
     * @param installDir
     * @return the main classloader we'll be using
     * @throws MalformedURLException
     */
    public static ClassLoader createClassLoader(String installDir)
            throws MalformedURLException, UnsupportedEncodingException {

        // decode installDir in case it is URL-encoded
        installDir = URLDecoder.decode(installDir, System.getProperty("helma.urlEncoding", "UTF-8")); //$NON-NLS-1$ //$NON-NLS-2$

        // set up the class path
        File libdir = new File(installDir, "lib"); //$NON-NLS-1$
        ArrayList jarlist = new ArrayList();

        // add all jar files from the lib directory
        addJars(jarlist, libdir);
        // add all jar files from the lib/ext directory
        addJars(jarlist, new File(libdir, "ext")); //$NON-NLS-1$

        URL[] urls = new URL[jarlist.size()];

        jarlist.toArray(urls);

        // find out if system classes should be excluded from class path
        String excludeSystemClasses = System.getProperty("helma.excludeSystemClasses"); //$NON-NLS-1$

        ClassLoader loader;

        if ("true".equalsIgnoreCase(excludeSystemClasses)) { //$NON-NLS-1$
            loader = new URLClassLoader(urls, null);
        } else {
            loader = new URLClassLoader(urls);
        }

        // set the new class loader as context class loader
        Thread.currentThread().setContextClassLoader(loader);
        return loader;
    }


    /**
     * Get the Helma install directory from the command line -i argument or
     * from the Jar URL from which this class was loaded. Additionally, the
     * System property "helma.home" is set to the install directory path.
     *
     * @param args
     * @return the base install directory we're running in
     * @throws IOException
     * @throws MalformedURLException
     */
    public static String getInstallDir(String[] args)
            throws IOException, MalformedURLException {
        // check if home directory is set via command line arg. If not,
        // we'll get it from the location of the jar file this class
        // has been loaded from.
        String installDir = null;

        // first, try to get helma home dir from command line options
        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("-i") && ((i + 1) < args.length)) { //$NON-NLS-1$
                installDir = args[i + 1];
            }
        }

        // try to get Helma installation directory
        if (installDir == null) {
            URL launcherUrl = ClassLoader.getSystemClassLoader()
                    .getResource("helma/main/launcher/Main.class"); //$NON-NLS-1$

            // this is a  JAR URL of the form
            //    jar:<url>!/{entry}
            // we strip away the jar: prefix and the !/{entry} suffix
            // to get the original jar file URL

            String jarUrl = launcherUrl.toString();

            if (!jarUrl.startsWith("jar:") || jarUrl.indexOf("!") < 0) { //$NON-NLS-1$ //$NON-NLS-2$
                installDir = System.getProperty("user.dir"); //$NON-NLS-1$
                System.err.println(Messages.getString("Main.5")); //$NON-NLS-1$
                System.err.println(Messages.getString("Main.6")); //$NON-NLS-1$
                System.err.println(Messages.getString("Main.7")); //$NON-NLS-1$
            } else {
                jarUrl = jarUrl.substring(4);

                int excl = jarUrl.indexOf("!"); //$NON-NLS-1$
                jarUrl = jarUrl.substring(0, excl);
                launcherUrl = new URL(jarUrl);

                File f = new File(launcherUrl.getPath()).getAbsoluteFile();

                installDir = f.getParentFile().getCanonicalPath();
            }
        }
        // set System property
        System.setProperty("helma.home", installDir); //$NON-NLS-1$
        // and return install dir
        return installDir;
    }

}
