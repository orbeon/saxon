package org.orbeon.saxon.functions;

import org.orbeon.saxon.expr.XPathContext;
import org.orbeon.saxon.om.Item;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.value.BooleanValue;
import org.orbeon.saxon.value.NumericValue;

/**
 * This class implements the saxon:is-whole-number() extension function,
 * which is specially-recognized by the system because calls are generated by the optimizer.
 *
 * <p>The function signature is <code>saxon:is-whole-number($arg as numeric?) as boolean</code></p>
 *
 * <p>The result is true if $arg is not empty and is equal to some integer.</p>
*/

public class IsWholeNumber extends SystemFunction {

    /**
     * Get the effective boolean value of the expression. This returns false if the value
     * is the empty sequence, a zero-length string, a number equal to zero, or the boolean
     * false. Otherwise it returns true.
     *
     * @param context The context in which the expression is to be evaluated
     * @return the effective boolean value
     * @throws org.orbeon.saxon.trans.XPathException
     *          if any dynamic error occurs evaluating the
     *          expression
     */

    public boolean effectiveBooleanValue(XPathContext context) throws XPathException {
        NumericValue val = (NumericValue)argument[0].evaluateItem(context);
        return val != null && val.isWholeNumber();
    }

    /**
    * Evaluate in a general context
    */

    public Item evaluateItem(XPathContext context) throws XPathException {
        return BooleanValue.get(effectiveBooleanValue(context));
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
// The Original Code is: all this file
//
// The Initial Developer of the Original Code is Michael H. Kay.
//
// Contributor(s):
//

