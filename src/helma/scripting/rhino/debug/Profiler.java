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

package helma.scripting.rhino.debug;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Formatter;
import java.util.HashMap;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.debug.DebugFrame;
import org.mozilla.javascript.debug.DebuggableScript;
import org.mozilla.javascript.debug.Debugger;

import helma.util.StringUtils;

public class Profiler implements Debugger {

    HashMap frames = new HashMap();

    /**
     * Create a new profiler.
     */
    public Profiler() {}

    /**
     * Implementws handleCompilationDone in interface org.mozilla.javascript.debug.Debugger
     */
    public void handleCompilationDone(Context cx, DebuggableScript script, String source) {}

    /**
     *  Implements getFrame() in interface org.mozilla.javascript.debug.Debugger
     */
    public DebugFrame getFrame(Context cx, DebuggableScript script) {
        if (script.isFunction()) {
            String name = getFunctionName(script);
            ProfilerFrame frame = (ProfilerFrame) this.frames.get(name);
            if (frame == null) {
                frame = new ProfilerFrame(name);
                this.frames.put(name, frame);
            }
            return frame;
        }
        return null;
    }

    /**
     * Get a string representation for the given script
     * @param script a function or script
     * @return the file and/or function name of the script
     */
    static String getFunctionName(DebuggableScript script) {
        if (script.isFunction()) {
            StringBuffer b = new StringBuffer(script.getSourceName()).append(" #"); //$NON-NLS-1$
            int[] lineNumbers = script.getLineNumbers();
            b.append(lineNumbers.length > 0 ? lineNumbers[0] : "?");
            String funcName = script.getFunctionName();
            if (funcName != null && funcName.length() > 0) {
                b.append(": ").append(script.getFunctionName()); //$NON-NLS-1$
            }
            return b.toString();
        }
        return script.getSourceName();
    }

    public String getResult() {
        ProfilerFrame[] f = (ProfilerFrame[]) this.frames.values().toArray(new ProfilerFrame[0]);
        Arrays.sort(f, new Comparator() {
            public int compare(Object o1, Object o2) {
                return ((ProfilerFrame)o2).runtime - ((ProfilerFrame)o1).runtime;
            }
        });
        int length = Math.min(100, f.length);
        int prefixLength = length < 2 ? 0 : Integer.MAX_VALUE;
        int maxLength = 0;
        for (int i = 0; i < length; i++) {
            maxLength = Math.max(maxLength, f[i].name.length());
            if (i < length - 1) {
                prefixLength = Math.min(prefixLength,
                    StringUtils.getCommonPrefix(f[i].name, f[i+1].name).length());
            }
        }
        maxLength = maxLength + 30 - prefixLength;
        StringBuffer buffer = new StringBuffer(Messages.getString("Profiler.0")); //$NON-NLS-1$
        for (int i = 0; i < maxLength; i++) {
            buffer.append('-');
        }
        buffer.append('\n');
        for (int i = 0; i < length; i++) {
            buffer.append(f[i].renderLine(prefixLength));
        }
        return buffer.toString();
    }

    class ProfilerFrame implements DebugFrame {

        int runtime, invocations, lineNumber, recursion;
        long start;
        String name;

        ProfilerFrame(String name) {
            this.name = name;
        }

        /**
         * Called when execution is ready to start bytecode interpretation
         * for entered a particular function or script.
         */
        public void onEnter(Context cx, Scriptable activation,
                            Scriptable thisObj, Object[] args) {

            if (recursion == 0) {
                start = System.nanoTime();
            }
            recursion++;
        }

        /**
         *  Called when thrown exception is handled by the function or script.
         */
        public void onExceptionThrown(Context cx, Throwable ex) {
            // TODO: figure out if this is called in addition or in place of to onExit
        }

        /**
         *  Called when the function or script for this frame is about to return.
         */
        public void onExit(Context cx, boolean byThrow, Object resultOrException) {
            this.invocations++;
            if (--recursion == 0) {
                runtime += System.nanoTime() - start;
            }
        }

        /**
         * Called when the function or script executes a 'debugger' statement.
         *
         * @param cx current Context for this thread
         */
        public void onDebuggerStatement(Context cx) {
            // not implemented
        }

        /**
         *  Called when executed code reaches new line in the source.
         */
        public void onLineChange(Context cx, int lineNumber) {
            this.lineNumber = lineNumber;
        }

        public String renderLine(int prefixLength) {
            double millis = this.runtime / 1000000.0;
            Formatter formatter = new java.util.Formatter();
            Object[] args = new Object[] {
                    Double.valueOf(millis),
                    Double.valueOf(millis / this.invocations),
                    Integer.valueOf(this.invocations),
                    name.substring(prefixLength)
            };
            formatter.format("%9.3f ms %9.3f ms %6d    %s%n", args); //$NON-NLS-1$
            String formattedLine = formatter.toString();
            formatter.close();
            return formattedLine;
        }
    }
}
