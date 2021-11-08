package org.orbeon.saxon.dotnet;

import org.orbeon.saxon.om.FastStringBuffer;
import org.orbeon.saxon.regex.*;
import org.orbeon.saxon.value.Whitespace;
import org.orbeon.saxon.charcode.UTF16;
import org.orbeon.saxon.charcode.XMLCharacterData;
import org.orbeon.saxon.sort.IntRangeSet;
import org.orbeon.saxon.Configuration;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * This class translates XML Schema regex syntax into .NET regex syntax.
 * Author: James Clark, Thai Open Source Software Center Ltd. See statement at end of file.
 * Modified by Michael Kay (a) to integrate the code into Saxon, (b) to support XPath additions
 * to the XML Schema regex syntax, (c) to target the .NET regex syntax instead of JDK 1.4
 * <p/>
 * This version of the regular expression translator treats each half of a surrogate pair as a separate
 * character, translating anything in an XPath regex that can match a non-BMP character into a Java
 * regex that matches the two halves of a surrogate pair independently. This approach doesn't work
 * under JDK 1.5, whose regex engine treats a surrogate pair as a single character.
 * <p/>
 * This translator is currently used for Saxon on .NET 1.1. It's almost the same as the JDK 1.4 version,
 * except that it avoids use of the "&&" operator, which isn't available on .NET. 
 */
public class DotNetRegexTranslator extends SurrogateRegexTranslator {


    /**
     * Translates XML Schema regexes into <code>java.util.regex</code> regexes.
     *
     * @see java.util.regex.Pattern
     * @see <a href="http://www.w3.org/TR/xmlschema-2/#regexs">XML Schema Part 2</a>
     */

    /**
     * CharClass for each block name in specialBlockNames.
     */
    private static final CharClass[] specialBlockCharClasses = {
        new CharRange(0x10300, 0x1032F),
        new CharRange(0x10330, 0x1034F),
        new CharRange(0x10400, 0x1044F),
        new CharRange(0x1D000, 0x1D0FF),
        new CharRange(0x1D100, 0x1D1FF),
        new CharRange(0x1D400, 0x1D7FF),
        new CharRange(0x20000, 0x2A6D6),
        new CharRange(0x2F800, 0x2FA1F),
        new CharRange(0xE0000, 0xE007F),
        new Union(new CharClass[]{
            new CharRange(0xE000, 0xF8FF),
            new CharRange(0xF0000, 0xFFFFD),
            new CharRange(0x100000, 0x10FFFD)
        }),
        Empty.getInstance(),
        Empty.getInstance(),
        Empty.getInstance()
    };

    private static final CharClass DOT_SCHEMA =
            new Complement(new Union(new CharClass[]{new SingleChar('\n'), new SingleChar('\r')}));

    private static final CharClass DOT_XPATH =
            new Dot();

    private static final CharClass ESC_d = new Property("Nd");

    private static final CharClass ESC_D = new Complement(ESC_d);

    private static final CharClass ESC_W = new Union(new CharClass[]{computeCategoryCharClass('P'),
                                                                     computeCategoryCharClass('Z'),
                                                                     computeCategoryCharClass('C')});
    //was: new Property("P"), new Property("Z"), new Property("C") }

    private static final CharClass ESC_w = new Complement(ESC_W);

    private static final CharClass ESC_s = new Union(new CharClass[]{
        new SingleChar(' '),
        new SingleChar('\n'),
        new SingleChar('\r'),
        new SingleChar('\t')
    });

    private static final CharClass ESC_S = new Complement(ESC_s);

    private static final CharClass ESC_i_10 = makeNameCharClass(XMLCharacterData.NAME_START_10_MASK);

    private static final CharClass ESC_i_11 = makeNameCharClass(XMLCharacterData.NAME_START_11_MASK);

    private static final CharClass ESC_I_10 = new Complement(ESC_i_10);

    private static final CharClass ESC_I_11 = new Complement(ESC_i_11);

    private static final CharClass ESC_c_10 = makeNameCharClass(XMLCharacterData.NAME_10_MASK);

    private static final CharClass ESC_c_11 = makeNameCharClass(XMLCharacterData.NAME_11_MASK);

    private static final CharClass ESC_C_10 = new Complement(ESC_c_10);

    private static final CharClass ESC_C_11 = new Complement(ESC_c_11);

    /**
     * Create a regular expression translator for the .NET platform
     */

    public DotNetRegexTranslator() {
    }

    /**
     * Translates a regular expression in the syntax of XML Schemas Part 2 into a regular
     * expression in the syntax of <code>java.util.regex.Pattern</code>.  The translation
     * assumes that the string to be matched against the regex uses surrogate pairs correctly.
     * If the string comes from XML content, a conforming XML parser will automatically
     * check this; if the string comes from elsewhere, it may be necessary to check
     * surrogate usage before matching.
     *
     * @param regExp a String containing a regular expression in the syntax of XML Schemas Part 2
     * @param xmlVersion the version of XML in use - this affects the meanings of the \i and \c character
     * class escapes
     * @param xpath  a boolean indicating whether the XPath 2.0 F+O extensions to the schema
     *               regex syntax are permitted
     * @param ignoreWhitespace true if the x flag is set, allowing ignorable whitespace in the regex
     * @param caseBlind true if the i flag is set, allowing case blind comparisons
     * @return a String containing a regular expression using the .NET regex syntax. If the xpath
     * parameter is false, the regular expression is designed for XML Schema and it will therefore
     * be implicitly anchored by enclosing it between "^(?:" and ")$"
     * @throws RegexSyntaxException if <code>regexp</code> is not a regular expression in the
     *                              syntax of XML Schemas Part 2, or XPath 2.0, as appropriate
     * @see java.util.regex.Pattern
     * @see <a href="http://www.w3.org/TR/xmlschema-2/#regexs">XML Schema Part 2</a>
     */
    public String translate(CharSequence regExp, int xmlVersion, boolean xpath, boolean ignoreWhitespace, boolean caseBlind)

            throws RegexSyntaxException {
        //System.err.println("Input regex: " + FastStringBuffer.diagnosticPrint(regExp));
        this.regExp = regExp;
        this.xmlVersion = xmlVersion;
        isXPath = xpath;
        this.ignoreWhitespace = ignoreWhitespace;
        this.caseBlind = caseBlind;
        length = regExp.length();
        advance();
        translateTop();
        //System.err.println("Output regex: " + FastStringBuffer.diagnosticPrint(result));
        if (xpath) {
            return result.toString();
        } else {
            return "^(?:" + result.toString() + ")$";
        }
    }

    /**
     * Get the number of captured groups for this regular expression
     * @return the number of captured groups
     */

    public int getNumberOfCapturedGroups() {
        return currentCapture;
    }


    static class Subtraction extends CharClass {
        private final CharClass cc1;
        private final CharClass cc2;

        Subtraction(CharClass cc1, CharClass cc2) {
            // min corresponds to intersection
            // complement corresponds to negation
            super(Math.min(cc1.getContainsBmp(), -cc2.getContainsBmp()),
                    Math.min(cc1.getContainsNonBmp(), -cc2.getContainsNonBmp()));
            this.cc1 = cc1;
            this.cc2 = cc2;
        }
//          following works for Java but not for .NET
//        void outputBmp(FastStringBuffer buf) {
//            buf.append('[');
//            cc1.outputBmp(buf);
//            buf.append("&&");
//            cc2.outputComplementBmp(buf);
//            buf.append(']');
//        }

        // .NET 2.0 supports character class subtraction syntax: [AB-[CD]].
        // But we want to work on .NET 1.1, so we use [AB](?![CD])

        public void outputBmp(FastStringBuffer buf) {
            buf.append("(?!");
            cc2.outputBmp(buf);
            buf.append(")");
            cc1.outputBmp(buf);
//            cc1.outputBmp(buf);
//            buf.append("(?!");
//            cc2.outputBmp(buf);
//            buf.append(")");
        }

//        void outputComplementBmp(FastStringBuffer buf) {
//            buf.append('[');
//            cc1.outputComplementBmp(buf);
//            cc2.outputBmp(buf);
//            buf.append(']');
//        }

        public void outputComplementBmp(FastStringBuffer buf) {
            // not(A and not(B)) => (not(A) or B)
            buf.append("(?:");
            cc1.outputComplementBmp(buf);
            buf.append("|");
            cc2.outputBmp(buf);
            buf.append(')');
        }

        public void addNonBmpRanges(List ranges) {
            List posList = new ArrayList(5);
            cc1.addNonBmpRanges(posList);
            List negList = new ArrayList(5);
            cc2.addNonBmpRanges(negList);
            sortRangeList(posList);
            sortRangeList(negList);
            Iterator negIter = negList.iterator();
            Range negRange;
            if (negIter.hasNext())
                negRange = (Range) negIter.next();
            else
                negRange = null;
            for (int i = 0, len = posList.size(); i < len; i++) {
                Range posRange = (Range) posList.get(i);
                while (negRange != null && negRange.getMax() < posRange.getMin()) {
                    if (negIter.hasNext())
                        negRange = (Range) negIter.next();
                    else
                        negRange = null;
                }
                // if negRange != null, negRange.max >= posRange.min
                int min = posRange.getMin();
                while (negRange != null && negRange.getMin() <= posRange.getMax()) {
                    if (min < negRange.getMin()) {
                        ranges.add(new Range(min, negRange.getMin() - 1));
                    }
                    min = negRange.getMax() + 1;
                    if (min > posRange.getMax())
                        break;
                    if (negIter.hasNext())
                        negRange = (Range) negIter.next();
                    else
                        negRange = null;
                }
                if (min <= posRange.getMax())
                    ranges.add(new Range(min, posRange.getMax()));
            }
        }
    }

    static class Union extends CharClass {
        private final List members;

        Union(CharClass[] v) {
            this(toList(v));
        }

        private static List toList(CharClass[] v) {
            List members = new ArrayList(5);
            for (int i = 0; i < v.length; i++)
                members.add(v[i]);
            return members;
        }

        Union(List members) {
            super(computeContainsBmp(members), computeContainsNonBmp(members));
            this.members = members;
        }

        public void outputBmp(FastStringBuffer buf) {
            boolean allSimpleChars = true;
            for (int i = 0, len = members.size(); i < len; i++) {
                if (!(members.get(i) instanceof SimpleCharClass)) {
                    allSimpleChars = false;
                    break;
                }
            }
            if (allSimpleChars) {
                buf.append('[');
                for (int i = 0, len = members.size(); i < len; i++) {
                    CharClass cc = (CharClass) members.get(i);
                    if (cc.getContainsBmp() != NONE) {
                        ((SimpleCharClass) cc).inClassOutputBmp(buf);
                    }
                }
                buf.append(']');
            } else {
                buf.append("(?:");
                boolean first = true;
                for (int i = 0, len = members.size(); i < len; i++) {
                    CharClass cc = (CharClass) members.get(i);
                    if (cc.getContainsBmp() != NONE) {
                        if (!first) {
                            buf.append('|');
                        }
                        first = false;
                        if (cc instanceof SimpleCharClass) {
                            ((SimpleCharClass) cc).inClassOutputBmp(buf);
                        } else {
                            cc.outputBmp(buf);
                        }
                    }
                }
                buf.append(')');
            }
        }

        public void outputComplementBmp(FastStringBuffer buf) {
            int len = members.size();
            int bmpMembers = 0;
            int simpleChars = 0;
            int nonSimpleChars = 0;
            for (int i = 0; i < len; i++) {
                CharClass cc = (CharClass) members.get(i);
                if (cc.getContainsBmp() != NONE) {
                    bmpMembers++;
                    if (cc instanceof SimpleCharClass) {
                        simpleChars++;
                    } else {
                        nonSimpleChars++;
                    }
                }
            }
            if (bmpMembers == 0) {
                // all members are NONE, so this is NONE, so complement is everything
                buf.append("[\u0000-\uFFFF]");
            } else {
                if (nonSimpleChars > 0) {
                    for (int i = 0; i < len; i++) {
                        CharClass cc = (CharClass) members.get(i);
                        if (cc.getContainsBmp() != NONE && !(cc instanceof SimpleCharClass)) {
                            buf.append("(?=");
                            cc.outputComplementBmp(buf);
                            buf.append(')');
                        }
                    }
                }
                if (simpleChars > 0) {
                    buf.append("[^");
                    for (int i = 0; i < len; i++) {
                        CharClass cc = (CharClass) members.get(i);
                        if (cc.getContainsBmp() != NONE && cc instanceof SimpleCharClass) {
                            ((SimpleCharClass) cc).inClassOutputBmp(buf);
                        }
                    }
                    buf.append(']');
                } else {
                    buf.append("[\u0000?-\uFFFF]");
                }
            }

        }

//        void outputComplementBmpAsConjunction(FastStringBuffer buf) {
//            boolean first = true;
//            int len = members.size();
//            for (int i = 0; i < len; i++) {
//                CharClass cc = (CharClass)members.get(i);
//                if (cc.getContainsBmp() != NONE && cc instanceof SimpleCharClass) {
//                    if (first) {
//                        buf.append("[^");
//                        first = false;
//                    }
//                    ((SimpleCharClass)cc).inClassOutputBmp(buf);
//                }
//            }
//            for (int i = 0; i < len; i++) {
//                CharClass cc = (CharClass)members.get(i);
//                if (cc.getContainsBmp() != NONE && !(cc instanceof SimpleCharClass)) {
//                    if (first) {
//                        buf.append('[');
//                        first = false;
//                    } else {
//                        buf.append("&&");
//                    }
//
//                    // can't have any members that are ALL, because that would make this ALL, which violates
//                    // the precondition for outputComplementBmp
//                    cc.outputComplementBmp(buf);
//                }
//            }
//            if (first == true)
//            // all members are NONE, so this is NONE, so complement is everything
//                buf.append("[\u0000-\uFFFF]");
//            else
//                buf.append(']');
//        }


        public void addNonBmpRanges(List ranges) {
            for (int i = 0, len = members.size(); i < len; i++)
                ((CharClass) members.get(i)).addNonBmpRanges(ranges);
        }

        private static int computeContainsBmp(List members) {
            int ret = NONE;
            for (int i = 0, len = members.size(); i < len; i++)
                ret = Math.max(ret, ((CharClass) members.get(i)).getContainsBmp());
            return ret;
        }

        private static int computeContainsNonBmp(List members) {
            int ret = NONE;
            for (int i = 0, len = members.size(); i < len; i++)
                ret = Math.max(ret, ((CharClass) members.get(i)).getContainsNonBmp());
            return ret;
        }
    }



    protected boolean translateAtom() throws RegexSyntaxException {
        switch (curChar) {
            case RegexData.EOS:
                if (!eos)
                    break;
                // fall through
            case '?':
            case '*':
            case '+':
            case ')':
            case '{':
            case '}':
            case '|':
            case ']':
                return false;
            case '(':
                copyCurChar();
                int thisCapture = ++currentCapture;
                translateRegExp();
                expect(')');
                captures.add(thisCapture);
                copyCurChar();
                return true;
            case '\\':
                advance();
                parseEsc().output(result);
                return true;
            case '[':
                inCharClassExpr = true;
                advance();
                parseCharClassExpr().output(result);
                return true;
            case '.':
                if (isXPath) {
                    DOT_XPATH.output(result);
                    advance();
                    return true;
                } else {
                    DOT_SCHEMA.output(result);
                    advance();
                    return true;
                }
            case '$':
            case '^':
                if (isXPath) {
                    copyCurChar();
                    return true;
                }
                result.append('\\');
                break;
            default:
                if (caseBlind) {
                    int thisChar = absorbSurrogatePair();
                    int[] variants = CaseVariants.getCaseVariants(thisChar);
                    if (variants.length > 0) {
                        CharClass[] chars = new CharClass[variants.length + 1];
                        chars[0] = makeSingleCharClass(thisChar);
                        for (int i = 0; i < variants.length; i++) {
                            chars[i + 1] = makeSingleCharClass(variants[i]);
                        }
                        Union union = new Union(chars);
                        union.output(result);
                        advance();
                        return true;
                    }
                    // else fall through
                }
                // else fall through
        }
        copyCurChar();
        return true;
    }

    private static CharClass makeSingleCharClass(int ch) {
        if (ch <= 65535) {
            return new SingleChar((char) ch);
        } else {
            return new WideSingleChar(ch);
        }
    }

    private static CharClass makeNameCharClass(byte mask) {
        List ranges = new ArrayList();
        // Add colon to the set of characters matched
        ranges.add(new SingleChar(':'));
        // Plus all the characters from the NCName tables
        IntRangeSet members = XMLCharacterData.getCategory(mask);
        int used = members.getNumberOfRanges();
        int[] startPoints = members.getStartPoints();
        int[] endPoints = members.getEndPoints();
        for (int i=0; i<used; i++) {
            if (startPoints[i] == endPoints[i] && i <= 65535) {
                ranges.add(new SingleChar((char)startPoints[i]));
            } else {
                ranges.add(new CharRange(startPoints[i], endPoints[i]));
            }
        }
        return new Union(ranges);
    }




//    private static CharClass makeCharClass(String categories, String includes, String excludeRanges) {
//        List includeList = new ArrayList(5);
//        for (int i = 0, len = categories.length(); i < len; i += 2)
//            includeList.add(new Property(categories.substring(i, i + 2)));
//        for (int i = 0, len = includes.length(); i < len; i++) {
//            int j = i + 1;
//            for (; j < len && includes.charAt(j) - includes.charAt(i) == j - i; j++)
//                ;
//            --j;
//            if (i == j - 1)
//                --j;
//            if (i == j)
//                includeList.add(new SingleChar(includes.charAt(i)));
//            else
//                includeList.add(new CharRange(includes.charAt(i), includes.charAt(j)));
//            i = j;
//        }
//        List excludeList = new ArrayList(5);
//        for (int i = 0, len = excludeRanges.length(); i < len; i += 2) {
//            char min = excludeRanges.charAt(i);
//            char max = excludeRanges.charAt(i + 1);
//            if (min == max)
//                excludeList.add(new SingleChar(min));
//            else if (min == max - 1) {
//                excludeList.add(new SingleChar(min));
//                excludeList.add(new SingleChar(max));
//            } else
//                excludeList.add(new CharRange(min, max));
//        }
//        return new Subtraction(new Union(includeList), new Union(excludeList));
//    }

    private CharClass parseEsc() throws RegexSyntaxException {
        switch (curChar) {
            case 'n':
                advance();
                return new SingleChar('\n');
            case 'r':
                advance();
                return new SingleChar('\r');
            case 't':
                advance();
                return new SingleChar('\t');
            case '\\':
            case '|':
            case '.':
            case '-':
            case '^':
            case '?':
            case '*':
            case '+':
            case '(':
            case ')':
            case '{':
            case '}':
            case '[':
            case ']':
                break;
            case 's':
                advance();
                return ESC_s;
            case 'S':
                advance();
                return ESC_S;
            case 'i':
                advance();
                return (xmlVersion == Configuration.XML10 ? ESC_i_10 : ESC_i_11);
            case 'I':
                advance();
                return (xmlVersion == Configuration.XML10 ? ESC_I_10 : ESC_I_11);
            case 'c':
                advance();
                return (xmlVersion == Configuration.XML10 ? ESC_c_10 : ESC_c_11);
            case 'C':
                advance();
                return (xmlVersion == Configuration.XML10 ? ESC_C_10 : ESC_C_11);
            case 'd':
                advance();
                return ESC_d;
            case 'D':
                advance();
                return ESC_D;
            case 'w':
                advance();
                return ESC_w;
            case 'W':
                advance();
                return ESC_W;
            case 'p':
                advance();
                return parseProp();
            case 'P':
                advance();
                return new Complement(parseProp());
            case '0':
            case '1':
            case '2':
            case '3':
            case '4':
            case '5':
            case '6':
            case '7':
            case '8':
            case '9':
                if (isXPath) {
                    if (inCharClassExpr) {
                        throw makeException("back-reference not allowed within []");
                    }
                    char c = curChar;
                    int c0 = (c - '0');
                    advance();
                    int c1 = "0123456789".indexOf(curChar);
                    if (c1 >= 0) {
                        // limit a back-reference to two digits, but only allow two if there is such a capture
                        int n = c0 * 10 + c1;
                        advance();
                        if (captures.contains(n)) {
                            // treat it as a two-digit back-reference
                            return new BackReference(n);
                        } else {
                            recede();
                        }
                    }
                    if (captures.contains(c0)) {
                        return new BackReference(c0);
                    } else {
                        // Changed by erratum FO.E4 to be an error
                        throw makeException("invalid backreference \\" + c0 + " (no such group)");
                    }
                } else {
                    throw makeException("digit not allowed after \\");
                }
            case '$':
                if (isXPath) {
                    break;
                }
                // otherwise fall through
            default:
                throw makeException("invalid escape sequence");
        }
        CharClass tem = new SingleChar(curChar);
        advance();
        return tem;
    }

    private CharClass parseProp() throws RegexSyntaxException {
        expect('{');
        int start = pos;
        for (; ;) {
            advance();
            if (curChar == '}')
                break;
            if (!isAsciiAlnum(curChar) && curChar != '-')
                expect('}');
        }
        CharSequence propertyNameCS = regExp.subSequence(start, pos - 1);
        if (ignoreWhitespace && !inCharClassExpr) {
            propertyNameCS = Whitespace.removeAllWhitespace(propertyNameCS);
        }
        String propertyName = propertyNameCS.toString();
        advance();
        switch (propertyName.length()) {
            case 0:
                throw makeException("empty property name");
            case 2:
                int sci = RegexData.subCategories.indexOf(propertyName);
                if (sci < 0 || sci % 2 == 1)
                    throw makeException("unknown category");
                return getSubCategoryCharClass(sci / 2);
            case 1:
                int ci = RegexData.categories.indexOf(propertyName.charAt(0));
                if (ci < 0)
                    throw makeException("unknown category", propertyName);
                return getCategoryCharClass(ci);
            default:
                if (!propertyName.startsWith("Is"))
                    break;
                String blockName = propertyName.substring(2);
                for (int i = 0; i < RegexData.specialBlockNames.length; i++)
                    if (blockName.equals(RegexData.specialBlockNames[i]))
                        return specialBlockCharClasses[i];
                if (!isBlock(blockName))
                    throw makeException("invalid block name", blockName);
                return new Property("Is" + blockName);
        }
        throw makeException("invalid property name", propertyName);
    }

    private CharClass parseCharClassExpr() throws RegexSyntaxException {
        boolean compl;
        if (curChar == '^') {
            advance();
            compl = true;
        } else {
            compl = false;
        }
        List members = new ArrayList(10);
        do {
            CharClass lower = parseCharClassEscOrXmlChar();
            members.add(lower);
            if (curChar == ']' || eos) {
                //addCaseVariant(lower, members);
                break;
            }
            //firstOrLast = isLastInGroup();
            if (curChar == '-') {
                char next = regExp.charAt(pos);
                if (next == '[') {
                    // hyphen denotes subtraction
                    // addCaseVariant(lower, members);
                    advance();
                    break;
                } else if (next == ']') {
                    // hyphen denotes a regular character - no need to do anything
                    // addCaseVariant(lower, members);
                    // Note: the spec rules are unclear here. We are allowing hyphen to represent
                    // itself in contexts like [A-Z-0-9]
                } else {
                    // hyphen denotes a character range
                    advance();
                    CharClass upper = parseCharClassEscOrXmlChar();
                    if (lower.getSingleChar() < 0 || upper.getSingleChar() < 0)
                        throw makeException("the ends of a range must be single characters");
                    if (lower.getSingleChar() > upper.getSingleChar())
                        throw makeException("invalid range (start > end)");
                    members.set(members.size() - 1,
                            new CharRange(lower.getSingleChar(), upper.getSingleChar()));
                    if (caseBlind) {
                        // Special-case A-Z and a-z
                        if (lower.getSingleChar() == 'a' && upper.getSingleChar() == 'z') {
                            members.add(new CharRange('A', 'Z'));
                            for (int v = 0; v < CaseVariants.ROMAN_VARIANTS.length; v++) {
                                members.add(makeSingleCharClass(CaseVariants.ROMAN_VARIANTS[v]));
                            }
                        } else if (lower.getSingleChar() == 'A' && upper.getSingleChar() == 'Z') {
                            members.add(new CharRange('a', 'z'));
                            for (int v = 0; v < CaseVariants.ROMAN_VARIANTS.length; v++) {
                                members.add(makeSingleCharClass(CaseVariants.ROMAN_VARIANTS[v]));
                            }
                        } else {
                            for (int k = lower.getSingleChar(); k <= upper.getSingleChar(); k++) {
                                int[] variants = CaseVariants.getCaseVariants(k);
                                for (int v = 0; v < variants.length; v++) {
                                    members.add(makeSingleCharClass(variants[v]));
                                }
                            }
                        }
                    }
                    // look for a subtraction
                    if (curChar == '-' && regExp.charAt(pos) == '[') {
                        advance();
                        expect('[');
                        break;
                    }
                }
            } else {
                // addCaseVariant(lower, members);
            }
        } while (curChar != ']');
        if (eos) {
            expect(']');
        }
        CharClass result;
        if (members.size() == 1)
            result = (CharClass) members.get(0);
        else
            result = new Union(members);
        if (compl)
            result = new Complement(result);
        if (curChar == '[') {
            advance();
            result = new Subtraction(result, parseCharClassExpr());
            expect(']');
        }
        inCharClassExpr = false;
        advance();
        return result;
    }

    private CharClass parseCharClassEscOrXmlChar() throws RegexSyntaxException {
        switch (curChar) {
            case RegexData.EOS:
                if (eos)
                    expect(']');
                break;
            case '\\':
                advance();
                return parseEsc();
            case '[':
            case ']':
                throw makeException("character must be escaped", new String(new char[]{curChar}));
            case '-':
                break;
        }
        CharClass tem;
        if (UTF16.isSurrogate(curChar)) {
            if (!UTF16.isHighSurrogate(curChar))
                throw makeException("invalid surrogate pair");
            char c1 = curChar;
            advance();
            if (!UTF16.isLowSurrogate(curChar))
                throw makeException("invalid surrogate pair");
            tem = new WideSingleChar(UTF16.combinePair(c1, curChar));
        } else
            tem = new SingleChar(curChar);
        advance();
        return tem;
    }

    private static synchronized CharClass getCategoryCharClass(int ci) {
        if (categoryCharClasses[ci] == null)
            categoryCharClasses[ci] = computeCategoryCharClass(RegexData.categories.charAt(ci));
        return categoryCharClasses[ci];
    }

    private static synchronized CharClass getSubCategoryCharClass(int sci) {
        if (subCategoryCharClasses[sci] == null)
            subCategoryCharClasses[sci] = computeSubCategoryCharClass(RegexData.subCategories.substring(sci * 2, (sci + 1) * 2));
        return subCategoryCharClasses[sci];
    }


    private static CharClass computeCategoryCharClass(char code) {
        List classes = new ArrayList(5);
        classes.add(new Property(new String(new char[]{code})));
        for (int ci = RegexData.CATEGORY_NAMES.indexOf(code); ci >= 0; ci = RegexData.CATEGORY_NAMES.indexOf(code, ci + 1)) {
            int[] addRanges = RegexData.CATEGORY_RANGES[ci / 2];
            for (int i = 0; i < addRanges.length; i += 2)
                classes.add(new CharRange(addRanges[i], addRanges[i + 1]));
        }
        if (code == 'P')
            classes.add(makeCharClass(RegexData.CATEGORY_Pi + RegexData.CATEGORY_Pf));
        if (code == 'L') {
            classes.add(new SingleChar(RegexData.UNICODE_3_1_ADD_Ll));
            classes.add(new SingleChar(RegexData.UNICODE_3_1_ADD_Lu));
        }
        if (code == 'C') {
            // JDK 1.4 leaves Cn out of C?
            classes.add(new Subtraction(new Property("Cn"),
                    new Union(new CharClass[]{new SingleChar(RegexData.UNICODE_3_1_ADD_Lu),
                                              new SingleChar(RegexData.UNICODE_3_1_ADD_Ll)})));
            List assignedRanges = new ArrayList(5);
            for (int i = 0; i < RegexData.CATEGORY_RANGES.length; i++)
                for (int j = 0; j < RegexData.CATEGORY_RANGES[i].length; j += 2)
                    assignedRanges.add(new CharRange(RegexData.CATEGORY_RANGES[i][j],
                            RegexData.CATEGORY_RANGES[i][j + 1]));
            classes.add(new Subtraction(new CharRange(UTF16.NONBMP_MIN, UTF16.NONBMP_MAX),
                    new Union(assignedRanges)));
        }
        if (classes.size() == 1)
            return (CharClass) classes.get(0);
        return new Union(classes);
    }

    private static CharClass computeSubCategoryCharClass(String name) {
        CharClass base = new Property(name);
        int sci = RegexData.CATEGORY_NAMES.indexOf(name);
        if (sci < 0) {
            if (name.equals("Cn")) {
                // Unassigned
                List assignedRanges = new ArrayList(5);
                assignedRanges.add(new SingleChar(RegexData.UNICODE_3_1_ADD_Lu));
                assignedRanges.add(new SingleChar(RegexData.UNICODE_3_1_ADD_Ll));
                for (int i = 0; i < RegexData.CATEGORY_RANGES.length; i++)
                    for (int j = 0; j < RegexData.CATEGORY_RANGES[i].length; j += 2)
                        assignedRanges.add(new CharRange(RegexData.CATEGORY_RANGES[i][j],
                                RegexData.CATEGORY_RANGES[i][j + 1]));
                return new Subtraction(new Union(new CharClass[]{base,
                                                                 new CharRange(UTF16.NONBMP_MIN, UTF16.NONBMP_MAX)}),
                        new Union(assignedRanges));
            }
            if (name.equals("Pi"))
                return makeCharClass(RegexData.CATEGORY_Pi);
            if (name.equals("Pf"))
                return makeCharClass(RegexData.CATEGORY_Pf);
            return base;
        }
        List classes = new ArrayList(5);
        classes.add(base);
        int[] addRanges = RegexData.CATEGORY_RANGES[sci / 2];
        for (int i = 0; i < addRanges.length; i += 2)
            classes.add(new CharRange(addRanges[i], addRanges[i + 1]));
        if (name.equals("Lu"))
            classes.add(new SingleChar(RegexData.UNICODE_3_1_ADD_Lu));
        else if (name.equals("Ll"))
            classes.add(new SingleChar(RegexData.UNICODE_3_1_ADD_Ll));
        else if (name.equals("Nl"))
            classes.add(new CharRange(RegexData.UNICODE_3_1_CHANGE_No_to_Nl_MIN, RegexData.UNICODE_3_1_CHANGE_No_to_Nl_MAX));
        else if (name.equals("No"))
            return new Subtraction(new Union(classes),
                    new CharRange(RegexData.UNICODE_3_1_CHANGE_No_to_Nl_MIN,
                            RegexData.UNICODE_3_1_CHANGE_No_to_Nl_MAX));
        return new Union(classes);
    }

    private static CharClass makeCharClass(String members) {
        List list = new ArrayList(5);
        for (int i = 0, len = members.length(); i < len; i++)
            list.add(new SingleChar(members.charAt(i)));
        return new Union(list);
    }

    /**
     * Convenience main method for testing purposes. Note that the actual testing is done using the
     * Java regex engine.
     *
     * @param args: (1) the regex, (2) xpath|schema, (3) target string to be matched
     * @throws RegexSyntaxException
     */

    public static void main(String[] args) throws RegexSyntaxException {
        System.err.println(FastStringBuffer.diagnosticPrint(args[0]));
        String s = new DotNetRegexTranslator().translate(
                args[0], Configuration.XML11, "xpath".equals(args[1]), false, true);
        System.err.println(FastStringBuffer.diagnosticPrint(s));
//        Pattern pattern = Pattern.compile(s);
//        if (args.length > 2) {
//            if ("xpath".equals(args[1])) {
//                System.err.println("Matches? " + pattern.matcher(args[2]).find());
//            } else {
//                System.err.println("Matches? " + pattern.matcher(args[2]).matches());
//            }
//        }
    }

}

/*
Copyright (c) 2001-2003 Thai Open Source Software Center Ltd
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are
met:

    Redistributions of source code must retain the above copyright
    notice, this list of conditions and the following disclaimer.

    Redistributions in binary form must reproduce the above copyright
    notice, this list of conditions and the following disclaimer in
    the documentation and/or other materials provided with the
    distribution.

    Neither the name of the Thai Open Source Software Center Ltd nor
    the names of its contributors may be used to endorse or promote
    products derived from this software without specific prior written
    permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
"AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE REGENTS OR
CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

 */

//
// The contents of this file are subject to the Mozilla Public License Version 1.0 (the "License");
// you may not use this file except in compliance with the License. You may obtain a copy of the
// License at http://www.mozilla.org/MPL/
//
// Software distributed under the License is distributed on an "AS IS" basis,
// WITHOUT WARRANTY OF ANY KIND, either express or implied.
// See the License for the specific language governing rights and limitations under the License.
//
// The Original Code is: all this file except changes marked.
//
// The Initial Developer of the Original Code is James Clark
//
// Portions created by (your name) are Copyright (C) (your legal entity). All Rights Reserved.
//
// Contributor(s): Michael Kay
//

