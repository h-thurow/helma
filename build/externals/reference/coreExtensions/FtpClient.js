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
 * $RCSfile: FtpClient.js,v $
 * $Author: zumbrunn $
 * $Revision: 8697 $
 * $Date: 2007-12-10 17:33:40 +0100 (Mon, 10. Dez 2007) $
 */

/**
 * @fileoverview Default properties and methods of  
 * the FtpClient prototype.
 */

 
/**
 * Constructor for File objects, to send and receive files from an FTP server.
 * <br /><br />
 * The FTP client needs Daniel Savarese's NetComponents 
 * library in the classpath in order to work.
 * <br /><br />
 * Example:
 * <pre>var ftp = new FtpClient("ftp.mydomain.com");</pre>
 * 
 * @param {String} server as String, the address of the FTP Server to connect to
 * @constructor
 * @deprecated use helma.Ftp instead
 * @see helma.Ftp
 */
function FtpClient(server) {}


/**
 * Sets transfer mode to ascii for transmitting text-based data.
 * <br /><br />
 * Example:
 * <pre>var ftp = new FtpClient("ftp.host.dom");
 * ftp.login("user", "pass");
 * ftp.ascii();</pre>
 */
FtpClient.prototype.ascii = function() {};


/**
 * Sets transfer mode to binary for transmitting images and other non-text files.
 * <br /><br />
 * Example:
 * <pre>var ftp = new FtpClient("ftp.host.dom");
 * ftp.login("user", "pass");
 * ftp.binary();</pre>
 */
FtpClient.prototype.binary = function() {};


/**
 * Changes the working directory on the FTP server.
 * <br /><br />
 * Example:
 * <pre>var ftp = new FtpClient("ftp.host.dom");
 * ftp.login("user", "pass");
 * &nbsp;
 * // use absolute pathname
 * ftp.cd("/home/users/fred/www");
 * &nbsp;
 * // change to parent directory
 * ftp.cd("..");
 * &nbsp;
 * // use relative pathname
 * ftp.cd("images");</pre>
 * 
 * @param {String} dir as String, the path that the remote working directory should be changed to
 */
FtpClient.prototype.cd = function(dir) {};


/**
 * Transfers a file from the FTP server to the local file system.
 * <br /><br />
 * Example:
 * <pre>var ftp = new FtpClient("ftp.host.dom");
 * ftp.login("user", "pass");
 * ftp.getFile(".htaccess", "htaccess.txt");</pre>
 * 
 * @param {String} source as String, the name of the file that should be downloaded
 * @param {String} dest as String, the name which the file should be stored under
 * @see FtpClient.cd
 * @see FtpClient.lcd
 */
FtpClient.prototype.getFile = function(source,dest) {};


/**
 * Retrieves a file from the FTP server and returns it as string.
 * <br /><br />
 * Example:
 * <pre>var ftp = new FtpClient("ftp.host.dom");
 * ftp.login("user", "pass");
 * var str = ftp.getString("messages.txt");</pre>
 * 
 * @param {String} source as String, the name of the file that should be downloaded
 * @return String containing the data of the downloaded file
 * @type String
 * @see FtpClient.cd
 */
FtpClient.prototype.getString = function(source) {};


/**
 * Changes the working directory of the local machine when being connected to an FTP server.
 * <br /><br />
 * Example:
 * <pre>var ftp = new FtpClient("ftp.host.dom");
 * ftp.login("user", "pass");
 * &nbsp;
 * // use absolute pathname
 * ftp.lcd("/home/users/fred/www");
 * &nbsp;
 * // change to parent directory
 * ftp.lcd("..");
 * &nbsp;
 * // use relative pathname
 * ftp.lcd("images");</pre>
 * 
 * @param {String} dir as String, the path that the local working directory should be changed to
 */
FtpClient.prototype.lcd = function(dir) {};


/**
 * Logs in to the FTP server.
 * <br /><br />
 * The function returns true if successful, false otherwise
 * <br /><br />
 * Example:
 * <pre>var ftp = new FtpClient("ftp.host.dom");
 * if (ftp.login("user", "pass"))
 * &nbsp;&nbsp;res.write("User logged in.");
 * else
 * &nbsp;&nbsp;res.write("Unable to log in.");
 * &nbsp;
 * <i>User logged in.</i></pre>
 * 
 * @param {String} username as String
 * @param {String} password as String
 * @return Boolean true if the login was successful, otherwise false
 * @type Boolean
 */
FtpClient.prototype.login = function(username,password) {};


/**
 * Terminates the current FTP session.
 * <br /><br />
 * Example:
 * <pre>var ftp = new FtpClient("ftp.host.dom");
 * ftp.login("user", "pass");
 * ftp.putFile("htaccess.txt", ".htaccess");
 * ftp.logout();</pre>
 */
FtpClient.prototype.logout = function() {};


/**
 * Creates a new directory on the server.
 * <br /><br />
 * The name of the directory is determined as the function's 
 * string parameter. Returns false when an error occured 
 * (e.g. due to access restrictions, directory already 
 * exists etc.), otherwise true.
 * <br /><br />
 * Example:
 * <pre>var ftp = new FtpClient("ftp.host.dom");
 * ftp.login("user", "pass");
 * if (ftp.mkdir("testdir"))
 * &nbsp;&nbsp;ftp.cd("testdir")
 * else
 * &nbsp;&nbsp;ftp.logout();</pre>
 * 
 * @param {String} name as String, the name of the directory to be created
 * @return Boolean true if the directory was successfully created, false if there was an error
 * @type Boolean
 */
FtpClient.prototype.mkdir = function(name) {};


/**
 * Transfers a file from the local file system to the remote server.
 * <br /><br />
 * Returns true if the transmission was successful, otherwise false.
 * <br /><br />
 * Example:
 * <pre>var ftp = new FtpClient("ftp.host.dom");
 * ftp.login("user", "pass");
 * if (ftp.putFile("testfile"))
 * &nbsp;&nbsp;res.write("File transferred successfully.");
 * else
 * &nbsp;&nbsp;res.write("Transfer error.");
 * &nbsp;
 * <i>File transferred successfully.</i></pre>
 * 
 * @param {String} dest as String, the name of the destination file to be uploaded
 * @return Boolean true if the file was successfully uploaded, false if there was an error
 * @type Boolean
 */
FtpClient.prototype.putFile = function(dest) {};


/**
 * Transfers text from a string to a file on the FTP server.
 * <br /><br />
 * Example:
 * <pre>var ftp = new FtpClient("ftp.host.dom");
 * ftp.login("user", "pass");
 * ftp.putString("Hello, World!", "message.txt");</pre>
 * 
 * @param {String} source as String, the text content that should be uploaded 
 * @param {String} dest as String, the name of the remote destination file
 * @return Boolean true if the file was successfully uploaded, false if there was an error
 * @type Boolean
 */
FtpClient.prototype.putString = function(source,dest) {};

