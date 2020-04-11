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
 * $RCSfile: HopObject.js,v $
 * $Author: root $
 * $Revision: 8604 $
 * $Date: 2007-09-28 15:16:38 +0200 (Fri, 28 Sep 2007) $
 */

/**
 * @fileoverview Default properties and methods of  
 * the User prototype.
 */


/**
 * The built-in User prototype.
 * <br /><br />
 * Normally, the User constructor is never called directly and 
 * the app.registerUser method is used instead to create new
 * users. The User prototype inherits from HopObject and you 
 * can customize it by creating a User directory in your app, 
 * like you would for any other prototype that extends from 
 * HopObject.
 * 
 * @constructor
 * @see app.getActiveUsers
 * @see app.getUser
 * @see app.getRegisteredUsers
 * @see session.user
 * @see session.login
 * @see session.logout
 */
function User() {}


User = new Function();


/**
 * If defined, the onLogout handler is called when the user is logged out.
 * 
 * @see session.logout
 */
User.prototype.onLogout = function() {};


