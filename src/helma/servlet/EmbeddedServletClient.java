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

package helma.servlet;

import helma.framework.core.Application;
import helma.main.*;
import javax.servlet.*;

/**
 *  Servlet client that runs a Helma application for the embedded
 *  web server
 */
public final class EmbeddedServletClient extends AbstractServletClient {
    private static final long serialVersionUID = -1716809853688477356L;

    private Application app = null;
    private String appName;

    /**
     * Creates a new EmbeddedServletClient object.
     */
    public EmbeddedServletClient() {
        super();
    }

    /**
     *
     *
     * @param init ...
     *
     * @throws ServletException ...
     */
    @Override
    public void init(ServletConfig init) throws ServletException {
        super.init(init);
        this.appName = init.getInitParameter("application"); //$NON-NLS-1$

        if (this.appName == null) {
            throw new ServletException(Messages.getString("EmbeddedServletClient.0")); //$NON-NLS-1$
        }
    }

    /**
     * Returns the {@link helma.framework.core.Application Applicaton}
     * instance the servlet is talking to.
     *
     * @return this servlet's application instance
     */
    @Override
    public Application getApplication() {
        if (this.app == null) {
            this.app = Server.getServer().getApplication(this.appName);
        }

        return this.app;
    }
}
