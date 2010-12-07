/*
 * Helma License Notice
 *
 * The contents of this file are subject to the Helma License
 * Version 2.0 (the "License"). You may not use this file except in
 * compliance with the License. A copy of the License is available at
 * http://adele.helma.org/download/helma/license.txt
 *
 * Copyright 2007 Helma Software. All Rights Reserved.
 *
 * $RCSfile$
 * $Author$
 * $Revision$
 * $Date$
 */

package helma.framework;

import java.io.Serializable;

public class UploadStatus implements Serializable {

    private static final long serialVersionUID = 8335579045959177198L;

    long current = 0;
    long total = 0;
    int itemsRead = 0;
    String error = null;
    long lastModified;

    public UploadStatus() {
        lastModified = System.currentTimeMillis();
    }

    public void update(long bytesRead, long contentLength, int itemsRead) {
        this.current = bytesRead;
        this.total = contentLength;
        this.itemsRead = itemsRead;
        lastModified = System.currentTimeMillis();
    }

    public void setError(String error) {
        this.error = error;
        lastModified = System.currentTimeMillis();
    }

    public String getError() {
        return error;
    }

    public long getCurrent() {
        return current;
    }

    public long getTotal() {
        return total;
    }

    public int getItemsRead() {
        return itemsRead;
    }

    public boolean isDisposable() {
        // Make upload status disposable if it hasn't been modified for the last
        // 10 minutes, regardless of whether the upload has finished or not
        return System.currentTimeMillis() - lastModified > 60000;
    }

    public String toString() {
        StringBuffer buffer = new StringBuffer("{current: ").append(current) //$NON-NLS-1$
                .append(", total: ").append(total) //$NON-NLS-1$
                .append(", itemsRead: ").append(itemsRead) //$NON-NLS-1$
                .append(", error: "); //$NON-NLS-1$
        if (error == null) {
            buffer.append("null"); //$NON-NLS-1$
        } else {
            buffer.append("\""); //$NON-NLS-1$
            buffer.append(error.replaceAll("\"", "\\\\\"")); //$NON-NLS-1$ //$NON-NLS-2$
            buffer.append("\""); //$NON-NLS-1$
        }
        return buffer.append("}").toString(); //$NON-NLS-1$
    }

}
