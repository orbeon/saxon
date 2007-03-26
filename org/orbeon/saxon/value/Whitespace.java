package org.orbeon.saxon.value;

import org.orbeon.saxon.om.FastStringBuffer;
import org.orbeon.saxon.tinytree.CompressedWhitespace;

/**
 * This class provides helper methods and constants for handling whitespace
 */
public class Whitespace {

    private Whitespace() {}


    /**
     * The values PRESERVE, REPLACE, and COLLAPSE represent the three options for whitespace
     * normalization. They are deliberately chosen in ascending strength order; given a number
     * of whitespace facets, only the strongest needs to be carried out.
     */

    public static final int PRESERVE = 0;
    public static final int REPLACE = 1;
    public static final int COLLAPSE = 2;

    /**
     * The values NONE, IGNORABLE, and ALL identify which kinds of whitespace text node
     * should be stripped when building a source tree
     */

    public static final int NONE = 10;
    public static final int IGNORABLE = 11;
    public static final int ALL = 12;
    public static final int UNSPECIFIED = 13;

    /**
     * Test whether a character is whitespace
     */

    public static boolean isWhitespace(int ch) {
        switch (ch) {
            case 9:
            case 10:
            case 13:
            case 32:
                return true;
            default:
                return false;
        }
    }

    /**
     * Apply schema-defined whitespace normalization to a string
     * @param action the action to be applied: one of PRESERVE, REPLACE, or COLLAPSE
     * @param value the value to be normalized
     * @return the value after normalization
     */

    public static CharSequence applyWhitespaceNormalization(int action, CharSequence value) {
        switch (action) {
            case PRESERVE:
                return value;
            case REPLACE:
                FastStringBuffer sb = new FastStringBuffer(value.length());
                for (int i=0; i<value.length(); i++) {
                    char c = value.charAt(i);
                    switch (c) {
                        case '\n':
                        case '\r':
                        case '\t':
                            sb.append(' ');
                        default:
                            sb.append(c);
                    }
                }
                return sb;
            case COLLAPSE:
                return collapseWhitespace(value);
            default:
                throw new IllegalArgumentException("Unknown whitespace facet value");
        }
    }

    /**
     * Remove all whitespace characters from a string
     */

    public static CharSequence removeAllWhitespace(CharSequence value) {
        if (containsWhitespace(value)) {
            FastStringBuffer sb = new FastStringBuffer(value.length());
            for (int i=0; i<value.length(); i++) {
                char c = value.charAt(i);
                if (c > 32 || !C0WHITE[c]) {
                    sb.append(c);
                }
            }
            return sb;
        } else {
            return value;
        }
    }

    /**
     * Remove leading whitespace characters from a string
     */

    public static CharSequence removeLeadingWhitespace(CharSequence value) {
        int start = 0;
        final int len = value.length();
        for (int i=0; i<len; i++) {
            char c = value.charAt(i);
            if (c > 32 || !C0WHITE[c]) {
                start = i;
                break;
            }
        }
        if (start == 0) {
            return value;
        } else if (start == len - 1) {
            return "";
        } else {
            return value.subSequence(start, len);
        }
    }

    /**
     * Determine if a string contains any whitespace
     */

    public static boolean containsWhitespace(CharSequence value) {
        final int len = value.length();
        for (int i=0; i<len; ) {
            char c = value.charAt(i++);
            if (c <= 32 && C0WHITE[c]) {
                return true;
            }
        }
        return false;
    }

    /**
     * Determine if a string is all-whitespace
     *
     * @param content the string to be tested
     * @return true if the supplied string contains no non-whitespace
     *     characters
     */

    public static final boolean isWhite(CharSequence content) {
        if (content instanceof CompressedWhitespace) {
            return true;
        }
        final int len = content.length();
        for (int i=0; i<len;) {
            // all valid XML 1.0 whitespace characters, and only whitespace characters, are <= 0x20
            // But XML 1.1 allows non-white characters that are also < 0x20, so we need a specific test for these
            char c = content.charAt(i++);
            if (c > 32 || !C0WHITE[c]) {
                return false;
            }
        }
        return true;
    }

    private static boolean[] C0WHITE = {
        false, false, false, false, false, false, false, false,  // 0-7
        false, true, true, false, false, true, false, false,     // 8-15
        false, false, false, false, false, false, false, false,  // 16-23
        false, false, false, false, false, false, false, false,  // 24-31
        true                                                     // 32
    };

    /**
    * Normalize whitespace as defined in XML Schema
    */

    public static CharSequence normalizeWhitespace(CharSequence in) {
        FastStringBuffer sb = new FastStringBuffer(in.length());
        for (int i=0; i<in.length(); i++) {
            char c = in.charAt(i);
            switch (c) {
                case '\n':
                case '\r':
                case '\t':
                    sb.append(' ');
                    break;
                default:
                    sb.append(c);
                    break;
            }
        }
        return sb;
    }

    /**
    * Collapse whitespace as defined in XML Schema
    */

    public static CharSequence collapseWhitespace(CharSequence in) {
        // TODO: is this method identical to NormalizeSpace.normalizeSpace()?
        int len = in.length();
        if (len==0 || !containsWhitespace(in)) {
            return in;
        }

        FastStringBuffer sb = new FastStringBuffer(len);
        boolean inWhitespace = true;
        int i = 0;
        for (; i<len; i++) {
            char c = in.charAt(i);
            switch (c) {
                case '\n':
                case '\r':
                case '\t':
                case ' ':
                    if (inWhitespace) {
                        // remove the whitespace
                    } else {
                        sb.append(' ');
                        inWhitespace = true;
                    }
                    break;
                default:
                    sb.append(c);
                    inWhitespace = false;
                    break;
            }
        }
        int nlen = sb.length();
        if (nlen>0 && sb.charAt(nlen-1)==' ') {
            sb.setLength(nlen-1);
        }
        return sb;
    }

    /**
     * Remove leading and trailing whitespace. This has the same effect as collapseWhitespace,
     * but is cheaper, for use by data types that do not allow internal whitespace.
     * @param in the input string whose whitespace is to be removed
     * @return the result of removing excess whitespace
     */
    public static CharSequence trimWhitespace(CharSequence in) {
        if (in.length()==0) {
            return in;
        }
        int first = 0;
        int last = in.length()-1;
        while (true) {
            final char x = in.charAt(first);
            if (x > 32 || !C0WHITE[x]) {
                break;
            }
            if (first++ >= last) {
                return "";
            }
        }
        while (true) {
            final char x = in.charAt(last);
            if (x > 32 || !C0WHITE[x]) {
                break;
            }
            last--;
        }
        if (first == 0 && last == in.length()-1) {
            return in;
        } else {
            return in.subSequence(first, last+1);
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
// The Original Code is: all this file
//
// The Initial Developer of the Original Code is Michael H. Kay.
//
// Contributor(s):
//

