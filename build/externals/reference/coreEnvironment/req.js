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
 * $RCSfile: req.js,v $
 * $Author: hannes $
 * $Revision: 8793 $
 * $Date: 2008-02-18 16:12:35 +0100 (Mon, 18. Feb 2008) $
 */

/**
 * @fileoverview Default properties and methods of the req object.
 */

/**
 * This object is automatically instantiated as the req property 
 * of the global object (or global.req) and there is no constructor 
 * to instantiate further instances.
 * <br /><br />
 * The req object is a host object representing the request  
 * that is currently handled by the scripting environment.
 * <p>For further details also see the JavaDocs for 
 * helma.framework.RequestBean. Since that class is a 
 * JavaBean all of its get- and set-methods are also directly 
 * available as properties of this object.
 * 
 * @type RequestBean
 * @see Packages.helma.framework.RequestBean
 */
req = new Object();


/**
 * Contains the name of the requested action
 * <br /><br />
 * The name without the suffix '_action' is provided. 
 * This property is read-only.
 * <br /><br />
 * Example:
 * <pre>res.write(req.action);
 * <i>edit</i></pre>
 * 
 * @type String
 */
req.action = new String();


/**
 * The function in charge of handling the current request.
 * <br /><br />
 * The req.actionHandler property allows the onRequest() 
 * method to set the function object to be invoked for 
 * handling the request, overriding the action resolved 
 * from the request path.
 * 
 * @type Function
 * @since 1.7
 */
req.actionHandler = new Function();


/**
 * Contains the cookie request parameters.
 * 
 * @type Object
 * @see req.get
 * @see req.data
 * @see req.params
 * @see req.postParams
 * @see req.queryParams
 */
req.cookies = new Object();


/**
 * Object containing any request parameters (GET, POST, Cookies, etc) and HTTP environmental variables.
 * <br /><br />
 * This object contains all passed request parameters as 
 * named properties, no matter whether these have been submitted 
 * as URL-parameters, as part of a HTTP POST request, or as 
 * cookie values.
 * <br /><br />
 * When more than one value is submitted for the same 
 * parameter name, req.data.paramname contains just a 
 * single (the first) value. That is why all parameters are 
 * additionally also provided with their values inside an 
 * array, through properites called req.data.paramname_array 
 * (the same parameter name, with an _array suffix attached). 
 * <br /><br />
 * Parameters can be grouped into objects using a "objectid[partid]"
 * syntax in the parameter name. For example, a parameter name of 
 * "foo[bar]" will result in a req.data.foo object being created. 
 * The actual parameter value will be available at req.data.foo.bar. 
 * This feature can be used recursively, meaning that a parameter 
 * named "foo[bar][dong]" will result in req.data.foo.bar.dong.
 * <br /><br />
 * Uploaded files are available in req.data as a MimePart object.
 * <br /><br />
 * Additionally Helma sets the following HTTP environment 
 * variables if they are available:
 * <br /><br />
 * <dl>
 * <dt>authorization</dt>
 * <dd>Equivalent to the variable 'authorization' sent in 
 * the request header.</dd>
 * <dt>http_browser</dt>
 * <dd>Name and version of the client browser. Equivalent 
 * to the variable 'User-Agent' sent in the request header.</dd>
 * <dt>http_host</dt>
 * <dd>Host name to which that request was sent to. 
 * Equivalent to the variable 'Host' sent in the request header.</dd>
 * <dt>http_language</dt>
 * <dd>Equivalent to the variable 'Accept-Language' sent 
 * in the request header.</dd>
 * <dt>http_language</dt>
 * <dd>Equivalent to the variable 'Accept-Language' sent 
 * in the request header.</dd>
 * <dt>http_remotehost</dt>
 * <dd>IP-Address of the client machine. Equivalent to a 
 * getRemoteAddr() call on the servletRequest.</dd>
 * <dt>http_referer</dt>
 * <dd>URL of the page the user came from. Equivalent to the 
 * variable 'Referer' sent in the request header.</dd>
 * <dt>http_get_remainder</dt>
 * <dd>Provides access to any bytes remaining after GET 
 * parameter parsing.</dd>
 * <dt>http_post_remainder</dt>
 * <dd>Provides access to any bytes remaining after POST 
 * parameter parsing.</dd>
 * </dl>
 * <br /><br />
 * All properties of req.data are read- and write-able.
 * 
 * @type Object
 * @see req.get
 */
req.data = new Object();

    
/**
 * Returns the HTTP method (in uppercase letters) of the current request.
 * <br /><br />
 * Usually 'GET' or 'POST'. For non-web-requests this 
 * function will return one of the following: 'XMLRPC', 
 * 'EXTERNAL' or 'INTERNAL'.
 * 
 * @type String
 * @see req.isGet
 * @see req.isPost
 */
req.method = new String();

    
/**
 * Contains any GET and POST request parameters.
 * <br /><br />
 * Combined properties of req.postParams and req.queryParams
 * 
 * @type Object
 * @see req.get
 * @see req.data
 * @see req.postParams
 * @see req.queryParams
 * @see req.cookies
 */
req.params = new Object();


/**
 * Returns the decrypted basic authentication password.
 * <br /><br />
 * Only for requests that contain user credentials sent with 
 * '<a href="http://en.wikipedia.org/wiki/Basic_authentication_scheme">
 * basic authentication scheme</a>' method, typically as a result 
 * of a previously returned 401 HTTP response.
 * 
 * @type String
 * @see req.username
 */
req.password = new String();


/**
 * Contains the path of the current request, relative to the application's mountpoint.
 * <br /><br />
 * Starts without a preceding slash. This property is read-only.
 * 
 * @type String
 * @see req.uri
 */
req.path = new Array();


/**
 * Contains any post request parameters.
 * 
 * @type Object
 * @see req.get
 * @see req.data
 * @see req.params
 * @see req.queryParams
 * @see req.cookies
 */
req.postParams = new Object();


/**
 * Contains any GET request style query string parameters.
 * 
 * @type Object
 * @see req.get
 * @see req.data
 * @see req.params
 * @see req.postParams
 * @see req.cookies
 */
req.queryParams = new Object();


/**
 * The running time of the current request in milliseconds 
 * <br /><br />
 * Provides the amount of time that has elapsed since the 
 * start of the processing of the current request, measured 
 * in milliseconds (integer).
 * 
 * @type Number
 */
req.runtime = new Number();

    
/**
 * Contains the full request URI path from the web server root.
 * <br /><br />
 * Compared to req.path, which only contains the path from 
 * wherever the application is mounted, req.uri provides easy
 * access to the full request URI. The purpose is to make it 
 * easy to link to the current page without having to invoke href().
 * 
 * For internal (non-HTTP) invocations, req.uri is null.
 * 
 * @type String
 * @since 1.7
 * @see req.path
 * @see HopObject.href
 */
req.uri = new String();


/**
 * Contains the decrypted basic authentication username.
 * <br /><br />
 * Only for requests that contain user credentials sent with 
 * '<a href="http://en.wikipedia.org/wiki/Basic_authentication_scheme">
 * basic authentication scheme</a>' method, typically as a result 
 * of a previously returned 401 HTTP response.
 * 
 * @type String
 * @see req.password
 */
req.username = new String();


/**
 * An alternative method of accessing properties 
 * otherwise made available through the req.data object.
 * 
 * @param {String} name as String, the request data property to read
 * @see req.data
 */
req.get = function(name) {};


/**
 * Returns the specified header value, or null
 * <br /><br />
 * If there are several headers by that name, the first header 
 * value is returned. Proxy to HttpServletRequest.getHeader().
 * 
 * @param {String} name as String, the header name
 * @return String the header value, or null
 * @see req.getHeaders
 */
req.getHeader = function(name) {};


/**
 * Returns an Array of all header values by the specified name
 * <br /><br />
 * Proxy to HttpServletRequest.getHeaders(), returns header 
 * values as string array.
 * 
 * @param {String} name as String, the header name
 * @return Array of Strings containing the header values
 * @see req.getHeader
 */
req.getHeaders = function(name) {};


/**
 * Returns the specified header value as a Number
 * <br /><br />
 * Proxy to HttpServletRequest.getIntHeader(), fails 
 * silently by returning -1.
 * 
 * @param {String} name as String, the header name
 * @return Number the header parsed as integer or -1
 */
req.getIntHeader = function(name) {};


/**
 * Returns the specified header value as a Date object
 * <br /><br />
 * Proxy to HttpServletRequest.getDateHeader(), fails 
 * silently by returning -1.
 * 
 * @param {String} name as String,  the header name
 * @return Date the date in milliseconds, or -1
 */
req.getDateHeader = function(name) {};


/**
 * Returns true if the current request is a HTTP GET request, false otherwise.
 * 
 * @type Boolean
 * @return Boolean
 * @see req.isPost
 * @see req.method
 */
req.isGet = function() {};

/**
 * Returns true if the current request is a HTTP POST request, false otherwise.
 * 
 * @type Boolean
 * @return Boolean
 * @see req.isGet
 * @see req.method
 */
req.isPost = function() {};


/**
 * Provides access to the HttpServletRequest object
 * <br /><br />
 * Returns an instance of the Java HttpServletRequest 
 * Class corresponding to the current request, which allows 
 * full access to the methods of that class.
 * 
 * @type HttpServletRequest
 * @return Packages.javax.servlet.http.HttpServletRequest object of the current request
 * @see Packages.javax.servlet.http.HttpServletRequest
 */
req.getServletRequest = function(){};

