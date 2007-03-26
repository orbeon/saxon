package org.orbeon.saxon.expr;
import org.orbeon.saxon.Configuration;
import org.orbeon.saxon.trans.DynamicError;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.type.ItemType;
import org.orbeon.saxon.type.TypeHierarchy;
import org.orbeon.saxon.value.Value;
import org.orbeon.saxon.value.SequenceExtent;

import java.io.PrintStream;
import java.util.Iterator;

/**
* Unary Expression: an expression taking a single operand expression
*/

public abstract class UnaryExpression extends ComputedExpression {

    protected Expression operand;

    public UnaryExpression(Expression p0) {
        operand = p0;
        Container parent = (p0 instanceof ComputedExpression ? ((ComputedExpression)p0).getParentExpression() : null);
        adoptChildExpression(p0);
        setParentExpression(parent);
    }

    public Expression getBaseExpression() {
        return operand;
    }

    /**
    * Simplify an expression
    * @return the simplified expression
    */

     public Expression simplify(StaticContext env) throws XPathException {
        operand = operand.simplify(env);
        return this;
    }

    /**
    * Type-check the expression. Default implementation for unary operators that accept
    * any kind of operand
    */

    public Expression typeCheck(StaticContext env, ItemType contextItemType) throws XPathException {
        operand = operand.typeCheck(env, contextItemType);
        // if the operand value is known, pre-evaluate the expression
        try {
            if (operand instanceof Value) {
                return Value.asValue(
                        SequenceExtent.makeSequenceExtent(
                                iterate(env.makeEarlyEvaluationContext())));
            }
                //return (Value)ExpressionTool.eagerEvaluate(this, env.makeEarlyEvaluationContext());
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
        operand = operand.optimize(opt, env, contextItemType);
        // if the operand value is known, pre-evaluate the expression
        try {
            if (operand instanceof Value) {
                return Value.asValue(
                        SequenceExtent.makeSequenceExtent(
                                iterate(env.makeEarlyEvaluationContext())));
                //return (Value)ExpressionTool.eagerEvaluate(this, env.makeEarlyEvaluationContext());
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
            operand = doPromotion(operand, offer);
            return this;
        }
    }

    /**
    * Get the immediate subexpressions of this expression
    */

    public Iterator iterateSubExpressions() {
        return new MonoIterator(operand);
    }

    /**
      * Replace one subexpression by a replacement subexpression
      * @param original the original subexpression
      * @param replacement the replacement subexpression
      * @return true if the original subexpression is found
      */

     public boolean replaceSubExpression(Expression original, Expression replacement) {
         boolean found = false;
         if (operand == original) {
             operand = replacement;
             found = true;
         }
                 return found;
     }


    /**
    * Get the static properties of this expression (other than its type). The result is
    * bit-signficant. These properties are used for optimizations. In general, if
    * property bit is set, it is true, but if it is unset, the value is unknown.
    */

    public int computeSpecialProperties() {
        return operand.getSpecialProperties();
    }

    /**
    * Determine the static cardinality. Default implementation returns the cardinality of the operand
    */

    public int computeCardinality() {
        return operand.getCardinality();
    }

    /**
     * Determine the data type of the expression, if possible. The default
     * implementation for unary expressions returns the item type of the operand
     * @return the item type of the items in the result sequence, insofar as this
     * is known statically.
     * @param th
     */

    public ItemType getItemType(TypeHierarchy th) {
        return operand.getItemType(th);
    }

    /**
    * Is this expression the same as another expression?
    */

    public boolean equals(Object other) {
        return this.getClass().equals(other.getClass()) &&
                this.operand.equals(((UnaryExpression)other).operand);
    }

    /**
    * get HashCode for comparing two expressions. Note that this hashcode gives the same
     * result for (A op B) and for (B op A), whether or not the operator is commutative.
    */

    public int hashCode() {
        return ("UnaryExpression " + getClass()).hashCode()  ^ operand.hashCode();
    }

    /**
    * Diagnostic print of expression structure
    */

    public void display(int level, PrintStream out, Configuration config) {
        out.println(ExpressionTool.indent(level) + displayOperator(config));
        operand.display(level+1, out, config);
    }

    /**
     * Give a string representation of the operator for use in diagnostics
     * @return the operator, as a string
     * @param config
     */

    protected abstract String displayOperator(Configuration config);

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
