/*
 * Helma License Notice
 *
 * The contents of this file are subject to the Helma License
 * Version 2.0 (the "License"). You may not use this file except in
 * compliance with the License. A copy of the License is available at
 * http://adele.helma.org/download/helma/license.txt
 *
 * Copyright 2010 dowee it solutions GmbH. All rights reserved.
 *
 * Contributions:
 *   Daniel Ruthardt
 *   Copyright 2010 dowee Limited. All rights reserved. 
 *
 */

package helma.scripting.quercus;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import helma.extensions.ConfigurationException;
import helma.extensions.InterfaceHelmaExtension;
import helma.framework.RedirectException;
import helma.framework.core.Application;
import helma.framework.core.Prototype;
import helma.framework.core.RequestEvaluator;
import helma.framework.repository.Resource;
import helma.main.Server;
import helma.objectmodel.INode;
import helma.objectmodel.IProperty;
import helma.scripting.ScriptingEngine;
import helma.scripting.ScriptingException;

import com.caucho.quercus.Quercus;
import com.caucho.quercus.QuercusException;
import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.QuercusClass;
import com.caucho.quercus.env.StringValue;
import com.caucho.quercus.env.Value;
import com.caucho.quercus.expr.Expr;
import com.caucho.quercus.expr.LiteralExpr;
import com.caucho.quercus.lib.ApacheModule;
import com.caucho.quercus.lib.ApcModule;
import com.caucho.quercus.lib.ArrayModule;
import com.caucho.quercus.lib.BcmathModule;
import com.caucho.quercus.lib.ClassesModule;
import com.caucho.quercus.lib.CtypeModule;
import com.caucho.quercus.lib.ErrorModule;
import com.caucho.quercus.lib.ExifModule;
import com.caucho.quercus.lib.FunctionModule;
import com.caucho.quercus.lib.HashModule;
import com.caucho.quercus.lib.HtmlModule;
import com.caucho.quercus.lib.HttpModule;
import com.caucho.quercus.lib.ImageModule;
import com.caucho.quercus.lib.JavaModule;
import com.caucho.quercus.lib.MailModule;
import com.caucho.quercus.lib.MathModule;
import com.caucho.quercus.lib.MhashModule;
import com.caucho.quercus.lib.MiscModule;
import com.caucho.quercus.lib.NetworkModule;
import com.caucho.quercus.lib.OptionsModule;
import com.caucho.quercus.lib.OutputModule;
import com.caucho.quercus.lib.QuercusModule;
import com.caucho.quercus.lib.TokenModule;
import com.caucho.quercus.lib.UrlModule;
import com.caucho.quercus.lib.VariableModule;
import com.caucho.quercus.lib.curl.CurlModule;
import com.caucho.quercus.lib.date.DateModule;
import com.caucho.quercus.lib.db.MysqlModule;
import com.caucho.quercus.lib.db.MysqliModule;
import com.caucho.quercus.lib.db.OracleModule;
import com.caucho.quercus.lib.db.PDOModule;
import com.caucho.quercus.lib.db.PostgresModule;
import com.caucho.quercus.lib.file.FileModule;
import com.caucho.quercus.lib.file.SocketModule;
import com.caucho.quercus.lib.file.StreamModule;
import com.caucho.quercus.lib.gettext.GettextModule;
import com.caucho.quercus.lib.i18n.MbstringModule;
import com.caucho.quercus.lib.i18n.UnicodeModule;
import com.caucho.quercus.lib.jms.JMSModule;
import com.caucho.quercus.lib.json.JsonModule;
import com.caucho.quercus.lib.mcrypt.McryptModule;
import com.caucho.quercus.lib.reflection.ReflectionModule;
import com.caucho.quercus.lib.regexp.CauchoRegexpModule;
import com.caucho.quercus.lib.regexp.JavaRegexpModule;
import com.caucho.quercus.lib.regexp.RegexpModule;
import com.caucho.quercus.lib.session.SessionModule;
import com.caucho.quercus.lib.simplexml.SimpleXMLModule;
import com.caucho.quercus.lib.spl.SplModule;
import com.caucho.quercus.lib.string.StringModule;
import com.caucho.quercus.lib.xml.DomModule;
import com.caucho.quercus.lib.xml.XMLWriterModule;
import com.caucho.quercus.lib.xml.XmlModule;
import com.caucho.quercus.lib.zip.ZipModule;
import com.caucho.quercus.lib.zlib.ZlibModule;
import com.caucho.quercus.module.ModuleContext;
import com.caucho.quercus.program.AbstractFunction;
import com.caucho.quercus.program.Function;
import com.caucho.quercus.program.InterpretedClassDef;
import com.caucho.quercus.program.JavaClassDef;
import com.caucho.quercus.program.ObjectMethod;
import com.caucho.vfs.WriteStream;

/**
 * Hop scripting engine implementation that allows to use the Quercus PHP
 * interpreter.
 * 
 * @author daniel.ruthardt
 */
public class QuercusEngine implements ScriptingEngine {

    /**
     * Indirect (pull) reference to ourself, which can be read in situations,
     * were it was not possible to push a direct refernece.
     */
    protected static ThreadLocal<QuercusEngine> ENGINE                     = new ThreadLocal<QuercusEngine>();

    /**
     * The application
     */
    private Application                         _application;

    /**
     * The request evaluator we belong to
     */
    private RequestEvaluator                    _requestEvaluator;

    /**
     * The quercus PHP engine
     */
    private Quercus                             _quercus;

    /**
     * The environment will be created on enterContext() and will be unique for
     * each function invoked
     */
    private Env                                 _environment;

    /**
     * The last this value, so it can be restored after a function has been
     * invoked
     */
    private Value                               _lastThisValue;

    /**
     * Global objects provided by extensions
     */
    private final HashMap<String, Object>       _globalObjectsOfExtensions = new HashMap<String, Object>();

    /**
     * Default constructor
     */
    public QuercusEngine() {
        // empty by intention
    }

    /*
     * (non-Javadoc)
     * @see org.warp.scripting.ScriptingEngine#abort()
     */
    @Override
    public void abort() {
        // TODO: implement
    }

    /*
     * (non-Javadoc)
     * @see org.warp.scripting.ScriptingEngine#deserialize(java.io.InputStream)
     */
    @Override
    public Object deserialize(final InputStream in) {
        // TODO: implement
        return null;
    }

    /*
     * (non-Javadoc)
     * @see org.warp.scripting.ScriptingEngine#enterContext()
     */
    @SuppressWarnings("unchecked")
    @Override
    public void enterContext() throws IOException, ScriptingException {
        // set indirect back reference to ourself
        ENGINE.set(this);

        // create the script envoirement
        this._environment = this._quercus.createEnv(null, new WriteStream(
                new ResponseStream(this._requestEvaluator.getResponse())), null,
                null);

        try {
            // update prototypes
            this._application.typemgr.checkPrototypes();
        } catch (final IOException e) {
            e.printStackTrace();
            return;
        }

        // get the global prototype
        final Prototype globalPrototype = this._application
                .getPrototypeByName("Global"); //$NON-NLS-1$
        if (globalPrototype != null) {
            // FIXME: resources need to be named *.js for getCodeResources()
            // scriptExtension should be moved to a getter on Application which
            // gets the information from a static
            // getter on ScriptingEngine
            // get all code resources
            final Resource[] resources = globalPrototype.getResources();
            // loop all code resources
            for (final Resource resource : resources) {
                if (resource.getName().endsWith(".php")) { //$NON-NLS-1$
                    try {
                        // include the code (i.e. load defined functions and add
                        // as methods)
                        final Iterator<Function> functions = this._quercus
                                .parseCode(resource.getContent())
                                .getFunctions().iterator();
                        while (functions.hasNext()) {
                            final Function function = functions.next();
                            this._environment.addFunction(function.getName(),
                                    function);
                        }
                    } catch (final IOException e) {
                        throw new ScriptingException(Messages.getString("QuercusEngine.0") //$NON-NLS-1$
                                + resource.getName() + Messages.getString("QuercusEngine.1"), //$NON-NLS-1$
                                e);
                    }
                }
            }
        }

        // FIXME: needed?
        this._environment.start();

        // create the HopObject class
        // FIXME: the class is visible in the PHP context. This is not that bad,
        // but at least it is confusing to have a
        // class called HopObjectJava and one called HopObject.
        final JavaClassDef classDefHopObject = new JavaClassDef(
                new ModuleContext(ClassLoader.getSystemClassLoader()),
                "HopObjectJava", HopObject.class); //$NON-NLS-1$
        this._environment.addClassDef("HopObjectJava", classDefHopObject); //$NON-NLS-1$

        boolean containsHopObject = false;
        // solve dependencies beteween prototypes and order them in a way that
        // all dependencies are (hopefully) resolved
        // TODO: further testing
        final ArrayList<Prototype> prototypesOrderedByDependencies = new ArrayList<Prototype>();
        Iterator<Prototype> prototypes = this._application.getPrototypes().iterator();
        while (prototypes.hasNext()) {
            final Prototype prototype = prototypes.next();
            // skip prototypes we already know or if it is the global prototype
            if (prototypesOrderedByDependencies.contains(prototype)
                    || prototype.getName().equals("Global")) { //$NON-NLS-1$
                continue;
            }

            if (prototype.getName().equals("HopObject")) { //$NON-NLS-1$
                // prototype is HopObject, we need to add it as first
                prototypesOrderedByDependencies.add(0, prototype);
                containsHopObject = true;
            } else if (prototype.getParentPrototype() != null) {
                // not HopObject, parent prototype is set
                if (prototype.getParentPrototype().getName()
                        .equals("HopObject")) { //$NON-NLS-1$
                    // not HopObject, parent protype is HopObject, we need to
                    // add right after HopObject
                    prototypesOrderedByDependencies.add(
                            prototypesOrderedByDependencies.indexOf(prototype
                                    .getParentPrototype()) + 1, prototype);
                } else if (prototypesOrderedByDependencies.indexOf(prototype
                        .getParentPrototype()) >= 0
                        && prototypesOrderedByDependencies.indexOf(prototype
                                .getParentPrototype()) < prototypesOrderedByDependencies
                                .size()) {
                    // not HopObject, parent is not HopObject, but is already
                    // added, we need to add after it
                    prototypesOrderedByDependencies.add(
                            prototypesOrderedByDependencies.indexOf(prototype
                                    .getParentPrototype()) + 1, prototype);
                } else {
                    // not HopObject, parent is not HopObject, parent is not
                    // added, we can add at the end
                    prototypesOrderedByDependencies.add(prototype);
                }
            } else {
                // not HopObject, parent prototype is null, we can safely add at
                // the end
                prototypesOrderedByDependencies.add(prototype);
            }
        }

        // check if HopObject is in the list
        if (!containsHopObject) {
            // HopObject is not in the list yet, add it as first
            prototypesOrderedByDependencies.add(0, new Prototype("HopObject", //$NON-NLS-1$
                    null, this._application, null));
        }

        // loop all prototypes in the (finally) correct order
        prototypes = prototypesOrderedByDependencies.iterator();
        while (prototypes.hasNext()) {
            final Prototype prototype = prototypes.next();

            String parentClass;
            if (prototype.getName().equals("HopObject")) { //$NON-NLS-1$
                // HopObject needs HopObjectJava as parent prototype
                parentClass = "HopObjectJava"; //$NON-NLS-1$
            } else if (prototype.getParentPrototype() != null) {
                parentClass = prototype.getParentPrototype().getName();
            } else {
                // default parent prototype is HopObject
                parentClass = "HopObject"; //$NON-NLS-1$
            }

            final InterpretedClassDef classDefinitionJava = new InterpretedClassDef(
                    prototype.getName(), parentClass, new String[0]);

            /*
             * System.out.println("Defining class " +
             * classDefinitionJava.getName() + " with " +
             * classDefinitionJava.getParentName() + " as parent");
             */

            // FIXME: resources need to be named *.js
            // scriptExtension should be moved to a getter on Application which
            // gets the information from a static
            // getter on ScriptingEngine
            // get all code resources
            final Resource[] resources = prototype.getResources();
            // loop all code resources
            for (final Resource resource : resources) {
                if (resource.getName().endsWith(".php")) { //$NON-NLS-1$
                    try {
                        // include the code (i.e. load defined functions and add
                        // as methods)
                        final Iterator<InterpretedClassDef> classes = this._quercus
                                .parseCode(resource.getContent()).getClasses()
                                .iterator();
                        while (classes.hasNext()) {
                            final InterpretedClassDef classDefinitionPHP = classes
                                    .next();
                            final Iterator<Map.Entry<String, AbstractFunction>> functions = classDefinitionPHP
                                    .functionSet().iterator();
                            while (functions.hasNext()) {
                                final AbstractFunction function = functions
                                        .next().getValue();
                                if (function instanceof ObjectMethod) {
                                    classDefinitionJava
                                            .addFunction(function.getName(),
                                                    (ObjectMethod) function);

                                    // System.out.println(" Adding method " +
                                    // function.getName());
                                }
                            }

                            // TODO: handle defined static and non-static fields
                            // as well
                            /*
                             * final Iterator<Entry<StringValue, Expr>> fields =
                             * classDefinitionPHP.fieldSet().iterator(); while
                             * (fields.hasNext()) { final Entry<StringValue,
                             * Expr> field = fields.next();
                             * classDefinitionJava.addValue(field.getKey(),
                             * field.getValue());
                             * System.out.println("Added field " +
                             * field.getKey()); }
                             */
                        }
                    } catch (final IOException e) {
                        throw new ScriptingException(e.getMessage(), e);
                    }
                }
            }

            // add a constructor, which simply calls the HopObjectJava
            // constructor with the class name as argument so
            // that the NodeInterface knows the prototype
            classDefinitionJava.addFunction("__construct", this._quercus.parseCode( //$NON-NLS-1$
                    "function __construct() {HopObjectJava::__construct(\"" //$NON-NLS-1$
                            + prototype.getName() + "\")}").getFunctions() //$NON-NLS-1$
                    .iterator().next());

            this._environment.addClass(prototype.getName(), classDefinitionJava);
            // System.out.println("Added.");
        }
    }

    /*
     * (non-Javadoc)
     * @see org.warp.scripting.ScriptingEngine#exitContext()
     */
    @Override
    public void exitContext() {
        // FIXME: is this really all what needs to be done?
        ENGINE.set(null);
    }

    /**
     * Returns the application we belong to
     * 
     * @return The application we belong to
     */
    public Application getApplication() {
        return this._application;
    }

    /**
     * Returns the environment of the currently invoked function
     * 
     * @return The environment of the currently invoked function or the
     *         environment of the latestly invoked function, if there is no
     *         function currently active
     */
    public Env getEnvironment() {
        return this._environment;
    }

    /*
     * (non-Javadoc)
     * @see org.warp.scripting.ScriptingEngine#getProperty(java.lang.Object,
     * java.lang.String)
     */
    @Override
    public Object getProperty(final Object thisObject, final String propertyName) {
        if (thisObject == null || thisObject instanceof INode
                && ((INode) thisObject).getPrototype().equals("Global")) { //$NON-NLS-1$
            return this._environment.getGlobalValue(propertyName);
        } else if (thisObject instanceof HopObject) {
            return ((HopObject) thisObject).getFieldExt(this._environment,
                    StringValue.create(propertyName).toStringValue());
        } else if (thisObject instanceof INode) {
            return new HopObject((INode) thisObject, this).getFieldExt(
                    this._environment, StringValue.create(propertyName)
                            .toStringValue());
        } else if (thisObject instanceof Value[]) {
            final Value[] values = (Value[]) thisObject;
            if (values.length > 0 && values[0] instanceof HopObject) {
                final Object value = ((HopObject) values[0]).getFieldExt(
                        this._environment,
                        StringValue.create(propertyName).toStringValue())
                        .toJavaObject();
                if (value instanceof IProperty) {
                    return ((IProperty) value).getValue();
                }

                return value;
            }
        }

        return null;
    }

    /**
     * Returns the Quercus PHP engine
     * 
     * @return The Quercus PHP engine
     */
    public Quercus getQuercus() {
        return this._quercus;
    }

    /**
     * Returns the request evaluator we belong to
     * 
     * @return The request evaluator we belong to
     */
    protected RequestEvaluator getRequestEvaluator() {
        return this._requestEvaluator;
    }

    /*
     * (non-Javadoc)
     * @see org.warp.scripting.ScriptingEngine#hasFunction(java.lang.Object,
     * java.lang.String, boolean)
     */
    @Override
    public boolean hasFunction(final Object thisObject,
            final String functionName, final boolean resolve) {
        QuercusClass classDef = null;

        if (thisObject == null || thisObject instanceof INode
                && ((INode) thisObject).getPrototype().equals("Global")) { //$NON-NLS-1$
            // no thisObject or an INode with prototype Global, look for a
            // global function
            return this._environment.findFunction(functionName) != null;
        } else if (thisObject instanceof HopObject) {
            // thisObject is a wrapped INode, look for a method of the
            // corresponding class
            classDef = this._environment
                    .getClass(((HopObject) thisObject).getName());
        } else if (thisObject instanceof INode) {
            // thisObject is an unwrapped INode, look for a method of the
            // corresponding class
            classDef = this._environment.getClass(((INode) thisObject)
                    .getPrototype());
        } else if (thisObject instanceof Value[]) {
            // thisObject is an array of values
            final Value[] values = (Value[]) thisObject;
            // check if the first value is a wrapped INode
            if (values.length > 0 && values[0] instanceof HopObject) {
                // first value is a wrapped INode, look for a method of the
                // corresponding class
                classDef = this._environment.getClass(((HopObject) values[0])
                        .getName());
            }
        }

        boolean found = false;
        // follow the parent chain as long as possible and try to find the
        // function (if not found above)
        while (!found && classDef != null) {
            found = classDef.findFunction(functionName) != null;
            if (!resolve) {
                return found;
            }

            // check for existance of parent class
            if (classDef.getParentName() != null) {
                classDef = this._environment.getClass(classDef.getParentName());
            } else {
                classDef = null;
            }
        }

        return found;
    }

    /*
     * (non-Javadoc)
     * @see org.warp.scripting.ScriptingEngine#hasProperty(java.lang.Object,
     * java.lang.String)
     */
    @Override
    public boolean hasProperty(final Object thisObject,
            final String propertyName) {
        if (thisObject == null || thisObject instanceof INode
                && ((INode) thisObject).getPrototype().equals("Global")) { //$NON-NLS-1$
            // no thisObject or an INode with prototype Global, look for a
            // global variable
            return this._environment.getGlobalValue(propertyName) != null;
        } else if (thisObject instanceof HopObject) {
            // thisObject is a wrapped INode, look for a value in the INode
            return ((HopObject) thisObject).getFieldExt(this._environment,
                    StringValue.create(propertyName).toStringValue()) != null;
        } else if (thisObject instanceof INode) {
            // thisObject is an unwrapped INode, look for a value in the INode
            return new HopObject((INode) thisObject, this).getFieldExt(
                    this._environment, StringValue.create(propertyName)
                            .toStringValue()) != null;
        } else if (thisObject instanceof Value[]) {
            // thisObject is an array of values
            final Value[] values = (Value[]) thisObject;
            // check if the first value is a wrapped INode
            if (values.length > 0 && values[0] instanceof HopObject) {
                // first value is a wrapped INode, look for a value in the INode
                return ((HopObject) values[0]).getFieldExt(this._environment,
                        StringValue.create(propertyName).toStringValue())
                        .toJavaObject() != null;
            }
        }

        return false;
    }

    /*
     * (non-Javadoc)
     * @see
     * org.warp.scripting.ScriptingEngine#init(org.warp.framework.core.Application
     * , org.warp.framework.core.RequestEvaluator)
     */
    @SuppressWarnings("unchecked")
    @Override
    public void init(final Application application,
            final RequestEvaluator requestEvaluator) {
        // set references
        this._application = application;
        this._requestEvaluator = requestEvaluator;

        // create the quercus php engine
        this._quercus = new Quercus();

        // load all available php modules to be as close to native php as
        // possible
        // TODO: make it configurable
        this._quercus.addModule(new ApacheModule());
        this._quercus.addModule(new ApcModule());
        this._quercus.addModule(new ArrayModule());
        this._quercus.addModule(new BcmathModule());
        this._quercus.addModule(new CauchoRegexpModule());
        this._quercus.addModule(new ClassesModule());
        this._quercus.addModule(new CtypeModule());
        this._quercus.addModule(new CurlModule());
        this._quercus.addModule(new DateModule());
        this._quercus.addModule(new DomModule());
        this._quercus.addModule(new ErrorModule());
        this._quercus.addModule(new ExifModule());
        this._quercus.addModule(new FileModule());
        this._quercus.addModule(new FunctionModule());
        this._quercus.addModule(new GettextModule());
        this._quercus.addModule(new HashModule());
        this._quercus.addModule(new HtmlModule());
        this._quercus.addModule(new HttpModule());
        this._quercus.addModule(new ImageModule());
        this._quercus.addModule(new JavaModule());
        this._quercus.addModule(new JavaRegexpModule());
        this._quercus.addModule(new JMSModule());
        this._quercus.addModule(new JsonModule());
        this._quercus.addModule(new MailModule());
        this._quercus.addModule(new MathModule());
        this._quercus.addModule(new MbstringModule());
        this._quercus.addModule(new McryptModule());
        this._quercus.addModule(new MhashModule());
        this._quercus.addModule(new MiscModule());
        this._quercus.addModule(new MysqliModule());
        this._quercus.addModule(new MysqlModule());
        this._quercus.addModule(new NetworkModule());
        this._quercus.addModule(new OptionsModule());
        this._quercus.addModule(new OutputModule());
        this._quercus.addModule(new OracleModule());
        this._quercus.addModule(new PDOModule());
        this._quercus.addModule(new PostgresModule());
        this._quercus.addModule(new QuercusModule());
        this._quercus.addModule(new ReflectionModule());
        this._quercus.addModule(new RegexpModule());
        this._quercus.addModule(new SessionModule());
        this._quercus.addModule(new SimpleXMLModule());
        this._quercus.addModule(new SocketModule());
        this._quercus.addModule(new SplModule());
        this._quercus.addModule(new StreamModule());
        this._quercus.addModule(new StringModule());
        this._quercus.addModule(new TokenModule());
        this._quercus.addModule(new UnicodeModule());
        this._quercus.addModule(new UrlModule());
        this._quercus.addModule(new VariableModule());
        this._quercus.addModule(new XMLWriterModule());
        this._quercus.addModule(new XmlModule());
        this._quercus.addModule(new ZipModule());
        this._quercus.addModule(new ZlibModule());

        // load our own module which provides some global functions
        this._quercus.addModule(new GlobalFunctions());

        // activate php to java byte code compilation (only available in the pro
        // engine, evaluation licenses are
        // available, but I didn't figure out how to load them yet)
        // FIXME: how to activate it?
        // TODO: make it configurable
        this._quercus.setCompile(true);
        this._quercus.setLazyCompile(true);

        // start the quercus php engine
        // FIXME: needed?
        this._quercus.start();

        // TODO: currently an extension is only able to provide global objects.
        // what else could an extension provide? in
        // the
        // context of this scripting engine, an extension might for example
        // provide Modules, classes, functions ...
        // check if a server is up and running
        if (Server.getServer() != null) {
            // get all extensions
            final Iterator extensions = Server.getServer().getExtensions()
                    .iterator();
            // loop all extensions
            while (extensions.hasNext()) {
                // get next extension
                final InterfaceHelmaExtension extension = (InterfaceHelmaExtension) extensions
                        .next();

                HashMap<String, ? extends Object> globals = null;
                try {
                    // init extension and get global objects provided by it
                    globals = extension.initScripting(this._application, this);
                } catch (final ConfigurationException e) {
                    this._application.logError(Messages.getString("QuercusEngine.2") //$NON-NLS-1$
                            + extension.getName() + Messages.getString("QuercusEngine.3")); //$NON-NLS-1$
                }

                // check if extension provides any global objects
                if (globals != null) {
                    // add global objects
                    this._globalObjectsOfExtensions.putAll(globals);
                }
            }
        }
    }

    /*
     * (non-Javadoc)
     * @see
     * org.warp.scripting.ScriptingEngine#injectCodeResource(java.lang.String,
     * org.warp.framework.repository.Resource)
     */
    @Override
    public void injectCodeResource(
            final String typename,
            final Resource resource) {
        // TODO: implement
    }

    /*
     * (non-Javadoc)
     * @see org.warp.scripting.ScriptingEngine#invoke(java.lang.Object,
     * java.lang.Object, java.lang.Object[], int, boolean)
     */
    @Override
    public Object invoke(final Object thisObject, final Object functionName,
            final Object[] arguments,
            final int argumentsWrapMode,
            final boolean resolve)
            throws ScriptingException {
        // check if functionName is invalid
        if (functionName == null || !(functionName instanceof String)) {
            throw new IllegalArgumentException(
                    Messages.getString("QuercusEngine.4")); //$NON-NLS-1$
        }

        // check if thisObject is invalid
        if (thisObject != null && !(thisObject instanceof INode)) {
            throw new IllegalArgumentException(
                    Messages.getString("QuercusEngine.5")); //$NON-NLS-1$
        }

        // check if we should invode a global function
        if (thisObject == null) {
            // to avoid errors, check if the function does not exist at all
            if (!hasFunction(null, (String) functionName, false)) {
                return null;
            }

            try {
                // global function call
                return this._environment.call((String) functionName).toJavaObject();
            } catch (final QuercusException e) {
                // check if we should throw an exception, or if we just caught a
                // RedirectExcpetion, which we will ignore
                if (!(e.getCause() instanceof RedirectException)) {
                    throw new ScriptingException(e.getMessage(), e);
                }
            }

            return null;
        }

        // to avoid errors, check if the function does not exist at all
        if (!hasFunction(thisObject, (String) functionName, true)) {
            return null;
        }

        // another function call might be active, store the current this value
        this._lastThisValue = this._environment.getThis();
        // set the new this value
        this._environment.setThis(new HopObject((INode) thisObject, this));

        // wrap the arguments (actually this is done to have the arguments a
        // little bit more unwrapped, than a direct
        // call to Env.wrapJava(arguments) would result in
        final Expr[] parameters = new Expr[arguments.length];
        for (int i = 0; i < arguments.length; i++) {
            parameters[i] = new LiteralExpr(this._environment.wrapJava(arguments[i]));
        }

        Object result = null;
        try {
            result = this._environment.getThis().callMethod(this._environment,
                    (StringValue) StringValue.create(functionName), parameters)
                    .toJavaObject();
        } catch (final QuercusException e) {
            // check if we should throw an exception, or if we just caught a
            // RedirectExcpetion, which we will ignore
            if (!(e.getCause() instanceof RedirectException)) {
                throw new ScriptingException(e.getMessage(), e);
            }
        }

        // flush output
        this._environment.flush();
        // restore the previous this value
        this._environment.setThis(this._lastThisValue);

        return result;
    }

    /*
     * (non-Javadoc)
     * @see org.warp.scripting.ScriptingEngine#isTypedObject(java.lang.Object)
     */
    @Override
    public boolean isTypedObject(final Object obj) {
        // TODO: implement
        return false;
    }

    /*
     * (non-Javadoc)
     * @see org.warp.scripting.ScriptingEngine#serialize(java.lang.Object,
     * java.io.OutputStream)
     */
    @Override
    public void serialize(final Object obj, final OutputStream out) {
        // TODO: implement
    }

    /*
     * (non-Javadoc)
     * @see org.warp.scripting.ScriptingEngine#setGlobals(java.util.Map)
     */
    @SuppressWarnings("unchecked")
    @Override
    public void setGlobals(final Map globals) throws ScriptingException {
        if (globals != null) {
            // loop all globals
            final Iterator<Map.Entry<String, Object>> globalObjects = globals
                    .entrySet().iterator();
            while (globalObjects.hasNext()) {
                final Map.Entry<String, Object> globalObject = globalObjects
                        .next();
                try {
                    // check type of global
                    if (globalObject.getValue() instanceof INode) {
                        // global is an INode, we need to wrap it as HopObject
                        final INode node = (INode) globalObject.getValue();
                        this._environment.setGlobalValue(globalObject.getKey(),
                                new HopObject(node, this));
                        // System.out.println("Adding global " +
                        // globalObject.getKey() + " wrapped");
                    } else {
                        // global is something else, let Quercus do the default
                        // wrapping
                        this._environment.setGlobalValue(globalObject.getKey(),
                                this._environment.wrapJava(globalObject.getValue()));
                        // System.out.println("Adding global " +
                        // globalObject.getKey() + " as is");
                    }
                } catch (final QuercusException e) {
                    throw new ScriptingException(e.getMessage(), e);
                }
            }
        }
    }

    @Override
    public void shutdown() {
        // TODO: implement
    }

    /*
     * (non-Javadoc)
     * @see org.warp.scripting.ScriptingEngine#toString(java.lang.Object)
     */
    @Override
    public String toString(final Object object) {
        // check type of object
        if (object != null && object instanceof Value) {
            // object is a value, convert it to java first, then call toString()
            // on it
            final Object value = ((Value) object).toJavaObject();
            if (value != null) {
                return value.toString();
            }
        } else if (object != null) {
            // object is already a java value, just call toString() on it
            return object.toString();
        }

        return null;
    }

    @Override
    public Object getGlobalProperty(String propertyName) {
        // TODO Auto-generated method stub
        return null;
    }

}