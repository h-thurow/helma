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
 * 	Daniel Ruthardt
 * 	Copyright 2010 dowee it solutions GmbH. All rights reserved.
 *
 * $RCSfile$
 * $Author$
 * $Revision$
 * $Date$
 */
package helma.scripting.rhino;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Properties;

import helma.objectmodel.INode;
import helma.objectmodel.db.DbMapping;
import helma.objectmodel.db.DbKey;
import helma.objectmodel.db.Node;
import helma.objectmodel.db.WrappedNodeManager;

import org.mozilla.javascript.*;

public class HopObjectCtor extends FunctionObject {

    private static final long serialVersionUID = 3787907922712636030L;

    // init flag to trigger prototype compilation on
    // static constructor property access
    boolean initialized;
    RhinoCore core;
    Scriptable protoProperty;

    static Method hopObjCtor;

    static long collectionId = 0;    

    static {
        try {
            hopObjCtor = HopObjectCtor.class.getMethod("jsConstructor", new Class[] { //$NON-NLS-1$
                Context.class, Object[].class, Function.class, Boolean.TYPE });
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(Messages.getString("HopObjectCtor.0")); //$NON-NLS-1$
        }
    }

    static final int attr = DONTENUM | PERMANENT;
    
    /**
     * Create and install a HopObject constructor.
     * Part of this is copied from o.m.j.FunctionObject.addAsConstructor().
     *
     * @param prototype
     */
    public HopObjectCtor(String protoName, RhinoCore core, Scriptable prototype) {
        super(protoName, hopObjCtor, core.global);
        this.core = core;
        this.protoProperty = prototype;
        addAsConstructor(core.global, prototype);
        defineProperty("getById", new GetById(core.global), attr); //$NON-NLS-1$
        defineProperty("getCollection", new HopCollection(core.global), attr); //$NON-NLS-1$
    }

    /**
     *  This method is used as HopObject constructor from JavaScript.
     */
    public static Object jsConstructor(Context cx, Object[] args,
                                       Function ctorObj, boolean inNewExpr)
                         throws JavaScriptException {
        HopObjectCtor ctor = (HopObjectCtor) ctorObj;
        RhinoCore core = ctor.core;
        String protoname = ctor.getFunctionName();

        // if this is a java object prototype, create a new java object
        // of the given class instead of a HopObject.
        if (core.app.isJavaPrototype(protoname)) {
            String classname = core.app.getJavaClassForPrototype(protoname);
            try {
                Class clazz = Class.forName(classname);
                // try to get the constructor matching our arguments
                Class[] argsTypes = new Class[args.length];
                for (int i=0; i<argsTypes.length; i++) {
                    argsTypes[i] = args[i] == null ? null : args[i].getClass();
                }
                Constructor cnst = clazz.getConstructor(argsTypes);
                // crate a new instance using the constructor
                Object obj = cnst.newInstance(args);
                return Context.toObject(obj, core.global);
            } catch (Exception x) {
                System.err.println(Messages.getString("HopObjectCtor.1")+x); //$NON-NLS-1$
                throw new EvaluatorException(x.toString());
            }
        } else {
            INode node = new Node(protoname, protoname,
                    core.app.getWrappedNodeManager());
            Scriptable proto = core.getPrototype(protoname);
            HopObject hobj = new HopObject(protoname, core, node, proto);

            if (proto != null) {
                Object f = ScriptableObject.getProperty(proto, protoname);
                if (!(f instanceof Function)) {
                    // backup compatibility: look up function constructor
                    f = ScriptableObject.getProperty(proto, "__constructor__"); //$NON-NLS-1$
                }
                if (f instanceof Function) {
                    ((Function) f).call(cx, core.global, hobj, args);
                }
            }

            return hobj;
        }
    }

    @Override
    public Object get(String name, Scriptable start) {
        if (!initialized && core.isInitialized()) {
            // trigger prototype compilation on static
            // constructor property access
            initialized = true;
            core.getPrototype(getFunctionName());
        }
        return super.get(name, start);
    }

    @Override
    public void put(String name, Scriptable start, Object value) {
        if (value instanceof Function) {
            // reset static function's parent scope, needed because of the way we compile
            // prototype code, using the prototype objects as scope
            Scriptable scriptable = (Scriptable) value;
            while (scriptable != null) {
                Scriptable scope = scriptable.getParentScope();
                if (scope == protoProperty) {
                    scriptable.setParentScope(core.global);
                    break;
                }
                scriptable = scope;
            }
        }
        super.put(name, start, value);
    }

    class GetById extends BaseFunction {

        private static final long serialVersionUID = -8041352998956882647L;

        public GetById(Scriptable scope) {
            ScriptRuntime.setFunctionProtoAndParent(this, scope);
        }

        /**
         * Retrieve any persistent HopObject by type name and id.
         *
         * @return the HopObject or null if it doesn't exist
         */
        @Override
        public Object call(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
            if (args.length < 1 || args.length > 2)
                throw new IllegalArgumentException(Messages.getString("HopObjectCtor.2")); //$NON-NLS-1$
            // If second argument is provided, use it as type name.
            // Otherwise, use our own type name.
            String type = args.length == 1 ?
                    HopObjectCtor.this.getFunctionName() :
                    Context.toString(args[1]);

            DbMapping dbmap = core.app.getDbMapping(type);
            if (dbmap == null)
                return null;
            Object node = null;
            try {
                DbKey key = new DbKey(dbmap, Context.toString(args[0]));
                node = core.app.getNodeManager().getNode(key);
            } catch (Exception x) {
                return null;
            }

            if (node == null
                    || !HopObjectCtor.this.core.app.getPrototype(node)
                            .isInstanceOf(type)) {
                return null;
            }

            return Context.toObject(node, this);
        }

        @Override
        public int getArity() {
            return 1; 
        }

        @Override
        public int getLength() {
            return 1;
        }

    }

    class HopCollection extends BaseFunction {

        private static final long serialVersionUID = -4046933261468527204L;

        public HopCollection(Scriptable scope) {
            ScriptRuntime.setFunctionProtoAndParent(this, scope);
        }

        @Override
        public Object call(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
            if (args.length != 1) {
                throw new IllegalArgumentException(Messages.getString("HopObjectCtor.3")); //$NON-NLS-1$
            }
            if (!(args[0] instanceof Scriptable)) {
                throw new IllegalArgumentException(Messages.getString("HopObjectCtor.4")); //$NON-NLS-1$
            }

            Scriptable desc = (Scriptable) args[0];
            Properties childmapping = core.scriptableToProperties(desc);
            if (!childmapping.containsKey("collection")) { //$NON-NLS-1$
                // if contained type isn't defined explicitly limit collection to our own type
                childmapping.put("collection", HopObjectCtor.this.getFunctionName()); //$NON-NLS-1$
            }

            Properties props = new Properties();
            props.put("_children", childmapping); //$NON-NLS-1$
            DbMapping dbmap = new DbMapping(core.app, null, props, true);
            dbmap.update();

            WrappedNodeManager nmgr = core.app.getWrappedNodeManager();
            Node node = new Node("HopQuery", Long.toString(collectionId++), null, nmgr); //$NON-NLS-1$
            node.setDbMapping(dbmap);
            node.setState(Node.VIRTUAL);
            return new HopObject("HopQuery", core, node, core.hopObjectProto); //$NON-NLS-1$
        }

        @Override
        public int getArity() {
            return 1;
        }

        @Override
        public int getLength() {
            return 1;
        }
    }

}
