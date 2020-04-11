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
 * $RCSfile: Mail.js,v $
 * $Author: zumbrunn $
 * $Revision: 8697 $
 * $Date: 2007-12-10 17:33:40 +0100 (Mon, 10. Dez 2007) $
 */

/**
 * @fileoverview Default properties and methods of  
 * the Mail prototype.
 */


/**
 * Helma's built-in mail client enables you to send e-mail via SMTP.
 * <br /><br />
 * A mail client object is created by using the Mail() constructor.
 * The mail object then can be manipulated and sent using the 
 * methods listed below.
 * <br /><br />
 * You'll need the JavaMail library installed for this to work. 
 * Also, don't forget to set your mail server via the smtp 
 * property in the app.properties or server.properties file.
 * <br /><br />
 * Note: Make sure that the SMTP server itself is well-configured, 
 * so that it accepts e-mails coming from your server and does 
 * not deny relaying. Best and fastest configuration is of course 
 * if you run your own SMTP server (e.g. postfix) which might be 
 * a bit tricky to set up, however.
 * 
 * @constructor
 * @deprecated use helma.Mail instead
 * @see helma.Mail
 */
function Mail(){}


/**
 * Adds a recipient to the list of addresses to get a "blind carbon copy" of an e-mail message.
 * <br /><br />
 * The emailString argument specifies the receipient's e-mail address. 
 * The optional nameString argument specifies the name of the recipient.
 * <br /><br />
 * Example:
 * <pre>var mail = new Mail();
 * mail.addBCC("hop&#64;helma.at");
 * mail.addBCC("tobi&#64;helma.at", "Tobi Schaefer");</pre>
 * 
 * @param {String} emailString as String, receipients email address
 * @param {String} nameString as String, optional receipients name
 */
Mail.prototype.addBCC = function(emailString, nameString) {};


/**
 * Adds an address to the list of carbon copy recipients.
 * <br /><br />
 * The emailString argument specifies the receipient's e-mail address. 
 * The optional nameString argument specifies the name of the recipient.
 * <br /><br />
 * Example:
 * <pre>var mail = new Mail();
 * mail.addCC("hop&#64;helma.at");
 * mail.addCC("tobi&#64;helma.at", "Tobi Schaefer");</pre>
 * 
 * @param {String} emailString as String, receipients email address
 * @param {String} nameString as String, optional receipients name
 */
Mail.prototype.addCC = function(emailString, nameString) {};


/**
 * Adds an attachment to an e-mail message.
 * <br /><br />
 * The attachment needs to be either a MIME Object or a java.io.file object.
 * <br /><br />
 * Use the getURL() function to retrieve a MIME object or wrap a 
 * java.io.File object around a file of the local file system.
 * <br /><br />
 * Example:
 * <pre>var file1 = getURL("http://localhost:8080/static/image.gif");
 * var file2 = getURL("file:////home/snoopy/woodstock.jpg");
 * var file3 = new java.io.File("/home/snoopy/woodstock.jpg");
 * var mail = new Mail();
 * mail.addPart(file1);
 * mail.addPart(file2);
 * mail.addPart(file3);
 * &nbsp;
 * mail.setFrom("snoopy&#64;doghouse.com");
 * mail.setTo("woodstock&#64;birdcage.com");
 * mail.setSubject("Look at this!");
 * mail.addText("I took a photograph from you. Neat, isn't it? -Snoop");
 * mail.send();</pre>
 * 
 * @param {fileOrMimeObject} File or Mime object to attach to the email
 * @param {String} nameString as String, optional name of the attachment
 * @see global.getUrl
 * @see mimePart
 * @see java.io.File
 */
Mail.prototype.addPart = function(fileOrMimeObject, nameString) {};


/**
 * Appends a string to the body text of an e-mail message.
 * <br /><br />
 * Example:
 * <pre>var mail = new Mail();
 * mail.addText("Hello, World!");</pre>
 * 
 * @param {String} text as String, to be appended to the message body
 */
Mail.addText = function(text) {};


/**
 * Adds a recipient to the address list of an e-mail message.
 * <br /><br />
 * The emailString argument specifies the receipient's e-mail address. 
 * The optional nameString argument specifies the name of the recipient.
 * <br /><br />
 * Example:
 * <pre>var mail = new Mail();
 * mail.setTo("hop&#64;helma.at");
 * mail.addTo("hopdoc&#64;helma.at");
 * mail.addTo("tobi&#64;helma.at", "Tobi Schaefer");</pre>
 * 
 * @param {String} emailString as String, receipients email address
 * @param {String} nameString as String, optional receipients name
 * @see setTo
 */
Mail.prototype.addTo = function(emailString, nameString) {};


/**
 * Sends an e-mail message.
 * <br /><br />
 * This function sends the message created with the Mail 
 * object using the SMTP server as specified in either 
 * the app.properties or the server.properties file.
 * <br /><br />
 * Example:
 * <pre>var mail = new Mail();
 * mail.setTo("watching&#64;michi.tv", "michi");
 * mail.addCC("franzi&#64;home.at", "franzi");
 * mail.addBCC("monie&#64;home.at");
 * mail.setFrom("chef&#64;frischfleisch.at", "Hannes");
 * mail.setSubject("Registration Conformation");
 * mail.addText("Thanks for your Registration...");
 * mail.send();</pre>
 */
Mail.prototype.send = function() {};


/**
 * Sets the sender of an e-mail message.
 * <br /><br />
 * The emailString argument specifies the receipient's 
 * e-mail address. The optional nameString argument 
 * specifies the name of the recipient.
 * <br /><br />
 * Example:
 * <pre>var mail = new Mail();
 * mail.setFrom("tobi&#64;helma.at", "Tobi Schaefer");</pre>
 * 
 * @param {String} emailString as String, sender email address
 * @param {String} nameString as String, optional sender name
 */
Mail.prototype.setFrom = function(emailString, nameString) {};


/**
 * Sets the subject of an e-mail message.
 * <br /><br />
 * Example:
 * <pre>var mail = new Mail();
 * mail.setSubject("Hello, World!");</pre>
 * 
 * @param {String} subject as String, the email subject
 */
Mail.prototype.setSubject = function(subject) {};


/**
 * Sets the recipient of an e-mail message.
 * &nbsp;
 * The emailString argument specifies the receipient's 
 * e-mail address. The optional nameString argument 
 * specifies the name of the recipient.
 * <br /><br />
 * Example:
 * <pre>var mail = new Mail();
 * mail.setTo("hop&#64;helma.at");</pre>
 * 
 * @param {String} emailString as String, receipients email address
 * @param {String} nameString as String, optional receipients name
 * @see Mail.addTo
 */
Mail.prototype.setTo = function(emailString, nameString) {};
