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
            b.append(script.getLineNumbers()[0]);
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

        Stack timer = new Stack();
        int runtime, invocations, lineNumber;
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

            long time = System.nanoTime();
            this.timer.push(new Long(time));
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
            this.invocations ++;
            Long time = (Long) this.timer.pop();
            this.runtime += System.nanoTime() - time.longValue();
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
            long millis = Math.round(this.runtime / 1000000);
            Formatter formatter = new java.util.Formatter();
            Object[] args = new Object[] {
                    Integer.valueOf((int) millis),
                    Integer.valueOf(Math.round(millis / this.invocations)),
                    Integer.valueOf(this.invocations),
                    this.name.substring(prefixLength)
            };
            formatter.format("%1$7d ms %2$5d ms %3$6d    %4$s%n", args); //$NON-NLS-1$
            return formatter.toString();
        }
    }
}
