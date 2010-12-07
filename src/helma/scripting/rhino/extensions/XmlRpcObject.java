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

package helma.scripting.rhino.extensions;

import helma.framework.core.Application;
import helma.scripting.rhino.RhinoCore;
import org.apache.xmlrpc.XmlRpcClient;
import org.mozilla.javascript.*;

import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.Vector;

/**
 * An extension to transparently call and serve XML-RPC from Rhino.
 * The extension adds constructors for XML-RPC clients and servers to the Global Object.
 *
 * All argument conversion is done automatically. Currently the following argument and return
 * types are supported:
 * <ul>
 * <li> plain objects (with all properties returned by ESObject.getProperties ())</li>
 * <li> arrays</li>
 * <li> strings</li>
 * <li> date objects</li>
 * <li> booleans</li>
 * <li> integer and float numbers (long values are not supported!)</li>
 * </ul>
 *
 */
public class XmlRpcObject extends BaseFunction {

    private static final long serialVersionUID = 1479373761583135438L;

    String url = null;
    String method = null;

    XmlRpcObject(String url) {
        this.url = url;
        this.method = null;
    }

    XmlRpcObject(String url, String method) {
        this.url = url;
        this.method = method;
    }

    /**
     *  This method is used as HopObject constructor from JavaScript.
     */
    public static Object xmlrpcObjectConstructor(Context cx, Object[] args,
                                              Function ctorObj, boolean inNewExpr) {
        if (args.length == 0 || args.length > 2) {
            throw new IllegalArgumentException(Messages.getString("XmlRpcObject.0")); //$NON-NLS-1$
        }
        if (args.length == 1) {
            String url = args[0].toString();
            return new XmlRpcObject(url);
        }
        String url = args[0].toString();
        String method = args[1].toString();
        return new XmlRpcObject(url, method);

    }

    /**
     * Called by the evaluator after the extension is loaded.
     */
    public static void init(Scriptable scope) {
        Method[] methods = XmlRpcObject.class.getDeclaredMethods();
        Member ctorMember = null;
        for (int i=0; i<methods.length; i++) {
            if ("xmlrpcObjectConstructor".equals(methods[i].getName())) { //$NON-NLS-1$
                ctorMember = methods[i];
                break;
            }
        }
        FunctionObject ctor = new FunctionObject("Remote", ctorMember, scope); //$NON-NLS-1$
        ScriptableObject.defineProperty(scope, "Remote", ctor, ScriptableObject.DONTENUM); //$NON-NLS-1$
        // ctor.addAsConstructor(scope, proto);
    }


    @Override
    public Object get(String name, Scriptable start) {
        String m = this.method == null ? name : this.method+"."+name; //$NON-NLS-1$
        return new XmlRpcObject(this.url, m);
    }

    @Override
    public Object call(Context cx,
                             Scriptable scope,
                             Scriptable thisObj,
                             Object[] args)
                      throws EvaluatorException {

        if (this.method == null) {
            throw new EvaluatorException(Messages.getString("XmlRpcObject.1")); //$NON-NLS-1$
        }

        RhinoCore core = RhinoCore.getCore();
        Scriptable retval = null;

        try {
            retval = Context.getCurrentContext().newObject(core.getScope());
            XmlRpcClient client = new XmlRpcClient(this.url);

            int l = args.length;
            Vector v = new Vector();

            for (int i = 0; i < l; i++) {
                Object arg = core.processXmlRpcResponse(args[i]);
                v.addElement(arg);
            }

            Object result = client.execute(this.method, v);
            // FIXME: Apache XML-RPC 2.0 seems to return Exceptions instead of
            // throwing them.
            if (result instanceof Exception) {
                throw (Exception) result;
            }
            retval.put("result", retval, core.processXmlRpcArgument(result)); //$NON-NLS-1$

        } catch (Exception x) {
            String msg = x.getMessage();

            if ((msg == null) || (msg.length() == 0)) {
                msg = x.toString();
            }
            retval.put("error", retval, msg); //$NON-NLS-1$

            Application app = RhinoCore.getCore().getApplication(); 
            app.logError(msg, x);
        }

        return retval;

    }

    @Override
    public String getClassName() {
        return "Remote"; //$NON-NLS-1$
    }

    @Override
    public String toString() {
        return "[Remote "+this.url+"]"; //$NON-NLS-1$ //$NON-NLS-2$
    }

    @Override
    public Object getDefaultValue(Class hint) {
        if (hint == null || hint == String.class) {
            return toString();
        }
        return super.getDefaultValue(hint);
    }

}
