package org.orbeon.saxon.value;

import org.orbeon.saxon.expr.Calculator;
import org.orbeon.saxon.expr.XPathContext;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.type.*;
import org.orbeon.saxon.om.StandardNames;

import java.math.BigDecimal;
import java.math.BigInteger;

/**
 * An integer value: note this is a subtype of decimal in XML Schema, not a primitive type.
 * This class supports integer values in the range permitted by a Java "long",
 * and also supports the built-in subtypes of xs:integer.
 */

public final class Int64Value extends IntegerValue {

    private long value;

    /**
     * Constructor supplying a long
     *
     * @param value the value of the IntegerValue
     */

    public Int64Value(long value) {
        this.value = value;
        typeLabel = BuiltInAtomicType.INTEGER;
    }

    /**
     * Constructor for a subtype, supplying a long and a type label.
     *
     * @param val The supplied value, as an integer
     * @param type the required item type, a subtype of xs:integer
     * @param check Set to true if the method is required to check that the value is in range;
     * false if the caller can guarantee that the value has already been checked.
     * @throws XPathException if the supplied value is out of range for the
     *      target type
     */

    public Int64Value(long val, BuiltInAtomicType type, boolean check) throws XPathException {
        value = val;
        typeLabel = type;
        if (check && !checkRange(value, type)) {
            XPathException err = new XPathException("Integer value " + val +
                    " is out of range for the requested type " + type.getDescription());
            err.setErrorCode("XPTY0004");
            err.setIsTypeError(true);
            throw err;
        }
    }

    /**
     * Factory method: allows Int64Value objects to be reused. Note that
     * a value obtained using this method must not be modified to set a type label, because
     * the value is in general shared.
     * @param value the integer value
     * @return an Int64Value with this integer value
     */

    public static Int64Value makeIntegerValue(long value) {
        if (value <= 20 && value >= 0) {
            return SMALL_INTEGERS[(int)value];
        } else {
            return new Int64Value(value);
        }
    }

    /**
     * Factory method to create a derived value, with no checking of the value against the
     * derived type
     * @param val the integer value
     * @param type the subtype of xs:integer
     * @return the constructed value
     */

    public static Int64Value makeDerived(long val, AtomicType type)  {
        Int64Value v = new Int64Value(val);
        v.typeLabel = type;
        return v;
    }

    /**
     * Factory method returning the integer -1, 0, or +1 according as the argument
     * is negative, zero, or positive
     * @param val the value to be tested
     * @return the Int64Value representing -1, 0, or +1
     */

    public static Int64Value signum(long val) {
        if (val == 0) {
            return IntegerValue.ZERO;
        } else {
            return (val < 0 ? IntegerValue.MINUS_ONE : IntegerValue.PLUS_ONE);
        }
    }

    /**
     * Create a copy of this atomic value, with a different type label
     *
     * @param typeLabel the type label of the new copy. The caller is responsible for checking that
     *                  the value actually conforms to this type.
     */

    public AtomicValue copyAsSubType(AtomicType typeLabel) {
        Int64Value v = new Int64Value(value);
        v.typeLabel = typeLabel;
        return v;
    }

    /**
     * Convert the value to a subtype of xs:integer
     * @param subtype the target subtype
     * @param validate true if validation is required; false if the caller already knows that the value is valid
     * @return null if the conversion succeeds; a ValidationFailure describing the failure if it fails. Note
     * that the exception is returned, not thrown.
     */

    public ValidationFailure convertToSubType(BuiltInAtomicType subtype, boolean validate) {
        if (!validate) {
            setSubType(subtype);
            return null;
        } else if (checkRange(subtype)) {
            return null;
        } else {
            ValidationFailure err = new ValidationFailure("String " + value +
                    " cannot be converted to integer subtype " + subtype.getDescription());
            err.setErrorCode("FORG0001");
            return err;
        }
    }


    /**
     * This class allows subtypes of xs:integer to be held, as well as xs:integer values.
     * This method sets the required type label. Note that this method modifies the value in situ.
     *
     * @param type the subtype of integer required
     * @return null if the operation succeeds, or a ValidationException if the value is out of range
     */

    public ValidationFailure validateAgainstSubType(BuiltInAtomicType type) {
        if (checkRange(value, type)) {
            return null;
        } else {
            ValidationFailure err = new ValidationFailure("Value " + value +
                    " cannot be converted to integer subtype " + type.getDescription());
            err.setErrorCode("FORG0001");
            return err;
        }
    }

    /**
     * This class allows subtypes of xs:integer to be held, as well as xs:integer values.
     * This method sets the required type label. It is the caller's responsibility to check that
     * the value is within range.
     * @param type the type label to be assigned
     */
    public void setSubType(AtomicType type) {
        typeLabel = type;
    }

    /**
     * This class allows subtypes of xs:integer to be held, as well as xs:integer values.
     * This method checks that the value is within range, and also sets the type label.
     * @param type the subtype of integer required
     * @return true if successful, false if value is out of range for the subtype
     */
    public boolean checkRange(BuiltInAtomicType type) {
        typeLabel = type;
        return checkRange(value, type);
    }

    /**
     * Get an object that implements XML Schema comparison semantics
     */

    public Comparable getSchemaComparable() {
        return new Int64Comparable(this);
    }

    protected static class Int64Comparable implements Comparable {

        protected Int64Value value;

        public Int64Comparable(Int64Value value) {
            this.value = value;
        }

        public long asLong() {
            return value.longValue();
        }

        public int compareTo(Object o) {
            if (o instanceof Int64Comparable) {
                long long0 = value.longValue();
                long long1 = ((Int64Comparable)o).value.longValue();
                if (long0 <= long1) {
                    if (long0 == long1) {
                        return 0;
                    } else {
                        return -1;
                    }
                } else {
                    return 1;
                }
            } else if (o instanceof BigIntegerValue.BigIntegerComparable) {
                return value.asBigInteger().compareTo(((BigIntegerValue.BigIntegerComparable)o).asBigInteger());
            } else if (o instanceof DecimalValue.DecimalComparable) {
                return value.getDecimalValue().compareTo(((DecimalValue.DecimalComparable)o).asBigDecimal());
            } else {
                return INDETERMINATE_ORDERING;
            }
        }
        
        public boolean equals(Object o) {
            if (o instanceof Int64Comparable) {
                return (asLong() == ((Int64Comparable)o).asLong());
            } else {
                return compareTo(o) == 0;
            }
        }
        public int hashCode() {
            // Must align with hashCodes for other subtypes of xs:decimal
            return (int)asLong();
        }

    }

    /**
     * Get the hashCode. This must conform to the rules for other NumericValue hashcodes
     * @see NumericValue#hashCode
     */

    public int hashCode() {
        if (value > Integer.MIN_VALUE && value < Integer.MAX_VALUE) {
            return (int) value;
        } else {
            return new Double(getDoubleValue()).hashCode();
        }
    }

    /**
     * Get the value
     * @return the value of the xs:integer, as a Java long
     */

    public long longValue() {
        return value;
    }

    /**
     * Return the effective boolean value of this integer
     * @return false if the integer is zero, otherwise true
     */
    public boolean effectiveBooleanValue() {
        return value != 0;
    }

    /**
     * Compare the value to another numeric value
     * @param other the numeric value to be compared to this value
     * @return -1 if this value is less than the other, 0 if they are equal,
     *     +1 if this value is greater
     */

    public int compareTo(Object other) {
        if (other instanceof Int64Value) {
            long val2 = ((Int64Value) other).value;
            if (value == val2) return 0;
            if (value < val2) return -1;
            return 1;
        } else if (other instanceof BigIntegerValue) {
            return new BigIntegerValue(value).compareTo(other);
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
        if (value == other) return 0;
        if (value < other) return -1;
        return 1;
    }

    /**
     * Convert to target data type
     *
     * @param requiredType an integer identifying the required atomic type
     * @param context XPath dynamic evaluation context
     * @return an AtomicValue, a value of the required type
     */

    public ConversionResult convertPrimitive(BuiltInAtomicType requiredType, boolean validate, XPathContext context) {
        switch (requiredType.getFingerprint()) {
            case StandardNames.XS_BOOLEAN:
                return BooleanValue.get(value != 0);

            case StandardNames.XS_NUMERIC:
            case StandardNames.XS_INTEGER:
            case StandardNames.XS_ANY_ATOMIC_TYPE:
                if (typeLabel == BuiltInAtomicType.INTEGER) {
                    return this;
                } else {
                    // promote subtype to type xs:integer
                    return copyAsSubType(BuiltInAtomicType.INTEGER);
                }

            case StandardNames.XS_NON_POSITIVE_INTEGER:
            case StandardNames.XS_NEGATIVE_INTEGER:
            case StandardNames.XS_LONG:
            case StandardNames.XS_INT:
            case StandardNames.XS_SHORT:
            case StandardNames.XS_BYTE:
            case StandardNames.XS_NON_NEGATIVE_INTEGER:
            case StandardNames.XS_POSITIVE_INTEGER:
            case StandardNames.XS_UNSIGNED_LONG:
            case StandardNames.XS_UNSIGNED_INT:
            case StandardNames.XS_UNSIGNED_SHORT:
            case StandardNames.XS_UNSIGNED_BYTE:
                Int64Value val = new Int64Value(value);
                ValidationFailure err = val.convertToSubType(requiredType, validate);
                if (err != null) {
                    return err;
                }
                return val;

            case StandardNames.XS_DOUBLE:
                return new DoubleValue((double) value);

            case StandardNames.XS_FLOAT:
                return new FloatValue((float) value);

            case StandardNames.XS_DECIMAL:
                return new DecimalValue(value);

            case StandardNames.XS_STRING:
                return new StringValue(getStringValue());

            case StandardNames.XS_UNTYPED_ATOMIC:
                return new UntypedAtomicValue(getStringValue());

            default:
                ValidationFailure err2 = new ValidationFailure("Cannot convert integer to " +
                        requiredType.getDisplayName());
                err2.setErrorCode("XPTY0004");
                return err2;
        }
    }

    /**
     * Get the value as a String
     *
     * @return a String representation of the value
     */

    public String getStringValue() {
        return Long.toString(value);
    }

    /**
     * Get the numeric value as a double
     *
     * @return A double representing this numeric value; NaN if it cannot be
     *         converted
     */
    public double getDoubleValue() {
        return (double) value;
    }

    /**
     * Get the numeric value converted to a float
     *
     * @return a float representing this numeric value; NaN if it cannot be converted
     */

    public float getFloatValue() {
        return (float)value;
    }

    /**
     * Get the numeric value converted to a decimal
     *
     * @return a decimal representing this numeric value;
     */

    public BigDecimal getDecimalValue() {
        return BigDecimal.valueOf(value);
    }

    /**
     * Negate the value
     *
     * @return the result of inverting the sign of the value
     */

    public NumericValue negate() {
        if (value == Long.MIN_VALUE) {
            return BigIntegerValue.makeIntegerValue(BigInteger.valueOf(value)).negate();
        } else {
            return new Int64Value(-value);
        }
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
        long absolute = Math.abs(value);
        if (scale >= 0) {
            return this;
        } else {
            if (scale < -15) {
                return new BigIntegerValue(value).roundHalfToEven(scale);
            }
            long factor = 1;
            for (long i = 1; i <= -scale; i++) {
                factor *= 10;
            }
            long modulus = absolute % factor;
            long rval = absolute - modulus;
            long d = modulus * 2;
            if (d > factor) {
                rval += factor;
            } else if (d < factor) {
                // no-op
            } else {
                // round to even
                if (rval % (2 * factor) == 0) {
                    // no-op
                } else {
                    rval += factor;
                }
            }
            if (value < 0) rval = -rval;
            return new Int64Value(rval);
        }
    }

    /**
     * Determine whether the value is negative, zero, or positive
     * @return -1 if negative, 0 if zero, +1 if positive, NaN if NaN
     */

    public double signum() {
        if (value > 0) return +1;
        if (value == 0) return 0;
        return -1;
    }

    /**
     * Add another integer
     */

    public IntegerValue plus(IntegerValue other) {
        // if either of the values is large, we use BigInteger arithmetic to be on the safe side
        if (other instanceof Int64Value) {
            long topa = (value >> 60) & 0xf;
            if (topa != 0 && topa != 0xf) {
                return new BigIntegerValue(value).plus(new BigIntegerValue(((Int64Value)other).value));
            }
            long topb = (((Int64Value)other).value >> 60) & 0xf;
            if (topb != 0 && topb != 0xf) {
                return new BigIntegerValue(value).plus(new BigIntegerValue(((Int64Value)other).value));
            }
            return makeIntegerValue(value + ((Int64Value)other).value);
        } else {
            return new BigIntegerValue(value).plus(other);
        }
    }

    /**
     * Subtract another integer
     */

    public IntegerValue minus(IntegerValue other) {
        // if either of the values is large, we use BigInteger arithmetic to be on the safe side
        if (other instanceof Int64Value) {
            long topa = (value >> 60) & 0xf;
            if (topa != 0 && topa != 0xf) {
                return new BigIntegerValue(value).minus(new BigIntegerValue(((Int64Value)other).value));
            }
            long topb = (((Int64Value)other).value >> 60) & 0xf;
            if (topb != 0 && topb != 0xf) {
                return new BigIntegerValue(value).minus(new BigIntegerValue(((Int64Value)other).value));
            }
            return makeIntegerValue(value - ((Int64Value)other).value);
        } else {
            return new BigIntegerValue(value).minus(other);
        }
    }

    /**
     * Multiply by another integer
     */

    public IntegerValue times(IntegerValue other) {
        // if either of the values is large, we use BigInteger arithmetic to be on the safe side
        if (other instanceof Int64Value) {
            if (isLong() || ((Int64Value)other).isLong()) {
                return new BigIntegerValue(value).times(new BigIntegerValue(((Int64Value)other).value));
            } else {
                return makeIntegerValue(value * ((Int64Value)other).value);
            }
        } else {
            return new BigIntegerValue(value).times(other);
        }
    }

    /**
     * Divide by another integer
     */

    public NumericValue div(IntegerValue other) throws XPathException {
        // if either of the values is large, we use BigInteger arithmetic to be on the safe side
        if (other instanceof Int64Value) {
            long quotient = ((Int64Value)other).value;
            if (quotient == 0) {
                throw new XPathException("Integer division by zero", "FOAR0001");
            }
            if (isLong() || ((Int64Value)other).isLong()) {
                return new BigIntegerValue(value).div(new BigIntegerValue(quotient));
            }

            // the result of dividing two integers is a decimal; but if
            // one divides exactly by the other, we implement it as an integer
            if (value % quotient == 0) {
                return makeIntegerValue(value / quotient);
            } else {
                return (NumericValue)Calculator.DECIMAL_DECIMAL[Calculator.DIV].compute(
                            new DecimalValue(value), new DecimalValue(quotient), null);
            }
        } else {
            return new BigIntegerValue(value).div(other);
        }
    }

    /**
     * Take modulo another integer
     */

    public IntegerValue mod(IntegerValue other) throws XPathException {
        // if either of the values is large, we use BigInteger arithmetic to be on the safe side
        if (other instanceof Int64Value) {
            long quotient = ((Int64Value) other).value;
            if (quotient == 0) {
                throw new XPathException("Integer modulo zero", "FOAR0001");
            }
            if (isLong() || ((Int64Value)other).isLong()) {
                return new BigIntegerValue(value).mod(new BigIntegerValue(((Int64Value)other).value));
            } else {
                return makeIntegerValue(value % quotient);
            }
        } else {
            return new BigIntegerValue(value).mod(other);
        }
    }

    /**
     * Integer divide by another integer
     */

    public IntegerValue idiv(IntegerValue other) throws XPathException {
       // if either of the values is large, we use BigInteger arithmetic to be on the safe side
        if (other instanceof Int64Value) {
            if (isLong() || ((Int64Value)other).isLong()) {
                return new BigIntegerValue(value).idiv(new BigIntegerValue(((Int64Value)other).value));
            }
            try {
                return makeIntegerValue(value / ((Int64Value) other).value);
            } catch (ArithmeticException err) {
                XPathException e;
                if ("/ by zero".equals(err.getMessage())) {
                    e = new XPathException("Integer division by zero", "FOAR0001");
                } else {
                    e = new XPathException("Integer division failure", err);
                }
                throw e;
            }
        } else {
            return new BigIntegerValue(value).idiv(other);
        }
    }

    /**
     * Test whether this value needs a long to hold it. Specifically, whether
     * the absolute value is > 2^31.
     */

    private boolean isLong() {
        long top = value >> 31;
        return top != 0 && top != 0x1ffffffffL;
    }

    /**
     * Get the value as a BigInteger
     */

    public BigInteger asBigInteger() {
        return BigInteger.valueOf(value);
    }

     /**
     * Get conversion preference for this value to a Java class.
     *
     * @param required the Java class to which conversion is required
     * @return the conversion preference. A low result indicates higher
     *     preference.
     */

    // Note: this table gives java Long preference over Integer, even if the
    // XML Schema type is xs:int

    /**
     * Convert to Java object (for passing to external functions)
     *
     * @param target The Java class to which conversion is required
     * @exception XPathException if conversion is not possible, or fails
     * @return the Java object that results from the conversion; always an
     *     instance of the target class
     */

//    public Object convertAtomicToJava(Class target, XPathContext context) throws XPathException {
//        if (target == Object.class) {
//            return new Long(value);
//        } else if (target.isAssignableFrom(Int64Value.class)) {
//            return this;
//        } else if (target == double.class || target == Double.class) {
//            return new Double(value);
//        } else if (target == float.class || target == Float.class) {
//            return new Float(value);
//        } else if (target == long.class || target == Long.class) {
//            return new Long(value);
//        } else if (target == int.class || target == Integer.class) {
//            return new Integer((int) value);
//        } else if (target == short.class || target == Short.class) {
//            return new Short((short) value);
//        } else if (target == byte.class || target == Byte.class) {
//            return new Byte((byte) value);
//        } else if (target == char.class || target == Character.class) {
//            return new Character((char) value);
//        } else if (target == BigInteger.class) {
//            return BigInteger.valueOf(value);
//        } else if (target == BigDecimal.class) {
//            return BigDecimal.valueOf(value);
//        } else {
//            Object o = convertSequenceToJava(target, context);
//            if (o == null) {
//                throw new XPathException("Conversion of integer to " + target.getName() +
//                        " is not supported");
//            }
//            return o;
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
// Contributor(s): none.
//

