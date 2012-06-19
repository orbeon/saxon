package org.orbeon.saxon.charcode;

/**
* This class defines properties of the Unicode character set
*/

public final class UnicodeCharacterSet implements CharacterSet {

    private static UnicodeCharacterSet theInstance = new UnicodeCharacterSet();

    /**
     * Private constructor to force the singular instance to be used
     */

    private UnicodeCharacterSet() {}

    /**
     * Get the singular instance of this class
     * @return the singular instance of this classthe singular instance of this class
     */

    public static UnicodeCharacterSet getInstance() {
        return theInstance;
    }

    public boolean inCharset(int c) {
        return true;
    }

    /**
    * Static method to generate the UTF-8 representation of a Unicode character
    * @param in the Unicode character, or the high half of a surrogate pair
    * @param in2 the low half of a surrogate pair (ignored unless the first argument is in the
    * range for a surrogate pair)
    * @param out an array of at least 4 bytes to hold the UTF-8 representation.
    * @return the number of bytes in the UTF-8 representation
    */

    public static int getUTF8Encoding(char in, char in2, byte[] out) {
        // See Tony Graham, "Unicode, a Primer", page 92
        int i = (int)in;
        if (i<=0x7f) {
            out[0] = (byte)i;
            return 1;
        } else if (i<=0x7ff) {
            out[0] = (byte)(0xc0 | ((in >> 6) & 0x1f));
            out[1] = (byte)(0x80 | (in & 0x3f));
            return 2;
        } else if (i>=0xd800 && i<=0xdbff) {
            // surrogate pair
            int j = (int)in2;
            if (!(j>=0xdc00 && j<=0xdfff)) {
                throw new IllegalArgumentException("Malformed Unicode Surrogate Pair (" + i + ',' + j + ')');
            }
            byte xxxxxx = (byte)(j & 0x3f);
            byte yyyyyy = (byte)(((i & 0x03) << 4) | ((j >> 6) & 0x0f));
            byte zzzz = (byte)((i >> 2) & 0x0f);
            byte uuuuu = (byte)(((i >> 6) & 0x0f) + 1);
            out[0] = (byte)(0xf0 | ((uuuuu >> 2) & 0x07));
            out[1] = (byte)(0x80 | ((uuuuu & 0x03) << 4) | zzzz);
            out[2] = (byte)(0x80 | yyyyyy);
            out[3] = (byte)(0x80 | xxxxxx);
            return 4;
        } else if (i>=0xdc00 && i<=0xdfff) {
            // second half of surrogate pair - ignore it
            return 0;
        } else {
            out[0] = (byte)(0xe0 | ((in >> 12) & 0x0f));
            out[1] = (byte)(0x80 | ((in >> 6) & 0x3f));
            out[2] = (byte)(0x80 | (in & 0x3f));
            return 3;
        }
    }

    /**
     * Decode a UTF8 character
     * @param in array of bytes representing a single UTF-8 encoded character
     * @param used number of bytes in the array that are actually used
     * @return the Unicode codepoint of this character
     * @throws IllegalArgumentException if the byte sequence is not a valid UTF-8 representation
     */

    public static int decodeUTF8(byte[] in, int used) throws IllegalArgumentException {
        int bottom = 0;
        for (int i=1; i<used; i++) {
            if ((in[i] & 0xc0) != 0x80) {
                throw new IllegalArgumentException("Byte " + (i+1) + " in UTF-8 sequence has wrong top bits");
            }
            bottom = (bottom<<6) + (in[i] & 0x3f);
        }
        if ((in[0] & 0x80) == 0) {
            // single byte sequence 0xxxxxxx
            if (used == 1) {
                return in[0];
            } else {
                throw new IllegalArgumentException("UTF8 single byte expected");
            }
        } else if ((in[0] & 0xe0) == 0xc0) {
            // two byte sequence
            if (used != 2) {
                throw new IllegalArgumentException("UTF8 sequence of two bytes expected");
            }
            return ((in[0] & 0x1f) << 6) + bottom;
        } else if ((in[0] & 0xf0) == 0xe0) {
            // three byte sequence
            if (used != 3) {
                throw new IllegalArgumentException("UTF8 sequence of three bytes expected");
            }
            return ((in[0] & 0x0f) << 12) + bottom;
        } else if ((in[0] & 0xf8) == 0xf8) {
            // four-byte sequence 11110uuu 10uuzzzz 10yyyyyy 10xxxxxx
            if (used != 4) {
                throw new IllegalArgumentException("UTF8 sequence of four bytes expected");
            }
            return ((in[0] & 0x07) << 24) + bottom;
        } else {
            throw new IllegalArgumentException("UTF8 invalid first byte");
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
// The Initial Developer of the Original Code is
// Aleksei Makarov [makarov@iitam.omsk.net.ru]
//
// Portions created by (your name) are Copyright (C) (your legal entity). All Rights Reserved.
//
// Contributor(s): none.
//
