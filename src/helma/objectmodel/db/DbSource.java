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

package helma.objectmodel.db;

import helma.util.ResourceProperties;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.DriverPropertyInfo;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.Statement;
import java.util.Enumeration;
import java.util.Properties;
import java.util.logging.Logger;

import java.util.Hashtable;

/**
 *  This class describes a releational data source (URL, driver, user and password).
 */
public class DbSource {
    private static ResourceProperties defaultProps = null;
    private Properties conProps;
    private final String name;
    private ResourceProperties props, subProps;
    protected String url;
    private String driver;
    private boolean isOracle, isMySQL, isPostgreSQL, isH2, isSQLite;
    private long lastRead = 0L;
    private Hashtable dbmappings = new Hashtable();
    // compute hashcode statically because it's expensive and we need it often
    private int hashcode;
    // thread local connection holder for non-transactor threads
    private ThreadLocal connection;
    
    /**
     * The class loader to use for loading JDBC driver classes.
     */
    private ClassLoader classLoader;

    /**
     * Creates a new DbSource object.
     *
     * @param name the db source name
     * @param props the properties
     * 
     * @throws NoDriverException if the JDBC driver could not be loaded or is unusable 
     */
    public DbSource(String name, ResourceProperties props, ClassLoader classLoader) 
             throws NoDriverException {
        this.name = name;
        this.props = props;
        this.classLoader = classLoader;
        init();
    }

    /**
     * Get a JDBC connection to the db source.
     *
     * @return a JDBC connection
     * 
     * @throws NoDriverException if the JDBC driver could not be loaded or is unusable
     * @throws SQLException if the connection couldn't be created
     */
    public synchronized Connection getConnection()
            throws NoDriverException, SQLException {
        Connection con;
        Transactor tx = Transactor.getInstance();
        if (tx != null) {
            con = tx.getConnection(this);
        } else {
            con = getThreadLocalConnection();
        }

        boolean fileUpdated = props.lastModified() > lastRead ||
                (defaultProps != null && defaultProps.lastModified() > lastRead);

        if (con == null || con.isClosed() || fileUpdated) {
            init();
            con = DriverManager.getConnection(url, conProps);

            // If we wanted to use SQL transactions, we'd set autoCommit to
            // false here and make commit/rollback invocations in Transactor methods;
            // System.err.println ("Created new Connection to "+url);
            if (tx != null) {
                tx.registerConnection(this, con);
            } else {
                connection.set(con);
            }
        }

        return con;
    }

    /**
     * Used for connections not managed by a Helma transactor
     * @return a thread local tested connection, or null
     */
    private Connection getThreadLocalConnection() {
        if (connection == null) {
            connection = new ThreadLocal();
            return null;
        }
        Connection con = (Connection) connection.get();
        if (con != null) {
            // test if connection is still ok
            try {
                Statement stmt = con.createStatement();
                stmt.execute("SELECT 1");
                stmt.close();
            } catch (SQLException sx) {
                try {
                    con.close();
                } catch (SQLException ignore) {/* nothing to do */}
                return null;
            }
        }
        return con;
    }

    /**
     * Set the db properties to newProps, and return the old properties.
     * @param newProps the new properties to use for this db source
     * @return the old properties
     * 
     * @throws NoDriverException if the JDBC driver could not be loaded or is unusable 
     */
    public synchronized ResourceProperties switchProperties(ResourceProperties newProps) 
            throws NoDriverException {
        ResourceProperties oldProps = props;
        props = newProps;
        init();
        return oldProps;
    }

    /**
     * Initialize the db source from the properties
     *
     * @throws NoDriverException if the JDBC driver could not be loaded or is unusable
     */
    private synchronized void init() throws NoDriverException {
        lastRead = (defaultProps == null) ? props.lastModified()
                                          : Math.max(props.lastModified(),
                                                     defaultProps.lastModified());
        // refresh sub-properties for this DbSource
        subProps = props.getSubProperties(name + '.');
        // use properties hashcode for ourselves
        hashcode = subProps.hashCode();
        // get JDBC URL and driver class name
        url = subProps.getProperty("url");
        driver = subProps.getProperty("driver");
        // sanity checks
        if (url == null) {
            throw new NullPointerException(name+".url is not defined in db.properties");
        }
        if (driver == null) {
            throw new NullPointerException(name+".driver class not defined in db.properties");
        }
        // test if this is an Oracle or MySQL driver
        isOracle = driver.startsWith("oracle.jdbc.driver");
        isMySQL = driver.startsWith("com.mysql.jdbc") ||
                  driver.startsWith("org.gjt.mm.mysql");
        isPostgreSQL = driver.equals("org.postgresql.Driver");
        isH2 = driver.equals("org.h2.Driver");
        isSQLite = driver.equals("org.sqlite.JDBC");
        
        // check if a custom class loader shall be tried
        if (classLoader != null) {
            try {
                // get the driver's class
                Class driverClass = Class.forName(driver, true, classLoader);
                // register the driver with the driver manager
                DriverManager.registerDriver(new DriverWrapper((Driver) driverClass.newInstance()));
            } catch (ClassNotFoundException exception) {
                // ignore, the class might still be loadable by the default class loader
            } catch (Exception e) {
                // generalize the exception
                throw new NoDriverException(e);
            }
        } else {
            try {
                // let the default class loader load the class (and thus register the driver with the driver manager)
                Class.forName(driver);
            } catch (ClassNotFoundException e) {
                // generalize the exception
                throw new NoDriverException(e);
            }
        }

        // set up driver connection properties
        conProps=new Properties();
        String prop = subProps.getProperty("user");
        if (prop != null) {
            conProps.put("user", prop);
        }
        prop = subProps.getProperty("password");
        if (prop != null) {
            conProps.put("password", prop);
        }

        // read any remaining extra properties to be passed to the driver
        for (Enumeration e = subProps.keys(); e.hasMoreElements(); ) {
            String key = (String) e.nextElement();

            // filter out properties we alread have
            if ("url".equalsIgnoreCase(key) ||
                "driver".equalsIgnoreCase(key) ||
                "user".equalsIgnoreCase(key) ||
                "password".equalsIgnoreCase(key)) {
                continue;
            }
            conProps.setProperty(key, subProps.getProperty(key));
        }
    }

    /**
     * Return the class name of the JDBC driver
     *
     * @return the class name of the JDBC driver
     */
    public String getDriverName() {
        return driver;
    }

    /**
     * Return the name of the db dource
     *
     * @return the name of the db dource
     */
    public String getName() {
        return name;
    }

    /**
     * Set the default (server-wide) properties
     *
     * @param props server default db.properties
     */
    public static void setDefaultProps(ResourceProperties props) {
        defaultProps = props;
    }

    /**
     * Check if this DbSource represents an Oracle database
     *
     * @return true if we're using an oracle JDBC driver
     */
    public boolean isOracle() {
        return isOracle;
    }

    /**
     * Check if this DbSource represents a MySQL database
     *
     * @return true if we're using a MySQL JDBC driver
     */
    public boolean isMySQL() {
        return isMySQL;
    }

    /**
     * Check if this DbSource represents a PostgreSQL database
     *
     * @return true if we're using a PostgreSQL JDBC driver
     */
    public boolean isPostgreSQL() {
        return isPostgreSQL;
    }

    /**
     * Check if this DbSource represents a H2 database
     *
     * @return true if we're using a H2 JDBC driver
     */
    public boolean isH2() {
        return isH2;
    }
    
    /**
     * Check if this DbSource represents a SQLite database
     *
     * @return true if we're using a SQLite JDBC driver
     */
    public boolean isSQLite() {
        return isSQLite;
    }

    /**
     * Register a dbmapping by its table name.
     *
     * @param dbmap the DbMapping instance to register
     */
    protected void registerDbMapping(DbMapping dbmap) {
        if (!dbmap.inheritsStorage() && dbmap.getTableName() != null) {
            dbmappings.put(dbmap.getTableName().toUpperCase(), dbmap);
        }
    }

    /**
     * Look up a DbMapping instance for the given table name.
     *
     * @param tablename the table name
     * @return the matching DbMapping instance
     */
    protected DbMapping getDbMapping(String tablename) {
        return (DbMapping) dbmappings.get(tablename.toUpperCase());
    }

    /**
     * Returns a hash code value for the object.
     */
    public int hashCode() {
        return hashcode;
    }

    /**
     * Indicates whether some other object is "equal to" this one.
     */
    public boolean equals(Object obj) {
        return obj instanceof DbSource && subProps.equals(((DbSource) obj).subProps);
    }
}

/**
 * Wraps a JDBC driver so that JDBC drivers loaded from custom class loaders can be registered in the JDBC Driver 
 * Manager.
 * 
 * @see "http://www.kfu.com/~nsayer/Java/dyn-jdbc.html"
 * @todo Future versions of Java (8+) might not need this workaround anymore, review and remove if and when not
 *  necessary anymore.
 */
class DriverWrapper implements Driver {
    
    /**
     * The actual driver wrapped by this wrapper.
     */
    private Driver driver;
    
    /**
     * Wraps the given driver.
     * 
     * @param d
     *  The driver to wrap.
     */
    DriverWrapper(Driver d) {
        // remember the driver being wrapped
        this.driver = d;
    }
    
    @Override
    public boolean acceptsURL(String u) throws SQLException {
        // delegate
        return this.driver.acceptsURL(u);
    }
    
    @Override
    public Connection connect(String u, Properties p) throws SQLException {
        // delegate
        return this.driver.connect(u, p);
    }
    
    @Override
    public int getMajorVersion() {
        // delegate
        return this.driver.getMajorVersion();
    }
    
    @Override
    public int getMinorVersion() {
        // delegate
        return this.driver.getMinorVersion();
    }
    
    @Override
    public DriverPropertyInfo[] getPropertyInfo(String u, Properties p) throws SQLException {
        // delegate
        return this.driver.getPropertyInfo(u, p);
    }
    
    @Override
    public boolean jdbcCompliant() {
        // delegate
        return this.driver.jdbcCompliant();
    }

    @Override
    public Logger getParentLogger() throws SQLFeatureNotSupportedException {
        // delegate
        return this.driver.getParentLogger();
    }
}
