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

import java.io.BufferedReader;
import java.io.StringReader;
import java.util.Properties;
import java.util.StringTokenizer;
import helma.framework.repository.Resource;

/**
 *  This file authenticates against a passwd source
 */
public class CryptResource {

    private Properties users;
    private CryptResource parentResource;
    private Resource resource;
    private long lastRead = 0;

    /**
     * Creates a new CryptSource object.
     *
     * @param resource ...
     * @param parentResource ...
     */
    public CryptResource(Resource resource, CryptResource parentResource) {
        this.resource = resource;
        this.parentResource = parentResource;
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
        if (this.resource.exists() && (this.resource.lastModified() > this.lastRead)) {
            readFile();
        } else if (!this.resource.exists() && (this.users.size() > 0)) {
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
            if (this.parentResource != null) {
                return this.parentResource.authenticate(username, pw);
            }
        }

        return false;
    }

    private synchronized void readFile() {
        BufferedReader reader = null;

        this.users = new Properties();

        try {
            reader = new BufferedReader(new StringReader(this.resource.getContent()));

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

}

