package net.sf.saxon.functions;
import net.sf.saxon.expr.Expression;
import net.sf.saxon.expr.StaticContext;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.om.Item;
import net.sf.saxon.type.RegexTranslator;
import net.sf.saxon.value.AtomicValue;
import net.sf.saxon.value.StringValue;
import net.sf.saxon.value.Value;
import net.sf.saxon.xpath.DynamicError;
import net.sf.saxon.xpath.XPathException;

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;


/**
* This class implements the replace() function for replacing
* substrings that match a regular expression
*/

public class Replace extends SystemFunction {

    private Pattern regexp;

    /**
    * Simplify and validate.
    * This is a pure function so it can be simplified in advance if the arguments are known
    */

     public Expression simplify(StaticContext env) throws XPathException {
        Expression e = simplifyArguments(env);

        // compile the regular expression once if possible
        if (!(e instanceof Value)) {
            regexp = Matches.tryToCompile(argument, 1, 3);

            // check that it's not a pattern that matches ""
            if (regexp != null && regexp.matcher("").matches()) {
                DynamicError err = new DynamicError(
                        "The regular expression must not be one that matches a zero-length string");
                err.setErrorCode("FORX0003");
                throw err;
            }
        }

        return e;
    }


    /**
    * Evaluate the function in a string context
    */

    public Item evaluateItem(XPathContext c) throws XPathException {

        AtomicValue arg0 = (AtomicValue)argument[0].evaluateItem(c);
        if (arg0==null) {
            return StringValue.EMPTY_STRING;
        }

        AtomicValue arg2 = (AtomicValue)argument[2].evaluateItem(c);
        String replacement = arg2.getStringValue();
        checkReplacement(replacement, c);

        Pattern re = regexp;
        if (re == null) {

            AtomicValue arg1 = (AtomicValue)argument[1].evaluateItem(c);

            String flags;

            if (argument.length == 3) {
                flags = "";
            } else {
                AtomicValue arg3 = (AtomicValue)argument[3].evaluateItem(c);
                flags = arg3.getStringValue();
            }

            try {
                String javaRegex = RegexTranslator.translate(
                        arg1.getStringValue(), true);
                re = Pattern.compile(javaRegex, Matches.setFlags(flags));
            } catch (RegexTranslator.RegexSyntaxException err) {
                DynamicError de = new DynamicError(err);
                de.setErrorCode("FORX0002");
                de.setXPathContext(c);
                throw de;
            } catch (PatternSyntaxException err) {
                DynamicError de = new DynamicError(err);
                de.setErrorCode("FORX0002");
                de.setXPathContext(c);
                throw de;
            }

            // check that it's not a pattern that matches ""
            if (re.matcher("").matches()) {
                dynamicError(
                        "The regular expression must not be one that matches a zero-length string", "FORX0003", c);
            }
        }
        String res = re.matcher(arg0.getStringValue()).replaceAll(replacement);
        return new StringValue(res);
    }

    /**
    * Check the contents of the replacement string
    */

    private void checkReplacement(String rep, XPathContext context) throws XPathException {
        for (int i=0; i<rep.length(); i++) {
            char c = rep.charAt(i);
            if (c == '$') {
                if (i+1 < rep.length()) {
                    char next = rep.charAt(++i);
                    if (next < '0' || next > '9') {
                        dynamicError("Invalid replacement string in replace(): $ sign must be followed by digit 0-9",
                                "FORX0004", context);
                    }
                } else {
                    dynamicError("Invalid replacement string in replace(): $ sign at end of string",
                            "FORX0004", context);
                }
            } else if (c == '\\') {
                if (i+1 < rep.length()) {
                    char next = rep.charAt(++i);
                    if (next != '\\' && next != '$') {
                        dynamicError("Invalid replacement string in replace(): \\ character must be followed by \\ or $",
                                "FORX0004", context);
                    }
                } else {
                    dynamicError("Invalid replacement string in replace(): \\ character at end of string",
                            "FORX0004", context);
                }
            }
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
