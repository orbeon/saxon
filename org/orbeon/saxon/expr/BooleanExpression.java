package org.orbeon.saxon.expr;
import org.orbeon.saxon.om.Item;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.type.ItemType;
import org.orbeon.saxon.type.Type;
import org.orbeon.saxon.type.TypeHierarchy;
import org.orbeon.saxon.value.BooleanValue;
import org.orbeon.saxon.functions.SystemFunction;
import org.orbeon.saxon.functions.BooleanFn;

import java.util.Iterator;
import java.util.List;


/**
* Boolean expression: two truth values combined using AND or OR.
*/

public class BooleanExpression extends BinaryExpression {

    public BooleanExpression(Expression p1, int operator, Expression p2) {
        super(p1, operator, p2);
    }

    /**
    * Determine the static cardinality. Returns [1..1]
    */

    public int computeCardinality() {
        return StaticProperty.EXACTLY_ONE;
    }

    /**
     * Perform optimisation of an expression and its subexpressions.
     * <p/>
     * <p>This method is called after all references to functions and variables have been resolved
     * to the declaration of the function or variable, and after all type checking has been done.</p>
     *
     * @param opt             the optimizer in use. This provides access to supporting functions; it also allows
     *                        different optimization strategies to be used in different circumstances.
     * @param env             the static context of the expression
     * @param contextItemType the static type of "." at the point where this expression is invoked.
     *                        The parameter is set to null if it is known statically that the context item will be undefined.
     *                        If the type of the context item is not known statically, the argument is set to
     *                        {@link org.orbeon.saxon.type.Type#ITEM_TYPE}
     * @return the original expression, rewritten if appropriate to optimize execution
     * @throws org.orbeon.saxon.trans.StaticError if an error is discovered during this phase
     *                                        (typically a type error)
     */

    public Expression optimize(Optimizer opt, StaticContext env, ItemType contextItemType) throws XPathException {
        // Rewrite [A and B] as [if (A) then B else false()]. The benefit of this is that when B is a recursive
        // function call, it is treated as a tail call (test qxmp290). To avoid disrupting other optimizations
        // of "and" expressions (specifically, where clauses in FLWOR expressions), do this ONLY if B is a user
        // function call (we can't tell if it's recursive), and it's not in a loop.
        final Expression e = super.optimize(opt, env, contextItemType);
        final TypeHierarchy th = env.getConfiguration().getTypeHierarchy();
        if (e == this && operator == Token.AND &&
                operand1 instanceof UserFunctionCall &&
                th.isSubType(operand1.getItemType(th), Type.BOOLEAN_TYPE) &&
                !containedInLoop(env)) {
            IfExpression cond = new IfExpression(operand0, operand1, BooleanValue.FALSE);
            cond.setLocationId(getLocationId());
            cond.setParentExpression(getParentExpression());
            return cond;
        }
        return this;
    }

    private boolean containedInLoop(StaticContext env) {
        ComputedExpression e = this;
        while (true) {
            Container c = e.getParentExpression();
            if (c instanceof Expression && ExpressionTool.isRepeatedSubexpression((Expression)c, e, env)) {
                return true;
            } else if (c instanceof ComputedExpression) {
                e = (ComputedExpression)c;
            } else {
                return false;
            }
        }
    }

    /**
     * Return the negation of this boolean expression, that is, an expression that returns true
     * when this expression returns false, and vice versa
     */

    public Expression negate(StaticContext env) {
        // Apply de Morgan's laws
        if (operator == Token.AND) {
            // not(A and B) ==> not(A) or not(B)
            BooleanFn not0 = (BooleanFn)SystemFunction.makeSystemFunction("not", 1, env.getNamePool());
            Expression[] args0 = {operand0};
            not0.setArguments(args0);
            BooleanFn not1 = (BooleanFn)SystemFunction.makeSystemFunction("not", 1, env.getNamePool());
            Expression[] args1 = {operand1};
            not1.setArguments(args1);
            return new BooleanExpression(not0, Token.OR, not1);
        } else {
            // not(A or B) => not(A) and not(B)
            BooleanFn not0 = (BooleanFn)SystemFunction.makeSystemFunction("not", 1, env.getNamePool());
            Expression[] args0 = {operand0};
            not0.setArguments(args0);
            BooleanFn not1 = (BooleanFn)SystemFunction.makeSystemFunction("not", 1, env.getNamePool());
            Expression[] args1 = {operand1};
            not1.setArguments(args1);
            return new BooleanExpression(not0, Token.AND, not1);
        }
    }

    /**
    * Evaluate the expression
    */

    public Item evaluateItem(XPathContext context) throws XPathException {
        return BooleanValue.get(effectiveBooleanValue(context));
    }

    /**
    * Evaluate as a boolean.
    */

    public boolean effectiveBooleanValue(XPathContext c) throws XPathException {
        switch(operator) {
            case Token.AND:
                return operand0.effectiveBooleanValue(c) && operand1.effectiveBooleanValue(c);

            case Token.OR:
                return operand0.effectiveBooleanValue(c) || operand1.effectiveBooleanValue(c);

            default:
                throw new UnsupportedOperationException("Unknown operator in boolean expression");
        }
    }

    /**
    * Determine the data type of the expression
    * @return Type.BOOLEAN
     * @param th
     */

    public ItemType getItemType(TypeHierarchy th) {
        return Type.BOOLEAN_TYPE;
    }

    /**
     * Construct a list containing the "anded" subexpressions of an expression:
     * if the expression is (A and B and C), this returns (A, B, C).
     * @param exp the expression to be decomposed
     * @param list the list to which the subexpressions are to be added.
     */

    public static void listAndComponents(Expression exp, List list) {
        if (exp instanceof BooleanExpression && ((BooleanExpression)exp).getOperator() == Token.AND) {
            for (Iterator iter = exp.iterateSubExpressions(); iter.hasNext();) {
                 listAndComponents((Expression)iter.next(), list);
            }
        } else {
            list.add(exp);
        }

        // TODO: could do more complete analysis to convert the expression to conjunctive normal form.
        // This is done by applying various transformations:
        //   not(not(X)) => X
        //   not(P and Q) => not(P) or not(Q)
        //   not(P or Q) => not(P) and not(Q)
        //   A or (B and C) => (A or B) and (A or C)
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
