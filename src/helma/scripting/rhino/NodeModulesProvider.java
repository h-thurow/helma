/*
 * Helma License Notice
 *
 * The contents of this file are subject to the Helma License
 * Version 2.0 (the "License"). You may not use this file except in
 * compliance with the License. A copy of the License is available at
 * http://adele.helma.org/download/helma/license.txt
 *
 * Copyright 2017 Daniel Ruthardt. All rights reserved.
 */

package helma.scripting.rhino;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;

import org.mozilla.javascript.commonjs.module.provider.ModuleSource;
import org.mozilla.javascript.commonjs.module.provider.UrlModuleSourceProvider;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * Bridges the gap between CommonJS-style module loading and NodeJS-style module loading.
 */
public class NodeModulesProvider extends UrlModuleSourceProvider {

    /**
     * Define the serialization UID.
     */
    private static final long serialVersionUID = 6858072487233136717L;

    /**
     * Delegates to the super constructor.
     */
    public NodeModulesProvider(Iterable<URI> privilegedUris, Iterable<URI> fallbackUris) {
        // do what would have been done anyways
        super(privilegedUris, fallbackUris);
    }
    
    @Override
    protected ModuleSource loadFromUri(URI uri, URI base, Object validator) throws IOException, URISyntaxException {
        // we assume the module is a directory
        File directory = new File(uri);
        // check if the module is an existing directory
        if (directory.exists() && directory.isDirectory()) {
            // we assume that there is a "package.json" file in the directory
            File packageFile = new File(directory, "package.json"); //$NON-NLS-1$
            
            // the default JS file for NodeJS modules is "index"
            String main = "index"; //$NON-NLS-1$
            // check if the module has a "package.json" file
            if (packageFile.exists() && packageFile.isFile()) {
                // parse the JSON file
                JsonObject json = new JsonParser()
                        .parse(new String(Files.readAllBytes(packageFile.toPath()))).getAsJsonObject();
                // get the main JS file's name from the JSON file
                main = json.has("main") ? json.get("main").getAsString() : main;  //$NON-NLS-1$ //$NON-NLS-2$
            }

            // rewrite the uri pointing to the module's main JS file rather than pointing to the module's
            // directory
            uri = URI.create(uri.toString() + "/" + main); //$NON-NLS-1$
        }

        // do what would have been done anyways
        return super.loadFromUri(uri, base, validator);
    }

}