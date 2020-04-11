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
 * $RCSfile: session.js,v $
 * $Author: zumbrunn $
 * $Revision: 8694 $
 * $Date: 2007-12-10 12:55:38 +0100 (Mon, 10. Dez 2007) $
 */

/**
 * @fileoverview Default properties and methods of the session object.
 */


/**
 * This object is automatically instantiated as the session property 
 * of the global object (or global.session).
 * <br /><br />
 * The session object is a host object representing the session  
 * for the request that is currently handled by the scripting 
 * environment.
 * <br /><br />
 * Each web request is associated with a SessionObject 
 * representing a 'user session'. Helma recognises requests 
 * being made from the same client within the same session through 
 * a session cookie named 'HopSession'. If no such cookie is sent 
 * with the request, Helma will set that a cookie with a random 
 * hash with the next response.
 * <br /><br />
 * Within the scripting environment 'session' always 
 * represents the current session of the user, who initiated 
 * the web request.
 * <br /><br />
 * Besides that default session object, it is also possible 
 * to fetch active sessions of other clients through the method 
 * app.getSessions(), and to create additional SessionObjects 
 * through app.createSession().
 * <br /><br />
 * For further details also see the JavaDocs for 
 * helma.framework.core.SessionBean. Since that class is a 
 * JavaBean all get- and set-methods are also directly 
 * available as properties of that object.
 * 
 * @type SessionBean
 * @see Packages.helma.framework.core.SessionBean
 */
session = new Object();


/**
 * The unique identifier for a session object (session cookie).
 * <br /><br />
 * Contains the unique identifier of the current 
 * session, which is equivalent to the value stored in
 * the HopSession-cookie on the client side.
 * <br /><br />
 * This property is read-only.
 * <br /><br />
 * Example:
 * <pre>res.writeln(session._id);
 * <i>1fcca129764400&#64;eefa22dfab</i></pre>
 * 
 * @type String
 */
session._id = new String();


/**
 * The cookie value of a session.
 * <br /><br />
 * Contains the unique identifier of the current 
 * session, which is equivalent to the value stored 
 * in the HopSession-cookie on the client side.
 * <br /><br />
 * This property is read-only.
 * <br /><br />
 * Example:
 * <pre>res.writeln(session.cookie);
 * <i>1fcca129764400&#64;eefa22dfab</i></pre>
 * 
 * @type String
 */
session.cookie = new String();


/**
 * Object providing space for run-time user data (user cache).
 * <br /><br />
 * This property of the SessionObject offers the 
 * possibility to store arbitrary data within the current 
 * user session. Note, that this can just be used as a 
 * temporary storage, since sessions are not stored 
 * persistently within Helma, and are generally lost when 
 * the application is restarted.
 * <br /><br />
 * Example:
 * <pre>session.data.lastclick = new Date();
 * session.data.language = "en";
 * &nbsp;
 * res.write(session.data);
 * <i>TransientNode session</i>
 * &nbsp;
 * for (var p in session.data)
 * &nbsp;&nbsp;res.writeln(p + ": " + session.data[p]);
 * <i>lastclick: Fri Jul 12 14:08:20 CEST 2002
 * language: en</i>
 * &nbsp;
 * res.write(session.data.lastclick);
 * <i>Fri Jul 12 14:08:20 CEST 2002</i>
 * &nbsp;
 * res.write(session.data["language"]);
 * <i>en</i></pre>
 * 
 * @type TransientNode
 */
session.data = new Object();


/**
 * The date a session was created or a login or logout was performed the last time.
 * <br /><br />
 * This is a convenience property to acknowledge 
 * that pages are often dependent on the login status 
 * and must be re-rendered when a user has logged in, 
 * logged out or is running with a new session.
 * <br /><br />
 * Contains the timestamp of when the associated 
 * client started the session, or logged in, or 
 * logged out the last time, whichever happened most 
 * recently.
 * <br /><br />
 * This property is read- and write-able.
 * <br /><br />
 * Example:
 * <pre>if (session.lastModified < this.modifytime)
 * &nbsp;&nbsp;renderSkin("main");
 * else
 * &nbsp;&nbsp;res.notModified();</pre>
 * 
 * @type Date
 */
session.lastModified = new Date();


/**
 * If set, the message will be available during the next request as res.message
 * 
 * @type String
 */
session.message = new String();


/**
 * A reference to the user object associated with the current session.
 * <br /><br />
 * Contains a reference to the UserObject associated 
 * with this session. This property is null if the client 
 * has not been logged in yet, or has already been logged 
 * out. Checking this for being unequal to null is the 
 * usual way to check whether a client is logged in or not. 
 * This property is read-only, but can be set through the 
 * method session.login(User usr).
 * <br /><br />
 * Example:
 * <pre>session.login("tobi", "mumbl3");
 * res.write(session.user);
 * <i>HopObject tobi</i>
 * &nbsp;
 * res.write(session.user.registered);
 * <i>Thu Jun 28 17:25:33 CEST 2001</i>
 * &nbsp;
 * res.write(session.user["url"]);
 * <i>http://helma.org</i></pre>
 * 
 * @type User
 */
session.user = new User();


/**
 * A date object representing the time this user's session was last active.
 * <br /><br />
 * Contains the timestamp of the last web request, that 
 * has been submitted by that client. This property is 
 * read-only, but can be set to the current time through 
 * session.touch().
 * <br /><br />
 * For new sessions, if the session represents a 
 * registered user the result equals the date the user 
 * was logged in the last time; otherwise the result 
 * equals the current date.
 * <br /><br />
 * This property is read-only
 * <br /><br />
 * Example:
 * <pre>res.write(session.lastActive())
 * <i>Thu Nov 02 16:12:13 GMT+01:00 2000</i></pre>
 * 
 * @return Date of last request by this user
 * @type Date
 */
session.lastActive = function() {};


/**
 * Logs in a user defined by its name and a password phrase, or by directly passing a HopObject.
 * <br /><br />
 * There are two variants of session.login():
 * <br /><br />
 * If called with one HopObject argument, the session 
 * is associated with the user represented by the 
 * HopObject.
 * <br /><br />
 * If called with two string arguments, it returns true 
 * if the user name / password pair matches the stored 
 * values in the database and false otherwise.
 * <br /><br />
 * Associates the passed User to that session, 
 * i.e. logs the client in as that User. The property 
 * user of the session object will refer to the User.
 * <br /><br />
 * Example:
 * <pre>var login = session.login("tobi", "mumbl3");
 * if (login)
 * &nbsp;&nbsp;res.write("Welcome back, " + session.user.name + "!");
 * else
 * &nbsp;&nbsp;res.write("Oops, please try again...");
 * <i>Welcome back, tobi!</i></pre>
 * 
 * @param {User|String} user either as User object to be logged in or the username to be checked as string
 * @param {String} password as String, if the first parameter is a username
 * @return Boolean true if the user was logged in, otherwise false
 * @type Boolean
 */
session.login = function(user,password) {};


/**
 * Logs out a user.
 * <br /><br />
 * Removes the reference to a User associated with 
 * the current session, if such a reference exists. 
 * Additionally the global function onLogout will 
 * be called.
 * <br /><br />
 * Example:
 * <pre>res.write(session);
 * <i>[Session for user tobi]</i>
 * &nbsp;
 * session.logout();
 * res.write(session);
 * <i>[Anonymous Session]</i></pre>
 */
session.logout = function() {};


/**
 * A date object representing the time a user's session was started.
 * <br /><br />
 * This property is read-only.
 * <br /><br />
 * Example:
 * <pre>res.write(session.onSince());
 * <i>Fri Aug 10 16:36:36 GMT+02:00 2001</i></pre>
 * 
 * @return Date when the current session was started.
 * @type Date
 */
session.onSince = function() {};


/**
 * Refreshes the user's session.
 * <br /><br />
 * The session's expiration date is set to the current date 
 * plus session timeout. This also happens automatically 
 * when a user request is sent to Helma.
 * <br /><br />
 * The lastActive property will be set to the current 
 * timestamp. Useful to artificially avoid a session timeout.
 * <br /><br />
 * Example:
 * <pre>res.writeln(session.lastActive);
 * <i>Fri Jul 12 14:40:20 CEST 2002</i>
 * &nbsp;
 * session.touch();
 * &nbsp;
 * res.writeln(session.lastActive);
 * <i>Fri Jul 12 14:55:20 CEST 2002</i></pre>
 */	
session.touch = function() {};

