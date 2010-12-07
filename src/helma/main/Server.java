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
 * $RCSfile: Server.java,v $
 * $Author$
 * $Revision$
 * $Date$
 */

package helma.main;

import helma.extensions.HelmaExtension;
import helma.framework.repository.FileResource;
import helma.framework.core.*;
import helma.objectmodel.db.DbSource;
import helma.util.*;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.xmlrpc.*;

import java.io.*;
import java.util.*;
import java.net.*;

import helma.util.ResourceProperties;

/**
 * Helma server main class.
 */
public class Server implements Runnable {
    // version string
    public static final String version = "1.7.0 (__builddate__)"; //$NON-NLS-1$

    // static server instance
    private static Server server;

    // Server home directory
    protected File hopHome;

    // server-wide properties
    ResourceProperties appsProps;
    ResourceProperties dbProps;
    ResourceProperties sysProps;

    // our logger
    private Log logger;
    // are we using helma.util.Logging?
    private boolean helmaLogging;

    // server start time
    public final long starttime;

    // if paranoid == true we only accept XML-RPC connections from
    // explicitly listed hosts.
    public boolean paranoid;
    private ApplicationManager appManager;
    private Vector extensions;
    private Thread mainThread;

    // configuration
    ServerConfig config;

    // map of server-wide database sources
    Hashtable dbSources;

    // the embedded web server
    // protected Serve websrv;
    protected JettyServer jetty;

    // the XML-RPC server
    protected WebServer xmlrpc;
    
    Thread shutdownhook;


    /**
     * Constructs a new Server instance with an array of command line options.
     * TODO make this a singleton
     * @param config the configuration
     */
    public Server(ServerConfig config) {
        server = this;
        this.starttime = System.currentTimeMillis();

        this.config = config;
        this.hopHome    = config.getHomeDir();
        if (this.hopHome == null) {
            throw new RuntimeException(Messages.getString("Server.0")); //$NON-NLS-1$
        }

        // create system properties
        this.sysProps = new ResourceProperties();
        if (config.hasPropFile()) {
            this.sysProps.addResource(new FileResource(config.getPropFile()));
        }
    }


    /**
     * Static main entry point.
     * @param args the command line arguments
     */
    public static void main(String[] args) throws IOException {
        loadServer(args);
        // parse properties files etc
        server.init();
        // start the server main thread
        server.start();
    }

    /**
     * Entry point used by launcher.jar to load a server instance
     * @param args the command line arguments
     * @return the server instance
     */
    public static Server loadServer(String[] args) {
        checkJavaVersion();

        ServerConfig config = null;
        try {
            config = getConfig(args);
        } catch (Exception cex) {
            printUsageError(Messages.getString("Server.1") + cex.getMessage()); //$NON-NLS-1$
            System.exit(1);
        }

        checkRunning(config);

        // create new server instance
        server = new Server(config);
        return server;
    }


    /**
      * check if we are running on a Java 2 VM - otherwise exit with an error message
      */
    public static void checkJavaVersion() {
        String javaVersion = System.getProperty("java.version"); //$NON-NLS-1$

        if ((javaVersion == null) || javaVersion.startsWith("1.3") //$NON-NLS-1$
                                  || javaVersion.startsWith("1.2") //$NON-NLS-1$
                                  || javaVersion.startsWith("1.1") //$NON-NLS-1$
                                  || javaVersion.startsWith("1.0")) { //$NON-NLS-1$
            System.err.println(Messages.getString("Server.2")); //$NON-NLS-1$

            if (javaVersion == null) { // don't think this will ever happen, but you never know
                System.err.println(Messages.getString("Server.3")); //$NON-NLS-1$
            } else {
                System.err.println(Messages.getString("Server.4") + javaVersion + //$NON-NLS-1$
                                   Messages.getString("Server.5")); //$NON-NLS-1$
            }

            System.exit(1);
        }
    }


    /**
      * parse the command line arguments, read a given server.properties file
      * and check the values given for server ports
      * @return ServerConfig if successfull
      * @throws Exception on any configuration error
      */
    public static ServerConfig getConfig(String[] args) throws Exception {

        ServerConfig config = new ServerConfig();

        // get possible environment setting for helma home
        if (System.getProperty("helma.home")!=null) { //$NON-NLS-1$
            config.setHomeDir(new File(System.getProperty("helma.home"))); //$NON-NLS-1$
        }

        parseArgs(config, args);

        guessConfig(config);

        // create system properties
        ResourceProperties sysProps = new ResourceProperties();
        sysProps.addResource(new FileResource(config.getPropFile()));

        // check if there's a property setting for those ports not specified via command line
        if (!config.hasWebsrvPort() && sysProps.getProperty("webPort") != null) { //$NON-NLS-1$
            try {
                config.setWebsrvPort(getInetSocketAddress(sysProps.getProperty("webPort"))); //$NON-NLS-1$
            } catch (Exception portx) {
                throw new Exception(Messages.getString("Server.6") + portx); //$NON-NLS-1$
            }
        }

        if (!config.hasAjp13Port() && sysProps.getProperty("ajp13Port") != null) { //$NON-NLS-1$
            try {
                config.setAjp13Port(getInetSocketAddress(sysProps.getProperty("ajp13Port"))); //$NON-NLS-1$
            } catch (Exception portx) {
                throw new Exception(Messages.getString("Server.7") + portx); //$NON-NLS-1$
            }
        }

        if (!config.hasXmlrpcPort() && sysProps.getProperty("xmlrpcPort") != null) { //$NON-NLS-1$
            try {
                config.setXmlrpcPort(getInetSocketAddress(sysProps.getProperty("xmlrpcPort"))); //$NON-NLS-1$
            } catch (Exception portx) {
                throw new Exception(Messages.getString("Server.8") + portx); //$NON-NLS-1$
            }
        }
        return config;
    }


    /**
      * parse argument list from command line and store values
      * in given ServerConfig object
      * @throws Exception when argument can't be parsed into an InetAddrPort
      * or invalid token is given.
      */
    public static void parseArgs(ServerConfig config, String[] args) throws Exception {
        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("-h") && ((i + 1) < args.length)) { //$NON-NLS-1$
                config.setHomeDir(new File(args[++i]));
            } else if (args[i].equals("-f") && ((i + 1) < args.length)) { //$NON-NLS-1$
                config.setPropFile(new File(args[++i]));
            } else if (args[i].equals("-a") && ((i + 1) < args.length)) { //$NON-NLS-1$
                config.setApps(StringUtils.split(args[++i]));
            } else if (args[i].equals("-x") && ((i + 1) < args.length)) { //$NON-NLS-1$
                try {
                    config.setXmlrpcPort(getInetSocketAddress(args[++i]));
                } catch (Exception portx) {
                    throw new Exception(Messages.getString("Server.9") + portx); //$NON-NLS-1$
                }
            } else if (args[i].equals("-w") && ((i + 1) < args.length)) { //$NON-NLS-1$
                try {
                    config.setWebsrvPort(getInetSocketAddress(args[++i]));
                } catch (Exception portx) {
                    throw new Exception(Messages.getString("Server.10") + portx); //$NON-NLS-1$
                }
            } else if (args[i].equals("-jk") && ((i + 1) < args.length)) { //$NON-NLS-1$
                try {
                    config.setAjp13Port(getInetSocketAddress(args[++i]));
                } catch (Exception portx) {
                    throw new Exception(Messages.getString("Server.11") + portx); //$NON-NLS-1$
                }
            } else if (args[i].equals("-c") && ((i + 1) < args.length)) { //$NON-NLS-1$
                config.setConfigFile(new File(args[++i]));
            } else if (args[i].equals("-i") && ((i + 1) < args.length)) { //$NON-NLS-1$
                // eat away the -i parameter which is meant for helma.main.launcher.Main
                i++;
            } else {
                throw new Exception(Messages.getString("Server.12") + args[i]); //$NON-NLS-1$
            }
        }
    }


    /**
      * get main property file from home dir or vice versa,
      * depending on what we have
      */
    public static void guessConfig(ServerConfig config) throws Exception {
        // get property file from hopHome:
        if (!config.hasPropFile()) {
            if (config.hasHomeDir()) {
                config.setPropFile(new File(config.getHomeDir(), "server.properties")); //$NON-NLS-1$
            } else {
                config.setPropFile(new File("server.properties")); //$NON-NLS-1$
            }
        }

        // create system properties
        ResourceProperties sysProps = new ResourceProperties();
        sysProps.addResource(new FileResource(config.getPropFile()));

        // try to get hopHome from property file
        if (!config.hasHomeDir() && sysProps.getProperty("hophome") != null) { //$NON-NLS-1$
            config.setHomeDir(new File(sysProps.getProperty("hophome"))); //$NON-NLS-1$
        }

        // use the directory where server.properties is located:
        if (!config.hasHomeDir() && config.hasPropFile()) {
            config.setHomeDir(config.getPropFile().getAbsoluteFile().getParentFile());
        }

        if (!config.hasPropFile()) {
            throw new Exception (Messages.getString("Server.13")); //$NON-NLS-1$
        }

        if (!config.hasHomeDir()) {
            throw new Exception (Messages.getString("Server.14")); //$NON-NLS-1$
        }
    }


    /**
      * print the usage hints and prefix them with a message.
      */
    public static void printUsageError(String msg) {
        System.out.println(msg);
        printUsageError();
    }


    /**
      * print the usage hints
      */
    public static void printUsageError() {
        System.out.println(""); //$NON-NLS-1$
        System.out.println(Messages.getString("Server.15")); //$NON-NLS-1$
        System.out.println(Messages.getString("Server.16")); //$NON-NLS-1$
        System.out.println(Messages.getString("Server.17")); //$NON-NLS-1$
        System.out.println(Messages.getString("Server.18")); //$NON-NLS-1$
        System.out.println(Messages.getString("Server.19")); //$NON-NLS-1$
        System.out.println(Messages.getString("Server.20")); //$NON-NLS-1$
        System.out.println(Messages.getString("Server.21")); //$NON-NLS-1$
        System.out.println(Messages.getString("Server.22")); //$NON-NLS-1$
        System.out.println(Messages.getString("Server.23")); //$NON-NLS-1$
        System.out.println(""); //$NON-NLS-1$
        System.out.println(Messages.getString("Server.24")); //$NON-NLS-1$
        System.out.println(Messages.getString("Server.25")); //$NON-NLS-1$
        System.out.println(Messages.getString("Server.26")); //$NON-NLS-1$
        System.out.println(Messages.getString("Server.27")); //$NON-NLS-1$
        System.out.println(""); //$NON-NLS-1$
        System.err.println(Messages.getString("Server.28")); //$NON-NLS-1$
        System.out.println(""); //$NON-NLS-1$
    }



    /**
     *  Check wheter a server is already running on any of the given ports
     *  - otherwise exit with an error message
     */
    public static void checkRunning(ServerConfig config) {
        // check if any of the specified server ports is in use already
        try {
            if (config.hasWebsrvPort()) {
                checkPort(config.getWebsrvPort());
            }

            if (config.hasXmlrpcPort()) {
                checkPort(config.getXmlrpcPort());
            }

            if (config.hasAjp13Port()) {
                checkPort(config.getAjp13Port());
            }
        } catch (Exception running) {
            System.out.println(running.getMessage());
            System.exit(1);
        }

    }


    /**
     *  Check whether a server port is available by trying to open a server socket
     */
    private static void checkPort(InetSocketAddress endpoint) throws IOException {
        try {
            ServerSocket sock = new ServerSocket();
            sock.bind(endpoint);
            sock.close();
        } catch (IOException x) {
            throw new IOException(Messages.getString("Server.29") + endpoint + Messages.getString("Server.30") + x.getMessage()); //$NON-NLS-1$ //$NON-NLS-2$
        }
    }


    /**
      * initialize the server
      */
    public void init() throws IOException {

        // set the log factory property
        String logFactory = this.sysProps.getProperty(Messages.getString("Server.31"), //$NON-NLS-1$
                                                 Messages.getString("Server.32")); //$NON-NLS-1$

        this.helmaLogging = Messages.getString("Server.33").equals(logFactory); //$NON-NLS-1$
        System.setProperty(Messages.getString("Server.34"), logFactory); //$NON-NLS-1$

        // set the current working directory to the helma home dir.
        // note that this is not a real cwd, which is not supported
        // by java. It makes sure relative to absolute path name
        // conversion is done right, so for Helma code, this should work.
        System.setProperty(Messages.getString("Server.35"), this.hopHome.getPath()); //$NON-NLS-1$

        // from now on it's safe to call getLogger() because hopHome is set up
        getLogger();

        String startMessage = Messages.getString("Server.36") + version + Messages.getString("Server.37") + //$NON-NLS-1$ //$NON-NLS-2$
                              System.getProperty(Messages.getString("Server.38")); //$NON-NLS-1$

        this.logger.info(startMessage);

        // also print a msg to System.out
        System.out.println(startMessage);

        this.logger.info(Messages.getString("Server.39") + this.hopHome); //$NON-NLS-1$


        // read db.properties file in helma home directory
        String dbPropfile = this.sysProps.getProperty(Messages.getString("Server.40")); //$NON-NLS-1$
        File file;
        if ((dbPropfile != null) && !Messages.getString("Server.41").equals(dbPropfile.trim())) { //$NON-NLS-1$
            file = new File(dbPropfile);
        } else {
            file = new File(this.hopHome, Messages.getString("Server.42")); //$NON-NLS-1$
        }

        this.dbProps = new ResourceProperties();
        this.dbProps.setIgnoreCase(false);
        this.dbProps.addResource(new FileResource(file));
        DbSource.setDefaultProps(this.dbProps);

        // read apps.properties file
        String appsPropfile = this.sysProps.getProperty(Messages.getString("Server.43")); //$NON-NLS-1$
        if ((appsPropfile != null) && !Messages.getString("Server.44").equals(appsPropfile.trim())) { //$NON-NLS-1$
            file = new File(appsPropfile);
        } else {
            file = new File(this.hopHome, Messages.getString("Server.45")); //$NON-NLS-1$
        }
        this.appsProps = new ResourceProperties();
        this.appsProps.setIgnoreCase(true);
        this.appsProps.addResource(new FileResource(file));

        this.paranoid = Messages.getString("Server.46").equalsIgnoreCase(this.sysProps.getProperty(Messages.getString("Server.47"))); //$NON-NLS-1$ //$NON-NLS-2$

        String language = this.sysProps.getProperty(Messages.getString("Server.48")); //$NON-NLS-1$
        String country = this.sysProps.getProperty(Messages.getString("Server.49")); //$NON-NLS-1$
        String timezone = this.sysProps.getProperty(Messages.getString("Server.50")); //$NON-NLS-1$

        if ((language != null) && (country != null)) {
            Locale.setDefault(new Locale(language, country));
        }

        if (timezone != null) {
            TimeZone.setDefault(TimeZone.getTimeZone(timezone));
        }

        // logger.debug("Locale = " + Locale.getDefault());
        // logger.debug("TimeZone = " +
        //                 TimeZone.getDefault().getDisplayName(Locale.getDefault()));

        this.dbSources = new Hashtable();

        // try to load the extensions
        this.extensions = new Vector();
        if (this.sysProps.getProperty(Messages.getString("Server.51")) != null) { //$NON-NLS-1$
            initExtensions();
        }
        this.jetty = JettyServer.init(this, this.config);
    }


    /**
      * initialize extensions
      */
    private void initExtensions() {
        StringTokenizer tok = new StringTokenizer(this.sysProps.getProperty(Messages.getString("Server.52")), Messages.getString("Server.53")); //$NON-NLS-1$ //$NON-NLS-2$
        while (tok.hasMoreTokens()) {
            String extClassName = tok.nextToken().trim();

            try {
                Class extClass = Class.forName(extClassName);
                HelmaExtension ext = (HelmaExtension) extClass.newInstance();
                ext.init(this);
                this.extensions.add(ext);
                this.logger.info(Messages.getString("Server.54") + extClassName); //$NON-NLS-1$
            } catch (Throwable e) {
                this.logger.error(Messages.getString("Server.55") + extClassName + Messages.getString("Server.56") + e.toString()); //$NON-NLS-1$ //$NON-NLS-2$
            }
        }
    }



    public void start() {
        // Start running, finishing setup and then entering a loop to check changes
        // in the apps.properties file.
        this.mainThread = new Thread(this);
        this.mainThread.start();
    }

    public void stop() {
        this.mainThread = null;
        this.appManager.stopAll();
    }

    public void shutdown() {
        getLogger().info(Messages.getString("Server.57")); //$NON-NLS-1$

        if (this.jetty != null) {
            try {
                this.jetty.stop();
                this.jetty.destroy();
            } catch (Exception x) {
                // exception in jettx stop. ignore.
            }
        }

        if (this.xmlrpc != null) {
            try {
                this.xmlrpc.shutdown();
            } catch (Exception x) {
                // exception in xmlrpc server shutdown, ignore.
            }
        }
        
        if (this.helmaLogging) {
            Logging.shutdown();
        }
        
        server = null;
        
        try {
            Runtime.getRuntime().removeShutdownHook(this.shutdownhook);
            // HACK: running the shutdownhook seems to be necessary in order
            // to prevent it from blocking garbage collection of helma 
            // classes/classloaders. Since we already set server to null it 
            // won't do anything anyhow.
            this.shutdownhook.start();
            this.shutdownhook = null;
        } catch (Exception x) {
            // invalid shutdown hook or already shutting down. ignore.
        }
    }

    /**
     *  The main method of the Server. Basically, we set up Applications and than
     *  periodically check for changes in the apps.properties file, shutting down
     *  apps or starting new ones.
     */
    public void run() {
        try {
            if (this.config.hasXmlrpcPort()) {
                InetSocketAddress xmlrpcPort = this.config.getXmlrpcPort();
                String xmlparser = this.sysProps.getProperty(Messages.getString("Server.58")); //$NON-NLS-1$

                if (xmlparser != null) {
                    XmlRpc.setDriver(xmlparser);
                }

                if (xmlrpcPort.getAddress() != null) {
                    this.xmlrpc = new WebServer(xmlrpcPort.getPort(), xmlrpcPort.getAddress());
                } else {
                    this.xmlrpc = new WebServer(xmlrpcPort.getPort());
                }

                if (this.paranoid) {
                    this.xmlrpc.setParanoid(true);

                    String xallow = this.sysProps.getProperty(Messages.getString("Server.59")); //$NON-NLS-1$

                    if (xallow != null) {
                        StringTokenizer st = new StringTokenizer(xallow, Messages.getString("Server.60")); //$NON-NLS-1$

                        while (st.hasMoreTokens())
                            this.xmlrpc.acceptClient(st.nextToken());
                    }
                }
                this.xmlrpc.start();
                this.logger.info(Messages.getString("Server.61") + (xmlrpcPort)); //$NON-NLS-1$
            }

            this.appManager = new ApplicationManager(this.appsProps, this);

            if (this.xmlrpc != null) {
                this.xmlrpc.addHandler(Messages.getString("Server.62"), this.appManager); //$NON-NLS-1$
            }

            // add shutdown hook to close running apps and servers on exit
            this.shutdownhook = new HelmaShutdownHook();
            Runtime.getRuntime().addShutdownHook(this.shutdownhook);
        } catch (Exception x) {
            throw new RuntimeException(Messages.getString("Server.63"), x); //$NON-NLS-1$
        }

        // set the security manager.
        // the default implementation is helma.main.HelmaSecurityManager.
        try {
            String secManClass = this.sysProps.getProperty(Messages.getString("Server.64")); //$NON-NLS-1$

            if (secManClass != null) {
                SecurityManager secMan = (SecurityManager) Class.forName(secManClass)
                                                                .newInstance();

                System.setSecurityManager(secMan);
                this.logger.info(Messages.getString("Server.65") + secManClass); //$NON-NLS-1$
            }
        } catch (Exception x) {
            this.logger.error(Messages.getString("Server.66"), x); //$NON-NLS-1$
        }

        // start embedded web server
        if (this.jetty != null) {
            try {
                this.jetty.start();
            } catch (Exception m) {
                throw new RuntimeException(Messages.getString("Server.67"), m); //$NON-NLS-1$
            }
        }

        // start applications
        this.appManager.startAll();

        while (Thread.currentThread() == this.mainThread) {
            try {
                Thread.sleep(3000L);
            } catch (InterruptedException ie) {
            }

            try {
                this.appManager.checkForChanges();
            } catch (Exception x) {
                this.logger.warn(Messages.getString("Server.68") + x); //$NON-NLS-1$
            }
        }
    }

    /**
     * Make sure this server has an ApplicationManager (e.g. used when
     * accessed from CommandlineRunner)
     */
    public void checkAppManager() {
        if (this.appManager == null) {
            this.appManager = new ApplicationManager(this.appsProps, this);
        }
    }

    /**
     *  Get an Iterator over the applications currently running on this Server.
     */
    public Object[] getApplications() {
        return this.appManager.getApplications();
    }

    /**
     * Get an Application by name
     */
    public Application getApplication(String name) {
        return this.appManager.getApplication(name);
    }

    /**
     *  Get a logger to use for output in this server.
     */
    public Log getLogger() {
        if (this.logger == null) {
            if (this.helmaLogging) {
                // set up system properties for helma.util.Logging
                String logDir = this.sysProps.getProperty(Messages.getString("Server.69"), Messages.getString("Server.70")); //$NON-NLS-1$ //$NON-NLS-2$

                if (!Messages.getString("Server.71").equals(logDir)) { //$NON-NLS-1$
                    // try to get the absolute logdir path

                    // set up helma.logdir system property
                    File dir = new File(logDir);
                    if (!dir.isAbsolute()) {
                        dir = new File(this.hopHome, logDir);
                    }

                    logDir = dir.getAbsolutePath();
                }
                System.setProperty(Messages.getString("Server.72"), logDir); //$NON-NLS-1$
            }
            this.logger = LogFactory.getLog(Messages.getString("Server.73")); //$NON-NLS-1$
        }

        return this.logger;
    }

    /**
     *  Get the Home directory of this server.
     */
    public File getHopHome() {
        return this.hopHome;
    }

    /**
     * Get the explicit list of apps if started with -a option
     * @return
     */
    public String[] getApplicationsOption() {
        return this.config.getApps();
    }

    /**
     * Get the main Server instance.
     */
    public static Server getServer() {
        return server;
    }

    /**
     *  Get the Server's  XML-RPC web server.
     */
    public static WebServer getXmlRpcServer() {
        return server.xmlrpc;
    }

    /**
     *
     *
     * @param key ...
     *
     * @return ...
     */
    public String getProperty(String key) {
        return (String) this.sysProps.get(key);
    }

    /**
     * Return the server.properties for this server
     * @return the server.properties
     */
    public ResourceProperties getProperties() {
        return this.sysProps;
    }

    /**
     * Return the server-wide db.properties
     * @return the server-wide db.properties
     */
    public ResourceProperties getDbProperties() {
        return this.dbProps;
    }

    /**
     * Return the apps.properties entries for a given application
     * @param appName the app name
     * @return the apps.properties subproperties for the given app
     */
    public ResourceProperties getAppsProperties(String appName) {
        if (appName == null) {
            return this.appsProps;
        }
        return this.appsProps.getSubProperties(appName + Messages.getString("Server.74")); //$NON-NLS-1$
    }

    /**
     *
     *
     * @return ...
     */
    public File getAppsHome() {
        String appHome = this.sysProps.getProperty(Messages.getString("Server.75"), Messages.getString("Server.76")); //$NON-NLS-1$ //$NON-NLS-2$

        if (appHome.trim().length() != 0) {
            return new File(appHome);
        }
        return new File(this.hopHome, Messages.getString("Server.77")); //$NON-NLS-1$
    }

    /**
     *
     *
     * @return ...
     */
    public File getDbHome() {
        String dbHome = this.sysProps.getProperty(Messages.getString("Server.78"), Messages.getString("Server.79")); //$NON-NLS-1$ //$NON-NLS-2$

        if (dbHome.trim().length() != 0) {
            return new File(dbHome);
        }
        return new File(this.hopHome, Messages.getString("Server.80")); //$NON-NLS-1$
    }

    /**
     *
     *
     * @return ...
     */
    public Vector getExtensions() {
        return this.extensions;
    }

    /**
     *
     *
     * @param name ...
     */
    public void startApplication(String name) {
        this.appManager.start(name);
        this.appManager.register(name);
    }

    /**
     *
     *
     * @param name ...
     */
    public void stopApplication(String name) {
        this.appManager.stop(name);
    }

    private static InetSocketAddress getInetSocketAddress(String inetAddrPort)
            throws UnknownHostException {
        InetAddress addr = null;
        int c = inetAddrPort.indexOf(':');
        if (c >= 0) {
            String a = inetAddrPort.substring(0, c);
            if (a.indexOf('/') > 0)
                a = a.substring(a.indexOf('/') + 1);
            inetAddrPort = inetAddrPort.substring(c + 1);

            if (a.length() > 0 && !Messages.getString("Server.81").equals(a)) { //$NON-NLS-1$
                addr = InetAddress.getByName(a);
            }
        }
        int port = Integer.parseInt(inetAddrPort);
        return new InetSocketAddress(addr, port);
    }
}


