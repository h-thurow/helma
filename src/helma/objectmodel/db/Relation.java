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

import java.sql.SQLException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Vector;

import helma.framework.core.Application;
import helma.objectmodel.NodeInterface;
import helma.objectmodel.PropertyInterface;
import helma.util.StringUtils;

/**
 * This describes how a property of a persistent Object is stored in a
 *  relational database table. This can be either a scalar property (string, date, number etc.)
 *  or a reference to one or more other objects.
 */
public final class Relation {
    // these constants define different type of property-to-db-mappings
    // there is an error in the description of this relation
    public final static int INVALID = -1;

    // a mapping of a non-object, scalar type
    public final static int PRIMITIVE = 0;

    // a 1-to-1 relation, i.e. a field in the table is a foreign key to another object
    public final static int REFERENCE = 1;

    // a 1-to-many relation, a field in another table points to objects of this type
    public final static int COLLECTION = 2;

    // a 1-to-1 reference with multiple or otherwise not-trivial constraints
    // this is managed differently than REFERENCE, hence the separate type.
    public final static int COMPLEX_REFERENCE = 3;

    // constraints linked together by OR or AND if applicable?
    public final static String AND = " AND "; //$NON-NLS-1$
    public final static String OR = " OR "; //$NON-NLS-1$
    public final static String XOR = " XOR "; //$NON-NLS-1$
    private String logicalOperator = AND;

    // prefix to use for symbolic names of joined tables. The name is composed
    // from this prefix and the name of the property we're doing the join for
    final static String JOIN_PREFIX = "JOIN_"; //$NON-NLS-1$

    // direct mapping is a very powerful feature:
    // objects of some types can be directly accessed
    // by one of their properties/db fields.
    // public final static int DIRECT = 3;
    // the DbMapping of the type we come from
    DbMapping ownType;

    // the DbMapping of the prototype we link to, unless this is a "primitive" (non-object) relation
    DbMapping otherType;

    // the column type, as defined in java.sql.Types
    int columnType;

    //  if this relation defines a virtual node, we need to provide a DbMapping for these virtual nodes
    DbMapping virtualMapping;
    String propName;
    String columnName;
    int reftype;
    Constraint[] constraints;
    boolean virtual;
    boolean readonly;
    boolean lazyLoading;
    boolean aggressiveLoading;
    boolean aggressiveCaching;
    boolean isPrivate = false;
    boolean referencesPrimaryKey = false;
    String updateCriteria;
    String accessName; // db column used to access objects through this relation
    String order;
    boolean autoSorted = false;
    String groupbyOrder;
    String groupby;
    String prototype;
    String groupbyPrototype;
    String filter;
    private String additionalTables;
    private boolean additionalTablesJoined = false;
    String queryHints;
    Vector filterFragments;
    Vector filterPropertyRefs;
    int maxSize = 0;
    int offset = 0;

    /**
     * This constructor makes a copy of an existing relation. Not all fields are copied, just those
     * which are needed in groupby- and virtual nodes defined by this relation. use
     * {@link Relation#getClone()} to get a full copy of this relation.
     */
    protected Relation(Relation rel) {
        // Note: prototype, groupby, groupbyPrototype and groupbyOrder aren't copied here.
        // these are set by the individual get*Relation() methods as appropriate.
        this.ownType =                  rel.ownType;
        this.otherType =                rel.otherType;
        this.propName =                 rel.propName;
        this.columnName =               rel.columnName;
        this.reftype =                  rel.reftype;
        this.order =                    rel.order;
        this.filter =                   rel.filter;
        this.filterFragments =          rel.filterFragments;
        this.filterPropertyRefs =       rel.filterPropertyRefs;
        this.additionalTables =         rel.additionalTables;
        this.additionalTablesJoined =   rel.additionalTablesJoined;
        this.queryHints =               rel.queryHints;
        this.maxSize =                  rel.maxSize;
        this.offset =                   rel.offset;
        this.constraints =              rel.constraints;
        this.accessName =               rel.accessName;
        this.logicalOperator =          rel.logicalOperator;
        this.lazyLoading =              rel.lazyLoading;
        this.aggressiveLoading =        rel.aggressiveLoading;
        this.aggressiveCaching =        rel.aggressiveCaching;
        this.updateCriteria =           rel.updateCriteria;
        this.autoSorted =               rel.autoSorted;
    }

    /**
     * Reads a relation entry from a line in a properties file.
     */
    public Relation(String propName, DbMapping ownType) {
        this.ownType = ownType;
        this.propName = propName;
        this.otherType = null;
    }

    /**
     * Update this relation object from a properties object.
     * @param desc the top level relation descriptor. For relations
     *             defined in a type.properties file, this is a string like
     *             "collection(Type)", but for relations defined from
     *             JavaScript, it is the top level descriptor object.
     * @param props The subproperties for this relation.
     */
    public void update(Object desc, Properties props) {
        Application app = this.ownType.getApplication();

        if (desc instanceof Properties || parseDescriptor(desc, props)) {
            // converted to internal foo.collection = Bar representation
            String proto;
            if (props.containsKey("collection")) { //$NON-NLS-1$
                proto = props.getProperty("collection"); //$NON-NLS-1$
                this.virtual = !"_children".equalsIgnoreCase(this.propName); //$NON-NLS-1$
                this.reftype = COLLECTION;
            } else if (props.containsKey("mountpoint")) { //$NON-NLS-1$
                proto = props.getProperty("mountpoint"); //$NON-NLS-1$
                this.reftype = COLLECTION;
                this.virtual = true;
                this.prototype = proto;
            } else if (props.containsKey("object")) { //$NON-NLS-1$
                proto = props.getProperty("object"); //$NON-NLS-1$
                if (this.reftype != COMPLEX_REFERENCE) {
                    this.reftype = REFERENCE;
                }
                this.virtual = false;
            } else {
                throw new RuntimeException(Messages.getString("Relation.0") + desc); //$NON-NLS-1$
            }

            this.otherType = app.getDbMapping(proto);

            if (this.otherType == null) {
                throw new RuntimeException(Messages.getString("Relation.1") + proto + //$NON-NLS-1$
                                           Messages.getString("Relation.2") + this.ownType.getTypeName()); //$NON-NLS-1$
            }

            // make sure the type we're referring to is up to date!
            if (this.otherType.needsUpdate()) {
                this.otherType.update();
            }

        }

        this.readonly = "true".equalsIgnoreCase(props.getProperty("readonly")); //$NON-NLS-1$ //$NON-NLS-2$
        this.isPrivate = "true".equalsIgnoreCase(props.getProperty("private")); //$NON-NLS-1$ //$NON-NLS-2$

        // the following options only apply to object and collection relations
        if ((this.reftype != PRIMITIVE) && (this.reftype != INVALID)) {
            Vector newConstraints = new Vector();

            parseOptions(newConstraints, props);

            this.constraints = new Constraint[newConstraints.size()];
            newConstraints.copyInto(this.constraints);


            if (this.reftype == REFERENCE || this.reftype == COMPLEX_REFERENCE) {
                if (this.constraints.length == 0) {
                    this.referencesPrimaryKey = true;
                } else {
                    boolean rprim = false;
                    for (int i=0; i<this.constraints.length; i++) {
                        if (this.constraints[i].foreignKeyIsPrimary()) {
                            rprim = true;
                            break;
                        }
                    }
                    this.referencesPrimaryKey = rprim;
                }

                // check if this is a non-trivial reference
                if (this.constraints.length > 1 || !usesPrimaryKey()) {
                    this.reftype = COMPLEX_REFERENCE;
                } else {
                    this.reftype = REFERENCE;
                }
            }

            if (this.reftype == COLLECTION) {
                this.referencesPrimaryKey = (this.accessName == null) ||
                        this.accessName.equalsIgnoreCase(this.otherType.getIDField());
            }

            // if DbMapping for virtual nodes has already been created,
            // update its subnode relation.
            // FIXME: needs to be synchronized?
            if (this.virtualMapping != null) {
                this.virtualMapping.lastTypeChange = this.ownType.lastTypeChange;
                this.virtualMapping.subRelation = getVirtualSubnodeRelation();
                this.virtualMapping.propRelation = getVirtualPropertyRelation();
            }
        } else {
            this.referencesPrimaryKey = false;
        }
    }

    /**
     * Converts old style foo = collection(Bar) mapping to new style
     * foo.collection = Bar mappinng and returns true if a non-primitive mapping
     * was encountered.
     * @param value the value of the top level property mapping
     * @param config the sub-map for this property mapping
     * @return true if the value describes a valid, non-primitive property mapping
     */
    protected boolean parseDescriptor(Object value, Map config) {
        String desc = value instanceof String ? (String) value : null;

        if (desc == null || "".equals(desc.trim())) { //$NON-NLS-1$
            if (this.propName != null) {
                this.reftype = PRIMITIVE;
                this.columnName = this.propName;
            } else {
                this.reftype = INVALID;
                this.columnName = this.propName;
            }
            return false;
        }
        desc = desc.trim();

        int open = desc.indexOf("("); //$NON-NLS-1$
        int close = desc.indexOf(")", open); //$NON-NLS-1$

        if (open > -1 && close > open) {
            String ref = desc.substring(0, open).trim();
            String proto = desc.substring(open + 1, close).trim();

            if ("collection".equalsIgnoreCase(ref)) { //$NON-NLS-1$
                config.put("collection", proto); //$NON-NLS-1$
            } else if ("mountpoint".equalsIgnoreCase(ref)) { //$NON-NLS-1$
                config.put("mountpoint", proto); //$NON-NLS-1$
            } else if ("object".equalsIgnoreCase(ref)) { //$NON-NLS-1$
                config.put("object", proto); //$NON-NLS-1$
            } else {
                throw new RuntimeException(Messages.getString("Relation.3") + desc); //$NON-NLS-1$
            }

            return true;

        }
        this.virtual = false;
        this.columnName = desc;
        this.reftype = PRIMITIVE;
        return false;

    }

    protected void parseOptions(Vector cnst, Properties props) {
        String loading = props.getProperty("loadmode"); //$NON-NLS-1$

        if (loading != null) {
            loading = loading.trim();
            if ("aggressive".equalsIgnoreCase(loading)) { //$NON-NLS-1$
                this.aggressiveLoading = true;
                this.lazyLoading = false;
            } else if ("lazy".equalsIgnoreCase(loading)) { //$NON-NLS-1$
                this.lazyLoading = true;
                this.aggressiveLoading = false;
            } else {
                System.err.println(Messages.getString("Relation.4") + this.ownType + Messages.getString("Relation.5") + loading); //$NON-NLS-1$ //$NON-NLS-2$
                this.aggressiveLoading = this.lazyLoading = false;
            }
        } else {
            this.aggressiveLoading = this.lazyLoading = false;
        }

        String caching = props.getProperty("cachemode"); //$NON-NLS-1$

        this.aggressiveCaching = (caching != null) &&
                            "aggressive".equalsIgnoreCase(caching.trim()); //$NON-NLS-1$

        // get order property
        this.order = props.getProperty("order"); //$NON-NLS-1$

        if ((this.order != null) && (this.order.trim().length() == 0)) {
            this.order = null;
        }

        // get the criteria(s) for updating this collection
        this.updateCriteria = props.getProperty("updatecriteria"); //$NON-NLS-1$

        // get the autosorting flag
        this.autoSorted = "auto".equalsIgnoreCase(props.getProperty("sortmode")); //$NON-NLS-1$ //$NON-NLS-2$

        // get additional filter property
        this.filter = props.getProperty("filter"); //$NON-NLS-1$

        if (this.filter != null) {
            if (this.filter.trim().length() == 0) {
                this.filter = null;
                this.filterFragments = this.filterPropertyRefs = null;
            } else {
                // parenthesise filter
                Vector fragments = new Vector();
                Vector propertyRefs = new Vector();
                parsePropertyString(this.filter, fragments, propertyRefs);
                // if no references where found, just use the filter string
                // otherwise use the filter fragments and proeprty refs instead
                if (propertyRefs.size() > 0) {
                    this.filterFragments = fragments;
                    this.filterPropertyRefs = propertyRefs;
                } else {
                    this.filterFragments = this.filterPropertyRefs = null;
                }
            }
        }

        // get additional tables
        this.additionalTables = props.getProperty("filter.additionalTables"); //$NON-NLS-1$

        if (this.additionalTables != null) {
            if (this.additionalTables.trim().length() == 0) {
                this.additionalTables = null;
            } else {
                String ucTables = this.additionalTables.toUpperCase();
                // create dependencies implied by additional tables
                DbSource dbsource = this.otherType.getDbSource();
                if (dbsource != null) {
                    String[] tables = StringUtils.split(ucTables, ", "); //$NON-NLS-1$
                    for (int i=0; i<tables.length; i++) {
                        // Skip some join-related keyworks we might encounter here
                        if ("AS".equals(tables[i]) || "ON".equals(tables[i])) { //$NON-NLS-1$ //$NON-NLS-2$
                            continue;
                        }
                        DbMapping dbmap = dbsource.getDbMapping(tables[i]);
                        if (dbmap != null) {
                            dbmap.addDependency(this.otherType);
                        }
                    }
                }
                // see wether the JOIN syntax is used. look for " join " with whitespaces on both sides
                // and for "join " at the beginning:
                this.additionalTablesJoined = (ucTables.indexOf(" JOIN ") != -1 || //$NON-NLS-1$
                        ucTables.startsWith("STRAIGHT_JOIN ") || ucTables.startsWith("JOIN "));  //$NON-NLS-1$//$NON-NLS-2$
            }
        }

        // get query hints
        this.queryHints = props.getProperty("hints"); //$NON-NLS-1$

        // get max size of collection
        this.maxSize = getIntegerProperty("maxSize", props, 0); //$NON-NLS-1$
        if (this.maxSize == 0) {
            // use limit as alias for maxSize
            this.maxSize = getIntegerProperty("limit", props, 0); //$NON-NLS-1$
        }
        this.offset = getIntegerProperty("offset", props, 0); //$NON-NLS-1$

        // get group by property
        this.groupby = props.getProperty("group"); //$NON-NLS-1$

        if (this.groupby != null && this.groupby.trim().length() == 0) {
            this.groupby = null;
        }

        if (this.groupby != null) {
            this.groupbyOrder = props.getProperty("group.order"); //$NON-NLS-1$

            if (this.groupbyOrder != null && this.groupbyOrder.trim().length() == 0) {
                this.groupbyOrder = null;
            }

            this.groupbyPrototype = props.getProperty("group.prototype"); //$NON-NLS-1$

            if (this.groupbyPrototype != null && this.groupbyPrototype.trim().length() == 0) {
                this.groupbyPrototype = null;
            }

            // aggressive loading and caching is not supported for groupby-nodes
            // aggressiveLoading = aggressiveCaching = false;
        }

        // check if subnode condition should be applied for property relations
        this.accessName = props.getProperty("accessname"); //$NON-NLS-1$

        // parse contstraints
        String local = props.getProperty("local"); //$NON-NLS-1$
        String foreign = props.getProperty("foreign"); //$NON-NLS-1$

        if (local != null && foreign != null) {
            cnst.addElement(new Constraint(local, foreign, false));
            this.columnName = local;
        }

        // parse additional contstraints from *.1 to *.9
        for (int i = 1; i < 10; i++) {
            local = props.getProperty("local."+i); //$NON-NLS-1$
            foreign = props.getProperty("foreign."+i); //$NON-NLS-1$

            if (local != null && foreign != null) {
                cnst.addElement(new Constraint(local, foreign, false));
            }
        }

        // parse constraints logic
        if (cnst.size() > 1) {
            String logic = props.getProperty("logicalOperator"); //$NON-NLS-1$
            if ("and".equalsIgnoreCase(logic)) { //$NON-NLS-1$
                this.logicalOperator = AND;
            } else if ("or".equalsIgnoreCase(logic)) { //$NON-NLS-1$
                this.logicalOperator = OR;
            } else if ("xor".equalsIgnoreCase(logic)) { //$NON-NLS-1$
                this.logicalOperator = XOR;
            } else {
                this.logicalOperator = AND;
            }
        } else {
            this.logicalOperator = AND;
        }

    }

    private int getIntegerProperty(String name, Properties props, int defaultValue) {
        Object value = props.get(name);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        } else if (value instanceof String) {
            try {
                return Integer.parseInt((String) value);
            } catch (NumberFormatException nfx) {
                this.ownType.getApplication().logError(Messages.getString("Relation.6") //$NON-NLS-1$
                        + name + Messages.getString("Relation.7") + value, nfx); //$NON-NLS-1$
            }
        }
        return defaultValue;
    }

    ///////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Get the configuration properties for this relation.
     */
    public Map getConfig() {
        return this.ownType.getSubProperties(this.propName + '.');
    }

    /**
     * Does this relation describe a virtual (collection) node?
     */
    public boolean isVirtual() {
        return this.virtual;
    }

    /**
     * Return the target type of this relation, or null if this is a primitive mapping.
     */
    public DbMapping getTargetType() {
        return this.otherType;
    }

    /**
     * Get the reference type of this relation.
     */
    public int getRefType() {
        return this.reftype;
    }

    /**
     * Tell if this relation represents a primitive (scalar) value mapping.
     */
    public boolean isPrimitive() {
        return this.reftype == PRIMITIVE;
    }

    /**
     *  Returns true if this Relation describes an object reference property
     */
    public boolean isReference() {
        return this.reftype == REFERENCE;
    }

    /**
     *  Returns true if this Relation describes either a primitive value
     *  or an object reference.
     */
    public boolean isPrimitiveOrReference() {
        return this.reftype == PRIMITIVE || this.reftype == REFERENCE;
    }

    /**
     *  Returns true if this Relation describes a collection.
     *  <b>NOTE:</b> this will return true both for collection objects
     *  (aka virtual nodes) and direct child object relations, so
     *  isVirtual() should be used to identify relations that define
     *  <i>collection properties</i>!
     */
    public boolean isCollection() {
        return this.reftype == COLLECTION;
    }

    /**
     *  Returns true if this Relation describes a complex object reference property
     */
    public boolean isComplexReference() {
        return this.reftype == COMPLEX_REFERENCE;
    }

    /**
     *  Tell wether the property described by this relation is to be handled as private, i.e.
     *  a change on it should not result in any changed object/collection relations.
     */
    public boolean isPrivate() {
        return this.isPrivate;
    }

    /**
     *  Check whether aggressive loading is set for this relation
     */
    public boolean loadAggressively() {
        return this.aggressiveLoading;
    }

    /**
     *  Returns the number of constraints for this relation.
     */
    public int countConstraints() {
        if (this.constraints == null)
            return 0;
        return this.constraints.length;
    }

    /**
     *  Returns true if the object represented by this Relation has to be
     *  created on demand at runtime by the NodeManager. This is true for:
     *
     *  - collection (aka virtual) nodes
     *  - nodes accessed via accessname
     *  - group nodes
     *  - complex reference nodes
     */
    public boolean createOnDemand() {
        if (this.otherType == null) {
            return false;
        }

        return this.virtual ||
            (this.otherType.isRelational() && this.accessName != null) ||
            (this.groupby != null) || isComplexReference();
    }

    /**
     *  Returns true if the object represented by this Relation has to be
     *  persisted in the internal db in order to be functional. This is true if
     *  the subnodes contained in this collection are stored in the embedded
     *  database. In this case, the collection itself must also be an ordinary
     *  object stored in the db, since a virtual collection would lose its
     *  its content after restarts.
     */
    public boolean needsPersistence() {
        if (!this.virtual) {
            // ordinary object references always need to be persisted
            return true;
        }

        // collections/mountpoints need to be persisted if the
        // child object type is non-relational. Depending on
        // whether prototype is null or not, we need to look at
        // otherType itself or otherType's subnode mapping.
        if (this.prototype == null) {
            // an ordinary, unprototyped virtual node -
            // otherType is the content type
            return !this.otherType.isRelational();
        }
        // a prototyped virtual node or mountpoint -
        // otherType is the virtual node type itself
        DbMapping sub = this.otherType.getSubnodeMapping();
        return sub != null && !sub.isRelational();
    }

    /**
     * Return the prototype to be used for object reached by this relation
     */
    public String getPrototype() {
        return this.prototype;
    }

    /**
     * Return the name of the local property this relation is defined for
     */
    public String getPropName() {
        return this.propName;
    }

    /**
     *
     *
     * @param ct ...
     */
    public void setColumnType(int ct) {
        this.columnType = ct;
    }

    /**
     *
     *
     * @return ...
     */
    public int getColumnType() {
        return this.columnType;
    }

    /**
     *  Get the group for a collection relation, if defined.
     *
     * @return the name of the column used to group child objects, if any.
     */
    public String getGroup() {
        return this.groupby;
    }

    /**
     * Add a constraint to the current list of constraints
     */
    protected void addConstraint(Constraint c) {
        if (this.constraints == null) {
            this.constraints = new Constraint[1];
            this.constraints[0] = c;
        } else {
            Constraint[] nc = new Constraint[this.constraints.length + 1];

            System.arraycopy(this.constraints, 0, nc, 0, this.constraints.length);
            nc[nc.length - 1] = c;
            this.constraints = nc;
        }
    }

    /**
     *
     *
     * @return true if the foreign key used for this relation is the
     * other object's primary key.
     */
    public boolean usesPrimaryKey() {
        return this.referencesPrimaryKey;
    }

    /**
     *
     *
     * @return ...
     */
    public boolean hasAccessName() {
        return this.accessName != null;
    }

    /**
     *
     *
     * @return ...
     */
    public String getAccessName() {
        return this.accessName;
    }

    /**
     *
     *
     * @return ...
     */
    public Relation getSubnodeRelation() {
        // return subnoderelation;
        return null;
    }

    /**
     * Return the local field name for updates.
     */
    public String getDbField() {
        return this.columnName;
    }

    /**
     * This is taken from org.apache.tools.ant ProjectHelper.java
     * distributed under the Apache Software License, Version 1.1
     *
     * Parses a string containing <code>${xxx}</code> style property
     * references into two lists. The first list is a collection
     * of text fragments, while the other is a set of string property names.
     * <code>null</code> entries in the first list indicate a property
     * reference from the second list.
     *
     * @param value     Text to parse. Must not be <code>null</code>.
     * @param fragments List to add text fragments to.
     *                  Must not be <code>null</code>.
     * @param propertyRefs List to add property names to.
     *                     Must not be <code>null</code>.
     */
    protected void parsePropertyString(String value, Vector fragments, Vector propertyRefs) {
        int prev = 0;
        int pos;
        //search for the next instance of $ from the 'prev' position
        while ((pos = value.indexOf("$", prev)) >= 0) { //$NON-NLS-1$

            //if there was any text before this, add it as a fragment
            //TODO, this check could be modified to go if pos>prev;
            //seems like this current version could stick empty strings
            //into the list
            if (pos > 0) {
                fragments.addElement(value.substring(prev, pos));
            }
            //if we are at the end of the string, we tack on a $
            //then move past it
            if (pos == (value.length() - 1)) {
                fragments.addElement("$"); //$NON-NLS-1$
                prev = pos + 1;
            } else if (value.charAt(pos + 1) != '{') {
                //peek ahead to see if the next char is a property or not
                //not a property: insert the char as a literal
                /*
                fragments.addElement(value.substring(pos + 1, pos + 2));
                prev = pos + 2;
                */
                if (value.charAt(pos + 1) == '$') {
                    //backwards compatibility two $ map to one mode
                    fragments.addElement("$"); //$NON-NLS-1$
                    prev = pos + 2;
                } else {
                    //new behaviour: $X maps to $X for all values of X!='$'
                    fragments.addElement(value.substring(pos, pos + 2));
                    prev = pos + 2;
                }

            } else {
                //property found, extract its name or bail on a typo
                int endName = value.indexOf('}', pos);
                if (endName < 0) {
                    throw new RuntimeException(Messages.getString("Relation.8") //$NON-NLS-1$
                                                 + value);
                }
                String propertyName = value.substring(pos + 2, endName);
                fragments.addElement(null);
                propertyRefs.addElement(propertyName);
                prev = endName + 1;
            }
        }
        //no more $ signs found
        //if there is any tail to the file, append it
        if (prev < value.length()) {
            fragments.addElement(value.substring(prev));
        }
    }

    /**
     *  get a DbMapping to use for virtual aka collection nodes.
     */
    public DbMapping getVirtualMapping() {
        // return null unless this relation describes a virtual/collection node.
        if (!this.virtual) {
            return null;
        }

        // create a synthetic DbMapping that describes how to fetch the
        // collection's child objects.
        if (this.virtualMapping == null) {
            // if the collection node is prototyped (a mountpoint), create
            // a virtual sub-mapping from the app's DbMapping for that prototype
            if (this.prototype != null) {
                this.virtualMapping = new DbMapping(this.ownType.app, this.prototype);
            } else {
                this.virtualMapping = new DbMapping(this.ownType.app, null);
                this.virtualMapping.subRelation = getVirtualSubnodeRelation();
                this.virtualMapping.propRelation = getVirtualPropertyRelation();
            }
        }
        this.virtualMapping.lastTypeChange = this.ownType.lastTypeChange;
        return this.virtualMapping;
    }

    /**
     * Return the db mapping for a propery relation.
     * @return the target mapping of this property relation
     */
    public DbMapping getPropertyMapping() {
        // if this is an untyped virtual node, it doesn't have a dbmapping
        if (!this.virtual || this.prototype != null) {
            return this.otherType;
        }
        return null;
    }

    /**
     * Return a Relation that defines the subnodes of a virtual node.
     */
    Relation getVirtualSubnodeRelation() {
        if (!this.virtual) {
            throw new RuntimeException(Messages.getString("Relation.9")); //$NON-NLS-1$
        }

        Relation vr = new Relation(this);

        vr.groupby = this.groupby;
        vr.groupbyOrder = this.groupbyOrder;
        vr.groupbyPrototype = this.groupbyPrototype;

        return vr;
    }

    /**
     * Return a Relation that defines the properties of a virtual node.
     */
    Relation getVirtualPropertyRelation() {
        if (!this.virtual) {
            throw new RuntimeException(Messages.getString("Relation.10")); //$NON-NLS-1$
        }

        Relation vr = new Relation(this);

        vr.groupby = this.groupby;
        vr.groupbyOrder = this.groupbyOrder;
        vr.groupbyPrototype = this.groupbyPrototype;

        return vr;
    }

    /**
     * Return a Relation that defines the subnodes of a group-by node.
     */
    Relation getGroupbySubnodeRelation() {
        if (this.groupby == null) {
            throw new RuntimeException(Messages.getString("Relation.11")); //$NON-NLS-1$
        }

        Relation vr = new Relation(this);

        vr.prototype = this.groupbyPrototype;
        vr.addConstraint(new Constraint(null, this.groupby, true));

        return vr;
    }

    /**
     * Return a Relation that defines the properties of a group-by node.
     */
    Relation getGroupbyPropertyRelation() {
        if (this.groupby == null) {
            throw new RuntimeException(Messages.getString("Relation.12")); //$NON-NLS-1$
        }

        Relation vr = new Relation(this);

        vr.prototype = this.groupbyPrototype;
        vr.addConstraint(new Constraint(null, this.groupby, true));

        return vr;
    }

    public StringBuffer getIdSelect() {
        StringBuffer buf = new StringBuffer("SELECT "); //$NON-NLS-1$

        if (this.queryHints != null) {
                buf.append(this.queryHints).append(" "); //$NON-NLS-1$
            }

        String table = this.otherType.getTableName();
        String idfield;
        if (this.groupby == null) {
            idfield = this.otherType.getIDField();
        } else {
            idfield = this.groupby;
            buf.append("DISTINCT "); //$NON-NLS-1$
        }

        if (idfield.indexOf('(') == -1 && idfield.indexOf('.') == -1) {
            buf.append(table).append('.');
        }
        buf.append(idfield).append(" FROM ").append(table); //$NON-NLS-1$
        appendAdditionalTables(buf);

        return buf;
    }

    public StringBuffer getCountSelect() {
        StringBuffer buf = new StringBuffer("SELECT "); //$NON-NLS-1$
        if (this.otherType.isOracle() && this.maxSize > 0) {
            buf.append("* FROM "); //$NON-NLS-1$
        } else {
            if (this.groupby == null) {
                buf.append("count(*) FROM "); //$NON-NLS-1$
            } else {
                buf.append("count(DISTINCT ").append(this.groupby).append(") FROM "); //$NON-NLS-1$ //$NON-NLS-2$
            }
        }

        buf.append(this.otherType.getTableName());
        appendAdditionalTables(buf);

        return buf;
    }

    public StringBuffer getNamesSelect() {
        // if we do a groupby query (creating an intermediate layer of groupby nodes),
        // retrieve the value of that field instead of the primary key
        String namefield = (this.groupby == null) ? this.accessName : this.groupby;
        String table = this.otherType.getTableName();
        StringBuffer buf = new StringBuffer("SELECT "); //$NON-NLS-1$
        buf.append(namefield).append(" FROM ").append(table); //$NON-NLS-1$
        appendAdditionalTables(buf);

        return buf;
    }

    /**
     *  Build the second half of an SQL select statement according to this relation
     *  and a local object.
     *
     * @throws SQLException
     * @throws NoDriverException if the JDBC driver could not be loaded or is unusable
     */
    public void buildQuery(StringBuffer q, Node home, boolean useOrder, boolean isCount)
            throws SQLException, NoDriverException {
        buildQuery(q, home, this.otherType, null, useOrder, isCount);
    }

    /**
     *  Build the second half of an SQL select statement according to this relation
     *  and a local object.
     *
     * @throws SQLException
     * @throws NoDriverException if the JDBC driver could not be loaded or is unusable
     */
    public void buildQuery(StringBuffer q, Node home, DbMapping otherDbm, String kstr,
                           boolean useOrder, boolean isCount)
            throws SQLException, NoDriverException {
        String prefix = " WHERE "; //$NON-NLS-1$
        Node nonvirtual = home.getNonVirtualParent();

        if (kstr != null && !isComplexReference()) {
            q.append(prefix);

            String accessColumn = (this.accessName == null) ?
                    otherDbm.getIDField() : this.accessName;
            otherDbm.appendCondition(q, accessColumn, kstr);

            prefix = " AND "; //$NON-NLS-1$
        }

        // render the constraints and filter
        renderConstraints(q, home, nonvirtual, otherDbm, prefix);

        // add joined fetch constraints
        this.ownType.addJoinConstraints(q, prefix);

        // add group and order clauses
        if (this.groupby != null) {
            if (useOrder && (this.groupbyOrder != null)) {
                q.append(" ORDER BY ").append(this.groupbyOrder); //$NON-NLS-1$
            }
        } else if (useOrder && (this.order != null)) {
            q.append(" ORDER BY ").append(this.order); //$NON-NLS-1$
        }

        // apply limit and offset, but not if the query is for a single object
        if (this.maxSize > 0 && kstr == null) {
            if (this.otherType.isOracle()) {
                // see http://www.oracle.com/technology/oramag/oracle/06-sep/o56asktom.html
                String selectItem = isCount ? "count(*)" : "*"; //$NON-NLS-1$ //$NON-NLS-2$
                if (this.offset > 0) {
                    q.insert(0, "SELECT " + selectItem + " FROM ( SELECT /*+ FIRST_ROWS(n) */ a.*, ROWNUM rnum FROM (");  //$NON-NLS-1$//$NON-NLS-2$
                    q.append(") a WHERE ROWNUM <= ").append(this.offset + this.maxSize).append(") WHERE rnum > ").append(this.offset); //$NON-NLS-1$ //$NON-NLS-2$
                } else {
                    q.insert(0, "SELECT /*+ FIRST_ROWS(n) */ " + selectItem + " FROM ("); //$NON-NLS-1$ //$NON-NLS-2$
                    q.append(") WHERE ROWNUM <= ").append(this.maxSize); //$NON-NLS-1$
                }
            } else {
                q.append(" LIMIT ").append(this.maxSize); //$NON-NLS-1$
                if (this.offset > 0) {
                    q.append(" OFFSET ").append(this.offset); //$NON-NLS-1$
                }
            }
        }

    }

    protected void appendAdditionalTables(StringBuffer q) {
        if (this.additionalTables != null) {
            q.append(this.additionalTablesJoined ? ' ' : ',');
            q.append(this.additionalTables);
        }
    }

    /**
     *  Build the filter.
     */
    protected void appendFilter(StringBuffer q, NodeInterface nonvirtual, String prefix) {
        q.append(prefix);
        q.append('(');
        if (this.filterFragments == null) {
            q.append(this.filter);
        } else {
            Enumeration i = this.filterFragments.elements();
            Enumeration j = this.filterPropertyRefs.elements();
            while (i.hasMoreElements()) {
                String fragment = (String) i.nextElement();
                if (fragment == null) {
                    // begin column version
                    String columnName = (String) j.nextElement();
                    Object value = null;
                    if (columnName != null) {
                        DbMapping dbmap = nonvirtual.getDbMapping();
                        String propertyName = dbmap.columnNameToProperty(columnName);
                        if (propertyName == null)
                            propertyName = columnName;
                        PropertyInterface property = nonvirtual.get(propertyName);
                        if (property != null) {
                            value = property.getStringValue();
                        }
                        if (value == null) {
                            if (columnName.equalsIgnoreCase(dbmap.getIDField())) {
                                value = nonvirtual.getID();
                            } else if (columnName.equalsIgnoreCase(dbmap.getNameField())) {
                                value = nonvirtual.getName();
                            } else if (columnName.equalsIgnoreCase(dbmap.getPrototypeField())) {
                                value = dbmap.getExtensionId();
                            }
                        }
                    }
                    // end column version
                    if (value != null) {
                        q.append(DbMapping.escapeString(value.toString()));
                    } else {
                        q.append("NULL"); //$NON-NLS-1$
                    }
                } else {
                    q.append(fragment);
                }
            }
        }
        q.append(')');
    }

    /**
     * Render contraints and filter conditions to an SQL query string buffer.
     *
     * @param q the query string
     * @param home our home node
     * @param prefix the prefix to use to append to the existing query (e.g. " AND ")
     *
     * @throws SQLException sql related exception
     * @throws NoDriverException if the JDBC driver could not be loaded or is unusable
     */
    public void renderConstraints(StringBuffer q, Node home, String prefix)
                             throws SQLException, NoDriverException {
        renderConstraints(q, home, home.getNonVirtualParent(), this.otherType, prefix);
    }

    /**
     * Render contraints and filter conditions to an SQL query string buffer.
     *
     * @param q the query string
     * @param home our home node
     * @param nonvirtual our non-virtual home nod
     * @param otherDbm the DbMapping of the remote Node
     * @param prefix the prefix to use to append to the existing query (e.g. " AND ")
     *
     * @throws SQLException sql related exception
     * @throws NoDriverException if the JDBC driver could not be loaded or is unusable
     */
    public void renderConstraints(StringBuffer q, Node home, Node nonvirtual,
                                  DbMapping otherDbm, String prefix)
                             throws SQLException, NoDriverException {

        if (this.constraints.length > 1 && this.logicalOperator != AND) {
            q.append(prefix);
            q.append("("); //$NON-NLS-1$
            prefix = ""; //$NON-NLS-1$
        }

        for (int i = 0; i < this.constraints.length; i++) {
            if (this.constraints[i].foreignKeyIsPrototype()) {
                // if foreign key is $prototype we already have this constraint
                // covered by doing the select on the proper table
                continue;
            }
            q.append(prefix);
            this.constraints[i].addToQuery(q, home, nonvirtual, otherDbm);
            prefix = this.logicalOperator;
        }

        if (this.constraints.length > 1 && this.logicalOperator != AND) {
            q.append(")"); //$NON-NLS-1$
            prefix = " AND "; //$NON-NLS-1$
        }

        // also take the prototype into consideration if someone
        // specifies an extension of an prototype inside the brakets of
        // a type.properties's collection, only nodes having this proto
        // sould appear inside the collection
        if (otherDbm.inheritsStorage()) {
            String protoField = otherDbm.getPrototypeField();
            String[] extensions = otherDbm.getExtensions();

            // extensions should never be null for extension- and
            // extended prototypes. nevertheless we check it here
            if (extensions != null && protoField != null) {
                q.append(prefix);
                otherDbm.appendCondition(q, protoField, extensions);
                prefix = " AND "; //$NON-NLS-1$
            }
        }

        if (this.filter != null) {
            appendFilter(q, nonvirtual, prefix);
        }
    }

    /**
     *  Render the constraints for this relation for use within
     *  a left outer join select statement for the base object.
     *
     * @param select the string buffer to write to
     * @param isOracle create Oracle pre-9 style left outer join
     */
    public void renderJoinConstraints(StringBuffer select, boolean isOracle) {
        for (int i = 0; i < this.constraints.length; i++) {
            select.append(this.ownType.getTableName());
            select.append("."); //$NON-NLS-1$
            select.append(this.constraints[i].localKey);
            select.append(" = "); //$NON-NLS-1$
            select.append(JOIN_PREFIX);
            select.append(this.propName);
            select.append("."); //$NON-NLS-1$
            select.append(this.constraints[i].foreignKey);
            if (isOracle) {
                // create old oracle style join - see
                // http://www.praetoriate.com/oracle_tips_outer_joins.htm
                select.append("(+)"); //$NON-NLS-1$
            }
            if (i == this.constraints.length-1) {
                select.append(" "); //$NON-NLS-1$
            } else {
                select.append(" AND "); //$NON-NLS-1$
            }
        }

    }

    /**
     * Get the order section to use for this relation
     */
    public String getOrder() {
        if (this.groupby != null) {
            return this.groupbyOrder;
        }
        return this.order;
    }

    /**
     *  Tell wether the property described by this relation is to be handled
     *  as readonly/write protected.
     */
    public boolean isReadonly() {
        return this.readonly;
    }

    /**
     * Get a copy of this relation.
     * @return a clone of this relation
     */
    public Relation getClone() {
        Relation rel = new Relation(this);
        rel.prototype        = this.prototype;
        rel.groupby          = this.groupby;
        rel.groupbyPrototype = this.groupbyPrototype;
        rel.groupbyOrder     = this.groupbyOrder;
        return rel;
    }

    /**
     * Check if the child node fullfills the constraints defined by this relation.
     * FIXME: This always returns false if the relation has a filter value set,
     * since we can't determine if the filter constraints are met without
     * querying the database.
     *
     * @param parent the parent object - may be a virtual or group node
     * @param child the child object
     * @return true if all constraints are met
     */
    public boolean checkConstraints(Node parent, Node child) {
        // problem: if a filter property is defined for this relation,
        // i.e. a piece of static SQL-where clause, we'd have to evaluate it
        // in order to check the constraints. Because of this, if a filter
        // is defined, we return false as soon as the modified-time is greater
        // than the create-time of the child, i.e. if the child node has been
        // modified since it was first fetched from the db.
        if (this.filter != null && child.lastModified() > child.created()) {
            return false;
        }

        // counter for constraints and satisfied constraints
        int count = 0;
        int satisfied = 0;

        NodeInterface nonvirtual = parent.getNonVirtualParent();
        DbMapping otherDbm = child.getDbMapping();
        if (otherDbm == null) {
            otherDbm = this.otherType;
        }

        for (int i = 0; i < this.constraints.length; i++) {
            Constraint cnst = this.constraints[i];
            String propname = cnst.foreignProperty(otherDbm);

            if (propname != null) {
                NodeInterface home = cnst.isGroupby ? parent
                                            : nonvirtual;
                String value = null;

                if (home != null) {
                    if (cnst.localKeyIsPrimary(home.getDbMapping())) {
                        value = home.getID();
                    } else if (cnst.localKeyIsPrototype()) {
                        value = home.getDbMapping().getStorageTypeName();
                    } else if (ownType.isRelational()) {
                        value = home.getString(cnst.localProperty());
                    } else {
                        value = home.getString(cnst.localKey);
                    }
                }

                count++;

                if (value != null && value.equals(child.getString(propname))) {
                    satisfied++;
                }
            }
        }

        // check if enough constraints are met depending on logical operator
        if (this.logicalOperator == OR) {
            return satisfied > 0;
        } else if (this.logicalOperator == XOR) {
            return satisfied == 1;
        } else {
            return satisfied == count;
        }
    }

    /**
     * Make sure that the child node fullfills the constraints defined by this relation by setting the
     * appropriate properties
     */
    public void setConstraints(Node parent, Node child) {

        // if logical operator is OR or XOR we just return because we
        // wouldn't know what to do anyway
        if (this.logicalOperator != AND) {
            return;
        }

        Node home = parent.getNonVirtualParent();

        for (int i = 0; i < this.constraints.length; i++) {
            Constraint cnst = this.constraints[i];
            // don't set groupby constraints since we don't know if the
            // parent node is the base node or a group node
            if (cnst.isGroupby) {
                continue;
            }

            // check if we update the local or the other object, depending on
            // whether the primary key of either side is used.
            boolean foreignIsPrimary = cnst.foreignKeyIsPrimary();
            if (foreignIsPrimary || cnst.foreignKeyIsPrototype()) {
                String localProp = cnst.localProperty();
                if (localProp == null) {
                    throw new RuntimeException(Messages.getString("Relation.13") + cnst.localKey + //$NON-NLS-1$
                       Messages.getString("Relation.14") + //$NON-NLS-1$
                       Relation.this);
                } else if (foreignIsPrimary && child.getState() == NodeInterface.TRANSIENT) {
                    throw new RuntimeException(this.propName + Messages.getString("Relation.15") + //$NON-NLS-1$
                       Messages.getString("Relation.16") + localProp); //$NON-NLS-1$
                } else {
                    String value = foreignIsPrimary ?
                            child.getID() : child.getDbMapping().getStorageTypeName();
                    home.setString(localProp, value);
                }
                continue;
            }

            DbMapping otherDbm = child.getDbMapping();
            if (otherDbm == null) {
                otherDbm = this.otherType;
            }

            Relation crel = otherDbm.columnNameToRelation(cnst.foreignKey);

            if (crel != null) {

                if (cnst.localKeyIsPrimary(home.getDbMapping())) {
                    // only set node if property in child object is defined as reference.
                    if (crel.reftype == REFERENCE) {
                        NodeInterface currentValue = child.getNode(crel.propName);

                        // we set the backwards reference iff the reference is currently unset, if
                        // is set to a transient object, or if the new target is not transient. This
                        // prevents us from overwriting a persistent refererence with a transient one,
                        // which would most probably not be what we want.
                        if ((currentValue == null) ||
                                ((currentValue != home) &&
                                ((currentValue.getState() == NodeInterface.TRANSIENT) ||
                                (home.getState() != NodeInterface.TRANSIENT)))) try {
                            child.setNode(crel.propName, home);
                        } catch (Exception ignore) {
                            // in some cases, getNonVirtualParent() doesn't work
                            // correctly for transient nodes, so this may fail.
                        }
                    } else if (crel.reftype == PRIMITIVE) {
                        if (home.getState() == NodeInterface.TRANSIENT) {
                            throw new RuntimeException(Messages.getString("Relation.17") + crel); //$NON-NLS-1$
                        }
                        child.setString(crel.propName, home.getID());
                    }
                } else if (crel.reftype == PRIMITIVE) {
                    if (cnst.localKeyIsPrototype()) {
                        child.setString(crel.propName, home.getDbMapping().getStorageTypeName());
                    } else {
                        Property prop = home.getProperty(cnst.localProperty());
                        if (prop != null) {
                            child.set(crel.propName, prop.getValue(), prop.getType());
                        } else {
                            prop = child.getProperty(cnst.foreignProperty(child.getDbMapping()));
                            if (prop != null) {
                                home.set(cnst.localProperty(), prop.getValue(), prop.getType());
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Unset the constraints that link two objects together.
     */
    public void unsetConstraints(Node parent, NodeInterface child) {
        Node home = parent.getNonVirtualParent();

        for (int i = 0; i < this.constraints.length; i++) {
            Constraint cnst = this.constraints[i];
            // don't set groupby constraints since we don't know if the
            // parent node is the base node or a group node
            if (cnst.isGroupby) {
                continue;
            }

            // check if we update the local or the other object, depending on
            // whether the primary key of either side is used.

            if (cnst.foreignKeyIsPrimary() || cnst.foreignKeyIsPrototype()) {
                String localProp = cnst.localProperty();
                if (localProp != null) {
                    home.setString(localProp, null);
                }
                continue;
            }

            DbMapping otherDbm = child.getDbMapping();
            if (otherDbm == null) {
                otherDbm = this.otherType;
            }

            Relation crel = otherDbm.columnNameToRelation(cnst.foreignKey);

            if (crel != null) {
                if (cnst.localKeyIsPrimary(home.getDbMapping())) {
                    // only set node if property in child object is defined as reference.
                    if (crel.reftype == REFERENCE) {
                        NodeInterface currentValue = child.getNode(crel.propName);

                        if ((currentValue == home)) {
                            child.setString(crel.propName, null);
                        }
                    } else if (crel.reftype == PRIMITIVE) {
                        child.setString(crel.propName, null);
                    }
                } else if (crel.reftype == PRIMITIVE) {
                    child.setString(crel.propName, null);
                }
            }
        }
    }

    /**
     *  Returns a map containing the key/value pairs for a specific Node
     */
    public Map getKeyParts(NodeInterface home) {
        Map map = new HashMap();
        for (int i=0; i<this.constraints.length; i++) {
            Constraint cnst = this.constraints[i];
            if (cnst.localKeyIsPrimary(this.ownType)) {
                map.put(cnst.foreignKey, home.getID());
            } else if (cnst.localKeyIsPrototype()) {
                map.put(cnst.foreignKey, home.getDbMapping().getStorageTypeName());
            } else {
                map.put(cnst.foreignKey, home.getString(cnst.localProperty()));
            }
        }
        // add filter as pseudo-constraint
        if (this.filter != null) {
            map.put("__filter__", this.filter); //$NON-NLS-1$
        }
        return map;
    }

    /**
     *
     *
     * @return ...
     */
    @Override
    public String toString() {
        String c = ""; //$NON-NLS-1$
        String spacer = ""; //$NON-NLS-1$

        if (this.constraints != null) {
            c = " constraints: "; //$NON-NLS-1$
            for (int i = 0; i < this.constraints.length; i++) {
                c += spacer;
                c += this.constraints[i].toString();
                spacer = ", "; //$NON-NLS-1$
            }
        }

        String target = this.otherType == null ? this.columnName : this.otherType.toString();

        return "Relation " + this.ownType+"."+this.propName + " -> " + target + c; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
    }

    /**
     * The Constraint class represents a part of the where clause in the query used to
     * establish a relation between database mapped objects.
     */
    class Constraint {
        String localKey;
        String foreignKey;
        boolean isGroupby;

        Constraint(String local, String foreign, boolean groupby) {
            this.localKey = local;
            this.foreignKey = foreign;
            this.isGroupby = groupby;
        }

        /**
         * @throws SQLException
         * @throws NoDriverException if the JDBC driver could not be loaded or is unusable
         */
        public void addToQuery(StringBuffer q, NodeInterface home, NodeInterface nonvirtual, DbMapping otherDbm)
                        throws SQLException, NoDriverException {
            String local;
            NodeInterface ref = this.isGroupby ? home : nonvirtual;

            if (localKeyIsPrimary(ref.getDbMapping())) {
                local = ref.getID();
            } else if (localKeyIsPrototype()) {
                local = ref.getDbMapping().getStorageTypeName();
            } else {
                String homeprop = Relation.this.ownType.columnNameToProperty(this.localKey);
                if (homeprop == null) {
                    throw new SQLException(Messages.getString("Relation.18") + this.localKey + //$NON-NLS-1$
                            Messages.getString("Relation.19") + Relation.this.ownType); //$NON-NLS-1$
                }
                local = ref.getString(homeprop);
            }

            String columnName;
            if (foreignKeyIsPrimary()) {
                columnName = otherDbm.getIDField();
            } else {
                columnName = this.foreignKey;
            }
            otherDbm.appendCondition(q, columnName, local);
        }

        public boolean foreignKeyIsPrimary() {
            return (this.foreignKey == null) ||
                    "$id".equalsIgnoreCase(this.foreignKey) || //$NON-NLS-1$
                   this.foreignKey.equalsIgnoreCase(Relation.this.otherType.getIDField());
        }

        public boolean foreignKeyIsPrototype() {
            return "$prototype".equalsIgnoreCase(this.foreignKey); //$NON-NLS-1$
        }

        public boolean localKeyIsPrimary(DbMapping homeMapping) {
            return (homeMapping == null) || (this.localKey == null) ||
                   "$id".equalsIgnoreCase(this.localKey) || //$NON-NLS-1$
                   this.localKey.equalsIgnoreCase(homeMapping.getIDField());
        }

        public boolean localKeyIsPrototype() {
            return "$prototype".equalsIgnoreCase(this.localKey); //$NON-NLS-1$
        }

        public String foreignProperty(DbMapping otherDbm) {
            if (otherDbm.isRelational())
                return otherDbm.columnNameToProperty(this.foreignKey);
            return this.foreignKey;
        }

        public String localProperty() {
            if (Relation.this.ownType.isRelational())
                return Relation.this.ownType.columnNameToProperty(this.localKey);
            return this.localKey;
        }

        @Override
        public String toString() {
            return this.localKey + "=" + Relation.this.otherType.getTypeName() + "." + this.foreignKey; //$NON-NLS-1$ //$NON-NLS-2$
        }
    }
}
