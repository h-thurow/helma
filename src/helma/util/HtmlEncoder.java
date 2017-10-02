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

package helma.util;

import java.util.*;

/**
 * This is a utility class to encode special characters and do formatting
 * for HTML output.
 */
public final class HtmlEncoder {

    // transformation table for characters 128 to 255. These actually fall into two
    // groups, put together for efficiency: "Windows" chacacters 128-159 such as
    // "smart quotes", which are encoded to valid Unicode entities, and
    // valid ISO-8859 caracters 160-255, which are encoded to the symbolic HTML
    // entity. Everything >= 256 is encoded to a numeric entity.
    //
    // for mor on HTML entities see http://www.pemberley.com/janeinfo/latin1.html  and
    // ftp://ftp.unicode.org/Public/MAPPINGS/VENDORS/MICSFT/WINDOWS/CP1252.TXT
    //
    static final String[] transform =  {
        "&euro;",   // 128 //$NON-NLS-1$
        "",           // empty string means character is undefined in unicode //$NON-NLS-1$
        "&#8218;", //$NON-NLS-1$
        "&#402;", //$NON-NLS-1$
        "&#8222;", //$NON-NLS-1$
        "&#8230;", //$NON-NLS-1$
        "&#8224;", //$NON-NLS-1$
        "&#8225;", //$NON-NLS-1$
        "&#710;", //$NON-NLS-1$
        "&#8240;", //$NON-NLS-1$
        "&#352;", //$NON-NLS-1$
        "&#8249;", //$NON-NLS-1$
        "&#338;", //$NON-NLS-1$
        "", //$NON-NLS-1$
        "&#381;", //$NON-NLS-1$
        "", //$NON-NLS-1$
        "", //$NON-NLS-1$
        "&#8216;", //$NON-NLS-1$
        "&#8217;", //$NON-NLS-1$
        "&#8220;", //$NON-NLS-1$
        "&#8221;", //$NON-NLS-1$
        "&#8226;", //$NON-NLS-1$
        "&#8211;", //$NON-NLS-1$
        "&#8212;", //$NON-NLS-1$
        "&#732;", //$NON-NLS-1$
        "&#8482;", //$NON-NLS-1$
        "&#353;", //$NON-NLS-1$
        "&#8250;", //$NON-NLS-1$
        "&#339;", //$NON-NLS-1$
        "", //$NON-NLS-1$
        "&#382;", //$NON-NLS-1$
        "&#376;",  // 159 //$NON-NLS-1$
        "&nbsp;",    // 160 //$NON-NLS-1$
        "&iexcl;", //$NON-NLS-1$
        "&cent;", //$NON-NLS-1$
        "&pound;", //$NON-NLS-1$
        "&curren;", //$NON-NLS-1$
        "&yen;", //$NON-NLS-1$
        "&brvbar;", //$NON-NLS-1$
        "&sect;", //$NON-NLS-1$
        "&uml;", //$NON-NLS-1$
        "&copy;", //$NON-NLS-1$
        "&ordf;", //$NON-NLS-1$
        "&laquo;", //$NON-NLS-1$
        "&not;", //$NON-NLS-1$
        "&shy;", //$NON-NLS-1$
        "&reg;", //$NON-NLS-1$
        "&macr;", //$NON-NLS-1$
        "&deg;", //$NON-NLS-1$
        "&plusmn;", //$NON-NLS-1$
        "&sup2;", //$NON-NLS-1$
        "&sup3;", //$NON-NLS-1$
        "&acute;", //$NON-NLS-1$
        "&micro;", //$NON-NLS-1$
        "&para;", //$NON-NLS-1$
        "&middot;", //$NON-NLS-1$
        "&cedil;", //$NON-NLS-1$
        "&sup1;", //$NON-NLS-1$
        "&ordm;", //$NON-NLS-1$
        "&raquo;", //$NON-NLS-1$
        "&frac14;", //$NON-NLS-1$
        "&frac12;", //$NON-NLS-1$
        "&frac34;", //$NON-NLS-1$
        "&iquest;", //$NON-NLS-1$
        "&Agrave;", //$NON-NLS-1$
        "&Aacute;", //$NON-NLS-1$
        "&Acirc;", //$NON-NLS-1$
        "&Atilde;", //$NON-NLS-1$
        "&Auml;", //$NON-NLS-1$
        "&Aring;", //$NON-NLS-1$
        "&AElig;", //$NON-NLS-1$
        "&Ccedil;", //$NON-NLS-1$
        "&Egrave;", //$NON-NLS-1$
        "&Eacute;", //$NON-NLS-1$
        "&Ecirc;", //$NON-NLS-1$
        "&Euml;", //$NON-NLS-1$
        "&Igrave;", //$NON-NLS-1$
        "&Iacute;", //$NON-NLS-1$
        "&Icirc;", //$NON-NLS-1$
        "&Iuml;", //$NON-NLS-1$
        "&ETH;", //$NON-NLS-1$
        "&Ntilde;", //$NON-NLS-1$
        "&Ograve;", //$NON-NLS-1$
        "&Oacute;", //$NON-NLS-1$
        "&Ocirc;", //$NON-NLS-1$
        "&Otilde;", //$NON-NLS-1$
        "&Ouml;", //$NON-NLS-1$
        "&times;", //$NON-NLS-1$
        "&Oslash;", //$NON-NLS-1$
        "&Ugrave;", //$NON-NLS-1$
        "&Uacute;", //$NON-NLS-1$
        "&Ucirc;", //$NON-NLS-1$
        "&Uuml;", //$NON-NLS-1$
        "&Yacute;", //$NON-NLS-1$
        "&THORN;", //$NON-NLS-1$
        "&szlig;", //$NON-NLS-1$
        "&agrave;", //$NON-NLS-1$
        "&aacute;", //$NON-NLS-1$
        "&acirc;", //$NON-NLS-1$
        "&atilde;", //$NON-NLS-1$
        "&auml;", //$NON-NLS-1$
        "&aring;", //$NON-NLS-1$
        "&aelig;", //$NON-NLS-1$
        "&ccedil;", //$NON-NLS-1$
        "&egrave;", //$NON-NLS-1$
        "&eacute;", //$NON-NLS-1$
        "&ecirc;", //$NON-NLS-1$
        "&euml;", //$NON-NLS-1$
        "&igrave;", //$NON-NLS-1$
        "&iacute;", //$NON-NLS-1$
        "&icirc;", //$NON-NLS-1$
        "&iuml;", //$NON-NLS-1$
        "&eth;", //$NON-NLS-1$
        "&ntilde;", //$NON-NLS-1$
        "&ograve;", //$NON-NLS-1$
        "&oacute;", //$NON-NLS-1$
        "&ocirc;", //$NON-NLS-1$
        "&otilde;", //$NON-NLS-1$
        "&ouml;", //$NON-NLS-1$
        "&divide;", //$NON-NLS-1$
        "&oslash;", //$NON-NLS-1$
        "&ugrave;", //$NON-NLS-1$
        "&uacute;", //$NON-NLS-1$
        "&ucirc;", //$NON-NLS-1$
        "&uuml;", //$NON-NLS-1$
        "&yacute;", //$NON-NLS-1$
        "&thorn;", //$NON-NLS-1$
        "&yuml;"    // 255 //$NON-NLS-1$
    };

    static final HashSet allTags = new HashSet();

    static {
        allTags.add("a"); //$NON-NLS-1$
        allTags.add("abbr"); //$NON-NLS-1$
        allTags.add("acronym"); //$NON-NLS-1$
        allTags.add("address"); //$NON-NLS-1$
        allTags.add("applet"); //$NON-NLS-1$
        allTags.add("area"); //$NON-NLS-1$
        allTags.add("b"); //$NON-NLS-1$
        allTags.add("base"); //$NON-NLS-1$
        allTags.add("basefont"); //$NON-NLS-1$
        allTags.add("bdo"); //$NON-NLS-1$
        allTags.add("bgsound"); //$NON-NLS-1$
        allTags.add("big"); //$NON-NLS-1$
        allTags.add("blink"); //$NON-NLS-1$
        allTags.add("blockquote"); //$NON-NLS-1$
        allTags.add("bq"); //$NON-NLS-1$
        allTags.add("body"); //$NON-NLS-1$
        allTags.add("br"); //$NON-NLS-1$
        allTags.add("button"); //$NON-NLS-1$
        allTags.add("caption"); //$NON-NLS-1$
        allTags.add("center"); //$NON-NLS-1$
        allTags.add("cite"); //$NON-NLS-1$
        allTags.add("code"); //$NON-NLS-1$
        allTags.add("col"); //$NON-NLS-1$
        allTags.add("colgroup"); //$NON-NLS-1$
        allTags.add("del"); //$NON-NLS-1$
        allTags.add("dfn"); //$NON-NLS-1$
        allTags.add("dir"); //$NON-NLS-1$
        allTags.add("div"); //$NON-NLS-1$
        allTags.add("dl"); //$NON-NLS-1$
        allTags.add("dt"); //$NON-NLS-1$
        allTags.add("dd"); //$NON-NLS-1$
        allTags.add("em"); //$NON-NLS-1$
        allTags.add("embed"); //$NON-NLS-1$
        allTags.add("fieldset"); //$NON-NLS-1$
        allTags.add("font"); //$NON-NLS-1$
        allTags.add("form"); //$NON-NLS-1$
        allTags.add("frame"); //$NON-NLS-1$
        allTags.add("frameset"); //$NON-NLS-1$
        allTags.add("h1"); //$NON-NLS-1$
        allTags.add("h2"); //$NON-NLS-1$
        allTags.add("h3"); //$NON-NLS-1$
        allTags.add("h4"); //$NON-NLS-1$
        allTags.add("h5"); //$NON-NLS-1$
        allTags.add("h6"); //$NON-NLS-1$
        allTags.add("head"); //$NON-NLS-1$
        allTags.add("html"); //$NON-NLS-1$
        allTags.add("hr"); //$NON-NLS-1$
        allTags.add("i"); //$NON-NLS-1$
        allTags.add("iframe"); //$NON-NLS-1$
        allTags.add("img"); //$NON-NLS-1$
        allTags.add("input"); //$NON-NLS-1$
        allTags.add("ins"); //$NON-NLS-1$
        allTags.add("isindex"); //$NON-NLS-1$
        allTags.add("kbd"); //$NON-NLS-1$
        allTags.add("label"); //$NON-NLS-1$
        allTags.add("legend"); //$NON-NLS-1$
        allTags.add("li"); //$NON-NLS-1$
        allTags.add("link"); //$NON-NLS-1$
        allTags.add("listing"); //$NON-NLS-1$
        allTags.add("map"); //$NON-NLS-1$
        allTags.add("marquee"); //$NON-NLS-1$
        allTags.add("menu"); //$NON-NLS-1$
        allTags.add("meta"); //$NON-NLS-1$
        allTags.add("nobr"); //$NON-NLS-1$
        allTags.add("noframes"); //$NON-NLS-1$
        allTags.add("noscript"); //$NON-NLS-1$
        allTags.add("object"); //$NON-NLS-1$
        allTags.add("ol"); //$NON-NLS-1$
        allTags.add("option"); //$NON-NLS-1$
        allTags.add("optgroup"); //$NON-NLS-1$
        allTags.add("p"); //$NON-NLS-1$
        allTags.add("param"); //$NON-NLS-1$
        allTags.add("plaintext"); //$NON-NLS-1$
        allTags.add("pre"); //$NON-NLS-1$
        allTags.add("q"); //$NON-NLS-1$
        allTags.add("s"); //$NON-NLS-1$
        allTags.add("samp"); //$NON-NLS-1$
        allTags.add("script"); //$NON-NLS-1$
        allTags.add("select"); //$NON-NLS-1$
        allTags.add("small"); //$NON-NLS-1$
        allTags.add("span"); //$NON-NLS-1$
        allTags.add("strike"); //$NON-NLS-1$
        allTags.add("strong"); //$NON-NLS-1$
        allTags.add("style"); //$NON-NLS-1$
        allTags.add("sub"); //$NON-NLS-1$
        allTags.add("sup"); //$NON-NLS-1$
        allTags.add("table"); //$NON-NLS-1$
        allTags.add("tbody"); //$NON-NLS-1$
        allTags.add("td"); //$NON-NLS-1$
        allTags.add("textarea"); //$NON-NLS-1$
        allTags.add("tfoot"); //$NON-NLS-1$
        allTags.add("th"); //$NON-NLS-1$
        allTags.add("thead"); //$NON-NLS-1$
        allTags.add("title"); //$NON-NLS-1$
        allTags.add("tr"); //$NON-NLS-1$
        allTags.add("tt"); //$NON-NLS-1$
        allTags.add("u"); //$NON-NLS-1$
        allTags.add("ul"); //$NON-NLS-1$
        allTags.add("var"); //$NON-NLS-1$
        allTags.add("wbr"); //$NON-NLS-1$
        allTags.add("xmp"); //$NON-NLS-1$
    }

    // HTML block tags need to suppress automatic newline to <br>
    // conversion around them to look good. However, they differ
    // in how many newlines around them should ignored. These sets
    // help to treat each tag right in newline conversion.
    static final HashSet internalTags = new HashSet();
    static final HashSet blockTags = new HashSet();
    static final HashSet semiBlockTags = new HashSet();

    static {
        // actual block level elements
        semiBlockTags.add("address"); //$NON-NLS-1$
        semiBlockTags.add("dir"); //$NON-NLS-1$
        semiBlockTags.add("div"); //$NON-NLS-1$
        semiBlockTags.add("table"); //$NON-NLS-1$

        blockTags.add("blockquote"); //$NON-NLS-1$
        blockTags.add("center"); //$NON-NLS-1$
        blockTags.add("dl"); //$NON-NLS-1$
        blockTags.add("fieldset"); //$NON-NLS-1$
        blockTags.add("form"); //$NON-NLS-1$
        blockTags.add("h1"); //$NON-NLS-1$
        blockTags.add("h2"); //$NON-NLS-1$
        blockTags.add("h3"); //$NON-NLS-1$
        blockTags.add("h4"); //$NON-NLS-1$
        blockTags.add("h5"); //$NON-NLS-1$
        blockTags.add("h6"); //$NON-NLS-1$
        blockTags.add("hr"); //$NON-NLS-1$
        blockTags.add("isindex"); //$NON-NLS-1$
        blockTags.add("ol"); //$NON-NLS-1$
        blockTags.add("p"); //$NON-NLS-1$
        blockTags.add("pre"); //$NON-NLS-1$
        blockTags.add("ul"); //$NON-NLS-1$

        internalTags.add("menu"); //$NON-NLS-1$
        internalTags.add("noframes"); //$NON-NLS-1$
        internalTags.add("noscript"); //$NON-NLS-1$

        /// to be treated as block level elements
        semiBlockTags.add("th"); //$NON-NLS-1$

        blockTags.add("br"); //$NON-NLS-1$
        blockTags.add("dd"); //$NON-NLS-1$
        blockTags.add("dt"); //$NON-NLS-1$
        blockTags.add("frameset"); //$NON-NLS-1$
        blockTags.add("li"); //$NON-NLS-1$
        blockTags.add("td"); //$NON-NLS-1$

        internalTags.add("tbody"); //$NON-NLS-1$
        internalTags.add("tfoot"); //$NON-NLS-1$
        internalTags.add("thead"); //$NON-NLS-1$
        internalTags.add("tr"); //$NON-NLS-1$
    }

    // set of tags that are always empty
    static final HashSet emptyTags = new HashSet();

    static {
        emptyTags.add("area"); //$NON-NLS-1$
        emptyTags.add("base"); //$NON-NLS-1$
        emptyTags.add("basefont"); //$NON-NLS-1$
        emptyTags.add("br"); //$NON-NLS-1$
        emptyTags.add("col"); //$NON-NLS-1$
        emptyTags.add("frame"); //$NON-NLS-1$
        emptyTags.add("hr"); //$NON-NLS-1$
        emptyTags.add("img"); //$NON-NLS-1$
        emptyTags.add("input"); //$NON-NLS-1$
        emptyTags.add("isindex"); //$NON-NLS-1$
        emptyTags.add("link"); //$NON-NLS-1$
        emptyTags.add("meta"); //$NON-NLS-1$
        emptyTags.add("param"); //$NON-NLS-1$
    }

    static final byte TAG_NAME = 0;
    static final byte TAG_SPACE = 1;
    static final byte TAG_ATT_NAME = 2;
    static final byte TAG_ATT_VAL = 3;

    static final byte TEXT = 0;
    static final byte SEMIBLOCK = 1;
    static final byte BLOCK = 2;
    static final byte INTERNAL = 3;

    static final String newLine = System.getProperty("line.separator"); //$NON-NLS-1$

    /**
     *  Do "smart" encodging on a string. This means that valid HTML entities and tags,
     *  Helma macros and HTML comments are passed through unescaped, while
     *  other occurrences of '<', '>' and '&' are encoded to HTML entities.
     */
    public final static String encode(String str) {
        if (str == null) {
            return null;
        }

        int l = str.length();

        if (l == 0) {
            return ""; //$NON-NLS-1$
        }

        // try to make stringbuffer large enough from the start
        StringBuffer ret = new StringBuffer(Math.round(l * 1.4f));

        encode(str, ret, false, null);

        return ret.toString();
    }

    /**
     *  Do "smart" encodging on a string. This means that valid HTML entities and tags,
     *  Helma macros and HTML comments are passed through unescaped, while
     *  other occurrences of '<', '>' and '&' are encoded to HTML entities.
     */
    public final static void encode(String str, StringBuffer ret) {
        encode(str, ret, false, null);
    }

    /**
     *  Do "smart" encodging on a string. This means that valid HTML entities and tags,
     *  Helma macros and HTML comments are passed through unescaped, while
     *  other occurrences of '<', '>' and '&' are encoded to HTML entities.
     *
     *  @param str the string to encode
     *  @param ret the string buffer to encode to
     *  @param paragraphs if true use p tags for paragraphs, otherwise just use br's
     *  @param allowedTags a set containing the names of allowed tags as strings. All other
     *                     tags will be escaped
     */
    public final static void encode(String str, StringBuffer ret,
                                    boolean paragraphs, Set allowedTags) {
        if (str == null) {
            return;
        }

        int l = str.length();

        // where to insert the <p> tag in case we want to create a paragraph later on
        int paragraphStart = ret.length();

        // what kind of element/text are we leaving and entering?
        // this is one of TEXT|SEMIBLOCK|BLOCK|INTERNAL
        // depending on this information, we decide whether and how to insert
        // paragraphs and line breaks. "entering" a tag means we're at the '<'
        // and exiting means we're at the '>', not that it's a start or close tag.
        byte entering = TEXT;
        byte exiting = TEXT;

        Stack openTags = new Stack();

        // are we currently within a < and a > that consitute some kind of tag?
        // we use tag balancing to know whether we are inside a tag (and should
        // pass things through unchanged) or outside (and should encode stuff).
        boolean insideTag = false;

        // are we inside an HTML tag?
        boolean insideHtmlTag = false;
        boolean insideCloseTag = false;
        byte htmlTagMode = TAG_NAME;

        // if we are inside a <code> tag, we encode everything to make
        // documentation work easier
        boolean insideCodeTag = false;
        boolean insidePreTag = false;

        // are we within a Helma <% macro %> tag? We treat macro tags and
        // comments specially, since we can't rely on tag balancing
        // to know when we leave a macro tag or comment.
        boolean insideMacroTag = false;

        // are we inside an HTML comment?
        boolean insideComment = false;

        // the quotation mark we are in within an HTML or Macro tag, if any
        char htmlQuoteChar = '\u0000';
        char macroQuoteChar = '\u0000';

        // number of newlines met since the last non-whitespace character
        int linebreaks = 0;

        // did we meet a backslash escape?
        boolean escape = false;

        boolean triggerBreak = false;

        for (int i = 0; i < l; i++) {
            char c = str.charAt(i);

            // step one: check if this is the beginning of an HTML tag, comment or
            // Helma macro.
            if (c == '<') {
                if (i < (l - 2)) {
                    if (!insideMacroTag && ('%' == str.charAt(i + 1))) {
                        // this is the beginning of a Helma macro tag
                        if (!insideCodeTag) {
                            insideMacroTag = insideTag = true;
                            macroQuoteChar = '\u0000';
                        }
                    } else if ('!' == str.charAt(i + 1)) {
                        // the beginning of an HTML comment or !doctype?
                        if (!insideCodeTag) {
                            if (str.regionMatches(i + 2, "--", 0, 2)) { //$NON-NLS-1$
                                insideComment = insideTag = true;
                            } else if (str.regionMatches(true, i+2, "doctype", 0, 7)) { //$NON-NLS-1$
                                insideHtmlTag = insideTag = true;
                            }
                        }
                    } else if (!insideTag) {
                        // check if this is a HTML tag.
                        insideCloseTag = ('/' == str.charAt(i + 1));
                        int tagStart = insideCloseTag ? (i + 2) : (i + 1);
                        int j = tagStart;

                        while ((j < l) && Character.isLetterOrDigit(str.charAt(j)))
                            j++;

                        if ((j > tagStart) && (j < l)) {
                            String tagName = str.substring(tagStart, j).toLowerCase();

                            if ("code".equals(tagName) && insideCloseTag && //$NON-NLS-1$
                                    insideCodeTag) {
                                insideCodeTag = false;
                            }

                            if (((allowedTags == null) || allowedTags.contains(tagName)) &&
                                    allTags.contains(tagName) && !insideCodeTag) {
                                insideHtmlTag = insideTag = true;
                                htmlQuoteChar = '\u0000';
                                htmlTagMode = TAG_NAME;

                                exiting = entering;
                                entering = TEXT;

                                if (internalTags.contains(tagName)) {
                                    entering = INTERNAL;
                                } else if (blockTags.contains(tagName)) {
                                    entering = BLOCK;
                                } else if (semiBlockTags.contains(tagName)) {
                                    entering = paragraphs ? BLOCK : SEMIBLOCK;
                                }

                                if (entering > 0) {
                                    triggerBreak = !insidePreTag;
                                }

                                if (insideCloseTag) {
                                    int t = openTags.search(tagName);

                                    if (t == -1) {
                                        i = j;
                                        insideHtmlTag = insideTag = false;

                                        continue;
                                    } else if (t > 1) {
                                        for (int k = 1; k < t; k++) {
                                            Object tag = openTags.pop();
                                            if (!emptyTags.contains(tag)) {
                                                ret.append("</"); //$NON-NLS-1$
                                                ret.append(tag);
                                                ret.append(">"); //$NON-NLS-1$
                                            }
                                        }
                                    }

                                    openTags.pop();
                                } else {
                                    openTags.push(tagName);
                                }

                                if ("code".equals(tagName) && !insideCloseTag) { //$NON-NLS-1$
                                    insideCodeTag = true;
                                }

                                if ("pre".equals(tagName)) { //$NON-NLS-1$
                                    insidePreTag = !insideCloseTag;
                                }
                            }
                        }
                    }
                } // if (i < l-2)
            }

            if ((triggerBreak || linebreaks > 0) && !Character.isWhitespace(c)) {

                if (!insideTag) {
                    exiting = entering;
                    entering = TEXT;
                    if (exiting >= SEMIBLOCK) {
                        paragraphStart = ret.length();
                    }
                }

                if (entering != INTERNAL && exiting != INTERNAL) {
                    int swallowBreaks = 0;
                    if (paragraphs && 
                          (entering != BLOCK || exiting != BLOCK) &&
                          (exiting < BLOCK) &&
                          (linebreaks > 1) &&
                          paragraphStart < ret.length()) {
                        ret.insert(paragraphStart, "<p>"); //$NON-NLS-1$
                        ret.append("</p>"); //$NON-NLS-1$
                        swallowBreaks = 2;
                    }

                    // treat entering a SEMIBLOCK as entering a TEXT 
                    int _entering = entering == SEMIBLOCK ? TEXT : entering;
                    for (int k = linebreaks-1; k>=0; k--) {
                        if (k >= swallowBreaks && k >= _entering && k >= exiting) {
                            ret.append("<br />"); //$NON-NLS-1$
                        }
                        ret.append(newLine);
                    }
                    if (exiting >= SEMIBLOCK || linebreaks > 1) {
                        paragraphStart = ret.length();
                    }

                }

                linebreaks = 0;
                triggerBreak = false;
            }

            switch (c) {
                case '<':

                    if (insideTag) {
                        ret.append('<');
                    } else {
                        ret.append("&lt;"); //$NON-NLS-1$
                    }

                    break;

                case '&':

                    // check if this is an HTML entity already,
                    // in which case we pass it though unchanged
                    if ((i < (l - 3)) && !insideCodeTag) {
                        // is this a numeric entity?
                        if (str.charAt(i + 1) == '#') {
                            int j = i + 2;

                            while ((j < l) && Character.isDigit(str.charAt(j)))
                                j++;

                            if ((j < l) && (str.charAt(j) == ';')) {
                                ret.append("&"); //$NON-NLS-1$

                                break;
                            }
                        } else {
                            int j = i + 1;

                            while ((j < l) && Character.isLetterOrDigit(str.charAt(j)))
                                j++;

                            if ((j < l) && (str.charAt(j) == ';')) {
                                ret.append("&"); //$NON-NLS-1$

                                break;
                            }
                        }
                    }

                    // we didn't reach a break, so encode as entity unless inside a tag
                    if (insideMacroTag) {
                        ret.append('&');
                    } else {
                        ret.append("&amp;"); //$NON-NLS-1$
                    }
                    break;

                case '\\':
                    ret.append(c);

                    if (insideTag && !insideComment) {
                        escape = !escape;
                    }

                    break;

                case '"':
                case '\'':
                    ret.append(c);

                    if (!insideComment) {
                        // check if the quote is escaped
                        if (insideMacroTag) {
                            if (escape) {
                                escape = false;
                            } else if (macroQuoteChar == c) {
                                macroQuoteChar = '\u0000';
                            } else if (macroQuoteChar == '\u0000') {
                                macroQuoteChar = c;
                            }
                        } else if (insideHtmlTag) {
                            if (escape) {
                                escape = false;
                            } else if (htmlQuoteChar == c) {
                                htmlQuoteChar = '\u0000';
                                htmlTagMode = TAG_SPACE;
                            } else if (htmlQuoteChar == '\u0000') {
                                htmlQuoteChar = c;
                            }
                        }
                    }

                    break;

                case '\n':
                    if (insideTag || insidePreTag) {
                        ret.append('\n');
                    } else {
                        linebreaks++;
                    }

                    break;
                case '\r':
                    if (insideTag || insidePreTag) {
                        ret.append('\r');
                    }
                    break;

                case '>':

                    // For Helma macro tags and comments, we overrule tag balancing,
                    // i.e. we don't require that '<' and '>' be balanced within
                    // macros and comments. Rather, we check for the matching closing tag.
                    if (insideComment) {
                        ret.append('>');
                        insideComment = !((str.charAt(i - 2) == '-') &&
                                        (str.charAt(i - 1) == '-'));
                    } else if (insideMacroTag) {
                        ret.append('>');
                        insideMacroTag = !((str.charAt(i - 1) == '%') &&
                                         (macroQuoteChar == '\u0000'));
                    } else if (insideHtmlTag) {
                        ret.append('>');

                        // only leave HTML tag if quotation marks are balanced
                        // within that tag.
                        insideHtmlTag = htmlQuoteChar != '\u0000';

                        // Check if this is an empty tag so we don't generate an
                        // additional </close> tag.
                        if (str.charAt(i - 1) == '/') {
                            // this is to avoid misinterpreting tags like
                            // <a href=http://foo/> as empty
                            if (!openTags.empty() && htmlTagMode != TAG_ATT_VAL &&
                                                     htmlTagMode != TAG_ATT_NAME) {
                                openTags.pop();
                            }
                        }

                        exiting = entering;
                        if (exiting > 0) {
                           triggerBreak = !insidePreTag;
                        }

                    } else {
                        ret.append("&gt;"); //$NON-NLS-1$
                    }

                    // check if we still are inside any kind of tag
                    insideTag = insideComment || insideMacroTag || insideHtmlTag;
                    insideCloseTag = insideTag;

                    break;

                default:

                    if (insideHtmlTag && !insideCloseTag) {
                        switch(htmlTagMode) {
                            case TAG_NAME:
                                if (!Character.isLetterOrDigit(c)) {
                                    htmlTagMode = TAG_SPACE;
                                }
                                break;
                            case TAG_SPACE:
                                if (Character.isLetterOrDigit(c)) {
                                    htmlTagMode = TAG_ATT_NAME;
                                }
                                break;
                            case TAG_ATT_NAME:
                                if (c == '=') {
                                    htmlTagMode = TAG_ATT_VAL;
                                } else if (c == ' ') {
                                    htmlTagMode = TAG_SPACE;
                                }
                                break;
                            case TAG_ATT_VAL:
                                if (Character.isWhitespace(c) && htmlQuoteChar == '\u0000') {
                                    htmlTagMode = TAG_SPACE;
                                }
                                break;
                        }
                    }
                    if (c < 128 || insideMacroTag) {
                        ret.append(c);
                    } else if ((c >= 128) && (c < 256)) {
                        ret.append(transform[c - 128]);
                    } else {
                        ret.append("&#"); //$NON-NLS-1$
                        ret.append((int) c);
                        ret.append(";"); //$NON-NLS-1$
                    }

                    escape = false;
            }
        }

        // if tags were opened but not closed, close them.
        int o = openTags.size();

        if (o > 0) {
            for (int k = 0; k < o; k++) {
                Object tag = openTags.pop();
                if (!emptyTags.contains(tag)) {
                    ret.append("</"); //$NON-NLS-1$
                    ret.append(tag);
                    ret.append(">"); //$NON-NLS-1$
                }
            }
        }

        // add remaining newlines we may have collected
        int swallowBreaks = 0;
        if (paragraphs && entering < BLOCK) {
            ret.insert(paragraphStart, "<p>"); //$NON-NLS-1$
            ret.append("</p>"); //$NON-NLS-1$
            swallowBreaks = 2;
        }

        if (linebreaks > 0) {
            for (int i = linebreaks-1; i>=0; i--) {
                if (i >= swallowBreaks && i > exiting) {
                    ret.append("<br />"); //$NON-NLS-1$
                }
                ret.append(newLine);
            }
        }
    }

    /**
     *
     */
    public final static String encodeFormValue(String str) {
        if (str == null) {
            return null;
        }

        int l = str.length();

        if (l == 0) {
            return ""; //$NON-NLS-1$
        }

        StringBuffer ret = new StringBuffer(Math.round(l * 1.2f));

        encodeAll(str, ret, false);

        return ret.toString();
    }

    /**
     *
     */
    public final static void encodeFormValue(String str, StringBuffer ret) {
        encodeAll(str, ret, false);
    }

    /**
     *
     */
    public final static String encodeAll(String str) {
        if (str == null) {
            return null;
        }

        int l = str.length();

        if (l == 0) {
            return ""; //$NON-NLS-1$
        }

        StringBuffer ret = new StringBuffer(Math.round(l * 1.2f));

        encodeAll(str, ret, true);

        return ret.toString();
    }

    /**
     *
     */
    public final static void encodeAll(String str, StringBuffer ret) {
        encodeAll(str, ret, true);
    }

    /**
     *
     */
    public final static void encodeAll(String str, StringBuffer ret, boolean encodeNewline) {
        if (str == null) {
            return;
        }

        int l = str.length();

        for (int i = 0; i < l; i++) {
            char c = str.charAt(i);

            switch (c) {
                case '<':
                    ret.append("&lt;"); //$NON-NLS-1$
                    break;

                case '>':
                    ret.append("&gt;"); //$NON-NLS-1$
                    break;

                case '&':
                    ret.append("&amp;"); //$NON-NLS-1$
                    break;

                case '"':
                    ret.append("&quot;"); //$NON-NLS-1$
                    break;

                case '\n':
                    if (encodeNewline) {
                        ret.append("<br />"); //$NON-NLS-1$
                    }
                    ret.append('\n');
                    break;

                default:
                    // ret.append (c);
                    if (c < 128) {
                        ret.append(c);
                    } else if ((c >= 128) && (c < 256)) {
                        ret.append(transform[c - 128]);
                    } else {
                        ret.append("&#"); //$NON-NLS-1$
                        ret.append((int) c);
                        ret.append(";"); //$NON-NLS-1$
                    }
            }
        }
    }

    /**
     *
     *
     * @param str ...
     *
     * @return ...
     */
    public final static String encodeXml(String str) {
        if (str == null) {
            return null;
        }

        int l = str.length();
        if (l == 0) {
            return ""; //$NON-NLS-1$
        }

        StringBuffer ret = new StringBuffer(Math.round(l * 1.2f));
        encodeXml(str, ret);
        return ret.toString();
    }

    /**
     *
     *
     * @param str ...
     * @param ret ...
     */
    public final static void encodeXml(String str, StringBuffer ret) {
        if (str == null) {
            return;
        }

        int l = str.length();

        for (int i = 0; i < l; i++) {
            char c = str.charAt(i);

            switch (c) {
                case '<':
                    ret.append("&lt;"); //$NON-NLS-1$
                    break;

                case '>':
                    ret.append("&gt;"); //$NON-NLS-1$
                    break;

                case '&':
                    ret.append("&amp;"); //$NON-NLS-1$
                    break;

                case '"':
                    ret.append("&quot;"); //$NON-NLS-1$
                    break;

                case '\'':
                    ret.append("&#39;"); //$NON-NLS-1$
                    break;

                default:

                    if (c < 0x20) {
                        // sort out invalid XML characters below 0x20 - all but 0x9, 0xA and 0xD.
                        // The trick is an adaption of java.lang.Character.isSpace().
                        if (((((1L << 0x9) | (1L << 0xA) | (1L << 0xD)) >> c) & 1L) != 0) {
                            ret.append(c);
                        }
                    } else {
                        ret.append(c);
                    }
            }
        }
    }

    // test method
    public static String printCharRange(int from, int to) {
        StringBuffer response = new StringBuffer();

        for (int i = from; i < to; i++) {
            response.append(i);
            response.append("      "); //$NON-NLS-1$
            response.append((char) i);
            response.append("      "); //$NON-NLS-1$

            if (i < 128) {
                response.append((char) i);
            } else if ((i >= 128) && (i < 256)) {
                response.append(transform[i - 128]);
            } else {
                response.append("&#"); //$NON-NLS-1$
                response.append(i);
                response.append(";"); //$NON-NLS-1$
            }

            response.append("\r\n"); //$NON-NLS-1$
        }

        return response.toString();
    }

    // for testing...
    public static void main(String[] args) {
        for (int i = 0; i < args.length; i++)
            System.err.println(encode(args[i]));
    }
}
 // end of class
