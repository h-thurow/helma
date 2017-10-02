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

import org.mozilla.javascript.tools.debugger.SwingGui;
import org.mozilla.javascript.tools.debugger.Dim;

import javax.swing.tree.*;
import javax.swing.*;
import javax.swing.event.TreeSelectionListener;
import javax.swing.event.TreeSelectionEvent;
import java.util.*;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.KeyAdapter;
import java.awt.*;

import helma.util.StringUtils;


public class HelmaDebugger extends Dim implements TreeSelectionListener {

    DebugGui gui;
    JTree tree;
    JList list;
    DebuggerTreeNode treeRoot;
    DefaultTreeModel treeModel;
    HashMap treeNodes = new HashMap();
    HashMap scriptNames = new HashMap();


    public HelmaDebugger(String title) {
        this.gui = new DebugGui(this, title);
        this.gui.pack();
        this.gui.setVisible(true);
    }

    void createTreeNode(String sourceName) {
        String[] path = StringUtils.split(sourceName, ":/\\"); //$NON-NLS-1$
        DebuggerTreeNode node = this.treeRoot;
        DebuggerTreeNode newNode = null;
        int start = Math.max(0, path.length - 3);
        for (int i = start; i < path.length; i++) {
            DebuggerTreeNode n = node.get(path[i]);
            if (n == null) {
                n = new DebuggerTreeNode(path[i]);
                node.add(n);
                if (newNode == null) newNode = n;
            }
            node = n;
        }
        this.treeNodes.put(sourceName, node);
        this.scriptNames.put(node, sourceName);
        if (newNode != null) {
            SwingUtilities.invokeLater(new NodeInserter(newNode));
        }
    }

    void openScript(TreePath path) {
        if (path == null)
            return;
        Object node = path.getLastPathComponent();
        if (node == null)
            return;
        String sourceName = (String) this.scriptNames.get(node);
        if (sourceName == null)
            return;
        this.gui.showFileWindow(sourceName, -1);
    }

    void openFunction(FunctionItem function) {
        if (function == null)
            return;
        FunctionSource src = function.src;
        if (src != null) {
            SourceInfo si = src.sourceInfo();
            String url = si.url();
            int lineNumber = src.firstLine();
            this.gui.showFileWindow(url, lineNumber);
        }
    }



   public void valueChanged(TreeSelectionEvent e) {
        DefaultMutableTreeNode node = (DefaultMutableTreeNode)
                this.tree.getLastSelectedPathComponent();

        if (node == null) return;

        Object script = this.scriptNames.get(node);
        if (script != null) {
            // openScript(script);
        }
    }

    public void setVisible(boolean visible) {
        this.gui.setVisible(visible);
    }

    @Override
    public void dispose() {
        super.dispose();
        this.gui.setVisible(false);
        // Calling dispose() on the gui causes shutdown hook to hang on windows -
        // see http://helma.org/bugs/show_bug.cgi?id=586#c2
        // gui.dispose();
    }

    class DebuggerTreeNode extends DefaultMutableTreeNode {

        private static final long serialVersionUID = -5881442554351749706L;

        public DebuggerTreeNode(Object obj) {
            super(obj);
        }

        public DebuggerTreeNode get(String name) {
            Enumeration children = this.children();
            while (children.hasMoreElements()) {
                DebuggerTreeNode node = (DebuggerTreeNode) children.nextElement();
                if (node != null && name.equals(node.getUserObject()))
                    return node;
            }
            return null;
        }
    }

    class NodeInserter implements Runnable {
        MutableTreeNode node;

        NodeInserter(MutableTreeNode node) {
            this.node = node;
        }

        public void run() {
            MutableTreeNode parent = (MutableTreeNode) this.node.getParent();
            if (parent == HelmaDebugger.this.treeRoot && HelmaDebugger.this.treeRoot.getChildCount() == 1) {
                HelmaDebugger.this.tree.makeVisible(new TreePath(new Object[]{parent, this.node}));
            }
            HelmaDebugger.this.treeModel.insertNodeInto(this.node, parent, parent.getIndex(this.node));
        }
    }

    class DebugGui extends SwingGui {

        private static final long serialVersionUID = 8930558796272502640L;

        String currentSourceUrl;

        public DebugGui(Dim dim, String title) {
            super(dim, title);
            Container contentPane = getContentPane();
            Component main = contentPane.getComponent(1);
            contentPane.remove(main);

            HelmaDebugger.this.treeRoot = new DebuggerTreeNode(title);
            HelmaDebugger.this.tree = new JTree(HelmaDebugger.this.treeRoot);
            HelmaDebugger.this.treeModel = new DefaultTreeModel(HelmaDebugger.this.treeRoot);
            HelmaDebugger.this.tree.setModel(HelmaDebugger.this.treeModel);
            HelmaDebugger.this.tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
            HelmaDebugger.this.tree.addTreeSelectionListener(HelmaDebugger.this);
            // tree.setRootVisible(false);
            // track double clicks
            HelmaDebugger.this.tree.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent evt) {
                    openScript(HelmaDebugger.this.tree.getSelectionPath());
                }
            });
            // track enter key
            HelmaDebugger.this.tree.addKeyListener(new KeyAdapter() {
                @Override
                public void keyPressed(KeyEvent evt) {
                    if (evt.getKeyCode() == KeyEvent.VK_ENTER)
                        openScript(HelmaDebugger.this.tree.getSelectionPath());
                }
            });
            JScrollPane treeScroller = new JScrollPane(HelmaDebugger.this.tree);
            treeScroller.setPreferredSize(new Dimension(180, 300));

            HelmaDebugger.this.list = new JList();
            // no bold font lists for me, thanks
            HelmaDebugger.this.list.setFont(HelmaDebugger.this.list.getFont().deriveFont(Font.PLAIN));
            HelmaDebugger.this.list.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent evt) {
                    openFunction((FunctionItem) HelmaDebugger.this.list.getSelectedValue());
                }
            });
            HelmaDebugger.this.list.addKeyListener(new KeyAdapter() {
                @Override
                public void keyPressed(KeyEvent evt) {
                    if (evt.getKeyCode() == KeyEvent.VK_ENTER)
                        openFunction((FunctionItem) HelmaDebugger.this.list.getSelectedValue());
                }
            });
            JScrollPane listScroller = new JScrollPane(HelmaDebugger.this.list);
            listScroller.setPreferredSize(new Dimension(180, 200));

            JSplitPane split1 = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
            split1.setTopComponent(treeScroller);
            split1.setBottomComponent(listScroller);
            split1.setOneTouchExpandable(true);
            split1.setResizeWeight(0.66);

            JSplitPane split2 = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
            split2.setLeftComponent(split1);
            split2.setRightComponent(main);
            split2.setOneTouchExpandable(true);
            contentPane.add(split2, BorderLayout.CENTER);
        }

        @Override
        public void updateSourceText(final Dim.SourceInfo sourceInfo) {
            // super.updateSourceText(sourceInfo);
            String filename = sourceInfo.url();
            if (!HelmaDebugger.this.treeNodes.containsKey(filename)) {
                createTreeNode(filename);
            }
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    updateFileWindow(sourceInfo);
                }
            });
        }

        @Override
        protected void showFileWindow(String sourceName, int lineNumber) {
            if (!isVisible())
                setVisible(true);
            if (!sourceName.equals(this.currentSourceUrl)) {
                updateFunctionList(sourceName);
                DebuggerTreeNode node = (DebuggerTreeNode) HelmaDebugger.this.treeNodes.get(sourceName);
                if (node != null) {
                    TreePath path = new TreePath(node.getPath());
                    HelmaDebugger.this.tree.setSelectionPath(path);
                    HelmaDebugger.this.tree.scrollPathToVisible(path);
                }
            }
            super.showFileWindow(sourceName, lineNumber);
        }

        private void updateFunctionList(String sourceName) {
            // display functions for opened script file
            this.currentSourceUrl = sourceName;
            Vector functions = new Vector();
            SourceInfo si = sourceInfo(sourceName);
            String[] lines = si.source().split("\\r\\n|\\r|\\n"); //$NON-NLS-1$
            int length = si.functionSourcesTop();
            for (int i = 0; i < length; i++) {
                FunctionSource src = si.functionSource(i);
                if (sourceName.equals(src.sourceInfo().url())) {
                    functions.add(new FunctionItem(src, lines));
                }
            }
            // Collections.sort(functions);
            HelmaDebugger.this.list.setListData(functions);
        }
    }

    class FunctionItem implements Comparable {

        FunctionSource src;
        String name;
        String line = ""; //$NON-NLS-1$

        FunctionItem(FunctionSource src, String[] lines) {
            this.src = src;
            this.name = src.name();
            if ("".equals(this.name)) { //$NON-NLS-1$
                try {
                    this.line = lines[src.firstLine() - 1];
                    int f = this.line.indexOf("function") - 1; //$NON-NLS-1$
                    StringBuffer b = new StringBuffer();
                    boolean assignment = false;
                    while (f-- > 0) {
                        char c = this.line.charAt(f);
                        if (c == ':' || c == '=')
                            assignment = true;
                        else if (assignment && Character.isJavaIdentifierPart(c)
                                || c == '$' || c == '.')
                            b.append(c);
                        else if (!Character.isWhitespace(c) || b.length() > 0)
                            break;
                    }
                    this.name = b.length() > 0 ? b.reverse().toString() : "<anonymous>"; //$NON-NLS-1$
                } catch (Exception x) {
                    // fall back to default name
                    this.name = "<anonymous>"; //$NON-NLS-1$
                }
            }
        }

        public int compareTo(Object o) {
            FunctionItem other = (FunctionItem) o;
            return this.name.compareTo(other.name);
        }

        @Override
        public String toString() {
            return this.name;
        }

    }
}
