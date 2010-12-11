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

package helma.scripting.rhino;

import helma.scripting.rhino.extensions.*;
import helma.framework.core.*;
import helma.objectmodel.db.*;
import helma.util.HtmlEncoder;
import helma.util.MimePart;
import helma.util.XmlUtils;
import org.mozilla.javascript.*;
import org.mozilla.javascript.serialize.*;
import org.xml.sax.SAXException;

import java.util.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.text.*;
import java.io.*;

/**
 * Helma global object defines a number of custom global functions.
 */
public class GlobalObject extends ImporterTopLevel implements PropertyRecorder {
    private static final long serialVersionUID = -5058912338247265290L;

    Application app;
    RhinoCore core;
    boolean isThreadScope = false;

    // fields to implement PropertyRecorder
    private volatile boolean isRecording = false;
    private volatile HashSet changedProperties;

    /**
     * Creates a new GlobalObject object.
     *
     * @param core ...
     * @param app ...
     */
    public GlobalObject(RhinoCore core, Application app, boolean isThreadScope) {
        this.core = core;
        this.app = app;
        this.isThreadScope = isThreadScope;
        if (isThreadScope) {
            setPrototype(core.global);
            setParentScope(null);
        }
    }

    /**
     * Initializes the global object. This is only done for the shared
     * global objects not the per-thread ones.
     */
    public void init() {
        String[] globalFuncs = {
                                   "renderSkin", "renderSkinAsString", "getProperty", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                                   "authenticate", "createSkin", "format", "encode",   //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$//$NON-NLS-4$
                                   "encodeXml", "encodeForm", "stripTags", "formatParagraphs",  //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
                                   "getXmlDocument", "getHtmlDocument", "seal",  //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$
                                   "getURL", "write", "writeln", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                                   "serialize", "deserialize", "defineLibraryScope",   //$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$
                                   "wrapJavaMap", "unwrapJavaMap", "toJava", "definePrototype"  //$NON-NLS-1$ //$NON-NLS-2$//$NON-NLS-3$ //$NON-NLS-4$
                               };

        defineFunctionProperties(globalFuncs, GlobalObject.class, DONTENUM | PERMANENT);
        put("app", this, Context.toObject(new ApplicationBean(this.app), this)); //$NON-NLS-1$
        put("Xml", this, Context.toObject(new XmlObject(this.core), this)); //$NON-NLS-1$
        put("global", this, this); //$NON-NLS-1$
        // Define dontEnum() on Object prototype
        String[] objFuncs = { "dontEnum" }; //$NON-NLS-1$
        ScriptableObject objproto = (ScriptableObject) getObjectPrototype(this);
        objproto.defineFunctionProperties(objFuncs, GlobalObject.class, DONTENUM | PERMANENT);
    }

    /**
     * Get the global object's class name
     *
     * @return the class name for the global object
     */
    @Override
    public String getClassName() {
        return "GlobalObject"; //$NON-NLS-1$
    }

    /**
     * Override ScriptableObject.put() to implement PropertyRecorder interface
     * and to synchronize method.
     *
     * @param name
     * @param start
     * @param value
     */
    @Override
    public void put(String name, Scriptable start, Object value) {
        // register property for PropertyRecorder interface
        if (this.isRecording) {
            // if during compilation a property is set on the thread scope
            // forward it to the shared scope (bug 504)
            if (this.isThreadScope) {
                this.core.global.put(name, this.core.global, value);
                return;
            }
            this.changedProperties.add(name);
        }
        super.put(name, start, value);
    }

    /**
     * Override ScriptableObject.get() to use the per-thread scope if possible,
     * and return the per-thread scope for "global".
     *
     * @param name
     * @param start
     * @return the property for the given name
     */
    @Override
    public Object get(String name, Scriptable start) {
        // register property for PropertyRecorder interface
        if (this.isRecording) {
            this.changedProperties.add(name);
        }
        // expose thread scope as global variable "global"
        if (this.isThreadScope && "global".equals(name)) { //$NON-NLS-1$
            return this;
        }
        return super.get(name, start);
    }

    /**
     *
     *
     * @param skinobj ...
     * @param paramobj ...
     *
     * @return ...
     */
    public boolean renderSkin(Object skinobj, Object paramobj)
            throws UnsupportedEncodingException, IOException {
        RhinoEngine engine = RhinoEngine.getRhinoEngine();
        Skin skin = engine.toSkin(skinobj, "global"); //$NON-NLS-1$

        if (skin != null) {
            skin.render(engine.reval, null, 
                    (paramobj == Undefined.instance) ? null : paramobj);
        }

        return true;
    }

    /**
     *
     *
     * @param skinobj ...
     * @param paramobj ...
     *
     * @return ...
     */
    public String renderSkinAsString(Object skinobj, Object paramobj)
            throws UnsupportedEncodingException, IOException {
        RhinoEngine engine = RhinoEngine.getRhinoEngine();
        Skin skin = engine.toSkin(skinobj, "global"); //$NON-NLS-1$

        if (skin != null) {
            return skin.renderAsString(engine.reval, null,
                    (paramobj == Undefined.instance) ? null : paramobj);
        }

        return ""; //$NON-NLS-1$
    }

    /**
     *
     *
     * @param propname ...
     * @param defvalue ...
     *
     * @return ...
     */
    public String getProperty(String propname, Object defvalue) {
        if (defvalue == Undefined.instance) {
            return this.app.getProperty(propname);
        }
        return this.app.getProperty(propname, toString(defvalue));
    }

    /**
     *
     *
     * @param user ...
     * @param pwd ...
     *
     * @return ...
     */
    public boolean authenticate(String user, String pwd) {
        return this.app.authenticate(user, pwd);
    }

    /**
     *  Create a Skin object from a string
     *
     * @param str the source string to parse
     *
     * @return a parsed skin object
     */
    public Object createSkin(String str) {
        return Context.toObject(new Skin(str, this.app), this);
    }

    /**
     * Retrieve a Document from the specified URL.
     *
     * @param location the URL to retrieve
     * @param condition either a LastModified date or an ETag string for conditional GETs
     * @param timeout the optional timeout value in milliseconds used for
     *        connecting to and reading from the given URL.
     *
     * @return a wrapped MIME object
     */
    public Object getURL(String location, Object condition, Object timeout) {
        if (location ==  null) {
            return null;
        }

        try {
            URL url = new URL(location);
            URLConnection con = url.openConnection();

            // do we have if-modified-since or etag headers to set?
            if (condition != null && condition != Undefined.instance) {
                if (condition instanceof Scriptable) {
                    Scriptable scr = (Scriptable) condition;
                    if ("Date".equals(scr.getClassName())) { //$NON-NLS-1$
                        Date date = new Date((long) ScriptRuntime.toNumber(scr));

                        con.setIfModifiedSince(date.getTime());

                        SimpleDateFormat format = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", //$NON-NLS-1$
                                                                   Locale.UK);

                        format.setTimeZone(TimeZone.getTimeZone("GMT")); //$NON-NLS-1$
                        con.setRequestProperty("If-Modified-Since", format.format(date)); //$NON-NLS-1$
                    }else {
                        con.setRequestProperty("If-None-Match", scr.toString()); //$NON-NLS-1$
                    }
                } else {
                    con.setRequestProperty("If-None-Match", condition.toString()); //$NON-NLS-1$
                }
            }

            String httpUserAgent = this.app.getProperty("httpUserAgent"); //$NON-NLS-1$

            if (httpUserAgent != null) {
                con.setRequestProperty("User-Agent", httpUserAgent); //$NON-NLS-1$
            }

            if (timeout != null && timeout != Undefined.instance) {
                int time = ScriptRuntime.toInt32(timeout);
                con.setConnectTimeout(time);
                con.setReadTimeout(time);
            }

            con.setAllowUserInteraction(false);

            String filename = url.getFile();
            String contentType = con.getContentType();
            long lastmod = con.getLastModified();
            String etag = con.getHeaderField("ETag"); //$NON-NLS-1$
            int length = con.getContentLength();
            int resCode = 0;

            if (con instanceof HttpURLConnection) {
                resCode = ((HttpURLConnection) con).getResponseCode();
            }

            ByteArrayOutputStream body = new ByteArrayOutputStream();

            if ((length != 0) && (resCode != 304)) {
                InputStream in = new BufferedInputStream(con.getInputStream());
                byte[] b = new byte[1024];
                int read;

                while ((read = in.read(b)) > -1)
                    body.write(b, 0, read);

                in.close();
            }

            MimePart mime = new MimePart(filename, body.toByteArray(), contentType);

            if (lastmod > 0) {
                mime.setLastModified(new Date(lastmod));
            }

            mime.setETag(etag);

            return Context.toObject(mime, this);
        } catch (Exception x) {
            this.app.logError(Messages.getString("GlobalObject.3")+location, x); //$NON-NLS-1$
        }

        return null;
    }

    /**
     *  Try to parse an object to a XML DOM tree. The argument must be
     *  either a URL, a piece of XML, an InputStream or a Reader.
     */
    public Object getXmlDocument(Object src) {
        try {
            Object p = src;
            if (p instanceof Wrapper) {
                p = ((Wrapper) p).unwrap();
            }
            Object doc = XmlUtils.parseXml(p);

            return Context.toObject(doc, this);
        } catch (Exception x) {
            this.app.logError(Messages.getString("GlobalObject.4"),  x); //$NON-NLS-1$
        }

        return null;
    }

    /**
     *  Try to parse an object to a XML DOM tree. The argument must be
     *  either a URL, a piece of XML, an InputStream or a Reader.
     */
    public Object getHtmlDocument(Object src) {
        try {
            Object p = src;
            if (p instanceof Wrapper) {
                p = ((Wrapper) p).unwrap();
            }
            Object doc = helma.util.XmlUtils.parseHtml(p);

            return Context.toObject(doc, this);
        } catch (IOException iox) {
            this.app.logError(Messages.getString("GlobalObject.5"), iox); //$NON-NLS-1$
        } catch (SAXException sx) {
            this.app.logError(Messages.getString("GlobalObject.6"), sx); //$NON-NLS-1$
        }

        return null;
    }

    /**
     * Creates a libary namespace in the global scope.
     *
     * @param name the name of the libary namespace
     * @deprecated should be implemented in JavaScript instead
     */
    @Deprecated
    public void defineLibraryScope(final String name) {
        Object obj = get(name, this);
        if (obj != NOT_FOUND) {
            // put the property again to fool PropertyRecorder
            // into believing it has been renewed
            put(name, this, obj);
            return;
        }
        ScriptableObject scope = new NativeObject() {
            private static final long serialVersionUID = 9205558066617631601L;

            @Override
            public String getClassName() {
                return name;
            }
        };
        scope.setPrototype(ScriptableObject.getObjectPrototype(this));
        put(name, this, scope);
    }

    /**
     * Wrap a java.util.Map so that it looks and behaves like a native JS object
     * @param obj a map
     * @return a wrapper that makes the map look like a JS object
     */
    public Object wrapJavaMap(Object obj) {
        if (obj instanceof Wrapper) {
            obj = ((Wrapper) obj).unwrap();
        }
        if (!(obj instanceof Map)) {
            throw ScriptRuntime.constructError("TypeError", //$NON-NLS-1$
                Messages.getString("GlobalObject.7") + obj); //$NON-NLS-1$
        }
        return new MapWrapper((Map) obj, this.core);
    }

    /**
     * Unwrap a map previously wrapped using {@link #wrapJavaMap(Object)}.
     * @param obj the wrapped map
     * @return the map exposed as java object
     */
    public Object unwrapJavaMap(Object obj) {
        if (!(obj instanceof MapWrapper)) {
            throw ScriptRuntime.constructError("TypeError", //$NON-NLS-1$
                Messages.getString("GlobalObject.8") + obj); //$NON-NLS-1$
        }
        obj = ((MapWrapper) obj).unwrap();
        return new NativeJavaObject(this.core.global, obj, Map.class);
    }

    /**
     * Convert an object into a wrapper that exposes the java
     * methods of the object to JavaScript. This is useful for
     * treating native numbers, strings, etc as their java
     * counterpart such as java.lang.Double, java.lang.String etc.
     * @param obj a java object that is wrapped in a special way
     * Rhino
     * @return the object wrapped as NativeJavaObject, exposing
     * the public methods of the underlying class.
     */
    public Object toJava(Object obj) {
        if (obj == null || obj instanceof NativeJavaObject
                || obj == Undefined.instance) {
            return obj;
        }
        if (obj instanceof Wrapper) {
            obj = ((Wrapper) obj).unwrap();
        } else if (obj instanceof Scriptable) {
            String className = ((Scriptable) obj).getClassName();
            if ("Date".equals(className)) { //$NON-NLS-1$
                return new NativeJavaObject(this,
                        new Date((long) ScriptRuntime.toNumber(obj)), null);
            }
        }
        return new NativeJavaObject(this, obj, null);
    }

    /**
     *
     *
     * @param obj ...
     *
     * @return ...
     */
    public String encode(Object obj) {
        return HtmlEncoder.encodeAll(toString(obj));
    }

    /**
     *
     *
     * @param obj ...
     *
     * @return ...
     */
    public String encodeXml(Object obj) {
        return HtmlEncoder.encodeXml(toString(obj));
    }

    /**
     *
     *
     * @param obj ...
     *
     * @return ...
     */
    public String encodeForm(Object obj) {
        return HtmlEncoder.encodeFormValue(toString(obj));
    }

    /**
     *
     *
     * @param obj ...
     *
     * @return ...
     */
    public String format(Object obj) {
        return HtmlEncoder.encode(toString(obj));
    }

    /**
     *
     *
     * @param obj ...
     *
     * @return ...
     */
    public String formatParagraphs(Object obj) {
        String str = toString(obj);

        if (str == null) {
            return null;
        }

        int l = str.length();

        if (l == 0) {
            return ""; //$NON-NLS-1$
        }

        // try to make stringbuffer large enough from the start
        StringBuffer buffer = new StringBuffer(Math.round(l * 1.4f));

        HtmlEncoder.encode(str, buffer, true, null);

        return buffer.toString();
    }

    /**
     *
     *
     * @param str ...
     */
    public void write(String str) {
        System.out.print(str);
    }

    /**
     *
     *
     * @param str ...
     */
    public void writeln(String str) {
        System.out.println(str);
    }

     /**
     * The seal function seals all supplied arguments.
     */
    public static void seal(Context cx, Scriptable thisObj, Object[] args,
                            Function funObj)
    {
        for (int i = 0; i != args.length; ++i) {
            Object arg = args[i];
            if (!(arg instanceof ScriptableObject) || arg == Undefined.instance)
            {
                if (!(arg instanceof Scriptable) || arg == Undefined.instance)
                {
                    throw new EvaluatorException(Messages.getString("GlobalObject.9")); //$NON-NLS-1$
                }
                throw new EvaluatorException(Messages.getString("GlobalObject.10")); //$NON-NLS-1$
            }
        }

        for (int i = 0; i != args.length; ++i) {
            Object arg = args[i];
            ((ScriptableObject)arg).sealObject();
        }
    }

    /**
     * (Try to) strip all HTML/XML style tags from the given string argument
     *
     * @param str a string
     * @return the string with tags removed
     */
    public String stripTags(String str) {
        if (str == null) {
            return null;
        }

        char[] c = str.toCharArray();
        boolean inTag = false;
        int i;
        int j = 0;

        for (i = 0; i < c.length; i++) {
            if (c[i] == '<') {
                inTag = true;
            }

            if (!inTag) {
                if (i > j) {
                    c[j] = c[i];
                }

                j++;
            }

            if (c[i] == '>') {
                inTag = false;
            }
        }

        if (i > j) {
            return new String(c, 0, j);
        }

        return str;
    }

    /**
     * Serialize a JavaScript object to a file.
     */
    public static void serialize(Context cx, Scriptable thisObj,
                                 Object[] args, Function funObj)
        throws IOException
    {
        if (args.length < 2) {
            throw Context.reportRuntimeError(
                Messages.getString("GlobalObject.11") + //$NON-NLS-1$
                Messages.getString("GlobalObject.12")); //$NON-NLS-1$
        }
        Object obj = args[0];
        File file = new File(Context.toString(args[1])).getAbsoluteFile();
        FileOutputStream fos = new FileOutputStream(file);
        Scriptable scope = RhinoCore.getCore().global;
        // use a ScriptableOutputStream that unwraps Wrappers
        ScriptableOutputStream out = new ScriptableOutputStream(fos, scope) {
            @Override
            protected Object replaceObject(Object obj) throws IOException {
                if (obj instanceof Wrapper)
                    obj = ((Wrapper) obj).unwrap();
                return super.replaceObject(obj);
            }
        };
        out.writeObject(obj);
        out.close();
    }

    /**
     * Read a previously serialized JavaScript object from a file.
     */
    public static Object deserialize(Context cx, Scriptable thisObj,
                                     Object[] args, Function funObj)
        throws IOException, ClassNotFoundException
    {
        if (args.length < 1) {
            throw Context.reportRuntimeError(
                Messages.getString("GlobalObject.13")); //$NON-NLS-1$
        }
        File file = new File(Context.toString(args[0])).getAbsoluteFile();
        FileInputStream fis = new FileInputStream(file);
        Scriptable scope = RhinoCore.getCore().global;
        ObjectInputStream in = new ScriptableInputStream(fis, scope);
        Object deserialized = in.readObject();
        in.close();
        return Context.toObject(deserialized, scope);
    }

    /**
     * Set DONTENUM attrubutes on the given properties in this object. 
     * This is set on the JavaScript Object prototype.
     */
    public static Object dontEnum (Context cx, Scriptable thisObj, 
                                   Object[] args, Function funObj) {
        if (!(thisObj instanceof ScriptableObject)) {
            throw new EvaluatorException(Messages.getString("GlobalObject.14")); //$NON-NLS-1$
        }
        ScriptableObject obj = (ScriptableObject) thisObj;
        for (int i=0; i<args.length; i++) {
            if (!(args[i] instanceof String)) {
                throw new EvaluatorException(Messages.getString("GlobalObject.15")); //$NON-NLS-1$
            }
            String str = (String) args[i];
            if (obj.has(str, obj)) {
                int attr = obj.getAttributes(str);
                if ((attr & PERMANENT) == 0)
                    obj.setAttributes(str, attr | DONTENUM);
            }
        }
        return null;
    }

    public Object definePrototype(String name, Scriptable desc) {
        if (name == null) {
            throw new IllegalArgumentException(Messages.getString("GlobalObject.16")); //$NON-NLS-1$
        }
        if (desc == null) {
            throw new IllegalArgumentException(Messages.getString("GlobalObject.17")); //$NON-NLS-1$
        }

        Prototype proto = this.core.app.definePrototype(name, this.core.scriptableToProperties(desc));
        RhinoCore.TypeInfo type = (RhinoCore.TypeInfo) this.core.prototypes.get(proto.getLowerCaseName());
        if (type == null) {
            type = this.core.initPrototype(proto);
        }
        this.core.setParentPrototype(proto, type);
        return type.objProto;
    }


    private static String toString(Object obj) {
        if (obj == null || obj == Undefined.instance) {
            // Note: we might return "" here in order
            // to handle null/undefined as empty string
            return null;
        }
        return Context.toString(obj);
    }

    /**
     * Tell this PropertyRecorder to start recording changes to properties
     */
    public void startRecording() {
        this.changedProperties = new HashSet();
        this.isRecording = true;
    }

    /**
     * Tell this PropertyRecorder to stop recording changes to properties
     */
    public void stopRecording() {
        this.isRecording = false;
    }

    /**
     * Returns a set containing the names of properties changed since
     * the last time startRecording() was called.
     *
     * @return a Set containing the names of changed properties
     */
    public Set getChangeSet() {
        return this.changedProperties;
    }

    /**
     * Clear the set of changed properties.
     */
    public void clearChangeSet() {
        this.changedProperties = null;
    }

    @Override
    public String toString() {
        return this.isThreadScope ? "[Thread Scope]" : "[Shared Scope]";  //$NON-NLS-1$//$NON-NLS-2$
    }
}
