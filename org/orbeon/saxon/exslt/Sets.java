package net.sf.saxon.exslt;
import net.sf.saxon.expr.*;
import net.sf.saxon.om.Item;
import net.sf.saxon.om.NodeInfo;
import net.sf.saxon.om.SequenceIterator;
import net.sf.saxon.sort.GlobalOrderComparer;
import net.sf.saxon.trans.DynamicError;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.value.SingletonNode;

/**
* This class implements extension functions in the
* http://exslt.org/sets namespace. <p>
*/

public abstract class Sets  {

    private Sets() {}

    /**
    * Return the intersection of two node-sets
    * @param p1 The first node-set
    * @param p2 The second node-set
    * @return A node-set containing all nodes that are in both p1 and p2
    */

    public static SequenceIterator intersection(SequenceIterator p1, SequenceIterator p2) throws XPathException {
        return new IntersectionEnumeration(p1, p2, GlobalOrderComparer.getInstance());
    }

    /**
    * Return the difference of two node-sets
    * @param p1 The first node-set
    * @param p2 The second node-set
    * @return A node-set containing all nodes that are in p1 and not in p2
    */

    public static SequenceIterator difference(SequenceIterator p1, SequenceIterator p2) throws XPathException {
        return new DifferenceEnumeration(p1, p2, GlobalOrderComparer.getInstance());
    }

    /**
    * Determine whether two node-sets contain at least one node in common
    * @param p1 The first node-set
    * @param p2 The second node-set
    * @return true if p1 and p2 contain at least one node in common (i.e. if the intersection
    * is not empty)
    */

    public static boolean hasSameNode(SequenceIterator p1, SequenceIterator p2) throws XPathException {
        SequenceIterator intersection =
            new IntersectionEnumeration(p1, p2, GlobalOrderComparer.getInstance());
        return intersection.next() != null;
    }

    /**
    * Find all the nodes in ns1 that are before the first node in ns2.
    * Return empty set if ns2 is empty,
    */

    public static SequenceIterator leading (
                     XPathContext context,
                     SequenceIterator ns1, SequenceIterator ns2) throws XPathException {

        NodeInfo first = null;

        // Find the first node in ns2 (in document order)

        GlobalOrderComparer comparer = GlobalOrderComparer.getInstance();
        while (true) {
            Item item = ns2.next();
            if (item == null) {
                if (first == null) {
                    return ns1;
                }
                break;
            }
            if (item instanceof NodeInfo) {
                NodeInfo node = (NodeInfo)item;
                if (first==null) {
                    first = node;
                } else {
                    if (comparer.compare(node, first) < 0) {
                        first = node;
                    }
                }
            } else {
                DynamicError e = new DynamicError(
                        "Operand of leading() contains an item that is not a node");
                e.setXPathContext(context);
                throw e;
            }
        }

        // Filter ns1 to select nodes that come before this one

        Expression filter = new IdentityComparison(
                                    new ContextItemExpression(),
                                    Token.PRECEDES,
                                    new SingletonNode(first));

        return new FilterIterator(ns1, filter, context);

    }

    /**
    * Find all the nodes in ns1 that are after the first node in ns2.
    * Return empty set if ns2 is empty,
    */

    public static SequenceIterator trailing (
                     XPathContext c,
                     SequenceIterator ns1, SequenceIterator ns2) throws XPathException {

        return net.sf.saxon.functions.Extensions.after(c, ns1, ns2);
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
