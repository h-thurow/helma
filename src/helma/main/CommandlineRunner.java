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

package helma.main;

import java.io.File;
import java.util.Vector;

import helma.framework.core.Application;

/**
 *  Helma command line runner class. This class creates and starts a single application,
 *  invokes a function in, writes its return value to the console and exits.
 *
 *  @author Stefan Pollach
 */
public class CommandlineRunner {

    /**
     * boot method for running a request from the command line.
     * This retrieves the Helma home directory, creates the app and
     * runs the function.
     *
     * @param args command line arguments
     *
     * @throws Exception if the Helma home dir or classpath couldn't be built
     */
    public static void main(String[] args) throws Exception {

        ServerConfig config = new ServerConfig();
        String commandStr = null;
        Vector funcArgs = new Vector();
    
        // get possible environment setting for helma home
        if (System.getProperty("helma.home")!=null) { //$NON-NLS-1$
            config.setHomeDir(new File(System.getProperty("helma.home"))); //$NON-NLS-1$
        }

        // parse arguments
        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("-h") && ((i + 1) < args.length)) { //$NON-NLS-1$
                config.setHomeDir(new File(args[++i]));
            } else if (args[i].equals("-f") && ((i + 1) < args.length)) { //$NON-NLS-1$
                config.setPropFile(new File(args[++i]));
            } else if (commandStr != null) {
                // we're past the command str, all args for the function
                funcArgs.add (args[i]);
            } else if ((i%2)==0 && !args[i].startsWith("-")) { //$NON-NLS-1$
                // first argument without a switch
                commandStr = args[i];
            }
        }

        // get server.properties from home dir or vv
        try {
            Server.guessConfig (config);
        } catch (Exception ex) {
            printUsageError(ex.toString());
            System.exit(1);
        }

        String appName = null;
        String function = null;
        // now split application name + path/function-name
        try {
            int pos1 = commandStr.indexOf("."); //$NON-NLS-1$
            appName = commandStr.substring(0, pos1);
            function = commandStr.substring(pos1+1);
        } catch (Exception ex) {
            printUsageError();
            System.exit(1);
        }

        // init a server instance and start the application
        Server server = new Server(config);
        server.init();
        server.checkAppManager();
        server.startApplication(appName);
        Application app = server.getApplication(appName);

        // execute the function
        try {
            Object result = app.executeExternal(function, funcArgs);
            if (result != null) {
                System.out.println(result.toString());
            }
        } catch (Exception ex) {
            System.out.println(Messages.getString("CommandlineRunner.0") + appName + ":");  //$NON-NLS-1$//$NON-NLS-2$
            System.out.println(ex.getMessage());
            if ("true".equals(server.getProperty("debug"))) { //$NON-NLS-1$ //$NON-NLS-2$
                System.out.println(""); //$NON-NLS-1$
                ex.printStackTrace();
            }
        }

        // stop the application and server
        server.stop();
        server.shutdown();
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
        System.out.println(Messages.getString("CommandlineRunner.1")); //$NON-NLS-1$
        System.out.println(""); //$NON-NLS-1$
        System.out.println(Messages.getString("CommandlineRunner.2")); //$NON-NLS-1$
        System.out.println(""); //$NON-NLS-1$
        System.out.println(Messages.getString("CommandlineRunner.3")); //$NON-NLS-1$
        System.out.println(Messages.getString("CommandlineRunner.4")); //$NON-NLS-1$
        System.out.println(Messages.getString("CommandlineRunner.5")); //$NON-NLS-1$
        System.out.println(""); //$NON-NLS-1$
    }

}
