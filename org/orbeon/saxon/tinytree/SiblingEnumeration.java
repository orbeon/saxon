package org.orbeon.saxon.tinytree;
import org.orbeon.saxon.om.AxisIteratorImpl;
import org.orbeon.saxon.om.Item;
import org.orbeon.saxon.om.SequenceIterator;
import org.orbeon.saxon.om.LookaheadIterator;
import org.orbeon.saxon.pattern.NodeTest;
import org.orbeon.saxon.style.StandardNames;
import org.orbeon.saxon.type.Type;
import org.orbeon.saxon.value.UntypedAtomicValue;
import org.orbeon.saxon.value.StringValue;

/**
* This class supports both the child:: and following-sibling:: axes, which are
* identical except for the route to the first candidate node.
* It enumerates either the children or the following siblings of the specified node.
* In the case of children, the specified node must always
* be a node that has children: to ensure this, construct the enumeration
* using NodeInfo#getEnumeration()
*/

final class SiblingEnumeration extends AxisIteratorImpl implements LookaheadIterator {

    private TinyTree tree;
    private int nextNodeNr;
    private NodeTest test;
    private TinyNodeImpl startNode;
    private TinyNodeImpl parentNode;
    private boolean getChildren;
    private boolean needToAdvance = false;

    /**
     * Return an enumeration over children or siblings of the context node
     * @param tree The TinyTree containing the context node
     * @param node The context node, the start point for the iteration
     * @param nodeTest Test that the selected nodes must satisfy, or null indicating
     * that all nodes are selected
     * @param getChildren True if children of the context node are to be returned, false
     * if following siblings are required
     */

    SiblingEnumeration(TinyTree tree, TinyNodeImpl node,
                              NodeTest nodeTest, boolean getChildren) {
        this.tree = tree;
        test = nodeTest;
        startNode = node;
        this.getChildren = getChildren;
        if (getChildren) {          // child:: axis
            parentNode = node;
            // move to first child
            // ASSERT: we don't invoke this code unless the node has children
            nextNodeNr = node.nodeNr + 1;

        } else {                    // following-sibling:: axis
            parentNode = (TinyNodeImpl)node.getParent();
            if (parentNode == null) {
                nextNodeNr = -1;
            } else {
                // move to next sibling
                nextNodeNr = tree.next[node.nodeNr];
                while (tree.nodeKind[nextNodeNr] == Type.PARENT_POINTER) {
                    // skip dummy nodes
                    nextNodeNr = tree.next[nextNodeNr];
                }
                if (nextNodeNr < node.nodeNr) {
                    // if "next" pointer goes backwards, it's really an owner pointer from the last sibling
                    nextNodeNr = -1;
                }
            }
        }

        // check if this matches the conditions
        if (nextNodeNr >= 0 && nodeTest != null) {
            if (!nodeTest.matches(this.tree, nextNodeNr)) {
                needToAdvance = true;
            }
        }
    }

    public Item next() {
         // if needToAdvance == false, we are already on the correct node.
        if (needToAdvance) {
            final int thisNode = nextNodeNr;
            if (test==null) {
                do {
                    nextNodeNr = tree.next[nextNodeNr];
                } while (tree.nodeKind[nextNodeNr] == Type.PARENT_POINTER);
            } else {
                do {
                    nextNodeNr = tree.next[nextNodeNr];
                } while ( nextNodeNr >= thisNode &&
                        !test.matches(tree, nextNodeNr));
            }

            if (nextNodeNr < thisNode) {    // indicates we've got to the last sibling
                nextNodeNr = -1;
                needToAdvance = false;
                current = null;
                position = -1;
                return null;
            }
        }

        if (nextNodeNr == -1) {
            return null;
        }
        needToAdvance = true;
        position++;

        // if the caller only wants the atomized value, get it directly, to avoid creating the Node object

        if (isAtomizing()) {
            int kind = tree.nodeKind[nextNodeNr];
            switch (kind) {
                case Type.TEXT: {
                    return new UntypedAtomicValue(TinyTextImpl.getStringValue(tree, nextNodeNr));
                }
                case Type.WHITESPACE_TEXT: {
                    return new UntypedAtomicValue(WhitespaceTextImpl.getStringValue(tree, nextNodeNr));
                }
                case Type.ELEMENT: {
                    int type = tree.getTypeAnnotation(nextNodeNr);
                    switch (type) {
                        case StandardNames.XDT_UNTYPED:
                        case StandardNames.XDT_UNTYPED_ATOMIC:
                            current = tree.getAtomizedValueOfUntypedNode(nextNodeNr);
                            return current;
                        case StandardNames.XS_STRING:
                            current = new StringValue(TinyParentNodeImpl.getStringValue(tree, nextNodeNr));
                            return current;
                            // TODO:PERF support fast path for other simple types
                    }
                }
                default:
                    break;
            }
        }
        current = tree.getNode(nextNodeNr);
        ((TinyNodeImpl)current).setParentNode(parentNode);
        return current;

    }

    /**
     * Test whether there are any more nodes to come. This method is used only when testing whether the
     * current item is the last in the sequence. It's not especially efficient, but is more efficient than
     * the alternative strategy which involves counting how many nodes there are in the sequence.
     * @return true if there are more items in the sequence
     */

    public boolean hasNext() {
        int n = nextNodeNr;
        if (needToAdvance) {
            if (test==null) {
                do {
                    n = tree.next[n];
                } while (tree.nodeKind[n] == Type.PARENT_POINTER);
            } else {
                do {
                    n = tree.next[n];
                } while ( n >= nextNodeNr &&
                        !test.matches(tree, n));
            }

            if (n < nextNodeNr) {    // indicates we've got to the last sibling
                return false;
            }
        }

        return (n != -1);
    }

    /**
    * Get another enumeration of the same nodes
    */

    public SequenceIterator getAnother() {
        return new SiblingEnumeration(tree, startNode, test, getChildren);
    }

    public int getProperties() {
        return LOOKAHEAD;
    }

}


//
// The contents of this file are subject to the Mozilla Public License Version 1.0 (the "License");
// you may not use this file except in compliance with the License. You may obtain a copy of the
// License at http://www.mozilla.org/MPL/
//
// Software distributed under the License is distributed on an "AS IS" basis,
// WITHOUT WARRANTY OF ANY KIND, either express or implied.
// See the License for the specific language governing rights and limitations under the License.
//
// The Original Code is: all this file.
//
// The Initial Developer of the Original Code is Michael H. Kay.
//
// Portions created by (your name) are Copyright (C) (your legal entity). All Rights Reserved.
//
// Contributor(s): none.
//
