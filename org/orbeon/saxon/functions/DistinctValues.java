package org.orbeon.saxon.functions;
import org.orbeon.saxon.expr.MappingFunction;
import org.orbeon.saxon.expr.MappingIterator;
import org.orbeon.saxon.expr.XPathContext;
import org.orbeon.saxon.om.Item;
import org.orbeon.saxon.om.SequenceIterator;
import org.orbeon.saxon.sort.AtomicSortComparer;
import org.orbeon.saxon.xpath.XPathException;
import org.orbeon.saxon.value.AtomicValue;

import java.util.HashSet;

/**
* The XPath 2.0 distinct-values() function
*/

public class DistinctValues extends CollatingFunction implements MappingFunction {

    /**
    * Evaluate the function to return an iteration of selected values or nodes.
    */

    public SequenceIterator iterate(XPathContext context) throws XPathException {
        SequenceIterator iter = argument[0].iterate(context);
        Object[] info = new Object[2];
        info[0] = new HashSet();
        info[1] = getAtomicSortComparer(1, context);
        return new MappingIterator(iter, this, null, info);
    }

    /**
    * Mapping function. Maps a duplicate item to null (no item in the result) and any other item
    * to itself. The general-purpose "info" object is in this case a pair of two objects, the HashSet
    * used for the value lookup, and the AtomicComparer used to compare values.
    */

    public Object map(Item item, XPathContext context, Object info) throws XPathException {
        HashSet lookup = (HashSet)((Object[])info)[0];
        AtomicSortComparer comparer = (AtomicSortComparer)((Object[])info)[1];
        AtomicSortComparer.ComparisonKey key = comparer.getComparisonKey((AtomicValue)item);
        if (lookup.contains(key)) {
            return null;
        } else {
            lookup.add(key);
            return item;
        }
    }

    /**
    * Get a AtomicSortComparer that can be used to compare values
    * @param arg the position of the argument (starting at 0) containing the collation name.
    * If this argument was not supplied, the default collation is used
    * @param context The dynamic evaluation context.
    */

    protected AtomicSortComparer getAtomicSortComparer(int arg, XPathContext context) throws XPathException {
        return new AtomicSortComparer(getCollator(arg, context, true));
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
