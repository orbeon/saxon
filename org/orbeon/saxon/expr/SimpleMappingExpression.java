package org.orbeon.saxon.expr;

import org.orbeon.saxon.om.*;
import org.orbeon.saxon.sort.DocumentOrderIterator;
import org.orbeon.saxon.sort.GlobalOrderComparer;
import org.orbeon.saxon.trace.Location;
import org.orbeon.saxon.type.ItemType;
import org.orbeon.saxon.value.AtomicValue;
import org.orbeon.saxon.value.Cardinality;
import org.orbeon.saxon.xpath.DynamicError;
import org.orbeon.saxon.xpath.XPathException;

import java.io.PrintStream;
import java.util.Iterator;

/**
 * A simple mapping expression is an expression A/B where B has a static type that is an atomic type.
 * For example, * / name().
 */

public final class SimpleMappingExpression extends ComputedExpression implements MappingFunction {

    private Expression start;
    private Expression step;
    private boolean isHybrid;

    /**
     * Constructor
     * @param start A node-set expression denoting the absolute or relative set of nodes from which the
     * navigation path should start.
     * @param step The step to be followed from each node in the start expression to yield a new
     * node-set
     * @param isHybrid if true, indicates that we don't know statically whether the step expression will
     * return nodes or atomic values. If false, we know it will return atomic values.
     */

    public SimpleMappingExpression(Expression start, Expression step, boolean isHybrid) {
        this.start = start;
        this.step = step;
        this.isHybrid = isHybrid;
        adoptChildExpression(start);
        adoptChildExpression(step);

    }

    /**
     * Get the start expression (the left-hand operand)
     */

    public Expression getStartExpression() {
        return start;
    }

    /**
     * Get the step expression (the right-hand operand)
     */

    public Expression getStepExpression() {
        return step;
    }
    /**
     * Determine the data type of the items returned by this exprssion
     * @return the type of the step
     */

    public final ItemType getItemType() {
        return step.getItemType();
    }

    /**
     * Simplify an expression
     * @return the simplified expression
     */

     public Expression simplify(StaticContext env) {
        // We rely on the fact that the operands were already simplified
        return this;
    }

    /**
     * Type-check the expression
     */

    public Expression analyze(StaticContext env, ItemType contextItemType) {
        // rely on the fact that the original path expression has already been analyzed
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
            start = start.promote(offer);
            if (offer.action == PromotionOffer.INLINE_VARIABLE_REFERENCES) {
                // Don't pass on other requests. We could pass them on, but only after augmenting
                // them to say we are interested in subexpressions that don't depend on either the
                // outer context or the inner context.
                step = step.promote(offer);
            }
            resetStaticProperties();
            return this;
        }
    }

    /**
     * Get the immediate subexpressions of this expression
     */

    public Iterator iterateSubExpressions() {
        return new PairIterator(start, step);
    }
    /**
     * Determine which aspects of the context the expression depends on. The result is
     * a bitwise-or'ed value composed from constants such as XPathContext.VARIABLES and
     * XPathContext.CURRENT_NODE
     */

    public int computeDependencies() {
        return start.getDependencies() |
                // not all dependencies in the step matter, because the context node, etc,
                // are not those of the outer expression
                (step.getDependencies() & StaticProperty.DEPENDS_ON_XSLT_CONTEXT);
    }

    /**
     * Get the static properties of this expression (other than its type). The result is
     * bit-signficant. These properties are used for optimizations. In general, if
     * property bit is set, it is true, but if it is unset, the value is unknown.
     */

    public int computeSpecialProperties() {
        int p = super.computeSpecialProperties();
        if ((start.getSpecialProperties() & step.getSpecialProperties() & StaticProperty.NON_CREATIVE) != 0) {
            p |= StaticProperty.NON_CREATIVE;
        }
        return p;
    }

    /**
     * Determine the static cardinality of the expression
     */

    public int computeCardinality() {
        int c1 = start.getCardinality();
        int c2 = step.getCardinality();
        return Cardinality.multiply(c1, c2);
    }

    /**
     * Is this expression the same as another expression?
     */

    public boolean equals(Object other) {
        if (!(other instanceof SimpleMappingExpression)) {
            return false;
        }
        SimpleMappingExpression p = (SimpleMappingExpression) other;
        return (start.equals(p.start) && step.equals(p.step));
    }

    /**
     * get HashCode for comparing two expressions
     */

    public int hashCode() {
        return "SimpleMappingExpression".hashCode() + start.hashCode() + step.hashCode();
    }

    /**
     * Iterate the path-expression in a given context
     * @param context the evaluation context
     */

    public SequenceIterator iterate(XPathContext context) throws XPathException {

        // This class delivers the result of the path expression in unsorted order,
        // without removal of duplicates. If sorting and deduplication are needed,
        // this is achieved by wrapping the path expression in a DocumentSorter

        SequenceIterator result = start.iterate(context);
        XPathContext context2 = context.newMinorContext();
        context2.setCurrentIterator(result);
        context2.setOriginatingConstructType(Location.PATH_EXPRESSION);

        result = new MappingIterator(result, this, context2, null);
        if (isHybrid) {
            // This case is rare so we don't worry too much about performance
            // Peek at the first node, and depending on its type, check that all the items
            // are atomic values or that all are nodes.
            Item first = result.next();
            if (first == null) {
                return EmptyIterator.getInstance();
            } else if (first instanceof AtomicValue) {
                return new MappingIterator(result.getAnother(), new AtomicValueChecker(), null, null);
            } else {
                return new DocumentOrderIterator(
                    new MappingIterator(result.getAnother(), new NodeChecker(), null, null),
                    GlobalOrderComparer.getInstance());
            }
        } else {
            return result;
        }
    }

    /**
     * Mapping function, from a node returned by the start iteration, to a sequence
     * returned by the child.
     */

    public Object map(Item item, XPathContext context, Object info) throws XPathException {
        return step.iterate(context);
    }

    /**
     * Diagnostic print of expression structure
     */

    public void display(int level, NamePool pool, PrintStream out) {
        out.println(ExpressionTool.indent(level) + "map /");
        start.display(level + 1, pool, out);
        step.display(level + 1, pool, out);
    }

    private static class AtomicValueChecker implements MappingFunction {
        public Object map(Item item, XPathContext context, Object info) throws XPathException {
            if (item instanceof AtomicValue) {
                return item;
            } else {
                DynamicError err = new DynamicError(
                        "Cannot mix nodes and atomic values in the result of a path expression");
                err.setErrorCode("XP0018");
                err.setXPathContext(context);
                throw err;
            }
        }
    }

   private static class NodeChecker implements MappingFunction {
        public Object map(Item item, XPathContext context, Object info) throws XPathException {
            if (item instanceof NodeInfo) {
                return item;
            } else {
                DynamicError err = new DynamicError(
                        "Cannot mix nodes and atomic values in the result of a path expression");
                err.setErrorCode("XP0018");
                err.setXPathContext(context);
                throw err;
            }
        }
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
