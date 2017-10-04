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

package helma.framework.repository;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public final class ZipResource extends AbstractResource {

    private String entryName;
    private ZipRepository repository;
    private String name;
    private String shortName;
    private String baseName;

    protected ZipResource(String zipentryName, ZipRepository repository) {
        this.entryName = zipentryName;
        this.repository = repository;

        int lastSlash = this.entryName.lastIndexOf('/');

        this.shortName = this.entryName.substring(lastSlash + 1);
        this.name = new StringBuffer(repository.getName()).append('/')
                .append(this.shortName).toString();

        // base name is short name with extension cut off
        int lastDot = this.shortName.lastIndexOf("."); //$NON-NLS-1$
        this.baseName = (lastDot == -1) ? this.shortName : this.shortName.substring(0, lastDot);
    }

    public long lastModified() {
        return this.repository.lastModified();
    }

    public InputStream getInputStream() throws IOException {
        ZipFile zipfile = null;
        try {
            zipfile = this.repository.getZipFile();
            ZipEntry entry = zipfile.getEntry(this.entryName);
            if (entry == null) {
                throw new IOException(Messages.getString("ZipResource.0") + this + Messages.getString("ZipResource.1")); //$NON-NLS-1$ //$NON-NLS-2$
            }
            int size = (int) entry.getSize();
            byte[] buf = new byte[size];
            InputStream in = zipfile.getInputStream(entry);
            int read = 0;
            while (read < size) {
                int r = in.read(buf, read, size-read);
                if (r == -1)
                    break;
                read += r;
            }
            in.close();
            return new ByteArrayInputStream(buf);
        } finally {
            zipfile.close();
        }
    }

    public boolean exists() {
        ZipFile zipfile = null;
        try {
            zipfile = this.repository.getZipFile();
            return (zipfile.getEntry(this.entryName) != null);
        } catch (Exception ex) {
            return false;
        } finally {
            try {
                zipfile.close();
            } catch (Exception ex) {}
        }
    }

    public String getContent(String encoding) throws IOException {
        ZipFile zipfile = null;
        try {
            zipfile = this.repository.getZipFile();
            ZipEntry entry = zipfile.getEntry(this.entryName);
            if (entry == null) {
                throw new IOException(Messages.getString("ZipResource.2") + this + Messages.getString("ZipResource.3")); //$NON-NLS-1$ //$NON-NLS-2$
            }
            InputStream in = zipfile.getInputStream(entry);
            int size = (int) entry.getSize();
            byte[] buf = new byte[size];
            int read = 0;
            while (read < size) {
                int r = in.read(buf, read, size-read);
                if (r == -1)
                    break;
                read += r;
            }
            in.close();
            return encoding == null ?
                    new String(buf) :
                    new String(buf, encoding);
        } finally {
            if (zipfile != null) {
                zipfile.close();
            }
        }
    }

    public String getContent() throws IOException {
        return getContent(null);
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

    public URL getUrl() {
        // TODO: we might want to return a Jar URL
        // http://java.sun.com/j2se/1.5.0/docs/api/java/net/JarURLConnection.html
        throw new UnsupportedOperationException(Messages.getString("ZipResource.4")); //$NON-NLS-1$
    }

    public long getLength() {
        ZipFile zipfile = null;
        try {
            zipfile = this.repository.getZipFile();
            return zipfile.getEntry(this.entryName).getSize();            
        } catch (Exception ex) {
            return 0;
        } finally {
            try {
                zipfile.close();
            } catch (Exception ex) {}
        }
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
        return obj instanceof ZipResource && this.name.equals(((ZipResource) obj).name);
    }

    @Override
    public String toString() {
        return getName();
    }
}
