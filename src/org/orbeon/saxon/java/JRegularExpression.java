package org.orbeon.saxon.java;

import org.orbeon.saxon.om.EmptyIterator;
import org.orbeon.saxon.om.SequenceIterator;
import org.orbeon.saxon.regex.RegexIterator;
import org.orbeon.saxon.regex.RegexSyntaxException;
import org.orbeon.saxon.regex.RegularExpression;
import org.orbeon.saxon.trans.XPathException;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A compiled regular expression implemented using the Java JDK regex package
 */
public class JRegularExpression implements RegularExpression {

    Pattern pattern;
    String javaRegex;
    int flagBits;

    /**
     * Create a regular expression, starting with an already-translated Java regex
     * @param javaRegex the regular expression after translation to Java notation
     * @param flagBits the option bits, derived from the user-specified flags
     */

    public JRegularExpression(String javaRegex, int flagBits) {
        this.flagBits = flagBits;
        this.javaRegex = javaRegex;
        String j = System.getProperty("java.version");
        if (j.startsWith("1.4")) {
            pattern = Pattern.compile(javaRegex, flagBits & (~Pattern.COMMENTS));
        } else {
            pattern = Pattern.compile(javaRegex, flagBits & (~(Pattern.COMMENTS | Pattern.CASE_INSENSITIVE)));
        }
    }

    /**
     * Create (compile) a regular expression
     * @param regex the source text of the regular expression, in XML Schema or XPath syntax
     * @param xmlVersion indicates whether \i and \c have their XML 1.0 or XML 1.1 meanings
     * @param syntax indicates whether XPath, XML Schema, or Java native regular expression syntax is to be used
     * @param flagBits the flags argument translated to the Java bit-significant integer representation
     * @throws org.orbeon.saxon.trans.XPathException if the syntax of the regular expression or flags is incorrect
     */

    public JRegularExpression(CharSequence regex, int xmlVersion, int syntax, int flagBits) throws XPathException {
        this.flagBits = flagBits;
        try {
            String j = System.getProperty("java.version");
            if (syntax == NATIVE_SYNTAX) {
                javaRegex = regex.toString();
                pattern = Pattern.compile(javaRegex, flagBits);
            } else if (j.startsWith("1.4")) {
                JDK14RegexTranslator translator = new JDK14RegexTranslator();
                translator.setIgnoreWhitespace((flagBits & Pattern.COMMENTS) != 0);
                javaRegex = translator.translate(
                        regex, xmlVersion, syntax==XPATH_SYNTAX);
                pattern = Pattern.compile(javaRegex, flagBits & (~Pattern.COMMENTS));
            } else {
                boolean ignoreWhitespace = ((flagBits & Pattern.COMMENTS) != 0);
                boolean caseBlind = ((flagBits & Pattern.CASE_INSENSITIVE) != 0);
                javaRegex = JDK15RegexTranslator.translate(
                        regex, xmlVersion, syntax==XPATH_SYNTAX, ignoreWhitespace, caseBlind);
                pattern = Pattern.compile(javaRegex, flagBits & (~(Pattern.COMMENTS | Pattern.CASE_INSENSITIVE)));
            }

        } catch (RegexSyntaxException e) {
            XPathException err = new XPathException(e.getMessage());
            err.setErrorCode("FORX0002");
            throw err;
        }
    }

    /**
     * Get the Java regular expression (after translation from an XPath regex, but before compilation)
     * @return the regular expression in Java notation
     */

    public String getJavaRegularExpression() {
        return javaRegex;
    }

    /**
     * Get the flag bits as used by the Java regular expression engine
     * @return the flag bits
     */

    public int getFlagBits() {
        return flagBits;
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
            return matcher.replaceAll(replacement.toString());
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
            return matcher.replaceAll(rep);
        }

    }

    /**
     * Use this regular expression to tokenize an input string.
     *
     * @param input the string to be tokenized
     * @return a SequenceIterator containing the resulting tokens, as objects of type StringValue
     */

    public SequenceIterator tokenize(CharSequence input) {
        if (input.length() == 0) {
            return EmptyIterator.getInstance();
        }
        return new JTokenIterator(input, pattern);
    }

    /**
     * Set the Java flags from the supplied XPath flags.
     * @param inFlags the flags as a string, e.g. "im"
     * @return the flags as a bit-significant integer
     * @throws XPathException if the supplied value is invalid
     */

    public static int setFlags(CharSequence inFlags) throws XPathException {
        int flags = Pattern.UNIX_LINES;
        for (int i=0; i<inFlags.length(); i++) {
            char c = inFlags.charAt(i);
            switch (c) {
            case'm':
                flags |= Pattern.MULTILINE;
                break;
            case'i':
                flags |= Pattern.CASE_INSENSITIVE;
                flags |= Pattern.UNICODE_CASE;
                break;
            case's':
                flags |= Pattern.DOTALL;
                break;
            case'x':
                flags |= Pattern.COMMENTS;  // note, this enables comments as well as whitespace
                break;
            default:
                XPathException err = new XPathException("Invalid character '" + c + "' in regular expression flags");
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
//


