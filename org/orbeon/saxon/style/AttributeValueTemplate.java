package net.sf.saxon.style;
import net.sf.saxon.expr.*;
import net.sf.saxon.functions.Concat;
import net.sf.saxon.functions.SystemFunction;
import net.sf.saxon.instruct.SimpleContentConstructor;
import net.sf.saxon.trans.StaticError;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.Type;
import net.sf.saxon.value.Cardinality;
import net.sf.saxon.value.StringValue;

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
                                  int lineNumber,
                                  StaticContext env) throws XPathException {

        List components = new ArrayList(5);

        int i0, i1, i8, i9;
        int len = avt.length();
        int last = 0;
        while (last < len) {

            i0 = avt.indexOf("{", last);
            i1 = avt.indexOf("{{", last);
            i8 = avt.indexOf("}", last);
            i9 = avt.indexOf("}}", last);

            if ((i0 < 0 || len < i0) && (i8 < 0 || len < i8)) {   // found end of string
                addStringComponent(components, avt, last, len);
                break;
            } else if (i8 >= 0 && (i0 < 0 || i8 < i0)) {             // found a "}"
                if (i8 != i9) {                        // a "}" that isn't a "}}"
                    StaticError err = new StaticError(
                            "Closing curly brace in attribute value template \"" + avt.substring(0,len) + "\" must be doubled");
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
                    components.add(makeFirstItem(exp, env));
                } else {
                    components.add(new SimpleContentConstructor(exp, StringValue.SINGLE_SPACE));
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

        Concat fn = (Concat) SystemFunction.makeSystemFunction("concat", components.size(), env.getNamePool());
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
    * Make an expression that extracts the first item of a sequence, after atomization
    */

    public static Expression makeFirstItem(Expression exp, StaticContext env) {
        if (!Type.isSubType(exp.getItemType(), Type.ANY_ATOMIC_TYPE)) {
            exp = new Atomizer(exp, env.getConfiguration());
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
