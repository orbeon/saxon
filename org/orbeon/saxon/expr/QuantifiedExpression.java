package net.sf.saxon.expr;
import net.sf.saxon.om.Item;
import net.sf.saxon.om.NamePool;
import net.sf.saxon.om.SequenceIterator;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.ItemType;
import net.sf.saxon.type.Type;
import net.sf.saxon.value.BooleanValue;
import net.sf.saxon.value.EmptySequence;
import net.sf.saxon.value.SequenceType;

import java.io.PrintStream;

/**
* A QuantifiedExpression tests whether some/all items in a sequence satisfy
* some condition.
*/

class QuantifiedExpression extends Assignation {

    private int operator;       // Tokenizer.SOME or Tokenizer.EVERY

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

    public Expression analyze(StaticContext env, ItemType contextItemType) throws XPathException {

        if (declaration==null) {
            // we've already done the type checking, no need to do it again
            return this;
        }

        // The order of events is critical here. First we ensure that the type of the
        // sequence expression is established. This is used to establish the type of the variable,
        // which in turn is required when type-checking the action part.

        sequence = sequence.analyze(env, contextItemType);
        if (sequence instanceof EmptySequence) {
            return BooleanValue.get(operator != Token.SOME);
        }

        // "some" and "every" have no ordering constraints

        sequence = ExpressionTool.unsorted(sequence, false);

        SequenceType decl = declaration.getRequiredType();
        SequenceType sequenceType = SequenceType.makeSequenceType(decl.getPrimaryType(),
                                             StaticProperty.ALLOWS_ZERO_OR_MORE);
        RoleLocator role = new RoleLocator(RoleLocator.VARIABLE, new Integer(nameCode), 0, env.getNamePool());
        sequence = TypeChecker.strictTypeCheck(
                                sequence, sequenceType, role, env);
        ItemType actualItemType = sequence.getItemType();
        declaration.refineTypeInformation(actualItemType,
                StaticProperty.EXACTLY_ONE,
                null,
                sequence.getSpecialProperties());

        declaration = null;     // let the garbage collector take it

        action = action.analyze(env, contextItemType);

        PromotionOffer offer = new PromotionOffer();
        offer.containingExpression = this;
        offer.action = PromotionOffer.RANGE_INDEPENDENT;
        Binding[] bindingList = {this};
        offer.bindingList = bindingList;
        action = action.promote(offer);
        if (offer.containingExpression instanceof LetExpression) {
            offer.containingExpression = offer.containingExpression.analyze(env, contextItemType);
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

        boolean some = (operator==Token.SOME);
        while (true) {
            Item it = base.next();
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
    */

	public ItemType getItemType() {
	    return Type.BOOLEAN_TYPE;
	}

    /**
    * Diagnostic print of expression structure
    */

    public void display(int level, NamePool pool, PrintStream out) {
        out.println(ExpressionTool.indent(level) + Token.tokens[operator] + " $" + getVariableName(pool) + " in");
        sequence.display(level+1, pool, out);
        out.println(ExpressionTool.indent(level) + "satisfies");
        action.display(level+1, pool, out);
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
