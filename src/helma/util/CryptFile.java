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

package helma.util;

import java.io.*;
import java.util.*;

/**
 *  This file authenticates against a passwd file
 */
public class CryptFile {
    private Properties users;
    private CryptFile parentFile;
    private File file;
    private long lastRead = 0;

    /**
     * Creates a new CryptFile object.
     *
     * @param file ...
     * @param parentFile ...
     */
    public CryptFile(File file, CryptFile parentFile) {
        this.file = file;
        this.parentFile = parentFile;
        this.users = new Properties();
    }

    /**
     *
     *
     * @param username ...
     * @param pw ...
     *
     * @return ...
     */
    public boolean authenticate(String username, String pw) {
        if (this.file.exists() && (this.file.lastModified() > this.lastRead)) {
            readFile();
        } else if (!this.file.exists() && (this.users.size() > 0)) {
            this.users.clear();
        }

        String realpw = this.users.getProperty(username);

        if (realpw != null) {
            try {
                // check if password matches
                // first we try with unix crypt algorithm
                String cryptpw = Crypt.crypt(realpw, pw);

                if (realpw.equals(cryptpw)) {
                    return true;
                }

                // then try MD5
                if (realpw.equals(MD5Encoder.encode(pw))) {
                    return true;
                }
            } catch (Exception x) {
                return false;
            }
        } else {
            if (this.parentFile != null) {
                return this.parentFile.authenticate(username, pw);
            }
        }

        return false;
    }

    private synchronized void readFile() {
        BufferedReader reader = null;

        this.users = new Properties();

        try {
            reader = new BufferedReader(new FileReader(this.file));

            String line = reader.readLine();

            while (line != null) {
                StringTokenizer st = new StringTokenizer(line, ":"); //$NON-NLS-1$

                if (st.countTokens() > 1) {
                    this.users.put(st.nextToken(), st.nextToken());
                }

                line = reader.readLine();
            }
        } catch (Exception ignore) {
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (Exception x) {
                }
            }

            this.lastRead = System.currentTimeMillis();
        }
    }

    /**
     *
     *
     * @param args ...
     */
    public static void main(String[] args) {
        CryptFile cf = new CryptFile(new File("passwd"), null); //$NON-NLS-1$

        System.err.println(cf.authenticate("hns", "asdf")); //$NON-NLS-1$ //$NON-NLS-2$
    }
}
