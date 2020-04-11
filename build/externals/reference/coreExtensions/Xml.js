/*
 * Helma License Notice
 *
 * The contents of this file are subject to the Helma License
 * Version 2.0 (the "License"). You may not use this file except in
 * compliance with the License. A copy of the License is available at
 * http://adele.helma.org/download/helma/license.txt
 *
 * Copyright 1998-2007 Helma Software. All Rights Reserved.
 *
 * $RCSfile: Xml.js,v $
 * $Author: zumbrunn $
 * $Revision: 8694 $
 * $Date: 2007-12-10 12:55:38 +0100 (Mon, 10. Dez 2007) $
 */

/**
 * @fileoverview Default properties and methods of the Xml object.
 */

 
/**
 * The Xml object provides easy means to convert XML to HopObject and HopObjects to XML.
 * 
 * @constructor
 * @ignore
 */
function Xml() {}


/**
 * Retrieves an XML document from a given URL and transforms it into a HopObject.
 * <br /><br />
 * Syntax:
 * <pre>Xml.get(urlString)
 * Xml.get(urlString, filenameString)</pre>
 * <br /><br />
 * The optional argument filenameString refers to a file containing 
 * a set of transformation rules how the XML data should be turned 
 * into a HopObject. Please take a look at the RFC for details.
 * <br /><br />
 * Example:
 * <pre>var obj = Xml.get("http://localhost:8080/antville/rss10");
 * for (var i in obj)
 * &nbsp;&nbsp;res.writeln(i + ": " + obj[i]);
 * &nbsp;
 * <i>xmlns_sy: http://purl.org/rss/1.0/modules/syndication/
 * xmlns_dc: http://purl.org/dc/elements/1.1/
 * item: HopObject item
 * channel: HopObject channel
 * xmlns: http://purl.org/rss/1.0/
 * xmlns_rdf: http://www.w3.org/1999/02/22-rdf-syntax-ns#</i></pre>
 * 
 * @param {String} urlString as String, containing the URL to fetch the XML from
 * @return HopObject
 * @type HopObject
 */
Xml.prototype.get = function(urlString) {};


/**
 * Parses the string argument as XML and transforms it into a HopObject.
 * <br /><br />
 * Syntax:
 * <pre>Xml.getFromString(xmlString)
 * Xml.getFromString(xmlString, filenameString)</pre>
 * <br /><br />
 * The optional argument filenameString refers to a file containing a 
 * set of transformation rules how the XML data should be turned into 
 * a HopObject. Please take a look at the RFC for details.
 * <br /><br />
 * Example:
 * <pre>var obj = Xml.getFromString("&lt;a&gt;hello &lt;b&gt;world&lt;/b&gt;&lt;/a&gt;");
 * for (var i in obj)
 * &nbsp;&nbsp;res.writeln(i + ": " + obj[i]);
 * &nbsp;
 * <i>b: HopObject b
 * text: hello</i></pre>
 * 
 * @param {String} xmlString as String in XML format
 * @return HopObject
 * @type HopObject
 */
Xml.prototype.getFromString = function(xmlString) {};


/**
 * Reads an XML tree from file and transforms it into a HopObject.
 * <br /><br />
 * Syntax:
 * <pre>Xml.read(filenameString)
 * Xml.read(filenameString, HopObject)</pre>
 * <br /><br />
 * If an optional HopObject is specified, the HopObject 
 * derived from the XML tree will be added to it.
 * <br /><br />
 * Example:
 * <pre>var obj = Xml.read("/tmp/dump.xml");
 * res.write(obj);
 * <i>HopObject message</i></pre>
 
 * @param String
 * @return HopObject
 * @type HopObject
 */
Xml.prototype.read = function() {};


/**
 * Reads an XML tree from a string and transforms it into a HopObject.
 * <br /><br />
 * Syntax:
 * <pre>Xml.readFromString(String)
 * Xml.readFromString(String, HopObject)</pre>
 * <br /><br />
 * If an optional HopObject is specified, the HopObject 
 * derived from the XML tree will be added to it.
 * <br /><br />
 * Example:
 * <pre>var f = new File("/tmp/dump.xml");
 * var obj = Xml.readFromString(f.readAll());
 * res.write(obj);
 * HopObject message</pre>
 * 
 * @param String
 * @return HopObject
 * @type HopObject
 */
Xml.prototype.readFromString = function() {};


/**
 * Dumps a HopObject as XML tree to a file.
 * <br /><br />
 * Syntax:
 * <pre>Xml.write(HopObject, filenameString)</pre>
 * <br /><br />
 * Example:
 * <pre>var obj = new message();
 * obj.title = "The Message";
 * obj.text = "Don't push me 'cause I'm close to the edge.";
 * root.add(obj);
 * Xml.write(obj, "/tmp/dump.xml");
 * &nbsp;
 * File /tmp/dump.xml: 
 * &nbsp;
 * &lt;?xml version="1.0"?&gt;
 * &lt;!-- printed by helma object publisher     --&gt;
 * &lt;!-- created Fri Jul 12 17:25:24 CEST 2002 --&gt;
 * &lt;xmlroot xmlns:hop="http://www.helma.org/docs/
 * guide/features/database"&gt;
 * &nbsp;&nbsp;&lt;hopobject id="8" name="message" prototype="message" 
 * created="1026487524440" lastModified="1026487524440"&gt;
 * &nbsp;&nbsp;&lt;hop:parent idref="0" prototyperef="root"/&gt;
 * &nbsp;&nbsp;&nbsp;&nbsp;&lt;title&gt;The Message&lt;/title&gt;
 * &nbsp;&nbsp;&nbsp;&nbsp;&lt;text&gt;Don't push me 'cause I'm close 
 * to the edge.&lt;/text&gt;
 * &nbsp;&nbsp;&lt;/hopobject&gt;
 * &lt;/xmlroot&gt;&lt;</pre>
 * 
 * @param HopObject and file name as string
 */
Xml.prototype.write = function() {};


/**
 * Stores a HopObject as XML tree in a string.
 * <br /><br />
 * Syntax:
 * <pre>Xml.writeToString(HopObject)</pre>
 * <br /><br />
 * Example:
 * <pre>var str = Xml.writeToString(this.get(0));
 * res.write(str);
 * &nbsp;
 * &lt;?xml version="1.0"?&gt;
 * &lt;!-- printed by helma object publisher     --&gt;
 * &lt;!-- created Fri Jul 12 17:25:24 CEST 2002 --&gt;
 * &lt;xmlroot xmlns:hop="http://www.helma.org/docs/
 * guide/features/database"&gt;
 * &nbsp;&nbsp;&lt;hopobject id="8" name="message" prototype="message" 
 * created="1026487524440" lastModified="1026487524440"&gt;
 * &nbsp;&nbsp;&lt;hop:parent idref="0" prototyperef="root"/&gt;
 * &nbsp;&nbsp;&nbsp;&nbsp;&lt;title&gt;The Message&lt;/title&gt;
 * &nbsp;&nbsp;&nbsp;&nbsp;&lt;text&gt;Don't push me 'cause I'm close 
 * to the edge.&lt;/text&gt;
 * &nbsp;&nbsp;&lt;/hopobject&gt;
 * &lt;/xmlroot&gt;</pre>
 * 
 * @param HopObject
 * @return String in XML encoded
 * @type String
 */
Xml.prototype.writeToString = function() {};


