/*
 * Helma License Notice
 *
 * The contents of this file are subject to the Helma License
 * Version 2.0 (the "License"). You may not use this file except in
 * compliance with the License. A copy of the License is available at
 * http://adele.helma.org/download/helma/license.txt
 *
 * Copyright 2010-2017 Daniel Ruthardt. All rights reserved.
 */

package helma.scripting.quercus;

import helma.framework.RedirectException;
import helma.framework.ResponseTrans;
import helma.framework.core.Skin;
import helma.scripting.ScriptingException;

import java.io.IOException;
import java.util.Map;

import com.caucho.quercus.annotation.Optional;
import com.caucho.quercus.env.ArrayValue;
import com.caucho.quercus.env.ArrayValueImpl;
import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.NullValue;
import com.caucho.quercus.env.ObjectExtJavaValue;
import com.caucho.quercus.env.StringValue;
import com.caucho.quercus.env.Value;
import com.caucho.quercus.module.AbstractQuercusModule;

/**
 * This class provides some of the global functions the JS engine is providing too.
 * The functions which are missing are not provided, as either equivalent functions or equivalent techniques 
 * exist in the PHP context.
 */
public class GlobalFunctions extends AbstractQuercusModule {

    /**
     * Default constructor
     */
    public GlobalFunctions() {
        // nothing to be done
    }

    /**
     * Looks up a property that was set in app.properties or server.properties.
     * Returns any property defined in [AppDir]/app.properties, resp. [HelmaHome]/server.properties that 
     * matches the passed property name. This lookup is case-insensitive. Through the second parameter it is 
     * possible to define a default value that is being returned, in case the property has not been set.
     * 
     * @param propertyName
     *  The name of the property to look up.
     * @param defaultValue
     *  The default/fallback value.
     * @return
     *  The resulting value for the checked property
     */
    public StringValue getProperty(final StringValue propertyName, 
            @Optional final StringValue defaultValue) {
        // return the property or the default value, should one be given
        return Env.getCurrent().createString(QuercusEngine.ENGINE.get().getApplication()
                .getProperty(propertyName.toJavaString(), defaultValue != null ? 
                        defaultValue.toJavaString() : null)).toStringValue();
    }

    /**
     * Renders the passed skin object to the response buffer.
     * The properties of the optional parameter object are accessible within the skin through the 'param' 
     * macro handler.
     * 
     * @param skin
     *  The skin object.
     * @param parameters
     *  Optional properties to be passed to the skin.
     */
    public void renderSkin(final Skin skin, @Optional final ArrayValueImpl parameters) {
        try {
            // render the skin
            skin.render(QuercusEngine.ENGINE.get().getRequestEvaluator(), null, parameters);
        } catch (final RedirectException e) {
            // ignored by intention
        }
    }

    /**
     * Renders the global skin matching the passed name to the response buffer.
     * The properties of the optional parameter object are accessible within the skin through the 'param' 
     * macro handler.
     * 
     * @param skin
     *  The name of the skin.
     * @param parameters
     *  Optional properties to be passed to the skin.
     */
    public Value renderSkin(final String name, @Optional final ArrayValueImpl parameters) {
        try {
            // delegate
            this.renderSkin(QuercusEngine.ENGINE.get().getSkin("Global", name), parameters); //$NON-NLS-1$
        } catch (ScriptingException e) {
            // return the exception as PHP error
            return Env.getCurrent().error(e);
        }

        // nothing to return
        return NullValue.NULL;
    }

    /**
     * Returns the result of the rendered skin object.
     * The properties of the optional parameter object are accessible within the skin through the 'param' 
     * macro handler.
     * 
     * @param skin
     *  The skin object.
     * @param parameters
     *  Optional properties to be passed to the skin.
     * @return
     *  The rendered skin.
     */
    public StringValue renderSkinAsString(final Skin skin, @Optional final ArrayValueImpl parameters) {
        try {
            // return the rendered skin
            return Env.getCurrent().createString(skin.renderAsString(QuercusEngine.ENGINE.get()
                    .getRequestEvaluator(), null, parameters));
        } catch (final RedirectException e) {
            // ignored by intention
        }

        // rendering failed, nothing to return
        return StringValue.EMPTY;
    }

    /**
     * Returns the result of a rendered global skin matching the passed name.
     * The properties of the optional parameter object are accessible within the skin through the 'param' 
     * macro handler.
     * 
     * @param skin
     *  The name of the skin.
     * @param parameters
     *  Optional properties to be passed to the skin.
     * @return
     *  The rendered skin.
     */
    public Value renderSkinAsString(final String name, @Optional final ArrayValueImpl parameters) { 
        try {
            // delegate
            return this.renderSkinAsString(QuercusEngine.ENGINE.get().getSkin("Global", name), parameters); //$NON-NLS-1$
        } catch (ScriptingException e) {
            // return the exception as PHP error
            return Env.getCurrent().error(e);
        }
    }

}