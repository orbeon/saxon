package net.sf.saxon.expr;
import net.sf.saxon.om.NamePool;
import net.sf.saxon.type.ItemType;
import net.sf.saxon.value.Value;
import net.sf.saxon.xpath.DynamicError;
import net.sf.saxon.xpath.XPathException;

import java.util.Iterator;
import java.io.PrintStream;

/**
* Unary Expression: an expression taking a single operand expression
*/

public abstract class UnaryExpression extends ComputedExpression {

    protected Expression operand;

    public UnaryExpression(Expression p0) {
        operand = p0;
        adoptChildExpression(p0);
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
    * Type-check the expression. Default implementation for binary operators that accept
    * any kind of operand
    */

    public Expression analyze(StaticContext env, ItemType contextItemType) throws XPathException {
        operand = operand.analyze(env, contextItemType);
        // if the operand value is known, pre-evaluate the expression
        try {
            if (operand instanceof Value) {
                return ExpressionTool.eagerEvaluate(this, null);
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
        operand = operand.promote(offer);
        return this;
    }

    /**
    * Get the immediate subexpressions of this expression
    */

    public Iterator iterateSubExpressions() {
        return new MonoIterator(operand);
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
     */

    public ItemType getItemType() {
        return operand.getItemType();
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

    public void display(int level, NamePool pool, PrintStream out) {
        out.println(ExpressionTool.indent(level) + displayOperator(pool));
        operand.display(level+1, pool, out);
    }

    /**
     * Give a string representation of the operator for use in diagnostics
     * @return the operator, as a string
     */

    protected abstract String displayOperator(NamePool pool);

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
