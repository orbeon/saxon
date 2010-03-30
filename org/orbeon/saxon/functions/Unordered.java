package org.orbeon.saxon.functions;
import org.orbeon.saxon.expr.*;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.type.ItemType;

/**
* XPath 2.0 unordered() function
*/

public class Unordered extends CompileTimeFunction {

    public Expression typeCheck(ExpressionVisitor visitor, ItemType contextItemType) throws XPathException {
        Expression exp = super.typeCheck(visitor, contextItemType);
        if (exp instanceof Unordered) {
            Optimizer opt = visitor.getConfiguration().getOptimizer();
            return ExpressionTool.unsorted(opt, ((Unordered)exp).argument[0], false);
        }
        return exp;
    }

    public Expression optimize(ExpressionVisitor visitor, ItemType contextItemType) throws XPathException {
        Expression exp = super.optimize(visitor, contextItemType);
        if (exp instanceof Unordered) {
            return ExpressionTool.unsorted(visitor.getConfiguration().getOptimizer(),
                    ((Unordered)exp).argument[0], false);
        }
        return exp;
    }

    /**
    * preEvaluate: called if the argument is constant
     * @param visitor an expression visitor
     */

    public Expression preEvaluate(ExpressionVisitor visitor) throws XPathException {
        return argument[0];
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
