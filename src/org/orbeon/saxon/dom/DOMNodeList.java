package org.orbeon.saxon.dom;
import org.orbeon.saxon.om.Item;
import org.orbeon.saxon.om.NodeInfo;
import org.orbeon.saxon.om.SequenceIterator;
import org.orbeon.saxon.om.VirtualNode;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.value.SequenceExtent;
import org.w3c.dom.Node;

import java.util.ArrayList;
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

    /**
     * Construct an node list that wraps a supplied SequenceExtent, checking that all the
     * items in the sequence are wrappers around DOM Nodes
    */

    public static DOMNodeList checkAndMake(SequenceExtent extent) throws XPathException {
        SequenceIterator it = extent.iterate();
        List list = new ArrayList(extent.getLength());
        while (true) {
            Item next = it.next();
            if (next==null) break;
            Object o = next;
            if (!(o instanceof NodeInfo)) {
                throw new XPathException("Supplied sequence contains an item that is not a Saxon NodeInfo");
            }
            if (o instanceof VirtualNode) {
                o = ((VirtualNode)o).getUnderlyingNode();
                if (!(o instanceof Node)) {
                    throw new XPathException("Supplied sequence contains an item that is not a wrapper around a DOM Node");
                }
                list.add(o);
            }

        }
        return new DOMNodeList(list);
    }

    /**
    * return the number of nodes in the list (DOM method)
    */

    public int getLength() {
        return sequence.size();
    }

    /**
     * Return the n'th item in the list (DOM method)
     * @return the n'th node in the node list, counting from zero; or null if the index is out of range
     * @throws java.lang.ClassCastException if the item is not a DOM Node
     */

    public Node item(int index) {
        if (index < 0 || index >= sequence.size()) {
            return null;
        } else {
            return (Node)sequence.get(index);
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

