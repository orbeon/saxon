package org.orbeon.saxon.tinytree;
import org.orbeon.saxon.om.AxisIteratorImpl;
import org.orbeon.saxon.om.Item;
import org.orbeon.saxon.om.SequenceIterator;
import org.orbeon.saxon.pattern.NodeTest;

/**
* Enumerate all the nodes on the preceding axis from a given start node.
* The calling code ensures that the start node is not a root, attribute,
* or namespace node. As well as the standard XPath preceding axis, this
* class also implements a Saxon-specific "preceding-or-ancestor" axis
* which returns ancestor nodes as well as preceding nodes. This is used
* when performing xsl:number level="any".
*/

final class PrecedingEnumeration extends AxisIteratorImpl {

    private TinyTree tree;
    private TinyNodeImpl startNode;
    private NodeTest test;
    private int nextAncestorDepth;
    private boolean includeAncestors;

    public PrecedingEnumeration(TinyTree doc, TinyNodeImpl node,
                                NodeTest nodeTest, boolean includeAncestors) {

        this.includeAncestors = includeAncestors;
        test = nodeTest;
        tree = doc;
        startNode = node;
        current = startNode;
        nextAncestorDepth = doc.depth[node.nodeNr] - 1;
    }

    public Item next() {
        int nextNodeNr = ((TinyNodeImpl)current).nodeNr;
        while (true) {
            if (!includeAncestors) {
                nextNodeNr--;
                // skip over ancestor elements
                while (nextAncestorDepth >= 0 && tree.depth[nextNodeNr] == nextAncestorDepth) {
                    if (nextAncestorDepth-- <= 0) {  // bug 1121528
                        current = null;
                        position = -1;
                        return null;
                    }
                    nextNodeNr--;
                }
            } else {
                if (tree.depth[nextNodeNr] == 0) {
                    current = null;
                    position = -1;
                    return null;
                } else {
                    nextNodeNr--;
                }
            }
            if (test.matches(tree, nextNodeNr)) {
                position++;
                current = tree.getNode(nextNodeNr);
                return current;
            }
            if (tree.depth[nextNodeNr] == 0) {
                current = null;
                position = -1;
                return null;
            }
        }
    }

    /**
    * Get another enumeration of the same nodes
    */

    public SequenceIterator getAnother() {
        return new PrecedingEnumeration(tree, startNode, test, includeAncestors);
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
