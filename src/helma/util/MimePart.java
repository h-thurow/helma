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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.util.StringTokenizer;

import org.apache.commons.fileupload.FileItem;

/**
 * This represents a MIME part of a HTTP file upload
 */
public class MimePart implements Serializable {
    private static final long serialVersionUID = 7800159441938112415L;

    private final String name;
    private int contentLength;
    private String contentType;
    private byte[] content;
    private Date lastModified;
    private String eTag;
    private FileItem fileItem;
    private File file;

    /**
     * Creates a new MimePart object.
     * @param name the file name
     * @param content the mime part content
     * @param contentType the content type
     */
    public MimePart(String name, byte[] content, String contentType) {
        this.name = normalizeFilename(name);
        this.content = (content == null) ? new byte[0] : content;
        this.contentType = contentType;
        this.contentLength = (content == null) ? 0 : content.length;
    }

    /**
     * Creates a new MimePart object from a file upload.
     * @param fileItem a commons fileupload file item
     */
    public MimePart(FileItem fileItem) {
        this.name = normalizeFilename(fileItem.getName());
        this.contentType = fileItem.getContentType();
        this.contentLength = (int) fileItem.getSize();
        if (fileItem.isInMemory()) {
            this.content = fileItem.get();
        } else {
            this.fileItem = fileItem;
        }
    }

    /**
     * @return the content type
     */
    public String getContentType() {
        return this.contentType;
    }

    /**
     * Get the number of bytes in the mime part's content
     * @return the content length
     */
    public int getContentLength() {
        return this.contentLength;
    }

    /**
     * Get the mime part's name
     * @return the file name
     */
    public String getName() {
        return this.name;
    }

    /**
     * Return the content of the mime part as byte array.
     * @return the mime part content as byte array
     */
    public byte[] getContent() {
        if (this.content == null && (this.fileItem != null || this.file != null)) {
            loadContent();
        }
        return this.content;
    }

    private synchronized void loadContent() {
        this.content = new byte[this.contentLength];
        try {
            InputStream in = getInputStream();
            int read = 0;
            while (read < this.contentLength) {
                int r = in.read(this.content, read, this.contentLength - read);
                if (r == -1)
                    break;
                read += r;
            }
            in.close();
        } catch (Exception x) {
            System.err.println(Messages.getString("MimePart.0") + x); //$NON-NLS-1$
            this.content = new byte[0];
        }
    }

    /**
     * Return an InputStream to read the content of the mime part
     * @return an InputStream for the mime part content
     * @throws IOException an I/O related error occurred
     */
    public InputStream getInputStream() throws IOException {
        if (this.file != null && this.file.canRead()) {
            return new FileInputStream(this.file);
        } else if (this.fileItem != null) {
            return this.fileItem.getInputStream();
        } else if (this.content != null) {
            return new ByteArrayInputStream(this.content);
        } else {
            return null;
        }
    }

    /**
     * Return the content of the mime part as string, if its content type is
     * null, text/* or application/text. Otherwise, return null.
     *
     * @return the content of the mime part as string
     */
    public String getText() {
        if ((this.contentType == null) || this.contentType.startsWith("text/") //$NON-NLS-1$
                                  || this.contentType.startsWith("application/text")) { //$NON-NLS-1$
            String charset = getSubHeader(this.contentType, "charset"); //$NON-NLS-1$
            byte[] content = getContent();
            if (charset != null) {
                try {
                    return new String(content, charset);
                } catch (UnsupportedEncodingException uee) {
                    return new String(content);
                }
            }
            return new String(content);
        }
        return null;
    }


    /**
     * Get the last modified date
     * @return the last modified date
     */
    public Date getLastModified() {
        return this.lastModified;
    }

    /**
     * Set the last modified date
     * @param lastModified the last modified date
     */
    public void setLastModified(Date lastModified) {
        this.lastModified = lastModified;
    }

    /**
     * Get the ETag of the mime part
     * @return the ETag
     */
    public String getETag() {
        return this.eTag;
    }

    /**
     * Set the ETag for the mime part
     * @param eTag the ETag
     */
    public void setETag(String eTag) {
        this.eTag = eTag;
    }

    /**
     * Write the mimepart to a directory, using its name as file name.
     *
     * @param dir the directory to write the file to
     * @return the absolute path name of the file written, or null if an error occurred
     */
    public String writeToFile(String dir) {
        return writeToFile(dir, null);
    }

    /**
     * Write the mimepart to a file.
     *
     * @param dir the directory to write the file to
     * @return the name of the file written, or null if an error occurred
     */
    public String writeToFile(String dir, String fname) {
        try {
            File base = new File(dir).getAbsoluteFile();

            // make directories if they don't exist
            if (!base.exists()) {
                base.mkdirs();
            }

            String filename = this.name;

            if (fname != null) {
                if (fname.indexOf(".") < 0) { //$NON-NLS-1$
                    // check if we can use extension from name
                    int ndot = (this.name == null) ? (-1) : this.name.lastIndexOf("."); //$NON-NLS-1$

                    if (ndot > -1) {
                        filename = fname + this.name.substring(ndot);
                    } else {
                        filename = fname;
                    }
                } else {
                    filename = fname;
                }
            }

            // set instance variable to the new file
            this.file = new File(base, filename);

            if (this.fileItem != null) {
                this.fileItem.write(this.file);
                // null out fileItem, since calling write() may have moved the temp file
                this.fileItem = null;
            } else {
                FileOutputStream fout = new FileOutputStream(this.file);
                fout.write(getContent());
                fout.close();
            }
            // return file name
            return filename;
        } catch (Exception x) {
            System.err.println(Messages.getString("MimePart.1") + x);             //$NON-NLS-1$
            return null;
        }
    }

    /**
     *  Get a sub-header from a header, e.g. the charset from
     *  <code>Content-Type: text/plain; charset="UTF-8"</code>
     */
    public static String getSubHeader(String header, String subHeaderName) {
        if (header == null) {
            return null;
        }

        StringTokenizer headerTokenizer = new StringTokenizer(header, ";"); //$NON-NLS-1$

        while (headerTokenizer.hasMoreTokens()) {
            String token = headerTokenizer.nextToken().trim();
            int i = token.indexOf("="); //$NON-NLS-1$

            if (i > 0) {
                String hname = token.substring(0, i).trim();

                if (hname.equalsIgnoreCase(subHeaderName)) {
                    String value = token.substring(i + 1);
                    return value.replace('"', ' ').trim();
                }
            }
        }

        return null;
    }

    /**
     * Normalize a upload file name. Internet Explorer on Windows sends
     * the whole path, so we cut off everything before the actual name.
     */
    public  static String normalizeFilename(String filename) {
        if (filename == null)
            return null;
        int idx = filename.lastIndexOf('/');
        if (idx > -1)
            filename = filename.substring(idx + 1);
        idx = filename.lastIndexOf('\\');
        if (idx > -1)
            filename = filename.substring(idx + 1);
        return filename;
    }

}
