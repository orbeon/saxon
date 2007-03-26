package org.orbeon.saxon.expr;

import org.orbeon.saxon.Configuration;
import org.orbeon.saxon.Err;
import org.orbeon.saxon.om.Item;
import org.orbeon.saxon.trans.DynamicError;
import org.orbeon.saxon.trans.StaticError;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.type.*;
import org.orbeon.saxon.value.*;

/**
 * Arithmetic Expression: an expression using one of the operators
 * plus, minus, multiply, div, idiv, mod.
 */

public class ArithmeticExpression extends BinaryExpression {

    private static final class Signature {
        int operand0;
        int operand1;
        int operation;
        ItemType resultType;

        Signature(int t0, int t1, int op, ItemType r) {
            operand0 = t0;
            operand1 = t1;
            operation = op;
            resultType = r;
        }
    }

    private static final int NUMERIC_ARITHMETIC = 0;
    private static final int DATE_AND_DURATION = 1;
    private static final int DATE_DIFFERENCE = 2;
    private static final int DURATION_ADDITION = 3;
    private static final int DURATION_MULTIPLICATION = 4;
    private static final int DURATION_DIVISION = 5;

    private static final int UNKNOWN = -1;
    private static final int UNKNOWN_10 = -2;

    private static final Signature[] plusTable = {
        new Signature(Type.NUMBER, Type.NUMBER, NUMERIC_ARITHMETIC, Type.NUMBER_TYPE),
        new Signature(Type.NUMBER, Type.UNTYPED_ATOMIC, NUMERIC_ARITHMETIC, Type.NUMBER_TYPE),
        new Signature(Type.UNTYPED_ATOMIC, Type.NUMBER, NUMERIC_ARITHMETIC, Type.NUMBER_TYPE),
        new Signature(Type.UNTYPED_ATOMIC, Type.UNTYPED_ATOMIC, NUMERIC_ARITHMETIC, Type.NUMBER_TYPE),

        new Signature(Type.DATE, Type.DURATION, DATE_AND_DURATION, Type.DATE_TYPE),
        new Signature(Type.DURATION, Type.DATE, DATE_AND_DURATION, Type.DATE_TYPE),

        new Signature(Type.TIME, Type.DURATION, DATE_AND_DURATION, Type.TIME_TYPE),
        new Signature(Type.DURATION, Type.TIME, DATE_AND_DURATION, Type.TIME_TYPE),

        new Signature(Type.DATE_TIME, Type.DURATION, DATE_AND_DURATION, Type.DATE_TIME_TYPE),
        new Signature(Type.DURATION, Type.DATE_TIME, DATE_AND_DURATION, Type.DATE_TIME_TYPE),

        new Signature(Type.DURATION, Type.DURATION, DURATION_ADDITION, Type.DURATION_TYPE)

    };

    private static final Signature[] minusTable = {
        new Signature(Type.NUMBER, Type.NUMBER, NUMERIC_ARITHMETIC, Type.NUMBER_TYPE),
        new Signature(Type.NUMBER, Type.UNTYPED_ATOMIC, NUMERIC_ARITHMETIC, Type.NUMBER_TYPE),
        new Signature(Type.UNTYPED_ATOMIC, Type.NUMBER, NUMERIC_ARITHMETIC, Type.NUMBER_TYPE),
        new Signature(Type.UNTYPED_ATOMIC, Type.UNTYPED_ATOMIC, NUMERIC_ARITHMETIC, Type.NUMBER_TYPE),

        new Signature(Type.DATE, Type.DATE, DATE_DIFFERENCE, Type.DAY_TIME_DURATION_TYPE),
        new Signature(Type.DATE, Type.DURATION, DATE_AND_DURATION, Type.DATE_TYPE),

        new Signature(Type.TIME, Type.TIME, DATE_DIFFERENCE, Type.DAY_TIME_DURATION_TYPE),
        new Signature(Type.TIME, Type.DURATION, DATE_AND_DURATION, Type.TIME_TYPE),

        new Signature(Type.DATE_TIME, Type.DATE_TIME, DATE_DIFFERENCE, Type.DAY_TIME_DURATION_TYPE),
        new Signature(Type.DATE_TIME, Type.DURATION, DATE_AND_DURATION, Type.DATE_TIME_TYPE),

        new Signature(Type.DURATION, Type.DURATION, DURATION_ADDITION, Type.DURATION_TYPE)

    };

    private static final Signature[] multiplyTable = {
        new Signature(Type.NUMBER, Type.NUMBER, NUMERIC_ARITHMETIC, Type.NUMBER_TYPE),
        new Signature(Type.NUMBER, Type.UNTYPED_ATOMIC, NUMERIC_ARITHMETIC, Type.NUMBER_TYPE),
        new Signature(Type.UNTYPED_ATOMIC, Type.NUMBER, NUMERIC_ARITHMETIC, Type.NUMBER_TYPE),
        new Signature(Type.UNTYPED_ATOMIC, Type.UNTYPED_ATOMIC, NUMERIC_ARITHMETIC, Type.NUMBER_TYPE),

        new Signature(Type.NUMBER, Type.DURATION, DURATION_MULTIPLICATION, Type.DURATION_TYPE),
        new Signature(Type.DURATION, Type.NUMBER, DURATION_MULTIPLICATION, Type.DURATION_TYPE)
    };

    private static final Signature[] divideTable = {
        new Signature(Type.NUMBER, Type.NUMBER, NUMERIC_ARITHMETIC, Type.NUMBER_TYPE),
        new Signature(Type.NUMBER, Type.UNTYPED_ATOMIC, NUMERIC_ARITHMETIC, Type.NUMBER_TYPE),
        new Signature(Type.UNTYPED_ATOMIC, Type.NUMBER, NUMERIC_ARITHMETIC, Type.NUMBER_TYPE),
        new Signature(Type.UNTYPED_ATOMIC, Type.UNTYPED_ATOMIC, NUMERIC_ARITHMETIC, Type.NUMBER_TYPE),

        new Signature(Type.DURATION, Type.NUMBER, DURATION_MULTIPLICATION, Type.DURATION_TYPE),
        new Signature(Type.DURATION, Type.DURATION, DURATION_DIVISION, Type.NUMBER_TYPE)
    };

    private static final Signature[] idivTable = {
        new Signature(Type.NUMBER, Type.NUMBER, NUMERIC_ARITHMETIC, Type.INTEGER_TYPE),
        new Signature(Type.NUMBER, Type.UNTYPED_ATOMIC, NUMERIC_ARITHMETIC, Type.INTEGER_TYPE),
        new Signature(Type.UNTYPED_ATOMIC, Type.NUMBER, NUMERIC_ARITHMETIC, Type.INTEGER_TYPE),
        new Signature(Type.UNTYPED_ATOMIC, Type.UNTYPED_ATOMIC, NUMERIC_ARITHMETIC, Type.INTEGER_TYPE)
    };

    private static final Signature[] modTable = {
        new Signature(Type.NUMBER, Type.NUMBER, NUMERIC_ARITHMETIC, Type.NUMBER_TYPE),
        new Signature(Type.NUMBER, Type.UNTYPED_ATOMIC, NUMERIC_ARITHMETIC, Type.NUMBER_TYPE),
        new Signature(Type.UNTYPED_ATOMIC, Type.NUMBER, NUMERIC_ARITHMETIC, Type.NUMBER_TYPE),
        new Signature(Type.UNTYPED_ATOMIC, Type.UNTYPED_ATOMIC, NUMERIC_ARITHMETIC, Type.NUMBER_TYPE)
    };

    private boolean backwardsCompatible = false;

    public ArithmeticExpression(Expression p1, int operator, Expression p2) {
        super(p1, operator, p2);
    }

    /**
     * Type-check the expression statically. We try to work out which particular
     * arithmetic function to use if the types of operands are known an compile time.
     */

    public Expression typeCheck(StaticContext env, ItemType contextItemType) throws XPathException {

        final TypeHierarchy th = env.getConfiguration().getTypeHierarchy();
        backwardsCompatible = env.isInBackwardsCompatibleMode();

        Expression oldOp0 = operand0;
        Expression oldOp1 = operand1;

        operand0 = operand0.typeCheck(env, contextItemType);
        operand1 = operand1.typeCheck(env, contextItemType);


        SequenceType atomicType = SequenceType.OPTIONAL_ATOMIC;

        RoleLocator role0 = new RoleLocator(RoleLocator.BINARY_EXPR, Token.tokens[operator], 0, null);
        role0.setSourceLocator(this);
        operand0 = TypeChecker.staticTypeCheck(operand0, atomicType, backwardsCompatible, role0, env);

        // System.err.println("First operand"); operand0.display(10);

        RoleLocator role1 = new RoleLocator(RoleLocator.BINARY_EXPR, Token.tokens[operator], 1, null);
        role1.setSourceLocator(this);
        operand1 = TypeChecker.staticTypeCheck(operand1, atomicType, backwardsCompatible, role1, env);

        if (operand0 != oldOp0) {
            adoptChildExpression(operand0);
        }

        if (operand1 != oldOp1) {
            adoptChildExpression(operand1);
        }

        if (backwardsCompatible) {
            if (operand0.getCardinality() == StaticProperty.EMPTY ||
                    operand1.getCardinality() == StaticProperty.EMPTY) {
                return DoubleValue.NaN;
            }
            Arithmetic10 exp = new Arithmetic10(operand0, operator, operand1);
            exp.setParentExpression(getParentExpression());
            ExpressionTool.copyLocationInfo(this, exp);
            return exp.simplify(env).typeCheck(env, contextItemType);
        }

        if (operand0.getCardinality() == StaticProperty.EMPTY ||
                operand1.getCardinality() == StaticProperty.EMPTY) {
            return EmptySequence.getInstance();
        }

        Expression e = super.typeCheck(env, contextItemType);
        if (e instanceof ArithmeticExpression) {
            int type0 = operand0.getItemType(th).getPrimitiveType();
            int type1 = operand1.getItemType(th).getPrimitiveType();
            final int action = getAction(type0, operator, type1, false);
            switch (action) {
                case NUMERIC_ARITHMETIC:
                    e = new NumericArithmetic(operand0, operator, operand1);
                    break;
                case DURATION_ADDITION:
                    e = new DurationAddition(operand0, operator, operand1);
                    break;
                case DURATION_MULTIPLICATION:
                    e = new DurationMultiplication(operand0, operator, operand1, env.getConfiguration());
                    break;
                case DURATION_DIVISION:
                    e = new DurationDivision(operand0, operator, operand1);
                    break;
                case DATE_AND_DURATION:
                    e = new DateAndDuration(operand0, operator, operand1, env.getConfiguration());
                    break;
                case DATE_DIFFERENCE:
                    e = new DateDifference(operand0, operator, operand1);
                    break;

                case UNKNOWN_10:
                    // can't actually happen on this path
                    e = new Arithmetic10(operand0, operator, operand1);
                    break;

                case UNKNOWN:
                    // either the types are not known yet, or they are wrong
                    if (    env.getConfiguration().getSchemaType(type0).isAtomicType() &&
                            type0 != Type.UNTYPED_ATOMIC &&
                            type0 != Type.ANY_ATOMIC &&
                            env.getConfiguration().getSchemaType(type1).isAtomicType() &&
                            type1 != Type.UNTYPED_ATOMIC &&
                            type1 != Type.ANY_ATOMIC) {
                        StaticError err = new StaticError("Unsuitable operands for arithmetic operation (" +
                                env.getNamePool().getDisplayName(type0) + ", " +
                                env.getNamePool().getDisplayName(type1) + ')');
                        err.setErrorCode("XPTY0004");
                        err.setIsTypeError(true);
                        err.setLocator(ExpressionTool.getLocator(this));
                        throw err;
                    }
                    // defer the decision on which method to use until run-time
                    return e;
            }
            ExpressionTool.copyLocationInfo(this, e);
            ComputedExpression.setParentExpression(e, getParentExpression());
// code deleted because we've already done this in super.typeCheck()
//            try {
//                if (operand0 instanceof Value && operand1 instanceof Value) {
//                    return (Value)e.evaluateItem(env.makeEarlyEvaluationContext());
//                }
//            } catch (DynamicError err) {
//                // Defer any error reporting until run-time
//            }
        }
        return e;
    }


    /**
     * Decide what action is required based on types of operands
     */

    private static int getAction(int type1, int operator, int type2, boolean backwardsCompatible) {
        if (type1 == Type.DAY_TIME_DURATION || type1 == Type.YEAR_MONTH_DURATION) {
            type1 = Type.DURATION;
        }
        if (type2 == Type.DAY_TIME_DURATION || type2 == Type.YEAR_MONTH_DURATION) {
            type2 = Type.DURATION;
        }
        Signature[] table = getOperatorTable(operator);
        int entry = getEntry(table, type1, type2);
        if (entry < 0) {
            return (backwardsCompatible ? UNKNOWN_10 : UNKNOWN);
        }
        return table[entry].operation;
    }

    private static Signature[] getOperatorTable(int operator) {
        switch (operator) {
            case Token.PLUS:
                return plusTable;
            case Token.MINUS:
            case Token.NEGATE:
                return minusTable;
            case Token.MULT:
                return multiplyTable;
            case Token.DIV:
                return divideTable;
            case Token.IDIV:
                return idivTable;
            case Token.MOD:
                return modTable;
            default:
                throw new IllegalArgumentException("Unknown arithmetic operator");
        }
    }

    private static int getEntry(Signature[] table, int type1, int type2) {
        if (Type.isNumericPrimitiveType(type1)) {
            type1 = Type.NUMBER;
        }
        if (Type.isNumericPrimitiveType(type2)) {
            type2 = Type.NUMBER;
        }
        for (int i = 0; i < table.length; i++) {
            if (type1 == table[i].operand0 &&
                    type2 == table[i].operand1) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Determine the data type of the expression, if this is known statically
     * @param th
     */

    public ItemType getItemType(TypeHierarchy th) {
        final ItemType t1 = operand0.getItemType(th);
        final int pt1 = t1.getPrimitiveType();
        final ItemType t2 = operand1.getItemType(th);
        final int pt2 = t2.getPrimitiveType();
        final Signature[] table = getOperatorTable(operator);
        final int entry = getEntry(table, pt1, pt2);

        if (entry < 0) {
            return Type.ANY_ATOMIC_TYPE;  // type is not known statically
        }

        ItemType resultType = table[entry].resultType;
        if (resultType == Type.NUMBER_TYPE) {
            AtomicType pat1 = (AtomicType)BuiltInSchemaFactory.getSchemaType(pt1);
            AtomicType pat2 = (AtomicType)BuiltInSchemaFactory.getSchemaType(pt2);
            resultType = NumericValue.promote(pat1, pat2, th);
            if (operator == Token.DIV && resultType == Type.INTEGER_TYPE) {
                // exception: integer div integer => decimal
                resultType = Type.DECIMAL_TYPE;
            } else {
                resultType = NumericValue.promote(pat1, pat2, th);
            }
        } else if (resultType == Type.DURATION_TYPE) {
            // if one of the operands is a subtype of duration, then the result will be the same subtype
            // (this isn't captured in the table because these types are not primitive).
            if (th.isSubType(t1, Type.DAY_TIME_DURATION_TYPE)) {
                resultType = Type.DAY_TIME_DURATION_TYPE;
            } else if (th.isSubType(t2, Type.DAY_TIME_DURATION_TYPE)) {
                resultType = Type.DAY_TIME_DURATION_TYPE;
            } else if (th.isSubType(t1, Type.YEAR_MONTH_DURATION_TYPE)) {
                resultType = Type.YEAR_MONTH_DURATION_TYPE;
            } else if (th.isSubType(t2, Type.YEAR_MONTH_DURATION_TYPE)) {
                resultType = Type.YEAR_MONTH_DURATION_TYPE;
            }
        }
        return resultType;
    }

    /**
     * Evaluate the expression. We only take this path if the type could not be determined
     * statically.
     */

    public Item evaluateItem(XPathContext context) throws XPathException {

        AtomicValue v1 = (AtomicValue)operand0.evaluateItem(context);
        if (v1 == null) {
            return null;
        }
        v1 = v1.getPrimitiveValue();

        AtomicValue v2 = (AtomicValue)operand1.evaluateItem(context);
        if (v2 == null) {
            return null;
        }
        v2 = v2.getPrimitiveValue();

        Configuration config = context.getConfiguration();
        final TypeHierarchy th = config.getTypeHierarchy();
        int action = getAction(v1.getItemType(th).getPrimitiveType(),
                operator,
                v2.getItemType(th).getPrimitiveType(),
                backwardsCompatible);

        Expression e;
        switch (action) {
            case NUMERIC_ARITHMETIC:
                e = new NumericArithmetic(v1, operator, v2);
                break;
            case DURATION_ADDITION:
                e = new DurationAddition(v1, operator, v2);
                break;
            case DURATION_MULTIPLICATION:
                e = new DurationMultiplication(v1, operator, v2, config);
                break;
            case DURATION_DIVISION:
                e = new DurationDivision(v1, operator, v2);
                break;
            case DATE_AND_DURATION:
                e = new DateAndDuration(v1, operator, v2, config);
                break;
            case DATE_DIFFERENCE:
                e = new DateDifference(v1, operator, v2);
                break;
            default:
                // types are not known yet. Force to double if in 1.0 mode
                if (backwardsCompatible) {
                    NumericValue nv1;
                    NumericValue nv2;
                    try {
                        nv1 = (NumericValue)v1.convert(Type.DOUBLE, context);
                        nv2 = (NumericValue)v2.convert(Type.DOUBLE, context);
                    } catch (XPathException err) {
                        typeError("Unsuitable operands for arithmetic operation (" +
                                v1.getItemType(th) + ", " +
                                v2.getItemType(th) + ')', "XPTY0004", context);
                        return null;
                    }
                    e = new NumericArithmetic(nv1, operator, nv2);
                } else {
                    typeError("Unsuitable operands for arithmetic operation (" +
                            v1.getItemType(th) + ", " +
                            v2.getItemType(th) + ')', "XPTY0004", context);
                    return null;
                }
        }
        ExpressionTool.copyLocationInfo(this, e);
        // this is a little hairy, because the new expression isn't a child of its parent. But it only
        // exists transiently. We can't change the parent expression at run time for thread safety reasons.
        ComputedExpression.setParentExpression(e, getParentExpression());
        return e.evaluateItem(context);
    }

    /**
     * Inner class to handle 1.0 backwards compatible arithmetic. Note that even though we are
     * in 1.0 mode, we still need to handle types such as dates and durations. However, in this
     * case all the decisions are made dynamically at run time.
     */
    private static class Arithmetic10 extends BinaryExpression {

        public Arithmetic10(Expression p1, int operator, Expression p2) {
            super(p1, operator, p2);
        }

        public Expression typeCheck(StaticContext env, ItemType contextItemType) throws XPathException {

            final TypeHierarchy th = env.getConfiguration().getTypeHierarchy();
            SequenceType atomicType = SequenceType.OPTIONAL_ATOMIC;

            RoleLocator role0 = new RoleLocator(RoleLocator.BINARY_EXPR, Token.tokens[operator], 0, null);
            role0.setSourceLocator(this);
            operand0 = TypeChecker.staticTypeCheck(operand0, atomicType, true, role0, env);

            RoleLocator role1 = new RoleLocator(RoleLocator.BINARY_EXPR, Token.tokens[operator], 1, null);
            role1.setSourceLocator(this);
            operand1 = TypeChecker.staticTypeCheck(operand1, atomicType, true, role1, env);

            Expression e = super.typeCheck(env, contextItemType);
            if (e instanceof ArithmeticExpression) {
                int type0 = operand0.getItemType(th).getPrimitiveType();
                int type1 = operand1.getItemType(th).getPrimitiveType();

                if (type0 == Type.BOOLEAN) {
                    type0 = Type.NUMBER;
                } else if (type0 == Type.STRING) {
                    type0 = Type.NUMBER;
                }
                if (type1 == Type.BOOLEAN) {
                    type1 = Type.NUMBER;
                } else if (type1 == Type.STRING) {
                    type1 = Type.NUMBER;
                }

                final int action = getAction(type0, operator, type1, true);
                switch (action) {
                    case NUMERIC_ARITHMETIC:
                        e = new NumericArithmetic(operand0, operator, operand1);
                        ((NumericArithmetic)e).setBackwardsCompatible(true);
                        break;
                    case DURATION_ADDITION:
                        e = new DurationAddition(operand0, operator, operand1);
                        break;
                    case DURATION_MULTIPLICATION:
                        e = new DurationMultiplication(operand0, operator, operand1, env.getConfiguration());
                        break;
                    case DURATION_DIVISION:
                        e = new DurationDivision(operand0, operator, operand1);
                        break;
                    case DATE_AND_DURATION:
                        e = new DateAndDuration(operand0, operator, operand1, env.getConfiguration());
                        break;
                    case DATE_DIFFERENCE:
                        e = new DateDifference(operand0, operator, operand1);
                        break;

                    case UNKNOWN_10:
                        //e = new Arithmetic10(operand0, operator, operand1);
                        // no action: defer the decision until run-time
                        break;

                    case UNKNOWN:
                        // either the types are not known yet, or they are wrong
                        if (env.getConfiguration().getSchemaType(type0).isAtomicType() &&
                                type0 != Type.UNTYPED_ATOMIC &&
                                type0 != Type.ANY_ATOMIC &&
                                env.getConfiguration().getSchemaType(type1).isAtomicType() &&
                                type1 != Type.UNTYPED_ATOMIC &&
                                type1 != Type.ANY_ATOMIC) {
                            StaticError err = new StaticError("Unsuitable operands for arithmetic operation (" +
                                    env.getNamePool().getDisplayName(type0) + ", " +
                                    env.getNamePool().getDisplayName(type1) + ')');
                            err.setErrorCode("XPTY0004");
                            err.setIsTypeError(true);
                            throw err;
                        }
                        return e;
                }
                ExpressionTool.copyLocationInfo(this, e);
                ComputedExpression.setParentExpression(e, getParentExpression());
// code deleted: already done during super.typeCheck()
//                try {
//                    if (operand0 instanceof Value && operand1 instanceof Value) {
//                        return (Value)e.evaluateItem(env.makeEarlyEvaluationContext());
//                    }
//                } catch (DynamicError err) {
//                    // Defer any error reporting until run-time
//                }
            }
            return e;
        }


        /**
         * Determine the data type of the expression, if possible. All expression return
         * sequences, in general; this method determines the type of the items within the
         * sequence, assuming that (a) this is known in advance, and (b) it is the same for
         * all items in the sequence.
         * <p/>
         * <p>This method should always return a result, though it may be the best approximation
         * that is available at the time.</p>
         *
         * @return a value such as Type.STRING, Type.BOOLEAN, Type.NUMBER,
         *         Type.NODE, or Type.ITEM (meaning not known at compile time)
         * @param th
         */

        public ItemType getItemType(TypeHierarchy th) {
            return Type.ANY_ATOMIC_TYPE;
        }

        /**
         * Evaluate the expression.
         */

        public Item evaluateItem(XPathContext context) throws XPathException {

            AtomicValue v1 = (AtomicValue)operand0.evaluateItem(context);
            if (v1 == null) {
                return DoubleValue.NaN;
            }
            v1 = v1.getPrimitiveValue();
            if (v1 instanceof BooleanValue || v1 instanceof StringValue || v1 instanceof NumericValue) {
                try {
                    v1 = v1.convert(Type.DOUBLE, context);
                } catch (XPathException e) {
                    return DoubleValue.NaN;
                }
            }

            AtomicValue v2 = (AtomicValue)operand1.evaluateItem(context);
            if (v2 == null) {
                return DoubleValue.NaN;
            }
            v2 = v2.getPrimitiveValue();
            if (v2 instanceof BooleanValue || v2 instanceof StringValue || v2 instanceof NumericValue) {
                try {
                    v2 = v2.convert(Type.DOUBLE, context);
                } catch (XPathException e) {
                    return DoubleValue.NaN;
                }
            }

            final TypeHierarchy th = context.getConfiguration().getTypeHierarchy();
            int action = getAction(v1.getItemType(th).getPrimitiveType(),
                    operator,
                    v2.getItemType(th).getPrimitiveType(),
                    true);

            switch (action) {
                case NUMERIC_ARITHMETIC:
                    try {
                        return NumericArithmetic.doArithmetic(v1, operator, v2, context, true);
                    } catch (XPathException ex) {
                        if (ex.getLocator() == null) {
                            ex.setLocator(this);
                        }
                        throw ex;
                    }

                case DURATION_ADDITION:
                    try {
                        return DurationAddition.doArithmetic(v1, operator, v2, context);
                    } catch (XPathException ex) {
                        if (ex.getLocator() == null) {
                            ex.setLocator(this);
                        }
                        throw ex;
                    }

                case DURATION_MULTIPLICATION:
                    try {
                        return DurationMultiplication.doArithmetic(v1, operator, v2, context);
                    } catch (XPathException ex) {
                        if (ex.getLocator() == null) {
                            ex.setLocator(this);
                        }
                        throw ex;
                    }

                case DURATION_DIVISION:
                    try {
                        return DurationDivision.doArithmetic(v1, v2, context);
                    } catch (XPathException ex) {
                        if (ex.getLocator() == null) {
                            ex.setLocator(this);
                        }
                        throw ex;
                    }

                case DATE_AND_DURATION:
                    try {
                        return DateAndDuration.doArithmetic(v1, operator, v2, context);
                    } catch (XPathException ex) {
                        if (ex.getLocator() == null) {
                            ex.setLocator(this);
                        }
                        throw ex;
                    }

                case DATE_DIFFERENCE:
                    try {
                        return DateDifference.doArithmetic(v1, v2, context);
                    } catch (XPathException ex) {
                        if (ex.getLocator() == null) {
                            ex.setLocator(this);
                        }
                        throw ex;
                    }

                default:
                    typeError("Unsuitable operands for arithmetic operation (" +
                            v1.getItemType(th) + ", " +
                            v2.getItemType(th) + ')', "XPTY0004", context);
                    return null;
            }

        }
    }

    /**
     * Inner class to handle numeric arithmetic expressions
     */

    public static class NumericArithmetic extends ArithmeticExpression {

        // TODO: do more of the work at compile-time
        // * eliminate the possibility of untypedAtomic operands
        // * don't use this class for 1.0 arithmetic
        // * generate type promotion code e.g. integer->decimal or decimal->double
        // * separate classes for integer/decimal/float/double arithmetic

        // * Change the structure to use delegation rather than inheritance: delegate to a Calculator
        // * object (which is not itself an expression, rather like a Comparer) and which can be allocated
        // * either statically or dynamically.

        boolean backCompatible = false;

        public NumericArithmetic(Expression p1, int operator, Expression p2) {
            super(p1, operator, p2);
        }

        public void setBackwardsCompatible(boolean flag) {
            backCompatible = flag;
        }

        public boolean isBackwardsCompatible() {
            return backCompatible;
        }

        /**
         * Evaluate the expression.
         */

        public Item evaluateItem(XPathContext context) throws XPathException {
            try {
                return doArithmetic(operand0, operator, operand1, context, backCompatible);
            } catch (XPathException err) {
                if (err.getLocator() == null) {
                    err.setLocator(this);
                }
                throw err;
            }
        }

        public static Item doArithmetic(Expression operand0, int operator, Expression operand1,
                                        XPathContext context, boolean backwardsCompatible)
                throws XPathException {


            AtomicValue v1 = ((AtomicValue)operand0.evaluateItem(context));
            if (v1 == null) {
                return (backwardsCompatible ? DoubleValue.NaN : null);
            }

            if (v1 instanceof UntypedAtomicValue) {
                try {
                    v1 = new DoubleValue(Value.stringToNumber(v1.getStringValueCS()));
                } catch (NumberFormatException e) {
                   if (backwardsCompatible) {
                        v1 = DoubleValue.NaN;
                    } else {
                        DynamicError err = new DynamicError("Failure converting untyped value " +
                            Err.wrap(v1.getStringValueCS(), Err.VALUE) + " to a number");
                        err.setErrorCode("FORG0001");
                        err.setXPathContext(context);
                        throw err;
                    }
                }
            } else {
                v1 = v1.getPrimitiveValue();
            }
            AtomicValue v2 = ((AtomicValue)operand1.evaluateItem(context));
            if (v2 == null) {
                return (backwardsCompatible ? DoubleValue.NaN : null);
            }

            if (v2 instanceof UntypedAtomicValue) {
                try {
                    v2 = new DoubleValue(Value.stringToNumber(v2.getStringValueCS()));
                } catch (NumberFormatException e) {
                    if (backwardsCompatible) {
                        v2 = DoubleValue.NaN;
                    } else {
                        DynamicError err = new DynamicError("Failure converting untyped value " +
                            Err.wrap(v2.getStringValueCS(), Err.VALUE) + " to a number");
                        err.setErrorCode("FORG0001");
                        err.setXPathContext(context);
                        throw err;
                    }
                }
            } else {
                v2 = v2.getPrimitiveValue();
            }

            if (operator == Token.NEGATE) {
                return ((NumericValue)v2).negate();
            } else {
                try {
                    return ((NumericValue)v1).arithmetic(operator, (NumericValue)v2, context);
                } catch (DynamicError err) {
                    if (err.getXPathContext() == null) {
                        err.setXPathContext(context);
                    }
                    throw err;
                } catch (ArithmeticException err) {
                    DynamicError e = new DynamicError("Arithmetic exception: " + err.getMessage());
                    e.setXPathContext(context);
                    throw e;
                }
            }
        }
    }

    /**
     * Inner class to handle addition and subtraction of two durations
     */

    public static class DurationAddition extends ArithmeticExpression {

        public DurationAddition(Expression p1, int operator, Expression p2) {
            super(p1, operator, p2);
        }

        /**
         * Evaluate the expression.
         */

        public Item evaluateItem(XPathContext context) throws XPathException {
            try {
                return doArithmetic(operand0, operator, operand1, context);
            } catch (XPathException err) {
                if (err.getLocator() == null) {
                    err.setLocator(this);
                }
                throw err;
            }
        }

        public static Item doArithmetic(Expression operand0, int operator, Expression operand1, XPathContext context)
        throws XPathException {

            AtomicValue av1 = (AtomicValue)operand0.evaluateItem(context);
            if (av1 == null) {
                return null;
            }
            DurationValue v1 = (DurationValue)av1.getPrimitiveValue();

            AtomicValue av2 = (AtomicValue)operand1.evaluateItem(context);
            if (av2 == null) {
                return null;
            }
            DurationValue v2 = (DurationValue)av2.getPrimitiveValue();

            if (operator == Token.PLUS) {
                return v1.add(v2);
            } else if (operator == Token.MINUS) {
                return v1.subtract(v2);
            } else {
                throw new AssertionError("Unknown operation on durations");
            }

        }
    }

    /**
     * Inner class to handle multiplication (or division) of a duration by a number
     */

    public static class DurationMultiplication extends ArithmeticExpression {

        public DurationMultiplication(Expression p1, int operator, Expression p2, Configuration config) {

            // by the time we get here, we know that one of the operands is a duration,
            // but it might be either one. We make it the first.

            super(p1, operator, p2);
            final TypeHierarchy th = config.getTypeHierarchy();
            if (th.isSubType(p2.getItemType(th), Type.DURATION_TYPE)) {
                operand0 = p2;
                operand1 = p1;
            }
        }

        /**
         * Evaluate the expression.
         */

        public Item evaluateItem(XPathContext context) throws XPathException {
            try {
                return doArithmetic(operand0, operator, operand1, context);
            } catch (XPathException err) {
                if (err.getLocator() == null) {
                    err.setLocator(this);
                }
                throw err;
            }
        }

        public static Item doArithmetic(Expression operand0, int operator, Expression operand1, XPathContext context)
        throws XPathException {

            AtomicValue av1 = (AtomicValue)operand0.evaluateItem(context);
            if (av1 == null) {
                return null;
            }
            DurationValue v1 = (DurationValue)av1.getPrimitiveValue();

            AtomicValue av2 = (AtomicValue)operand1.evaluateItem(context);
            if (av2 == null) {
                return null;
            }
            NumericValue v2 = (NumericValue)av2.getPrimitiveValue();

            double d = v2.getDoubleValue();

            if (operator == Token.DIV) {
                d = 1.0 / d;
            }

            return v1.multiply(d);

        }
    }

    /**
     * Inner class to handle division of two durations to give a number
     */

    public static class DurationDivision extends ArithmeticExpression {

        public DurationDivision(Expression p1, int operator, Expression p2) {
            super(p1, operator, p2);
        }

        /**
         * Evaluate the expression.
         */

        public Item evaluateItem(XPathContext context) throws XPathException {
            try {
                return doArithmetic(operand0, operand1, context);
            } catch (XPathException err) {
                if (err.getLocator() == null) {
                    err.setLocator(this);
                }
                throw err;
            }
        }

        public static Item doArithmetic(Expression operand0, Expression operand1, XPathContext context)
        throws XPathException {

            AtomicValue av1 = (AtomicValue)operand0.evaluateItem(context);
            if (av1 == null) {
                return null;
            }
            DurationValue v1 = (DurationValue)av1.getPrimitiveValue();

            AtomicValue av2 = (AtomicValue)operand1.evaluateItem(context);
            if (av2 == null) {
                return null;
            }
            DurationValue v2 = (DurationValue)av2.getPrimitiveValue();

            return v1.divide(v2);

        }
    }


    /**
     * Inner class to handle addition or subtraction of a Date (or Time, or DateTime) and a Duration
     */

    public static class DateAndDuration extends ArithmeticExpression {

        public DateAndDuration(Expression p1, int operator, Expression p2, Configuration config) {

            // by the time we get here, we know that one of the operands is a duration,
            // but it might be either one. We make it the second.

            super(p1, operator, p2);
            final TypeHierarchy th = config.getTypeHierarchy();
            if (th.isSubType(p1.getItemType(th), Type.DURATION_TYPE)) {
                operand0 = p2;
                operand1 = p1;
            }

        }

        /**
         * Evaluate the expression.
         */

        public Item evaluateItem(XPathContext context) throws XPathException {
            try {
                return doArithmetic(operand0, operator, operand1, context);
            } catch (XPathException err) {
                if (err.getLocator() == null) {
                    err.setLocator(this);
                }
                throw err;
            }
        }

        public static Item doArithmetic(Expression operand0, int operator, Expression operand1, XPathContext context)
        throws XPathException {
            AtomicValue av1 = (AtomicValue)operand0.evaluateItem(context);
            if (av1 == null) {
                return null;
            }
            CalendarValue v1 = (CalendarValue)av1.getPrimitiveValue();

            AtomicValue av2 = (AtomicValue)operand1.evaluateItem(context);
            if (av2 == null) {
                return null;
            }
            DurationValue v2 = (DurationValue)av2.getPrimitiveValue();

            if (operator == Token.MINUS) {
                v2 = v2.multiply(-1.0);
            }

            return v1.add(v2);

        }
    }

    /**
     * Inner class to handle subtraction of a Date (or Time, or DateTime) from another, to return a Duration
     */

    public static class DateDifference extends ArithmeticExpression {

        public DateDifference(Expression p1, int operator, Expression p2) {
            super(p1, operator, p2);
        }

        /**
         * Evaluate the expression.
         */

        public Item evaluateItem(XPathContext context) throws XPathException {
            try {
                return doArithmetic(operand0, operand1, context);
            } catch (XPathException err) {
                if (err.getLocator() == null) {
                    err.setLocator(this);
                }
                throw err;
            }
        }

        public static Item doArithmetic(Expression operand0, Expression operand1, XPathContext context)
        throws XPathException {
            AtomicValue av1 = (AtomicValue)operand0.evaluateItem(context);
            if (av1 == null) {
                return null;
            }
            CalendarValue v1 = (CalendarValue)av1.getPrimitiveValue();

            AtomicValue av2 = (AtomicValue)operand1.evaluateItem(context);
            if (av2 == null) {
                return null;
            }
            CalendarValue v2 = (CalendarValue)av2.getPrimitiveValue();

            return v1.subtract(v2, context);

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
// The Initial Developer of the Original Code is Michael H. Kay
//
// Portions created by (your name) are Copyright (C) (your legal entity). All Rights Reserved.
//
// Contributor(s): none.
//
