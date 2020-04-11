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
 * $RCSfile: File.js,v $
 * $Author: zumbrunn $
 * $Revision: 8697 $
 * $Date: 2007-12-10 17:33:40 +0100 (Mon, 10. Dez 2007) $
 */

/**
 * @fileoverview Default properties and methods of  
 * the File prototype.
 */


/**
 * Constructor for File objects, providing read and write access to the file system.
 * <br /><br />
 * Example:
 * <pre>var fileOrDir = new File('static/test.txt');</pre>
 * 
 * @param {String} filepath as String
 * @constructor
 * @deprecated use helma.File instead
 * @see helma.File
 */
File = function(filepath) {};


/**
 * Returns the name of the file or directory represented by this File object.
 * <br /><br />
 * This is just the last name in the pathname's name sequence. If the pathname's 
 * name sequence is empty, then the empty string is returned.
 * 
 * @return String containing the name of the file or directory
 * @type String
 */
File.prototype.getName = function() {};


/**
 * Returns the pathname string of this File object's parent directory.
 * 
 * @return String containing the pathname of the parent directory
 * @type String
 */
File.prototype.getParent = function() {};


/**
 * Tests whether this File object's pathname is absolute. 
 * <br /><br />
 * The definition of absolute pathname is system dependent. 
 * On UNIX systems, a pathname is absolute if its prefix is "/". 
 * On Microsoft Windows systems, a pathname is absolute if its prefix 
 * is a drive specifier followed by "\\", or if its prefix is "\\".
 * 
 * @return Boolean if this abstract pathname is absolute, false otherwise
 * @type Boolean
 */
File.prototype.isAbsolute = function() {};


/**
 * Appends a string to the file represented by this File object.
 * 
 * @param {String} data as String, to be written to the file
 * @return Boolean
 * @type Boolean
 * @see File.writeln
 */
File.prototype.write = function(data) {};


/**
 * Deletes the file or directory represented by this File object.
 * 
 * @return Boolean
 * @type Boolean
 */
File.prototype.remove = function() {};


/**
 * List of all files within the directory represented by this File object.
 * <br /><br />
 * You may pass a RegExp Pattern to return just files matching this pattern.
 * <br /><br />
 * Example:
 * <pre>var xmlFiles = dir.list(/.*\.xml/);</pre>
 * 
 * @param {RegExp} pattern as RegExp, optional pattern to test each file name against
 * @return Array the list of file names
 * @type Array
 */
File.prototype.list = function(pattern) {};


/**
 * Purges the content of the file represented by this File object.
 * 
 * @return Boolean
 * @type Boolean
 */
File.prototype.flush = function() {};


/**
 * Appends a string with a platform specific end of 
 * line to the file represented by this File object.
 * 
 * @param {String} data as String, to be written to the file
 * @return Boolean
 * @type Boolean
 * @see File.write
 */
File.prototype.writeln = function(data) {};


/**
 * Closes the file represented by this File object.
 * 
 * @return Boolean
 * @type Boolean
 */
File.prototype.close = function() {};


/**
 * Returns the pathname string of this File object. 
 * <br /><br />
 * The resulting string uses the default name-separator character 
 * to separate the names in the name sequence.
 * 
 * @return String of this file's pathname
 * @type String
 */
File.prototype.getPath = function() {};


/**
 * Opens the file represented by this File object.
 * 
 * @return Boolean
 * @type Boolean
 */
File.prototype.open = function() {};


/**
 * Contains the last error that occured, if any.
 * @return String
 * @type String
 * @see File.clearError
 */
File.prototype.error = function(Object) {};


/**
 * Tests whether the application can read the file 
 * represented by this File object.
 * 
 * @return Boolean true if the file exists and can be read; false otherwise
 * @type Boolean
 */
File.prototype.canRead = function() {};


/**
 * Tests whether the file represented by this File object is writable.
 * 
 * @return Boolean true if the file exists and can be modified; false otherwise.
 * @type Boolean
 */
File.prototype.canWrite = function() {};


/**
 * Tests whether the file or directory represented by this File object exists.
 * 
 * @return Boolean true if the file or directory exists; false otherwise
 * @type Boolean
 */
File.prototype.exists = function() {};


/**
 * Returns the absolute pathname string of this file.
 * <br /><br />
 * If this File object's pathname is already absolute, then the pathname 
 * string is simply returned as if by the getPath() method. If this 
 * abstract pathname is the empty abstract pathname then the pathname 
 * string of the current user directory, which is named by the system 
 * property user.dir, is returned. Otherwise this pathname is resolved 
 * in a system-dependent way. On UNIX systems, a relative pathname is 
 * made absolute by resolving it against the current user directory. 
 * On Microsoft Windows systems, a relative pathname is made absolute 
 * by resolving it against the current directory of the drive named by 
 * the pathname, if any; if not, it is resolved against the current user 
 * directory.
 * 
 * @return String The absolute pathname string
 * @type String
 */
File.prototype.getAbsolutePath = function() {};


/**
 * Returns the length of the file represented by this File object. 
 * <br /><br />
 * The return value is unspecified if this pathname denotes a directory.
 * 
 * @return Number The length, in bytes, of the file, or 0L if the file does not exist
 * @type Number
 */
File.prototype.getLength = function() {};


/**
 * Tests whether the file represented by this File object is a directory.
 * 
 * @return Boolean true if this File object is a directory and exists; false otherwise
 * @type Boolean
 */
File.prototype.isDirectory = function() {};


/**
 * Tests whether the file represented by this File object is a normal file. 
 * <br /><br />
 * A file is normal if it is not a directory and, in addition, satisfies 
 * other system-dependent criteria. Any non-directory file created by a 
 * Java application is guaranteed to be a normal file.
 * 
 * @return Boolean true if this File object is a normal file and exists; false otherwise
 * @type Boolean
 */
File.prototype.isFile = function() {};


/**
 * Returns the time when the file represented by this File object was last modified.
 * <br /><br />
 * A number representing the time the file was last modified, 
 * measured in milliseconds since the epoch (00:00:00 GMT, January 1, 1970), 
 * or 0L if the file does not exist or if an I/O error occurs.
 * 
 * @return Number in milliseconds since 00:00:00 GMT, January 1, 1970
 * @type Number
 */
File.prototype.lastModified = function() {};


/**
 * Creates the directory represented by this File object.
 * 
 * @return Boolean true if the directory was created; false otherwise
 * @type Boolean
 */
File.prototype.mkdir = function() {};


/**
 * Renames the file represented by this File object.
 * <br /><br />
 * Whether or not this method can move a file from one filesystem to another is 
 * platform-dependent. The return value should always be checked to make sure 
 * that the rename operation was successful. 
 * 
 * @param {FileObject} dest as FileObject of the new path
 * @return true if the renaming succeeded; false otherwise
 * @type Boolean
 */
File.prototype.renameTo = function(dest) {};


/**
 * Returns true if the file represented by this File object
 * has been read entirely and the end of file has been reached.
 * 
 * @return Boolean
 * @type Boolean
 */
File.prototype.eof = function() {};


/**
 * Returns true if the file represented by this File object
 * is currently open.
 * 
 * @return Boolean
 * @type Boolean
 */
File.prototype.isOpened = function() {};


/**
 * This methods reads characters until an end of line/file is encountered 
 * then returns the string for these characters (without any end of line 
 * character).
 * 
 * @return String of the next unread line in the file
 * @type String
 */
File.prototype.readln = function() {};


/**
 * Clears any error message that may otherwise be returned by the error method.
 * 
 * @see File.error
 */
File.prototype.clearError = function() {};


/**
 * This methods reads all the lines contained in the 
 * file and returns them.
 * 
 * @return String of all the lines in the file
 * @type String
 */
File.prototype.readAll = function() {};

