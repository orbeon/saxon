package org.orbeon.saxon.expr;

import org.orbeon.saxon.sort.IntHashMap;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.type.AtomicType;
import org.orbeon.saxon.type.BuiltInAtomicType;
import org.orbeon.saxon.type.Type;
import org.orbeon.saxon.type.TypeHierarchy;
import org.orbeon.saxon.value.*;
import org.orbeon.saxon.om.StandardNames;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.BigInteger;

/**
 * This class evaluates arithmetic expressions; it acts as a helper class to the ArithmeticExpression
 * class. There are many subclasses for the different kinds of arithmetic expression, and static methods
 * that allow the right subclass to be selected, either at compile time or at run time.
 */
                                     
public abstract class Calculator implements Serializable {

    public static final int PLUS = 0;
    public static final int MINUS = 1;
    public static final int TIMES = 2;
    public static final int DIV = 3;
    public static final int MOD = 4;
    public static final int IDIV = 5;

    /**
     * Calculators used for the six operators when the static type information does not allow
     * a more specific calculator to be chosen
     */

    public static Calculator[] ANY_ANY = {
        new AnyPlusAny(),
        new AnyMinusAny(),
        new AnyTimesAny(),
        new AnyDivAny(),
        new AnyModAny(),
        new AnyIdivAny()
    };

    /**
     * Calculators used when the first operand is a double
     */

    public static Calculator[] DOUBLE_DOUBLE = {
        new DoublePlusDouble(),
        new DoubleMinusDouble(),
        new DoubleTimesDouble(),
        new DoubleDivDouble(),
        new DoubleModDouble(),
        new DoubleIdivDouble()
    };

    public static Calculator[] DOUBLE_FLOAT = DOUBLE_DOUBLE;
    public static Calculator[] DOUBLE_DECIMAL = DOUBLE_DOUBLE;
    public static Calculator[] DOUBLE_INTEGER = DOUBLE_DOUBLE;

    /**
     * Calculators used when the first operand is a float
     */

    public static Calculator[] FLOAT_DOUBLE = DOUBLE_DOUBLE;
    public static Calculator[] FLOAT_FLOAT = {
        new FloatPlusFloat(),
        new FloatMinusFloat(),
        new FloatTimesFloat(),
        new FloatDivFloat(),
        new FloatModFloat(),
        new FloatIdivFloat()
    };
    public static Calculator[] FLOAT_DECIMAL = FLOAT_FLOAT;
    public static Calculator[] FLOAT_INTEGER = FLOAT_FLOAT;

    /**
     * Calculators used when the first operand is a decimal
     */

    public static Calculator[] DECIMAL_DOUBLE = DOUBLE_DOUBLE;
    public static Calculator[] DECIMAL_FLOAT = FLOAT_FLOAT;
    public static Calculator[] DECIMAL_DECIMAL = {
        new DecimalPlusDecimal(),
        new DecimalMinusDecimal(),
        new DecimalTimesDecimal(),
        new DecimalDivDecimal(),
        new DecimalModDecimal(),
        new DecimalIdivDecimal()
    };
    public static Calculator[] DECIMAL_INTEGER = DECIMAL_DECIMAL;

    /**
     * Calculators used when the first operand is an integer
     */

    public static Calculator[] INTEGER_DOUBLE = DOUBLE_DOUBLE;
    public static Calculator[] INTEGER_FLOAT = FLOAT_FLOAT;
    public static Calculator[] INTEGER_DECIMAL = DECIMAL_DECIMAL;
    public static Calculator[] INTEGER_INTEGER = {
        new IntegerPlusInteger(),
        new IntegerMinusInteger(),
        new IntegerTimesInteger(),
        new IntegerDivInteger(),
        new IntegerModInteger(),
        new IntegerIdivInteger()
    };

    /**
     * Calculators used when both operands are xs:dateTime, xs:date, or xs:time
     */

    public static Calculator[] DATETIME_DATETIME = {
        null,
        new DateTimeMinusDateTime(),
        null, null, null, null
    };

    /**
     * Calculators used when the first operand is xs:dateTime, xs:date, or xs:time,
     * and the second is a duration
     */

    public static Calculator[] DATETIME_DURATION = {
        new DateTimePlusDuration(),
        new DateTimeMinusDuration(),
        null, null, null, null
    };

    /**
     * Calculators used when the second operand is xs:dateTime, xs:date, or xs:time,
     * and the first is a duration
     */

    public static Calculator[] DURATION_DATETIME = {
        new DurationPlusDateTime(),
        null, null, null, null, null
    };

    /**
     * Calculators used when the both operands are durations
     */

    public static Calculator[] DURATION_DURATION = {
        new DurationPlusDuration(),
        new DurationMinusDuration(),
        null,
        new DurationDivDuration(),
        null, null
    };

    /**
     * Calculators used when the first operand is a duration and the second is numeric
     */

    public static Calculator[] DURATION_NUMERIC = {
        null, null,
        new DurationTimesNumeric(),
        new DurationDivNumeric(),
        null, null
    };

    /**
     * Calculators used when the second operand is a duration and the first is numeric
     */

    public static Calculator[] NUMERIC_DURATION = {
        null, null,
        new NumericTimesDuration(),
        null, null, null
    };

    /**
     * Table mapping argument types to the Calculator class used to implement them
     */

    private static IntHashMap table = new IntHashMap(100);
    private static IntHashMap nameTable = new IntHashMap(100);

    private static void def(int typeA, int typeB, Calculator[] calculatorSet, String setName) {
        int key = (typeA & 0xffff)<<16 | (typeB & 0xffff);
        table.put(key, calculatorSet);
        nameTable.put(key, setName);
        // As well as the entries added directly, we also add derived entries for other types
        // considered primitive
        if (typeA == StandardNames.XS_DURATION) {
            def(StandardNames.XS_DAY_TIME_DURATION, typeB, calculatorSet, setName);
            def(StandardNames.XS_YEAR_MONTH_DURATION, typeB, calculatorSet, setName);
        }
        if (typeB == StandardNames.XS_DURATION) {
            def(typeA, StandardNames.XS_DAY_TIME_DURATION, calculatorSet, setName);
            def(typeA, StandardNames.XS_YEAR_MONTH_DURATION, calculatorSet, setName);
        }
        if (typeA == StandardNames.XS_DATE_TIME) {
            def(StandardNames.XS_DATE, typeB, calculatorSet, setName);
            def(StandardNames.XS_TIME, typeB, calculatorSet, setName);
        }
        if (typeB == StandardNames.XS_DATE_TIME) {
            def(typeA, StandardNames.XS_DATE, calculatorSet, setName);
            def(typeA, StandardNames.XS_TIME, calculatorSet, setName);
        }
        if (typeA == StandardNames.XS_DOUBLE) {
            def(StandardNames.XS_UNTYPED_ATOMIC, typeB, calculatorSet, setName);
        }
        if (typeB == StandardNames.XS_DOUBLE) {
            def(typeA, StandardNames.XS_UNTYPED_ATOMIC, calculatorSet, setName);
        }
    }

    /**
     * Factory method to get a calculator for a given combination of types
     * @param typeA fingerprint of the primitive type of the first operand
     * @param typeB fingerprint of the primitive type of the second operand
     * @param operator the arithmetic operator in use
     * @param mustResolve indicates that a concrete Calculator is required (rather than
     * an ANY_ANY calculator which needs to be further resolved at run-time)
     * @return null if no suitable Calculator can be found.
     */

    public static Calculator getCalculator(int typeA, int typeB, int operator, boolean mustResolve) {
        int key = (typeA & 0xffff)<<16 | (typeB & 0xffff);
        Calculator[] set = (Calculator[])table.get(key);
        if (set == null) {
            if (mustResolve) {
                return null;
            } else {
                return ANY_ANY[operator];
            }
        } else {
            return set[operator];
        }
    }

    /**
     * Get the name of the calculator set for a given combination of types
     * @param typeA the fingerprint of the primitive type of the first operand
     * @param typeB the fingerprint of the primitive type of the second operand
     * @return null if no suitable Calculator can be found.
     */

    public static String getCalculatorSetName(int typeA, int typeB) {
        int key = (typeA & 0xffff)<<16 | (typeB & 0xffff);
        return (String)nameTable.get(key);
    }

    static {
        def(StandardNames.XS_DOUBLE, StandardNames.XS_DOUBLE, DOUBLE_DOUBLE, "DOUBLE_DOUBLE");
        def(StandardNames.XS_DOUBLE, StandardNames.XS_FLOAT, DOUBLE_FLOAT, "DOUBLE_FLOAT");
        def(StandardNames.XS_DOUBLE, StandardNames.XS_DECIMAL, DOUBLE_DECIMAL, "DOUBLE_DECIMAL");
        def(StandardNames.XS_DOUBLE, StandardNames.XS_INTEGER, DOUBLE_INTEGER, "DOUBLE_INTEGER");
        def(StandardNames.XS_FLOAT, StandardNames.XS_DOUBLE, FLOAT_DOUBLE, "FLOAT_DOUBLE");
        def(StandardNames.XS_FLOAT, StandardNames.XS_FLOAT, FLOAT_FLOAT, "FLOAT_FLOAT");
        def(StandardNames.XS_FLOAT, StandardNames.XS_DECIMAL, FLOAT_DECIMAL, "FLOAT_DECIMAL");
        def(StandardNames.XS_FLOAT, StandardNames.XS_INTEGER, FLOAT_INTEGER, "FLOAT_INTEGER");
        def(StandardNames.XS_DECIMAL, StandardNames.XS_DOUBLE, DECIMAL_DOUBLE, "DECIMAL_DOUBLE");
        def(StandardNames.XS_DECIMAL, StandardNames.XS_FLOAT, DECIMAL_FLOAT, "DECIMAL_FLOAT");
        def(StandardNames.XS_DECIMAL, StandardNames.XS_DECIMAL, DECIMAL_DECIMAL, "DECIMAL_DECIMAL");
        def(StandardNames.XS_DECIMAL, StandardNames.XS_INTEGER, DECIMAL_INTEGER, "DECIMAL_INTEGER");
        def(StandardNames.XS_INTEGER, StandardNames.XS_DOUBLE, INTEGER_DOUBLE, "INTEGER_DOUBLE");
        def(StandardNames.XS_INTEGER, StandardNames.XS_FLOAT, INTEGER_FLOAT, "INTEGER_FLOAT");
        def(StandardNames.XS_INTEGER, StandardNames.XS_DECIMAL, INTEGER_DECIMAL, "INTEGER_DECIMAL");
        def(StandardNames.XS_INTEGER, StandardNames.XS_INTEGER, INTEGER_INTEGER, "INTEGER_INTEGER");
        def(StandardNames.XS_DATE_TIME, StandardNames.XS_DATE_TIME, DATETIME_DATETIME, "DATETIME_DATETIME");
        def(StandardNames.XS_DATE_TIME, StandardNames.XS_DURATION, DATETIME_DURATION, "DATETIME_DURATION");
        def(StandardNames.XS_DURATION, StandardNames.XS_DATE_TIME, DURATION_DATETIME, "DURATION_DATETIME");
        def(StandardNames.XS_DURATION, StandardNames.XS_DURATION, DURATION_DURATION, "DURATION_DURATION");
        def(StandardNames.XS_DURATION, StandardNames.XS_DOUBLE, DURATION_NUMERIC, "DURATION_NUMERIC");
        def(StandardNames.XS_DURATION, StandardNames.XS_FLOAT, DURATION_NUMERIC, "DURATION_NUMERIC");
        def(StandardNames.XS_DURATION, StandardNames.XS_DECIMAL, DURATION_NUMERIC, "DURATION_NUMERIC");
        def(StandardNames.XS_DURATION, StandardNames.XS_INTEGER, DURATION_NUMERIC, "DURATION_NUMERIC");
        def(StandardNames.XS_DOUBLE, StandardNames.XS_DURATION, NUMERIC_DURATION, "NUMERIC_DURATION");
        def(StandardNames.XS_FLOAT, StandardNames.XS_DURATION, NUMERIC_DURATION, "NUMERIC_DURATION");
        def(StandardNames.XS_DECIMAL, StandardNames.XS_DURATION, NUMERIC_DURATION, "NUMERIC_DURATION");
        def(StandardNames.XS_INTEGER, StandardNames.XS_DURATION, NUMERIC_DURATION, "NUMERIC_DURATION");
    }

    /**
     * Perform an arithmetic operation
     * @param a the first operand. Must not be null, and must be an instance of the type implied by the
     * class name.
     * @param b the second operand. Must not be null, and must be an instance of the type implied by the
     * class name.
     * @param c the XPath dynamic evaluation context
     * @throws XPathException in the event of an arithmetic error
     * @return the result of the computation, as a value of the correct primitive type
     */

    public abstract AtomicValue compute(AtomicValue a, AtomicValue b, XPathContext c) throws XPathException;

    /**
     * Get the type of the result of the calculator, given arguments types typeA and typeB
     * @param typeA the type of the first operand
     * @param typeB the type of the second operand
     * @return the type of the result
     */

    public abstract AtomicType getResultType(AtomicType typeA, AtomicType typeB);

    /**
     * Arithmetic: anyAtomicType + AnyAtomicType
     */

    private static class AnyPlusAny extends Calculator {
        public AtomicValue compute(AtomicValue a, AtomicValue b, XPathContext c) throws XPathException {
            TypeHierarchy th = c.getConfiguration().getTypeHierarchy();
            Calculator calc = getCalculator(
                    a.getItemType(th).getPrimitiveType(), b.getItemType(th).getPrimitiveType(), PLUS, true);
            if (calc == null) {
                throw new XPathException("Unsuitable types for + operation (" +
                        Type.displayTypeName(a) + ", " + Type.displayTypeName(b) + ")", "XPTY0004", c);
            } else {
                return calc.compute(a, b, c);
            }
        }
        public AtomicType getResultType(AtomicType typeA, AtomicType typeB) {
            return BuiltInAtomicType.ANY_ATOMIC;
        }
    }

    /**
     * Arithmetic: anyAtomicType - AnyAtomicType
     */

    private static class AnyMinusAny extends Calculator {
        public AtomicValue compute(AtomicValue a, AtomicValue b, XPathContext c) throws XPathException {
            TypeHierarchy th = c.getConfiguration().getTypeHierarchy();
            Calculator calc = getCalculator(
                    a.getItemType(th).getPrimitiveType(), b.getItemType(th).getPrimitiveType(), MINUS, true);
            if (calc == null) {
                throw new XPathException("Unsuitable types for - operation (" +
                        Type.displayTypeName(a) + ", " + Type.displayTypeName(b) + ")", "XPTY0004", c);
            } else {
                return calc.compute(a, b, c);
            }
        }
        public AtomicType getResultType(AtomicType typeA, AtomicType typeB) {
            return BuiltInAtomicType.ANY_ATOMIC;
        }
    }

    /**
     * Arithmetic: anyAtomicType * AnyAtomicType
     */

    private static class AnyTimesAny extends Calculator {
        public AtomicValue compute(AtomicValue a, AtomicValue b, XPathContext c) throws XPathException {
            TypeHierarchy th = c.getConfiguration().getTypeHierarchy();
            Calculator calc = getCalculator(
                    a.getItemType(th).getPrimitiveType(), b.getItemType(th).getPrimitiveType(), TIMES, true);
            if (calc == null) {
                throw new XPathException("Unsuitable types for * operation (" +
                        Type.displayTypeName(a) + ", " + Type.displayTypeName(b) + ")", "XPTY0004", c);
            } else {
                return calc.compute(a, b, c);
            }
        }
        public AtomicType getResultType(AtomicType typeA, AtomicType typeB) {
            return BuiltInAtomicType.ANY_ATOMIC;
        }
    }

    /**
     * Arithmetic: anyAtomicType div AnyAtomicType
     */

    private static class AnyDivAny extends Calculator {
        public AtomicValue compute(AtomicValue a, AtomicValue b, XPathContext c) throws XPathException {
            TypeHierarchy th = c.getConfiguration().getTypeHierarchy();
            Calculator calc = getCalculator(
                    a.getItemType(th).getPrimitiveType(), b.getItemType(th).getPrimitiveType(), DIV, true);
            if (calc == null) {
                throw new XPathException("Unsuitable types for div operation (" +
                        Type.displayTypeName(a) + ", " + Type.displayTypeName(b) + ")", "XPTY0004", c);
            } else {
                return calc.compute(a, b, c);
            }
        }
        public AtomicType getResultType(AtomicType typeA, AtomicType typeB) {
            return BuiltInAtomicType.ANY_ATOMIC;
        }
    }

    /**
     * Arithmetic: anyAtomicType mod AnyAtomicType
     */

    private static class AnyModAny extends Calculator {
        public AtomicValue compute(AtomicValue a, AtomicValue b, XPathContext c) throws XPathException {
            TypeHierarchy th = c.getConfiguration().getTypeHierarchy();
            Calculator calc = getCalculator(
                    a.getItemType(th).getPrimitiveType(), b.getItemType(th).getPrimitiveType(), MOD, true);
            if (calc == null) {
                throw new XPathException("Unsuitable types for mod operation (" +
                        Type.displayTypeName(a) + ", " + Type.displayTypeName(b) + ")", "XPTY0004", c);
            } else {
                return calc.compute(a, b, c);
            }
        }
        public AtomicType getResultType(AtomicType typeA, AtomicType typeB) {
            return BuiltInAtomicType.ANY_ATOMIC;
        }
    }

    /**
     * Arithmetic: anyAtomicType idiv AnyAtomicType
     */

    private static class AnyIdivAny extends Calculator {
        public AtomicValue compute(AtomicValue a, AtomicValue b, XPathContext c) throws XPathException {
            TypeHierarchy th = c.getConfiguration().getTypeHierarchy();
            Calculator calc = getCalculator(
                    a.getItemType(th).getPrimitiveType(), b.getItemType(th).getPrimitiveType(), IDIV, true);
            if (calc == null) {
                throw new XPathException("Unsuitable types for idiv operation (" +
                        Type.displayTypeName(a) + ", " + Type.displayTypeName(b) + ")", "XPTY0004", c);
            } else {
                return calc.compute(a, b, c);
            }
        }
        public AtomicType getResultType(AtomicType typeA, AtomicType typeB) {
            return BuiltInAtomicType.ANY_ATOMIC;
        }
    }
    
    /**
     * Arithmetic: double + double (including types that promote to double)
     */

    private static class DoublePlusDouble extends Calculator {
        public AtomicValue compute(AtomicValue a, AtomicValue b, XPathContext c) throws XPathException {
            return new DoubleValue(((NumericValue)a).getDoubleValue() + ((NumericValue)b).getDoubleValue());
        }
        public AtomicType getResultType(AtomicType typeA, AtomicType typeB) {
            return BuiltInAtomicType.DOUBLE;
        }
    }

    /**
     * Arithmetic: double - double (including types that promote to double)
     */

    private static class DoubleMinusDouble extends Calculator {
        public AtomicValue compute(AtomicValue a, AtomicValue b, XPathContext c) throws XPathException {
            return new DoubleValue(((NumericValue)a).getDoubleValue() - ((NumericValue)b).getDoubleValue());
        }
        public AtomicType getResultType(AtomicType typeA, AtomicType typeB) {
            return BuiltInAtomicType.DOUBLE;
        }
    }

    /**
     * Arithmetic: double * double (including types that promote to double)
     */

    private static class DoubleTimesDouble extends Calculator {
        public AtomicValue compute(AtomicValue a, AtomicValue b, XPathContext c) throws XPathException {
            return new DoubleValue(((NumericValue)a).getDoubleValue() * ((NumericValue)b).getDoubleValue());
        }
        public AtomicType getResultType(AtomicType typeA, AtomicType typeB) {
            return BuiltInAtomicType.DOUBLE;
        }
    }

    /**
     * Arithmetic: double div double (including types that promote to double)
     */

    private static class DoubleDivDouble extends Calculator {
        public AtomicValue compute(AtomicValue a, AtomicValue b, XPathContext c) throws XPathException {
            return new DoubleValue(((NumericValue)a).getDoubleValue() / ((NumericValue)b).getDoubleValue());
        }
        public AtomicType getResultType(AtomicType typeA, AtomicType typeB) {
            return BuiltInAtomicType.DOUBLE;
        }
    }

    /**
     * Arithmetic: double mod double (including types that promote to double)
     */

    private static class DoubleModDouble extends Calculator {
        public AtomicValue compute(AtomicValue a, AtomicValue b, XPathContext c) throws XPathException {
            return new DoubleValue(((NumericValue)a).getDoubleValue() % ((NumericValue)b).getDoubleValue());
        }
        public AtomicType getResultType(AtomicType typeA, AtomicType typeB) {
            return BuiltInAtomicType.DOUBLE;
        }
    }

    /**
     * Arithmetic: double idiv double (including types that promote to double)
     */

    private static class DoubleIdivDouble extends Calculator {
        public AtomicValue compute(AtomicValue a, AtomicValue b, XPathContext c) throws XPathException {
            double A = ((NumericValue)a).getDoubleValue();
            double B = ((NumericValue)b).getDoubleValue();
            if (B == 0.0) {
                throw new XPathException("Integer division by zero", "FOAR0001", c);
            }
            if (Double.isNaN(A) || Double.isInfinite(A)) {
                throw new XPathException("First operand of idiv is NaN or infinity", "FOAR0002", c);
            }
            if (Double.isNaN(B)) {
                throw new XPathException("Second operand of idiv is NaN", "FOAR0002", c);
            }
            return new DoubleValue(A / B).convert(BuiltInAtomicType.INTEGER, true, c).asAtomic();
        }
        public AtomicType getResultType(AtomicType typeA, AtomicType typeB) {
            return BuiltInAtomicType.INTEGER;
        }
    }

    /**
     * Arithmetic: float + float (including types that promote to float)
     */

    private static class FloatPlusFloat extends Calculator {
        public AtomicValue compute(AtomicValue a, AtomicValue b, XPathContext c) throws XPathException {
            return new FloatValue(((NumericValue)a).getFloatValue() + ((NumericValue)b).getFloatValue());
        }
        public AtomicType getResultType(AtomicType typeA, AtomicType typeB) {
            return BuiltInAtomicType.FLOAT;
        }
    }

    /**
     * Arithmetic: float - float (including types that promote to float)
     */

    private static class FloatMinusFloat extends Calculator {
        public AtomicValue compute(AtomicValue a, AtomicValue b, XPathContext c) throws XPathException {
            return new FloatValue(((NumericValue)a).getFloatValue() - ((NumericValue)b).getFloatValue());
        }
        public AtomicType getResultType(AtomicType typeA, AtomicType typeB) {
            return BuiltInAtomicType.FLOAT;
        }
    }

    /**
     * Arithmetic: float * float (including types that promote to float)
     */

    private static class FloatTimesFloat extends Calculator {
        public AtomicValue compute(AtomicValue a, AtomicValue b, XPathContext c) throws XPathException {
            return new FloatValue(((NumericValue)a).getFloatValue() * ((NumericValue)b).getFloatValue());
        }
        public AtomicType getResultType(AtomicType typeA, AtomicType typeB) {
            return BuiltInAtomicType.FLOAT;
        }
    }

    /**
     * Arithmetic: float div float (including types that promote to float)
     */

    private static class FloatDivFloat extends Calculator {
        public AtomicValue compute(AtomicValue a, AtomicValue b, XPathContext c) throws XPathException {
            return new FloatValue(((NumericValue)a).getFloatValue() / ((NumericValue)b).getFloatValue());
        }
        public AtomicType getResultType(AtomicType typeA, AtomicType typeB) {
            return BuiltInAtomicType.FLOAT;
        }
    }

    /**
     * Arithmetic: float mod float (including types that promote to float)
     */

    private static class FloatModFloat extends Calculator {
        public AtomicValue compute(AtomicValue a, AtomicValue b, XPathContext c) throws XPathException {
            return new FloatValue(((NumericValue)a).getFloatValue() % ((NumericValue)b).getFloatValue());
        }
        public AtomicType getResultType(AtomicType typeA, AtomicType typeB) {
            return BuiltInAtomicType.FLOAT;
        }
    }

    /**
     * Arithmetic: float idiv float (including types that promote to float)
     */

    private static class FloatIdivFloat extends Calculator {
        public AtomicValue compute(AtomicValue a, AtomicValue b, XPathContext c) throws XPathException {
            float A = ((NumericValue)a).getFloatValue();
            float B = ((NumericValue)b).getFloatValue();
            if (B == 0.0) {
                throw new XPathException("Integer division by zero", "FOAR0001", c);
            }
            if (Double.isNaN(A) || Double.isInfinite(A)) {
                throw new XPathException("First operand of idiv is NaN or infinity", "FOAR0002", c);
            }
            if (Double.isNaN(B)) {
                throw new XPathException("Second operand of idiv is NaN", "FOAR0002", c);
            }
            return new FloatValue(A / B).convert(BuiltInAtomicType.INTEGER, true, c).asAtomic();
        }
        public AtomicType getResultType(AtomicType typeA, AtomicType typeB) {
            return BuiltInAtomicType.INTEGER;
        }
    }

    /**
     * Arithmetic: decimal + decimal (including types that promote to decimal, that is, integer)
     */

    private static class DecimalPlusDecimal extends Calculator {
        public AtomicValue compute(AtomicValue a, AtomicValue b, XPathContext c) throws XPathException {
            if (a instanceof IntegerValue && b instanceof IntegerValue) {
                return ((IntegerValue)a).plus((IntegerValue)b);
            } else {
                return new DecimalValue(
                    ((NumericValue)a).getDecimalValue().add(((NumericValue)b).getDecimalValue()));
            }
        }
        public AtomicType getResultType(AtomicType typeA, AtomicType typeB) {
            return BuiltInAtomicType.DECIMAL;
        }
    }

    /**
     * Arithmetic: decimal - decimal (including types that promote to decimal, that is, integer)
     */

    private static class DecimalMinusDecimal extends Calculator {
        public AtomicValue compute(AtomicValue a, AtomicValue b, XPathContext c) throws XPathException {
            if (a instanceof IntegerValue && b instanceof IntegerValue) {
                return ((IntegerValue)a).minus((IntegerValue)b);
            } else {
                return new DecimalValue(
                    ((NumericValue)a).getDecimalValue().subtract(((NumericValue)b).getDecimalValue()));
            }
        }
        public AtomicType getResultType(AtomicType typeA, AtomicType typeB) {
            return BuiltInAtomicType.DECIMAL;
        }
    }

    /**
     * Arithmetic: decimal * decimal (including types that promote to decimal, that is, integer)
     */

    private static class DecimalTimesDecimal extends Calculator {
        public AtomicValue compute(AtomicValue a, AtomicValue b, XPathContext c) throws XPathException {
            if (a instanceof IntegerValue && b instanceof IntegerValue) {
                return ((IntegerValue)a).times((IntegerValue)b);
            } else {
                return new DecimalValue(
                    ((NumericValue)a).getDecimalValue().multiply(((NumericValue)b).getDecimalValue()));
            }
        }
        public AtomicType getResultType(AtomicType typeA, AtomicType typeB) {
            return BuiltInAtomicType.DECIMAL;
        }
    }

    /**
     * Arithmetic: decimal div decimal (including types that promote to decimal, that is, integer)
     */

    private static class DecimalDivDecimal extends Calculator {
        public AtomicValue compute(AtomicValue a, AtomicValue b, XPathContext c) throws XPathException {
            final BigDecimal A = ((NumericValue)a).getDecimalValue();
            final BigDecimal B = ((NumericValue)b).getDecimalValue();
            int scale = Math.max(DecimalValue.DIVIDE_PRECISION,
                            Math.max(A.scale(), B.scale()));
            try {
                BigDecimal result = A.divide(B, scale, BigDecimal.ROUND_HALF_DOWN);
                return new DecimalValue(result);
            } catch (ArithmeticException err) {
                if (((NumericValue)b).compareTo(0) == 0) {
                    throw new XPathException("Decimal divide by zero", "FOAR0001", c);
                } else {
                    throw err;
                }
            }
        }
        public AtomicType getResultType(AtomicType typeA, AtomicType typeB) {
            return BuiltInAtomicType.DECIMAL;
        }
    }

    /**
     * Arithmetic: decimal mod decimal (including types that promote to decimal, that is, integer)
     */

    private static class DecimalModDecimal extends Calculator {
        public AtomicValue compute(AtomicValue a, AtomicValue b, XPathContext c) throws XPathException {
            if (a instanceof IntegerValue && b instanceof IntegerValue) {
                return ((IntegerValue)a).mod((IntegerValue)b);
            }

            final BigDecimal A = ((NumericValue)a).getDecimalValue();
            final BigDecimal B = ((NumericValue)b).getDecimalValue();
            try {
                BigDecimal quotient = A.divide(B, 0, BigDecimal.ROUND_DOWN);
                BigDecimal remainder = A.subtract(quotient.multiply(B));
                return new DecimalValue(remainder);
            } catch (ArithmeticException err) {
                if (((NumericValue)b).compareTo(0) == 0) {
                    throw new XPathException("Decimal modulo zero", "FOAR0001", c);
                } else {
                    throw err;
                }
            }
        }
        public AtomicType getResultType(AtomicType typeA, AtomicType typeB) {
            return BuiltInAtomicType.DECIMAL;
        }
    }

    /**
     * Arithmetic: decimal idiv decimal (including types that promote to decimal, that is, integer)
     */

    private static class DecimalIdivDecimal extends Calculator {
        public AtomicValue compute(AtomicValue a, AtomicValue b, XPathContext c) throws XPathException {
            if (a instanceof IntegerValue && b instanceof IntegerValue) {
                return ((IntegerValue)a).idiv((IntegerValue)b);
            } 

            final BigDecimal A = ((NumericValue)a).getDecimalValue();
            final BigDecimal B = ((NumericValue)b).getDecimalValue();
            if (B.signum() == 0) {
                throw new XPathException("Integer division by zero", "FOAR0001", c);
            }
            BigInteger quot = A.divide(B, 0, BigDecimal.ROUND_DOWN).toBigInteger();
            return BigIntegerValue.makeIntegerValue(quot);
        }
        public AtomicType getResultType(AtomicType typeA, AtomicType typeB) {
            return BuiltInAtomicType.INTEGER;
        }
    }

    /**
     * Arithmetic: integer + integer
     */

    private static class IntegerPlusInteger extends Calculator {
        public AtomicValue compute(AtomicValue a, AtomicValue b, XPathContext c) throws XPathException {
            return ((IntegerValue)a).plus((IntegerValue)b);
        }
        public AtomicType getResultType(AtomicType typeA, AtomicType typeB) {
            return BuiltInAtomicType.INTEGER;
        }
    }

    /**
     * Arithmetic: integer - integer
     */

    private static class IntegerMinusInteger extends Calculator {
        public AtomicValue compute(AtomicValue a, AtomicValue b, XPathContext c) throws XPathException {
            return ((IntegerValue)a).minus((IntegerValue)b);
        }
        public AtomicType getResultType(AtomicType typeA, AtomicType typeB) {
            return BuiltInAtomicType.INTEGER;
        }
    }

    /**
     * Arithmetic: integer * integer
     */

    private static class IntegerTimesInteger extends Calculator {
        public AtomicValue compute(AtomicValue a, AtomicValue b, XPathContext c) throws XPathException {
            return ((IntegerValue)a).times((IntegerValue)b);
        }
        public AtomicType getResultType(AtomicType typeA, AtomicType typeB) {
            return BuiltInAtomicType.INTEGER;
        }
    }

    /**
     * Arithmetic: integer div integer
     */

    private static class IntegerDivInteger extends Calculator {
        public AtomicValue compute(AtomicValue a, AtomicValue b, XPathContext c) throws XPathException {
            return ((IntegerValue)a).div((IntegerValue)b);
        }
        public AtomicType getResultType(AtomicType typeA, AtomicType typeB) {
            return BuiltInAtomicType.DECIMAL;
        }
    }

    /**
     * Arithmetic: integer mod integer
     */

    private static class IntegerModInteger extends Calculator {
        public AtomicValue compute(AtomicValue a, AtomicValue b, XPathContext c) throws XPathException {
            return ((IntegerValue)a).mod((IntegerValue)b);
        }
        public AtomicType getResultType(AtomicType typeA, AtomicType typeB) {
            return BuiltInAtomicType.INTEGER;
        }
    }

    /**
     * Arithmetic: integer idiv integer
     */

    private static class IntegerIdivInteger extends Calculator {
        public AtomicValue compute(AtomicValue a, AtomicValue b, XPathContext c) throws XPathException {
            return ((IntegerValue)a).idiv((IntegerValue)b);
        }
        public AtomicType getResultType(AtomicType typeA, AtomicType typeB) {
            return BuiltInAtomicType.INTEGER;
        }
    }

    /**
     * Arithmetic: date/time/dateTime - date/time/dateTime
     */

    private static class DateTimeMinusDateTime extends Calculator {
        public AtomicValue compute(AtomicValue a, AtomicValue b, XPathContext c) throws XPathException {
            return ((CalendarValue)a).subtract((CalendarValue)b, c);
        }
        public AtomicType getResultType(AtomicType typeA, AtomicType typeB) {
            return BuiltInAtomicType.DAY_TIME_DURATION;
        }
    }

    /**
     * Arithmetic: date/time/dateTime + duration
     */

    private static class DateTimePlusDuration extends Calculator {
        public AtomicValue compute(AtomicValue a, AtomicValue b, XPathContext c) throws XPathException {
            return ((CalendarValue)a).add((DurationValue)b);
        }
        public AtomicType getResultType(AtomicType typeA, AtomicType typeB) {
            return typeA;
        }
    }

    /**
     * Arithmetic: date/time/dateTime - duration
     */

    private static class DateTimeMinusDuration extends Calculator {
        public AtomicValue compute(AtomicValue a, AtomicValue b, XPathContext c) throws XPathException {
            return ((CalendarValue)a).add(((DurationValue)b).multiply(-1.0));
        }
        public AtomicType getResultType(AtomicType typeA, AtomicType typeB) {
            return typeA;
        }
    }

    /**
     * Arithmetic: duration + date/time/dateTime
     */

    private static class DurationPlusDateTime extends Calculator {
        public AtomicValue compute(AtomicValue a, AtomicValue b, XPathContext c) throws XPathException {
            return ((CalendarValue)b).add((DurationValue)a);
        }
        public AtomicType getResultType(AtomicType typeA, AtomicType typeB) {
            return typeB;
        }
    }

    /**
     * Arithmetic: duration + duration
     */

    private static class DurationPlusDuration extends Calculator {
        public AtomicValue compute(AtomicValue a, AtomicValue b, XPathContext c) throws XPathException {
            return ((DurationValue)a).add((DurationValue)b);
        }
        public AtomicType getResultType(AtomicType typeA, AtomicType typeB) {
            return typeA;
        }
    }

    /**
     * Arithmetic: duration - duration
     */

    private static class DurationMinusDuration extends Calculator {
        public AtomicValue compute(AtomicValue a, AtomicValue b, XPathContext c) throws XPathException {
            return ((DurationValue)a).subtract((DurationValue)b);
        }
        public AtomicType getResultType(AtomicType typeA, AtomicType typeB) {
            return typeA;
        }
    }

    /**
     * Arithmetic: duration div duration
     */

    private static class DurationDivDuration extends Calculator {
        public AtomicValue compute(AtomicValue a, AtomicValue b, XPathContext c) throws XPathException {
            return ((DurationValue)a).divide((DurationValue)b);
        }
        public AtomicType getResultType(AtomicType typeA, AtomicType typeB) {
            return BuiltInAtomicType.DECIMAL;
        }
    }

    /**
     * Arithmetic: duration * number
     */

    private static class DurationTimesNumeric extends Calculator {
        public AtomicValue compute(AtomicValue a, AtomicValue b, XPathContext c) throws XPathException {
            return ((DurationValue)a).multiply(((NumericValue)b).getDoubleValue());
        }
        public AtomicType getResultType(AtomicType typeA, AtomicType typeB) {
            return typeA;
        }
    }

    /**
     * Arithmetic: number * duration
     */

    private static class NumericTimesDuration extends Calculator {
        public AtomicValue compute(AtomicValue a, AtomicValue b, XPathContext c) throws XPathException {
            return ((DurationValue)b).multiply(((NumericValue)a).getDoubleValue());
        }
        public AtomicType getResultType(AtomicType typeA, AtomicType typeB) {
            return typeB;
        }
    }

    /**
     * Arithmetic: duration div number
     */

    private static class DurationDivNumeric extends Calculator {
        public AtomicValue compute(AtomicValue a, AtomicValue b, XPathContext c) throws XPathException {
            double d = 1.0 / ((NumericValue)b).getDoubleValue();
            return ((DurationValue)a).multiply(d);
        }
        public AtomicType getResultType(AtomicType typeA, AtomicType typeB) {
            return typeA;
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
// The Original Code is: all this file
//
// The Initial Developer of the Original Code is Michael H. Kay.
//
// Contributor(s):
//

