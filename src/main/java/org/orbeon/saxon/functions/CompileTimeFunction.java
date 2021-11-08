package org.orbeon.saxon.functions;
import org.orbeon.saxon.expr.Expression;
import org.orbeon.saxon.expr.ExpressionVisitor;
import org.orbeon.saxon.expr.XPathContext;
import org.orbeon.saxon.om.Item;
import org.orbeon.saxon.om.SequenceIterator;
import org.orbeon.saxon.trans.XPathException;

/**
* Abtract class representing a function call that is always rewritten at compile-time:
* it can never be executed
*/

public abstract class CompileTimeFunction extends SystemFunction {

    /**
    * preEvaluate: this method suppresses compile-time evaluation by doing nothing.
     * (this is because the default implementation of preEvaluate() calls evaluate() which
     * is not available for these functions)
     * @param visitor an expression visitor
     */

    public Expression preEvaluate(ExpressionVisitor visitor) throws XPathException {
        return this;
    }

    /**
    * Evaluate as a single item
    */

    public final Item evaluateItem(XPathContext c) throws XPathException {
        throw new IllegalStateException("Function " + getName(c) + " should have been resolved at compile-time");
    }

    /**
    * Iterate over the results of the function
    */

    public final SequenceIterator iterate(XPathContext c) {
        throw new IllegalStateException("Function " + getName(c) + " should have been resolved at compile-time");
    }

    private String getName(XPathContext c) {
        return getDisplayName();
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
// Contributor(s): none.
//
