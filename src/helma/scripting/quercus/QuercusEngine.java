/*
 * Helma License Notice
 *
 * The contents of this file are subject to the Helma License
 * Version 2.0 (the "License"). You may not use this file except in
 * compliance with the License. A copy of the License is available at
 * http://adele.helma.org/download/helma/license.txt
 *
 * Copyright 2010-2017 Daniel Ruthardt. All rights reserved.
 */

package helma.scripting.quercus;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.caucho.quercus.Quercus;
import com.caucho.quercus.QuercusException;
import com.caucho.quercus.env.ArrayValueImpl;
import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.NullValue;
import com.caucho.quercus.env.QuercusClass;
import com.caucho.quercus.env.StringValue;
import com.caucho.quercus.env.Value;
import com.caucho.quercus.function.AbstractFunction;
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
import com.caucho.quercus.lib.JavaModule;
import com.caucho.quercus.lib.MathModule;
import com.caucho.quercus.lib.MhashModule;
import com.caucho.quercus.lib.MiscModule;
import com.caucho.quercus.lib.NetworkModule;
import com.caucho.quercus.lib.OptionsModule;
import com.caucho.quercus.lib.OutputModule;
import com.caucho.quercus.lib.QuercusModule;
import com.caucho.quercus.lib.TokenModule;
import com.caucho.quercus.lib.VariableModule;
import com.caucho.quercus.lib.date.DateModule;
import com.caucho.quercus.lib.db.MysqlModule;
import com.caucho.quercus.lib.db.MysqliModule;
import com.caucho.quercus.lib.db.OracleModule;
import com.caucho.quercus.lib.db.PDOModule;
import com.caucho.quercus.lib.db.PostgresModule;
import com.caucho.quercus.lib.dom.QuercusDOMModule;
import com.caucho.quercus.lib.file.FileModule;
import com.caucho.quercus.lib.file.SocketModule;
import com.caucho.quercus.lib.file.StreamModule;
import com.caucho.quercus.lib.i18n.MbstringModule;
import com.caucho.quercus.lib.i18n.UnicodeModule;
import com.caucho.quercus.lib.jms.JMSModule;
import com.caucho.quercus.lib.json.JsonModule;
import com.caucho.quercus.lib.mail.MailModule;
import com.caucho.quercus.lib.mcrypt.McryptModule;
import com.caucho.quercus.lib.pdf.PDFModule;
import com.caucho.quercus.lib.reflection.ReflectionModule;
import com.caucho.quercus.lib.regexp.RegexpModule;
import com.caucho.quercus.lib.session.SessionModule;
import com.caucho.quercus.lib.simplexml.SimpleXMLModule;
import com.caucho.quercus.lib.spl.SplModule;
import com.caucho.quercus.lib.string.StringModule;
import com.caucho.quercus.lib.xml.XmlModule;
import com.caucho.quercus.lib.zip.ZipModule;
import com.caucho.quercus.lib.zlib.ZlibModule;
import com.caucho.quercus.module.ModuleContext;
import com.caucho.quercus.program.Function;
import com.caucho.quercus.program.InterpretedClassDef;
import com.caucho.quercus.program.JavaClassDef;
import com.caucho.quercus.program.ObjectMethod;
import com.caucho.vfs.WriteStream;

import helma.extensions.ConfigurationException;
import helma.extensions.HelmaExtensionInterface;
import helma.framework.RedirectException;
import helma.framework.ResponseTrans;
import helma.framework.core.Application;
import helma.framework.core.Prototype;
import helma.framework.core.RequestEvaluator;
import helma.framework.core.Skin;
import helma.framework.repository.ResourceInterface;
import helma.main.Server;
import helma.objectmodel.NodeInterface;
import helma.scripting.ScriptingEngineInterface;
import helma.scripting.ScriptingException;

/**
 * Helma scripting engine implementation that allows to use the Quercus PHP interpreter.
 */
public class QuercusEngine implements ScriptingEngineInterface {

    /**
     * Indirect (pull) reference to ourself, which can be read in situations, were it was not possible to 
     * push a direct refernece.
     */
    protected static ThreadLocal<QuercusEngine> ENGINE = new ThreadLocal<QuercusEngine>();

    /**
     * The application we belong to.
     */
    private Application _application;

    /**
     * The request evaluator we belong to.
     */
    private RequestEvaluator _requestEvaluator;

    /**
     * The quercus PHP engine.
     */
    private Quercus _quercus;

    /**
     * The PHP context.
     * It will be created on enterContext() and will be unique for each invoked function.
     */
    private Env _environment;

    /**
     * The last this value, so it can be restored after a function has been invoked.
     */
    private Value _lastThisValue;

    /**
     * Global objects provided by extensions.
     */
    private final HashMap<String, Object> _globalObjectsOfExtensions = new HashMap<String, Object>();

    
    /**
     * Default constructor.
     */
    public QuercusEngine() {
        // nothing to be done
    }

    /*
     * (non-Javadoc)
     * @see org.warp.scripting.ScriptingEngine#abort()
     */
    @Override
    public void abort() {
        // let the PHP context die
        this._environment.die();
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

        // re-init the scripting engine
        // TODO try to cache more and re-init less
        this.init(this._application, this._requestEvaluator);

        // create the PHP context
        this._environment = this._quercus.createEnv(null, new WriteStream(new ResponseStream(
                this._requestEvaluator.getResponse())), null, null);

        // update prototypes
        this._application.typemgr.checkPrototypes();

        // get the global prototype
        final Prototype globalPrototype = this._application.getPrototypeByName("Global"); //$NON-NLS-1$
        if (globalPrototype != null) {
            // TODO resources need to be named *.js for getCodeResources()
            // scriptExtension should be moved to a getter on Application which gets the information from a 
            // static getter on ScriptingEngineInterface get all code resources
            final ResourceInterface[] resources = globalPrototype.getResources();
            // loop all code resources
            for (final ResourceInterface resource : resources) {
                // check if the current resource is a PHP resource
                if (resource.getName().endsWith(".php")) { //$NON-NLS-1$
                    try {
                        // include the code (i.e. load defined functions and add as methods)
                        final Iterator<Function> functions = this._quercus.parseCode(StringValue.create(
                                resource.getContent()).toStringValue()).getFunctionList().iterator();
                        // loop all functions
                        while (functions.hasNext()) {
                            // get the current function
                            final Function function = functions.next();
                            // add the current function to the PHP context as global function
                            this._environment.addFunction(function.getName(), function);
                        }
                    } catch (final IOException e) {
                        throw new ScriptingException(Messages.getString("QuercusEngine.0") //$NON-NLS-1$
                                + resource.getName() + Messages.getString("QuercusEngine.1"), e); //$NON-NLS-1$
                    }
                }
            }
        }

        // start the PHP context
        this._environment.start();

        // create the wrapper class
        this._environment.addClassDef("HopObjectWrapper", new JavaClassDef(new ModuleContext(null, //$NON-NLS-1$
                ClassLoader.getSystemClassLoader()), "HopObjectWrapper", HopObject.class)); //$NON-NLS-1$

        // if the HopObject prototype is defined explicetely
        boolean containsHopObject = false;
        // solve dependencies beteween prototypes and order them in a way that all dependencies are 
        // (hopefully) resolved
        // TODO: further testing
        final ArrayList<Prototype> prototypesOrderedByDependencies = new ArrayList<Prototype>();
        // get all prototypes
        Iterator<Prototype> prototypes = this._application.getPrototypes().iterator();
        // loop all prototypes
        while (prototypes.hasNext()) {
            // get the next prototype
            final Prototype prototype = prototypes.next();
            // check if the current prototype is already known (the Global prototype is always already known)
            if (prototypesOrderedByDependencies.contains(prototype) 
                    || prototype.getName().equals("Global")) { //$NON-NLS-1$
                // ignore the current prototype, it is already known
                continue;
            }

            // check if the current prototype is the HopObject prototype
            if (prototype.getName().equalsIgnoreCase("HopObject")) { //$NON-NLS-1$
                // add the HopObject prototype as first prototype, all other prototypes depend on it
                prototypesOrderedByDependencies.add(0, prototype);
                // the HopObject prototype is defined explicitely
                containsHopObject = true;
            }
            // check if the current prototype has a parent prototype, i.e. depends on another prototype
            else if (prototype.getParentPrototype() != null) {
                // check if the current prototype's parent prototype is the HopObject prototype
                if (prototype.getParentPrototype().getName().equalsIgnoreCase("HopObject")) { //$NON-NLS-1$
                    // add the current prototype right after the HopObject prototype
                    prototypesOrderedByDependencies.add(prototypesOrderedByDependencies
                            .indexOf(prototype.getParentPrototype()) + 1, prototype);
                }
                // check if the current prototype's parent prototype is already known
                else if (prototypesOrderedByDependencies.indexOf(prototype.getParentPrototype()) >= 0
                        && prototypesOrderedByDependencies.indexOf(prototype.getParentPrototype()) < 
                        prototypesOrderedByDependencies.size()) {
                    // add the current prototype right after the already known parent prototype
                    prototypesOrderedByDependencies.add(prototypesOrderedByDependencies
                            .indexOf(prototype.getParentPrototype()) + 1, prototype);
                } else {
                    // the current prototype is not the HopObject prototype, the parent prototype is not the
                    // HopObject prototype and the parent prototype is not known already, add the current
                    // prototype at the end (which will hopefully prevent issues)
                    prototypesOrderedByDependencies.add(prototype);
                }
            } else {
                // the current prototype is not the HopObject prototype and also does not have a parent
                // prototype (can this even be possible?)
                // TODO check if this case is even possible
                prototypesOrderedByDependencies.add(prototype);
            }
        }

        // check if the HopObject prototype is still not known
        if (!containsHopObject) {
            // add the HopObject prototype as first prototype
            prototypesOrderedByDependencies.add(0, new Prototype("HopObject", null, this._application, //$NON-NLS-1$
                    null));
        }

        // get all prototypes now sorted by dependency
        prototypes = prototypesOrderedByDependencies.iterator();
        // loop all prototypes now sorted by dependency
        while (prototypes.hasNext()) {
            // get the next prototype
            final Prototype prototype = prototypes.next();

            // the PHP parent class
            String parentClass;
            // check if the current prototype is the HopObject prototype
            if (prototype.getName().equalsIgnoreCase("HopObject")) { //$NON-NLS-1$
                // use the wrapper class as parent class 
                parentClass = "HopObjectWrapper"; //$NON-NLS-1$
            }
            // check if the current prototype has a parent prototype
            else if (prototype.getParentPrototype() != null) {
                // us the corresponding class as parent class
                parentClass = prototype.getParentPrototype().getName();
            } else {
                // default to the HopObject class
                parentClass = "HopObject"; //$NON-NLS-1$
            }

            // create a PHP class for the current prototype the quick and ugly way
            final InterpretedClassDef classDefinitionJava = this._quercus.parseCode(StringValue.create(
                "class " + prototype.getName() + " extends " + parentClass + " {" + //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                    "public function __construct($prototype = null) {" + //$NON-NLS-1$
                        "parent::__construct($prototype ? $prototype : get_called_class())" + //$NON-NLS-1$
                    "}" + //$NON-NLS-1$
                "}").toStringValue()).getClassList().iterator().next(); //$NON-NLS-1$

            // FIXME resources need to be named *.js
            // scriptExtension should be moved to a getter on Application which gets the information from a 
            // static getter on ScriptingEngineInterface get all code resources
            final ResourceInterface[] resources = prototype.getResources();
            // loop all code resources
            for (final ResourceInterface resource : resources) {
                // check if the current resource is a PHP resource
                if (resource.getName().endsWith(".php")) { //$NON-NLS-1$
                    try {
                        // include the code (i.e. load defined functions and add as methods)
                        final Iterator<InterpretedClassDef> classes = this._quercus
                                .parseCode(StringValue.create(resource.getContent()).toStringValue())
                                .getClassList().iterator();
                        // loop all defined classes
                        while (classes.hasNext()) {
                            // get the next class
                            final InterpretedClassDef classDefinitionPHP = classes.next();
                            // get all defined functions
                            final Iterator<Map.Entry<StringValue, AbstractFunction>> functions = 
                                    classDefinitionPHP.functionSet().iterator();
                            // loop all defined functions
                            while (functions.hasNext()) {
                                // get the next function
                                final AbstractFunction function = functions.next().getValue();
                                // check if the current function is a method
                                if (function instanceof ObjectMethod) {
                                    // add the current function to the prototype's class
                                    classDefinitionJava.addFunction(
                                            StringValue.create(function.getName()).toStringValue(), 
                                            (ObjectMethod) function);
                                }
                            }

                            // TODO handle all the other stuff like static functions, fields, etc.
                        }
                    } catch (final IOException e) {
                        throw new ScriptingException(e.getMessage(), e);
                    }
                }
            }

            // add the current prototype's class to the PHP context
            this._environment.addClass(prototype.getName(), classDefinitionJava);
        }
    }

    /*
     * (non-Javadoc)
     * @see org.warp.scripting.ScriptingEngine#exitContext()
     */
    @Override
    public void exitContext() {
        // exit the PHP context
        this._environment.exit();
        // close the PHP context
        this._environment.close();
        // cleanup
        this._environment = null;
        ENGINE.set(null);
    }

    /**
     * Returns the application we belong to.
     * 
     * @return
     *  The application we belong to.
     */
    public Application getApplication() {
        // return the application we belong to
        return this._application;
    }

    /**
     * Returns the the currently invoked function's PHP context.
     * 
     * @return 
     *  The PHP context of the currently invoked function or the latest invoked function, if no function is
     *  currently active.
     */
    public Env getEnvironment() {
        // return the currently invoked function's PHP context
        return this._environment;
    }

    /*
     * (non-Javadoc)
     * @see org.warp.scripting.ScriptingEngine#getProperty(java.lang.Object,
     * java.lang.String)
     */
    @Override
    public Object getProperty(final Object thisObject, final String propertyName) {
        if (thisObject == null || thisObject instanceof NodeInterface
                && ((NodeInterface) thisObject).getPrototype().equals("Global")) { //$NON-NLS-1$
            return this._environment.getGlobalValue(propertyName);
        } else if (thisObject instanceof HopObject) {
            return ((HopObject) thisObject).getFieldExt(this._environment,
                    this._environment.createString(propertyName));
        } else if (thisObject instanceof ArrayValueImpl) {
            return ((ArrayValueImpl) thisObject).get(Env.getCurrent().createString(propertyName));
        } else if (thisObject instanceof NodeInterface) {
            return new HopObject((NodeInterface) thisObject).getFieldExt(Env.getCurrent(), 
                    Env.getCurrent().createString(propertyName));
        }/* else if (thisObject instanceof Value[]) {
            final Value[] values = (Value[]) thisObject;
            if (values.length > 0 && values[0] instanceof HopObject) {
                final Object value = ((HopObject) values[0]).getFieldExt(
                        this._environment,
                        StringValue.create(propertyName).toStringValue())
                        .toJavaObject();
                if (value instanceof PropertyInterface) {
                    return ((PropertyInterface) value).getValue();
                }

                return value;
            }
        } else if (thisObject instanceof ArrayValue) {
            return ((ArrayValue) thisObject).get(StringValue.create(propertyName));
        }*/
        
        return null;
    }

    /**
     * Returns the Quercus PHP engine.
     * 
     * @return
     *  The Quercus PHP engine.
     */
    public Quercus getQuercus() {
        // return the Quercus PHP engine
        return this._quercus;
    }

    /**
     * Returns the request evaluator we belong to.
     * 
     * @return
     *  The request evaluator we belong to.
     */
    protected RequestEvaluator getRequestEvaluator() {
        // return the request evaluator we belong to
        return this._requestEvaluator;
    }

    /*
     * (non-Javadoc)
     * @see org.warp.scripting.ScriptingEngine#hasFunction(java.lang.Object,
     * java.lang.String, boolean)
     */
    @Override
    public boolean hasFunction(final Object thisObject, final String functionName, final boolean resolve) {
        QuercusClass classDefinition = null;

        if (thisObject == null || thisObject instanceof NodeInterface
                && ((NodeInterface) thisObject).getPrototype().equals("Global")) { //$NON-NLS-1$
            // no thisObject or an NodeInterface with prototype Global, look for a global function
            return this._environment.findFunction(this._environment.createString(functionName)) != null;
        } else if (thisObject instanceof HopObject) {
            // thisObject is a wrapped NodeInterface, look for a method of the corresponding class
            classDefinition = this._environment.getClass(((HopObject) thisObject).getNode().getPrototype());
        } else if (thisObject instanceof NodeInterface) {
            // thisObject is an unwrapped NodeInterface, look for a method of the corresponding class
            classDefinition = this._environment.getClass(((NodeInterface) thisObject).getPrototype());
        }

        boolean found = false;
        // follow the parent chain as long as possible and try to find the function (if not found above)
        while (!found && classDefinition != null) {
            found = classDefinition.findFunction(functionName) != null;
            if (!resolve) {
                return found;
            }

            // check for existance of parent class
            if (classDefinition.getParentName() != null) {
                classDefinition = this._environment.getClass(classDefinition.getParentName());
            } else {
                classDefinition = null;
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
    public boolean hasProperty(final Object thisObject, final String propertyName) {
        if (thisObject == null || thisObject instanceof NodeInterface
                && ((NodeInterface) thisObject).getPrototype().equals("Global")) { //$NON-NLS-1$
            // no thisObject or an NodeInterface with prototype Global, look for a global variable
            return this._environment.getGlobalValue(propertyName) != null;
        } else if (thisObject instanceof HopObject) {
            // thisObject is a wrapped NodeInterface, look for a value in the NodeInterface
            return ((HopObject) thisObject).getFieldExt(this._environment,
                    this._environment.createString(propertyName)) != null;
        } else if (thisObject instanceof ArrayValueImpl) {
            return ((ArrayValueImpl) thisObject).keyExists(Env.getCurrent().createString(propertyName));
        } else if (thisObject instanceof NodeInterface) {
            // thisObject is an unwrapped NodeInterface, look for a value in the NodeInterface
            return new HopObject((NodeInterface) thisObject).getFieldExt(Env.getCurrent(), 
                    Env.getCurrent().createString(propertyName)) != NullValue.NULL;
        }/* else if (thisObject instanceof Value[]) {
            // thisObject is an array of values
            final Value[] values = (Value[]) thisObject;
            // check if the first value is a wrapped NodeInterface
            if (values.length > 0 && values[0] instanceof HopObject) {
                // first value is a wrapped NodeInterface, look for a value in the NodeInterface
                return ((HopObject) values[0]).getFieldExt(this._environment,
                        StringValue.create(propertyName).toStringValue())
                        .toJavaObject() != null;
            }
        } else if (thisObject instanceof ArrayValue) {
            return ((ArrayValue) thisObject).keyExists(StringValue.create(propertyName));
        }*/

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
        // disable unicode support
        this._quercus.setUnicodeSemantics(false);

        // load all available php modules to be as close to native php as
        // possible
        // TODO: make it configurable
        this._quercus.addInitModule(new ApacheModule());
        this._quercus.addInitModule(new ApcModule());
        this._quercus.addInitModule(new ArrayModule());
        this._quercus.addInitModule(new BcmathModule());
        this._quercus.addInitModule(new ClassesModule());
        this._quercus.addInitModule(new CtypeModule());
        this._quercus.addInitModule(new DateModule());
        this._quercus.addInitModule(new ErrorModule());
        this._quercus.addInitModule(new ExifModule());
        this._quercus.addInitModule(new FileModule());
        this._quercus.addInitModule(new FunctionModule());
        this._quercus.addInitModule(new HashModule());
        this._quercus.addInitModule(new HtmlModule());
        this._quercus.addInitModule(new HttpModule());
        this._quercus.addInitModule(new JavaModule());
        this._quercus.addInitModule(new JMSModule());
        this._quercus.addInitModule(new JsonModule());
        this._quercus.addInitModule(new MailModule());
        this._quercus.addInitModule(new MathModule());
        this._quercus.addInitModule(new MbstringModule());
        this._quercus.addInitModule(new McryptModule());
        this._quercus.addInitModule(new MhashModule());
        this._quercus.addInitModule(new MiscModule());
        this._quercus.addInitModule(new MysqliModule());
        this._quercus.addInitModule(new MysqlModule());
        this._quercus.addInitModule(new NetworkModule());
        this._quercus.addInitModule(new OptionsModule());
        this._quercus.addInitModule(new OracleModule());
        this._quercus.addInitModule(new OutputModule());
        this._quercus.addInitModule(new PDFModule());
        this._quercus.addInitModule(new PDOModule());
        this._quercus.addInitModule(new PostgresModule());
        this._quercus.addInitModule(new QuercusDOMModule());
        this._quercus.addInitModule(new QuercusModule());
        this._quercus.addInitModule(new ReflectionModule());
        this._quercus.addInitModule(new RegexpModule());
        this._quercus.addInitModule(new SessionModule());
        this._quercus.addInitModule(new SimpleXMLModule());
        this._quercus.addInitModule(new SocketModule());
        this._quercus.addInitModule(new SplModule());
        this._quercus.addInitModule(new StreamModule());
        this._quercus.addInitModule(new StringModule());
        this._quercus.addInitModule(new TokenModule());
        this._quercus.addInitModule(new UnicodeModule());
        this._quercus.addInitModule(new VariableModule());
        this._quercus.addInitModule(new XmlModule());
        this._quercus.addInitModule(new ZipModule());
        this._quercus.addInitModule(new ZlibModule());

        // load our own module which provides some global functions
        this._quercus.addInitModule(new GlobalFunctions());
        
        // init the modules
        this._quercus.init();

        // activate php to java byte code compilation (only available in the pro engine, evaluation licenses 
        // are available, but I didn't figure out how to load them yet)
        // FIXME: how to activate it?
        // TODO: make it configurable
        this._quercus.setCompile(false);
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
                final HelmaExtensionInterface extension = (HelmaExtensionInterface) extensions
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
            final ResourceInterface resource) {
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
        if (thisObject != null && !(thisObject instanceof NodeInterface)) {
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
                return this._environment.call(StringValue.create((String) functionName).toStringValue()).toJavaObject();
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
        this._environment.setThis(new HopObject((NodeInterface) thisObject));

        // wrap the arguments (actually this is done to have the arguments a
        // little bit more unwrapped, than a direct
        // call to Env.wrapJava(arguments) would result in
        final Value[] parameters = new Value[arguments.length];
        for (int i = 0; i < arguments.length; i++) {
            parameters[i] = this._environment.wrapJava(arguments[i]);
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
            final Iterator<Map.Entry<String, Object>> globalObjects = globals.entrySet().iterator();
            while (globalObjects.hasNext()) {
                final Map.Entry<String, Object> globalObject = globalObjects.next();
                try {
                    // check type of global
                    if (globalObject.getValue() instanceof NodeInterface) {
                        // global is a NodeInterface, we need to wrap it as HopObject
                        final NodeInterface node = (NodeInterface) globalObject.getValue();
                        this._environment.setGlobalValue(globalObject.getKey(), new HopObject(node));
                    } else {
                        // global is something else, let Quercus do the default
                        // wrapping
                        this._environment.setGlobalValue(globalObject.getKey(),
                                this._environment.wrapJava(globalObject.getValue()));
                    }
                } catch (final QuercusException e) {
                    throw new ScriptingException(e.getMessage(), e);
                }
            }
        }
    }

    @Override
    public void shutdown() {
        this._quercus.close();
        this._quercus = null;
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

    /**
     * Returns a skin for the given name and class.
     * 
     * @param className
     *  The name of the class.
     * @param name
     *  The name of the skin.
     * @return
     *  The skin for the given name and class. 
     * 
     * @throws ScriptingException
     */
    public Skin getSkin(final String className, final String name) throws ScriptingException {
        final ResponseTrans response = this.getRequestEvaluator().getResponse();
        
        Skin skin;
        if (name.startsWith("#")) { //$NON-NLS-1$
            // evaluate relative subskin name against currently rendering skin
            skin = response.getActiveSkin();
            return skin == null ? null : skin.getSubskin(name.substring(1));
        }

        final Integer hashCode = Integer.valueOf(className.hashCode() + name.hashCode());
        skin = response.getCachedSkin(hashCode);

        if (skin == null) {
            // retrieve res.skinpath, an array of objects that tell us where to look for skins (strings for 
            // directory names and INodes for internal, db-stored skinsets)
            final Object[] skinpath = response.getSkinpath();
            try {
                skin = this.getApplication().getSkin(className, name, skinpath);
            } catch (final IOException e) {
                throw new ScriptingException(Messages.getString("QuercusEngine.6"), e); //$NON-NLS-1$
            }
            
            response.cacheSkin(hashCode, skin);
        }
        
        return skin;
    }

}