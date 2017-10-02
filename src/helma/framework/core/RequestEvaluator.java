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

package helma.framework.core;

import helma.framework.*;
import helma.objectmodel.*;
import helma.objectmodel.db.*;
import helma.scripting.*;
import java.lang.reflect.*;
import java.util.*;

import org.apache.xmlrpc.XmlRpcRequestProcessor;
import org.apache.xmlrpc.XmlRpcServerRequest;
import org.apache.commons.logging.Log;

/**
 * This class does the work for incoming requests. It holds a transactor thread
 * and an EcmaScript evaluator to get the work done. Incoming threads are
 * blocked until the request has been serviced by the evaluator, or the timeout
 * specified by the application has passed. In the latter case, the evaluator thread
 * is killed and an error message is returned.
 */
public final class RequestEvaluator implements Runnable {
    static final int NONE = 0; // no request
    static final int HTTP = 1; // via HTTP gateway
    static final int XMLRPC = 2; // via XML-RPC
    static final int INTERNAL = 3; // generic function call, e.g. by scheduler
    static final int EXTERNAL = 4; // function from script etc

    public static final Object[] EMPTY_ARGS = new Object[0];

    public final Application app;

    protected ScriptingEngineInterface scriptingEngine;

    // skin depth counter, used to avoid recursive skin rendering
    protected int skinDepth;

    private volatile RequestTrans req;
    private volatile ResponseTrans res;

    // the one and only transactor thread
    private volatile Thread thread;

    private volatile Transactor transactor;

    // the type of request to be serviced,
    // used to coordinate worker and waiter threads
    private volatile int reqtype;

    // the object on which to invoke a function, if specified
    private volatile Object thisObject;

    // the method to be executed
    private volatile Object function;

    // the session object associated with the current request
    private volatile Session session;

    // arguments passed to the function
    private volatile Object[] args;

    // the result of the operation
    private volatile Object result;

    // the exception thrown by the evaluator, if any.
    private volatile Exception exception;

    // For numbering threads.
    private int threadId;


    /**
     *  Create a new RequestEvaluator for this application.
     *  @param app the application
     */
    public RequestEvaluator(Application app) {
        this.app = app;
    }

    protected synchronized void initScriptingEngine() {
        if (this.scriptingEngine == null) {
            String engineClassName = this.app.getProperty("scriptingEngine", //$NON-NLS-1$
                                                     "helma.scripting.rhino.RhinoEngine"); //$NON-NLS-1$
            try {
                this.app.setCurrentRequestEvaluator(this);
                Class clazz = this.app.getClassLoader().loadClass(engineClassName);

                this.scriptingEngine = (ScriptingEngineInterface) clazz.newInstance();
                this.scriptingEngine.init(this.app, this);
            } catch (Exception x) {
                Throwable t = x;

                if (x instanceof InvocationTargetException) {
                    t = ((InvocationTargetException) x).getTargetException();
                }

                this.app.logEvent(Messages.getString("RequestEvaluator.0")); //$NON-NLS-1$
                this.app.logEvent(Messages.getString("RequestEvaluator.1")); //$NON-NLS-1$
                this.app.logEvent(Messages.getString("RequestEvaluator.2") + t.toString()); //$NON-NLS-1$
                this.app.logEvent(Messages.getString("RequestEvaluator.3")); //$NON-NLS-1$
                this.app.logError(Messages.getString("RequestEvaluator.4"), t); //$NON-NLS-1$

                // null out invalid scriptingEngine
                this.scriptingEngine = null;
                // rethrow exception
                if (t instanceof RuntimeException) {
                    throw((RuntimeException) t);
                }
                throw new RuntimeException(t.toString(), t);
            } finally {
                this.app.setCurrentRequestEvaluator(null);
            }
        }
    }

    protected synchronized void shutdown() {
        if (this.scriptingEngine != null) {
            this.scriptingEngine.shutdown();
        }
    }

    /**
     *
     */
    public void run() {
        // first, set a local variable to the current transactor thread so we know
        // when it's time to quit because another thread took over.
        Thread localThread = Thread.currentThread();

        // spans whole execution loop - close connections in finally clause
        try {

            // while this thread is serving requests
            while (localThread == this.thread) {

                // object reference to ressolve request path
                Object currentElement;

                // Get req and res into local variables to avoid memory caching problems
                // in unsynchronized method.
                RequestTrans req = getRequest();
                ResponseTrans res = getResponse();

                // request path object
                RequestPath requestPath = new RequestPath(this.app);

                String txname = req.getMethod() + ":" + req.getPath(); //$NON-NLS-1$
                Log eventLog = this.app.getEventLog();
                if (eventLog.isDebugEnabled()) {
                    eventLog.debug(txname + Messages.getString("RequestEvaluator.5")); //$NON-NLS-1$
                }

                int tries = 0;
                boolean done = false;
                Throwable error = null;
                String functionName = this.function instanceof String ?
                        (String) this.function : null;

                while (!done && localThread == this.thread) {
                    // catch errors in path resolution and script execution
                    try {

                        // initialize scripting engine
                        initScriptingEngine();
                        this.app.setCurrentRequestEvaluator(this);
                        // update scripting prototypes
                        this.scriptingEngine.enterContext();


                        // avoid going into transaction if called function doesn't exist.
                        // this only works for the (common) case that method is a plain
                        // method name, not an obj.method path
                        if (this.reqtype == INTERNAL) {
                            // if object is an instance of NodeHandle, get the node object itself.
                            if (this.thisObject instanceof NodeHandle) {
                                this.thisObject = ((NodeHandle) this.thisObject).getNode(this.app.nmgr.safe);
                                // If no valid node object return immediately
                                if (this.thisObject == null) {
                                    done = true;
                                    this.reqtype = NONE;
                                    break;
                                }
                            }
                            // If function doesn't exist, return immediately
                            if (functionName != null && !this.scriptingEngine.hasFunction(this.thisObject, functionName, true)) {
                                this.app.logEvent(missingFunctionMessage(this.thisObject, functionName));
                                done = true;
                                this.reqtype = NONE;
                                break;
                            }
                        } else if (this.function != null && functionName == null) {
                            // only internal requests may pass a function instead of a function name
                            throw new IllegalStateException(Messages.getString("RequestEvaluator.6")); //$NON-NLS-1$
                        }

                        // Update transaction name in case we're processing an error
                        if (error != null) {
                            txname = "error:" + txname; //$NON-NLS-1$
                        }

                        // begin transaction
                        this.transactor = Transactor.getInstance(this.app.nmgr);
                        this.transactor.begin(txname);

                        Object root = this.app.getDataRoot(this.scriptingEngine);
                        initGlobals(root, requestPath);

                        String action = null;

                        if (error != null) {
                            res.setError(error);
                        }

                        switch (this.reqtype) {
                            case HTTP:

                                // bring over the message from a redirect
                                this.session.recoverResponseMessages(res);

                                // catch redirect in path resolution or script execution
                                try {
                                    // catch object not found in path resolution
                                    try {
                                        if (error != null) {
                                            // there was an error in the previous loop, call error handler
                                            currentElement = root;
                                            res.setStatus(500);

                                            // do not reset the requestPath so error handler can use the original one
                                            // get error handler action
                                            String errorAction = this.app.props.getProperty("error", //$NON-NLS-1$
                                                    "error"); //$NON-NLS-1$

                                            action = getAction(currentElement, errorAction, req);

                                            if (action == null) {
                                                throw new RuntimeException(error);
                                            }
                                        } else if ((req.getPath() == null) ||
                                                "".equals(req.getPath().trim())) { //$NON-NLS-1$
                                            currentElement = root;
                                            requestPath.add(null, currentElement);

                                            action = getAction(currentElement, null, req);

                                            if (action == null) {
                                                throw new NotFoundException(Messages.getString("RequestEvaluator.7")); //$NON-NLS-1$
                                            }
                                        } else {
                                            // march down request path...
                                            StringTokenizer st = new StringTokenizer(req.getPath(),
                                                    "/"); //$NON-NLS-1$
                                            int ntokens = st.countTokens();

                                            // limit path to < 50 tokens
                                            if (ntokens > 50) {
                                                throw new RuntimeException(Messages.getString("RequestEvaluator.8")); //$NON-NLS-1$
                                            }

                                            String[] pathItems = new String[ntokens];

                                            for (int i = 0; i < ntokens; i++)
                                                pathItems[i] = st.nextToken();

                                            currentElement = root;
                                            requestPath.add(null, currentElement);

                                            for (int i = 0; i < ntokens; i++) {
                                                if (currentElement == null) {
                                                    throw new NotFoundException(Messages.getString("RequestEvaluator.9")); //$NON-NLS-1$
                                                }

                                                if (pathItems[i].length() == 0) {
                                                    continue;
                                                }

                                                // if we're at the last element of the path,
                                                // try to interpret it as action name.
                                                if (i == (ntokens - 1) && !req.getPath().endsWith("/")) { //$NON-NLS-1$
                                                    action = getAction(currentElement, pathItems[i], req);
                                                }

                                                if (action == null) {
                                                    currentElement = getChildElement(currentElement,
                                                            pathItems[i]);

                                                    // add object to request path if suitable
                                                    if (currentElement != null) {
                                                        // add to requestPath array
                                                        requestPath.add(pathItems[i], currentElement);
                                                    }
                                                }
                                            }

                                            if (currentElement == null) {
                                                throw new NotFoundException(Messages.getString("RequestEvaluator.10")); //$NON-NLS-1$
                                            }

                                            if (action == null) {
                                                action = getAction(currentElement, null, req);
                                            }

                                            if (action == null) {
                                                throw new NotFoundException(Messages.getString("RequestEvaluator.11")); //$NON-NLS-1$
                                            }
                                        }
                                    } catch (NotFoundException notfound) {
                                        if (error != null) {

                                            // we already have an error and the error template wasn't found,
                                            // display it instead of notfound message
                                            throw new RuntimeException();
                                        }

                                        // The path could not be resolved. Check if there is a "not found" action
                                        // specified in the property file.
                                        res.setStatus(404);

                                        String notFoundAction = this.app.props.getProperty("notfound", //$NON-NLS-1$
                                                "notfound"); //$NON-NLS-1$

                                        currentElement = root;
                                        action = getAction(currentElement, notFoundAction, req);

                                        if (action == null) {
                                            throw new NotFoundException(notfound.getMessage());
                                        }
                                    }

                                    // register path objects with their prototype names in
                                    // res.handlers
                                    Map macroHandlers = res.getMacroHandlers();
                                    int l = requestPath.size();
                                    Prototype[] protos = new Prototype[l];

                                    for (int i = 0; i < l; i++) {

                                        Object obj = requestPath.get(i);

                                        protos[i] = this.app.getPrototype(obj);

                                        // immediately register objects with their direct prototype name
                                        if (protos[i] != null) {
                                            macroHandlers.put(protos[i].getName(), obj);
                                            macroHandlers.put(protos[i].getLowerCaseName(), obj);
                                        }
                                    }

                                    // in a second pass, we register path objects with their indirect
                                    // (i.e. parent prototype) names, starting at the end and only
                                    // if the name isn't occupied yet.
                                    for (int i = l - 1; i >= 0; i--) {
                                        if (protos[i] != null) {
                                            protos[i].registerParents(macroHandlers, requestPath.get(i));
                                        }
                                    }

                                    /////////////////////////////////////////////////////////////////////////////
                                    // end of path resolution section
                                    /////////////////////////////////////////////////////////////////////////////
                                    // beginning of execution section

                                    // set the req.action property, cutting off the _action suffix
                                    req.setAction(action);

                                    // reset skin recursion detection counter
                                    this.skinDepth = 0;

                                    // try calling onRequest() function on object before
                                    // calling the actual action
                                    this.scriptingEngine.invoke(currentElement,
                                            "onRequest", //$NON-NLS-1$
                                            EMPTY_ARGS,
                                            ScriptingEngineInterface.ARGS_WRAP_DEFAULT,
                                            false);

                                    // reset skin recursion detection counter
                                    this.skinDepth = 0;

                                    Object actionProcessor = req.getActionHandler() != null ?
                                        req.getActionHandler() : action;

                                    // do the actual action invocation
                                    if (req.isXmlRpc()) {
                                        XmlRpcRequestProcessor xreqproc = new XmlRpcRequestProcessor();
                                        XmlRpcServerRequest xreq = xreqproc.decodeRequest(req.getServletRequest()
                                                .getInputStream());
                                        Vector args = xreq.getParameters();
                                        args.add(0, xreq.getMethodName());
                                        this.result = this.scriptingEngine.invoke(currentElement,
                                                actionProcessor,
                                                args.toArray(),
                                                ScriptingEngineInterface.ARGS_WRAP_XMLRPC,
                                                false);
                                        res.writeXmlRpcResponse(this.result);
                                        this.app.xmlrpcCount += 1;
                                    } else {
                                        this.scriptingEngine.invoke(currentElement,
                                                actionProcessor,
                                                EMPTY_ARGS,
                                                ScriptingEngineInterface.ARGS_WRAP_DEFAULT,
                                                false);
                                    }

                                    // try calling onResponse() function on object before
                                    // calling the actual action
                                    this.scriptingEngine.invoke(currentElement,
                                            "onResponse", //$NON-NLS-1$
                                            EMPTY_ARGS,
                                            ScriptingEngineInterface.ARGS_WRAP_DEFAULT,
                                            false);

                                } catch (RedirectException redirect) {
                                    // if there is a message set, save it on the user object for the next request
                                    if (res.getRedirect() != null)
                                        this.session.storeResponseMessages(res);
                                }

                                // check if request is still valid, or if the requesting thread has stopped waiting already
                                if (localThread != this.thread) {
                                    return;
                                }
                                commitTransaction();
                                done = true;

                                break;

                            case XMLRPC:
                            case EXTERNAL:

                                try {
                                    currentElement = root;

                                    if (functionName.indexOf('.') > -1) {
                                        StringTokenizer st = new StringTokenizer(functionName, "."); //$NON-NLS-1$
                                        int cnt = st.countTokens();

                                        for (int i = 1; i < cnt; i++) {
                                            String next = st.nextToken();
                                            currentElement = getChildElement(currentElement, next);
                                        }

                                        if (currentElement == null) {
                                            throw new NotFoundException(Messages.getString("RequestEvaluator.12") + //$NON-NLS-1$
                                                    this.function + Messages.getString("RequestEvaluator.13")); //$NON-NLS-1$
                                        }

                                        functionName = st.nextToken();
                                    }

                                    if (this.reqtype == XMLRPC) {
                                        // check XML-RPC access permissions
                                        String proto = this.app.getPrototypeName(currentElement);
                                        this.app.checkXmlRpcAccess(proto, functionName);
                                    }

                                    // reset skin recursion detection counter
                                    this.skinDepth = 0;
                                    if (!this.scriptingEngine.hasFunction(currentElement, functionName, false)) {
                                        throw new NotFoundException(missingFunctionMessage(currentElement, functionName));
                                    }
                                    this.result = this.scriptingEngine.invoke(currentElement,
                                            functionName, this.args,
                                            ScriptingEngineInterface.ARGS_WRAP_XMLRPC,
                                            false);
                                    // check if request is still valid, or if the requesting thread has stopped waiting already
                                    if (localThread != this.thread) {
                                        return;
                                    }
                                    commitTransaction();
                                } catch (Exception x) {
                                    // check if request is still valid, or if the requesting thread has stopped waiting already
                                    if (localThread != this.thread) {
                                        return;
                                    }
                                    abortTransaction();
                                    this.app.logError(txname + " " + error, x); //$NON-NLS-1$

                                    // If the transactor thread has been killed by the invoker thread we don't have to
                                    // bother for the error message, just quit.
                                    if (localThread != this.thread) {
                                        return;
                                    }

                                    this.exception = x;
                                }

                                done = true;
                                break;

                            case INTERNAL:

                                try {
                                    // reset skin recursion detection counter
                                    this.skinDepth = 0;

                                    this.result = this.scriptingEngine.invoke(this.thisObject,
                                            this.function,
                                            this.args,
                                            ScriptingEngineInterface.ARGS_WRAP_DEFAULT,
                                            true);
                                    // check if request is still valid, or if the requesting thread has stopped waiting already
                                    if (localThread != this.thread) {
                                        return;
                                    }
                                    commitTransaction();
                                } catch (Exception x) {
                                    // check if request is still valid, or if the requesting thread has stopped waiting already
                                    if (localThread != this.thread) {
                                        return;
                                    }
                                    abortTransaction();
                                    this.app.logError(txname + " " + error, x); //$NON-NLS-1$

                                    // If the transactor thread has been killed by the invoker thread we don't have to
                                    // bother for the error message, just quit.
                                    if (localThread != this.thread) {
                                        return;
                                    }

                                    this.exception = x;
                                }

                                done = true;
                                break;

                        } // switch (reqtype)
                    } catch (AbortException x) {
                        // res.abort() just aborts the transaction and
                        // leaves the response untouched
                        // check if request is still valid, or if the requesting thread has stopped waiting already
                        if (localThread != this.thread) {
                            return;
                        }
                        abortTransaction();
                        done = true;
                    } catch (ConcurrencyException x) {
                        res.reset();

                        if (++tries < 8) {
                            // try again after waiting some period
                            // check if request is still valid, or if the requesting thread has stopped waiting already
                            if (localThread != this.thread) {
                                return;
                            }
                            abortTransaction();

                            try {
                                // wait a bit longer with each try
                                int base = 800 * tries;
                                Thread.sleep((long) (base + (Math.random() * base * 2)));
                            } catch (InterruptedException interrupt) {
                                // we got interrrupted, create minimal error message
                                res.reportError(interrupt);
                                done = true;
                                // and release resources and thread
                                this.thread = null;
                                this.transactor = null;
                            }
                        } else {
                            // check if request is still valid, or if the requesting thread has stopped waiting already
                            if (localThread != this.thread) {
                                return;
                            }
                            abortTransaction();

                            // error in error action. use traditional minimal error message
                            res.reportError(Messages.getString("RequestEvaluator.14")); //$NON-NLS-1$
                            done = true;
                        }
                    } catch (Throwable x) {
                        // check if request is still valid, or if the requesting thread has stopped waiting already
                        if (localThread != this.thread) {
                            return;
                        }
                        abortTransaction();

                        // If the transactor thread has been killed by the invoker thread we don't have to
                        // bother for the error message, just quit.
                        if (localThread != this.thread) {
                            return;
                        }

                        res.reset();

                        // check if we tried to process the error already,
                        // or if this is an XML-RPC request
                        if (error == null) {
                            if (!(x instanceof NotFoundException)) {
                                this.app.errorCount += 1;
                            }

                            // set done to false so that the error will be processed
                            done = false;
                            error = x;

                            this.app.logError(txname + " " + error, x); //$NON-NLS-1$

                            if (req.isXmlRpc()) {
                                // if it's an XML-RPC exception immediately generate error response
                                if (!(x instanceof Exception)) {
                                    // we need an exception to pass to XML-RPC responder
                                    x = new Exception(x.toString(), x);
                                }
                                res.writeXmlRpcError((Exception) x);
                                done = true;
                            }
                        } else {
                            // error in error action. use traditional minimal error message
                            res.reportError(error);
                            done = true;
                        }
                    } finally {
                        this.app.setCurrentRequestEvaluator(null);
                        // exit execution context
                        if (this.scriptingEngine != null) {
                            try {
                                this.scriptingEngine.exitContext();
                            } catch (Throwable t) {
                                // broken rhino, just get out of here
                            }
                        }
                    }
                }

                notifyAndWait();

            }
        } finally {
            Transactor tx = Transactor.getInstance();
            if (tx != null) tx.closeConnections();
        }
    }

    /**
     * Called by the transactor thread when it has successfully fulfilled a request.
     * @throws Exception transaction couldn't be committed
     */
    synchronized void commitTransaction() throws Exception {
        Thread localThread = Thread.currentThread();

        if (localThread == this.thread) {
            Transactor tx = Transactor.getInstance();
            if (tx != null)
                tx.commit();
        } else {
            throw new TimeoutException();
        }
    }

    /**
     * Called by the transactor thread when the request didn't terminate successfully.
     */
    synchronized void abortTransaction() {
        Transactor tx = Transactor.getInstance();
        if (tx != null) tx.abort();
    }

    /**
     * Initialize and start the transactor thread.
     */
    private synchronized void startTransactor() {
        if (!this.app.isRunning()) {
            throw new ApplicationStoppedException();
        }

        if ((this.thread == null) || !this.thread.isAlive()) {
            // app.logEvent ("Starting Thread");
            this.thread = new Thread(this.app.threadgroup, this, this.app.getName() + "-" + (++this.threadId)); //$NON-NLS-1$
            this.thread.setContextClassLoader(this.app.getClassLoader());
            this.thread.start();
        } else {
            notifyAll();
        }
    }

    /**
     * Tell waiting thread that we're done, then wait for next request
     */
    synchronized void notifyAndWait() {
        Thread localThread = Thread.currentThread();

        // make sure there is only one thread running per instance of this class
        // if localrtx != rtx, the current thread has been aborted and there's no need to notify
        if (localThread != this.thread) {
            // A new request came in while we were finishing the last one.
            // Return to run() to get the work done.
            Transactor tx = Transactor.getInstance();
            if (tx != null) {
                tx.closeConnections();
            }
            return;
        }

        this.reqtype = NONE;
        notifyAll();

        try {
            // wait for request, max 10 min
            wait(1000 * 60 * 10);
        } catch (InterruptedException ix) {
            // we got interrrupted, releases resources and thread
            this.thread = null;
            this.transactor = null;
        }

        //  if no request arrived, release ressources and thread
        if ((this.reqtype == NONE) && (this.thread == localThread)) {
            // comment this in to release not just the thread, but also the scripting engine.
            // currently we don't do this because of the risk of memory leaks (objects from
            // framework referencing into the scripting engine)
            // scriptingEngine = null;
            this.thread = null;
            this.transactor = null;
        }
    }

    /**
     * Stop this request evaluator's current thread. This is called by the
     * waiting thread when it times out and stops waiting, or from an outside
     * thread. If currently active kill the request, otherwise just notify.
     */
    synchronized boolean stopTransactor() {
        Transactor t = this.transactor;
        this.thread = null;
        this.transactor = null;
        boolean stopped = false;
        if (t != null && t.isActive()) {
            // let the scripting engine know that the
            // current transaction is being aborted.
            if (this.scriptingEngine != null) {
                this.scriptingEngine.abort();
            }

            this.app.logEvent(Messages.getString("RequestEvaluator.15") + t); //$NON-NLS-1$

            this.reqtype = NONE;

            t.kill();
            t.abort();
            t.closeConnections();
            stopped = true;
        }
        notifyAll();
        return stopped;
    }

    /**
     * Invoke an action function for a HTTP request. The function is dispatched
     * in a new thread and waits for it to finish.
     *
     * @param req the incoming HTTP request
     * @param session the client's session
     * @return the result returned by the invocation
     * @throws Exception any exception thrown by the invocation
     */
    public synchronized ResponseTrans invokeHttp(RequestTrans req, Session session)
                                      throws Exception {
        initObjects(req, session);

        this.app.activeRequests.put(req, this);

        startTransactor();
        wait(this.app.requestTimeout);

        if (this.reqtype != NONE && stopTransactor()) {
            this.res.reset();
            this.res.reportError(Messages.getString("RequestEvaluator.16")); //$NON-NLS-1$
        }

        session.commit(this.app.sessionMgr);
        return this.res;
    }

    /**
     * This checks if the Evaluator is already executing an equal request.
     * If so, attach to it and wait for it to complete. Otherwise return null,
     * so the application knows it has to run the request.
     */
    public synchronized ResponseTrans attachHttpRequest(RequestTrans req)
                                             throws Exception {
        // Get a reference to the res object at the time we enter
        ResponseTrans localRes = this.res;

        if (localRes == null || !req.equals(this.req)) {
            return null;
        }

        if (this.reqtype != NONE) {
            wait(this.app.requestTimeout);
        }

        return localRes;
    }

    /*
     * TODO invokeXmlRpc(), invokeExternal() and invokeInternal() are basically the same
     * and should be unified
     */

    /**
     * Invoke a function for an XML-RPC request. The function is dispatched in a new thread
     * and waits for it to finish.
     *
     * @param functionName the name of the function to invoke
     * @param args the arguments
     * @return the result returned by the invocation
     * @throws Exception any exception thrown by the invocation
     */
    public synchronized Object invokeXmlRpc(String functionName, Object[] args)
                                     throws Exception {
        initObjects(functionName, XMLRPC, RequestTrans.XMLRPC);
        this.function = functionName;
        this.args = args;

        startTransactor();
        wait(this.app.requestTimeout);

        if (this.reqtype != NONE && stopTransactor()) {
            this.exception = new RuntimeException(Messages.getString("RequestEvaluator.17")); //$NON-NLS-1$
        }

        // reset res for garbage collection (res.data may hold reference to evaluator)
        this.res = null;

        if (this.exception != null) {
            throw (this.exception);
        }

        return this.result;
    }



    /**
     * Invoke a function for an external request. The function is dispatched
     * in a new thread and waits for it to finish.
     *
     * @param functionName the name of the function to invoke
     * @param args the arguments
     * @return the result returned by the invocation
     * @throws Exception any exception thrown by the invocation
     */
    public synchronized Object invokeExternal(String functionName, Object[] args)
                                     throws Exception {
        initObjects(functionName, EXTERNAL, RequestTrans.EXTERNAL);
        this.function = functionName;
        this.args = args;

        startTransactor();
        wait();

        if (this.reqtype != NONE && stopTransactor()) {
            this.exception = new RuntimeException(Messages.getString("RequestEvaluator.18")); //$NON-NLS-1$
        }

        // reset res for garbage collection (res.data may hold reference to evaluator)
        this.res = null;

        if (this.exception != null) {
            throw (this.exception);
        }

        return this.result;
    }

    /**
     * Invoke a function internally and directly, using the thread we're running on.
     *
     * @param obj the object to invoke the function on
     * @param function the function or name of the function to invoke
     * @param args the arguments
     * @return the result returned by the invocation
     * @throws Exception any exception thrown by the invocation
     */
    public Object invokeDirectFunction(Object obj, Object function, Object[] args)
                                throws Exception {
        return this.scriptingEngine.invoke(obj, function, args,
                ScriptingEngineInterface.ARGS_WRAP_DEFAULT, true);
    }

    /**
     * Invoke a function internally. The function is dispatched in a new thread
     * and waits for it to finish.
     *
     * @param object the object to invoke the function on
     * @param function the function or name of the function to invoke
     * @param args the arguments
     * @return the result returned by the invocation
     * @throws Exception any exception thrown by the invocation
     */
    public synchronized Object invokeInternal(Object object, Object function,
                                              Object[] args)
                                       throws Exception {
        // give internal call more time (15 minutes) to complete
        return invokeInternal(object, function, args, 60000L * 15);
    }

    /**
     * Invoke a function internally. The function is dispatched in a new thread
     * and waits for it to finish.
     *
     * @param object the object to invoke the function on
     * @param function the function or name of the function to invoke
     * @param args the arguments
     * @param timeout the time in milliseconds to wait for the function to return, or
     * -1 to wait indefinitely
     * @return the result returned by the invocation
     * @throws Exception any exception thrown by the invocation
     */
    public synchronized Object invokeInternal(Object object, Object function,
                                              Object[] args, long timeout)
                                       throws Exception {
        initObjects(function, INTERNAL, RequestTrans.INTERNAL);
        this.thisObject = object;
        this.function = function;
        this.args = args;

        startTransactor();
        if (timeout < 0)
            wait();
        else
            wait(timeout);

        if (this.reqtype != NONE && stopTransactor()) {
            this.exception = new RuntimeException(Messages.getString("RequestEvaluator.19")); //$NON-NLS-1$
        }

        // reset res for garbage collection (res.data may hold reference to evaluator)
        this.res = null;

        if (this.exception != null) {
            throw (this.exception);
        }

        return this.result;
    }


    /**
     * Init this evaluator's objects from a RequestTrans for a HTTP request
     *
     * @param req
     * @param session
     */
    private synchronized void initObjects(RequestTrans req, Session session) {
        this.req = req;
        this.reqtype = HTTP;
        this.session = session;
        this.res = new ResponseTrans(this.app, req);
        this.result = null;
        this.exception = null;
    }

    /**
     * Init this evaluator's objects for an internal, external or XML-RPC type
     * request.
     *
     * @param function the function name or object
     * @param reqtype the request type
     * @param reqtypeName the request type name
     */
    private synchronized void initObjects(Object function, int reqtype, String reqtypeName) {
        this.reqtype = reqtype;
        String functionName = function instanceof String ?
                (String) function : "<function>"; //$NON-NLS-1$
        this.req = new RequestTrans(reqtypeName, functionName);
        this.session = new Session(functionName, this.app);
        this.res = new ResponseTrans(this.app, this.req);
        this.result = null;
        this.exception = null;
    }

    /**
     * Initialize the globals in the scripting engine for the current request.
     *
     * @param root
     * @throws ScriptingException
     */
    private synchronized void initGlobals(Object root, Object requestPath)
                throws ScriptingException {
        HashMap globals = new HashMap();

        globals.put("root", root); //$NON-NLS-1$
        globals.put("session", new SessionBean(this.session)); //$NON-NLS-1$
        globals.put("req", new RequestBean(this.req)); //$NON-NLS-1$
        globals.put("res", new ResponseBean(this.res)); //$NON-NLS-1$
        globals.put("app", new ApplicationBean(this.app)); //$NON-NLS-1$
        globals.put("path", requestPath); //$NON-NLS-1$

        // enter execution context
        this.scriptingEngine.setGlobals(globals);
    }

    /**
     * Get the child element with the given name from the given object.
     *
     * @param obj
     * @param name
     * @return
     * @throws ScriptingException
     */
    private Object getChildElement(Object obj, String name) throws ScriptingException {
        if (this.scriptingEngine.hasFunction(obj, "getChildElement", false)) { //$NON-NLS-1$
            return this.scriptingEngine.invoke(obj, "getChildElement", new Object[] {name}, //$NON-NLS-1$
                                          ScriptingEngineInterface.ARGS_WRAP_DEFAULT, false);
        }

        if (obj instanceof PathElementInterface) {
            return ((PathElementInterface) obj).getChildElement(name);
        }

        return null;
    }

    /**
     *  Null out some fields, mostly for the sake of garbage collection.
     */
    synchronized void recycle() {
        this.res = null;
        this.req = null;
        this.session = null;
        this.function = null;
        this.args = null;
        this.result = null;
        this.exception = null;
    }

    /**
     * Check if an action with a given name is defined for a scripted object. If it is,
     * return the action's function name. Otherwise, return null.
     */
    public String getAction(Object obj, String action, RequestTrans req) {
        if (obj == null)
            return null;

        if (action == null)
            action = "main"; //$NON-NLS-1$

        StringBuffer buffer = new StringBuffer(action).append("_action"); //$NON-NLS-1$
        // record length so we can check without method
        // afterwards for GET, POST, HEAD requests
        int length = buffer.length();

        if (req.checkXmlRpc()) {
            // append _methodname
            buffer.append("_xmlrpc"); //$NON-NLS-1$
            if (this.scriptingEngine.hasFunction(obj, buffer.toString(), false)) {
                // handle as XML-RPC request
                req.setMethod(RequestTrans.XMLRPC);
                return buffer.toString();
            }
            // cut off method in case it has been appended
            buffer.setLength(length);
        }

        String method = req.getMethod();
        // append HTTP method to action name
        if (method != null) {
            // append _methodname
            buffer.append('_').append(method.toLowerCase());
            if (this.scriptingEngine.hasFunction(obj, buffer.toString(), false))
                return buffer.toString();

            // cut off method in case it has been appended
            buffer.setLength(length);
        }

        // if no method specified or "ordinary" request try action without method
        if (method == null || "GET".equalsIgnoreCase(method) || //$NON-NLS-1$
                              "POST".equalsIgnoreCase(method) || //$NON-NLS-1$
                              "HEAD".equalsIgnoreCase(method)) { //$NON-NLS-1$
            if (this.scriptingEngine.hasFunction(obj, buffer.toString(), false))
                return buffer.toString();
        }

        return null;
    }

    /**
     * Returns this evaluator's scripting engine
     */
    public ScriptingEngineInterface getScriptingEngine() {
        if (this.scriptingEngine == null) {
            initScriptingEngine();
        }
        return this.scriptingEngine;
    }

    /**
     * Get the request object for the current request.
     *
     * @return the request object
     */
    public synchronized RequestTrans getRequest() {
        return this.req;
    }

    /**
     * Get the response object for the current request.
     *
     * @return the response object
     */
    public synchronized ResponseTrans getResponse() {
        return this.res;
    }

    /**
     * Get the current transactor thread
     *
     * @return the current transactor thread
     */
    public synchronized Thread getThread() {
        return this.thread;
    }

    /**
     * Return the current session
     *
     * @return the session for the current request
     */
    public synchronized Session getSession() {
        return this.session;
    }

    private String missingFunctionMessage(Object obj, String funcName) {
        if (obj == null)
            return Messages.getString("RequestEvaluator.20") + funcName + Messages.getString("RequestEvaluator.21"); //$NON-NLS-1$ //$NON-NLS-2$
        return Messages.getString("RequestEvaluator.22") + funcName + Messages.getString("RequestEvaluator.23") + obj; //$NON-NLS-1$ //$NON-NLS-2$
    }
}
