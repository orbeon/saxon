package org.orbeon.saxon.instruct;

import org.orbeon.saxon.Controller;
import org.orbeon.saxon.expr.*;
import org.orbeon.saxon.om.Item;
import org.orbeon.saxon.om.SequenceIterator;
import org.orbeon.saxon.trace.TraceListener;
import org.orbeon.saxon.trace.ExpressionPresenter;
import org.orbeon.saxon.trace.InstructionInfo;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.type.ItemType;
import org.orbeon.saxon.type.TypeHierarchy;

import java.util.Iterator;

/**
 * This class is a tracing wrapper around any expression: it delivers the same result as the
 * wrapped expression, but traces its evaluation to a TraceListener
 */

public class TraceWrapper extends Instruction {
    Expression child;   // the instruction or other expression to be traced

    /**
     * Simplify an expression. This performs any static optimization (by rewriting the expression
     * as a different expression). The default implementation does nothing.
     *
     * @exception org.orbeon.saxon.trans.XPathException if an error is discovered during expression
     *     rewriting
     * @return the simplified expression
     * @param visitor an expression visitor
     */

    public Expression simplify(ExpressionVisitor visitor) throws XPathException {
        child = visitor.simplify(child);
        return this;
    }

    public Expression typeCheck(ExpressionVisitor visitor, ItemType contextItemType) throws XPathException {
        child = visitor.typeCheck(child, contextItemType);
        adoptChildExpression(child);
        return this;
    }

    public Expression optimize(ExpressionVisitor visitor, ItemType contextItemType) throws XPathException {
        child = visitor.optimize(child, contextItemType);
        adoptChildExpression(child);
        return this;
    }


    /**
     * Copy an expression. This makes a deep copy.
     *
     * @return the copy of the original expression
     */

    public Expression copy() {
        throw new UnsupportedOperationException("copy");
    }

    /**
     * Offer promotion for this subexpression. The offer will be accepted if the subexpression
     * is not dependent on the factors (e.g. the context item) identified in the PromotionOffer.
     * This method is always called at compile time.
     *
     * @param offer details of the offer, for example the offer to move
     *              expressions that don't depend on the context to an outer level in
     *              the containing expression
     * @return if the offer is not accepted, return this expression unchanged.
     *         Otherwise return the result of rewriting the expression to promote
     *         this subexpression
     * @throws org.orbeon.saxon.trans.XPathException
     *          if any error is detected
     */

    public Expression promote(PromotionOffer offer) throws XPathException {
        // Many rewrites are not attempted if tracing is activated. But those that are, for example
        // rewriting of calls to current(), must be carried out.
        Expression newChild = child.promote(offer);
        if (newChild != child) {
            child = newChild;
            adoptChildExpression(child);
            return this;
        }
        return this;
    }

    /**
     * Execute this instruction, with the possibility of returning tail calls if there are any.
     * This outputs the trace information via the registered TraceListener,
     * and invokes the instruction being traced.
     * @param context the dynamic execution context
     * @return either null, or a tail call that the caller must invoke on return
     * @throws org.orbeon.saxon.trans.XPathException
     */
    public TailCall processLeavingTail(XPathContext context) throws XPathException {
        Controller controller = context.getController();
        TraceListener listener = controller.getTraceListener();
    	if (controller.isTracing()) {
   	        listener.enter(getInstructionInfo(), context);
        }
        // Don't attempt tail call optimization when tracing, the results are too confusing
        child.process(context);
   	    if (controller.isTracing()) {
   	        listener.leave(getInstructionInfo());
        }
        return null;
    }

    /**
     * Get the item type of the items returned by evaluating this instruction
     * @return the static item type of the instruction
     * @param th the type hierarchy cache
     */

    public ItemType getItemType(TypeHierarchy th) {
        return child.getItemType(th);
    }

    /**
     * Determine the static cardinality of the expression. This establishes how many items
     * there will be in the result of the expression, at compile time (i.e., without
     * actually evaluating the result.
     *
     * @return one of the values Cardinality.ONE_OR_MORE,
     *         Cardinality.ZERO_OR_MORE, Cardinality.EXACTLY_ONE,
     *         Cardinality.ZERO_OR_ONE, Cardinality.EMPTY. This default
     *         implementation returns ZERO_OR_MORE (which effectively gives no
     *         information).
     */

    public int getCardinality() {
        return child.getCardinality();
    }

    /**
     * Determine which aspects of the context the expression depends on. The result is
     * a bitwise-or'ed value composed from constants such as {@link org.orbeon.saxon.expr.StaticProperty#DEPENDS_ON_CONTEXT_ITEM} and
     * {@link org.orbeon.saxon.expr.StaticProperty#DEPENDS_ON_CURRENT_ITEM}. The default implementation combines the intrinsic
     * dependencies of this expression with the dependencies of the subexpressions,
     * computed recursively. This is overridden for expressions such as FilterExpression
     * where a subexpression's dependencies are not necessarily inherited by the parent
     * expression.
     *
     * @return a set of bit-significant flags identifying the dependencies of
     *     the expression
     */

    public int getDependencies() {
        return child.getDependencies();
    }

    /**
     * Determine whether this instruction creates new nodes.
     *
     *
     */

    public final boolean createsNewNodes() {
        return (child.getSpecialProperties() & StaticProperty.NON_CREATIVE) == 0;
    }

    /**
     * Get the static properties of this expression (other than its type). The result is
     * bit-signficant. These properties are used for optimizations. In general, if
     * property bit is set, it is true, but if it is unset, the value is unknown.
     *
     * @return a set of flags indicating static properties of this expression
     */

    public int computeDependencies() {
        return child.computeDependencies();
    }

    /**
     * Evaluate an expression as a single item. This always returns either a single Item or
     * null (denoting the empty sequence). No conversion is done. This method should not be
     * used unless the static type of the expression is a subtype of "item" or "item?": that is,
     * it should not be called if the expression may return a sequence. There is no guarantee that
     * this condition will be detected.
     *
     * @param context The context in which the expression is to be evaluated
     * @exception org.orbeon.saxon.trans.XPathException if any dynamic error occurs evaluating the
     *     expression
     * @return the node or atomic value that results from evaluating the
     *     expression; or null to indicate that the result is an empty
     *     sequence
     */

    public Item evaluateItem(XPathContext context) throws XPathException {
        Controller controller = context.getController();
        if (controller.isTracing()) {
            controller.getTraceListener().enter(getInstructionInfo(), context);
        }
        Item result = child.evaluateItem(context);
        if (controller.isTracing()) {
            controller.getTraceListener().leave(getInstructionInfo());
        }
        return result;
    }

    /**
     * Return an Iterator to iterate over the values of a sequence. The value of every
     * expression can be regarded as a sequence, so this method is supported for all
     * expressions. This default implementation handles iteration for expressions that
     * return singleton values: for non-singleton expressions, the subclass must
     * provide its own implementation.
     *
     * @exception org.orbeon.saxon.trans.XPathException if any dynamic error occurs evaluating the
     *     expression
     * @param context supplies the context for evaluation
     * @return a SequenceIterator that can be used to iterate over the result
     *     of the expression
     */

    public SequenceIterator iterate(XPathContext context) throws XPathException {
        Controller controller = context.getController();
        if (controller.isTracing()) {
            controller.getTraceListener().enter(getInstructionInfo(), context);
        }
        SequenceIterator result = child.iterate(context);
        if (controller.isTracing()) {
            controller.getTraceListener().leave(getInstructionInfo());
        }
        return result;
    }

    public Iterator iterateSubExpressions() {
        return new MonoIterator(child);
    }

    /**
      * Replace one subexpression by a replacement subexpression
      * @param original the original subexpression
      * @param replacement the replacement subexpression
      * @return true if the original subexpression is found
      */

     public boolean replaceSubExpression(Expression original, Expression replacement) {
         boolean found = false;
         if (child == original) {
             child = replacement;
             found = true;
         }
         return found;
     }

    /**
     * Get the instruction details
     * @return the details of the child instruction (the instruction being traced)
     */

    public InstructionInfo getInstructionInfo() {
        return child;
    }


    public int getInstructionNameCode() {
        if (child instanceof Instruction) {
            return ((Instruction)child).getInstructionNameCode();
        } else {
            return -1;
        }
    }

    /**
     * Diagnostic print of expression structure. The abstract expression tree
     * is written to the supplied output destination.
     */

    public void explain(ExpressionPresenter out) {
        child.explain(out);
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
