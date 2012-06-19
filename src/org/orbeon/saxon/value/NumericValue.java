package org.orbeon.saxon.value;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.type.*;
import org.orbeon.saxon.expr.XPathContext;
import org.orbeon.saxon.sort.StringCollator;

import java.math.BigDecimal;

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
        if (in.indexOf('e') >= 0 || in.indexOf('E') >= 0) {
            try {
                return new DoubleValue(Double.parseDouble(in));
            } catch (NumberFormatException e) {
                return DoubleValue.NaN;
            }
        } else if (in.indexOf('.') >= 0) {
            ConversionResult v = DecimalValue.makeDecimalValue(in, true);
            if (v instanceof ValidationFailure) {
                return DoubleValue.NaN;
            } else {
                return (NumericValue)v;
            }
        } else {
            ConversionResult v = Int64Value.stringToInteger(in);
            if (v instanceof ValidationFailure) {
                return DoubleValue.NaN;
            } else {
                return (NumericValue)v;
            }
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
            return ((DoubleValue)convertPrimitive(BuiltInAtomicType.DOUBLE, true, null).asAtomic()).getDoubleValue();
        } catch (XPathException err) {
            return Double.NaN;
        }
    }

    /**
     * Get the numeric value converted to a float
     * @return a float representing this numeric value; NaN if it cannot be converted
     */

    public float getFloatValue() {
        try {
            return ((FloatValue)convertPrimitive(BuiltInAtomicType.FLOAT, true, null).asAtomic()).getFloatValue();
        } catch (XPathException err) {
            return Float.NaN;
        }
    }

    /**
     * Get the numeric value converted to a decimal
     * @return a decimal representing this numeric value;
     * @throws XPathException if the value cannot be converted, for example if it is NaN or infinite
     */

    public BigDecimal getDecimalValue() throws XPathException {
        return ((DecimalValue)convertPrimitive(BuiltInAtomicType.DECIMAL, true, null).asAtomic()).getDecimalValue();
    }




    /**
     * Test whether a value is an integer (an instance of a subtype of xs:integer)
     * @param value the value being tested
     * @return true if the value is an instance of xs:integer or a type derived therefrom
     */

    public static boolean isInteger(AtomicValue value) {
        return (value instanceof IntegerValue);
    }

    /**
     * Return the numeric value as a Java long.
     *
     * @exception org.orbeon.saxon.trans.XPathException if the value cannot be converted
     * @return the numeric value as a Java long. This performs truncation
     *     towards zero.
     */
    public long longValue() throws XPathException {
        return ((Int64Value)convertPrimitive(BuiltInAtomicType.INTEGER, true, null).asAtomic()).longValue();
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
     * Implement the XPath 2.0 round-half-to-even() function
     *
     * @param scale the decimal position for rounding: e.g. 2 rounds to a
     *     multiple of 0.01, while -2 rounds to a multiple of 100
     * @return a value, of the same type as the original, rounded towards the
     *     nearest multiple of 10**(-scale), with rounding towards the nearest
     *      even number if two values are equally near
     */

    public abstract NumericValue roundHalfToEven(int scale);

    /**
     * Determine whether the value is negative, zero, or positive
     * @return -1 if negative, 0 if zero (including negative zero), +1 if positive, NaN if NaN
     */

    public abstract double signum();

    /**
     * Determine whether the value is a whole number, that is, whether it compares
     * equal to some integer
     *
     * @return true if the value is a whole number
     */

    public abstract boolean isWholeNumber();

    /**
     * Get a Comparable value that implements the XPath ordering comparison semantics for this value.
     * Returns null if the value is not comparable according to XPath rules. The default implementation
     * returns null. This is overridden for types that allow ordered comparisons in XPath: numeric, boolean,
     * string, date, time, dateTime, yearMonthDuration, dayTimeDuration, and anyURI.
     * @param ordered
     * @param collator
     * @param context
     */

    public Object getXPathComparable(boolean ordered, StringCollator collator, XPathContext context) {
        return this;
    }

    /**
     * Compare the value to another numeric value
     *
     * @exception ClassCastException if the other value is not a NumericValue
     *     (the parameter is declared as Object to satisfy the Comparable
     *     interface)
     * @param other The other numeric value
     * @return -1 if this one is the lower, 0 if they are numerically equal,
     *     +1 if this one is the higher, or if either value is NaN. Where NaN values are
     *     involved, they should be handled by the caller before invoking this method.
     */

    // This is the default implementation. Subclasses of number avoid the conversion to double
    // when comparing with another number of the same type.

    public int compareTo(Object other) {
        double a = getDoubleValue();
        double b = ((NumericValue)other).getDoubleValue();
        if (a == b) return 0;
        if (a < b) return -1;
        return +1;
    }

    /**
     * Compare the value to a long
     * @param other the value to be compared with
     * @return -1 if this is less, 0 if this is equal, +1 if this is greater or if this is NaN
     */

    public abstract int compareTo(long other);

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
     * @param typeHierarchy the type hierarchy cache
     * @return the item type that should be used for arithmetic between
     *     operands of the two specified item types
     */

    public static ItemType promote(ItemType v1, ItemType v2, TypeHierarchy typeHierarchy) {
        ItemType t1 = (typeHierarchy.isSubType(v1, BuiltInAtomicType.NUMERIC) ? v1 : BuiltInAtomicType.DOUBLE);
        ItemType t2 = (typeHierarchy.isSubType(v2, BuiltInAtomicType.NUMERIC) ? v2 : BuiltInAtomicType.DOUBLE);

        if (t1 == t2) return t1;

        if (t1.equals(BuiltInAtomicType.DOUBLE) || t2.equals(BuiltInAtomicType.DOUBLE)) {
            return BuiltInAtomicType.DOUBLE;
        }

        if (t1.equals(BuiltInAtomicType.FLOAT) || t2.equals(BuiltInAtomicType.FLOAT)) {
            return BuiltInAtomicType.FLOAT;
        }

        if (t1.equals(BuiltInAtomicType.DECIMAL) || t2.equals(BuiltInAtomicType.DECIMAL)) {
            return BuiltInAtomicType.DECIMAL;
        }

        return BuiltInAtomicType.INTEGER;
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