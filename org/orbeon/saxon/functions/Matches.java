package net.sf.saxon.functions;
import net.sf.saxon.expr.Expression;
import net.sf.saxon.expr.StaticContext;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.om.Item;
import net.sf.saxon.trans.DynamicError;
import net.sf.saxon.trans.StaticError;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.RegexTranslator;
import net.sf.saxon.value.AtomicValue;
import net.sf.saxon.value.BooleanValue;
import net.sf.saxon.value.StringValue;
import net.sf.saxon.value.Value;

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;


/**
* This class implements the matches() function for regular expression matching
*/

public class Matches extends SystemFunction {

    private Pattern regexp;

    /**
    * Simplify and validate.
    * This is a pure function so it can be simplified in advance if the arguments are known
    * @return the simplified expression
    * @throws net.sf.saxon.trans.StaticError if any error is found (e.g. invalid regular expression)
    */

     public Expression simplify(StaticContext env) throws XPathException {
        Expression e = simplifyArguments(env);

        // compile the regular expression once if possible
        if (!(e instanceof Value)) {
            regexp = Matches.tryToCompile(argument, 1, 2);
        }

        return e;
    }

    /**
     * Set the Java flags from the supplied XPath flags.
     * @param inFlags the flags as a string, e.g. "im"
     * @return the flags as a bit-significant integer
     * @throws net.sf.saxon.trans.StaticError if the supplied value is invalid
     */

    public static int setFlags(CharSequence inFlags) throws StaticError {
        int flags = Pattern.UNIX_LINES;
        for (int i=0; i<inFlags.length(); i++) {
            char c = inFlags.charAt(i);
            switch (c) {
                case 'm':
                    flags |= Pattern.MULTILINE;
                    break;
                case 'i':
                    flags |= Pattern.CASE_INSENSITIVE;
                    break;
                case 's':
                    flags |= Pattern.DOTALL;
                    break;
                case 'x':
                    flags |= Pattern.COMMENTS;  // note, this enables comments as well as whitespace
                    break;
                default:
                    StaticError err = new StaticError("Invalid character '" + c + "' in regular expression flags");
                    err.setErrorCode("FORX0001");
                    throw err;
            }
        }
        return flags;
    }

    /**
     * Try to precompile the arguments to the function. This method is shared by
     * the implementations of the three XPath functions matches(), replace(), and
     * tokenize().
     * @param args the supplied arguments to the function, as an array
     * @param patternArg the position of the argument containing the regular expression
     * @param flagsArg the position of the argument containing the flags
     * @return the compiled regular expression, or null indicating that the information
     * is not available statically so it cannot be precompiled
     * @throws net.sf.saxon.trans.StaticError if any failure occurs, in particular, if the regular
     * expression is invalid
     */

    protected static Pattern tryToCompile(Expression[] args, int patternArg, int flagsArg)
    throws StaticError {
        if (patternArg > args.length - 1) {
            // too few arguments were supplied; the error will be reported in due course
            return null;
        }
        CharSequence flagstr = null;
        if (args.length-1 < flagsArg) {
            flagstr = "";
        } else if (args[flagsArg] instanceof StringValue) {
            flagstr = ((StringValue)args[flagsArg]).getStringValueCS();
        }

        if (args[patternArg] instanceof StringValue && flagstr != null) {
            int flags = Matches.setFlags(flagstr);

            try {
                String javaRegex = RegexTranslator.translate(
                        ((StringValue)args[patternArg]).getStringValueCS(), true);
                return Pattern.compile(javaRegex, flags);
            } catch (RegexTranslator.RegexSyntaxException err) {
                throw new StaticError(err);
            } catch (PatternSyntaxException err) {
                throw new StaticError(err);
            }
        } else {
            return null;
        }
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
        };

        Pattern re = regexp;
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
                String javaRegex = RegexTranslator.translate(pat.getStringValueCS(), true);
                re = Pattern.compile(javaRegex, setFlags(flags));
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
            } catch (StaticError serr) {
                dynamicError(serr.getMessage(), serr.getErrorCodeLocalPart(), c);
            }
        }
        return BooleanValue.get(re.matcher(sv0.getStringValueCS()).find());
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
