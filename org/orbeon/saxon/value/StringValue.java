package org.orbeon.saxon.value;

import org.orbeon.saxon.Err;
import org.orbeon.saxon.expr.XPathContext;
import org.orbeon.saxon.om.*;
import org.orbeon.saxon.trans.DynamicError;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.trans.SaxonErrorCode;
import org.orbeon.saxon.type.*;


/**
 * An atomic value of type xs:string
 */

public class StringValue extends AtomicValue {

    public static final StringValue EMPTY_STRING = new StringValue("");
    public static final StringValue SINGLE_SPACE = new StringValue(" ");
    public static final StringValue TRUE = new StringValue("true");
    public static final StringValue FALSE = new StringValue("false");

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
     * Factory method. Unlike the constructor, this avoids creating a new StringValue in the case
     * of a zero-length string (and potentially other strings, in future)
     * @param value the String value. Null is taken as equivalent to "".
     * @return the corresponding StringValue
     */

    public static StringValue makeStringValue(CharSequence value) {
        if (value == null || value.length() == 0) {
            return StringValue.EMPTY_STRING;
        } else {
            return new StringValue(value);
        }
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
     * @param context
     * @return the result of the conversion, if successful. If unsuccessful, the value returned
     * will be a ValidationErrorValue. The caller must check for this condition. No exception is thrown, instead
     * the exception will be encapsulated within the ErrorValue.
     */

    public AtomicValue convertPrimitive(BuiltInAtomicType requiredType, boolean validate, XPathContext context) {
        int req = requiredType.getFingerprint();
        if (req == Type.STRING || req == Type.ANY_ATOMIC || req == Type.ITEM) {
            return this;
        }
        return convertStringToBuiltInType(value, requiredType,
                (validate ? context.getConfiguration().getNameChecker() : null));
    }

    /**
     * Convert a string value to another built-in data type, with control over how validation is
     * handled.
     * @param value the value to be converted
     * @param requiredType the required atomic type
     * @param checker if validation is required, a NameChecker. If set to null, the caller guarantees that
     * the value is valid for the target data type, and that further validation is therefore not required.
     * Note that a validation failure may be reported even if validation was not requested.
     * @return the result of the conversion, if successful. If unsuccessful, the value returned
     * will be a {@link ValidationErrorValue}. The caller must check for this condition. No exception is thrown, instead
     * the exception will be encapsulated within the ValidationErrorValue.
     */

    public static AtomicValue convertStringToBuiltInType(CharSequence value, BuiltInAtomicType requiredType,
                                            NameChecker checker) {
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


                case Type.UNSIGNED_LONG:
                case Type.UNSIGNED_INT:
                case Type.UNSIGNED_SHORT:
                case Type.UNSIGNED_BYTE:
                    if (checker != null) {
                        for (int c=0; c<value.length(); c++) {
                            if (value.charAt(c) == '+') {
                                ValidationException err = new ValidationException(
                                        "An unsigned number must not contain a plus sign");
                                return new ValidationErrorValue(err);
                            }
                        }
                    }
                    // fall through
                case Type.NON_POSITIVE_INTEGER:
                case Type.NEGATIVE_INTEGER:
                case Type.LONG:
                case Type.INT:
                case Type.SHORT:
                case Type.BYTE:
                case Type.NON_NEGATIVE_INTEGER:
                case Type.POSITIVE_INTEGER:
                    AtomicValue iv = IntegerValue.stringToInteger(value);
                    if (iv instanceof ValidationErrorValue) {
                        // indicates that the conversion failed
                        return iv;
                    }
                    ValidationException err;
                    if (iv instanceof IntegerValue) {
                        err = ((IntegerValue)iv).convertToSubtype(requiredType, checker != null);
                    } else {
                        err = ((BigIntegerValue)iv).convertToSubType(requiredType, checker != null);
                    }
                    return (err == null ? iv : new ValidationErrorValue(err));
                case Type.DECIMAL:
                    return DecimalValue.makeDecimalValue(value, checker != null);
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
                case Type.ANY_ATOMIC:
                case Type.ITEM:
                    return makeStringValue(value);
                case Type.NORMALIZED_STRING:
                case Type.TOKEN:
                case Type.LANGUAGE:
                case Type.NAME:
                case Type.NCNAME:
                case Type.ID:
                case Type.IDREF:
                case Type.ENTITY:
                case Type.NMTOKEN:
                    return RestrictedStringValue.makeRestrictedString(value, requiredType.getFingerprint(), checker);
                case Type.ANY_URI:
                    if (AnyURIValue.isValidURI(value)) {
                        return new AnyURIValue(value);
                    } else {
                        throw new ValidationException("Invalid URI: " + value.toString());
                    }
                case Type.HEX_BINARY:
                    return new HexBinaryValue(value);
                case Type.BASE64_BINARY:
                    return new Base64BinaryValue(value);
                default:
                    ValidationException ve = new ValidationException("Cannot convert string to type " +
                            Err.wrap(requiredType.getDisplayName()));
                    ve.setErrorCode("XPTY0004");
                    ve.setIsTypeError(true);
                    throw ve;
            }
        } catch (ValidationException err) {
            if (err.getErrorCodeLocalPart() == null) {
                err.setErrorCode("FORG0001");
            }
            return new ValidationErrorValue(err);
        } catch (XPathException err) {
            if (err.getErrorCodeLocalPart() == null) {
                err.setErrorCode("FORG0001");
            }
            ValidationException ve = new ValidationException(err.getMessage());
            if (err.getErrorCodeLocalPart() == null) {
                ve.setErrorCode("FORG0001");
            } else {
                ve.setErrorCode(err.getErrorCodeLocalPart());
            }
            return new ValidationErrorValue(ve);
        }
    }

    /**
     * Convert the value to a given type. The result of the conversion will be
     * an atomic value of the required type. This method works where the target
     * type is a built-in atomic type and also where it is a user-defined atomic
     * type.
     *
     * @param targetType the type to which the value is to be converted
     * @param checker   a NameChecker if validation is required, null if the caller already knows that the
     *                   value is valid
     * @return the value after conversion if successful; or a {@link ValidationErrorValue} if conversion failed. The
     *         caller must check for this condition. Validation may fail even if validation was not requested.
     */

    public static AtomicValue convertStringToAtomicType(
            CharSequence value, AtomicType targetType, NameChecker checker) {
        if (targetType instanceof BuiltInAtomicType) {
            return convertStringToBuiltInType(value, (BuiltInAtomicType)targetType, checker);
        } else {
            AtomicValue v =
                    convertStringToBuiltInType(value, (BuiltInAtomicType)targetType.getPrimitiveItemType(), checker);
            if (v instanceof ValidationErrorValue) {
                // conversion has failed
                return v;
            }
            return targetType.makeDerivedValue(v, value, checker != null);
        }
    }


    /**
     * Return the type of the expression
     * @return Type.STRING (always)
     * @param th
     */

    public ItemType getItemType(TypeHierarchy th) {
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
     * Determine whether the string is a zero-length string. This may
     * be more efficient than testing whether the length is equal to zero
     */

    public boolean isZeroLength() {
        return value.length() == 0;
    }

    /**
     * Determine whether the string contains surrogate pairs
     * @return true if the string contains any non-BMP characters
     */

    public boolean containsSurrogatePairs() {
        if (length == -1) {
            getStringLength();
        }
        return (length != value.length());
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

    public int[] expand() {
        int[] array = new int[getStringLength()];
        int o = 0;
        int len = value.length();
        for (int i = 0; i < len; i++) {
            int charval;
            int c = value.charAt(i);
            if (c >= 55296 && c <= 56319) {
                // we'll trust the data to be sound
                charval = ((c - 55296) * 1024) + ((int) value.charAt(i + 1) - 56320) + 65536;
                i++;
            } else {
                charval = c;
            }
            array[o++] = charval;
        }
        return array;
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
     * Contract an array of integers containing Unicode codepoints into a Java string
     */

    public static CharSequence contract(int[] codes, int used) {
        FastStringBuffer sb = new FastStringBuffer(codes.length);
        for (int i=0; i<used; i++) {
            if (codes[i]<65536) {
                sb.append((char)codes[i]);
            }
            else {  // output a surrogate pair
                sb.append(XMLChar.highSurrogate(codes[i]));
                sb.append(XMLChar.lowSurrogate(codes[i]));
            }
        }
        return sb;
    }

    /**
     * Determine if two StringValues are equal, according to XML Schema rules. (This method
     * is not used for XPath comparisons, which are always under the control of a collation.)
     * @throws ClassCastException if the values are not comparable
     */

    public boolean equals(Object other) {
        // TODO: is this method used? It probably shouldn't be...
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

    /**
     * Test whether this StringValue is equal to another under the rules of the codepoint collation
     */

    public boolean codepointEquals(StringValue other) {
        // avoid conversion of CharSequence to String if values are different lengths
        if (value.length() != other.value.length()) {
            return false;
        }
        return value.toString().equals(other.value.toString());
        // It might be better to do character-by-character comparison in all cases; or it might not.
        // We do it this way in the hope that string comparison compiles to native code.
    }

    /**
     * Get the effective boolean value of a string
     * @param context not used
     * @return true if the string has length greater than zero
     */

    public boolean effectiveBooleanValue(XPathContext context) {
        return value.length() > 0;
    }


    /**
     * Convert to Java object (for passing to external functions)
     */

    public Object convertToJava(Class target, XPathContext context) throws XPathException {
        if (target == Object.class) {
            return getStringValue();
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
                DynamicError de = new DynamicError("Cannot convert xs:string to Java char unless length is 1");
                de.setXPathContext(context);
                de.setErrorCode(SaxonErrorCode.SXJE0005);
                throw de;
            }
        } else {
            Object o = super.convertToJava(target, context);
            if (o == null) {
                DynamicError err = new DynamicError(
                        "Conversion of xs:string to " + target.getName() + " is not supported");
                err.setXPathContext(context);
                err.setErrorCode(SaxonErrorCode.SXJE0006);
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

        int inpos = 0;        // 0-based index of the current Java char
        int outpos = 0;       // 1-based value of position() function
        int current = -1;     // Unicode codepoint most recently returned

        /**
         * Create an iterator over a string
         */

        public CharacterIterator() {
        }

        public Item next() {
            if (inpos < value.length()) {
                int c = value.charAt(inpos++);
                if (c >= 55296 && c <= 56319) {
                    // we'll trust the data to be sound
                    current = ((c - 55296) * 1024) + ((int) value.charAt(inpos++) - 56320) + 65536;
                } else {
                    current = c;
                }
                outpos++;
                return new IntegerValue(current);
            } else {
                outpos = -1;
                return null;
            }
        }

        public Item current() {
            if (outpos < 1) {
                return null;
            }
            return new IntegerValue(current);
        }

        public int position() {
            return outpos;
        }

        public SequenceIterator getAnother() {
            return new CharacterIterator();
        }

        /**
         * Get properties of this iterator, as a bit-significant integer.
         *
         * @return the properties of this iterator. This will be some combination of
         *         properties such as {@link #GROUNDED} and {@link #LAST_POSITION_FINDER}. It is always
         *         acceptable to return the value zero, indicating that there are no known special properties.
         */

        public int getProperties() {
            return 0;
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

