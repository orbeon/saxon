package org.orbeon.saxon.value;

import org.orbeon.saxon.Err;
import org.orbeon.saxon.expr.XPathContext;
import org.orbeon.saxon.om.Item;
import org.orbeon.saxon.om.SequenceIterator;
import org.orbeon.saxon.trans.DynamicError;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.type.BuiltInAtomicType;
import org.orbeon.saxon.type.ItemType;
import org.orbeon.saxon.type.Type;
import org.orbeon.saxon.type.ValidationException;


/**
 * An atomic value of type xs:string
 */

public class StringValue extends AtomicValue {

    public static final StringValue EMPTY_STRING = new StringValue("");
    public static final StringValue SINGLE_SPACE = new StringValue(" ");

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
     * Get the value of the item as a CharSequence. This is in some cases more efficient than
     * the version of the method that returns a String.
     */

    public final CharSequence getStringValueCS() {
        return value;
    }

    /**
     * Convert a value to another primitive data type, with control over how validation is
     * handled.
     * @param requiredType type code of the required atomic type
     * @param validate true if validation is required. If set to false, the caller guarantees that
     * the value is valid for the target data type, and that further validation is therefore not required.
     * Note that a validation failure may be reported even if validation was not requested.
     * @return the result of the conversion, if successful. If unsuccessful, the value returned
     * will be an ErrorValue. The caller must check for this condition. No exception is thrown, instead
     * the exception will be encapsulated within the ErrorValue.
     */

    public AtomicValue convertPrimitive(BuiltInAtomicType requiredType, boolean validate) {

        try {
            switch (requiredType.getFingerprint()) {
                case Type.BOOLEAN: {
                    return BooleanValue.fromString(value);
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
                    AtomicValue iv = IntegerValue.stringToInteger(value);
                    if (iv instanceof ErrorValue) {
                        // indicates that the conversion failed
                        return iv;
                    }
                    if (iv instanceof IntegerValue) {
                        ValidationException err = ((IntegerValue)iv).convertToSubtype(requiredType, validate);
                        if (err != null) {
                            return new ErrorValue(err);
                        }
                    } else {
                        ((BigIntegerValue)iv).setSubType(requiredType);
                        return iv;
                    }
                case Type.DECIMAL:
                    return DecimalValue.makeDecimalValue(value, validate);
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
                    return RestrictedStringValue.makeRestrictedString(value, requiredType.getFingerprint(), validate);
                case Type.ANY_URI:
                    return new AnyURIValue(value);
                case Type.HEX_BINARY:
                    return new HexBinaryValue(value);
                case Type.BASE64_BINARY:
                    return new Base64BinaryValue(value);
                default:
                    throw new ValidationException("Cannot convert string to type " +
                            Err.wrap(requiredType.getDisplayName()));
            }
        } catch (ValidationException err) {
            if (err.getErrorCodeLocalPart() == null) {
                err.setErrorCode("FORG0001");
            }
            return new ErrorValue(err);
        } catch (XPathException err) {
            if (err.getErrorCodeLocalPart() == null) {
                err.setErrorCode("FORG0001");
            }
            return new ErrorValue(new ValidationException(err));
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

    public int getStringLength() {
        // memo function; only compute it the first time
        if (length == -1) {
            length = getStringLength(value);
        }
        return length;
    }

    /**
     * Get the length of a string, as defined in XPath. This is not the same as the Java length,
     * as a Unicode surrogate pair counts as a single character.
     * @param s The string whose length is required
     */

    public static int getStringLength(CharSequence s) {
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
        int[] array = new int[getStringLength(s)];
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
     * Determine if two StringValues are equal, according to XML Schema rules. (This method
     * is not used for XPath comparisons, which are always under the control of a collation.)
     * @throws ClassCastException if the values are not comparable
     */

    public boolean equals(Object other) {
        // For XML Schema purposes a String is never equal to a URI
        if (other instanceof AnyURIValue) {
            throw new ClassCastException("Cannot compare string to anyURI");
        }
        // Force a ClassCastException if the other value isn't a string or derived from string
        StringValue otherVal = (StringValue) ((AtomicValue) other).getPrimitiveValue();
        // cannot use equals() directly on two unlike CharSequences
        return getStringValue().equals(otherVal.getStringValue());
    }

    public int hashCode() {
        return getStringValue().hashCode();
    }

    public boolean effectiveBooleanValue(XPathContext context) throws XPathException {
        return value.length() > 0;
    }


    /**
     * Convert to Java object (for passing to external functions)
     */

    public Object convertToJava(Class target, XPathContext context) throws XPathException {
        if (target == Object.class) {
            return value;
        } else if (target.isAssignableFrom(StringValue.class)) {
            return this;
        } else if (target == String.class || target == CharSequence.class) {
            return getStringValue();
        } else if (target == boolean.class) {
            BooleanValue bval = (BooleanValue) convert(Type.BOOLEAN);
            return Boolean.valueOf(bval.getBooleanValue());
        } else if (target == Boolean.class) {
            BooleanValue bval = (BooleanValue) convert(Type.BOOLEAN);
            return Boolean.valueOf(bval.getBooleanValue());
        } else if (target == double.class) {
            DoubleValue dval = (DoubleValue) convert(Type.DOUBLE);
            return new Double(dval.getDoubleValue());
        } else if (target == Double.class) {
            DoubleValue dval = (DoubleValue) convert(Type.DOUBLE);
            return new Double(dval.getDoubleValue());
        } else if (target == float.class) {
            DoubleValue dval = (DoubleValue) convert(Type.DOUBLE);
            return new Float(dval.getDoubleValue());
        } else if (target == Float.class) {
            DoubleValue dval = (DoubleValue) convert(Type.DOUBLE);
            return new Float(dval.getDoubleValue());
        } else if (target == long.class) {
            IntegerValue dval = (IntegerValue) convert(Type.INTEGER);
            return new Long(dval.longValue());
        } else if (target == Long.class) {
            IntegerValue dval = (IntegerValue) convert(Type.INTEGER);
            return new Long(dval.longValue());
        } else if (target == int.class) {
            IntegerValue dval = (IntegerValue) convert(Type.INTEGER);
            return new Integer((int) dval.longValue());
        } else if (target == Integer.class) {
            IntegerValue dval = (IntegerValue) convert(Type.INTEGER);
            return new Integer((int) dval.longValue());
        } else if (target == short.class) {
            IntegerValue dval = (IntegerValue) convert(Type.INTEGER);
            return new Short((short) dval.longValue());
        } else if (target == Short.class) {
            IntegerValue dval = (IntegerValue) convert(Type.INTEGER);
            return new Short((short) dval.longValue());
        } else if (target == byte.class) {
            IntegerValue dval = (IntegerValue) convert(Type.INTEGER);
            return new Byte((byte) dval.longValue());
        } else if (target == Byte.class) {
            IntegerValue dval = (IntegerValue) convert(Type.INTEGER);
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
            Object o = super.convertToJava(target, context);
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
        return "\"" + value + '\"';
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

