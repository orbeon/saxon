package org.orbeon.saxon.functions;
import org.orbeon.saxon.Configuration;
import org.orbeon.saxon.Platform;
import org.orbeon.saxon.type.ItemType;
import org.orbeon.saxon.expr.Expression;
import org.orbeon.saxon.expr.ExpressionVisitor;
import org.orbeon.saxon.expr.XPathContext;
import org.orbeon.saxon.om.EmptyIterator;
import org.orbeon.saxon.om.SequenceIterator;
import org.orbeon.saxon.regex.RegularExpression;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.value.AtomicValue;

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
     * @param visitor an expression visitor
     */

    public Expression simplify(ExpressionVisitor visitor) throws XPathException {
        Expression e = simplifyArguments(visitor);
        if (e == this) {
            maybePrecompile(visitor);
        }
        return e;
    }

    private void maybePrecompile(ExpressionVisitor visitor) throws XPathException {
        // compile the regular expression once if possible
        if (regexp == null) {
            try {
                regexp = Matches.tryToCompile(argument, 1, 2, visitor.getStaticContext());
            } catch (XPathException err) {
                err.setLocator(this);
                throw err;
            }
            // check that it's not a pattern that matches ""
            if (regexp != null && regexp.matches("")) {
                XPathException err = new XPathException("The regular expression in tokenize() must not be one that matches a zero-length string");
                err.setErrorCode("FORX0003");
                err.setLocator(this);
                throw err;
            }
        }
    }


    /**
     * Perform optimisation of an expression and its subexpressions.
     * <p/>
     * <p>This method is called after all references to functions and variables have been resolved
     * to the declaration of the function or variable, and after all type checking has been done.</p>
     *
     * @param visitor         an expression visitor
     * @param contextItemType the static type of "." at the point where this expression is invoked.
     *                        The parameter is set to null if it is known statically that the context item will be undefined.
     *                        If the type of the context item is not known statically, the argument is set to
     *                        {@link org.orbeon.saxon.type.Type#ITEM_TYPE}
     * @return the original expression, rewritten if appropriate to optimize execution
     * @throws org.orbeon.saxon.trans.XPathException
     *          if an error is discovered during this phase
     *          (typically a type error)
     */

    public Expression optimize(ExpressionVisitor visitor, ItemType contextItemType) throws XPathException {
        Expression e = super.optimize(visitor, contextItemType);
        // try once again to compile the regular expression once if possible
        // (used when the regex has been identified as a constant as a result of earlier rewrites)
        if (e == this) {
            maybePrecompile(visitor);
        }
        return e;
    }

    /**
     * Get the compiled regular expression if available, otherwise return null
     * @return the compiled regular expression, or null
     */

    public RegularExpression getCompiledRegularExpression() {
        return regexp;
    }

    /**
    * Iterate over the results of the function
    */

    public SequenceIterator iterate(XPathContext c) throws XPathException {
        AtomicValue sv = (AtomicValue)argument[0].evaluateItem(c);
        if (sv==null) {
            return EmptyIterator.getInstance();
        }
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
                final Platform platform = Configuration.getPlatform();
                final int xmlVersion = c.getConfiguration().getXMLVersion();
                re = platform.compileRegularExpression(
                        pattern, xmlVersion, RegularExpression.XPATH_SYNTAX, flags);
            } catch (XPathException err) {
                XPathException de = new XPathException(err);
                de.setErrorCode("FORX0002");
                de.setXPathContext(c);
                de.setLocator(this);
                throw de;
            }
            // check that it's not a pattern that matches ""
            if (re.matches("")) {
                XPathException err = new XPathException("The regular expression in tokenize() must not be one that matches a zero-length string");
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
