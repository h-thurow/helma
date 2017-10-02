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

package helma.util;

import java.io.*;
import javax.activation.*;

/**
 * Makes MimeParts usable as Datasources in the Java Activation Framework (JAF)
 */
public class MimePartDataSource implements DataSource {
    private MimePart part;
    private String name;

    /**
     * Creates a new MimePartDataSource object.
     *
     * @param part ...
     */
    public MimePartDataSource(MimePart part) {
        this.part = part;
        this.name = part.getName();
    }

    /**
     * Creates a new MimePartDataSource object.
     *
     * @param part ...
     * @param name ...
     */
    public MimePartDataSource(MimePart part, String name) {
        this.part = part;
        this.name = name;
    }

    /**
     *
     *
     * @return ...
     *
     * @throws IOException ...
     */
    public InputStream getInputStream() {
        return new ByteArrayInputStream(this.part.getContent());
    }

    /**
     *
     *
     * @return ...
     *
     * @throws IOException ...
     */
    public OutputStream getOutputStream() throws IOException {
        throw new IOException(Messages.getString("MimePartDataSource.0")); //$NON-NLS-1$
    }

    /**
     *
     *
     * @return ...
     */
    public String getContentType() {
        return this.part.getContentType();
    }

    /**
     *
     *
     * @return ...
     */
    public String getName() {
        return this.name;
    }
}
