package net.sf.saxon.dom;
import net.sf.saxon.om.Item;
import net.sf.saxon.om.NodeInfo;
import net.sf.saxon.om.SequenceIterator;
import net.sf.saxon.om.VirtualNode;
import net.sf.saxon.trans.DynamicError;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.value.SequenceExtent;
import net.sf.saxon.value.Value;
import org.w3c.dom.Node;

import java.util.List;

/**
* This class wraps a list of nodes as a DOM NodeList
*/

public final class DOMNodeList implements org.w3c.dom.NodeList {
    private List sequence;

    /**
     * Construct an node list that wraps a supplied SequenceExtent. This constructor does
     * not check that the items in the supplied SequenceExtent are indeed DOM Nodes.
     */

    public DOMNodeList(List extent) {
        sequence = extent;
    }

//    /**
//     * Construct an node list that wraps a supplied SequenceExtent, checking that all the
//     * items in the sequence are either DOM Nodes or Saxon NodeInfo objects
//    */
//
//    public static DOMNodeList checkAndMake(SequenceExtent extent) throws XPathException {
//        // check that all the items are DOM nodes
//        SequenceIterator it = extent.iterate(null);
//        while (true) {
//            Item next = it.next();
//            if (next==null) break;
//            Object o = next;
//            if (!(o instanceof Node || o instanceof NodeInfo)) {
//                throw new DynamicError("Supplied sequence contains an item that is neither a DOM nor Saxon Node");
//            }
//        }
//        return new DOMNodeList(extent);
//    }

    /**
     * Return the sequence of nodes as a Saxon Value
     */

//    public Value getSequence() {
//        return sequence;
//    }

    /**
    * return the number of nodes in the list (DOM method)
    */

    public int getLength() {
        return sequence.size();
    }

    /**
    * Return the n'th item in the list (DOM method)
    * @throws java.lang.ClassCastException if the item is not a DOM Node
    */

    public Node item(int index) {
        return (Node)sequence.get(index);
//        while (o instanceof VirtualNode) {
//            o = ((VirtualNode)o).getUnderlyingNode();
//        }
//        if (o instanceof Node) {
//            return (Node)o;
//        } else if (o instanceof NodeInfo) {
//            return NodeOverNodeInfo.wrap((NodeInfo)o);
//        } else {
//            throw new IllegalStateException(
//                    "Sequence cannot be used as a DOM NodeList, because it contains an item that is not a node");
//        }
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

