package org.orbeon.saxon.sort;

import org.orbeon.saxon.Configuration;
import org.orbeon.saxon.expr.*;
import org.orbeon.saxon.om.Item;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.type.ExternalObjectType;
import org.orbeon.saxon.type.ItemType;
import org.orbeon.saxon.type.TypeHierarchy;
import org.orbeon.saxon.value.ObjectValue;
import org.orbeon.saxon.value.Value;

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
    int[] evaluationModes;

    public TupleExpression(int width) {
        components = new Expression[width];
        evaluationModes = new int[width];
    }

    public void setExpression(int i, Expression exp) {
        components[i] = exp;
        adoptChildExpression(components[i]);
        evaluationModes[i] = ExpressionTool.UNDECIDED;
    }

     public Expression simplify(StaticContext env) throws XPathException {
        for (int i=0; i<components.length; i++) {
            components[i] = components[i].simplify(env);
            adoptChildExpression(components[i]);
        }
        return this;
    }

    public Expression promote(PromotionOffer offer) throws XPathException {
        for (int i=0; i<components.length; i++) {
            components[i] = doPromotion(components[i], offer);
            adoptChildExpression(components[i]);
        }
        return this;
    }

    public ItemType getItemType(TypeHierarchy th) {
        return new ExternalObjectType(Object.class, th.getConfiguration());
    }

    public Expression typeCheck(StaticContext env, ItemType contextItemType) throws XPathException {
        for (int i=0; i<components.length; i++) {
            components[i] = components[i].typeCheck(env, contextItemType);
            adoptChildExpression(components[i]);
        }
        return this;
    }

    /**
     * Perform optimisation of an expression and its subexpressions.
     * <p/>
     * <p>This method is called after all references to functions and variables have been resolved
     * to the declaration of the function or variable, and after all type checking has been done.</p>
     *
     * @param opt             the optimizer in use. This provides access to supporting functions; it also allows
     *                        different optimization strategies to be used in different circumstances.
     * @param env             the static context of the expression
     * @param contextItemType the static type of "." at the point where this expression is invoked.
     *                        The parameter is set to null if it is known statically that the context item will be undefined.
     *                        If the type of the context item is not known statically, the argument is set to
     *                        {@link org.orbeon.saxon.type.Type#ITEM_TYPE}
     * @return the original expression, rewritten if appropriate to optimize execution
     * @throws org.orbeon.saxon.trans.StaticError if an error is discovered during this phase
     *                                        (typically a type error)
     */

    public Expression optimize(Optimizer opt, StaticContext env, ItemType contextItemType) throws XPathException {
        for (int i=0; i<components.length; i++) {
            components[i] = components[i].optimize(opt, env, contextItemType);
            adoptChildExpression(components[i]);
            if (i < 2) {
                // Although TupleExpressions could be used for many purposes, in practice they are used
                // for delivering the tuples used in a FLWOR "order by" clause; the first item in the tuple
                // is the return value of the FLWOR, while the others are the sort keys. The return value and
                // the first sort key will always be needed, so we evaluate them eagerly. The second and subsequent
                // sort keys will often not be needed, so we evaluate them lazily.
                evaluationModes[i] = ExpressionTool.eagerEvaluationMode(components[i]);
            } else {
                evaluationModes[i] = ExpressionTool.lazyEvaluationMode(components[i]);
            }
        }
        return this;
    }

    public void display(int level, PrintStream out, Configuration config) {
        out.println(ExpressionTool.indent(level) + "Tuple");
        for (int i=0; i<components.length; i++) {
            components[i].display(level+1, out, config);
        }
    }


    public Item evaluateItem(XPathContext context) throws XPathException {
        Value[] tuple = new Value[components.length];
        for (int i=0; i<components.length; i++) {
            tuple[i] = Value.asValue(ExpressionTool.evaluate(components[i], evaluationModes[i], context, 10));
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

    /**
      * Replace one subexpression by a replacement subexpression
      * @param original the original subexpression
      * @param replacement the replacement subexpression
      * @return true if the original subexpression is found
      */

     public boolean replaceSubExpression(Expression original, Expression replacement) {
         boolean found = false;
         for (int i=0; i<components.length; i++) {
             if (components[i] == original) {
                 components[i] = replacement;
                 found = true;
             }
         }
                 return found;
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
// Contributor(s): none
//