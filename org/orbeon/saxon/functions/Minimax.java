package org.orbeon.saxon.functions;
import org.orbeon.saxon.Err;
import org.orbeon.saxon.expr.XPathContext;
import org.orbeon.saxon.om.Item;
import org.orbeon.saxon.om.SequenceIterator;
import org.orbeon.saxon.sort.DescendingComparer;
import org.orbeon.saxon.value.*;
import org.orbeon.saxon.xpath.XPathException;

import java.util.Comparator;

/**
* This class implements the min() and max() functions
*/

public class Minimax extends CollatingFunction {

    public static final int MIN = 2;
    public static final int MAX = 3;

    /**
    * Evaluate the function
    */

    public Item evaluateItem(XPathContext context) throws XPathException {

        Comparator collator = getAtomicComparer(1, context);

        if (operation == MAX) {
            collator = new DescendingComparer(collator);
        }

        SequenceIterator iter = argument[0].iterate(context);

        AtomicValue min = (AtomicValue)iter.next();
        if (min == null) return null;
        if (min instanceof UntypedAtomicValue) {
            try {
                min = new DoubleValue(Value.stringToNumber(min.getStringValue()));
            } catch (NumberFormatException e) {
                dynamicError("Failure converting " +
                                                 Err.wrap(min.getStringValue()) +
                                                 " to a number", context);
            }
        }

        while (true) {
            AtomicValue test = (AtomicValue)iter.next();
            if (test==null) break;
            AtomicValue test2 = test;
            if (test instanceof UntypedAtomicValue) {
                try {
                    test2 = new DoubleValue(Value.stringToNumber(test.getStringValue()));
                } catch (NumberFormatException e) {
                    dynamicError("Failure converting " +
                                                     Err.wrap(test.getStringValue()) +
                                                     " to a number", "FORG0001", context);
                }
            }
            if (test2 instanceof NumericValue && ((NumericValue)test2).isNaN()) {
                // if there's a NaN in the sequence, return NaN
                return test2;
            }
            try {
                if (collator.compare(test2, min) < 0) {
                    min = test2;
                }
            } catch (ClassCastException err) {
                typeError("Cannot compare " + min.getItemType() +
                                   " with " + test2.getItemType(), "FORG0007", context);
                return null;
            }
        }
        return min;
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
