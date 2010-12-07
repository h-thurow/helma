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
    Repository repository;
    String name;
    String shortName;
    String baseName;

    public FileResource(File file) {
        this(file, null);
    }

    protected FileResource(File file, Repository repository) {
        this.file = file;

        this.repository = repository;
        name = file.getAbsolutePath();
        shortName = file.getName();
        // base name is short name with extension cut off
        int lastDot = shortName.lastIndexOf("."); //$NON-NLS-1$
        baseName = (lastDot == -1) ? shortName : shortName.substring(0, lastDot);
    }

    public String getName() {
        return name;
    }

    public String getShortName() {
        return shortName;
    }

    public String getBaseName() {
        return baseName;
    }

    public InputStream getInputStream() throws IOException {
        return new FileInputStream(file);
    }

    public URL getUrl() {
        try {
            return new URL("file:" + file.getAbsolutePath()); //$NON-NLS-1$
        } catch (MalformedURLException ex) {
            return null;
        }
    }

    public long lastModified() {
        return file.lastModified();
    }

    public String getContent(String encoding) throws IOException {
        InputStream in = getInputStream();
        int size = (int) file.length();
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
        return file.length();
    }

    public boolean exists() {
        return file.exists();
    }

    public Repository getRepository() {
        return repository;
    }

    public int hashCode() {
        return 17 + name.hashCode();
    }

    public boolean equals(Object obj) {
        return obj instanceof FileResource && name.equals(((FileResource)obj).name);
    }

    public String toString() {
        return getName();
    }
}
