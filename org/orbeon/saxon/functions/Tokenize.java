package org.orbeon.saxon.functions;
import org.orbeon.saxon.Platform;
import org.orbeon.saxon.expr.Expression;
import org.orbeon.saxon.expr.StaticContext;
import org.orbeon.saxon.expr.XPathContext;
import org.orbeon.saxon.expr.Container;
import org.orbeon.saxon.om.EmptyIterator;
import org.orbeon.saxon.om.SequenceIterator;
import org.orbeon.saxon.regex.RegularExpression;
import org.orbeon.saxon.trans.DynamicError;
import org.orbeon.saxon.trans.StaticError;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.value.AtomicValue;
import org.orbeon.saxon.value.Value;

import java.util.regex.Pattern;


/**
* This class implements the tokenize() function for regular expression matching. This returns a
* sequence of strings representing the unmatched substrings: the separators which match the
* regular expression are not returned.
*/

public class Tokenize extends SystemFunction  {

    private RegularExpression regexp;

    /**
    * Simplify and validate.
    * This is a pure function so it can be simplified in advance if the arguments are known
    */

     public Expression simplify(StaticContext env) throws XPathException {
        Expression e = simplifyArguments(env);

        // compile the regular expression once if possible
        if (regexp == null && !(e instanceof Value)) {
            try {
                regexp = Matches.tryToCompile(argument, 1, 2, env);
            } catch (StaticError err) {
                err.setLocator(this);
                throw err;
            }
            // check that it's not a pattern that matches ""
            if (regexp != null && regexp.matches("")) {
                StaticError err = new StaticError(
                        "The regular expression in tokenize() must not be one that matches a zero-length string");
                err.setErrorCode("FORX0003");
                err.setLocator(this);
                throw err;
            }
        }

        return e;
    }

    /**
    * Iterate over the results of the function
    */

    public SequenceIterator iterate(XPathContext c) throws XPathException {
        AtomicValue sv = (AtomicValue)argument[0].evaluateItem(c);
        if (sv==null) {
            return EmptyIterator.getInstance();
        };
        CharSequence input = sv.getStringValueCS();
        if (input.length() == 0) {
            return EmptyIterator.getInstance();
        }

        RegularExpression re = regexp;
        if (re == null) {

            sv = (AtomicValue)argument[1].evaluateItem(c);
            CharSequence pattern = sv.getStringValueCS();

            CharSequence flags;
            if (argument.length==2) {
                flags = "";
            } else {
                sv = (AtomicValue)argument[2].evaluateItem(c);
                flags = sv.getStringValueCS();
            }

            try {
                final Platform platform = c.getConfiguration().getPlatform();
                re = platform.compileRegularExpression(pattern, true, flags);
            } catch (XPathException err) {
                DynamicError de = new DynamicError(err);
                de.setErrorCode("FORX0002");
                de.setXPathContext(c);
                de.setLocator(this);
                throw de;
            }
            // check that it's not a pattern that matches ""
            if (re.matches("")) {
                StaticError err = new StaticError(
                        "The regular expression in tokenize() must not be one that matches a zero-length string");
                err.setErrorCode("FORX0003");
                err.setLocator(this);
                throw err;
            }

        }
        return re.tokenize(input);
    }


    /**
     * Simple command-line interface for testing.
     * @param args (1) the string to be tokenized (2) the regular expression
     * @throws Exception
     */

    public static void main(String[] args) throws Exception {
        String in = args[0];
        String[] out = Pattern.compile(args[1]).split(in, 0);
        System.out.println("results");
        for (int i=0; i<out.length; i++) {
            System.out.println('[' + out[i] + ']');
        }
        System.out.println("end results");
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
