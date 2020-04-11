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
 * $RCSfile: HopObject.js,v $
 * $Author: zumbrunn $
 * $Revision: 9715 $
 * $Date: 2009-05-06 21:54:38 +0200 (Mit, 06. Mai 2009) $
 */

/**
 * @fileoverview Default properties and methods of  
 * the HopObject prototype.
 */


/**
 * Constructor for HopObject objects, providing the 
 * building blocks of the Helma framework.
 * <br /><br />
 * Extends the standard JavaScript object with 
 * Helma-specific properties and functions. The HopObject is
 * the basic building block of a Helma application. The 
 * website root object as well as custom types defined by 
 * the application inherit from the HopObject prototype.
 * <br /><br />
 * HopObjects can be given special Helma specific 
 * properties, such as "collections" that can be configured 
 * to map to relational databases, and will make such data 
 * available when rendering "skins".
 * <br /><br />
 * HopObjects can be in transient state or are persistently 
 * mapped on a database. HopObjects that are directly or 
 * indirectly attached to the application's root object are 
 * automatically persisted using the built-in XML database, if 
 * they are not otherwise mapped to a relational database. 
 * see JavaDocs for helma.scripting.rhino.HopObject
 * 
 * @constructor
 * @see Packages.helma.scripting.rhino.HopObject
 */
function HopObject() {}


/**
 * The creation date of the HopObject.
 * @type Date
 */
HopObject.prototype.__created__ = new Date();


/**
 * The unique id of the HopObject.
 * This property is read-only
 * @readonly
 * @type Number
 */
HopObject.prototype._id = new Number();


/**
 * The date when the HopObject was last modified.
 * @type Date
 */
HopObject.prototype.__lastModified__ = new Date();


/**
 * The accessname of the HopObject.
 * @type String
 */
HopObject.prototype._name = new String();


/**
 * The parent collection containing this HopObject.
 * @type HopObject
 */
HopObject.prototype._parent = new HopObject();


/**
 * The name of the prototype from which this HopObject is inheriting.
 * @type String
 */
HopObject.prototype._prototype = new String();


/**
 * Cache object providing space for arbitrary run-time data.
 * <br /><br />
 * Each HopObject contains a cache object to store temporary 
 * properties (strings, numbers, objects etc.), a transient 
 * HopObject, which can be used for caching purposes.
 * <br /><br />
 * These properties can be accessed in any thread until the 
 * application is restarted or the HopObject is invalidated, 
 * either manually using the invalidate method, or when the 
 * clearCache method is being called, or whenever Helma is updating 
 * the HopObject's data from a remote database.
 * <br /><br />
 * There is no way to make the cache object persistent.
 * <br /><br />
 * Example:
 * <pre>var obj = root.get(0);
 * obj.cache.message = "This is a temporary message."</pre>
 * 
 * @type Object
 * @see HopObject.clearCache
 */
HopObject.prototype.cache = new Object();


/**
 * Attaches a HopObject as an additional subnode.
 * <br /><br />
 * Adds a HopObject as new subnode to another HopObject.
 * The new subnode is added after the last subnode already 
 * contained in the parent HopObject. 
 * <br /><br />
 * If the subnode is already attached, it is moved to the 
 * last index position.
 * 
 * @param {HopObject} subnode as HopObject to add to this node.
 * @return Boolean true if the addition or move was successful.
 * @type Boolean
 * @see HopObject.addAt
 */
HopObject.prototype.add = function(subnode) {};


/**
 * Attaches an additional subnode at the given index.
 * <br /><br />
 * Adds a HopObject to a collection at a certain position, 
 * and shifts the index of all succeeding objects in that 
 * collection by one. Index positions start with 0. Any 
 * out of range moves will move the subnode to the last
 * index position.
 * <br /><br />
 * Just makes sense for HopObjects, that are not mapped on 
 * a relational DB, since the sort order of the collection 
 * would otherwise be defined by type.properties, resp. by 
 * the database itself. Returns true in case of success.
 * 
 * If the subnode is already attached, it will be moved to
 * the specified index position.
 * 
 * @param {Number} position as Number, the index position where the subnode is to be inserted.
 * @param {HopObject} subnode as HopObject to add to this node.
 * @return Boolean true if the addition or move was successful.
 * @type Boolean
 * @see HopObject.add
 */
HopObject.prototype.addAt = function(position,subnode) {};


/**
 * Clears this HopObject's cache property.
 * <br /><br />
 * Removes all information stored in the cache object. 
 * Doing this by just calling 'obj.cache = null' is not 
 * possible, since the property itself can not be set.
 * 
 * @see HopObject.cache
 */
HopObject.prototype.clearCache = function() {};


/**
 * Determines if a HopObject contains a certain subnode.
 * 
 * @deprecated use indexOf instead
 * @param {HopObject} obj as HopObject, the node to look for
 * @return Number of the index position or -1
 * @type Number
 * @see HopObject.indexOf
 */
HopObject.prototype.contains = function(obj) {};


/**
 * Get the number of subnodes attached to this HopObject.
 * <br /><br />
 * Example:
 * <pre>res.write(root.count());
 * <i>5</i></pre>
 * 
 * @return Number of subnodes
 * @type Number
 * @see HopObject.size
 */
HopObject.prototype.count = function() {};


/**
 * Retrieves a persisted HopObject that is a subnode or 
 * a mapped property of this HopObject. 
 * <br /><br />
 * If the argument is a number, this method returns the subnode 
 * of this HopObject at the corresponding index position.
 * <br /><br />
 * If the argument is a string and matches a property name 
 * mapped in this prototype's type.properties file to a 
 * mountpoint, object, or collection, this function returns 
 * the corresponding HopObject.
 * <br /><br />
 * If the string argument produces no such match, the
 * behavior depends on whether this HopObject's _children have 
 * an accessname defined in the prototype's type.properties. 
 * <br /><br />
 * If an accessname is defined, this function first attempts 
 * to return the subnode with the corresponding name. 
 * Otherwise, or if that attempt fails, a string argument will 
 * result in a null return unless the string argument is 
 * numeric, in which case this function will return the child 
 * with an _id matching the numeric value of the argument. 
 * However, retrieving a HopObject based on its _id value is 
 * better achieved using the getById() HopObject method.
 * <br /><br />
 * Example:
 * <pre>root.get(0);
 * HopObject author
 * root.get(1);
 * <i>HopObject story</i>
 * &nbsp;
 * root.get("date");
 * Wed Oct 18 02:01:41 GMT+01:00 1971
 * root.get("title");
 * <i>The Nudist On The Late Shift</i>
 * 
 * @param {String} id as String or Number
 * @return HopObject the subnode with the specified id or name
 * @type HopObject
 * @see HopObject.getById
 */
HopObject.prototype.get = function(id) {};


/**
 * Retrieves the specified HopObject.
 * <br /><br />
 * If called on a HopObject instance, this getById() 
 * retrieves a child object by ID. If called on a HopObject 
 * constructor, it retrieves the persisted HopObject of 
 * that prototype and with the specified ID.
 * <br /><br />
 * Fetches a HopObject of a certain prototype through 
 * its ID and its prototype name. The prototype name can 
 * either be passed as a second argument, or alternatively 
 * the function can also be called on the prototype itself 
 * with a single argument (e.g. Story.getById(123)).
 * <br /><br />
 * In case of multiple prototypes being mapped on the 
 * same table (which is for instance the case with 
 * inherited prototypes) Helma will not check whether 
 * the prototype of the fetched object actually matches 
 * the specified prototype.
 * <br /><br />
 * Note, that this refers to the static method 'getById', 
 * not to be mixed up with the method getById called on a 
 * specific HopObject.
 * <br /><br />
 * Example:
 * <pre>//get the child with id 17
 * var child = root.getById(17);
 * writeln(child._id);
 * <i>17</i>
 * &nbsp;
 * //get the child at index 17 of the HopObject's children-collection
 * child = root.get(17);
 * writeln(child._id);
 * <i>42</i>
 * &nbsp;
 * //get the child with the name "17" (_children.accessname = name) of the HopObject
 * child = root.get("17");
 * writeln(child._id);
 * <i>69</i>
 * writeln(child.name);
 * <i>17</i>
 * &nbsp;
 * //get the persistent HopObject with prototype "Page" and id 127
 * var page = HopObject.getById(127, "Page");
 * writeln(page._id);
 * <i>127</i>
 * &nbsp;
 * //get the persistent HopObject with prototype "Page" and id 127
 * var page = Page.getById(127);
 * writeln(page._id);
 * <i>127</i></pre>
 * 
 * @param {Number} id as Number
 * @param {String} proto as String, the name of the prototype
 * @return HopObject that was retrieved
 * @type HopObject
 * @see HopObject.get
 */
HopObject.prototype.getById = function(id,proto){};  


/**
 * Returns a collection of HopObjects as defined by
 * a provided properties object.
 * <br /><br />
 * The getCollection function is a static method of HopObject 
 * constructors, taking a single JS object argument, which  
 * provides the collection properties as you would otherwise  
 * define them in the _children section of a type.properties file.
 * <br /><br />
 * Examples:
 * <pre>var c = Page.getCollection({
 * &nbsp;&nbsp;order: "name",
 * &nbsp;&nbsp;filter: "id > 10",
 * };</pre>
 * You can also specify the type of contained objects using  
 * the "collection" property, so the following collection is 
 * equivalent to the one defined above:
 * <pre>var c = HopObject.getCollection({
 * &nbsp;&nbsp;collection: "Page",
 * &nbsp;&nbsp;order: "name",
 * &nbsp;&nbsp;filter: "id > 10",
 * };</pre>
 * Note that for "nested" properties such as group.order or 
 * local.1 you have to use quoted "flat" properties, not 
 * nested objects:
 * <pre>var c = Page.getCollection({
 * &nbsp;&nbsp;group: "author",
 * &nbsp;&nbsp;"group.order": "author",
 * &nbsp;&nbsp;"group.prototype": "AuthorGroup"
 * });</pre>
 * Additionally, the collection properties "limit" and 
 * "offset" provide support for easy pagination. In order 
 * to fetch pages 11-20, you would do something like this:
 * <pre>var q = Page.getCollection({
 * &nbsp;&nbsp;limit: 10,
 * &nbsp;&nbsp;offset: 10
 * });</pre>
 * Note that "limit" is just an alias for "maxSize" introduced 
 * to be more consistent with the underlying SQL syntax.
 * <br /><br />
 * This feature is currently implemented and known to work 
 * on MySQL, Postgresql, and Oracle.
 * 
 * @param {Object} props as Object, properties defining the desired collection
 * @return Array the collection of HopObjects as defined by the provided properties
 * @type Array
 */
HopObject.getCollection = function(props) {};


/**
 * Optional handler to override the default URL path resolution.
 * <br /><br />
 * If defined, the getChildElement() method is called on an 
 * object contained in the request path in order to determine 
 * the URL path resolution from that object to its (potential) 
 * child objects.
 * <br /><br />
 * The string parameter passed to this method is the name of 
 * the next element in the URL path being resolved. The object 
 * being returned by this method is then used to continue 
 * resolving the URL path.
 * <br /><br />
 * Example:
 * <pre>function getChildElement(name) {
 * &nbsp;&nbsp;if (name == 'transient') {
 * &nbsp;&nbsp;&nbsp;&nbsp;return new HopObject();
 * &nbsp;&nbsp;}
 * &nbsp;&nbsp;else {
 * &nbsp;&nbsp;&nbsp;&nbsp;return this.get(name);
 * &nbsp;&nbsp;}
 * }</pre>
 * 
 * @param {String} name as String, the name of the child element in the requested URL path
 * @return HopObject the child object to be used for further path resolution
 * @type HopObject
 */
HopObject.prototype.getChildElement = function(name) {};


/**
 * Returns a collection including all subnodes of a 
 * HopObject ordered according to the properties 
 * listed in the string passed as argument.
 * <br /><br />
 * While the sort pattern, passed as string argument, uses 
 * an SQL-like notation for the sorting, it contains 
 * property names, not database column names.
 * <br /><br />
 * The method returns a collection object that is ordered 
 * according to the specified sort order. The returned 
 * collections are cached and bound to the original 
 * collections, i.e. they reflect changes to the original 
 * collection. Both initial ordering and re-ordering are 
 * done on the Helma side, meaning that no additional db 
 * traffic is generated by multiple ordered views.
 * <br /><br />
 * Example:
 * <pre>var orderedByDate = hobj.getOrderedView("createtime desc, name");
 * var collection = orderedByDate.list();
 * for (var i in collection) {
 * &nbsp;&nbsp;doSomething(collection[i]);
 * }
 * // other syntax examples:
 * var orderedByName = hobj.getOrderedView("name");
 * var collectionByName = hobj.collection.getOrderedView("name");</pre>
 * 
 * @param {String} name as String, the property names to sort by
 * @return HopObject HopObject containing the sorted subnodes
 * @type HopObject
 */
HopObject.prototype.getOrderedView = function(name) {};


/**
 * Returns a helma.framework.repository.Resource object 
 * defined for the prototype.
 * <br /><br />
 * Returns a resource referenced by its name for the 
 * current HopObject's prototype - getResource() walks 
 * up the inheritance chain and through all defined 
 * repositories to find the resource and returns null 
 * if unsucessful.
 * <br /><br />
 * Example:
 * <pre>root.getResource("main.skin");
 * /usr/local/helma-apps/myApp/repository1/HopObject/main.skin
 * root.getResource("type.properties");
 * /usr/local/helma-apps/myApp/repository3/Root/type.properties
 * root.getResource("Functions.js");
 * /usr/local/helma-apps/myApp/repository17/Root/Functions.js
 * 
 * @param {String} resourceName as String, the name of the requested resource
 * @return helma.framework.repository.Resource
 * @type helma.framework.repository.Resource
 * @see HopObject.getResources
 */
HopObject.prototype.getResource = function(resourceName) {};


/**
 * Returns an Array of helma.framework.repository.Resource 
 * objects defined for the prototype.
 * <br /><br />
 * Returns an array of resources by the specified name for 
 * the current HopObject's prototype - getResources() walks 
 * up the inheritance chain and through all defined 
 * repositories to collect all the resources by that name 
 * and returns null if unsucessful.
 * 
 * @param {String} resourceName as String, the name of the requested resource
 * @return Array of helma.framework.repository.Resource objects
 * @type Array
 * @see HopObject.getResource
 */
HopObject.prototype.getResources = function(resourceName) {};


/** 
 * Returns the absoulte URL path of a HopObject relative 
 * to the application's root.
 * <br /><br />
 * This function is useful when referring to a HopObject 
 * in a markup tag (e.g. with a href attribute in an HTML 
 * &lt;a&gt;-tag). An optional string argument is appended 
 * to the return value.
 * <br /><br />
 * Example:
 * <pre>var obj = root.get(0);
 * &nbsp;
 * res.write('&lt;a href="' + obj.href() + '"&gt;');
 * <i>&lt;a href="/main/"&gt;</i>
 * &nbsp;
 * res.write('&lt;a href="' + obj.href('edit') + '"&gt;');
 * <i>&lt;a href="/main/edit"&gt;</i></pre>
 * 
 * @param {String} action as String, optional part to be attached to the URL of this HopObject
 * @return String of the URL path for this HopObject
 * @type String
 */
HopObject.prototype.href = function(action) {};


/**  
 * Marks a HopObject as invalid so that it is fetched 
 * again from the database.
 * <br /><br />
 * Helma will overwrite the HopObject's node cache 
 * with the database contents the next time the 
 * HopObject is accessed.
 * <br /><br />
 * In other words, use this function to kick out 
 * an HopObject of Helma's node cache and force a 
 * database retrieval of the HopObject data.
 * <br /><br />
 * Example:
 * <pre>var obj = this.get(0);
 * obj.invalidate();</pre>
 * 
 * @parm {String} childId as String, optional id of a subnode to invalidate 
 */
HopObject.prototype.invalidate = function(childId) {};


/**
 * Determines if a HopObject contains a certain subnode.
 * <br /><br />
 * Returns the index position of a Subnode contained by a HopObject 
 * (as usual for JavaScript, 0 refers to the first position).
 * <br /><br />
 * The index position is a relative value inside a HopObject 
 * (not to be confused with a Hop ID which is unique for each HopObject).
 * <br /><br/>
 * If there is no appropriate subnode inside the HopObject the returned value equals -1.
 * <br /><br />
 * Example:
 * <pre>var obj = root.get("myObject");
 * res.write(root.indexOf(obj));
 * <i>23</i>
 * &nbsp;
 * obj = root.get("blobject");
 * res.write(root.indexOf(obj));
 * <i>-1</i></pre>
 * 
 * @param {HopObject} obj as HopObject, the node to look for
 * @return Number of the index position or -1
 * @type Number
 */
HopObject.prototype.indexOf = function(obj) {};

/**
 * Returns true if the HopObject is in persistent state, meaning
 * that it is stored in the database, and false if it is transient.
 * Persistent state is also assumed if the object is currently in 
 * the process of being inserted into or deleted from the database. 
 *
 * @return true if the HopObject is in persistent state
 * @see HopObject.isTransient
 */
HopObject.prototype.isPersistent = function() {};

/**
 * Returns true if the HopObject is in transient state, meaning that
 * it is not stored in the database. This method returns false if the   
 * object is stored in the database, or is in the process of being 
 * inserted into or deleted from the database.
 *
 * @return true if the the HopObject is in transient state
 * @see HopObject.isPersistent
 */
HopObject.prototype.isTransient = function() {};

/**   
 * Returns an array including all subnodes of a HopObject.
 * <br /><br />
 * The startIndex and length parameters are optional, if 
 * omitted, an array of the entire collection of subnodes 
 * is returned, otherwise only the specified range.
 * <br /><br />
 * Example:
 * <pre>var objectList = root.list();
 * for (var i=0; i &lt; objectList.length; i++){
 * &nbsp;&nbsp;var myObject = objectList[i];
 * &nbsp;&nbsp;res.writeln(myObject.created);
 * }
 * <i>Wed Oct 18 02:01:41 GMT+01:00 1971
 * Fri Nov 03 13:25:15 GMT+01:00 2000
 * Mon May 29 07:43:09 GMT+01:00 1999</i></pre>
 * 
 * @param {Number} startIndex as Number
 * @param {Number} length as Number
 * @return Array of HopObjects
 * @type Array
 */
HopObject.prototype.list = function(startIndex, length) {};
  

/**
 * Stores a transient HopObject and all HopObjects 
 * reachable from it to database.
 * <br /><br />
 * The function returns the id (primary key) of the 
 * newly stored HopObject as string, or null if the 
 * HopObject couldn't be stored for some reason.
 * <br /><br />
 * Example:
 * <pre>var hobj = new HopObject();
 * hobj.foo = new HopObject();
 * res.debug(hobj.persist());
 * 2
 * res.debug(hobj.foo._id)
 * 3</pre>
 */
HopObject.prototype.persist = function() {};



/**  
 * Manually retrieving a particular set of subnodes.
 * <br /><br />
 * This function provides some control of how many 
 * subnodes Helma should retrieve from the database and 
 * hold prepared in the node cache for further processing.
 * <br /><br />
 * This means that for large collections Helma does 
 * not need to retrieve neither the subset of subnodes 
 * via one SQL statement for each subnode nor the whole 
 * collection at once via one statement.
 * <br /><br />
 * Moreover, only subnodes are retrieved that are not 
 * in the node cache already which leads to a maximum of 
 * caching efficiency and loading performance.
 * <br /><br />
 * Example:
 * <pre>res.writeln(root.length);
 * 53874
 * root.prefetchChildren(0, 3);
 * for (var i=0; i&lt;3; i++)
 * &nbsp;&nbsp;res.writeln(i + ": " + root.get(i));
 * <i>HopObject 1
 * HopObject 5
 * HopObject 4</i></pre>
 * 
 * @param {Number} startIndex as Number
 * @param {Number} length as Number
 */
HopObject.prototype.prefetchChildren = function(startIndex, length) {};


/**
 * Deletes a HopObject from the database.
 * <br /><br />
 * The remove() function deletes a persistent HopObject 
 * from the database.
 * <br /><br />
 * Note that additionally you may want to call the 
 * removeChild() function on any object holding the 
 * deleted object in its child collection in order to 
 * notify it that the child object has been removed.
 * <br /><br />
 * Example:
 * <pre>res.write(parent.size());
 * 24
 * var child = parent.get(5);
 * child.remove();
 * parent.removeChild(child);
 * res.write(parent.size());
 * 23</pre>
 * 
 * @see HopObject.removeChild
 */
HopObject.prototype.remove = function() {};


/**
 * Notifies a parent object that a child object has been removed.
 * <br /><br />
 * The removeChild() function lets a parent object 
 * know that a child object has been removed. 
 * Note that calling removeChild() will not actually 
 * delete the child object. Directly call remove() 
 * on the child object in order to delete it from 
 * the database.
 * <br /><br />
 * Example:
 * <pre>res.write(parent.size());
 * <i>24</i>
 * var child = parent.get(5);
 * child.remove();
 * parent.removeChild(child);
 * res.write(parent.size());
 * <i>23</i></pre>
 * 
 * @param {HopObject} child as HopObject
 * @see HopObject.remove
 */
HopObject.prototype.removeChild = function(child) {}; 


/**
 * Renders a skin of a HopObject and writes the result 
 * to the output buffer.
 * <br /><br />
 * Either renders the provided SkinObject or a skin in the 
 * HopObject's prototype chain by the specified name. 
 * <br /><br />
 * A skin can contain markup (e.g. HTML or XML) and macros. 
 * Macros are references to Helma functions wrapped in special 
 * tags (<% and %>). For more information about skin and macro 
 * techniques please refer to the section about About Skins.
 * <br /><br />
 * Optionally, a JavaScript object can be assigned to the 
 * function call as second argument. This object's properties 
 * later can be accessed from the skin via macro calls of 
 * the kind <% param.propertyName %>.
 * <br /><br />
 * If a param property is not set but referred to 
 * in the skin file, it will be replaced with an empty string. 
 * Please note that this behaviour is different from generic 
 * macro calls.
 * <br /><br />
 * Example:
 * <pre>Contents of the file root/example.skin:
 * &lt;html&gt;
 * &lthead&gt;
 * &nbsp;&nbsp;&lt;title&gt;Hello, &lt;% param.title %&gt;!&lt;/title&gt;
 * &lt;/head&gt;
 * &lt;body bgcolor="&lt;% param.bgcolor %&gt;">
 * I greet you &lt;% param.amount %&gt; times.
 * &lt;/body&gt;
 * &lt;/html&gt;</pre>
 * <b>Rendering the skin:</b>
 * <pre>var param = new Object();
 * param.bgcolor = "#ffcc00";
 * param.title = "World";
 * param.amount = "12345";
 * root.renderSkin("example", param);
 * &nbsp;
 * <i>&lt;html&gt;
 * &lt;head&gt;
 * &lt;title&gt;Hello, World!&lt;/title&gt;
 * &lt;/head&gt;
 * &lt;body bgcolor="#ffcc00"&gt;
 * I greet you 12345 times.
 * &lt;/body&gt;</i></pre>
 * 
 * @param {String} skin as SkinObject or the name of the skin as String
 * @param {Object} params optional, properties to be passed to the skin
 * @see HopObject.renderSkinAsString
 * @see global.renderSkin
 * @see global.renderSkinAsString
 */ 
HopObject.prototype.renderSkin = function(skin, params) {};


/**
 * Renders a skin of a HopObject and returns the result as string.
 * <br /><br />
 * Either renders the provided SkinObject or a skin in the 
 * HopObject's prototype chain by the specified name. 
 * <br /><br />
 * Similar to renderSkin(), this function returns the 
 * result of the rendered skin as string instead of 
 * immediately writing it to the response object.
 * <br /><br />
 * Example:
 * <pre>var str = root.renderSkinAsString("example", param);
 * res.write(str);
 * &nbsp;
 * which is equivalent to
 * &nbsp;
 * root.renderSkin("example", param);</pre>
 * 
 * @param {String} skin as SkinObject or the name of the skin as String
 * @param {Object} params as Object, optional properties to be passed to the skin
 * @return String containing the result of the rendered skin
 * @type String
 * @see HopObject.renderSkin
 * @see global.renderSkin
 * @see global.renderSkinAsString
 */
HopObject.prototype.renderSkinAsString = function(skin, params) {};


/**
 * Get the number of subnodes attached to this HopObject.
 * <br /><br />
 * Example:
 * <pre>res.write(root.size());
 * <i>5</i></pre>
 * 
 * @return Number of subnodes
 * @type Number
 * @see HopObject.count
 */
HopObject.prototype.size = function() {};


/**
 * Refetches updateable Subnode-Collections from the database. 
 * <br /><br />
 * The following conditions must be met to make a 
 * subnodecollection updateable:<br />
 * 1) the collection must be specified with 
 * collection.updateable=true<br />
 * 2) the id's of this collection must be in ascending order, 
 * meaning, that new records do have a higher id than the 
 * last record loaded by this collection.
 * 
 * @return Number of updated nodes
 * @type Number
 */
HopObject.prototype.update = function() {};


/**
 * If defined, the onCodeUpdate() function is called whenever 
 * the code is recompiled.
 * &nbsp;
 * This is most useful when code from repositories is 
 * dynamically modified by the application during runtime.
 */
HopObject.prototype.onCodeUpdate = function() {};


/**
 * If a HopObject prototype defines a function named onInit(), 
 * it will be called on each HopObject that is fetched from 
 * embedded or relational database.
 * <br /><br />
 * Note that it is not called for HopObjects created using 
 * a constructor.
 */
HopObject.prototype.onInit = function() {};


/**
 * If a HopObject prototype defines a function named onPersist(), 
 * it will be called on each HopObject immediately before it is 
 * stored to embedded or relational database.
 */
HopObject.prototype.onPersist = function() {};


/**
 * If a HopObject prototype defines a function named 
 * onUnhandledMacro(), it will be called when a macro is rendered 
 * on the HopObject that can't be mapped to a macro function or 
 * property of the HopObject. 
 * 
 * @param {String} name as String, the name of the unhandled macro
 */
HopObject.prototype.onUnhandledMacro = function(name) {};


/**
 * If defined, the onRequest() function is called on the 
 * object specified by the request path just before the 
 * action is invoked.
 * <br /><br />
 * This is useful for performing some code before each and 
 * every action in every prototype (when defining the 
 * function for the hopobject prototype) or for each action 
 * in one prototype. Often, it is used for checking access 
 * permissions.
 */
HopObject.prototype.onRequest = function() {};


/**
 * If defined, the onResponse() method is called after 
 * each page request is handled but before the response is
 * returned. 
 * <br /><br />
 * This method is NOT called when a response is forwarded 
 * or redirected using res.forward or res.redirect respectively.
 * <br /><br />
 * You can access the current repsonse buffer using the 
 * method res.getBuffer.
 * 
 * @see res.getBuffer
 */
HopObject.prototype.onResponse = function() {};


