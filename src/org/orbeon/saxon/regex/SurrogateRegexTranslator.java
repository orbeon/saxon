package org.orbeon.saxon.regex;

import org.orbeon.saxon.om.FastStringBuffer;
import org.orbeon.saxon.charcode.UTF16;

import java.util.List;
import java.util.ArrayList;

/**
 * Abstract superclass for the JDK 1.4 and .NET regex translators, or in principle for any other
 * target regex dialect in which "." matches a UTF-16 16-bit code rather than a Unicode character
 */
public abstract class SurrogateRegexTranslator extends RegexTranslator {

    // TODO: could go further in terms of achieving reuse of common code between the two subclasses.
    // The main thing needed is a factory method for the classes that differ, e.g. Union and Subtraction,
    // which in turn means that static data using these classes needs to be dynamically initialized.

    protected static final CharClass[] categoryCharClasses = new CharClass[RegexData.categories.length()];
    protected static final CharClass[] subCategoryCharClasses = new CharClass[RegexData.subCategories.length() / 2];

    /**
     * Object representing a character class
     */

    protected static abstract class CharClass {

        private final int containsBmp;
        // if it contains ALL and containsBmp != NONE, then the generated class for containsBmp must
        // contain all the high surrogates
        private final int containsNonBmp;

        /**
         * Create a character class
         * @param containsBmp NONE, SOME, or ALL, depending on whether the character class contains all
         * the BMP characters, some of the BMP characters, or none of the BMP characters
         * @param containsNonBmp NONE, SOME, or ALL, depending on whether the character class contains all
         * the non-BMP characters, some of the non-BMP characters, or none of the non-BMP characters
         */

        protected CharClass(int containsBmp, int containsNonBmp) {
            this.containsBmp = containsBmp;
            this.containsNonBmp = containsNonBmp;
        }

        /**
         * Determine whether this character class contains NONE, SOME, or ALL of the BMP characters
         * @return NONE, SOME, or ALL
         */

        public int getContainsBmp() {
            return containsBmp;
        }

        /**
         * Determine whether this character class contains NONE, SOME, or ALL of the non-BMP characters
         * @return NONE, SOME, or ALL
         */

        public int getContainsNonBmp() {
            return containsNonBmp;
        }

        /**
         * Output a representation of this character class to the supplied buffer
         * @param buf the supplied buffer
         */

        public final void output(FastStringBuffer buf) {
            switch (containsNonBmp) {
                case NONE:
                    if (containsBmp == NONE) {
                        buf.append(NOT_ALLOWED_CLASS);
                    } else {
                        outputBmp(buf);
                    }
                    break;
                case ALL:
                    buf.append("(?:");
                    if (containsBmp == NONE) {
                        buf.append(SURROGATES1_CLASS);
                        buf.append(SURROGATES2_CLASS);
                    } else {
                        outputBmp(buf);
                        buf.append(SURROGATES2_CLASS);
                        buf.append('?');
                    }
                    buf.append(')');
                    break;
                case SOME:
                    buf.append("(?:");
                    boolean needSep = false;
                    if (containsBmp != NONE) {
                        needSep = true;
                        outputBmp(buf);
                    }
                    List ranges = new ArrayList(10);
                    addNonBmpRanges(ranges);
                    sortRangeList(ranges);
                    String hi = highSurrogateRanges(ranges);
                    if (hi.length() > 0) {
                        if (needSep) {
                            buf.append('|');
                        } else {
                            needSep = true;
                        }
                        buf.append('[');
                        for (int i = 0, len = hi.length(); i < len; i += 2) {
                            char min = hi.charAt(i);
                            char max = hi.charAt(i + 1);
                            if (min == max) {
                                buf.append(min);
                            } else {
                                buf.append(min);
                                buf.append('-');
                                buf.append(max);
                            }
                        }
                        buf.append(']');
                        buf.append(SURROGATES2_CLASS);
                    }
                    String lo = lowSurrogateRanges(ranges);
                    for (int i = 0, len = lo.length(); i < len; i += 3) {
                        if (needSep) {
                            buf.append('|');
                        } else {
                            needSep = true;
                        }
                        buf.append(lo.charAt(i));
                        char min = lo.charAt(i + 1);
                        char max = lo.charAt(i + 2);
                        if (min == max && (i + 3 >= len || lo.charAt(i + 3) != lo.charAt(i))) {
                            buf.append(min);
                        } else {
                            buf.append('[');
                            for (; ;) {
                                if (min == max) {
                                    buf.append(min);
                                } else {
                                    buf.append(min);
                                    buf.append('-');
                                    buf.append(max);
                                }
                                if (i + 3 >= len || lo.charAt(i + 3) != lo.charAt(i)) {
                                    break;
                                }
                                i += 3;
                                min = lo.charAt(i + 1);
                                max = lo.charAt(i + 2);
                            }
                            buf.append(']');
                        }
                    }
                    if (!needSep) {
                        buf.append(NOT_ALLOWED_CLASS);
                    }
                    buf.append(')');
                    break;
            }
        }

        /**
         * Output a representation of the subset of this character class that's within the BMP, to
         * a supplied buffer
         * @param buf the supplied buffer
         */

        public abstract void outputBmp(FastStringBuffer buf);

        /**
         * Output a representation of the complement of the subset of this character class that's within the BMP, to
         * a supplied buffer
         * @param buf the supplied buffer
         */

        public abstract void outputComplementBmp(FastStringBuffer buf);

        /**
         * If this character class contains a single character, get that character
         * @return the single character matched by this character class, or -1 if it matches multiple characters
         */

        public int getSingleChar() {
            return -1;
        }

        /**
         * Add to a supplied List, ranges of non-BMP characters that are matched by this character class.
         * Default implementation does nothing.
         * @param ranges a List to which this method will add zero or more Range objects denoting ranges
         * of non-BMP characters
         */

        public void addNonBmpRanges(List ranges) {
        }
    }

    /**
     * Simple Character Class - essentially, anything other than a Union or Subtraction between two
     * character classes.
     */

    public static abstract class SimpleCharClass extends CharClass {

        /**
         * Create a SimpleCharClass
         * @param containsBmp true if the class includes BMP characters
         * @param containsNonBmp true if the class includes non-BMP characters
         */
        public SimpleCharClass(int containsBmp, int containsNonBmp) {
            super(containsBmp, containsNonBmp);
        }

       /**
         * Output a representation of the subset of this character class that's within the BMP, to
         * a supplied buffer
         * @param buf the supplied buffer
         */

        public void outputBmp(FastStringBuffer buf) {
            buf.append('[');
            inClassOutputBmp(buf);
            buf.append(']');
        }

       /**
         * Output a representation of the subset of this character class that's outwith the BMP, to
         * a supplied buffer. Must not call if containsBmp == ALL
         * @param buf the supplied buffer
         */

        public void outputComplementBmp(FastStringBuffer buf) {
            if (getContainsBmp() == NONE) {
                buf.append("[\u0000-\uFFFF]");
            } else {
                buf.append("[^");
                inClassOutputBmp(buf);
                buf.append(']');
            }
        }

        /**
         * Output a representation of the subset of this character class that's within the BMP, to
         * a supplied buffer, using regex syntax that will be valid within a character class
         * expression (that is, within square brackets)
         * @param buf the supplied buffer
         */

        public abstract void inClassOutputBmp(FastStringBuffer buf);
    }

    /**
     * Character class that matches a single specific character in the BMP
     */

    public static class SingleChar extends SimpleCharClass {
        private final char c;

        /**
         * Create a character class for a single BMP character
         * @param c the character
         */

        public SingleChar(char c) {
            super(SOME, NONE);
            this.c = c;
        }

        /**
         * Get the character represented by this character class
         * @return the character
         */

        public int getSingleChar() {
            return c;
        }

       /**
        * Output a representation of this character class to
        * a supplied buffer @param buf the supplied buffer
        */

        public void outputBmp(FastStringBuffer buf) {
            inClassOutputBmp(buf);
        }

        public void inClassOutputBmp(FastStringBuffer buf) {
            if (isJavaMetaChar(c)) {
                buf.append('\\');
                buf.append(c);
            } else {
                switch (c) {
                    case '\r':
                        buf.append("\\r");
                        break;
                    case '\n':
                        buf.append("\\n");
                        break;
                    case '\t':
                        buf.append("\\t");
                        break;
                    case ' ':
                        buf.append("\\x20");
                        break;
                    default:
                        buf.append(c);
                }
            }
        }

    }

    /**
     * Character class that matches a single specific character outside the BMP
     */

    public static class WideSingleChar extends SimpleCharClass {
        private final int c;

        /**
         * Create a character class for a single non-BMP character
         * @param c the character
         */

        public WideSingleChar(int c) {
            super(NONE, SOME);
            if (c <= 65535) {
                throw new IllegalArgumentException("Internal error: WideSingleChar handles non-BMP characters only");
            }
            this.c = c;
        }

        public void inClassOutputBmp(FastStringBuffer buf) {
            throw new AssertionError("WideSingleChar handles non-BMP characters only");
        }

        public int getSingleChar() {
            return c;
        }

        public void addNonBmpRanges(List ranges) {
            ranges.add(new Range(c, c));
        }
    }

    /**
     * Character class that matches nothing
     */

    public static class Empty extends SimpleCharClass {
        private static final Empty instance = new Empty();

        private Empty() {
            super(NONE, NONE);
        }

        /**
         * Return the singular instance of this class
         * @return the singular instance
         */

        public static Empty getInstance() {
            return instance;
        }

        public void inClassOutputBmp(FastStringBuffer buf) {
            throw new AssertionError("Attempt to output BMP character for empty class");
        }

    }

    /**
     * Character class that matches any character within a range of codepoints
     */

    public static class CharRange extends SimpleCharClass {
        private final int lower;
        private final int upper;

        /**
         * Create a character class for a range of characters
         * @param lower the lower end of the range
         * @param upper the upper end of the range
         */

        public CharRange(int lower, int upper) {
            super(lower < UTF16.NONBMP_MIN ? SOME : NONE,
                    // don't use ALL here, because that requires that the BMP class contains high surrogates
                    upper >= UTF16.NONBMP_MIN ? SOME : NONE);
            this.lower = lower;
            this.upper = upper;
        }

        public void inClassOutputBmp(FastStringBuffer buf) {
            if (lower >= UTF16.NONBMP_MIN)
                throw new RuntimeException("BMP output botch");
            if (isJavaMetaChar((char) lower))
                buf.append('\\');
            buf.append((char) lower);
            buf.append('-');
            if (upper < UTF16.NONBMP_MIN) {
                if (isJavaMetaChar((char) upper))
                    buf.append('\\');
                buf.append((char) upper);
            } else
                buf.append('\uFFFF');
        }

        public void addNonBmpRanges(List ranges) {
            if (upper >= UTF16.NONBMP_MIN)
                ranges.add(new Range(lower < UTF16.NONBMP_MIN ? UTF16.NONBMP_MIN : lower, upper));
        }
    }

    /**
     * Character class containing characters that share a given Unicode property
     */

    public static class Property extends SimpleCharClass {
        private final String name;

        /**
         * Create a character class for a named property
         * @param name the name of the property
         */

        public Property(String name) {
            super(SOME, NONE);
            this.name = name;
        }

        public void outputBmp(FastStringBuffer buf) {
            inClassOutputBmp(buf);
        }

        public void inClassOutputBmp(FastStringBuffer buf) {
            buf.append("\\p{");
            buf.append(name);
            buf.append('}');
        }

        public void outputComplementBmp(FastStringBuffer buf) {
            buf.append("\\P{");
            buf.append(name);
            buf.append('}');
        }
    }

    /**
     * Character class representing a back-reference.
     */

    public static class BackReference extends CharClass {
        private final int i;

        /**
         * Create a character class representing a back reference
         * @param i the subexpression to which this is a backreference
         */

        public BackReference(int i) {
            super(SOME, NONE);
            this.i = i;
        }

        public void outputBmp(FastStringBuffer buf) {
            inClassOutputBmp(buf);
        }

        public void outputComplementBmp(FastStringBuffer buf) {
            inClassOutputBmp(buf);
        }

        void inClassOutputBmp(FastStringBuffer buf) {
            if (i != -1) {
                buf.append("(?:\\" + i + ")");  // terminate the back-reference with a syntactic separator
            } else {
                buf.append("(?:)"); // matches a zero-length string, while allowing a quantifier
            }
        }
    }

    /**
     * Character class representing the characters matched by the XPath "." metacharacter
     */

    public static class Dot extends CharClass {

        /**
         * Create a character class for the "." metacharacter
         */

        public Dot() {
            super(SOME, NONE);  // a deliberate lie
        }

       public void outputBmp(FastStringBuffer buf) {
            buf.append("(?:.|");
            buf.append(SURROGATES1_CLASS);
            buf.append(SURROGATES2_CLASS);
            buf.append(")");
        }

        public void outputComplementBmp(FastStringBuffer buf) {
            buf.append("[^\\n]");
        }

        void inClassOutputBmp(FastStringBuffer buf) {
            buf.append(".");
        }
    }

    /**
     * Character class representing the complement of another character class, that is, all
     * characters that the other class doesn't match.
     */

    public static class Complement extends CharClass {
        private final CharClass cc;

        /**
         * Create a character class representing the complement of another character class
         * @param cc the character class of which this is the complement
         */

        public Complement(CharClass cc) {
            super(-cc.getContainsBmp(), -cc.getContainsNonBmp());
            this.cc = cc;
        }

        public void outputBmp(FastStringBuffer buf) {
            cc.outputComplementBmp(buf);
        }

        public void outputComplementBmp(FastStringBuffer buf) {
            cc.outputBmp(buf);
        }

        public void addNonBmpRanges(List ranges) {
            List tem = new ArrayList(5);
            cc.addNonBmpRanges(tem);
            sortRangeList(tem);
            int c = UTF16.NONBMP_MIN;
            for (int i = 0, len = tem.size(); i < len; i++) {
                Range r = (Range) tem.get(i);
                if (r.getMin() > c)
                    ranges.add(new Range(c, r.getMin() - 1));
                c = r.getMax() + 1;
            }
            if (c != UTF16.NONBMP_MAX + 1)
                ranges.add(new Range(c, UTF16.NONBMP_MAX));
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

