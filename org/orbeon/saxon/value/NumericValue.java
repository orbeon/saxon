package net.sf.saxon.value;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.type.ItemType;
import net.sf.saxon.type.Type;
import net.sf.saxon.xpath.XPathException;

/**
 * NumericValue is an abstract superclass for IntegerValue, DecimalValue,
 * FloatValue, and DoubleValue
 */

public abstract class NumericValue extends AtomicValue implements Comparable {

    /**
     * Get a numeric value by parsing a string; the type of numeric value depends
     * on the lexical form of the string, following the rules for XPath numeric
     * literals.
     * @param in the input string
     * @return a NumericValue representing the value of the string. Returns Double.NaN if the
     * value cannot be parsed as a string.
     */

    public static NumericValue parseNumber(String in) {
        try {
            if (in.indexOf('e') >= 0 || in.indexOf('E') >= 0) {
                return new DoubleValue(Double.parseDouble(in));
            } else if (in.indexOf('.') >= 0) {
                return new DecimalValue(in);
            } else {
                return IntegerValue.stringToInteger(in);
            }
        } catch (XPathException e) {
            return DoubleValue.NaN;
        }
    }
    /**
     * Get the numeric value as a double
     *
     * @return A double representing this numeric value; NaN if it cannot be
     *     converted
     */
    public double getDoubleValue() {
        try {
            return ((DoubleValue)convert(Type.DOUBLE, null)).getDoubleValue();
        } catch (XPathException err) {
            return Double.NaN;
        }
    }

    /**
     * Test whether the value is the double/float value NaN
     */

    public boolean isNaN() {
        return false;
    }

    /**
     * Test whether the value is an integer (an instance of a subtype of xs:integer)
     */

    public static boolean isInteger(AtomicValue value) {
        if (value instanceof IntegerValue) {
            return true;
        } else if (value instanceof BigIntegerValue) {
            return true;
        } else if (!value.hasBuiltInType() && NumericValue.isInteger(value.getPrimitiveValue())) {
            return true;
        }
        return false;
    }

    /**
     * Return the numeric value as a Java long.
     *
     * @exception XPathException if the value cannot be converted
     * @return the numeric value as a Java long. This performs truncation
     *     towards zero.
     */
    public long longValue() throws XPathException {
        return ((IntegerValue)convert(Type.INTEGER, null)).longValue();
    }

    /**
     * Change the sign of the number
     *
     * @return a value, of the same type as the original, with its sign
     *     inverted
     */

    public abstract NumericValue negate();

    /**
     * Implement the XPath floor() function
     *
     * @return a value, of the same type as that supplied, rounded towards
     *     minus infinity
     */

    public abstract NumericValue floor();

    /**
     * Implement the XPath ceiling() function
     *
     * @return a value, of the same type as that supplied, rounded towards
     *     plus infinity
     */

    public abstract NumericValue ceiling();

    /**
     * Implement the XPath round() function
     *
     * @return a value, of the same type as that supplied, rounded towards the
     *      nearest whole number (0.5 rounded up)
     */

    public abstract NumericValue round();

    /**
     * Implement the XPath 2.0 round-to-half-even() function
     *
     * @param scale the decimal position for rounding: e.g. 2 rounds to a
     *     multiple of 0.01, while -2 rounds to a multiple of 100
     * @return a value, of the same type as the original, rounded towards the
     *     nearest multiple of 10**(-scale), with rounding towards the nearest
     *      even number if two values are equally near
     */

    public abstract NumericValue roundToHalfEven(int scale);

    /**
     * Determine whether the value is negative, zero, or positive
     * @return -1 if negative, 0 if zero (including negative zero), +1 if positive, NaN if NaN
     */ 

    public abstract double signum();

    /**
     * Perform a binary arithmetic operation
     *
     * @param operator the binary arithmetic operation to be performed. Uses
     *     the constants defined in the Tokenizer class
     * @param other the other operand
     * @exception XPathException if an arithmetic error occurs
     * @return the result of the arithmetic operation
     * @see net.sf.saxon.expr.Tokenizer
     */

    public abstract NumericValue arithmetic(int operator, NumericValue other, XPathContext context)
    throws XPathException;

    /**
     * Determine whether the value is a whole number, that is, whether it compares
     * equal to some integer
     *
     * @return true if the value is a whole number
     */

    public abstract boolean isWholeNumber();

    /**
     * Compare the value to another numeric value
     *
     * @exception ClassCastException if the other value is not a NumericValue
     *     (the parameter is declared as Object to satisfy the Comparable
     *     interface)
     * @param other The other numeric value
     * @return -1 if this one is the lower, 0 if they are numerically equal,
     *     +1 if this one is the higher.
     */

    // This is the default implementation. Subclasses of number avoid the conversion to double
    // when comparing with another number of the same type.

    public int compareTo(Object other) {
        if (other instanceof AtomicValue && !((AtomicValue)other).hasBuiltInType()) {
            return compareTo(((AtomicValue)other).getPrimitiveValue());
        }
        if (!(other instanceof NumericValue)) {
            throw new ClassCastException("Numeric values are not comparable to " + other.getClass());
        }
        double a = this.getDoubleValue();
        double b = ((NumericValue)other).getDoubleValue();
        if (a == b) return 0;
        if (a < b) return -1;
        return +1;
    }

    /**
     * The equals() function compares numeric equality among integers, decimals, floats, doubles, and
     * their subtypes
     *
     * @param other the value to be compared with this one
     * @return true if the two values are numerically equal
     */

    public final boolean equals(Object other) {
        return compareTo(other) == 0;
    }

    /**
     * Identify lowest common supertype of two numeric values for promotion purposes
     *
     * @param v1 the item type of the first operand
     * @param v2 the item type of the second operand
     * @return the item type that should be used for arithmetic between
     *     operands of the two specified item types
     */

    public static ItemType promote(ItemType v1, ItemType v2) {
        ItemType t1 = (Type.isSubType(v1, Type.NUMBER_TYPE) ? v1 : Type.DOUBLE_TYPE);
        ItemType t2 = (Type.isSubType(v2, Type.NUMBER_TYPE) ? v2 : Type.DOUBLE_TYPE);

        if (t1 == t2) return t1;

        if (t1 == Type.DOUBLE_TYPE || t2 == Type.DOUBLE_TYPE) {
            return Type.DOUBLE_TYPE;
        }

        if (t1 == Type.FLOAT_TYPE || t2 == Type.FLOAT_TYPE) {
            return Type.FLOAT_TYPE;
        }

        if (t1 == Type.DECIMAL_TYPE || t2 == Type.DECIMAL_TYPE) {
            return Type.DECIMAL_TYPE;
        }

        return Type.INTEGER_TYPE;
    }

    /**
     * hashCode() must be the same for two values that are equal. One
     * way to ensure this is to convert the value to a double, and take the
     * hashCode of the double. But this is expensive in the common case where
     * we are comparing integers. So we adopt the rule: for values that are in
     * the range of a Java Integer, we use the int value as the hashcode. For
     * values outside that range, we convert to a double and take the hashCode of
     * the double. This method needs to have a compatible implementation in
     * each subclass.
     *
     * @return the hash code of the numeric value
     */

    public abstract int hashCode();

    /**
     * Produce a string representation of the value
     * @return The result of casting the number to a string
     */
    public String toString() {
        return getStringValue();
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
// The Initial Developer of the Original Code is Saxonica Limited
//
// Portions created by (your name) are Copyright (C) (your legal entity). All Rights Reserved.
//
// Contributor(s): none
//