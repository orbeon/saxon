package org.orbeon.saxon.value;

import org.orbeon.saxon.expr.XPathContext;
import org.orbeon.saxon.om.FastStringBuffer;
import org.orbeon.saxon.om.StandardNames;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.type.AtomicType;
import org.orbeon.saxon.type.BuiltInAtomicType;
import org.orbeon.saxon.type.ConversionResult;
import org.orbeon.saxon.type.ValidationFailure;
import org.orbeon.saxon.sort.StringCollator;

import java.util.Arrays;

/**
 * A value of type xs:hexBinary
 */

public class HexBinaryValue extends AtomicValue {

    private byte[] binaryValue;


    /**
     * Constructor: create a hexBinary value from a supplied string, in which
     * each octet is represented by a pair of values from 0-9, a-f, A-F
     *
     * @param in character representation of the hexBinary value
     */

    public HexBinaryValue(CharSequence in) throws XPathException {
        CharSequence s = Whitespace.trimWhitespace(in);
        if ((s.length() & 1) != 0) {
            XPathException err = new XPathException("A hexBinary value must contain an even number of characters");
            err.setErrorCode("FORG0001");
            throw err;
        }
        binaryValue = new byte[s.length() / 2];
        for (int i = 0; i < binaryValue.length; i++) {
            binaryValue[i] = (byte)((fromHex(s.charAt(2 * i)) << 4) +
                    (fromHex(s.charAt(2 * i + 1))));
        }
        typeLabel = BuiltInAtomicType.HEX_BINARY;
    }

    /**
     * Constructor: create a HexBinary value from a supplied string in hexBinary encoding,
     * with a specified type. This method throws no checked exceptions; the caller is expected
     * to ensure that the string is a valid Base64 lexical representation, that it conforms
     * to the specified type, and that the type is indeed a subtype of xs:base64Binary.
     * An unchecked exception such as an IllegalArgumentException may be thrown if these
     * conditions are not satisfied, but this is not guaranteed.
     *
     * @param s    the value in hexBinary encoding, with no leading or trailing whitespace
     * @param type the atomic type. This must be xs:base64binary or a subtype.
     */

    public HexBinaryValue(CharSequence s, AtomicType type) {
        if ((s.length() & 1) != 0) {
            throw new IllegalArgumentException(
                    "A hexBinary value must contain an even number of characters");
        }
        binaryValue = new byte[s.length() / 2];
        try {
            for (int i = 0; i < binaryValue.length; i++) {
                binaryValue[i] = (byte)((fromHex(s.charAt(2 * i)) << 4) +
                        (fromHex(s.charAt(2 * i + 1))));
            }
        } catch (XPathException e) {
            throw new IllegalArgumentException(e.getMessage());
        }
        typeLabel = type;
    }

    /**
     * Constructor: create a hexBinary value from a given array of bytes
     *
     * @param value the value as an array of bytes
     */

    public HexBinaryValue(byte[] value) {
        binaryValue = value;
        typeLabel = BuiltInAtomicType.HEX_BINARY;
    }

    /**
     * Create a primitive copy of this atomic value (usually so that the type label can be changed).
     *
     * @param typeLabel the target type (a derived type from hexBinary)
     */

    public AtomicValue copyAsSubType(AtomicType typeLabel) {
        HexBinaryValue v = new HexBinaryValue(binaryValue);
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
        return BuiltInAtomicType.HEX_BINARY;
    }

    /**
     * Get the binary value
     *
     * @return the binary value, as a byte array
     */

    public byte[] getBinaryValue() {
        return binaryValue;
    }

    /**
     * Decode a single hex digit
     *
     * @param c the hex digit
     * @return the numeric value of the hex digit
     * @throws XPathException if it isn't a hex digit
     */

    private int fromHex(char c) throws XPathException {
        int d = "0123456789ABCDEFabcdef".indexOf(c);
        if (d > 15) {
            d = d - 6;
        }
        if (d < 0) {
            XPathException err = new XPathException("Invalid hexadecimal digit");
            err.setErrorCode("FORG0001");
            throw err;
        }
        return d;
    }

    /**
     * Convert to target data type
     *
     * @param requiredType an integer identifying the required atomic type
     * @param context      XPath dynamic evaluation context
     * @return an AtomicValue, a value of the required type; or an ErrorValue
     */

    public ConversionResult convertPrimitive(BuiltInAtomicType requiredType, boolean validate, XPathContext context) {
        switch (requiredType.getPrimitiveType()) {
        case StandardNames.XS_HEX_BINARY:
        case StandardNames.XS_ANY_ATOMIC_TYPE:
            return this;
        case StandardNames.XS_STRING:
            return new StringValue(getStringValueCS());
        case StandardNames.XS_UNTYPED_ATOMIC:
            return new UntypedAtomicValue(getStringValueCS());
        case StandardNames.XS_BASE64_BINARY:
            return new Base64BinaryValue(binaryValue);

        default:
            ValidationFailure err = new ValidationFailure("Cannot convert hexBinarry to " +
                    requiredType.getDisplayName());
            err.setErrorCode("XPTY0004");
            return err;
        }
    }

    /**
     * Convert to string
     *
     * @return the canonical representation.
     */

    public String getStringValue() {
        String digits = "0123456789ABCDEF";
        FastStringBuffer sb = new FastStringBuffer(binaryValue.length * 2);
        for (int i = 0; i < binaryValue.length; i++) {
            sb.append(digits.charAt((binaryValue[i] >> 4) & 0xf));
            sb.append(digits.charAt(binaryValue[i] & 0xf));
        }
        return sb.toString();
    }


    /**
     * Get the number of octets in the value
     *
     * @return the number of octets (bytes) in the value
     */

    public int getLengthInOctets() {
        return binaryValue.length;
    }

    /**
     * Convert to Java object (for passing to external functions)
     */

//    public Object convertAtomicToJava(Class target, XPathContext context) throws XPathException {
//
//        if (target.isAssignableFrom(HexBinaryValue.class)) {
//            return this;
//        } else if (target == Object.class) {
//            return getStringValue();
//        } else {
//            Object o = super.convertSequenceToJava(target, context);
//            if (o == null) {
//                throw new XPathException("Conversion of hexBinary to " + target.getName() +
//                        " is not supported");
//            }
//            return o;
//        }
//    }

    /**
     * Support XML Schema comparison semantics
     */

    public Comparable getSchemaComparable() {
        return new HexBinaryComparable();
    }

    private class HexBinaryComparable implements Comparable {

        public HexBinaryValue getHexBinaryValue() {
            return HexBinaryValue.this;
        }

        public int compareTo(Object o) {
            if (o instanceof HexBinaryComparable &&
                    Arrays.equals(getHexBinaryValue().binaryValue,
                            ((HexBinaryComparable)o).getHexBinaryValue().binaryValue)) {
                return 0;
            } else {
                return INDETERMINATE_ORDERING;
            }
        }

        public boolean equals(Object o) {
            return compareTo(o) == 0;
        }

        public int hashCode() {
            return HexBinaryValue.this.hashCode();
        }
    }


    /**
     * Get an object value that implements the XPath equality and ordering comparison semantics for this value.
     * If the ordered parameter is set to true, the result will be a Comparable and will support a compareTo()
     * method with the semantics of the XPath lt/gt operator, provided that the other operand is also obtained
     * using the getXPathComparable() method. In all cases the result will support equals() and hashCode() methods
     * that support the semantics of the XPath eq operator, again provided that the other operand is also obtained
     * using the getXPathComparable() method. A context argument is supplied for use in cases where the comparison
     * semantics are context-sensitive, for example where they depend on the implicit timezone or the default
     * collation.
     *
     * @param ordered true if an ordered comparison is required. In this case the result is null if the
     *                type is unordered; in other cases the returned value will be a Comparable.
     * @param collator
     *@param context the XPath dynamic evaluation context, used in cases where the comparison is context
     *                sensitive @return an Object whose equals() and hashCode() methods implement the XPath comparison semantics
     *         with respect to this atomic value. If ordered is specified, the result will either be null if
     *         no ordering is defined, or will be a Comparable
     */

    public Object getXPathComparable(boolean ordered, StringCollator collator, XPathContext context) {
        return (ordered ? null : this);
    }

    /**
     * Test if the two hexBinary or Base64Binaryvalues are equal.
     */

    public boolean equals(Object other) {
        return Arrays.equals(binaryValue, ((HexBinaryValue)other).binaryValue);
    }

    public int hashCode() {
        return Base64BinaryValue.byteArrayHashCode(binaryValue);
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
// The Initial Developer of the Original Code is Michael H. Kay, Saxonica.
//
// Portions created by (your name) are Copyright (C) (your legal entity). All Rights Reserved.
//
// Contributor(s): none.
//

