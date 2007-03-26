package org.orbeon.saxon.dotnet;

import org.orbeon.saxon.Err;
import org.orbeon.saxon.om.FastStringBuffer;
import org.orbeon.saxon.om.XMLChar;
import org.orbeon.saxon.regex.CaseVariants;
import org.orbeon.saxon.regex.RegexSyntaxException;
import org.orbeon.saxon.sort.IntHashSet;
import org.orbeon.saxon.value.Whitespace;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * This class translates XML Schema regex syntax into JDK 1.4 regex syntax.
 * Author: James Clark
 * Modified by Michael Kay (a) to integrate the code into Saxon, (b) to support XPath additions
 * to the XML Schema regex syntax, (c) to target the .NET regex syntax instead of JDK 1.4
 * <p/>
 * This version of the regular expression translator treats each half of a surrogate pair as a separate
 * character, translating anything in an XPath regex that can match a non-BMP character into a Java
 * regex that matches the two halves of a surrogate pair independently. This approach doesn't work
 * under JDK 1.5, whose regex engine treats a surrogate pair as a single character.
 * <p/>
 * This translator is currently used for Saxon on .NET 1.1. It's almost the same as the JDK 1.4 version,
 * except that it avoids use of the "&&" operator, which isn't available on .NET. It would probably be
 * possible to use this version for JDK 1.4 as well, but I have avoided that for the time being because
 * of the risk of introducing bugs on the Java platform, and because of shortage of test cases.
 */
public class DotNetRegexTranslator {


    /**
     * Translates XML Schema regexes into <code>java.util.regex</code> regexes.
     *
     * @see java.util.regex.Pattern
     * @see <a href="http://www.w3.org/TR/xmlschema-2/#regexs">XML Schema Part 2</a>
     */

    private CharSequence regExp;
    private boolean isXPath;
    private boolean ignoreWhitespace;
    private boolean caseBlind;
    private boolean inCharClassExpr;
    private int pos = 0;
    private int length;
    private char curChar;
    private boolean eos = false;
    private final FastStringBuffer result = new FastStringBuffer(32);
    private int currentCapture = 0;
    private IntHashSet captures = new IntHashSet();

    private static final String categories = "LMNPZSC";
    private static final CharClass[] categoryCharClasses = new CharClass[categories.length()];
    private static final String subCategories = "LuLlLtLmLoMnMcMeNdNlNoPcPdPsPePiPfPoZsZlZpSmScSkSoCcCfCoCn";
    private static final CharClass[] subCategoryCharClasses = new CharClass[subCategories.length() / 2];

    private static final int NONBMP_MIN = 0x10000;
    private static final int NONBMP_MAX = 0x10FFFF;
    private static final char SURROGATE2_MIN = '\uDC00';
    private static final char SURROGATE2_MAX = '\uDFFF';

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

    private static final CharClass DOT_XPATH =
            new Dot();

    private static final CharClass ESC_d = new Property("Nd");

    private static final CharClass ESC_D = new Complement(ESC_d);

    private static final CharClass ESC_W = new Union(
            new CharClass[]{computeCategoryCharClass('P'),
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

    public DotNetRegexTranslator() {}

    /**
     * Translates a regular expression in the syntax of XML Schemas Part 2 into a regular
     * expression in the syntax of <code>java.util.regex.Pattern</code>.  The translation
     * assumes that the string to be matched against the regex uses surrogate pairs correctly.
     * If the string comes from XML content, a conforming XML parser will automatically
     * check this; if the string comes from elsewhere, it may be necessary to check
     * surrogate usage before matching.
     *
     * @param regExp a String containing a regular expression in the syntax of XML Schemas Part 2
     * @param xpath a boolean indicating whether the XPath 2.0 F+O extensions to the schema
     * regex syntax are permitted
     * @return a String containing a regular expression in the syntax of java.util.regex.Pattern
     * @throws RegexSyntaxException if <code>regexp</code> is not a regular expression in the
     * syntax of XML Schemas Part 2, or XPath 2.0, as appropriate
     * @see java.util.regex.Pattern
     * @see <a href="http://www.w3.org/TR/xmlschema-2/#regexs">XML Schema Part 2</a>
     */
    public String translate(CharSequence regExp, boolean xpath, boolean ignoreWhitespace, boolean caseBlind)

            throws RegexSyntaxException {
        //System.err.println("Input regex: " + FastStringBuffer.diagnosticPrint(regExp));
        this.regExp = regExp;
        this.isXPath = xpath;
        this.ignoreWhitespace = ignoreWhitespace;
        this.caseBlind = caseBlind;
        this.length = regExp.length();
        advance();
        translateTop();
        //System.err.println("Output regex: " + FastStringBuffer.diagnosticPrint(result));
        return result.toString();
    }

    public int getNumberOfCapturedGroups() {
        return currentCapture;
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
        if (curChar=='?' && isXPath) {
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
            result.append(""+Integer.MAX_VALUE);
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
                    result.append(""+Integer.MAX_VALUE);
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
            Range other = (Range)o;
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

        private final int containsBmp;
        // if it contains ALL and containsBmp != NONE, then the generated class for containsBmp must
        // contain all the high surrogates
        private final int containsNonBmp;

        protected CharClass(int containsBmp, int containsNonBmp) {
            this.containsBmp = containsBmp;
            this.containsNonBmp = containsNonBmp;
        }

        int getContainsBmp() {
            return containsBmp;
        }

        int getContainsNonBmp() {
            return containsNonBmp;
        }

        final void output(FastStringBuffer buf) {
            switch (containsNonBmp) {
            case NONE:
                if (containsBmp == NONE)
                    buf.append(NOT_ALLOWED_CLASS);
                else
                    outputBmp(buf);
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
                    if (needSep)
                        buf.append('|');
                    else
                        needSep = true;
                    buf.append('[');
                    for (int i = 0, len = hi.length(); i < len; i += 2) {
                        char min = hi.charAt(i);
                        char max = hi.charAt(i + 1);
                        if (min == max)
                            buf.append(min);
                        else {
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
                    if (needSep)
                        buf.append('|');
                    else
                        needSep = true;
                    buf.append(lo.charAt(i));
                    char min = lo.charAt(i + 1);
                    char max = lo.charAt(i + 2);
                    if (min == max && (i + 3 >= len || lo.charAt(i + 3) != lo.charAt(i)))
                        buf.append(min);
                    else {
                        buf.append('[');
                        for (; ;) {
                            if (min == max)
                                buf.append(min);
                            else {
                                buf.append(min);
                                buf.append('-');
                                buf.append(max);
                            }
                            if (i + 3 >= len || lo.charAt(i + 3) != lo.charAt(i))
                                break;
                            i += 3;
                            min = lo.charAt(i + 1);
                            max = lo.charAt(i + 2);
                        }
                        buf.append(']');
                    }
                }
                if (!needSep)
                    buf.append(NOT_ALLOWED_CLASS);
                buf.append(')');
                break;
            }
        }

        static String highSurrogateRanges(List ranges) {
            FastStringBuffer highRanges = new FastStringBuffer(ranges.size() * 2);
            for (int i = 0, len = ranges.size(); i < len; i++) {
                Range r = (Range)ranges.get(i);
                char min1 = XMLChar.highSurrogate(r.getMin());
                char min2 = XMLChar.lowSurrogate(r.getMin());
                char max1 = XMLChar.highSurrogate(r.getMax());
                char max2 = XMLChar.lowSurrogate(r.getMax());
                if (min2 != SURROGATE2_MIN)
                    min1++;
                if (max2 != SURROGATE2_MAX)
                    max1--;
                if (max1 >= min1) {
                    highRanges.append(min1);
                    highRanges.append(max1);
                }
            }
            return highRanges.toString();
        }

        static String lowSurrogateRanges(List ranges) {
            FastStringBuffer lowRanges = new FastStringBuffer(ranges.size() * 2);
            for (int i = 0, len = ranges.size(); i < len; i++) {
                Range r = (Range)ranges.get(i);
                char min1 = XMLChar.highSurrogate(r.getMin());
                char min2 = XMLChar.lowSurrogate(r.getMin());
                char max1 = XMLChar.highSurrogate(r.getMax());
                char max2 = XMLChar.lowSurrogate(r.getMax());
                if (min1 == max1) {
                    if (min2 != SURROGATE2_MIN || max2 != SURROGATE2_MAX) {
                        lowRanges.append(min1);
                        lowRanges.append(min2);
                        lowRanges.append(max2);
                    }
                } else {
                    if (min2 != SURROGATE2_MIN) {
                        lowRanges.append(min1);
                        lowRanges.append(min2);
                        lowRanges.append(SURROGATE2_MAX);
                    }
                    if (max2 != SURROGATE2_MAX) {
                        lowRanges.append(max1);
                        lowRanges.append(SURROGATE2_MIN);
                        lowRanges.append(max2);
                    }
                }
            }
            return lowRanges.toString();
        }

        abstract void outputBmp(FastStringBuffer buf);

        abstract void outputComplementBmp(FastStringBuffer buf);

        int getSingleChar() {
            return -1;
        }

        void addNonBmpRanges(List ranges) {
        }


        static void sortRangeList(List ranges) {
            Collections.sort(ranges);
            int toIndex = 0;
            int fromIndex = 0;
            int len = ranges.size();
            while (fromIndex < len) {
                Range r = (Range)ranges.get(fromIndex);
                int min = r.getMin();
                int max = r.getMax();
                while (++fromIndex < len) {
                    Range r2 = (Range)ranges.get(fromIndex);
                    if (r2.getMin() > max + 1)
                        break;
                    if (r2.getMax() > max)
                        max = r2.getMax();
                }
                if (max != r.getMax())
                    r = new Range(min, max);
                ranges.set(toIndex++, r);
            }
            while (len > toIndex)
                ranges.remove(--len);
        }

    }

    static abstract class SimpleCharClass extends CharClass {
        SimpleCharClass(int containsBmp, int containsNonBmp) {
            super(containsBmp, containsNonBmp);
        }

        void outputBmp(FastStringBuffer buf) {
            buf.append('[');
            inClassOutputBmp(buf);
            buf.append(']');
        }

        // must not call if containsBmp == ALL
        void outputComplementBmp(FastStringBuffer buf) {
            if (getContainsBmp() == NONE)
                buf.append("[\u0000-\uFFFF]");
            else {
                buf.append("[^");
                inClassOutputBmp(buf);
                buf.append(']');
            }
        }

        abstract void inClassOutputBmp(FastStringBuffer buf);
    }

    static class SingleChar extends SimpleCharClass {
        private final char c;

        SingleChar(char c) {
            super(SOME, NONE);
            this.c = c;
        }

        int getSingleChar() {
            return c;
        }

        void outputBmp(FastStringBuffer buf) {
            inClassOutputBmp(buf);
        }

        void inClassOutputBmp(FastStringBuffer buf) {
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
            return;
        }

    }

    static class WideSingleChar extends SimpleCharClass {
        private final int c;

        WideSingleChar(int c) {
            super(NONE, SOME);
            this.c = c;
        }

        void inClassOutputBmp(FastStringBuffer buf) {
            throw new RuntimeException("BMP output botch");
        }

        int getSingleChar() {
            return c;
        }

        void addNonBmpRanges(List ranges) {
            ranges.add(new Range(c, c));
        }
    }

    static class Empty extends SimpleCharClass {
        private static final Empty instance = new Empty();

        private Empty() {
            super(NONE, NONE);
        }

        static Empty getInstance() {
            return instance;
        }

        void inClassOutputBmp(FastStringBuffer buf) {
            throw new RuntimeException("BMP output botch");
        }

    }

    static class CharRange extends SimpleCharClass {
        private final int lower;
        private final int upper;

        CharRange(int lower, int upper) {
            super(lower < NONBMP_MIN ? SOME : NONE,
                  // don't use ALL here, because that requires that the BMP class contains high surrogates
                  upper >= NONBMP_MIN ? SOME : NONE);
            this.lower = lower;
            this.upper = upper;
        }

        void inClassOutputBmp(FastStringBuffer buf) {
            if (lower >= NONBMP_MIN)
                throw new RuntimeException("BMP output botch");
            if (isJavaMetaChar((char)lower))
                buf.append('\\');
            buf.append((char)lower);
            buf.append('-');
            if (upper < NONBMP_MIN) {
                if (isJavaMetaChar((char)upper))
                    buf.append('\\');
                buf.append((char)upper);
            } else
                buf.append('\uFFFF');
        }

        void addNonBmpRanges(List ranges) {
            if (upper >= NONBMP_MIN)
                ranges.add(new Range(lower < NONBMP_MIN ? NONBMP_MIN : lower, upper));
        }
    }

    static class Property extends SimpleCharClass {
        private final String name;

        Property(String name) {
            super(SOME, NONE);
            this.name = name;
        }

        void outputBmp(FastStringBuffer buf) {
            inClassOutputBmp(buf);
        }

        void inClassOutputBmp(FastStringBuffer buf) {
            buf.append("\\p{");
            buf.append(name);
            buf.append('}');
        }

        void outputComplementBmp(FastStringBuffer buf) {
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

        void outputBmp(FastStringBuffer buf) {
            cc1.outputBmp(buf);
            buf.append("(?!");
            cc2.outputBmp(buf);
            buf.append(")");
        }

//        void outputComplementBmp(FastStringBuffer buf) {
//            buf.append('[');
//            cc1.outputComplementBmp(buf);
//            cc2.outputBmp(buf);
//            buf.append(']');
//        }

        void outputComplementBmp(FastStringBuffer buf) {
            // not(A and not(B)) => (not(A) or B)
            buf.append("(?:");
            cc1.outputComplementBmp(buf);
            buf.append("|");
            cc2.outputBmp(buf);
            buf.append(')');
        }

        void addNonBmpRanges(List ranges) {
            List posList = new ArrayList(5);
            cc1.addNonBmpRanges(posList);
            List negList = new ArrayList(5);
            cc2.addNonBmpRanges(negList);
            sortRangeList(posList);
            sortRangeList(negList);
            Iterator negIter = negList.iterator();
            Range negRange;
            if (negIter.hasNext())
                negRange = (Range)negIter.next();
            else
                negRange = null;
            for (int i = 0, len = posList.size(); i < len; i++) {
                Range posRange = (Range)posList.get(i);
                while (negRange != null && negRange.getMax() < posRange.getMin()) {
                    if (negIter.hasNext())
                        negRange = (Range)negIter.next();
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
                        negRange = (Range)negIter.next();
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

        void outputBmp(FastStringBuffer buf) {
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
                    CharClass cc = (CharClass)members.get(i);
                    if (cc.getContainsBmp() != NONE) {
                        ((SimpleCharClass)cc).inClassOutputBmp(buf);
                    }
                }
                buf.append(']');
            } else {
                buf.append("(?:");
                boolean first = true;
                for (int i = 0, len = members.size(); i < len; i++) {
                    CharClass cc = (CharClass)members.get(i);
                    if (cc.getContainsBmp() != NONE) {
                        if (!first) {
                            buf.append('|');
                        }
                        first = false;
                        if (cc instanceof SimpleCharClass) {
                            ((SimpleCharClass)cc).inClassOutputBmp(buf);
                        } else {
                            cc.outputBmp(buf);
                        }
                    }
                }
                buf.append(')');
            }
        }

        void outputComplementBmp(FastStringBuffer buf) {
            int len = members.size();
            int bmpMembers = 0;
            int simpleChars = 0;
            int nonSimpleChars = 0;
            for (int i = 0; i < len; i++) {
                CharClass cc = (CharClass)members.get(i);
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
                        CharClass cc = (CharClass)members.get(i);
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
                        CharClass cc = (CharClass)members.get(i);
                        if (cc.getContainsBmp() != NONE && cc instanceof SimpleCharClass) {
                            ((SimpleCharClass)cc).inClassOutputBmp(buf);
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


        void addNonBmpRanges(List ranges) {
            for (int i = 0, len = members.size(); i < len; i++)
                ((CharClass)members.get(i)).addNonBmpRanges(ranges);
        }

        private static int computeContainsBmp(List members) {
            int ret = NONE;
            for (int i = 0, len = members.size(); i < len; i++)
                ret = Math.max(ret, ((CharClass)members.get(i)).getContainsBmp());
            return ret;
        }

        private static int computeContainsNonBmp(List members) {
            int ret = NONE;
            for (int i = 0, len = members.size(); i < len; i++)
                ret = Math.max(ret, ((CharClass)members.get(i)).getContainsNonBmp());
            return ret;
        }
    }

    static class BackReference extends CharClass {
       private final int i;

        BackReference(int i) {
            super(SOME, NONE);
            this.i = i;
        }

        void outputBmp(FastStringBuffer buf) {
            inClassOutputBmp(buf);
        }

        void outputComplementBmp(FastStringBuffer buf) {
            inClassOutputBmp(buf);
        }

        void inClassOutputBmp(FastStringBuffer buf) {
            buf.append("\\" + i + ".{0}");  // terminate the back-reference with a syntactic separator
        }
    }

    static class Dot extends CharClass {

        Dot() {
            super(SOME, NONE);  // a deliberate lie
        }

        void outputBmp(FastStringBuffer buf) {
            buf.append("(?:.|");
            buf.append(SURROGATES1_CLASS);
            buf.append(SURROGATES2_CLASS);
            buf.append(")");
        }

        void outputComplementBmp(FastStringBuffer buf) {
            buf.append("[^\\n]");
        }

        void inClassOutputBmp(FastStringBuffer buf) {
            buf.append(".");
        }
    }

    static class Complement extends CharClass {
        private final CharClass cc;

        Complement(CharClass cc) {
            super(-cc.getContainsBmp(), -cc.getContainsNonBmp());
            this.cc = cc;
        }

        void outputBmp(FastStringBuffer buf) {
            cc.outputComplementBmp(buf);
        }

        void outputComplementBmp(FastStringBuffer buf) {
            cc.outputBmp(buf);
        }

        void addNonBmpRanges(List ranges) {
            List tem = new ArrayList(5);
            cc.addNonBmpRanges(tem);
            sortRangeList(tem);
            int c = NONBMP_MIN;
            for (int i = 0, len = tem.size(); i < len; i++) {
                Range r = (Range)tem.get(i);
                if (r.getMin() > c)
                    ranges.add(new Range(c, r.getMin() - 1));
                c = r.getMax() + 1;
            }
            if (c != NONBMP_MAX + 1)
                ranges.add(new Range(c, NONBMP_MAX));
        }
    }

    private boolean translateAtom() throws RegexSyntaxException {
        switch (curChar) {
        case EOS:
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
                        CharClass[] chars = new CharClass[variants.length+1];
                        chars[0] = makeSingleCharClass(thisChar);
                        for (int i=0; i<variants.length; i++) {
                            chars[i+1] = makeSingleCharClass(variants[i]);
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
        if (ch > 65535) {
            return new SingleChar((char)ch);
        } else {
            return new WideSingleChar(ch);
        }
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
        case '0': case '1': case '2': case '3': case '4':
        case '5': case '6': case '7': case '8': case '9':
            if (isXPath) {
                char c = curChar;
                int c0 = (c - '0');
                advance();
                int c1 = "0123456789".indexOf(curChar);
                if (c1 >= 0) {
                    // limit a back-reference to two digits, but only allow two if there is such a capture
                    int n = c0*10 + c1;
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
        } else
            compl = false;
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
                            members.add(makeSingleCharClass(CaseVariants.ROMAN_VARIANTS[v]));
                        }
                    } else if (lower.getSingleChar() == 'A' && upper.getSingleChar() == 'Z') {
                        members.add(new CharRange('a', 'z'));
                        for (int v=0; v<CaseVariants.ROMAN_VARIANTS.length; v++) {
                            members.add(makeSingleCharClass(CaseVariants.ROMAN_VARIANTS[v]));
                        }
                    } else {
                        for (int k = lower.getSingleChar(); k <= upper.getSingleChar(); k++) {
                            int[] variants = CaseVariants.getCaseVariants(k);
                            for (int v=0; v<variants.length; v++) {
                                members.add(makeSingleCharClass(variants[v]));
                            }
                        }
                    }
                }

                if (curChar == '-') {
                    advance();
                    expect('[');
                    break;
                }
            }
        } while (curChar != ']');
        CharClass result;
        if (members.size() == 1)
            result = (CharClass)members.get(0);
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
        CharClass tem;
        if (XMLChar.isSurrogate(curChar)) {
            if (!XMLChar.isHighSurrogate(curChar))
                throw makeException("invalid surrogate pair");
            char c1 = curChar;
            advance();
            if (!XMLChar.isLowSurrogate(curChar))
                throw makeException("invalid surrogate pair");
            tem = new WideSingleChar(XMLChar.supplemental(c1, curChar));
        } else
            tem = new SingleChar(curChar);
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

    private static boolean isJavaMetaChar(char c) {
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
            return (CharClass)classes.get(0);
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

    /**
     * Convenience main method for testing purposes. Note that the actual testing is done using the
     * Java regex engine.
     * @param args: (1) the regex, (2) xpath|schema, (3) target string to be matched
     * @throws RegexSyntaxException
     */

    public static void main(String[] args) throws RegexSyntaxException {
        System.err.println(FastStringBuffer.diagnosticPrint(args[0]));
        String s = new DotNetRegexTranslator().translate(args[0], "xpath".equals(args[1]), false, true);
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

