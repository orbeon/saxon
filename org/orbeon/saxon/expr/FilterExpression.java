package org.orbeon.saxon.expr;
import org.orbeon.saxon.functions.Last;
import org.orbeon.saxon.functions.SystemFunction;
import org.orbeon.saxon.om.EmptyIterator;
import org.orbeon.saxon.om.NamePool;
import org.orbeon.saxon.om.SequenceIterator;
import org.orbeon.saxon.om.SingletonIterator;
import org.orbeon.saxon.type.AnyItemType;
import org.orbeon.saxon.type.ItemType;
import org.orbeon.saxon.type.Type;
import org.orbeon.saxon.value.*;
import org.orbeon.saxon.xpath.StaticError;
import org.orbeon.saxon.xpath.XPathException;

import java.io.PrintStream;
import java.util.Iterator;

/**
* A FilterExpression contains a base expression and a filter predicate, which may be an
* integer expression (positional filter), or a boolean expression (qualifier)
*/

public final class FilterExpression extends ComputedExpression {

    private Expression start;
    private Expression filter;
    private int filterDependencies;         // the aspects of the context on which the filter
                                            // expression depends
    private boolean filterIsPositional;     // true if the value of the filter might depend on
                                            // the context position

    /**
    * Constructor
    * @param start A node-set expression denoting the absolute or relative set of nodes from which the
    * navigation path should start.
    * @param filter An expression defining the filter predicate
    */

    public FilterExpression(Expression start, Expression filter, StaticContext env) throws StaticError {
        this.start = start;
        this.filter = filter;
        adoptChildExpression(start);
        adoptChildExpression(filter);

        try {
            filterDependencies = filter.simplify(env).getDependencies();
        } catch (XPathException e) {
            throw e.makeStatic();
        }

        // the reason we simplify the filter before checking its dependencies
            // is to ensure that functions like name() are expanded to use the
            // context node as an implicit argument
    }

    /**
    * Get the data type of the items returned
    * @return an integer representing the data type
    */

    public ItemType getItemType() {
        return start.getItemType();
    }

    /**
    * Get the underlying expression
    * @return the expression being filtered
    */

    public Expression getBaseExpression() {
        return start;
    }

    /**
    * Get the filter expression
    * @return the expression acting as the filter predicate
    */

    public Expression getFilter() {
        return filter;
    }

    /**
    * Determine if the filter is positional
    * @return true if the value of the filter depends on the position of the item against
    * which it is evaluated
    */

    public boolean isPositional() {
        return isPositionalFilter(filter);
    }

    /**
    * Simplify an expression
    * @return the simplified expression
    * @throws XPathException if any failure occurs
    */

     public Expression simplify(StaticContext env) throws XPathException {

        start = start.simplify(env);
        filter = filter.simplify(env);

        // ignore the filter if the base expression is an empty sequence
        if (start instanceof EmptySequence) {
            return start;
        }

        // check whether the filter is a constant true() or false()
        if (filter instanceof Value && !(filter instanceof NumericValue)) {
            try {
                if (filter.effectiveBooleanValue(null)) {
                    return start;
                } else {
                    return EmptySequence.getInstance();
                }
            } catch (XPathException e) {
                throw new StaticError(e);
                // Cannot happen
            }
        }

        // check whether the filter is [last()] (note, position()=last() is handled elsewhere)

        if (filter instanceof Last) {
            filter = new IsLastExpression(true);
        }

        return this;

    }

    /**
    * Type-check the expression
    * @param env the static context
    * @return the expression after type-checking (potentially modified to add run-time
    * checks and/or conversions)
    */

    public Expression analyze(StaticContext env, ItemType contextItemType) throws XPathException {

        start = start.analyze(env, contextItemType);
        filter = filter.analyze(env, start.getItemType());

        // The filter expression never needs to be sorted

        filter = ExpressionTool.unsorted(filter, false);

        // detect head expressions (E[1]) and tail expressions (E[position()!=1])
        // and treat them specially

        if (filter instanceof IntegerValue) {
            if (((IntegerValue)filter).longValue() == 1) {
                return new FirstItemExpression(start);
            }
        }

        if (filter instanceof PositionRange) {
            PositionRange range = (PositionRange)filter;
            int min = range.getMinPosition();
            int max = range.getMaxPosition();
            if (min == 1 && max == 1) {
                return new FirstItemExpression(start);
            }
            if (max == Integer.MAX_VALUE) {
                return new TailExpression(start, min);
            }
        }

        // determine whether the filter might depend on position
        filterIsPositional = isPositionalFilter(filter);

        // if the filter is positional, try changing f[a and b] to f[a][b] to increase
        // the chances of finishing early.

        if (filterIsPositional &&
                filter instanceof BooleanExpression &&
                ((BooleanExpression)filter).operator == Token.AND) {
            BooleanExpression bf = (BooleanExpression)filter;
            if (isExplicitlyPositional(bf.operand0) &&
                    !isExplicitlyPositional(bf.operand1)) {
                Expression p0 = forceToBoolean(bf.operand0, env.getNamePool());
                Expression p1 = forceToBoolean(bf.operand1, env.getNamePool());
                FilterExpression f1 = new FilterExpression(start, p0, env);
                FilterExpression f2 = new FilterExpression(f1, p1, env);
                //System.err.println("Simplified to: ");
                //f2.display(10);
                return f2.analyze(env, contextItemType);
            }
            if (isExplicitlyPositional(bf.operand1) &&
                    !isExplicitlyPositional(bf.operand0)) {
                Expression p0 = forceToBoolean(bf.operand0, env.getNamePool());
                Expression p1 = forceToBoolean(bf.operand1, env.getNamePool());
                FilterExpression f1 = new FilterExpression(start, p1, env);
                FilterExpression f2 = new FilterExpression(f1, p0, env);
                //System.err.println("Simplified to: ");
                //f2.display(10);
                return f2.analyze(env, contextItemType);
            }
        }

        // If any subexpressions within the filter are not dependent on the focus,
        // promote them: this causes them to be evaluated once, outside the filter
        // expression

        PromotionOffer offer = new PromotionOffer();
        offer.action = PromotionOffer.FOCUS_INDEPENDENT;
        offer.promoteDocumentDependent = (start.getSpecialProperties() & StaticProperty.CONTEXT_DOCUMENT_NODESET) != 0;
        offer.containingExpression = this;
        filter = filter.promote(offer);

        if (offer.containingExpression instanceof LetExpression) {
            offer.containingExpression = offer.containingExpression.analyze(env, contextItemType);
        }

        return offer.containingExpression;
    }

    /**
     * Construct an expression that obtains the effective boolean value of a given expression,
     * by wrapping it in a call of the boolean() function
     */

    private static Expression forceToBoolean(Expression in, NamePool namePool) {
        if (in.getItemType().getPrimitiveType() == Type.BOOLEAN) {
            return in;
        }
        Expression[] args = {in};
        FunctionCall fn = SystemFunction.makeSystemFunction("boolean", 1, namePool);
        fn.setArguments(args);
        return fn;
    }

    /**
    * Promote this expression if possible
    * @param offer details of the promotion that is possible
    * @return the promoted expression (or the original expression, unchanged)
    */

    public Expression promote(PromotionOffer offer) throws XPathException {
        Expression exp = offer.accept(this);
        if (exp != null) {
            return exp;
        } else {
            if (!(offer.action == PromotionOffer.UNORDERED && filterIsPositional)) {
                start = start.promote(offer);
            }
            if (offer.action == PromotionOffer.INLINE_VARIABLE_REFERENCES) {
                // Don't pass on other requests. We could pass them on, but only after augmenting
                // them to say we are interested in subexpressions that don't depend on either the
                // outer context or the inner context.
                filter = filter.promote(offer);
            }
            return this;
        }
    }

    /**
    * Determine whether an expression, when used as a filter, is positional
    */

    private static boolean isPositionalFilter(Expression exp) {
        ItemType type = exp.getItemType();
        return ( type==Type.ANY_ATOMIC_TYPE ||
                 type instanceof AnyItemType ||
                 Type.isSubType(type, Type.NUMBER_TYPE) ||
                 isExplicitlyPositional(exp));
    }

    /**
    * Determine whether an expression, when used as a filter, has an explicit dependency on position() or last()
    */

    private static boolean isExplicitlyPositional(Expression exp) {
        return (exp.getDependencies() & (StaticProperty.DEPENDS_ON_POSITION|StaticProperty.DEPENDS_ON_LAST)) != 0;
    }

    /**
    * Get the immediate subexpressions of this expression
    * @return the subexpressions, as an array
    */

    public Iterator iterateSubExpressions() {
        return new PairIterator(start, filter);
    }

    /**
    * Get the static cardinality of this expression
    * @return the cardinality. The method attempts to determine the case where the
    * filter predicate is guaranteed to select at most one item from the sequence being filtered
    */

    public int computeCardinality() {
        if (filter instanceof NumericValue) {
            return StaticProperty.ALLOWS_ZERO_OR_ONE;
        }
        if (!Cardinality.allowsMany(start.getCardinality())) {
            return StaticProperty.ALLOWS_ZERO_OR_ONE;
        }
        if (filter instanceof PositionRange) {
            PositionRange p = (PositionRange)filter;
            if (p.getMinPosition() == p.getMaxPosition()) {
                return StaticProperty.ALLOWS_ZERO_OR_ONE;
            }
        }
        if (filter instanceof IsLastExpression && ((IsLastExpression)filter).getCondition()) {
            return StaticProperty.ALLOWS_ZERO_OR_ONE;
        }
        int sc = start.getCardinality();
        switch (sc) {
            case StaticProperty.ALLOWS_ONE_OR_MORE:
                return StaticProperty.ALLOWS_ZERO_OR_MORE;
            case StaticProperty.EXACTLY_ONE:
                return StaticProperty.ALLOWS_ZERO_OR_ONE;
            default:
                return sc;
        }
    }

    /**
    * Get the static properties of this expression (other than its type). The result is
    * bit-significant. These properties are used for optimizations. In general, if
    * property bit is set, it is true, but if it is unset, the value is unknown.
    * @return the static properties of the expression, as a bit-significant value
    */

    public int computeSpecialProperties() {
        return start.getSpecialProperties();
    }

    /**
    * Is this expression the same as another expression?
    * @param other the expression to be compared with this one
    * @return true if the two expressions are statically equivalent
    */

    public boolean equals(Object other) {
        if (other instanceof FilterExpression) {
            FilterExpression f = (FilterExpression)other;
            return (start.equals(f.start) &&
                    filter.equals(f.filter));
        }
        return false;
    }

    /**
    * get HashCode for comparing two expressions
    * @return the hash code
    */

    public int hashCode() {
        return "FilterExpression".hashCode() + start.hashCode() + filter.hashCode();
    }

    /**
    * Iterate over the results, returning them in the correct order
    * @param context the dynamic context for the evaluation
    * @return an iterator over the expression results
    * @throws XPathException if any dynamic error occurs
    */

    public SequenceIterator iterate(XPathContext context) throws XPathException {

        // fast path where both operands are constants

        if (start instanceof SequenceValue && filter instanceof IntegerValue) {
            int pos = (int)((IntegerValue)filter).longValue();
            return SingletonIterator.makeIterator(((SequenceValue)start).itemAt(pos-1));
        }

        // get an iterator over the base nodes

        SequenceIterator base = start.iterate(context);

        // quick exit for an empty sequence

        if (base instanceof EmptyIterator) {
            return base;
        }

        // The filter expression might be a variable reference as a result of previous optimization.
        // This case can be handled like a constant value

        Expression filterValue = filter;
        if (filter instanceof VariableReference) {
            filterValue = ((VariableReference)filter).evaluateVariable(context);
        }

		// Test whether the filter is (now) a constant value

		if (filterValue instanceof Value) {
		    if (filterValue instanceof NumericValue) {
		        // Filter is a constant number
		        if (((NumericValue)filterValue).isWholeNumber()) {
		            int pos = (int)(((NumericValue)filterValue).longValue());
		            if (pos >= 1) {
		                return PositionIterator.make(base, pos, pos);
		            } else {
		                // index is less than one, no items will be selected
		                return EmptyIterator.getInstance();
		            }
		        } else {
		            // a non-integer value will never be equal to position()
		            return EmptyIterator.getInstance();
		        }
		    } else {
		        // Filter is a constant that we can treat as boolean
		        if (filterValue.effectiveBooleanValue(context)) {
		            return base;
		        } else {
		            return EmptyIterator.getInstance();
		        }
		    }
		}

        // Construct the FilterIterator to do the actual filtering

        SequenceIterator result;

		// Test whether the filter is a position range, e.g. [position()>$x]
        // TODO: handle all such cases with a TailExpression

        if (filter instanceof PositionRange) {
            PositionRange pr = (PositionRange)filter;
            // System.err.println("Using PositionIterator requireSort=" + requireSort + " isReverse=" + isReverseAxisFilter);
            result = PositionIterator.make(base, pr.getMinPosition(), pr.getMaxPosition());

        } else if (filterIsPositional) {
            result = new FilterIterator(base, filter, context);

        } else {
            result = new FilterIterator.NonNumeric(base, filter, context);
        }

        return result;
    }

    /**
    * Determine which aspects of the context the expression depends on. The result is
    * a bitwise-or'ed value composed from constants such as XPathContext.VARIABLES and
    * XPathContext.CURRENT_NODE
    * @return the dependencies
    */

    public int computeDependencies() {
        // not all dependencies in the filter expression matter, because the context node,
        // position, and size are not dependent on the outer context.
        return (start.getDependencies() |
                (filterDependencies & StaticProperty.DEPENDS_ON_XSLT_CONTEXT));
    }



    /**
    * Diagnostic print of expression structure
    * @param level the indentation level
     * @param out
     */

    public void display(int level, NamePool pool, PrintStream out) {
        out.println(ExpressionTool.indent(level) + "filter []");
        start.display(level+1, pool, out);
        filter.display(level+1, pool, out);
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
