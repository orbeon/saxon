package net.sf.saxon.value;
import net.sf.saxon.Configuration;
import net.sf.saxon.Err;
import net.sf.saxon.expr.Token;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.style.StandardNames;
import net.sf.saxon.type.ItemType;
import net.sf.saxon.type.Type;
import net.sf.saxon.xpath.DynamicError;
import net.sf.saxon.xpath.XPathException;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.regex.Pattern;

/**
* A decimal value
*/

public final class DecimalValue extends NumericValue {

    private final static int DIVIDE_PRECISION = 18;

    private BigDecimal value;

    /**
    * Constructor supplying a BigDecimal
    * @param value the value of the DecimalValue
    */

    public DecimalValue(BigDecimal value) {
        this.value = value;
        loseTrailingZeros();
    }

    private static Pattern decimalPattern = Pattern.compile("(\\-|\\+)?((\\.[0-9]+)|([0-9]+(\\.[0-9]*)?))");

    /**
    * Constructor supplying a String
    * @param in the value of the DecimalValue
    */

    public DecimalValue(CharSequence in) throws XPathException {
        String trimmed = trimWhitespace(in).toString();
        try {
            if (!decimalPattern.matcher(trimmed).matches()) {
                DynamicError err = new DynamicError(
                        "Cannot convert string " + Err.wrap(trimmed, Err.VALUE) + " to decimal");
                err.setErrorCode("FORG0001");
                throw err;
            }
            this.value = new BigDecimal(trimmed);
            if (trimmed.charAt(trimmed.length()-1) == '0') {
                loseTrailingZeros();
            }
        } catch (NumberFormatException err) {
            DynamicError e = new DynamicError(
                    "Cannot convert string " + Err.wrap(trimmed, Err.VALUE) + " to decimal");
            e.setErrorCode("FORG0001");
            throw e;
        }
    }

    /**
    * Constructor supplying a double
    * @param in the value of the DecimalValue
    */

    public DecimalValue(double in) throws XPathException {
        try {
            this.value = new BigDecimal(in);
            loseTrailingZeros();
        } catch (NumberFormatException err) {
            // Must be a special value such as NaN or infinity
            DynamicError e = new DynamicError("Cannot convert double " + Err.wrap(in+"", Err.VALUE) + " to decimal");
            e.setErrorCode("FORG0001");
            throw e;
        }
    }

    /**
    * Constructor supplying a long integer
    * @param in the value of the DecimalValue
    */

    public DecimalValue(long in) {
        this.value = BigDecimal.valueOf(in);
    }

    /**
    * Remove insignificant trailing zeros (the Java BigDecimal class retains trailing zeros,
    * but the XPath 2.0 xs:decimal type does not)
    */

    private void loseTrailingZeros() {
        // this is an odd way to do it, but it seems the simplest method available
        if (value.scale() > 0) {
            String s = value.toString();
            int len = s.length();
            if (s.charAt(len-1) != '0') return;
            while (s.charAt(len-1) == '0') {
                len--;
            }
            value = new BigDecimal(s.substring(0, len));
        }
    }

    /**
    * Get the value
    */

    public BigDecimal getValue() {
        return value;
    }

    /**
     * Get the hashCode. This must conform to the rules for other NumericValue hashcodes
     * @see NumericValue#hashCode
     */

    public int hashCode() {
        BigDecimal round = value.setScale(0, BigDecimal.ROUND_DOWN);
        long value = round.longValue();
        if (value > Integer.MIN_VALUE && value < Integer.MAX_VALUE) {
            return (int)value;
        } else {
            return new Double(this.getDoubleValue()).hashCode();
        }
    }

    public boolean effectiveBooleanValue(XPathContext context) {
        return value.signum() != 0;
    }

    /**
    * Convert to target data type
    */

    public AtomicValue convert(int requiredType, XPathContext context) throws XPathException {
        switch(requiredType) {
        case Type.BOOLEAN:
                // 0.0 => false, anything else => true
            return BooleanValue.get(value.signum()!=0);
        case Type.NUMBER:
        case Type.DECIMAL:
        case Type.ATOMIC:
        case Type.ITEM:
            return this;
        case Type.INTEGER:
            return BigIntegerValue.makeValue(value.toBigInteger());
        case Type.DOUBLE:
            return new DoubleValue(value.doubleValue());
        case Type.FLOAT:
            return new FloatValue(value.floatValue());
        case Type.STRING:
            return new StringValue(getStringValue());
        case Type.UNTYPED_ATOMIC:
            return new UntypedAtomicValue(getStringValue());
        default:
            DynamicError err = new DynamicError("Cannot convert decimal to " +
                                     StandardNames.getDisplayName(requiredType));
            err.setXPathContext(context);
            err.setErrorCode("FORG0001");
            throw err;
        }
    }

    /**
    * Get the value as a String
    * @return a String representation of the value
    */

    public String getStringValue() {
        String s = value.toString();
        //if (value.scale()==0) {
        //    s += ".0";
        //}
        return s;
    }

    /**
    * Determine the data type of the expression
    * @return Type.DECIMAL
    */

    public ItemType getItemType() {
        return Type.DECIMAL_TYPE;
    }

    /**
    * Negate the value
    */

    public NumericValue negate() {
        return new DecimalValue(value.negate());
    }

    /**
    * Implement the XPath floor() function
    */

    public NumericValue floor() {
        return new DecimalValue(value.setScale(0, BigDecimal.ROUND_FLOOR));
    }

    /**
    * Implement the XPath ceiling() function
    */

    public NumericValue ceiling() {
        return new DecimalValue(value.setScale(0, BigDecimal.ROUND_CEILING));
    }

    /**
    * Implement the XPath round() function
    */

    public NumericValue round() {
        // The XPath rules say that we should round to the nearest integer, with .5 rounding towards
        // positive infinity. Unfortunately this is not one of the rounding modes that the Java BigDecimal
        // class supports.

        // If the value is positive, we use ROUND_HALF_UP; if it is negative, we use ROUND_HALF_DOWN (here "UP"
        // means "away from zero")

        switch (value.signum()) {
            case -1:
                return new DecimalValue(value.setScale(0, BigDecimal.ROUND_HALF_DOWN));
            case 0:
                return this;
            case +1:
                return new DecimalValue(value.setScale(0, BigDecimal.ROUND_HALF_UP));
            default:
                // can't happen
                return this;
        }

    }

    /**
    * Implement the XPath round-to-half-even() function
    */

    public NumericValue roundToHalfEven(int scale) {
        if (scale<0) {
            try {
                AtomicValue val = convert(Type.INTEGER, null);
                if (val instanceof IntegerValue) {
                    return ((IntegerValue)val).roundToHalfEven(scale);
                } else {
                    return ((BigIntegerValue)val).roundToHalfEven(scale);
                }
            } catch (XPathException err) {
                throw new IllegalArgumentException("internal error in integer-decimal conversion");
            }
        } else {
            return new DecimalValue(value.setScale(scale, BigDecimal.ROUND_HALF_EVEN));
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
    */

    public boolean isWholeNumber() {
        return value.scale()==0 ||
               value.equals(value.setScale(0, BigDecimal.ROUND_DOWN));
    }

    /**
    * Evaluate a binary arithmetic operator.
    */

    public NumericValue arithmetic(int operator, NumericValue other, XPathContext context) throws XPathException {
        if (other instanceof DecimalValue) {
            try {
                switch(operator) {
                    case Token.PLUS:
                        return new DecimalValue(value.add(((DecimalValue)other).value));
                    case Token.MINUS:
                        return new DecimalValue(value.subtract(((DecimalValue)other).value));
                    case Token.MULT:
                        return new DecimalValue(value.multiply(((DecimalValue)other).value));
                    case Token.DIV:
                        int scale = Math.max(DIVIDE_PRECISION,
                                             Math.max(value.scale(), ((DecimalValue)other).value.scale()));
                        //int scale = value.scale() + ((DecimalValue)other).value.scale() + DIVIDE_PRECISION;
                        BigDecimal result = value.divide(((DecimalValue)other).value, scale, BigDecimal.ROUND_HALF_DOWN);
                        return new DecimalValue(result);
                    case Token.IDIV:
                        BigInteger quot = value.divide(((DecimalValue)other).value, 0, BigDecimal.ROUND_DOWN).toBigInteger();
                        return BigIntegerValue.makeValue(quot);
                    case Token.MOD:
                        //BigDecimal quotient = value.divide(((DecimalValue)other).value, ((DecimalValue)other).value.scale(), BigDecimal.ROUND_DOWN);
                        BigDecimal quotient = value.divide(((DecimalValue)other).value, 0, BigDecimal.ROUND_DOWN);
                        BigDecimal remainder = value.subtract(quotient.multiply(((DecimalValue)other).value));
                        return new DecimalValue(remainder);
                    default:
                        throw new AssertionError("Unknown operator");
                }
            } catch (ArithmeticException err) {
                throw new DynamicError(err);
            }
        } else if (NumericValue.isInteger(other)) {
            return arithmetic(operator, (DecimalValue)other.convert(Type.DECIMAL, context), context);
        } else {
            NumericValue n = (NumericValue)convert(other.getItemType().getPrimitiveType(), context);
            return n.arithmetic(operator, other, context);
        }
    }

    /**
    * Compare the value to another numeric value
    */

    public int compareTo(Object other) {
        if ((NumericValue.isInteger((NumericValue)other))) {
            // deliberately triggers a ClassCastException if other value is the wrong type
            try {
                return compareTo(((NumericValue)other).convert(Type.DECIMAL, null));
            } catch (XPathException err) {
                throw new AssertionError("Conversion of integer to decimal should never fail");
            }
        } else if (other instanceof BigIntegerValue) {
            return value.compareTo(((BigIntegerValue)other).asDecimal());
        } else if (other instanceof DecimalValue) {
            return value.compareTo(((DecimalValue)other).value);
        } else {
            return super.compareTo(other);
        }
    }

    /**
    * Convert to Java object (for passing to external functions)
    */

    public Object convertToJava(Class target, Configuration config, XPathContext context) throws XPathException {
        if (target==Object.class || target.isAssignableFrom(BigDecimal.class)) {
            return value;
        } else if (target.isAssignableFrom(DecimalValue.class)) {
            return this;
        } else if (target==boolean.class) {
            BooleanValue bval = (BooleanValue)convert(Type.BOOLEAN, null);
            return Boolean.valueOf(bval.getBooleanValue());
        } else if (target==Boolean.class) {
            BooleanValue bval = (BooleanValue)convert(Type.BOOLEAN, null);
            return Boolean.valueOf(bval.getBooleanValue());
        } else if (target==String.class || target==CharSequence.class) {
            return getStringValue();
        } else if (target==double.class || target==Double.class) {
            return new Double(value.doubleValue());
        } else if (target==float.class || target==Float.class) {
            return new Float(value.floatValue());
        } else if (target==long.class || target==Long.class) {
            return new Long(value.longValue());
        } else if (target==int.class || target==Integer.class) {
            return new Integer(value.intValue());
        } else if (target==short.class || target==Short.class) {
            return new Short(value.shortValue());
        } else if (target==byte.class || target==Byte.class) {
            return new Byte(value.byteValue());
        } else if (target==char.class || target==Character.class) {
            return new Character((char)value.intValue());
        } else {
            Object o = super.convertToJava(target, config, context);
            if (o == null) {
                throw new DynamicError("Conversion of decimal to " + target.getName() +
                        " is not supported");
            }
            return o;
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
// The Original Code is: all this file except the asStringXT() and zeros() methods (not currently used).
//
// The Initial Developer of the Original Code is Michael H. Kay.
//
// Portions created by (xt) are Copyright (C) (James Clark). All Rights Reserved.
//
// Contributor(s): none.
//

