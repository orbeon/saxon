package org.orbeon.saxon.style;
import org.orbeon.saxon.expr.*;
import org.orbeon.saxon.functions.Concat;
import org.orbeon.saxon.functions.StringJoin;
import org.orbeon.saxon.functions.SystemFunction;
import org.orbeon.saxon.om.NamePool;
import org.orbeon.saxon.type.Type;
import org.orbeon.saxon.value.Cardinality;
import org.orbeon.saxon.value.StringValue;
import org.orbeon.saxon.xpath.XPathException;
import org.orbeon.saxon.xpath.StaticError;

import java.util.ArrayList;
import java.util.List;

/**
* This class represents an attribute value template. The class allows an AVT to be parsed, and
* can construct an Expression that returns the effective value of the AVT.
*
* This is an abstract class that is never instantiated, it contains static methods only.
*/

public abstract class AttributeValueTemplate {

    private AttributeValueTemplate() {}


    /**
     * Static factory method to create an AVT from an XSLT string representation.
    */

    public static Expression make(String avt,
                                  int start,
                                  char terminator,
                                  int lineNumber,
                                  StaticContext env) throws XPathException {

        List components = new ArrayList();

        int i0, i1, i2, i8, i9, len, last;
        last = start;
        len = avt.length();
        while (last < len) {
            i2 = avt.indexOf(terminator, last);
            i0 = avt.indexOf("{", last);
            i1 = avt.indexOf("{{", last);
            i8 = avt.indexOf("}", last);
            i9 = avt.indexOf("}}", last);

            if ((i0 < 0 || i2 < i0) && (i8 < 0 || i2 < i8)) {   // found end of string
                addStringComponent(components, avt, last, i2);
                last = i2;
                break;
            } else if (i0 >= 0 && i0 != i1 && i8 < i0) {   // found a "{" with no matching "}"
                StaticError err = new StaticError(
                        "Unmatched opening curly brace in attribute value template \"" + avt.substring(0,i2) + "\"");
                err.setErrorCode("XT0350");
                throw err;
            } else if (i8 >= 0 && (i0 < 0 || i8 < i0)) {             // found a "}"
                if (i8 != i9) {                        // a "}" that isn't a "}}"
                    StaticError err = new StaticError(
                            "Closing curly brace in attribute value template \"" + avt.substring(0,i2) + "\" must be doubled");
                    err.setErrorCode("XT0360");
                    throw err;
                }
                addStringComponent(components, avt, last, i8 + 1);
                last = i8 + 2;
            } else if (i1 >= 0 && i1 == i0) {              // found a doubled "{{"
                addStringComponent(components, avt, last, i1 + 1);
                last = i1 + 2;
            } else if (i0 >= 0) {                        // found a single "{"
                if (i0 > last) {
                    addStringComponent(components, avt, last, i0);
                }
                Expression exp;
                ExpressionParser parser = new ExpressionParser();
                exp = parser.parse(avt, i0 + 1, Token.RCURLY, lineNumber, env);
                exp = exp.simplify(env);
                last = parser.getTokenizer().currentTokenStartOffset + 1;

                if (env.isInBackwardsCompatibleMode()) {
                    components.add(makeFirstItem(exp));
                } else {
                    components.add(makeStringJoin(exp, env.getNamePool()));
                }

            } else {
                throw new IllegalStateException("Internal error parsing AVT");
            }
        }

        // is it empty?

        if (components.size() == 0) {
            return StringValue.EMPTY_STRING;
        }

        // is it a single component?

        if (components.size() == 1) {
            return ((Expression) components.get(0)).simplify(env);
        }

        // otherwise, return an expression that concatenates the components

        Concat fn = (Concat) SystemFunction.makeSystemFunction("concat", env.getNamePool());
        Expression[] args = new Expression[components.size()];
        components.toArray(args);
        fn.setArguments(args);
        fn.setLocationId(env.getLocationMap().allocateLocationId(env.getSystemId(), lineNumber));
        return fn.simplify(env);

    }

    private static void addStringComponent(List components, String avt, int start, int end) {
        if (start < end) {
            components.add(new StringValue(avt.substring(start, end)));
        }
    }

    /**
    * Make a string-join expression that concatenates the string-values of items in
    * a sequence with intervening spaces. This may be simplified later as a result
    * of type-checking.
    */

    public static Expression makeStringJoin(Expression exp, NamePool namePool) {

        exp = new Atomizer(exp);
        exp = new AtomicSequenceConverter(exp, Type.STRING_TYPE);

		StringJoin fn = (StringJoin)SystemFunction.makeSystemFunction("string-join", namePool);
		Expression[] args = new Expression[2];
		args[0] = exp;
		args[1] = new StringValue(" ");
		fn.setArguments(args);
        if (exp instanceof ComputedExpression) {
            fn.setLocationId(((ComputedExpression)exp).getLocationId());
        }
		return fn;
    }

    /**
    * Make an expression that extracts the first item of a sequence, after atomization
    */

    public static Expression makeFirstItem(Expression exp) {
        if (!Type.isSubType(exp.getItemType(), Type.ANY_ATOMIC_TYPE)) {
            exp = new Atomizer(exp);
        }
        if (Cardinality.allowsMany(exp.getCardinality())) {
            exp = new FirstItemExpression(exp);
        }
        if (!Type.isSubType(exp.getItemType(), Type.STRING_TYPE)) {
            exp = new AtomicSequenceConverter(exp, Type.STRING_TYPE);
        }
        return exp;
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
// The Initial Developer of the Original Code is Michael H. Kay.
//
// Portions created by (your name) are Copyright (C) (your legal entity). All Rights Reserved.
//
// Contributor(s): none.
//
