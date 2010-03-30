package org.orbeon.saxon.expr;

import org.orbeon.saxon.om.Item;
import org.orbeon.saxon.om.SequenceIterator;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.type.ItemType;

/**
 * A LazyExpression is an expression that forces lazy evaluation: it must not be evaluated eagerly,
 * because a failure must not be reported unless the value is actually referenced. This is used
 * for an expression that has been moved out of a loop. If the loop iterates zero times, the expression
 * will not be evaluated, and in particular, it will not cause a dynamic error.
 *
 * <p>Note that the LazyExpression class does not itself implement any kind of delayed evaluation:
 * calling its evaluateItem() and iterate() methods produces an immediate result. Instead, the existence
 * of a LazyExpression on the expression tree acts as a signal to other classes that evaluation should
 * be delayed, typically by holding the result of the iterate() method in a Closure object.</p>
 */

public class LazyExpression extends UnaryExpression {

    /**
     * Create a LazyExpression
     * @param operand the expression to be evaluated lazily
     */

    public LazyExpression(Expression operand) {
        super(operand);
    }

    /**
     * Create a LazyExpression (factory method)
     * @param operand the expression to be evaluated lazily
     * @return the LazyExpression
     */

    public static Expression makeLazyExpression(Expression operand) {
        if (operand instanceof LazyExpression || operand instanceof Literal) {
            return operand;
        } else {
            return new LazyExpression(operand);
        }
    }

    /**
     * The typeCheck method suppresses compile-time evaluation
     * @param visitor an expression visitor
     * @param contextItemType the static type of the context item
     * @return the expression after typechecking
     * @throws XPathException
     */

    public Expression typeCheck(ExpressionVisitor visitor, ItemType contextItemType) throws XPathException {
        operand = visitor.typeCheck(operand, contextItemType);
        return this;
    }

    /**
     * Perform optimisation of an expression and its subexpressions. This method
     * suppresses compile-time evaluation
     */

    public Expression optimize(ExpressionVisitor visitor, ItemType contextItemType) throws XPathException {
        operand = visitor.optimize(operand, contextItemType);
        return this;
    }


    /**
     * Evaluate an expression as a single item. This always returns either a single Item or
     * null (denoting the empty sequence). No conversion is done. This method should not be
     * used unless the static type of the expression is a subtype of "item" or "item?": that is,
     * it should not be called if the expression may return a sequence. There is no guarantee that
     * this condition will be detected.
     *
     * @param context The context in which the expression is to be evaluated
     * @return the node or atomic value that results from evaluating the
     *         expression; or null to indicate that the result is an empty
     *         sequence
     * @throws org.orbeon.saxon.trans.XPathException
     *          if any dynamic error occurs evaluating the
     *          expression
     */

    public Item evaluateItem(XPathContext context) throws XPathException {
        return operand.evaluateItem(context);
    }

    /**
     * Return an Iterator to iterate over the values of a sequence. The value of every
     * expression can be regarded as a sequence, so this method is supported for all
     * expressions. This default implementation handles iteration for expressions that
     * return singleton values: for non-singleton expressions, the subclass must
     * provide its own implementation.
     *
     * @param context supplies the context for evaluation
     * @return a SequenceIterator that can be used to iterate over the result
     *         of the expression
     * @throws org.orbeon.saxon.trans.XPathException
     *          if any dynamic error occurs evaluating the
     *          expression
     */

    public SequenceIterator iterate(XPathContext context) throws XPathException {
        return operand.iterate(context);
    }

    /**
     * Process the instruction, without returning any tail calls
     *
     * @param context The dynamic context, giving access to the current node,
     *                the current variables, etc.
     */

    public void process(XPathContext context) throws XPathException {
        operand.process(context);
    }

    /**
     * Copy an expression. This makes a deep copy.
     *
     * @return the copy of the original expression
     */

    public Expression copy() {
        return new LazyExpression(getBaseExpression().copy());
    }

    protected String displayExpressionName() {
        return "lazy";
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

