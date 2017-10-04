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

package helma.util;


import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.StringTokenizer;

/**
 * Utility class for String manipulation.
 */
public class StringUtils {


    /**
     *  Split a string into an array of strings. Use comma and space
     *  as delimiters.
     */
    public static String[] split(String str) {
        return split(str, ", \t\n\r\f"); //$NON-NLS-1$
    }

    /**
     *  Split a string into an array of strings.
     */
    public static String[] split(String str, String delim) {
        if (str == null) {
            return new String[0];
        }
        StringTokenizer st = new StringTokenizer(str, delim);
        String[] s = new String[st.countTokens()];
        for (int i=0; i<s.length; i++) {
            s[i] = st.nextToken();
        }
        return s;
    }

    /**
     *  Split a string into an array of lines.
     *  @param str the string to split
     *  @return an array of lines
     */
    public static String[] splitLines(String str) {
        return str.split("\\r\\n|\\r|\\n"); //$NON-NLS-1$
    }

    /**
     * Get the character array for a string. Useful for use from
     * Rhino, where the Java String methods are not readily available
     * without constructing a new String instance.
     * @param str a string
     * @return the char array
     */
    public static char[] toCharArray(String str) {
        return str == null ? new char[0] : str.toCharArray();
    }

    /**
     * Collect items of a string enumeration into a String array.
     * @param en an enumeration of strings
     * @return the enumeration values as string array
     */
    public static String[] collect(Enumeration en) {
        List list = new ArrayList();
        while (en.hasMoreElements()) {
            list.add(en.nextElement());
        }
        return (String[]) list.toArray(new String[list.size()]);
    }

    /**
     * Get the largest common prefix of Strings s1 and s2
     * @param s1 a string
     * @param s2 another string
     * @return the largest prefix shared by both strings
     */
    public static String getCommonPrefix(String s1, String s2) {
        if (s1.indexOf(s2) == 0) {
            return s2;
        } else if (s2.indexOf(s1) == 0) {
            return s1;
        }
        int length = Math.min(s1.length(), s2.length());
        for (int i = 0; i < length; i++) {
            if (s1.charAt(i) != s2.charAt(i)) {
                return s1.substring(0, i);
            }
        }
        return s1.substring(0, length);
    }

}
