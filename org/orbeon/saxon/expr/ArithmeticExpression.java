package net.sf.saxon.expr;

import net.sf.saxon.om.Item;
import net.sf.saxon.trans.DynamicError;
import net.sf.saxon.trans.StaticError;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.ItemType;
import net.sf.saxon.type.Type;
import net.sf.saxon.type.AtomicType;
import net.sf.saxon.value.*;

/**
 * Arithmetic Expression: an expression using one of the operators
 * plus, minus, multiply, div, idiv, mod.
 */

class ArithmeticExpression extends BinaryExpression {

    private static class Signature {
        ItemType operand0;
        ItemType operand1;
        int operation;
        ItemType resultType;

        Signature(ItemType t0, ItemType t1, int op, ItemType r) {
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

    private static Signature[] plusTable = {
        new Signature(Type.NUMBER_TYPE, Type.NUMBER_TYPE, NUMERIC_ARITHMETIC, Type.NUMBER_TYPE),
        new Signature(Type.NUMBER_TYPE, Type.UNTYPED_ATOMIC_TYPE, NUMERIC_ARITHMETIC, Type.NUMBER_TYPE),
        new Signature(Type.UNTYPED_ATOMIC_TYPE, Type.NUMBER_TYPE, NUMERIC_ARITHMETIC, Type.NUMBER_TYPE),
        new Signature(Type.UNTYPED_ATOMIC_TYPE, Type.UNTYPED_ATOMIC_TYPE, NUMERIC_ARITHMETIC, Type.NUMBER_TYPE),

        new Signature(Type.DATE_TYPE, Type.DURATION_TYPE, DATE_AND_DURATION, Type.DATE_TYPE),
        new Signature(Type.DURATION_TYPE, Type.DATE_TYPE, DATE_AND_DURATION, Type.DATE_TYPE),

        new Signature(Type.TIME_TYPE, Type.DURATION_TYPE, DATE_AND_DURATION, Type.TIME_TYPE),
        new Signature(Type.DURATION_TYPE, Type.TIME_TYPE, DATE_AND_DURATION, Type.TIME_TYPE),

        new Signature(Type.DATE_TIME_TYPE, Type.DURATION_TYPE, DATE_AND_DURATION, Type.DATE_TIME_TYPE),
        new Signature(Type.DURATION_TYPE, Type.DATE_TIME_TYPE, DATE_AND_DURATION, Type.DATE_TIME_TYPE),

        new Signature(Type.DURATION_TYPE, Type.DURATION_TYPE, DURATION_ADDITION, Type.DURATION_TYPE)

    };

    private static Signature[] minusTable = {
        new Signature(Type.NUMBER_TYPE, Type.NUMBER_TYPE, NUMERIC_ARITHMETIC, Type.NUMBER_TYPE),
        new Signature(Type.NUMBER_TYPE, Type.UNTYPED_ATOMIC_TYPE, NUMERIC_ARITHMETIC, Type.NUMBER_TYPE),
        new Signature(Type.UNTYPED_ATOMIC_TYPE, Type.NUMBER_TYPE, NUMERIC_ARITHMETIC, Type.NUMBER_TYPE),
        new Signature(Type.UNTYPED_ATOMIC_TYPE, Type.UNTYPED_ATOMIC_TYPE, NUMERIC_ARITHMETIC, Type.NUMBER_TYPE),

        new Signature(Type.DATE_TYPE, Type.DATE_TYPE, DATE_DIFFERENCE, Type.DAY_TIME_DURATION_TYPE),
        new Signature(Type.DATE_TYPE, Type.DURATION_TYPE, DATE_AND_DURATION, Type.DATE_TYPE),

        new Signature(Type.TIME_TYPE, Type.TIME_TYPE, DATE_DIFFERENCE, Type.DAY_TIME_DURATION_TYPE),
        new Signature(Type.TIME_TYPE, Type.DURATION_TYPE, DATE_AND_DURATION, Type.TIME_TYPE),

        new Signature(Type.DATE_TIME_TYPE, Type.DATE_TIME_TYPE, DATE_DIFFERENCE, Type.DAY_TIME_DURATION_TYPE),
        new Signature(Type.DATE_TIME_TYPE, Type.DURATION_TYPE, DATE_AND_DURATION, Type.DATE_TIME_TYPE),

        new Signature(Type.DURATION_TYPE, Type.DURATION_TYPE, DURATION_ADDITION, Type.DURATION_TYPE)

    };

    private static Signature[] multiplyTable = {
        new Signature(Type.NUMBER_TYPE, Type.NUMBER_TYPE, NUMERIC_ARITHMETIC, Type.NUMBER_TYPE),
        new Signature(Type.NUMBER_TYPE, Type.UNTYPED_ATOMIC_TYPE, NUMERIC_ARITHMETIC, Type.NUMBER_TYPE),
        new Signature(Type.UNTYPED_ATOMIC_TYPE, Type.NUMBER_TYPE, NUMERIC_ARITHMETIC, Type.NUMBER_TYPE),
        new Signature(Type.UNTYPED_ATOMIC_TYPE, Type.UNTYPED_ATOMIC_TYPE, NUMERIC_ARITHMETIC, Type.NUMBER_TYPE),

        new Signature(Type.NUMBER_TYPE, Type.DURATION_TYPE, DURATION_MULTIPLICATION, Type.DURATION_TYPE),
        new Signature(Type.DURATION_TYPE, Type.NUMBER_TYPE, DURATION_MULTIPLICATION, Type.DURATION_TYPE)
    };

    private static Signature[] divideTable = {
        new Signature(Type.NUMBER_TYPE, Type.NUMBER_TYPE, NUMERIC_ARITHMETIC, Type.NUMBER_TYPE),
        new Signature(Type.NUMBER_TYPE, Type.UNTYPED_ATOMIC_TYPE, NUMERIC_ARITHMETIC, Type.NUMBER_TYPE),
        new Signature(Type.UNTYPED_ATOMIC_TYPE, Type.NUMBER_TYPE, NUMERIC_ARITHMETIC, Type.NUMBER_TYPE),
        new Signature(Type.UNTYPED_ATOMIC_TYPE, Type.UNTYPED_ATOMIC_TYPE, NUMERIC_ARITHMETIC, Type.NUMBER_TYPE),

        new Signature(Type.DURATION_TYPE, Type.NUMBER_TYPE, DURATION_MULTIPLICATION, Type.DURATION_TYPE),
        new Signature(Type.DURATION_TYPE, Type.DURATION_TYPE, DURATION_DIVISION, Type.NUMBER_TYPE)
    };

    private static Signature[] idivTable = {
        new Signature(Type.NUMBER_TYPE, Type.NUMBER_TYPE, NUMERIC_ARITHMETIC, Type.INTEGER_TYPE),
        new Signature(Type.NUMBER_TYPE, Type.UNTYPED_ATOMIC_TYPE, NUMERIC_ARITHMETIC, Type.INTEGER_TYPE),
        new Signature(Type.UNTYPED_ATOMIC_TYPE, Type.NUMBER_TYPE, NUMERIC_ARITHMETIC, Type.INTEGER_TYPE),
        new Signature(Type.UNTYPED_ATOMIC_TYPE, Type.UNTYPED_ATOMIC_TYPE, NUMERIC_ARITHMETIC, Type.INTEGER_TYPE)
    };

    private static Signature[] modTable = {
        new Signature(Type.NUMBER_TYPE, Type.NUMBER_TYPE, NUMERIC_ARITHMETIC, Type.NUMBER_TYPE),
        new Signature(Type.NUMBER_TYPE, Type.UNTYPED_ATOMIC_TYPE, NUMERIC_ARITHMETIC, Type.NUMBER_TYPE),
        new Signature(Type.UNTYPED_ATOMIC_TYPE, Type.NUMBER_TYPE, NUMERIC_ARITHMETIC, Type.NUMBER_TYPE),
        new Signature(Type.UNTYPED_ATOMIC_TYPE, Type.UNTYPED_ATOMIC_TYPE, NUMERIC_ARITHMETIC, Type.NUMBER_TYPE)
    };

    private boolean backwardsCompatible = false;

    public ArithmeticExpression(Expression p1, int operator, Expression p2) {
        super(p1, operator, p2);
    }

    /**
     * Type-check the expression statically. We try to work out which particular
     * arithmetic function to use if the types of operands are known an compile time.
     */

    public Expression analyze(StaticContext env, ItemType contextItemType) throws XPathException {

        backwardsCompatible = env.isInBackwardsCompatibleMode();

        operand0 = operand0.analyze(env, contextItemType);
        operand1 = operand1.analyze(env, contextItemType);


        SequenceType atomicType = SequenceType.OPTIONAL_ATOMIC;
        // TODO: this is using the function call rules. Arithetic expressions have slightly different rules.

        RoleLocator role0 = new RoleLocator(RoleLocator.BINARY_EXPR, Token.tokens[operator], 0, null);
        operand0 = TypeChecker.staticTypeCheck(operand0, atomicType, backwardsCompatible, role0, env);
        // System.err.println("First operand"); operand0.display(10);

        RoleLocator role1 = new RoleLocator(RoleLocator.BINARY_EXPR, Token.tokens[operator], 1, null);
        operand1 = TypeChecker.staticTypeCheck(operand1, atomicType, backwardsCompatible, role1, env);
        // System.err.println("Second operand"); operand1.display(10);

        if (backwardsCompatible) {
            Expression exp = new Arithmetic10(operand0, operator, operand1);
            return exp.simplify(env).analyze(env, contextItemType);
        }

        Expression e = super.analyze(env, contextItemType);
        if (e instanceof ArithmeticExpression) {
            AtomicType type0 = (AtomicType)operand0.getItemType().getPrimitiveItemType();
            AtomicType type1 = (AtomicType)operand1.getItemType().getPrimitiveItemType();
            if (backwardsCompatible) {
                if (type0 == Type.BOOLEAN_TYPE) {
                    type0 = Type.NUMBER_TYPE;
                } else if (type0 == Type.STRING_TYPE) {
                    type0 = Type.NUMBER_TYPE;
                }
                if (type1 == Type.BOOLEAN_TYPE) {
                    type1 = Type.NUMBER_TYPE;
                } else if (type1 == Type.STRING_TYPE) {
                    type1 = Type.NUMBER_TYPE;
                }
            }
            final int action = getAction(type0, operator, type1);
            switch (action) {
                case NUMERIC_ARITHMETIC:
                    e = new NumericArithmetic(operand0, operator, operand1);
                    break;
                case DURATION_ADDITION:
                    e = new DurationAddition(operand0, operator, operand1);
                    break;
                case DURATION_MULTIPLICATION:
                    e = new DurationMultiplication(operand0, operator, operand1);
                    break;
                case DURATION_DIVISION:
                    e = new DurationDivision(operand0, operator, operand1);
                    break;
                case DATE_AND_DURATION:
                    e = new DateAndDuration(operand0, operator, operand1);
                    break;
                case DATE_DIFFERENCE:
                    e = new DateDifference(operand0, operator, operand1);
                    break;

                case UNKNOWN_10:
                    e = new Arithmetic10(operand0, operator, operand1);
                    break;

                case UNKNOWN:
                    // either the types are not known yet, or they are wrong
                    if (Type.isSubType(type0, Type.ANY_ATOMIC_TYPE) &&
                            type0 != Type.UNTYPED_ATOMIC_TYPE &&
                            type0 != Type.ANY_ATOMIC_TYPE &&
                            Type.isSubType(type1, Type.ANY_ATOMIC_TYPE) &&
                            type1 != Type.UNTYPED_ATOMIC_TYPE &&
                            type1 != Type.ANY_ATOMIC_TYPE) {
                        StaticError err = new StaticError("Unsuitable operands for arithmetic operation (" +
                                type0.toString(env.getNamePool()) + ", " +
                                type1.toString(env.getNamePool()) + ')');
                        err.setErrorCode("XP0006");
                        err.setIsTypeError(true);
                        err.setLocator(ExpressionTool.getLocator(this));
                        throw err;
                    }
                    return e;
            }
            ExpressionTool.copyLocationInfo(this, e);
            if (e instanceof ComputedExpression) {
                ((ComputedExpression)e).setParentExpression(getParentExpression());
            }
            try {
                if (operand0 instanceof Value && operand1 instanceof Value) {
                    return ExpressionTool.eagerEvaluate(e, null);
                }
            } catch (DynamicError err) {
                // Defer any error reporting until run-time
            }
        }
        return e;
    }

    /**
     * Decide what action is required based on types of operands
     */

    private int getAction(AtomicType type1, int operator, AtomicType type2) {
        if (type1.getFingerprint() == Type.DAY_TIME_DURATION) {
            type1 = Type.DURATION_TYPE;
        } else if (type1.getFingerprint() == Type.YEAR_MONTH_DURATION) {
            type1 = Type.DURATION_TYPE;
        }
        if (type2.getFingerprint() == Type.DAY_TIME_DURATION) {
            type2 = Type.DURATION_TYPE;
        } else if (type2.getFingerprint() == Type.YEAR_MONTH_DURATION) {
            type2 = Type.DURATION_TYPE;
        }
        Signature[] table = getOperatorTable(operator);
        int entry = getEntry(table, type1, type2);
        if (entry < 0) {
            return (backwardsCompatible ? UNKNOWN_10 : UNKNOWN);
        }
        return table[entry].operation;
    }

    private Signature[] getOperatorTable(int operator) {
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

    private int getEntry(Signature[] table, ItemType type1, ItemType type2) {
        if (Type.isNumericPrimitiveType(type1)) {
            type1 = Type.NUMBER_TYPE;
        }
        if (Type.isNumericPrimitiveType(type2)) {
            type2 = Type.NUMBER_TYPE;
        }
        for (int i = 0; i < table.length; i++) {
            if (type1.equals(table[i].operand0) &&
                    type2.equals(table[i].operand1)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Determine the data type of the expression, if this is known statically
     */

    public ItemType getItemType() {
        final ItemType t1 = operand0.getItemType();
        final ItemType pt1 = t1.getPrimitiveItemType();
        final ItemType t2 = operand1.getItemType();
        final ItemType pt2 = t2.getPrimitiveItemType();
        final Signature[] table = getOperatorTable(operator);
        final int entry = getEntry(table, pt1, pt2);

        if (entry < 0) {
            return Type.ANY_ATOMIC_TYPE;  // type is not known statically
        }

        ItemType resultType = table[entry].resultType;
        if (resultType == Type.NUMBER_TYPE) {
            resultType = NumericValue.promote(pt1, pt2);
            // exception: integer div integer => decimal
            if (operator == Token.DIV && resultType == Type.INTEGER_TYPE) {
                resultType = Type.DECIMAL_TYPE;
            }
        } else if (resultType == Type.DURATION_TYPE) {
            // if one of the operands is a subtype of duration, then the result will be the same subtype
            // (this isn't captured in the table because these types are not primitive).
            if (Type.isSubType(t1, Type.DAY_TIME_DURATION_TYPE)) {
                resultType = Type.DAY_TIME_DURATION_TYPE;
            } else if (Type.isSubType(t2, Type.DAY_TIME_DURATION_TYPE)) {
                resultType = Type.DAY_TIME_DURATION_TYPE;
            } else if (Type.isSubType(t1, Type.YEAR_MONTH_DURATION_TYPE)) {
                resultType = Type.YEAR_MONTH_DURATION_TYPE;
            } else if (Type.isSubType(t2, Type.YEAR_MONTH_DURATION_TYPE)) {
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

        int action = getAction((AtomicType)v1.getItemType().getPrimitiveItemType(),
                operator,
                (AtomicType)v2.getItemType().getPrimitiveItemType());

        Expression e;
        switch (action) {
            case NUMERIC_ARITHMETIC:
                e = new NumericArithmetic(v1, operator, v2);
                break;
            case DURATION_ADDITION:
                e = new DurationAddition(v1, operator, v2);
                break;
            case DURATION_MULTIPLICATION:
                e = new DurationMultiplication(v1, operator, v2);
                break;
            case DURATION_DIVISION:
                e = new DurationDivision(v1, operator, v2);
                break;
            case DATE_AND_DURATION:
                e = new DateAndDuration(v1, operator, v2);
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
                        nv1 = (NumericValue)v1.convert(Type.DOUBLE);
                        nv2 = (NumericValue)v2.convert(Type.DOUBLE);
                    } catch (XPathException err) {
                        typeError("Unsuitable operands for arithmetic operation (" +
                                v1.getItemType() + ", " +
                                v2.getItemType() + ')', "XP0006", context);
                        return null;
                    }
                    e = new NumericArithmetic(nv1, operator, nv2);
                } else {
                    typeError("Unsuitable operands for arithmetic operation (" +
                            v1.getItemType() + ", " +
                            v2.getItemType() + ')', "XP0006", context);
                    return null;
                }
        }
        ExpressionTool.copyLocationInfo(this, e);
        if (e instanceof ComputedExpression) {
            ((ComputedExpression)e).setParentExpression(getParentExpression());
        }
        return e.evaluateItem(context);
    }

    /**
     * Inner class to handle 1.0 backwards compatible arithmetic
     */
    private class Arithmetic10 extends BinaryExpression {

        public Arithmetic10(Expression p1, int operator, Expression p2) {
            super(p1, operator, p2);
        }

        public Expression analyze(StaticContext env, ItemType contextItemType) throws XPathException {

            SequenceType atomicType = SequenceType.OPTIONAL_ATOMIC;

            RoleLocator role0 = new RoleLocator(RoleLocator.BINARY_EXPR, Token.tokens[operator], 0, null);
            operand0 = TypeChecker.staticTypeCheck(operand0, atomicType, backwardsCompatible, role0, env);

            RoleLocator role1 = new RoleLocator(RoleLocator.BINARY_EXPR, Token.tokens[operator], 1, null);
            operand1 = TypeChecker.staticTypeCheck(operand1, atomicType, backwardsCompatible, role1, env);

            Expression e = super.analyze(env, contextItemType);
            if (e instanceof ArithmeticExpression) {
                AtomicType type0 = (AtomicType)operand0.getItemType().getPrimitiveItemType();
                AtomicType type1 = (AtomicType)operand1.getItemType().getPrimitiveItemType();
                if (backwardsCompatible) {
                    if (type0 == Type.BOOLEAN_TYPE) {
                        type0 = Type.NUMBER_TYPE;
                    } else if (type0 == Type.STRING_TYPE) {
                        type0 = Type.NUMBER_TYPE;
                    }
                    if (type1 == Type.BOOLEAN_TYPE) {
                        type1 = Type.NUMBER_TYPE;
                    } else if (type1 == Type.STRING_TYPE) {
                        type1 = Type.NUMBER_TYPE;
                    }
                }
                final int action = getAction(type0, operator, type1);
                switch (action) {
                    case NUMERIC_ARITHMETIC:
                        e = new NumericArithmetic(operand0, operator, operand1);
                        break;
                    case DURATION_ADDITION:
                        e = new DurationAddition(operand0, operator, operand1);
                        break;
                    case DURATION_MULTIPLICATION:
                        e = new DurationMultiplication(operand0, operator, operand1);
                        break;
                    case DURATION_DIVISION:
                        e = new DurationDivision(operand0, operator, operand1);
                        break;
                    case DATE_AND_DURATION:
                        e = new DateAndDuration(operand0, operator, operand1);
                        break;
                    case DATE_DIFFERENCE:
                        e = new DateDifference(operand0, operator, operand1);
                        break;

                    case UNKNOWN_10:
                        e = new Arithmetic10(operand0, operator, operand1);
                        break;

                    case UNKNOWN:
                        // either the types are not known yet, or they are wrong
                        if (Type.isSubType(type0, Type.ANY_ATOMIC_TYPE) &&
                                type0 != Type.UNTYPED_ATOMIC_TYPE &&
                                type0 != Type.ANY_ATOMIC_TYPE &&
                                Type.isSubType(type1, Type.ANY_ATOMIC_TYPE) &&
                                type1 != Type.UNTYPED_ATOMIC_TYPE &&
                                type1 != Type.ANY_ATOMIC_TYPE) {
                            StaticError err = new StaticError("Unsuitable operands for arithmetic operation (" +
                                    type0.toString(env.getNamePool()) + ", " +
                                    type1.toString(env.getNamePool()) + ')');
                            err.setErrorCode("XP0006");
                            err.setIsTypeError(true);
                            throw err;
                        }
                        return e;
                }
                ExpressionTool.copyLocationInfo(this, e);
                if (e instanceof ComputedExpression) {
                    ((ComputedExpression)e).setParentExpression(getParentExpression());
                }
                try {
                    if (operand0 instanceof Value && operand1 instanceof Value) {
                        return ExpressionTool.eagerEvaluate(e, null);
                    }
                } catch (DynamicError err) {
                    // Defer any error reporting until run-time
                }
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
         */

        public ItemType getItemType() {
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
                    v1 = v1.convert(Type.DOUBLE);
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
                    v2 = v2.convert(Type.DOUBLE);
                } catch (XPathException e) {
                    return DoubleValue.NaN;
                }
            }

            int action = getAction((AtomicType)v1.getItemType().getPrimitiveItemType(),
                    operator,
                    (AtomicType)v2.getItemType().getPrimitiveItemType());

            Expression e;
            switch (action) {
                case NUMERIC_ARITHMETIC:
                    e = new NumericArithmetic(v1, operator, v2);
                    break;
                case DURATION_ADDITION:
                    e = new DurationAddition(v1, operator, v2);
                    break;
                case DURATION_MULTIPLICATION:
                    e = new DurationMultiplication(v1, operator, v2);
                    break;
                case DURATION_DIVISION:
                    e = new DurationDivision(v1, operator, v2);
                    break;
                case DATE_AND_DURATION:
                    e = new DateAndDuration(v1, operator, v2);
                    break;
                case DATE_DIFFERENCE:
                    e = new DateDifference(v1, operator, v2);
                    break;
                default:
                    typeError("Unsuitable operands for arithmetic operation (" +
                            v1.getItemType() + ", " +
                            v2.getItemType() + ')', "XP0006", context);
                    return null;
            }
            ExpressionTool.copyLocationInfo(this, e);
            if (e instanceof ComputedExpression) {
                ((ComputedExpression)e).setParentExpression(getParentExpression());
            }
            return e.evaluateItem(context);
        }
    }

    /**
     * Inner class to handle numeric arithmetic expressions
     */

    public static class NumericArithmetic extends ArithmeticExpression {

        public NumericArithmetic(Expression p1, int operator, Expression p2) {
            super(p1, operator, p2);
        }

        /**
         * Evaluate the expression.
         */

        public Item evaluateItem(XPathContext context) throws XPathException {
            // TODO: try to do more of the work at compile time
            AtomicValue v1 = ((AtomicValue)operand0.evaluateItem(context));
            if (v1 == null) {
                return null;
            }
            v1 = v1.getPrimitiveValue();
            if (v1 instanceof UntypedAtomicValue) {
                try {
                    v1 = new DoubleValue(Value.stringToNumber(v1.getStringValueCS()));
                } catch (NumberFormatException e) {
                    v1 = DoubleValue.NaN;
                }
            }
            AtomicValue v2 = ((AtomicValue)operand1.evaluateItem(context));
            if (v2 == null) {
                return null;
            }
            v2 = v2.getPrimitiveValue();
            if (v2 instanceof UntypedAtomicValue) {
                try {
                    v2 = new DoubleValue(Value.stringToNumber(v2.getStringValueCS()));
                } catch (NumberFormatException e) {
                    v2 = DoubleValue.NaN;
                }
            }

            if (operator == Token.NEGATE) {
                return ((NumericValue)v2).negate();
            } else {
                try {
                    return ((NumericValue)v1).arithmetic(operator, (NumericValue)v2, context);
                } catch (DynamicError err) {
                    err.setXPathContext(context);
                    err.setLocator(ExpressionTool.getLocator(this));
                    throw err;
                } catch (ArithmeticException err) {
                    DynamicError e = new DynamicError("Arithmetic exception: " + err.getMessage());
                    e.setXPathContext(context);
                    e.setLocator(ExpressionTool.getLocator(this));
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
                return v1.add(v2, context);
            } else if (operator == Token.MINUS) {
                return v1.subtract(v2, context);
            } else {
                throw new AssertionError("Unknown operation on durations");
            }

        }
    }

    /**
     * Inner class to handle multiplication (or division) of a duration by a number
     */

    public static class DurationMultiplication extends ArithmeticExpression {

        public DurationMultiplication(Expression p1, int operator, Expression p2) {

            // by the time we get here, we know that one of the operands is a duration,
            // but it might be either one. We make it the first.

            super(p1, operator, p2);
            if (Type.isSubType(p2.getItemType(), Type.DURATION_TYPE)) {
                operand0 = p2;
                operand1 = p1;
            }
        }

        /**
         * Evaluate the expression.
         */

        public Item evaluateItem(XPathContext context) throws XPathException {

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

            return v1.multiply(d, context);

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

            return v1.divide(v2, context);

        }
    }


    /**
     * Inner class to handle addition or subtraction of a Date (or Time, or DateTime) and a Duration
     */

    public static class DateAndDuration extends ArithmeticExpression {

//        public DateAndDuration() {}

        public DateAndDuration(Expression p1, int operator, Expression p2) {

            // by the time we get here, we know that one of the operands is a duration,
            // but it might be either one. We make it the second.

            super(p1, operator, p2);
            if (Type.isSubType(p1.getItemType(), Type.DURATION_TYPE)) {
                operand0 = p2;
                operand1 = p1;
            }

        }

        /**
         * Evaluate the expression.
         */

        public Item evaluateItem(XPathContext context) throws XPathException {

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
                v2 = v2.multiply(-1.0, context);
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
