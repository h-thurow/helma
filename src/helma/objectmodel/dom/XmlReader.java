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

package helma.objectmodel.dom;

import helma.objectmodel.INode;
import helma.objectmodel.db.WrappedNodeManager;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Stack;

/**
 * 
 */
public final class XmlReader extends DefaultHandler implements XmlConstants {
    static SAXParserFactory factory = SAXParserFactory.newInstance();
    private INode rootNode;
    private INode currentNode;
    private Stack nodeStack;
    private HashMap convertedNodes;
    private String elementType = null;
    private String elementName = null;
    private StringBuffer charBuffer = null;
    boolean parsingHopObject;
    WrappedNodeManager nmgr;

    /**
     * Creates a new XmlReader object.
     */
    public XmlReader(WrappedNodeManager nmgr) {
        this.nmgr = nmgr;
    }

    /**
     * main entry to read an xml-file.
     */
    public INode read(File file, INode helmaNode)
               throws ParserConfigurationException, SAXException, IOException {
        try {
            return read(new FileInputStream(file), helmaNode);
        } catch (FileNotFoundException notfound) {
            System.err.println(Messages.getString("XmlReader.0") + file.getAbsolutePath()); //$NON-NLS-1$

            return helmaNode;
        }
    }

    /**
     * read an InputStream with xml-content.
     */
    public INode read(InputStream in, INode helmaNode)
               throws ParserConfigurationException, SAXException, IOException {
        return read(new InputSource(in), helmaNode);
    }

    /**
     * read an character reader with xml-content.
     */
    public INode read(Reader in, INode helmaNode)
               throws ParserConfigurationException, SAXException, IOException {
        return read(new InputSource(in), helmaNode);
    }

    /**
     * read an InputSource with xml-content.
     */
    public INode read(InputSource in, INode helmaNode)
               throws ParserConfigurationException, SAXException, IOException {
        if (helmaNode == null) {
            throw new RuntimeException(Messages.getString("XmlReader.1")); //$NON-NLS-1$
        }

        SAXParser parser = factory.newSAXParser();

        this.rootNode = helmaNode;
        this.currentNode = null;
        this.convertedNodes = new HashMap();
        this.nodeStack = new Stack();
        this.parsingHopObject = true;

        parser.parse(in, this);

        return this.rootNode;
    }

    /**
     *
     *
     * @param namespaceURI ...
     * @param localName ...
     * @param qName ...
     * @param atts ...
     *
     * @throws SAXException ...
     */
    @Override
    public void startElement(String namespaceURI, String localName, String qName,
                             Attributes atts) throws SAXException {
        // System.err.println ("XML-READ: startElement "+namespaceURI+", "+localName+", "+qName+", "+atts.getValue("id"));
        // discard the first element called xmlroot
        if ("xmlroot".equals(qName) && (this.currentNode == null)) { //$NON-NLS-1$
            return;
        }

        // if currentNode is null, this must be the hopobject node
        String id = atts.getValue("id"); //$NON-NLS-1$

        if (id != null) {
            // check if there is a current node.
            if (this.currentNode == null) {
                // If currentNode is null, this is the root node we're parsing.
                this.currentNode = this.rootNode;
            } else if ("hop:child".equals(qName)) { //$NON-NLS-1$
                // it's an anonymous child node
                this.nodeStack.push(this.currentNode);
                this.currentNode = this.currentNode.createNode(null);
            } else {
                // it's a named node property
                this.nodeStack.push(this.currentNode);

                // property name may be encoded as "propertyname" attribute,
                // otherwise it is the element name
                String propName = atts.getValue("propertyname"); //$NON-NLS-1$

                if (propName == null) {
                    propName = qName;
                }

                this.currentNode = this.currentNode.createNode(propName);
            }

            // set the prototype on the current node and
            // add it to the map of parsed nodes.
            String prototype = atts.getValue("prototype"); //$NON-NLS-1$

            if (!"".equals(prototype) && !"hopobject".equals(prototype)) { //$NON-NLS-1$ //$NON-NLS-2$
                this.currentNode.setPrototype(prototype);
                this.currentNode.setDbMapping(this.nmgr.getDbMapping(prototype));
            }

            String key = id + "-" + prototype; //$NON-NLS-1$

            this.convertedNodes.put(key, this.currentNode);

            return;
        }

        // check if we have a currentNode to set properties on,
        // otherwise throw exception.
        if (this.currentNode == null) {
            throw new SAXException(Messages.getString("XmlReader.2")); //$NON-NLS-1$
        }

        // check if we are inside a HopObject - otherwise throw an exception
        if (!this.parsingHopObject) {
            throw new SAXException(Messages.getString("XmlReader.3")); //$NON-NLS-1$
        }

        // if we got so far, the element is not a hopobject. Set flag to prevent
        // the hopobject stack to be popped when the element
        // is closed.
        this.parsingHopObject = false;

        // Is it a reference to an already parsed node?
        String idref = atts.getValue("idref"); //$NON-NLS-1$

        if (idref != null) {
            // a reference to a node that should have been parsed
            // and lying in our cache of parsed nodes.
            String prototyperef = atts.getValue("prototyperef"); //$NON-NLS-1$
            String key = idref + "-" + prototyperef; //$NON-NLS-1$
            INode n = (INode) this.convertedNodes.get(key);

            // if not a reference to a node we already read, try to
            // resolve against the NodeManager.
            if (n == null) {
                n = this.nmgr.getNode(idref, this.nmgr.getDbMapping(prototyperef));
            }

            if (n != null) {
                if ("hop:child".equals(qName)) { //$NON-NLS-1$
                    // add an already parsed node as child to current node
                    this.currentNode.addNode(n);
                } else {
                    // set an already parsed node as node property to current node
                    // property name may be encoded as "propertyname" attribute,
                    // otherwise it is the element name
                    String propName = atts.getValue("propertyname"); //$NON-NLS-1$

                    if (propName == null) {
                        propName = qName;
                    }
                    
                    if ("hop:parent".equals(qName)) { //$NON-NLS-1$
                        // FIXME: we ought to set parent here, but we're 
                        // dealing with INodes, which don't have a setParent().
                    } else {
                        this.currentNode.setNode(propName, n);
                    }
                }
            }
        } else {
            // It's a primitive property. Remember the property name and type
            // so we can properly parse/interpret the character data when we
            // get it later on.
            this.elementType = atts.getValue("type"); //$NON-NLS-1$

            if (this.elementType == null) {
                this.elementType = "string"; //$NON-NLS-1$
            }

            // property name may be encoded as "propertyname" attribute,
            // otherwise it is the element name
            this.elementName = atts.getValue("propertyname"); //$NON-NLS-1$

            if (this.elementName == null) {
                this.elementName = qName;
            }

            if (this.charBuffer == null) {
                this.charBuffer = new StringBuffer();
            } else {
                this.charBuffer.setLength(0);
            }
        }
    }

    /**
     *
     *
     * @param ch ...
     * @param start ...
     * @param length ...
     *
     * @throws SAXException ...
     */
    @Override
    public void characters(char[] ch, int start, int length) {
        // System.err.println ("CHARACTERS: "+new String (ch, start, length));
        // append chars to char buffer
        if (this.elementType != null) {
            this.charBuffer.append(ch, start, length);
        }
    }

    /**
     *
     *
     * @param namespaceURI ...
     * @param localName ...
     * @param qName ...
     *
     * @throws SAXException ...
     */
    @Override
    public void endElement(String namespaceURI, String localName, String qName) {
        if (this.elementType != null) {
            String charValue = this.charBuffer.toString();

            this.charBuffer.setLength(0);

            if ("boolean".equals(this.elementType)) { //$NON-NLS-1$
                if ("true".equals(charValue)) { //$NON-NLS-1$
                    this.currentNode.setBoolean(this.elementName, true);
                } else {
                    this.currentNode.setBoolean(this.elementName, false);
                }
            } else if ("date".equals(this.elementType)) { //$NON-NLS-1$
                SimpleDateFormat format = new SimpleDateFormat(DATEFORMAT);

                try {
                    Date date = format.parse(charValue);

                    this.currentNode.setDate(this.elementName, date);
                } catch (ParseException e) {
                    this.currentNode.setString(this.elementName, charValue);
                }
            } else if ("float".equals(this.elementType)) { //$NON-NLS-1$
                this.currentNode.setFloat(this.elementName, (new Double(charValue)).doubleValue());
            } else if ("integer".equals(this.elementType)) { //$NON-NLS-1$
                this.currentNode.setInteger(this.elementName, (new Long(charValue)).longValue());
            } else {
                this.currentNode.setString(this.elementName, charValue);
            }

            this.elementName = null;
            this.elementType = null;
            charValue = null;
        }

        if (this.parsingHopObject && !this.nodeStack.isEmpty()) {
            this.currentNode = (INode) this.nodeStack.pop();
        } else {
            this.parsingHopObject = true; // the next element end tag closes a hopobject again
        }
    }
}
