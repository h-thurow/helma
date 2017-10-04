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

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

/**
 * ResourceInterface represents a pointer to some kind of information (code, skin, ...)
 * from which the content can be fetched
 */
public interface ResourceInterface {

    /**
     * Returns the date the resource was last modified
     * @return last modified date
     */
    public long lastModified();

    /**
     * Checks wether this resource actually (still) exists
     * @return true if the resource exists
     */
    public boolean exists();

    /**
     * Returns the lengh of the resource's content
     * @return content length
     * @throws IOException I/O related problem
     */
    public long getLength() throws IOException;

    /**
     * Returns an input stream to the content of the resource
     * @return content input stream
     * @throws IOException I/O related problem
     */
    public InputStream getInputStream() throws IOException;

    /**
     * Returns the content of the resource in a given encoding
     * @param encoding the character encoding
     * @return content
     * @throws IOException I/O related problem
     */
    public String getContent(String encoding) throws IOException;

    /**
     * Returns the content of the resource
     * @return content
     * @throws IOException I/O related problem
     */
    public String getContent() throws IOException;

    /**
     * Returns the name of the resource; does not include the name of the
     * repository the resource was fetched from
     * @return name of the resource
     */
    public String getName();

    /**
     * Returns the short name of the resource which is its name exclusive file
     * ending if it exists
     * @return short name of the resource
     */
    public String getShortName();

    /**
     * Returns the short name of the resource with the file extension
     * (everything following the last dot character) cut off.
     * @return the file name without the file extension
     */
    public String getBaseName();

    /**
     * Returns an url to the resource if the repository of this resource is
     * able to provide urls
     * @return url to the resource
     * @throws UnsupportedOperationException if resource does not support URL schema
     */
    public URL getUrl() throws UnsupportedOperationException;

    /**
     * Get a ResourceInterface this ResourceInterface is overloading
     * @return the overloaded resource
     */
    public ResourceInterface getOverloadedResource();

    /**
     * Method for registering a ResourceInterface this ResourceInterface is overloading
     * @param res the overloaded resource
     */
    public void setOverloadedResource(ResourceInterface res);

    /**
     * Returns the repository the resource does belong to
     * @return upper repository
     */
    public RepositoryInterface getRepository();

}
