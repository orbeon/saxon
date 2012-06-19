package org.orbeon.saxon.value;

import org.orbeon.saxon.type.*;
import org.orbeon.saxon.trans.Err;
import org.orbeon.saxon.om.StandardNames;
import org.orbeon.saxon.trans.XPathException;

import java.math.BigInteger;

/**
 * This class represents the XPath built-in type xs:integer. It is used for all
 * subtypes of xs:integer, other than user-defined subtypes. There are two implementations
 * of IntegerValue: {@link Int64Value}, which accommodates values up to 2^63, and
 * {@link BigIntegerValue}, which accommodates unlimited-length integers.
 */

public abstract class IntegerValue extends NumericValue {

//    static {
//        BuiltInAtomicType.init();
//    }
    /**
     * IntegerValue representing the value -1
     */
    public static final Int64Value MINUS_ONE = new Int64Value(-1);
    /**
     * IntegerValue representing the value zero
     */
    public static final Int64Value ZERO = new Int64Value(0);
    /**
     * IntegerValue representing the value +1
     */
    public static final Int64Value PLUS_ONE = new Int64Value(+1);
    /**
     * Array of small integer values
     */
    public static final Int64Value[] SMALL_INTEGERS = {
        ZERO,
        PLUS_ONE,
        new Int64Value(2),
        new Int64Value(3),
        new Int64Value(4),
        new Int64Value(5),
        new Int64Value(6),
        new Int64Value(7),
        new Int64Value(8),
        new Int64Value(9),
        new Int64Value(10),
        new Int64Value(11),
        new Int64Value(12),
        new Int64Value(13),
        new Int64Value(14),
        new Int64Value(15),
        new Int64Value(16),
        new Int64Value(17),
        new Int64Value(18),
        new Int64Value(19),
        new Int64Value(20)
    };
    /**
     * IntegerValue representing the maximum value for a long
     */
    public static final Int64Value MAX_LONG = new Int64Value(Long.MAX_VALUE);
    /**
     * IntegerValue representing the minimum value for a long
     */
    public static final Int64Value MIN_LONG = new Int64Value(Long.MIN_VALUE);
    /**
     * Static data identifying the min and max values for each built-in subtype of xs:integer.
     * This is a sequence of triples, each holding the fingerprint of the type, the minimum
     * value, and the maximum value. The special value NO_LIMIT indicates that there is no
     * minimum (or no maximum) for this type. The special value MAX_UNSIGNED_LONG represents the
     * value 2^64-1
     */
    private static long NO_LIMIT = -9999;
    private static long MAX_UNSIGNED_LONG = -9998;

    private static long[] ranges = {
            // arrange so the most frequently used types are near the start
            StandardNames.XS_INTEGER, NO_LIMIT, NO_LIMIT,
            StandardNames.XS_LONG, Long.MIN_VALUE, Long.MAX_VALUE,
            StandardNames.XS_INT, Integer.MIN_VALUE, Integer.MAX_VALUE,
            StandardNames.XS_SHORT, Short.MIN_VALUE, Short.MAX_VALUE,
            StandardNames.XS_BYTE, Byte.MIN_VALUE, Byte.MAX_VALUE,
            StandardNames.XS_NON_NEGATIVE_INTEGER, 0, NO_LIMIT,
            StandardNames.XS_POSITIVE_INTEGER, 1, NO_LIMIT,
            StandardNames.XS_NON_POSITIVE_INTEGER, NO_LIMIT, 0,
            StandardNames.XS_NEGATIVE_INTEGER, NO_LIMIT, -1,
            StandardNames.XS_UNSIGNED_LONG, 0, MAX_UNSIGNED_LONG,
            StandardNames.XS_UNSIGNED_INT, 0, 4294967295L,
            StandardNames.XS_UNSIGNED_SHORT, 0, 65535,
            StandardNames.XS_UNSIGNED_BYTE, 0, 255};

    /**
     * Factory method: makes either an Int64Value or a BigIntegerValue depending on the value supplied
     * @param value the supplied integer value
     * @return the value as a BigIntegerValue or Int64Value as appropriate
     */

    public static IntegerValue makeIntegerValue(BigInteger value) {
        if (value.compareTo(BigIntegerValue.MAX_LONG) > 0 || value.compareTo(BigIntegerValue.MIN_LONG) < 0) {
            return new BigIntegerValue(value);
        } else {
            return Int64Value.makeIntegerValue(value.longValue());
        }
    }

    /**
     * This class allows subtypes of xs:integer to be held, as well as xs:integer values.
     * This method sets the required type label. Note that this method modifies the value in situ.
     * @param type the subtype of integer required
     * @param validate true if validation is required, false if the caller warrants that the value
     * is valid for the subtype
     * @return null if the operation succeeds, or a ValidationException if the value is out of range
     */

    public abstract ValidationFailure convertToSubType(BuiltInAtomicType type, boolean validate);

    /**
     * This class allows subtypes of xs:integer to be held, as well as xs:integer values.
     * This method sets the required type label. Note that this method modifies the value in situ.
     * @param type the subtype of integer required
     * @return null if the operation succeeds, or a ValidationException if the value is out of range
     */

    public abstract ValidationFailure validateAgainstSubType(BuiltInAtomicType type);

    /**
     * Check that a value is in range for the specified subtype of xs:integer
     *
     * @param value the value to be checked
     * @param type the required item type, a subtype of xs:integer
     * @return true if successful, false if value is out of range for the subtype
     */

    public static boolean checkRange(long value, BuiltInAtomicType type) {
        int fp = type.getFingerprint();
        for (int i = 0; i < ranges.length; i += 3) {
            if (ranges[i] == fp) {
                long min = ranges[i+1];
                if (min != NO_LIMIT && value < min) {
                    return false;
                }
                long max = ranges[i+2];
                return (max == NO_LIMIT || max == MAX_UNSIGNED_LONG || value <= max);
            }
        }
        throw new IllegalArgumentException(
                "No range information found for integer subtype " + type.getDescription());
    }

    /**
     * Check that a BigInteger is within the required range for a given integer subtype.
     * This method is expensive, so it should not be used unless the BigInteger is outside the range of a long.
     * @param big the supplied BigInteger
     * @param type the derived type (a built-in restriction of xs:integer) to check the value against
     * @return true if the value is within the range for the derived type
     */

    public static boolean checkBigRange(BigInteger big, BuiltInAtomicType type) {

        for (int i = 0; i < ranges.length; i += 3) {
            if (ranges[i] == type.getFingerprint()) {
                long min = ranges[i+1];
                if (min != NO_LIMIT && BigInteger.valueOf(min).compareTo(big) > 0) {
                    return false;
                }
                long max = ranges[i+2];
                if (max == NO_LIMIT) {
                    return true;
                } else if (max == MAX_UNSIGNED_LONG) {
                    return BigIntegerValue.MAX_UNSIGNED_LONG.compareTo(big) >= 0;
                } else {
                    return BigInteger.valueOf(max).compareTo(big) >= 0;
                }
            }
        }
        throw new IllegalArgumentException(
                "No range information found for integer subtype " + type.getDescription());
    }

    /**
     * Static factory method to convert strings to integers.
     * @param s CharSequence representing the string to be converted
     * @return either an Int64Value or a BigIntegerValue representing the value of the String, or
     * a ValidationFailure encapsulating an Exception if the value cannot be converted.
     */

    public static ConversionResult stringToInteger(CharSequence s) {

        int len = s.length();
        int start = 0;
        int last = len - 1;
        while (start < len && s.charAt(start) <= 0x20) {
            start++;
        }
        while (last > start && s.charAt(last) <= 0x20) {
            last--;
        }
        if (start > last) {
            return numericError("Cannot convert zero-length string to an integer");
        }
        if (last - start < 16) {
            // for short numbers, we do the conversion ourselves, to avoid throwing unnecessary exceptions
            boolean negative = false;
            long value = 0;
            int i=start;
            if (s.charAt(i) == '+') {
                i++;
            } else if (s.charAt(i) == '-') {
                negative = true;
                i++;
            }
            if (i > last) {
                return numericError("Cannot convert string " + Err.wrap(s, Err.VALUE) +
                        " to integer: no digits after the sign");
            }
            while (i <= last) {
                char d = s.charAt(i++);
                if (d >= '0' && d <= '9') {
                    value = 10*value + (d-'0');
                } else {
                    return numericError("Cannot convert string " + Err.wrap(s, Err.VALUE) + " to an integer");
                }
            }
            return Int64Value.makeIntegerValue((negative ? -value : value));
        } else {
            // for longer numbers, rely on library routines
            try {
                CharSequence t = Whitespace.trimWhitespace(s);
                if (t.charAt(0) == '+') {
                    t = t.subSequence(1, t.length());
                }
                if (t.length() < 16) {
                    return new Int64Value(Long.parseLong(t.toString()));
                } else {
                    return new BigIntegerValue(new BigInteger(t.toString()));
                }
            } catch (NumberFormatException err) {
                return numericError("Cannot convert string " + Err.wrap(s, Err.VALUE) + " to an integer");
            }
        }
    }

    /**
     * Helper method to handle errors converting a string to a number
     * @param message error message
     * @return a ValidationFailure encapsulating an Exception describing the error
     */
    private static ValidationFailure numericError(String message) {
        ValidationFailure err = new ValidationFailure(message);
        err.setErrorCode("FORG0001");
        return err;
    }

    /**
     * Determine the primitive type of the value. This delivers the same answer as
     * getItemType().getPrimitiveItemType(). The primitive types are
     * the 19 primitive types of XML Schema, plus xs:integer, xs:dayTimeDuration and xs:yearMonthDuration,
     * and xs:untypedAtomic. For external objects, the result is AnyAtomicType.
     */

    public BuiltInAtomicType getPrimitiveType() {
        return BuiltInAtomicType.INTEGER;
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
     * @param other the other integer
     * @return the result of the addition
     */

    public abstract IntegerValue plus(IntegerValue other);

    /**
     * Subtract another integer
     * @param other the other integer
     * @return the result of the subtraction
     */

    public abstract IntegerValue minus(IntegerValue other);

    /**
     * Multiply by another integer
     * @param other the other integer
     * @return the result of the multiplication
     */

    public abstract IntegerValue times(IntegerValue other);

    /**
     * Divide by another integer
     * @param other the other integer
     * @return the result of the division
     * @throws XPathException if the other integer is zero
     */

    public abstract NumericValue div(IntegerValue other) throws XPathException;

    /**
     * Take modulo another integer
     * @param other the other integer
     * @return the result of the modulo operation (the remainder)
     * @throws XPathException if the other integer is zero
     */

    public abstract IntegerValue mod(IntegerValue other) throws XPathException;

    /**
     * Integer divide by another integer
     * @param other the other integer
     * @return the result of the integer division
     * @throws XPathException if the other integer is zero
     */

    public abstract IntegerValue idiv(IntegerValue other) throws XPathException;

    /**
     * Get the value as a BigInteger
     * @return the value, as a BigInteger
     */

    public abstract BigInteger asBigInteger();

    /**
     * Get the signum of an int
     * @param i the int
     * @return -1 if the integer is negative, 0 if it is zero, +1 if it is positive
     */

    protected static int signum(int i) {
        return (i >> 31) | (-i >>> 31);
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

