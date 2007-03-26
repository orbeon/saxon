package org.orbeon.saxon.expr;

import org.orbeon.saxon.functions.*;
import org.orbeon.saxon.om.Item;
import org.orbeon.saxon.sort.AtomicComparer;
import org.orbeon.saxon.sort.CodepointCollator;
import org.orbeon.saxon.sort.GenericAtomicComparer;
import org.orbeon.saxon.trans.DynamicError;
import org.orbeon.saxon.trans.StaticError;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.type.AtomicType;
import org.orbeon.saxon.type.ItemType;
import org.orbeon.saxon.type.Type;
import org.orbeon.saxon.type.TypeHierarchy;
import org.orbeon.saxon.value.*;

import java.util.Comparator;

/**
 * ValueComparison: a boolean expression that compares two atomic values
 * for equals, not-equals, greater-than or less-than. Implements the operators
 * eq, ne, lt, le, gt, ge
 */

public final class ValueComparison extends BinaryExpression implements ComparisonExpression {

    private AtomicComparer comparer;
    // TODO: stylesheet compilation will fail if this is a RuleBasedCollator
    private BooleanValue resultWhenEmpty = null;
    private boolean operand0MaybeUntyped = true;
    private boolean operand1MaybeUntyped = true;

    /**
     * Create a relational expression identifying the two operands and the operator
     *
     * @param p1 the left-hand operand
     * @param op the operator, as a token returned by the Tokenizer (e.g. Token.LT)
     * @param p2 the right-hand operand
     */

    public ValueComparison(Expression p1, int op, Expression p2) {
        super(p1, op, p2);
    }

    /**
     * Get the AtomicComparer used to compare atomic values. This encapsulates any collation that is used
     */

    public AtomicComparer getAtomicComparer() {
        return comparer;
    }

    /**
     * Get the primitive (singleton) operator used: one of Token.FEQ, Token.FNE, Token.FLT, Token.FGT,
     * Token.FLE, Token.FGE
     */

    public int getSingletonOperator() {
        return operator;
    }

    /**
     * Determine whether untyped atomic values should be converted to the type of the other operand
     *
     * @return true if untyped values should be converted to the type of the other operand, false if they
     *         should be converted to strings.
     */

    public boolean convertsUntypedToOther() {
        return false;
    }

    /**
     * Set the result to be returned if one of the operands is an empty sequence
     */

    public void setResultWhenEmpty(BooleanValue value) {
        resultWhenEmpty = value;
    }

    /**
     * Get the result to be returned if one of the operands is an empty sequence
     */

    public BooleanValue getResultWhenEmpty() {
        return resultWhenEmpty;
    }

    /**
     * Type-check the expression
     */

    public Expression typeCheck(StaticContext env, ItemType contextItemType) throws XPathException {

        operand0 = operand0.typeCheck(env, contextItemType);
        if (operand0 instanceof EmptySequence) {
            return (resultWhenEmpty == null ? operand0 : resultWhenEmpty);
        }

        operand1 = operand1.typeCheck(env, contextItemType);
        if (operand1 instanceof EmptySequence) {
            return (resultWhenEmpty == null ? operand1 : resultWhenEmpty);
        }

        final SequenceType optionalAtomic = SequenceType.OPTIONAL_ATOMIC;
        final TypeHierarchy th = env.getConfiguration().getTypeHierarchy();

        RoleLocator role0 = new RoleLocator(RoleLocator.BINARY_EXPR, Token.tokens[operator], 0, null);
        role0.setSourceLocator(this);
        operand0 = TypeChecker.staticTypeCheck(operand0, optionalAtomic, false, role0, env);

        RoleLocator role1 = new RoleLocator(RoleLocator.BINARY_EXPR, Token.tokens[operator], 1, null);
        role1.setSourceLocator(this);
        operand1 = TypeChecker.staticTypeCheck(operand1, optionalAtomic, false, role1, env);

        AtomicType t0 = operand0.getItemType(th).getAtomizedItemType();
        AtomicType t1 = operand1.getItemType(th).getAtomizedItemType();

        int p0 = t0.getPrimitiveType();
        if (p0 == Type.UNTYPED_ATOMIC) {
            p0 = Type.STRING;
        }
        int p1 = t1.getPrimitiveType();
        if (p1 == Type.UNTYPED_ATOMIC) {
            p1 = Type.STRING;
        }

        operand0MaybeUntyped = th.relationship(t0, Type.UNTYPED_ATOMIC_TYPE) != TypeHierarchy.DISJOINT;
        operand1MaybeUntyped = th.relationship(t1, Type.UNTYPED_ATOMIC_TYPE) != TypeHierarchy.DISJOINT;

        if (!Type.isComparable(p0, p1, Token.isOrderedOperator(operator))) {
            boolean opt0 = Cardinality.allowsZero(operand0.getCardinality());
            boolean opt1 = Cardinality.allowsZero(operand1.getCardinality());
            if (opt0 || opt1) {
                // This is a comparison such as (xs:integer? eq xs:date?). This is almost
                // certainly an error, but we need to let it through because it will work if
                // one of the operands is an empty sequence.

                String which = null;
                if (opt0) which = "the first operand is";
                if (opt1) which = "the second operand is";
                if (opt0 && opt1) which = "one or both operands are";
                env.issueWarning("Comparison of " + t0.toString(env.getNamePool()) + (opt0 ? "?" : "") + " to " + t1.toString(env.getNamePool()) +
                        (opt1 ? "?" : "") + " will fail unless " + which + " empty", this);

            } else {
                StaticError err =
                        new StaticError("Cannot compare " + t0.toString(env.getNamePool()) +
                        " to " + t1.toString(env.getNamePool()));
                err.setIsTypeError(true);
                err.setErrorCode("XPTY0004");
                err.setLocator(this);
                throw err;
            }
        }
        if (!(operator == Token.FEQ || operator == Token.FNE)) {
            if (!Type.isOrdered(p0)) {
                StaticError err = new StaticError("Type " + t0.toString(env.getNamePool()) + " is not an ordered type");
                err.setErrorCode("XPTY0004");
                err.setIsTypeError(true);
                err.setLocator(this);
                throw err;
            }
            if (!Type.isOrdered(p1)) {
                StaticError err = new StaticError("Type " + t1.toString(env.getNamePool()) + " is not an ordered type");
                err.setErrorCode("XPTY0004");
                err.setIsTypeError(true);
                err.setLocator(this);
                throw err;
            }
        }

        Comparator comp = env.getCollation(env.getDefaultCollationName());
        if (comp == null) {
            comp = CodepointCollator.getInstance();
        }
        comparer = GenericAtomicComparer.makeAtomicComparer(p0, p1, comp, env.getConfiguration());
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

        // evaluate the expression now if both arguments are constant

        if ((operand0 instanceof Value) && (operand1 instanceof Value)) {
            return (AtomicValue) evaluateItem(env.makeEarlyEvaluationContext());
        }        

        // optimise count(x) eq 0 (or gt 0, ne 0, eq 0, etc)

        if (Aggregate.isCountFunction(operand0) &&
                operand1 instanceof AtomicValue) {
            if (isZero(operand1)) {
                if (operator == Token.FEQ || operator == Token.FLE) {
                    // rewrite count(x)=0 as empty(x)
                    FunctionCall fn = SystemFunction.makeSystemFunction("empty", 1, env.getNamePool());
                    Expression[] args = new Expression[1];
                    args[0] = ((FunctionCall) operand0).argument[0];
                    fn.setArguments(args);
                    fn.setParentExpression(getParentExpression());
                    return fn;
                } else if (operator == Token.FNE || operator == Token.FGT) {
                    // rewrite count(x)!=0, count(x)>0 as exists(x)
                    FunctionCall fn = SystemFunction.makeSystemFunction("exists", 1, env.getNamePool());
                    Expression[] args = new Expression[1];
                    args[0] = ExpressionTool.unsorted(opt, ((FunctionCall) operand0).argument[0], false);
                    fn.setArguments(args);
                    fn.setParentExpression(getParentExpression());
                    return fn;
                } else if (operator == Token.FGE) {
                    // rewrite count(x)>=0 as true()
                    return BooleanValue.TRUE;
                } else {  // singletonOperator == Token.FLT
                    // rewrite count(x)<0 as false()
                    return BooleanValue.FALSE;
                }
            } else if (operand1 instanceof IntegerValue &&
                    (operator == Token.FGT || operator == Token.FGE)) {
                // rewrite count(x) gt n as exists(x[n+1])
                //     and count(x) ge n as exists(x[n])
                long val = ((IntegerValue) operand1).longValue();
                if (operator == Token.FGT) {
                    val++;
                }
                FunctionCall fn = SystemFunction.makeSystemFunction("exists", 1, env.getNamePool());
                Expression[] args = new Expression[1];
                FilterExpression filter =
                        new FilterExpression(((FunctionCall) operand0).argument[0],
                                new IntegerValue(val));
                args[0] = filter;
                fn.setArguments(args);
                fn.setParentExpression(getParentExpression());
                return fn;
            }
        }

        // optimise (0 eq count(x)), etc

        if (Aggregate.isCountFunction(operand1) && isZero(operand0)) {
            ValueComparison vc =
                    new ValueComparison(operand1, Token.inverse(operator), operand0);
            vc.setParentExpression(getParentExpression());
            return vc.typeCheck(env, contextItemType).optimize(opt, env, contextItemType);
        }

        // optimise string-length(x) = 0, >0, !=0 etc

        if ((operand0 instanceof StringLength) &&
                (((StringLength) operand0).getNumberOfArguments() == 1) && isZero(operand1)) {
            ((StringLength) operand0).setShortcut();
        }

        // optimise (0 = string-length(x)), etc

        if ((operand1 instanceof StringLength) &&
                (((StringLength) operand1).getNumberOfArguments() == 1) && isZero(operand0)) {
            ((StringLength) operand1).setShortcut();
        }

        // optimise [position()=last()] etc

        if ((operand0 instanceof Position) && (operand1 instanceof Last)) {
            switch (operator) {
                case Token.FEQ:
                case Token.FGE:
                    IsLastExpression iletrue = new IsLastExpression(true);
                    iletrue.setParentExpression(getParentExpression());
                    iletrue.setLocationId(getLocationId());
                    return iletrue;
                case Token.FNE:
                case Token.FLT:
                    IsLastExpression ilefalse = new IsLastExpression(false);
                    ilefalse.setParentExpression(getParentExpression());
                    ilefalse.setLocationId(getLocationId());
                    return ilefalse;
                case Token.FGT:
                    return BooleanValue.FALSE;
                case Token.FLE:
                    return BooleanValue.TRUE;
            }
        }
        if ((operand0 instanceof Last) && (operand1 instanceof Position)) {
            switch (operator) {
                case Token.FEQ:
                case Token.FLE:
                    IsLastExpression iletrue = new IsLastExpression(true);
                    iletrue.setParentExpression(getParentExpression());
                    iletrue.setLocationId(getLocationId());
                    return iletrue;
                case Token.FNE:
                case Token.FGT:
                    IsLastExpression ilefalse = new IsLastExpression(false);
                    ilefalse.setParentExpression(getParentExpression());
                    ilefalse.setLocationId(getLocationId());
                    return ilefalse;
                case Token.FLT:
                    return BooleanValue.FALSE;
                case Token.FGE:
                    return BooleanValue.TRUE;
            }
        }

        // optimise [position() < n] etc

        if (operand0 instanceof Position) {
            boolean isInteger = (operand1 instanceof IntegerValue);
            int pos = 0;
            if (isInteger) {
                pos = (int) ((IntegerValue) operand1).longValue();
                if (pos < 0) {
                    pos = 0;
                }
            }
            switch (operator) {
                case Token.FEQ:
                    return makePositionRange(operand1);
                case Token.FGE:
                    return makeOpenPositionRange(operand1);
                case Token.FNE:
                    if (isInteger && pos == 1) {
                        return makePositionRange(2, Integer.MAX_VALUE);
                    } else {
                        break;
                    }
                case Token.FLT:
                    if (isInteger) {
                        return makePositionRange(1, pos - 1);
                    } else {
                        break;
                    }
                case Token.FGT:
                    if (isInteger) {
                        return makePositionRange(pos + 1, Integer.MAX_VALUE);
                    } else {
                        break;
                    }
                case Token.FLE:
                    if (isInteger) {
                        return makePositionRange(1, pos);
                    } else {
                        break;
                    }
            }
        }

        if (operand1 instanceof Position) {
            int pos = 0;
            boolean isInteger = (operand0 instanceof IntegerValue);
            if (isInteger) {
                pos = (int) ((IntegerValue) operand0).longValue();
                if (pos < 0) {
                    pos = 0;
                }
            }

            switch (operator) {
                case Token.FEQ:
                    return makePositionRange(operand0);
                case Token.FLE:
                    return makeOpenPositionRange(operand0);
                case Token.FNE:
                    if (isInteger && pos == 1) {
                        return makePositionRange(2, Integer.MAX_VALUE);
                    } else {
                        break;
                    }
                case Token.FGT:
                    if (isInteger) {
                        return makePositionRange(1, pos - 1);
                    } else {
                        break;
                    }
                case Token.FLT:
                    if (isInteger) {
                        return makePositionRange(pos + 1, Integer.MAX_VALUE);
                    } else {
                        break;
                    }
                case Token.FGE:
                    if (isInteger) {
                        return makePositionRange(1, pos);
                    } else {
                        break;
                    }
            }
        }

        // optimize comparison against an integer constant

        TypeHierarchy th = env.getConfiguration().getTypeHierarchy();
        if (operand1 instanceof IntegerValue &&
                operand0.getCardinality() == StaticProperty.EXACTLY_ONE &&
                th.isSubType(operand0.getItemType(th), Type.NUMBER_TYPE)) {
            return new CompareToIntegerConstant(operand0, operator, ((IntegerValue) operand1).longValue());
        }

        if (operand0 instanceof IntegerValue &&
                operand1.getCardinality() == StaticProperty.EXACTLY_ONE &&
                th.isSubType(operand1.getItemType(th), Type.NUMBER_TYPE)) {
            return new CompareToIntegerConstant(operand1, Token.inverse(operator), ((IntegerValue) operand0).longValue());
        }

        // optimize generate-id(X) = generate-id(Y) as "X is Y"
        // This construct is often used in XSLT 1.0 stylesheets.
        // Only do this if we know the arguments are singletons, because "is" doesn't
        // do first-value extraction.

        if (NamePart.isGenerateIdFunction(operand0) && NamePart.isGenerateIdFunction(operand1)) {
            FunctionCall f0 = (FunctionCall) operand0;
            FunctionCall f1 = (FunctionCall) operand1;
            if (!Cardinality.allowsMany(f0.argument[0].getCardinality()) &&
                    !Cardinality.allowsMany(f1.argument[0].getCardinality()) &&
                    (operator == Token.FEQ)) {
                IdentityComparison id =
                        new IdentityComparison(f0.argument[0],
                                Token.IS,
                                f1.argument[0]);
                id.setGenerateIdEmulation(true);
                id.setLocationId(getLocationId());
                id.setParentExpression(getParentExpression());
                return id.simplify(env).typeCheck(env, contextItemType).optimize(opt, env, contextItemType);
            }
        }


        return this;
    }

    private PositionRange makePositionRange(int min, int max) {
        PositionRange pr = new PositionRange(min, max);
        pr.setParentExpression(getParentExpression());
        pr.setLocationId(getLocationId());
        return pr;
    }

    private PositionRange makeOpenPositionRange(Expression min) {
        PositionRange pr = new PositionRange(min, null);
        pr.setParentExpression(getParentExpression());
        pr.setLocationId(getLocationId());
        return pr;
    }

    private Expression makePositionRange(Expression pos) {
        if (pos instanceof Position) {
            // position() = position() is always true. Arises in conformance tests!
            return BooleanValue.TRUE;
        }
        if ((pos.getDependencies() &
                (StaticProperty.DEPENDS_ON_POSITION | StaticProperty.DEPENDS_ON_CONTEXT_ITEM)) != 0) {
            // no optimization possible for expressions like [position() = .]
            return this;
        }
        PositionRange pr = new PositionRange(pos);
        pr.setParentExpression(getParentExpression());
        pr.setLocationId(getLocationId());
        return pr;
    }

    /**
     * Return the negation of this value comparison: that is, a value comparison that returns true()
     * if and only if the original returns false(). The result must be the same as not(this) even in the
     * case where one of the operands is ().
     */

    public ValueComparison negate() {
        ValueComparison vc = new ValueComparison(operand0, Token.negate(operator), operand1);
        vc.comparer = comparer;
        if (resultWhenEmpty == null || resultWhenEmpty == BooleanValue.FALSE) {
            vc.resultWhenEmpty = BooleanValue.TRUE;
        } else {
            vc.resultWhenEmpty = BooleanValue.FALSE;
        }
        vc.setLocationId(getLocationId());
        return vc;
    }


    /**
     * Test whether an expression is constant zero
     */

    private static boolean isZero(Expression exp) {
        if (exp instanceof NumericValue) {
            return ((NumericValue) exp).compareTo(0) == 0;
        } else {
            return false;
        }
    }

    /**
     * Evaluate the effective boolean value of the expression
     *
     * @param context the given context for evaluation
     * @return a boolean representing the result of the comparison of the two operands
     */

    public boolean effectiveBooleanValue(XPathContext context) throws XPathException {
        try {
            AtomicValue v0 = ((AtomicValue) operand0.evaluateItem(context));
            if (v0 == null) {
                return (resultWhenEmpty == BooleanValue.TRUE);  // normally false
            }
            if (operand0MaybeUntyped && v0 instanceof UntypedAtomicValue) {
                v0 = new StringValue(((UntypedAtomicValue) v0).getStringValueCS());
            }
            AtomicValue v1 = ((AtomicValue) operand1.evaluateItem(context));
            if (v1 == null) {
                return (resultWhenEmpty == BooleanValue.TRUE);  // normally false
            }
            if (operand1MaybeUntyped && v1 instanceof UntypedAtomicValue) {
                v1 = new StringValue(((UntypedAtomicValue) v1).getStringValueCS());
            }
            return compare(v0, operator, v1, comparer);
        } catch (DynamicError e) {
            // re-throw the exception with location information added
            if (e.getXPathContext() == null) {
                e.setXPathContext(context);
            }
            if (e.getLocator() == null) {
                e.setLocator(this);
            }
            throw e;
        }
    }

    /**
     * Compare two atomic values, using a specified operator and collation
     *
     * @param v1       the first operand
     * @param op       the operator, as defined by constants such as {@link Token#FEQ} or
     *                 {@link Token#FLT}
     * @param v2       the second operand
     * @param collator the Collator to be used when comparing strings
     * @throws org.orbeon.saxon.trans.DynamicError
     *          if the values are not comparable
     */

    static boolean compare(AtomicValue v1, int op, AtomicValue v2, AtomicComparer collator)
            throws DynamicError {
        // TODO: do the NaN tests in the comparers where it actually arises
        if (v1 instanceof NumericValue && ((NumericValue) v1).isNaN()) {
            return (op == Token.FNE);
        }
        if (v2 instanceof NumericValue && ((NumericValue) v2).isNaN()) {
            return (op == Token.FNE);
        }
        try {
            switch (op) {
                case Token.FEQ:
                    return collator.comparesEqual(v1, v2);
                case Token.FNE:
                    return !collator.comparesEqual(v1, v2);
                case Token.FGT:
                    return collator.compare(v1, v2) > 0;
                case Token.FLT:
                    return collator.compare(v1, v2) < 0;
                case Token.FGE:
                    return collator.compare(v1, v2) >= 0;
                case Token.FLE:
                    return collator.compare(v1, v2) <= 0;
                default:
                    throw new UnsupportedOperationException("Unknown operator " + op);
            }
        } catch (ClassCastException err) {
            DynamicError e2 = new DynamicError("Cannot compare " + v1.getItemType(null) + " to " + v2.getItemType(null));
            e2.setErrorCode("XPTY0004");
            e2.setIsTypeError(true);
            throw e2;
        }
    }

    /**
     * Evaluate the expression in a given context
     *
     * @param context the given context for evaluation
     * @return a BooleanValue representing the result of the numeric comparison of the two operands,
     *         or null representing the empty sequence
     */

    public Item evaluateItem(XPathContext context) throws XPathException {
        try {
            AtomicValue v1 = (AtomicValue) operand0.evaluateItem(context);
            if (v1 == null) {
                return resultWhenEmpty;
            }
            if (v1 instanceof UntypedAtomicValue) {
                v1 = v1.convert(Type.STRING, context);
            }
            AtomicValue v2 = (AtomicValue) operand1.evaluateItem(context);
            if (v2 == null) {
                return resultWhenEmpty;
            }
            if (v2 instanceof UntypedAtomicValue) {
                v2 = v2.convert(Type.STRING, context);
            }
            return BooleanValue.get(compare(v1, operator, v2, comparer));
        } catch (DynamicError e) {
            // re-throw the exception with location information added
            if (e.getXPathContext() == null) {
                e.setXPathContext(context);
            }
            if (e.getLocator() == null) {
                e.setLocator(this);
            }
            throw e;
        }
    }


    /**
     * Determine the data type of the expression
     *
     * @param th
     * @return Type.BOOLEAN
     */

    public ItemType getItemType(TypeHierarchy th) {
        return Type.BOOLEAN_TYPE;
    }

    /**
     * Determine the static cardinality.
     */

    public int computeCardinality() {
        if (resultWhenEmpty != null) {
            return StaticProperty.EXACTLY_ONE;
        } else {
            return super.computeCardinality();
        }
    }

    protected String displayOperator() {
        return Token.tokens[operator] +
                (resultWhenEmpty == null ? "" : " (on empty return " + resultWhenEmpty + ')');
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
