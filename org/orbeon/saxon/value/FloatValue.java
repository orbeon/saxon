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

/**
* A numeric (single precision floating point) value
*/

public final class FloatValue extends NumericValue {
    private float value;

    /**
     * Constructor supplying a string
     */

    public FloatValue(CharSequence val) throws DynamicError {
        try {
            this.value = (float)Value.stringToNumber(val);
        } catch (NumberFormatException e) {
            throw new DynamicError("Cannot convert string " + Err.wrap(val, Err.VALUE) + " to a float");
        }
    }

    /**
    * Constructor supplying a float
    * @param value the value of the float
    */

    public FloatValue(float value) {
        this.value = value;
    }

    /**
    * Get the value
    */

    public float getValue() {
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
            return new Double(this.getDoubleValue()).hashCode();
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
     * @param context
     * @return true unless the value is zero or NaN
     */
    public boolean effectiveBooleanValue(XPathContext context) {
        return (value!=0.0 && !Float.isNaN(value));
    }

    /**
    * Convert to target data type
    * @param requiredType an integer identifying the required atomic type
    * @return an AtomicValue, a value of the required type
    * @throws XPathException if the conversion is not possible
    */

    public AtomicValue convert(int requiredType, XPathContext context) throws XPathException {
        switch(requiredType) {
        case Type.BOOLEAN:
            return BooleanValue.get(value!=0.0 && !Float.isNaN(value));
        case Type.FLOAT:
        case Type.NUMBER:
        case Type.ATOMIC:
        case Type.ITEM:
            return this;
        case Type.INTEGER:
            if (Float.isNaN(value)) {
                DynamicError err = new DynamicError("Cannot convert float NaN to an integer");
                err.setErrorCode("FORG0001");
                err.setXPathContext(context);
                throw err;
            }
            if (Float.isInfinite(value)) {
                DynamicError err = new DynamicError("Cannot convert float infinity to an integer");
                err.setErrorCode("FORG0001");
                err.setXPathContext(context);
                throw err;
            }
            if (value > Long.MAX_VALUE || value < Long.MIN_VALUE) {
                return new BigIntegerValue(new BigDecimal(value).toBigInteger());
            }
            return new IntegerValue((long)value);
        case Type.DECIMAL:
            return new DecimalValue(value);
        case Type.DOUBLE:
            return new DoubleValue((double)value);
        case Type.STRING:
            return new StringValue(getStringValue());
        case Type.UNTYPED_ATOMIC:
            return new UntypedAtomicValue(getStringValue());
        default:
            DynamicError err = new DynamicError("Cannot convert float to " +
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
        // TODO: check whether the Java and XMLSchema specs give the same answer
        if (value == Float.POSITIVE_INFINITY) {
            return "INF";
        } else if (value == Float.NEGATIVE_INFINITY) {
            return "-INF";
        } else {
            return "" + value;
        }
    }

    /**
    * Determine the data type of the expression
    * @return Type.DOUBLE
    */

    public ItemType getItemType() {
        return Type.FLOAT_TYPE;
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
        if (value > -0.5 && value < 0.0) return new DoubleValue(-0.0);
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

    public NumericValue roundToHalfEven(int scale) {
        try {
            return (FloatValue)new DoubleValue((double)value).roundToHalfEven(scale).convert(Type.FLOAT, null);
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
        return value == Math.floor(value);
    }

    /**
    * Evaluate a binary arithmetic operator.
    */

    public NumericValue arithmetic(int operator, NumericValue other, XPathContext context) throws XPathException {
        if (other instanceof FloatValue) {
            switch(operator) {
                case Token.PLUS:
                    return new FloatValue(value + ((FloatValue)other).value);
                case Token.MINUS:
                    return new FloatValue(value - ((FloatValue)other).value);
                case Token.MULT:
                    return new FloatValue(value * ((FloatValue)other).value);
                case Token.DIV:
                    return new FloatValue(value / ((FloatValue)other).value);
                case Token.IDIV:
                    return (NumericValue)(new FloatValue(value / ((FloatValue)other).value).convert(Type.INTEGER, context));
                case Token.MOD:
                    return new FloatValue(value % ((FloatValue)other).value);
                default:
                    throw new UnsupportedOperationException("Unknown operator");
            }
        } else if (other instanceof DoubleValue) {
            return ((DoubleValue)convert(Type.DOUBLE, context)).arithmetic(operator, other, context);
        } else {
            return arithmetic(operator, (FloatValue)other.convert(Type.FLOAT, context), context);
        }
    }

    /**
    * Convert to Java object (for passing to external functions)
    */

    public Object convertToJava(Class target, Configuration config, XPathContext context) throws XPathException {
        if (target==Object.class) {
            return new Double(value);
        } else if (target.isAssignableFrom(DoubleValue.class)) {
            return this;
        } else if (target==boolean.class || target==Boolean.class) {
            return Boolean.valueOf(value != 0.0 && !Float.isNaN(value));
        } else if (target==String.class || target==CharSequence.class) {
            return getStringValue();
        } else if (target==double.class || target==Double.class) {
            return new Double((double)value);
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
            Object o = super.convertToJava(target, config, context);
            if (o == null) {
                DynamicError err = new DynamicError("Conversion of float to " + target.getName() +
                        " is not supported");
                err.setXPathContext(context);
                err.setErrorCode("SAXON:0000");
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
// The Original Code is: all this file.
//
// The Initial Developer of the Original Code is Michael H. Kay.
//
// Contributor(s): none.
//

