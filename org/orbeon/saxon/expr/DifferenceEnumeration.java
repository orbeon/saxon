package net.sf.saxon.expr;
import net.sf.saxon.om.Item;
import net.sf.saxon.om.NodeInfo;
import net.sf.saxon.om.SequenceIterator;
import net.sf.saxon.sort.NodeOrderComparer;
import net.sf.saxon.trans.XPathException;


/**
* An enumeration representing a nodeset that is teh difference of two other NodeSets.
* There is an "except" operator in XPath 2.0 to create such an expression.
*/


public class DifferenceEnumeration implements SequenceIterator {


    private SequenceIterator p1;
    private SequenceIterator p2;

    private NodeInfo nextNode1 = null;
    private NodeInfo nextNode2 = null;
    private NodeOrderComparer comparer;

    //private NodeInfo nextNode = null;
    private NodeInfo current = null;
    private int position = 0;

    /**
    * Form an enumeration of the difference of two nodesets, that is, the nodes
    * that are in p1 and that are not in p2.
    * @param p1 the first operand
    * @param p2 the second operand
    * @param comparer the comparer
    */

    public DifferenceEnumeration(SequenceIterator p1, SequenceIterator p2,
                                 NodeOrderComparer comparer) throws XPathException {
        this.p1 = p1;
        this.p2 = p2;
        this.comparer = comparer;

        // move to the first node in each input nodeset

        nextNode1 = next(p1);
        nextNode2 = next(p2);
    }

    /**
    * Get the next item from one of the input sequences,
    * checking that it is a node.
    */

    private NodeInfo next(SequenceIterator iter) throws XPathException {
        return (NodeInfo)iter.next();
        // rely on type-checking to prevent a ClassCastException
    }

    public Item next() throws XPathException {
        // main merge loop: if the node in p1 has a lower key value that that in p2, return it;
        // if they are equal, advance both nodesets; if p1 is higher, advance p2.

        while (true) {

            if (nextNode1 == null) {
                return null;
            }

            if (nextNode2 == null) {
                // second node-set is exhausted; return the next node from the first node-set
                return deliver();
            }

            int c = comparer.compare(nextNode1, nextNode2);
            if (c<0) {                              // p1 is lower
                return deliver();

            } else if (c>0) {                       // p1 is higher
                nextNode2 = next(p2);
                if (nextNode2 == null) {
                    return deliver();
                }

            } else {                                // keys are equal
                nextNode2 = next(p2);
                nextNode1 = next(p1);
            }
        }
    }

    /**
     * Deliver the next node from the first node-set, advancing the iterator to
     * look-ahead for the next item, and setting the current and position variables.
     * @return the next node from the first node-set
     * @throws XPathException
     */
    private NodeInfo deliver() throws XPathException {
        current = nextNode1;
        nextNode1 = next(p1);
        position++;
        return current;
    }

    public Item current() {
        return current;
    }

    public int position() {
        return position;
    }

    public SequenceIterator getAnother() throws XPathException {
        return new DifferenceEnumeration(p1.getAnother(), p2.getAnother(), comparer);
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
