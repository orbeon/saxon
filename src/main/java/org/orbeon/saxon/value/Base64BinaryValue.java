package org.orbeon.saxon.value;

import org.orbeon.saxon.expr.XPathContext;
import org.orbeon.saxon.om.FastStringBuffer;
import org.orbeon.saxon.om.StandardNames;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.type.*;
import org.orbeon.saxon.sort.StringCollator;

import java.io.ByteArrayOutputStream;
import java.util.Arrays;

/**
 * A value of type xs:base64Binary
 */

public class Base64BinaryValue extends AtomicValue {

    private byte[] binaryValue;


    /**
     * Constructor: create a base64Binary value from a supplied string in base64 encoding
     * @param s the lexical representation of the base64 binary value. There is no requirement
     * that whitespace should already be collapsed.
     */

    public Base64BinaryValue(CharSequence s) throws XPathException {
        Base64Decoder decoder = new Base64Decoder();
        try {
            decoder.translate(s);
        } catch (IllegalArgumentException e) {
            XPathException err = new XPathException(e.getMessage());
            err.setErrorCode("FORG0001");
            throw err;
        }
        binaryValue = decoder.getByteArray();
        typeLabel = BuiltInAtomicType.BASE64_BINARY;
    }

    /**
     * Constructor: create a base64Binary value from a supplied string in base64 encoding,
     * with a specified type. This method throws no checked exceptions; the caller is expected
     * to ensure that the string is a valid Base64 lexical representation, that it conforms
     * to the specified type, and that the type is indeed a subtype of xs:base64Binary.
     * An unchecked exception such as an IllegalArgumentException may be thrown if these
     * conditions are not satisfied, but this is not guaranteed.
     * @param s the value in base64 encoding
     * @param type the atomic type. This must be xs:base64binary or a subtype.
     */

    public Base64BinaryValue(CharSequence s, AtomicType type) {
        Base64Decoder decoder = new Base64Decoder();
        decoder.translate(s);
        binaryValue = decoder.getByteArray();
        typeLabel = type;
    }


    /**
     * Constructor: create a base64Binary value from a given array of bytes
     * @param value array of bytes holding the octet sequence
     */

    public Base64BinaryValue(byte[] value) {
        binaryValue = value;
        typeLabel = BuiltInAtomicType.BASE64_BINARY;
    }

    /**
     * Create a copy of this atomic value (usually so that the type label can be changed).
     * The type label of the copy will be reset to the primitive type.
     * @param typeLabel the type label to be attached to the value, a subtype of xs:base64Binary
     * @return the copied value
     */

    public AtomicValue copyAsSubType(AtomicType typeLabel) {
        Base64BinaryValue v = new Base64BinaryValue(binaryValue);
        v.typeLabel = typeLabel;
        return v;        
    }

    /**
     * Get the binary value
     * @return the octet sequence that is the typed value
     */

    public byte[] getBinaryValue() {
        return binaryValue;
    }

    public BuiltInAtomicType getPrimitiveType() {
        return BuiltInAtomicType.BASE64_BINARY;
    }

    /**
     * Convert to target data type
     * @param requiredType an integer identifying the required atomic type
     * @param context the XPath dynamic evaluation context
     * @return an AtomicValue, a value of the required type; or an ErrorValue
     */

    public ConversionResult convertPrimitive(BuiltInAtomicType requiredType, boolean validate, XPathContext context) {
        switch (requiredType.getPrimitiveType()) {
        case StandardNames.XS_BASE64_BINARY:
        case StandardNames.XS_ANY_ATOMIC_TYPE:
            return this;
        case StandardNames.XS_STRING:
            return new StringValue(getStringValueCS());
        case StandardNames.XS_UNTYPED_ATOMIC:
            return new UntypedAtomicValue(getStringValueCS());
        case StandardNames.XS_HEX_BINARY:
            return new HexBinaryValue(binaryValue);
        default:
            ValidationFailure err = new ValidationFailure("Cannot convert base64Binary to " +
                                     requiredType.getDisplayName());
            err.setErrorCode("XPTY0004");
            return err;
        }
    }

    /**
     * Convert to string
     * @return the canonical representation.
     */

    public String getStringValue() {
        Base64Encoder encoder = new Base64Encoder();
        encoder.translate(binaryValue);
        return new String(encoder.getCharArray());
    }

    /**
     * Get the number of octets in the value
     * @return the number of octets
     */

     public int getLengthInOctets() {
         return binaryValue.length;
     }

    /**
     * Convert to Java object (for passing to external functions)
     */

//    public Object convertAtomicToJava(Class target, XPathContext context) throws XPathException {
//
//        if (target.isAssignableFrom(Base64BinaryValue.class)) {
//            return this;
//        } else if (target == Object.class) {
//            return getStringValue();
//        } else {
//            Object o = super.convertSequenceToJava(target, context);
//            if (o == null) {
//                throw new XPathException("Conversion of base64Binary to " + target.getName() +
//                        " is not supported");
//            }
//            return o;
//        }
//    }

    /**
     * Support XML Schema comparison semantics
     */

    public Comparable getSchemaComparable() {
        return new Base64BinaryComparable();
    }

    private class Base64BinaryComparable implements Comparable {

        public Base64BinaryValue getBase64BinaryValue() {
            return Base64BinaryValue.this;
        }

         public int compareTo(Object o) {
             if (o instanceof Base64BinaryComparable &&
                     Arrays.equals(getBase64BinaryValue().binaryValue,
                             ((Base64BinaryComparable)o).getBase64BinaryValue().binaryValue)) {
                 return 0;
             } else {
                 return INDETERMINATE_ORDERING;
             }
         }

         public boolean equals(Object o) {
             return compareTo(o) == 0;
         }

         public int hashCode() {
             return Base64BinaryValue.this.hashCode();
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
     * Test if the two base64Binary values are equal.
     */

    public boolean equals(Object other) {
        return Arrays.equals(binaryValue, ((Base64BinaryValue)other).binaryValue);
    }

    public int hashCode() {
        return byteArrayHashCode(binaryValue);
    }

    protected static int byteArrayHashCode(byte[] value) {
        long h = 0;
        for (int i=0; i<Math.min(value.length, 64); i++) {
            h = (h<<1) ^ value[i];
        }
        return (int)((h >> 32) ^ h);
    }

     /*
     *
     * The contents of this [inner class] are subject to the Netscape Public
     * License Version 1.1 (the "License"); you may not use this file
     * except in compliance with the License. You may obtain a copy of
     * the License at http://www.mozilla.org/NPL/
     *
     * Software distributed under the License is distributed on an "AS
     * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
     * implied. See the License for the specific language governing
     * rights and limitations under the License.
     *
     * The Original Code is mozilla.org code.
     *
     * The Initial Developer of the Original Code is Netscape
     * Communications Corporation.  Portions created by Netscape are
     * Copyright (C) 1999 Netscape Communications Corporation. All
     * Rights Reserved.
     *
     * Contributor(s):
     */

    /**
     * Byte to text encoder using base 64 encoding. To create a base 64
     * encoding of a byte stream call {@link #translate} for every
     * sequence of bytes and {@link #getCharArray} to mark closure of
     * the byte stream and retrieve the text presentation.
     *
     * @author Based on code from the Mozilla Directory SDK
     */
    private static final class Base64Encoder {

        private FastStringBuffer out = new FastStringBuffer(256);

        private int buf = 0;                     // a 24-bit quantity

        private int buf_bytes = 0;               // how many octets are set in it

        private char line[] = new char[74];      // output buffer

        private int line_length = 0;             // output buffer fill pointer

        //static private final byte crlf[] = "\r\n".getBytes();

        private static final char map[] = {
            'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', // 0-7
            'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', // 8-15
            'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', // 16-23
            'Y', 'Z', 'a', 'b', 'c', 'd', 'e', 'f', // 24-31
            'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', // 32-39
            'o', 'p', 'q', 'r', 's', 't', 'u', 'v', // 40-47
            'w', 'x', 'y', 'z', '0', '1', '2', '3', // 48-55
            '4', '5', '6', '7', '8', '9', '+', '/', // 56-63
        };


        private void encode_token() {
            int i = line_length;
            line[i] = map[0x3F & (buf >> 18)];   // sextet 1 (octet 1)
            line[i + 1] = map[0x3F & (buf >> 12)];   // sextet 2 (octet 1 and 2)
            line[i + 2] = map[0x3F & (buf >> 6)];    // sextet 3 (octet 2 and 3)
            line[i + 3] = map[0x3F & buf];           // sextet 4 (octet 3)
            line_length += 4;
            buf = 0;
            buf_bytes = 0;
        }


        private void encode_partial_token() {
            int i = line_length;
            line[i] = map[0x3F & (buf >> 18)];   // sextet 1 (octet 1)
            line[i + 1] = map[0x3F & (buf >> 12)];   // sextet 2 (octet 1 and 2)

            if (buf_bytes == 1)
                line[i + 2] = '=';
            else
                line[i + 2] = map[0x3F & (buf >> 6)];  // sextet 3 (octet 2 and 3)

            if (buf_bytes <= 2)
                line[i + 3] = '=';
            else
                line[i + 3] = map[0x3F & buf];         // sextet 4 (octet 3)
            line_length += 4;
            buf = 0;
            buf_bytes = 0;
        }


        private void flush_line() {
            out.append(line, 0, line_length);
            line_length = 0;
        }


        /**
         * Given a sequence of input bytes, produces a sequence of output bytes
         * using the base64 encoding.  If there are bytes in `out' already, the
         * new bytes are appended, so the caller should do `out.setLength(0)'
         * first if that's desired.
         * @param in the octet sequence to be encoded in Base64
         */
        public final void translate(byte[] in) {
            int in_length = in.length;

            for (int i = 0; i < in_length; i++) {
                if (buf_bytes == 0)
                    buf = (buf & 0x00FFFF) | (in[i] << 16);
                else if (buf_bytes == 1)
                    buf = (buf & 0xFF00FF) | ((in[i] << 8) & 0x00FFFF);
                else
                    buf = (buf & 0xFFFF00) | (in[i] & 0x0000FF);

                if ((++buf_bytes) == 3) {
                    encode_token();
                    if (line_length >= 72) {
                        flush_line();
                    }
                }

                if (i == (in_length - 1)) {
                    if ((buf_bytes > 0) && (buf_bytes < 3))
                        encode_partial_token();
                    if (line_length > 0)
                        flush_line();
                }
            }

            for (int i = 0; i < line.length; i++)
                line[i] = 0;
        }


        public char[] getCharArray() {
            char[] ch;

            if (buf_bytes != 0)
                encode_partial_token();
            flush_line();
            for (int i = 0; i < line.length; i++)
                line[i] = 0;
            ch = new char[out.length()];
            if (out.length() > 0)
                out.getChars(0, out.length(), ch, 0);
            return ch;
        }
    }

    /*
     *
     * The contents of this [inner class] are subject to the Netscape Public
     * License Version 1.1 (the "License"); you may not use this file
     * except in compliance with the License. You may obtain a copy of
     * the License at http://www.mozilla.org/NPL/
     *
     * Software distributed under the License is distributed on an "AS
     * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
     * implied. See the License for the specific language governing
     * rights and limitations under the License.
     *
     * The Original Code is mozilla.org code.
     *
     * The Initial Developer of the Original Code is Netscape
     * Communications Corporation.  Portions created by Netscape are
     * Copyright (C) 1999 Netscape Communications Corporation. All
     * Rights Reserved.
     *
     * Contributor(s):
     */


    /**
     * Base 64 text to byte decoder. To produce the binary  array from
     * base 64 encoding call {@link #translate} for each sequence of
     * characters and {@link #getByteArray} to mark closure of the
     * character stream and retrieve the binary contents.
     *
     * @author Based on code from the Mozilla Directory SDK
     */

    private static final class Base64Decoder {
        private ByteArrayOutputStream out = new ByteArrayOutputStream();

        private byte token[] = new byte[4];      // input buffer

        private byte bytes[] = new byte[3];      // output buffer

        private int token_length = 0;            // input buffer length

        private static final byte NUL = 127;     // must be out of range 0-64

        private static final byte EOF = 126;     // must be out of range 0-64

        private static final byte SP = 125;     // must be out of range 0-64

        private static final byte[] map = {
            NUL, NUL, NUL, NUL, NUL, NUL, NUL, NUL, //   x00 - x07
            NUL, SP, SP, NUL, NUL, SP, NUL, NUL,    //   x08 - x0F
            NUL, NUL, NUL, NUL, NUL, NUL, NUL, NUL, //   x10 - x17
            NUL, NUL, NUL, NUL, NUL, NUL, NUL, NUL, //   x18 - x1F
            SP, NUL, NUL, NUL, NUL, NUL, NUL, NUL,  //   x20 - x2F   !"#$%&'
            NUL, NUL, NUL, 62, NUL, NUL, NUL, 63,   //   050-057  ()*+,-./
            52, 53, 54, 55, 56, 57, 58, 59,         //   060-067  01234567
            60, 61, NUL, NUL, NUL, EOF, NUL, NUL,   //   070-077  89:;<=>?

            NUL, 0, 1, 2, 3, 4, 5, 6,               //   100-107  @ABCDEFG
            7, 8, 9, 10, 11, 12, 13, 14,            //   110-117  HIJKLMNO
            15, 16, 17, 18, 19, 20, 21, 22,         //   120-127  PQRSTUVW
            23, 24, 25, NUL, NUL, NUL, NUL, NUL,    //   130-137  XYZ[\]^_
            NUL, 26, 27, 28, 29, 30, 31, 32,        //   140-147  `abcdefg
            33, 34, 35, 36, 37, 38, 39, 40,         //   150-157  hijklmno
            41, 42, 43, 44, 45, 46, 47, 48,         //   160-167  pqrstuvw
            49, 50, 51, NUL, NUL, NUL, NUL, NUL,    //   170-177  xyz{|}~

            NUL, NUL, NUL, NUL, NUL, NUL, NUL, NUL, //   200-207
            NUL, NUL, NUL, NUL, NUL, NUL, NUL, NUL, //   210-217
            NUL, NUL, NUL, NUL, NUL, NUL, NUL, NUL, //   220-227
            NUL, NUL, NUL, NUL, NUL, NUL, NUL, NUL, //   230-237
            NUL, NUL, NUL, NUL, NUL, NUL, NUL, NUL, //   240-247
            NUL, NUL, NUL, NUL, NUL, NUL, NUL, NUL, //   250-257
            NUL, NUL, NUL, NUL, NUL, NUL, NUL, NUL, //   260-267
            NUL, NUL, NUL, NUL, NUL, NUL, NUL, NUL, //   270-277

            NUL, NUL, NUL, NUL, NUL, NUL, NUL, NUL, //   300-307
            NUL, NUL, NUL, NUL, NUL, NUL, NUL, NUL, //   310-317
            NUL, NUL, NUL, NUL, NUL, NUL, NUL, NUL, //   320-327
            NUL, NUL, NUL, NUL, NUL, NUL, NUL, NUL, //   330-337
            NUL, NUL, NUL, NUL, NUL, NUL, NUL, NUL, //   340-347
            NUL, NUL, NUL, NUL, NUL, NUL, NUL, NUL, //   350-357
            NUL, NUL, NUL, NUL, NUL, NUL, NUL, NUL, //   360-367
            NUL, NUL, NUL, NUL, NUL, NUL, NUL, NUL, //   370-377
        };


        // Fast routine that assumes full 4-char tokens with no '=' in them.
        //
        private void decode_token() {
            int num = ((token[0] << 18) |
                    (token[1] << 12) |
                    (token[2] << 6) |
                    (token[3]));

            bytes[0] = (byte)(0xFF & (num >> 16));
            bytes[1] = (byte)(0xFF & (num >> 8));
            bytes[2] = (byte)(0xFF & num);

            out.write(bytes, 0, 3);
        }


        // Hairier routine that deals with the final token, which can have fewer
        // than four characters, and that might be padded with '='.
        //
        private void decode_final_token() {

            byte b0 = token[0];
            byte b1 = token[1];
            byte b2 = token[2];
            byte b3 = token[3];

            int eq_count = 0;

            if (b0 == EOF) {
                b0 = 0;
                eq_count++;
            }
            if (b1 == EOF) {
                b1 = 0;
                eq_count++;
            }
            if (b2 == EOF) {
                b2 = 0;
                eq_count++;
            }
            if (b3 == EOF) {
                b3 = 0;
                eq_count++;
            }

            if (eq_count > 2) {
                throw new IllegalArgumentException("The number of '=' signs at the end of a base64 value must not exceed 2");
            }
            if (eq_count == 2 && (b1 & 0x0F) != 0) {
                throw new IllegalArgumentException("In base64, if the value ends with '==' then the last character must be one of [AQgw]");
            }
            if (eq_count == 1 && (b2 & 0x03) != 0) {
                throw new IllegalArgumentException("In base64, if the value ends with '=' then the last character must be one of [AEIMQUYcgkosw048]");
            }

            int num = ((b0 << 18) | (b1 << 12) | (b2 << 6) | (b3));

            // eq_count will be 0, 1, or 2.
            // No "=" padding means 4 bytes mapped to 3, the normal case,
            //        not handled in this routine.
            // "xxx=" means 3 bytes mapped to 2.
            // "xx==" means 2 bytes mapped to 1.
            // "x===" can't happen, because "x" would then be encoding
            //        only 6 bits, not 8, the minimum possible.

            out.write((byte)(num >> 16));             // byte 1, count = 0 or 1 or 2
            if (eq_count <= 1) {
                out.write((byte)((num >> 8) & 0xFF)); // byte 2, count = 0 or 1
                if (eq_count == 0) {
                    out.write((byte)(num & 0xFF));    // byte 3, count = 0
                }
            }
        }

        /**
         * Decode the base 64 string into a byte array (which can subsequently be accessed using getByteArray()
         * @param str the base 64 string
         * @throws IllegalArgumentException if the base64 string is incorrectly formatted
         */

        public final void translate(CharSequence str) throws IllegalArgumentException {
            if (token == null) // already saw eof marker?
                return;
            int length = str.length();
            int lengthAtEOF;
            int found_eq = 0;
            for (int i = 0; i < length; i++) {
                char c = str.charAt(i);
                if (c > 127) {
                    throw new IllegalArgumentException("non-ASCII character in Base64 value (at offset " + i + ')');
                }
                byte t = map[c];
                if (t == NUL) {
                    throw new IllegalArgumentException("invalid character '" + c + "' in Base64 value (at offset " + i + ')');
                }
                if (found_eq > 0 && t != EOF && t != SP) {
                    throw new IllegalArgumentException("In Base64, an '=' character can appear only at the end");
                }
                if (t == EOF) {
                    if (found_eq > 0) {
                        found_eq++;
                        if (found_eq > 2) {
                            throw new IllegalArgumentException("Base64 value can contain at most two '=' characters");
                        }
                        token_length = (token_length + 1) % 4;
                    } else {
                        found_eq = 1;
                        lengthAtEOF = token_length;
                        eof();
                        token_length = (lengthAtEOF + 1) % 4;
                    }
                } else if (t != SP) {
                    token[token_length++] = t;
                    if (token_length == 4) {
                        if (found_eq == 0) {
                            decode_token();
                        }
                        token_length = 0;
                    }
                }
            }
            if (token_length != 0) {
                throw new IllegalArgumentException("Base64 input must be a multiple of four characters");
            }
        }

        private void eof() {
            if (token != null && token_length != 0) {
                while (token_length < 4) {
                    token[token_length++] = EOF;
                }
                decode_final_token();
            }
            token_length = 0;
            token = new byte[4];
            bytes = new byte[3];
        }

        public byte[] getByteArray() {
            eof();
            return out.toByteArray();
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
// The Original Code is: all this file, with the exception of the two inner classes which
// were originated by Mozilla/Netscape and reached Saxon via Castor.
//
// The Initial Developer of the Original Code is Michael H. Kay, Saxonica.
//
// Portions created by (your name) are Copyright (C) (your legal entity). All Rights Reserved.
//
// Contributor(s): none.
//

