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
 * $RCSfile$
 * $Author$
 * $Revision$
 * $Date$
 */

package helma.framework;

import helma.objectmodel.db.Transactor;
import helma.scripting.ScriptingException;

import javax.servlet.http.HttpServletResponse;
import java.io.Serializable;
import java.io.StringWriter;
import java.io.PrintWriter;
import java.util.Date;
import java.util.Map;

/**
 * 
 */
public class ResponseBean implements Serializable {
    private static final long serialVersionUID = -6807623667477109800L;

    ResponseTrans res;

    /**
     * Creates a new ResponseBean object.
     *
     * @param res the wrapped ResponseTrans
     */
    public ResponseBean(ResponseTrans res) {
        this.res = res;
    }

    /**
     * Write an object to the response buffer by converting it to a string
     * and then HTML-encoding it.
     *
     * @param obj the object to write to the response buffer 
     */
    public void encode(Object obj) {
        this.res.encode(obj);
    }

    /**
     * Write an object to the response buffer by converting it to a string
     * and then XML-encoding it.
     *
     * @param obj the object to write to the response buffer 
     */
    public void encodeXml(Object obj) {
        this.res.encodeXml(obj);
    }

    /**
     * Write an object to the response buffer by converting it to a string
     * and then encoding it for form/text area content use.
     *
     * @param obj the object to write to the response buffer
     */
    public void encodeForm(Object obj) {
        this.res.encodeForm(obj);
    }

    /**
     * Write an object to the response buffer by converting it to a string
     * and then HTML-formatting it.
     *
     * @param obj the object to write to the response buffer 
     */
    public void format(Object obj) {
        this.res.format(obj);
    }

    /**
     * Redirect the request to a different URL
     *
     * @param url the URL to redirect to
     * @throws RedirectException to immediately terminate the request
     */
    public void redirect(String url) throws RedirectException {
        this.res.redirect(url);
    }

    /**
     * Internally forward the request to a different URL
     *
     * @param url the URL to forward to
     * @throws RedirectException to immediately terminate the request
     */
    public void forward(String url) throws RedirectException {
        this.res.forward(url);
    }

    /**
     * Immediately stop processing the current request
     *
     * @throws RedirectException to immediately terminate the request
     */
    public void stop() throws RedirectException {
        this.res.redirect(null);
    }

    /**
     * Reset the response object, clearing all content previously written to it
     */
    public void reset() {
        this.res.reset();
    }

    /**
     * Reset the response buffer, clearing all content previously written to it
     */
    public void resetBuffer() {
        this.res.resetBuffer();
    }

    /**
     * Returns the ServletResponse instance for this Response.
     * Returns null for internal and XML-RPC requests.
     * @return the servlet response
     */
    public HttpServletResponse getServletResponse() {
        return this.res.getServletResponse();
    }

    /**
     * Set a HTTP cookie with the name and value that is discarded when the 
     * HTTP client is closed
     *
     * @param key the cookie name
     * @param value the cookie value
     */
    public void setCookie(String key, String value) {
        this.res.setCookie(key, value, -1, null, null);
    }

    /**
     * Set a HTTP cookie with the name and value that is stored by the 
     * HTTP client for the given number of days. A days value of 0 means the
     * cookie should be immediately discarded.
     *
     * @param key the cookie name
     * @param value the cookie value
     * @param days number of days the cookie should be stored
     */
    public void setCookie(String key, String value, int days) {
        this.res.setCookie(key, value, days, null, null);
    }

    /**
     * Set a HTTP cookie with the name and value that is only applied to 
     * the URLs matching the given path and is stored by the 
     * HTTP client for the given number of days. A days value of 0 means the
     * cookie should be immediately discarded.
     *
     * @param key the cookie name
     * @param value the cookie value
     * @param days number of days the cookie should be stored
     * @param path the URL path to apply the cookie to
     */
    public void setCookie(String key, String value, int days, String path) {
        this.res.setCookie(key, value, days, path, null);
    }

    /**
     * Set a HTTP cookie with the name and value that is only applied to 
     * the URLs matching the given path and is stored by the 
     * HTTP client for the given number of days. A days value of 0 means the
     * cookie should be immediately discarded.
     *
     * @param key the cookie name
     * @param value the cookie value
     * @param days number of days the cookie should be stored
     * @param path the URL path to apply the cookie to
     * @param domain domain
     */
    public void setCookie(String key, String value, int days, String path, String domain) {
        this.res.setCookie(key, value, days, path, domain);
    }
    
    /**
     * Unset a previously set HTTP cookie, causing it to be discarded immedialtely by the 
     * HTTP client.
     *
     * @param key the name of the cookie to be discarded
     */
    public void unsetCookie(String key) {
        this.res.setCookie(key, "", 0, null, null); //$NON-NLS-1$
    }

    /**
     * Directly write a string to the response buffer without any transformation.
     *
     * @param str the string to write to the response buffer
     */
    public void write(String str) {
        this.res.write(str);
    }

    /**
     * Write string to response buffer and append a platform dependent newline sequence.
     *
     * @param str the string to write to the response buffer
     */
    public void writeln(String str) {
        this.res.writeln(str);
    }

    /**
     * Write a platform dependent newline sequence to response buffer.
     */
    public void writeln() {
        this.res.writeln();
    }

    /**
     * Directly write a byte array to the response buffer without any transformation.
     *
     * @param bytes the string to write to the response buffer
     */
    public void writeBinary(byte[] bytes) {
        this.res.writeBinary(bytes);
    }

    /**
     * add an HTML formatted debug message to the end of the page.
     *
     * @param message the message
     */
    public void debug(String message) {
        this.res.debug(message);
    }

    /**
     * Return a string representation for this object
     *
     * @return string representation
     */
    @Override
    public String toString() {
        return "[Response]"; //$NON-NLS-1$
    }

    // property-related methods
    
    /**
     * Return the current cachability setting for this response
     * 
     * @return true if the response may be cached by the HTTP client, false otherwise
     */
    public boolean getCache() {
        return this.res.isCacheable();
    }

    /**
     * Set true cachability setting for this response
     *
     * @param cache true if the response may be cached by the HTTP client, false otherwise
     */
    public void setCache(boolean cache) {
        this.res.setCacheable(cache);
    }

    /**
     * Get the current charset/encoding name for the response
     *
     * @return The charset name
     */
    public String getCharset() {
        return this.res.getCharset();
    }

    /**
     * Set the charset/encoding name for the response
     *
     * @param charset The charset name
     */
    public void setCharset(String charset) {
        this.res.setCharset(charset);
    }

    /**
     * Get the current content type name for the response
     *
     * @return the content type
     */
    public String getContentType() {
        return this.res.getContentType();
    }

    /**
     * Set the content type for the response
     *
     * @param contentType The charset name
     */
    public void setContentType(String contentType) {
        this.res.setContentType(contentType);
    }

    /**
     * Proxy to HttpServletResponse.addHeader()
     * @param name the header name
     * @param value the header value
     */
    public void addHeader(String name, String value) {
        this.res.addHeader(name, value);
    }

    /**
     * Proxy to HttpServletResponse.addDateHeader()
     * @param name the header name
     * @param value the header value
     */
    public void addDateHeader(String name, Date value) {
        this.res.addDateHeader(name, value);
    }

    /**
     * Proxy to HttpServletResponse.setHeader()
     * @param name the header name
     * @param value the header value
     */
    public void setHeader(String name, String value) {
        this.res.setHeader(name, value);
    }

    /**
     * Proxy to HttpServletResponse.setDateHeader()
     * @param name the header name
     * @param value the header value
     */
    public void setDateHeader(String name, Date value) {
        this.res.setDateHeader(name, value);
    }


    /**
     * Get the data map for the response
     *
     * @return the data object
     */
    public Map getData() {
        return this.res.getResponseData();
    }

    /**
     * Get the macro handlers map for the response
     *
     * @return the macro handlers map
     */
    public Map getHandlers() {
        return this.res.getMacroHandlers();
    }
    
    /**
     * Get the meta map for the response
     *
     * @return the meta map
     */
    public Map getMeta() {
        return this.res.getMetaData();
    }

    /**
     * Get the current error message for the response, if any
     *
     * @return the error message
     */
    public String getError() {
        return this.res.getErrorMessage();
    }

    /**
     * Get the uncaught exception for the response, if any
     * @return the uncaught exception
     */
    public Throwable getException() {
        return this.res.getError();
    }

    /**
     * Return the Javascript stack trace of an uncought exception.
     * @return the script stack trace of any uncaught exception or null.
     */
    public String getScriptStack() {
        Throwable t = this.res.getError();
        if (t instanceof ScriptingException)
            return ((ScriptingException) t).getScriptStackTrace();
        return null;
    }

    /**
     * Get the Java stack trace of an uncaught exception.
     * @return the java stack trace of an uncaught exception or null.
     */
    public String getJavaStack() {
        Throwable t = this.res.getError();
        if (t == null)
            return null;
        else if (t instanceof ScriptingException)
            return ((ScriptingException) t).getJavaStackTrace();
        StringWriter w = new StringWriter();
        t.printStackTrace(new PrintWriter(w));
        return w.toString();
    }

    /**
     * Get the current message for the response, if set
     *
     * @return the message
     */
    public String getMessage() {
        return this.res.getMessage();
    }

    /**
     * Set the message property for the response
     *
     * @param message the message property
     */
    public void setMessage(String message) {
        this.res.setMessage(message);
    }

    /**
     * Get the HTTP authentication realm for the response
     *
     * @return the HTTP authentication realm
     */
    public String getRealm() {
        return this.res.getRealm();
    }

    /**
     * Set the HTTP authentication realm for the response
     *
     * @param realm the HTTP authentication realm
     */
    public void setRealm(String realm) {
        this.res.setRealm(realm);
    }

    /**
     * Set the skin search path for the response
     *
     * @param arr an array containing files or nodes containing skins
     */
    public void setSkinpath(Object[] arr) {
        this.res.setSkinpath(arr);
    }

    /**
     * Get the skin search path for the response
     *
     * @return The array of files or nodes used to search for skins
     */
    public Object[] getSkinpath() {
        return this.res.getSkinpath();
    }

    /**
     * Get the HTTP status code for this response
     *
     * @return the HTTP status code
     */
    public int getStatus() {
        return this.res.getStatus();
    }

    /**
     * Set the HTTP status code for this response
     *
     * @param status the HTTP status code
     */
    public void setStatus(int status) {
        this.res.setStatus(status);
    }

    /**
     * Get the last modified date for this response
     *
     * @return the last modified date
     */
    public Date getLastModified() {
        long modified = this.res.getLastModified();

        if (modified > -1) {
            return new Date(modified);
        }
        return null;
    }

    /**
     * Set the last modified date for this response
     *
     * @param date the last modified date
     */
    public void setLastModified(Date date) {
        if (date == null) {
            this.res.setLastModified(-1);
        } else {
            this.res.setLastModified(date.getTime());
        }
    }

    /**
     * Get the ETag for this response
     *
     * @return the HTTP etag
     */
    public String getETag() {
        return this.res.getETag();
    }

    /**
     * Set the HTTP Etag for this response
     *
     * @param etag the HTTP ETag
     */
    public void setETag(String etag) {
        this.res.setETag(etag);
    }

    /**
     * Add an item to this response's dependencies. If no dependency has changed between 
     * requests, an HTTP not-modified response will be generated.
     *
     * @param what a string item this response depends on
     */
    public void dependsOn(String what) {
        this.res.dependsOn(what);
    }

    /**
     * Digest this response's dependencies to conditionally create a HTTP not-modified response
     */
    public void digest() {
        this.res.digestDependencies();
    }

    /**
     * Push a string buffer on the response object. All further
     * writes will be redirected to this buffer.
     */
    public void push() {
        this.res.pushBuffer(null);
    }

    /**
     * Pop a string buffer from the response object containing
     * all the writes since the last pushBuffer
     *
     * @return ...
     */
    public String pop() {
        return this.res.popString();
    }

    /**
     * Old version for push() kept for compatibility
     * @deprecated
     */
    @Deprecated
    public void pushStringBuffer() {
        this.res.pushBuffer(null);
    }

    /**
     * Old version for pop() kept for compatibility
     * @deprecated
     * @return ...
     */
    @Deprecated
    public String popStringBuffer() {
        return this.res.popString();
    }

    /**
     * Push a string buffer on the response object. All further
     * writes will be redirected to this buffer.
     * @param buffer the string buffer
     * @return the new stringBuffer
     */
    public StringBuffer pushBuffer(StringBuffer buffer) {
        return this.res.pushBuffer(buffer);
    }

    /**
     * Push a string buffer on the response object. All further
     * writes will be redirected to this buffer.
     * @return the new stringBuffer
     */
    public StringBuffer pushBuffer() {
        return this.res.pushBuffer(null);
    }

   /**
    * Pops the current response buffer without converting it to a string
    * @return the stringBuffer
    */
   public StringBuffer popBuffer() {
        return this.res.popBuffer();
    }

   /**
    * Returns the current response buffer as string.
    *
    * @return the response buffer as string
    */
   public String getBuffer() {
       return this.res.getBuffer().toString();
   }

    /**
     * Commit changes made during the course of the current transaction
     * and start a new one
     *
     * @throws Exception thrown if commit fails
     */
    public void commit() throws Exception {
        Transactor tx = Transactor.getInstance();
        if (tx != null) {
            String tname = tx.getTransactionName();
            tx.commit();
            tx.begin(tname);
        }
    }
    
    /**
     * Executes any changes made during the course of the current
     * transaction without actually committing the transaction.
     * 
     * @throws Exception
     */
    public void execute() throws Exception {
    	 Transactor tx = Transactor.getInstance();
         if (tx != null) {
             tx.execute();
         }
    }

    /**
     * Rollback the current transaction and start a new one.
     *
     * @throws Exception thrown if rollback fails
     */
    public void rollback() throws Exception {
        Transactor tx = Transactor.getInstance();
        if (tx != null) {
            String tname = tx.getTransactionName();
            tx.abort();
            tx.begin(tname);
        }
    }

    /**
     * Rollback the current database transaction and abort execution.
     * This has the same effect as calling rollback() and then stop().
     *
     * @throws AbortException thrown to exit the the current execution
     */
    public void abort() throws AbortException {
        throw new AbortException();
    }

}
