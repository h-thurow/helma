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

package helma.objectmodel.db;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;


/**
 *  This is the internal representation of a database key. It is constructed
 *  from the logical table (type) name and the object's primary key
 *  within the table. Currently only single keys are supported.
 */
public final class DbKey implements KeyInterface, Serializable {
    // the name of the prototype which defines the storage of this object.
    // this is the name of the object's prototype, or one of its ancestors.
    // If null, the object is stored in the embedded db.
    private String storageName;

    // the id that defines this key's object within the above storage space
    private String id;

    // lazily initialized hashcode
    private transient int hashcode = 0;

    static final long serialVersionUID = 1618863960930966588L;

    /**
     * make a key for a persistent Object, describing its datasource and id.
     */
    public DbKey(DbMapping dbmap, String id) {
        if (id == null) {
            throw new IllegalArgumentException(Messages.getString("DbKey.0")); //$NON-NLS-1$
        }
        this.id = id;
        this.storageName = (dbmap == null) ? null : dbmap.getStorageTypeName();
    }

    /**
     *
     *
     * @param what the other key to be compared with this one
     *
     * @return true if both keys are identical
     */
    @Override
    public boolean equals(Object what) {
        if (what == this) {
            return true;
        }

        if (!(what instanceof DbKey)) {
            return false;
        }

        DbKey k = (DbKey) what;

        // storageName is an interned string (by DbMapping, from where we got it)
        // so we can compare by using == instead of the equals method.
        return (this.storageName == k.storageName) && ((this.id == k.id) || this.id.equals(k.id));
    }

    /**
     *
     *
     * @return this key's hash code
     */
    @Override
    public int hashCode() {
        if (this.hashcode == 0) {
            this.hashcode = (this.storageName == null) ? (17 + (37 * this.id.hashCode()))
                                             : (17 + (37 * this.storageName.hashCode()) +
                                             (+37 * this.id.hashCode()));
        }

        return this.hashcode;
    }

    /**
     *
     *
     * @return the key of this key's object's parent object
     */
    public KeyInterface getParentKey() {
        return null;
    }

    /**
     *
     *
     * @return the unique storage name for this key's object
     */
    public String getStorageName() {
        return this.storageName;
    }

    /**
     *
     *
     * @return this key's object's id
     */
    public String getID() {
        return this.id;
    }

    /**
     *
     *
     * @return a string representation for this key
     */
    @Override
    public String toString() {
        return (this.storageName == null) ? ("[" + this.id + "]") : (this.storageName + "[" + this.id + "]");  //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
    }

    // We implement write/readObject to set storageName
    // to the interned version of the string.

    private void writeObject(ObjectOutputStream stream) throws IOException {
        stream.writeObject(this.storageName);
        stream.writeObject(this.id);
    }

    private void readObject(ObjectInputStream stream)
                                        throws IOException, ClassNotFoundException {
        this.storageName = (String) stream.readObject();
        this.id = (String) stream.readObject();
        // if storageName is not null, set it to the interned version
        if (this.storageName != null) {
            this.storageName = this.storageName.intern();
        }
    }

}
