package helma.scripting.rhino.debug;

import org.mozilla.javascript.debug.Debugger;
import org.mozilla.javascript.debug.DebuggableScript;
import org.mozilla.javascript.debug.DebugFrame;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;

import java.util.*;

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
            ProfilerFrame frame = (ProfilerFrame) frames.get(name);
            if (frame == null) {
                frame = new ProfilerFrame(name);
                frames.put(name, frame);
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
            StringBuffer b = new StringBuffer(script.getSourceName()).append(" #");
            b.append(script.getLineNumbers()[0]);
            String funcName = script.getFunctionName();
            if (funcName != null && funcName.length() > 0) {
                b.append(": ").append(script.getFunctionName());
            }
            return b.toString();
        } else {
            return script.getSourceName();
        }
    }

    public String getResult() {
        ProfilerFrame[] f = (ProfilerFrame[]) frames.values().toArray(new ProfilerFrame[0]);
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
        StringBuffer buffer = new StringBuffer("       total      average  calls    path\n");
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
            invocations++;
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
                    Double.valueOf(millis / invocations),
                    Integer.valueOf(invocations),
                    name.substring(prefixLength)
            };
            formatter.format("%9.3f ms %9.3f ms %6d    %s%n", args);
            return formatter.toString();
        }
    }
}
