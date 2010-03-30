package org.orbeon.saxon.instruct;
import org.orbeon.saxon.expr.*;
import org.orbeon.saxon.om.StandardNames;
import org.orbeon.saxon.trace.ExpressionPresenter;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.type.ItemType;
import org.orbeon.saxon.type.TypeHierarchy;

import java.util.Iterator;


/**
* Handler for saxon:while elements in stylesheet. <br>
* The saxon:while element has a mandatory attribute test, a boolean expression.
* The content is output repeatedly so long as the test condition is true.
*/

public class While extends Instruction {

    private Expression test;
    private Expression action;

    public While(Expression test, Expression action) {
        this.test = test;
        this.action = action;
        adoptChildExpression(test);
        adoptChildExpression(action);
    }

    /**
    * Get the name of this instruction for diagnostic and tracing purposes
    * @return the string "saxon:while"
    */

    public int getInstructionNameCode() {
        return StandardNames.SAXON_WHILE;
    }

    /**
     * Get the action expression (the content of the for-each)
     */

    public Expression getActionExpression() {
        return action;
    }

    /**
     * Simplify an expression. This performs any static optimization (by rewriting the expression
     * as a different expression).
     *
     * @exception XPathException if an error is discovered during expression
     *     rewriting
     * @return the simplified expression
     * @param visitor an expression visitor
     */

    public Expression simplify(ExpressionVisitor visitor) throws XPathException {
        test = visitor.simplify(test);
        action = visitor.simplify(action);
        return this;
    }

    public Expression typeCheck(ExpressionVisitor visitor, ItemType contextItemType) throws XPathException {
        test = visitor.typeCheck(test, contextItemType);
        adoptChildExpression(test);
        action = visitor.typeCheck(action, contextItemType);
        adoptChildExpression(action);
        return this;
    }

    public Expression optimize(ExpressionVisitor visitor, ItemType contextItemType) throws XPathException {
        test = visitor.optimize(test, contextItemType);
        adoptChildExpression(test);
        action = visitor.optimize(action, contextItemType);
        adoptChildExpression(action);
        return this;
    }


    /**
     * Copy an expression. This makes a deep copy.
     *
     * @return the copy of the original expression
     */

    public Expression copy() {
        return new While(test.copy(), action.copy());
    }

    /**
     * Get the item type of the items returned by evaluating this instruction
     *
     * @return the static item type of the instruction
     * @param th the type hierarchy cache
     */

    public ItemType getItemType(TypeHierarchy th) {
        return action.getItemType(th);
    }

    /**
     * Handle promotion offers, that is, non-local tree rewrites.
     * @param offer The type of rewrite being offered
     * @throws XPathException
     */

    protected void promoteInst(PromotionOffer offer) throws XPathException {
        if (offer.action != PromotionOffer.EXTRACT_GLOBAL_VARIABLES) {
            test = doPromotion(test, offer);
        }
        action = doPromotion(action, offer);
    }

    /**
     * Determine whether this instruction creates new nodes.
     * This implementation returns true if the "action" creates new nodes.
     * (Nodes created by the condition can't contribute to the result).
     */

    public final boolean createsNewNodes() {
        int props = action.getSpecialProperties();
        return ((props & StaticProperty.NON_CREATIVE) == 0);
    }

    /**
     * Get all the XPath expressions associated with this instruction
     * (in XSLT terms, the expression present on attributes of the instruction,
     * as distinct from the child instructions in a sequence construction)
     */

    public Iterator iterateSubExpressions() {
        return new PairIterator(test, action);
    }

    /**
     * Given an expression that is an immediate child of this expression, test whether
     * the evaluation of the parent expression causes the child expression to be
     * evaluated repeatedly
     * @param child the immediate subexpression
     * @return true if the child expression is evaluated repeatedly
     */

    public boolean hasLoopingSubexpression(Expression child) {
        return child == action;
    }

    /**
      * Replace one subexpression by a replacement subexpression
      * @param original the original subexpression
      * @param replacement the replacement subexpression
      * @return true if the original subexpression is found
      */

     public boolean replaceSubExpression(Expression original, Expression replacement) {
         boolean found = false;
         if (test == original) {
             test = replacement;
             found = true;
         }
         if (action == original) {
             action = replacement;
             found = true;
         }
                 return found;
     }


    public TailCall processLeavingTail(XPathContext context) throws XPathException {
        while (test.effectiveBooleanValue(context)) {
            action.process(context);
        }
        return null;
    }


    /**
     * Diagnostic print of expression structure. The abstract expression tree
     * is written to the supplied output destination.
     */

    public void explain(ExpressionPresenter out) {
        out.startElement("saxonWhile");
        test.explain(out);
        out.startSubsidiaryElement("do");
        action.explain(out);
        out.endSubsidiaryElement();
        out.endElement();
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
