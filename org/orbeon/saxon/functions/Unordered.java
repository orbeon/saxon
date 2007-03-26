package org.orbeon.saxon.functions;
import org.orbeon.saxon.expr.Expression;
import org.orbeon.saxon.expr.ExpressionTool;
import org.orbeon.saxon.expr.StaticContext;
import org.orbeon.saxon.expr.Optimizer;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.type.ItemType;

/**
* XPath 2.0 unordered() function
*/

public class Unordered extends CompileTimeFunction {

    public Expression typeCheck(StaticContext env, ItemType contextItemType) throws XPathException {
        Expression exp = super.typeCheck(env, contextItemType);
        if (exp instanceof Unordered) {
            Optimizer opt = env.getConfiguration().getOptimizer();
            return ExpressionTool.unsorted(opt, ((Unordered)exp).argument[0], true);
        }
        return exp;
    }

    public Expression optimizer(Optimizer opt, StaticContext env, ItemType contextItemType) throws XPathException {
        Expression exp = super.optimize(opt, env, contextItemType);
        if (exp instanceof Unordered) {
            return ExpressionTool.unsorted(opt, ((Unordered)exp).argument[0], true);
        }
        return exp;
    }

    /**
    * preEvaluate: called if the argument is constant
    */

    public Expression preEvaluate(StaticContext env) throws XPathException {
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
