package net.sf.saxon.expr;
import net.sf.saxon.om.Item;
import net.sf.saxon.om.LookaheadIterator;
import net.sf.saxon.om.NodeInfo;
import net.sf.saxon.om.SequenceIterator;
import net.sf.saxon.sort.NodeOrderComparer;
import net.sf.saxon.trans.XPathException;

/**
* An enumeration representing a nodeset that is a union of two other NodeSets.
*/

public class UnionEnumeration implements SequenceIterator, LookaheadIterator {

    private SequenceIterator e1;
    private SequenceIterator e2;
    private NodeInfo nextNode1 = null;
    private NodeInfo nextNode2 = null;
    private NodeOrderComparer comparer;
    private NodeInfo current = null;
    private int position = 0;

    /**
    * Create the iterator. The two input iterators must return nodes in document
    * order for this to work.
    */

    public UnionEnumeration(SequenceIterator p1, SequenceIterator p2,
                            NodeOrderComparer comparer) throws XPathException {
        this.e1 = p1;
        this.e2 = p2;
        this.comparer = comparer;

        nextNode1 = next(e1);
        nextNode2 = next(e2);
    }

    /**
    * Get the next item from one of the input sequences,
    * checking that it is a node.
    */

    private NodeInfo next(SequenceIterator iter) throws XPathException {
        return (NodeInfo)iter.next();
        // we rely on the type-checking mechanism to prevent a ClassCastException here
    }

    public boolean hasNext() {
        return nextNode1!=null || nextNode2!=null;
    }

    public Item next() throws XPathException {

        // main merge loop: take a value from whichever set has the lower value

        position++;
        if (nextNode1 != null && nextNode2 != null) {
            int c = comparer.compare(nextNode1, nextNode2);
            if (c<0) {
                current = nextNode1;
                nextNode1 = next(e1);
                return current;

            } else if (c>0) {
                current = nextNode2;
                nextNode2 = next(e2);
                return current;

            } else {
                current = nextNode2;
                nextNode2 = next(e2);
                nextNode1 = next(e1);
                return current;
            }
        }

        // collect the remaining nodes from whichever set has a residue

        if (nextNode1!=null) {
            current = nextNode1;
            nextNode1 = next(e1);
            return current;
        }
        if (nextNode2!=null) {
            current = nextNode2;
            nextNode2 = next(e2);
            return current;
        }
        current = null;
        return null;
    }

    public Item current() {
        return current;
    }

    public int position() {
        return position;
    }

    public SequenceIterator getAnother() throws XPathException {
        return new UnionEnumeration(e1.getAnother(), e2.getAnother(), comparer);
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
