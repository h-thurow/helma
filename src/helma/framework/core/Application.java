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
 *   Copyright 2010 dowee Limited. All rights reserved.  
 *
 * $RCSfile$
 * $Author$
 * $Revision$
 * $Date$
 */

package helma.framework.core;

import helma.extensions.ConfigurationException;
import helma.extensions.InterfaceHelmaExtension;
import helma.framework.*;
import helma.framework.repository.*;
import helma.main.Server;
import helma.objectmodel.*;
import helma.objectmodel.db.*;
import helma.util.*;
import helma.scripting.ScriptingEngine;
import helma.scripting.ScriptingException;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import java.util.ArrayList;


/**
 * The central class of a Helma application. This class keeps a pool of 
 * request evaluators (threads with JavaScript interpreters), waits for
 * requests from the Web server or XML-RPC port and dispatches them to
 * the evaluators.
 */
public final class Application implements Runnable {
    // the name of this application
    private String name;

    // application sources
    ArrayList repositories;

    // properties and db-properties
    ResourceProperties props;

    // properties and db-properties
    ResourceProperties dbProps;

    // This application's main directory
    File appDir;

    // Helma server hopHome directory
    File hopHome;

    // embedded db directory
    File dbDir;

    // this application's node manager
    protected NodeManager nmgr;

    // the root of the website, if a custom root object is defined.
    // otherwise this is managed by the NodeManager and not cached here.
    Object rootObject = null;
    String rootObjectClass;
    // if defined this will cause us to get the root object straight
    // from the scripting engine, circumventing all hopobject db fluff
    String rootObjectPropertyName;
    String rootObjectFunctionName;

    // The session manager
    SessionManager sessionMgr;

     /**
     *  The type manager checks if anything in the application's prototype definitions
     * has been updated prior to each evaluation.
     */
    public TypeManager typemgr;

    /**
     * The skin manager for this application
     */
    protected SkinManager skinmgr;

    /**
     * Collections for evaluator thread pooling
     */
    protected Stack freeThreads;
    protected Vector allThreads;
    boolean running = false;
    boolean debug;
    long starttime;
    Hashtable dbSources;

    // map of app modules reflected at app.modules
    Map modules;

    // internal worker thread for scheduler, session cleanup etc.
    Thread worker;
    // request timeout defaults to 60 seconds
    long requestTimeout = 60000;
    ThreadGroup threadgroup;

    // threadlocal variable for the current RequestEvaluator
    ThreadLocal currentEvaluator = new ThreadLocal();

    // Map of requesttrans -> active requestevaluators
    Hashtable activeRequests;

    String logDir;

    // Two logs for each application
    Log eventLog;
    Log accessLog;

    // Symbolic names for each log
    String eventLogName;
    String accessLogName;

    // A transient node that is shared among all evaluators
    protected INode cachenode;

    // some fields for statistics
    protected volatile long requestCount = 0;
    protected volatile long xmlrpcCount = 0;
    protected volatile long errorCount = 0;

    // the URL-prefix to use for links into this application
    private String baseURI;
    // the name of the root prototype as far as href() is concerned
    private String hrefRootPrototype;

    // the id of the object to use as root object
    String rootId = "0"; //$NON-NLS-1$

    // Db mappings for some standard prototypes
    private DbMapping rootMapping;
    private DbMapping userRootMapping;
    private DbMapping userMapping;

    // name of response encoding
    String charset;

    // password file to use for authenticate() function
    private CryptResource pwfile;

    // Map of java class names to object prototypes
    ResourceProperties classMapping;

    // Map of extensions allowed for public skins
    Properties skinExtensions;

    // time we last read the properties file
    private long lastPropertyRead = -1L;

    // the set of prototype/function pairs which are allowed to be called via XML-RPC
    private HashSet xmlrpcAccess;

    // the name under which this app serves XML-RPC requests. Defaults to the app name
    private String xmlrpcHandlerName;

    // the list of currently active cron jobs
    Hashtable activeCronJobs = null;
    // the list of custom cron jobs
    Hashtable customCronJobs = null;

    private ResourceComparator resourceComparator;
    private Resource currentCodeResource;

    // Field to cache unmapped java classes
    private final static String CLASS_NOT_MAPPED = Messages.getString("Application.0"); //$NON-NLS-1$

    /**
     * Namespace search path for global macros
     */
    String[] globalMacroPath = null;

    /**
     *  Simple constructor for dead application instances.
     */
    public Application(String name) {
        this.name = name;
    }

    /**
     * Build an application with the given name with the given sources. No
     * Server-wide properties are created or used.
     */
    public Application(String name, Repository[] repositories, File dbDir)
                throws IllegalArgumentException {
        this(name, null, repositories, null, dbDir);
    }

    /**
     * Build an application with the given name and server instance. The
     * app directories will be created if they don't exist already.
     */
    public Application(String name, Server server)
                throws IllegalArgumentException {
        this(name, server, new Repository[0], null, null);
    }

    /**
     * Build an application with the given name, server instance, sources and
     * db directory.
     */
    public Application(String name, Server server, Repository[] repositories,
                       File customAppDir, File customDbDir)
                throws IllegalArgumentException {
        if ((name == null) || (name.trim().length() == 0)) {
            throw new IllegalArgumentException(Messages.getString("Application.1") + name); //$NON-NLS-1$
        }

        if (repositories.length == 0) {
            throw new java.lang.IllegalArgumentException(Messages.getString("Application.2") + name); //$NON-NLS-1$
        }

        this.name = name;

        this.repositories = new ArrayList();
        this.repositories.addAll(Arrays.asList(repositories));
        this.resourceComparator = new ResourceComparator(this);

        this.appDir = customAppDir;
        this.dbDir = customDbDir;

        // system-wide properties, default to null
        ResourceProperties sysProps;

        // system-wide properties, default to null
        ResourceProperties sysDbProps;

        sysProps = sysDbProps = null;
        this.hopHome = null;

        if (server != null) {
            this.hopHome = server.getHopHome();

            if (this.dbDir == null) {
                this.dbDir = new File(server.getDbHome(), name);
            }

            // get system-wide properties
            sysProps = server.getProperties();
            sysDbProps = server.getDbProperties();
        }

        if (!this.dbDir.exists()) {
            this.dbDir.mkdirs();
        }

        if (this.appDir == null) {
            for (int i=repositories.length-1; i>=0; i--) {
                if (repositories[i] instanceof FileRepository) {
                    this.appDir = new File(repositories[i].getName());
                    break;
                }
            }
        }

        // give the Helma Thread group a name so the threads can be recognized
        this.threadgroup = new ThreadGroup("TX-" + name); //$NON-NLS-1$

        // create app-level properties
        this.props = new ResourceProperties(this, "app.properties", sysProps); //$NON-NLS-1$

        // get log names
        this.accessLogName = this.props.getProperty("accessLog", //$NON-NLS-1$
                new StringBuffer("helma.").append(name).append(".access").toString());  //$NON-NLS-1$//$NON-NLS-2$
        this.eventLogName = this.props.getProperty("eventLog", //$NON-NLS-1$
                new StringBuffer("helma.").append(name).append(".event").toString());  //$NON-NLS-1$//$NON-NLS-2$

        // create app-level db sources
        this.dbProps = new ResourceProperties(this, "db.properties", sysDbProps, false); //$NON-NLS-1$

        // the passwd file, to be used with the authenticate() function
        CryptResource parentpwfile = null;

        if (this.hopHome != null) {
            parentpwfile = new CryptResource(new FileResource(new File(this.hopHome, "passwd")), null); //$NON-NLS-1$
        }

        this.pwfile = new CryptResource(repositories[0].getResource("passwd"), parentpwfile); //$NON-NLS-1$

        // the properties that map java class names to prototype names
        this.classMapping = new ResourceProperties(this, "class.properties"); //$NON-NLS-1$
        this.classMapping.setIgnoreCase(false);

        // get class name of root object if defined. Otherwise native Helma objectmodel will be used.
        this.rootObjectClass = this.classMapping.getProperty("root"); //$NON-NLS-1$

        updateProperties();

        this.dbSources = new Hashtable();
        this.modules = new SystemMap();
    }

    /**
     * Get the application ready to run, initializing the evaluators and type manager.
     */
    public void init()
            throws DatabaseException, IllegalAccessException, InstantiationException,
                   ClassNotFoundException, InterruptedException {
        init(null);
    }

    /**
     * Get the application ready to run, initializing the evaluators and type manager.
     *
     * @param ignoreDirs comma separated list of directory names to ignore
     */
    public void init(final String ignoreDirs)
            throws DatabaseException, IllegalAccessException, InstantiationException,
                   ClassNotFoundException, InterruptedException {

        Initializer i = new Initializer(ignoreDirs);
        i.start();
        i.join();
        if (i.exception != null) {
            if (i.exception instanceof DatabaseException)
                throw (DatabaseException) i.exception;
            if (i.exception instanceof IllegalAccessException)
                throw (IllegalAccessException) i.exception;
            if (i.exception instanceof InstantiationException)
                throw (InstantiationException) i.exception;
            if (i.exception instanceof ClassNotFoundException)
                throw (ClassNotFoundException) i.exception;
            throw new RuntimeException(i.exception);
        }
    }

    // We need to call initialize in a fresh thread because the calling thread could
    // already be associated with a rhino context, for example when starting from the
    // manage application.
    class Initializer extends Thread {
        Exception exception = null;
        String ignoreDirs;

        Initializer(String dirs) {
            super(Application.this.name + "-init"); //$NON-NLS-1$
            this.ignoreDirs = dirs;
        }

        @Override
        public void run() {
            try {
                synchronized (Application.this) {
                    initInternal();
                }
            } catch (Exception x) {
                this.exception = x;
            }
        }

        private void initInternal()
                throws DatabaseException, IllegalAccessException,
                       InstantiationException, ClassNotFoundException {
            Application.this.running = true;
            // create and init type mananger
            Application.this.typemgr = new TypeManager(Application.this, this.ignoreDirs);
            // set the context classloader. Note that this must be done before
            // using the logging framework so that a new LogFactory gets created
            // for this app.
            Thread.currentThread().setContextClassLoader(Application.this.typemgr.getClassLoader());
            try {
                Application.this.typemgr.createPrototypes();
            } catch (Exception x) {
                logError(Messages.getString("Application.3"), x); //$NON-NLS-1$
            }

            if (Server.getServer() != null) {
                Vector extensions = Server.getServer().getExtensions();

                for (int i = 0; i < extensions.size(); i++) {
                    InterfaceHelmaExtension ext = (InterfaceHelmaExtension) extensions.get(i);

                    try {
                        ext.applicationStarted(Application.this);
                    } catch (ConfigurationException e) {
                        logEvent(Messages.getString("Application.4") + ext.getName() + ": " +  //$NON-NLS-1$//$NON-NLS-2$
                                 e.toString());
                    }
                }
            }

            // create and init evaluator/thread lists
            Application.this.freeThreads = new Stack();
            Application.this.allThreads = new Vector();

            Application.this.activeRequests = new Hashtable();
            Application.this.activeCronJobs = new Hashtable();
            Application.this.customCronJobs = new Hashtable();

            // create the skin manager
            Application.this.skinmgr = new SkinManager(Application.this);

            // read in root id, root prototype, user prototype
            Application.this.rootId = Application.this.props.getProperty("rootid", "0"); //$NON-NLS-1$ //$NON-NLS-2$
            String rootPrototype = Application.this.props.getProperty("rootprototype", "root"); //$NON-NLS-1$ //$NON-NLS-2$
            String userPrototype = Application.this.props.getProperty("userprototype", "user"); //$NON-NLS-1$ //$NON-NLS-2$

            Application.this.rootMapping = getDbMapping(rootPrototype);
            if (Application.this.rootMapping == null)
                throw new RuntimeException(Messages.getString("Application.5") + rootPrototype); //$NON-NLS-1$
            Application.this.userMapping = getDbMapping(userPrototype);
            if (Application.this.userMapping == null)
                throw new RuntimeException(Messages.getString("Application.6") + userPrototype); //$NON-NLS-1$

            // The whole user/userroot handling is basically old
            // ugly obsolete crap. Don't bother.
            ResourceProperties p = new ResourceProperties();
            String usernameField = (Application.this.userMapping != null) ? Application.this.userMapping.getNameField() : null;

            if (usernameField == null) {
                usernameField = "name"; //$NON-NLS-1$
            }

            p.put("_children", "collection(" + userPrototype + ")"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            p.put("_children.accessname", usernameField); //$NON-NLS-1$
            Application.this.userRootMapping = new DbMapping(Application.this, "__userroot__", p); //$NON-NLS-1$
            Application.this.userRootMapping.update();

            // create the node manager
            Application.this.nmgr = new NodeManager(Application.this);
            Application.this.nmgr.init(Application.this.dbDir.getAbsoluteFile(), Application.this.props);

            // create the app cache node exposed as app.data
            Application.this.cachenode = new TransientNode("app"); //$NON-NLS-1$

            // create and init session manager
            String sessionMgrImpl = Application.this.props.getProperty("sessionManagerImpl", //$NON-NLS-1$
                                                      "helma.framework.core.SessionManager"); //$NON-NLS-1$
            Application.this.sessionMgr = (SessionManager) Class.forName(sessionMgrImpl).newInstance();
            logEvent(Messages.getString("Application.7") + sessionMgrImpl); //$NON-NLS-1$
            Application.this.sessionMgr.init(Application.this);

            // read the sessions if wanted
            if ("true".equalsIgnoreCase(getProperty("persistentSessions"))) {  //$NON-NLS-1$//$NON-NLS-2$
                RequestEvaluator ev = getEvaluator();
                try {
                    ev.initScriptingEngine();
                    Application.this.sessionMgr.loadSessionData(null, ev.scriptingEngine);
                } finally {
                    releaseEvaluator(ev);
                }
            }
            
            // preallocate minThreads request evaluators
            int minThreads = 0;

            try {
                minThreads = Integer.parseInt(Application.this.props.getProperty("minThreads")); //$NON-NLS-1$
            } catch (Exception ignore) {
                // not parsable as number, keep 0
            }

            if (minThreads > 0) {
                logEvent(Messages.getString("Application.8")+minThreads+Messages.getString("Application.9") + Application.this.name); //$NON-NLS-1$ //$NON-NLS-2$
            }

            for (int i = 0; i < minThreads; i++) {
                RequestEvaluator ev = new RequestEvaluator(Application.this);

                if (i == 0) {
                    ev.initScriptingEngine();
                }
                Application.this.freeThreads.push(ev);
                Application.this.allThreads.addElement(ev);
            }
        }
    }


    /**
     *  Create and start scheduler and cleanup thread
     */
    public synchronized void start() {
        this.starttime = System.currentTimeMillis();

        // as first thing, invoke global onStart() function
        RequestEvaluator eval = null;
        try {
            eval = getEvaluator();
            eval.invokeInternal(null, "onStart", RequestEvaluator.EMPTY_ARGS); //$NON-NLS-1$
        } catch (Exception xcept) {
            logError(Messages.getString("Application.10") + this.name + ".onStart()", xcept);  //$NON-NLS-1$//$NON-NLS-2$
        } finally {
            releaseEvaluator(eval);
        }

        this.worker = new Thread(this, this.name + "-worker"); //$NON-NLS-1$
        this.worker.setPriority(Thread.NORM_PRIORITY + 1);
        this.worker.start();
    }

    /**
     * This is called to shut down a running application.
     */
    public synchronized void stop() {
        // invoke global onStop() function
        RequestEvaluator eval = null;
        try {
            eval = getEvaluator();
            eval.invokeInternal(null, "onStop", RequestEvaluator.EMPTY_ARGS); //$NON-NLS-1$
        } catch (Exception x) {
            logError(Messages.getString("Application.11") + this.name + ".onStop()", x);  //$NON-NLS-1$//$NON-NLS-2$
        }

        // mark app as stopped
        this.running = false;

        // stop all threads, this app is going down
        if (this.worker != null) {
            this.worker.interrupt();
        }

        this.worker = null;

        // stop evaluators
        if (this.allThreads != null) {
            for (Enumeration e = this.allThreads.elements(); e.hasMoreElements();) {
                RequestEvaluator ev = (RequestEvaluator) e.nextElement();
                ev.stopTransactor();
                ev.shutdown();
            }
        }

        // remove evaluators
        this.allThreads.removeAllElements();
        this.freeThreads.clear();

        // shut down node manager and embedded db
        try {
            this.nmgr.shutdown();
        } catch (DatabaseException dbx) {
            System.err.println(Messages.getString("Application.12") + dbx); //$NON-NLS-1$
        }

        // tell the extensions that we're stopped.
        if (Server.getServer() != null) {
            Vector extensions = Server.getServer().getExtensions();

            for (int i = 0; i < extensions.size(); i++) {
                InterfaceHelmaExtension ext = (InterfaceHelmaExtension) extensions.get(i);

                ext.applicationStopped(this);
            }
        }

        // store the sessions if wanted
        if ("true".equalsIgnoreCase(getProperty("persistentSessions"))) { //$NON-NLS-1$ //$NON-NLS-2$
            // sessionMgr.storeSessionData(null);
            this.sessionMgr.storeSessionData(null, eval.scriptingEngine);
        }
        this.sessionMgr.shutdown();
    }

    /**
     * Returns true if this app is currently running
     *
     * @return true if the app is running
     */
    public boolean isRunning() {
        return this.running;
    }

    /**
     * Get the application directory.
     *
     * @return the application directory, or first file based repository
     */
    public File getAppDir() {
        return this.appDir;
    }

    /**
     * Get a comparator for comparing Resources according to the order of
     * repositories they're contained in.
     *
     * @return a comparator that sorts resources according to their repositories
     */
    public ResourceComparator getResourceComparator() {
        return this.resourceComparator;
    }

    /**
     * Returns a free evaluator to handle a request.
     */
    public RequestEvaluator getEvaluator() {
        if (!this.running) {
            throw new ApplicationStoppedException();
        }

        // first try
        try {
            return (RequestEvaluator) this.freeThreads.pop();
        } catch (EmptyStackException nothreads) {
            int maxThreads = 50;

            String maxThreadsProp = this.props.getProperty("maxThreads"); //$NON-NLS-1$
            if (maxThreadsProp != null) {
                try {
                    maxThreads = Integer.parseInt(maxThreadsProp);
                } catch (Exception ignore) {
                    logEvent(Messages.getString("Application.13") + maxThreadsProp); //$NON-NLS-1$
                }
            }

            synchronized (this) {
                // allocate a new evaluator
                if (this.allThreads.size() < maxThreads) {
                    logEvent(Messages.getString("Application.14") + (this.allThreads.size() + 1) + //$NON-NLS-1$
                             Messages.getString("Application.15") + this.name); //$NON-NLS-1$

                    RequestEvaluator ev = new RequestEvaluator(this);

                    this.allThreads.addElement(ev);

                    return (ev);
                }
            }
        }

        // we can't create a new evaluator, so we wait if one becomes available.
        // give it 3 more tries, waiting 3 seconds each time.
        for (int i = 0; i < 4; i++) {
            try {
                Thread.sleep(3000);

                return (RequestEvaluator) this.freeThreads.pop();
            } catch (EmptyStackException nothreads) {
                // do nothing
            } catch (InterruptedException inter) {
                throw new RuntimeException(Messages.getString("Application.16")); //$NON-NLS-1$
            }
        }

        // no luck, give up.
        throw new RuntimeException(Messages.getString("Application.17")); //$NON-NLS-1$
    }

    /**
     * Returns an evaluator back to the pool when the work is done.
     */
    public void releaseEvaluator(RequestEvaluator ev) {
        if (ev != null) {
            ev.recycle();
            this.freeThreads.push(ev);
        }
    }

    /**
     * This can be used to set the maximum number of evaluators which will be allocated.
     * If evaluators are required beyound this number, an error will be thrown.
     */
    public boolean setNumberOfEvaluators(int n) {
        if ((n < 2) || (n > 511)) {
            return false;
        }

        int current = this.allThreads.size();

        synchronized (this.allThreads) {
            if (n > current) {
                int toBeCreated = n - current;

                for (int i = 0; i < toBeCreated; i++) {
                    RequestEvaluator ev = new RequestEvaluator(this);

                    this.freeThreads.push(ev);
                    this.allThreads.addElement(ev);
                }
            } else if (n < current) {
                int toBeDestroyed = current - n;

                for (int i = 0; i < toBeDestroyed; i++) {
                    try {
                        RequestEvaluator re = (RequestEvaluator) this.freeThreads.pop();
                        this.allThreads.removeElement(re);
                        re.stopTransactor();
                    } catch (EmptyStackException empty) {
                        return false;
                    }
                }
            }
        }

        return true;
    }

    /**
     *  Return the number of currently active threads
     */
    public int getActiveThreads() {
        return 0;
    }

    /**
     *  Execute a request coming in from a web client.
     */
    public ResponseTrans execute(RequestTrans req) {
        this.requestCount += 1;

        // get user for this request's session
        Session session = createSession(req.getSession());
        session.touch();

        ResponseTrans res = null;
        RequestEvaluator ev = null;

        // are we responsible for releasing the evaluator and closing the result?
        boolean primaryRequest = false;

        try {
            // first look if a request with same user/path/data is already being executed.
            // if so, attach the request to its output instead of starting a new evaluation
            // this helps to cleanly solve "doubleclick" kind of users
            ev = (RequestEvaluator) this.activeRequests.get(req);

            if (ev != null) {
                res = ev.attachHttpRequest(req);
                if (res != null) {
                    // we can only use the existing response object if the response
                    // wasn't written to the HttpServletResponse directly.
                    res.waitForClose();
                    if (res.getContent() == null) {
                        res = null;
                    }
                }
            }

            if (res == null) {
                primaryRequest = true;

                // if attachHttpRequest returns null this means we came too late
                // and the other request was finished in the meantime
                // check if the properties file has been updated
                updateProperties();

                // get evaluator and invoke
                ev = getEvaluator();
                res = ev.invokeHttp(req, session);
            }
        } catch (ApplicationStoppedException stopped) {
            // let the servlet know that this application has gone to heaven
            throw stopped;
        } catch (Exception x) {
            this.errorCount += 1;
            res = new ResponseTrans(this, req);
            res.reportError(x);
        } finally {
            if (primaryRequest) {
                this.activeRequests.remove(req);
                releaseEvaluator(ev);

                // response needs to be closed/encoded before sending it back
                try {
                    res.close(this.charset);
                } catch (UnsupportedEncodingException uee) {
                    logError(Messages.getString("Application.18"), uee); //$NON-NLS-1$
                }
            }
        }

        return res;
    }

    /**
     *  Called to execute a method via XML-RPC, usally by helma.main.ApplicationManager
     *  which acts as default handler/request dispatcher.
     */
    public Object executeXmlRpc(String method, Vector args)
                         throws Exception {
        this.xmlrpcCount += 1;

        Object retval = null;
        RequestEvaluator ev = null;

        try {
            // check if the properties file has been updated
            updateProperties();

            // get evaluator and invoke
            ev = getEvaluator();
            retval = ev.invokeXmlRpc(method, args.toArray());
        } finally {
            releaseEvaluator(ev);
        }

        return retval;
    }


    public Object executeExternal(String method, Vector args)
                        throws Exception {
        Object retval = null;
        RequestEvaluator ev = null;
        try {
            // check if the properties file has been updated
            updateProperties();
            // get evaluator and invoke
            ev = getEvaluator();
            retval = ev.invokeExternal(method, args.toArray());
        } finally {
            releaseEvaluator(ev);
        }
        return retval;
    }

    /**
     * Reset the application's object cache, causing all objects to be refetched from
     * the database.
     */
    public void clearCache() {
        this.nmgr.clearCache();
    }

    /**
     * Returns the number of elements in the NodeManager's cache
     */
    public int getCacheUsage() {
        return this.nmgr.countCacheEntries();
    }

    /**
     *  Set the application's root element to an arbitrary object. After this is called
     *  with a non-null object, the helma node manager will be bypassed. This function
     * can be used to script and publish any Java object structure with Helma.
     */
    public void setDataRoot(Object root) {
        this.rootObject = root;
    }

    /**
     * This method returns the root object of this application's object tree.
     */
    public Object getDataRoot() throws Exception {
        return getDataRoot(null);
    }

    /**
     * This method returns the root object of this application's object tree.
     */
    protected Object getDataRoot(ScriptingEngine engine) throws Exception {
        if (this.rootObject != null) {
            return this.rootObject;
        }
        // check if we have a custom root object class
        if (this.rootObjectClass != null) {
            // create custom root element.
            try {
                if (this.classMapping.containsKey("root.factory.class") && //$NON-NLS-1$
                        this.classMapping.containsKey("root.factory.method")) { //$NON-NLS-1$
                    String rootFactory = this.classMapping.getProperty("root.factory.class"); //$NON-NLS-1$
                    Class c = this.typemgr.getClassLoader().loadClass(rootFactory);
                    Method m = c.getMethod(
                            this.classMapping.getProperty("root.factory.method"), //$NON-NLS-1$
                            (Class[]) null);
                    this.rootObject = m.invoke(c, (Object[]) null);
                } else {
                    String rootClass = this.classMapping.getProperty("root"); //$NON-NLS-1$
                    Class c = this.typemgr.getClassLoader().loadClass(rootClass);
                    this.rootObject = c.newInstance();
                }
            } catch (Exception e) {
                throw new RuntimeException(Messages.getString("Application.19") + //$NON-NLS-1$
                        e.toString());
            }
            return this.rootObject;
        } else if (this.rootObjectPropertyName != null || this.rootObjectFunctionName != null) {
            // get root object from a global scripting engine property or function
            if (engine == null) {
                RequestEvaluator reval = getEvaluator();
                try {
                    return getDataRootFromEngine(reval.getScriptingEngine());
                } finally {
                    releaseEvaluator(reval);
                }
            }
            return getDataRootFromEngine(engine);
        } else {
            // no custom root object is defined - use standard helma objectmodel
            return this.nmgr.getRootNode();
        }
    }

    private Object getDataRootFromEngine(ScriptingEngine engine)
            throws ScriptingException {
        return this.rootObjectPropertyName != null ?
                engine.getGlobalProperty(this.rootObjectPropertyName) :
                engine.invoke(null, this.rootObjectFunctionName,
                        RequestEvaluator.EMPTY_ARGS,
                        ScriptingEngine.ARGS_WRAP_DEFAULT, true);
    }

    /**
     *  Return the prototype of the object to be used as this application's root object
     */
    public DbMapping getRootMapping() {
        return this.rootMapping;
    }

    /**
     *  Return the id of the object to be used as this application's root object
     */
    public String getRootId() {
        return this.rootId;
    }

    /**
     * Returns the Object which contains registered users of this application.
     */
    public INode getUserRoot() {
        INode users = this.nmgr.safe.getNode("1", this.userRootMapping); //$NON-NLS-1$

        users.setDbMapping(this.userRootMapping);

        return users;
    }

    /**
     * Returns the node manager for this application. The node manager is
     * the gateway to the helma.objectmodel packages, which perform the mapping
     * of objects to relational database tables or the embedded database.
     */
    public NodeManager getNodeManager() {
        return this.nmgr;
    }

    /**
     * Returns a wrapper containing the node manager for this application. The node manager is
     * the gateway to the helma.objectmodel packages, which perform the mapping of objects to
     * relational database tables or the embedded database.
     */
    public WrappedNodeManager getWrappedNodeManager() {
        return this.nmgr.safe;
    }

    /**
     * Return the application's session manager
     * @return the SessionManager instance used by this app
     */
    public SessionManager getSessionManager() {
        return this.sessionMgr;
    }

    /**
     *  Return a transient node that is shared by all evaluators of this application ("app node")
     */
    public INode getCacheNode() {
        return this.cachenode;
    }

    /**
     * Returns a Node representing a registered user of this application by his or her user name.
     */
    public INode getUserNode(String uid) {
        try {
            INode users = getUserRoot();

            return (INode) users.getChildElement(uid);
        } catch (Exception x) {
            return null;
        }
    }

    /**
     * Return a prototype for a given node. If the node doesn't specify a prototype,
     * return the generic hopobject prototype.
     */
    public Prototype getPrototype(Object obj) {
        String protoname = getPrototypeName(obj);

        if (protoname == null) {
            return this.typemgr.getPrototype("hopobject"); //$NON-NLS-1$
        }

        Prototype p = this.typemgr.getPrototype(protoname);

        if (p == null) {
            p = this.typemgr.getPrototype("hopobject"); //$NON-NLS-1$
        }

        return p;
    }

    /**
     * Return the prototype with the given name, if it exists
     */
    public Prototype getPrototypeByName(String name) {
        return this.typemgr.getPrototype(name);
    }

    /**
     * Return a collection containing all prototypes defined for this application
     */
    public Collection getPrototypes() {
        return this.typemgr.getPrototypes();
    }

    /**
     * Programmatically define a new prototype. If a prototype with this name already exists return
     * the existing prototype.
     * @param name the prototype name
     * @param typeProps custom type properties map
     * @return the new prototype
     */
    public Prototype definePrototype(String name, Map typeProps) {
        Prototype proto = this.typemgr.getPrototype(name);
        if (proto == null) {
            proto = this.typemgr.createPrototype(name, null, typeProps);
        } else {
            proto.setTypeProperties(typeProps);
        }
        return proto;
    }

    /**
     *  Return a skin for a given object. The skin is found by determining the prototype
     *  to use for the object, then looking up the skin for the prototype.
     */
    public Skin getSkin(String protoname, String skinname, Object[] skinpath) throws IOException {
        Prototype proto = getPrototypeByName(protoname);

        if (proto == null) {
            return null;
        }

        return this.skinmgr.getSkin(proto, skinname, skinpath);
    }

    /**
     * Return the session currently associated with a given Hop session ID.
     * Create a new session if necessary.
     */
    public Session createSession(String sessionId) {
        return this.sessionMgr.createSession(sessionId);
    }

    /**
     * Return a list of Helma nodes (HopObjects -  the database object representing the user,
     *  not the session object) representing currently logged in users.
     */
    public List getActiveUsers() {
        return this.sessionMgr.getActiveUsers();
    }

    /**
     * Return a list of Helma nodes (HopObjects -  the database object representing the user,
     *  not the session object) representing registered users of this application.
     */
    public List getRegisteredUsers() {
        ArrayList list = new ArrayList();
        INode users = getUserRoot();

        // add all child nodes to the list
        for (Enumeration e = users.getSubnodes(); e.hasMoreElements();) {
            list.add(e.nextElement());
        }

        // if none, try to get them from properties (internal db)
        if (list.size() == 0) {
            for (Enumeration e = users.properties(); e.hasMoreElements();) {
                list.add(users.getNode((String) e.nextElement()));
            }
        }

        return list;
    }

    /**
     * Return an array of <code>SessionBean</code> objects currently associated
     * with a given Helma user.
     */
    public List getSessionsForUsername(String username) {
        return this.sessionMgr.getSessionsForUsername(username);
    }

    /**
     * Return the session currently associated with a given Hop session ID.
     */
    public Session getSession(String sessionId) {
        return this.sessionMgr.getSession(sessionId);
    }

    /**
     *  Return the whole session map.
     */
    public Map getSessions() {
        return this.sessionMgr.getSessions();
    }

    /**
     * Returns the number of currenty active sessions.
     */
    public int countSessions() {
        return this.sessionMgr.countSessions();
    }

    /**
     * Register a user with the given user name and password.
     */
    public INode registerUser(String uname, String password) {
        if (uname == null) {
            return null;
        }

        uname = uname.toLowerCase().trim();

        if ("".equals(uname)) { //$NON-NLS-1$
            return null;
        }

        INode unode;

        try {
            INode users = getUserRoot();

            // check if a user with this name is already registered
            unode = (INode) users.getChildElement(uname);
            if (unode != null) {
                return null;
            }

            unode = new Node(uname, "user", this.nmgr.safe); //$NON-NLS-1$

            String usernameField = (this.userMapping != null) ? this.userMapping.getNameField() : null;
            String usernameProp = null;

            if (usernameField != null) {
                usernameProp = this.userMapping.columnNameToProperty(usernameField);
            }

            if (usernameProp == null) {
                usernameProp = "name"; //$NON-NLS-1$
            }

            unode.setName(uname);
            unode.setString(usernameProp, uname);
            unode.setString("password", password); //$NON-NLS-1$

            return users.addNode(unode);

        } catch (Exception x) {
            logEvent(Messages.getString("Application.20") + x); //$NON-NLS-1$

            return null;
        }
    }

    /**
     * Log in a user given his or her user name and password.
     */
    public boolean loginSession(String uname, String password, Session session) {
        // Check the name/password of a user and log it in to the current session
        if (uname == null) {
            return false;
        }

        uname = uname.toLowerCase().trim();

        if ("".equals(uname)) { //$NON-NLS-1$
            return false;
        }

        try {
            INode users = getUserRoot();
            Node unode = (Node) users.getChildElement(uname);
            if (unode == null)
                return false;

            String pw = unode.getString("password"); //$NON-NLS-1$

            if ((pw != null) && pw.equals(password)) {
                // let the old user-object forget about this session
                session.logout();
                session.login(unode);

                return true;
            }
        } catch (Exception x) {
            return false;
        }

        return false;
    }

    /**
     * Log out a session from this application.
     */
    public void logoutSession(Session session) {
        session.logout();
    }

    /**
     * In contrast to login, this works outside the Hop user object framework. Instead, the user is
     * authenticated against a passwd file in the application directory. This is to have some sort of
     * authentication available *before* the application is up and running, i.e. for application setup tasks.
     */
    public boolean authenticate(String uname, String password) {
        if ((uname == null) || (password == null)) {
            return false;
        }

        return this.pwfile.authenticate(uname, password);
    }

    /**
     *  Return the href to the root of this application.
     * @return the root element's URL
     * @throws UnsupportedEncodingException if the application's charset property
     *         is not a valid encoding name
     */
    public String getRootHref() throws UnsupportedEncodingException {
        return getNodeHref(null, null, null);
    }

    /**
     * Return a path to be used in a URL pointing to the given element  and action
     * @param elem the object to get the URL for
     * @param actionName an optional action name
     * @param queryParams optional map of query parameters
     * @return the element's URL
     * @throws UnsupportedEncodingException if the application's charset property
     *         is not a valid encoding name
     */
    public String getNodeHref(Object elem, String actionName, Map queryParams)
            throws UnsupportedEncodingException {
        StringBuffer buffer = new StringBuffer(this.baseURI);

        composeHref(elem, buffer, 0);

        if (actionName != null) {
            buffer.append(UrlEncoded.encode(actionName, this.charset));
        }
        if (queryParams != null) {
            appendQueryParams(buffer, queryParams, null, 0);
        }

        return buffer.toString();
    }

    private int appendQueryParams(StringBuffer buffer, Map params,
                                  String prefix, int count)
            throws UnsupportedEncodingException {
        for (Iterator it = params.entrySet().iterator(); it.hasNext();) {
            Map.Entry entry = (Map.Entry) it.next();
            Object value = entry.getValue();
            if (value == null) {
                continue;
            }
            String key = UrlEncoded.encode(entry.getKey().toString(), this.charset);
            if (prefix != null) key = prefix + '[' + key + ']';
            if (value instanceof Map) {
                count = appendQueryParams(buffer, (Map) value, key, count);
            } else {
                buffer.append(count++ == 0 ? '?' : '&');
                buffer.append(key);
                buffer.append('=');
                buffer.append(UrlEncoded.encode(value.toString(), this.charset));
            }
        }
        return count;
    }

    private void composeHref(Object elem, StringBuffer b, int pathCount)
            throws UnsupportedEncodingException {
        if ((elem == null) || (pathCount > 50)) {
            return;
        }

        if ((this.hrefRootPrototype != null) &&
             this.hrefRootPrototype.equals(getPrototypeName(elem))) {
            return;
        }

        Object parent = getParentElement(elem);

        if (parent == null) {
            return;
        }

        // recurse to parent element
        composeHref(getParentElement(elem), b, ++pathCount);

        // append ourselves
        String ename = getElementName(elem);
        if (ename != null) {
            b.append(UrlEncoded.encode(ename, this.charset));
            b.append("/"); //$NON-NLS-1$
        }
    }

    /**
     *  Returns the baseURI for Hrefs in this application.
     */
    public String getBaseURI() {
        return this.baseURI;
    }


    /**
     *  This method sets the base URL of this application which will be prepended to
     *  the actual object path.
     */
    public void setBaseURI(String uri) {
        if (uri == null) {
            this.baseURI = "/"; //$NON-NLS-1$
        } else if (!uri.endsWith("/")) { //$NON-NLS-1$
            this.baseURI = uri + "/"; //$NON-NLS-1$
        } else {
            this.baseURI = uri;
        }
    }

    /**
     *  Return true if the baseURI property is defined in the application
     *  properties, false otherwise.
     */
    public boolean hasExplicitBaseURI() {
        return this.props.containsKey("baseuri"); //$NON-NLS-1$
    }

    /**
     * Returns the prototype name that Hrefs in this application should
     * start with.
     */
    public String getHrefRootPrototype() {
        return this.hrefRootPrototype;
    }

    /**
     * Tell other classes whether they should output logging information for
     * this application.
     */
    public boolean debug() {
        return this.debug;
    }

    /**
     * Get the current RequestEvaluator, or null if the calling thread
     * is not evaluating a request.
     *
     * @return the RequestEvaluator belonging to the current thread
     */
    public RequestEvaluator getCurrentRequestEvaluator() {
        return (RequestEvaluator) this.currentEvaluator.get();
    }

    /**
     * Set the current RequestEvaluator for the calling thread.
     * @param eval the RequestEvaluator belonging to the current thread
     */
    protected void setCurrentRequestEvaluator(RequestEvaluator eval) {
        this.currentEvaluator.set(eval);
    }

    /**
     *  Utility function invoker for the methods below. This *must* be called
     *  by an active RequestEvaluator thread.
     */
    private Object invokeFunction(Object obj, String func, Object[] args) {
        RequestEvaluator reval = getCurrentRequestEvaluator();
        if (reval != null) {
            if (args == null) {
                args = RequestEvaluator.EMPTY_ARGS;
            }
            try {
                return reval.invokeDirectFunction(obj, func, args);
            } catch (Exception x) {
                if (this.debug) {
                    System.err.println(Messages.getString("Application.21") + //$NON-NLS-1$
                                        func + Messages.getString("Application.22") + x); //$NON-NLS-1$
                }
            }
        }
        return null;
    }

    /**
     *  Return the application's classloader
     */
    public ClassLoader getClassLoader() {
        return this.typemgr.getClassLoader();
    }

    //////////////////////////////////////////////////////////////////////////////////////////////////////////
    ///   The following methods mimic the IPathElement interface. This allows us
    ///   to script any Java object: If the object implements IPathElement (as does
    ///   the Node class in Helma's internal objectmodel) then the corresponding
    ///   method is called in the object itself. Otherwise, a corresponding script function
    ///   is called on the object.
    //////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Return the name to be used to get this element from its parent
     */
    public String getElementName(Object obj) {
        if (obj instanceof IPathElement) {
            return ((IPathElement) obj).getElementName();
        }

        Object retval = invokeFunction(obj, "getElementName", RequestEvaluator.EMPTY_ARGS); //$NON-NLS-1$

        if (retval != null) {
            return retval.toString();
        }

        return null;
    }

    /**
     * Retrieve a child element of this object by name.
     */
    public Object getChildElement(Object obj, String name) {
        if (obj instanceof IPathElement) {
            return ((IPathElement) obj).getChildElement(name);
        }

        Object[] arg = new Object[] { name };
        return invokeFunction(obj, "getChildElement", arg); //$NON-NLS-1$
    }

    /**
     * Return the parent element of this object.
     */
    public Object getParentElement(Object obj) {
        if (obj instanceof IPathElement) {
            return ((IPathElement) obj).getParentElement();
        }

        return invokeFunction(obj, "getParentElement", RequestEvaluator.EMPTY_ARGS); //$NON-NLS-1$
    }

    /**
     * Get the name of the prototype to be used for this object. This will
     * determine which scripts, actions and skins can be called on it
     * within the Helma scripting and rendering framework.
     */
    public String getPrototypeName(Object obj) {
        if (obj == null) {
            return "global"; //$NON-NLS-1$
        } else if (obj instanceof IPathElement) {
            // check if e implements the IPathElement interface
            return ((IPathElement) obj).getPrototype();
        }

        // How class name to prototype name lookup works:
        // If an object is not found by its direct class name, a cache entry is added
        // for the class name. For negative result, the string "(unmapped)" is used
        // as cache value.
        //
        // Caching is done directly in classProperties, as ResourceProperties have
        // the nice effect of being purged when the underlying resource is updated,
        // so cache invalidation happens implicitely.

        Class clazz = obj.getClass();
        String className = clazz.getName();
        String protoName = this.classMapping.getProperty(className);
        // fast path: direct hit, either positive or negative
        if (protoName != null) {
            return protoName == CLASS_NOT_MAPPED ? null : protoName;
        }

        // walk down superclass path. We already checked the actual class,
        // and we know that java.lang.Object does not implement any interfaces,
        // and the code is streamlined a bit to take advantage of this.
        while (clazz != Object.class) {
            // check interfaces
            Class[] classes = clazz.getInterfaces();
            for (int i = 0; i < classes.length; i++) {
                protoName = this.classMapping.getProperty(classes[i].getName());
                if (protoName != null) {
                    // cache the class name for the object so we run faster next time
                    this.classMapping.setProperty(className, protoName);
                    return protoName;
                }
            }
            clazz = clazz.getSuperclass();
            protoName = this.classMapping.getProperty(clazz.getName());
            if (protoName != null) {
                // cache the class name for the object so we run faster next time
                this.classMapping.setProperty(className, protoName);
                return protoName == CLASS_NOT_MAPPED ? null : protoName;
            }
        }
        // not mapped - cache negative result
        this.classMapping.setProperty(className, CLASS_NOT_MAPPED);
        return null;
    }

    ////////////////////////////////////////////////////////////////////////

    /**
     * Log an application error
     */
    public void logError(String msg, Throwable error) {
        if (this.eventLog == null) {
            this.eventLog = getLogger(this.eventLogName);
        }
        this.eventLog.error(msg, error);
    }

    /**
     * Log an application error
     */
    public void logError(String msg) {
        if (this.eventLog == null) {
            this.eventLog = getLogger(this.eventLogName);
        }
        this.eventLog.error(msg);
    }

    /**
     * Log a generic application event
     */
    public void logEvent(String msg) {
        getEventLog().info(msg);
    }

    /**
     * Log a generic application debug message
     */
    public void logDebug(String msg) {
        getEventLog().debug(msg);
    }

    /**
     * Log an application access
     */
    public void logAccess(String msg) {
        getAccessLog().info(msg);
    }

    /**
     * get the app's event log.
     */
    public Log getEventLog() {
        if (this.eventLog == null) {
            this.eventLog = getLogger(this.eventLogName);
            setEventLogLevel();
        }
        return this.eventLog;
    }

    /**
     * get the app's access log.
     */
    public Log getAccessLog() {
        if (this.accessLog == null) {
            this.accessLog = getLogger(this.accessLogName);
        }
        return this.accessLog;
    }

    /**
     *  Get a logger object to log events for this application.
     */
    public Log getLogger(String logname) {
        if ("console".equals(this.logDir) || "console".equals(logname)) {  //$NON-NLS-1$//$NON-NLS-2$
            return Logging.getConsoleLog();
        }
        return LogFactory.getLog(logname);
    }

    private void setEventLogLevel() {
        // set log level for event log in case it is a helma.util.Logger
        if (this.eventLog instanceof Logger) {
            if (this.debug) {
                if (!this.eventLog.isDebugEnabled()) {
                    ((Logger) this.eventLog).setLogLevel(Logger.DEBUG);
                }
            } else if (this.eventLog.isDebugEnabled()) {
                ((Logger) this.eventLog).setLogLevel(Logger.INFO);
            }
        }
    }

    /**
     * The run method performs periodic tasks like executing the scheduler method and
     * kicking out expired user sessions.
     */
    public void run() {

        // interval between session cleanups
        long lastSessionCleanup = System.currentTimeMillis();

        while (Thread.currentThread() == this.worker) {

            try {
                // interval between scheduler runs
                long sleepInterval = 60000;

                try {
                    String sleepProp = this.props.getProperty("schedulerInterval"); //$NON-NLS-1$
                    if (sleepProp != null) {
                        sleepInterval = Math.max(1000, Integer.parseInt(sleepProp) * 1000);
                    } else {
                        sleepInterval = CronJob.millisToNextFullMinute();
                    }
                } catch (Exception ignore) {
                    // we'll use the default interval
                }

                // sleep until the next full minute
                try {
                    Thread.sleep(sleepInterval);
                } catch (InterruptedException x) {
                    this.worker = null;
                    break;
                }

                // purge sessions
                try {
                    lastSessionCleanup = this.sessionMgr.cleanupSessions(lastSessionCleanup);
                } catch (Exception x) {
                    logError(Messages.getString("Application.23") + x, x); //$NON-NLS-1$
                } catch (LinkageError x) {
                    logError(Messages.getString("Application.24") + x, x); //$NON-NLS-1$
                }

                // execute cron jobs
                try {
                    executeCronJobs();
                } catch (Exception x) {
                    logError(Messages.getString("Application.25") + x, x); //$NON-NLS-1$
                } catch (LinkageError x) {
                    logError(Messages.getString("Application.26") + x, x); //$NON-NLS-1$
                }

            } catch (VirtualMachineError error) {
                logError(Messages.getString("Application.27") + error, error); //$NON-NLS-1$
            }
        }

        // when interrupted, shutdown running cron jobs
        synchronized (this.activeCronJobs) {
            for (Iterator i = this.activeCronJobs.values().iterator(); i.hasNext();) {
                ((CronRunner) i.next()).interrupt();
                i.remove();
            }
        }

        logEvent(Messages.getString("Application.28") + this.name + Messages.getString("Application.29")); //$NON-NLS-1$ //$NON-NLS-2$
    }

    /**
     * Executes cron jobs for the application, which are either
     * defined in app.properties or via app.addCronJob().
     * This method is called by run().
     */
    private void executeCronJobs() {
        // loop-local cron job data
        List jobs = CronJob.parse(this.props.getSubProperties("cron.")); //$NON-NLS-1$
        Date date = new Date();

        jobs.addAll(this.customCronJobs.values());
        CronJob.sort(jobs);

        if (this.debug) {
            logEvent(Messages.getString("Application.30") + jobs); //$NON-NLS-1$
        }
        if (!this.activeCronJobs.isEmpty()) {
            logEvent(Messages.getString("Application.31") + this.activeCronJobs); //$NON-NLS-1$
        }

        for (Iterator i = jobs.iterator(); i.hasNext();) {
            CronJob job = (CronJob) i.next();

            if (job.appliesToDate(date)) {
                // check if the job is already active ...
                if (this.activeCronJobs.containsKey(job.getName())) {
                    logEvent(job + Messages.getString("Application.32")); //$NON-NLS-1$

                    continue;
                }

                RequestEvaluator evaluator;

                try {
                    evaluator = getEvaluator();
                } catch (RuntimeException rt) {
                    if (this.running) {
                        logEvent(Messages.getString("Application.33") + job + //$NON-NLS-1$
                                Messages.getString("Application.34")); //$NON-NLS-1$

                        continue;
                    }
                    break;
                }

                // if the job has a long timeout or we're already late during this minute
                // the job is run from an extra thread
                if ((job.getTimeout() > 20000) ||
                        (CronJob.millisToNextFullMinute() < 30000)) {
                    CronRunner runner = new CronRunner(evaluator, job);

                    this.activeCronJobs.put(job.getName(), runner);
                    runner.start();
                } else {
                    try {
                        evaluator.invokeInternal(null, job.getFunction(),
                                RequestEvaluator.EMPTY_ARGS, job.getTimeout());
                    } catch (Exception ex) {
                        logEvent(Messages.getString("Application.35") + job + ": " + ex);  //$NON-NLS-1$//$NON-NLS-2$
                    } finally {
                        releaseEvaluator(evaluator);
                    }
                }
            }
        }
    }

    /**
     * Check whether a prototype is for scripting a java class, i.e. if there's an entry
     * for it in the class.properties file.
     */
    public boolean isJavaPrototype(String typename) {
        return this.classMapping.contains(typename);
    }

    /**
     * Return the java class that a given prototype wraps, or null.
     */
    public String getJavaClassForPrototype(String typename) {

        for (Iterator it = this.classMapping.entrySet().iterator(); it.hasNext();) {
            Map.Entry entry = (Map.Entry) it.next();

            if (typename.equals(entry.getValue())) {
                return (String) entry.getKey();
            }
        }

        return null;
    }


    /**
     * Return a DbSource object for a given name. A DbSource is a relational database defined
     * in a db.properties file.
     */
    public DbSource getDbSource(String name) {
        String dbSrcName = name.toLowerCase();
        DbSource dbs = (DbSource) this.dbSources.get(dbSrcName);

        if (dbs != null) {
            return dbs;
        }

        try {
            dbs = new DbSource(name, this.dbProps);
            this.dbSources.put(dbSrcName, dbs);
        } catch (Exception problem) {
            logEvent(Messages.getString("Application.36") + name +": "); //$NON-NLS-1$ //$NON-NLS-2$
            logEvent(problem.toString());
        }

        return dbs;
    }

    /**
     * Return the name of this application
     */
    public String getName() {
        return this.name;
    }

    /**
     * Add a repository to this app's repository list. This is used for
     * ZipRepositories contained in top-level file repositories, for instance.
     *
     * @param rep the repository to add
     * @param current the current/parent repository
     * @return if the repository was not yet contained
     */
    public boolean addRepository(Repository rep, Repository current) {
        if (rep != null && !this.repositories.contains(rep)) {
            // Add the new repository before its parent/current repository.
            // This establishes the order of compilation between FileRepositories
            // and embedded ZipRepositories, or repositories added
            // via app.addRepository()
            if (current != null) {
                int pos = this.repositories.indexOf(current);
                if (pos > -1) {
                    this.repositories.add(pos, rep);
                    return true;
                }
            }
            // no parent or parent not in app's repositories, add at end of list.
            this.repositories.add(rep);
            return true;
        }
        return false;
    }

    /**
     * Searches for the index of the given repository for this app.
     * The arguement must be a root argument, or -1 will be returned.
     *
     * @param   rep one of this app's root repositories.
     * @return  the index of the first occurrence of the argument in this
     *          list; returns <tt>-1</tt> if the object is not found.
     */
    public int getRepositoryIndex(Repository rep) {
        return this.repositories.indexOf(rep);
    }

    /**
     * Returns the repositories of this application
     * @return iterator through application repositories
     */
    public List getRepositories() {
        return Collections.unmodifiableList(this.repositories);
    }

    /**
     * Set the code resource currently being evaluated/compiled. This is used
     * to set the proper parent repository when a new repository is added
     * via app.addRepository().
     *
     * @param resource the resource being currently evaluated/compiled
     */
    public void setCurrentCodeResource(Resource resource) {
        this.currentCodeResource = resource;
    }

    /**
     * Set the code resource currently being evaluated/compiled. This is used
     * to set the proper parent repository when a new repository is added
     * via app.addRepository().

     * @return the resource being currently evaluated/compiled
     */
    public Resource getCurrentCodeResource() {
        return this.currentCodeResource;
    }

    /**
     * Return the directory of the Helma server
     */
    public File getServerDir() {
        return this.hopHome;
    }

    /**
     * Get the DbMapping associated with a prototype name in this application
     */
    public DbMapping getDbMapping(String typename) {
        Prototype proto = this.typemgr.getPrototype(typename);

        if (proto == null) {
            return null;
        }

        return proto.getDbMapping();
    }

    /**
     * Return the current upload status.
     * @param req the upload RequestTrans
     * @return the current upload status.
     */
    public UploadStatus getUploadStatus(RequestTrans req) {
        String uploadId = (String) req.get("upload_id"); //$NON-NLS-1$
        if (uploadId == null)
            return null;

        String sessionId = req.getSession();
        Session session = getSession(sessionId);
        if (session == null)
            return null;
        return session.createUpload(uploadId);
    }



    private synchronized void updateProperties() {
        // if so property file has been updated, re-read props.
        if (this.props.lastModified() > this.lastPropertyRead) {
            // force property update
            this.props.update();

            // character encoding to be used for responses
            this.charset = this.props.getProperty("charset", "UTF-8");  //$NON-NLS-1$//$NON-NLS-2$

            // debug flag
            this.debug = "true".equalsIgnoreCase(this.props.getProperty("debug"));  //$NON-NLS-1$//$NON-NLS-2$

            // if rhino debugger is enabled use higher (10 min) default request timeout
            String defaultReqTimeout =
                    "true".equalsIgnoreCase(this.props.getProperty("rhino.debug")) ? //$NON-NLS-1$ //$NON-NLS-2$
                        "600" : "60";  //$NON-NLS-1$//$NON-NLS-2$
            String reqTimeout = this.props.getProperty("requesttimeout", defaultReqTimeout); //$NON-NLS-1$
            try {
                this.requestTimeout = Long.parseLong(reqTimeout) * 1000L;
            } catch (Exception ignore) {
                // go with default value
                this.requestTimeout = 60000L;
            }

            // set base URI
            String base = this.props.getProperty("baseuri"); //$NON-NLS-1$

            if (base != null) {
                setBaseURI(base);
            } else if (this.baseURI == null) {
                this.baseURI = "/"; //$NON-NLS-1$
            }

            this.hrefRootPrototype = this.props.getProperty("hrefrootprototype"); //$NON-NLS-1$
            this.rootObjectPropertyName = this.props.getProperty("rootobjectpropertyname"); //$NON-NLS-1$
            this.rootObjectFunctionName = this.props.getProperty("rootobjectfunctionname"); //$NON-NLS-1$

            // update the XML-RPC access list, containting prototype.method
            // entries of functions that may be called via XML-RPC
            String xmlrpcAccessProp = this.props.getProperty("xmlrpcaccess"); //$NON-NLS-1$
            HashSet xra = new HashSet();

            if (xmlrpcAccessProp != null) {
                StringTokenizer st = new StringTokenizer(xmlrpcAccessProp, ",; "); //$NON-NLS-1$

                while (st.hasMoreTokens()) {
                    String token = st.nextToken().trim();

                    xra.add(token.toLowerCase());
                }
            }

            this.xmlrpcAccess = xra;

            // if node manager exists, update it
            if (this.nmgr != null) {
                this.nmgr.updateProperties(this.props);
            }

            // update extensions
            if (Server.getServer() != null) {
                Vector extensions = Server.getServer().getExtensions();

                for (int i = 0; i < extensions.size(); i++) {
                    InterfaceHelmaExtension ext = (InterfaceHelmaExtension) extensions.get(i);

                    try {
                        ext.applicationUpdated(this);
                    } catch (ConfigurationException e) {
                        logEvent(Messages.getString("Application.38")+ext+": "+e); //$NON-NLS-1$ //$NON-NLS-2$
                    }
                }
            }

            String loggerFactory = this.props.getProperty("loggerFactory", "helma.util.Logging");  //$NON-NLS-1$//$NON-NLS-2$
            if ("helma.util.Logging".equals(loggerFactory)) { //$NON-NLS-1$
                this.logDir = this.props.getProperty("logdir", "log"); //$NON-NLS-1$ //$NON-NLS-2$
                if (System.getProperty("helma.logdir") == null) { //$NON-NLS-1$
                    // set up helma.logdir system property in case we're using it
                    // FIXME: this sets a global System property, should be per-app
                    File dir = new File(this.logDir);
                    System.setProperty("helma.logdir", dir.getAbsolutePath()); //$NON-NLS-1$
                }
            } else {
                this.logDir = null;
            }

            // set log level for event log in case debug flag has changed
            setEventLogLevel();

            // set prop read timestamp
            this.lastPropertyRead = this.props.lastModified();
        }
    }

    /**
     *  Get a checksum that mirrors the state of this application in the sense
     *  that if anything in the applciation changes, the checksum hopefully will
     *  change, too.
     */
    public long getChecksum() {
        return this.starttime +
               this.typemgr.getLastCodeUpdate() +
               this.props.getChecksum();
    }

    /**
     * Proxy method to get a property from the applications properties.
     */
    public String getProperty(String propname) {
        return this.props.getProperty(propname);
    }

    /**
     * Proxy method to get a property from the applications properties.
     */
    public String getProperty(String propname, String defvalue) {
        return this.props.getProperty(propname, defvalue);
    }

    /**
     * Get the application's app properties
     *
     * @return the properties reflecting the app.properties
     */
    public ResourceProperties getProperties() {
        return this.props;
    }

    /**
     * Get the application's db properties
     *
     * @return the properties reflecting the db.properties
     */
    public ResourceProperties getDbProperties() {
        return this.dbProps;
    }


    /**
     * Return the XML-RPC handler name for this app. The contract is to
     * always return the same string, even if it has been changed in the properties file
     * during runtime, so the app gets unregistered correctly.
     */
    public String getXmlRpcHandlerName() {
        if (this.xmlrpcHandlerName == null) {
            this.xmlrpcHandlerName = this.props.getProperty("xmlrpcHandlerName", this.name); //$NON-NLS-1$
        }

        return this.xmlrpcHandlerName;
    }

    /**
     * Return a string representation for this app.
     */
    @Override
    public String toString() {
        return "[Application "+this.name+"]"; //$NON-NLS-1$ //$NON-NLS-2$
    }

    /**
     *
     */
    public int countThreads() {
        return this.threadgroup.activeCount();
    }

    /**
     *
     */
    public int countEvaluators() {
        return this.allThreads.size();
    }

    /**
     *
     */
    public int countFreeEvaluators() {
        return this.freeThreads.size();
    }

    /**
     *
     */
    public int countActiveEvaluators() {
        return this.allThreads.size() - this.freeThreads.size();
    }

    /**
     *
     */
    public int countMaxActiveEvaluators() {
        // return typemgr.countRegisteredRequestEvaluators () -1;
        // not available due to framework refactoring
        return -1;
    }

    /**
     *
     */
    public long getRequestCount() {
        return this.requestCount;
    }

    /**
     *
     */
    public long getXmlrpcCount() {
        return this.xmlrpcCount;
    }

    /**
     *
     */
    public long getErrorCount() {
        return this.errorCount;
    }

    /**
     *
     *
     * @return ...
     */
    public long getStarttime() {
        return this.starttime;
    }

    /**
     * Return the name of the character encoding used by this application
     *
     * @return the character encoding
     */
    public String getCharset() {
        return this.charset;
    }

    /**
     * Periodically called to log thread stats for this application
     */
    public void printThreadStats() {
        logEvent(Messages.getString("Application.40") + this.name + ": " + this.threadgroup.activeCount() +  //$NON-NLS-1$//$NON-NLS-2$
                 Messages.getString("Application.41")); //$NON-NLS-1$

        Runtime rt = Runtime.getRuntime();
        long free = rt.freeMemory();
        long total = rt.totalMemory();

        logEvent(Messages.getString("Application.42") + (free / 1024) + Messages.getString("Application.43")); //$NON-NLS-1$ //$NON-NLS-2$
        logEvent(Messages.getString("Application.44") + (total / 1024) + Messages.getString("Application.45")); //$NON-NLS-1$ //$NON-NLS-2$
    }

    /**
     * Check if a method may be invoked via XML-RPC on a prototype.
     */
    protected void checkXmlRpcAccess(String proto, String method)
                              throws Exception {
        String key = proto + "." + method; //$NON-NLS-1$

        // XML-RPC access items are case insensitive and stored in lower case
        if (!this.xmlrpcAccess.contains(key.toLowerCase())) {
            throw new Exception(Messages.getString("Application.46") + key + Messages.getString("Application.47")); //$NON-NLS-1$ //$NON-NLS-2$
        }
    }

    class CronRunner extends Thread {
        RequestEvaluator thisEvaluator;
        CronJob job;

        public CronRunner(RequestEvaluator thisEvaluator, CronJob job) {
            this.thisEvaluator = thisEvaluator;
            this.job = job;
        }

        @Override
        public void run() {
            try {
                this.thisEvaluator.invokeInternal(null, this.job.getFunction(),
                                             RequestEvaluator.EMPTY_ARGS, this.job.getTimeout());
            } catch (Exception ex) {
                logEvent(Messages.getString("Application.48") + this.job + ": " + ex);  //$NON-NLS-1$//$NON-NLS-2$
            } finally {
                releaseEvaluator(this.thisEvaluator);
                this.thisEvaluator = null;
                Application.this.activeCronJobs.remove(this.job.getName());
            }
        }

        @Override
        public String toString() {
            return "CronRunner[" + this.job + "]"; //$NON-NLS-1$ //$NON-NLS-2$
        }
    }
}
