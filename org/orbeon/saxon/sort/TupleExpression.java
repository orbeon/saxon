package net.sf.saxon.sort;

import net.sf.saxon.expr.*;
import net.sf.saxon.om.Item;
import net.sf.saxon.om.NamePool;
import net.sf.saxon.om.SequenceIterator;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.ExternalObjectType;
import net.sf.saxon.type.ItemType;
import net.sf.saxon.value.ObjectValue;
import net.sf.saxon.value.Value;

import java.io.PrintStream;
import java.util.Arrays;
import java.util.Iterator;

/**
 * A tuple expression is an expression that returns a tuple. Specifically,
 * it is a list of n expressions, which are evaluated to create a list of n items.
 * Tuple expressions are used during the evaluation of a FLWR expression. A tuple
 * is not a value within the XPath/XQuery type system, so it is represented as
 * an external object, specifically as a Java array wrapped inside an ObjectValue.
 *
 */
public class TupleExpression extends ComputedExpression {

    Expression[] components;

    public TupleExpression(int width) {
        components = new Expression[width];
    }

    public void setExpression(int i, Expression exp) {
        components[i] = exp;
    }

     public Expression simplify(StaticContext env) throws XPathException {
        for (int i=0; i<components.length; i++) {
            components[i] = components[i].simplify(env);
        }
        return this;
    }

    public Expression promote(PromotionOffer offer) throws XPathException {
        for (int i=0; i<components.length; i++) {
            components[i] = components[i].promote(offer);
        }
        return this;
    }

    public ItemType getItemType() {
        return new ExternalObjectType(Object.class);
    }

    public Expression analyze(StaticContext env, ItemType contextItemType) throws XPathException {
        for (int i=0; i<components.length; i++) {
            components[i] = components[i].analyze(env, contextItemType);
        }
        return this;
    }

    public void display(int level, NamePool pool, PrintStream out) {
        out.println(ExpressionTool.indent(level) + "Tuple");
        for (int i=0; i<components.length; i++) {
            components[i].display(level+1, pool, out);
        }
    }


    public Item evaluateItem(XPathContext context) throws XPathException {
        Value[] tuple = new Value[components.length];
        // TODO: Use eager evaluation to get the first sort key, because it will always be needed.
        // For the return value and the other sort keys, use lazy evaluation
        for (int i=0; i<components.length; i++) {
            tuple[i] = Value.asValue(ExpressionTool.lazyEvaluate(components[i], context, true));
        }
        return new ObjectValue(tuple);
    }

    /**
     * Get the cardinality of the expression. This is exactly one, in the sense
     * that evaluating the TupleExpression returns a single tuple.
     * @return the static cardinality - EXACTLY_ONE
     */

    public int computeCardinality() {
        return StaticProperty.EXACTLY_ONE;
    }

    public int getIntrinsicDependencies() {
        return 0;
    }

    public Iterator iterateSubExpressions() {
        return Arrays.asList(components).iterator();
    }

//    public SequenceIterator iterate(XPathContext context) throws XPathException {
//        return super.iterate(context);
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
// Contributor(s): none
//