/*
 * Helma License Notice
 *
 * The contents of this file are subject to the Helma License
 * Version 2.0 (the "License"). You may not use this file except in
 * compliance with the License. A copy of the License is available at
 * http://adele.helma.org/download/helma/license.txt
 *
 * Copyright 1998-2003 Helma Software. All Rights Reserved.
 */

package helma.framework;

import javax.servlet.http.HttpServletRequest;
import java.io.Serializable;
import java.util.Map;

/**
 * 
 */
public class RequestBean implements Serializable {
    private static final long serialVersionUID = -6826881712426326687L;

    RequestTrans req;

    /**
     * Creates a new RequestBean object.
     *
     * @param req ...
     */
    public RequestBean(RequestTrans req) {
        this.req = req;
    }

    /**
     *
     *
     * @param name ...
     *
     * @return ...
     */
    public Object get(String name) {
        return this.req.get(name);
    }


    /**
     * Return the method of the request. This may either be a HTTP method or
     * one of the Helma pseudo methods defined in RequestTrans.
     */
    public String getMethod() {
        return this.req.getMethod();
    }

    /**
     *
     *
     * @return ...
     */
    public boolean isGet() {
        return this.req.isGet();
    }

    /**
     *
     *
     * @return ...
     */
    public boolean isPost() {
        return this.req.isPost();
    }

    /**
     * Returns the Servlet request represented by this RequestTrans instance.
     * Returns null for internal and XML-RPC requests.
     */
    public HttpServletRequest getServletRequest() {
        return this.req.getServletRequest();
    }

    /**
     * Proxy to HttpServletRequest.getHeader().
     * @param name the header name
     * @return the header value, or null
     */
    public String getHeader(String name) {
        return this.req.getHeader(name);        
    }

    /**
     * Proxy to HttpServletRequest.getHeaders(), returns header values as string array.
     * @param name the header name
     * @return the header values as string array
     */
    public String[] getHeaders(String name) {
        return this.req.getHeaders(name);
    }

    /**
     * Proxy to HttpServletRequest.getIntHeader(), fails silently by returning -1.
     * @param name the header name
     * @return the header parsed as integer or -1
     */
    public int getIntHeader(String name) {
        return this.req.getIntHeader(name);
    }

    /**
     * Proxy to HttpServletRequest.getDateHeader(), fails silently by returning -1.
     * @param name the header name
     * @return the date in milliseconds, or -1
     */
    public long getDateHeader(String name) {
        return this.req.getDateHeader(name);
    }

    /**
     * @return A string representation of this request
     */
    @Override
    public String toString() {
        return "[Request]"; //$NON-NLS-1$
    }

    /**
     * @return the invoked action
     */
    public String getAction() {
        return this.req.getAction();
    }

    /**
     * @return The req.data map containing request parameters, cookies and
     * assorted HTTP headers
     */
    public Map getData() {
        return this.req.getRequestData();
    }

    /**
     * @return the req.params map containing combined query and post parameters
     */
    public Map getParams() {
        return this.req.getParams();
    }

    /**
     * @return the req.queryParams map containing parameters parsed from the query string
     */
    public Map getQueryParams() {
        return this.req.getQueryParams();
    }

    /**
     * @return the req.postParams map containing params parsed from post data
     */
    public Map getPostParams() {
        return this.req.getPostParams();
    }

    /**
     * @return the req.cookies map containing request cookies
     */
    public Map getCookies() {
        return this.req.getCookies();
    }

    /**
     * @return the time this request has been running, in milliseconds
     */
    public long getRuntime() {
        return (System.currentTimeMillis() - this.req.getStartTime());
    }

    /**
     * @return the password if using HTTP basic authentication
     */
    public String getPassword() {
        return this.req.getPassword();
    }

    /**
     * @return the request path
     */
    public String getPath() {
        return this.req.getPath();
    }

    /**
     * @return the request URI
     */
    public String getUri() {
        return this.req.getUri();
    }

    /**
     * @return the username if using HTTP basic authentication
     */
    public String getUsername() {
        return this.req.getUsername();
    }

    /**
     * The action handler allows the onRequest() method to set the function object
     * to be invoked for processing the request, overriding the action resolved
     * from the request path.
     * @return the action handler
     */
    public Object getActionHandler() {
        return this.req.getActionHandler();
    }

    /**
     * The action handler allows the onRequest() method to set the function object
     * to be invoked for processing the request, overriding the action resolved
     * from the request path.
     * @param handler the action handler
     */
    public void setActionHandler(Object handler) {
        this.req.setActionHandler(handler);
    }
}
