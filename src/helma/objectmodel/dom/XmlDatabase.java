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

package helma.objectmodel.dom;

import helma.objectmodel.*;
import helma.objectmodel.db.NodeManager;
import helma.objectmodel.db.Node;
import helma.framework.core.Application;

import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.util.ArrayList;

import org.xml.sax.SAXException;

/**
 * A simple XML-database
 */
public final class XmlDatabase implements DatabaseInterface {

    protected File dbHomeDir;
    protected Application app;
    protected NodeManager nmgr;
    protected XmlIDGenerator idgen;

    // character encoding to use when writing files.
    // use standard encoding by default.
    protected String encoding = null;

    /**
     * Initializes the database from an application.
     * @param app
     * @throws DatabaseException
     */
    public void init(File dbHome, Application app) throws DatabaseException {
        this.app = app;
        this.nmgr = app.getNodeManager();
        this.dbHomeDir = dbHome;

        if (!this.dbHomeDir.exists() && !this.dbHomeDir.mkdirs()) {
            throw new DatabaseException(Messages.getString("XmlDatabase.0")+this.dbHomeDir); //$NON-NLS-1$
        }

        if (!this.dbHomeDir.canWrite()) {
            throw new DatabaseException(Messages.getString("XmlDatabase.1")+this.dbHomeDir); //$NON-NLS-1$
        }

        File stylesheet = new File(this.dbHomeDir, "helma.xsl"); //$NON-NLS-1$
        // if style sheet doesn't exist, copy it
        if (!stylesheet.exists()) {
            copyStylesheet(stylesheet);
        }

        this.encoding = app.getCharset();

        // get the initial id generator value
        long idBaseValue;
        try {
            idBaseValue = Long.parseLong(app.getProperty("idBaseValue", "1")); //$NON-NLS-1$ //$NON-NLS-2$
            // 0 and 1 are reserved for root nodes
            idBaseValue = Math.max(1L, idBaseValue);
        } catch (NumberFormatException ignore) {
            idBaseValue = 1L;
        }

        TransactionInterface txn = null;

        try {
            txn = beginTransaction();

            try {
                this.idgen = getIDGenerator();

                if (this.idgen.getValue() < idBaseValue) {
                    this.idgen.setValue(idBaseValue);
                }
            } catch (ObjectNotFoundException notfound) {
                // will start with idBaseValue+1
                this.idgen = new XmlIDGenerator(idBaseValue);
            }

            // check if we need to set the id generator to a base value
            Node node = null;

            try {
                getNode(txn, "0"); //$NON-NLS-1$
            } catch (ObjectNotFoundException notfound) {
                node = new Node("root", "0", "Root", this.nmgr.safe); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                node.setDbMapping(app.getDbMapping("root")); //$NON-NLS-1$
                insertNode(txn, node.getID(), node);
                // register node with nodemanager cache
                // nmgr.registerNode(node);
            }

            try {
                getNode(txn, "1"); //$NON-NLS-1$
            } catch (ObjectNotFoundException notfound) {
                node = new Node("users", "1", null, this.nmgr.safe); //$NON-NLS-1$ //$NON-NLS-2$
                node.setDbMapping(app.getDbMapping("__userroot__")); //$NON-NLS-1$
                insertNode(txn, node.getID(), node);
                // register node with nodemanager cache
                // nmgr.registerNode(node);
            }

            commitTransaction(txn);
        } catch (Exception x) {
            x.printStackTrace();

            try {
                abortTransaction(txn);
            } catch (Exception ignore) {
            }

            throw (new DatabaseException(Messages.getString("XmlDatabase.2"))); //$NON-NLS-1$
        }
    }

    /**
     * Try to copy style sheet for XML files to database directory
     */
    private void copyStylesheet(File destination) {
        InputStream in = null;
        FileOutputStream out = null;
        byte[] buffer = new byte[1024];
        int read;

        try {
            in = getClass().getResourceAsStream("helma.xsl"); //$NON-NLS-1$
            out = new FileOutputStream(destination);
            while ((read = in.read(buffer, 0, buffer.length)) > 0) {
                out.write(buffer, 0, read);
            }
        } catch (IOException iox) {
            System.err.println(Messages.getString("XmlDatabase.3")+iox); //$NON-NLS-1$
        } finally {
            try {
                if (out != null)
                    out.close();
                if (in != null)
                    in.close();
            } catch (IOException ignore) {
            }
        }
    }

    /**
     * Shut down the database
     */
    public void shutdown() {
        // nothing to do
    }

    /**
     * Start a new transaction.
     *
     * @return the new tranaction object
     * @throws DatabaseException
     */
    public TransactionInterface beginTransaction() throws DatabaseException {
        return new XmlTransaction();
    }

    /**
     * committ the given transaction, makint its changes persistent
     *
     * @param txn
     * @throws DatabaseException
     */
    public void commitTransaction(TransactionInterface txn) throws DatabaseException {
        if (this.idgen.dirty) {
            try {
                saveIDGenerator(txn);
                this.idgen.dirty = false;
            } catch (IOException x) {
                throw new DatabaseException(x.toString());
            }
        }
        txn.commit();
    }

    /**
     * Abort the given transaction
     *
     * @param txn
     * @throws DatabaseException
     */
    public void abortTransaction(TransactionInterface txn) throws DatabaseException {
        txn.abort();
    }

    /**
     * Get the id for the next new object to be stored.
     *
     * @return the id for the next new object to be stored
     * @throws ObjectNotFoundException
     */
    public String nextID() throws ObjectNotFoundException {
        if (this.idgen == null) {
            getIDGenerator();
        }

        return this.idgen.newID();
    }

    /**
     * Get the id-generator for this database.
     *
     * @param txn
     * @return the id-generator for this database
     * @throws ObjectNotFoundException
     */
    public XmlIDGenerator getIDGenerator()
                               throws ObjectNotFoundException {
        File file = new File(this.dbHomeDir, "idgen.xml"); //$NON-NLS-1$

        this.idgen = XmlIDGenerator.getIDGenerator(file);

        return this.idgen;
    }

    /**
     * Write the id-generator to file.
     *
     * @param txn
     * @throws IOException
     */
    public void saveIDGenerator(TransactionInterface txn)
                         throws IOException {
        File tmp = File.createTempFile("idgen.xml.", ".tmp", this.dbHomeDir); //$NON-NLS-1$ //$NON-NLS-2$

        XmlIDGenerator.saveIDGenerator(this.idgen, tmp);

        File file = new File(this.dbHomeDir, "idgen.xml"); //$NON-NLS-1$
        if (file.exists() && !file.canWrite()) {
            throw new IOException(Messages.getString("XmlDatabase.4")+file); //$NON-NLS-1$
        }
        Resource res = new Resource(file, tmp);
        txn.addResource(res, TransactionInterface.ADDED);
    }

    /**
     * Retrieves a Node from the database.
     *
     * @param txn the current transaction
     * @param kstr the key
     * @return the object associated with the given key
     * @throws IOException if an I/O error occurred loading the object.
     * @throws ObjectNotFoundException if no object is stored by this key.
     */
    public NodeInterface getNode(TransactionInterface txn, String kstr)
                  throws IOException, ObjectNotFoundException {
        File f = new File(this.dbHomeDir, kstr + ".xml"); //$NON-NLS-1$

        if (!f.exists()) {
            throw new ObjectNotFoundException(Messages.getString("XmlDatabase.5") + kstr); //$NON-NLS-1$
        }

       try {
            XmlDatabaseReader reader = new XmlDatabaseReader(this.nmgr);
            Node node = reader.read(f);

            return node;
        } catch (ParserConfigurationException x) {
            this.app.logError(Messages.getString("XmlDatabase.6") +f, x); //$NON-NLS-1$
            throw new IOException(x.toString());
        } catch (SAXException x) {
            this.app.logError(Messages.getString("XmlDatabase.7") +f, x); //$NON-NLS-1$
            throw new IOException(x.toString());
        }
    }
    /**
     * Save a node with the given key. Writes the node to a temporary file
     * which is copied to its final name when the transaction is committed.
     *
     * @param txn
     * @param kstr
     * @param node
     * @throws java.io.IOException
     */
    public void insertNode(TransactionInterface txn, String kstr, NodeInterface node)
                throws IOException {
        File f = new File(this.dbHomeDir, kstr + ".xml"); //$NON-NLS-1$

        if (f.exists()) {
            throw new IOException(Messages.getString("XmlDatabase.8") + kstr); //$NON-NLS-1$
        }

        // apart from the above check insertNode() is equivalent to updateNode()
        updateNode(txn, kstr, node);
    }

    /**
     * Update a node with the given key. Writes the node to a temporary file
     * which is copied to its final name when the transaction is committed.
     *
     * @param txn
     * @param kstr
     * @param node
     * @throws java.io.IOException
     */
    public void updateNode(TransactionInterface txn, String kstr, NodeInterface node)
                throws IOException {
        XmlWriter writer = null;
        File tmp = File.createTempFile(kstr + ".xml.", ".tmp", this.dbHomeDir); //$NON-NLS-1$ //$NON-NLS-2$

        if (this.encoding != null) {
            writer = new XmlWriter(tmp, this.encoding);
        } else {
            writer = new XmlWriter(tmp);
        }

        writer.setMaxLevels(1);
        writer.write(node);
        writer.close();

        File file = new File(this.dbHomeDir, kstr+".xml"); //$NON-NLS-1$
        if (file.exists() && !file.canWrite()) {
            throw new IOException(Messages.getString("XmlDatabase.9")+file); //$NON-NLS-1$
        }
        Resource res = new Resource(file, tmp);
        txn.addResource(res, TransactionInterface.ADDED);
    }

    /**
     * Marks an element from the database as deleted
     *
     * @param txn
     * @param kstr
     * @throws IOException
     */
    public void deleteNode(TransactionInterface txn, String kstr) {
        Resource res = new Resource(new File(this.dbHomeDir, kstr+".xml"), null); //$NON-NLS-1$
        txn.addResource(res, TransactionInterface.DELETED);
    }

    /**
     * set the file encoding to use
     *
     * @param encoding the database's file encoding
     */
    public void setEncoding(String encoding) {
        this.encoding = encoding;
    }

    /**
     * get the file encoding used by this database
     *
     * @return the file encoding used by this database
     */
    public String getEncoding() {
        return this.encoding;
    }

    class XmlTransaction implements TransactionInterface {

        ArrayList writeFiles = new ArrayList();
        ArrayList deleteFiles = new ArrayList();

        /**
         * Complete the transaction by making its changes persistent.
         */
        public void commit() throws DatabaseException {
            // move through updated/created files and persist them
            int l = this.writeFiles.size();
            for (int i=0; i<l; i++) {
                Resource res = (Resource) this.writeFiles.get(i);
                try {
                    // because of a Java/Windows quirk, we have to delete
                    // the existing file before trying to overwrite it
                    if (res.file.exists()) {
                        res.file.delete();
                    }
                    // move temporary file to permanent name
                    if (res.tmpfile.renameTo(res.file)) {
                        // success - delete tmp file
                        res.tmpfile.delete();
                    } else {
                        // error - leave tmp file and print a message
                        XmlDatabase.this.app.logError(Messages.getString("XmlDatabase.10")+res.file); //$NON-NLS-1$
                        XmlDatabase.this.app.logError(Messages.getString("XmlDatabase.11")+res.tmpfile); //$NON-NLS-1$
                    }
                } catch (SecurityException ignore) {
                    // shouldn't happen
                }
            }

            // move through deleted files and delete them
            l = this.deleteFiles.size();
            for (int i=0; i<l; i++) {
                Resource res = (Resource) this.deleteFiles.get(i);
                // delete files enlisted as deleted
                try {
                    res.file.delete();
                } catch (SecurityException ignore) {
                    // shouldn't happen
                }
            }
            // clear registered resources
            this.writeFiles.clear();
            this.deleteFiles.clear();
        }

        /**
         * Rollback the transaction, forgetting the changed items
         */
        public void abort() throws DatabaseException {
            int l = this.writeFiles.size();
            for (int i=0; i<l; i++) {
                Resource res = (Resource) this.writeFiles.get(i);
                // delete tmp files created by this transaction
                try {
                    res.tmpfile.delete();
                } catch (SecurityException ignore) {
                    // shouldn't happen
                }
            }

            // clear registered resources
            this.writeFiles.clear();
            this.deleteFiles.clear();
        }

        /**
         * Adds a resource to the list of resources encompassed by this transaction
         *
         * @param res the resource to add
         * @param status the status of the resource (ADDED|UPDATED|DELETED)
         */
        public void addResource(Object res, int status)
               throws DatabaseException {
            if (status == DELETED) {
                this.deleteFiles.add(res);
            } else {
                this.writeFiles.add(res);
            }
        }

    }

    /**
     * A holder class for two files, the temporary file and the permanent one
     */
    class Resource {
        File tmpfile;
        File file;

        public Resource(File file, File tmpfile) {
            this.file = file;
            this.tmpfile = tmpfile;
        }
    }


    class IDGenerator {

    }

}
