package net.sf.saxon.expr;
import net.sf.saxon.om.Item;
import net.sf.saxon.om.NamePool;
import net.sf.saxon.om.SequenceIterator;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.ItemType;
import net.sf.saxon.type.SchemaType;
import net.sf.saxon.type.Type;
import net.sf.saxon.value.BooleanValue;
import net.sf.saxon.value.Cardinality;
import net.sf.saxon.value.Value;
import net.sf.saxon.value.EmptySequence;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Iterator;


/**
* An IfExpression returns the value of either the "then" part or the "else" part,
* depending on the value of the condition
*/

public class IfExpression extends ComputedExpression {

    private Expression condition;
    private Expression thenExp;
    private Expression elseExp;

    /**
    * Constructor
    */

    public IfExpression(Expression condition, Expression thenExp, Expression elseExp) {
        this.condition = condition;
        this.thenExp = thenExp;
        this.elseExp = elseExp;
        adoptChildExpression(condition);
        adoptChildExpression(thenExp);
        adoptChildExpression(elseExp);
    }

    public Expression getCondition() {
        return condition;
    }

    public Expression getThenExpression() {
        return thenExp;
    }

    public Expression getElseExpression() {
        return elseExp;
    }

    public void setCondition(Expression exp) {
        condition = exp;
        adoptChildExpression(exp);
    }

    public void setThenExpression(Expression exp) {
        thenExp = exp;
        adoptChildExpression(exp);
    }

    /**
    * Simplify an expression
    */

     public Expression simplify(StaticContext env) throws XPathException {

        condition = condition.simplify(env);

        if (condition instanceof Value) {
            return (condition.effectiveBooleanValue(null) ? thenExp.simplify(env) : elseExp.simplify(env));
        }
        thenExp = thenExp.simplify(env);
        elseExp = elseExp.simplify(env);
        return this;
    }

    /**
    * Type-check the expression
    */

    public Expression analyze(StaticContext env, ItemType contextItemType) throws XPathException {
        condition = condition.analyze(env, contextItemType);
        // If the condition after typechecking is reduced to a constant,
        // cut it down to the appropriate branch. This is especially important
        // when handling typeswitch, as otherwise the unused branches will
        // generate a type error.
        if (condition instanceof BooleanValue) {
            if (((BooleanValue)condition).getBooleanValue()) {
                return thenExp.analyze(env, contextItemType);
            } else {
                return elseExp.analyze(env, contextItemType);
            }
        } else {
            thenExp = thenExp.analyze(env, contextItemType);
            elseExp = elseExp.analyze(env, contextItemType);
            return simplify(env);
        }
    }

    /**
    * Promote this expression if possible
    */

    public Expression promote(PromotionOffer offer) throws XPathException {
        Expression exp = offer.accept(this);
        if (exp != null) {
            return exp;
        } else {
            // Promote subexpressions in the condition, but not in the "then" and "else"
            // branches, because these are guaranteed not to be evaluated if the condition
            // is false (bzw true).
            condition = condition.promote(offer);

            // allow "unordered" to trickle down to the subexpressions
            if (offer.action == PromotionOffer.UNORDERED ||
                    offer.action == PromotionOffer.INLINE_VARIABLE_REFERENCES) {
                thenExp = thenExp.promote(offer);
                elseExp = elseExp.promote(offer);
            }
            return this;
        }
    }

    /**
    * Get the immediate subexpressions of this expression
    */

    public Iterator iterateSubExpressions() {
        ArrayList a = new ArrayList(3);
        a.add(condition);
        a.add(thenExp);
        a.add(elseExp);
        return a.iterator();
    }

    /**
    * Mark tail calls on used-defined functions. For most expressions, this does nothing.
    */

    public boolean markTailFunctionCalls() {
        boolean a = ExpressionTool.markTailFunctionCalls(thenExp);
        boolean b = ExpressionTool.markTailFunctionCalls(elseExp);
        return a || b;
    }

    /**
     * Check that any elements and attributes constructed or returned by this expression are acceptable
     * in the content model of a given complex type. It's always OK to say yes, since the check will be
     * repeated at run-time. The process of checking element and attribute constructors against the content
     * model of a complex type also registers the type of content expected of those constructors, so the
     * static validation can continue recursively.
     */

    public void checkPermittedContents(SchemaType parentType, StaticContext env, boolean whole) throws XPathException {
        thenExp.checkPermittedContents(parentType, env, whole);
        elseExp.checkPermittedContents(parentType, env, whole);
    } 

    /**
    * Evaluate the conditional expression in a given context
    * @param context the evaluation context
    */

    public Item evaluateItem(XPathContext context) throws XPathException {
        if (condition.effectiveBooleanValue(context)) {
            return thenExp.evaluateItem(context);
        } else {
            return elseExp.evaluateItem(context);
        }
    }

    /**
    * Iterate the path-expression in a given context
    * @param context the evaluation context
    */

    public SequenceIterator iterate(XPathContext context) throws XPathException {
        if (condition.effectiveBooleanValue(context)) {
            return thenExp.iterate(context);
        } else {
            return elseExp.iterate(context);
        }
    }

    /**
     * Process this expression as an instruction, writing results to the current
     * receiver
     */

    public void process(XPathContext context) throws XPathException {
        if (condition.effectiveBooleanValue(context)) {
            thenExp.process(context);
        } else {
            elseExp.process(context);
        }
    }


    /**
    * Get data type of items in sequence returned by expression
    */

    public ItemType getItemType() {
        return Type.getCommonSuperType(thenExp.getItemType(), elseExp.getItemType());
    }

    /**
    * Determine the static cardinality of the result
    */

    public int computeCardinality() {
        return Cardinality.union(thenExp.getCardinality(), elseExp.getCardinality());
    }

    /**
    * Get the static properties of this expression (other than its type). The result is
    * bit-signficant. These properties are used for optimizations. In general, if
    * property bit is set, it is true, but if it is unset, the value is unknown.
    */

    public int computeSpecialProperties() {
        // if one branch is empty, return the properties of the other branch
        if (thenExp instanceof EmptySequence) {
            return elseExp.getSpecialProperties();
        }
        if (elseExp instanceof EmptySequence) {
            return thenExp.getSpecialProperties();
        }
        // otherwise return the properties that are shared by both subexpressions
        return thenExp.getSpecialProperties() & elseExp.getSpecialProperties();
    }

    /**
    * Diagnostic print of expression structure
    */

    public void display(int level, NamePool pool, PrintStream out) {
        out.println(ExpressionTool.indent(level) + "if (");
        condition.display(level+1, pool, out);
        out.println(ExpressionTool.indent(level) + "then");
        thenExp.display(level+1, pool, out);
        out.println(ExpressionTool.indent(level) + "else");
        elseExp.display(level+1, pool, out);
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
