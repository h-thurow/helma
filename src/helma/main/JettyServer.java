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

package helma.main;


import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.ajp.Ajp13SocketConnector;
import org.eclipse.jetty.server.nio.SelectChannelConnector;
import org.eclipse.jetty.xml.XmlConfiguration;

import java.net.URL;
import java.net.InetSocketAddress;
import java.io.IOException;
import java.io.File;

public class JettyServer {

    // the embedded web server
    protected org.eclipse.jetty.server.Server http;

    // the AJP13 Listener, used for connecting from external webserver to servlet via JK
    protected Ajp13SocketConnector ajp13;

    public static JettyServer init(Server server, ServerConfig config) throws IOException {
        File configFile = config.getConfigFile();
        if (configFile != null && configFile.exists()) {
            return new JettyServer(configFile.toURI().toURL());
        } else if (config.hasWebsrvPort() || config.hasAjp13Port()) {
            return new JettyServer(config.getWebsrvPort(), config.getAjp13Port(), server);
        }
        return null;
    }

    private JettyServer(URL url) throws IOException {
        this.http = new org.eclipse.jetty.server.Server();

        try {
            XmlConfiguration config = new XmlConfiguration(url);
            config.configure(this.http);

            openListeners();
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(Messages.getString("JettyServer.0") + e); //$NON-NLS-1$
        }
    }

    private JettyServer(InetSocketAddress webPort, InetSocketAddress ajpPort, Server server)
            throws IOException {

        this.http = new org.eclipse.jetty.server.Server();
        this.http.setServer(this.http);

        // start embedded web server if port is specified
        if (webPort != null) {
        	Connector conn = new SelectChannelConnector();
        	conn.setHost(webPort.getAddress().getHostAddress());
        	conn.setPort(webPort.getPort());

        	this.http.addConnector(conn);
        }

        // activate the ajp13-listener
        if (ajpPort != null) {
            // create AJP13Listener
        	this.ajp13 = new Ajp13SocketConnector();
        	this.ajp13.setHost(ajpPort.getAddress().getHostAddress());
        	this.ajp13.setPort(ajpPort.getPort());

        	this.http.addConnector(this.ajp13);

            // jetty6 does not support protection of AJP13 connections anymore
            if (server.sysProps.containsKey("allowAJP13")) { //$NON-NLS-1$
                String message = Messages.getString("JettyServer.1") + //$NON-NLS-1$
                        Messages.getString("JettyServer.2") + //$NON-NLS-1$
                        Messages.getString("JettyServer.3"); //$NON-NLS-1$
                server.getLogger().error(message);
                throw new RuntimeException(message);
            }

            server.getLogger().info(Messages.getString("JettyServer.4") + (ajpPort));             //$NON-NLS-1$
        }
        openListeners();
    }

    public org.eclipse.jetty.server.Server getHttpServer() {
        return this.http;
    }

    public void start() throws Exception {
        this.http.start();
        if (this.ajp13 != null) {
            this.ajp13.start();
        }
    }

    public void stop() throws Exception {
        this.http.stop();
        if (this.ajp13 != null) {
            this.ajp13.stop();
        }
    }

    public void destroy() {
        this.http.destroy();
    }

    private void openListeners() throws IOException {
        // opening the listener here allows us to run on priviledged port 80 under jsvc
        // even as non-root user, because init() is called with root privileges
        // while start() will be called with the user we will actually run as
        Connector[] connectors = this.http.getConnectors();
        for (int i = 0; i < connectors.length; i++) {
            connectors[i].open();
        }
    }
}
