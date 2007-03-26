package org.orbeon.saxon.value;
import org.orbeon.saxon.Err;
import org.orbeon.saxon.expr.Token;
import org.orbeon.saxon.expr.XPathContext;
import org.orbeon.saxon.om.FastStringBuffer;
import org.orbeon.saxon.trans.DynamicError;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.trans.SaxonErrorCode;
import org.orbeon.saxon.type.*;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.regex.Matcher;

/**
* A numeric (double precision floating point) value
*/

public final class DoubleValue extends NumericValue {

    public static final DoubleValue ZERO = new DoubleValue(0.0);
    public static final DoubleValue ONE = new DoubleValue(1.0);
    public static final DoubleValue NaN = new DoubleValue(Double.NaN);

    private double value;

    /**
     * Constructor supplying a string
     * @throws ValidationException if the string does not have the correct lexical form for a double.
     * Note that the error will contain no error code or context information.
     */

    public DoubleValue(CharSequence val) throws ValidationException {
        try {
            value = Value.stringToNumber(val);
        } catch (NumberFormatException e) {
            throw new ValidationException("Cannot convert string " + Err.wrap(val, Err.VALUE) + " to a double");
        }
    }

    /**
    * Constructor supplying a double
    * @param value the value of the NumericValue
    */

    public DoubleValue(double value) {
        this.value = value;
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
     * @param context
     * @return the effective boolean value (true unless the value is zero or NaN)
     */
    public boolean effectiveBooleanValue(XPathContext context) {
        return (value!=0.0 && !Double.isNaN(value));
    }

    /**
    * Convert to target data type
    * @param requiredType an integer identifying the required atomic type
    * @param context
     * @return an AtomicValue, a value of the required type
    */

    public AtomicValue convertPrimitive(BuiltInAtomicType requiredType, boolean validate, XPathContext context) {
        switch(requiredType.getPrimitiveType()) {
        case Type.BOOLEAN:
            return BooleanValue.get(effectiveBooleanValue(null));
        case Type.DOUBLE:
        case Type.NUMBER:
        case Type.ANY_ATOMIC:
        case Type.ITEM:
            return this;
        case Type.INTEGER:
            if (Double.isNaN(value)) {
                ValidationException err = new ValidationException("Cannot convert double NaN to an integer");
                err.setErrorCode("FOCA0002");
                return new ValidationErrorValue(err);
            }
            if (Double.isInfinite(value)) {
                ValidationException err = new ValidationException("Cannot convert double infinity to an integer");
                //err.setXPathContext(context);
                err.setErrorCode("FOCA0002");
                return new ValidationErrorValue(err);
            }
            if (value > Long.MAX_VALUE || value < Long.MIN_VALUE) {
                return new BigIntegerValue(new BigDecimal(value).toBigInteger());
            }
            return new IntegerValue((long)value);
        case Type.DECIMAL:
                try {
                    return new DecimalValue(value);
                } catch (ValidationException e) {
                    return new ValidationErrorValue(e);
                }
            case Type.FLOAT:
            return new FloatValue((float)value);
        case Type.STRING:
            return new StringValue(getStringValueCS());
        case Type.UNTYPED_ATOMIC:
            return new UntypedAtomicValue(getStringValueCS());
        default:
            ValidationException err = new ValidationException("Cannot convert double to " +
                                     requiredType.getDisplayName());
            err.setErrorCode("XPTY0004");
            err.setIsTypeError(true);
            return new ValidationErrorValue(err);
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
     * Internal method used for conversion of a double to a string
     * @param value the actual value
     * @return the value converted to a string, according to the XPath casting rules.
     */

    public static CharSequence doubleToString(double value) {
        FastStringBuffer s = FloatingPointConverter.appendDouble(new FastStringBuffer(20), value);
        return s;
    }


    /**
     * Internal method used for conversion of a double to a string
     * @param value the actual value
     * @param javaString the result of converting the double to a string using the Java conventions.
     * This value is adjusted as necessary to cater for the differences between the Java and XPath rules.
     * @return the value converted to a string, according to the XPath casting rules.
     */

    public static CharSequence doubleToStringOLD(double value, String javaString) {
        if (value==0.0) {
            if (javaString.charAt(0) == '-') {
                return "-0";
            } else {
                return "0";
            }
        }
        if (Double.isInfinite(value)) {
            return (value > 0 ? "INF" : "-INF");
        }
        if (Double.isNaN(value)) {
            return "NaN";
        }
        final double absval = Math.abs(value);
        String s = javaString;
        if (absval < 1.0e-6 || absval >= 1.0e+6) {
            if (s.indexOf('E')<0) {
                // need to use scientific notation, but Java isn't using it
                // (Java's cutoff is 1.0E7, while XPath's is 1.0E6)
                // So we have for example -2000000.0 rather than -2.0e6
                FastStringBuffer sb = new FastStringBuffer(32);
                Matcher matcher = nonExponentialPattern.matcher(s);
                if (matcher.matches()) {
                    sb.append(matcher.group(1));
                    sb.append('.');
                    sb.append(matcher.group(2));
                    final String fraction = matcher.group(4);
                    if ("0".equals(fraction)) {
                        sb.append("E" + (matcher.group(2).length() + matcher.group(3).length()));
                        return sb;
                    } else {
                        sb.append(matcher.group(3));
                        sb.append(matcher.group(4));
                        sb.append("E" + (matcher.group(2).length() + matcher.group(3).length()));
                        return sb;
                    }
                } else {
                    // fallback, this shouldn't happen
                    return s;
                }
            } else {
                return s;
            }
        }
        int len = s.length();
        if (s.endsWith("E0")) {
            s = s.substring(0, len - 2);
        }
        if (s.endsWith(".0")) {
            return s.substring(0, len - 2);
        }
        int e = s.indexOf('E');
        if (e < 0) {
            // For some reason, Double.toString() in Java can return strings such as "0.0040"
            // so we remove any trailing zeros
            while (s.charAt(len - 1) == '0' && s.charAt(len - 2) != '.') {
                s = s.substring(0, --len);
            }
            return s;
        }
        int exp = Integer.parseInt(s.substring(e + 1));
        String sign;
        if (s.charAt(0) == '-') {
            sign = "-";
            s = s.substring(1);
            --e;
        } else {
            sign = "";
        }
        int nDigits = e - 2;
        if (exp >= nDigits) {
            return sign + s.substring(0, 1) + s.substring(2, e) + zeros(exp - nDigits);
        } else if (exp > 0) {
            return sign + s.substring(0, 1) + s.substring(2, 2 + exp) + '.' + s.substring(2 + exp, e);
        } else {
            while (s.charAt(e-1) == '0') e--;
            return sign + "0." + zeros(-1 - exp) + s.substring(0, 1) + s.substring(2, e);
        }
    }

    static String zeros(int n) {
        char[] buf = new char[n];
        for (int i = 0; i < n; i++)
            buf[i] = '0';
        return new String(buf);
    }

    /**
    * Determine the data type of the expression
    * @return Type.DOUBLE
     * @param th
     */

    public ItemType getItemType(TypeHierarchy th) {
        return Type.DOUBLE_TYPE;
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
        if (value > -0.5 && value < 0.0) {
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
            dec.setScale(scale, BigDecimal.ROUND_HALF_EVEN);
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
    * Evaluate a binary arithmetic operator.
    */

    public NumericValue arithmetic(int operator, NumericValue other, XPathContext context) throws XPathException {
        if (other instanceof DoubleValue) {
            switch(operator) {
                case Token.PLUS:
                    return new DoubleValue(value + ((DoubleValue)other).value);
                case Token.MINUS:
                    return new DoubleValue(value - ((DoubleValue)other).value);
                case Token.MULT:
                    return new DoubleValue(value * ((DoubleValue)other).value);
                case Token.DIV:
                    return new DoubleValue(value / ((DoubleValue)other).value);
                case Token.IDIV:
                    if (((DoubleValue)other).value == 0.0) {
                        DynamicError e = new DynamicError("Integer division by zero");
                        e.setErrorCode("FOAR0001");
                        e.setXPathContext(context);
                        throw e;
                    }
                    if (isNaN() || Double.isInfinite(value)) {
                        DynamicError e = new DynamicError("First operand of idiv is NaN or infinity");
                        e.setErrorCode("FOAR0002");
                        e.setXPathContext(context);
                        throw e;
                    }
                    if (other.isNaN()) {
                        DynamicError e = new DynamicError("Second operand of idiv is NaN");
                        e.setErrorCode("FOAR0002");
                        e.setXPathContext(context);
                        throw e;
                    }
                    return (NumericValue)(new DoubleValue(value / ((DoubleValue)other).value).convert(Type.INTEGER, context));
                case Token.MOD:
                    return new DoubleValue(value % ((DoubleValue)other).value);
                default:
                    throw new UnsupportedOperationException("Unknown operator");
            }
        } else {
            return arithmetic(operator, (DoubleValue)other.convert(Type.DOUBLE, context), context);
        }
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
    * Convert to Java object (for passing to external functions)
    */

    public Object convertToJava(Class target, XPathContext context) throws XPathException {
        if (target==Object.class) {
            return new Double(value);
        } else if (target.isAssignableFrom(DoubleValue.class)) {
            return this;
        } else if (target==boolean.class) {
            return Boolean.valueOf(effectiveBooleanValue(context));
        } else if (target==Boolean.class) {
            return Boolean.valueOf(effectiveBooleanValue(context));
        } else if (target==String.class || target==CharSequence.class) {
            return getStringValue();
        } else if (target==double.class || target==Double.class) {
            return new Double(value);
        } else if (target==float.class ||target==Float.class ) {
            return new Float(value);
        } else if (target==long.class || target==Long.class) {
            return new Long((long)value);
        } else if (target==int.class || target==Integer.class) {
            return new Integer((int)value);
        } else if (target==short.class || target==Short.class) {
            return new Short((short)value);
        } else if (target==byte.class || target==Byte.class) {
            return new Byte((byte)value);
        } else if (target==char.class || target==Character.class) {
            return new Character((char)value);
        } else {
            Object o = super.convertToJava(target, context);
            if (o == null) {
                DynamicError err = new DynamicError("Conversion of double to " + target.getName() +
                        " is not supported");
                err.setXPathContext(context);
                err.setErrorCode(SaxonErrorCode.SXJE0002);
            }
            return o;
        }
    }

    /**
     * Diagnostic method
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
            BigDecimal dec = new BigDecimal(m);
            if (exponent > 0) {
                dec = dec.multiply(new BigDecimal(BigInteger.valueOf(2).pow(exponent)));
            } else {
                dec = dec.divide(new BigDecimal(BigInteger.valueOf(2).pow(-exponent)), BigDecimal.ROUND_UNNECESSARY);
            }
            System.err.println("Exact value: " + (s>0?"":"-") + dec);
        }
    }

//    public static void main(String[] args) {
//        System.err.println("== 1.1 ==");
//        printInternalForm(1.1e0);
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
// Portions created by (xt) are Copyright (C) (James Clark). All Rights Reserved.
//
// Contributor(s): none.
//

