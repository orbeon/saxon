package net.sf.saxon.expr;
import net.sf.saxon.om.NamePool;
import net.sf.saxon.value.Value;
import net.sf.saxon.xpath.XPathException;

import java.util.Iterator;

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
        declaration.fixupReferences(this);
        adoptChildExpression(action);
    }

    /**
     * Set the "sequence" expression - the one to which the variable is bound
     */

    public void setSequence(Expression sequence) {
        this.sequence = sequence;
        adoptChildExpression(sequence);
    }

    /**
    * Set the slot number for the range variable
    */

    public void setSlotNumber(int nr) {
        slotNumber = nr;
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
            sequence = sequence.promote(offer);
            if (offer.action == PromotionOffer.INLINE_VARIABLE_REFERENCES ||
                    offer.action == PromotionOffer.UNORDERED) {
                // Don't pass on other requests. We could pass them on, but only after augmenting
                // them to say we are interested in subexpressions that don't depend on either the
                // outer context or the inner context.
                action = action.promote(offer);
            }
            return this;
        }
    }

    /**
    * Get the immediate subexpressions of this expression
    */

//    public Expression[] getSubExpressions() {
//        Expression[] exp = new Expression[2];
//        exp[0] = sequence;
//        exp[1] = action;
//        return exp;
//    }

    public Iterator iterateSubExpressions() {
        return new PairIterator(sequence, action);
    }

    // Following methods implement the VariableDeclaration interface, in relation to the range
    // variable

//    public boolean isGlobal() {
//        return false;
//    }

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
        String slot = (slotNumber == -999 ? "unallocated" : slotNumber+"");
        if (variableName == null) {
            return "zz:var" + hashCode() + " [" + slot + ']';
        } else {
            return variableName + " [" + slot + ']';
        }
    }

    /**
    * Get the value of the range variable
    */

    public Value evaluateVariable(XPathContext context) throws XPathException {
        //return context.getController().getBindery().getLocalVariable(slotNumber);
        return context.evaluateLocalVariable(slotNumber);
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
