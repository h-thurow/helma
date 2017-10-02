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

package helma.objectmodel.db;

import helma.util.StringUtils;


/**
 *  This class describes a parent relation between releational nodes.
 */
public class ParentInfo {
    public final String propname;
    public final String[] virtualnames;
    public final String collectionname;
    public final boolean isroot;

    /**
     * Creates a new ParentInfo object.
     *
     * @param desc a single parent info descriptor
     */
    public ParentInfo(String desc) {

        // [named] isn't used anymore, we just want to keep the parsing compatible.
        int n = desc.indexOf("[named]"); //$NON-NLS-1$
        desc = n > -1 ? desc.substring(0, n) : desc;

        String[] parts = StringUtils.split(desc, "."); //$NON-NLS-1$

        switch (parts.length) {
        	case 0:
        		this.propname = this.collectionname = null;
        		this.virtualnames = new String[0];
        		break;
        	case 1:
        		this.propname = parts[0].trim();
        		this.virtualnames = new String[0];
        		this.collectionname = null;
        		break;
        	default:
        		this.propname = parts[0].trim();
        		this.virtualnames = new String[parts.length - 2];
        		this.collectionname = parts[parts.length - 1];
        		
        		for (int i = 1; i < parts.length - 1; i++) {
            		this.virtualnames[i - 1] = parts[i];
            	}
        }

        this.isroot = "root".equalsIgnoreCase(this.propname); //$NON-NLS-1$
    }

    /**
     * @return a string representation of the parent info
     */
    @Override
    public String toString() {
        StringBuffer b = new StringBuffer("ParentInfo[").append(this.propname); //$NON-NLS-1$
        if (this.virtualnames.length > 0) {
        	for (int i = 0; i < this.virtualnames.length; i++) {
        		b.append(".").append(this.virtualnames[i]);	 //$NON-NLS-1$
        	}
        }
        if (this.collectionname != null)
            b.append(".").append(this.collectionname); //$NON-NLS-1$
        return b.append("]").toString(); //$NON-NLS-1$
    }
}
