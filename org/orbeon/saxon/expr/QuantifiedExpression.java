package org.orbeon.saxon.expr;
import org.orbeon.saxon.Configuration;
import org.orbeon.saxon.om.Item;
import org.orbeon.saxon.om.SequenceIterator;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.type.ItemType;
import org.orbeon.saxon.type.Type;
import org.orbeon.saxon.type.TypeHierarchy;
import org.orbeon.saxon.value.BooleanValue;
import org.orbeon.saxon.value.EmptySequence;
import org.orbeon.saxon.value.SequenceType;

import java.io.PrintStream;

/**
* A QuantifiedExpression tests whether some/all items in a sequence satisfy
* some condition.
*/

class QuantifiedExpression extends Assignation {

    private int operator;       // Token.SOME or Token.EVERY

    public void setOperator(int operator) {
        this.operator = operator;
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

    public Expression typeCheck(StaticContext env, ItemType contextItemType) throws XPathException {

        final TypeHierarchy th = env.getConfiguration().getTypeHierarchy();
        if (declaration==null) {
            // we've already done the type checking, no need to do it again
            return this;
        }

        // The order of events is critical here. First we ensure that the type of the
        // sequence expression is established. This is used to establish the type of the variable,
        // which in turn is required when type-checking the action part.

        sequence = sequence.typeCheck(env, contextItemType);
        if (sequence instanceof EmptySequence) {
            return BooleanValue.get(operator != Token.SOME);
        }

        // "some" and "every" have no ordering constraints

        Optimizer opt = env.getConfiguration().getOptimizer();
        sequence = ExpressionTool.unsorted(opt, sequence, false);

        SequenceType decl = declaration.getRequiredType();
        SequenceType sequenceType = SequenceType.makeSequenceType(decl.getPrimaryType(),
                                             StaticProperty.ALLOWS_ZERO_OR_MORE);
        RoleLocator role = new RoleLocator(RoleLocator.VARIABLE, new Integer(nameCode), 0, env.getNamePool());
        role.setSourceLocator(this);
        sequence = TypeChecker.strictTypeCheck(
                                sequence, sequenceType, role, env);
        ItemType actualItemType = sequence.getItemType(th);
        declaration.refineTypeInformation(actualItemType,
                StaticProperty.EXACTLY_ONE,
                null,
                sequence.getSpecialProperties(), env);

        declaration = null;     // let the garbage collector take it

        action = action.typeCheck(env, contextItemType);
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
        sequence = sequence.optimize(opt, env, contextItemType);
        action = action.optimize(opt, env, contextItemType);
        PromotionOffer offer = new PromotionOffer(opt);
        offer.containingExpression = this;
        offer.action = PromotionOffer.RANGE_INDEPENDENT;
        Binding[] bindingList = {this};
        offer.bindingList = bindingList;
        action = doPromotion(action, offer);
        if (offer.containingExpression instanceof LetExpression) {
            offer.containingExpression =
                    offer.containingExpression.typeCheck(env, contextItemType).optimize(opt, env, contextItemType);
        }
        return offer.containingExpression;

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
        while (true) {
            final Item it = base.next();
            if (it == null) {
                break;
            }
            context.setLocalVariable(slotNumber, it);
            if (some == action.effectiveBooleanValue(context)) {
                return some;
            }
        }
        return !some;
    }


    /**
    * Determine the data type of the items returned by the expression
    * @return Type.BOOLEAN
     * @param th
     */

	public ItemType getItemType(TypeHierarchy th) {
	    return Type.BOOLEAN_TYPE;
	}

    /**
    * Diagnostic print of expression structure
    */

    public void display(int level, PrintStream out, Configuration config) {
        out.println(ExpressionTool.indent(level) + Token.tokens[operator] +
                " $" + getVariableName(config.getNamePool()) + " in");
        sequence.display(level+1, out, config);
        out.println(ExpressionTool.indent(level) + "satisfies");
        action.display(level+1, out, config);
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
