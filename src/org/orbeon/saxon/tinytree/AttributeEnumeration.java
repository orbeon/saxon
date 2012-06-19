package org.orbeon.saxon.tinytree;
import org.orbeon.saxon.om.*;
import org.orbeon.saxon.pattern.NameTest;
import org.orbeon.saxon.pattern.NodeTest;
import org.orbeon.saxon.om.StandardNames;
import org.orbeon.saxon.type.Type;
import org.orbeon.saxon.value.Value;
import org.orbeon.saxon.value.UntypedAtomicValue;
import org.orbeon.saxon.value.StringValue;
import org.orbeon.saxon.trans.XPathException;

/**
* AttributeEnumeration is an iterator over all the attribute nodes of an Element.
*/

final class AttributeEnumeration extends AxisIteratorImpl {

    private TinyTree tree;
    private int element;
    private NodeTest nodeTest;
    private int index;
    private int currentNodeNr;

    /**
    * Constructor. Note: this constructor will only be called if the relevant node
    * is an element and if it has one or more attributes. Otherwise an EmptyEnumeration
    * will be constructed instead.
    * @param tree: the containing TinyTree
    * @param element: the node number of the element whose attributes are required
    * @param nodeTest: condition to be applied to the names of the attributes selected
    */

    AttributeEnumeration(TinyTree tree, int element, NodeTest nodeTest) {

        this.nodeTest = nodeTest;
        this.tree = tree;
        this.element = element;
        index = tree.alpha[element];
        currentNodeNr = -1;
    }

    /**
    * Move to the next node in the iteration.
    */

    public boolean moveNext() {
        while (true) {
            if (index >= tree.numberOfAttributes || tree.attParent[index] != element) {
                index = Integer.MAX_VALUE;
                current = null;
                position = -1;
                currentNodeNr = -1;
                return false;
            }
            int typeCode = tree.getAttributeAnnotation(index); 
            if ((typeCode & NodeInfo.IS_DTD_TYPE) != 0) {
                typeCode = StandardNames.XS_UNTYPED_ATOMIC;
            }
            if (nodeTest.matches(Type.ATTRIBUTE, tree.attCode[index], typeCode)) {
                position++;
                currentNodeNr = index++;
                if (nodeTest instanceof NameTest) {
                    // there can only be one match, so abandon the search after this node
                    index = Integer.MAX_VALUE;
                }
                current = null;
                return true;
            }
            index++;
        }
    }

    /**
     * Get the next item in the sequence. <BR>
     *
     * @return the next Item. If there are no more nodes, return null.
     */

    public Item next() {
        if (moveNext()) {
            current = tree.getAttributeNode(currentNodeNr);
        } else {
            current = null;
        }
        return current;
    }

    /**
     * Get the current node in the sequence.
     *
     * @return the node returned by the most recent call on next(), or the node on which we positioned using
     * moveNext()
     */

    public Item current() {
        if (current == null) {
            if (currentNodeNr == -1) {
                return null;
            } else {
                current = tree.getAttributeNode(currentNodeNr);
            }
        }
        return current;
    }

    /**
     * Return the atomized value of the current node.
     *
     * @return the atomized value.
     * @throws NullPointerException if there is no current node
     */

    public Value atomize() throws XPathException {
        if (currentNodeNr == -1) {
            throw new NullPointerException();
        }
        int typeCode = tree.getAttributeAnnotation(currentNodeNr);
        if ((typeCode & NodeInfo.IS_DTD_TYPE) != 0) {
            typeCode = StandardNames.XS_UNTYPED_ATOMIC;
        }
        // optimization: avoid creating the Node object if not needed
        if (typeCode == StandardNames.XS_UNTYPED_ATOMIC) {
            return new UntypedAtomicValue(tree.attValue[currentNodeNr]);
        } else if (typeCode == StandardNames.XS_STRING) {
            return new StringValue(tree.attValue[currentNodeNr]);
        } else {
            return ((NodeInfo)current()).atomize();
        }
    }

    /**
     * Return the string value of the current node.
     *
     * @return the string value, as an instance of CharSequence.
     * @throws NullPointerException if there is no current node
     */

    public CharSequence getStringValue() {
        if (currentNodeNr == -1) {
            throw new NullPointerException();
        }
        return tree.attValue[currentNodeNr];
    }

    /**
    * Get another iteration over the same nodes
    */

    public SequenceIterator getAnother() {
        return new AttributeEnumeration(tree, element, nodeTest);
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
