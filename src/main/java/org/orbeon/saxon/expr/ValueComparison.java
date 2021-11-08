package org.orbeon.saxon.expr;

import org.orbeon.saxon.functions.*;
import org.orbeon.saxon.om.Item;
import org.orbeon.saxon.om.NamePool;
import org.orbeon.saxon.om.StandardNames;
import org.orbeon.saxon.sort.*;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.trans.NoDynamicContextException;
import org.orbeon.saxon.type.*;
import org.orbeon.saxon.value.*;

import java.io.IOException;
import java.io.ObjectInputStream;

/**
 * ValueComparison: a boolean expression that compares two atomic values
 * for equals, not-equals, greater-than or less-than. Implements the operators
 * eq, ne, lt, le, gt, ge
 */

public final class ValueComparison extends BinaryExpression implements ComparisonExpression, Negatable {

    private AtomicComparer comparer;
    private BooleanValue resultWhenEmpty = null;
    //private boolean operand0MaybeUntyped = true;
    //private boolean operand1MaybeUntyped = true;
    private boolean needsRuntimeComparabilityCheck;

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
     * Deserialization method ensures that there is only one BooleanValue.TRUE and only one BooleanValue.FALSE
     * @param in the input stream
     */

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        if (resultWhenEmpty != null) {
            resultWhenEmpty = (resultWhenEmpty.getBooleanValue() ? BooleanValue.TRUE : BooleanValue.FALSE);
        }
    }

    /**
     * Set the AtomicComparer used to compare atomic values
     * @param comparer the AtomicComparer
     */

    public void setAtomicComparer(AtomicComparer comparer) {
        this.comparer = comparer;
    }

    /**
     * Get the AtomicComparer used to compare atomic values. This encapsulates any collation that is used.
     * Note that the comparer is always known at compile time.
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
     * @param value the result to be returned if an operand is empty. Supply null to mean the empty sequence.
     */

    public void setResultWhenEmpty(BooleanValue value) {
        resultWhenEmpty = value;
    }

    /**
     * Get the result to be returned if one of the operands is an empty sequence
     * @return BooleanValue.TRUE, BooleanValue.FALSE, or null (meaning the empty sequence)
     */

    public BooleanValue getResultWhenEmpty() {
        return resultWhenEmpty;
    }

    /**
     * Determine whether a run-time check is needed to check that the types of the arguments
     * are comparable
     * @return true if a run-time check is needed
     */

    public boolean needsRuntimeComparabilityCheck() {
        return needsRuntimeComparabilityCheck;
    }

    /**
     * Type-check the expression
     */

    public Expression typeCheck(ExpressionVisitor visitor, ItemType contextItemType) throws XPathException {

        NamePool namePool = visitor.getConfiguration().getNamePool();
        TypeHierarchy th = visitor.getConfiguration().getTypeHierarchy();
        StaticContext env = visitor.getStaticContext();

        operand0 = visitor.typeCheck(operand0, contextItemType);
        if (Literal.isEmptySequence(operand0)) {
            return (resultWhenEmpty == null ? operand0 : Literal.makeLiteral(resultWhenEmpty));
        }

        operand1 = visitor.typeCheck(operand1, contextItemType);
        if (Literal.isEmptySequence(operand1)) {
            return (resultWhenEmpty == null ? operand1 : Literal.makeLiteral(resultWhenEmpty));
        }

        final SequenceType optionalAtomic = SequenceType.OPTIONAL_ATOMIC;

        RoleLocator role0 = new RoleLocator(RoleLocator.BINARY_EXPR, Token.tokens[operator], 0);
        //role0.setSourceLocator(this);
        operand0 = TypeChecker.staticTypeCheck(operand0, optionalAtomic, false, role0, visitor);

        RoleLocator role1 = new RoleLocator(RoleLocator.BINARY_EXPR, Token.tokens[operator], 1);
        //role1.setSourceLocator(this);
        operand1 = TypeChecker.staticTypeCheck(operand1, optionalAtomic, false, role1, visitor);

        AtomicType t0 = operand0.getItemType(th).getAtomizedItemType();
        AtomicType t1 = operand1.getItemType(th).getAtomizedItemType();

        if (t0.isExternalType() || t1.isExternalType()) {
            XPathException err = new XPathException("Cannot perform comparisons involving external objects");
            err.setIsTypeError(true);
            err.setErrorCode("XPTY0004");
            err.setLocator(this);
            throw err;
        }

        BuiltInAtomicType p0 = (BuiltInAtomicType)t0.getPrimitiveItemType();
        if (p0.equals(BuiltInAtomicType.UNTYPED_ATOMIC)) {
            p0 = BuiltInAtomicType.STRING;
        }
        BuiltInAtomicType p1 = (BuiltInAtomicType)t1.getPrimitiveItemType();
        if (p1.equals(BuiltInAtomicType.UNTYPED_ATOMIC)) {
            p1 = BuiltInAtomicType.STRING;
        }

        //operand0MaybeUntyped = th.relationship(p0, BuiltInAtomicType.UNTYPED_ATOMIC) != TypeHierarchy.DISJOINT;
        //operand1MaybeUntyped = th.relationship(p1, BuiltInAtomicType.UNTYPED_ATOMIC) != TypeHierarchy.DISJOINT;

        needsRuntimeComparabilityCheck =
                p0.equals(BuiltInAtomicType.ANY_ATOMIC) || p1.equals(BuiltInAtomicType.ANY_ATOMIC);

        if (!Type.isComparable(p0, p1, Token.isOrderedOperator(operator))) {
            boolean opt0 = Cardinality.allowsZero(operand0.getCardinality());
            boolean opt1 = Cardinality.allowsZero(operand1.getCardinality());
            if (opt0 || opt1) {
                // This is a comparison such as (xs:integer? eq xs:date?). This is almost
                // certainly an error, but we need to let it through because it will work if
                // one of the operands is an empty sequence.

                // ORBEON: 2013-05-03: Don't issue warning if any of the operands is xs:untypedAtomic.
                //
                // - At compile time, we might not know the type. But at runtime, we might thanks to type annotations,
                //   and the comparison indeed can (and does) succeed in such cases. So the warning is not only annoying
                //   but incorrect (because the comparison can succeed even if the operands are not empty)!
                // - Note that the `opt0 || opt1` test above could also fail when the cardinality does not allow an
                //   empty sequence, and then we would incorrectly go to the error case at compile time. We don't seem
                //   to hit this however. When could we have cardinality information at compile time but not the actual
                //   type? Maybe with a function parameter like `element()?`?
                // - I am wondering, in the case where Saxon uses a schema (which we don't have as we are using the
                //   open source version), whether users wouldn't also get the incorrect warning, and even whether they
                //   could not incorrectly hit the error case. This because even with a schema, types can still be
                //   unknown at compile time.
                if (! t0.equals(BuiltInAtomicType.UNTYPED_ATOMIC) && ! t1.equals(BuiltInAtomicType.UNTYPED_ATOMIC)) {
                    String which = null;
                    if (opt0) which = "the first operand is";
                    if (opt1) which = "the second operand is";
                    if (opt0 && opt1) which = "one or both operands are";

                    visitor.getStaticContext().issueWarning("Comparison of " + t0.toString(namePool) +
                            (opt0 ? "?" : "") + " to " + t1.toString(namePool) +
                            (opt1 ? "?" : "") + " will fail unless " + which + " empty", this);
                }
                needsRuntimeComparabilityCheck = true;
            } else {
                XPathException err = new XPathException("Cannot compare " + t0.toString(namePool) +
                        " to " + t1.toString(namePool));
                err.setIsTypeError(true);
                err.setErrorCode("XPTY0004");
                err.setLocator(this);
                throw err;
            }
        }
        if (!(operator == Token.FEQ || operator == Token.FNE)) {
            if (!p0.isOrdered()) {
                XPathException err = new XPathException("Type " + t0.toString(env.getNamePool()) + " is not an ordered type");
                err.setErrorCode("XPTY0004");
                err.setIsTypeError(true);
                err.setLocator(this);
                throw err;
            }
            if (!p1.isOrdered()) {
                XPathException err = new XPathException("Type " + t1.toString(env.getNamePool()) + " is not an ordered type");
                err.setErrorCode("XPTY0004");
                err.setIsTypeError(true);
                err.setLocator(this);
                throw err;
            }
        }

        if (comparer == null) {
            final String defaultCollationName = env.getDefaultCollationName();
            StringCollator comp = env.getCollation(defaultCollationName);
            if (comp == null) {
                comp = CodepointCollator.getInstance();
            }
            comparer = GenericAtomicComparer.makeAtomicComparer(
                    p0, p1, comp, env.getConfiguration().getConversionContext());
        }
        return this;
    }

    /**
     * Perform optimisation of an expression and its subexpressions.
     * <p/>
     * <p>This method is called after all references to functions and variables have been resolved
     * to the declaration of the function or variable, and after all type checking has been done.</p>
     *
     * @param visitor an expression visitor
     * @param contextItemType the static type of "." at the point where this expression is invoked.
     *                        The parameter is set to null if it is known statically that the context item will be undefined.
     *                        If the type of the context item is not known statically, the argument is set to
     *                        {@link org.orbeon.saxon.type.Type#ITEM_TYPE}
     * @return the original expression, rewritten if appropriate to optimize execution
     * @throws XPathException if an error is discovered during this phase
     *                                        (typically a type error)
     */

    public Expression optimize(ExpressionVisitor visitor, ItemType contextItemType) throws XPathException {

        Optimizer opt = visitor.getConfiguration().getOptimizer();
        TypeHierarchy th = visitor.getConfiguration().getTypeHierarchy();

        operand0 = visitor.optimize(operand0, contextItemType);
        operand1 = visitor.optimize(operand1, contextItemType);

        Value value0 = null;
        Value value1 = null;

        if (operand0 instanceof Literal) {
            value0 = ((Literal)operand0).getValue();
        }

        if (operand1 instanceof Literal) {
            value1 = ((Literal)operand1).getValue();
        }

        // evaluate the expression now if both arguments are constant

        if ((value0 != null) && (value1 != null)) {
            try {
                AtomicValue r = (AtomicValue)evaluateItem(visitor.getStaticContext().makeEarlyEvaluationContext());
                //noinspection RedundantCast
                return Literal.makeLiteral(r == null ? (Value)EmptySequence.getInstance() : (Value)r);
            } catch (NoDynamicContextException e) {
                // early evaluation failed, typically because the implicit context isn't available.
                // Try again at run-time
                return this;
            }
        }        

        // optimise count(x) eq 0 (or gt 0, ne 0, eq 0, etc)

        if (Aggregate.isCountFunction(operand0) && Literal.isAtomic(operand1)) {
            if (isZero(value1)) {
                if (operator == Token.FEQ || operator == Token.FLE) {
                    // rewrite count(x)=0 as empty(x)
                    Expression result = SystemFunction.makeSystemFunction(
                            "empty", new Expression[]{((FunctionCall) operand0).argument[0]});
                    opt.trace("Rewrite count()=0 as:", result);
                    return result;
                } else if (operator == Token.FNE || operator == Token.FGT) {
                    // rewrite count(x)!=0, count(x)>0 as exists(x)
                    Expression arg = ExpressionTool.unsorted(opt, ((FunctionCall)operand0).argument[0], false);
                    Expression result = SystemFunction.makeSystemFunction("exists", new Expression[] {arg});
                    opt.trace("Rewrite count()>0 as:", result);
                    return result;
                } else if (operator == Token.FGE) {
                    // rewrite count(x)>=0 as true()
                    return Literal.makeLiteral(BooleanValue.TRUE);
                } else {  // singletonOperator == Token.FLT
                    // rewrite count(x)<0 as false()
                    return Literal.makeLiteral(BooleanValue.FALSE);
                }
            } else if (value1 instanceof Int64Value &&
                    (operator == Token.FGT || operator == Token.FGE)) {
                // rewrite count(x) gt n as exists(x[n+1])
                //     and count(x) ge n as exists(x[n])
                long val = ((Int64Value) value1).longValue();
                if (operator == Token.FGT) {
                    val++;
                }
                FilterExpression filter = new FilterExpression(((FunctionCall) operand0).argument[0],
                                Literal.makeLiteral(Int64Value.makeIntegerValue(val)));
                ExpressionTool.copyLocationInfo(this, filter);
                Expression result = SystemFunction.makeSystemFunction("exists", new Expression[]{filter});
                opt.trace("Rewrite count()>=N as:", result);
                return result;
            }
        }

        // optimise (0 eq count(x)), etc

        if (Aggregate.isCountFunction(operand1) && isZero(value0)) {
            ValueComparison vc =
                    new ValueComparison(operand1, Token.inverse(operator), operand0);
            ExpressionTool.copyLocationInfo(this, vc);
            return visitor.optimize(visitor.typeCheck(vc, contextItemType), contextItemType);
        }

        // optimise string-length(x) = 0, >0, !=0 etc

        if ((operand0 instanceof StringLength) &&
                (((StringLength) operand0).getNumberOfArguments() == 1) &&
                isZero(value1)) {
            Expression arg = (((StringLength)operand0).getArguments()[0]);
            switch (operator) {
                case Token.FEQ:
                case Token.FLE:
                    return SystemFunction.makeSystemFunction("not", new Expression[]{arg});
                case Token.FNE:
                case Token.FGT:
                    return SystemFunction.makeSystemFunction("boolean", new Expression[]{arg});
                case Token.FGE:
                    return Literal.makeLiteral(BooleanValue.TRUE);
                case Token.FLT:
                    return Literal.makeLiteral(BooleanValue.FALSE);
            }
        }

        // optimise (0 = string-length(x)), etc

        if ((operand1 instanceof StringLength) &&
                        (((StringLength) operand1).getNumberOfArguments() == 1) &&
                        isZero(value0)) {
            Expression arg = (((StringLength)operand1).getArguments()[0]);
            switch (operator) {
                case Token.FEQ:
                case Token.FGE:
                    return SystemFunction.makeSystemFunction("not", new Expression[]{arg});
                case Token.FNE:
                case Token.FLT:
                    return SystemFunction.makeSystemFunction("boolean", new Expression[]{arg});
                case Token.FLE:
                    return Literal.makeLiteral(BooleanValue.TRUE);
                case Token.FGT:
                    return Literal.makeLiteral(BooleanValue.FALSE);
            }
        }

        // optimise string="" etc
        // Note we can change S!="" to boolean(S) for cardinality zero-or-one, but we can only
        // change S="" to not(S) for cardinality exactly-one.

        int p0 = operand0.getItemType(th).getPrimitiveType();
        if ((p0 == StandardNames.XS_STRING ||
                p0 == StandardNames.XS_ANY_URI ||
                p0 == StandardNames.XS_UNTYPED_ATOMIC) &&
                operand1 instanceof Literal &&
                ((Literal)operand1).getValue() instanceof StringValue &&
                ((StringValue)((Literal)operand1).getValue()).isZeroLength() &&
                comparer instanceof CodepointCollatingComparer) {

            switch (operator) {
                case Token.FNE:
                case Token.FGT:
                    return SystemFunction.makeSystemFunction("boolean", new Expression[]{operand0});
                case Token.FEQ:
                case Token.FLE:
                    if (operand0.getCardinality() == StaticProperty.EXACTLY_ONE) {
                        return SystemFunction.makeSystemFunction("not", new Expression[]{operand0});
                    }
            }
        }

        // optimize "" = string etc

        int p1 = operand1.getItemType(th).getPrimitiveType();
        if ((p1 == StandardNames.XS_STRING ||
                p1 == StandardNames.XS_ANY_URI ||
                p1 == StandardNames.XS_UNTYPED_ATOMIC) &&
                operand0 instanceof Literal &&
                ((Literal)operand0).getValue() instanceof StringValue &&
                ((StringValue)((Literal)operand0).getValue()).isZeroLength() &&
                comparer instanceof CodepointCollatingComparer) {

            switch (operator) {
                case Token.FNE:
                case Token.FLT:
                    return SystemFunction.makeSystemFunction("boolean", new Expression[]{operand1});
                case Token.FEQ:
                case Token.FGE:
                    if (operand1.getCardinality() == StaticProperty.EXACTLY_ONE) {
                        return SystemFunction.makeSystemFunction("not", new Expression[]{operand1});
                    }
            }
        }


        // optimise [position()=last()] etc

        if ((operand0 instanceof Position) && (operand1 instanceof Last)) {
            switch (operator) {
                case Token.FEQ:
                case Token.FGE:
                    IsLastExpression iletrue = new IsLastExpression(true);
                    ExpressionTool.copyLocationInfo(this, iletrue);
                    return iletrue;
                case Token.FNE:
                case Token.FLT:
                    IsLastExpression ilefalse = new IsLastExpression(false);
                    ExpressionTool.copyLocationInfo(this, ilefalse);
                    return ilefalse;
                case Token.FGT:
                    return Literal.makeLiteral(BooleanValue.FALSE);
                case Token.FLE:
                    return Literal.makeLiteral(BooleanValue.TRUE);
            }
        }
        if ((operand0 instanceof Last) && (operand1 instanceof Position)) {
            switch (operator) {
                case Token.FEQ:
                case Token.FLE:
                    IsLastExpression iletrue = new IsLastExpression(true);
                    ExpressionTool.copyLocationInfo(this, iletrue);
                    return iletrue;
                case Token.FNE:
                case Token.FGT:
                    IsLastExpression ilefalse = new IsLastExpression(false);
                    ExpressionTool.copyLocationInfo(this, ilefalse);
                    return ilefalse;
                case Token.FLT:
                    return Literal.makeLiteral(BooleanValue.FALSE);
                case Token.FGE:
                    return Literal.makeLiteral(BooleanValue.TRUE);
            }
        }

        // optimize comparison against an integer constant

        if (value1 instanceof Int64Value &&
                operand0.getCardinality() == StaticProperty.EXACTLY_ONE &&
                th.isSubType(operand0.getItemType(th), BuiltInAtomicType.NUMERIC)) {
            return new CompareToIntegerConstant(operand0, operator, ((Int64Value)value1).longValue());
        }

        if (value0 instanceof Int64Value &&
                operand1.getCardinality() == StaticProperty.EXACTLY_ONE &&
                th.isSubType(operand1.getItemType(th), BuiltInAtomicType.NUMERIC)) {
            return new CompareToIntegerConstant(operand1, Token.inverse(operator), ((Int64Value) value0).longValue());
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
                ExpressionTool.copyLocationInfo(this, id);
                return visitor.optimize(visitor.typeCheck(visitor.simplify(id), contextItemType), contextItemType);
            }
        }

        return this;
    }


    /**
     * Check whether this specific instance of the expression is negatable
     *
     * @return true if it is
     */

    public boolean isNegatable(ExpressionVisitor visitor) {
        // Expression is not negatable if it might involve NaN
        TypeHierarchy th = visitor.getConfiguration().getTypeHierarchy();
        return !maybeNaN(operand0, th) && !maybeNaN(operand1, th);
    }

    private boolean maybeNaN(Expression exp, TypeHierarchy th) {
        return th.relationship(exp.getItemType(th), BuiltInAtomicType.DOUBLE) != TypeHierarchy.DISJOINT ||
                th.relationship(exp.getItemType(th), BuiltInAtomicType.FLOAT) != TypeHierarchy.DISJOINT;
    }

    /**
     * Return the negation of this value comparison: that is, a value comparison that returns true()
     * if and only if the original returns false(). The result must be the same as not(this) even in the
     * case where one of the operands is ().
     * @return the inverted comparison
     */

    public Expression negate() {
        ValueComparison vc = new ValueComparison(operand0, Token.negate(operator), operand1);
        vc.comparer = comparer;
        if (resultWhenEmpty == null || resultWhenEmpty == BooleanValue.FALSE) {
            vc.resultWhenEmpty = BooleanValue.TRUE;
        } else {
            vc.resultWhenEmpty = BooleanValue.FALSE;
        }
        ExpressionTool.copyLocationInfo(this, vc);
        return vc;
    }


    /**
     * Test whether an expression is constant zero
     * @param v the value to be tested
     * @return true if the operand is the constant zero (of any numeric data type)
     */

    private static boolean isZero(Value v) {
        return v instanceof NumericValue && ((NumericValue)v).compareTo(0) == 0;
    }


    /**
     * Copy an expression. This makes a deep copy.
     *
     * @return the copy of the original expression
     */

    public Expression copy() {
        ValueComparison vc = new ValueComparison(operand0.copy(), operator, operand1.copy());
        vc.comparer = comparer;
        vc.resultWhenEmpty = resultWhenEmpty;
        //vc.operand0MaybeUntyped = operand0MaybeUntyped;
        //vc.operand1MaybeUntyped = operand1MaybeUntyped;
        vc.needsRuntimeComparabilityCheck = needsRuntimeComparabilityCheck;
        return vc;
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
//            if (operand0MaybeUntyped && v0 instanceof UntypedAtomicValue) {
//                // commmented out 2007-12-03 - not really necessary? If not, can lose the untyped0MaybeUntyped variables
//                v0 = new StringValue(v0.getStringValueCS());
//            }
            AtomicValue v1 = ((AtomicValue) operand1.evaluateItem(context));
            if (v1 == null) {
                return (resultWhenEmpty == BooleanValue.TRUE);  // normally false
            }
//            if (operand1MaybeUntyped && v1 instanceof UntypedAtomicValue) {
//                v1 = new StringValue(v1.getStringValueCS());
//            }
            if (needsRuntimeComparabilityCheck &&
                    !Type.isComparable(v0.getPrimitiveType(), v1.getPrimitiveType(), Token.isOrderedOperator(operator))) {
                XPathException e2 = new XPathException("Cannot compare " + Type.displayTypeName(v0) +
                    " to " + Type.displayTypeName(v1));
                e2.setErrorCode("XPTY0004");
                e2.setIsTypeError(true);
                throw e2;
            }
            return compare(v0, operator, v1, comparer.provideContext(context));
        } catch (XPathException e) {
            // re-throw the exception with location information added
            e.maybeSetLocation(this);
            e.maybeSetContext(context);
            throw e;
        } catch (ClassCastException err) {
            throw new XPathException("CCE", this);
        }
    }

    /**
     * Compare two atomic values, using a specified operator and collation
     *
     * @param v0       the first operand
     * @param op       the operator, as defined by constants such as {@link Token#FEQ} or
     *                 {@link Token#FLT}
     * @param v1       the second operand
     * @param collator the Collator to be used when comparing strings
     * @return the result of the comparison: -1 for LT, 0 for EQ, +1 for GT
     * @throws XPathException if the values are not comparable
     */

    static boolean compare(AtomicValue v0, int op, AtomicValue v1, AtomicComparer collator)
            throws XPathException {
        if (v0.isNaN() || v1.isNaN()) {
            return (op == Token.FNE);
        }
        try {
            switch (op) {
                case Token.FEQ:
                    return collator.comparesEqual(v0, v1);
                case Token.FNE:
                    return !collator.comparesEqual(v0, v1);
                case Token.FGT:
                    return collator.compareAtomicValues(v0, v1) > 0;
                case Token.FLT:
                    return collator.compareAtomicValues(v0, v1) < 0;
                case Token.FGE:
                    return collator.compareAtomicValues(v0, v1) >= 0;
                case Token.FLE:
                    return collator.compareAtomicValues(v0, v1) <= 0;
                default:
                    throw new UnsupportedOperationException("Unknown operator " + op);
            }
        } catch (ClassCastException err) {
            XPathException e2 = new XPathException("Cannot compare " + Type.displayTypeName(v0) +
                    " to " + Type.displayTypeName(v1));
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
            AtomicValue v0 = (AtomicValue) operand0.evaluateItem(context);
            if (v0 == null) {
                return resultWhenEmpty;
            }
//            if (v0 instanceof UntypedAtomicValue) {
//                v0 = v0.convert(BuiltInAtomicType.STRING, true, context).asAtomic();
//            }
            AtomicValue v1 = (AtomicValue) operand1.evaluateItem(context);
            if (v1 == null) {
                return resultWhenEmpty;
            }
//            if (v1 instanceof UntypedAtomicValue) {
//                v1 = v1.convert(BuiltInAtomicType.STRING, true, context).asAtomic();
//            }
            if (needsRuntimeComparabilityCheck &&
                    !Type.isComparable(v0.getPrimitiveType(), v1.getPrimitiveType(), Token.isOrderedOperator(operator))) {
                XPathException e2 = new XPathException("Cannot compare " + Type.displayTypeName(v0) +
                    " to " + Type.displayTypeName(v1));
                e2.setErrorCode("XPTY0004");
                e2.setIsTypeError(true);
                throw e2;
            }
            return BooleanValue.get(compare(v0, operator, v1, comparer.provideContext(context)));
        } catch (XPathException e) {
            // re-throw the exception with location information added
            e.maybeSetLocation(this);
            e.maybeSetContext(context);
            throw e;
        }
    }


    /**
     * Determine the data type of the expression
     *
     * @param th the type hierarchy cache
     * @return Type.BOOLEAN
     */

    public ItemType getItemType(TypeHierarchy th) {
        return BuiltInAtomicType.BOOLEAN;
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
