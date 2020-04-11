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
 * $RCSfile: res.js,v $
 * $Author: zumbrunn $
 * $Revision: 8777 $
 * $Date: 2008-02-05 17:12:29 +0100 (Die, 05. Feb 2008) $
 */

/**
 * @fileoverview Default properties and methods of the res object.
 */


/**
 * This object is automatically instantiated as the res property 
 * of the global object (or global.res) and there is no constructor 
 * to instantiate further instances.
 * <br /><br />
 * The res object is a host object representing the response 
 * for the request that is currently handled by the scripting 
 * environment.
 * <br /><br />
 * For further details also see the JavaDocs for 
 * Packages.helma.framework.ResponseBean. Since that class is a 
 * JavaBean all of its get- and set-methods are also directly 
 * available as properties of this object.
 * 
 * @type ResponseBean
 * @see Packages.helma.framework.ResponseBean
 */
res = new Object();


/**
 * Specifies on a per-response base whether to allow client-side caching of the response or not.
 * <br /><br />
 * The default value is set to true but can be changed 
 * globally by passing caching = false to the initial 
 * Helma java call.
 * <br /><br />
 * Please refer to the section about servlet 
 * properties for more details.
 * <br /><br />
 * Example:
 * <pre>res.cache = true</pre>
 * 
 * @type Boolean
 */
res.cache = new Boolean();


/**
 * Sets the character encoding of the output.
 * <br /><br />
 * Sets Helma's character encoding according to the 
 * <a href=http://java.sun.com/products/jdk/1.2/docs/guide/internat/encoding.doc.html">
 * list of supported encodings</a>.
 * By default, Helma uses ISO-8859-1 (western) encoding. It is also possible to 
 * set the encoding server or application wide using the charset 
 * property in a properties file.
 * <br /><br />
 * Be careful when setting the charset of a document containing 
 * a FORM. The browser returns the user-input encoded in the 
 * same way as the document. So if you set for instance the 
 * charest to UTF-8 (and don't set it in the app.properties or 
 * server.properties) you'll get scrambled input because Helma 
 * assumes the input is encoded in the standard encoding 
 * (default is ISO-8859-1). So you have to convert the returned 
 * strings from UTF8 to ISO-8859-1 (e.g. var string = 
 * new java.lang.String(new java.lang.String((req.data.input ? 
 * req.data.input : "")).getBytes("ISO-8859-1"), "UTF-8");).
 * <br /><br />
 * Example:
 * <pre>res.charset = "UTF8";
 * res.write("&Auml; &Ouml; &Uuml; &auml; &ouml; &uuml; &szlig;");
 * // this is displayed if the brower's encoding is set to "Western"
 * &Atilde; &bdquo; &Atilde; &ndash; &Atilde; &oelig; &Atilde; &curren; &Atilde; &para; &Atilde; &frac14; &Atilde; &Yuml;</pre>
 * 
 * @type String
 */
res.charset = new String();


/**
 * Sets the content type of the HTTP response.
 * <br /><br />
 * The default value is "text/html".
 * <br /><br />
 * Example:
 * <pre>res.contentType='text/plain';
 * res.contentType='application/xhtml+xml';</pre>
 * 
 * @type String
 */
res.contentType = new String();


/**
 * Object providing space for custom output data.
 * <br /><br />
 * This object can be used to attach any property propertyName 
 * onto it to make it available in a skin via the response macro.
 * <br /><br />
 * Example:
 * <pre>File root/main.skin:
 * &lt;html&gt;
 * &lt;head&gt;
 * &lt;title&gt;&lt;% response.title %&gt;&lt;/title&gt;
 * &lt;/head&gt;
 * &lt;body&gt;
 * &lt;% response.body %&gt;
 * &lt;/body&gt;
 * &lt;/html&gt;
 * &nbsp;
 * File root/main.hac:
 * res.data.title = "Test";
 * res.data.body = "Hello, World!";
 * root.renderSkin("main");
 * &nbsp;
 * <i>&lt;html&gt;
 * &lt;head&gt;
 * &lt;title&gt;Test&lt;/title&gt;
 * &lt;/head&gt;
 * &lt;body&gt;
 * Hello, World!
 * &lt;/body&gt;
 * &lt;/html&gt;</i></pre> 
 * 
 * @type Object
 */
res.data = new Object();


/**
 * Property containing an internal Helma error message.
 * <br /><br />
 * This property contains a string describing an error if one should 
 * have occured. If no error was detected, res.error contains null.
 * <br /><br />
 * A good place for this function is a custom error page to display errors in a pretty layout.
 * <br /><br />
 * Example:
 * <pre>res.write(res.error);
 * <i>Runtime error Syntax error detected near line 1, column 24, after in string starting 
 * with: 'function main_action (arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10) {'...</i></pre>
 * 
 * @type String
 * @see res.exception
 * @see res.javaStack
 * @see res.scriptStack
 */
res.error = new String();


/**
 * Get or set the HTTP ETag for this response.
 * 
 * @type String
 */
res.etag = new String();


/**
 * Get the uncaught exception for the response, if any
 * 
 * @type String
 * @since 1.7
 * @see res.error
 * @see res.javaStack
 * @see res.scriptStack
 */
res.exception = new String();


/**
 * The res.handlers object is a container in the response object used to register 
 * macro handlers for the current request/response.
 * <br /><br />
 * Adding an object to res.handlers will make its macro functions and/or 
 * properties available as macros for skins.
 * <br /><br />
 * By default, res.handlers contains the objects in the request path, registered by 
 * their prototype names. For instance, res.handlers.root will always be 
 * a reference to the app's root object.
 * 
 * @type Object
 */
res.handlers = new Object();


/**
 * Get the Java stack trace of an uncaught exception, if any
 * 
 * @type String
 * @since 1.7
 * @see res.error
 * @see res.exception
 * @see res.scriptStack
 */
res.javaStack = new String();


/**
 * Sets the date the resource was retrieved by the remote client.
 * <br /><br />
 * By setting this property the remote client is told that the 
 * served resource has been modified at the given time. You then 
 * can check if the resource has changed given that the client 
 * includes this date in future requests for this resource.
 * <br /><br />
 * Example:
 * <pre>//in this example this.modificationDate is a property storing
 * //the time of the last changes to the current object
 * res.lastModified = this.modificationDate;</pre>
 * 
 * @type Date
 */
res.lastModified = new Date();


/**
 * Buffers a string for output.
 * <br /><br />
 * When this property is set its value can be retrieved either by 
 * res.message from within an action or a JavaScript file or by 
 * the response macro from within a skin file.
 * <br /><br />
 * Additionally, it will survive an HTTP redirect which can be necessary 
 * to display warnings or error messages to the user. The property's 
 * value will be reset to null not later than a redirected response was output.
 * <br /><br />
 * Note: Please be aware that both res.message and res.data.message corresponds 
 * to the response.message macro. However, res.message has higher priority if both 
 * values should be set.
 * <br /><br />
 * Example:
 * <pre>File main.skin:
 * &lt;% response.message %>
 * &nbsp;
 * File main.hac:
 * res.message = "Hello, World!";
 * res.data.message = "Test";
 * this.renderSkin("main");
 * &nbsp;
 * <i>Hello, World!</i></pre>
 * 
 * @type String
 */
res.message = new String();


/**
 * Object providing space for general purpose per request meta information.
 * <br /><br />
 * This object can be used to attach any property propertyName onto 
 * it to make it available within the scope a request.
 * <br /><br />
 * Example:
 * <pre>Inside an onRequest handler function:
 * &nbsp;&nbsp;res.meta.permissions = this.doSomething();
 * &nbsp;
 * Inside an action such as delete.hac:
 * if(res.meta.permissions | 7)
 * &nbsp;&nbsp;this.remove();</pre>
 * 
 * @type Object
 */
res.meta = new Object();


/**
 * Sets the realm for a HTTP-authentication challenge
 * <br /><br />
 * This defines the realm for a HTTP-authentication challenge. 
 * The realm is the name of a protected space. A server can have 
 * several realms, but resources within one realm should share 
 * the same authorisation - a valid authorisation should be valid 
 * as well for requesting another resource within the same realm.
 * <br /><br />
 * Example:
 * <pre>res.status = 401;
 * res.realm = "Helma protected area";</pre>
 * 
 * @type String
 */
res.realm = new String();


/**
 * Get the Javascript stack trace of an uncought exception, if any
 * 
 * @type String
 * @since 1.7
 * @see res.error
 * @see res.exception
 * @see res.javaStack
 */
res.scriptStack = new String();


/**
 * Provides direct access to the servlet response header, a
 * Java object of the class Packages.javax.servlet.http.HttpServletResponse
 * <br /><br />
 * Example:
 * <pre>res.servletResponse.setHeader( "Content-Disposition", 
 * &nbsp;&nbsp;( this.mimetype.startsWith( "image/" )? "" : "attachment;" ) +  
 * &nbsp;&nbsp;"filename=\"" + this.name + "\"" );</pre>
 * 
 * @type HttpServletResponse
 * @see Packages.javax.servlet.http.HttpServletResponse
 */
res.servletResponse = new Object();


/**
 * The res.skinpath can be set to an array that tells Helma where to 
 * look for skinsets for the current request/response.
 * <br /><br />
 * The elements of the array should be either HopObjects, 
 * which will be interpreted as database-resident skinsets, or strings, 
 * which will be interpreted as directory paths leading to file based 
 * skin sets.
 * <br /><br />
 * If hobj is a HopObject element in the res.skinpath array, then a skin 
 * for prototype proto named sname is expected to reside in a string 
 * property in hobj.proto.sname.skin.
 * <br /><br />
 * If path is a string, then the same skin will be looked up in a file 
 * called path/proto/sname.skin.
 * 
 * @type Array
 */
res.skinpath = new Array();


/**
 * Read/write property containing the response's HTTP-status code.
 * <br /><br />
 * This property defines the status code of the HTTP response.
 * Like any HTTP-Server Helma can send the client software additional information 
 * describing the response. For a list of all codes and their meaning see RFC 2616.
 * One of the common usages is to deny access and ask for authentication by responding 
 * with status code 401. The client software usually responds by asking the user 
 * for username and password.
 * The default value is 200 which is the code for "OK".
 * <br /><br />
 * Example:
 * <pre>res.status = 401;
 * //make sure to set res.realm as well when asking for http authentication
 * res.realm = "Helma protected area";</pre>
 * 
 * @type Number
 */
res.status = new Number();


/**
 * Aborts the current transaction by throwing an Error
 * 
 * @see res.stop
 * @see res.rollback
 */
res.abort = function() {};


/**
 * Proxy to res.servletResponse.addHeader()
 * 
 * @param {String} name as String, the header name
 * @param {String} value as String, the header value
 * @see res.servletResponse
 * @see res.addDateHeader
 * @see res.setHeader
 * @see res.setDateHeader

 */
res.addHeader = function(name, value) {};


/**
 * Proxy to res.servletResponse.addDateHeader()
 * 
 * @param {String} name as String, the header name
 * @param {Date} value as Date, the header value
 * @see res.servletResponse
 * @see res.addHeader
 * @see res.setHeader
 * @see res.setDateHeader
 */
res.addDateHeader = function(name, value) {};


/**
 * Commits the current transaction, writing all changed objects to the database, and starts a new transaction.
 * <br /><br />
 * Usually, Helma automatically wraps each request into a 
 * database transaction. Use res.commit() if you have to 
 * modify a huge ammount of HopObjects within one request and 
 * want to partition the transaction into several smaller ones.
 * <br /><br />
 * Example:
 * <pre>for (var i in collection1)
 * &nbsp;&nbsp;collection1.get(i).counter += 1;
 * // commit changes to elements of collection1 to db
 * res.commit();
 * for (var i in collection2)
 * &nbsp;&nbsp;collection2.get(i).counter -= 1;
 * // changes to elements of collection2 are committed 
 * // automatically when the request terminates</pre>
 * 
 * @see res.rollback
 */
res.commit = function() {};


/**
 * Append any value to the output of the current page. 
 * <br /><br />
 * All values written with res.debug() will appear as separate 
 * items at the bottom of the page. This assumes that the 
 * content type of the current request is text/html. Since items 
 * are simply appended to the full page, res.debug() will most likely break 
 * the well-fomedness of your page, but it is usually displayed properly.
 * <br /><br />
 * Transforms the passed argument to a string (only if necessary) and 
 * appends the result to the response debug buffer.
 * <br /><br />
 * Example:
 * <pre>var now = new Date();
 * res.debug("current time: " + now);
 * &nbsp;
 * <i>current time: Tue May 20 18:59:02 CEST 2003</i></pre>
 * 
 * @param {String} text as String
 */
res.debug = function(text) {};


/**
 * Add an item to this response's dependencies. 
 * <br /><br />
 * If no dependency has changed between requests, 
 * an HTTP not-modified response will be generated.
 * 
 * @param {String} what as string, an item this response depends on
 */
res.dependsOn = function(what) {};


/**
 * Digest this response's dependencies to conditionally create 
 * a HTTP not-modified response.
 */
res.digest = function() {};


/**
 * Writes a string as smooth HTML to the output buffer.
 * <br /><br />
 * Encodes a string by replacing linebreaks with &lt;br /&gt; tags 
 * and diacritical characters as well as HTML tags with their 
 * equivalent HTML entities and immediately outputs it.
 * <br /><br />
 * Example:
 * <pre>res.encode("&lt;b&gt;Bananer v&auml;xer\n minsann inte p&aring; tr&auml;d.&lt;/b&gt;");
 * <i>&amp;lt;b&amp;gt;Bananer v&amp;auml;xer
 * &amp;lt;br /&amp;gt; &amp;lt;br&amp;gt; minsann inte p&amp;aring; tr&amp;auml;d.&amp;lt;/b&amp;gt;</i></pre>
 * 
 * @param {String} textToEncode as String
 * @see res.encodeForm
 * @see res.encodeXml
 */
res.encode = function(textToEncode) {};


/**
 * Writes a string to the output buffer, leaving linebreaks untouched.
 * <br /><br />
 * Performs the following string manipulations:
 * <ul>
 * <li>Unlike encode, leaves linebreaks untouched. 
 *     This is what you usually want to do for encoding form 
 *     content (esp. with text input values).</li>
 * <li>All special characters (i.e. non ASCII) are being 
 *     replaced with their equivalent HTML entity.</li>
 * <li>Existing markup tags are being encoded.</li>
 * </ul>
 * <br /><br />
 * Example:
 * <pre>var str = res.encodeForm("&lt;b&gt;Bananer v&auml;xer\n minsann inte p&aring; tr&auml;d.&lt;/b&gt;");
 * &nbsp;
 * <i>&amp;lt;b&amp;gt;Bananer v&amp;auml;xer
 * minsann inte p&amp;aring; tr&amp;auml;d.&amp;lt;/b&amp;gt;</i></pre>
 * 
 * @param {String} textToEncode as String
 * @see res.encode
 * @see res.encodeXml
 */
res.encodeForm = function(textToEncode) {};


/**
 * Writes a string to the output buffer, replacing some characters with their equivalent XML entities.
 * <br /><br />
 * This function substitutes the characters '<', '>', '&', as well as single and double quotes with 
 * their predefined XML 1.0 entities. Additionally, some characters that are illegal in 
 * XML documents are silently dropped.
 * <br /><br />
 * encodeXml() is usually used to encode text for safe inclusion in parsed character data sections 
 * of XML documents.
 * <br /><br />
 * Example:
 * <pre>res.encodeXml("&lt;title&gt;Sm&oslash;rebr&amp;oslash;d&lt;/title&gt;");
 * <i>&amp;lt;title&amp;gt;Sm&oslash;rebr&amp;amp;oslash;d&amp;lt;/title&amp;gt;</i></pre>
 * 
 * @param {String} textToEncode as String
 * @see res.encode
 * @see res.encodeForm
 */
res.encodeXml = function(textToEncode) {};


/**
 * Writes a string as smooth HTML to the output buffer.
 * <br /><br />
 * This function outputs a string after substituting special characters 
 * in a string with their equivalent HTML entities but keeping markup tags 
 * as is and also tries to avoid <br /> tags where ever possible (e.g. in table structures).
 * <br /><br />
 * Example:
 * <pre>res.format("&lt;table&gt;\n&lt;tr&gt;&lt;td&gt;Bananer v&auml;xer\n minsann 
 * inte p&aring; tr&auml;d.&lt;/td&gt;&lt;/tr&gt;\n&lt;/table&gt;");
 * <i>&lt;table&gt;
 * &lt;tr&gt;&lt;td&gt;Bananer v&amp;auml;xer
 * &lt;br /&gt;
 * minsann inte p&amp;aring; tr&amp;auml;d.&lt;/td&gt;&lt;/tr&gt;
 * &lt;/table&gt;</i></pre>
 * 
 * @param {String} textToFormat as String
 * @see res.encode
 */
res.format = function(textToFormat) {};


/**
 * Serves a static file from the application's protectedStatic directory.
 * <br /><br />
 * Used to serve static files from the application's 
 * protectedStatic directory. This "protected" static 
 * directory needs to be set by defining an 
 * appname.protectedStatic property in the apps.properties 
 * file. The contents of that directory are not made 
 * directly accessible through the web, only through the 
 * use of this res.forward() method.
 * 
 * @param {String} path as String, relative to this app's protectedStatic directory
 */
res.forward = function(path) {};


/**
 * Provides access to the contents of the current repsonse buffer.
 * <br /><br />
 * Useful for example when parsing/modifying the response after
 * the request has been handled but before the response is returned.
 * 
 * @return String
 * @type String
 * @see HopObject.prototype.onResponse
 */
res.getBuffer = function() {};


/**
 * Pops a string buffer from the response object and returns its string value.
 * <br /><br />
 * This function "pops" a string buffer from the response object. 
 * The returned string contains all output written to the response 
 * since the last call of res.push().
 * <br /><br />
 * Note that more than one string buffers can be pushed on the 
 * response object, and output always goes to the last one.
 * <br /><br />
 * Example:
 * <pre>res.push();
 * res.write("foo");
 * res.encode('.');
 * res.write("bar");
 * var str = res.pop()
 * // str is "foo.bar"</pre>
 * 
 * @return String
 * @type String
 * @see res.push
 * @see res.pushBuffer
 * @see res.popBuffer
 */
res.pop = function() {};


/**
 * Pops the current response buffer without converting it to a string.
 * <br /><br />
 * This function "pops" the current StringBuffer from the 
 * response object and returns it as a Java StringBuffer object. 
 * 
 * @return StringBuffer
 * @type StringBuffer
 * @see res.pop
 * @see res.pushBuffer
 * @see res.push
 */
res.popBuffer = function() {};


/**
 * Pushes a new string buffer on the response object.
 * <br /><br />
 * This function "pushes" a fresh string buffer on the response 
 * object to which all response output will be redirected 
 * until res.pop() is called to retrieve the generated string.
 * <br /><br />
 * Note that more than one string buffers can be pushed on the 
 * response object, and output always goes to the last one.
 * <br /><br />
 * Example:
 * <pre>res.push();
 * res.write("foo");
 * res.encode('.');
 * res.write("bar");
 * var str = res.pop();
 * // str is "foo.bar"</pre>
 * 
 * @see res.pop
 * @see res.pushBuffer
 * @see res.popBuffer
 */
res.push = function() {};


/**
 * Pushes a Java StringBuffer object on the reponse object.
 * <br /><br />
 * This function "pushes" the provided StringBuffer to the 
 * response object. All further writes will be redirected 
 * to this buffer.
 * 
 * @param {StringBuffer} strBuffer as StringBuffer Java object, the StringBuffer to be pushed to the reponse object
 * @see res.pop
 * @see res.push
 * @see res.popBuffer
 */
res.pushBuffer = function(strBuffer) {};


/**
 * Sends an HTTP redirect message to the client.
 * <br /><br />
 * Note: JavaScript code following this command will be ignored.
 * <br /><br />
 * Example:
 * <pre>res.redirect(this.href());</pre>
 */
res.redirect = function() {};


/**
 * Resets the current output buffer.
 * <br /><br />
 * Example:
 * <pre>res.write("Test");
 * // changing my mind (e.g. an error occured)
 * res.reset();
 * res.write("Hello, World!");
 * &nbsp;
 * <i>Hello, World!</i></pre>
 */
res.reset = function() {};


/**
 * Resets the response buffer, clearing all content 
 * previously written to it.
 */
res.resetBuffer = function() {};


/**
 * Rollback the current transaction and start a new one.
 * 
 * @throws Exception thrown if rollback fails
 * @see res.commit
 * @see res.abort
 * @see res.stop
 */
res.rollback = function() {};


/**
 * Sets a cookie to be sent to the client machine.
 * <br /><br />
 * All arguments after nameString and valueString are optional.
 * <br /><br />
 * daysNumber specifies the number of days until the cookie expires.
 * If set to 0, the cookie will be deleted immediately.
 * If the argument is not specified or set to a negative value, the cookie 
 * will be discarded is at the end of the browser session.
 * <br /><br />
 * pathString specifies the path on which to set the cookie (defaults to /)
 * <br /><br />
 * domainString specifies the domain on which to set the cookie (defaults to unset)
 * <br /><br />
 * Example:
 * <pre>res.setCookie("username", "michi");
 * res.setCookie("password", "strenggeheim", 10, "/mypath", ".mydomain.org");</pre>
 * 
 * @param {String} key as String
 * @param {String} value as String
 * @param {Number} days as Number
 * @param {String} path as String
 * @param {String} domain as String
 */
res.setCookie = function(key,value,days,path,domain) {};


/**
 * Proxy to res.servletResponse.setHeader()
 * 
 * @param {String} name as String, the header name
 * @param {String} value as String, the header value
 * @see res.servletResponse
 * @see res.addHeader
 * @see res.addDateHeader
 * @see res.setDateHeader
 */
res.setHeader = function(name, value) {};


/**
 * Proxy to res.servletResponse.setDateHeader()
 * 
 * @param {String} name as String, the header name
 * @param {Date} value as Date, the header value
 * @see res.servletResponse
 * @see res.addHeader
 * @see res.addDateHeader
 * @see res.setHeader
 */
res.setDateHeader = function(name, value) {};


/**
 * Stops the execution of the request and sends the content of the reponse buffer.
 * <br /><br />
 * Note that this method is intended for use in the context of an 
 * onRequest handler and not for use inside of actions, which would 
 * likely lead to bad coding practices.
 * <br /><br />
 * Example:
 * <pre>function onRequest() {
 * &nbsp;&nbsp;if (fooIsCached()) {
 * &nbsp;&nbsp;&nbsp;&nbsp;res.write(fooFromCache());
 * &nbsp;&nbsp;&nbsp;&nbsp;res.stop();
 * &nbsp;&nbsp;}
 * }</pre>
 * 
 * @see res.abort
 * @see res.rollback
 */
res.stop = function() {};


/**
 * Writes a string to the output buffer.
 * <br /><br />
 * Transforms the passed argument to a string (only if necessary) 
 * and appends the result to the response string buffer. Throws an error 
 * if no argument or an undefined argument is passed.
 * <br /><br />
 * Example:
 * <pre>var now = new Date();
 * res.write("current time: " + now);
 * &nbsp;
 * <i>current time: Thu Feb 15 23:34:29 GMT+01:00 2001</i></pre>
 * 
 * @param {String} text as String
 */
res.write = function(text) {};


/**
 * Writes binary data to the output buffer.
 * <br /><br />
 * This function takes one argument, which must be a 
 * Java byte array. Useful when handling binary data 
 * retrieved via http file upload.
 * <br /><br />
 * Example:
 * <pre>var upload = req.data.fileUpload;
 * res.writeBinary(upload.getContent());</pre>
 * 
 * @param {Packages.java.ByteArray} data as Packages.java.ByteArray
 */
res.writeBinary = function(data) {};


/**
 * Writes a string to the output buffer, adding a linebreak.
 * <br /><br />
 * Transforms the passed argument to a string 
 * (only if necessary) and appends the result together 
 * with a trailing linebreak to the response string 
 * buffer. Throws an error if no argument or an undefined 
 * argument is passed.
 * <br /><br />
 * Example:
 * <pre>var now = new Date();
 * res.writeln("current time:");
 * res.writeln(now);
 * &nbsp;
 * <i>current time:<br />
 * Thu Feb 15 23:34:29 GMT+01:00 2001</i></pre>
 * 
 * @param {String} text as String
 */
res.writeln = function(text) {};


/**
 * Unset a previously set HTTP cookie, causing it to be 
 * discarded by the HTTP client immediately.
 *
 * @param {String} key as String, the name of the cookie to be discarded
 */
res.unsetCookie = function(key) {};

