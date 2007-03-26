package org.orbeon.saxon.regex;

import org.orbeon.saxon.Err;
import org.orbeon.saxon.om.FastStringBuffer;
import org.orbeon.saxon.om.XMLChar;
import org.orbeon.saxon.sort.IntHashSet;
import org.orbeon.saxon.value.Whitespace;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * This class translates XML Schema regex syntax into JDK 1.5 regex syntax. This differs from the JDK 1.4
 * translator because JDK 1.5 handles non-BMP characters (wide characters) in places where JDK 1.4 does not,
 * for example in a range such as [X-Y]. This enables much of the code from the 1.4 translator to be
 * removed.
 * Author: James Clark
 * Modified by Michael Kay (a) to integrate the code into Saxon, and (b) to support XPath additions
 * to the XML Schema regex syntax. This version also removes most of the complexities of handling non-BMP
 * characters, since JDK 1.5 handles these natively.
 */
public class JDK15RegexTranslator {

    // TODO: retrofit the changes for handling caseBlind comparison to the JDK 1.4 translator


    /**
     * Translates XML Schema and XPath regexes into <code>java.util.regex</code> regexes.
     *
     * @see java.util.regex.Pattern
     * @see <a href="http://www.w3.org/TR/xmlschema-2/#regexs">XML Schema Part 2</a>
     */

    private final CharSequence regExp;
    private boolean isXPath;
    private boolean ignoreWhitespace;
    private boolean inCharClassExpr;
    private boolean caseBlind;
    private int pos = 0;
    private final int length;
    private char curChar;
    private boolean eos = false;
    private int currentCapture = 0;
    private IntHashSet captures = new IntHashSet();
    private final FastStringBuffer result = new FastStringBuffer(32);

    private static final String categories = "LMNPZSC";
    private static final CharClass[] categoryCharClasses = new CharClass[categories.length()];
    private static final String subCategories = "LuLlLtLmLoMnMcMeNdNlNoPcPdPsPePiPfPoZsZlZpSmScSkSoCcCfCoCn";
    private static final CharClass[] subCategoryCharClasses = new CharClass[subCategories.length() / 2];

    private static final int NONBMP_MIN = 0x10000;
    private static final int NONBMP_MAX = 0x10FFFF;

    //static final Localizer localizer = new Localizer(RegexTranslator.class);

    private static final String[] blockNames = {
        "BasicLatin",
        "Latin-1Supplement",
        "LatinExtended-A",
        "LatinExtended-B",
        "IPAExtensions",
        "SpacingModifierLetters",
        "CombiningDiacriticalMarks",
        "Greek",
        "Cyrillic",
        "Armenian",
        "Hebrew",
        "Arabic",
        "Syriac",
        "Thaana",
        "Devanagari",
        "Bengali",
        "Gurmukhi",
        "Gujarati",
        "Oriya",
        "Tamil",
        "Telugu",
        "Kannada",
        "Malayalam",
        "Sinhala",
        "Thai",
        "Lao",
        "Tibetan",
        "Myanmar",
        "Georgian",
        "HangulJamo",
        "Ethiopic",
        "Cherokee",
        "UnifiedCanadianAboriginalSyllabics",
        "Ogham",
        "Runic",
        "Khmer",
        "Mongolian",
        "LatinExtendedAdditional",
        "GreekExtended",
        "GeneralPunctuation",
        "SuperscriptsandSubscripts",
        "CurrencySymbols",
        "CombiningMarksforSymbols",
        "LetterlikeSymbols",
        "NumberForms",
        "Arrows",
        "MathematicalOperators",
        "MiscellaneousTechnical",
        "ControlPictures",
        "OpticalCharacterRecognition",
        "EnclosedAlphanumerics",
        "BoxDrawing",
        "BlockElements",
        "GeometricShapes",
        "MiscellaneousSymbols",
        "Dingbats",
        "BraillePatterns",
        "CJKRadicalsSupplement",
        "KangxiRadicals",
        "IdeographicDescriptionCharacters",
        "CJKSymbolsandPunctuation",
        "Hiragana",
        "Katakana",
        "Bopomofo",
        "HangulCompatibilityJamo",
        "Kanbun",
        "BopomofoExtended",
        "EnclosedCJKLettersandMonths",
        "CJKCompatibility",
        "CJKUnifiedIdeographsExtensionA",
        "CJKUnifiedIdeographs",
        "YiSyllables",
        "YiRadicals",
        "HangulSyllables",
        // surrogates excluded because there are never any *characters* with codes in surrogate range
        // "PrivateUse", excluded because 3.1 adds non-BMP ranges
        "CJKCompatibilityIdeographs",
        "AlphabeticPresentationForms",
        "ArabicPresentationForms-A",
        "CombiningHalfMarks",
        "CJKCompatibilityForms",
        "SmallFormVariants",
        "ArabicPresentationForms-B",
        "Specials",
        "HalfwidthandFullwidthForms",
        "Specials"
    };


    /**
     * Names of blocks including ranges outside the BMP.
     */
    private static final String[] specialBlockNames = {
        "OldItalic",
        "Gothic",
        "Deseret",
        "ByzantineMusicalSymbols",
        "MusicalSymbols",
        "MathematicalAlphanumericSymbols",
        "CJKUnifiedIdeographsExtensionB",
        "CJKCompatibilityIdeographsSupplement",
        "Tags",
        "PrivateUse",
        "HighSurrogates",
        "HighPrivateUseSurrogates",
        "LowSurrogates",
    };

// This file was automatically generated by CategoriesGen

    static final String CATEGORY_NAMES = "NoLoMnCfLlNlPoLuMcNdSoSmCo";

    static final int[][] CATEGORY_RANGES = {
        {
            // No
            0x10107, 0x10133,
            0x10320, 0x10323
        },
        {
            // Lo
            0x10000, 0x1000b,
            0x1000d, 0x10026,
            0x10028, 0x1003a,
            0x1003c, 0x1003d,
            0x1003f, 0x1004d,
            0x10050, 0x1005d,
            0x10080, 0x100fa,
            0x10300, 0x1031e,
            0x10330, 0x10349,
            0x10380, 0x1039d,
            0x10450, 0x1049d,
            0x10800, 0x10805,
            0x10808, 0x10808,
            0x1080a, 0x10835,
            0x10837, 0x10838,
            0x1083c, 0x1083c,
            0x1083f, 0x1083f,
            0x20000, 0x2a6d6,
            0x2f800, 0x2fa1d
        },
        {
            // Mn
            0x1d167, 0x1d169,
            0x1d17b, 0x1d182,
            0x1d185, 0x1d18b,
            0x1d1aa, 0x1d1ad,
            0xe0100, 0xe01ef
        },
        {
            // Cf
            0x1d173, 0x1d17a,
            0xe0001, 0xe0001,
            0xe0020, 0xe007f
        },
        {
            // Ll
            0x10428, 0x1044f,
            0x1d41a, 0x1d433,
            0x1d44e, 0x1d454,
            0x1d456, 0x1d467,
            0x1d482, 0x1d49b,
            0x1d4b6, 0x1d4b9,
            0x1d4bb, 0x1d4bb,
            0x1d4bd, 0x1d4c3,
            0x1d4c5, 0x1d4cf,
            0x1d4ea, 0x1d503,
            0x1d51e, 0x1d537,
            0x1d552, 0x1d56b,
            0x1d586, 0x1d59f,
            0x1d5ba, 0x1d5d3,
            0x1d5ee, 0x1d607,
            0x1d622, 0x1d63b,
            0x1d656, 0x1d66f,
            0x1d68a, 0x1d6a3,
            0x1d6c2, 0x1d6da,
            0x1d6dc, 0x1d6e1,
            0x1d6fc, 0x1d714,
            0x1d716, 0x1d71b,
            0x1d736, 0x1d74e,
            0x1d750, 0x1d755,
            0x1d770, 0x1d788,
            0x1d78a, 0x1d78f,
            0x1d7aa, 0x1d7c2,
            0x1d7c4, 0x1d7c9
        },
        {
            // Nl
            0x1034a, 0x1034a
        },
        {
            // Po
            0x10100, 0x10101,
            0x1039f, 0x1039f
        },
        {
            // Lu
            0x10400, 0x10427,
            0x1d400, 0x1d419,
            0x1d434, 0x1d44d,
            0x1d468, 0x1d481,
            0x1d49c, 0x1d49c,
            0x1d49e, 0x1d49f,
            0x1d4a2, 0x1d4a2,
            0x1d4a5, 0x1d4a6,
            0x1d4a9, 0x1d4ac,
            0x1d4ae, 0x1d4b5,
            0x1d4d0, 0x1d4e9,
            0x1d504, 0x1d505,
            0x1d507, 0x1d50a,
            0x1d50d, 0x1d514,
            0x1d516, 0x1d51c,
            0x1d538, 0x1d539,
            0x1d53b, 0x1d53e,
            0x1d540, 0x1d544,
            0x1d546, 0x1d546,
            0x1d54a, 0x1d550,
            0x1d56c, 0x1d585,
            0x1d5a0, 0x1d5b9,
            0x1d5d4, 0x1d5ed,
            0x1d608, 0x1d621,
            0x1d63c, 0x1d655,
            0x1d670, 0x1d689,
            0x1d6a8, 0x1d6c0,
            0x1d6e2, 0x1d6fa,
            0x1d71c, 0x1d734,
            0x1d756, 0x1d76e,
            0x1d790, 0x1d7a8
        },
        {
            // Mc
            0x1d165, 0x1d166,
            0x1d16d, 0x1d172
        },
        {
            // Nd
            0x104a0, 0x104a9,
            0x1d7ce, 0x1d7ff
        },
        {
            // So
            0x10102, 0x10102,
            0x10137, 0x1013f,
            0x1d000, 0x1d0f5,
            0x1d100, 0x1d126,
            0x1d12a, 0x1d164,
            0x1d16a, 0x1d16c,
            0x1d183, 0x1d184,
            0x1d18c, 0x1d1a9,
            0x1d1ae, 0x1d1dd,
            0x1d300, 0x1d356
        },
        {
            // Sm
            0x1d6c1, 0x1d6c1,
            0x1d6db, 0x1d6db,
            0x1d6fb, 0x1d6fb,
            0x1d715, 0x1d715,
            0x1d735, 0x1d735,
            0x1d74f, 0x1d74f,
            0x1d76f, 0x1d76f,
            0x1d789, 0x1d789,
            0x1d7a9, 0x1d7a9,
            0x1d7c3, 0x1d7c3
        },
        {
            // Co
            0xf0000, 0xffffd,
            0x100000, 0x10fffd
        }
    };

    // end of generated code

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

// This file was automatically generated by NamingExceptionsGen
// class NamingExceptions {
    static final String NMSTRT_INCLUDES =
            "\u003A\u005F\u02BB\u02BC\u02BD\u02BE\u02BF\u02C0\u02C1\u0559" +
            "\u06E5\u06E6\u212E";
    static final String NMSTRT_EXCLUDE_RANGES =
            "\u00AA\u00BA\u0132\u0133\u013F\u0140\u0149\u0149\u017F\u017F" +
            "\u01C4\u01CC\u01F1\u01F3\u01F6\u01F9\u0218\u0233\u02A9\u02AD" +
            "\u03D7\u03D7\u03DB\u03DB\u03DD\u03DD\u03DF\u03DF\u03E1\u03E1" +
            "\u0400\u0400\u040D\u040D\u0450\u0450\u045D\u045D\u048C\u048F" +
            "\u04EC\u04ED\u0587\u0587\u06B8\u06B9\u06BF\u06BF\u06CF\u06CF" +
            "\u06FA\u07A5\u0950\u0950\u0AD0\u0AD0\u0D85\u0DC6\u0E2F\u0E2F" +
            "\u0EAF\u0EAF\u0EDC\u0F00\u0F6A\u1055\u1101\u1101\u1104\u1104" +
            "\u1108\u1108\u110A\u110A\u110D\u110D\u1113\u113B\u113D\u113D" +
            "\u113F\u113F\u1141\u114B\u114D\u114D\u114F\u114F\u1151\u1153" +
            "\u1156\u1158\u1162\u1162\u1164\u1164\u1166\u1166\u1168\u1168" +
            "\u116A\u116C\u116F\u1171\u1174\u1174\u1176\u119D\u119F\u11A2" +
            "\u11A9\u11AA\u11AC\u11AD\u11B0\u11B6\u11B9\u11B9\u11BB\u11BB" +
            "\u11C3\u11EA\u11EC\u11EF\u11F1\u11F8\u1200\u18A8\u207F\u2124" +
            "\u2128\u2128\u212C\u212D\u212F\u217F\u2183\u3006\u3038\u303A" +
            "\u3131\u4DB5\uA000\uA48C\uF900\uFFDC";
    static final String NMSTRT_CATEGORIES = "LlLuLoLtNl";
    static final String NMCHAR_INCLUDES =
            "\u002D\u002E\u003A\u005F\u00B7\u0387\u212E";
    static final String NMCHAR_EXCLUDE_RANGES =
            "\u00AA\u00B5\u00BA\u00BA\u0132\u0133\u013F\u0140\u0149\u0149" +
            "\u017F\u017F\u01C4\u01CC\u01F1\u01F3\u01F6\u01F9\u0218\u0233" +
            "\u02A9\u02B8\u02E0\u02EE\u0346\u034E\u0362\u037A\u03D7\u03D7" +
            "\u03DB\u03DB\u03DD\u03DD\u03DF\u03DF\u03E1\u03E1\u0400\u0400" +
            "\u040D\u040D\u0450\u0450\u045D\u045D\u0488\u048F\u04EC\u04ED" +
            "\u0587\u0587\u0653\u0655\u06B8\u06B9\u06BF\u06BF\u06CF\u06CF" +
            "\u06FA\u07B0\u0950\u0950\u0AD0\u0AD0\u0D82\u0DF3\u0E2F\u0E2F" +
            "\u0EAF\u0EAF\u0EDC\u0F00\u0F6A\u0F6A\u0F96\u0F96\u0FAE\u0FB0" +
            "\u0FB8\u0FB8\u0FBA\u1059\u1101\u1101\u1104\u1104\u1108\u1108" +
            "\u110A\u110A\u110D\u110D\u1113\u113B\u113D\u113D\u113F\u113F" +
            "\u1141\u114B\u114D\u114D\u114F\u114F\u1151\u1153\u1156\u1158" +
            "\u1162\u1162\u1164\u1164\u1166\u1166\u1168\u1168\u116A\u116C" +
            "\u116F\u1171\u1174\u1174\u1176\u119D\u119F\u11A2\u11A9\u11AA" +
            "\u11AC\u11AD\u11B0\u11B6\u11B9\u11B9\u11BB\u11BB\u11C3\u11EA" +
            "\u11EC\u11EF\u11F1\u11F8\u1200\u18A9\u207F\u207F\u20DD\u20E0" +
            "\u20E2\u2124\u2128\u2128\u212C\u212D\u212F\u217F\u2183\u2183" +
            "\u3006\u3006\u3038\u303A\u3131\u4DB5\uA000\uA48C\uF900\uFFDC";
    static final String NMCHAR_CATEGORIES = "LlLuLoLtNlMcMeMnLmNd";
// end of generated code

    private static final CharClass ESC_S = new Complement(ESC_s);

    private static final CharClass ESC_i = makeCharClass(NMSTRT_CATEGORIES,
            NMSTRT_INCLUDES,
            NMSTRT_EXCLUDE_RANGES);

    private static final CharClass ESC_I = new Complement(ESC_i);

    private static final CharClass ESC_c = makeCharClass(NMCHAR_CATEGORIES,
            NMCHAR_INCLUDES,
            NMCHAR_EXCLUDE_RANGES);

    private static final CharClass ESC_C = new Complement(ESC_c);

    private static final char EOS = '\0';

    private JDK15RegexTranslator(CharSequence regExp) {
        this.regExp = regExp;
        this.length = regExp.length();
        advance();
    }

    /**
     * Translates a regular expression in the syntax of XML Schemas Part 2 into a regular
     * expression in the syntax of <code>java.util.regex.Pattern</code>.  The translation
     * assumes that the string to be matched against the regex uses surrogate pairs correctly.
     * If the string comes from XML content, a conforming XML parser will automatically
     * check this; if the string comes from elsewhere, it may be necessary to check
     * surrogate usage before matching.
     *
     * @param regexp a String containing a regular expression in the syntax of XML Schemas Part 2
     * @param xpath  a boolean indicating whether the XPath 2.0 F+O extensions to the schema
     *               regex syntax are permitted
     * @throws RegexSyntaxException if <code>regexp</code> is not a regular expression in the
     *                              syntax of XML Schemas Part 2, or XPath 2.0, as appropriate
     * @see java.util.regex.Pattern
     * @see <a href="http://www.w3.org/TR/xmlschema-2/#regexs">XML Schema Part 2</a>
     */
    public static String translate(CharSequence regexp, boolean xpath, boolean ignoreWhitespace, boolean caseBlind)
            throws RegexSyntaxException {

        //System.err.println("Input regex: " + regexp);
        JDK15RegexTranslator tr = new JDK15RegexTranslator(regexp);
        tr.isXPath = xpath;
        tr.ignoreWhitespace = ignoreWhitespace;
        tr.caseBlind = caseBlind;
        tr.translateTop();
        //System.err.println("Output regex: " + tr.result.toString());
        return tr.result.toString();
    }

    private void advance() {
        if (pos < length) {
            curChar = regExp.charAt(pos++);
            if (ignoreWhitespace && !inCharClassExpr) {
                while (Whitespace.isWhitespace(curChar)) {
                    advance();
                }
            }
        } else {
            pos++;
            curChar = EOS;
            eos = true;
        }
    }

    private int absorbSurrogatePair() throws RegexSyntaxException {
        if (XMLChar.isSurrogate(curChar)) {
            if (!XMLChar.isHighSurrogate(curChar))
                throw makeException("invalid surrogate pair");
            char c1 = curChar;
            advance();
            if (!XMLChar.isLowSurrogate(curChar))
                throw makeException("invalid surrogate pair");
            return XMLChar.supplemental(c1, curChar);
        } else {
            return curChar;
        }
    }

    private void recede() {
        // The caller must ensure we don't fall off the start of the expression
        if (eos) {
            curChar = regExp.charAt(length - 1);
            pos = length;
            eos = false;
        } else {
            curChar = regExp.charAt(--pos);
        }
        if (ignoreWhitespace && !inCharClassExpr) {
            while (Whitespace.isWhitespace(curChar)) {
                recede();
            }
        }
    }


    private void translateTop() throws RegexSyntaxException {
        translateRegExp();
        if (!eos)
            throw makeException("expected end of string");
    }

    private void translateRegExp() throws RegexSyntaxException {
        translateBranch();
        while (curChar == '|') {
            copyCurChar();
            translateBranch();
        }
    }

    private void translateBranch() throws RegexSyntaxException {
        while (translateAtom())
            translateQuantifier();
    }

    private void translateQuantifier() throws RegexSyntaxException {
        switch (curChar) {
            case '*':
            case '?':
            case '+':
                copyCurChar();
                break;
            case '{':
                copyCurChar();
                translateQuantity();
                expect('}');
                copyCurChar();
                break;
            default:
                return;
        }
        if (curChar == '?' && isXPath) {
            copyCurChar();
        }
    }

    private void translateQuantity() throws RegexSyntaxException {
        String lower = parseQuantExact().toString();
        int lowerValue = -1;
        try {
            lowerValue = Integer.parseInt(lower);
            result.append(lower);
        } catch (NumberFormatException e) {
            // JDK 1.4 cannot handle ranges bigger than this
            result.append("" + Integer.MAX_VALUE);
        }
        if (curChar == ',') {
            copyCurChar();
            if (curChar != '}') {
                String upper = parseQuantExact().toString();
                try {
                    int upperValue = Integer.parseInt(upper);
                    result.append(upper);
                    if (lowerValue < 0 || upperValue < lowerValue)
                        throw makeException("invalid range in quantifier");
                } catch (NumberFormatException e) {
                    result.append("" + Integer.MAX_VALUE);
                    if (lowerValue < 0 && new BigDecimal(lower).compareTo(new BigDecimal(upper)) > 0)
                        throw makeException("invalid range in quantifier");
                }
            }
        }
    }

    private CharSequence parseQuantExact() throws RegexSyntaxException {
        FastStringBuffer buf = new FastStringBuffer(10);
        do {
            if ("0123456789".indexOf(curChar) < 0)
                throw makeException("expected digit in quantifier");
            buf.append(curChar);
            advance();
        } while (curChar != ',' && curChar != '}');
        return buf;
    }

    private void copyCurChar() {
        result.append(curChar);
        advance();
    }

    static final int NONE = -1;
    static final int SOME = 0;
    static final int ALL = 1;

    static final String SURROGATES1_CLASS = "[\uD800-\uDBFF]";
    static final String SURROGATES2_CLASS = "[\uDC00-\uDFFF]";
    static final String NOT_ALLOWED_CLASS = "[\u0000&&[^\u0000]]";

    static final class Range implements Comparable {
        private final int min;
        private final int max;

        Range(int min, int max) {
            this.min = min;
            this.max = max;
        }

        int getMin() {
            return min;
        }

        int getMax() {
            return max;
        }

        public int compareTo(Object o) {
            Range other = (Range) o;
            if (this.min < other.min)
                return -1;
            if (this.min > other.min)
                return 1;
            if (this.max > other.max)
                return -1;
            if (this.max < other.max)
                return 1;
            return 0;
        }
    }

    static abstract class CharClass {

        protected CharClass() {
        }

        abstract void output(FastStringBuffer buf);

        abstract void outputComplement(FastStringBuffer buf);


        int getSingleChar() {
            return -1;
        }

    }

    static abstract class SimpleCharClass extends CharClass {
        SimpleCharClass() {

        }

        void output(FastStringBuffer buf) {
            buf.append('[');
            inClassOutput(buf);
            buf.append(']');
        }

        void outputComplement(FastStringBuffer buf) {
            buf.append("[^");
            inClassOutput(buf);
            buf.append(']');
        }

        abstract void inClassOutput(FastStringBuffer buf);
    }

    static class SingleChar extends SimpleCharClass {
        private final int c;

        SingleChar(int c) {
            this.c = c;
        }

        int getSingleChar() {
            return c;
        }

        void output(FastStringBuffer buf) {
            inClassOutput(buf);
        }

        void inClassOutput(FastStringBuffer buf) {
            if (isJavaMetaChar(c)) {
                buf.append('\\');
                buf.append((char) c);
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
                        buf.appendWideChar(c);
                }
            }
            return;
        }

    }


    static class Empty extends SimpleCharClass {
        private static final Empty instance = new Empty();

        private Empty() {

        }

        static Empty getInstance() {
            return instance;
        }

        void inClassOutput(FastStringBuffer buf) {
            throw new RuntimeException("BMP output botch");
        }

    }

    static class CharRange extends SimpleCharClass {
        private final int lower;
        private final int upper;

        CharRange(int lower, int upper) {
            this.lower = lower;
            this.upper = upper;
        }

        void inClassOutput(FastStringBuffer buf) {
            if (isJavaMetaChar(lower)) {
                buf.append('\\');
            }
            buf.appendWideChar(lower);
            buf.append('-');
            if (isJavaMetaChar(upper)) {
                buf.append('\\');
            }
            buf.appendWideChar(upper);
        }

    }

    static class Property extends SimpleCharClass {
        private final String name;

        Property(String name) {
            this.name = name;
        }

        void inClassOutput(FastStringBuffer buf) {
            buf.append("\\p{");
            buf.append(name);
            buf.append('}');
        }

        void outputComplement(FastStringBuffer buf) {
            buf.append("\\P{");
            buf.append(name);
            buf.append('}');
        }
    }

    static class Subtraction extends CharClass {
        private final CharClass cc1;
        private final CharClass cc2;

        Subtraction(CharClass cc1, CharClass cc2) {
            // min corresponds to intersection
            // complement corresponds to negation
            this.cc1 = cc1;
            this.cc2 = cc2;
        }

        void output(FastStringBuffer buf) {
            buf.append('[');
            cc1.output(buf);
            buf.append("&&");
            cc2.outputComplement(buf);
            buf.append(']');
        }

        void outputComplement(FastStringBuffer buf) {
            buf.append('[');
            cc1.outputComplement(buf);
            cc2.output(buf);
            buf.append(']');
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
            this.members = members;
        }

        void output(FastStringBuffer buf) {
            buf.append('[');
            for (int i = 0, len = members.size(); i < len; i++) {
                CharClass cc = (CharClass) members.get(i);
                cc.output(buf);
            }
            buf.append(']');
        }

        void outputComplement(FastStringBuffer buf) {
            boolean first = true;
            int len = members.size();
            for (int i = 0; i < len; i++) {
                CharClass cc = (CharClass) members.get(i);
                if (cc instanceof SimpleCharClass) {
                    if (first) {
                        buf.append("[^");
                        first = false;
                    }
                    ((SimpleCharClass) cc).inClassOutput(buf);
                }
            }
            for (int i = 0; i < len; i++) {
                CharClass cc = (CharClass) members.get(i);
                if (!(cc instanceof SimpleCharClass)) {
                    if (first) {
                        buf.append('[');
                        first = false;
                    } else {
                        buf.append("&&");
                    }
                    cc.outputComplement(buf);
                }
            }
            if (first == true) {
                // empty union, so the complement is everything
                buf.append("[\u0001-");
                buf.appendWideChar(NONBMP_MAX);
                buf.append("]");
            } else {
                buf.append(']');
            }
        }
    }

    static class BackReference extends CharClass {
        private final int i;

        BackReference(int i) {
            this.i = i;
        }

        void output(FastStringBuffer buf) {
            inClassOutput(buf);
        }

        void outputComplement(FastStringBuffer buf) {
            inClassOutput(buf);
        }

        void inClassOutput(FastStringBuffer buf) {
            buf.append("\\" + i);
        }
    }


    static class Complement extends CharClass {
        private final CharClass cc;

        Complement(CharClass cc) {
            this.cc = cc;
        }

        void output(FastStringBuffer buf) {
            cc.outputComplement(buf);
        }

        void outputComplement(FastStringBuffer buf) {
            cc.output(buf);
        }
    }

    private boolean translateAtom() throws RegexSyntaxException {
        switch (curChar) {
            case EOS:
                if (!eos)
                    break;
                // else fall through
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
                    // under XPath, "." has the same meaning as in JDK 1.5
                    break;
                } else {
                    // under XMLSchema, "." means anything except \n or \r, which is different from the XPath/JDK rule
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
                        CharClass[] chars = new CharClass[variants.length+1];
                        chars[0] = new SingleChar(thisChar);
                        for (int i=0; i<variants.length; i++) {
                            chars[i+1] = new SingleChar(variants[i]);
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


    private static CharClass makeCharClass(String categories, String includes, String excludeRanges) {
        List includeList = new ArrayList(5);
        for (int i = 0, len = categories.length(); i < len; i += 2)
            includeList.add(new Property(categories.substring(i, i + 2)));
        for (int i = 0, len = includes.length(); i < len; i++) {
            int j = i + 1;
            for (; j < len && includes.charAt(j) - includes.charAt(i) == j - i; j++)
                ;
            --j;
            if (i == j - 1)
                --j;
            if (i == j)
                includeList.add(new SingleChar(includes.charAt(i)));
            else
                includeList.add(new CharRange(includes.charAt(i), includes.charAt(j)));
            i = j;
        }
        List excludeList = new ArrayList(5);
        for (int i = 0, len = excludeRanges.length(); i < len; i += 2) {
            char min = excludeRanges.charAt(i);
            char max = excludeRanges.charAt(i + 1);
            if (min == max)
                excludeList.add(new SingleChar(min));
            else if (min == max - 1) {
                excludeList.add(new SingleChar(min));
                excludeList.add(new SingleChar(max));
            } else
                excludeList.add(new CharRange(min, max));
        }
        return new Subtraction(new Union(includeList), new Union(excludeList));
    }

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
                return ESC_i;
            case 'I':
                advance();
                return ESC_I;
            case 'c':
                advance();
                return ESC_c;
            case 'C':
                advance();
                return ESC_C;
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
                    return new BackReference(c0);
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
        String propertyName = regExp.subSequence(start, pos - 1).toString();
        advance();
        switch (propertyName.length()) {
            case 0:
                throw makeException("empty property name");
            case 2:
                int sci = subCategories.indexOf(propertyName);
                if (sci < 0 || sci % 2 == 1)
                    throw makeException("unknown category");
                return getSubCategoryCharClass(sci / 2);
            case 1:
                int ci = categories.indexOf(propertyName.charAt(0));
                if (ci < 0)
                    throw makeException("unknown category", propertyName);
                return getCategoryCharClass(ci);
            default:
                if (!propertyName.startsWith("Is"))
                    break;
                String blockName = propertyName.substring(2);
                for (int i = 0; i < specialBlockNames.length; i++)
                    if (blockName.equals(specialBlockNames[i]))
                        return specialBlockCharClasses[i];
                if (!isBlock(blockName))
                    throw makeException("invalid block name", blockName);
                return new Property("In" + blockName);
        }
        throw makeException("invalid property name", propertyName);
    }

    private static boolean isBlock(String name) {
        for (int i = 0; i < blockNames.length; i++)
            if (name.equals(blockNames[i]))
                return true;
        return false;
    }

    private static boolean isAsciiAlnum(char c) {
        if ('a' <= c && c <= 'z')
            return true;
        if ('A' <= c && c <= 'Z')
            return true;
        if ('0' <= c && c <= '9')
            return true;
        return false;
    }

    private void expect(char c) throws RegexSyntaxException {
        if (curChar != c)
            throw makeException("expected", new String(new char[]{c}));
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
        boolean first = true;
        do {
            CharClass lower = parseCharClassEscOrXmlChar(first);
            first = false;
            members.add(lower);
            if (curChar == '-') {
                advance();
                if (curChar == ']') {   // MHK: [+-] is reallowed by Schema Oct 2004 2nd edition
                    break;
                }
                if (curChar == '[') {
                    break;
                }
                CharClass upper = parseCharClassEscOrXmlChar(first);
                if (lower.getSingleChar() < 0 || upper.getSingleChar() < 0)
                    throw makeException("multi_range");
                if (lower.getSingleChar() > upper.getSingleChar())
                    throw makeException("invalid range (start > end)");
                members.set(members.size() - 1,
                        new CharRange(lower.getSingleChar(), upper.getSingleChar()));
                if (caseBlind) {
                    // Special-case A-Z and a-z
                    if (lower.getSingleChar() == 'a' && upper.getSingleChar() == 'z') {
                        members.add(new CharRange('A', 'Z'));
                        for (int v=0; v<CaseVariants.ROMAN_VARIANTS.length; v++) {
                            members.add(new SingleChar(CaseVariants.ROMAN_VARIANTS[v]));
                        }
                    } else if (lower.getSingleChar() == 'A' && upper.getSingleChar() == 'Z') {
                        members.add(new CharRange('a', 'z'));
                        for (int v=0; v<CaseVariants.ROMAN_VARIANTS.length; v++) {
                            members.add(new SingleChar(CaseVariants.ROMAN_VARIANTS[v]));
                        }
                    } else {
                        for (int k = lower.getSingleChar(); k <= upper.getSingleChar(); k++) {
                            int[] variants = CaseVariants.getCaseVariants(k);
                            for (int v=0; v<variants.length; v++) {
                                members.add(new SingleChar(variants[v]));
                            }
                        }
                    }
                }
                if (curChar == '-') {
                    advance();
                    expect('[');
                    break;
                }
            } else {
                int[] variants = CaseVariants.getCaseVariants(lower.getSingleChar());
                for (int v=0; v<variants.length; v++) {
                    members.add(new SingleChar(variants[v]));
                }
            }
        } while (curChar != ']');
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

    private CharClass parseCharClassEscOrXmlChar(boolean first) throws RegexSyntaxException {
        switch (curChar) {
            case EOS:
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
                if (!first) {
                    throw makeException("character must be escaped", new String(new char[]{curChar}));
                }
                break;
        }
        CharClass tem = new SingleChar(absorbSurrogatePair());
        advance();
        return tem;
    }

    private RegexSyntaxException makeException(String key) {
        return new RegexSyntaxException("Error at character " + (pos - 1) +
                " in regular expression " + Err.wrap(regExp, Err.VALUE) + ": " + key);
    }

    private RegexSyntaxException makeException(String key, String arg) {
        return new RegexSyntaxException("Error at character " + (pos - 1) +
                " in regular expression " + Err.wrap(regExp, Err.VALUE) + ": " + key +
                " (" + arg + ')');
    }

    private static boolean isJavaMetaChar(int c) {
        switch (c) {
            case '\\':
            case '^':
            case '?':
            case '*':
            case '+':
            case '(':
            case ')':
            case '{':
            case '}':
            case '|':
            case '[':
            case ']':
            case '-':
            case '&':
            case '$':
            case '.':
                return true;
        }
        return false;
    }

    private static synchronized CharClass getCategoryCharClass(int ci) {
        if (categoryCharClasses[ci] == null)
            categoryCharClasses[ci] = computeCategoryCharClass(categories.charAt(ci));
        return categoryCharClasses[ci];
    }

    private static synchronized CharClass getSubCategoryCharClass(int sci) {
        if (subCategoryCharClasses[sci] == null)
            subCategoryCharClasses[sci] = computeSubCategoryCharClass(subCategories.substring(sci * 2, (sci + 1) * 2));
        return subCategoryCharClasses[sci];
    }

    private static final char UNICODE_3_1_ADD_Lu = '\u03F4';   // added in 3.1
    private static final char UNICODE_3_1_ADD_Ll = '\u03F5';   // added in 3.1
    // 3 characters changed from No to Nl between 3.0 and 3.1
    private static final char UNICODE_3_1_CHANGE_No_to_Nl_MIN = '\u16EE';
    private static final char UNICODE_3_1_CHANGE_No_to_Nl_MAX = '\u16F0';
    private static final String CATEGORY_Pi = "\u00AB\u2018\u201B\u201C\u201F\u2039"; // Java doesn't know about category Pi
    private static final String CATEGORY_Pf = "\u00BB\u2019\u201D\u203A"; // Java doesn't know about category Pf

    private static CharClass computeCategoryCharClass(char code) {
        List classes = new ArrayList(5);
        classes.add(new Property(new String(new char[]{code})));
        for (int ci = CATEGORY_NAMES.indexOf(code); ci >= 0; ci = CATEGORY_NAMES.indexOf(code, ci + 1)) {
            int[] addRanges = CATEGORY_RANGES[ci / 2];
            for (int i = 0; i < addRanges.length; i += 2)
                classes.add(new CharRange(addRanges[i], addRanges[i + 1]));
        }
        if (code == 'P')
            classes.add(makeCharClass(CATEGORY_Pi + CATEGORY_Pf));
        if (code == 'L') {
            classes.add(new SingleChar(UNICODE_3_1_ADD_Ll));
            classes.add(new SingleChar(UNICODE_3_1_ADD_Lu));
        }
        if (code == 'C') {
            // JDK 1.4 leaves Cn out of C?
            classes.add(new Subtraction(new Property("Cn"),
                    new Union(new CharClass[]{new SingleChar(UNICODE_3_1_ADD_Lu),
                                              new SingleChar(UNICODE_3_1_ADD_Ll)})));
            List assignedRanges = new ArrayList(5);
            for (int i = 0; i < CATEGORY_RANGES.length; i++)
                for (int j = 0; j < CATEGORY_RANGES[i].length; j += 2)
                    assignedRanges.add(new CharRange(CATEGORY_RANGES[i][j],
                            CATEGORY_RANGES[i][j + 1]));
            classes.add(new Subtraction(new CharRange(NONBMP_MIN, NONBMP_MAX),
                    new Union(assignedRanges)));
        }
        if (classes.size() == 1)
            return (CharClass) classes.get(0);
        return new Union(classes);
    }

    private static CharClass computeSubCategoryCharClass(String name) {
        CharClass base = new Property(name);
        int sci = CATEGORY_NAMES.indexOf(name);
        if (sci < 0) {
            if (name.equals("Cn")) {
                // Unassigned
                List assignedRanges = new ArrayList(5);
                assignedRanges.add(new SingleChar(UNICODE_3_1_ADD_Lu));
                assignedRanges.add(new SingleChar(UNICODE_3_1_ADD_Ll));
                for (int i = 0; i < CATEGORY_RANGES.length; i++)
                    for (int j = 0; j < CATEGORY_RANGES[i].length; j += 2)
                        assignedRanges.add(new CharRange(CATEGORY_RANGES[i][j],
                                CATEGORY_RANGES[i][j + 1]));
                return new Subtraction(new Union(new CharClass[]{base, new CharRange(NONBMP_MIN, NONBMP_MAX)}),
                        new Union(assignedRanges));
            }
            if (name.equals("Pi"))
                return makeCharClass(CATEGORY_Pi);
            if (name.equals("Pf"))
                return makeCharClass(CATEGORY_Pf);
            return base;
        }
        List classes = new ArrayList(5);
        classes.add(base);
        int[] addRanges = CATEGORY_RANGES[sci / 2];
        for (int i = 0; i < addRanges.length; i += 2)
            classes.add(new CharRange(addRanges[i], addRanges[i + 1]));
        if (name.equals("Lu"))
            classes.add(new SingleChar(UNICODE_3_1_ADD_Lu));
        else if (name.equals("Ll"))
            classes.add(new SingleChar(UNICODE_3_1_ADD_Ll));
        else if (name.equals("Nl"))
            classes.add(new CharRange(UNICODE_3_1_CHANGE_No_to_Nl_MIN, UNICODE_3_1_CHANGE_No_to_Nl_MAX));
        else if (name.equals("No"))
            return new Subtraction(new Union(classes),
                    new CharRange(UNICODE_3_1_CHANGE_No_to_Nl_MIN,
                            UNICODE_3_1_CHANGE_No_to_Nl_MAX));
        return new Union(classes);
    }

    private static CharClass makeCharClass(String members) {
        List list = new ArrayList(5);
        for (int i = 0, len = members.length(); i < len; i++)
            list.add(new SingleChar(members.charAt(i)));
        return new Union(list);
    }

    public static void main(String[] args) throws RegexSyntaxException {
        String s = translate(args[0], args[1].equals("xpath"), false, true);
        for (int i = 0, len = s.length(); i < len; i++) {
            char c = s.charAt(i);
            if (c >= 0x20 && c <= 0x7e)
                System.err.print(c);
            else {
                System.err.print("\\u");
                for (int shift = 12; shift >= 0; shift -= 4)
                    System.err.print("0123456789ABCDEF".charAt((c >> shift) & 0xF));
            }
        }
        System.err.println();
    }


//}


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
// The Original Code is: all this file except changes marked.
//
// The Initial Developer of the Original Code is James Clark
//
// Portions created by (your name) are Copyright (C) (your legal entity). All Rights Reserved.
//
// Contributor(s): Michael Kay
//

