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
        		propname = collectionname = null;
        		virtualnames = new String[0];
        		break;
        	case 1:
        		propname = parts[0].trim();
        		virtualnames = new String[0];
        		collectionname = null;
        		break;
        	default:
        		propname = parts[0].trim();
        		virtualnames = new String[parts.length - 2];
        		collectionname = parts[parts.length - 1];
        		
        		for (int i = 1; i < parts.length - 1; i++) {
            		virtualnames[i - 1] = parts[i];
            	}
        }

        isroot = "root".equalsIgnoreCase(propname); //$NON-NLS-1$
    }

    /**
     * @return a string representation of the parent info
     */
    public String toString() {
        StringBuffer b = new StringBuffer("ParentInfo[").append(propname); //$NON-NLS-1$
        if (virtualnames.length > 0) {
        	for (int i = 0; i < virtualnames.length; i++) {
        		b.append(".").append(virtualnames[i]);	 //$NON-NLS-1$
        	}
        }
        if (collectionname != null)
            b.append(".").append(collectionname); //$NON-NLS-1$
        return b.append("]").toString(); //$NON-NLS-1$
    }
}
