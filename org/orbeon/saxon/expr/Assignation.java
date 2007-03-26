package org.orbeon.saxon.expr;
import org.orbeon.saxon.om.NamePool;
import org.orbeon.saxon.om.ValueRepresentation;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.value.EmptySequence;
import org.orbeon.saxon.value.MemoClosure;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
* Assignation is an abstract superclass for the kinds of expression
* that declare range variables: for, some, and every.
*/

public abstract class Assignation extends ComputedExpression implements Binding {

    protected int slotNumber = -999;     // slot number for range variable
                                         // (initialized to ensure a crash if no real slot is allocated)
    protected Expression sequence;       // the expression over which the variable ranges
    protected Expression action;         // the action performed for each value of the variable
    protected String variableName;
    protected int nameCode;

    protected transient RangeVariableDeclaration declaration;

    /**
     * Set the reference to the variable declaration
     */

    public void setVariableDeclaration (RangeVariableDeclaration decl) {
        declaration = decl;
        nameCode = decl.getNameCode();
        variableName = decl.getVariableName();
    }

    /**
     * Get the variable declaration
     */

    public RangeVariableDeclaration getVariableDeclaration() {
        return declaration;
    }

    /**
     * Add the "return" or "satisfies" expression, and fix up all references to the
     * range variable that occur within that expression
     * @param action the expression that occurs after the "return" keyword of a "for"
     * expression, the "satisfies" keyword of "some/every", or the ":=" operator of
     * a "let" expression.
     *
     * <p>This method must be called <b>after</b> calling setVariableDeclaration()</p>
     */

    public void setAction(Expression action) {
        this.action = action;
        if (declaration != null) {
            declaration.fixupReferences(this);
        }
        adoptChildExpression(action);
    }

    /**
     * Indicate whether the binding is local or global. A global binding is one that has a fixed
     * value for the life of a query or transformation; any other binding is local.
     */

    public final boolean isGlobal() {
        return false;
    }

    /**
    * Test whether it is permitted to assign to the variable using the saxon:assign
    * extension element. This will only be for an XSLT global variable where the extra
    * attribute saxon:assignable="yes" is present.
    */

    public final boolean isAssignable() {
        return false;
    }

    /**
     * If this is a local variable held on the local stack frame, return the corresponding slot number.
     * In other cases, return -1.
     */

    public int getLocalSlotNumber() {
        return slotNumber;
    }

    /**
     * Get the action expression
     */

    public Expression getAction() {
        return action;
    }

    /**
     * Set the "sequence" expression - the one to which the variable is bound
     */

    public void setSequence(Expression sequence) {
        this.sequence = sequence;
        adoptChildExpression(sequence);
    }

    /**
     * Get the "sequence" expression - the one to which the variable is bound
     */

    public Expression getSequence() {
        return sequence;
    }

    /**
    * Set the slot number for the range variable
    */

    public void setSlotNumber(int nr) {
        slotNumber = nr;
    }

    /**
     * Get the number of slots required. Normally 1, except for a FOR expression with an AT clause, where it is 2.
     */

    public int getRequiredSlots() {
        return 1;
    }

    /**
    * Simplify the expression
    */

     public Expression simplify(StaticContext env) throws XPathException {
        sequence = sequence.simplify(env);
        action = action.simplify(env);
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
            sequence = doPromotion(sequence, offer);
            if (offer.action == PromotionOffer.INLINE_VARIABLE_REFERENCES ||
                    offer.action == PromotionOffer.UNORDERED ||
                    offer.action == PromotionOffer.REPLACE_CURRENT) {
                action = doPromotion(action, offer);
            } else if (offer.action == PromotionOffer.RANGE_INDEPENDENT) {
                // Pass the offer to the action expression only if the action isn't dependent on the
                // variable bound by this assignation
                Binding[] savedBindingList = offer.bindingList;
                Binding[] newBindingList = extendBindingList(offer.bindingList);
                offer.bindingList = newBindingList;
                action = doPromotion(action, offer);
                offer.bindingList = savedBindingList;
            }
            return this;
        }
    }

    /**
     * Suppress validation on contained element constructors, on the grounds that the parent element
     * is already performing validation. The default implementation does nothing.
     */

    public void suppressValidation(int validationMode) {
        if (action instanceof ComputedExpression) {
            ((ComputedExpression)action).suppressValidation(validationMode);
        }
    }

    /**
     * Extend an array of variable bindings to include the binding(s) defined in this expression
     */

    protected Binding[] extendBindingList(Binding[] in) {
        Binding[] newBindingList = new Binding[in.length+1];
        System.arraycopy(in, 0, newBindingList, 0, in.length);
        newBindingList[in.length] = this;
        return newBindingList;
    }

    /**
     * Promote a WHERE clause whose condition doesn't depend on the variable being bound.
     * This rewrites an expression of the form
     *
     * <p>let $i := SEQ return if (C) then R else ()</p>
     *
     * <p>to the form:</p>
     *
     * <p>if (C) then (let $i := SEQ return R) else ()
     */

    protected Expression promoteWhereClause(Binding positionBinding) {
        if (action instanceof IfExpression) {
            Container container = getParentExpression();
            IfExpression ifex = (IfExpression)action;
            Expression condition = ifex.getCondition();
            Expression elseex = ifex.getElseExpression();
            if (elseex instanceof EmptySequence) {
                Binding[] bindingList;
                if (positionBinding == null) {
                    Binding[] bl = {this};
                    bindingList = bl;
                } else {
                    Binding[] bl = {this, positionBinding};
                    bindingList = bl;
                }
                List list = new ArrayList(5);
                Expression promotedCondition = null;
                BooleanExpression.listAndComponents(condition, list);
                for (int i=list.size()-1; i>=0; i--) {
                    Expression term = (Expression)list.get(i);
                    if (!ExpressionTool.dependsOnVariable(term, bindingList)) {
                        if (promotedCondition == null) {
                            promotedCondition = term;
                        } else {
                            promotedCondition = new BooleanExpression(term, Token.AND, promotedCondition);
                        }
                        list.remove(i);
                    }
                }
                if (promotedCondition != null) {
                    if (list.size() == 0) {
                        // the whole if() condition has been promoted
                        Expression oldThen = ifex.getThenExpression();
                        setAction(oldThen);
                        ifex.setParentExpression(container);
                        ifex.setThenExpression(this);
                        return ifex;
                    } else {
                        // one or more terms of the if() condition have been promoted
                        Expression retainedCondition = (Expression)list.get(0);
                        for (int i=1; i<list.size(); i++) {
                            retainedCondition = new BooleanExpression(
                                    retainedCondition, Token.AND, (Expression)list.get(i));
                        }
                        ifex.setCondition(retainedCondition);
                        IfExpression newIf = new IfExpression(
                                promotedCondition, this, EmptySequence.getInstance());
                        newIf.setParentExpression(container);
                        return newIf;
                    }
                }

            }
        }
        return null;
    }

    /**
    * Get the immediate subexpressions of this expression
    */

    public Iterator iterateSubExpressions() {
        return new PairIterator(sequence, action);
    }

    /**
     * Replace one subexpression by a replacement subexpression
     * @param original the original subexpression
     * @param replacement the replacement subexpression
     * @return true if the original subexpression is found
     */

    public boolean replaceSubExpression(Expression original, Expression replacement) {
        boolean found = false;
        if (sequence == original) {
            sequence = replacement;
            found = true;
        }
        if (action == original) {
            action = replacement;
            found = true;
        }
        return found;
    }


    // Following methods implement the VariableDeclaration interface, in relation to the range
    // variable

    public int getVariableNameCode() {
        return nameCode;
    }

    public int getVariableFingerprint() {
        return nameCode & 0xfffff;
    }

    /**
    * Get the display name of the range variable, for diagnostics only
    */

    public String getVariableName(NamePool pool) {
        if (variableName == null) {
            return "zz:var" + hashCode();
        } else {
            return variableName;
        }
    }

    /**
    * Get the value of the range variable
    */

    public ValueRepresentation evaluateVariable(XPathContext context) throws XPathException {
//        if (slotNumber == -999) {
//            display(10, context.getController().getNamePool(), System.err);
//        }
        ValueRepresentation actual = context.evaluateLocalVariable(slotNumber);
        if (actual instanceof MemoClosure && ((MemoClosure)actual).isFullyRead()) {
            actual = ((MemoClosure)actual).materialize();
            context.setLocalVariable(slotNumber, actual);
        }
        return actual;
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
