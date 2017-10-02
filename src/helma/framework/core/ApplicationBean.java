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

import helma.objectmodel.NodeInterface;
import helma.objectmodel.db.DbSource;
import helma.util.CronJob;
import helma.util.SystemMap;
import helma.util.WrappedMap;
import helma.framework.repository.*;
import helma.framework.FutureResultInterface;
import helma.main.Server;

import java.io.File;
import java.io.Serializable;
import java.io.IOException;
import java.util.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Application bean that provides a handle to the scripting environment to
 * application specific functionality.
 */
public class ApplicationBean implements Serializable {
    private static final long serialVersionUID = -5053315391709405106L;

    transient Application app;
    WrappedMap properties = null;

    /**
     * Creates a new ApplicationBean object.
     *
     * @param app ...
     */
    public ApplicationBean(Application app) {
        this.app = app;
    }

    /**
     * Clear the application cache.
     */
    public void clearCache() {
        this.app.clearCache();
    }

    /**
     * Get the app's event logger. This is a Log with the
     * category helma.[appname].event.
     *
     * @return the app logger.
     */
    public Log getLogger() {
        return  this.app.getEventLog();
    }

    /**
     * Get the app logger. This is a commons-logging Log with the
     * category <code>logname</code>.
     *
     * @return a logger for the given log name.
     */
    public Log getLogger(String logname) {
        return  LogFactory.getLog(logname);
    }

    /**
     * Log a INFO message to the app log.
     *
     * @param msg the log message
     */
    public void log(Object msg) {
        getLogger().info(msg);
    }

    /**
     * Log a INFO message to the log defined by logname.
     *
     * @param logname the name (category) of the log
     * @param msg the log message
     */
    public void log(String logname, Object msg) {
        getLogger(logname).info(msg);
    }

    /**
     * Log a DEBUG message to the app log if debug is set to true in
     * app.properties.
     *
     * @param msg the log message
     */
    public void debug(Object msg) {
        if (this.app.debug()) {
            getLogger().debug(msg);
        }
    }

    /**
     * Log a DEBUG message to the log defined by logname
     * if debug is set to true in app.properties.
     *
     * @param logname the name (category) of the log
     * @param msg the log message
     */
    public void debug(String logname, Object msg) {
        if (this.app.debug()) {
            getLogger(logname).debug(msg);
        }
    }

    /**
     * Returns the app's repository list.
     *
     * @return the an array containing this app's repositories
     */
    public Object[] getRepositories() {
        return this.app.getRepositories().toArray();
    }

    /**
     * Add a repository to the app's repository list. The .zip extension
     * is automatically added, if the original library path does not
     * point to an existing file or directory.
     *
     * @param obj the repository, relative or absolute path to the library.
     */
    public synchronized void addRepository(Object obj) {
        ResourceInterface current = this.app.getCurrentCodeResource();
        RepositoryInterface parent = current == null ?
                null : current.getRepository().getRootRepository();
        RepositoryInterface rep;
        if (obj instanceof String) {
            String path = (String) obj;
            File file = findResource(null, path);
            if (file == null) {
                file = findResource(this.app.hopHome, path);
                if (file == null) {
                    throw new RuntimeException(Messages.getString("ApplicationBean.0") + path); //$NON-NLS-1$
                }
            }
            if (file.isDirectory()) {
                rep = new FileRepository(file);
            } else if (file.isFile()) {
                if (file.getName().endsWith(".zip")) { //$NON-NLS-1$
                    rep = new ZipRepository(file);
                } else {
                    rep = new SingleFileRepository(file);
                }
            } else {
                throw new RuntimeException(Messages.getString("ApplicationBean.1") + file); //$NON-NLS-1$
            }
        } else if (obj instanceof RepositoryInterface) {
            rep = (RepositoryInterface) obj;
        } else {
            throw new RuntimeException(Messages.getString("ApplicationBean.2") + obj); //$NON-NLS-1$
        }
        this.app.addRepository(rep, parent);
        try {
            this.app.typemgr.checkRepository(rep, true);
        } catch (IOException iox) {
            getLogger().error(Messages.getString("ApplicationBean.3") + rep, iox); //$NON-NLS-1$
        }
    }

    /**
     * Helper method to resolve a repository path. Returns null if no file is found.
     * @param parent the parent file
     * @param path the repository path
     * @return an existing file, or null
     */
    private File findResource(File parent, String path) {
        File file = new File(parent, path).getAbsoluteFile();
        if (!file.exists()) {
            // if file does not exist, try with .zip and .js extensions appended
            file = new File(parent, path + ".zip").getAbsoluteFile(); //$NON-NLS-1$
            if (!file.exists()) {
                file = new File(parent, path + ".js").getAbsoluteFile(); //$NON-NLS-1$
            }
        }
        return file.exists() ? file : null;
    }

    /**
     * Get the app's classloader
     * @return the app's classloader
     */
    public ClassLoader getClassLoader() {
        return this.app.getClassLoader();
    }

    /**
     * Return the number of currently active sessions
     * @return the current number of active sessions
     */
    public int countSessions() {
        return this.app.countSessions();
    }

    /**
     * Get a session object for the specified session id
     * @param sessionID the session id
     * @return the session belonging to the session id, or null
     */
    public SessionBean getSession(String sessionID) {
        if (sessionID == null) {
            return null;
        }

        Session session = this.app.getSession(sessionID.trim());

        if (session == null) {
            return null;
        }

        return new SessionBean(session);
    }

    /**
     * Create a new session with the given session id
     * @param sessionID the session id
     * @return the newly created session
     */
    public SessionBean createSession(String sessionID) {
        if (sessionID == null) {
            return null;
        }

        Session session = this.app.createSession(sessionID.trim());

        if (session == null) {
            return null;
        }

        return new SessionBean(session);
    }

    /**
     * Get an array of all active sessions
     * @return an array of session beans
     */
    public SessionBean[] getSessions() {
        Map sessions = this.app.getSessions();
        SessionBean[] array = new SessionBean[sessions.size()];
        int i = 0;

        Iterator it = sessions.values().iterator();
        while (it.hasNext()) {
            array[i++] = new SessionBean((Session) it.next());
        }

        return array;
    }

    /**
     * Register a user with the given name and password using the
     * database mapping of the User prototype
     * @param username the user name
     * @param password the user password
     * @return the newly registered user, or null if we failed
     */
    public NodeInterface registerUser(String username, String password) {
        if ((username == null) || (password == null) || "".equals(username.trim()) || //$NON-NLS-1$
                "".equals(password.trim())) { //$NON-NLS-1$
            return null;
        }
        return this.app.registerUser(username, password);
    }

    /**
     * Get a user object with the given name
     * @param username the user name
     * @return the user object, or null
     */
    public NodeInterface getUser(String username) {
        if ((username == null) || "".equals(username.trim())) { //$NON-NLS-1$
            return null;
        }

        return this.app.getUserNode(username);
    }

    /**
     * Get an array of currently active registered users
     * @return an array of user nodes
     */
    public NodeInterface[] getActiveUsers() {
        List activeUsers = this.app.getActiveUsers();

        return (NodeInterface[]) activeUsers.toArray(new NodeInterface[0]);
    }

    /**
     * Get an array of all registered users
     * @return an array containing all registered users
     */
    public NodeInterface[] getRegisteredUsers() {
        List registeredUsers = this.app.getRegisteredUsers();

        return (NodeInterface[]) registeredUsers.toArray(new NodeInterface[0]);
    }

    /**
     * Get an array of all currently active sessions for a given user node
     * @param usernode the user node
     * @return an array of sessions for the given user
     */
    public SessionBean[] getSessionsForUser(NodeInterface usernode) {
        if (usernode == null) {
            return new SessionBean[0];
        }
        return getSessionsForUser(usernode.getName());
    }

    /**
     * Get an array of all currently active sessions for a given user name
     * @param username the user node
     * @return an array of sessions for the given user
     */
    public SessionBean[] getSessionsForUser(String username) {
        if ((username == null) || "".equals(username.trim())) { //$NON-NLS-1$
            return new SessionBean[0];
        }

        List userSessions = this.app.getSessionsForUsername(username);

        return (SessionBean[]) userSessions.toArray(new SessionBean[0]);
    }

    /**
     * Add a cron job that will run once a minute
     * @param functionName the function name
     */
    public void addCronJob(String functionName) {
        CronJob job = new CronJob(functionName);

        job.setFunction(functionName);
        this.app.customCronJobs.put(functionName, job);
    }

    /**
     * Add a cron job that will run at the specified time intervals
     *
     * @param functionName the function name
     * @param year comma separated list of years, or *
     * @param month comma separated list of months, or *
     * @param day comma separated list of days, or *
     * @param weekday comma separated list of weekdays, or *
     * @param hour comma separated list of hours, or *
     * @param minute comma separated list of minutes, or *
     */
    public void addCronJob(String functionName, String year, String month, String day,
                           String weekday, String hour, String minute) {
        CronJob job = CronJob.newJob(functionName, year, month, day, weekday, hour, minute);

        this.app.customCronJobs.put(functionName, job);
    }

    /**
     * Unregister a previously registered cron job
     * @param functionName the function name
     */
    public void removeCronJob(String functionName) {
        this.app.customCronJobs.remove(functionName);
    }

    /**
     * Returns an read-only map of the custom cron jobs registered with the app
     *
     * @return a map of cron jobs
     */
    public Map getCronJobs() {
        return new WrappedMap(this.app.customCronJobs, true);
    }

    /**
     * Returns the number of elements in the NodeManager's cache
     */
    public int getCacheusage() {
        return this.app.getCacheUsage();
    }

    /**
     * Returns the app's data node used to share data between the app's evaluators
     *
     * @return the app.data node
     */
    public NodeInterface getData() {
        return this.app.getCacheNode();
    }

    /**
     * Returns the app's modules map used to register application modules
     *
     * @return the module map
     */
    public Map getModules() {
        return this.app.modules;
    }

    /**
     * Returns the absolute path of the app dir. When using repositories this
     * equals the first file based repository.
     *
     * @return the app dir
     */
    public String getDir() {
        return this.app.getAppDir().getAbsolutePath();
    }

    /**
     * @return the app name
     */
    public String getName() {
        return this.app.getName();
    }

    /**
     * @return the application start time
     */
    public Date getUpSince() {
        return new Date(this.app.starttime);
    }

    /**
     * @return the number of requests processed by this app
     */
    public long getRequestCount() {
        return this.app.getRequestCount();
    }

    /**
     * @return the number of XML-RPC requests processed
     */
    public long getXmlrpcCount() {
        return this.app.getXmlrpcCount();
    }

    /**
     * @return the number of errors encountered
     */
    public long getErrorCount() {
        return this.app.getErrorCount();
    }

    /**
     * @return the wrapped helma.framework.core.Application object
     */
    public Application get__app__() {
        return this.app;
    }

    /**
     * Get a wrapper around the app's properties
     *
     * @return a readonly wrapper around the application's app properties
     */
    public Map getProperties() {
        if (this.properties == null) {
            this.properties = new WrappedMap(this.app.getProperties(), true);
        }
        return this.properties;
    }

    /**
     * Get a wrapper around the app's db properties
     *
     * @return a readonly wrapper around the application's db properties
     */
    public Map getDbProperties() {
        return new WrappedMap(this.app.getDbProperties(), true);
    }

    /**
     * Return a DbSource object for a given name.
     */
    public DbSource getDbSource(String name) {
        return this.app.getDbSource(name);
    }

    /**
     * Get a wrapper around the app's apps.properties
     *
     * @return a readonly wrapper around the application's apps.properties
     */
    public Map getAppsProperties() {
        Server server = Server.getServer();
        if (server == null)
            return new SystemMap();
        return new WrappedMap(server.getAppsProperties(this.app.getName()), true);
    }

    /**
     * Get an array of this app's prototypes
     *
     * @return an array containing the app's prototypes
     */
    public Prototype[] getPrototypes() {
        return (Prototype[]) this.app.getPrototypes().toArray(new Prototype[0]);
    }

    /**
     * Get a prototype by name.
     *
     * @param name the prototype name
     * @return the prototype
     */
    public Prototype getPrototype(String name) {
        return this.app.getPrototypeByName(name);
    }

    /**
     * Get the number of currently available threads/request evaluators
     * @return the currently available threads
     */
    public int getFreeThreads() {
        return this.app.countFreeEvaluators();
    }

    /**
     * Get the number of currently active request threads
     * @return the number of currently active threads
     */
    public int getActiveThreads() {
        return this.app.countActiveEvaluators();
    }

    /**
     * Get the maximal thread number for this application
     * @return the maximal number of threads/request evaluators
     */
    public int getMaxThreads() {
        return this.app.countEvaluators();
    }

    /**
     * Set the maximal thread number for this application
     * @param n the maximal number of threads/request evaluators
     */
    public void setMaxThreads(int n) {
        // add one to the number to compensate for the internal scheduler.
        this.app.setNumberOfEvaluators(n + 1);
    }

    /**
     *  Return a skin for a given object. The skin is found by determining the prototype
     *  to use for the object, then looking up the skin for the prototype.
     */
    public Skin getSkin(String protoname, String skinname, Object[] skinpath) {
        try {
            return this.app.getSkin(protoname, skinname, skinpath);
        } catch (Exception x) {
            return null;
        }
    }

    /**
     * Return a map of skin resources
     *
     * @return a map containing the skin resources
     */
    public Map getSkinfiles() {
        Map skinz = new SystemMap();

        for (Iterator it = this.app.getPrototypes().iterator(); it.hasNext();) {
            Prototype p = (Prototype) it.next();

            Object skinmap = p.getScriptableSkinMap();
            skinz.put(p.getName(), skinmap);
            skinz.put(p.getLowerCaseName(), skinmap);
        }

        return skinz;
    }

    /**
     * Return a map of skin resources including the app-specific skinpath
     *
     * @param skinpath an array of directory paths or HopObjects to search for skins
     * @return a map containing the skin resources
     */
    public Map getSkinfilesInPath(Object[] skinpath) {
        Map skinz = new SystemMap();

        for (Iterator it = this.app.getPrototypes().iterator(); it.hasNext();) {
            Prototype p = (Prototype) it.next();

            Object skinmap = p.getScriptableSkinMap(skinpath);
            skinz.put(p.getName(), skinmap);
            skinz.put(p.getLowerCaseName(), skinmap);
        }

        return skinz;
    }

    /**
     * Return the absolute application directory (appdir property
     * in apps.properties file)
     * @return the app directory as absolute path
     */
    public String getAppDir() {
        return this.app.getAppDir().getAbsolutePath();
    }

    /**
     * Return the absolute server directory
     * @return the server directory as absolute path
     */
    public String getServerDir() {
        File f = this.app.getServerDir();

        if (f == null) {
            return this.app.getAppDir().getAbsolutePath();
        }

        return f.getAbsolutePath();
    }

    /**
     * Return the app's default charset/encoding.
     * @return the app's charset
     */
    public String getCharset() {
        return this.app.getCharset();
    }

    /**
     * Set the path for global macro resolution
     * @param path an array of global namespaces, or null
     */
    public void setGlobalMacroPath(String[] path) {
        this.app.globalMacroPath = path;
    }

    /**
     * Get the path for global macro resolution
     * @return an array of global namespaces, or null
     */
    public String[] getGlobalMacroPath() {
        return this.app.globalMacroPath;
    }

    /**
     * Trigger a synchronous Helma invocation with a default timeout of 30 seconds.
     *
     * @param thisObject the object to invoke the function on,
     * or null for global invokation
     * @param function the function or function name to invoke
     * @param args an array of arguments
     * @return the value returned by the function
     * @throws Exception exception thrown by the function
     */
    public Object invoke(Object thisObject, Object function, Object[] args)
            throws Exception {
        // default timeout of 30 seconds
        return invoke(thisObject, function, args, 30000L);
    }

    /**
     * Trigger a synchronous Helma invocation.
     *
     * @param thisObject the object to invoke the function on,
     * or null for global invokation
     * @param function the function or function name to invoke
     * @param args an array of arguments
     * @param timeout the timeout in milliseconds. After waiting
     * this long, we will try to interrupt the invocation
     * @return the value returned by the function
     * @throws Exception exception thrown by the function
     */
    public Object invoke(Object thisObject, Object function,
                         Object[] args, long timeout)
            throws Exception {
        RequestEvaluator reval = this.app.getEvaluator();
        try {
            return reval.invokeInternal(thisObject, function, args, timeout);
        } finally {
            this.app.releaseEvaluator(reval);
        }
    }

    /**
     * Trigger an asynchronous Helma invocation. This method returns
     * immedately with an object that allows to track the result of the
     * function invocation with the following properties:
     *
     * <ul>
     * <li>running - true while the function is running, false afterwards</li>
     * <li>result - the value returned by the function, if any</li>
     * <li>exception - the exception thrown by the function, if any</li>
     * <li>waitForResult() - wait indefinitely until invocation terminates
     * and return the result</li>
     * <li>waitForResult(t) - wait for the specified number of milliseconds
     * for invocation to terminate and return the result</li>
     * </ul>
     *
     * @param thisObject the object to invoke the function on,
     * or null for global invokation
     * @param function the function or function name to invoke
     * @param args an array of arguments
     * this long, we will try to interrupt the invocation
     * @return an object with the properties described above
     */
    public FutureResultInterface invokeAsync(Object thisObject,
                              final Object function,
                              final Object[] args) {
        // default timeout of 15 minutes
        return new AsyncInvoker(thisObject, function, args, 60000L * 15);
    }

    /**
     * Trigger an asynchronous Helma invocation. This method returns
     * immedately with an object that allows to track the result of the
     * function invocation with the following methods and properties:
     *
     * <ul>
     * <li>running - true while the function is running, false afterwards</li>
     * <li>result - the value returned by the function, if any</li>
     * <li>exception - the exception thrown by the function, if any</li>
     * <li>waitForResult() - wait indefinitely until invocation terminates
     * and return the result</li>
     * <li>waitForResult(t) - wait for the specified number of milliseconds
     * for invocation to terminate and return the result</li>
     * </ul>
     *
     * @param thisObject the object to invoke the function on,
     * or null for global invokation
     * @param function the function or function name to invoke
     * @param args an array of arguments
     * @param timeout the timeout in milliseconds. After waiting
     * this long, we will try to interrupt the invocation
     * @return an object with the properties described above
     */
    public FutureResultInterface invokeAsync(Object thisObject, Object function,
                              Object[] args, long timeout) {
        return new AsyncInvoker(thisObject, function, args, timeout);
    }

    /**
     * Return a string presentation of this AppBean
     * @return string description of this app bean object
     */
    @Override
    public String toString() {
        return "[Application " + this.app.getName() + "]"; //$NON-NLS-1$ //$NON-NLS-2$
    }

    class AsyncInvoker extends Thread implements FutureResultInterface {

        private Object thisObject;
        private Object function;
        private Object[] args;
        private long timeout;

        private Object result;
        private Exception exception;
        private boolean running = true;

        private AsyncInvoker(Object thisObj, Object func, Object[] args, long timeout) {
            this.thisObject = thisObj;
            this.function = func;
            this.args = args;
            this.timeout = timeout;
            start();
        }

        @Override
        public void run() {
            RequestEvaluator reval = null;
            try {
                reval = ApplicationBean.this.app.getEvaluator();
                setResult(reval.invokeInternal(this.thisObject, this.function, this.args, this.timeout));
            } catch (Exception x) {
                setException(x);
            } finally {
                this.running = false;
                ApplicationBean.this.app.releaseEvaluator(reval);
            }
        }

        public synchronized boolean getRunning() {
            return this.running;
        }

        private synchronized void setResult(Object obj) {
            this.result = obj;
            this.running = false;
            notifyAll();
        }

        public synchronized Object getResult() {
            return this.result;
        }

        public synchronized Object waitForResult() throws InterruptedException {
            if (!this.running)
                return this.result;
            wait();
            return this.result;
        }

        public synchronized Object waitForResult(long timeout)
                throws InterruptedException {
            if (!this.running)
                return this.result;
            wait(timeout);
            return this.result;
        }

        private synchronized void setException(Exception x) {
            this.exception = x;
            this.running = false;
            notifyAll();
        }

        public synchronized Exception getException() {
            return this.exception;
        }

        @Override
        public String toString() {
            return new StringBuffer("AsyncInvokeThread{running: ").append(this.running) //$NON-NLS-1$
                    .append(", result: ").append(this.result).append(", exception: ")  //$NON-NLS-1$//$NON-NLS-2$
                    .append(this.exception).append("}").toString(); //$NON-NLS-1$
        }

    }
}
