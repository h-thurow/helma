/*
 * Helma License Notice
 *
 * The contents of this file are subject to the Helma License
 * Version 2.0 (the "License"). You may not use this file except in
 * compliance with the License. A copy of the License is available at
 * http://adele.helma.org/download/helma/license.txt
 *
 * Copyright 1998-2003 Helma Software. All Rights Reserved.
 *
 * Contributions:
 *   Daniel Ruthardt
 *   Copyright 2010 dowee Limited. All rights reserved. 
 *
 * $RCSfile$
 * $Author$
 * $Revision$
 * $Date$
 */

package helma.framework;

import helma.util.SystemMap;
import helma.util.StringUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Cookie;

import org.apache.commons.codec.binary.Base64;

import java.io.*;
import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * A Transmitter for a request from the servlet client. Objects of this
 * class are directly exposed to JavaScript as global property req.
 */
public class RequestTrans implements Serializable {

    static final long serialVersionUID = 5398880083482000580L;

    // HTTP methods
    public final static String GET = "GET"; //$NON-NLS-1$
    public final static String POST = "POST"; //$NON-NLS-1$
    public final static String DELETE = "DELETE"; //$NON-NLS-1$
    public final static String HEAD = "HEAD"; //$NON-NLS-1$
    public final static String OPTIONS = "OPTIONS"; //$NON-NLS-1$
    public final static String PUT = "PUT"; //$NON-NLS-1$
    public final static String TRACE = "TRACE"; //$NON-NLS-1$
    // Helma pseudo-methods
    public final static String XMLRPC = "XMLRPC"; //$NON-NLS-1$
    public final static String EXTERNAL = "EXTERNAL"; //$NON-NLS-1$
    public final static String INTERNAL = "INTERNAL"; //$NON-NLS-1$

    // the servlet request and response, may be null
    final HttpServletRequest request;
    final HttpServletResponse response;

    // the path info of the request
    private final String path;

    // the uri of the request
    private final String uri;

    // the request's session id
    private String session;

    // the map of form and cookie data
    private final Map values = new DataComboMap();

    private ParamComboMap params;
    private ParameterMap queryParams, postParams, cookies;
    
    // the HTTP request method
    private String method;

    // timestamp of client-cached version, if present in request
    private long ifModifiedSince = -1;

    // set of ETags the client sent with If-None-Match header
    private final Set etags = new HashSet();

    // when was execution started on this request?
    private final long startTime;

    // the name of the action being invoked
    private String action;
    private Object actionHandler = null;
    private String httpUsername;
    private String httpPassword;

    static private final Pattern paramPattern = Pattern.compile("\\[(.+?)\\]"); //$NON-NLS-1$

    /**
     *  Create a new Request transmitter with an empty data map.
     */
    public RequestTrans(String method, String path) {
        this.method = method;
        this.path = path;
        this.uri = null;
        this.request = null;
        this.response = null;
        this.startTime = System.currentTimeMillis();
    }

    /**
     *  Create a new request transmitter with the given data map.
     */
    public RequestTrans(HttpServletRequest request,
                        HttpServletResponse response, String path) {
        this.method = request.getMethod();
        this.request = request;
        this.response = response;
        this.path = path;
        this.uri = request.getRequestURI();
        this.startTime = System.currentTimeMillis();

        // do standard HTTP variables
        String header = request.getHeader("Host"); //$NON-NLS-1$
        if (header != null) {
            this.values.put("http_host", header.toLowerCase()); //$NON-NLS-1$
        }

        header = request.getHeader("Referer"); //$NON-NLS-1$
        if (header != null) {
            this.values.put("http_referer", header); //$NON-NLS-1$
        }

        try {
            long ifModifiedSince = request.getDateHeader("If-Modified-Since"); //$NON-NLS-1$
            if (ifModifiedSince > -1) {
               setIfModifiedSince(ifModifiedSince);
            }
        } catch (IllegalArgumentException ignore) {
            // not a date header
        }

        header = request.getHeader("If-None-Match"); //$NON-NLS-1$
        if (header != null) {
            setETags(header);
        }

        header = request.getRemoteAddr();
        if (header != null) {
            this.values.put("http_remotehost", header); //$NON-NLS-1$
        }

        header = request.getHeader("User-Agent"); //$NON-NLS-1$
        if (header != null) {
            this.values.put("http_browser", header); //$NON-NLS-1$
        }

        header = request.getHeader("Accept-Language"); //$NON-NLS-1$
        if (header != null) {
            this.values.put("http_language", header); //$NON-NLS-1$
        }

        header = request.getHeader("authorization"); //$NON-NLS-1$
        if (header != null) {
            this.values.put("authorization", header); //$NON-NLS-1$
        }
    }

    /**
     * Return true if we should try to handle this as XML-RPC request.
     *
     * @return true if this might be an XML-RPC request.
     */
    public synchronized boolean checkXmlRpc() {
        if ("POST".equalsIgnoreCase(this.method)) { //$NON-NLS-1$
            String contentType = this.request.getContentType();
            if (contentType == null) {
                return false;
            }
            int semi = contentType.indexOf(";"); //$NON-NLS-1$
            if (semi > -1) {
                contentType = contentType.substring(0, semi);
            }
            return "text/xml".equalsIgnoreCase(contentType.trim()); //$NON-NLS-1$
        }
        return false;
    }

    /**
     * Return true if this request is in fact handled as XML-RPC request.
     * This implies that {@link #checkXmlRpc()} returns true and a matching
     * XML-RPC action was found.
     *
     * @return true if this request is handled as XML-RPC request.
     */
    public synchronized boolean isXmlRpc() {
        return XMLRPC.equals(this.method);
    }

    /**
     * Set a cookie
     * @param name the cookie name
     * @param cookie the cookie
     */
    public void setCookie(String name, Cookie cookie) {
        if (this.cookies == null) {
            this.cookies = new ParameterMap();
        }
        this.cookies.put(name, cookie);
    }

    /**
     * @return a map containing the cookies sent with this request
     */
    public Map getCookies() {
        if (this.cookies == null) {
            this.cookies = new ParameterMap();
        }
        return this.cookies;
    }

    /**
     * @return the combined query and post parameters for this request
     */
    public Map getParams() {
        if (this.params == null) {
            this.params = new ParamComboMap();
        }
        return this.params;
    }

    /**
     * @return get the query parameters for this request
     */
    public Map getQueryParams() {
        if (this.queryParams == null) {
            this.queryParams = new ParameterMap();
        }
        return this.queryParams;
    }

    /**
     * @return get the post parameters for this request
     */
    public Map getPostParams() {
        if (this.postParams == null) {
            this.postParams = new ParameterMap();
        }
        return this.postParams;
    }

    /**
     * set the request parameters
     */
    public void setParameters(Map parameters, boolean isPost) {
        if (isPost) {
            this.postParams = new ParameterMap(parameters);
        } else {
            this.queryParams = new ParameterMap(parameters);
        }
    }

    /**
     * Add a post parameter to the request
     * @param name the parameter name
     * @param value the parameter value
     */
    public void addPostParam(String name, Object value) {
        if (this.postParams == null) {
            this.postParams = new ParameterMap();
        }
        Object previous = this.postParams.getRaw(name);
        if (previous instanceof Object[]) {
            Object[] array = (Object[]) previous;
            Object[] values = new Object[array.length + 1];
            System.arraycopy(array, 0, values, 0, array.length);
            values[array.length] = value;
            this.postParams.put(name, values);
        } else if (previous == null) {
            this.postParams.put(name, new Object[] {value});
        }
    }

    /**
     * Set a parameter value in this request transmitter. This
     * parses foo[bar][baz] as nested objects/maps.
     */
    public void set(String name, Object value) {
        this.values.put(name, value);
    }

    /**
     *  Get a value from the requests map by key.
     */
    public Object get(String name) {
        try {
            return this.values.get(name);
        } catch (Exception x) {
            return null;
        }
    }

    /**
     *  Get the data map for this request transmitter.
     */
    public Map getRequestData() {
        return this.values;
    }

    /**
     * Returns the Servlet request represented by this RequestTrans instance.
     * Returns null for internal and XML-RPC requests.
     */
    public HttpServletRequest getServletRequest() {
        return this.request;
    }

    /**
     * Proxy to HttpServletRequest.getHeader().
     * @param name the header name
     * @return the header value, or null
     */
    public String getHeader(String name) {
        return this.request == null ? null : this.request.getHeader(name);
    }

    /**
     * Proxy to HttpServletRequest.getHeaders(), returns header values as string array.
     * @param name the header name
     * @return the header values as string array
     */
    public String[] getHeaders(String name) {
        return this.request == null ?
                null : StringUtils.collect(this.request.getHeaders(name));
    }

    /**
     * Proxy to HttpServletRequest.getIntHeader(), fails silently by returning -1.
     * @param name the header name
     * @return the header parsed as integer or -1
     */
    public int getIntHeader(String name) {
        try {
            return this.request == null ? -1 : getIntHeader(name);
        } catch (NumberFormatException nfe) {
            return -1;
        }
    }

    /**
     * Proxy to HttpServletRequest.getDateHeader(), fails silently by returning -1.
     * @param name the header name
     * @return the date in milliseconds, or -1
     */
    public long getDateHeader(String name) {
        try {
            return this.request == null ? -1 : getDateHeader(name);
        } catch (NumberFormatException nfe) {
            return -1;
        }
    }

    /**
     * Returns the Servlet response for this request.
     * Returns null for internal and XML-RPC requests.
     */
    public HttpServletResponse getServletResponse() {
        return this.response;
    }

    /**
     *  The hash code is computed from the session id if available. This is used to
     *  detect multiple identic requests.
     */
    @Override
    public int hashCode() {
        if (this.session == null || this.path == null) {
            return super.hashCode();
        }
        return 17 + (37 * this.session.hashCode()) +
                    (37 * this.path.hashCode());
    }

    /**
     * A request is considered equal to another one if it has the same method,
     * path, session, request data, and conditional get data. This is used to
     * evaluate multiple simultanous identical requests only once.
     */
    @Override
    public boolean equals(Object what) {
        if (what instanceof RequestTrans) {
            if (this.session == null || this.path == null) {
                return super.equals(what);
            }
            RequestTrans other = (RequestTrans) what;
            return (this.session.equals(other.session)
                    && this.path.equalsIgnoreCase(other.path)
                    && this.values.equals(other.values)
                    && this.ifModifiedSince == other.ifModifiedSince
                    && this.etags.equals(other.etags));
        }
        return false;
    }

    /**
     * Return the method of the request. This may either be a HTTP method or
     * one of the Helma pseudo methods defined in this class.
     */
    public synchronized String getMethod() {
        return this.method;
    }

    /**
     * Set the method of this request.
     *
     * @param method the method.
     */
    public synchronized void setMethod(String method) {
        this.method = method;
    }

    /**
     *  Return true if this object represents a HTTP GET Request.
     */
    public boolean isGet() {
        return GET.equalsIgnoreCase(this.method);
    }

    /**
     *  Return true if this object represents a HTTP GET Request.
     */
    public boolean isPost() {
        return POST.equalsIgnoreCase(this.method);
    }

    /**
     * Get the request's session id
     */
    public String getSession() {
        return this.session;
    }

    /**
     * Set the request's session id
     */
    public void setSession(String session) {
        this.session = session;
    }

    /**
     * Get the request's path
     */
    public String getPath() {
        return this.path;
    }

    /**
     * Get the request's path
     */
    public String getUri() {
        return this.uri;
    }

    /**
     * Get the request's action.
     */
    public String getAction() {
        return this.action;
    }

    /**
     * Set the request's action.
     */
    public void setAction(String action) {
        int suffix = action.lastIndexOf("_action"); //$NON-NLS-1$
        this.action = suffix > -1 ? action.substring(0, suffix) : action;
    }

    /**
     * Get the request's action handler. The action handler allows the
     * onRequest() method to set the function object to be invoked for processing
     * the request, overriding the action resolved from the request path.
     * @return the action handler function
     */
    public Object getActionHandler() {
        return this.actionHandler;
    }

    /**
     * Set the request's action handler. The action handler allows the
     * onRequest() method to set the function object to be invoked for processing
     * the request, overriding the action resolved from the request path.
     * @param handler the action handler
     */
    public void setActionHandler(Object handler) {
        this.actionHandler = handler;
    }

    /**
     * Get the time the request was created.
     */
    public long getStartTime() {
        return this.startTime;
    }

    /**
     *
     *
     * @param since ...
     */
    public void setIfModifiedSince(long since) {
        this.ifModifiedSince = since;
    }

    /**
     *
     *
     * @return ...
     */
    public long getIfModifiedSince() {
        return this.ifModifiedSince;
    }

    /**
     *
     *
     * @param etagHeader ...
     */
    public void setETags(String etagHeader) {
        if (etagHeader.indexOf(",") > -1) { //$NON-NLS-1$
            StringTokenizer st = new StringTokenizer(etagHeader, ", \r\n"); //$NON-NLS-1$
            while (st.hasMoreTokens())
                this.etags.add(st.nextToken());
        } else {
            this.etags.add(etagHeader);
        }
    }

    /**
     *
     *
     * @return ...
     */
    public Set getETags() {
        return this.etags;
    }

    /**
     *
     *
     * @param etag ...
     *
     * @return ...
     */
    public boolean hasETag(String etag) {
        if ((this.etags == null) || (etag == null)) {
            return false;
        }

        return this.etags.contains(etag);
    }

    /**
     *
     *
     * @return ...
     */
    public String getUsername() {
        if (this.httpUsername != null) {
            return this.httpUsername;
        }

        String auth = (String) get("authorization"); //$NON-NLS-1$

        if ((auth == null) || "".equals(auth)) { //$NON-NLS-1$
            return null;
        }

        decodeHttpAuth(auth);

        return this.httpUsername;
    }

    /**
     *
     *
     * @return ...
     */
    public String getPassword() {
        if (this.httpPassword != null) {
            return this.httpPassword;
        }

        String auth = (String) get("authorization"); //$NON-NLS-1$

        if ((auth == null) || "".equals(auth)) { //$NON-NLS-1$
            return null;
        }

        decodeHttpAuth(auth);

        return this.httpPassword;
    }

    private void decodeHttpAuth(String auth) {
        if (auth == null) {
            return;
        }

        StringTokenizer tok;

        if (auth.startsWith("Basic ")) { //$NON-NLS-1$
            tok = new StringTokenizer(new String(Base64.decodeBase64((auth.substring(6)))),
                                      ":"); //$NON-NLS-1$
        } else {
            tok = new StringTokenizer(new String(Base64.decodeBase64(auth)), ":"); //$NON-NLS-1$
        }

        try {
            this.httpUsername = tok.nextToken();
        } catch (NoSuchElementException e) {
            this.httpUsername = null;
        }

        try {
            this.httpPassword = tok.nextToken();
        } catch (NoSuchElementException e) {
            this.httpPassword = null;
        }
    }

    @Override
    public String toString() {
        return this.method + ":" + this.path; //$NON-NLS-1$
    }

    class ParameterMap extends SystemMap {

        private static final long serialVersionUID = 7632860503639617076L;

        public ParameterMap() {
            super();
        }

        public ParameterMap(Map map) {
            super((int) (map.size() / 0.75f) + 1);
            for (Iterator i = map.entrySet().iterator(); i.hasNext(); ) {
                Map.Entry e = (Map.Entry) i.next();
                put(e.getKey(), e.getValue());
            }
        }

        @Override
        public Object put(Object key, Object value) {
            if (key instanceof String) {
                String name = (String) key;
                int bracket = name.indexOf('[');
                if (bracket > -1 && name.endsWith("]")) { //$NON-NLS-1$
                    Matcher matcher = paramPattern.matcher(name);
                    String partName = name.substring(0, bracket);
                    return putInternal(partName, matcher, value);
                }
            }
            Object previous = super.get(key);
            if (previous != null && (previous instanceof Map || value instanceof Map))
                throw new RuntimeException(Messages.getString("RequestTrans.0") + key + Messages.getString("RequestTrans.1")); //$NON-NLS-1$ //$NON-NLS-2$
            return super.put(key, value);
        }

        private Object putInternal(String name, Matcher matcher, Object value) {
            Object previous = super.get(name);
            if (matcher.find()) {
                ParameterMap map = null;
                if (previous instanceof ParameterMap) {
                    map = (ParameterMap) previous;
                } else if (previous == null) {
                    map = new ParameterMap();
                    super.put(name, map);
                } else {
                    throw new RuntimeException(Messages.getString("RequestTrans.2") + name + Messages.getString("RequestTrans.3")); //$NON-NLS-1$ //$NON-NLS-2$
                }
                String partName = matcher.group(1);
                return map.putInternal(partName, matcher, value);
            }
            if (previous != null && (previous instanceof Map || value instanceof Map))
                throw new RuntimeException(Messages.getString("RequestTrans.4") + name + Messages.getString("RequestTrans.5")); //$NON-NLS-1$ //$NON-NLS-2$
            return super.put(name, value);
        }

        @Override
        public Object get(Object key) {
            if (key instanceof String) {
                Object value = super.get(key);
                String name = (String) key;
                if (name.endsWith("_array") && value == null) { //$NON-NLS-1$
                    value = super.get(name.substring(0, name.length() - 6));
                    return value instanceof Object[] ? value : null;
                } else if (name.endsWith("_cookie") && value == null) { //$NON-NLS-1$
                    value = super.get(name.substring(0, name.length() - 7));
                    return value instanceof Cookie ? value : null;
                } else if (value instanceof Object[]) {
                    Object[] values = ((Object[]) value);
                    return values.length > 0 ? values[0] : null;
                } else if (value instanceof Cookie) {
                    Cookie cookie = (Cookie) value;
                    return cookie.getValue();
                }
            }
            return super.get(key);
        }

        protected Object getRaw(Object key) {
            return super.get(key);
        }
    }

    class DataComboMap extends SystemMap {

        private static final long serialVersionUID = 5737810055554406299L;

        @Override
        public Object get(Object key) {
            Object value = super.get(key);
            if (value != null)
                return value;
            if (RequestTrans.this.postParams != null && (value = RequestTrans.this.postParams.get(key)) != null)
                return value;
            if (RequestTrans.this.queryParams != null && (value = RequestTrans.this.queryParams.get(key)) != null)
                return value;
            if (RequestTrans.this.cookies != null && (value = RequestTrans.this.cookies.get(key)) != null)
                return value;
            return null;
        }

        @Override
        public boolean containsKey(Object key) {
            return get(key) != null;
        }

        @Override
        public Set entrySet() {
            Set entries = new HashSet(super.entrySet());
            if (RequestTrans.this.postParams != null) entries.addAll(RequestTrans.this.postParams.entrySet());
            if (RequestTrans.this.queryParams != null) entries.addAll(RequestTrans.this.queryParams.entrySet());
            if (RequestTrans.this.cookies != null) entries.addAll(RequestTrans.this.cookies.entrySet());
            return entries;
        }

        @Override
        public Set keySet() {
            Set keys = new HashSet(super.keySet());
            if (RequestTrans.this.postParams != null) keys.addAll(RequestTrans.this.postParams.keySet());
            if (RequestTrans.this.queryParams != null) keys.addAll(RequestTrans.this.queryParams.keySet());
            if (RequestTrans.this.cookies != null) keys.addAll(RequestTrans.this.cookies.keySet());
            return keys;
        }
    }

    class ParamComboMap extends SystemMap {
        private static final long serialVersionUID = -9177176570950359431L;

        @Override
        public Object get(Object key) {
            Object value;
            if (RequestTrans.this.postParams != null && (value = RequestTrans.this.postParams.get(key)) != null)
                return value;
            if (RequestTrans.this.queryParams != null && (value = RequestTrans.this.queryParams.get(key)) != null)
                return value;
            return null;
        }

        @Override
        public boolean containsKey(Object key) {
            return get(key) != null;
        }

        @Override
        public Set entrySet() {
            Set entries = new HashSet();
            if (RequestTrans.this.postParams != null) entries.addAll(RequestTrans.this.postParams.entrySet());
            if (RequestTrans.this.queryParams != null) entries.addAll(RequestTrans.this.queryParams.entrySet());
            return entries;
        }

        @Override
        public Set keySet() {
            Set keys = new HashSet();
            if (RequestTrans.this.postParams != null) keys.addAll(RequestTrans.this.postParams.keySet());
            if (RequestTrans.this.queryParams != null) keys.addAll(RequestTrans.this.queryParams.keySet());
            return keys;
        }
    }
}
