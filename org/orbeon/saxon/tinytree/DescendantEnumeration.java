package org.orbeon.saxon.tinytree;
import org.orbeon.saxon.om.AxisIteratorImpl;
import org.orbeon.saxon.om.Item;
import org.orbeon.saxon.om.SequenceIterator;
import org.orbeon.saxon.pattern.NodeTest;
import org.orbeon.saxon.style.StandardNames;

/**
* This class supports both the descendant:: and descendant-or-self:: axes, which are
* identical except for the route to the first candidate node.
* It enumerates descendants of the specified node.
* The calling code must ensure that the start node is not an attribute or namespace node.
*/

final class DescendantEnumeration extends AxisIteratorImpl {

    private TinyTree tree;
    private TinyNodeImpl startNode;
    private boolean includeSelf;
    private int nextNodeNr;
    private int startDepth;
    private NodeTest test;

    /**
     * Create an iterator over the descendant axis
     * @param doc the containing TinyTree
     * @param node the node whose descendants are required
     * @param nodeTest test to be satisfied by each returned node
     * @param includeSelf true if the start node is to be included
     */

    DescendantEnumeration(TinyTree doc, TinyNodeImpl node, NodeTest nodeTest, boolean includeSelf) {
        tree = doc;
        startNode = node;
        this.includeSelf = includeSelf;
        test = nodeTest;
        nextNodeNr = node.nodeNr;
        startDepth = doc.depth[nextNodeNr];
    }

    public Item next() {
        if (position==0 && includeSelf && test.matches(startNode)) {
            current = startNode;
            position++;
            return current;
        }

        do {
            nextNodeNr++;
            try {
                if (tree.depth[nextNodeNr] <= startDepth) {
                    nextNodeNr = -1;
                    current = null;
                    position = -1;
                    return null;
                }
            } catch (ArrayIndexOutOfBoundsException e) {
                // this shouldn't happen. If it does happen, it means the tree wasn't properly closed
                // during construction (there is no stopper node at the end). In this case, we'll recover
                // by returning end-of sequence
                nextNodeNr = -1;
                current = null;
                position = -1;
                return null;
            }
        } while (!test.matches(tree, nextNodeNr));

        position++;
        if (isAtomizing() && tree.getTypeAnnotation(nextNodeNr) == StandardNames.XDT_UNTYPED) {
            current = tree.getAtomizedValueOfUntypedNode(nextNodeNr);
        } else {
            current = tree.getNode(nextNodeNr);
        }

        return current;
    }

    /**
    * Get another enumeration of the same nodes
    */

    public SequenceIterator getAnother() {
        return new DescendantEnumeration(tree, startNode, test, includeSelf);
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
