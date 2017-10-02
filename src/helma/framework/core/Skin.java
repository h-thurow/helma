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

package helma.framework.core;

import helma.framework.*;
import helma.framework.repository.ResourceInterface;
import helma.objectmodel.ConcurrencyException;
import helma.util.*;
import helma.scripting.ScriptingEngineInterface;

import java.util.*;
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
        this.length = this.source.length;
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
        this.length = this.source.length;
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

    public static Skin getSkin(ResourceInterface res, Application app) throws IOException {
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
        for (int i = this.offset; i < (this.length - 1); i++) {
            if (this.source[i] == '<' && this.source[i + 1] == '%' && !escape) {
                // found macro start tag
                Macro macro = new Macro(i, 2);
                if (macro.isSubskinMacro) {
                    new Skin(this.parentSkin, macro);
                    this.length = i;
                    break;
                }
                if (!macro.isCommentMacro) {
                    this.hasContent = true;
                }
                partBuffer.add(macro);
                i = macro.end - 1;
            } else {
                if (!this.hasContent && !Character.isWhitespace(this.source[i])){
                    this.hasContent = true;
                }
                escape = this.source[i] == '\\' && !escape;
            }
        }

        this.macros = new Macro[partBuffer.size()];
        partBuffer.toArray(this.macros);
    }

    private void addSubskin(String name, Skin subskin) {
        if (this.subskins == null) {
            this.subskins = new HashMap();
        }
        this.subskins.put(name, subskin);
    }
    
    /**
     * Return the list of macros found by the parser
     * @return the list of macros
     */
    public Macro[] getMacros() {
    	return this.macros;
    }

    /**
     * Check if this skin has a main skin, as opposed to consisting just of subskins
     * @return true if this skin contains a main skin
     */
    public boolean hasMainskin() {
        return this.hasContent;
    }

    /**
     * Check if this skin contains a subskin with the given name
     * @param name a subskin name
     * @return true if the given subskin exists
     */
    public boolean hasSubskin(String name) {
        return this.subskins != null && this.subskins.containsKey(name);
    }

    /**
     * Get a subskin by name
     * @param name the subskin name
     * @return the subskin
     */
    public Skin getSubskin(String name) {
        return this.subskins == null ? null : (Skin) this.subskins.get(name);
    }

    /**
     * Return an array of subskin names defined in this skin
     * @return a string array containing this skin's substrings
     */
    public String[] getSubskinNames() {
        return this.subskins == null ?
                new String[0] :
                (String[]) this.subskins.keySet().toArray(new String[0]);
    }

    public String getExtends() {
        return this.extendz;
    }

    /**
     * Get the raw source text this skin was parsed from
     */
    public String getSource() {
        return new String(this.source, this.offset, this.length - this.offset);
    }

    /**
     * Render this skin and return it as string
     */
    public String renderAsString(RequestEvaluator reval, Object thisObject, Object paramObject)
                throws RedirectException {
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
                throws RedirectException {
        // check for endless skin recursion
        if (++reval.skinDepth > 50) {
            throw new RuntimeException(Messages.getString("Skin.0")); //$NON-NLS-1$
        }

        ResponseTrans res = reval.getResponse();

        if (this.macros == null) {
            res.write(this.source, this.offset, this.length - this.offset);
            reval.skinDepth--;
            return;
        }

        // register param object, remember previous one to reset afterwards
        Map handlers = res.getMacroHandlers();
        Object previousParam = handlers.put("param", paramObject); //$NON-NLS-1$
        Skin previousSkin = res.switchActiveSkin(this.parentSkin);

        try {
            int written = this.offset;
            Map handlerCache = null;

            if (this.macros.length > 3) {
                handlerCache = new HashMap();
            }
            RenderContext cx = new RenderContext(reval, thisObject, handlerCache);

            for (int i = 0; i < this.macros.length; i++) {
                if (this.macros[i].start > written) {
                    res.write(this.source, written, this.macros[i].start - written);
                }

                this.macros[i].render(cx);
                written = this.macros[i].end;
            }

            if (written < this.length) {
                res.write(this.source, written, this.length - written);
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
        for (int i = 0; i < this.macros.length; i++) {
            if (this.macros[i] != null) {
                Macro m = this.macros[i];

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
        if (this.sandbox == null) {
            this.sandbox = new HashSet();
        }

        this.sandbox.add(macroname);
    }

    private Object processParameter(Object value, RenderContext cx)
            throws Exception {
        if (value instanceof Macro) {
            return ((Macro) value).invokeAsParameter(cx);
        }
        return value;
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

            if (this.isSubskinMacro) {
                if (i + 1 < Skin.this.length && Skin.this.source[i] == '\r' && Skin.this.source[i + 1] == '\n')
                    this.end = Math.min(Skin.this.length, i + 2);
                else if (i < Skin.this.length && (Skin.this.source[i] == '\r' || Skin.this.source[i] == '\n'))
                    this.end = Math.min(Skin.this.length, i + 1);
                else
                    this.end = Math.min(Skin.this.length, i);
            } else {
                this.end = Math.min(Skin.this.length, i);
            }

            this.path = StringUtils.split(this.name, "."); //$NON-NLS-1$
            if (this.path.length <= 1) {
                this.handlerType = HANDLER_GLOBAL;
            } else {
                String handlerName = this.path[0];
                if ("this".equalsIgnoreCase(handlerName)) { //$NON-NLS-1$
                    this.handlerType = HANDLER_THIS;
                } else if ("response".equalsIgnoreCase(handlerName)) { //$NON-NLS-1$
                    this.handlerType = HANDLER_RESPONSE;
                } else if ("request".equalsIgnoreCase(handlerName)) { //$NON-NLS-1$
                    this.handlerType = HANDLER_REQUEST;
                } else if ("session".equalsIgnoreCase(handlerName)) { //$NON-NLS-1$
                    this.handlerType = HANDLER_SESSION;
                } else if ("param".equalsIgnoreCase(handlerName)) { //$NON-NLS-1$
                    this.handlerType = HANDLER_PARAM;
                }
            }

            if (".extends".equals(this.name)) { //$NON-NLS-1$
                if (Skin.this.parentSkin != Skin.this) {
                    throw new RuntimeException(Messages.getString("Skin.1")); //$NON-NLS-1$
                }
                if (this.positionalParams == null || this.positionalParams.size() < 1
                        || !(this.positionalParams.get(0) instanceof String)) {
                    throw new RuntimeException(Messages.getString("Skin.2")); //$NON-NLS-1$
                }
                Skin.this.extendz = (String) this.positionalParams.get(0);
                this.isCommentMacro = true; // don't render
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
            for (i = this.start + macroOffset; i < Skin.this.length - 1; i++) {

                switch (Skin.this.source[i]) {

                    case '<':

                        if (state == PARSE_PARAM && quotechar == '\u0000'
                                && b.length() == 0 && Skin.this.source[i + 1] == '%') {
                            Macro macro = new Macro(i, 2);
                            addParameter(lastParamName, macro);
                            lastParamName = null;
                            b.setLength(0);
                            i = macro.end - 1;
                        } else {
                            b.append(Skin.this.source[i]);
                            escape = false;
                        }
                        break;

                    case '%':

                        if ((state != PARSE_PARAM || quotechar == '\u0000' || lenient)
                                && Skin.this.source[i + 1] == '>') {
                            state = PARSE_DONE;
                            break loop;
                        }
                        b.append(Skin.this.source[i]);
                        escape = false;
                        break;

                    case '/':

                        b.append(Skin.this.source[i]);
                        escape = false;

                        if (state == PARSE_MACRONAME && "//".equals(b.toString())) { //$NON-NLS-1$
                            this.isCommentMacro = true;
                            // just continue parsing the macro as this is the only way
                            // to correctly catch embedded macros - see bug 588
                        }
                        break;

                    case '#':

                        if (state == PARSE_MACRONAME && b.length() == 0) {
                            // this is a subskin/skinlet
                            this.isSubskinMacro = true;
                            break;
                        }
                        b.append(Skin.this.source[i]);
                        escape = false;
                        break;

                    case '|':

                        if (!escape && quotechar == '\u0000') {
                            this.filterChain = new Macro(i, 1);
                            i = this.filterChain.end - 2;
                            lastParamName = null;
                            b.setLength(0);
                            state = PARSE_DONE;
                            break loop;
                        }
                        b.append(Skin.this.source[i]);
                        escape = false;
                        break;

                    case '\\':

                        if (escape) {
                            b.append(Skin.this.source[i]);
                        }

                        escape = !escape;

                        break;

                    case '"':
                    case '\'':

                        if (!escape && state == PARSE_PARAM) {
                            if (quotechar == Skin.this.source[i]) {
                                if (Skin.this.source[i + 1] != '%' && !Character.isWhitespace(Skin.this.source[i + 1]) && !lenient) {
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
                                quotechar = Skin.this.source[i];
                                b.setLength(0);
                            } else {
                                b.append(Skin.this.source[i]);
                            }
                        } else {
                            b.append(Skin.this.source[i]);
                        }

                        escape = false;

                        break;

                    case ' ':
                    case '\t':
                    case '\n':
                    case '\r':
                    case '\f':

                        if (state == PARSE_MACRONAME && b.length() > 0) {
                            this.name = b.toString().trim();
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
                                b.append(Skin.this.source[i]);
                                escape = false;
                            }
                        }

                        break;

                    case '=':

                        if (!escape && quotechar == '\u0000' && state == PARSE_PARAM && lastParamName == null) {
                            lastParamName = b.toString().trim();
                            b.setLength(0);
                        } else {
                            b.append(Skin.this.source[i]);
                            escape = false;
                        }

                        break;

                    default:
                        b.append(Skin.this.source[i]);
                        escape = false;
                }

                if (i == Skin.this.length - 2 && !lenient &&
                        (state != PARSE_DONE ||quotechar != '\u0000')) {
                    // macro tag is not properly terminated, switch to lenient mode                    
                    reset();
                    return parse(macroOffset, true);
                }
            }

            if (b.length() > 0) {
                if (this.name == null) {
                    this.name = b.toString().trim();
                } else {
                    addParameter(lastParamName, b.toString());
                }
            }

            if (state != PARSE_DONE) {
                Skin.this.app.logError(Messages.getString("Skin.3") +this); //$NON-NLS-1$
            }

            return i + 2;
        }

        private void reset() {
            this.filterChain = null;
            this.name = null;
            this.standardParams = new StandardParams();
            this.namedParams = null;
            this.positionalParams = null;
        }

        private void addParameter(String name, Object value) {
            if (!(value instanceof String)) {
                this.hasNestedMacros = true;                
            }
            if (name == null) {
                // take shortcut for positional parameters
                if (this.positionalParams == null) {
                    this.positionalParams = new ArrayList();
                }
                this.positionalParams.add(value);
                return;
            }
            // check if this is parameter is relevant to us
            if ("prefix".equals(name)) { //$NON-NLS-1$
                this.standardParams.prefix = value;
            } else if ("suffix".equals(name)) { //$NON-NLS-1$
                this.standardParams.suffix = value;
            } else if ("encoding".equals(name)) { //$NON-NLS-1$
                if ("html".equals(value)) { //$NON-NLS-1$
                    this.encoding = ENCODE_HTML;
                } else if ("xml".equals(value)) { //$NON-NLS-1$
                    this.encoding = ENCODE_XML;
                } else if ("form".equals(value)) { //$NON-NLS-1$
                    this.encoding = ENCODE_FORM;
                } else if ("url".equals(value)) { //$NON-NLS-1$
                    this.encoding = ENCODE_URL;
                } else if ("all".equals(value)) { //$NON-NLS-1$
                    this.encoding = ENCODE_ALL;
                } else {
                    Skin.this.app.logEvent(Messages.getString("Skin.4") + value); //$NON-NLS-1$
                }
            } else if ("default".equals(name)) { //$NON-NLS-1$
                this.standardParams.defaultValue = value;
            } else if ("failmode".equals(name)) { //$NON-NLS-1$
                this.standardParams.setFailMode(value);
            }

            // Add parameter to parameter map
            if (this.namedParams == null) {
                this.namedParams = new HashMap();
            }
            this.namedParams.put(name, value);
        }

        private Object invokeAsMacro(RenderContext cx, StandardParams stdParams, boolean asObject)
                throws Exception {

            // immediately return for comment macros
            if (this.isCommentMacro || this.name == null) {
                return null;
            }

            if ((Skin.this.sandbox != null) && !Skin.this.sandbox.contains(this.name)) {
                throw new MacroException(Messages.getString("Skin.5") + this.name); //$NON-NLS-1$
            }

            Object handler = null;
            Object value = null;
            ScriptingEngineInterface engine = cx.reval.scriptingEngine;

            if (this.handlerType != HANDLER_GLOBAL) {
                handler = cx.resolveHandler(this.path[0], this.handlerType);
                handler = resolvePath(handler, cx.reval);
            }

            if (this.handlerType == HANDLER_GLOBAL || handler != null) {
                // check if a function called name_macro is defined.
                // if so, the macro evaluates to the function. Otherwise,
                // a property/field with the name is used, if defined.
                String propName = this.path[this.path.length - 1];
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
                }
                if (this.handlerType == HANDLER_RESPONSE) {
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
                    } else if (this.standardParams.verboseFailmode(handler, engine)) {
                        throw new MacroException(Messages.getString("Skin.6") + this.name); //$NON-NLS-1$
                    }
                } else {
                    value = engine.getProperty(handler, propName);
                }
                return filter(value, cx);
            } else if (this.standardParams.verboseFailmode(handler, engine)) {
                throw new MacroException(Messages.getString("Skin.7") + this.name); //$NON-NLS-1$
            }
            return filter(null, cx);
        }

        /**
         * Render this macro as nested macro, only converting to string
         * if necessary.
         */
        Object invokeAsParameter(RenderContext cx) throws Exception {
            StandardParams stdParams = this.standardParams.render(cx);            
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
                throws RedirectException {
            StringBuffer buffer = cx.reval.getResponse().getBuffer();
            // remember length of response buffer before calling macro
            int bufLength = buffer.length();
            try {
                StandardParams stdParams = this.standardParams.render(cx);
                boolean asObject = this.filterChain != null;
                Object value = invokeAsMacro(cx, stdParams, asObject);

                // check if macro wrote out to response buffer
                if (buffer.length() == bufLength) {
                    // If the macro function didn't write anything to the response itself,
                    // we interpret its return value as macro output.
                    writeResponse(value, cx.reval, stdParams, true);
                } else {
                    // if an encoding is specified, re-encode the macro's output
                    if (this.encoding != ENCODE_NONE) {
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
                Skin.this.app.logError(msg);
            } catch (Exception x) {
                String msg = x.getMessage();
                if ((msg == null) || (msg.length() < 10)) {
                    msg = x.toString();
                }
                msg = new StringBuffer(Messages.getString("Skin.8")).append(this.name) //$NON-NLS-1$
                        .append(": ").append(msg).toString(); //$NON-NLS-1$
                cx.reval.getResponse().write(" [" + msg + "] ");  //$NON-NLS-1$//$NON-NLS-2$
                Skin.this.app.logError(msg, x);
            }
        }

        private Object filter(Object returnValue, RenderContext cx)
                throws Exception {
            // invoke filter chain if defined
            if (this.filterChain != null) {
                return this.filterChain.invokeAsFilter(returnValue, cx);
            }
            return returnValue;
        }

        private Object invokeAsFilter(Object returnValue, RenderContext cx)
                throws Exception {

            if (this.name == null) {
                throw new MacroException(Messages.getString("Skin.9")); //$NON-NLS-1$
            } else if (Skin.this.sandbox != null && !Skin.this.sandbox.contains(this.name)) {
                throw new MacroException(Messages.getString("Skin.10") + this.name); //$NON-NLS-1$
            }
            Object handlerObject = null;

            if (this.handlerType != HANDLER_GLOBAL) {
                handlerObject = cx.resolveHandler(this.path[0], this.handlerType);
                handlerObject = resolvePath(handlerObject, cx.reval);
            }

            String propName = this.path[this.path.length - 1] + "_filter"; //$NON-NLS-1$
            String funcName = resolveFunctionName(handlerObject, propName,
                    cx.reval.scriptingEngine);

            if (funcName != null) {
                Object[] arguments = prepareArguments(1, cx);
                arguments[0] = returnValue;
                Object retval = cx.reval.invokeDirectFunction(handlerObject,
                                                           funcName,
                                                           arguments);

                return filter(retval, cx);
            }
            throw new MacroException(Messages.getString("Skin.11") + this.name); //$NON-NLS-1$
        }

        private Object[] prepareArguments(int offset, RenderContext cx)
                throws Exception {
            int nPosArgs = (this.positionalParams == null) ? 0 : this.positionalParams.size();
            Object[] arguments = new Object[offset + 1 + nPosArgs];

            if (this.namedParams == null) {
                arguments[offset] = new SystemMap(4);
            } else if (this.hasNestedMacros) {
                SystemMap map = new SystemMap((int) (this.namedParams.size() * 1.5));
                for (Iterator it = this.namedParams.entrySet().iterator(); it.hasNext(); ) {
                    Map.Entry entry = (Map.Entry) it.next();
                    Object value = entry.getValue();
                    if (!(value instanceof String))
                        value = processParameter(value, cx);
                    map.put(entry.getKey(), value);
                }
                arguments[offset] = map;
            } else {
                // pass a clone/copy of the parameter map so if the script changes it,
                arguments[offset] = new CopyOnWriteMap(this.namedParams);
            }
            if (this.positionalParams != null) {
                for (int i = 0; i < nPosArgs; i++) {
                    Object value = this.positionalParams.get(i);
                    if (!(value instanceof String))
                        value = processParameter(value, cx);
                    arguments[offset + 1 + i] = value;
                }
            }
            return arguments;
        }

        private Object resolvePath(Object handler, RequestEvaluator reval) throws Exception {
            for (int i = 1; i < this.path.length - 1; i++) {
                Object[] arguments = {this.path[i]};
                Object next = reval.invokeDirectFunction(handler, "getMacroHandler", arguments); //$NON-NLS-1$
                if (next != null) {
                    handler = next;
                } else if (!reval.scriptingEngine.isTypedObject(handler)) {
                    handler = reval.scriptingEngine.getProperty(handler, this.path[i]);
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
                                           ScriptingEngineInterface engine) {
            if (this.handlerType == HANDLER_GLOBAL) {
                String[] macroPath = Skin.this.app.globalMacroPath;
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

                switch (this.encoding) {
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
                        buffer.append(UrlEncoded.encode(text, Skin.this.app.charset));

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
            return "[Macro: " + this.name + "]"; //$NON-NLS-1$ //$NON-NLS-2$
        }

        /**
         * Return the full name of the macro in handler.name notation
         * @return the macro name
         */
        public String getName() {
            return this.name;
        }

        /**
         * Return the numeric type of the macro handler
         * @return the handler type
         */
        public int getHandlerType() {
        	return this.handlerType;
        }

        /**
         * Return the list of named parameters
         * @return the list of named parameters
         */
        public Map getNamedParams() {
        	return this.namedParams;
        }
        
        /**
         * Return the list of positional parameters
         * @return the list of positional parameters
         */
        public List getPositionalParams() {
        	return this.positionalParams;
        }
        
        public boolean hasNestedMacros() {
        	return this.hasNestedMacros;
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
            this.prefix = map.get("prefix"); //$NON-NLS-1$
            this.suffix = map.get("suffix"); //$NON-NLS-1$
            this.defaultValue = map.get("default"); //$NON-NLS-1$
        }

        boolean containsMacros() {
            return !(this.prefix instanceof String)
                || !(this.suffix instanceof String)
                || !(this.defaultValue instanceof String);
        }

        void setFailMode(Object value) {
            if ("silent".equals(value)) //$NON-NLS-1$
                this.failmode = FAIL_SILENT;
            else if ("verbose".equals(value)) //$NON-NLS-1$
                this.failmode = FAIL_VERBOSE;
            else if (value != null)
                Skin.this.app.logEvent(Messages.getString("Skin.12") + value); //$NON-NLS-1$
        }

        boolean verboseFailmode(Object handler, ScriptingEngineInterface engine) {
            return (this.failmode == FAIL_VERBOSE) ||
                   (this.failmode == FAIL_DEFAULT &&
                       (handler == null ||
                        engine.isTypedObject(handler)));
        }

        StandardParams render(RenderContext cx)
                throws Exception {
            if (!containsMacros())
                return this;
            StandardParams stdParams = new StandardParams();
            stdParams.prefix = renderToString(this.prefix, cx);
            stdParams.suffix = renderToString(this.suffix, cx);
            stdParams.defaultValue = renderToString(this.defaultValue, cx);
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
                    return this.thisObject;
                case HANDLER_RESPONSE:
                    return this.reval.getResponse().getResponseData();
                case HANDLER_REQUEST:
                    return this.reval.getRequest().getRequestData();
                case HANDLER_SESSION:
                    return this.reval.getSession().getCacheNode();
            }

            // try to get handler from handlerCache first
            if (this.handlerCache != null && this.handlerCache.containsKey(handlerName)) {
                return this.handlerCache.get(handlerName);
            }

            // if handler object wasn't found in cache first check this-object
            if (this.thisObject != null) {
                // not a global macro - need to find handler object
                // was called with this object - check this-object for matching prototype
                Prototype proto = Skin.this.app.getPrototype(this.thisObject);

                if (proto != null && proto.isInstanceOf(handlerName)) {
                    return cacheHandler(handlerName, this.thisObject);
                }
            }

            // next look in res.handlers
            Map macroHandlers = this.reval.getResponse().getMacroHandlers();
            Object obj = macroHandlers.get(handlerName);
            if (obj != null) {
                return cacheHandler(handlerName, obj);
            }

            // finally walk down the this-object's parent chain
            if (this.thisObject != null) {
                obj = Skin.this.app.getParentElement(this.thisObject);
                // walk down parent chain to find handler object,
                // limiting to 50 passes to avoid infinite loops
                int maxloop = 50;
                while (obj != null && maxloop-- > 0) {
                    Prototype proto = Skin.this.app.getPrototype(obj);

                    if (proto != null && proto.isInstanceOf(handlerName)) {
                        return cacheHandler(handlerName, obj);
                    }

                    obj = Skin.this.app.getParentElement(obj);
                }
            }

            return cacheHandler(handlerName, null);
        }

        private Object cacheHandler(String name, Object handler) {
            if (this.handlerCache != null) {
                this.handlerCache.put(name, handler);
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

