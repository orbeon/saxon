package net.sf.saxon.sort;

import net.sf.saxon.expr.LastPositionFinder;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.expr.XPathContextMajor;
import net.sf.saxon.om.Item;
import net.sf.saxon.om.SequenceIterator;
import net.sf.saxon.trace.InstructionInfoProvider;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.value.AtomicValue;

/**
 * A SortedGroupIterator is a modified SortedIterator. It sorts a sequence of groups,
 * and is itself a GroupIterator. The modifications retain extra information about
 * the items being sorted. The items are each the leading item of a group, and as well
 * as the item itself, the iterator preserves information about the group: specifically,
 * an iterator over the items in the group, and the value of the grouping key (if any).
 */

public class SortedGroupIterator extends SortedIterator implements GroupIterator {

    private InstructionInfoProvider origin;

    public SortedGroupIterator(XPathContext context, GroupIterator base,
                               FixedSortKeyDefinition[] sortKeys,
                               InstructionInfoProvider origin) throws XPathException {
        super(context, base, sortKeys);
        this.origin = origin;
        // add two items to each tuple, for the iterator over the items in the group,
        // and the grouping key, respectively.
        recordSize += 2;
    }

    /**
     * Override the method that builds the array of values and sort keys.
     * @throws XPathException
     */

    protected void buildArray() throws XPathException {
        int allocated;
        if (base instanceof LastPositionFinder) {
            allocated = ((LastPositionFinder)base).getLastPosition();
        } else {
            allocated = 100;
        }

        nodeKeys = new Object[allocated * recordSize];
        count = 0;

        XPathContextMajor c2 = context.newContext();
        c2.setCurrentIterator(base);
        c2.setOrigin(origin);
        c2.setCurrentGroupIterator((GroupIterator)base);
                // this provides the context for evaluating the sort key

        // initialise the array with data

        while (true) {
            Item item = base.next();
            if (item == null) {
                break;
            }
            if (count==allocated) {
                allocated *= 2;
                Object[] nk2 = new Object[allocated * recordSize];
                System.arraycopy(nodeKeys, 0, nk2, 0, count * recordSize);
                nodeKeys = nk2;
            }
            int k = count*recordSize;
            nodeKeys[k] = item;
            for (int n=0; n<sortkeys.length; n++) {
                nodeKeys[k+n+1] = sortkeys[n].getSortKey().evaluateItem(c2);
            }
            nodeKeys[k+sortkeys.length+1] = new Integer(count);
            // extra code added to superclass
            nodeKeys[k+sortkeys.length+2] = ((GroupIterator)base).getCurrentGroupingKey();
            nodeKeys[k+sortkeys.length+3] = ((GroupIterator)base).iterateCurrentGroup();
            count++;
        }
    }

    public AtomicValue getCurrentGroupingKey() {
        return (AtomicValue)nodeKeys[(index-1)*recordSize+sortkeys.length+2];
    }

    public SequenceIterator iterateCurrentGroup() throws XPathException {
        SequenceIterator iter =
                (SequenceIterator)nodeKeys[(index-1)*recordSize+sortkeys.length+3];
        return iter.getAnother();
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