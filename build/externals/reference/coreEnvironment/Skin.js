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
 * $RCSfile: Skin.js,v $
 * $Author: zumbrunn $
 * $Revision: 8695 $
 * $Date: 2007-12-10 13:04:11 +0100 (Mon, 10. Dez 2007) $
 */

/**
 * @fileoverview Default properties and methods of 
 * the Skin objects.
 */

/**
 * A Skin object that can be passed to the global functions 
 * renderSkin, resp. renderSkinAsString.
 * <br /><br />
 * Skins can either be defined by text files using a *.skin 
 * suffix, placed in an application's code repository or 
 * are created using the global createSkin function. There 
 * is no constructor to instantiate these objects.
 * 
 * @type Packages.helma.framework.core.Skin
 * @see Packages.helma.framework.core.Skin
 * @see global.createSkin
 * @see global.renderSkin
 * @see global.renderSkinAsString
 */
Skin = new Object();


/**
 * Limits the range of allowed macros to be rendered in a skin to an explicit set.
 * <br /><br />
 * This is useful e.g. when text entered by non-trusted users is 
 * interpreted as skins to provide macro functions on a user-level.
 * <br /><br />
 * Example:
 * <pre>// Two macro functions defined in a JavaScript file:
 * &nbsp;
 * function isAllowed_macro() {
 * &nbsp;&nbsp;return("Hello");
 * }
 * &nbsp;
 * function isForbidden_macro() {
 * &nbsp;&nbsp;return("World");
 * }
 * &nbsp;
 * // The action that enables one of the macros:
 * &nbsp;
 * function test_action() {
 * &nbsp;&nbsp;var str = "<% root.isAllowed %>, <% root.isForbidden %>!";
 * &nbsp;&nbsp;var skin = createSkin(str);
 * &nbsp;&nbsp;// as soon as we call allowMacro() on a skin, only those
 * &nbsp;&nbsp;// macros explicitely set are allowed to be evaluated.
 * &nbsp;&nbsp;// all others will result in an error msg.
 * &nbsp;&nbsp;skin.allowMacro("root.isAllowed");
 * &nbsp;&nbsp;renderSkin(skin);
 * }
 * &nbsp;
 * Hello, [Macro root.isForbidden not allowed in sandbox]!</pre>
 * 
 * @param {String} macroname as String
 */
Skin.prototype.allowMacro = function(macroname) {};


/**
 * Checks whether a skin does contain a specific macro.
 * <br /><br />
 * This is useful to make sure a user-edited skin does 
 * not contain any macro with which the application would break.<p>
 * <br /><br />
 * Example:
 * <pre>var skin1 = createSkin("myMacro");
 * var skin2 = createSkin("<% myMacro %>");
 * var skin3 = createSkin("<% this.myMacro %>");
 * &nbsp;
 * res.writeln(skin1.containsMacro("myMacro"));
 * false
 * &nbsp;
 * res.writeln(skin2.containsMacro("myMacro"));
 * true
 * &nbsp;
 * res.writeln(skin3.containsMacro("myMacro"));
 * false
 * &nbsp;
 * res.writeln(skin3.containsMacro("this.myMacro"));
 * true</pre>
 * 
 * @param {String} macroname as String
 * @return Boolean true if the skin contains the specified macro, otherwise false
 * @type Boolean
 */
Skin.prototype.containsMacro = function(macroname) {};


/**
 * Returns the source of the unrendered skin.
 * 
 * @return String with the source of the unrendered skin 
 * @type String
 */
Skin.prototype.getSource = function() {};


/**
 * Returns a subskin by that name.
 * 
 * @param {String} name as String, the subskin name
 * @return Skin the subskin
 * @type Skin
 */
Skin.prototype.getSubskin = function(name) {};


/**
 * Returns an array of names of all the subskins defined in this skin.
 * 
 * @return Array of Strings containing this skin's subskin names
 * @type Array
 */
Skin.prototype.getSubskinNames = function() {};


/**
 * Checks if this skin has a main skin, as opposed to consisting just of subskins.
 * 
 * @return Boolean true if this skin contains a main skin
 * @type Boolean
 */
Skin.prototype.hasMainskin = function() {};


/**
 * Check if this skin contains a subskin with the given name.
 * 
 * @param {String} name as String, a subskin name
 * @return Boolean true if the given subskin exists
 * @type Boolean
 */
Skin.prototype.hasSubskin = function(name) {};

