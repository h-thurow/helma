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
 * Contributions:
 *   Daniel Ruthardt
 *   Copyright 2010 dowee Limited. All rights reserved. 
 *
 * $RCSfile$
 * $Author$
 * $Revision$
 * $Date$
 */

package helma.objectmodel.dom;


import helma.objectmodel.NodeInterface;
import helma.objectmodel.PropertyInterface;
import helma.objectmodel.db.DbMapping;
import helma.objectmodel.db.Node;
import helma.util.HtmlEncoder;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

/**
 * 
 */
public class XmlWriter extends OutputStreamWriter implements XmlConstantsInterface {
    private final static String LINESEPARATOR = System.getProperty("line.separator"); //$NON-NLS-1$
    private static int fileid;
    private Vector convertedNodes;
    private int maxLevels = 3;
    private String indent = "  "; //$NON-NLS-1$
    private StringBuffer prefix = new StringBuffer();
    private SimpleDateFormat format = new SimpleDateFormat(DATEFORMAT);
    private boolean dbmode = true;

    // the helma.objectmodel.NodeStateInterface of the node we're writing
    public int rootState;

    // Only add encoding to XML declaration if it was explicitly set, not when we're using
    // the platform's standard encoding.
    private String explicitEncoding;

    /**
     * empty constructor, will use System.out as outputstream.
     */
    public XmlWriter() {
        super(System.out);
    }

    /**
     * Creates a new XmlWriter object.
     *
     * @param out ...
     */
    public XmlWriter(OutputStream out) {
        super(out);
    }

    /**
     * Creates a new XmlWriter object.
     *
     * @param out ...
     * @param enc ...
     *
     * @throws UnsupportedEncodingException ...
     */
    public XmlWriter(OutputStream out, String enc) throws UnsupportedEncodingException {
        super(out, enc);
        this.explicitEncoding = enc;
    }

    /**
     * Creates a new XmlWriter object.
     *
     * @param desc ...
     *
     * @throws FileNotFoundException ...
     */
    public XmlWriter(String desc) throws FileNotFoundException {
        super(new FileOutputStream(desc));
    }

    /**
     * Creates a new XmlWriter object.
     *
     * @param desc ...
     * @param enc ...
     *
     * @throws FileNotFoundException ...
     * @throws UnsupportedEncodingException ...
     */
    public XmlWriter(String desc, String enc)
              throws FileNotFoundException, UnsupportedEncodingException {
        super(new FileOutputStream(desc), enc);
        this.explicitEncoding = enc;
    }

    /**
     * Creates a new XmlWriter object.
     *
     * @param file ...
     *
     * @throws FileNotFoundException ...
     */
    public XmlWriter(File file) throws FileNotFoundException {
        super(new FileOutputStream(file));
    }

    /**
     * Creates a new XmlWriter object.
     *
     * @param file ...
     * @param enc ...
     *
     * @throws FileNotFoundException ...
     * @throws UnsupportedEncodingException ...
     */
    public XmlWriter(File file, String enc)
              throws FileNotFoundException, UnsupportedEncodingException {
        super(new FileOutputStream(file), enc);
        this.explicitEncoding = enc;
    }

    // Set of prototypes at which to stop writing.
    // private Set stopTypes = null;

    /**
     * create ids that can be used for temporary files.
     */
    public static int generateID() {
        return fileid++;
    }

    /**
     * by default writing only descends 50 levels into the node tree to prevent
     * infite loops. number can be changed here.
     */
    public void setMaxLevels(int levels) {
        this.maxLevels = levels;
    }

    /**
     *
     *
     * @param dbmode ...
     */
    public void setDatabaseMode(boolean dbmode) {
        this.dbmode = dbmode;
    }

    /**
     *  Set a group of prototypes at which to stop XML serialization.
     */

    /* public void setStopTypes (Set set) {
       this.stopTypes = set;
       } */

    /**
     * set the number of space chars
     */
    public void setIndent(int ct) {
        StringBuffer tmp = new StringBuffer();

        for (int i = 0; i < ct; i++) {
            tmp.append(" "); //$NON-NLS-1$
        }

        this.indent = tmp.toString();
    }

    /**
     * starting point for printing a node tree.
     * creates document header too and initializes
     * the cache of already converted nodes.
     */
    public boolean write(NodeInterface node) throws IOException {
        this.rootState = node.getState();
        this.convertedNodes = new Vector();

        if (this.explicitEncoding == null) {
            writeln("<?xml version=\"1.0\"?>"); //$NON-NLS-1$
        } else {
            writeln("<?xml version=\"1.0\" encoding=\"" + this.explicitEncoding + "\"?>"); //$NON-NLS-1$ //$NON-NLS-2$
        }
        // add style sheet processing instruction
        writeln("<?xml-stylesheet type=\"text/xsl\" href=\"helma.xsl\"?>"); //$NON-NLS-1$

        // writeln ("<!-- printed by helma object publisher     -->");
        // writeln ("<!-- created " + (new Date()).toString() + " -->" );
        write("<xmlroot xmlns:hop=\""); //$NON-NLS-1$
        write(NAMESPACE);
        writeln("\">"); //$NON-NLS-1$
        write(node, null, null, 0);
        writeln("</xmlroot>"); //$NON-NLS-1$
        this.convertedNodes = null;

        return true;
    }

    /**
     * write a hopobject and print all its properties and children.
     * references are made here if a node already has been fully printed
     * or if this is the last level that's going to be dumped
     */
    public void write(NodeInterface node, String elementName, String propName, int level)
               throws IOException {
        if (node == null) {
            return;
        }

        // if (stopTypes != null && stopTypes.contains (node.getPrototype()))
        // 	return;
        int previousLength = this.prefix.length();

        this.prefix.append(this.indent);

        if (++level > this.maxLevels) {
            writeReferenceTag(node, elementName, propName);
            this.prefix.setLength(previousLength);

            return;
        }

        if (this.convertedNodes.contains(node)) {
            writeReferenceTag(node, elementName, propName);
        } else if (this.rootState == NodeInterface.TRANSIENT &&
                   node.getState() > NodeInterface.TRANSIENT) {
            // if we are writing a transient node, and that node
            // holds a reference to a persistent one, just write a
            // reference tag to that persistent node.
            writeReferenceTag(node, elementName, propName);

        } else {
            this.convertedNodes.addElement(node);
            writeTagOpen(node, elementName, propName);

            NodeInterface parent = node.getParent();

            if (parent != null) {
                writeReferenceTag(parent, "hop:parent", null); //$NON-NLS-1$
            }

            writeProperties(node, level);
            writeChildren(node, level);
            writeTagClose(elementName);
        }

        this.prefix.setLength(previousLength);
    }

    /**
     * loop through properties and print them with their property-name
     * as elementname
     */
    private void writeProperties(NodeInterface node, int level)
                          throws IOException {
        Enumeration e = null;

        if (this.dbmode && node instanceof Node) {
            // a newly constructed db.Node doesn't have a propMap,
            // but returns an enumeration of all it's db-mapped properties
            Hashtable props = ((Node) node).getPropMap();

            if (props == null) {
                return;
            }

            e = props.keys();
        } else {
            e = node.properties();
        }

        while (e.hasMoreElements()) {
            String key = (String) e.nextElement();
            if (this.dbmode && key.charAt(0) == '_') {
                continue;
            }
            PropertyInterface prop = node.get(key);

            if (prop != null) {
                boolean validName = isValidElementName(key);
                String elementName;
                String propName;

                if (validName) {
                    elementName = key;
                    propName = null;
                } else {
                    elementName = "property"; //$NON-NLS-1$
                    propName = key;
                }

                int type = prop.getType();

                if (type == PropertyInterface.NODE) {
                    write(prop.getNodeValue(), elementName, propName, level);
                } else {
                    writeProperty(prop, elementName, propName);
                }
            }
        }
    }

    /* public void writeNullProperty (String key) throws IOException {
       write (prefix.toString());
       write (indent);
       write ("<");
       write (key);
       write (" type=\"null\"/>");
       write (LINESEPARATOR);
       } */

    /**
     * write a single property, set attribute type according to type,
     * apply xml-encoding.
     */
    public void writeProperty(PropertyInterface property, String elementName, String propName)
                       throws IOException {
        int propType = property.getType();

        // we can't encode java objects in XML
        if (propType == PropertyInterface.JAVAOBJECT) {
            return;
        }

        write(this.prefix.toString());
        write(this.indent);
        write("<"); //$NON-NLS-1$
        write(elementName);

        if (propName != null) {
            write(" propertyname=\""); //$NON-NLS-1$
            write(HtmlEncoder.encodeXml(propName));
            write("\""); //$NON-NLS-1$
        }

        switch (propType) {
            case PropertyInterface.BOOLEAN:
                write(" type=\"boolean\">"); //$NON-NLS-1$
                write(property.getStringValue());

                break;

            case PropertyInterface.FLOAT:
                write(" type=\"float\">"); //$NON-NLS-1$
                write(property.getStringValue());

                break;

            case PropertyInterface.INTEGER:
                write(" type=\"integer\">"); //$NON-NLS-1$
                write(property.getStringValue());

                break;

            case PropertyInterface.DATE:
                write(" type=\"date\">"); //$NON-NLS-1$
                write(this.format.format(property.getDateValue()));

                break;

            case PropertyInterface.STRING:
                write(">"); //$NON-NLS-1$

                String str = HtmlEncoder.encodeXml(property.getStringValue());

                if (str != null) {
                    write(str);
                }
                break;
        }

        write("</"); //$NON-NLS-1$
        write(elementName);
        write(">"); //$NON-NLS-1$
        write(LINESEPARATOR);
    }

    /**
     * loop through the children-array and print them as <hop:child>
     */
    private void writeChildren(NodeInterface node, int level) throws IOException {
        if (this.dbmode && node instanceof Node) {
            Node dbNode = (Node) node;
            DbMapping smap = (dbNode.getDbMapping() == null) ? null
                                                             : dbNode.getDbMapping()
                                                                     .getSubnodeMapping();

            if ((smap != null) && smap.isRelational()) {
                return;
            }
        }

        Enumeration e = node.getSubnodes();

        while (e.hasMoreElements()) {
            NodeInterface nextNode = (NodeInterface) e.nextElement();

            write(nextNode, "hop:child", null, level); //$NON-NLS-1$
        }
    }

    /**
     * write an opening tag for a node. Include id and prototype, use a
     * name if parameter is non-empty.
     */
    public void writeTagOpen(NodeInterface node, String name, String propName)
                      throws IOException {
        write(this.prefix.toString());
        write("<"); //$NON-NLS-1$
        write((name == null) ? "hopobject" : name); //$NON-NLS-1$
        write(" id=\""); //$NON-NLS-1$
        write(node.getID());

        if (propName != null) {
            write("\" propertyname=\""); //$NON-NLS-1$
            write(HtmlEncoder.encodeXml(propName));
        }

        write("\" name=\""); //$NON-NLS-1$
        write(HtmlEncoder.encodeXml(node.getName()));
        write("\" prototype=\""); //$NON-NLS-1$
        write(getNodePrototype(node));
        write("\" created=\""); //$NON-NLS-1$
        write(Long.toString(node.created()));
        write("\" lastModified=\""); //$NON-NLS-1$
        write(Long.toString(node.lastModified()));

        //FIXME: do we need anonymous-property?
        write("\">"); //$NON-NLS-1$
        write(LINESEPARATOR);
    }

    /**
     * write a closing tag for a node
     * e.g. </root>
     */
    public void writeTagClose(String name) throws IOException {
        write(this.prefix.toString());
        write("</"); //$NON-NLS-1$
        write((name == null) ? "hopobject" : name); //$NON-NLS-1$
        write(">"); //$NON-NLS-1$
        write(LINESEPARATOR);
    }

    /**
     * write a tag holding a reference to an element that has
     * been written out before.
     * e.g. <parent idref="35" prototyperef="hopobject"/>
     */
    public void writeReferenceTag(NodeInterface node, String name, String propName)
                           throws IOException {
        write(this.prefix.toString());
        write("<"); //$NON-NLS-1$
        write((name == null) ? "hopobject" : name); //$NON-NLS-1$
        write(" idref=\""); //$NON-NLS-1$
        write(node.getID());

        if (propName != null) {
            write("\" propertyname=\""); //$NON-NLS-1$
            write(HtmlEncoder.encodeXml(propName));
        }

        write("\" prototyperef=\""); //$NON-NLS-1$
        write(getNodePrototype(node));
        write("\"/>"); //$NON-NLS-1$
        write(LINESEPARATOR);
    }

    /**
     * retrieve prototype-string of a node, defaults to "hopobject"
     */
    private String getNodePrototype(NodeInterface node) {
        if ((node.getPrototype() == null) || "".equals(node.getPrototype())) { //$NON-NLS-1$
            return "hopobject"; //$NON-NLS-1$
        }
        return node.getPrototype();
    }

    /**
     *
     *
     * @param str ...
     *
     * @throws IOException ...
     */
    public void writeln(String str) throws IOException {
        write(str);
        write(LINESEPARATOR);
    }

    /**
     *  Check if a string is usable as XML element name. If not, the name
     *  will be appended as attribute to the XML element. We are
     *  conservative here, preferring to return false rather too often than
     *  not enough.
     */
    private boolean isValidElementName(String str) {
        char c = str.charAt(0);

        if (!Character.isLetter(c)) {
            return false;
        }

        int l = str.length();

        for (int i = 1; i < l; i++) {
            c = str.charAt(i);

            if (!Character.isLetterOrDigit(c) && (c != '-') && (c != '_')) {
                return false;
            }
        }

        return true;
    }
}
