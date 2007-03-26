package org.orbeon.saxon.expr;
import org.orbeon.saxon.Configuration;
import org.orbeon.saxon.trans.DynamicError;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.type.ItemType;
import org.orbeon.saxon.value.Cardinality;
import org.orbeon.saxon.value.Value;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
* Binary Expression: a numeric or boolean expression consisting of the
* two operands and an operator
*/

public abstract class BinaryExpression extends ComputedExpression {

    protected Expression operand0;
    protected Expression operand1;
    protected int operator;       // represented by the token number from class Tokenizer

    /**
    * Create a binary expression identifying the two operands and the operator
    * @param p0 the left-hand operand
    * @param op the operator, as a token returned by the Tokenizer (e.g. Token.AND)
    * @param p1 the right-hand operand
    */

    public BinaryExpression(Expression p0, int op, Expression p1) {
        this.operator = op;
        operand0 = p0;
        operand1 = p1;
        adoptChildExpression(p0);
        adoptChildExpression(p1);
    }

    /**
    * Simplify an expression
    * @return the simplified expression
    */

     public Expression simplify(StaticContext env) throws XPathException {
        operand0 = operand0.simplify(env);
        operand1 = operand1.simplify(env);
        return this;
    }

    /**
    * Type-check the expression. Default implementation for binary operators that accept
    * any kind of operand
    */

    public Expression typeCheck(StaticContext env, ItemType contextItemType) throws XPathException {
        operand0 = operand0.typeCheck(env, contextItemType);
        operand1 = operand1.typeCheck(env, contextItemType);
        // if both operands are known, pre-evaluate the expression
        try {
            if ((operand0 instanceof Value) && (operand1 instanceof Value)) {
                return Value.asValue(evaluateItem(env.makeEarlyEvaluationContext()));
            }
        } catch (DynamicError err) {
            // if early evaluation fails, suppress the error: the value might
            // not be needed at run-time
        }
        return this;
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
        operand0 = operand0.optimize(opt, env, contextItemType);
        operand1 = operand1.optimize(opt, env, contextItemType);
        // if both operands are known, pre-evaluate the expression
        try {
            if ((operand0 instanceof Value) && (operand1 instanceof Value)) {
                return Value.asValue(evaluateItem(env.makeEarlyEvaluationContext()));
            }
        } catch (DynamicError err) {
            // if early evaluation fails, suppress the error: the value might
            // not be needed at run-time
        }
        return this;
    }


    /**
    * Promote this expression if possible
    */

    public Expression promote(PromotionOffer offer) throws XPathException {
        Expression exp = offer.accept(this);
        if (exp != null) {
            return exp;
        } else {
            if (offer.action != PromotionOffer.UNORDERED) {
                operand0 = doPromotion(operand0, offer);
                operand1 = doPromotion(operand1, offer);
            }
            return this;
        }
    }

    /**
    * Get the immediate subexpressions of this expression
    */

    public Iterator iterateSubExpressions() {
        return new PairIterator(operand0, operand1);
    }

    /**
     * Replace one subexpression by a replacement subexpression
     * @param original the original subexpression
     * @param replacement the replacement subexpression
     * @return true if the original subexpression is found
     */

    public boolean replaceSubExpression(Expression original, Expression replacement) {
        boolean found = false;
        if (operand0 == original) {
            operand0 = replacement;
            found = true;
        }
        if (operand1 == original) {
            operand1 = replacement;
            found = true;
        }
        return found;
    }


    /**
     * Get the operator
     */

    public int getOperator() {
        return operator;
    }

    /**
     * Get the operands
     */

    public Expression[] getOperands() {
        Expression[] ops = {operand0, operand1};
        return ops;
    }

    /**
    * Determine the static cardinality. Default implementation returns [0..1] if either operand
     * can be empty, or [1..1] otherwise.
    */

    public int computeCardinality() {
        if (Cardinality.allowsZero(operand0.getCardinality()) ||
                Cardinality.allowsZero(operand1.getCardinality())) {
            return StaticProperty.ALLOWS_ZERO_OR_ONE;
        } else {
            return StaticProperty.EXACTLY_ONE;
        }
    }

    /**
     * Determine the special properties of this expression
     * @return {@link StaticProperty#NON_CREATIVE}. This is overridden
     * for some subclasses.
     */

    public int computeSpecialProperties() {
        int p = super.computeSpecialProperties();
        return p | StaticProperty.NON_CREATIVE;
    }

    /**
     * Determine whether a binary operator is commutative, that is, A op B = B op A.
     * @param operator
     * @return true if the operator is commutative
     */

    protected static boolean isCommutative(int operator) {
        return (operator == Token.AND ||
                operator == Token.OR ||
                operator == Token.UNION ||
                operator == Token.INTERSECT ||
                operator == Token.PLUS ||
                operator == Token.MULT ||
                operator == Token.EQUALS ||
                operator == Token.FEQ ||
                operator == Token.NE ||
                operator == Token.FNE
                );
    }

    /**
     * Determine whether an operator is associative, that is, ((a^b)^c) = (a^(b^c))
     */

    protected static boolean isAssociative(int operator) {
        return (operator == Token.AND ||
                operator == Token.OR ||
                operator == Token.UNION ||
                operator == Token.INTERSECT ||
                operator == Token.PLUS ||
                operator == Token.MULT
                );
    }
    /**
     * Test if one operator is the inverse of another, so that (A op1 B) is
     * equivalent to (B op2 A). Commutative operators are the inverse of themselves
     * and are therefore not listed here.
     * @param op1 the first operator
     * @param op2 the second operator
     * @return true if the operators are the inverse of each other
     */
    protected static boolean isInverse(int op1, int op2) {
        return op1 != op2 && op1 == Token.inverse(op2);
    }

    /**
    * Is this expression the same as another expression?
    */

    public boolean equals(Object other) {
        if (other instanceof BinaryExpression) {
            BinaryExpression b = (BinaryExpression)other;
            if (operator == b.operator) {
                if (operand0.equals(b.operand0) &&
                        operand1.equals(b.operand1)) {
                    return true;
                }
                if (isCommutative(operator) &&
                        operand0.equals(b.operand1) &&
                        operand1.equals(b.operand0)) {
                    return true;
                }
                if (isAssociative(operator) &&
                        pairwiseEqual(flattenExpression(new ArrayList(4)),
                                b.flattenExpression(new ArrayList(4)))) {
                    return true;
                }
            }
            if (isInverse(operator, b.operator) &&
                    operand0.equals(b.operand1) &&
                    operand1.equals(b.operand0)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Flatten an expression with respect to an associative operator: for example
     * the expression (a+b) + (c+d) becomes list(a,b,c,d), with the list in canonical
     * order (sorted by hashCode)
     */

    private List flattenExpression(List list) {
        if (operand0 instanceof BinaryExpression &&
                ((BinaryExpression)operand0).operator == operator) {
            ((BinaryExpression)operand0).flattenExpression(list);
        } else {
            int h = operand0.hashCode();
            list.add(operand0);
            int i = list.size()-1;
            while (i > 0 && h > list.get(i-1).hashCode()) {
                list.set(i, list.get(i-1));
                list.set(i-1, operand0);
                i--;
            }
        }
        if (operand1 instanceof BinaryExpression &&
                ((BinaryExpression)operand1).operator == operator) {
            ((BinaryExpression)operand1).flattenExpression(list);
        } else {
            int h = operand1.hashCode();
            list.add(operand1);
            int i = list.size()-1;
            while (i > 0 && h > list.get(i-1).hashCode()) {
                list.set(i, list.get(i-1));
                list.set(i-1, operand1);
                i--;
            }
        }
        return list;
    }

    /**
     * Compare whether two lists of expressions are pairwise equal
     */

    private boolean pairwiseEqual(List a, List b) {
        if (a.size() != b.size()) {
            return false;
        }
        for (int i=0; i<a.size(); i++) {
            if (!a.get(i).equals(b.get(i))) {
                return false;
            }
        }
        return true;
    }

    /**
     * Get a hashCode for comparing two expressions. Note that this hashcode gives the same
     * result for (A op B) and for (B op A), whether or not the operator is commutative.
    */

    public int hashCode() {
        // Ensure that an operator and its inverse get the same hash code,
        // so that (A lt B) has the same hash code as (B gt A)
        int op = Math.min(operator, Token.inverse(operator));
        return ("BinaryExpression " + op).hashCode()
                ^ operand0.hashCode()
                ^ operand1.hashCode();
    }

    /**
    * Diagnostic print of expression structure
    */

    public void display(int level, PrintStream out, Configuration config) {
        out.println(ExpressionTool.indent(level) + "operator " + displayOperator());
                //+ " [" + hashCode() + "]")
        operand0.display(level+1, out, config);
        operand1.display(level+1, out, config);
    }

    protected String displayOperator() {
        return Token.tokens[operator];
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
