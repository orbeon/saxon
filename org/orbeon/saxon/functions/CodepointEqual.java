package org.orbeon.saxon.functions;
import org.orbeon.saxon.expr.XPathContext;
import org.orbeon.saxon.om.Item;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.value.AtomicValue;
import org.orbeon.saxon.value.BooleanValue;

/**
* XPath 2.0 codepoint-equal() function.
* Compares two strings using the unicode codepoint collation. (The function was introduced
 * specifically to allow URI comparison: URIs are promoted to strings when necessary.)
*/

public class CodepointEqual extends SystemFunction {

    /**
    * Evaluate the expression
    */

    public Item evaluateItem(XPathContext context) throws XPathException {
        AtomicValue op1 = (AtomicValue)argument[0].evaluateItem(context);
        if (op1 == null) {
            return null;
        }
        AtomicValue op2 = (AtomicValue)argument[1].evaluateItem(context);
        if (op2 == null) {
            return null;
        }

        return BooleanValue.get(op1.getStringValue().equals(op2.getStringValue()));
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
// The Initial Developer of the Original Code is Michael Kay
//
// Portions created by (your name) are Copyright (C) (your legal entity). All Rights Reserved.
//
// Contributor(s): none.
//
