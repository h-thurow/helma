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

package helma.objectmodel;

import java.io.Serializable;

/**
 * This is passed to NodeListeners when a node is modified.
 */
public class NodeEvent implements Serializable {
    private static final long serialVersionUID = 4322426080131107600L;

    public static final int CONTENT_CHANGED = 0;
    public static final int PROPERTIES_CHANGED = 1;
    public static final int NODE_REMOVED = 2;
    public static final int NODE_RENAMED = 3;
    public static final int SUBNODE_ADDED = 4;
    public static final int SUBNODE_REMOVED = 5;
    public int type;
    public String id;
    public transient NodeInterface node;
    public transient Object arg;

    /**
     * Creates a new NodeEvent object.
     *
     * @param node ...
     * @param type ...
     */
    public NodeEvent(NodeInterface node, int type) {
        super();
        this.node = node;
        this.id = node.getID();
        this.type = type;
    }

    /**
     * Creates a new NodeEvent object.
     *
     * @param node ...
     * @param type ...
     * @param arg ...
     */
    public NodeEvent(NodeInterface node, int type, Object arg) {
        super();
        this.node = node;
        this.id = node.getID();
        this.type = type;
        this.arg = arg;
    }

    /**
     *
     *
     * @return ...
     */
    @Override
    public String toString() {
        switch (this.type) {
            case CONTENT_CHANGED:
                return Messages.getString("NodeEvent.0"); //$NON-NLS-1$

            case PROPERTIES_CHANGED:
                return Messages.getString("NodeEvent.1"); //$NON-NLS-1$

            case NODE_REMOVED:
                return Messages.getString("NodeEvent.2"); //$NON-NLS-1$

            case NODE_RENAMED:
                return Messages.getString("NodeEvent.3"); //$NON-NLS-1$

            case SUBNODE_ADDED:
                return Messages.getString("NodeEvent.4"); //$NON-NLS-1$

            case SUBNODE_REMOVED:
                return Messages.getString("NodeEvent.5"); //$NON-NLS-1$
        }

        return Messages.getString("NodeEvent.6"); //$NON-NLS-1$
    }
}
