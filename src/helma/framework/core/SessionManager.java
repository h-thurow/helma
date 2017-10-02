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

package helma.framework.core;

import helma.objectmodel.INode;
import helma.objectmodel.db.NodeHandle;
import helma.objectmodel.db.Transactor;
import helma.scripting.ScriptingEngine;

import java.util.*;
import java.io.*;

public class SessionManager {

    protected Hashtable sessions;

    protected Application app;

    public SessionManager() {
        sessions = new Hashtable();
    }

    public void init(Application app) {
        this.app = app;
    }

    public void shutdown() {
        sessions.clear();
    }

    public Session createSession(String sessionId) {
        Session session = getSession(sessionId);
        if (session == null) {
            session = new Session(sessionId, app);
        }
        return session;
    }

    public Session getSession(String sessionId) {
        if (sessionId == null) {
            return null;
        }
        return (Session) sessions.get(sessionId);
    }

    public void registerSession(Session session) {
        sessions.put(session.getSessionId(), session);        
    }

    /**
     *  Return the whole session map. We return a clone of the table to prevent
     * actual changes from the table itself, which is managed by the application.
     * It is safe and allowed to manipulate the session objects contained in the table, though.
     */
    public Map getSessions() {
        return (Map) sessions.clone();
    }

    /**
     * Returns the number of currenty active sessions.
     */
    public int countSessions() {
        return sessions.size();
    }

    /**
     * Remove the session from the sessions-table and logout the user.
     */
    public void discardSession(Session session) {
        session.logout();
        sessions.remove(session.getSessionId());
    }


    /**
     * Return an array of <code>SessionBean</code> objects currently associated with a given
     * Helma user.
     */
    public List getSessionsForUsername(String username) {
        ArrayList list = new ArrayList();

        if (username == null) {
            return list;
        }

        Enumeration e = sessions.elements();
        while (e.hasMoreElements()) {
            Session s = (Session) e.nextElement();

            if (s != null && username.equals(s.getUID())) {
                // append to list if session is logged in and fits the given username
                list.add(new SessionBean(s));
            }
        }

        return list;
    }

    /**
     * Return a list of Helma nodes (HopObjects -  the database object representing the user,
     *  not the session object) representing currently logged in users.
     */
    public List getActiveUsers() {
        ArrayList list = new ArrayList();

        for (Enumeration e = sessions.elements(); e.hasMoreElements();) {
            Session s = (Session) e.nextElement();

            if (s != null && s.isLoggedIn()) {
                // returns a session if it is logged in and has not been
                // returned before (so for each logged-in user is only added once)
                INode node = s.getUserNode();

                // we check again because user may have been logged out between the first check
                if (node != null && !list.contains(node)) {
                    list.add(node);
                }
            }
        }

        return list;
    }


    /**
     * Dump session state to a file.
     *
     * @param f the file to write session into, or null to use the default sesssion store.
     */
    public void storeSessionData(File f, ScriptingEngine engine) {
        if (f == null) {
            f = new File(app.dbDir, "sessions");
        }

        try {
            OutputStream ostream = new BufferedOutputStream(new FileOutputStream(f));
            ObjectOutputStream p = new ObjectOutputStream(ostream);

            synchronized (sessions) {
                p.writeInt(sessions.size());

                for (Enumeration e = sessions.elements(); e.hasMoreElements();) {
                    try {
                        engine.serialize(e.nextElement(), p);
                        // p.writeObject(e.nextElement());
                    } catch (NotSerializableException nsx) {
                        // not serializable, skip this session
                        app.logError("Error serializing session.", nsx);
                    }
                }
            }

            p.flush();
            ostream.close();
            app.logEvent("stored " + sessions.size() + " sessions in file");
        } catch (Exception e) {
            app.logError("error storing session data.", e);
        }
    }

    /**
     * loads the serialized session table from a given file or from dbdir/sessions
     */
    public void loadSessionData(File f, ScriptingEngine engine) {
        if (f == null) {
            f = new File(app.dbDir, "sessions");
        }

        // compute session timeout value
        int sessionTimeout = 30;

        try {
            sessionTimeout = Math.max(0,
                                      Integer.parseInt(app.getProperty("sessionTimeout",
                                                                         "30")));
        } catch (Exception ignore) {
            System.out.println(ignore.toString());
        }

        long now = System.currentTimeMillis();
        Transactor tx = Transactor.getInstance(app.getNodeManager());

        try {
            tx.begin("sessionloader");
            // load the stored data:
            InputStream istream = new BufferedInputStream(new FileInputStream(f));
            ObjectInputStream p = new ObjectInputStream(istream);
            int size = p.readInt();
            int ct = 0;
            Hashtable newSessions = new Hashtable();

            while (ct < size) {
                Session session = (Session) engine.deserialize(p);

                if ((now - session.lastTouched()) < (sessionTimeout * 60000)) {
                    session.setApp(app);
                    newSessions.put(session.getSessionId(), session);
                }

                ct++;
            }

            p.close();
            istream.close();
            sessions = newSessions;
            app.logEvent("loaded " + newSessions.size() + " sessions from file");
            tx.commit();
        } catch (FileNotFoundException fnf) {
            // suppress error message if session file doesn't exist
            tx.abort();
        } catch (Exception e) {
            app.logError("error loading session data.", e);
            tx.abort();
        } finally {
            tx.closeConnections();
        }

    }

    /**
     * Purge sessions that have not been used for a certain amount of time.
     * This is called by run().
     *
     * @param lastSessionCleanup the last time sessions were purged
     * @return the updated lastSessionCleanup value
     */
    protected long cleanupSessions(long lastSessionCleanup) {

        long now = System.currentTimeMillis();
        long sessionCleanupInterval = 60000;

        // check if we should clean up user sessions
        if ((now - lastSessionCleanup) > sessionCleanupInterval) {

            // get session timeout
            int sessionTimeout = 30;

            try {
                sessionTimeout = Math.max(0,
                        Integer.parseInt(app.getProperty("sessionTimeout", "30")));
            } catch (NumberFormatException nfe) {
                app.logEvent("Invalid sessionTimeout setting: " + app.getProperty("sessionTimeout"));
            }

            RequestEvaluator thisEvaluator = null;

            try {

                thisEvaluator = app.getEvaluator();

                Session[] sessionArray = (Session[]) sessions.values().toArray(new Session[0]);

                for (int i = 0; i < sessionArray.length; i++) {
                    Session session = sessionArray[i];

                    session.pruneUploads();
                    if ((now - session.lastTouched()) > (sessionTimeout * 60000)) {
                        NodeHandle userhandle = session.userHandle;

                        if (userhandle != null) {
                            try {
                                Object[] param = {session.getSessionId()};

                                thisEvaluator.invokeInternal(userhandle, "onLogout", param);
                            } catch (Exception x) {
                                // errors should already be logged by requestevaluator, but you never know
                                app.logError("Error in onLogout", x);
                            }
                        }

                        discardSession(session);
                    }
                }
            } catch (Exception cx) {
                app.logError("Error cleaning up sessions", cx);
            } finally {
                if (thisEvaluator != null) {
                    app.releaseEvaluator(thisEvaluator);
                }
            }
            return now;
        } else {
            return lastSessionCleanup;
        }
    }


}
