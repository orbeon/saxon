package net.sf.saxon.charcode;

import net.sf.saxon.om.XMLChar;

import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.util.HashMap;

/**
* This class establishes properties of a character set that is
 * known to the Java VM but not specifically known to Saxon. It avoids
 * using the encoder.canEncode() method because there is a known bug
 * (in JDK 1.4.2) that for some encodings, this returns true for
 * every character. So this version of the class actually attempts
 * to encode the characters, and catches the exception when it fails.
*/

public class BuggyCharacterSet implements CharacterSet {

    private static HashMap map;

    private CharsetEncoder encoder;

    // This class is written on the assumption that the CharsetEncoder.canEncode()
    // method may be expensive. For BMP characters, it therefore remembers the results
    // so each character is only looked up the first time it is encountered.

    private byte[] charinfo = new byte[65536];
        // rely on initialization to zeroes

    //private final static byte UNKNOWN = 0;
    private static final byte GOOD = 1;
    private static final byte BAD = 2;

    private BuggyCharacterSet(Charset charset) {
        encoder = charset.newEncoder();
    }
    
    public static synchronized BuggyCharacterSet makeCharSet(Charset charset) {
        if (map == null) {
            map = new HashMap(10);
        }
        BuggyCharacterSet c = (BuggyCharacterSet)map.get(charset);
        if (c == null) {
            c = new BuggyCharacterSet(charset);
            map.put(charset, c);
        }
        return c;
    }

    public final boolean inCharset(int c) {
        // Assume ASCII chars are always OK
        if (c <= 127) {
            return true;
        }
        try {
            if (c <= 65535) {
                if (charinfo[c] == GOOD) {
                    return true;
                } else if (charinfo[c] == BAD) {
                    return false;
                } else {
                    charinfo[c] = BAD;  // guilty until proved innocent
                    char[] cc = {(char)c};
                    encoder.encode(CharBuffer.wrap(cc));
                    charinfo[c] = GOOD;
                    return true;
                }
            } else {
                char[] ss = { XMLChar.highSurrogate(c),
                              XMLChar.lowSurrogate(c) };
                encoder.encode(CharBuffer.wrap(ss));
                return true;
            }
        } catch (CharacterCodingException ex) {
            return false;
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
