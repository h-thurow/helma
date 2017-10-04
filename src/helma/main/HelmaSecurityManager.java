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

package helma.main;

import java.io.FileDescriptor;
import java.net.InetAddress;
import java.security.Permission;
import java.util.HashSet;

import helma.framework.core.AppClassLoader;

/**
 *  Liberal security manager for Helma system that makes sure application code
 *  is not allowed to exit the VM and set a security manager.
 *
 *  This class can be subclassed to implement actual security policies. It contains
 *  a utility method <code>getApplication</code> that can be used to determine
 *  the name of the application trying to execute the action in question, if any.
 */
public class HelmaSecurityManager extends SecurityManager {
    // The set of actions forbidden to application code.
    // We are pretty permissive, forbidding only System.exit() 
    // and setting the security manager.
    private final static HashSet forbidden = new HashSet();

    static {
        forbidden.add("exitVM"); //$NON-NLS-1$
        forbidden.add("setSecurityManager"); //$NON-NLS-1$
    }

    /**
     *
     *
     * @param p ...
     */
    @Override
    public void checkPermission(Permission p) {
        if (p instanceof RuntimePermission) {
            if (forbidden.contains(p.getName())) {
                Class[] classes = getClassContext();

                for (int i = 0; i < classes.length; i++) {
                    if (classes[i].getClassLoader() instanceof AppClassLoader) {
                        throw new SecurityException(p.getName() +
                                                    Messages.getString("HelmaSecurityManager.0")); //$NON-NLS-1$
                    }
                }
            }
        }
    }

    /**
     *
     *
     * @param p ...
     * @param context ...
     */
    @Override
    public void checkPermission(Permission p, Object context) {
    }

    /**
     *
     */
    @Override
    public void checkCreateClassLoader() {
    }

    /**
     *
     *
     * @param thread ...
     */
    @Override
    public void checkAccess(Thread thread) {
    }

    /**
     *
     *
     * @param group ...
     */
    @Override
    public void checkAccess(ThreadGroup group) {
    }

    /**
     *
     *
     * @param status ...
     */
    @Override
    public void checkExit(int status) {
        Class[] classes = getClassContext();

        for (int i = 0; i < classes.length; i++) {
            if (classes[i].getClassLoader() instanceof AppClassLoader) {
                throw new SecurityException(Messages.getString("HelmaSecurityManager.1")); //$NON-NLS-1$
            }
        }
    }

    /**
     *
     *
     * @param cmd ...
     */
    @Override
    public void checkExec(String cmd) {
    }

    /**
     *
     *
     * @param lib ...
     */
    @Override
    public void checkLink(String lib) {
    }

    /**
     *
     *
     * @param fdesc ...
     */
    @Override
    public void checkRead(FileDescriptor fdesc) {
    }

    /**
     *
     *
     * @param file ...
     */
    @Override
    public void checkRead(String file) {
    }

    /**
     *
     *
     * @param file ...
     * @param context ...
     */
    @Override
    public void checkRead(String file, Object context) {
    }

    /**
     *
     *
     * @param fdesc ...
     */
    @Override
    public void checkWrite(FileDescriptor fdesc) {
    }

    /**
     *
     *
     * @param file ...
     */
    @Override
    public void checkWrite(String file) {
    }

    /**
     *
     *
     * @param file ...
     */
    @Override
    public void checkDelete(String file) {
    }

    /**
     *
     *
     * @param host ...
     * @param port ...
     */
    @Override
    public void checkConnect(String host, int port) {
    }

    /**
     *
     *
     * @param host ...
     * @param port ...
     * @param context ...
     */
    @Override
    public void checkConnect(String host, int port, Object context) {
    }

    /**
     *
     *
     * @param port ...
     */
    @Override
    public void checkListen(int port) {
    }

    /**
     *
     *
     * @param host ...
     * @param port ...
     */
    @Override
    public void checkAccept(String host, int port) {
    }

    /**
     *
     *
     * @param addr ...
     */
    @Override
    public void checkMulticast(InetAddress addr) {
    }

    /**
     *
     */
    @Override
    public void checkPropertiesAccess() {
    }

    /**
     *
     *
     * @param key ...
     */
    @Override
    public void checkPropertyAccess(String key) {
    }

    /**
     *
     */
    @Override
    public void checkPrintJobAccess() {
    }

    /**
     *
     *
     * @param pkg ...
     */
    @Override
    public void checkPackageAccess(String pkg) {
    }

    /**
     *
     *
     * @param pkg ...
     */
    @Override
    public void checkPackageDefinition(String pkg) {
    }

    /**
     *
     */
    @Override
    public void checkSetFactory() {
    }

    /**
     *
     *
     * @param target ...
     */
    @Override
    public void checkSecurityAccess(String target) {
    }

    /**
     *  Utility method that returns the name of the application trying
     *  to execute the code in question. Returns null if the current code
     *  does not belong to any application.
     */
    protected String getApplication() {
        Class[] classes = getClassContext();

        for (int i = 0; i < classes.length; i++) {
            if (classes[i].getClassLoader() instanceof AppClassLoader) {
                return ((AppClassLoader) classes[i].getClassLoader()).getAppName();
            }
        }

        // no application class loader found in stack - return null
        return null;
    }
}
