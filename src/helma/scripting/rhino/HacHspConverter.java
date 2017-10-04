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

package helma.scripting.rhino;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.StringTokenizer;

import helma.framework.repository.ResourceInterface;

/**
 *  Support for .hac (action) and .hsp (template) files
 */
public class HacHspConverter {

    public static String convertHac(ResourceInterface action, String encoding)
            throws IOException {
        String functionName = action.getBaseName().replace('.', '_') + "_action"; //$NON-NLS-1$
        return composeFunction(functionName, null, action.getContent(encoding));
    }

    public static String convertHsp(ResourceInterface template, String encoding)
            throws IOException {
        String functionName = template.getBaseName().replace('.', '_');
        String body = processHspBody(template.getContent(encoding));
        return composeFunction(functionName,
                               "arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10", //$NON-NLS-1$
                               body);
    }

    public static String convertHspAsString(ResourceInterface template, String encoding)
            throws IOException {
        String functionName = template.getBaseName().replace('.', '_') + "_as_string"; //$NON-NLS-1$
        String body = processHspBody(template.getContent(encoding));
        return composeFunction(functionName,
                               "arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10", //$NON-NLS-1$
                               "res.pushStringBuffer(); " + body + //$NON-NLS-1$
                               "\r\nreturn res.popStringBuffer();\r\n"); //$NON-NLS-1$
    }

    static String composeFunction(String funcname, String args, String body) {
        if ((body == null) || "".equals(body.trim())) { //$NON-NLS-1$
            body = ";\r\n"; //$NON-NLS-1$
        } else {
            body = body + "\r\n"; //$NON-NLS-1$
        }

        StringBuffer f = new StringBuffer("function "); //$NON-NLS-1$

        f.append(funcname);
        f.append(" ("); //$NON-NLS-1$
        if (args != null)
            f.append(args);
        f.append(") {"); //$NON-NLS-1$
        f.append(body);
        f.append("}"); //$NON-NLS-1$

        return f.toString();
    }

    static String processHspBody(String content) {
        ArrayList partBuffer = new ArrayList();
        char[] cnt = content.toCharArray();
        int l = cnt.length;

        if (l == 0) {
            return ""; //$NON-NLS-1$
        }

        // if last character is whitespace, swallow it.
        // this is necessary for some inner templates to look ok.
        if (Character.isWhitespace(cnt[l - 1])) {
            l -= 1;
        }

        int lastIdx = 0;

        for (int i = 0; i < (l - 1); i++) {
            if ((cnt[i] == '<') && (cnt[i + 1] == '%')) {
                int j = i + 2;

                while ((j < (l - 1)) && ((cnt[j] != '%') || (cnt[j + 1] != '>'))) {
                    j++;
                }

                if (j > (i + 2)) {
                    if ((i - lastIdx) > 0) {
                        partBuffer.add(new HspBodyPart(new String(cnt, lastIdx,
                                                              i - lastIdx), true));
                    }

                    String script = new String(cnt, i + 2, (j - i) - 2);

                    partBuffer.add(new HspBodyPart(script, false));
                    lastIdx = j + 2;
                }

                i = j + 1;
            }
        }

        if (lastIdx < l) {
            partBuffer.add(new HspBodyPart(new String(cnt, lastIdx, l - lastIdx),
                                       true));
        }

        StringBuffer templateBody = new StringBuffer();
        int nparts = partBuffer.size();

        for (int k = 0; k < nparts; k++) {
            HspBodyPart nextPart = (HspBodyPart) partBuffer.get(k);

            if (nextPart.isStatic || nextPart.content.trim().startsWith("=")) { //$NON-NLS-1$
                // check for <%= ... %> statements
                if (!nextPart.isStatic) {
                    nextPart.content = nextPart.content.trim().substring(1).trim();

                    // cut trailing ";"
                    while (nextPart.content.endsWith(";")) //$NON-NLS-1$
                        nextPart.content = nextPart.content.substring(0,
                                                                      nextPart.content.length() -
                                                                      1);
                }

                StringTokenizer st = new StringTokenizer(nextPart.content, "\r\n", true); //$NON-NLS-1$
                String nextLine = st.hasMoreTokens() ? st.nextToken() : null;

                // count newLines we "swallow", see explanation below
                int newLineCount = 0;

                templateBody.append("res.write ("); //$NON-NLS-1$

                if (nextPart.isStatic) {
                    templateBody.append("\""); //$NON-NLS-1$
                }

                while (nextLine != null) {
                    if ("\n".equals(nextLine)) { //$NON-NLS-1$
                        // append a CRLF
                        newLineCount++;
                        templateBody.append("\\r\\n"); //$NON-NLS-1$
                    } else if (!"\r".equals(nextLine)) { //$NON-NLS-1$
                        try {
                            StringReader lineReader = new StringReader(nextLine);
                            int c = lineReader.read();

                            while (c > -1) {
                                if (nextPart.isStatic &&
                                        (((char) c == '"') || ((char) c == '\\'))) {
                                    templateBody.append('\\');
                                }

                                templateBody.append((char) c);
                                c = lineReader.read();
                            }
                        } catch (IOException srx) {
                        }
                    }

                    nextLine = st.hasMoreTokens() ? st.nextToken() : null;
                }

                if (nextPart.isStatic) {
                    templateBody.append("\""); //$NON-NLS-1$
                }

                templateBody.append("); "); //$NON-NLS-1$

                // append the number of lines we have "swallowed" into
                // one write statement, so error messages will *approximately*
                // give correct line numbers.
                for (int i = 0; i < newLineCount; i++) {
                    templateBody.append("\r\n"); //$NON-NLS-1$
                }
            } else {
                templateBody.append(nextPart.content);

                if (!nextPart.content.trim().endsWith(";")) { //$NON-NLS-1$
                    templateBody.append(";"); //$NON-NLS-1$
                }
            }
        }

        // templateBody.append ("\r\nreturn null;\r\n");
        return templateBody.toString();
    }

    static class HspBodyPart {
        String content;
        boolean isPart;
        boolean isStatic;

        public HspBodyPart(String content, boolean isStatic) {
            this.isPart = false;
            this.content = content;
            this.isStatic = isStatic;
        }

        public String getName() {
            return this.isStatic ? null : this.content;
        }

        @Override
        public String toString() {
            return "Template.Part [" + this.content + "," + this.isStatic + "]";   //$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$
        }
    }
}
