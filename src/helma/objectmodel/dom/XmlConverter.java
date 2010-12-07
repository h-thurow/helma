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
import helma.util.SystemProperties;
import org.w3c.dom.*;
import org.xml.sax.InputSource;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * 
 */
public class XmlConverter implements XmlConstants {
    private boolean DEBUG = false;
    private boolean sparse = false;
    private Properties props;
    private char defaultSeparator = '_';
    private int offset = 0;

    /**
     * Creates a new XmlConverter object.
     */
    public XmlConverter() {
        props = new SystemProperties();
    }

    /**
     * Creates a new XmlConverter object.
     *
     * @param propFile ...
     */
    public XmlConverter(String propFile) {
        props = new SystemProperties(propFile);
        extractProperties(props);
    }

    /**
     * Creates a new XmlConverter object.
     *
     * @param propFile ...
     */
    public XmlConverter(File propFile) {
        this(propFile.getAbsolutePath());
    }

    /**
     * Creates a new XmlConverter object.
     *
     * @param props ...
     */
    public XmlConverter(Properties props) {
        this.props = props;
        extractProperties(props);
    }

    /**
     *
     *
     * @param desc ...
     * @param helmaNode ...
     *
     * @return ...
     *
     * @throws RuntimeException ...
     */
    public INode convert(String desc, INode helmaNode)
                  throws RuntimeException {
        try {
            return convert(new URL(desc), helmaNode);
        } catch (MalformedURLException notanurl) {
            try {
                return convert(new File(desc), helmaNode);
            } catch (FileNotFoundException notfound) {
                throw new RuntimeException(Messages.getString("XmlConverter.0") + desc); //$NON-NLS-1$
            }
        } catch (IOException ioerror) {
            throw new RuntimeException(Messages.getString("XmlConverter.1") + desc); //$NON-NLS-1$
        }
    }

    /**
     *
     *
     * @param file ...
     * @param helmaNode ...
     *
     * @return ...
     *
     * @throws RuntimeException ...
     * @throws FileNotFoundException ...
     */
    public INode convert(File file, INode helmaNode)
                  throws RuntimeException, FileNotFoundException {
        return convert(new FileInputStream(file), helmaNode);
    }

    /**
     *
     *
     * @param url ...
     * @param helmaNode ...
     *
     * @return ...
     *
     * @throws RuntimeException ...
     * @throws IOException ...
     * @throws MalformedURLException ...
     */
    public INode convert(URL url, INode helmaNode)
                  throws RuntimeException, IOException, MalformedURLException {
        return convert(url.openConnection().getInputStream(), helmaNode);
    }

    /**
     *
     *
     * @param in ...
     * @param helmaNode ...
     *
     * @return ...
     *
     * @throws RuntimeException ...
     */
    public INode convert(InputStream in, INode helmaNode)
                  throws RuntimeException {
        Document document = XmlUtil.parse(in);

        if ((document != null) && (document.getDocumentElement() != null)) {
            return convert(document.getDocumentElement(), helmaNode, new HashMap());
        } else {
            return helmaNode;
        }
    }

    /**
     *
     *
     * @param xml ...
     * @param helmaNode ...
     *
     * @return ...
     *
     * @throws RuntimeException ...
     */
    public INode convertFromString(String xml, INode helmaNode)
                            throws RuntimeException {
        Document document = XmlUtil.parse(new InputSource(new StringReader(xml)));

        if ((document != null) && (document.getDocumentElement() != null)) {
            return convert(document.getDocumentElement(), helmaNode, new HashMap());
        } else {
            return helmaNode;
        }
    }

    /**
     *
     *
     * @param element ...
     * @param helmaNode ...
     * @param nodeCache ...
     *
     * @return ...
     */
    public INode convert(Element element, INode helmaNode, Map nodeCache) {
        offset++;

        // previousNode is used to cache previous nodes with the same prototype
        // so we can reset it in the nodeCache after we've run
        Object previousNode = null;

        if (DEBUG) {
            debug(Messages.getString("XmlConverter.2") + element.getNodeName()); //$NON-NLS-1$
        }

        String prototype = props.getProperty(element.getNodeName() + "._prototype"); //$NON-NLS-1$

        if ((prototype == null) && !sparse) {
            prototype = "HopObject"; //$NON-NLS-1$
        }

        // if we have a prototype (either explicit or implicit "hopobject"),
        // set it on the Helma node and store it in the node cache.
        if (prototype != null) {
            helmaNode.setName(element.getNodeName());
            helmaNode.setPrototype(prototype);
            previousNode = nodeCache.put(prototype, helmaNode);
        }

        // check attributes of the current element
        attributes(element, helmaNode, nodeCache);

        // check child nodes of the current element
        if (element.hasChildNodes()) {
            children(element, helmaNode, nodeCache);
        }

        // if it exists, restore the previous node we've replaced in the node cache.
        if (previousNode != null) {
            nodeCache.put(prototype, previousNode);
        }

        offset--;

        return helmaNode;
    }

    /**
     * parse xml children and create hopobject-children
     */
    private INode children(Element element, helma.objectmodel.INode helmaNode,
                           Map nodeCache) {
        NodeList list = element.getChildNodes();
        int len = list.getLength();
        boolean nodeIsInitialized = !nodeCache.isEmpty();
        StringBuffer textcontent = new StringBuffer();
        String domKey;
        String helmaKey;

        for (int i = 0; i < len; i++) {
            // loop through the list of children
            org.w3c.dom.Node childNode = list.item(i);

            // if the current node hasn't been initialized yet, try if it can
            // be initialized and converted from one of the child elements.
            if (!nodeIsInitialized) {
                if (childNode.getNodeType() == Node.ELEMENT_NODE) {
                    convert((Element) childNode, helmaNode, nodeCache);

                    if (helmaNode.getPrototype() != null) {
                        return helmaNode;
                    }
                }

                continue;
            }

            // if it's text content of this element -> append to StringBuffer
            if ((childNode.getNodeType() == Node.TEXT_NODE) ||
                    (childNode.getNodeType() == Node.CDATA_SECTION_NODE)) {
                textcontent.append(childNode.getNodeValue().trim());

                continue;
            }

            // it's some kind of element (property or child)
            if (childNode.getNodeType() == Node.ELEMENT_NODE) {
                Element childElement = (Element) childNode;

                // get the basic key we have to look for in the properties-table
                domKey = element.getNodeName() + "." + childElement.getNodeName(); //$NON-NLS-1$

                // is there a childtext-2-property mapping?
                if ((props != null) && props.containsKey(domKey + "._text")) { //$NON-NLS-1$
                    helmaKey = props.getProperty(domKey + "._text"); //$NON-NLS-1$

                    if (helmaKey.equals("")) { //$NON-NLS-1$
                        // if property is set but without value, read elementname for this mapping
                        helmaKey = childElement.getNodeName().replace(':',
                                                                      defaultSeparator);
                    }

                    if (DEBUG) {
                        debug(Messages.getString("XmlConverter.3") + helmaKey + //$NON-NLS-1$
                              Messages.getString("XmlConverter.4") + domKey); //$NON-NLS-1$
                    }

                    // check if helmaKey contains an explicit prototype name in which to
                    // set the property.
                    int dot = helmaKey.indexOf("."); //$NON-NLS-1$

                    if (dot > -1) {
                        String prototype = helmaKey.substring(0, dot);
                        INode node = (INode) nodeCache.get(prototype);

                        helmaKey = helmaKey.substring(dot + 1);

                        if ((node != null) && (node.getString(helmaKey) == null)) {
                            node.setString(helmaKey, XmlUtil.getTextContent(childNode));
                        }
                    } else if (helmaNode.getString(helmaKey) == null) {
                        helmaNode.setString(helmaKey, XmlUtil.getTextContent(childNode));

                        if (DEBUG) {
                            debug(Messages.getString("XmlConverter.5") + helmaKey + //$NON-NLS-1$
                                  Messages.getString("XmlConverter.6")); //$NON-NLS-1$
                        }
                    }

                    continue;
                }

                // is there a simple child-2-property mapping?
                // (lets the user define to use only one element and make this a property
                // and simply ignore other elements of the same name)
                if ((props != null) && props.containsKey(domKey + "._property")) { //$NON-NLS-1$
                    helmaKey = props.getProperty(domKey + "._property"); //$NON-NLS-1$

                    // if property is set but without value, read elementname for this mapping:
                    if (helmaKey.equals("")) { //$NON-NLS-1$
                        helmaKey = childElement.getNodeName().replace(':',
                                                                      defaultSeparator);
                    }

                    if (DEBUG) {
                        debug(Messages.getString("XmlConverter.7") + helmaKey + //$NON-NLS-1$
                              Messages.getString("XmlConverter.8") + domKey); //$NON-NLS-1$
                    }

                    // get the node on which to opererate, depending on the helmaKey
                    // value from the properties file.
                    INode node = helmaNode;
                    int dot = helmaKey.indexOf("."); //$NON-NLS-1$

                    if (dot > -1) {
                        String prototype = helmaKey.substring(0, dot);

                        if (!prototype.equalsIgnoreCase(node.getPrototype())) {
                            node = (INode) nodeCache.get(prototype);
                        }

                        helmaKey = helmaKey.substring(dot + 1);
                    }

                    if (node == null) {
                        continue;
                    }

                    if (node.getNode(helmaKey) == null) {
                        convert(childElement, node.createNode(helmaKey), nodeCache);

                        if (DEBUG) {
                            debug(Messages.getString("XmlConverter.9") + childElement.toString() + //$NON-NLS-1$
                                  node.getNode(helmaKey).toString());
                        }
                    }

                    continue;
                }

                // map it to one of the children-lists
                helma.objectmodel.INode newHelmaNode = null;
                String childrenMapping = props.getProperty(element.getNodeName() +
                                                           "._children"); //$NON-NLS-1$

                // do we need a mapping directly among _children of helmaNode?
                // can either be through property elname._children=_all or elname._children=childname
                if ((childrenMapping != null) &&
                        (childrenMapping.equals("_all") || //$NON-NLS-1$
                        childrenMapping.equals(childElement.getNodeName()))) {
                    newHelmaNode = convert(childElement, helmaNode.createNode(null),
                                           nodeCache);
                }

                // in which virtual subnode collection should objects of this type be stored?
                helmaKey = props.getProperty(domKey);

                if ((helmaKey == null) && !sparse) {
                    helmaKey = childElement.getNodeName().replace(':', defaultSeparator);
                }

                if (helmaKey == null) {
                    // we don't map this child element itself since we do
                    // sparse parsing, but there may be something of interest
                    // in the child's attributes and child elements.
                    attributes(childElement, helmaNode, nodeCache);
                    children(childElement, helmaNode, nodeCache);

                    continue;
                }

                // get the node on which to opererate, depending on the helmaKey
                // value from the properties file.
                INode node = helmaNode;
                int dot = helmaKey.indexOf("."); //$NON-NLS-1$

                if (dot > -1) {
                    String prototype = helmaKey.substring(0, dot);

                    if (!prototype.equalsIgnoreCase(node.getPrototype())) {
                        node = (INode) nodeCache.get(prototype);
                    }

                    helmaKey = helmaKey.substring(dot + 1);
                }

                if (node == null) {
                    continue;
                }

                // try to get the virtual node
                INode worknode = null;

                if ("_children".equals(helmaKey)) { //$NON-NLS-1$
                    worknode = node;
                } else {
                    worknode = node.getNode(helmaKey);

                    if (worknode == null) {
                        // if virtual node doesn't exist, create it
                        worknode = helmaNode.createNode(helmaKey);
                    }
                }

                if (DEBUG) {
                    debug(Messages.getString("XmlConverter.10") + childElement.getNodeName() + //$NON-NLS-1$
                          Messages.getString("XmlConverter.11") + worknode.toString()); //$NON-NLS-1$
                }

                // now mount it, possibly re-using the helmaNode that's been created before
                if (newHelmaNode != null) {
                    worknode.addNode(newHelmaNode);
                } else {
                    convert(childElement, worknode.createNode(null), nodeCache);
                }
            }

            // forget about other types (comments etc)
            continue;
        }

        // if there's some text content for this element, map it:
        if ((textcontent.length() > 0) && !sparse) {
            helmaKey = props.getProperty(element.getNodeName() + "._text"); //$NON-NLS-1$

            if (helmaKey == null) {
                helmaKey = "text"; //$NON-NLS-1$
            }

            if (DEBUG) {
                debug(Messages.getString("XmlConverter.12") + textcontent + Messages.getString("XmlConverter.13") + helmaKey + //$NON-NLS-1$ //$NON-NLS-2$
                      Messages.getString("XmlConverter.14") + helmaNode); //$NON-NLS-1$
            }

            helmaNode.setString(helmaKey, textcontent.toString().trim());
        }

        return helmaNode;
    }

    /**
     * set element's attributes as properties of helmaNode
     */
    private INode attributes(Element element, INode helmaNode, Map nodeCache) {
        NamedNodeMap nnm = element.getAttributes();
        int len = nnm.getLength();

        for (int i = 0; i < len; i++) {
            org.w3c.dom.Node attr = nnm.item(i);
            String helmaKey = props.getProperty(element.getNodeName() + "._attribute." + //$NON-NLS-1$
                                                attr.getNodeName());

            // unless we only map explicit attributes, use attribute name as property name
            // in case no property name was defined.
            if ((helmaKey == null) && !sparse) {
                helmaKey = attr.getNodeName().replace(':', defaultSeparator);
            }

            if (helmaKey != null) {
                // check if the mapping contains the prototype to which 
                // the property should be applied
                int dot = helmaKey.indexOf("."); //$NON-NLS-1$

                if (dot > -1) {
                    String prototype = helmaKey.substring(0, dot);
                    INode node = (INode) nodeCache.get(prototype);

                    if (node != null) {
                        node.setString(helmaKey.substring(dot + 1), attr.getNodeValue());
                    }
                } else if (helmaNode.getPrototype() != null) {
                    helmaNode.setString(helmaKey, attr.getNodeValue());
                }
            }
        }

        return helmaNode;
    }

    /**
     * utility function
     */
    private void extractProperties(Properties props) {
        if (props.containsKey("separator")) { //$NON-NLS-1$
            defaultSeparator = props.getProperty("separator").charAt(0); //$NON-NLS-1$
        }

        sparse = "sparse".equalsIgnoreCase(props.getProperty("_mode")); //$NON-NLS-1$ //$NON-NLS-2$
    }

    /** for testing */
    void debug(Object msg) {
        for (int i = 0; i < offset; i++) {
            System.out.print("   "); //$NON-NLS-1$
        }

        System.out.println(msg.toString());
    }

    /**
     *
     *
     * @param args ...
     */
    public static void main(String[] args) {
    }
}
