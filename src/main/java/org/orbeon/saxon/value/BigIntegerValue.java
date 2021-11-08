package org.orbeon.saxon.value;
import org.orbeon.saxon.expr.XPathContext;
import org.orbeon.saxon.expr.Calculator;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.type.*;
import org.orbeon.saxon.om.StandardNames;

import java.math.BigDecimal;
import java.math.BigInteger;

/**
 * An integer value: note this is a subtype of decimal in XML Schema, not a primitive type.
 * The abstract class IntegerValue is used to represent any xs:integer value; this implementation
 * is used for values that do not fit comfortably in a Java long; including the built-in subtype xs:unsignedLong
 */

public final class BigIntegerValue extends IntegerValue {

    private BigInteger value;

    private static final BigInteger MAX_INT = BigInteger.valueOf(Integer.MAX_VALUE);
    private static final BigInteger MIN_INT = BigInteger.valueOf(Integer.MIN_VALUE);
    public static final BigInteger MAX_LONG = BigInteger.valueOf(Long.MAX_VALUE);
    public static final BigInteger MIN_LONG = BigInteger.valueOf(Long.MIN_VALUE);
    public static final BigInteger MAX_UNSIGNED_LONG = new BigInteger("18446744073709551615");
    public static final BigIntegerValue ZERO = new BigIntegerValue(BigInteger.ZERO);

    /**
     * Construct an xs:integer value from a Java BigInteger
     * @param value the supplied BigInteger
     */

    public BigIntegerValue(BigInteger value) {
        this.value = value;
        typeLabel = BuiltInAtomicType.INTEGER;
    }

    /**
     * Construct an xs:integer value from a Java BigInteger, supplying a type label.
     * It is the caller's responsibility to ensure that the supplied value conforms
     * with the rules for the specified type.
     * @param value the value of the integer
     * @param typeLabel the type, which must represent a type derived from xs:integer
     */

    public BigIntegerValue(BigInteger value, AtomicType typeLabel) {
        this.value = value;
        this.typeLabel = typeLabel;
    }

    /**
     * Construct an xs:integer value from a Java long. Note: normally, if the value fits in a long,
     * then an Int64Value should be used. This constructor is largely for internal use, when operations
     * are required that require two integers to use the same implementation class to be used.
     * @param value the supplied Java long
     */

    public BigIntegerValue(long value) {
        this.value = BigInteger.valueOf(value);
        typeLabel = BuiltInAtomicType.INTEGER;
    }

    /**
     * Create a copy of this atomic value, with a different type label
     *
     * @param typeLabel the type label of the new copy. The caller is responsible for checking that
     *                  the value actually conforms to this type.
     */

    public AtomicValue copyAsSubType(AtomicType typeLabel) {
        BigIntegerValue v = new BigIntegerValue(value);
        v.typeLabel = typeLabel;
        return v;
    }

    /**
     * This class allows subtypes of xs:integer to be held, as well as xs:integer values.
     * This method sets the required type label. Note that this method modifies the value in situ.
     * @param type the subtype of integer required
     * @return null if the operation succeeds, or a ValidationException if the value is out of range
     */

    public ValidationFailure convertToSubType(BuiltInAtomicType type, boolean validate) {
        if (!validate) {
            typeLabel = type;
            return null;
        }
        if (IntegerValue.checkBigRange(value, type)) {
            typeLabel = type;
            return null;
        } else {
            ValidationFailure err = new ValidationFailure(
                    "Integer value is out of range for subtype " + type.getDisplayName());
            err.setErrorCode("FORG0001");
            return err;
        }
    }


    /**
     * This class allows subtypes of xs:integer to be held, as well as xs:integer values.
     * This method checks that the value is valid against the rules for a given subtype.
     *
     * @param type the subtype of integer required
     * @return null if the operation succeeds, or a ValidationException if the value is out of range
     */

    public ValidationFailure validateAgainstSubType(BuiltInAtomicType type) {
        if (IntegerValue.checkBigRange(value, type)) {
            typeLabel = type;
            return null;
        } else {
            ValidationFailure err = new ValidationFailure(
                    "Integer value is out of range for subtype " + type.getDisplayName());
            err.setErrorCode("FORG0001");
            return err;
        }
    }

    /**
     * Get the hashCode. This must conform to the rules for other NumericValue hashcodes
     * @see NumericValue#hashCode
     */

    public int hashCode() {
        if (value.compareTo(MIN_INT) >= 0 && value.compareTo(MAX_INT) <= 0) {
            return value.intValue();
        } else {
            return new Double(getDoubleValue()).hashCode();
        }
    }

    /**
     * Get the value as a long
     *
     * @return the value of the xs:integer, as a Java long
     */

    public long longValue() {
        return value.longValue();
    }

    /**
     * Get the value as a BigInteger
     * @return the value of the xs:integer as a Java BigInteger
     */

    public BigInteger asBigInteger() {
        return value;
    }

    /**
     * Test whether the value is within the range that can be held in a 64-bit signed integer
     * @return true if the value is within range for a long
     */

    public boolean isWithinLongRange() {
        return value.compareTo(MIN_LONG) >= 0 && value.compareTo(MAX_LONG) <= 0;
    }

    /**
     * Convert the value to a BigDecimal
     * @return the resulting BigDecimal
     */

    public BigDecimal asDecimal() {
        return new BigDecimal(value);
    }

    /**
     * Return the effective boolean value of this integer
     *
     * @return false if the integer is zero, otherwise true
     */
    public boolean effectiveBooleanValue() {
        return value.compareTo(BigInteger.ZERO) != 0;
    }

    /**
     * Compare the value to another numeric value
     *
     * @param other the numeric value to be compared to this value
     * @return -1 if this value is less than the other, 0 if they are equal,
     *     +1 if this value is greater
     */

    public int compareTo(Object other) {
        if (other instanceof BigIntegerValue) {
            return value.compareTo(((BigIntegerValue)other).value);
        } else if (other instanceof DecimalValue) {
            return asDecimal().compareTo(((DecimalValue)other).getDecimalValue());
        } else {
            return super.compareTo(other);
        }
    }

    /**
     * Compare the value to a long
     * @param other the value to be compared with
     * @return -1 if this is less, 0 if this is equal, +1 if this is greater or if this is NaN
     */

    public int compareTo(long other) {
        if (other == 0) {
            return value.signum();
        }
        return value.compareTo((BigInteger.valueOf(other)));
    }


    /**
     * Convert to target data type
     *
     * @param requiredType identifies the required atomic type
     * @param context the XPath dynamic evaluation context
     * @return an AtomicValue, a value of the required type; or an ErrorValue
     */

    public ConversionResult convertPrimitive(BuiltInAtomicType requiredType, boolean validate, XPathContext context) {
        switch (requiredType.getFingerprint()) {
            case StandardNames.XS_BOOLEAN:
                return BooleanValue.get(effectiveBooleanValue());

            case StandardNames.XS_NUMERIC:
            case StandardNames.XS_INTEGER:
            case StandardNames.XS_ANY_ATOMIC_TYPE:
                return this;

            case StandardNames.XS_NON_POSITIVE_INTEGER:
            case StandardNames.XS_NEGATIVE_INTEGER:
            case StandardNames.XS_LONG:
            case StandardNames.XS_INT:
            case StandardNames.XS_SHORT:
            case StandardNames.XS_BYTE:
            case StandardNames.XS_NON_NEGATIVE_INTEGER:
            case StandardNames.XS_POSITIVE_INTEGER:
            case StandardNames.XS_UNSIGNED_INT:
            case StandardNames.XS_UNSIGNED_SHORT:
            case StandardNames.XS_UNSIGNED_BYTE:
                if (isWithinLongRange()) {
                    Int64Value val = Int64Value.makeIntegerValue(longValue());
                    ValidationFailure err = val.convertToSubType(requiredType, validate);
                    if (err == null) {
                        return val;
                    } else {
                        return err;
                    }
                } else {
                    BigIntegerValue val = new BigIntegerValue(value);
                    ValidationFailure err = val.convertToSubType(requiredType, validate);
                    if (err == null) {
                        return val;
                    } else {
                        return err;
                    }
                }

            case StandardNames.XS_UNSIGNED_LONG:
                if (value.signum() < 0 || value.bitLength() > 64) {
                    ValidationFailure err = new ValidationFailure("Integer value is out of range for type " +
                            requiredType.getDisplayName());
                    err.setErrorCode("FORG0001");
                    return err;
                } else if (isWithinLongRange()) {
                    Int64Value val = Int64Value.makeIntegerValue(longValue());
                    ValidationFailure err = val.convertToSubType(requiredType, validate);
                    if (err != null) {
                        return err;
                    }
                    return val;
                } else {
                    BigIntegerValue nv = new BigIntegerValue(value);
                    ValidationFailure err = nv.convertToSubType(requiredType, validate);
                    if (err != null) {
                        return err;
                    }
                    return nv;
                }

            case StandardNames.XS_DOUBLE:
                return new DoubleValue(value.doubleValue());

            case StandardNames.XS_FLOAT:
                return new FloatValue(value.floatValue());

            case StandardNames.XS_DECIMAL:
                return new DecimalValue(new BigDecimal(value));

            case StandardNames.XS_STRING:
                return new StringValue(getStringValueCS());

            case StandardNames.XS_UNTYPED_ATOMIC:
                return new UntypedAtomicValue(getStringValueCS());

            default:
                ValidationFailure err = new ValidationFailure("Cannot convert integer to " +
                                         requiredType.getDisplayName());
                err.setErrorCode("XPTY0004");
                return err;
        }
    }

    /**
     * Get the value as a String
     * @return a String representation of the value
     */

    public String getStringValue() {
        return value.toString();
    }

    /**
     * Get the numeric value as a double
     *
     * @return A double representing this numeric value; NaN if it cannot be
     *         converted
     */
    public double getDoubleValue() {
        return value.doubleValue();
    }

    /**
     * Get the numeric value converted to a decimal
     *
     * @return a decimal representing this numeric value;
     */

    public BigDecimal getDecimalValue() {
        return new BigDecimal(value);
    }

    /**
     * Negate the value
     * @return the result of inverting the sign of the value
     */

    public NumericValue negate() {
        return new BigIntegerValue(value.negate());
    }

    /**
     * Implement the XPath floor() function
     * @return the integer value, unchanged
     */

    public NumericValue floor() {
        return this;
    }

    /**
     * Implement the XPath ceiling() function
     * @return the integer value, unchanged
     */

    public NumericValue ceiling() {
        return this;
    }

    /**
     * Implement the XPath round() function
     * @return the integer value, unchanged
     */

    public NumericValue round() {
        return this;
    }

    /**
     * Implement the XPath round-to-half-even() function
     *
     * @param scale number of digits required after the decimal point; the
     *     value -2 (for example) means round to a multiple of 100
     * @return if the scale is >=0, return this value unchanged. Otherwise
     *     round it to a multiple of 10**-scale
     */

    public NumericValue roundHalfToEven(int scale) {
        if (scale >= 0) {
            return this;
        } else {
            BigInteger factor = BigInteger.valueOf(10).pow(-scale);
            BigInteger[] pair = value.divideAndRemainder(factor);
            int up = pair[1].compareTo(factor.divide(BigInteger.valueOf(2)));
            if (up > 0) {
                // remainder is > .5
                pair[0] = pair[0].add(BigInteger.valueOf(1));
            } else if (up == 0) {
                // remainder == .5
                if (pair[0].mod(BigInteger.valueOf(2)).signum() != 0) {
                    // last digit of quotient is odd: make it even
                    pair[0] = pair[0].add(BigInteger.valueOf(1));
                }
            }
            return makeIntegerValue(pair[0].multiply(factor));
        }
    }

    /**
     * Determine whether the value is negative, zero, or positive
     * @return -1 if negative, 0 if zero, +1 if positive, NaN if NaN
     */

    public double signum() {
        return value.signum();
    }

    /**
     * Determine whether the value is a whole number, that is, whether it compares
     * equal to some integer
     *
     * @return always true for this implementation
     */

    public boolean isWholeNumber() {
        return true;
    }

    /**
     * Add another integer
     */

    public IntegerValue plus(IntegerValue other) {
        if (other instanceof BigIntegerValue) {
            return makeIntegerValue(value.add(((BigIntegerValue)other).value));
        } else {
            //noinspection RedundantCast
            return makeIntegerValue(value.add(BigInteger.valueOf(((Int64Value)other).longValue())));
        }
    }

    /**
     * Subtract another integer
     */

    public IntegerValue minus(IntegerValue other) {
        if (other instanceof BigIntegerValue) {
            return makeIntegerValue(value.subtract(((BigIntegerValue)other).value));
        } else {
            //noinspection RedundantCast
            return makeIntegerValue(value.subtract(BigInteger.valueOf(((Int64Value)other).longValue())));
        }
    }

    /**
     * Multiply by another integer
     */

    public IntegerValue times(IntegerValue other) {
        if (other instanceof BigIntegerValue) {
            return makeIntegerValue(value.multiply(((BigIntegerValue)other).value));
        } else {
            //noinspection RedundantCast
            return makeIntegerValue(value.multiply(BigInteger.valueOf(((Int64Value)other).longValue())));
        }
    }

    /**
     * Divide by another integer
     *
     * @throws org.orbeon.saxon.trans.XPathException
     *          if the other integer is zero
     */

    public NumericValue div(IntegerValue other) throws XPathException {
        BigInteger oi;
        if (other instanceof BigIntegerValue) {
            oi = ((BigIntegerValue)other).value;
        } else {
            oi = BigInteger.valueOf(other.longValue());
        }
        DecimalValue a = new DecimalValue(new BigDecimal(value));
        DecimalValue b = new DecimalValue(new BigDecimal(oi));
        return (NumericValue)Calculator.DECIMAL_DECIMAL[Calculator.DIV].compute(a, b, null);
    }

    /**
     * Take modulo another integer
     *
     * @throws org.orbeon.saxon.trans.XPathException
     *          if the other integer is zero
     */

    public IntegerValue mod(IntegerValue other) throws XPathException {
        try {
            if (other instanceof BigIntegerValue) {
                return makeIntegerValue(value.remainder(((BigIntegerValue)other).value));
            } else {
                return makeIntegerValue(value.remainder(BigInteger.valueOf(other.longValue())));
            }
        } catch (ArithmeticException err) {
            XPathException e;
            if (BigInteger.valueOf(other.longValue()).signum() == 0) {
                e = new XPathException("Integer modulo zero", "FOAR0001");
            } else {
                e = new XPathException("Integer mod operation failure", err);
            }
            throw e;
        }
    }

    /**
     * Integer divide by another integer
     *
     * @throws org.orbeon.saxon.trans.XPathException
     *          if the other integer is zero
     */

    public IntegerValue idiv(IntegerValue other) throws XPathException {
        BigInteger oi;
        if (other instanceof BigIntegerValue) {
            oi = ((BigIntegerValue)other).value;
        } else {
            oi = BigInteger.valueOf(other.longValue());
        }
        try {
            return makeIntegerValue(value.divide(oi));
        } catch (ArithmeticException err) {
            XPathException e;
            if ("/ by zero".equals(err.getMessage())) {
                e = new XPathException("Integer division by zero", "FOAR0001");
            } else {
                e = new XPathException("Integer division failure", err);
            }
            throw e;
        }
    }

    /**
     * Get an object that implements XML Schema comparison semantics
     */

    public Comparable getSchemaComparable() {
        return new BigIntegerComparable(this);
    }

    protected static class BigIntegerComparable implements Comparable {

        protected BigIntegerValue value;

        public BigIntegerComparable(BigIntegerValue value) {
            this.value = value;
        }

        public BigInteger asBigInteger() {
            return value.asBigInteger();
        }

        public int compareTo(Object o) {
            if (o instanceof Int64Value.Int64Comparable) {
                return asBigInteger().compareTo(BigInteger.valueOf(((Int64Value.Int64Comparable)o).asLong()));
            } else if (o instanceof BigIntegerComparable) {
                return asBigInteger().compareTo(((BigIntegerComparable)o).asBigInteger());
            } else if (o instanceof DecimalValue.DecimalComparable) {
                return value.getDecimalValue().compareTo(((DecimalValue.DecimalComparable)o).asBigDecimal());
            } else {
                return INDETERMINATE_ORDERING;
            }
        }

        public boolean equals(Object o) {
            return compareTo(o) == 0;
        }
        public int hashCode() {
            // Must align with hashCodes for other subtypes of xs:decimal
            BigInteger big = value.asBigInteger();
            if (big.compareTo(MAX_LONG) < 0 && big.compareTo(MIN_LONG) > 0) {
                Int64Value iv = new Int64Value(big.longValue());
                return iv.hashCode();
            }
            return big.hashCode();
        }
    }

    /**
     * Reduce a value to its simplest form.
     */

    public Value reduce() throws XPathException {
        if (compareTo(Long.MAX_VALUE) < 0 && compareTo(Long.MIN_VALUE) > 0) {
            Int64Value iv = new Int64Value(longValue());
            iv.setTypeLabel(typeLabel);
            return iv;
        }
        return this;
    }

    /**
     * Convert to Java object (for passing to external functions)
     *
     * @param target The Java class to which conversion is required
     * @exception XPathException if conversion is not possible, or fails
     * @return the Java object that results from the conversion; always an
     *     instance of the target class
     */

//    public Object convertAtomicToJava(Class target, XPathContext context) throws XPathException {
//        if (isWithinLongRange()) {
//            Int64Value val = Int64Value.makeIntegerValue(longValue());
//            return val.convertToJava(target, context);
//        } else if (target.isAssignableFrom(Int64Value.class)) {
//            return this;
//        } else if (target == BigInteger.class) {
//            return value;
//        } else {
//            return convertPrimitive(BuiltInAtomicType.DECIMAL, true, context).asAtomic().convertToJava(target, context);
//        }
//    }


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
// The Original Code is: all this file except the asStringXT() and zeros() methods (not currently used).
//
// The Initial Developer of the Original Code is Michael H. Kay.
//
// Portions created by (your name) are Copyright (C) (your legal entity). All Rights Reserved.
//
// Contributor(s): none.
//

