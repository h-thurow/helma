/*
 * Helma License Notice
 *
 * The contents of this file are subject to the Helma License
 * Version 2.0 (the "License"). You may not use this file except in
 * compliance with the License. A copy of the License is available at
 * http://adele.helma.org/download/helma/license.txt
 *
 * Copyright 1998-2007 Helma Software. All Rights Reserved.
 *
 * $RCSfile: app.js,v $
 * $Author: zumbrunn $
 * $Revision: 9529 $
 * $Date: 2009-02-20 01:31:56 +0100 (Fre, 20. Feb 2009) $
 */

/**
 * @fileoverview Default properties and methods of the app object.
 */


/**
 * This object is automatically instantiated as the app property 
 * of the global object (or global.app) and there is no constructor 
 * to instantiate further instances.
 * <br /><br />
 * The app object is a host object representing the application 
 * for which the current scripting environment is provided.
 * <br /><br />
 * For further details also see the JavaDocs for 
 * Packages.helma.framework.core.ApplicationBean. Since that class is a 
 * JavaBean all of its get- and set-methods are also directly 
 * available as properties of this object.
 * 
 * @type ApplicationBean
 * @see Packages.helma.framework.core.ApplicationBean
 */
app = new Object();


/**
 * This property contains a reference this application's instance of the Packages.helma.framework.core.Application class.
 * <br /><br />
 * It represents the currently running application, and 
 * offers some additional public methods. See Helma's JavaDocs 
 * on Packages.helma.framework.core.Application for more information.
 * 
 * @type Application
 * @see Packages.helma.framework.core.Application
 */
app.__app__ = new Object();


/**
 * Object that serves as a cache for application specific data.
 * <br /><br />
 * This property offers the possibility to store arbitrary data 
 * on an application wide level, and is available as long as the 
 * application is running.
 * <br /><br />
 * Note, that this can just be used as a temporary storage 
 * (i.e. as a 'cache'), since this data is not stored persistently 
 * within Helma, and is lost when the application is restarted. 
 * However, unlike global properties, any data stored in this 
 * object will not be garbage collected during runtime.
 * <br /><br />
 * Example:
 * <pre>app.data.runlevel = 4;
 * app.data.language = "en";
 * &nbsp;
 * res.write(app.data);
 * <i>TransientNode app</i>
 * &nbsp;
 * for (var p in app.data)
 * &nbsp;&nbsp;res.writeln(p + ": " + app.data[p]);
 * <i>runlevel: 4
 * language: en</i>
 * &nbsp;
 * res.write(app.data.runlevel);
 * <i>4</i>
 * &nbsp;
 * res.write(app.data["language"]);
 * <i>en</i></pre>
 * 
 * @type TransientNode
 * @see app.modules
 */
app.data = new Object();


/**
 * The app.globalMacroPath property allows to define a search path for global macros. 
 * This is a String array containing a list of global namespaces.
 * 
 * @type String[]
 */
app.globalMacroPath = new Object();


/**
 * This property contains the maximum number of additional threads (=request evaluators)
 * <br /><br />
 * The maximum number of additional threads that are being 
 * created by Helma to handle incoming requests. This property 
 * is readable and writeable.
 * 
 * @return Number of additional threads (integer) 
 * @type Number
 * @see app.getActiveThreads
 * @see app.getFreeThreads
 * @see app.getMaxThreads
 */
app.maxThreads = new Number();


/**
 * Map object that can be used as an application wide data cache
 * <br /><br />
 * This property offers a dedicated place to store 
 * module-related data on an application wide level. 
 * Note, that this can just be used as a temporary 
 * storage (i.e. as a 'cache'), since this data is not 
 * stored persistently within Helma, and is lost when 
 * the application is restarted.
 * 
 * @type Map
 * @see app.data
 */
app.modules = new Object();


/**
 * Map of any specified application or server properties
 * <br /><br />
 * This property offers access to each key/value 
 * pair defined in either app.properties or in 
 * server.properties. These properties are read-only.
 * <br /><br />
 * To get a Map of apps.properties for this application,
 * use the app.getAppsProperties method instead.
 * <br /><br />
 * Example:
 * <pre># File app.properties:
 * debug = true
 * color = #ffcc00
 * &nbsp;
 * res.write(app.properties);
 * <i>{debug=true,color=#ffcc00}</i>
 * &nbsp;
 * for (var p in app.properties)
 * &nbsp;&nbsp;res.writeln(p + ": " + app.properties[p]);
 * <i>debug: true
 * color: #ffcc00</i>
 * &nbsp;
 * res.write(app.properties.debug);
 * <i>true</i>
 * &nbsp;
 * res.write(app.properties["color"]);
 * <i>#ffcc00</i></pre>
 * 
 * @type Map
 * @see getAppsProperties
 */
app.properties = new Object();


/**
 * This allows the application to set a callback function for pre-processing 
 * macro parameters formatted as $(...). The function is expected to take 
 * the raw parameter value as argument and return the processed parameter.
 * 
 * @type Function
 */
app.processMacroParameter = new Object();


/**
 * List of the application's code repositories
 * <br /><br />
 * Returns an array of FileRepository, SingleFileRepository, 
 * MultiFileRepository and ZipRepository objects.
 * 
 * @return Array of repository objects
 * @type Array
 * @see app.addRepository
 * @see Packages.helma.framework.repository.FileRepository
 * @see Packages.helma.framework.repository.SingleFileRepository
 * @see Packages.helma.framework.repository.MultiFileRepository
 * @see Packages.helma.framework.repository.ZipRepository
 */
app.repositories = new Object();


/**
 * Adds a code repository to the current application
 * 
 * @param {String} repository as String, the path to the code repository 
 * @param {Repository} repository as a Repository object, implementing the Repository interface
 * @see app.repositories
 */
app.addRepository = function(repository) {};


/**
 * Adds a global function to the list of CronJobs that are being called periodically.
 * <br /><br />
 * If the property 'schedulerInterval' has not been set 
 * otherwise in app.properties, the function will be called 
 * every 60 seconds.
 * <br /><br />
 * By passing along further arguments it is possible to define 
 * at what times that function should be called. The same syntax 
 * ('*', '1,10,15', '1-5',..) as for Unix' crontab file can be used. 
 * Note, that if the property 'schedulerInterval' has been set in 
 * app.properties below 60, the function will be called several 
 * times in the minute, that it is supposed to run.
 * 
 * @param {String} functionName as String, name of function to be scheduled as cron job
 * @param {String} crontab syntax of comma delimited arguments for year, month, day, weekday, hour, minute
 * @see app.getCronJobs
 * @see app.removeCronJob
 */
app.addCronJob = function(functionNameAsString, year, month, day, weekday, hour, minute) {};


/**
 * Removes all objects from the object cache for the current application.
 * <br /><br />
 * By calling this method it is possible to make sure Helma 
 * will fetch all objects fresh from the database.
 * 
 * @see app.getCacheUsage
 */
app.clearCache = function() {};


/**
 * Returns the number of currently active sessions.
 * 
 * @return Number of sessions (integer)
 * @type Number
 * @see app.createSession
 * @see app.getSession
 * @see app.getSessions
 * @see app.getSessionsForUser
 */
app.countSessions = function() {};


/**
 * Creates a SessionObject with the passed sessionID as its unique identifier.
 * <br /><br />
 * If a session with that ID already exists, that session will be returned.
 * 
 * @param {String} String of the ID for this session
 * @return SessionObject for the specified 
 * @type SessionObject
 * @see app.countSessions
 * @see app.getSession
 * @see app.getSessions
 * @see app.getSessionsForUser
 */
app.createSession = function(sessionID) {};


/**
 * Writes a string to a log file if debug is set to true in app.properties.
 * <br /><br />
 * Either the text provided as the first argument is written 
 * to the eventLog file or the text that is provided as the second 
 * argument is written to a log file who's name is specified as the 
 * first argument.
 * <br /><br />
 * Writing to a file can be overridden by setting the property 
 * "logdir" in the server.properties or app.properties file to 
 * the value "console".
 * <br /><br />
 * Example:
 * <pre># File helma/apps/test/app.properties:
 * debug = true
 * &nbsp;
 * // File helma/apps/test/root/main.hac:
 * app.debug("This message is written to the test application's event log.");
 * app.debug("custom", "This message is written to the custom.log file.");
 * &nbsp;
 * File helma/log/test_event.log:
 * <i>[2006/07/11 17:08] This message is written to the test application's event log.</i>
 * &nbsp;
 * File helma/log/custom.log:
 * <i>[2006/07/11 17:08] This message is written to the custom.log file.</i></pre>
 * 
 * @param {String} filenameOrText as String, the log filename to write to or the text to write to the eventLog file
 * @param {String} text as String the text to write, if the filename was specified as the first argument
 * @see app.log
 */
app.debug = function(filenameOrText,text) {};


/**
 * Returns the number of currently active threads (=request evaluators).
 * 
 * @return Number of currently active threads (integer) 
 * @type Number
 * @see app.getFreeThreads
 * @see app.getMaxThreads
 * @see app.maxThreads
 */
app.getActiveThreads = function() {};


/**
 * Returns an array of Users, that are currently logged in.
 * 
 * @return Array of User objects
 * @type Array
 * @see app.getUser
 * @see app.getRegisteredUsers
 * @see app.registerUser
 */
app.getActiveUsers = function() {};


/**
 * Returns the absolute path to the application directory.
 * <br /><br />
 * For multiple repositories this is either, the directory 
 * specified as 'appdir' in apps.properties, or the first 
 * FileRepository occurring in the repository list.
 * 
 * @return String with the absolut path to the application directory
 * @type String
 * @see app.getDir
 * @see app.getServerDir
 */
app.getAppDir = function() {};


/**
 * Get a wrapper around the app's apps.properties
 * <br /><br />
 * To get a Map of app.properties and server.properties for 
 * this application, use app.properties instead.
 * 
 * @return a readonly wrapper around the application's apps.properties
 * @type Map
 * @see app.properties
 */
app.getAppsProperties = function() {};


/**
 * Returns the number of currently cached objects for the current application.
 * 
 * @return Number of cached objects
 * @type Number
 * @see app.clearCache
 */
app.getCacheusage = function() {};


/**
 * Returns the app's ClassLoader.
 * 
 * @return ClassLoader
 * @type ClassLoader
 */
app.getClassLoader = function() {};


/**
 * Returns an object of scheduled cron jobs.
 * <br /><br />
 * Returns a JavaScript object with the function names as 
 * property names and the Packages.helma.util.CronJob instance as 
 * property values.
 * 
 * @return Object with Packages.helma.util.CronJob properties
 * @type Object
 * @see app.addCronJob
 * @see app.removeCronJob
 */
app.getCronJobs = function() {};


/**
 * Returns the absolute path to the application directory.
 * <br /><br />
 * For multiple repositories this is either, the directory 
 * specified as 'appdir' in apps.properties, or the first 
 * FileRepository occurring in the repository list.
 * 
 * @return String with the absolut path to the application directory
 * @type String
 * @see app.getAppDir
 * @see app.getServerDir
 */
app.getDir = function() {};


/**
 * Returns the number of errors that have occurred since the application has been started.
 * 
 * @return Number of errors (integer)
 * @type Number
 * @see app.getUpSince
 */
app.getErrorCount = function() {};


/**
 * Returns the number of currently free threads (i.e. request evaluators).
 * <br /><br />
 * This is equivalent to app.getMaxThreads() minus app.getActiveThreads().
 * 
 * @return Number of free threads (integer)
 * @type Number
 * @see app.getMaxThreads
 * @see app.getActiveThreads
 * @see app.maxThreads
 */
app.getFreeThreads = function() {};


/**
 * Returns the app's event logger. 
 * <br /><br />
 * This is a  commons-logging Log with the
 * category helma.[appname].event or the
 * specified logname.
 * 
 * @param {String} logname as String, optional log category
 * @return Log
 * @type Log
 * @see app.log
 */
app.getLogger = function(logname) {};


/**
 * Returns the maximum number of threads (i.e. request evaluators).
 * 
 * @return Number of threads (integer)
 * @type Number
 * @see app.getActiveThreads
 * @see app.getFreeThreads
 * @see app.maxThreads
 */
app.getMaxThreads = function() {};


/**
 * Returns the name of the current application, i.e. the name used in apps.properties.
 * 
 * @return String with application's name
 * @type String
 */
app.getName = function() {};


/**
 * Returns a prototype by name.
 *
 * @param {String} name as String, the prototype name
 * @return Prototype
 * @type Prototype
 */
app.getPrototype = function(name) {};


/**
 * Returns an array of this app's prototypes
 *
 * @return Array containing the app's prototypes
 * @type Array
 */
app.getPrototypes = function() {};


/**
 * Returns an array of all existing users.
 * 
 * @return Array of User objects
 * @type Array
 * @see app.getActiveUsers
 * @see app.getUser
 * @see app.registerUser
 */
app.getRegisteredUsers = function() {};


/**
 * Returns the number of web requests that occurred since the application has been started.
 * 
 * @return Number of web requests served (integer)
 * @type Number
 * @see app.getUpSince
 */
app.getRequestCount = function() {};


/**
 * Returns the absolute path to the home directory of this Helma installation.
 * <br /><br />
 * If Helma is run in embedded mode, this will be 
 * equal to the application directory.
 * 
 * @return String with absolute path to home directory
 * @type String
 * @see app.getAppDir
 * @see app.getDir
 */
app.getServerDir = function() {};


/**
 * Returns a SessionObject identified through the passed sessionID, if such a session exists.
 * 
 * @param {String} sessionID as String, the ID of an existing session
 * @return SessionObject matching the specified ID or null if there is no matching session
 * @type SessionObject
 * @see app.countSessions
 * @see app.createSession
 * @see app.getSessions
 * @see app.getSessionsForUser
 */
app.getSession = function(sessionID) {};


/**
 * Returns an array of all currently active sessions, represented as SessionObjects.
 * 
 * @return Array of SessionObject
 * @type Array
 * @see app.countSessions
 * @see app.createSession
 * @see app.getSession
 * @see app.getSessionsForUser
 */
app.getSessions = function() {};


/**
 * Returns an array of active sessions for the specified user
 * <br /><br />
 * Returns an array of all currently active sessions, which 
 * have been associated with the passed User. Returns an empty 
 * array if no User is passed.
 * <br /><br />
 * Passing a string of the username as argument has been 
 * deprecated in favor of passing a User object.
 * 
 * @param {User} user as User object
 * @return Array of SessionObject
 * @type Array
 * @see app.countSessions
 * @see app.createSession
 * @see app.getSession
 * @see app.getSessions
 */
app.getSessionsForUser = function(user) {};


/**
 * Returns a skin for a given object.
 * <br /><br />
 * The skin is found by determining the prototype to use for 
 * the object, then looking up the skin for the prototype.
 * 
 * @param {String} protoname as String
 * @param {String} skinname as String
 * @param {Array} skinpath as Array, directory paths or HopObjects to search for skins
 * @return Skin
 * @type Skin
 */
app.getSkin = function(protoname, skinname, skinpath) {};


/**
 * Returns a Map that allows access to all defined file-based skins.
 * <br /><br />
 * The map contains for each prototype one entry (for 
 * prototypes containing also uppercase letters, also an 
 * entry with the lowercased prototype name is contained). 
 * This entry contains for each skin file residing in that 
 * prototype directory, an entry with the name of the file 
 * and the content/source of that file as its value.
 * 
 * @return Map of SkinMap objects
 * @type Map
 * @see app.getSkinfilesInPath
 */
app.getSkinfiles = function() {};


/**
 * Return a map of skin resources including the specified,
 * app-specific skinpath
 * <br /><br />
 * The map contains for each prototype one entry (for 
 * prototypes containing also uppercase letters, also an 
 * entry with the lowercased prototype name is contained). 
 * This entry contains for each skin file residing in that 
 * prototype directory, an entry with the name of the file 
 * and the content/source of that file as its value.
 * 
 * @param {Array} skinpath as Array, directory paths or HopObjects to search for skins
 * @return Map of SkinMap objects
 * @type Map
 * @see app.getSkinfiles
 */
app.getSkinfilesInPath = function(skinpath) {};


/**
 * Returns the timestamp of when that application has been started.
 * 
 * @return Date when the application last started
 * @type Date
 * @see app.getErrorCount
 */
app.getUpSince = function() {};


/**
 * Returns a User identified through the passed username.
 * <br /><br />
 * The prototype User must have a username defined 
 * through the '_name'-property in type.properties for 
 * this to work.
 * 
 * @param {String} username as String
 * @return User object with the specified _name property
 * @type User
 * @see app.getActiveUsers
 * @see app.getRegisteredUsers
 * @see app.registerUser
 */
app.getUser = function(username) {};


/**
 * Returns the number of XmlRpc requests that occurred since the application has been started.
 * 
 * @return Number of XmlRpc requests served (integer)
 * @type Number
 * @see app.getUpSince
 */
app.getXmlrpcCount = function() {};


/**
 * Trigger a synchronous Helma invocation.
 *
 * @param {Object} thisObject as Object, the object to invoke the function on, or null for global invocation
 * @param {Function} fnc the function or function name to invoke
 * @param {Array} args as Array, optional arguments to be passed to the function fnc
 * @param {Number} timeout as Number, optional amount of milliseconds after which the invocation should be interrupted
 * @return the value returned by the function fnc
 * @throws Exception exception thrown by the function
 * @see app.invokeAsync
 */
app.invoke = function(thisObject,fnc,args,timeout) {};


/**
 * Trigger an asynchronous Helma invocation. 
 * <br /><br />
 * This method returns immedately with an object that 
 * allows to track the result of the function invocation 
 * with the following methods and properties:
 * <ul>
 * <li>running - true while the function is running, false afterwards</li>
 * <li>result - the value returned by the function, if any</li>
 * <li>exception - the exception thrown by the function, if any</li>
 * <li>waitForResult() - wait indefinitely until invocation terminates
 * and return the result</li>
 * <li>waitForResult(t) - wait for the specified number of milliseconds
 * for invocation to terminate and return the result</li>
 * </ul>
 * Setting the timeout to -1 will let the task run forever instead 
 * of the default maximum of 15 minutes.
 * 
 * @param {Object} thisObject as Object, the object to invoke the function on, or null for global invocation
 * @param {Function} fnc the function or function name to invoke
 * @param {Array} args as Array, optional arguments to be passed to the function fnc
 * @param {Number} timeout as Number, optional amount of milliseconds after which the invocation should be interrupted
 * @return Object with the properties described above
 * @type Object
 * @see app.invoke
 */
app.invokeAsync = function(thisObject,fnc,args,timeout) {};


/**
 * Writes a string to a log file.
 * <br /><br />
 * Either the text provided as the first argument is written 
 * to the eventLog file or the text that is provided as the second 
 * argument is written to a log file who's name is specified as 
 * the first argument.
 * <br /><br />
 * Writing to a file can be overriden by setting the property 
 * "logdir" in the server.properties or app.properties file to 
 * the value "console".
 * <br /><br />
 * Example:
 * <pre>app.debug("This message is written to the test application's event log.");
 * app.debug("custom", "This message is written to the custom.log file.");
 * &nbsp;
 * File helma/log/test_event.log:
 * <i>[2006/07/11 17:08] This message is written to the test application's event log.</i>
 * &nbsp;
 * File helma/log/custom.log:
 * <i>[2006/07/11 17:08] This message is written to the custom.log file.</i></pre>
 * 
 * @param {String} filenameOrText as String, the log filename to write to or the text to write to the eventLog file
 * @param {String} text as String the text to write, if the filename was specified as the first argument
 * @see app.debug
 */
app.log = function(filenameOrText,text) {};


/**
 * Creates a new User object
 * <br /><br />
 * Creates a new HopObject of prototype User, stores it 
 * persistently, and returns the created User object. 
 * <br /><br />
 * When mapping the User prototype to a relational 
 * database, the type.properties file must contain mappings
 * for the '_name'-property (the username) and for a 
 * property named 'password'.
 * 
 * @param {String} username as String
 * @param {String} password as String
 * @return User object that was created
 * @type User
 * @see app.getActiveUsers
 * @see app.getUser
 * @see app.getRegisteredUsers
 */
app.registerUser = function(username, password) {};


/**
 * Removes a CronJob, identified through the passed function name, from the list of CronJobs.
 * 
 * @param {String} functionName as String
 * @see app.addCronJob
 * @see app.getCronJobs
 */
app.removeCronJob = function(functionName) {};



