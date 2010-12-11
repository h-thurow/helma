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

package helma.framework.repository;

import java.net.*;
import java.io.*;

public class FileResource extends AbstractResource {

    File file;
    RepositoryInterface repository;
    String name;
    String shortName;
    String baseName;

    public FileResource(File file) {
        this(file, null);
    }

    protected FileResource(File file, RepositoryInterface repository) {
        this.file = file;

        this.repository = repository;
        this.name = file.getAbsolutePath();
        this.shortName = file.getName();
        // base name is short name with extension cut off
        int lastDot = this.shortName.lastIndexOf("."); //$NON-NLS-1$
        this.baseName = (lastDot == -1) ? this.shortName : this.shortName.substring(0, lastDot);
    }

    public String getName() {
        return this.name;
    }

    public String getShortName() {
        return this.shortName;
    }

    public String getBaseName() {
        return this.baseName;
    }

    public InputStream getInputStream() throws IOException {
        return new FileInputStream(this.file);
    }

    public URL getUrl() {
        try {
            return new URL("file:" + this.file.getAbsolutePath()); //$NON-NLS-1$
        } catch (MalformedURLException ex) {
            return null;
        }
    }

    public long lastModified() {
        return this.file.lastModified();
    }

    public String getContent(String encoding) throws IOException {
        InputStream in = getInputStream();
        int size = (int) this.file.length();
        byte[] buf = new byte[size];
        int read = 0;
        while (read < size) {
            int r = in.read(buf, read, size - read);
            if (r == -1)
                break;
            read += r;
        }
        in.close();
        return encoding == null ?
                new String(buf) :
                new String(buf, encoding);
    }

    public String getContent() throws IOException {
        return getContent(null);
    }

    public long getLength() {
        return this.file.length();
    }

    public boolean exists() {
        return this.file.exists();
    }

    public RepositoryInterface getRepository() {
        return this.repository;
    }

    @Override
    public int hashCode() {
        return 17 + this.name.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof FileResource && this.name.equals(((FileResource)obj).name);
    }

    @Override
    public String toString() {
        return getName();
    }
}
