package net.sf.saxon.dom;
import net.sf.saxon.om.Item;
import net.sf.saxon.om.SequenceIterator;
import net.sf.saxon.om.VirtualNode;
import net.sf.saxon.value.SequenceExtent;
import net.sf.saxon.value.SequenceValue;
import net.sf.saxon.xpath.DynamicError;
import net.sf.saxon.xpath.XPathException;
import org.w3c.dom.Node;

/**
* This class wraps a SequenceExtent as a DOM NodeList
*  - though this will only work if the items are nodes, and if the nodes themselves
* implement the DOM Node interface (which is true of the two Saxon tree models,
* but not necessarily of all possible implementations).
*/

public final class DOMNodeList implements org.w3c.dom.NodeList {
    private SequenceExtent sequence;

    /**
     * Construct an node list that wraps a supplied SequenceExtent. This constructor does
     * not check that the items in the supplied SequenceExtent are indeed DOM Nodes.
     */

    public DOMNodeList(SequenceExtent extent) {
        sequence = extent;
    }

    /**
     * Construct an node list that wraps a supplied SequenceExtent, checking that all the
     * items in the sequence are DOM Nodes
    */

    public static DOMNodeList checkAndMake(SequenceExtent extent) throws XPathException {
        // check that all the items are DOM nodes
        SequenceIterator it = extent.iterate(null);
        Node[] nodes = new Node[extent.getLength()];
        int i=0;
        while (true) {
            Item next = it.next();
            if (next==null) break;
            Object o = next;
            while (o instanceof VirtualNode) {
                o = ((VirtualNode)o).getUnderlyingNode();
            }
            if (!(o instanceof Node)) {
                throw new DynamicError("Supplied sequence contains an item that is not a DOM Node");
            }
            nodes[i++] = (Node)o;

        }
        return new DOMNodeList(extent);
    }

    /**
     * Return the sequence of nodes as a Saxon SequenceValue
     */

    public SequenceValue getSequence() {
        return sequence;
    }

    /**
    * return the number of nodes in the list (DOM method)
    */

    public int getLength() {
        return sequence.getLength();
    }

    /**
    * Return the n'th item in the list (DOM method)
    * @throws java.lang.ClassCastException if the item is not a DOM Node
    */

    public Node item(int index) {
        Object o = sequence.itemAt(index);
        if (o instanceof VirtualNode) {
            o = ((VirtualNode)o).getUnderlyingNode();
        }
        if (o instanceof Node) {
            return (Node)o;
        } else {
            throw new ClassCastException("NodeList contains a non-DOM node");
        }
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

