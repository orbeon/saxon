package org.orbeon.saxon.dotnet;

import cli.System.ArgumentException;
import cli.System.Text.RegularExpressions.Regex;
import cli.System.Text.RegularExpressions.RegexOptions;
import org.orbeon.saxon.om.FastStringBuffer;
import org.orbeon.saxon.om.SequenceIterator;
import org.orbeon.saxon.regex.RegexIterator;
import org.orbeon.saxon.regex.RegexSyntaxException;
import org.orbeon.saxon.regex.RegularExpression;
import org.orbeon.saxon.trans.XPathException;

/**
 * A compiled regular expression implemented using the .NET regex package
 */
public class DotNetRegularExpression implements RegularExpression {

    Regex pattern;
    int groupCount;

    /**
     * Create (compile) a regular expression
     * @param regex the source text of the regular expression, in XML Schema or XPath syntax
     * @param xmlVersion indicates whether XML 1.0 or XML 1.1 is in use
     * @param syntax indicates whether this is an XPath regular expression, an XML Schema regular expression,
     * or a regex in .NET native regex syntax
     * @param flags the flags argument as supplied to functions such as fn:matches(), in string form
     * @throws org.orbeon.saxon.trans.XPathException if the syntax of the regular expression or flags is incorrect
     */

    public DotNetRegularExpression(CharSequence regex, int xmlVersion, int syntax, CharSequence flags)
            throws XPathException {
        String translated = "";
        try {
            if (syntax == NATIVE_SYNTAX) {
                groupCount = 9;
                pattern = new Regex(regex.toString(), setFlags(flags));
            } else {
                DotNetRegexTranslator translator = new DotNetRegexTranslator();
                translated = translator.translate(
                        regex, xmlVersion, syntax==XPATH_SYNTAX, isIgnoreWhitespace(flags), isCaseBlind(flags));
                groupCount = translator.getNumberOfCapturedGroups();
                pattern = new Regex(translated, setFlags(flags));
            }
            //noinspection ConstantIfStatement
            if (false) {
                // to keep the compiler happy
                throw new ArgumentException();
            }
        } catch (RegexSyntaxException e) {
            throw new XPathException(e.getMessage());
        } catch (ArgumentException e) {
            throw new XPathException("Error in translated regular expression. Input regex = " +
                    FastStringBuffer.diagnosticPrint(regex) + ". Translated regex = " + 
                    FastStringBuffer.diagnosticPrint(translated) + ". Message = " + e.getMessage());
        }
    }

    /**
     * Use this regular expression to analyze an input string, in support of the XSLT
     * analyze-string instruction. The resulting RegexIterator provides both the matching and
     * non-matching substrings, and allows them to be distinguished. It also provides access
     * to matched subgroups.
     */

    public RegexIterator analyze(CharSequence input) {
        return new DotNetRegexIterator(input.toString(), pattern);
    }

    /**
     * Determine whether the regular expression contains a match of a given string
     *
     * @param input the string to match
     * @return true if the string matches, false otherwise
     */

    public boolean containsMatch(CharSequence input) {
        return pattern.IsMatch(input.toString());
    }

    /**
     * Determine whether the regular expression matches (the whole of) a given string
     *
     * @param input the string to match
     * @return true if the regular expression matches the whole input string, false otherwise
     */

    public boolean matches(CharSequence input) {
        // We rely on the fact that this method is only used for the XML Schema pattern facet, and
        // the regular expressions are preprocessed in that case to add implicit anchoring. The method
        // is also used to test if the pattern matches an empty string, which is OK.
        return pattern.IsMatch(input.toString());
    }

    /**
     * Replace all substrings of a supplied input string that match the regular expression
     * with a replacement string.
     *
     * @param input       the input string on which replacements are to be performed
     * @param replacement the replacement string in the format of the XPath replace() function
     * @return the result of performing the replacement
     * @throws org.orbeon.saxon.trans.XPathException
     *          if the replacement string is invalid
     */

    public CharSequence replace(CharSequence input, CharSequence replacement) throws XPathException {
        // preprocess the replacement string: .NET uses $$ to represent $, and doesn't treat \ specially
        // The calling code will already have validated the replacement string, so we can assume for example
        // that "\" will be followed by "\" or "$".
        FastStringBuffer sb = new FastStringBuffer(replacement.length() + 4);
        for (int i=0; i<replacement.length(); i++) {
            final char ch = replacement.charAt(i);
            if (ch == '\\') {
                if (replacement.charAt(i+1) == '\\') {
                    sb.append('\\');
                } else if (replacement.charAt(i+1) == '$') {
                    sb.append("$$");
                } else {
                    throw new IllegalArgumentException("bad replacement string");
                }
                i++;
            } else if (ch == '$') {
                int n = 0;
                while (true) {
                    if (i+1 >= replacement.length()) {
                        break;
                    }
                    char d = replacement.charAt(i+1);
                    int dval = "0123456789".indexOf(d);
                    if (dval < 0) {
                        break;
                    }
                    i++;
                    n = n*10 + dval;
                }
                processGroupReference(n, sb);
            } else {
                sb.append(ch);
            }
        }
        //System.err.println("original replacement string: " + replacement);
        //System.err.println("processed replacement string: " + sb);
        return pattern.Replace(input.toString(), sb.toString());
    }

    /**
     * Translate a group reference in the replacement string from XPath notation into .NET notation
     * This closely follows the algorithm in F+O section 7.6.3 fn:replace
     * @param n the consecutive sequence of digits following a "$" sign
     * @param sb teh buffer to contain the replacement string in .NET notation
     */

    private void processGroupReference(int n, FastStringBuffer sb) {
        if (n == 0) {
            sb.append("$0");
        } else if (n <= groupCount) {
            sb.append("${" + n + '}');
        } else if (n <= 9) {
            // no-op - group reference is replaced by zero-length string
        } else {
            // try replacing $67 by ${6}7
            int n0 = n / 10;
            int n1 = n % 10;
            processGroupReference(n0, sb);
            sb.append("" + n1);
        }
    }

    /**
     * Use this regular expression to tokenize an input string.
     *
     * @param input the string to be tokenized
     * @return a SequenceIterator containing the resulting tokens, as objects of type StringValue
     */

    public SequenceIterator tokenize(CharSequence input) {
        return new DotNetTokenIterator(input, pattern);
    }

    /**
     * Set the Java flags from the supplied XPath flags.
     * @param inFlags the flags as a string, e.g. "im"
     * @return the flags as a RegexOptions FlagsAttribute
     * @throws XPathException if the supplied value is invalid
     */

    public static RegexOptions setFlags(CharSequence inFlags) throws XPathException {
        int flags = 0;
        for (int i=0; i<inFlags.length(); i++) {
            char c = inFlags.charAt(i);
            switch (c) {
            case'm':
                flags |= RegexOptions.Multiline;
                break;
            case'i':
                // flags |= RegexOptions.IgnoreCase;
                break;
            case's':
                flags |= RegexOptions.Singleline;
                break;
            case'x':
                //flags |= RegexOptions.IgnorePatternWhitespace;
                break;
            default:
                XPathException err = new XPathException("Invalid character '" + c + "' in regular expression flags");
                err.setErrorCode("FORX0001");
                throw err;
            }
        }
        return RegexOptions.wrap(flags);
    }

    /**
     * Test whether the 'x' flag is set.
     * @param inFlags the flags as a string, e.g. "im"
     * @return true if the 'x' flag is set
     */

    public static boolean isIgnoreWhitespace(CharSequence inFlags) {
        for (int i=0; i<inFlags.length(); i++) {
            if (inFlags.charAt(i) == 'x') {
                return true;
            }
        }
        return false;
    }

    /**
     * Test whether the 'i' flag is set.
     * @param inFlags the flags as a string, e.g. "im"
     * @return true if the 'i' flag is set
     */

    public static boolean isCaseBlind(CharSequence inFlags) {
        for (int i=0; i<inFlags.length(); i++) {
            if (inFlags.charAt(i) == 'i') {
                return true;
            }
        }
        return false;
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
// Contributor(s):
//


