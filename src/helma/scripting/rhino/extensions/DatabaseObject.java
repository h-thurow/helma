// Database.java
// FESI Copyright (c) Jean-Marc Lugrin, 1999
//
// This program is free software; you can redistribute it and/or
// modify it under the terms of the GNU Lesser General Public
// License as published by the Free Software Foundation; either
// version 2 of the License, or (at your option) any later version.

// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
// Lesser General Public License for more details.

// You should have received a copy of the GNU Lesser General Public
// License along with this library; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA


// Modified to use Helma database connections, Hannes Wallnoefer 2000-2003

package helma.scripting.rhino.extensions;

import helma.objectmodel.db.DbSource;
import java.util.Enumeration;
import java.util.Vector;
import java.io.IOException;
import java.io.Reader;
import java.math.BigDecimal;
import java.sql.*;


/**
  * A Database object, representing a connection to a JDBC Driver
  */
public class DatabaseObject {

    private transient Connection connection = null; // Null if not connected
    private transient DatabaseMetaData databaseMetaData = null;
    private transient String driverName = null;
    private transient Exception lastError = null;
    private transient boolean driverOK = false;

    /**
     * Create a new database object based on a hop data source.
     *
     * @param dbsource The name of the DB source
     */

    public DatabaseObject(DbSource dbsource) {
        try {
            this.connection = dbsource.getConnection ();
            this.driverName = dbsource.getDriverName ();
        } catch (Exception e) {
            // System.err.println("##Cannot find driver class: " + e);
            // e.printStackTrace();
            this.lastError = e;
        }
        this.driverOK = true;
    }

    /**
     * Create a new database object based on a driver name, with driver on the classpath
     *
     * @param driverName The class name of the JDBC driver
     */

    DatabaseObject(String driverName) {
        this.driverName = driverName;
        try {
            Class driverClass = Class.forName(driverName);
            if (!Driver.class.isAssignableFrom(driverClass)) {

                // System.err.println("##Bad class " + driverClass);
                this.lastError = new RuntimeException(Messages.getString("DatabaseObject.0") + driverClass + Messages.getString("DatabaseObject.1")); //$NON-NLS-1$ //$NON-NLS-2$
            }
            driverClass.newInstance(); // may be needed by some drivers, harmless for others
        } catch (ClassNotFoundException e) {
            // System.err.println("##Cannot find driver class: " + e);
            // e.printStackTrace();
            this.lastError = e;
        } catch (InstantiationException e) {

            // System.err.println("##Cannot instantiate driver class: " + e);
            // e.printStackTrace();
            this.lastError = e;
        } catch (IllegalAccessException e) {
            // ignore as this may happen depending on driver, it may not be severe
            // for example because the instance was created at class initialization time
        }
        this.driverOK = true;
    }


    /**
     * Create the database prototype object which cannot be connected
     *
     */

    DatabaseObject() {
        this.driverName = null;
        this.driverOK = false; // Avoid usage of this object
    }

    public String getClassName() {
        return "DatabaseObject"; //$NON-NLS-1$
    }

    @Override
    public String toString() {
         if (this.driverName==null) return "[database protoype]"; //$NON-NLS-1$
         return "[Database: '" + this.driverName + //$NON-NLS-1$
                 (this.driverOK ?
                     (this.connection==null ? "' - disconnected] " : " - connected]") //$NON-NLS-1$ //$NON-NLS-2$
                 : " - in error]"); //$NON-NLS-1$
    }

    public String toDetailString() {
        return "ES:[Object: builtin " + this.getClass().getName() + ":" +  //$NON-NLS-1$//$NON-NLS-2$
            this.toString() + "]"; //$NON-NLS-1$
    }

    public Object getLastError() {
        if (this.lastError == null) {
            return null;
        }
        return this.lastError;
    }


    /**
     * Connect to the database, using the specific url, optional user name and password
     *
     * @param  url the database URL
     * @param  userName the database user name
     * @param  password the database password
     * @return  true if successful, false otherwise
     */
    public boolean connect(String url, String userName, String password) {
        if (!this.driverOK) {
            this.lastError = new SQLException(Messages.getString("DatabaseObject.2")); //$NON-NLS-1$
            return false;
        }
        this.lastError = null;
        try {
            if (userName == null) {
                this.connection = DriverManager.getConnection(url);
            } else {
                this.connection = DriverManager.getConnection(url,userName,password);
            }
        } catch(Exception e) {
            // System.err.println("##Cannot connect: " + e);
            // e.printStackTrace();
            this.lastError = e;
            return false;
        }
        return true;
    }


    /**
     * Disconnect from the database, nop if not conected
     *
     * @return  true if successful, false if error during idsconnect
     */
    public boolean disconnect() {
        if (!this.driverOK) {
            this.lastError = new SQLException(Messages.getString("DatabaseObject.3")); //$NON-NLS-1$
            return false;
        }
        this.lastError = null;
        if (this.connection != null) {
             try {
                this.connection.close();
                this.connection = null;
                this.lastError = null;
           } catch (SQLException e) {
                // System.err.println("##Cannot disonnect: " + e);
                // e.printStackTrace();
                this.lastError = e;
                return false;
            }
        }
        return true;
    }

    public void release()  {
        if (this.driverOK) {
            disconnect();
        }
    }

    public RowSet executeRetrieval(String sql) {
        if (this.connection==null) {
            this.lastError = new SQLException(Messages.getString("DatabaseObject.4")); //$NON-NLS-1$
            return null;
        }
        Statement statement = null;
        ResultSet resultSet = null;

        try {
            this.connection.setReadOnly(true);
            statement = this.connection.createStatement();
            resultSet = statement.executeQuery(sql);     // will return true if first result is a result set

            return new RowSet(sql, this, statement, resultSet);
        } catch (SQLException e) {
            // System.err.println("##Cannot retrieve: " + e);
            // e.printStackTrace();
            this.lastError = e;
            try {
                if (statement != null) statement.close();
            } catch (Exception ignored) {
            }
            statement = null;
            return null;
        }
    }

    public int executeCommand(String sql) {
        int count = 0;

        if (this.connection==null) {
            this.lastError = new SQLException(Messages.getString("DatabaseObject.5")); //$NON-NLS-1$
            return -1;
        }

        Statement statement = null;
        try {

            this.connection.setReadOnly(false);
            statement = this.connection.createStatement();
            count = statement.executeUpdate(sql);     // will return true if first result is a result set
        } catch (SQLException e) {
            // System.err.println("##Cannot retrieve: " + e);
            // e.printStackTrace();
            this.lastError = e;
            try {
                if (statement != null) statement.close();
            } catch (Exception ignored) {
            }
            statement = null;
            return -1;
        }
        try {
            statement.close();
        } catch (SQLException e) {
            // ignored
        }
        return count;
    }

    public Object getMetaData()
    {
      if (this.databaseMetaData == null)
         try {
            this.databaseMetaData = this.connection.getMetaData();
         } catch (SQLException e) {
            // ignored
         }
      return this.databaseMetaData;
    }

    /**
      * A RowSet object
      */
    public static class RowSet {

        private transient String sql = null;
        private transient Statement statement = null;
        private transient ResultSet resultSet = null;
        private transient ResultSetMetaData resultSetMetaData = null;
        private transient Vector colNames = null;
        private transient boolean lastRowSeen = false;
        private transient boolean firstRowSeen = false;
        private transient Exception lastError = null;

        RowSet(String sql,
                    DatabaseObject database,
                    Statement statement,
                    ResultSet resultSet) throws SQLException {
            this.sql = sql;
            this.statement = statement;
            this.resultSet = resultSet;

            if (sql==null) throw new NullPointerException("sql"); //$NON-NLS-1$
            if (resultSet==null) throw new NullPointerException("resultSet"); //$NON-NLS-1$
            if (statement==null) throw new NullPointerException("statement"); //$NON-NLS-1$
            if (database==null) throw new NullPointerException("database"); //$NON-NLS-1$

            try {

                this.resultSetMetaData = resultSet.getMetaData();
                int numcols = this.resultSetMetaData.getColumnCount();
                //IServer.getLogger().log("$$NEXT : " + numcols);
                this.colNames = new Vector(numcols);
                for (int i=0; i<numcols; i++) {
                   String colName = this.resultSetMetaData.getColumnLabel(i+1);
                   //IServer.getLogger().log("$$COL : " + colName);
                   this.colNames.addElement(colName);
                }
            } catch(SQLException e) {
                this.colNames = new Vector(); // An empty one
                throw new SQLException(Messages.getString("DatabaseObject.6")+e); //$NON-NLS-1$

                // System.err.println("##Cannot get column names: " + e);
                // e.printStackTrace();
            }
        }


        public String getClassName() {
            return "RowSet"; //$NON-NLS-1$
        }

        public String toDetailString() {
            return "ES:[Object: builtin " + this.getClass().getName() + ":" + //$NON-NLS-1$ //$NON-NLS-2$
                this.toString() + "]"; //$NON-NLS-1$
        }

        public int getColumnCount() {
            return this.colNames.size();
        }

        public Object getMetaData()
        {
          return this.resultSetMetaData;
        }

        public Object getLastError() {
            if (this.lastError == null) {
                return null;
            }
            return this.lastError;
        }


        public void release() {
            try {
                if (this.statement!= null) this.statement.close();
                if (this.resultSet != null) this.resultSet.close();
            } catch (SQLException e) {
                // ignored
            }
            this.statement = null;
            this.resultSet = null;
            this.resultSetMetaData = null;
        }

        public boolean hasMoreRows() {
            return !this.lastRowSeen;   // Simplistic implementation
        }

        public String getColumnName(int idx) {
           if (this.resultSet == null) {
                this.lastError = new SQLException(Messages.getString("DatabaseObject.7")); //$NON-NLS-1$
                return null;
           }
            if (idx>0 && idx <=this.colNames.size()) {
                return (String) this.colNames.elementAt(idx-1); // to base 0
            }
            this.lastError = new SQLException(Messages.getString("DatabaseObject.8") + idx + //$NON-NLS-1$
                                        Messages.getString("DatabaseObject.9") +this.colNames.size()); //$NON-NLS-1$
            return null;
        }


        public int getColumnDatatypeNumber(int idx) {
           if (this.resultSet == null) {
                this.lastError = new SQLException(Messages.getString("DatabaseObject.10")); //$NON-NLS-1$
                return -1;
           }
            if (idx>0 && idx <=this.colNames.size()) {
                try {
                    return this.resultSetMetaData.getColumnType(idx);
                } catch (SQLException e) {
                    this.lastError = e;
                    return -1;
                }
            }
            this.lastError = new SQLException(Messages.getString("DatabaseObject.11") + idx + //$NON-NLS-1$
                                        Messages.getString("DatabaseObject.12") +this.colNames.size()); //$NON-NLS-1$
            return -1;
        }


        public String getColumnDatatypeName(int idx) {
           if (this.resultSet == null) {
                this.lastError = new SQLException(Messages.getString("DatabaseObject.13")); //$NON-NLS-1$
                return null;
           }
            if (idx>0 && idx <=this.colNames.size()) {
                try {
                    return this.resultSetMetaData.getColumnTypeName(idx);
                } catch (SQLException e) {
                    this.lastError = e;
                    return null;
                }
            }
            this.lastError = new SQLException(Messages.getString("DatabaseObject.14") + idx + //$NON-NLS-1$
                                        Messages.getString("DatabaseObject.15") +this.colNames.size()); //$NON-NLS-1$
            return null;
        }


        public Object getColumnItem(String propertyName) {
           if (this.resultSet == null) {
                this.lastError = new SQLException(Messages.getString("DatabaseObject.16")); //$NON-NLS-1$
                return null;
           }
           if (!this.firstRowSeen) {
                this.lastError = new SQLException(Messages.getString("DatabaseObject.17")); //$NON-NLS-1$
                return null;
           }
           try {
                try {
                    int index = Integer.parseInt(propertyName);
                    return getProperty(index);
                } catch (NumberFormatException e) {
                    int index = this.resultSet.findColumn(propertyName);
                    return getProperty(index);
                }
           } catch (SQLException e) {
              //System.err.println("##Cannot get property '" + propertyName + "' " + e);
              //e.printStackTrace();
              this.lastError = e;
           }
           return null;
        }

        /* FIXME: dunno if this method is still used somewhere
        public Object getProperty(String propertyName, int hash) {
            //System.err.println(" &&& Getting property '" + propertyName + "'");

            // Length property is firsy checked

            // First return system or or prototype properties
            if (propertyName.equals("length")) {
                 return new Integer(colNames.size());
            } else {
               if (resultSet == null) {
                    lastError = new SQLException("Attempt to access a released result set");
                    return null;
               }
                if (!firstRowSeen) {
                    lastError = new SQLException("Attempt to access data before the first row is read");
                    return null;
                }
               try {
                    int index = -1; // indicates not a valid index value
                    try {
                        char c = propertyName.charAt(0);
                        if ('0' <= c && c <= '9') {
                           index = Integer.parseInt(propertyName);
                        }
                    } catch (NumberFormatException e) {
                    } catch (StringIndexOutOfBoundsException e) { // for charAt
                    }
                    if (index>=0) {
                        return getProperty(index);
                    }
                   Object value = resultSet.getObject(propertyName);
                   // IServer.getLogger().log("&& @VALUE : " + value);
                   lastError = null;
                   return value;
               } catch (SQLException e) {
                  // System.err.println("##Cannot get property '" + propertyName + "' " + e);
                  // e.printStackTrace();
                  lastError = e;
               }
            }
            return null;
        }
        */

        public Object getProperty(int index) {
            if (!this.firstRowSeen) {
                this.lastError = new SQLException(Messages.getString("DatabaseObject.18")); //$NON-NLS-1$
                return null;
            }
            if (this.resultSet == null) {
                this.lastError = new SQLException(Messages.getString("DatabaseObject.19")); //$NON-NLS-1$
                return null;
            }

            this.lastError = null;
            try {
                int type = this.resultSetMetaData.getColumnType(index);
                switch (type) {
                    case Types.BIT:
                    case Types.BOOLEAN:
                        return this.resultSet.getBoolean(index) ? Boolean.TRUE : Boolean.FALSE;

                    case Types.TINYINT:
                    case Types.BIGINT:
                    case Types.SMALLINT:
                    case Types.INTEGER:
                        return new Long(this.resultSet.getLong(index));

                    case Types.REAL:
                    case Types.FLOAT:
                    case Types.DOUBLE:
                        return new Double(this.resultSet.getDouble(index));

                    case Types.DECIMAL:
                    case Types.NUMERIC:
                        BigDecimal num = this.resultSet.getBigDecimal(index);
                        if (num == null) {
                            break;
                        }

                        if (num.scale() > 0) {
                            return new Double(num.doubleValue());
                        }
                        return new Long(num.longValue());

                    case Types.VARBINARY:
                    case Types.BINARY:
                        return this.resultSet.getString(index);

                    case Types.LONGVARBINARY:
                    case Types.LONGVARCHAR:
                        try {
                            return this.resultSet.getString(index);
                        } catch (SQLException x) {
                            Reader in = this.resultSet.getCharacterStream(index);
                            char[] buffer = new char[2048];
                            int read = 0;
                            int r = 0;

                            while ((r = in.read(buffer, read, buffer.length - read)) > -1) {
                                read += r;

                                if (read == buffer.length) {
                                    // grow input buffer
                                    char[] newBuffer = new char[buffer.length * 2];

                                    System.arraycopy(buffer, 0, newBuffer, 0,
                                            buffer.length);
                                    buffer = newBuffer;
                                }
                            }
                            return new String(buffer, 0, read);
                        }

                    case Types.DATE:
                    case Types.TIME:
                    case Types.TIMESTAMP:
                        return this.resultSet.getTimestamp(index);

                    case Types.NULL:
                        return null;

                    case Types.CLOB:
                        Clob cl = this.resultSet.getClob(index);
                        if (cl == null) {
                            return null;
                        }
                        char[] c = new char[(int) cl.length()];
                        Reader isr = cl.getCharacterStream();
                        isr.read(c);
                        return String.copyValueOf(c);

                    default:
                        return this.resultSet.getString(index);
                }
            } catch (SQLException e) {
                // System.err.println("##Cannot get property: " + e);
                // e.printStackTrace();
                this.lastError = e;
            } catch (IOException ioe) {
                this.lastError = ioe;
            }

            return null;
        }

        /*
         * Returns an enumerator for the key elements of this object.
         *
         * @return the enumerator - may have 0 length of coulmn names where not found
         */
       public Enumeration getProperties() {
           if (this.resultSet == null) {
                return (new Vector()).elements();
           }
           return this.colNames.elements();
       }


        public String[] getSpecialPropertyNames() {
            return new String[] {"length"}; //$NON-NLS-1$
        }


        public boolean next() {
            boolean status = false;
            if (this.lastRowSeen) {
                this.lastError = new SQLException(Messages.getString("DatabaseObject.20")); //$NON-NLS-1$
                return false;
            }
            if (this.resultSet == null) {
                this.lastError = new SQLException(Messages.getString("DatabaseObject.21")); //$NON-NLS-1$
                return false;
            }
            try {
                status = this.resultSet.next();
                this.lastError = null;
            } catch (SQLException e) {
                // System.err.println("##Cannot do next:" + e);
                // e.printStackTrace();
                this.lastError = e;
            }
            if (status) this.firstRowSeen = true;
            else this.lastRowSeen = true;
            return status;
       }

        @Override
        public String toString() {
            return "[RowSet: '"+this.sql+"'" +  //$NON-NLS-1$//$NON-NLS-2$
                   (this.resultSet==null ? " - released]" : //$NON-NLS-1$
                       (this.lastRowSeen ? " - at end]" : //$NON-NLS-1$
                       (this.firstRowSeen ? "]" : " - at start]")));  //$NON-NLS-1$//$NON-NLS-2$
        }

    }

}


