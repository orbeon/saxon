package net.sf.saxon.value;

import net.sf.saxon.Configuration;
import net.sf.saxon.Err;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.om.Item;
import net.sf.saxon.om.SequenceIterator;
import net.sf.saxon.style.StandardNames;
import net.sf.saxon.type.AtomicType;
import net.sf.saxon.type.BuiltInSchemaFactory;
import net.sf.saxon.type.ItemType;
import net.sf.saxon.type.Type;
import net.sf.saxon.xpath.DynamicError;
import net.sf.saxon.xpath.XPathException;


/**
 * An atomic value of type xs:string
 */

public class StringValue extends AtomicValue {

    public static final StringValue EMPTY_STRING = new StringValue("");

    // We hold the value as a CharSequence (it may be a StringBuffer rather than a string)
    // But the first time this is converted to a string, we keep it as a string

    protected CharSequence value;     // may be zero-length, will never be null
    protected int length = -1;  // the length in XML characters - not necessarily the same as the Java length

    /**
     * Protected constructor for use by subtypes
     */

    protected StringValue() {
        value = "";
    }

    /**
     * Constructor. Note that although a StringValue may wrap any kind of CharSequence
     * (usually a String, but it can also be, for example, a StringBuffer), the caller
     * is responsible for ensuring that the value is immutable.
     * @param value the String value. Null is taken as equivalent to "".
     */

    public StringValue(CharSequence value) {
        this.value = (value == null ? "" : value);
    }

    /**
     * Get the string value as a String
     */

    public final String getStringValue() {
        return (String) (value = value.toString());
    }

    /**
     * Convert to target data type
     * @param requiredType an integer identifying the required atomic type
     * @return an AtomicValue, a value of the required type
     * @throws XPathException if the conversion is not possible
     */

    public AtomicValue convert(int requiredType, XPathContext context) throws XPathException {

        try {
            switch (requiredType) {
                case Type.BOOLEAN: {
                    String val = trimWhitespace(value).toString();
                    if ("0".equals(val) || "false".equals(val)) {
                        return BooleanValue.FALSE;
                    } else if ("1".equals(val) || "true".equals(val)) {
                        return BooleanValue.TRUE;
                    } else {
                        DynamicError err = new DynamicError(
                                "The string " + Err.wrap(val, Err.VALUE) + " cannot be cast to a boolean");
                        err.setXPathContext(context);
                        err.setErrorCode("FORG0001");
                        throw err;
                    }
                }
                case Type.NUMBER:
                case Type.DOUBLE:
                    return new DoubleValue(value);
                case Type.INTEGER:
                    return IntegerValue.stringToInteger(value);

                case Type.NON_POSITIVE_INTEGER:
                case Type.NEGATIVE_INTEGER:
                case Type.LONG:
                case Type.INT:
                case Type.SHORT:
                case Type.BYTE:
                case Type.NON_NEGATIVE_INTEGER:
                case Type.POSITIVE_INTEGER:
                case Type.UNSIGNED_LONG:
                case Type.UNSIGNED_INT:
                case Type.UNSIGNED_SHORT:
                case Type.UNSIGNED_BYTE:
                    NumericValue iv = IntegerValue.stringToInteger(value);
                    AtomicType subtype = (AtomicType) BuiltInSchemaFactory.getSchemaType(requiredType);
                    if (iv instanceof IntegerValue) {
                        ((IntegerValue)iv).setSubType(subtype);
                        return iv;
                    } else {
                        return DerivedAtomicValue.makeValue(iv, value.toString(), subtype, true);
                    }
                case Type.DECIMAL:
                    return new DecimalValue(value);
                case Type.FLOAT:
                    return new FloatValue(value);
                case Type.DATE:
                    return new DateValue(value);
                case Type.DATE_TIME:
                    return new DateTimeValue(value);
                case Type.TIME:
                    return new TimeValue(value);
                case Type.G_YEAR:
                    return new GYearValue(value);
                case Type.G_YEAR_MONTH:
                    return new GYearMonthValue(value);
                case Type.G_MONTH:
                    return new GMonthValue(value);
                case Type.G_MONTH_DAY:
                    return new GMonthDayValue(value);
                case Type.G_DAY:
                    return new GDayValue(value);
                case Type.DURATION:
                    return new DurationValue(value);
                case Type.YEAR_MONTH_DURATION:
                    return new MonthDurationValue(value);
                case Type.DAY_TIME_DURATION:
                    return new SecondsDurationValue(value);
                case Type.UNTYPED_ATOMIC:
                case Type.ANY_SIMPLE_TYPE:
                    return new UntypedAtomicValue(value);
                case Type.STRING:
                case Type.ATOMIC:
                case Type.ITEM:
                    return this;
                case Type.NORMALIZED_STRING:
                case Type.TOKEN:
                case Type.LANGUAGE:
                case Type.NAME:
                case Type.NCNAME:
                case Type.ID:
                case Type.IDREF:
                case Type.ENTITY:
                case Type.NMTOKEN:
                    return new RestrictedStringValue(value, requiredType);
                case Type.ANY_URI:
                    return new AnyURIValue(value);
                case Type.HEX_BINARY:
                    return new HexBinaryValue(value);
                case Type.BASE64_BINARY:
                    return new Base64BinaryValue(value);
                default:
                    throw new DynamicError("Cannot convert string to " + StandardNames.getDisplayName(requiredType));
            }
        } catch (DynamicError err) {
            err.setXPathContext(context);
            err.setErrorCode("FORG0001");
            throw err;
        }
    }

    /**
     * Return the type of the expression
     * @return Type.STRING (always)
     */

    public ItemType getItemType() {
        return Type.STRING_TYPE;
    }

    /**
     * Get the length of this string, as defined in XPath. This is not the same as the Java length,
     * as a Unicode surrogate pair counts as a single character
     */

    public int getLength() {
        // memo function; only compute it the first time
        if (length == -1) {
            length = getLength(value);
        }
        return length;
    }

    /**
     * Get the length of a string, as defined in XPath. This is not the same as the Java length,
     * as a Unicode surrogate pair counts as a single character.
     * @param s The string whose length is required
     */

    public static int getLength(CharSequence s) {
        int n = 0;
        for (int i = 0; i < s.length(); i++) {
            int c = (int) s.charAt(i);
            if (c < 55296 || c > 56319) n++;    // don't count high surrogates, i.e. D800 to DBFF
        }
        return n;
    }

    /**
     * Iterate over a string, returning a sequence of integers representing the Unicode code-point values
     */

    public SequenceIterator iterateCharacters() {
        return new CharacterIterator();
    }

    /**
     * Expand a string containing surrogate pairs into an array of 32-bit characters
     */

    public static int[] expand(CharSequence s) {
        int[] array = new int[getLength(s)];
        int o = 0;
        for (int i = 0; i < s.length(); i++) {
            int charval;
            int c = s.charAt(i);
            if (c >= 55296 && c <= 56319) {
                // we'll trust the data to be sound
                charval = ((c - 55296) * 1024) + ((int) s.charAt(i + 1) - 56320) + 65536;
                i++;
            } else {
                charval = c;
            }
            array[o++] = charval;
        }
        return array;
    }

    /**
     * Determine if two StringValues are equal
     * @throws ClassCastException if the values are not comparable
     */

    public boolean equals(Object other) {
        // Force a ClassCastException if the other value isn't a string or derived from string
        StringValue otherVal = (StringValue) ((AtomicValue) other).getPrimitiveValue();
        // cannot use equals() directly on two unlike CharSequences
        return getStringValue().equals(otherVal.getStringValue());
    }

    public int hashCode() {
        return getStringValue().hashCode();
    }

    public boolean effectiveBooleanValue(XPathContext context) {
        return value.length() > 0;
    }


    /**
     * Convert to Java object (for passing to external functions)
     */

    public Object convertToJava(Class target, Configuration config, XPathContext context) throws XPathException {
        if (target == Object.class) {
            return value;
        } else if (target.isAssignableFrom(StringValue.class)) {
            return this;
        } else if (target == String.class || target == CharSequence.class) {
            return getStringValue();
        } else if (target == boolean.class) {
            BooleanValue bval = (BooleanValue) convert(Type.BOOLEAN, context);
            return Boolean.valueOf(bval.getBooleanValue());
        } else if (target == Boolean.class) {
            BooleanValue bval = (BooleanValue) convert(Type.BOOLEAN, context);
            return Boolean.valueOf(bval.getBooleanValue());
        } else if (target == double.class) {
            DoubleValue dval = (DoubleValue) convert(Type.DOUBLE, context);
            return new Double(dval.getDoubleValue());
        } else if (target == Double.class) {
            DoubleValue dval = (DoubleValue) convert(Type.DOUBLE, context);
            return new Double(dval.getDoubleValue());
        } else if (target == float.class) {
            DoubleValue dval = (DoubleValue) convert(Type.DOUBLE, context);
            return new Float(dval.getDoubleValue());
        } else if (target == Float.class) {
            DoubleValue dval = (DoubleValue) convert(Type.DOUBLE, context);
            return new Float(dval.getDoubleValue());
        } else if (target == long.class) {
            IntegerValue dval = (IntegerValue) convert(Type.INTEGER, context);
            return new Long(dval.longValue());
        } else if (target == Long.class) {
            IntegerValue dval = (IntegerValue) convert(Type.INTEGER, context);
            return new Long(dval.longValue());
        } else if (target == int.class) {
            IntegerValue dval = (IntegerValue) convert(Type.INTEGER, context);
            return new Integer((int) dval.longValue());
        } else if (target == Integer.class) {
            IntegerValue dval = (IntegerValue) convert(Type.INTEGER, context);
            return new Integer((int) dval.longValue());
        } else if (target == short.class) {
            IntegerValue dval = (IntegerValue) convert(Type.INTEGER, context);
            return new Short((short) dval.longValue());
        } else if (target == Short.class) {
            IntegerValue dval = (IntegerValue) convert(Type.INTEGER, context);
            return new Short((short) dval.longValue());
        } else if (target == byte.class) {
            IntegerValue dval = (IntegerValue) convert(Type.INTEGER, context);
            return new Byte((byte) dval.longValue());
        } else if (target == Byte.class) {
            IntegerValue dval = (IntegerValue) convert(Type.INTEGER, context);
            return new Byte((byte) dval.longValue());
        } else if (target == char.class || target == Character.class) {
            if (value.length() == 1) {
                return new Character(value.charAt(0));
            } else {
                DynamicError de = new DynamicError("Cannot convert string to Java char unless length is 1");
                de.setXPathContext(context);
                de.setErrorCode("SAXON:0000");
                throw de;
            }
        } else {
            Object o = super.convertToJava(target, config, context);
            if (o == null) {
                DynamicError err = new DynamicError(
                        "Conversion of string to " + target.getName() + " is not supported");
                err.setXPathContext(context);
                err.setErrorCode("SAXON:0000");
                throw err;
            }
            return o;
        }
    }

    public String toString() {
        return "\"" + value + "\"";
    }


    /**
     * CharacterIterator is used to iterate over the characters in a string,
     * returning them as integers representing the Unicode code-point.
     */


    public final class CharacterIterator implements SequenceIterator {

        int pos;
        int current;

        /**
         * Create an iterator over a string
         */

        public CharacterIterator() {
            pos = 0;
        }

        public Item next() {
            if (pos < value.length()) {
                int c = value.charAt(pos++);
                if (c >= 55296 && c <= 56319) {
                    // we'll trust the data to be sound
                    current = ((c - 55296) * 1024) + ((int) value.charAt(pos++) - 56320) + 65536;
                } else {
                    current = c;
                }
                return new IntegerValue(current);
            } else {
                return null;
            }
        }

        public Item current() {
            return new IntegerValue(current);
        }

        public int position() {
            return pos + 1;
        }

        public SequenceIterator getAnother() {
            return new CharacterIterator();
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
// Portions created by (your name) are Copyright (C) (your legal entity). All Rights Reserved.
//
// Contributor(s): none.
//

