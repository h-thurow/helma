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

package helma.framework.core;

import helma.framework.*;
import helma.framework.repository.Resource;
import helma.objectmodel.ConcurrencyException;
import helma.util.*;
import helma.scripting.ScriptingEngine;

import java.util.*;
import java.io.UnsupportedEncodingException;
import java.io.Reader;
import java.io.InputStreamReader;
import java.io.IOException;

/**
 * This represents a Helma skin, i.e. a template created from containing Macro tags
 * that will be dynamically evaluated.. It uses the request path array
 * from the RequestEvaluator object to resolve Macro handlers by type name.
 */
public final class Skin {

    private Macro[] macros;
    private Application app;
    private char[] source;
    private int offset, length; // start and end index of skin content
    private HashSet sandbox;
    private HashMap subskins;
    private Skin parentSkin = this;
    private String extendz = null;
    private boolean hasContent = false;

    static private final int PARSE_MACRONAME = 0;
    static private final int PARSE_PARAM = 1;
    static private final int PARSE_DONE = 2;

    static private final int ENCODE_NONE = 0;
    static private final int ENCODE_HTML = 1;
    static private final int ENCODE_XML = 2;
    static private final int ENCODE_FORM = 3;
    static private final int ENCODE_URL = 4;
    static private final int ENCODE_ALL = 5;

    static private final int HANDLER_RESPONSE = 0;
    static private final int HANDLER_REQUEST = 1;
    static private final int HANDLER_SESSION = 2;
    static private final int HANDLER_PARAM = 3;
    static private final int HANDLER_GLOBAL = 4;
    static private final int HANDLER_THIS = 5;
    static private final int HANDLER_OTHER = 6;

    static private final int FAIL_DEFAULT = 0;
    static private final int FAIL_SILENT = 1;
    static private final int FAIL_VERBOSE = 2;

    /**
     * Create a skin without any restrictions on which macros are allowed to be called from it
     */
    public Skin(String content, Application app) {
        this.app = app;
        this.sandbox = null;
        this.source = content.toCharArray();
        this.offset = 0;
        this.length = source.length;
        parse();
    }

    /**
     * Create a skin with a sandbox which contains the names of macros allowed to be called
     */
    public Skin(String content, Application app, HashSet sandbox) {
        this.app = app;
        this.sandbox = sandbox;
        this.source = content.toCharArray();
        this.offset = 0;
        length = source.length;
        parse();
    }

    /**
     *  Create a skin without any restrictions on the macros from a char array.
     */
    public Skin(char[] content, int length, Application app) {
        this.app = app;
        this.sandbox = null;
        this.source = content;
        this.offset = 0;
        this.length = length;
        parse();
    }

    /**
     *  Subskin constructor.
     */
    private Skin(Skin parentSkin, Macro anchorMacro) {
        this.parentSkin = parentSkin;
        this.app = parentSkin.app;
        this.sandbox = parentSkin.sandbox;
        this.source = parentSkin.source;
        this.offset = anchorMacro.end;
        this.length = parentSkin.length;
        parentSkin.addSubskin(anchorMacro.name, this);
        parse();
    }

    public static Skin getSkin(Resource res, Application app) throws IOException {
        String encoding = app.getProperty("skinCharset"); //$NON-NLS-1$
        Reader reader;
        if (encoding == null) {
            reader = new InputStreamReader(res.getInputStream());
        } else {
            reader = new InputStreamReader(res.getInputStream(), encoding);
        }

        int length = (int) res.getLength();
        char[] characterBuffer = new char[length];
        int read = 0;
        try {
            while (read < length) {
                int r = reader.read(characterBuffer, read, length - read);
                if (r == -1)
                    break;
                read += r;
            }
        } finally {
            reader.close();
        }
        return new Skin(characterBuffer, read, app);
    }

    /**
     * Parse a skin object from source text
     */
    private void parse() {
        ArrayList partBuffer = new ArrayList();

        boolean escape = false;
        for (int i = offset; i < (length - 1); i++) {
            if (source[i] == '<' && source[i + 1] == '%' && !escape) {
                // found macro start tag
                Macro macro = new Macro(i, 2);
                if (macro.isSubskinMacro) {
                    new Skin(parentSkin, macro);
                    length = i;
                    break;
                } else {
                    if (!macro.isCommentMacro) {
                        hasContent = true;
                    }
                    partBuffer.add(macro);
                }
                i = macro.end - 1;
            } else {
                if (!hasContent && !Character.isWhitespace(source[i])){
                    hasContent = true;
                }
                escape = source[i] == '\\' && !escape;
            }
        }

        macros = new Macro[partBuffer.size()];
        partBuffer.toArray(macros);
    }

    private void addSubskin(String name, Skin subskin) {
        if (subskins == null) {
            subskins = new HashMap();
        }
        subskins.put(name, subskin);
    }
    
    /**
     * Return the list of macros found by the parser
     * @return the list of macros
     */
    public Macro[] getMacros() {
    	return macros;
    }

    /**
     * Check if this skin has a main skin, as opposed to consisting just of subskins
     * @return true if this skin contains a main skin
     */
    public boolean hasMainskin() {
        return hasContent;
    }

    /**
     * Check if this skin contains a subskin with the given name
     * @param name a subskin name
     * @return true if the given subskin exists
     */
    public boolean hasSubskin(String name) {
        return subskins != null && subskins.containsKey(name);
    }

    /**
     * Get a subskin by name
     * @param name the subskin name
     * @return the subskin
     */
    public Skin getSubskin(String name) {
        return subskins == null ? null : (Skin) subskins.get(name);
    }

    /**
     * Return an array of subskin names defined in this skin
     * @return a string array containing this skin's substrings
     */
    public String[] getSubskinNames() {
        return subskins == null ?
                new String[0] :
                (String[]) subskins.keySet().toArray(new String[0]);
    }

    public String getExtends() {
        return extendz;
    }

    /**
     * Get the raw source text this skin was parsed from
     */
    public String getSource() {
        return new String(source, offset, length - offset);
    }

    /**
     * Render this skin and return it as string
     */
    public String renderAsString(RequestEvaluator reval, Object thisObject, Object paramObject)
                throws RedirectException, UnsupportedEncodingException {
        String result = ""; //$NON-NLS-1$
        ResponseTrans res = reval.getResponse();
        res.pushBuffer(null);
        try {
            render(reval, thisObject, paramObject);
        } finally {
            result = res.popString();
        }
        return result;
    }


    /**
     * Render this skin
     */
    public void render(RequestEvaluator reval, Object thisObject, Object paramObject)
                throws RedirectException, UnsupportedEncodingException {
        // check for endless skin recursion
        if (++reval.skinDepth > 50) {
            throw new RuntimeException(Messages.getString("Skin.0")); //$NON-NLS-1$
        }

        ResponseTrans res = reval.getResponse();

        if (macros == null) {
            res.write(source, offset, length - offset);
            reval.skinDepth--;
            return;
        }

        // register param object, remember previous one to reset afterwards
        Map handlers = res.getMacroHandlers();
        Object previousParam = handlers.put("param", paramObject); //$NON-NLS-1$
        Skin previousSkin = res.switchActiveSkin(parentSkin);

        try {
            int written = offset;
            Map handlerCache = null;

            if (macros.length > 3) {
                handlerCache = new HashMap();
            }
            RenderContext cx = new RenderContext(reval, thisObject, handlerCache);

            for (int i = 0; i < macros.length; i++) {
                if (macros[i].start > written) {
                    res.write(source, written, macros[i].start - written);
                }

                macros[i].render(cx);
                written = macros[i].end;
            }

            if (written < length) {
                res.write(source, written, length - written);
            }
        } finally {
            reval.skinDepth--;
            res.switchActiveSkin(previousSkin);
            if (previousParam == null) {
                handlers.remove("param"); //$NON-NLS-1$
            } else {
                handlers.put("param", previousParam); //$NON-NLS-1$
            }
        }
    }

    /**
     * Check if a certain macro is present in this skin. The macro name is in handler.name notation
     */
    public boolean containsMacro(String macroname) {
        for (int i = 0; i < macros.length; i++) {
            if (macros[i] instanceof Macro) {
                Macro m = macros[i];

                if (macroname.equals(m.name)) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     *  Adds a macro to the list of allowed macros. The macro is in handler.name notation.
     */
    public void allowMacro(String macroname) {
        if (sandbox == null) {
            sandbox = new HashSet();
        }

        sandbox.add(macroname);
    }

    private Object processParameter(Object value, RenderContext cx)
            throws Exception {
        if (value instanceof Macro) {
            return ((Macro) value).invokeAsParameter(cx);
        } else {
            return value;
        }
    }

    public class Macro {
        public final int start, end;
        String name;
        String[] path;
        int handlerType = HANDLER_OTHER;
        int encoding = ENCODE_NONE;
        boolean hasNestedMacros = false;

        // default render parameters - may be overridden if macro changes
        // param.prefix/suffix/default
        StandardParams standardParams = new StandardParams();
        Map namedParams = null;
        List positionalParams = null;
        // filters defined via <% foo | bar %>
        Macro filterChain;

        // comment macros are silently dropped during rendering
        boolean isCommentMacro = false;
        // subskin macros delimits the beginning of a new subskin
        boolean isSubskinMacro = false;

        /**
         * Create and parse a new macro.
         * @param start the start of the macro within the skin source
         * @param macroOffset offset of the macro content from the start index
         */
        Macro(int start, int macroOffset) {
            this.start = start;

            int i = parse(macroOffset, false);

            if (isSubskinMacro) {
                if (i + 1 < length && source[i] == '\r' && source[i + 1] == '\n')
                    end = Math.min(length, i + 2);
                else if (i < length && (source[i] == '\r' || source[i] == '\n'))
                    end = Math.min(length, i + 1);
                else
                    end = Math.min(length, i);
            } else {
                end = Math.min(length, i);
            }

            path = StringUtils.split(name, "."); //$NON-NLS-1$
            if (path.length <= 1) {
                handlerType = HANDLER_GLOBAL;
            } else {
                String handlerName = path[0];
                if ("this".equalsIgnoreCase(handlerName)) { //$NON-NLS-1$
                    handlerType = HANDLER_THIS;
                } else if ("response".equalsIgnoreCase(handlerName)) { //$NON-NLS-1$
                    handlerType = HANDLER_RESPONSE;
                } else if ("request".equalsIgnoreCase(handlerName)) { //$NON-NLS-1$
                    handlerType = HANDLER_REQUEST;
                } else if ("session".equalsIgnoreCase(handlerName)) { //$NON-NLS-1$
                    handlerType = HANDLER_SESSION;
                } else if ("param".equalsIgnoreCase(handlerName)) { //$NON-NLS-1$
                    handlerType = HANDLER_PARAM;
                }
            }

            if (".extends".equals(name)) { //$NON-NLS-1$
                if (parentSkin != Skin.this) {
                    throw new RuntimeException(Messages.getString("Skin.1")); //$NON-NLS-1$
                }
                if (positionalParams == null || positionalParams.size() < 1
                        || !(positionalParams.get(0) instanceof String)) {
                    throw new RuntimeException(Messages.getString("Skin.2")); //$NON-NLS-1$
                }
                extendz = (String) positionalParams.get(0);
                isCommentMacro = true; // don't render
            }
        }

        private int parse(int macroOffset, boolean lenient) {
            int state = PARSE_MACRONAME;
            boolean escape = false;
            char quotechar = '\u0000';
            String lastParamName = null;
            StringBuffer b = new StringBuffer();
            int i;

            loop:
            for (i = start + macroOffset; i < length - 1; i++) {

                switch (source[i]) {

                    case '<':

                        if (state == PARSE_PARAM && quotechar == '\u0000'
                                && b.length() == 0 && source[i + 1] == '%') {
                            Macro macro = new Macro(i, 2);
                            addParameter(lastParamName, macro);
                            lastParamName = null;
                            b.setLength(0);
                            i = macro.end - 1;
                        } else {
                            b.append(source[i]);
                            escape = false;
                        }
                        break;

                    case '%':

                        if ((state != PARSE_PARAM || quotechar == '\u0000' || lenient)
                                && source[i + 1] == '>') {
                            state = PARSE_DONE;
                            break loop;
                        }
                        b.append(source[i]);
                        escape = false;
                        break;

                    case '/':

                        b.append(source[i]);
                        escape = false;

                        if (state == PARSE_MACRONAME && "//".equals(b.toString())) { //$NON-NLS-1$
                            isCommentMacro = true;
                            // just continue parsing the macro as this is the only way
                            // to correctly catch embedded macros - see bug 588
                        }
                        break;

                    case '#':

                        if (state == PARSE_MACRONAME && b.length() == 0) {
                            // this is a subskin/skinlet
                            isSubskinMacro = true;
                            break;
                        }
                        b.append(source[i]);
                        escape = false;
                        break;

                    case '|':

                        if (!escape && quotechar == '\u0000') {
                            filterChain = new Macro(i, 1);
                            i = filterChain.end - 2;
                            lastParamName = null;
                            b.setLength(0);
                            state = PARSE_DONE;
                            break loop;
                        }
                        b.append(source[i]);
                        escape = false;
                        break;

                    case '\\':

                        if (escape) {
                            b.append(source[i]);
                        }

                        escape = !escape;

                        break;

                    case '"':
                    case '\'':

                        if (!escape && state == PARSE_PARAM) {
                            if (quotechar == source[i]) {
                                if (source[i + 1] != '%' && !Character.isWhitespace(source[i + 1]) && !lenient) {
                                    // closing quotes and next character is not space or end tag -
                                    // switch to lenient mode
                                    reset();
                                    return parse(macroOffset, true);
                                }
                                // add parameter
                                addParameter(lastParamName, b.toString());
                                lastParamName = null;
                                b.setLength(0);
                                quotechar = '\u0000';
                            } else if (quotechar == '\u0000') {
                                quotechar = source[i];
                                b.setLength(0);
                            } else {
                                b.append(source[i]);
                            }
                        } else {
                            b.append(source[i]);
                        }

                        escape = false;

                        break;

                    case ' ':
                    case '\t':
                    case '\n':
                    case '\r':
                    case '\f':

                        if (state == PARSE_MACRONAME && b.length() > 0) {
                            name = b.toString().trim();
                            b.setLength(0);
                            state = PARSE_PARAM;
                        } else if (state == PARSE_PARAM) {
                            if (quotechar == '\u0000') {
                                if (b.length() > 0) {
                                    // add parameter
                                    addParameter(lastParamName, b.toString());
                                    lastParamName = null;
                                    b.setLength(0);
                                }
                            } else {
                                b.append(source[i]);
                                escape = false;
                            }
                        }

                        break;

                    case '=':

                        if (!escape && quotechar == '\u0000' && state == PARSE_PARAM && lastParamName == null) {
                            lastParamName = b.toString().trim();
                            b.setLength(0);
                        } else {
                            b.append(source[i]);
                            escape = false;
                        }

                        break;

                    default:
                        b.append(source[i]);
                        escape = false;
                }

                if (i == length - 2 && !lenient &&
                        (state != PARSE_DONE ||quotechar != '\u0000')) {
                    // macro tag is not properly terminated, switch to lenient mode                    
                    reset();
                    return parse(macroOffset, true);
                }
            }

            if (b.length() > 0) {
                if (name == null) {
                    name = b.toString().trim();
                } else {
                    addParameter(lastParamName, b.toString());
                }
            }

            if (state != PARSE_DONE) {
                app.logError(Messages.getString("Skin.3") +this); //$NON-NLS-1$
            }

            return i + 2;
        }

        private void reset() {
            filterChain = null;
            name = null;
            standardParams = new StandardParams();
            namedParams = null;
            positionalParams = null;
        }

        private void addParameter(String name, Object value) {
            if (!(value instanceof String)) {
                hasNestedMacros = true;                
            }
            if (name == null) {
                // take shortcut for positional parameters
                if (positionalParams == null) {
                    positionalParams = new ArrayList();
                }
                positionalParams.add(value);
                return;
            }
            // check if this is parameter is relevant to us
            if ("prefix".equals(name)) { //$NON-NLS-1$
                standardParams.prefix = value;
            } else if ("suffix".equals(name)) { //$NON-NLS-1$
                standardParams.suffix = value;
            } else if ("encoding".equals(name)) { //$NON-NLS-1$
                if ("html".equals(value)) { //$NON-NLS-1$
                    encoding = ENCODE_HTML;
                } else if ("xml".equals(value)) { //$NON-NLS-1$
                    encoding = ENCODE_XML;
                } else if ("form".equals(value)) { //$NON-NLS-1$
                    encoding = ENCODE_FORM;
                } else if ("url".equals(value)) { //$NON-NLS-1$
                    encoding = ENCODE_URL;
                } else if ("all".equals(value)) { //$NON-NLS-1$
                    encoding = ENCODE_ALL;
                } else {
                    app.logEvent(Messages.getString("Skin.4") + value); //$NON-NLS-1$
                }
            } else if ("default".equals(name)) { //$NON-NLS-1$
                standardParams.defaultValue = value;
            } else if ("failmode".equals(name)) { //$NON-NLS-1$
                standardParams.setFailMode(value);
            }

            // Add parameter to parameter map
            if (namedParams == null) {
                namedParams = new HashMap();
            }
            namedParams.put(name, value);
        }

        private Object invokeAsMacro(RenderContext cx, StandardParams stdParams, boolean asObject)
                throws Exception {

            // immediately return for comment macros
            if (isCommentMacro || name == null) {
                return null;
            }

            if ((sandbox != null) && !sandbox.contains(name)) {
                throw new MacroException(Messages.getString("Skin.5") + name); //$NON-NLS-1$
            }

            Object handler = null;
            Object value = null;
            ScriptingEngine engine = cx.reval.scriptingEngine;

            if (handlerType != HANDLER_GLOBAL) {
                handler = cx.resolveHandler(path[0], handlerType);
                handler = resolvePath(handler, cx.reval);
            }

            if (handlerType == HANDLER_GLOBAL || handler != null) {
                // check if a function called name_macro is defined.
                // if so, the macro evaluates to the function. Otherwise,
                // a property/field with the name is used, if defined.
                String propName = path[path.length - 1];
                String funcName = resolveFunctionName(handler, propName + "_macro", engine); //$NON-NLS-1$

                // remember length of response buffer before calling macro
                StringBuffer buffer = cx.reval.getResponse().getBuffer();
                int bufLength = buffer.length();

                if (funcName != null) {

                    Object[] arguments = prepareArguments(0, cx);
                    // get reference to rendered named params for after invocation
                    Map params = (Map) arguments[0];
                    value = cx.reval.invokeDirectFunction(handler,
                            funcName,
                            arguments);

                    // update StandardParams to override defaults in case the macro changed anything
                    if (stdParams != null) stdParams.readFrom(params);

                    // if macro has a filter chain and didn't return anything, use output
                    // as filter argument.
                    if (asObject && value == null && buffer.length() > bufLength) {
                        value = buffer.substring(bufLength);
                        buffer.setLength(bufLength);
                    }

                    return filter(value, cx);
                } else {
                    if (handlerType == HANDLER_RESPONSE) {
                        // some special handling for response handler
                        if ("message".equals(propName)) //$NON-NLS-1$
                            value = cx.reval.getResponse().getMessage();
                        else if ("error".equals(propName)) //$NON-NLS-1$
                            value = cx.reval.getResponse().getErrorMessage();
                        if (value != null)
                            return filter(value, cx);
                    }
                    // display error message unless onUnhandledMacro is defined or silent failmode is on
                    if (!engine.hasProperty(handler, propName)) {
                        if (engine.hasFunction(handler, "onUnhandledMacro", false)) { //$NON-NLS-1$
                            Object[] arguments = prepareArguments(1, cx);
                            arguments[0] = propName;
                            value = cx.reval.invokeDirectFunction(handler,  "onUnhandledMacro", arguments); //$NON-NLS-1$
                            // if macro has a filter chain and didn't return anything, use output
                            // as filter argument.
                            if (asObject && value == null && buffer.length() > bufLength) {
                                value = buffer.substring(bufLength);
                                buffer.setLength(bufLength);
                            }
                        } else if (standardParams.verboseFailmode(handler, engine)) {
                            throw new MacroException(Messages.getString("Skin.6") + name); //$NON-NLS-1$
                        }
                    } else {
                        value = engine.getProperty(handler, propName);
                    }
                    return filter(value, cx);
                }
            } else if (standardParams.verboseFailmode(handler, engine)) {
                throw new MacroException(Messages.getString("Skin.7") + name); //$NON-NLS-1$
            }
            return filter(null, cx);
        }

        /**
         * Render this macro as nested macro, only converting to string
         * if necessary.
         */
        Object invokeAsParameter(RenderContext cx) throws Exception {
            StandardParams stdParams = standardParams.render(cx);            
            Object value = invokeAsMacro(cx, stdParams, true);
            if (stdParams.prefix != null || stdParams.suffix != null) {
                ResponseTrans res = cx.reval.getResponse();
                res.pushBuffer(null);
                writeResponse(value, cx.reval, stdParams, true);
                return res.popString();
            } else if (stdParams.defaultValue != null &&
                    (value == null || "".equals(value))) { //$NON-NLS-1$
                return stdParams.defaultValue;
            } else {
                return value;
            }
        }

        /**
         *  Render the macro given a handler object.
         */
        void render(RenderContext cx)
                throws RedirectException, UnsupportedEncodingException {
            StringBuffer buffer = cx.reval.getResponse().getBuffer();
            // remember length of response buffer before calling macro
            int bufLength = buffer.length();
            try {
                StandardParams stdParams = standardParams.render(cx);
                boolean asObject = filterChain != null;
                Object value = invokeAsMacro(cx, stdParams, asObject);

                // check if macro wrote out to response buffer
                if (buffer.length() == bufLength) {
                    // If the macro function didn't write anything to the response itself,
                    // we interpret its return value as macro output.
                    writeResponse(value, cx.reval, stdParams, true);
                } else {
                    // if an encoding is specified, re-encode the macro's output
                    if (encoding != ENCODE_NONE) {
                        String output = buffer.substring(bufLength);

                        buffer.setLength(bufLength);
                        writeResponse(output, cx.reval, stdParams, false);
                    } else {
                        // insert prefix,
                        if (stdParams.prefix != null) {
                            buffer.insert(bufLength, stdParams.prefix);
                        }
                        // append suffix
                        if (stdParams.suffix != null) {
                            buffer.append(stdParams.suffix);
                        }
                    }

                    // Append macro return value even if it wrote something to the response,
                    // but don't render default value in case it returned nothing.
                    // We do this for the sake of consistency.
                    writeResponse(value, cx.reval, stdParams, false);
                }

            } catch (RedirectException redir) {
                throw redir;
            } catch (ConcurrencyException concur) {
                throw concur;
            } catch (TimeoutException timeout) {
                throw timeout;
            } catch (MacroException mx) {
                String msg = mx.getMessage();
                cx.reval.getResponse().write(" [" + msg + "] "); //$NON-NLS-1$ //$NON-NLS-2$
                app.logError(msg);
            } catch (Exception x) {
                String msg = x.getMessage();
                if ((msg == null) || (msg.length() < 10)) {
                    msg = x.toString();
                }
                msg = new StringBuffer(Messages.getString("Skin.8")).append(name) //$NON-NLS-1$
                        .append(": ").append(msg).toString(); //$NON-NLS-1$
                cx.reval.getResponse().write(" [" + msg + "] ");  //$NON-NLS-1$//$NON-NLS-2$
                app.logError(msg, x);
            }
        }

        private Object filter(Object returnValue, RenderContext cx)
                throws Exception {
            // invoke filter chain if defined
            if (filterChain != null) {
                return filterChain.invokeAsFilter(returnValue, cx);
            } else {
                return returnValue;
            }
        }

        private Object invokeAsFilter(Object returnValue, RenderContext cx)
                throws Exception {

            if (name == null) {
                throw new MacroException(Messages.getString("Skin.9")); //$NON-NLS-1$
            } else if (sandbox != null && !sandbox.contains(name)) {
                throw new MacroException(Messages.getString("Skin.10") + name); //$NON-NLS-1$
            }
            Object handlerObject = null;

            if (handlerType != HANDLER_GLOBAL) {
                handlerObject = cx.resolveHandler(path[0], handlerType);
                handlerObject = resolvePath(handlerObject, cx.reval);
            }

            String propName = path[path.length - 1] + "_filter"; //$NON-NLS-1$
            String funcName = resolveFunctionName(handlerObject, propName,
                    cx.reval.scriptingEngine);

            if (funcName != null) {
                Object[] arguments = prepareArguments(1, cx);
                arguments[0] = returnValue;
                Object retval = cx.reval.invokeDirectFunction(handlerObject,
                                                           funcName,
                                                           arguments);

                return filter(retval, cx);
            } else {
                throw new MacroException(Messages.getString("Skin.11") + name); //$NON-NLS-1$
            }
        }

        private Object[] prepareArguments(int offset, RenderContext cx)
                throws Exception {
            int nPosArgs = (positionalParams == null) ? 0 : positionalParams.size();
            Object[] arguments = new Object[offset + 1 + nPosArgs];

            if (namedParams == null) {
                arguments[offset] = new SystemMap(4);
            } else if (hasNestedMacros) {
                SystemMap map = new SystemMap((int) (namedParams.size() * 1.5));
                for (Iterator it = namedParams.entrySet().iterator(); it.hasNext(); ) {
                    Map.Entry entry = (Map.Entry) it.next();
                    Object value = entry.getValue();
                    if (!(value instanceof String))
                        value = processParameter(value, cx);
                    map.put(entry.getKey(), value);
                }
                arguments[offset] = map;
            } else {
                // pass a clone/copy of the parameter map so if the script changes it,
                arguments[offset] = new CopyOnWriteMap(namedParams);
            }
            if (positionalParams != null) {
                for (int i = 0; i < nPosArgs; i++) {
                    Object value = positionalParams.get(i);
                    if (!(value instanceof String))
                        value = processParameter(value, cx);
                    arguments[offset + 1 + i] = value;
                }
            }
            return arguments;
        }

        private Object resolvePath(Object handler, RequestEvaluator reval) throws Exception {
            for (int i = 1; i < path.length - 1; i++) {
                Object[] arguments = {path[i]};
                Object next = reval.invokeDirectFunction(handler, "getMacroHandler", arguments); //$NON-NLS-1$
                if (next != null) {
                    handler = next;
                } else if (!reval.scriptingEngine.isTypedObject(handler)) {
                    handler = reval.scriptingEngine.getProperty(handler, path[i]);
                    if (handler == null) {
                        return null;
                    }
                } else {
                    return null;
                }
            }
            return handler;
        }

        private String resolveFunctionName(Object handler, String functionName,
                                           ScriptingEngine engine) {
            if (handlerType == HANDLER_GLOBAL) {
                String[] macroPath = app.globalMacroPath;
                if (macroPath == null || macroPath.length == 0) {
                    if (engine.hasFunction(null, functionName, false))
                        return functionName;
                } else {
                    for (int i = 0; i < macroPath.length; i++) {
                        String path = macroPath[i];
                        String funcName = path == null || path.length() == 0 ?
                                functionName : path + "." + functionName; //$NON-NLS-1$
                        if (engine.hasFunction(null, funcName, true))
                            return funcName;
                    }
                }
            } else {
                if (engine.hasFunction(handler, functionName, false))
                    return functionName;
            }
            return null;
        }

        /**
         * Utility method for writing text out to the response object.
         */
        void writeResponse(Object value, RequestEvaluator reval,
                           StandardParams stdParams, boolean useDefault)
                throws Exception {
            String text;
            StringBuffer buffer = reval.getResponse().getBuffer();

            if (value == null || "".equals(value)) { //$NON-NLS-1$
                if (useDefault) {
                    text = (String) stdParams.defaultValue;
                } else {
                    return;
                }
            } else {
                text = reval.scriptingEngine.toString(value);
            }

            if ((text != null) && (text.length() > 0)) {
                // only write prefix/suffix if value is not null, if we write the default
                // value provided by the macro tag, we assume it's already complete
                if (stdParams.prefix != null && value != null) {
                    buffer.append(stdParams.prefix);
                }

                switch (encoding) {
                    case ENCODE_NONE:
                        buffer.append(text);

                        break;

                    case ENCODE_HTML:
                        HtmlEncoder.encode(text, buffer);

                        break;

                    case ENCODE_XML:
                        HtmlEncoder.encodeXml(text, buffer);

                        break;

                    case ENCODE_FORM:
                        HtmlEncoder.encodeFormValue(text, buffer);

                        break;

                    case ENCODE_URL:
                        buffer.append(UrlEncoded.encode(text, app.charset));

                        break;

                    case ENCODE_ALL:
                        HtmlEncoder.encodeAll(text, buffer);

                        break;
                }

                if (stdParams.suffix != null && value != null) {
                    buffer.append(stdParams.suffix);
                }
            }
        }

        @Override
        public String toString() {
            return "[Macro: " + name + "]"; //$NON-NLS-1$ //$NON-NLS-2$
        }

        /**
         * Return the full name of the macro in handler.name notation
         * @return the macro name
         */
        public String getName() {
            return name;
        }

        /**
         * Return the numeric type of the macro handler
         * @return the handler type
         */
        public int getHandlerType() {
        	return handlerType;
        }

        /**
         * Return the list of named parameters
         * @return the list of named parameters
         */
        public Map getNamedParams() {
        	return namedParams;
        }
        
        /**
         * Return the list of positional parameters
         * @return the list of positional parameters
         */
        public List getPositionalParams() {
        	return positionalParams;
        }
        
        public boolean hasNestedMacros() {
        	return hasNestedMacros;
        }

    }

    class StandardParams {
        Object prefix = null;
        Object suffix = null;
        Object defaultValue = null;
        int failmode = FAIL_DEFAULT;

        StandardParams() {}

        StandardParams(Map map) {
            readFrom(map);
        }

        void readFrom(Map map) {
            prefix = map.get("prefix"); //$NON-NLS-1$
            suffix = map.get("suffix"); //$NON-NLS-1$
            defaultValue = map.get("default"); //$NON-NLS-1$
        }

        boolean containsMacros() {
            return !(prefix instanceof String)
                || !(suffix instanceof String)
                || !(defaultValue instanceof String);
        }

        void setFailMode(Object value) {
            if ("silent".equals(value)) //$NON-NLS-1$
                failmode = FAIL_SILENT;
            else if ("verbose".equals(value)) //$NON-NLS-1$
                failmode = FAIL_VERBOSE;
            else if (value != null)
                app.logEvent(Messages.getString("Skin.12") + value); //$NON-NLS-1$
        }

        boolean verboseFailmode(Object handler, ScriptingEngine engine) {
            return (failmode == FAIL_VERBOSE) ||
                   (failmode == FAIL_DEFAULT &&
                       (handler == null ||
                        engine.isTypedObject(handler)));
        }

        StandardParams render(RenderContext cx)
                throws Exception {
            if (!containsMacros())
                return this;
            StandardParams stdParams = new StandardParams();
            stdParams.prefix = renderToString(prefix, cx);
            stdParams.suffix = renderToString(suffix, cx);
            stdParams.defaultValue = renderToString(defaultValue, cx);
            return stdParams;
        }

        String renderToString(Object obj, RenderContext cx) throws Exception {
            Object value = processParameter(obj, cx);
            if (value == null)
                return null;
            else if (value instanceof String)
                return (String) value;
            else
                return cx.reval.scriptingEngine.toString(value);
        }

    }

    class RenderContext {
        final RequestEvaluator reval;
        final Object thisObject;
        final Map handlerCache;

        RenderContext(RequestEvaluator reval, Object thisObject, Map handlerCache) {
            this.reval = reval;
            this.thisObject = thisObject;
            this.handlerCache = handlerCache;
        }

        private Object resolveHandler(String handlerName, int handlerType) {
            switch (handlerType) {
                case HANDLER_THIS:
                    return thisObject;
                case HANDLER_RESPONSE:
                    return reval.getResponse().getResponseData();
                case HANDLER_REQUEST:
                    return reval.getRequest().getRequestData();
                case HANDLER_SESSION:
                    return reval.getSession().getCacheNode();
            }

            // try to get handler from handlerCache first
            if (handlerCache != null && handlerCache.containsKey(handlerName)) {
                return handlerCache.get(handlerName);
            }

            // if handler object wasn't found in cache first check this-object
            if (thisObject != null) {
                // not a global macro - need to find handler object
                // was called with this object - check this-object for matching prototype
                Prototype proto = app.getPrototype(thisObject);

                if (proto != null && proto.isInstanceOf(handlerName)) {
                    return cacheHandler(handlerName, thisObject);
                }
            }

            // next look in res.handlers
            Map macroHandlers = reval.getResponse().getMacroHandlers();
            Object obj = macroHandlers.get(handlerName);
            if (obj != null) {
                return cacheHandler(handlerName, obj);
            }

            // finally walk down the this-object's parent chain
            if (thisObject != null) {
                obj = app.getParentElement(thisObject);
                // walk down parent chain to find handler object,
                // limiting to 50 passes to avoid infinite loops
                int maxloop = 50;
                while (obj != null && maxloop-- > 0) {
                    Prototype proto = app.getPrototype(obj);

                    if (proto != null && proto.isInstanceOf(handlerName)) {
                        return cacheHandler(handlerName, obj);
                    }

                    obj = app.getParentElement(obj);
                }
            }

            return cacheHandler(handlerName, null);
        }

        private Object cacheHandler(String name, Object handler) {
            if (handlerCache != null) {
                handlerCache.put(name, handler);
            }
            return handler;
        }

    }

    /**
     * Exception type for unhandled, forbidden or failed macros
     */
    class MacroException extends Exception {
        private static final long serialVersionUID = 396025641010781784L;

        MacroException(String message) {
            super(message);
        }
    }

}

