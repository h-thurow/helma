/*
 * Helma License Notice
 *
 * The contents of this file are subject to the Helma License
 * Version 2.0 (the "License"). You may not use this file except in
 * compliance with the License. A copy of the License is available at
 * http://adele.helma.org/download/helma/license.txt
 *
 * Copyright 2010 dowee it solutions GmbH. All rights reserved.
 */

package helma.scripting.quercus;

import helma.framework.ResponseTrans;

import java.io.IOException;
import com.caucho.quercus.env.ObjectExtJavaValue;
import com.caucho.quercus.env.StringValue;
import com.caucho.quercus.module.AbstractQuercusModule;

/**
 * This class provides some of the global functions the JS engine is providing
 * too. The functions which are missing are not provided, as either equivalent
 * functions or equivalent techniques exist in the PHP context.
 */
public class GlobalFunctions extends AbstractQuercusModule {

    /**
     * Default constructor
     */
    public GlobalFunctions() {
        // no initialization needed
    }

    /**
     * Get a Helma database connection specified in db.properties
     * 
     * @param name
     *            The name of the database connection to get
     * @return a DatabaseObject for the specified DbConnection
     */
    public ObjectExtJavaValue getDBConnection(
            final StringValue name) {
        // TODO: implement
        QuercusEngine.ENGINE.get().getEnvironment().error(
                Messages.getString("GlobalFunctions.0")); //$NON-NLS-1$
        return null;
    }

    /**
     * Resturns a global property specified in app.properties
     * 
     * @param name
     *            The name of the property to get
     * @param defaultValue
     *            The default value to return, if the property is not specified
     * @return The value of the property or the provided default property, if
     *         the property to get is unspecified
     */
    public StringValue getProperty(final StringValue name,
            final StringValue defaultValue) {
        return StringValue.create(
                QuercusEngine.ENGINE.get().getApplication().getProperty(
                        name.toJavaString(), defaultValue.toJavaString()))
                .toStringValue();
    }

    /**
     * Returns the appropriate skin for the given name.<br/>
     * The returned skin might be a subskin, a cached skin or a newly loaded
     * skin.
     * 
     * @param name
     *            The name of the skin to get
     * @return A skin with the given name or null, if no skin by the given name
     *         could be found
     * @throws ScriptingException
     */
    private helma.framework.core.Skin getSkin(final String name)
            throws helma.scripting.ScriptingException {
        final QuercusEngine engine = QuercusEngine.ENGINE.get();
        helma.framework.core.Skin skin;
        final ResponseTrans response = engine.getRequestEvaluator()
                .getResponse();
        if (name.startsWith("#")) { //$NON-NLS-1$
            // evaluate relative subskin name against currently rendering skin
            skin = response.getActiveSkin();
            return skin == null ? null : skin.getSubskin(name.substring(1));
        }

        final Integer hashCode = Integer.valueOf("Global".hashCode() //$NON-NLS-1$
                + name.hashCode());
        skin = response.getCachedSkin(hashCode);

        if (skin == null) {
            // retrieve res.skinpath, an array of objects that tell us where to
            // look for skins
            // (strings for directory names and INodes for internal, db-stored
            // skinsets)
            final Object[] skinpath = response.getSkinpath();
            try {
                skin = engine.getApplication()
                        .getSkin("Global", name, skinpath); //$NON-NLS-1$
            } catch (final IOException e) {
                throw new helma.scripting.ScriptingException(
                        Messages.getString("GlobalFunctions.1"), //$NON-NLS-1$
                        e);
            }
            response.cacheSkin(hashCode, skin);
        }
        return skin;
    }

    /**
     * Renders a global skin by the given name providing the given parameters
     * 
     * @param name
     *            The name of the global skin to render
     * @param parameters
     *            The parameters to provide to the skin
     */
    public void renderSkin(final StringValue name,
            final ObjectExtJavaValue parameters) {
        final QuercusEngine engine = QuercusEngine.ENGINE.get();

        if (parameters != null
                && !(parameters.toJavaObject() instanceof HopObject)) {
            engine.getEnvironment().error(
                    Messages.getString("GlobalFunctions.2")); //$NON-NLS-1$
        }

        helma.framework.core.Skin skin;
        try {
            skin = getSkin(name.toJavaString());
        } catch (final helma.scripting.ScriptingException e) {
            engine.getEnvironment().error(e);
            return;
        }

        if (skin != null) {
            try {
                skin.render(engine.getRequestEvaluator(), null, parameters);
            } catch (final helma.framework.RedirectException e) {
                // ignored by intention
            }
        }
    }

    /**
     * Returns the result of rendering a global skin by the given name providing
     * the given parameters
     * 
     * @param name
     *            The name of the global skin to render
     * @param parameters
     *            The parameters to provide to the skin
     * @return The result of rendering the skin
     */
    public StringValue renderSkinAsString(final StringValue name,
            final ObjectExtJavaValue parameters) {
        final QuercusEngine engine = QuercusEngine.ENGINE.get();

        if (parameters != null
                && !(parameters.toJavaObject() instanceof HopObject)) {
            engine.getEnvironment().error(
                    Messages.getString("GlobalFunctions.3")); //$NON-NLS-1$
        }

        helma.framework.core.Skin skin;
        try {
            skin = getSkin(name.toJavaString());
        } catch (final helma.scripting.ScriptingException e) {
            engine.getEnvironment().error(e);
            return StringValue.EMPTY;
        }

        if (skin != null) {
            try {
                return StringValue.create(
                        skin.renderAsString(engine.getRequestEvaluator(),
                                parameters, skin)).toStringValue();
            } catch (final helma.framework.RedirectException e) {
                // ignored by intention
            }
        }

        return StringValue.EMPTY;
    }

    /**
     * Writes the given string to System.out
     * 
     * @param string String to write to System.out
     */
    public void write(final StringValue string) {
        System.out.print(string.toJavaString());
    }

    /**
     * Writes the string extended with a platform specific newline to System.out
     * 
     * @param string String to write to System.out with newline
     */
    public void writeln(final StringValue string) {
        System.out.println(string.toJavaString());
    }

}