package org.orbeon.saxon.expr;
import org.orbeon.saxon.Configuration;
import org.orbeon.saxon.functions.Last;
import org.orbeon.saxon.functions.SystemFunction;
import org.orbeon.saxon.om.*;
import org.orbeon.saxon.trans.StaticError;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.type.AnyItemType;
import org.orbeon.saxon.type.ItemType;
import org.orbeon.saxon.type.Type;
import org.orbeon.saxon.type.TypeHierarchy;
import org.orbeon.saxon.value.*;

import java.io.PrintStream;
import java.util.Iterator;

/**
* A FilterExpression contains a base expression and a filter predicate, which may be an
* integer expression (positional filter), or a boolean expression (qualifier)
*/

public final class FilterExpression extends ComputedExpression {

    private Expression start;
    private Expression filter;
    private boolean filterIsPositional;         // true if the value of the filter might depend on
                                                // the context position
    private boolean filterIsSingletonBoolean;   // true if the filter expression always returns a single boolean
    private int isIndexable = 0;

    /**
    * Constructor
    * @param start A node-set expression denoting the absolute or relative set of nodes from which the
    * navigation path should start.
     * @param filter An expression defining the filter predicate
     */

    public FilterExpression(Expression start, Expression filter) {
        this.start = start;
        this.filter = filter;
        adoptChildExpression(start);
        adoptChildExpression(filter);
    }

    /**
     * Indicate that the string value or typed value of nodes returned by this expression is used
     */

    public void setStringValueIsUsed() {
        if (start instanceof ComputedExpression) {
            ((ComputedExpression)start).setStringValueIsUsed();
        }
    }


    /**
    * Get the data type of the items returned
    * @return an integer representing the data type
     * @param th
     */

    public ItemType getItemType(TypeHierarchy th) {
        return start.getItemType(th);
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
     * @param th the Type Hierarchy (for cached access to type information)
     */

    public boolean isPositional(TypeHierarchy th) {
        return isPositionalFilter(filter, th);
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
                    ComputedExpression.setParentExpression(start, getParentExpression());
                    return start;
                } else {
                    return EmptySequence.getInstance();
                }
            } catch (XPathException e) {
                throw StaticError.makeStaticError(e);
            }
        }

        // check whether the filter is [last()] (note, position()=last() is handled elsewhere)

        if (filter instanceof Last) {
            filter = new IsLastExpression(true);
            adoptChildExpression(filter);
        }

        return this;

    }

    /**
    * Type-check the expression
    * @param env the static context
    * @return the expression after type-checking (potentially modified to add run-time
    * checks and/or conversions)
    */

    public Expression typeCheck(StaticContext env, ItemType contextItemType) throws XPathException {

        final TypeHierarchy th = env.getConfiguration().getTypeHierarchy();
        Expression start2 = start.typeCheck(env, contextItemType);
        if (start2 != start) {
            start = start2;
            adoptChildExpression(start2);
        }
        Expression filter2 = filter.typeCheck(env, start.getItemType(th));
        if (filter2 != filter) {
            filter = filter2;
            adoptChildExpression(filter2);
        }

        // The filter expression usually doesn't need to be sorted

        filter2 = ExpressionTool.unsortedIfHomogeneous(env.getConfiguration().getOptimizer(), filter, false);
        if (filter2 != filter) {
            filter = filter2;
            adoptChildExpression(filter2);
        }

        // detect head expressions (E[1]) and tail expressions (E[position()!=1])
        // and treat them specially

        if (filter instanceof IntegerValue) {
            if (((IntegerValue)filter).longValue() == 1) {
                FirstItemExpression fie = new FirstItemExpression(start);
                fie.setParentExpression(getParentExpression());
                fie.setLocationId(getLocationId());
                return fie;
            }
        }

        if (filter instanceof PositionRange) {
            PositionRange range = (PositionRange)filter;
            if (range.isFirstPositionOnly()) {
                FirstItemExpression fie = new FirstItemExpression(start);
                fie.setParentExpression(getParentExpression());
                fie.setLocationId(getLocationId());
                return fie;
            }
            TailExpression tail = range.makeTailExpression(start);
            if (tail != null) {
                tail.setParentExpression(getParentExpression());
                tail.setLocationId(getLocationId());
                return tail;
            }
        }

        // determine whether the filter might depend on position
        filterIsPositional = isPositionalFilter(filter, th);
        filterIsSingletonBoolean =
                filter.getCardinality() == StaticProperty.EXACTLY_ONE &&
                filter.getItemType(th) == Type.BOOLEAN_TYPE;

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

        final TypeHierarchy th = env.getConfiguration().getTypeHierarchy();
        Expression start2 = start.optimize(opt, env, contextItemType);
        if (start2 != start) {
            start = start2;
            adoptChildExpression(start2);
        }
        Expression filter2 = filter.optimize(opt, env, start.getItemType(th));
        if (filter2 != filter) {
            filter = filter2;
            adoptChildExpression(filter2);
        }

        // The filter expression usually doesn't need to be sorted

        filter2 = ExpressionTool.unsortedIfHomogeneous(opt, filter, false);
        if (filter2 != filter) {
            filter = filter2;
            adoptChildExpression(filter2);
        }

        // detect head expressions (E[1]) and tail expressions (E[position()!=1])
        // and treat them specially

        if (filter instanceof IntegerValue) {
            if (((IntegerValue)filter).longValue() == 1) {
                FirstItemExpression fie = new FirstItemExpression(start);
                fie.setLocationId(getLocationId());
                fie.setParentExpression(getParentExpression());
                return fie;
            }
        }

        // the filter expression may have been reduced to a constant boolean by previous optimizations

        if (filter instanceof BooleanValue) {
            if (((BooleanValue)filter).getBooleanValue()) {
                ComputedExpression.setParentExpression(start, getParentExpression());
                return start;
            } else {
                return EmptySequence.getInstance();
            }
        }

        if (filter instanceof PositionRange) {
            PositionRange range = (PositionRange)filter;
            if (range.isFirstPositionOnly()) {
                FirstItemExpression fie = new FirstItemExpression(start);
                fie.setLocationId(getLocationId());
                fie.setParentExpression(getParentExpression());
                return fie;
            }
            TailExpression tail = range.makeTailExpression(start);
            if (tail != null) {
                tail.setParentExpression(getParentExpression());
                tail.setLocationId(getLocationId());
                return tail;
            }
        }

        // determine whether the filter might depend on position
        filterIsPositional = isPositionalFilter(filter, th);
        filterIsSingletonBoolean =
                        filter.getCardinality() == StaticProperty.EXACTLY_ONE &&
                        filter.getItemType(th) == Type.BOOLEAN_TYPE;


        // determine whether the filter is indexable
        if (filterIsPositional) {
            isIndexable = 0;
        } else {
            isIndexable = opt.isIndexableFilter(filter);
            // If the filter is indexable and the base expression is an absolute path expression,
            // consider creating a key
            if (isIndexable != 0) {
                Expression k = opt.tryToConvertFilterExpressionToKey(this, env);
                if (k != null) {
                    return k;
                }
                if (start instanceof VariableReference) {
                    Binding binding = ((VariableReference)start).getBinding();
                    if (binding instanceof LetExpression) {
                        ((LetExpression)binding).setIndexedVariable();
                    }
                }
            }
        }

        // if the filter is positional, try changing f[a and b] to f[a][b] to increase
        // the chances of finishing early.

        if (filterIsPositional &&
                filter instanceof BooleanExpression &&
                ((BooleanExpression)filter).operator == Token.AND) {
            BooleanExpression bf = (BooleanExpression)filter;
            if (isExplicitlyPositional(bf.operand0) &&
                    !isExplicitlyPositional(bf.operand1)) {
                Expression p0 = forceToBoolean(bf.operand0, env.getConfiguration());
                Expression p1 = forceToBoolean(bf.operand1, env.getConfiguration());
                FilterExpression f1 = new FilterExpression(start, p0);
                FilterExpression f2 = new FilterExpression(f1, p1);
                f2.setLocationId(getLocationId());
                f2.setParentExpression(getParentExpression());
                return f2.optimize(opt, env, contextItemType);
            }
            if (isExplicitlyPositional(bf.operand1) &&
                    !isExplicitlyPositional(bf.operand0)) {
                Expression p0 = forceToBoolean(bf.operand0, env.getConfiguration());
                Expression p1 = forceToBoolean(bf.operand1, env.getConfiguration());
                FilterExpression f1 = new FilterExpression(start, p1);
                FilterExpression f2 = new FilterExpression(f1, p0);
                f2.setLocationId(getLocationId());
                f2.setParentExpression(getParentExpression());
                return f2.optimize(opt, env, contextItemType);
            }
        }


        if (filter instanceof PositionRange && !((PositionRange)filter).hasFocusDependentRange()) {
            SliceExpression slice = new SliceExpression(start, (PositionRange)filter);
            slice.setLocationId(getLocationId());
            slice.setParentExpression(getParentExpression());
            return slice;
        };

        // If any subexpressions within the filter are not dependent on the focus,
        // promote them: this causes them to be evaluated once, outside the filter
        // expression. Note: we do this even if the filter is numeric, because it ensures that
        // the subscript is pre-evaluated, allowing direct indexing into the sequence.

        PromotionOffer offer = new PromotionOffer(opt);
        offer.action = PromotionOffer.FOCUS_INDEPENDENT;
        offer.promoteDocumentDependent = (start.getSpecialProperties() & StaticProperty.CONTEXT_DOCUMENT_NODESET) != 0;
        offer.containingExpression = this;
        filter2 = doPromotion(filter, offer);
        if (filter2 != filter) {
            filter = filter2;
            adoptChildExpression(filter2);
        }

        if (offer.containingExpression instanceof LetExpression) {
            offer.containingExpression = offer.containingExpression.optimize(opt, env, contextItemType);
        }

        return offer.containingExpression;
    }

    /**
     * Construct an expression that obtains the effective boolean value of a given expression,
     * by wrapping it in a call of the boolean() function
     */

    private static Expression forceToBoolean(Expression in, Configuration config) {
        final TypeHierarchy th = config.getTypeHierarchy();
        if (in.getItemType(th).getPrimitiveType() == Type.BOOLEAN) {
            return in;
        }
        Expression[] args = {in};
        FunctionCall fn = SystemFunction.makeSystemFunction("boolean", 1, config.getNamePool());
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
                start = doPromotion(start, offer);
            }
            if (offer.action == PromotionOffer.INLINE_VARIABLE_REFERENCES ||
                    offer.action == PromotionOffer.REPLACE_CURRENT) {
                // Don't pass on other requests. We could pass them on, but only after augmenting
                // them to say we are interested in subexpressions that don't depend on either the
                // outer context or the inner context.
                filter = doPromotion(filter, offer);
            }
            return this;
        }
    }

    /**
    * Determine whether an expression, when used as a filter, is positional
    */

    private static boolean isPositionalFilter(Expression exp, TypeHierarchy th) {
        ItemType type = exp.getItemType(th);
        if (type == Type.BOOLEAN_TYPE) {
            // common case, get it out of the way quickly
            return isExplicitlyPositional(exp);
        }
        return ( type == Type.ANY_ATOMIC_TYPE ||
                 type instanceof AnyItemType ||
                 type == Type.INTEGER_TYPE ||
                 type == Type.NUMBER_TYPE ||
                 th.isSubType(type, Type.NUMBER_TYPE) ||
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
     * Replace one subexpression by a replacement subexpression
     * @param original the original subexpression
     * @param replacement the replacement subexpression
     * @return true if the original subexpression is found
     */

    public boolean replaceSubExpression(Expression original, Expression replacement) {
        boolean found = false;
        if (start == original) {
            start = replacement;
            found = true;
        }
        if (filter == original) {
            filter = replacement;
            found = true;
        }
        return found;
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
            if (p.matchesAtMostOneItem()) {
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

        // Fast path where both operands are constants, or simple variable references

        Expression startExp = start;
        ValueRepresentation startValue = null;
        if (startExp instanceof ValueRepresentation) {
            startValue = (ValueRepresentation)startExp;
        } else if (startExp instanceof VariableReference) {
            startValue = ((VariableReference)startExp).evaluateVariable(context);
        }

        if (startValue instanceof Value) {
            startExp = (Value)startValue;
        }

        if (startValue instanceof EmptySequence) {
            return EmptyIterator.getInstance();
        }

        ValueRepresentation filterValue = null;
        if (filter instanceof ValueRepresentation) {
            filterValue = (ValueRepresentation)filter;
        } else if (filter instanceof VariableReference) {
            filterValue = ((VariableReference)filter).evaluateVariable(context);
        }

        // Handle the case where the filter is a value. Because of earlier static rewriting, this covers
        // all cases where the filter expression is independent of the context, that is, where the
        // value of the filter expression is the same for all items in the sequence being filtered.

        if (filterValue != null) {
            if (filterValue instanceof Value) {
                filterValue = ((Value)filterValue).reduce();

                if (filterValue instanceof AtomicValue) {
                    filterValue = ((AtomicValue)filterValue).getPrimitiveValue();

                    if (filterValue instanceof NumericValue) {
                        // Filter is a constant number
                        if (((NumericValue)filterValue).isWholeNumber()) {
                            int pos = (int)(((NumericValue)filterValue).longValue());
                            if (startValue != null) {
                                if (startValue instanceof Value) {
                                    // if sequence is a value, use direct indexing
                                    return SingletonIterator.makeIterator(((Value)startValue).itemAt(pos-1));
                                } else if (startValue instanceof NodeInfo) {
                                    // sequence to be filtered is a single node
                                    if (pos == 1) {
                                        return SingletonIterator.makeIterator((NodeInfo)startValue);
                                    } else {
                                        return EmptyIterator.getInstance();
                                    }
                                }
                            }
                            if (pos >= 1) {
                                SequenceIterator base = startExp.iterate(context);
                                return PositionIterator.make(base, pos, pos);
                            } else {
                                // index is less than one, no items will be selected
                                return EmptyIterator.getInstance();
                            }
                        } else {
                            // a non-integer value will never be equal to position()
                            return EmptyIterator.getInstance();
                        }
                    }
                }

                // Filter is a constant that we can treat as boolean

                if (((Value)filterValue).effectiveBooleanValue(context)) {
                    return start.iterate(context);
                } else {
                    return EmptyIterator.getInstance();
                }
            } else if (filterValue instanceof NodeInfo) {
                return start.iterate(context);
            }
        }

        // see if the sequence to be filtered is an indexed value; if so, use the index

        if (isIndexable != 0 && startValue instanceof Closure && ((Closure)startValue).isIndexable()) {
            SequenceIterator indexedResult = context.getConfiguration().getOptimizer()
                    .tryIndexedFilter(startValue, filter, isIndexable, context);
            if (indexedResult != null) {
                return indexedResult;
            }
        }

        // get an iterator over the base nodes

        SequenceIterator base = startExp.iterate(context);

        // quick exit for an empty sequence

        if (base instanceof EmptyIterator) {
            return base;
        }

        if (filterIsPositional) {
            FilterIterator fi = new FilterIterator(base, filter, context);
            fi.setFilterIsSingletonBoolean(filterIsSingletonBoolean);
            return fi;
        }

        return new FilterIterator.NonNumeric(base, filter, context);

    }

    /**
     * Test whether the filter is amenable to indexing
     */

    public boolean isIndexable() {
        return isIndexable != 0;
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
                (filter.getDependencies() & (StaticProperty.DEPENDS_ON_XSLT_CONTEXT |
                    StaticProperty.DEPENDS_ON_LOCAL_VARIABLES |
                    StaticProperty.DEPENDS_ON_USER_FUNCTIONS)));
    }



    /**
    * Diagnostic print of expression structure
    * @param level the indentation level
     @param out
     @param config
     */

    public void display(int level, PrintStream out, Configuration config) {
        out.println(ExpressionTool.indent(level) + "filter []");
        start.display(level+1, out, config);
        filter.display(level+1, out, config);
    }

    public PathMap.PathMapNode addToPathMap(PathMap pathMap, PathMap.PathMapNode pathMapNode) {
        if (start instanceof ComputedExpression) {
            PathMap.PathMapNode target = ((ComputedExpression)start).addToPathMap(pathMap, pathMapNode);
            if (filter instanceof ComputedExpression) {
                ((ComputedExpression)filter).addToPathMap(pathMap, target);
            }
            return target;
        }
        return pathMapNode;
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
