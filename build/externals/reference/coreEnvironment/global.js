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
 * $RCSfile: global.js,v $
 * $Author: hannes $
 * $Revision: 9988 $
 * $Date: 2009-11-10 09:51:27 +0100 (Die, 10. Nov 2009) $
 */

/**
 * @fileoverview Objects and functions made available in the 
 * global scope of the Helma environment.
 */

/**
 * Built-in reference to the global object. 
 * <br /><br />
 * Useful as a way to access the global scope from within 
 * the local scope of a function.
 * <br /><br />
 * The global object is based on a library scope, compiled from 
 * the application's code repositories and is updated automatically 
 * whenever any code repositories are modified, without the need 
 * to restart/reset a running application. Each request receives
 * a fresh global scope based on the application's library scope.
 * <br /><br />
 * Modifications to global variables that are defined at 
 * compile-time are synchronized between requests, while global  
 * variables newly created at runtime are cleared when the 
 * current request scope is purged. For global variables created
 * at runtime to survive between requests, they need to be 
 * declared once using defineLibraryScope().
 * 
 * @type Object
 * @memberof global
 * @see global.defineLibraryScope
 */
global = new Object();


/**
 * Library object for the HelmaLib javascript library.
 * <br /><br />
 * HelmaLib is organized into two groups of modules:
 * <ul>
 * <li>modules/core which contains extensions to core 
 * JavaScript types such as Object, Array, or Date.</li>
 * <li>modules/helma which provides new functionality to 
 * JavaScript, usually by wrapping a Java library.</li>
 * </ul>
 * <br /><br />
 * To use a HelmaLib module in your Helma application, you 
 * need to add it to the app's repositories. The simplest 
 * way to do so is by using the app.addRepository() method:
 * <pre>app.addRepository("modules/helma/Search.js");</pre>
 * <br /><br />
 * If you are looking for more Helma libraries, be sure 
 * to check out the <a href="https://dev.orf.at/trac/jala/wiki/">Jala project</a>!
 * 
 * @type Object
 * @see Array
 * @see Date
 * @see Number
 * @see Object
 * @see String
 * @see helma.Aspects
 * @see helma.Chart
 * @see helma.Color
 * @see helma.Database
 * @see helma.File
 * @see helma.Ftp
 * @see helma.Group
 * @see helma.Html
 * @see helma.Http
 * @see helma.Image
 * @see helma.Mail
 * @see helma.Search
 * @see helma.Skin
 * @see helma.Ssh
 * @see helma.Url
 * @see helma.Zip
 */
helma = new Object();


/**
 * Represents the root of the object model hierarchy.
 * <br /><br />
 * Each application has only one root object. This single
 * instance of the root object inherits its properties from 
 * the Root prototype, which itself inherits from the 
 * HopObject prototype. 
 * <br /><br />
 * The root object serves as the starting point against 
 * which the URI path of incoming requests are resolved.
 * 
 * @type HopObject
 * @see HopObject
 */
root = new HopObject();


/**
 * Accessing objects in the URI path.
 * <br /><br />
 * The objects in the URI request path are accessible 
 * as array members of the global path object as well 
 * as named properties of path via their prototype name.
 * <br /><br />
 * When Helma receives an HTTP request e.g. from a browser, 
 * it maps the URI path of the URL to HopObjects. This way, 
 * the path is interpreted as an hierarchy of HopObjects.
 * <br /><br />
 * For instance, if an object in the URI path has a prototype called 
 * "story", it will be accessible as path["story"] or path.story.
 * <br /><br />
 * <pre>for (var i=0; i &lt; path.length; i++)
 * &nbsp;&nbsp;res.writeln(path[i]);
 * &nbsp;
 * <i>HopObject file
 * HopObject document
 * HopObject story
 * HopObject note</i>
 * &nbsp;
 * var obj = path.story;
 * res.write(obj);
 * &nbsp;
 * <i>HopObject story</i></pre>
 * The path object behaves similar to a Javascript 
 * array, but it is actually an instance of the 
 * Packages.helma.scripting.rhino.PathWrapper Java class.
 * 
 * @type PathWrapper
 * @see Packages.helma.scripting.rhino.PathWrapper
 */
path = new Array();


/**
 * This object is automatically instantiated as the app property 
 * of the global object (or global.app) and there is no constructor 
 * to instantiate further instances.
 * <br /><br />
 * The app object is a host object representing the application 
 * for which the current scripting environment is provided.
 * <br /><br />
 * For further details also see the JavaDocs for 
 * Packages.helma.framework.core.ApplicationBean. Since that class 
 * is a JavaBean all of its get- and set-methods are also directly 
 * available as properties of this object.
 * 
 * @type app
 * @see app
 */
app = new Object();


/**
 * This object is automatically instantiated as the req property 
 * of the global object (or global.req) and there is no constructor 
 * to instantiate further instances.
 * <br /><br />
 * The req object is a host object representing the request  
 * that is currently handled by the scripting environment.
 * <p>For further details also see the JavaDocs for 
 * Packages.helma.framework.RequestBean. Since that class is a 
 * JavaBean all of its get- and set-methods are also directly 
 * available as properties of this object.
 * 
 * @type req
 * @see req
 */
req = new Object();


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
 * @type res
 * @see res
 */
res = new Object();


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
 * Packages.helma.framework.core.SessionBean. Since that class 
 * is a JavaBean all get- and set-methods are also directly 
 * available as properties of that object.
 * 
 * @type session
 * @see session
 */
session = new Object();


/**
 * Constructor for HopObject objects, providing the 
 * building blocks of the Helma framework.
 * <br /><br />
 * Extends the standard JavaScript object with 
 * Helma-specific properties and functions. The HopObject is
 * the basic building block of a Helma application. The 
 * website root object as well as custom types defined by 
 * the application inherit from the HopObject prototype.
 * <br /><br />
 * HopObjects can be given special Helma specific 
 * properties, such as "collections" that can be configured 
 * to map to relational databases, and will make such data 
 * available when rendering "skins".
 * <br /><br />
 * HopObjects can be in transient state or are persistently 
 * mapped on a database. HopObjects that are directly or 
 * indirectly attached to the application's root object are 
 * automatically persisted using the built-in XML database, if 
 * they are not otherwise mapped to a relational database. 
 * see JavaDocs for helma.scripting.rhino.HopObject
 * 
 * @constructor
 * @type HopObject
 * @see HopObject
 */
HopObject = new Function();


/**
 * Constructor for File objects, providing read and write access to the file system.
 * <br /><br />
 * Example:
 * <pre>var fileOrDir = new File('static/test.txt');</pre>
 * 
 * @param {String} filepath as String
 * @constructor
 * @deprecated use helma.File instead
 * @type File
 * @see File
 */
File = new Function(filepath);


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
 * @type FtpClient
 * @see helma.Ftp
 * @see FtpClient
 */
FtpClient = new Function(server);


/**
 * Helma's built-in image object allows you to read, manipulate, and save images.
 * <br /><br />
 * An image object is created using the Image() constructor.
 * <br /><br />
 * Example:
 * <pre>var img = new Image("http://helma.org/image.gif");</pre>
 * 
 * @param {String} url as String
 * @constructor
 * @type Image
 * @see Image
 */
Image = new Function(url);


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
 * @type Mail
 * @see helma.Mail
 * @see Mail
 */
Mail = new Function();


/**
 * Prevents the specified property from being enumerated.
 * <br /><br />
 * Useful for example in order to extend the Object and Array 
 * prototypes without breaking for/in loops, since the added 
 * methods would otherwise get enumerated together with  
 * instance properties.
 * 
 * @param {String} name as String, the name of the property that should not be enumerable
 */
Object.prototype.dontEnum = function(name) {};


/**
 * Constructor for Remote objects, implementing an XML-RPC client.
 * <br /><br />
 * XML-RPC is a simple framework to enable one machine to 
 * execute remote procedures on another using HTTP and XML.
 * <br /><br />
 * In Helma such calls are performed using a Remote object. 
 * Remote objects are created with the URL of the XML-RPC 
 * service. Functions of the remote XML-RPC service then 
 * can be called just like local functions.
 * <br /><br />
 * To compensate for the missing exception handling, 
 * Remote objects return result wrappers which contain 
 * either the result of the remote function, or a 
 * description of the error if one occurred.
 * <br /><br />
 * Example:
 * <pre>var xr = new Remote("http://helma.domain.tld:5056/");
 * var msg1 = xr.helmaorg.getXmlRpcMessage();
 * if (msg1.error)
 * &nbsp;&nbsp;res.write(msg1.error);
 * else
 * &nbsp;&nbsp;res.write(msg1.result);
 * &nbsp;
 * <i>Hello, Xml-Rpc World!</i>
 * &nbsp;
 * var msg2 = xr.hotelGuide.hotels.grandimperial.getXmlRpcMessage();
 * if (!msg2.error)
 * &nbsp;&nbsp;res.write(msg2.result);
 * &nbsp;
 * <i>Welcome to the Grand Imperial Hotel, Vienna!</i>
 * &nbsp;
 * var msg3 = xr.kolin.document.comments["23"].getXmlRpcMessage();
 * if (!msg3.error)
 * &nbsp;&nbsp;res.write(msg3.result);
 * &nbsp;
 * <i>Here you can write your comments.</i>
 * &nbsp;
 * var xr = new Remote("http://betty.userland.com/RPC2");
 * var state = xr.examples.getStateName(23);
 * if (!state.error)
 * &nbsp;&nbsp;res.write(state.result);
 * &nbsp;
 * <i>Minnesota</i></pre>
 * 
 * @param {String} server as String
 * @constructor
 * @see Packages.helma.scripting.rhino.extensions.XmlRpcObject
 * @type XmlRpcObject
 */
Remote = new Function(server);


/**
 * The built-in User prototype.
 * <br /><br />
 * Normally, the User constructor is never called directly and 
 * the app.registerUser method is used instead to create new
 * users.
 * 
 * @see User
 * @constructor
 * @type HopObject
 */
User = new Function();


/**
 * The Xml object provides easy means to convert XML to 
 * HopObject and HopObjects to XML.
 * 
 * @type Xml
 * @see Xml
 */
Xml = new Object();


/**
 * Authenticates a user against a standard Unix password file. 
 * <br /><br />
 * Returns true if the provided credentials match an according 
 * entry in the files  [AppDir]/passwd or  [HelmaHome]/passwd. 
 * The stored passwords in these files must be either encrypted 
 * with the unix crypt algorithm, or with the MD5 algorithm. 
 * The Apache web server provides the utility tool 'htpasswd' 
 * to generate such password files.
 * <br /><br />
 * Example:
 * <pre>var login = authenticate("user", "pass");
 * if (login)
 * &nbsp;&nbsp;&nbsp;res.write("Welcome back!");
 * else
 * &nbsp;&nbsp;&nbsp;res.write("Oops, please try again...");
 * &nbsp;
 * <i>Welcome back!</i></pre>
 * 
 * @param {String} username as String
 * @param {String} password as String
 * @return Boolean true or false depending on whether 
 *         the authentication was successful
 * @type Boolean
 */
authenticate = function(username,password) {};


/**
 * Creates a Skin object from the passed String.
 * <br /><br />
 * The returned object can be passed to the global functions 
 * renderSkin, resp. renderSkinAsString.
 * <br /><br />
 * Skins can also be defined by text files using a *.skin 
 * suffix, placed in an application's code repository. The 
 * createSkin method provides the ability to create skins 
 * dynamically as an alternative.
 * <br /><br />
 * Example:
 * <pre>var str = "Hello, &lt;% response.body %&gt;!";
 * var skin = createSkin(str);
 * res.data.body = "World";
 * renderSkin(skin);
 * &nbsp;
 * <i>Hello, World!</i></pre>
 * 
 * @param {String} skin as String
 * @return Skin generated from the skin string
 * @type Skin
 * @see #Skin
 * @see #renderSkin
 * @see #renderSkinAsString
 */
createSkin = function(skin) {};


/**
 * Declares a new global variable to not be cleared between requests.
 * 
 * @param {String} namespace as String, the name of the protected namespace
 * @see #global
 */
defineLibraryScope = function(namespace) {};


/**
 * Deserialize a JavaScript object that was previously serialized to a file.
 * 
 * @param {String} filename as String, the file to deserialize the object from
 * @return Object the deserialized object
 * @type Object
 */
deserialize = function(filename) {};


/**
 * Encodes a string for HTML output and inserts linebreak tags.
 * <br /><br />
 * Performs the following string manipulations:
 * <ul>
 * <li>All line breaks (i.e. carriage returns and line feeds) 
 *     are replaced with &lt;br /> tags.</li>
 * <li>All special characters (i.e. non ASCII) are being 
 *     replaced with their equivalent HTML entity.</li>
 * <li>Existing markup tags are being encoded.</li>
 * </ul>
 * <br /><br />
 * Example:
 * <pre>var str = encode("&lt;b&gt;Bananer v&auml;xer\n minsann inte p&aring; tr&auml;d.&lt;b&gt;");
 * res.write(str);
 * &nbsp;
 * <i>&amp;lt;b&amp;gt;Bananer v&amp;auml;xer
 * &lt;br /&gt; &amp;lt;br&amp;gt; minsann inte p&amp;aring; tr&amp;auml;d.&amp;lt;/b&amp;gt;</i></pre>
 * 
 * @param {String} text as String
 * @return String the modified string
 * @type String
 * @see #encodeForm
 * @see #encodeXml
 * @see #format
 * @see #formatParagraphs
 */
encode = function(text) {};


/**
 * Encodes a string for HTML output, leaving linebreaks untouched.
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
 * <pre>var str = encodeForm("&lt;b&gt;Bananer v&auml;xer\n minsann inte p&aring; tr&auml;d.&lt;/b&gt;");
 * res.write(str);
 * &nbsp;
 * <i>&amp;lt;b&amp;gt;Bananer v&amp;auml;xer
 * minsann inte p&amp;aring; tr&amp;auml;d.&amp;lt;/b&amp;gt;</i></pre>
 * 
 * @param {String} text as String
 * @return String the modified string
 * @type String
 * @see #encode
 * @see #encodeXml
 * @see #format
 * @see #formatParagraphs
 */
encodeForm = function(text) {};


/**
 * Encodes a string for XML output.
 * <br /><br />
 * Performs the following string manipulations:
 * <ul>
 * <li>All special characters (i.e. non ASCII) are being 
 *     replaced with their equivalent HTML entity.</li>
 * <li>Existing tags, single and double quotes, as well as 
 *     ampersands are being encoded.</li>
 * <li>Some invalid XML characters below '0x20' are removed</li>
 * </ul>
 * <br /><br />
 * Example:
 * <pre>var str = encodeXml("&lt;title&gt;Sm&oslash;rebr&oslash;d&lt;/title&gt;");
 * res.write(str);
 * &nbsp;
 * <i>&amp;lt;title&amp;gt;Sm&oslash;rebr&amp;amp;oslash:d&amp;lt;/title&amp;gt;</i></pre>
 * 
 * @param {String} text as String
 * @return String the modified string
 * @type String
 * @see #encode
 * @see #encodeForm
 * @see #format
 * @see #formatParagraphs
 */
encodeXml = function(text) {};


/**
 * Encodes a string for HTML output, leaving existing markup tags untouched.
 * <br /><br />
 * Performs the following string manipulations:
 * <ul>
 * <li>All line breaks (i.e. carriage returns and line feeds) are
 *     replaced with &lt;br /> tags, with the exception of line breaks
 *     that follow certain block tags (e.g. table, div, h1, ..).</li>
 * <li>All special characters (i.e. non ASCII) are being replaced 
 *     with their equivalent HTML entity.</li>
 * </ul>
 * <br /><br />
 * Example:
 * <pre>var str = format("&lt;b&gt;Bananer v&auml;xer\n minsann inte p&aring; tr&auml;d.&lt;/b&gt;");
 * res.write(str);
 * &nbsp;
 * <i>&lt;b&gt;Bananer v&amp;auml;xer
 * &lt;br /&gt; minsann inte p&amp;aring; tr&amp;auml;d.&lt;/b&gt;</i></pre>
 * 
 * @param {String} text as String
 * @return String the modified string
 * @type String
 * @see #encode
 * @see #encodeForm
 * @see #encodeXml
 * @see #formatParagraphs
 */
format = function(text) {};


/**
 * Encodes a string for HTML output, inserting paragraph tags.
 * <br /><br />
 * Performs the following string manipulations:
 * <ul>
 * <li>Empty lines (double line breaks) are considered to indicate a 
 *     paragraph, and are surrounded with &lt;p> tags.</li>
 * <li>All single line breaks (i.e. carriage returns and line feeds) are
 *     replaced with &lt;br /> tags, with the exception of line breaks
 *     that follow certain block tags (e.g. table, div, h1, ..).</li>
 * <li>All special characters (i.e. non ASCII) are being replaced 
 *     with their equivalent HTML entity.</li>
 * </ul>
 * <br /><br />
 * Example:
 * <pre>var str = format("Sm&oslash;rebr&oslash;d:\n\n&lt;b&gt;Bananer v&auml;xer\n minsann inte p&aring; tr&auml;d.&lt;/b&gt;");
 * res.write(str);
 * &nbsp;
 * <i>&lt;p&gt;Sm&amp;oslash;rebr&amp;oslash;d:&lt;/p&gt;
 * &nbsp;
 * &lt;p&gt;&lt;b&gt;Bananer v&amp;auml;xer
 * &lt;br /&gt; minsann inte p&amp;aring; tr&amp;auml;d.&lt;/b&gt;&lt;/p&gt;</i></pre>
 * 
 * @param {String} text as String
 * @return String the modified string
 * @type String
 * @see #encode
 * @see #encodeForm
 * @see #encodeXml
 * @see #format
 */
formatParagraphs = function(text) {};


/**
 * Connects to a relational database, and returns a DatabaseObject 
 * representing that connection. 
 * <br /><br />
 * The Direct DB interface allows the developer to directly access 
 * relational databases defined in the db.properties file without 
 * going through the Helma object model layer.
 * <br /><br />
 * The passed string must match one of the data sources being defined 
 * in [AppDir]/db.properties, or an error will be thrown. 
 * &nbsp;
 * Also see the reference on the DatabaseObject and the documentation 
 * for the methods of the returned dbConnection and dbIterator objects.
 * <br /><br />
 * Example:
 * <pre>var dbConnection = getDBConnection("db_source_name");
 * &nbsp;
 * var dbRowset = dbConnection.executeRetrieval("select title from dummy");
 * while (dbRowset.next())
 * &nbsp;&nbsp;res.writeln(dbRowset.getColumnItem("title"));
 * &nbsp;
 * var deletedRows = dbConnection.executeCommand("delete from foobar");
 * if(deletedRows){
 * &nbsp;&nbsp;res.writeln(deletedRows + " rows successfully deleted");
 * }</pre>
 * <br /><br />
 * See the documentation of the databaseObject for a list of the 
 * available methods and properties of the returned object.
 * 
 * @param {String} datasource name as String
 * @return Packages.helma.scripting.rhino.extensions.DatabaseObject - a reference to the created db connection
 * @type DatabaseObject
 * @see Packages.helma.scripting.rhino.extensions.DatabaseObject
 * @see DatabaseObject
 * @see DatabaseObject.RowSet
 * @see helma.Database
 * @deprecated use helma.Database instead
 */
getDBConnection = function(datasource) {};


/**
 * Parses an HTML string to an XML DOM tree.
 * <br /><br />
 * Tries to parse a string to a XML DOM tree using Xerces' HTML 
 * parser (nekohtml). The argument must be either a URL, a piece 
 * of XML, an InputStream or a Reader. Returns an instance of 
 * org.apache.html.dom.HTMLDocumentImpl.
 * <br /><br />
 * See the JavaDocs for that class for further details.<br />
 * FIXME: Links to other recommended HTML parsers
 * 
 * @param {String} source as String, HTML formatted
 * @return Packages.org.apache.html.dom.HTMLDocumentImpl XML DOM tree object
 * @type HTMLDocumentImpl
 * @see Packages.org.apache.html.dom.HTMLDocumentImpl
 * @see #getXmlDocument
 */
getHtmlDocument = function(source) {};


/**
 * Looks up a property that was set in app.properties or server.properties
 * &nbsp;
 * Returns any property defined in  [AppDir]/app.properties, 
 * resp. [HelmaHome]/server.properties that matches the passed 
 * property name. This lookup is case-insensitive. Through the 
 * optional second parameter it is possible to define a default 
 * value that is being returned, in case the property has not 
 * been set.
 * 
 * @param {String} property as String, the name of the property to look up
 * @param {String} defaultvalue as String, optional default/fallback value
 * @return String with the resulting value for the checked property
 * @type String
 */
getProperty = function(property,defaultvalue) {};


/**
 * Retrieves a file/document from the passed URL as a MimePart 
 * Object, and therefore functions as a minimalist version of 
 * a HttpClient. The optional second parameter can either 
 * contain a (last-modified) date object, or an eTag string, 
 * which will be passed along with the Http request to the 
 * specified URL.
 * Also see the reference on the MimePart Object
 * 
 * @param {String} url as String
 * @param {String} etagOrDate either the etag as String or a Date object
 * @param {Number} timeout optional request timeout in milliseconds
 * @return Packages.helma.util.MimePart MimePart object of received Http response
 * @type MimePart
 * @see Packages.helma.util.MimePart
 * @see mimePart
 */
getURL = function(url,etagOrDate, timeout) {};


/**
 * Parses an XML string to an XML DOM tree.
 * <br /><br />
 * Tries to parse a string to a XML DOM tree using the 
 * Crimson' Parser. The argument must be either a URL, a 
 * piece of XML, an InputStream or a Reader. Returns an 
 * instance of org.apache.crimson.tree.XmlDocument.
 * <br /><br />
 * See the JavaDocs for that class for further details.<br />
 * FIXME: Link to JavaDocs and other recommended XML parsers
 * 
 * @param {String} source as String, using XML syntax
 * @return Packages.org.apache.crimson.tree.XmlDocument XML DOM tree object
 * @type XmlDocument
 * @see #getHtmlDocument
 */
getXmlDocument = function(source) {};


/**
 * Renders the passed SkinObject or a global skin matching the 
 * passed name to the response buffer.
 * &nbsp;
 * The properties of the optional parameter object are accessible 
 * within the skin through the 'param' macro handler.
 * 
 * @param {String} skin as SkinObject or the name of the skin as String
 * @param {Object} params as Object, optional properties to be passed to the skin
 * @see global.renderSkinAsString
 * @see HopObject.renderSkin
 * @see HopObject.renderSkinAsString
 */
renderSkin = function(skin,params){};


/**
 * Returns the result of the rendered SkinObject or a rendered 
 * global skin matching the passed name.
 * &nbsp;
 * The properties of the optional parameter object are accessible 
 * within the skin through the 'param' macro handler.
 * 
 * @param {String} skin as SkinObject or the name of the skin as String
 * @param {Object} params as Object, optional properties to be passed to the skin
 * @return String of the rendered skin
 * @type String
 * @see global.renderSkin
 * @see HopObject.renderSkin
 * @see HopObject.renderSkinAsString
 */
renderSkinAsString = function(skin,params) {};


/**
 * Seals an object, and prevents any further modifications of 
 * that object. If any property is tried to be modified after 
 * it has been sealed, an error is thrown.
 * 
 * @param {Object} obj that is to be sealed
 */
seal = function(obj) {};


/**
 * Serialize a JavaScript object to a file.
 * 
 * @param {Object} obj as Object, the object to be serialized
 * @param {String} filename as String, the file to serialize the object to
 */
serialize = function(obj,filename) {};


/**
 * Removes any markup tags contained in the passed string, and 
 * returns the modified string.
 * 
 * @param {String} markup as String, the text that is to be stripped of tags
 * @return String with the tags stripped out
 * @type String
 */
stripTags = function(markup) {};


/**
 * Converts a Javascript String, Date, etc to its Java counterpart.
 * <br /><br />
 * Converts an object into a wrapper that exposes the java
 * methods of the object to JavaScript. This is useful for
 * treating native numbers, strings, etc as their java
 * counterpart such as java.lang.Double, java.lang.String etc.
 * 
 * @param {Object} obj as String, Date, Boolean, Number, etc.
 * @return NativeJavaObject wrapping the provided object
 */
toJava = function(obj) {};


/**
 * Unwrap a map previously wrapped using wrapJavaMap().
 * 
 * @param {Object} obj as Object, a map previously wrapped using wrapJavaMap()
 * @return Packages.java.util.Map the unwrapped map object
 * @type Map
 * @see Packages.java.util.Map
 * @see #wrapJavaMap
 */
unwrapJavaMap = function(obj){};


/**
 * Wrap a java.util.Map so that it looks and behaves 
 * like a native JS object.
 * 
 * @param {Packages.java.util.Map} obj as Packages.java.util.Map to be wrapped
 * @return Object wrapping the map, making it look like a JS object
 * @type Object
 * @see #unwrapJavaMap
 */
wrapJavaMap = function(obj){};


/**
 * Writes a string to java.lang.System.out, i.e. to the console. 
 * Useful for debugging purposes.
 * 
 * @param {String} text as String, the message that is to be output
 */
write = function(text){};


/**
 * Writes a string together with a line break to 
 * java.lang.System.out, i.e. to the console. Useful for 
 * debugging purposes.
 * 
 * @param {String} text as String, the message that is to be output
 */
writeln = function(text) {};


/**
 * If defined, the onLogout() function is called when a user 
 * is logged out.
 */
onLogout = function() {};


/**
 * If defined, the onStart() function is called when the 
 * application is started.
 * <br /><br />
 * This is most useful for any initialization purposes 
 * your application may require.
 */
onStart = function() {};


/**
 * If defined, the onStop() function is called when the 
 * application is stopped.
 */
onStop = function() {};


/**
 * If defined, the onUnhandledMacro() function is called when 
 * a global macro call can not be handled.
 * <br /><br />
 * For macros that are not global, an onUnhandledMacro() 
 * method would need to be defined on the handler object. 
 * 
 * @param {String} name as String, the name of the unhandled macro
 */
onUnhandledMacro = function(name) {};


