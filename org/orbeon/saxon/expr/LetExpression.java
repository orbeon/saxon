package net.sf.saxon.expr;
import net.sf.saxon.om.Item;
import net.sf.saxon.om.NamePool;
import net.sf.saxon.om.SequenceIterator;
import net.sf.saxon.om.ValueRepresentation;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.ItemType;
import net.sf.saxon.type.SchemaType;
import net.sf.saxon.value.Value;

import java.io.PrintStream;

/**
* A LetExpression is modelled on the XQuery syntax let $x := expr return expr. This syntax
* is not available in the surface XPath language, but it is used internally in an optimized
* expression tree.
*/

public class LetExpression extends Assignation {

    // This boolean is set if static analysis reveals that the value of the variable will
    // be referenced more than once.
    boolean keepValue;

    public LetExpression() {}

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

        RoleLocator role = new RoleLocator(RoleLocator.VARIABLE, new Integer(nameCode), 0, env.getNamePool());
        sequence = TypeChecker.strictTypeCheck(
                        sequence, declaration.getRequiredType(), role, env);
        ItemType actualItemType = sequence.getItemType();
        declaration.refineTypeInformation(actualItemType,
                        sequence.getCardinality(),
                        (sequence instanceof Value ? (Value)sequence : null),
                        sequence.getSpecialProperties());

        action = action.analyze(env, contextItemType);

        int count = declaration.getReferenceCount(this);
        if (count == 0) {
            // variable is not used - no need to evaluate it
            return action;
        } else if (count == 1) {
            keepValue = false;
        } else {
            keepValue = true;
        }

        declaration = null;     // let the garbage collector take it

        // Try to promote any WHERE clause appearing within the LET expression

        Expression p = promoteWhereClause(null);
        if (p != null) {
            return p;
        }

        return this;
    }

    /**
     * Check that any elements and attributes constructed or returned by this expression are acceptable
     * in the content model of a given complex type. It's always OK to say yes, since the check will be
     * repeated at run-time. The process of checking element and attribute constructors against the content
     * model of a complex type also registers the type of content expected of those constructors, so the
     * static validation can continue recursively.
     */

    public void checkPermittedContents(SchemaType parentType, StaticContext env, boolean whole) throws XPathException {
        action.checkPermittedContents(parentType, env, whole);
    }        

    /**
    * Iterate over the sequence of values
    */

    public SequenceIterator iterate(XPathContext context) throws XPathException {
        ValueRepresentation val = eval(context);
        context.setLocalVariable(slotNumber, val);
        return action.iterate(context);
    }

    /**
     * Evaluate the variable. (This is overridden in a subclass).
     */

    protected ValueRepresentation eval(XPathContext context) throws XPathException {
        return ExpressionTool.lazyEvaluate(sequence, context, keepValue);
    }

    /**
    * Evaluate the expression as a singleton
    */

    public Item evaluateItem(XPathContext context) throws XPathException {
        ValueRepresentation val = eval(context);
        context.setLocalVariable(slotNumber, val);
        return action.evaluateItem(context);
    }

    /**
     * Process this expression as an instruction, writing results to the current
     * outputter
     */

    public void process(XPathContext context) throws XPathException {
        ValueRepresentation val = eval(context);
        context.setLocalVariable(slotNumber, val);
        action.process(context);
    }


    /**
    * Determine the data type of the items returned by the expression, if possible
    * @return one of the values Type.STRING, Type.BOOLEAN, Type.NUMBER, Type.NODE,
    * or Type.ITEM (meaning not known in advance)
    */

	public ItemType getItemType() {
	    return action.getItemType();
	}

    /**
	* Determine the static cardinality of the expression
	*/

	public int computeCardinality() {
        return action.getCardinality();
	}

    /**
    * Get the static properties of this expression (other than its type). The result is
    * bit-signficant. These properties are used for optimizations. In general, if
    * property bit is set, it is true, but if it is unset, the value is unknown.
    */

    public int computeSpecialProperties() {
        return action.getSpecialProperties();
    }

    /**
     * Mark tail function calls
     */

    public boolean markTailFunctionCalls() {
        return ExpressionTool.markTailFunctionCalls(action);
    }

    /**
    * Promote this expression if possible
    */

    public Expression promote(PromotionOffer offer) throws XPathException {
        Expression exp = offer.accept(this);
        if (exp != null) {
            return exp;
        } else {
            // pass the offer on to the sequence expression
            sequence = sequence.promote(offer);
            if (offer.action == PromotionOffer.INLINE_VARIABLE_REFERENCES ||
                    offer.action == PromotionOffer.UNORDERED) {
                action = action.promote(offer);
            } else if (offer.action == PromotionOffer.RANGE_INDEPENDENT
//                  ||  offer.action == PromotionOffer.WHERE_CLAUSE
            ) {
                // Pass the offer to the action expression only if the action isn't depending on the
                // variable bound by this let expression
                Binding[] savedBindingList = offer.bindingList;
                Binding[] newBindingList = new Binding[offer.bindingList.length+1];
                System.arraycopy(offer.bindingList, 0, newBindingList, 0, offer.bindingList.length);
                newBindingList[offer.bindingList.length] = this;
                offer.bindingList = newBindingList;
                action = action.promote(offer);
                offer.bindingList = savedBindingList;
            }
            // if this results in the expression (let $x := $y return Z), replace all references to
            // to $x by references to $y in the Z part, and eliminate this LetExpression by
            // returning the action part.
            if (sequence instanceof VariableReference) {
                //System.err.println("Before INLINING:"); display(10);
                PromotionOffer offer2 = new PromotionOffer();
                offer2.action = PromotionOffer.INLINE_VARIABLE_REFERENCES;
                Binding[] bindingList = {this};
                offer2.bindingList = bindingList;
                offer2.containingExpression = sequence;
                action = action.promote(offer2);
                //System.err.println("After INLINING:"); action.display(10);
                return action;
            }
            return this;
        }
    }

    /**
    * Diagnostic print of expression structure
    */

    public void display(int level, NamePool pool, PrintStream out) {
        out.println(ExpressionTool.indent(level) + "let $" + getVariableName(pool) + " :=");
        sequence.display(level+1, pool, out);
        out.println(ExpressionTool.indent(level) + "return");
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
