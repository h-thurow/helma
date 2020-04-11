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
 * $RCSfile: MimePart.js,v $
 * $Author: zumbrunn $
 * $Revision: 8655 $
 * $Date: 2007-11-26 12:46:58 +0100 (Mon, 26. Nov 2007) $
 */

/**
 * @fileoverview Default properties and methods of objects of the 
 * class Packages.helma.util.MimePart.
 */


/**
 * A MimePart object represents a MIME element and makes its 
 * properties and contents available to the scripting environment. 
 * <br /><br />
 * Other than calling the Packages.helma.util.MimePart java class 
 * directly, there is no constructor to instantiate these objects. 
 * 
 * @type Packages.helma.util.MimePart
 * @see Packages.helma.util.MimePart
 * @see global.getURL
 * @see Mail.prototype.addPart
 */
MimePart = new Object();


/**
 * The content length of a MIME part
 * 
 * @type Number
 */	
MimePart.prototype.contentLength = new Number();


/**
 * The content type of a MIME part 
 * 
 * @type String
 */	
MimePart.prototype.contentType = new String();


/**
 * The eTag of a MIME part header
 * 
 * @type String
 */	
MimePart.prototype.eTag = new String();


/**
 * An Java InputStream from which the body of the MIME part
 * can be read.
 *
 * @type java.io.InputStream
 */
MimePart.prototype.inputStream = new Object();


/**
 * The content of a MIME part as java ByteArray
 * 
 * @type ByteArray
 */	
MimePart.prototype.content = new Object();


/**
 * The content of a MIME part as text
 * 
 * @type String
 */	
MimePart.prototype.text = new String();


/**
 * The date header of a MIME part
 * 
 * @type Date
 */	
MimePart.prototype.lastModified = new Date();


/**
 * The name header of a MIME part
 * 
 * @type String
 */	
MimePart.prototype.name = new String();


/**
 * Writes the content of a MIME part to the local file system.
 * <br /><br />
 * Note that the file's current file extension is automaticaly added 
 * to the specified name argument.
 * 
 * @param {String} dir as String, the path to the directory where the file should be written
 * @param {String} name as String, the file name to be used when writting the file
 * @return String the filepath of the written file 
 */	
MimePart.prototype.writeToFile = function(dir,name) {};


/**
 * Get a sub-header from a header, e.g. the charset from 
 * Content-Type: text/plain; charset="UTF-8"
 * 
 * @param {String} header as String, the header from which the sub-header should be returned
 * @param {String} subHeaderName as String, the sub-header that should be retrieved from the header
 * @return String
 * @type String
 */	
MimePart.prototype.getSubHeader = function(header, subHeaderName) {};


/**
 * Normalize a upload file name. Internet Explorer on Windows sends 
 * the whole path, so we cut off everything before the actual name.
 * 
 * @param {String} filename as String, the filepath that is to be reduce to a filename
 * @return String
 * @type String
 */
MimePart.prototype.normalizeFilename = function(filename) {};

