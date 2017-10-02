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

package helma.extensions;

import helma.framework.core.Application;
import helma.main.Server;
import helma.scripting.ScriptingEngineInterface;

import java.util.HashMap;

/**
 * Helma extensions have to implement this. The extensions to be loaded are
 * defined in <code>server.properties</code> by setting <code>extensions =
 * packagename.classname, packagename.classname</code>.
 */
public interface HelmaExtensionInterface {

    /**
     * called by the Server at startup time. should check wheter the needed classes
     * are present and throw a ConfigurationException if not.
     */
    public void init(Server server) throws ConfigurationException;

    /**
     * called when an Application is started. This should be <b>synchronized</b> when
     * any self-initialization is performed.
     */
    public void applicationStarted(Application app)
            throws ConfigurationException;

    /**
     * called when an Application is stopped.
     * This should be <b>synchronized</b> when any self-destruction is performed.
     */
    public void applicationStopped(Application app);

    /**
     * called when an Application's properties are have been updated.
     * note that this will be called at startup once *before* applicationStarted().
     */
    public void applicationUpdated(Application app);

    /**
     * called by the ScriptingEngineInterface when it is initizalized. Throws a ConfigurationException
     * when this type of ScriptingEngineInterface is not supported. New methods and prototypes can be
     * added to the scripting environment. New global vars should be returned in a HashMap
     * with pairs of varname and ESObjects. This method should be <b>synchronized</b>, if it
     * performs any other self-initialization outside the scripting environment.
     */
    public HashMap initScripting(Application app,
            ScriptingEngineInterface engine) throws ConfigurationException;

    /**
     *
     *
     * @return ...
     */
    public String getName();

}
