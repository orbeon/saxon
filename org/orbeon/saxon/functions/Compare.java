package net.sf.saxon.functions;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.om.Item;
import net.sf.saxon.sort.AtomicComparer;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.value.AtomicValue;
import net.sf.saxon.value.IntegerValue;

//import java.util.Comparator;

/**
* XSLT 2.0 compare() function
*/

// Supports string comparison using a collation

public class Compare extends CollatingFunction {

    /**
    * Evaluate the expression
    */

    public Item evaluateItem(XPathContext context) throws XPathException {

        AtomicValue arg0 = (AtomicValue)argument[0].evaluateItem(context);
        if (arg0==null) {
            return null;
        }
        arg0 = arg0.getPrimitiveValue();

        AtomicValue arg1 = (AtomicValue)argument[1].evaluateItem(context);
        if (arg1==null) {
            return null;
        }
        arg1 = arg1.getPrimitiveValue();

        AtomicComparer collator = getAtomicComparer(2, context);

        int result = collator.compare(arg0, arg1);
        if (result < 0) {
            return IntegerValue.MINUS_ONE;
        } else if (result > 0) {
            return IntegerValue.PLUS_ONE;
        } else {
            return IntegerValue.ZERO;
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
