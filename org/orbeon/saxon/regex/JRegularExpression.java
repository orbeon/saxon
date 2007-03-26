package org.orbeon.saxon.regex;

import org.orbeon.saxon.om.SequenceIterator;
import org.orbeon.saxon.trans.DynamicError;
import org.orbeon.saxon.trans.XPathException;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A compiled regular expression implemented using the Java JDK regex package
 */
public class JRegularExpression implements RegularExpression {

    Pattern pattern;
    //int groupCount = -1;

    /**
     * Create (compile) a regular expression
     * @param regex the source text of the regular expression, in XML Schema or XPath syntax
     * @param isXPath set to true if this is an XPath regular expression, false if it is XML Schema
     * @param flags the flags argument as supplied to functions such as fn:matches(), in string form
     * @throws org.orbeon.saxon.trans.XPathException if the syntax of the regular expression or flags is incorrect
     */

    public JRegularExpression(CharSequence regex, boolean isXPath, CharSequence flags)
            throws XPathException {
        try {
            int flagBits = setFlags(flags);
            String j = System.getProperty("java.version");
            String translated;
            if (j.startsWith("1.4")) {
                JDK14RegexTranslator translator = new JDK14RegexTranslator();
                translator.setIgnoreWhitespace((flagBits & Pattern.COMMENTS) != 0);
                translated = translator.translate(regex, isXPath);
                pattern = Pattern.compile(translated, flagBits & (~Pattern.COMMENTS));
            } else {
                boolean ignoreWhitespace = ((flagBits & Pattern.COMMENTS) != 0);
                boolean caseBlind = ((flagBits & Pattern.CASE_INSENSITIVE) != 0);
                translated = JDK15RegexTranslator.translate(regex, isXPath, ignoreWhitespace, caseBlind);
                pattern = Pattern.compile(translated, flagBits & (~(Pattern.COMMENTS | Pattern.CASE_INSENSITIVE)));
            }

        } catch (RegexSyntaxException e) {
            throw new DynamicError(e.getMessage());
        }
    }

    /**
     * Use this regular expression to analyze an input string, in support of the XSLT
     * analyze-string instruction. The resulting RegexIterator provides both the matching and
     * non-matching substrings, and allows them to be distinguished. It also provides access
     * to matched subgroups.
     */

    public RegexIterator analyze(CharSequence input) {
        return new JRegexIterator(input.toString(), pattern);
    }

    /**
     * Determine whether the regular expression contains a match for a given string
     *
     * @param input the string to match
     * @return true if the string matches, false otherwise
     */

    public boolean containsMatch(CharSequence input) {
        return pattern.matcher(input).find();
    }

    /**
     * Determine whether the regular expression match a given string in its entirety
     *
     * @param input the string to match
     * @return true if the string matches, false otherwise
     */

    public boolean matches(CharSequence input) {
        return pattern.matcher(input).matches();
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
        Matcher matcher = pattern.matcher(input);
        try {
            String res = matcher.replaceAll(replacement.toString());
            return res;
        } catch (IndexOutOfBoundsException e) {
            // this occurs if the replacement string references a group $n and there are less than n
            // capturing subexpressions in the regex. In this case we're supposed to replace $n by an
            // empty string. We do this by modifying the replacement string.
            int gps = matcher.groupCount();
            if (gps >= 9) {
                // don't know what's gone wrong here
                throw e;
            }
            String r = replacement.toString();
            // remove occurrences of $n from the replacement string, if n is greater than the number of groups
            String f = "\\$[" + (gps+1) + "-9]";
            String rep = Pattern.compile(f).matcher(r).replaceAll("");
            String res = matcher.replaceAll(rep);
            return res;
        }

    }

    /**
     * Use this regular expression to tokenize an input string.
     *
     * @param input the string to be tokenized
     * @return a SequenceIterator containing the resulting tokens, as objects of type StringValue
     */

    public SequenceIterator tokenize(CharSequence input) {
        return new JTokenIterator(input, pattern);
    }

    /**
     * Set the Java flags from the supplied XPath flags.
     * @param inFlags the flags as a string, e.g. "im"
     * @return the flags as a bit-significant integer
     * @throws DynamicError if the supplied value is invalid
     */

    public static int setFlags(CharSequence inFlags) throws DynamicError {
        int flags = Pattern.UNIX_LINES;
        for (int i=0; i<inFlags.length(); i++) {
            char c = inFlags.charAt(i);
            switch (c) {
                case 'm':
                    flags |= Pattern.MULTILINE;
                    break;
                case 'i':
                    flags |= Pattern.CASE_INSENSITIVE;
                    flags |= Pattern.UNICODE_CASE;
                    break;
                case 's':
                    flags |= Pattern.DOTALL;
                    break;
                case 'x':
                    flags |= Pattern.COMMENTS;  // note, this enables comments as well as whitespace
                    break;
                default:
                    DynamicError err = new DynamicError("Invalid character '" + c + "' in regular expression flags");
                    err.setErrorCode("FORX0001");
                    throw err;
            }
        }
        return flags;
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
// Portions marked "e.g." are from Edwin Glaser (edwin@pannenleiter.de)
//


