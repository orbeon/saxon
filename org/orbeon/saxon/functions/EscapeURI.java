package org.orbeon.saxon.functions;
import org.orbeon.saxon.charcode.UnicodeCharacterSet;
import org.orbeon.saxon.event.HTMLURIEscaper;
import org.orbeon.saxon.expr.XPathContext;
import org.orbeon.saxon.om.FastStringBuffer;
import org.orbeon.saxon.om.Item;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.value.StringValue;

/**
 * This class supports the functions encode-for-uri() and iri-to-uri()
 */

public class EscapeURI extends SystemFunction {

    public static final int ENCODE_FOR_URI = 1;
    public static final int IRI_TO_URI = 2;
    public static final int HTML_URI = 3;

    /**
    * Evaluate the function
    */

    public Item evaluateItem(XPathContext c) throws XPathException {
        Item item = argument[0].evaluateItem(c);
        if (item == null) {
            return StringValue.EMPTY_STRING;
        }
        final CharSequence s = item.getStringValueCS();
        switch (operation) {
            case ENCODE_FOR_URI:
                return StringValue.makeStringValue(escape(s, "-_.~"));
            case IRI_TO_URI:
                return StringValue.makeStringValue(iriToUri(s));
            case HTML_URI:
                return StringValue.makeStringValue(HTMLURIEscaper.escapeURL(s, false));
            default:
                throw new UnsupportedOperationException("Unknown escape operation");
        }
    }

    /**
     * Escape special characters in a URI. The characters that are %HH-encoded are
     * all non-ASCII characters
     * @param s the URI to be escaped
     * @return the %HH-encoded string
     */

    public static CharSequence iriToUri(CharSequence s) {
        // TODO: the spec has been changed again to say that characters that are illegal in an IRI,
        // for example "\", must be %-encoded.
        if (allAscii(s)) {
            // it's worth doing a prescan to avoid the cost of copying in the common all-ASCII case
            return s;
        }
        FastStringBuffer sb = new FastStringBuffer(s.length()+20);
        for (int i=0; i<s.length(); i++) {
            final char c = s.charAt(i);
            if (c<=0x20 || c>=0x7f) {
                escapeChar(c, ((i+1)<s.length() ? s.charAt(i+1) : ' '), sb);
            } else {
                sb.append(c);
            }
        }
        return sb;
    }

    private static boolean allAscii(CharSequence s) {
        for (int i=0; i<s.length(); i++) {
            final char c = s.charAt(i);
            if (c<=0x20 || c>=0x7f) {
                return false;
            }
        }
        return true;
    }


    /**
     * Escape special characters in a URI. The characters that are %HH-encoded are
     * all non-ASCII characters, plus all ASCII characters except (a) letter A-Z
     * and a-z, (b) digits 0-9, and (c) characters listed in the allowedPunctuation
     * argument
     * @param s the URI to be escaped
     * @param allowedPunctuation ASCII characters other than letters and digits that
     * should NOT be %HH-encoded
     * @return the %HH-encoded string
     */

    public static CharSequence escape(CharSequence s, String allowedPunctuation) {
        FastStringBuffer sb = new FastStringBuffer(s.length());
        for (int i=0; i<s.length(); i++) {
            char c = s.charAt(i);
            if ((c>='a' && c<='z') || (c>='A' && c<='Z') || (c>='0' && c<='9')) {
                sb.append(c);
            } else if (c<=0x20 || c>=0x7f) {
                escapeChar(c, ((i+1)<s.length() ? s.charAt(i+1) : ' '), sb);
            } else if (allowedPunctuation.indexOf(c) >= 0) {
                sb.append(c);
            } else {
                escapeChar(c, ' ', sb);
            }

        }
        return sb;
    }

    private static final String hex = "0123456789ABCDEF";

    private static void escapeChar(char c, char c2, FastStringBuffer sb) {
        byte[] array = new byte[4];
        int used = UnicodeCharacterSet.getUTF8Encoding(c, c2, array);
        for (int b=0; b<used; b++) {
            int v = (int)array[b] & 0xff;
            sb.append('%');
            sb.append(hex.charAt(v/16));
            sb.append(hex.charAt(v%16));
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
// The Initial Developer of the Original Code is Michael H. Kay
//
// Portions created by (your name) are Copyright (C) (your legal entity). All Rights Reserved.
//
// Contributor(s): none.
//
