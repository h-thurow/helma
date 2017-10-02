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

package helma.scripting.rhino;

/**
 *  A helper class to serve as Map key for two strings
 */
final class SkinKey {

    private final String type;

    private final String id;

    // lazily initialized hashcode
    private transient int hashcode = 0;

    /**
     * make a key for a persistent Object, describing its datasource and id.
     */
    public SkinKey(String type, String id) {
        this.type = type == null ? "" : type; //$NON-NLS-1$
        this.id = id;
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

        if (!(what instanceof SkinKey)) {
            return false;
        }

        SkinKey k = (SkinKey) what;

        return (this.type.equals(k.type)) && (this.id.equals(k.id));
    }

    /**
     *
     *
     * @return this key's hash code
     */
    @Override
    public int hashCode() {
        if (this.hashcode == 0) {
            this.hashcode = (17 + (37 * this.type.hashCode()) +
                        (+37 * this.id.hashCode()));
        }

        return this.hashcode;
    }

}
