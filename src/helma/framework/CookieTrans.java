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

package helma.framework;

import java.io.Serializable;
import javax.servlet.http.Cookie;

/**
 *  Cookie Transmitter. A simple, serializable representation
 *  of an HTTP cookie.
 */
public final class CookieTrans implements Serializable {
    private static final long serialVersionUID = 1811202114296536258L;

    String name;
    String value;
    String path;
    String domain;
    int days = -1;
    boolean secure;
    boolean httpOnly;

    CookieTrans(String name, String value) {
        this.name = name;
        this.value = value;
    }

    void setValue(String value) {
        this.value = value;
    }

    void setDays(int days) {
        this.days = days;
    }

    void setPath(String path) {
        this.path = path;
    }

    void setDomain(String domain) {
        this.domain = domain;
    }

    /**
     *
     *
     * @return ...
     */
    public String getName() {
        return this.name;
    }

    /**
     *
     *
     * @return ...
     */
    public String getValue() {
        return this.value;
    }

    /**
     *
     *
     * @return ...
     */
    public int getDays() {
        return this.days;
    }

    /**
     *
     *
     * @return ...
     */
    public String getPath() {
        return this.path;
    }

    /**
     *
     *
     * @return ...
     */
    public String getDomain() {
        return this.domain;
    }

    public boolean isSecure() {
        return secure;
    }

    void isSecure(boolean secure) {
        this.secure = secure;
    }

    public boolean isHttpOnly() {
        return httpOnly;
    }

    void isHttpOnly(boolean httpOnly) {
        this.httpOnly = httpOnly;
    }

    /**
     *
     *
     * @param defaultPath ...
     * @param defaultDomain ...
     *
     * @return ...
     */
    public Cookie getCookie(String defaultPath, String defaultDomain) {
        Cookie c = new Cookie(this.name, this.value);

        // NOTE: If cookie version is set to 1, cookie values will be quoted.
        // c.setVersion(1);

        if (this.days > -1) {
            // Cookie time to live, days -> seconds
            c.setMaxAge(this.days * 60 * 60 * 24);
        }

        if (this.path != null) {
            c.setPath(this.path);
        } else if (defaultPath != null) {
            c.setPath(defaultPath);
        }

        if (this.domain != null) {
            c.setDomain(this.domain);
        } else if (defaultDomain != null) {
            c.setDomain(defaultDomain);
        }

        c.setHttpOnly(httpOnly);
        c.setSecure(secure);

        return c;
    }
}
