/*
Copyright (c) 2009, incava.org
All rights reserved.

Redistribution and use in source and binary forms, with or without modification,
are permitted provided that the following conditions are met:

    * Redistributions of source code must retain the above copyright notice,
    * this list of conditions and the following disclaimer.

    * Redistributions in binary form must reproduce the above copyright notice,
    * this list of conditions and the following disclaimer in the documentation
    * and/or other materials provided with the distribution.

    * Neither the name of incava.org nor the names of its contributors may be
    * used to endorse or promote products derived from this software without
    * specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
(INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package helma.util;

import java.util.*;


/**
 * Compares two lists, returning a list of the additions, changes, and deletions
 * between them. A <code>Comparator</code> may be passed as an argument to the
 * constructor, and will thus be used. If not provided, the initial value in the
 * <code>a</code> ("from") list will be looked at to see if it supports the
 * <code>Comparable</code> interface. If so, its <code>equals</code> and
 * <code>compareTo</code> methods will be invoked on the instances in the "from"
 * and "to" lists; otherwise, for speed, hash codes from the objects will be
 * used instead for comparison.
 *
 * <p>The file FileDiff.java shows an example usage of this class, in an
 * application similar to the Unix "diff" program.</p>
 */
public class Diff
{
    /**
     * The source list, AKA the "from" values.
     */
    protected List a;

    /**
     * The target list, AKA the "to" values.
     */
    protected List b;

    /**
     * The list of differences, as <code>Difference</code> instances.
     */
    protected List diffs = new ArrayList();

    /**
     * The pending, uncommitted difference.
     */
    private Difference pending;

    /**
     * The comparator used, if any.
     */
    private Comparator comparator;

    /**
     * The thresholds.
     */
    private TreeMap thresh;

    /**
     * Constructs the Diff object for the two arrays, using the given comparator.
     */
    public Diff(Object[] a, Object[] b, Comparator comp)
    {
        this(Arrays.asList(a), Arrays.asList(b), comp);
    }

    /**
     * Constructs the Diff object for the two arrays, using the default
     * comparison mechanism between the objects, such as <code>equals</code> and
     * <code>compareTo</code>.
     */
    public Diff(Object[] a, Object[] b)
    {
        this(a, b, null);
    }

    /**
     * Constructs the Diff object for the two lists, using the given comparator.
     */
    public Diff(List a, List b, Comparator comp)
    {
        this.a = a;
        this.b = b;
        this.comparator = comp;
        this.thresh = null;
    }

    /**
     * Constructs the Diff object for the two lists, using the default
     * comparison mechanism between the objects, such as <code>equals</code> and
     * <code>compareTo</code>.
     */
    public Diff(List a, List b)
    {
        this(a, b, null);
    }

    /**
     * Runs diff and returns the results.
     */
    public Change diff()
    {
        traverseSequences();

        // add the last difference, if pending:
        if (this.pending != null) {
            this.diffs.add(this.pending);
        }

        return Change.fromList(this.diffs);
    }

    /**
     * Traverses the sequences, seeking the longest common subsequences,
     * invoking the methods <code>finishedA</code>, <code>finishedB</code>,
     * <code>onANotB</code>, and <code>onBNotA</code>.
     */
    protected void traverseSequences()
    {
        Integer[] matches = getLongestCommonSubsequences();

        int lastA = this.a.size() - 1;
        int lastB = this.b.size() - 1;
        int bi = 0;
        int ai;

        int lastMatch = matches.length - 1;
        
        for (ai = 0; ai <= lastMatch; ++ai) {
            Integer bLine = matches[ai];

            if (bLine == null) {
                onANotB(ai, bi);
            }
            else {
                while (bi < bLine.intValue()) {
                    onBNotA(ai, bi++);
                }

                onMatch(ai, bi++);
            }
        }

        boolean calledFinishA = false;
        boolean calledFinishB = false;

        while (ai <= lastA || bi <= lastB) {

            // last A?
            if (ai == lastA + 1 && bi <= lastB) {
                if (!calledFinishA && callFinishedA()) {
                    finishedA(lastA);
                    calledFinishA = true;
                }
                else {
                    while (bi <= lastB) {
                        onBNotA(ai, bi++);
                    }
                }
            }

            // last B?
            if (bi == lastB + 1 && ai <= lastA) {
                if (!calledFinishB && callFinishedB()) {
                    finishedB(lastB);
                    calledFinishB = true;
                }
                else {
                    while (ai <= lastA) {
                        onANotB(ai++, bi);
                    }
                }
            }

            if (ai <= lastA) {
                onANotB(ai++, bi);
            }

            if (bi <= lastB) {
                onBNotA(ai, bi++);
            }
        }
    }

    /**
     * Override and return true in order to have <code>finishedA</code> invoked
     * at the last element in the <code>a</code> array.
     */
    protected boolean callFinishedA()
    {
        return false;
    }

    /**
     * Override and return true in order to have <code>finishedB</code> invoked
     * at the last element in the <code>b</code> array.
     */
    protected boolean callFinishedB()
    {
        return false;
    }

    /**
     * Invoked at the last element in <code>a</code>, if
     * <code>callFinishedA</code> returns true.
     */
    protected void finishedA(int lastA)
    {
    }

    /**
     * Invoked at the last element in <code>b</code>, if
     * <code>callFinishedB</code> returns true.
     */
    protected void finishedB(int lastB)
    {
    }

    /**
     * Invoked for elements in <code>a</code> and not in <code>b</code>.
     */
    protected void onANotB(int ai, int bi)
    {
        if (this.pending == null) {
            this.pending = new Difference(ai, ai, bi, -1);
        }
        else {
            this.pending.setDeleted(ai);
        }
    }

    /**
     * Invoked for elements in <code>b</code> and not in <code>a</code>.
     */
    protected void onBNotA(int ai, int bi)
    {
        if (this.pending == null) {
            this.pending = new Difference(ai, -1, bi, bi);
        }
        else {
            this.pending.setAdded(bi);
        }
    }

    /**
     * Invoked for elements matching in <code>a</code> and <code>b</code>.
     */
    protected void onMatch(int ai, int bi)
    {
        if (this.pending == null) {
            // no current pending
        }
        else {
            this.diffs.add(this.pending);
            this.pending = null;
        }
    }

    /**
     * Compares the two objects, using the comparator provided with the
     * constructor, if any.
     */
    protected boolean equals(Object x, Object y)
    {
        return this.comparator == null ? x.equals(y) : this.comparator.compare(x, y) == 0;
    }
    
    /**
     * Returns an array of the longest common subsequences.
     */
    public Integer[] getLongestCommonSubsequences()
    {
        int aStart = 0;
        int aEnd = this.a.size() - 1;

        int bStart = 0;
        int bEnd = this.b.size() - 1;

        TreeMap matches = new TreeMap();

        while (aStart <= aEnd && bStart <= bEnd && equals(this.a.get(aStart), this.b.get(bStart))) {
            matches.put(Integer.valueOf(aStart++), Integer.valueOf(bStart++));
        }

        while (aStart <= aEnd && bStart <= bEnd && equals(this.a.get(aEnd), this.b.get(bEnd))) {
            matches.put(Integer.valueOf(aEnd--), Integer.valueOf(bEnd--));
        }

        Map bMatches = null;
        if (this.comparator == null) {
            if (this.a.size() > 0 && this.a.get(0) instanceof Comparable) {
                // this uses the Comparable interface
                bMatches = new TreeMap();
            }
            else {
                // this just uses hashCode()
                bMatches = new HashMap();
            }
        }
        else {
            // we don't really want them sorted, but this is the only Map
            // implementation (as of JDK 1.4) that takes a comparator.
            bMatches = new TreeMap(this.comparator);
        }

        for (int bi = bStart; bi <= bEnd; ++bi) {
            Object         element    = this.b.get(bi);
            Object          key       = element;
            List positions = (List) bMatches.get(key);
            
            if (positions == null) {
                positions = new ArrayList();
                bMatches.put(key, positions);
            }
            
            positions.add(Integer.valueOf(bi));
        }

        this.thresh = new TreeMap();
        Map links = new HashMap();

        for (int i = aStart; i <= aEnd; ++i) {
            Object aElement  = this.a.get(i);
            List positions = (List) bMatches.get(aElement);

            if (positions != null) {
                Integer  k   = Integer.valueOf(0);
                ListIterator pit = positions.listIterator(positions.size());
                while (pit.hasPrevious()) {
                    Integer j = (Integer) pit.previous();

                    k = insert(j, k);

                    if (k == null) {
                        // nothing
                    }
                    else {
                        Object value = k.intValue() > 0 ? links.get(Integer.valueOf(k.intValue() - 1)) : null;
                        links.put(k, new Object[] { value, Integer.valueOf(i), j });
                    }   
                }
            }
        }

        if (this.thresh.size() > 0) {
            Integer  ti   = (Integer) this.thresh.lastKey();
            Object[] link = (Object[])links.get(ti);
            while (link != null) {
                Integer x = (Integer)link[1];
                Integer y = (Integer)link[2];
                matches.put(x, y);
                link = (Object[])link[0];
            }
        }

        int       size = matches.size() == 0 ? 0 : 1 + ((Integer) matches.lastKey()).intValue();
        Integer[] ary  = new Integer[size];
        for (Iterator it = matches.keySet().iterator(); it.hasNext();) {
            Integer idx = (Integer) it.next();
            Integer val = (Integer) matches.get(idx);
            ary[idx.intValue()] = val;
        }
        return ary;
    }

    /**
     * Returns whether the integer is not zero (including if it is not null).
     */
    protected static boolean isNonzero(Integer i)
    {
        return i != null && i.intValue() != 0;
    }

    /**
     * Returns whether the value in the map for the given index is greater than
     * the given value.
     */
    protected boolean isGreaterThan(Integer index, Integer val)
    {
        Integer lhs = (Integer) this.thresh.get(index);
        return lhs != null && val != null && lhs.compareTo(val) > 0;
    }

    /**
     * Returns whether the value in the map for the given index is less than
     * the given value.
     */
    protected boolean isLessThan(Integer index, Integer val)
    {
        Integer lhs = (Integer) this.thresh.get(index);
        return lhs != null && (val == null || lhs.compareTo(val) < 0);
    }

    /**
     * Returns the value for the greatest key in the map.
     */
    protected Integer getLastValue()
    {
        return (Integer) this.thresh.get(this.thresh.lastKey());
    }

    /**
     * Adds the given value to the "end" of the threshold map, that is, with the
     * greatest index/key.
     */
    protected void append(Integer value)
    {
        Integer addIdx = null;
        if (this.thresh.size() == 0) {
            addIdx = Integer.valueOf(0);
        }
        else {
            Integer lastKey = (Integer) this.thresh.lastKey();
            addIdx = Integer.valueOf(lastKey.intValue() + 1);
        }
        this.thresh.put(addIdx, value);
    }

    /**
     * Inserts the given values into the threshold map.
     */
    protected Integer insert(Integer j, Integer k)
    {
        if (isNonzero(k) && isGreaterThan(k, j) && isLessThan(Integer.valueOf(k.intValue() - 1), j)) {
            this.thresh.put(k, j);
        }
        else {
            int high = -1;
            
            if (isNonzero(k)) {
                high = k.intValue();
            }
            else if (this.thresh.size() > 0) {
                high = ((Integer) this.thresh.lastKey()).intValue();
            }

            // off the end?
            if (high == -1 || j.compareTo(getLastValue()) > 0) {
                append(j);
                k = Integer.valueOf(high + 1);
            }
            else {
                // binary search for insertion point:
                int low = 0;
        
                while (low <= high) {
                    int     index = (high + low) / 2;
                    Integer val   = (Integer) this.thresh.get(Integer.valueOf(index));
                    int     cmp   = j.compareTo(val);

                    if (cmp == 0) {
                        return null;
                    }
                    else if (cmp > 0) {
                        low = index + 1;
                    }
                    else {
                        high = index - 1;
                    }
                }
        
                this.thresh.put(Integer.valueOf(low), j);
                k = Integer.valueOf(low);
            }
        }

        return k;
    }

    /**
     * This is the orignal class used for representation of a single change.
     * We replace this with our own {@link Change} class for reasons of compatibility
     * and easier scripting. 
     */
    class Difference
    {
        public static final int NONE = -1;

        /**
         * The point at which the deletion starts.
         */
        private int delStart = NONE;

        /**
         * The point at which the deletion ends.
         */
        private int delEnd = NONE;

        /**
         * The point at which the addition starts.
         */
        private int addStart = NONE;

        /**
         * The point at which the addition ends.
         */
        private int addEnd = NONE;

        /**
         * Creates the difference for the given start and end points for the
         * deletion and addition.
         */
        public Difference(int delStart, int delEnd, int addStart, int addEnd)
        {
            this.delStart = delStart;
            this.delEnd   = delEnd;
            this.addStart = addStart;
            this.addEnd   = addEnd;
        }

        /**
         * The point at which the deletion starts, if any. A value equal to
         * <code>NONE</code> means this is an addition.
         */
        public int getDeletedStart()
        {
            return this.delStart;
        }

        /**
         * The point at which the deletion ends, if any. A value equal to
         * <code>NONE</code> means this is an addition.
         */
        public int getDeletedEnd()
        {
            return this.delEnd;
        }

        /**
         * The point at which the addition starts, if any. A value equal to
         * <code>NONE</code> means this must be an addition.
         */
        public int getAddedStart()
        {
            return this.addStart;
        }

        /**
         * The point at which the addition ends, if any. A value equal to
         * <code>NONE</code> means this must be an addition.
         */
        public int getAddedEnd()
        {
            return this.addEnd;
        }

        /**
         * Sets the point as deleted. The start and end points will be modified to
         * include the given line.
         */
        public void setDeleted(int line)
        {
            this.delStart = Math.min(line, this.delStart);
            this.delEnd   = Math.max(line, this.delEnd);
        }

        /**
         * Sets the point as added. The start and end points will be modified to
         * include the given line.
         */
        public void setAdded(int line)
        {
            this.addStart = Math.min(line, this.addStart);
            this.addEnd   = Math.max(line, this.addEnd);
        }

        /**
         * Compares this object to the other for equality. Both objects must be of
         * type Difference, with the same starting and ending points.
         */
        @Override
        public boolean equals(Object obj)
        {
            if (obj instanceof Difference) {
                Difference other = (Difference)obj;

                return (this.delStart == other.delStart &&
                        this.delEnd   == other.delEnd &&
                        this.addStart == other.addStart &&
                        this.addEnd   == other.addEnd);
            }
            return false;
        }

        /**
         * Returns a string representation of this difference.
         */
        @Override
        public String toString()
        {
            StringBuffer buf = new StringBuffer();
            buf.append("del: [" + this.delStart + ", " + this.delEnd + "]"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            buf.append(" "); //$NON-NLS-1$
            buf.append("add: [" + this.addStart + ", " + this.addEnd + "]");  //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$
            return buf.toString();
        }

    }


    /**
     * A legacy adapter that is compatible to the interface of the old GPL licenced Diff.
     */
    public static class Change {

        public final Change link;
        public final int line0;
        public final int line1;
        public final int inserted;
        public final int deleted;

        public static Change fromList(List diffs) {
            Iterator iter = diffs.iterator();
            return iter.hasNext() ? new Change(iter, 0, 0) : null;
        }

        private Change(Iterator iter, int prev0, int prev1) {
            Difference diff = (Difference) iter.next();
            if (diff.getDeletedEnd() == Difference.NONE) {
                this.line0 = prev0 + diff.getAddedStart() - prev1;
                this.deleted = 0;
            } else {
                this.line0 = diff.getDeletedStart();
                this.deleted = diff.getDeletedEnd() - this.line0 + 1;
            }
            if (diff.getAddedEnd() == Difference.NONE) {
                this.line1 = prev1 + diff.getDeletedStart() - prev0;
                this.inserted = 0;
            } else {
                this.line1 = diff.getAddedStart();
                this.inserted = diff.getAddedEnd() - this.line1 + 1;
            }
            this.link = iter.hasNext() ? new Change(iter, this.line0 + this.deleted, this.line1 + this.inserted) : null;
        }
    }
}

