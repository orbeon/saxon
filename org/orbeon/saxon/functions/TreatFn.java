package net.sf.saxon.functions;
import net.sf.saxon.expr.*;
import net.sf.saxon.om.Item;
import net.sf.saxon.om.SequenceIterator;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.ItemType;

/**
* This class supports the XPath 2.0 functions exactly-one(), one-or-more(), zero-or-one().
* Because Saxon doesn't do strict static type checking, these are essentially identity
* functions; the run-time type checking is done as part of the function call mechanism
*/

public class TreatFn extends SystemFunction {

    /**
     * Type-check the expression. This also calls preEvaluate() to evaluate the function
     * if all the arguments are constant; functions that do not require this behavior
     * can override the preEvaluate method.
     */

    public Expression analyze(StaticContext env, ItemType contextItemType) throws XPathException {
        Expression exp = super.analyze(env, contextItemType);
        if (exp instanceof TreatFn) {
            if (((TreatFn)exp).argument[0] instanceof CardinalityChecker) {
                // Normal path. Modify the CardinalityChecker that does the work so that it
                // returns the correct error code in the event of a failure.
                CardinalityChecker cc = (CardinalityChecker)((TreatFn)exp).argument[0];
                if (operation == StaticProperty.EXACTLY_ONE) {
                    cc.setErrorCode("FORG0005");
                } else if (operation == StaticProperty.ALLOWS_ONE_OR_MORE) {
                    cc.setErrorCode("FORG0004");
                } else {
                    cc.setErrorCode("FORG0003");
                }
            }
        }
        return exp;
    }

    /**
    * Evaluate the function
    */

    public Item evaluateItem(XPathContext context) throws XPathException {
        return argument[0].evaluateItem(context);
    }

    /**
    * Iterate over the results of the function
    */

    public SequenceIterator iterate(XPathContext context) throws XPathException {
       return argument[0].iterate(context);
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
