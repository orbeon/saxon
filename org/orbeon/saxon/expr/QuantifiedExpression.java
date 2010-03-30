package org.orbeon.saxon.expr;
import org.orbeon.saxon.trace.ExpressionPresenter;
import org.orbeon.saxon.om.Item;
import org.orbeon.saxon.om.SequenceIterator;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.type.BuiltInAtomicType;
import org.orbeon.saxon.type.ItemType;
import org.orbeon.saxon.type.TypeHierarchy;
import org.orbeon.saxon.value.BooleanValue;
import org.orbeon.saxon.value.SequenceType;
import org.orbeon.saxon.functions.BooleanFn;

/**
* A QuantifiedExpression tests whether some/all items in a sequence satisfy
* some condition.
*/

public class QuantifiedExpression extends Assignation {

    private int operator;       // Token.SOME or Token.EVERY

    /**
     * Set the operator, either {@link Token#SOME} or {@link Token#EVERY}
     * @param operator the operator
     */

    public void setOperator(int operator) {
        this.operator = operator;
    }

    /**
     * Get the operator, either {@link Token#SOME} or {@link Token#EVERY}
     * @return the operator
     */

    public int getOperator() {
        return operator;
    }

    /**
    * Determine the static cardinality
    */

    public int computeCardinality() {
        return StaticProperty.EXACTLY_ONE;
    }

    /**
    * Type-check the expression
    */

    public Expression typeCheck(ExpressionVisitor visitor, ItemType contextItemType) throws XPathException {

        final TypeHierarchy th = visitor.getConfiguration().getTypeHierarchy();

        // The order of events is critical here. First we ensure that the type of the
        // sequence expression is established. This is used to establish the type of the variable,
        // which in turn is required when type-checking the action part.

        sequence = visitor.typeCheck(sequence, contextItemType);
        if (Literal.isEmptySequence(sequence)) {
            return Literal.makeLiteral(BooleanValue.get(operator != Token.SOME));
        }

        // "some" and "every" have no ordering constraints

        Optimizer opt = visitor.getConfiguration().getOptimizer();
        sequence = ExpressionTool.unsorted(opt, sequence, false);

        SequenceType decl = getRequiredType();
        SequenceType sequenceType = SequenceType.makeSequenceType(decl.getPrimaryType(),
                                             StaticProperty.ALLOWS_ZERO_OR_MORE);
        RoleLocator role = new RoleLocator(RoleLocator.VARIABLE, getVariableQName(), 0);
        //role.setSourceLocator(this);
        sequence = TypeChecker.strictTypeCheck(
                                sequence, sequenceType, role, visitor.getStaticContext());
        ItemType actualItemType = sequence.getItemType(th);
        refineTypeInformation(actualItemType,
                StaticProperty.EXACTLY_ONE,
                null,
                sequence.getSpecialProperties(), visitor, this);

        //declaration = null;     // let the garbage collector take it

        action = visitor.typeCheck(action, contextItemType);
        XPathException err = TypeChecker.ebvError(action, visitor.getConfiguration().getTypeHierarchy());
        if (err != null) {
            err.setLocator(this);
            throw err;
        }
        return this;
    }

    /**
     * Perform optimisation of an expression and its subexpressions.
     * <p/>
     * <p>This method is called after all references to functions and variables have been resolved
     * to the declaration of the function or variable, and after all type checking has been done.</p>
     *
     * @param visitor an expression visitor
     * @param contextItemType the static type of "." at the point where this expression is invoked.
     *                        The parameter is set to null if it is known statically that the context item will be undefined.
     *                        If the type of the context item is not known statically, the argument is set to
     *                        {@link org.orbeon.saxon.type.Type#ITEM_TYPE}
     * @return the original expression, rewritten if appropriate to optimize execution
     * @throws XPathException if an error is discovered during this phase
     *                                        (typically a type error)
     */

    public Expression optimize(ExpressionVisitor visitor, ItemType contextItemType) throws XPathException {

        Optimizer opt = visitor.getConfiguration().getOptimizer();

        sequence = visitor.optimize(sequence, contextItemType);
        action = visitor.optimize(action, contextItemType);
        Expression ebv = BooleanFn.rewriteEffectiveBooleanValue(action, visitor, contextItemType);
        if (ebv != null) {
            action = ebv;
            adoptChildExpression(ebv);
        }
        PromotionOffer offer = new PromotionOffer(opt);
        offer.containingExpression = this;
        offer.action = PromotionOffer.RANGE_INDEPENDENT;
        offer.bindingList = new Binding[] {this};
        action = doPromotion(action, offer);
        if (offer.containingExpression instanceof LetExpression) {
            offer.containingExpression =
                    visitor.optimize(visitor.typeCheck(offer.containingExpression, contextItemType), contextItemType);
        }
        return offer.containingExpression;

    }

    /**
     * Check to ensure that this expression does not contain any updating subexpressions.
     * This check is overridden for those expressions that permit updating subexpressions.
     *
     * @throws org.orbeon.saxon.trans.XPathException
     *          if the expression has a non-permitted updateing subexpression
     */

    public void checkForUpdatingSubexpressions() throws XPathException {
        sequence.checkForUpdatingSubexpressions();
        action.checkForUpdatingSubexpressions();
    }

    /**
     * Determine whether this is an updating expression as defined in the XQuery update specification
     * @return true if this is an updating expression
     */

    public boolean isUpdatingExpression() {
        return false;
    }

    /**
     * Copy an expression. This makes a deep copy.
     * @return the copy of the original expression
     */

    public Expression copy() {
        QuantifiedExpression qe = new QuantifiedExpression();
        qe.setOperator(operator);
        qe.setVariableQName(variableName);
        qe.setRequiredType(requiredType);
        qe.setSequence(sequence.copy());
        Expression newAction = action.copy();
        qe.setAction(newAction);
        qe.variableName = variableName;
        ExpressionTool.rebindVariableReferences(newAction, this, qe);
        return qe;
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
     * Determine the special properties of this expression
     * @return {@link StaticProperty#NON_CREATIVE}.
     */

    public int computeSpecialProperties() {
        int p = super.computeSpecialProperties();
        return p | StaticProperty.NON_CREATIVE;
    }

    /**
    * Evaluate the expression to return a singleton value
    */

    public Item evaluateItem(XPathContext context) throws XPathException {
        return BooleanValue.get(effectiveBooleanValue(context));
    }

    /**
    * Get the result as a boolean
    */

    public boolean effectiveBooleanValue(XPathContext context) throws XPathException {

        // First create an iteration of the base sequence.

        SequenceIterator base = sequence.iterate(context);

        // Now test to see if some or all of the tests are true. The same
        // logic is used for the SOME and EVERY operators

        final boolean some = (operator==Token.SOME);
        int slot = getLocalSlotNumber();
        while (true) {
            final Item it = base.next();
            if (it == null) {
                break;
            }
            context.setLocalVariable(slot, it);
            if (some == action.effectiveBooleanValue(context)) {
                base.close();
                return some;
            }
        }
        return !some;
    }


    /**
    * Determine the data type of the items returned by the expression
    * @return Type.BOOLEAN
     * @param th the type hierarchy cache
     */

	public ItemType getItemType(TypeHierarchy th) {
	    return BuiltInAtomicType.BOOLEAN;
	}

    /**
     * Diagnostic print of expression structure. The abstract expression tree
     * is written to the supplied output destination.
     */

    public void explain(ExpressionPresenter out) {
        out.startElement(Token.tokens[operator]);
        out.emitAttribute("variable", getVariableName());
        out.startSubsidiaryElement("in");
        sequence.explain(out);
        out.endSubsidiaryElement();
        out.startSubsidiaryElement("satisfies");
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
// The Initial Developer of the Original Code is Michael H. Kay
//
// Portions created by (your name) are Copyright (C) (your legal entity). All Rights Reserved.
//
// Contributor(s): none.
//
