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

package helma.main;

import helma.framework.core.*;
import helma.framework.repository.Repository;
import helma.framework.repository.FileRepository;
import helma.util.StringUtils;
import org.apache.xmlrpc.XmlRpcHandler;
import org.apache.commons.logging.Log;
import org.mortbay.jetty.handler.ContextHandler;
import org.mortbay.jetty.handler.ContextHandlerCollection;
import org.mortbay.jetty.handler.ResourceHandler;
import org.mortbay.jetty.servlet.ServletHandler;
import org.mortbay.jetty.servlet.ServletHolder;

import java.io.*;
import java.util.*;
import helma.util.ResourceProperties;
import helma.servlet.EmbeddedServletClient;

/**
 * This class is responsible for starting and stopping Helma applications.
 */
public class ApplicationManager implements XmlRpcHandler {
    private Hashtable descriptors;
    private Hashtable applications;
    private Hashtable xmlrpcHandlers;
    private ResourceProperties props;
    private Server server;
    private long lastModified;
    private ContextHandlerCollection context;
    private JettyServer jetty = null;

    /**
     * Creates a new ApplicationManager object.
     *
     * @param props the properties defining the running apps
     * @param server the server instance
     */
    public ApplicationManager(ResourceProperties props, Server server) {
        this.props = props;
        this.server = server;
        descriptors = new Hashtable();
        applications = new Hashtable();
        xmlrpcHandlers = new Hashtable();
        lastModified = 0;
        jetty = server.jetty;
    }

    /**
     * Called regularely check applications property file
     * to create and start new applications.
     */
    protected void checkForChanges() {
        if (props.lastModified() > lastModified && server.getApplicationsOption() == null) {
            try {
                for (Enumeration e = props.keys(); e.hasMoreElements();) {
                    String appName = (String) e.nextElement();

                    if ((appName.indexOf(".") == -1) && //$NON-NLS-1$
                            (applications.get(appName) == null)) {
                        AppDescriptor appDesc = new AppDescriptor(appName);
                        appDesc.start();
                        appDesc.bind();
                    }
                }

                // then stop deleted ones
                for (Enumeration e = descriptors.elements(); e.hasMoreElements();) {
                    AppDescriptor appDesc = (AppDescriptor) e.nextElement();

                    // check if application has been removed and should be stopped
                    if (!props.containsKey(appDesc.appName)) {
                        appDesc.stop();
                    } else if (server.jetty != null) {
                        // If application continues to run, remount
                        // as the mounting options may have changed.
                        AppDescriptor ndesc = new AppDescriptor(appDesc.appName);
                        ndesc.app = appDesc.app;
                        appDesc.unbind();
                        ndesc.bind();
                        descriptors.put(ndesc.appName, ndesc);
                    }
                }
            } catch (Exception mx) {
                getLogger().error(Messages.getString("ApplicationManager.0"), mx); //$NON-NLS-1$
            }

            lastModified = System.currentTimeMillis();
        }
    }


    /**
     *  Start an application by name
     */
    public void start(String appName) {
        AppDescriptor desc = new AppDescriptor(appName);
        desc.start();
    }

    /**
     *  Bind an application by name
     */
    public void register(String appName) {
        AppDescriptor desc = (AppDescriptor) descriptors.get(appName);
        if (desc != null) {
            desc.bind();
        }
    }

    /**
     *  Stop an application by name
     */
    public void stop(String appName) {
        AppDescriptor desc = (AppDescriptor) descriptors.get(appName);
        if (desc != null) {
            desc.stop();
        }
    }


    /**
     * Start all applications listed in the properties
     */
    public void startAll() {
        try {
            String[] apps = server.getApplicationsOption();
            if (apps != null) {
                for (int i = 0; i < apps.length; i++) {
                    AppDescriptor desc = new AppDescriptor(apps[i]);
                    desc.start();
                }
            } else {
                for (Enumeration e = props.keys(); e.hasMoreElements();) {
                    String appName = (String) e.nextElement();

                    if (appName.indexOf(".") == -1) { //$NON-NLS-1$
                        String appValue = props.getProperty(appName);

                        if (appValue != null && appValue.length() > 0) {
                            appName = appValue;
                        }

                        AppDescriptor desc = new AppDescriptor(appName);
                        desc.start();
                    }
                }
            }

            for (Enumeration e = descriptors.elements(); e.hasMoreElements();) {
                AppDescriptor appDesc = (AppDescriptor) e.nextElement();
                appDesc.bind();
            }

            lastModified = System.currentTimeMillis();
        } catch (Exception mx) {
            getLogger().error(Messages.getString("ApplicationManager.1"), mx); //$NON-NLS-1$
            mx.printStackTrace();
        }
    }

    /**
     *  Stop all running applications.
     */
    public void stopAll() {
        for (Enumeration en = descriptors.elements(); en.hasMoreElements();) {
            try {
                AppDescriptor appDesc = (AppDescriptor) en.nextElement();

                appDesc.stop();
            } catch (Exception x) {
                // ignore exception in application shutdown
            }
        }
    }

    /**
     *  Get an array containing all currently running applications.
     */
    public Object[] getApplications() {
        return applications.values().toArray();
    }

    /**
     *  Get an application by name.
     */
    public Application getApplication(String name) {
        return (Application) applications.get(name);
    }

    /**
     * Implements org.apache.xmlrpc.XmlRpcHandler.execute()
     */
    public Object execute(String method, Vector params)
                   throws Exception {
        int dot = method.indexOf("."); //$NON-NLS-1$

        if (dot == -1) {
            throw new Exception(Messages.getString("ApplicationManager.2") + method + //$NON-NLS-1$
                                Messages.getString("ApplicationManager.3")); //$NON-NLS-1$
        }

        if ((dot == 0) || (dot == (method.length() - 1))) {
            throw new Exception(Messages.getString("ApplicationManager.4") + method + Messages.getString("ApplicationManager.5")); //$NON-NLS-1$ //$NON-NLS-2$
        }

        String handler = method.substring(0, dot);
        String method2 = method.substring(dot + 1);
        Application app = (Application) xmlrpcHandlers.get(handler);

        if (app == null) {
            app = (Application) xmlrpcHandlers.get("*"); //$NON-NLS-1$
            // use the original method name, the handler is resolved within the app.
            method2 = method;
        }

        if (app == null) {
            throw new Exception(Messages.getString("ApplicationManager.6") + handler + Messages.getString("ApplicationManager.7") + method); //$NON-NLS-1$ //$NON-NLS-2$
        }

        return app.executeXmlRpc(method2, params);
    }

    private String getMountpoint(String mountpoint) {
        mountpoint = mountpoint.trim();

        if ("".equals(mountpoint)) { //$NON-NLS-1$
            return "/"; //$NON-NLS-1$
        } else if (!mountpoint.startsWith("/")) { //$NON-NLS-1$
            return "/" + mountpoint; //$NON-NLS-1$
        }

        return mountpoint;
    }

    private String joinMountpoint(String prefix, String suffix) {
        if (prefix.endsWith("/") || suffix.startsWith("/")) {  //$NON-NLS-1$//$NON-NLS-2$
            return prefix+suffix;
        } else {
            return prefix+"/"+suffix; //$NON-NLS-1$
        }
    }

    private String getPathPattern(String mountpoint) {
        if (!mountpoint.startsWith("/")) { //$NON-NLS-1$
            mountpoint = "/"+mountpoint; //$NON-NLS-1$
        }

        if ("/".equals(mountpoint)) { //$NON-NLS-1$
            return "/"; //$NON-NLS-1$
        }

        if (mountpoint.endsWith("/")) { //$NON-NLS-1$
            return mountpoint.substring(0, mountpoint.length()-1);
        }

        return mountpoint;
    }

    private File getAbsoluteFile(String path) {
        // make sure our directory has an absolute path,
        // see http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4117557
        File file = new File(path);
        if (file.isAbsolute()) {
            return file;
        } else {
            return file.getAbsoluteFile();
        }
    }

    private Log getLogger() {
        return server.getLogger();
    }

    private String findResource(String path) {
        File file = new File(path);
        if (!file.isAbsolute() && !file.exists()) {
            file = new File(server.getHopHome(), path);
        }
        return file.getAbsolutePath();
    }

    /**
     *  Inner class that describes an application and its start settings.
     */
    class AppDescriptor {

        Application app;

        private ContextHandler staticContext = null;
        private ContextHandler appContext = null;

        String appName;
        File appDir;
        File dbDir;
        String mountpoint;
        String pathPattern;
        String staticDir;
        String protectedStaticDir;
        String staticMountpoint;
        boolean staticIndex;
        String[] staticHome;
        String xmlrpcHandlerName;
        String cookieDomain;
        String sessionCookieName;
        String protectedSessionCookie;
        String uploadLimit;
        String uploadSoftfail;
        String debug;
        Repository[] repositories;
        String servletClassName;

        /**
         * extend apps.properties, add [appname].ignore
         */
        String ignoreDirs;

        /**
         * Creates an AppDescriptor from the properties.
         * @param name the application name
         */
        AppDescriptor(String name) {
            ResourceProperties conf = props.getSubProperties(name + '.');
            appName = name;
            mountpoint = getMountpoint(conf.getProperty("mountpoint", appName)); //$NON-NLS-1$
            pathPattern = getPathPattern(mountpoint);
            staticDir = conf.getProperty("static"); //$NON-NLS-1$
            staticMountpoint = getPathPattern(conf.getProperty("staticMountpoint", //$NON-NLS-1$
                                        joinMountpoint(mountpoint, "static"))); //$NON-NLS-1$
            staticIndex = "true".equalsIgnoreCase(conf.getProperty("staticIndex"));  //$NON-NLS-1$//$NON-NLS-2$
            String home = conf.getProperty("staticHome"); //$NON-NLS-1$
            if (home == null) {
                staticHome = new String[] {"index.html", "index.htm"}; //$NON-NLS-1$ //$NON-NLS-2$
            } else {
                staticHome = StringUtils.split(home, ","); //$NON-NLS-1$
            }
            protectedStaticDir = conf.getProperty("protectedStatic"); //$NON-NLS-1$

            cookieDomain = conf.getProperty("cookieDomain"); //$NON-NLS-1$
            sessionCookieName = conf.getProperty("sessionCookieName"); //$NON-NLS-1$
            protectedSessionCookie = conf.getProperty("protectedSessionCookie"); //$NON-NLS-1$
            uploadLimit = conf.getProperty("uploadLimit"); //$NON-NLS-1$
            uploadSoftfail = conf.getProperty("uploadSoftfail"); //$NON-NLS-1$
            debug = conf.getProperty("debug"); //$NON-NLS-1$
            String appDirName = conf.getProperty("appdir"); //$NON-NLS-1$
            appDir = (appDirName == null) ? null : getAbsoluteFile(appDirName);
            String dbDirName = conf.getProperty("dbdir"); //$NON-NLS-1$
            dbDir = (dbDirName == null) ? null : getAbsoluteFile(dbDirName);
            servletClassName = conf.getProperty("servletClass"); //$NON-NLS-1$

            // got ignore dirs
            ignoreDirs = conf.getProperty("ignore"); //$NON-NLS-1$

            // read and configure app repositories
            ArrayList repositoryList = new ArrayList();
            Class[] parameters = { String.class };
            for (int i = 0; true; i++) {
                String repositoryArgs = conf.getProperty("repository." + i); //$NON-NLS-1$

                if (repositoryArgs != null) {
                    // lookup repository implementation
                    String repositoryImpl = conf.getProperty("repository." + i + //$NON-NLS-1$
                                                              ".implementation"); //$NON-NLS-1$
                    if (repositoryImpl == null) {
                        // implementation not set manually, have to guess it
                        if (repositoryArgs.endsWith(".zip")) { //$NON-NLS-1$
                            repositoryArgs = findResource(repositoryArgs);
                            repositoryImpl = "helma.framework.repository.ZipRepository"; //$NON-NLS-1$
                        } else if (repositoryArgs.endsWith(".js")) { //$NON-NLS-1$
                            repositoryArgs = findResource(repositoryArgs);
                            repositoryImpl = "helma.framework.repository.SingleFileRepository"; //$NON-NLS-1$
                        } else {
                            repositoryArgs = findResource(repositoryArgs);
                            repositoryImpl = "helma.framework.repository.FileRepository"; //$NON-NLS-1$
                        }
                    }

                    try {
                        Repository newRepository = (Repository) Class.forName(repositoryImpl)
                                .getConstructor(parameters)
                                .newInstance(new Object[] {repositoryArgs});
                        repositoryList.add(newRepository);
                    } catch (Exception ex) {
                        getLogger().error(Messages.getString("ApplicationManager.8") + repositoryArgs + Messages.getString("ApplicationManager.9") + //$NON-NLS-1$ //$NON-NLS-2$
                                           Messages.getString("ApplicationManager.10"), ex); //$NON-NLS-1$
                    }
                } else {
                    // we always scan repositories 0-9, beyond that only if defined
                    if (i > 9) {
                        break;
                    }
                }
            }

            if (appDir != null) {
                FileRepository appRep = new FileRepository(appDir);
                if (!repositoryList.contains(appRep)) {
                    repositoryList.add(appRep);
                }
            } else if (repositoryList.isEmpty()) {
                repositoryList.add(new FileRepository(
                        new File(server.getAppsHome(), appName)));
            }
            repositories = new Repository[repositoryList.size()];
            repositories = (Repository[]) repositoryList.toArray(repositories);
        }


        void start() {
            getLogger().info(Messages.getString("ApplicationManager.11") + appName); //$NON-NLS-1$

            try {
                // create the application instance
                app = new Application(appName, server, repositories, appDir, dbDir);

                // register ourselves
                descriptors.put(appName, this);
                applications.put(appName, app);

                // the application is started later in the register method, when it's bound
                app.init(ignoreDirs);

                // set application URL prefix if it isn't set in app.properties
                if (!app.hasExplicitBaseURI()) {
                    app.setBaseURI(mountpoint);
                }

                app.start();
            } catch (Exception x) {
                getLogger().error(Messages.getString("ApplicationManager.12") + appName, x); //$NON-NLS-1$
                x.printStackTrace();
            }
        }

        void stop() {
            getLogger().info(Messages.getString("ApplicationManager.13") + appName); //$NON-NLS-1$

            // unbind application
            unbind();

            // stop application
            try {
                app.stop();
                getLogger().info(Messages.getString("ApplicationManager.14") + appName); //$NON-NLS-1$
            } catch (Exception x) {
                getLogger().error(Messages.getString("ApplicationManager.15"), x); //$NON-NLS-1$
            }

            descriptors.remove(appName);
            applications.remove(appName);
        }

        void bind() {
            try {
                getLogger().info(Messages.getString("ApplicationManager.16") + appName + " :: " + app.hashCode() + " :: " + this.hashCode());   //$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$

                // set application URL prefix if it isn't set in app.properties
                if (!app.hasExplicitBaseURI()) {
                    app.setBaseURI(mountpoint);
                }

                // bind to Jetty HTTP server
                if (jetty != null) {
                    if(context == null) {
                        context = new ContextHandlerCollection();
                        jetty.getHttpServer().setHandler(context);
                    }

                    // if there is a static direcory specified, mount it
                    if (staticDir != null) {

                        File staticContent = getAbsoluteFile(staticDir);

                        getLogger().info(Messages.getString("ApplicationManager.17") + staticContent.getPath()); //$NON-NLS-1$
                        getLogger().info(Messages.getString("ApplicationManager.18") + staticMountpoint); //$NON-NLS-1$
                        
                        ResourceHandler rhandler = new ResourceHandler();
                        rhandler.setResourceBase(staticContent.getPath());
                        rhandler.setWelcomeFiles(staticHome);
                        
                        staticContext = context.addContext(staticMountpoint, ""); //$NON-NLS-1$
                        staticContext.setHandler(rhandler);
                        
                        staticContext.start();
                    }
                    
                    appContext = context.addContext(pathPattern, ""); //$NON-NLS-1$

                    ServletHandler handler = new ServletHandler();
                    Class servletClass = servletClassName == null ?
                            EmbeddedServletClient.class : Class.forName(servletClassName);

                    ServletHolder holder = new ServletHolder(servletClass);
                    handler.addServletWithMapping(holder, "/*"); //$NON-NLS-1$

                    holder.setInitParameter("application", appName); //$NON-NLS-1$
                    // holder.setInitParameter("mountpoint", mountpoint);

                    if (cookieDomain != null) {
                        holder.setInitParameter("cookieDomain", cookieDomain); //$NON-NLS-1$
                    }

                    if (sessionCookieName != null) {
                        holder.setInitParameter("sessionCookieName", sessionCookieName); //$NON-NLS-1$
                    }

                    if (protectedSessionCookie != null) {
                        holder.setInitParameter("protectedSessionCookie", protectedSessionCookie); //$NON-NLS-1$
                    }

                    if (uploadLimit != null) {
                        holder.setInitParameter("uploadLimit", uploadLimit); //$NON-NLS-1$
                    }

                    if (uploadSoftfail != null) {
                        holder.setInitParameter("uploadSoftfail", uploadSoftfail); //$NON-NLS-1$
                    }

                    if (debug != null) {
                        holder.setInitParameter("debug", debug); //$NON-NLS-1$
                    }
                    
                    appContext.setHandler(handler);

                    if (protectedStaticDir != null) {
                        File protectedContent = getAbsoluteFile(protectedStaticDir);
                        appContext.setResourceBase(protectedContent.getPath());
                        getLogger().info(Messages.getString("ApplicationManager.19") + //$NON-NLS-1$
                                       protectedContent.getPath());
                    }

                    // Remap the context paths and start
                    context.mapContexts();
                    appContext.start();
                }

                // register as XML-RPC handler
                xmlrpcHandlerName = app.getXmlRpcHandlerName();
                xmlrpcHandlers.put(xmlrpcHandlerName, app);
            } catch (Exception x) {
                getLogger().error(Messages.getString("ApplicationManager.20"), x); //$NON-NLS-1$
                x.printStackTrace();
            }
        }

        void unbind() {
            getLogger().info(Messages.getString("ApplicationManager.21") + appName); //$NON-NLS-1$

            try {
                // unbind from Jetty HTTP server
                if (jetty != null) {
                    if (appContext != null) {
                        context.removeHandler(appContext);
                        appContext.stop();
                        appContext.destroy();
                        appContext = null;
                    }

                    if (staticContext != null) {
                        context.removeHandler(staticContext);
                        staticContext.stop();
                        staticContext.destroy();
                        staticContext = null;
                    }
                    context.mapContexts();                    
                }

                // unregister as XML-RPC handler
                if (xmlrpcHandlerName != null) {
                    xmlrpcHandlers.remove(xmlrpcHandlerName);
                }
            } catch (Exception x) {
                getLogger().error(Messages.getString("ApplicationManager.22"), x); //$NON-NLS-1$
            }

        }

        public String toString() {
            return "[AppDescriptor "+app+"]"; //$NON-NLS-1$ //$NON-NLS-2$
        }
    }
}
