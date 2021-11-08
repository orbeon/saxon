package org.orbeon.saxon.functions;
import org.orbeon.saxon.Configuration;
import org.orbeon.saxon.Platform;
import org.orbeon.saxon.expr.*;
import org.orbeon.saxon.om.Item;
import org.orbeon.saxon.regex.RegularExpression;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.type.ItemType;
import org.orbeon.saxon.value.AtomicValue;
import org.orbeon.saxon.value.BooleanValue;
import org.orbeon.saxon.value.StringValue;


/**
* This class implements the matches() function for regular expression matching
*/

public class Matches extends SystemFunction {

    private RegularExpression regexp;

    /**
     * Simplify and validate.
     * This is a pure function so it can be simplified in advance if the arguments are known
     * @return the simplified expression
     * @throws XPathException if any error is found (e.g. invalid regular expression)
     * @param visitor an expression visitor
     */

     public Expression simplify(ExpressionVisitor visitor) throws XPathException {
        Expression e = simplifyArguments(visitor);
        // compile the regular expression once if possible
        if (e == this) {
            maybePrecompile(visitor);
        }
        return e;
    }

    /**
     * Precompile the regular expression if possible
     * @param visitor an expression visitor
     */

    private void maybePrecompile(ExpressionVisitor visitor) throws XPathException {
        if (regexp == null) {
            try {
                regexp = tryToCompile(argument, 1, 2, visitor.getStaticContext());
            } catch (XPathException err) {
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
     * Get the compiled regular expression, returning null if the regex has not been compiled
     * @return the compiled regular expression, or null
     */

    public RegularExpression getCompiledRegularExpression() {
        return regexp;
    }

    /**
     * Evaluate the matches() function to give a Boolean value.
     * @param c  The dynamic evaluation context
     * @return the result as a BooleanValue, or null to indicate the empty sequence
     * @throws XPathException on an error
     */

    public Item evaluateItem(XPathContext c) throws XPathException {
        AtomicValue sv0 = (AtomicValue)argument[0].evaluateItem(c);
        if (sv0==null) {
            sv0 = StringValue.EMPTY_STRING;
        }

        RegularExpression re = regexp;

        if (re == null) {
            AtomicValue pat = (AtomicValue)argument[1].evaluateItem(c);
            if (pat==null) return null;

            CharSequence flags;
            if (argument.length==2) {
                flags = "";
            } else {
                AtomicValue sv2 = (AtomicValue)argument[2].evaluateItem(c);
                if (sv2==null) return null;
                flags = sv2.getStringValueCS();
            }

            try {
                final Platform platform = Configuration.getPlatform();
                final int xmlVersion = c.getConfiguration().getXMLVersion();
                re = platform.compileRegularExpression(
                        pat.getStringValueCS(), xmlVersion, RegularExpression.XPATH_SYNTAX, flags);
            } catch (XPathException err) {
                XPathException de = new XPathException(err);
                if (de.getErrorCodeLocalPart() == null) {
                    de.setErrorCode("FORX0002");
                }
                de.setXPathContext(c);
                throw de;
            }
        }
        return BooleanValue.get(re.containsMatch(sv0.getStringValueCS()));
    }

    /**
     * Temporary test rig, used to submit bug report to Sun
     */
//     public static void main(String[] args) throws Exception {
//
//        matches("\u212a", "K");
//        matches("\u212a", "[A-Z]");
//        matches("\u212a", "I|J|K|L");
//        matches("\u212a", "[IJKL]");
//        matches("\u212a", "k");
//        matches("\u212a", "[a-z]");
//        matches("\u212a", "i|j|k|l");
//        matches("\u212a", "[ijkl]");
//    }
//
//    private static void matches(String in, String pattern) {
//        System.err.println("Java version " + System.getProperty("java.version"));
//        int flags = Pattern.UNIX_LINES;
//        flags |= Pattern.CASE_INSENSITIVE;
//        flags |= Pattern.UNICODE_CASE;
//        Pattern p = Pattern.compile(pattern, flags);
//        boolean b = p.matcher(in).find();
//        System.err.println("Pattern " + pattern + ": " + (b ? " match" : "no match"));
//    }

//    Results of this test with JDK 1.5.0_05:
//
//    Pattern K:  match
//    Java version 1.5.0_05
//    Pattern [A-Z]: no match
//    Java version 1.5.0_05
//    Pattern I|J|K|L:  match
//    Java version 1.5.0_05
//    Pattern [IJKL]: no match
//    Java version 1.5.0_05
//    Pattern k:  match
//    Java version 1.5.0_05
//    Pattern [a-z]:  match
//    Java version 1.5.0_05
//    Pattern i|j|k|l:  match
//    Java version 1.5.0_05
//    Pattern [ijkl]: no match

    /**
     * Try to precompile the arguments to the function. This method is shared by
     * the implementations of the three XPath functions matches(), replace(), and
     * tokenize().
     * @param args the supplied arguments to the function, as an array
     * @param patternArg the position of the argument containing the regular expression
     * @param flagsArg the position of the argument containing the flags
     * @param env the static context
     * @return the compiled regular expression, or null indicating that the information
     * is not available statically so it cannot be precompiled
     * @throws XPathException if any failure occurs, in particular, if the regular
     * expression is invalid
     */

    public static RegularExpression tryToCompile(Expression[] args, int patternArg, int flagsArg, StaticContext env)
    throws XPathException {
        if (patternArg > args.length - 1) {
            // too few arguments were supplied; the error will be reported in due course
            return null;
        }
        CharSequence flagstr = null;
        if (args.length-1 < flagsArg) {
            flagstr = "";
        } else if (args[flagsArg] instanceof StringLiteral) {
            flagstr = ((StringLiteral)args[flagsArg]).getStringValue();
        }

        if (args[patternArg] instanceof StringLiteral && flagstr != null) {
            try {
                Platform platform = Configuration.getPlatform();
                String in = ((StringLiteral)args[patternArg]).getStringValue();
                final int xmlVersion = env.getConfiguration().getXMLVersion();
                int syntax = RegularExpression.XPATH_SYNTAX;
                // TODO: Find a better (conformant) way of switching this option on
                if (flagstr.length() > 0 && flagstr.charAt(0) == '!') {
                    flagstr = flagstr.subSequence(1, flagstr.length());
                    syntax = RegularExpression.NATIVE_SYNTAX;
                }
                return platform.compileRegularExpression(in, xmlVersion, syntax, flagstr);
            } catch (XPathException err) {
                if (err.getErrorCodeLocalPart() == null) {
                    err.setErrorCode("FORX0002");
                }
                throw err;
            }
        } else {
            return null;
        }
    }


//    public static void main(String[] args) {
//        System.out.println(Pattern.matches("(X)(2)?(3)?(4)?(5)?(6)?(7)?(8)?(9)?(10)?((Y)(\\12))", "XYY"));
////        String pat = "(X)(2)(3)(4)(5)(6)(7)(8)(9)(10)(11(12)(13)\\11)";
////        System.out.println(Pattern.matches(pat, "X2345678910111213X1"));
//    }
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
