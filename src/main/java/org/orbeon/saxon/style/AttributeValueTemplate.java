package org.orbeon.saxon.style;
import org.orbeon.saxon.Configuration;
import org.orbeon.saxon.expr.*;
import org.orbeon.saxon.functions.Concat;
import org.orbeon.saxon.functions.SystemFunction;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.type.BuiltInAtomicType;
import org.orbeon.saxon.type.TypeHierarchy;
import org.orbeon.saxon.value.Cardinality;
import org.orbeon.saxon.value.StringValue;

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
        ExpressionVisitor visitor = ExpressionVisitor.make(env);
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
                    XPathException err = new XPathException("Closing curly brace in attribute value template \"" + avt.substring(0, len) + "\" must be doubled");
                    err.setErrorCode("XTSE0370");
                    err.setIsStaticError(true);
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
                exp = visitor.simplify(exp);
                last = parser.getTokenizer().currentTokenStartOffset + 1;

                if (env.isInBackwardsCompatibleMode()) {
                    components.add(makeFirstItem(exp, env));
                } else {
                    components.add(visitor.simplify(
                            makeSimpleContentConstructor(
                                    exp,
                                    new StringLiteral(StringValue.SINGLE_SPACE), env.getConfiguration())));
                }

            } else {
                throw new IllegalStateException("Internal error parsing AVT");
            }
        }

        // is it empty?

        if (components.size() == 0) {
            return new StringLiteral(StringValue.EMPTY_STRING);
        }

        // is it a single component?

        if (components.size() == 1) {
            return visitor.simplify((Expression) components.get(0));
        }

        // otherwise, return an expression that concatenates the components
        
        Expression[] args = new Expression[components.size()];
        components.toArray(args);
        Concat fn = (Concat) SystemFunction.makeSystemFunction("concat", args);
        fn.setLocationId(env.getLocationMap().allocateLocationId(env.getSystemId(), lineNumber));
        return visitor.simplify(fn);

    }

    /**
     * ORBEON: Backport from 9.3.
     *
     * Construct an expression that implements the rules of "constructing simple content":
     * given an expression to select the base sequence, and an expression to compute the separator,
     * build an (unoptimized) expression to produce the value of the node as a string.
     * @param select the expression that selects the base sequence
     * @param separator the expression that computes the separator
     * @param config the Saxon configuration
     * @return an expression that returns a string containing the string value of the constructed node
     */
    public static Expression makeSimpleContentConstructor(Expression select, Expression separator, Configuration config) {
        // Merge adjacent text nodes
        //select = new AdjacentTextNodeMerger(select);
        // Atomize the result
        select = new Atomizer(select, config);
        // Convert each atomic value to a string
        select = new AtomicSequenceConverter(select, BuiltInAtomicType.STRING);
        // Join the resulting strings with a separator
        select = SystemFunction.makeSystemFunction("string-join", new Expression[]{select, separator});
        // All that's left for the instruction to do is to construct the right kind of node
        return select;
    }

    private static void addStringComponent(List components, String avt, int start, int end) {
        if (start < end) {
            components.add(new StringLiteral(avt.substring(start, end)));
        }
    }

    /**
    * Make an expression that extracts the first item of a sequence, after atomization
    */

    public static Expression makeFirstItem(Expression exp, StaticContext env) {
        final TypeHierarchy th = env.getConfiguration().getTypeHierarchy();
        if (!exp.getItemType(th).isAtomicType()) {
            exp = new Atomizer(exp, env.getConfiguration());
        }
        if (Cardinality.allowsMany(exp.getCardinality())) {
            exp = new FirstItemExpression(exp);
        }
        if (!th.isSubType(exp.getItemType(th), BuiltInAtomicType.STRING)) {
            exp = new AtomicSequenceConverter(exp, BuiltInAtomicType.STRING);
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
