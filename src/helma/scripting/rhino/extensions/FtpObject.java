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

import org.apache.commons.net.ftp.*;
import java.io.*;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.FunctionObject;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.Undefined;
import java.lang.reflect.Member;
import java.lang.reflect.Method;


/**
 * A FTP-client object that allows to do some FTP from HOP applications.
 * FTP support is far from complete but can easily be extended if more
 * functionality is needed.
 * This uses the NetComponent classes from savarese.org (ex oroinc.com).
 */
public class FtpObject extends ScriptableObject {
    private static final long serialVersionUID = 3470670009973887555L;

    private FTPClient ftpclient;
    private String server;
    private Exception lastError = null;
    private File localDir = null;

    /**
     * Create a new FTP Client
     *
     * @param srvstr the name of the server to connect to
     */
    FtpObject(String srvstr) {
        this.server = srvstr;
    }

    FtpObject() {
    }

    /**
     *
     *
     * @return ...
     */
    @Override
    public String getClassName() {
        return "FtpClient"; //$NON-NLS-1$
    }

    /**
     *
     *
     * @return ...
     */
    @Override
    public String toString() {
        return "[FtpClient]"; //$NON-NLS-1$
    }

    /**
     *
     *
     * @return ...
     */
    public String toDetailString() {
        return "ES:[Object: builtin " + this.getClass().getName() + ":" +  //$NON-NLS-1$//$NON-NLS-2$
               this.toString() + "]"; //$NON-NLS-1$
    }

    Exception getLastError() {
        if (lastError == null) {
            return null;
        } else {
            return lastError;
        }
    }

    /**
     * Login to the FTP server
     *
     * @param   username the user name
     * @param   password the user's password
     * @return  true if successful, false otherwise
     */
    public boolean login(String username, String password) {
        if (server == null) {
            return false;
        }

        try {
            ftpclient = new FTPClient();
            ftpclient.connect(server);

            return ftpclient.login(username, password);
        } catch (Exception x) {
            return false;
        } catch (NoClassDefFoundError x) {
            return false;
        }
    }

    public boolean cd(String path) {
        if (ftpclient == null) {
            return false;
        }

        try {
            ftpclient.changeWorkingDirectory(path);

            return true;
        } catch (Exception wrong) {
        }

        return false;
    }

    public boolean mkdir(String dir) {
        if (ftpclient == null) {
            return false;
        }

        try {
            return ftpclient.makeDirectory(dir);
        } catch (Exception wrong) {
        }

        return false;
    }

    public boolean lcd(String dir) {
        try {
            localDir = new File(dir);

            if (!localDir.exists()) {
                localDir.mkdirs();
            }

            return true;
        } catch (Exception wrong) {
        }

        return false;
    }

    public boolean putFile(String localFile, String remoteFile) {
        if (ftpclient == null) {
            return false;
        }

        try {
            File f = (localDir == null) ? new File(localFile) : new File(localDir, localFile);
            InputStream fin = new BufferedInputStream(new FileInputStream(f));

            ftpclient.storeFile(remoteFile, fin);
            fin.close();

            return true;
        } catch (Exception wrong) {
        }

        return false;
    }

    public boolean putString(Object obj, String remoteFile) {
        if (ftpclient == null || obj == null) {
            return false;
        }

        try {
            byte[] bytes = null;

            // check if this already is a byte array
            if (obj instanceof byte[]) {
                bytes = (byte[]) obj;
            }

            if (bytes == null) {
                bytes = obj.toString().getBytes();
            }

            ByteArrayInputStream bin = new ByteArrayInputStream(bytes);

            ftpclient.storeFile(remoteFile, bin);

            return true;
        } catch (Exception wrong) {
        }

        return false;
    }

    public boolean getFile(String remoteFile, String localFile) {
        if (ftpclient == null) {
            return false;
        }

        try {
            File f = (localDir == null) ? new File(localFile) : new File(localDir, localFile);
            OutputStream out = new BufferedOutputStream(new FileOutputStream(f));

            ftpclient.retrieveFile(remoteFile, out);
            out.close();

            return true;
        } catch (Exception wrong) {
        }

        return false;
    }

    public Object getString(String remoteFile) {
        if (ftpclient == null) {
            return null;
        }

        try {
            ByteArrayOutputStream bout = new ByteArrayOutputStream();

            ftpclient.retrieveFile(remoteFile, bout);

            return bout.toString();
        } catch (Exception wrong) {
        }

        return null;
    }

    /**
     * Disconnect from FTP server
     *
     * @return  true if successful, false otherwise
     */
    public boolean logout() {
        if (ftpclient != null) {
            try {
                ftpclient.logout();
            } catch (IOException ignore) {
            }

            try {
                ftpclient.disconnect();
            } catch (IOException ignore) {
            }
        }

        return true;
    }

    public boolean binary() {
        if (ftpclient != null) {
            try {
                ftpclient.setFileType(FTP.BINARY_FILE_TYPE);

                return true;
            } catch (IOException ignore) {
            }
        }

        return false;
    }

    public boolean ascii() {
        if (ftpclient != null) {
            try {
                ftpclient.setFileType(FTP.ASCII_FILE_TYPE);

                return true;
            } catch (IOException ignore) {
            }
        }

        return false;
    }



    public static FtpObject ftpObjCtor(Context cx, Object[] args,
                Function ctorObj, boolean inNewExpr) {
        if (args.length != 1 || args[0] == Undefined.instance) {
            throw new IllegalArgumentException(Messages.getString("FtpObject.0")); //$NON-NLS-1$
        }
        return new FtpObject(args[0].toString());
    }

    public static void init(Scriptable scope) {
        Method[] methods = FtpObject.class.getDeclaredMethods();
        ScriptableObject proto = new FtpObject();
        proto.setPrototype(getObjectPrototype(scope));
        Member ctorMember = null;
        for (int i=0; i<methods.length; i++) {
            if ("ftpObjCtor".equals(methods[i].getName())) { //$NON-NLS-1$
                ctorMember = methods[i];
                break;
            }
        }
        FunctionObject ctor = new FunctionObject("FtpClient", ctorMember, scope); //$NON-NLS-1$
        ctor.addAsConstructor(scope, proto);
        String[] ftpFuncs = {
                "login", "cd", "mkdir", "lcd", "putFile",   //$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
                "putString", "getFile", "getString", "logout",  //$NON-NLS-1$ //$NON-NLS-2$//$NON-NLS-3$ //$NON-NLS-4$
                "binary", "ascii"  //$NON-NLS-1$//$NON-NLS-2$
                            };
        try {
            proto.defineFunctionProperties(ftpFuncs, FtpObject.class, 0);
        } catch (Exception ignore) {
            System.err.println (Messages.getString("FtpObject.1")+ignore); //$NON-NLS-1$
        }
    }

}
