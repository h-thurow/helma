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

package helma.extensions.demo;


import helma.extensions.ConfigurationException;
import helma.extensions.HelmaExtensionInterface;
import helma.framework.core.Application;
import helma.main.Server;
import helma.scripting.ScriptingEngineInterface;
import helma.scripting.rhino.RhinoEngine;
import java.util.HashMap;

/**
 * a demo extension implementation, to activate this add <code>extensions =
 * helma.extensions.demo.DemoExtensions</code> to your <code>server.properties</code>.
 * a new global object <code>demo</code> that wraps helma.main.Server
 * will be added to the scripting environment.
 */
public class DemoExtension implements HelmaExtensionInterface {
    /**
     *
     *
     * @param server ...
     *
     * @throws ConfigurationException ...
     */
    @Override
    public void init(Server server) throws ConfigurationException {
        try {
            // just a demo with the server class itself (which is always there, obviously)
            Class.forName("helma.main.Server"); //$NON-NLS-1$
        } catch (ClassNotFoundException e) {
            throw new ConfigurationException(Messages.getString("DemoExtension.0")); //$NON-NLS-1$
        }
    }

    /**
     *
     *
     * @param app ...
     *
     * @throws ConfigurationException ...
     */
    @Override
    public void applicationStarted(Application app) throws ConfigurationException {
        app.logEvent(Messages.getString("DemoExtension.1") + app.getName()); //$NON-NLS-1$
    }

    /**
     *
     *
     * @param app ...
     */
    @Override
    public void applicationStopped(Application app) {
        app.logEvent(Messages.getString("DemoExtension.2") + app.getName()); //$NON-NLS-1$
    }

    /**
     *
     *
     * @param app ...
     */
    @Override
    public void applicationUpdated(Application app) {
        app.logEvent(Messages.getString("DemoExtension.3") + app.getName()); //$NON-NLS-1$
    }

    /**
     *
     *
     * @param app ...
     * @param engine ...
     *
     * @return ...
     *
     * @throws ConfigurationException ...
     */
    @Override
    public HashMap initScripting(Application app, ScriptingEngineInterface engine)
                          throws ConfigurationException {
        if (!(engine instanceof RhinoEngine)) {
            throw new ConfigurationException(Messages.getString("DemoExtension.4") + engine.toString() + //$NON-NLS-1$
                                             Messages.getString("DemoExtension.5")); //$NON-NLS-1$
        }

        app.logEvent(Messages.getString("DemoExtension.6") + app.getName() + Messages.getString("DemoExtension.7") + //$NON-NLS-1$ //$NON-NLS-2$
                     engine.toString());

        // initialize prototypes and global vars here
        HashMap globals = new HashMap();

        globals.put("demo", Server.getServer()); //$NON-NLS-1$

        return globals;
    }

    /**
     *
     *
     * @return ...
     */
    @Override
    public String getName() {
        return "DemoExtension"; //$NON-NLS-1$
    }
}
