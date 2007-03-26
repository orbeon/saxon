package org.orbeon.saxon.expr;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.type.ItemType;

/**
 * An EagerLetExpression is the same as a LetExpression except that the variable is evaluated using
 * eager evaluation rather than lazy evaluation. This is used when performing diagnostic tracing.
 */

public class EagerLetExpression extends LetExpression {

    public EagerLetExpression() {}

    public Expression optimize(Optimizer opt, StaticContext env, ItemType contextItemType) throws XPathException {
        Expression e = super.optimize(opt, env, contextItemType);
        if (e == this) {
            evaluationMode = ExpressionTool.eagerEvaluationMode(sequence);
        }
        return e;
    }

    /**
     * Evaluate the variable.
     */ 
    
//    protected ValueRepresentation eval(XPathContext context) throws XPathException {
//        return ExpressionTool.eagerEvaluate(sequence, context);
//    }

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
