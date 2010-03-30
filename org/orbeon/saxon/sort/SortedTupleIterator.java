package org.orbeon.saxon.sort;

import org.orbeon.saxon.Configuration;
import org.orbeon.saxon.expr.XPathContext;
import org.orbeon.saxon.om.SequenceIterator;
import org.orbeon.saxon.om.ValueRepresentation;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.value.Closure;
import org.orbeon.saxon.value.EmptySequence;
import org.orbeon.saxon.value.ObjectValue;

/**
 * A SortedTupleIterator is a modified SortedIterator. Whereas the sorted iterator
 * used by XSLT computes the sort key of each item in a sequence, using that item
 * as the context item, the SortedTupleIterator used by XQuery precomputes the sort
 * keys from scratch; they do not need to be a function of the item being sorted.
 *
 * <p>The items returned by the SortedTupleIterator are instance of ObjectValue,
 * which encapsulate a Value representing the contents of the tuple.</p>
 */

public class SortedTupleIterator extends SortedIterator {

    /**
     * Create a sorted tuple iterator
     * @param context the dynamic context
     * @param base the base iterator, which returns the unsorted tuples. Each tuple is represented
     * by an ObjectValue which wraps an array of Value objects. The first Value object represents
     * the tuple itself. Subsequent Value objects represent the sort key values, in order.
     * @param comparators the comparators used for comparing sort keys
     */

    public SortedTupleIterator(XPathContext context, SequenceIterator base,
                               AtomicComparer[] comparators) {
        super(context, base, null, comparators);
        setHostLanguage(Configuration.XQUERY);
    }

    /**
     * Override the method that builds the array of values and sort keys.
     * @throws XPathException
     */

    protected void buildArray() throws XPathException {
        int allocated = 100;
        nodeKeys = new Object[allocated * recordSize];
        count = 0;

        // initialise the array with data

        while (true) {
            ObjectValue tupleObject = (ObjectValue)base.next();
            if (tupleObject == null) {
                break;
            }
            ValueRepresentation[] tuple = (ValueRepresentation[])tupleObject.getObject();
            if (count==allocated) {
                allocated *= 2;
                Object[] nk2 = new Object[allocated * recordSize];
                System.arraycopy(nodeKeys, 0, nk2, 0, count * recordSize);
                nodeKeys = nk2;
            }
            int k = count*recordSize;
            nodeKeys[k] = new ObjectValue(tuple[0]);
                // this is the "item" that will be returned by the TupleIterator.
                // In general it is actually a sequence, so we wrap it in an ObjectValue
                // It subsequently gets unwrapped by the MappingFunction applied to the
                // output of the SortedTupleIterator.
            for (int n=1; n<=comparators.length; n++) {
                ValueRepresentation v = tuple[n];
                if (v instanceof Closure) {
                    v = ((Closure)v).reduce();
                }
                if (v instanceof EmptySequence) {
                    nodeKeys[k+n] = null;
                } else {
                    nodeKeys[k+n] = v;
                }
            }
            nodeKeys[k+comparators.length+1] = new Integer(count);
            count++;
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
// The Initial Developer of the Original Code is Michael H. Kay
//
// Portions created by (your name) are Copyright (C) (your legal entity). All Rights Reserved.
//
// Contributor(s): none
//