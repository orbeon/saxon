package org.orbeon.saxon.expr;
import org.orbeon.saxon.Configuration;
import org.orbeon.saxon.pattern.AnyNodeTest;
import org.orbeon.saxon.instruct.TailCall;
import org.orbeon.saxon.instruct.TailCallReturner;
import org.orbeon.saxon.om.Item;
import org.orbeon.saxon.om.SequenceIterator;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.type.ItemType;
import org.orbeon.saxon.type.SchemaType;
import org.orbeon.saxon.type.Type;
import org.orbeon.saxon.type.TypeHierarchy;
import org.orbeon.saxon.value.BooleanValue;
import org.orbeon.saxon.value.Cardinality;
import org.orbeon.saxon.value.EmptySequence;
import org.orbeon.saxon.value.Value;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Iterator;


/**
* An IfExpression returns the value of either the "then" part or the "else" part,
* depending on the value of the condition
*/

public class IfExpression extends ComputedExpression implements TailCallReturner {

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
            final boolean b;
            try {
                b = condition.effectiveBooleanValue(env.makeEarlyEvaluationContext());
            } catch (XPathException err) {
                err.setLocator(this);
                throw err;
            }
            return (b ? thenExp.simplify(env) : elseExp.simplify(env));
        }
        thenExp = thenExp.simplify(env);
        elseExp = elseExp.simplify(env);
        return this;
    }

    /**
    * Type-check the expression
    */

    public Expression typeCheck(StaticContext env, ItemType contextItemType) throws XPathException {
        condition = condition.typeCheck(env, contextItemType);

        // If the type of the expression is known to be a sequence of one
        // or more nodes, we can treat it as a constant true

        TypeHierarchy th = env.getConfiguration().getTypeHierarchy();
        if (th.isSubType(condition.getItemType(th), AnyNodeTest.getInstance())
                && !Cardinality.allowsZero(condition.getCardinality())) {
            condition = BooleanValue.TRUE;
        }

        // If the condition after typechecking is reduced to a constant,
        // cut it down to the appropriate branch. This is especially important
        // when handling typeswitch, as otherwise the unused branches will
        // generate a type error.
        
        if (condition instanceof BooleanValue) {
            if (((BooleanValue)condition).getBooleanValue()) {
                ComputedExpression.setParentExpression(thenExp, getParentExpression());
                return thenExp.typeCheck(env, contextItemType);
            } else {
                ComputedExpression.setParentExpression(elseExp, getParentExpression());
                return elseExp.typeCheck(env, contextItemType);
            }
        } else {
            XPathException err = TypeChecker.ebvError(condition, env.getConfiguration().getTypeHierarchy());
            if (err != null) {
                err.setLocator(this);
                throw err;
            }
            thenExp = thenExp.typeCheck(env, contextItemType);
            elseExp = elseExp.typeCheck(env, contextItemType);
//            adoptChildExpression(condition);
//            adoptChildExpression(thenExp);
//            adoptChildExpression(elseExp);
            return this;
        }
    }

    public Expression optimize(Optimizer opt, StaticContext env, ItemType contextItemType) throws XPathException {
        condition = condition.optimize(opt, env, contextItemType);
        thenExp = thenExp.optimize(opt, env, contextItemType);
        elseExp = elseExp.optimize(opt, env, contextItemType);
        return this;
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
            condition = doPromotion(condition, offer);

            // allow some types of promotion to trickle down to the subexpressions
            if (offer.action == PromotionOffer.UNORDERED ||
                    offer.action == PromotionOffer.INLINE_VARIABLE_REFERENCES ||
                    //offer.action == PromotionOffer.RANGE_INDEPENDENT ||
                    offer.action == PromotionOffer.REPLACE_CURRENT) {
                thenExp = doPromotion(thenExp, offer);
                elseExp = doPromotion(elseExp, offer);
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
     * Replace one subexpression by a replacement subexpression
     * @param original the original subexpression
     * @param replacement the replacement subexpression
     * @return true if the original subexpression is found
     */

    public boolean replaceSubExpression(Expression original, Expression replacement) {
        boolean found = false;
        if (condition == original) {
            condition = replacement;
            found = true;
        }
        if (thenExp == original) {
            thenExp = replacement;
            found = true;
        }
        if (elseExp == original) {
            elseExp = replacement;
            found = true;
        }
                return found;
    }

    /**
     * Suppress validation on contained element constructors, on the grounds that the parent element
     * is already performing validation. The default implementation does nothing.
     */

    public void suppressValidation(int validationMode) {
        if (thenExp instanceof ComputedExpression) {
            ((ComputedExpression)thenExp).suppressValidation(validationMode);
        }
        if (elseExp instanceof ComputedExpression) {
            ((ComputedExpression)elseExp).suppressValidation(validationMode);
        }
    }

    /**
    * Mark tail calls on used-defined functions. For most expressions, this does nothing.
    */

    public boolean markTailFunctionCalls(int nameCode, int arity) {
        boolean a = ExpressionTool.markTailFunctionCalls(thenExp, nameCode, arity);
        boolean b = ExpressionTool.markTailFunctionCalls(elseExp, nameCode, arity);
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
     * ProcessLeavingTail: called to do the real work of this instruction. This method
     * must be implemented in each subclass. The results of the instruction are written
     * to the current Receiver, which can be obtained via the Controller.
     *
     * @param context The dynamic context of the transformation, giving access to the current node,
     *                the current variables, etc.
     * @return null if the instruction has completed execution; or a TailCall indicating
     *         a function call or template call that is delegated to the caller, to be made after the stack has
     *         been unwound so as to save stack space.
     */

    public TailCall processLeavingTail(XPathContext context) throws XPathException {
        if (condition.effectiveBooleanValue(context)) {
            if (thenExp instanceof TailCallReturner) {
                return ((TailCallReturner)thenExp).processLeavingTail(context);
            } else {
                thenExp.process(context);
                return null;
            }
        } else {
            if (elseExp instanceof TailCallReturner) {
                return ((TailCallReturner)elseExp).processLeavingTail(context);
            } else {
                elseExp.process(context);
                return null;
            }
        }
    }


    /**
    * Get data type of items in sequence returned by expression
     * @param th
     */

    public ItemType getItemType(TypeHierarchy th) {
        return Type.getCommonSuperType(thenExp.getItemType(th), elseExp.getItemType(th), th);
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

    public void display(int level, PrintStream out, Configuration config) {
        out.println(ExpressionTool.indent(level) + "if (");
        condition.display(level+1, out, config);
        out.println(ExpressionTool.indent(level) + "then");
        thenExp.display(level+1, out, config);
        out.println(ExpressionTool.indent(level) + "else");
        elseExp.display(level+1, out, config);
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
