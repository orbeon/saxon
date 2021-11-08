package org.orbeon.saxon.value;
import org.orbeon.saxon.trans.Err;
import org.orbeon.saxon.expr.XPathContext;
import org.orbeon.saxon.om.FastStringBuffer;
import org.orbeon.saxon.om.StandardNames;
import org.orbeon.saxon.type.*;

import java.math.BigDecimal;
import java.math.BigInteger;

/**
* A numeric (double precision floating point) value
*/

public final class DoubleValue extends NumericValue {

    public static final DoubleValue ZERO = new DoubleValue(0.0);
    public static final DoubleValue NEGATIVE_ZERO = new DoubleValue(-0.0);
    public static final DoubleValue ONE = new DoubleValue(1.0);
    public static final DoubleValue NaN = new DoubleValue(Double.NaN);

    private double value;

    /**
     * Constructor supplying a string
     * @param val the string representation of the double value, conforming to the XML Schema lexical
     * representation of xs:double, with leading and trailing spaces permitted
     * @throws ValidationException if the string does not have the correct lexical form for a double.
     * Note that the error will contain no error code or context information.
     */

    public DoubleValue(CharSequence val) throws ValidationException {
        try {
            value = Value.stringToNumber(val);
        } catch (NumberFormatException e) {
            throw new ValidationException("Cannot convert string " + Err.wrap(val, Err.VALUE) + " to a double");
        }
        typeLabel = BuiltInAtomicType.DOUBLE;
    }

    /**
    * Constructor supplying a double
    * @param value the value of the NumericValue
    */

    public DoubleValue(double value) {
        this.value = value;
        typeLabel = BuiltInAtomicType.DOUBLE;
    }

    /**
     * Constructor supplying a double and an AtomicType, for creating
     * a value that belongs to a user-defined subtype of xs:double. It is
     * the caller's responsibility to ensure that the supplied value conforms
     * to the supplied type.
     * @param value the value of the NumericValue
     * @param type the type of the value. This must be a subtype of xs:double, and the
     * value must conform to this type. The methosd does not check these conditions.
     */

    public DoubleValue(double value, AtomicType type) {
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
        DoubleValue v = new DoubleValue(value);
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
        return BuiltInAtomicType.DOUBLE;
    }

    /**
     * Return this numeric value as a double
     * @return the value as a double
     */

    public double getDoubleValue() {
        return value;
    }

    /**
     * Get the hashCode. This must conform to the rules for other NumericValue hashcodes
     * @see NumericValue#hashCode
     */

    public int hashCode() {
        if (value > Integer.MIN_VALUE && value < Integer.MAX_VALUE) {
            return (int)value;
        } else {
            return new Double(value).hashCode();
        }
    }

    /**
     * Test whether the value is the double/float value NaN
     */

    public boolean isNaN() {
        return Double.isNaN(value);
    }

    /**
     * Get the effective boolean value
     * @return the effective boolean value (true unless the value is zero or NaN)
     */
    public boolean effectiveBooleanValue() {
        return (value!=0.0 && !Double.isNaN(value));
    }

    /**
     * Convert to target data type
     * @param requiredType an integer identifying the required atomic type
     * @param validate true if the supplied value must be validated, false if the caller warrants that it is
     * valid
     * @param context the XPath dynamic context
     * @return an AtomicValue, a value of the required type
    */

    public ConversionResult convertPrimitive(BuiltInAtomicType requiredType, boolean validate, XPathContext context) {
        switch(requiredType.getFingerprint()) {
        case StandardNames.XS_BOOLEAN:
            return BooleanValue.get(effectiveBooleanValue());
        case StandardNames.XS_DOUBLE:
        case StandardNames.XS_NUMERIC:
        case StandardNames.XS_ANY_ATOMIC_TYPE:
            return this;
        case StandardNames.XS_INTEGER:
            if (Double.isNaN(value)) {
                ValidationFailure err = new ValidationFailure("Cannot convert double NaN to an integer");
                err.setErrorCode("FOCA0002");
                return err;
            }
            if (Double.isInfinite(value)) {
                ValidationFailure err = new ValidationFailure("Cannot convert double INF to an integer");
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
            case StandardNames.XS_FLOAT:
            return new FloatValue((float)value);
        case StandardNames.XS_STRING:
            return new StringValue(getStringValueCS());
        case StandardNames.XS_UNTYPED_ATOMIC:
            return new UntypedAtomicValue(getStringValueCS());
        default:
            ValidationFailure err = new ValidationFailure("Cannot convert double to " +
                                     requiredType.getDisplayName());
            err.setErrorCode("XPTY0004");
            return err;
        }
    }

    static java.util.regex.Pattern nonExponentialPattern =
            java.util.regex.Pattern.compile(
                    "(-?[0-9])([0-9]+?)(0*)\\.([0-9]*)");

    /**
     * Convert the double to a string according to the XPath 2.0 rules
     * @return the string value
     */
    public String getStringValue() {
        return doubleToString(value).toString(); //, Double.toString(value)).toString();
    }

    /**
     * Convert the double to a string according to the XPath 2.0 rules
     * @return the string value
     */
    public CharSequence getStringValueCS() {
        return doubleToString(value); //, Double.toString(value));
    }

    /**
     * Get the canonical lexical representation as defined in XML Schema. This is not always the same
     * as the result of casting to a string according to the XPath rules. For xs:double, the canonical
     * representation always uses exponential notation.
     */

    public CharSequence getCanonicalLexicalRepresentation() {
        FastStringBuffer fsb = new FastStringBuffer(20);
        return FloatingPointConverter.appendDoubleExponential(fsb, value);
    }

    /**
     * Internal method used for conversion of a double to a string
     * @param value the actual value
     * @return the value converted to a string, according to the XPath casting rules.
     */

    public static CharSequence doubleToString(double value) {
        return FloatingPointConverter.appendDouble(new FastStringBuffer(20), value);
    }


    /**
    * Negate the value
    */

    public NumericValue negate() {
        return new DoubleValue(-value);
    }

    /**
    * Implement the XPath floor() function
    */

    public NumericValue floor() {
        return new DoubleValue(Math.floor(value));
    }

    /**
    * Implement the XPath ceiling() function
    */

    public NumericValue ceiling() {
        return new DoubleValue(Math.ceil(value));
    }

    /**
    * Implement the XPath round() function
    */

    public NumericValue round() {
        if (Double.isNaN(value)) {
            return this;
        }
        if (Double.isInfinite(value)) {
            return this;
        }
        if (value == 0.0) {
            return this;    // handles the negative zero case
        }
        if (value >= -0.5 && value < 0.0) {
            return new DoubleValue(-0.0);
        }
        if (value > Long.MIN_VALUE && value < Long.MAX_VALUE) {
            return new DoubleValue(Math.round(value));
        }

        // A double holds fewer significant digits than a long. Therefore,
        // if the double is outside the range of a long, it cannot have
        // any signficant digits after the decimal point. So in this
        // case, we return the original value unchanged

        return this;
    }

    /**
    * Implement the XPath round-to-half-even() function
    */

    public NumericValue roundHalfToEven(int scale) {
        if (Double.isNaN(value)) return this;
        if (Double.isInfinite(value)) return this;
        if (value==0.0) return this;    // handles the negative zero case

        // Convert to a scaled integer, by multiplying by 10^scale

        double factor = Math.pow(10, scale+1);
        double d = Math.abs(value * factor);

        if (Double.isInfinite(d)) {
            // double arithmetic has overflowed - do it in decimal
            BigDecimal dec = new BigDecimal(value);
            dec = dec.setScale(scale, BigDecimal.ROUND_HALF_EVEN);
            return new DoubleValue(dec.doubleValue());
        }

        // Now apply any rounding needed, using the "round half to even" rule

        double rem = d % 10;
        if (rem > 5) {
            d += (10-rem);
        } else if (rem < 5){
            d -= rem;
        } else {
            // round half to even - check the last bit
            if ((d % 20) == 15) {
                d +=5 ;
            } else {
                d -=5;
            }
        }

        // Now convert back to the original magnitude

        d /= factor;
        if (value < 0) {
            d = 0.0 -d;
        }
        return new DoubleValue(d);

    }

    /**
     * Determine whether the value is negative, zero, or positive
     * @return -1 if negative, 0 if zero (including negative zero), +1 if positive, NaN if NaN
     */

    public double signum() {
        if (Double.isNaN(value)) {
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
        return value == Math.floor(value) && !Double.isInfinite(value);
    }

    /**
     * Compare the value to a long.
     * @param other the value to be compared with
     * @return -1 if this is less, 0 if this is equal, +1 if this is greater or if this is NaN
     */

    public int compareTo(long other) {
        double otherDouble = (double)other;
        if (value == otherDouble) return 0;
        if (value < otherDouble) return -1;
        return +1;
    }

    /**
     * Get an object that implements XML Schema comparison semantics
     */

    public Comparable getSchemaComparable() {
        return new Double(value);
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
//            return new Double(value);
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
//                XPathException err = new XPathException("Conversion of double to " + target.getName() +
//                        " is not supported");
//                err.setXPathContext(context);
//                err.setErrorCode(SaxonErrorCode.SXJE0002);
//            }
//            return o;
//        }
//    }

    /**
     * Diagnostic method: print the sign, exponent, and significand
     * @param d the double to be diagnosed
     */

    public static void printInternalForm(double d) {
        System.err.println("==== Double " + d + " ====");
        long bits = Double.doubleToLongBits(d);
        System.err.println("Internal form: " + Long.toHexString(bits));
        if (bits == 0x7ff0000000000000L) {
            System.err.println("+Infinity");
        } else if (bits == 0xfff0000000000000L) {
            System.err.println("-Infinity");
        } else if (bits == 0x7ff8000000000000L) {
            System.err.println("NaN");
        } else {
            int s = ((bits >> 63) == 0) ? 1 : -1;
            int e = (int)((bits >> 52) & 0x7ffL);
            long m = (e == 0) ?
                             (bits & 0xfffffffffffffL) << 1 :
                             (bits & 0xfffffffffffffL) | 0x10000000000000L;
            int exponent = e-1075;
            System.err.println("Sign: " + s);
            System.err.println("Exponent: " + exponent);
            System.err.println("Significand: " + m);
            BigDecimal dec = BigDecimal.valueOf(m);
            if (exponent > 0) {
                dec = dec.multiply(new BigDecimal(BigInteger.valueOf(2).pow(exponent)));
            } else {
                // Next line is sometimes failing, e.g. on -3.62e-5. Not investigated.
                dec = dec.divide(new BigDecimal(BigInteger.valueOf(2).pow(-exponent)), BigDecimal.ROUND_HALF_EVEN);
            }
            System.err.println("Exact value: " + (s>0?"":"-") + dec);
        }
    }

//    public static void main(String[] args) {
//        System.err.println("-3.62E-5");
//        printInternalForm(-3.62E-5);
//    }
//        System.err.println("(next below)");
//        printInternalForm(Double.longBitsToDouble(Double.doubleToLongBits(1.1e0)-1));
//        System.err.println("(next above)");
//        printInternalForm(Double.longBitsToDouble(Double.doubleToLongBits(1.1e0)+1));
//        System.err.println("== 2.2 ==");
//        printInternalForm(2.2e0);
//        System.err.println("(next below)");
//        printInternalForm(Double.longBitsToDouble(Double.doubleToLongBits(2.2e0)-1));
//        System.err.println("(next above)");
//        printInternalForm(Double.longBitsToDouble(Double.doubleToLongBits(2.2e0)+1));
//
//        System.err.println("== 3.3 ==");
//        printInternalForm(3.3e0);
//        System.err.println("(next below)");
//        printInternalForm(Double.longBitsToDouble(Double.doubleToLongBits(3.3e0)-1));
//        System.err.println("(next above)");
//        printInternalForm(Double.longBitsToDouble(Double.doubleToLongBits(3.3e0)+1));
//
//
//        System.err.println("== 1.1 + 2.2 ==");
//        printInternalForm(1.1e0 + 2.2e0);
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

