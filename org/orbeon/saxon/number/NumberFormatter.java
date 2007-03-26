package org.orbeon.saxon.number;
import org.orbeon.saxon.om.FastStringBuffer;
import org.orbeon.saxon.om.XMLChar;

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
  * Class NumberFormatter defines a method to format a ArrayList of integers as a character
  * string according to a supplied format specification.
  * @author Michael H. Kay
  */

public class NumberFormatter implements Serializable {

    public static boolean methodInitialized = false;
    public static Method isLetterOrDigitMethod = null;
    private static int[] nonBmpZeroDigits = {0x104a0, 0x107ce, 0x107d8, 0x107e2, 0x107ec, 0x107f6};

    private ArrayList formatTokens;
    private ArrayList punctuationTokens;
    private boolean startsWithPunctuation;

    /**
    * Tokenize the format pattern.
    * @param format the format specification. Contains one of the following values:<ul>
    * <li>"1": conventional decimal numbering</li>
    * <li>"a": sequence a, b, c, ... aa, ab, ac, ...</li>
    * <li>"A": sequence A, B, C, ... AA, AB, AC, ...</li>
    * <li>"i": sequence i, ii, iii, iv, v ...</li>
    * <li>"I": sequence I, II, III, IV, V, ...</li>
    * </ul>
    * This symbol may be preceded and followed by punctuation (any other characters) which is
    * copied to the output string.
    */

    public void prepare(String format) {

        // Tokenize the format string into alternating alphanumeric and non-alphanumeric tokens

        if (format.length()==0) {
            format="1";
        }

        formatTokens = new ArrayList(10);
        punctuationTokens = new ArrayList(10);

        int len = format.length();
        int i=0;
        int t;
        boolean first = true;
        startsWithPunctuation = true;

        while (i<len) {
            int c = format.charAt(i);
            t=i;
            if (XMLChar.isHighSurrogate(c)) {
                c = XMLChar.supplemental((char)c, format.charAt(++i));
            }
            while (isLetterOrDigit(c)) {
                i++;
                if (i==len) break;
                c = format.charAt(i);
                if (XMLChar.isHighSurrogate(c)) {
                    c = XMLChar.supplemental((char)c, format.charAt(++i));
                }
            }
            if (i>t) {
                String tok = format.substring(t, i);
                formatTokens.add(tok);
                if (first) {
                    punctuationTokens.add(".");
                    startsWithPunctuation = false;
                    first = false;
                }
            }
            if (i==len) break;
            t=i;
            c = format.charAt(i);
            if (XMLChar.isHighSurrogate(c)) {
                c = XMLChar.supplemental((char)c, format.charAt(++i));
            }
            while (!isLetterOrDigit(c)) {
                first = false;
                i++;
                if (i==len) break;
                c = format.charAt(i);
                if (XMLChar.isHighSurrogate(c)) {
                    c = XMLChar.supplemental((char)c, format.charAt(++i));
                }
            }
            if (i>t) {
                String sep = format.substring(t, i);
                punctuationTokens.add(sep);
            }
        }

        if (formatTokens.size() == 0) {
            formatTokens.add("1");
            if (punctuationTokens.size() == 1) {
                punctuationTokens.add(punctuationTokens.get(0));
            }
        }

    }

    /**
     * Determine whether a (possibly non-BMP) character is a letter or digit.
     * @param c the codepoint of the character to be tested
     * @return For a non-BMP character: on JDK1.4, always returns false. On JDK 1.5, returns the result
     * of calling Character.isLetterOrDigit(int codepoint).
     */

    private static boolean isLetterOrDigit(int c) {
        if (c <= 65535) {
            return Character.isLetterOrDigit((char)c);
        } else {
            // on JDK 1.5, we introspectively call the Character.isLetterOrDigit(codepoint) method.
            // on JDK 1.4, we give up on the letters, but use local data to test for numbers.
            if (!methodInitialized) {
                try {
                    Class[] args = {int.class};
                    isLetterOrDigitMethod = Character.class.getDeclaredMethod("isLetterOrDigit", args);
                } catch (NoSuchMethodException err) {
                    //
                }
            }
            if (isLetterOrDigitMethod != null) {
                try {
                    Object[] args = {new Integer(c)};
                    Boolean b = (Boolean)isLetterOrDigitMethod.invoke(null, args);
                    return b.booleanValue();
                } catch (IllegalAccessException e) {
                    return false;
                } catch (InvocationTargetException e) {
                    return false;
                }
            } else {
                // give up on the letters, but we know about the digits, which are more important...
                return getDigitValue(c) != -1;
            }
        }
    }

    /**
     * Determine whether a character represents a digit and if so, which digit.
     * @param in the Unicode character being tested. It's known that this is alphanumeric.
     * @return -1 if it's not a digit, otherwise the digit value.
     */

    public static int getDigitValue(int in) {
        if (in <= 65535) {
            if (Character.isDigit((char)in)) {
                return Character.getNumericValue((char)in);
            } else {
                return -1;
            }
        } else {
            for (int z=0; z<nonBmpZeroDigits.length; z++) {
                if (in >= nonBmpZeroDigits[z] && in <= nonBmpZeroDigits[z]+9) {
                    return in - nonBmpZeroDigits[z];
                }
            }
            return -1;
        }
    }


    /**
    * Format a list of numbers.
    * @param numbers the numbers to be formatted (a sequence of integer values; it may also contain
     * preformatted strings as part of the error recovery fallback)
    * @return the formatted output string.
    */

    public CharSequence format(List numbers, int groupSize, String groupSeparator,
                        String letterValue, String ordinal, Numberer numberer) {

        FastStringBuffer sb = new FastStringBuffer(20);
        int num = 0;
        int tok = 0;
        // output first punctuation token
        if (startsWithPunctuation) {
            sb.append((String)punctuationTokens.get(tok));
        }
        // output the list of numbers
        while (num<numbers.size()) {
            if (num>0) {
                if (tok==0 && startsWithPunctuation) {
                    // The first punctuation token isn't a separator if it appears before the first
                    // formatting token. Such a punctuation token is used only once, at the start.
                    sb.append(".");
                } else {
                    sb.append((String)punctuationTokens.get(tok));
                }
            }
            Object o = numbers.get(num++);
            String s;
            if (o instanceof Long) {
                long nr = ((Long)o).longValue();

                s = numberer.format(nr, (String)formatTokens.get(tok),
                             groupSize, groupSeparator, letterValue, ordinal);
            } else {
                s = o.toString();
            }
            sb.append(s);
            tok++;
            if (tok==formatTokens.size()) tok--;
        }
        // output the final punctuation token
        if (punctuationTokens.size()>formatTokens.size()) {
            sb.append((String)punctuationTokens.get(punctuationTokens.size()-1));
        }
        return sb.condense();
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
