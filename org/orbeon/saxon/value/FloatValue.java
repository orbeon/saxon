package org.orbeon.saxon.value;
import org.orbeon.saxon.trans.Err;
import org.orbeon.saxon.expr.XPathContext;
import org.orbeon.saxon.om.FastStringBuffer;
import org.orbeon.saxon.om.StandardNames;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.type.*;

import java.math.BigDecimal;

/**
* A numeric (single precision floating point) value
*/

public final class FloatValue extends NumericValue {

    public static final FloatValue ZERO = new FloatValue((float)0.0);
    public static final FloatValue NEGATIVE_ZERO = new FloatValue((float)-0.0);
    public static final FloatValue ONE = new FloatValue((float)1.0);
    public static final FloatValue NaN = new FloatValue(Float.NaN);

    private float value;

    /**
     * Constructor supplying a string
     */

    public FloatValue(CharSequence val) throws ValidationException {
        try {
            value = (float)Value.stringToNumber(val);
        } catch (NumberFormatException e) {
            throw new ValidationException("Cannot convert string " + Err.wrap(val, Err.VALUE) + " to a float");
        }
        typeLabel = BuiltInAtomicType.FLOAT;
    }

    /**
    * Constructor supplying a float
    * @param value the value of the float
    */

    public FloatValue(float value) {
        this.value = value;
        typeLabel = BuiltInAtomicType.FLOAT;
    }

    /**
     * Constructor supplying a float and an AtomicType, for creating
     * a value that belongs to a user-defined subtype of xs:float. It is
     * the caller's responsibility to ensure that the supplied value conforms
     * to the supplied type.
     * @param value the value of the NumericValue
     * @param type the type of the value. This must be a subtype of xs:float, and the
     * value must conform to this type. The method does not check these conditions.
     */

    public FloatValue(float value, AtomicType type) {
        this.value = value;
        typeLabel = type;
    }

    /**
     * Create a copy of this atomic value, with a different type label
     *
     * @param typeLabel the type label of the new copy. The caller is responsible for checking that
     *                  the value actually conforms to this type.
     */

    public AtomicValue copyAsSubType(AtomicType typeLabel) {
        FloatValue v = new FloatValue(value);
        v.typeLabel = typeLabel;
        return v;
    }

    /**
     * Determine the primitive type of the value. This delivers the same answer as
     * getItemType().getPrimitiveItemType(). The primitive types are
     * the 19 primitive types of XML Schema, plus xs:integer, xs:dayTimeDuration and xs:yearMonthDuration,
     * and xs:untypedAtomic. For external objects, the result is AnyAtomicType.
     */

    public BuiltInAtomicType getPrimitiveType() {
        return BuiltInAtomicType.FLOAT;
    }

    /**
    * Get the value
    */

    public float getFloatValue() {
        return value;
    }

    public double getDoubleValue() {
        return (double)value;
    }

    /**
     * Get the hashCode. This must conform to the rules for other NumericValue hashcodes
     * @see NumericValue#hashCode
     */

    public int hashCode() {
        if (value > Integer.MIN_VALUE && value < Integer.MAX_VALUE) {
            return (int)value;
        } else {
            return new Double(getDoubleValue()).hashCode();
        }
    }

    /**
     * Test whether the value is the double/float value NaN
     */

    public boolean isNaN() {
        return Float.isNaN(value);
    }

    /**
     * Get the effective boolean value
     * @return true unless the value is zero or NaN
     */
    public boolean effectiveBooleanValue() {
        return (value!=0.0 && !Float.isNaN(value));
    }

    /**
     * Convert to target data type
     * @param requiredType an integer identifying the required atomic type
     * @param context XPath dynamic context. Not used when converting from float
     * @return an AtomicValue, a value of the required type; or an ErrorValue
    */

    public ConversionResult convertPrimitive(BuiltInAtomicType requiredType, boolean validate, XPathContext context) {
        switch(requiredType.getFingerprint()) {
        case StandardNames.XS_BOOLEAN:
            return BooleanValue.get(value!=0.0 && !Float.isNaN(value));
        case StandardNames.XS_FLOAT:
        case StandardNames.XS_NUMERIC:
        case StandardNames.XS_ANY_ATOMIC_TYPE:
            return this;
        case StandardNames.XS_INTEGER:
            if (Float.isNaN(value)) {
                ValidationFailure err = new ValidationFailure("Cannot convert float NaN to an integer");
                err.setErrorCode("FOCA0002");
                return err;
            }
            if (Float.isInfinite(value)) {
                ValidationFailure err = new ValidationFailure("Cannot convert float infinity to an integer");
                err.setErrorCode("FOCA0002");
                return err;
            }
            if (value > Long.MAX_VALUE || value < Long.MIN_VALUE) {
                return new BigIntegerValue(new BigDecimal(value).toBigInteger());
            }
            return Int64Value.makeIntegerValue((long)value);
        case StandardNames.XS_UNSIGNED_LONG:
        case StandardNames.XS_UNSIGNED_INT:
        case StandardNames.XS_UNSIGNED_SHORT:
        case StandardNames.XS_UNSIGNED_BYTE:
        case StandardNames.XS_NON_POSITIVE_INTEGER:
        case StandardNames.XS_NEGATIVE_INTEGER:
        case StandardNames.XS_LONG:
        case StandardNames.XS_INT:
        case StandardNames.XS_SHORT:
        case StandardNames.XS_BYTE:
        case StandardNames.XS_NON_NEGATIVE_INTEGER:
        case StandardNames.XS_POSITIVE_INTEGER:
            ConversionResult iv = convertPrimitive(BuiltInAtomicType.INTEGER, validate, context);
            if (iv instanceof ValidationFailure) {
                return iv;
            }
            return ((IntegerValue)iv).convertPrimitive(requiredType, validate, context);   
        case StandardNames.XS_DECIMAL:
                try {
                    return new DecimalValue(value);
                } catch (ValidationException e) {
                    return new ValidationFailure(e);
                }
            case StandardNames.XS_DOUBLE:
            return new DoubleValue((double)value);
        case StandardNames.XS_STRING:
            return new StringValue(getStringValueCS());
        case StandardNames.XS_UNTYPED_ATOMIC:
            return new UntypedAtomicValue(getStringValueCS());
        default:
            ValidationFailure err = new ValidationFailure("Cannot convert float to " +
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
       return getStringValueCS().toString();
    }

    /**
    * Get the value as a String
    * @return a String representation of the value
    */

    public CharSequence getStringValueCS() {
        return floatToString(value); // , Float.toString(value));
    }

    /**
     * Get the canonical lexical representation as defined in XML Schema. This is not always the same
     * as the result of casting to a string according to the XPath rules. For xs:float, the canonical
     * representation always uses exponential notation.
     */

    public CharSequence getCanonicalLexicalRepresentation() {
        FastStringBuffer fsb = new FastStringBuffer(20);
        return FloatingPointConverter.appendFloatExponential(fsb, value);
    }

    /**
     * Regex indicating that a number may be worth rounding
     */

    static java.util.regex.Pattern roundablePattern =
            java.util.regex.Pattern.compile(
                    ".*99999.*|.*00000.*");


    /**
     * Internal method used for conversion of a float to a string
     * @param value the actual value
     * @return the value converted to a string, according to the XPath casting rules.
     */

    static CharSequence floatToString(float value) {
        return FloatingPointConverter.appendFloat(new FastStringBuffer(20), value);
    }

    /**
    * Negate the value
    */

    public NumericValue negate() {
        return new FloatValue(-value);
    }

    /**
    * Implement the XPath floor() function
    */

    public NumericValue floor() {
        return new FloatValue((float)Math.floor(value));
    }

    /**
    * Implement the XPath ceiling() function
    */

    public NumericValue ceiling() {
        return new FloatValue((float)Math.ceil(value));
    }

    /**
    * Implement the XPath round() function
    */

    public NumericValue round() {
        if (Float.isNaN(value)) return this;
        if (Float.isInfinite(value)) return this;
        if (value==0.0) return this;    // handles the negative zero case
        if (value >= -0.5 && value < 0.0) return new DoubleValue(-0.0);
        if (value > Integer.MIN_VALUE && value < Integer.MAX_VALUE) {
            return new FloatValue((float)Math.round(value));
        }

        // if the float is larger than the maximum int, then
        // it can't have any significant digits after the decimal
        // point, so return it unchanged

        return this;
    }

    /**
    * Implement the XPath round-to-half-even() function
    */

    public NumericValue roundHalfToEven(int scale) {
        try {
            return (FloatValue)
                    new DoubleValue((double)value).roundHalfToEven(scale).convertPrimitive(BuiltInAtomicType.FLOAT, true, null).asAtomic();
        } catch (XPathException err) {
            throw new AssertionError(err);
        }
    }

    /**
     * Determine whether the value is negative, zero, or positive
     * @return -1 if negative, 0 if zero (including negative zero), +1 if positive, NaN if NaN
     */

    public double signum() {
        if (Float.isNaN(value)) {
            return value;
        }
        if (value > 0) return 1;
        if (value == 0) return 0;
        return -1;
    }

    /**
    * Determine whether the value is a whole number, that is, whether it compares
    * equal to some integer
    */

    public boolean isWholeNumber() {
        return value == Math.floor(value) && !Float.isInfinite(value);
    }

    public int compareTo(Object other) {
        if (!(other instanceof NumericValue)) {
            throw new ClassCastException("Numeric values are not comparable to " + other.getClass());
        }
        if (other instanceof FloatValue) {
            float otherFloat = ((FloatValue)other).value;
            if (value == otherFloat) return 0;
            if (value < otherFloat) return -1;
            return +1;
        }
        if (other instanceof DoubleValue) {
            return super.compareTo(other);
        }
        try {
            return compareTo(((NumericValue)other).convertPrimitive(BuiltInAtomicType.FLOAT, true, null).asAtomic());
        } catch (XPathException err) {
            throw new ClassCastException("Operand of comparison cannot be promoted to xs:float");
        }
    }

    /**
     * Compare the value to a long
     * @param other the value to be compared with
     * @return -1 if this is less, 0 if this is equal, +1 if this is greater or if this is NaN
     */

    public int compareTo(long other) {
        float otherFloat = (float)other;
        if (value == otherFloat) return 0;
        if (value < otherFloat) return -1;
        return +1;
    }

    /**
     * Get an object that implements XML Schema comparison semantics
     */

    public Comparable getSchemaComparable() {
        return new Float(value);
    }    

    /**
    * Convert to Java object (for passing to external functions)
    */

//    public Object convertAtomicToJava(Class target, XPathContext context) throws XPathException {
//        if (target==Object.class) {
//            return new Double(value);
//        } else if (target.isAssignableFrom(DoubleValue.class)) {
//            return this;
//        } else if (target==double.class || target==Double.class) {
//            return new Double((double)value);
//        } else if (target==float.class ||target==Float.class ) {
//            return new Float(value);
//        } else if (target==long.class || target==Long.class) {
//            return new Long((long)value);
//        } else if (target==int.class || target==Integer.class) {
//            return new Integer((int)value);
//        } else if (target==short.class || target==Short.class) {
//            return new Short((short)value);
//        } else if (target==byte.class || target==Byte.class) {
//            return new Byte((byte)value);
//        } else if (target==char.class || target==Character.class) {
//            return new Character((char)value);
//        } else {
//            Object o = convertSequenceToJava(target, context);
//            if (o == null) {
//                XPathException err = new XPathException("Conversion of float to " + target.getName() +
//                        " is not supported");
//                err.setXPathContext(context);
//                err.setErrorCode(SaxonErrorCode.SXJE0004);
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
// The Original Code is: all this file.
//
// The Initial Developer of the Original Code is Michael H. Kay.
//
// Contributor(s): none.
//

