package org.orbeon.saxon.expr;
import org.orbeon.saxon.Configuration;
import org.orbeon.saxon.instruct.DocumentInstr;
import org.orbeon.saxon.instruct.TailCall;
import org.orbeon.saxon.instruct.TailCallReturner;
import org.orbeon.saxon.om.Item;
import org.orbeon.saxon.om.SequenceIterator;
import org.orbeon.saxon.om.ValueRepresentation;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.type.ItemType;
import org.orbeon.saxon.type.SchemaType;
import org.orbeon.saxon.type.TypeHierarchy;
import org.orbeon.saxon.value.Cardinality;
import org.orbeon.saxon.value.Value;

import java.io.PrintStream;

/**
* A LetExpression is modelled on the XQuery syntax let $x := expr return expr. This syntax
* is not available in the surface XPath language, but it is used internally in an optimized
* expression tree.
*/

public class LetExpression extends Assignation implements TailCallReturner {

    // This integer holds an approximation to the number of times that the declared variable is referenced.
    // The value 1 means there is only one reference and it is not in a loop, which means that the value will
    // not be retained in memory. If there are multiple references or references within a loop, the value will
    // be a small integer > 1. The special value FILTERED indicates that there is a reference within a loop
    // in the form $x[predicate], which indicates that the value should potentially be indexable.
    int refCount;
    int evaluationMode = ExpressionTool.UNDECIDED;

    public LetExpression() {}

    public void setIndexedVariable() {
        refCount = RangeVariableDeclaration.FILTERED;
    }

    /**
    * Type-check the expression
    */

    public Expression typeCheck(StaticContext env, ItemType contextItemType) throws XPathException {

        if (declaration==null) {
            // we've already done the type checking, no need to do it again
            return this;
        }

        // The order of events is critical here. First we ensure that the type of the
        // sequence expression is established. This is used to establish the type of the variable,
        // which in turn is required when type-checking the action part.

        sequence = sequence.typeCheck(env, contextItemType);

        RoleLocator role = new RoleLocator(RoleLocator.VARIABLE, new Integer(nameCode), 0, env.getNamePool());
        role.setSourceLocator(this);
        sequence = TypeChecker.strictTypeCheck(
                        sequence, declaration.getRequiredType(), role, env);
        final TypeHierarchy th = env.getConfiguration().getTypeHierarchy();
        final ItemType actualItemType = sequence.getItemType(th);
        declaration.refineTypeInformation(actualItemType,
                        sequence.getCardinality(),
                        (sequence instanceof Value ? (Value)sequence : null),
                        sequence.getSpecialProperties(), env);

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
        if (declaration != null) {
            // if this is an XSLT construct of the form <xsl:variable>text</xsl:variable>, try to replace
            // it by <xsl:variable select=""/>. This can be done if all the references to the variable use
            // its value as a string (rather than, say, as a node or as a boolean)
            if (sequence instanceof DocumentInstr && ((DocumentInstr)sequence).isTextOnly()) {
                if (declaration.allReferencesAreStrings()) {
                    sequence = ((DocumentInstr)sequence).getStringValueExpression(env);
                    adoptChildExpression(sequence);
                }
            }
            refCount = declaration.getReferenceCount(this, env);
            if (refCount == 0) {
                // variable is not used - no need to evaluate it
                return action;
            }
            declaration = null;     // let the garbage collector take it
        }

        int tries = 0;
        while (tries++ < 5) {
            Expression seq2 = sequence.optimize(opt, env, contextItemType);
            if (seq2 == sequence) {
                break;
            }
            sequence = seq2;
            adoptChildExpression(sequence);
            resetStaticProperties();
        }

        tries = 0;
        while (tries++ < 5) {
            Expression act2 = action.optimize(opt, env, contextItemType);
            if (act2 == action) {
                break;
            }
            action = act2;
            adoptChildExpression(action);
            resetStaticProperties();
        }

        // Try to promote any WHERE clause appearing within the LET expression

        Expression p = promoteWhereClause(null);
        if (p != null) {
            return p;
        }

        evaluationMode = ExpressionTool.lazyEvaluationMode(sequence);
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
        // minimize stack consumption by evaluating nested LET expressions iteratively
        LetExpression let = this;
        while (true) {
            ValueRepresentation val = let.eval(context);
            context.setLocalVariable(let.slotNumber, val);
            if (let.action instanceof LetExpression) {
                let = (LetExpression)let.action;
            } else {
                break;
            }
        }
        return let.action.iterate(context);
    }

    /**
     * Evaluate the variable. (This is overridden in a subclass).
     */

    protected ValueRepresentation eval(XPathContext context) throws XPathException {
        if (evaluationMode == ExpressionTool.UNDECIDED) {
            evaluationMode = ExpressionTool.lazyEvaluationMode(sequence);
        }
        return ExpressionTool.evaluate(sequence, evaluationMode, context, refCount);
    }

    /**
    * Evaluate the expression as a singleton
    */

    public Item evaluateItem(XPathContext context) throws XPathException {
        // minimize stack consumption by evaluating nested LET expressions iteratively
        LetExpression let = this;
        while (true) {
            ValueRepresentation val = let.eval(context);
            context.setLocalVariable(let.slotNumber, val);
            if (let.action instanceof LetExpression) {
                let = (LetExpression)let.action;
            } else {
                break;
            }
        }
        return let.action.evaluateItem(context);
    }

    /**
     * Process this expression as an instruction, writing results to the current
     * outputter
     */

    public void process(XPathContext context) throws XPathException {
        // minimize stack consumption by evaluating nested LET expressions iteratively
        LetExpression let = this;
        while (true) {
            ValueRepresentation val = let.eval(context);
            context.setLocalVariable(let.slotNumber, val);
            if (let.action instanceof LetExpression) {
                let = (LetExpression)let.action;
            } else {
                break;
            }
        }
        let.action.process(context);
    }


    /**
    * Determine the data type of the items returned by the expression, if possible
    * @return one of the values Type.STRING, Type.BOOLEAN, Type.NUMBER, Type.NODE,
    * or Type.ITEM (meaning not known in advance)
     * @param th
     */

	public ItemType getItemType(TypeHierarchy th) {
	    return action.getItemType(th);
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
        int props = action.getSpecialProperties();
        int seqProps = sequence.getSpecialProperties();
        if ((seqProps & StaticProperty.NON_CREATIVE) == 0) {
            props &= ~StaticProperty.NON_CREATIVE;
        }
        return props;
    }

    /**
     * Mark tail function calls
     */

    public boolean markTailFunctionCalls(int nameCode, int arity) {
        return ExpressionTool.markTailFunctionCalls(action, nameCode, arity);
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
            sequence = doPromotion(sequence, offer);
            if (offer.action == PromotionOffer.INLINE_VARIABLE_REFERENCES ||
                    offer.action == PromotionOffer.UNORDERED ||
                    offer.action == PromotionOffer.REPLACE_CURRENT) {
                action = doPromotion(action, offer);
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
                action = doPromotion(action, offer);
                offer.bindingList = savedBindingList;
            }
            // if this results in the expression (let $x := $y return Z), replace all references to
            // to $x by references to $y in the Z part, and eliminate this LetExpression by
            // returning the action part.
            if (sequence instanceof VariableReference) {
                replaceVariable(offer.getOptimizer(), sequence);
                return action;
            }
            // similarly, for (let $x := lazy($y) return Z)
            if (sequence instanceof LazyExpression &&
                    ((LazyExpression)sequence).getBaseExpression() instanceof VariableReference) {
                replaceVariable(offer.getOptimizer(), ((LazyExpression)sequence).getBaseExpression());
                return action;
            }

            return this;
        }
    }

    private void replaceVariable(Optimizer opt, Expression seq) throws XPathException {
        PromotionOffer offer2 = new PromotionOffer(opt);
        offer2.action = PromotionOffer.INLINE_VARIABLE_REFERENCES;
        Binding[] bindingList = {this};
        offer2.bindingList = bindingList;
        offer2.containingExpression = seq;
        action = doPromotion(action, offer2);
        if (offer2.accepted) {
            // there might be further references to the variable
            offer2.accepted = false;
            replaceVariable(opt, seq);
        }
    }

    /**
     * ProcessLeavingTail: called to do the real work of this instruction.
     * The results of the instruction are written
     * to the current Receiver, which can be obtained via the Controller.
     *
     * @param context The dynamic context of the transformation, giving access to the current node,
     *                the current variables, etc.
     * @return null if the instruction has completed execution; or a TailCall indicating
     *         a function call or template call that is delegated to the caller, to be made after the stack has
     *         been unwound so as to save stack space.
     */

    public TailCall processLeavingTail(XPathContext context) throws XPathException {
        // minimize stack consumption by evaluating nested LET expressions iteratively
        LetExpression let = this;
        while (true) {
            ValueRepresentation val = let.eval(context);
            context.setLocalVariable(let.slotNumber, val);
            if (let.action instanceof LetExpression) {
                let = (LetExpression)let.action;
            } else {
                break;
            }
        }
        if (let.action instanceof TailCallReturner) {
            return ((TailCallReturner)let.action).processLeavingTail(context);
        } else {
            let.action.process(context);
            return null;
        }
    }

    /**
    * Diagnostic print of expression structure
    */

    public void display(int level, PrintStream out, Configuration config) {
        out.println(ExpressionTool.indent(level) + "let $" + getVariableName(config.getNamePool()) +
                "[refCount=" + refCount + "] as " +
                sequence.getItemType(config.getTypeHierarchy()).toString(config.getNamePool()) +
                Cardinality.getOccurrenceIndicator(sequence.getCardinality()) +
                " :=");
        sequence.display(level+1, out, config);
        out.println(ExpressionTool.indent(level) + "return");
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
