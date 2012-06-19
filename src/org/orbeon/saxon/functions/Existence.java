package org.orbeon.saxon.functions;
import org.orbeon.saxon.expr.*;
import org.orbeon.saxon.om.Item;
import org.orbeon.saxon.om.LookaheadIterator;
import org.orbeon.saxon.om.SequenceIterator;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.type.ItemType;
import org.orbeon.saxon.value.BooleanValue;

/** Implement the exists() and empty() functions **/

public class Existence extends SystemFunction implements Negatable {

    public static final int EXISTS = 0;
    public static final int EMPTY = 1;

    /**
     * Static analysis: prevent sorting of the argument
     */

    public void checkArguments(ExpressionVisitor visitor) throws XPathException {
        super.checkArguments(visitor);
        Optimizer opt = visitor.getConfiguration().getOptimizer();
        argument[0] = ExpressionTool.unsorted(opt, argument[0], false);
    }


//    public Expression typeCheck(ExpressionVisitor visitor, ItemType contextItemType) throws XPathException {
//        return super.typeCheck(visitor, contextItemType);    //AUTO
//    }

    /**
     * Perform optimisation of an expression and its subexpressions.
     * <p/>
     * <p>This method is called after all references to functions and variables have been resolved
     * to the declaration of the function or variable, and after all type checking has been done.</p>
     *
     * @param visitor         an expression visitor
     * @param contextItemType the static type of "." at the point where this expression is invoked.
     *                        The parameter is set to null if it is known statically that the context item will be undefined.
     *                        If the type of the context item is not known statically, the argument is set to
     *                        {@link org.orbeon.saxon.type.Type#ITEM_TYPE}
     * @return the original expression, rewritten if appropriate to optimize execution
     * @throws org.orbeon.saxon.trans.XPathException
     *          if an error is discovered during this phase
     *          (typically a type error)
     */

    public Expression optimize(ExpressionVisitor visitor, ItemType contextItemType) throws XPathException {
        Expression e2 = super.optimize(visitor, contextItemType);
        if (e2 != this) {
            return e2;
        }
        // See if we can deduce the answer from the cardinality
        int c = argument[0].getCardinality();
        if (c == StaticProperty.ALLOWS_ONE_OR_MORE) {
            return new Literal(BooleanValue.get(operation == EXISTS));
        } else if (c == StaticProperty.ALLOWS_ZERO) {
            return new Literal(BooleanValue.get(operation == EMPTY));
        }
        // Rewrite
        //    exists(A|B) => exists(A) or exists(B)
        //    empty(A|B) => empty(A) and empty(B)
        if (argument[0] instanceof VennExpression) {
            VennExpression v = (VennExpression)argument[0];
            if (v.getOperator() == Token.UNION) {
                int newop = (operation == EXISTS ? Token.OR : Token.AND);
                FunctionCall e0 = SystemFunction.makeSystemFunction(
                        getFunctionName().getLocalName(), new Expression[]{v.getOperands()[0]});
                FunctionCall e1 = SystemFunction.makeSystemFunction(
                        getFunctionName().getLocalName(), new Expression[]{v.getOperands()[1]});
                return new BooleanExpression(e0, newop, e1).optimize(visitor, contextItemType);
            }
        }
        return this;
    }

    /**
     * Check whether this specific instance of the expression is negatable
     *
     * @return true if it is
     */

    public boolean isNegatable(ExpressionVisitor visitor) {
        return true;
    }

    /**
     * Return the negation of the expression
     * @return the negation of the expression
     */

    public Expression negate() {
        FunctionCall fc = SystemFunction.makeSystemFunction(
                (operation == EXISTS ? "empty" : "exists"), getArguments());
        fc.setLocationId(getLocationId());
        return fc;
    }

    /**
    * Evaluate the function in a boolean context
    */

    public boolean effectiveBooleanValue(XPathContext c) throws XPathException {
        SequenceIterator iter = argument[0].iterate(c);
        boolean result = false;
        if ((iter.getProperties() & SequenceIterator.LOOKAHEAD) != 0) {
            switch (operation) {
                case EXISTS:
                    result = ((LookaheadIterator)iter).hasNext();
                    break;
                case EMPTY:
                    result = !((LookaheadIterator)iter).hasNext();
                    break;
            }
        } else {
            switch (operation) {
                case EXISTS:
                    result = iter.next() != null;
                    break;
                case EMPTY: 
                    result = iter.next() == null;
                    break;
            }
        }
        iter.close();
        return result;
    }

    /**
    * Evaluate in a general context
    */

    public Item evaluateItem(XPathContext c) throws XPathException {
        return BooleanValue.get(effectiveBooleanValue(c));
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
