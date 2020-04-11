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
 * $RCSfile: DatabaseObject.js,v $
 * $Author: zumbrunn $
 * $Revision: 8695 $
 * $Date: 2007-12-10 13:04:11 +0100 (Mon, 10. Dez 2007) $
 */

/**
 * @fileoverview Default properties and methods of objects of the 
 * class Packages.helma.scripting.rhino.extensions.DatabaseObject.
 */


/**
 * A DatabaseObject represents a connection to a relational database 
 * and is created using the global getDBConnection function. There 
 * is no constructor to instantiate these objects. 
 * <br /><br />
 * The Direct DB interface allows the developer to directly access 
 * relational databases defined in the db.properties file without 
 * going through the Helma object model layer.
 * <br /><br />
 * The string passed to the getDBConnection function must match one 
 * of the data sources being defined in [AppDir]/db.properties, or an 
 * error will be thrown. 
 * <br /><br />
 * Example:
 * <pre>var dbConnection = getDBConnection("db_source_name");
 * &nbsp;
 * var dbRowset = dbConnection.executeRetrieval("select title from dummy");
 * while (dbRowset.next())
 * &nbsp;&nbsp;res.writeln(dbRowset.getColumnItem("title"));
 * <br /><br />
 * var deletedRows = dbConnection.executeCommand("delete from foobar");
 * if(deletedRows){
 * &nbsp;&nbsp;res.writeln(deletedRows + " rows successfully deleted");
 * }</pre>
 * <br /><br />
 * See also the documentation of the global getDBConnection() method.
 * 
 * @deprecated use helma.Database instead
 * @see Packages.helma.scripting.rhino.extensions.DatabaseObject
 * @see global.getDBConnection
 * @see DatabaseObject.RowSet
 * @see helma.Database
 */
DatabaseObject = undefined;


/**
 * Return the last error which occured when connecting or 
 * executing a statement, undefined if none.
 * 
 * @return undefined if none
 */
DatabaseObject.prototype.getLastError = function() {};


/**
 * Return the meta data attached to the connection.
 */	
DatabaseObject.prototype.getMetaData = function() {};


/**
 * Used to implement SELECT and other value returning statements. 
 * <br /><br />
 * Return a RowSet object implementing the RowsetAccess protocol 
 * if the request is successful. Returns false otherwise. Additional 
 * arguments are ignored, and data is always returned as a RowsetAccess. 
 * The cursor is positioned before the fist row. If false was returned, 
 * getLastError may be called on the database object to get the error 
 * information. Note that an error is generated (rather than returning a status) 
 * if the connection was not successful.
 * 
 * @param {String} sqlString as String
 * @return DatabaseObject.RowSet with the resulting rowset, or false in case of error
 * @type DatabaseObject.RowSet
 */	
DatabaseObject.prototype.executeRetrieval = function(sqlString) {};


/**
 * Used to implement INSERT, UPDATE, ddl statements and other 
 * non value returning statements. 
 * <br /><br />
 * Returns the number of rows impacted if the request is successful. 
 * Returns false otherwise. If false was returned, getLastError may be 
 * called on the database object to get the error information. Note 
 * that an error is generated (rather than returning a status) if the 
 * connection was not successful.
 * 
 * @param {String} sqlString as String
 * @return Number of rows impacted if successful or false if error.
 * @type Number
 */ 
DatabaseObject.prototype.executeCommand = function(sqlString) {};


/**
 * Rowset object representing the results of a value 
 * returning database query initiated using the 
 * dbConnection.executeRetrieval method. There 
 * is no constructor to instantiate these objects. 
 * <br /><br />
 * In addition, the RowSet will return any column name property 
 * as its value, any index as the value of the corresponding column, 
 * and enumerate column names. The enumeration will only contain 
 * the column names, not any property added to the prototype or 
 * the object itself. However the internal names (valueOf, 
 * toString, length), the properties of the prototype (including 
 * the routines of the RowSetAccessprotocol) takes precedence over 
 * the name of columns when accessed as a property. The usage of 
 * indexed access or of the routine getColumnItemis therefore prefered.
 * <br /><br />
 * The RowSet string representation includes the SQL statement 
 * and its logical position if at start or at end.
 * 
 * @see global.getDBConnection
 * @see dbConnection
 */
DatabaseObject.RowSet = new Object();


/**
 * Get the next row of results, return true if there is a next row, 
 * false otherwise. Note that next must be called before the first 
 * row can be accesses.
 * 
 * @return Boolean true if a next row exists else false
 * @type Boolean
 */
DatabaseObject.RowSet.prototype.next = function() {};


/**
 * Optimistic view of the possibility that more rows are present.
 * <br /><br />
 * Currently only returns false if next returned false. It is 
 * possible to call this routine at any time.
 * 
 * @return Boolean false if calling next() returned false
 * @type Boolean
 */
DatabaseObject.RowSet.prototype.hasMoreRows = function() {};


/**
 * Return the last error which occured when connecting or executing a 
 * statement, null (which test as false) if none.
 * 
 * @return last error or else null if none
 */
DatabaseObject.RowSet.prototype.getLastError = function() {};


/**
 * Return the meta data attached to the row set.
 */
DatabaseObject.RowSet.prototype.getMetaData = function() {};


/**
 * Get the number of columns of this result, identical to 
 * the length attribute. It is possible to call this 
 * routine before the first record is fetched.
 * 
 * @return Number of colums
 * @type Number
 */
DatabaseObject.RowSet.prototype.getColumnCount = function() {};


/**
 * Get the name of a column, in a way which is always working. 
 * The names can be accessed as properties, but they are shadowed 
 * by the functions and properties of the RowSetAccess prototype object. 
 * It is possible to call this routine before the first record is fetched.
 * 
 * @return String containing the name of a column
 * @type String
 */
DatabaseObject.RowSet.prototype.getColumnName = function() {};


/**
 * Get the number of the datatype associated with the column. 
 * See the jdbc documentation for details. It is possible to 
 * call this routine before the first record is fetched.
 * 
 * @return Number of data-types associated with the column
 * @type Number
 */
DatabaseObject.RowSet.prototype.getColumnDatatypeNumber = function() {};


/**
 * Get the  name of the datatype associated with the column. 
 * See the jdbc documentation for details. Some database do 
 * not return a valid name, in that case undefined is returned. 
 * It is possible to call this routine before the first record is fetched.
 * 
 * @return String containing the name of the data-type associated with the column
 * @type String
 */
DatabaseObject.RowSet.prototype.getColumnDatatypeName = function() {};


/**
 * Get the value of a column by its name (the value can be accessed by 
 * number simply indexing them - this is not faster than by name for FESI). 
 * The proper value is returned even if the name is used for a property of 
 * the RowSetAccess protocol, as next or length. A record must be available 
 * (that is next must have been called at least once).
 * 
 * @param {String} name as String, name of the column
 * @return value of a column
 */
DatabaseObject.RowSet.prototype.getColumnItem = function(name) {};

