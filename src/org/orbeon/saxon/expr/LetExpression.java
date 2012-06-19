package org.orbeon.saxon.expr;

import org.orbeon.saxon.evpull.EventIterator;
import org.orbeon.saxon.instruct.DocumentInstr;
import org.orbeon.saxon.instruct.GlobalVariable;
import org.orbeon.saxon.instruct.TailCall;
import org.orbeon.saxon.instruct.TailCallReturner;
import org.orbeon.saxon.om.Item;
import org.orbeon.saxon.om.SequenceIterator;
import org.orbeon.saxon.om.StructuredQName;
import org.orbeon.saxon.om.ValueRepresentation;
import org.orbeon.saxon.trace.ExpressionPresenter;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.type.ItemType;
import org.orbeon.saxon.type.SchemaType;
import org.orbeon.saxon.type.TypeHierarchy;
import org.orbeon.saxon.value.Cardinality;
import org.orbeon.saxon.value.SequenceType;

import java.util.List;
import java.util.ArrayList;

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

    /**
     * Create a LetExpression
     */

    public LetExpression() {
        //System.err.println("let");
    }

    /**
     * Indicate that the variable bound by this let expression should be indexable
     * (because it is used in an appropriate filter expression)
     */

    public void setIndexedVariable() {
        refCount = FilterExpression.FILTERED;
    }

    /**
     * Test whether the variable bound by this let expression should be indexable
     * @return true if the variable should be indexable
     */

    public boolean isIndexedVariable() {
        return (refCount == FilterExpression.FILTERED);
    }

    /**
     * Get the (nominal) count of the number of references to this variable
     * @return zero if there are no references, one if there is a single reference that is not in
     * a loop, some higher number if there are multiple references (or a single reference in a loop),
     * or the special value @link RangeVariable#FILTERED} if there are any references
     * in filter expressions that require searching.
     */

    public int getNominalReferenceCount() {
        return refCount;
    }

    /**
     * Register a variable reference that refers to the variable bound in this let expression
     * @param v the variable reference
     */

    public void addReference(VariableReference v) {
        refCount++;
    }

    /**
     * Type-check the expression
     */

    public Expression typeCheck(ExpressionVisitor visitor, ItemType contextItemType) throws XPathException {

        // The order of events is critical here. First we ensure that the type of the
        // sequence expression is established. This is used to establish the type of the variable,
        // which in turn is required when type-checking the action part.

        sequence = visitor.typeCheck(sequence, contextItemType);

        RoleLocator role = new RoleLocator(RoleLocator.VARIABLE, getVariableQName(), 0);
        //role.setSourceLocator(this);
        sequence = TypeChecker.strictTypeCheck(
                sequence, requiredType, role, visitor.getStaticContext());
        final TypeHierarchy th = visitor.getConfiguration().getTypeHierarchy();
        final ItemType actualItemType = sequence.getItemType(th);

        refineTypeInformation(actualItemType,
                sequence.getCardinality(),
                (sequence instanceof Literal ? ((Literal) sequence).getValue() : null),
                sequence.getSpecialProperties(), visitor, this);

        action = visitor.typeCheck(action, contextItemType);
        return this;
    }


    /**
     * Determine whether this expression implements its own method for static type checking
     *
     * @return true - this expression has a non-trivial implementation of the staticTypeCheck()
     *         method
     */

    public boolean implementsStaticTypeCheck() {
        return true;
    }

    /**
     * Static type checking for let expressions is delegated to the expression itself,
     * and is performed on the "action" expression, to allow further delegation to the branches
     * of a conditional
     * @param req the required type
     * @param backwardsCompatible true if backwards compatibility mode applies
     * @param role the role of the expression in relation to the required type
     * @param visitor an expression visitor
     * @return the expression after type checking (perhaps augmented with dynamic type checking code)
     * @throws XPathException if failures occur, for example if the static type of one branch of the conditional
     * is incompatible with the required type
     */

    public Expression staticTypeCheck(SequenceType req,
                                             boolean backwardsCompatible,
                                             RoleLocator role, ExpressionVisitor visitor)
    throws XPathException {
        action = TypeChecker.staticTypeCheck(action, req, backwardsCompatible, role, visitor);
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

        StaticContext env = visitor.getStaticContext();
        Optimizer opt = visitor.getConfiguration().getOptimizer();

        // if this is a construct of the form "let $j := EXP return $j" replace it with EXP
        // Remarkably, people do write this, and it can also be produced by previous rewrites
        // Note that type checks will already have been added to the sequence expression

        if (action instanceof VariableReference &&
                ((VariableReference) action).getBinding() == this) {
            return visitor.optimize(sequence, contextItemType);
        }

        /**
         * Unless this has already been done, find and count the references to this variable
         */

        // if this is an XSLT construct of the form <xsl:variable>text</xsl:variable>, try to replace
        // it by <xsl:variable select=""/>. This can be done if all the references to the variable use
        // its value as a string (rather than, say, as a node or as a boolean)
        if (sequence instanceof DocumentInstr && ((DocumentInstr) sequence).isTextOnly()) {
            if (allReferencesAreFlattened()) {
                sequence = ((DocumentInstr) sequence).getStringValueExpression(env);
                adoptChildExpression(sequence);
            }
        }

        if (!isIndexedVariable()) {
            refCount = ExpressionTool.getReferenceCount(action, this, false);
        }
        if (refCount == 0) {
            // variable is not used - no need to evaluate it
            Expression a = visitor.optimize(action, contextItemType);
            ExpressionTool.copyLocationInfo(this, a);
            return a;
        }

        if (refCount == 1 || sequence instanceof Literal) {
            // Either there's only one reference, and it's not in a loop.
            // Or the variable is bound to a constant value.
            // In these two cases we can inline the reference.
            // That is, we replace "let $x := SEQ return f($x)" by "f(SEQ)". Note, we rely on the fact
            // that any context-changing expression is treated as a loop, and generates a refCount greater
            // than one.
            replaceVariable(opt, sequence);
            return visitor.optimize(action, contextItemType);
        }

        int tries = 0;
        while (tries++ < 5) {
            Expression seq2 = visitor.optimize(sequence, contextItemType);
            if (seq2 == sequence) {
                break;
            }
            sequence = seq2;
            adoptChildExpression(sequence);
            visitor.resetStaticProperties();
        }

        tries = 0;
        while (tries++ < 5) {
            Expression act2 = visitor.optimize(action, contextItemType);
            if (act2 == action) {
                break;
            }
            action = act2;
            adoptChildExpression(action);
            visitor.resetStaticProperties();
        }

        // Try to promote any WHERE clause appearing within the LET expression

        Expression p = promoteWhereClause(null);
        if (p != null) {
            return p;
        }

        evaluationMode = (isIndexedVariable() ?
                ExpressionTool.MAKE_CLOSURE :
                ExpressionTool.lazyEvaluationMode(sequence));
        return this;
    }

    /**
     * Determine whether all references to this variable are using the value either
     * (a) by atomizing it, or (b) by taking its string value. (This excludes usages
     * such as testing the existence of a node or taking the effective boolean value).
     * @return true if all references are known to atomize (or stringify) the value,
     * false otherwise. The value false may indicate "not known".
     */

    private boolean allReferencesAreFlattened() {
        List references = new ArrayList();
        ExpressionTool.gatherVariableReferences(action, this, references);
        for (int i=references.size()-1; i>=0; i--) {
            BindingReference bref = (BindingReference)references.get(i);
            if (bref instanceof VariableReference) {
                VariableReference ref = (VariableReference)bref;
                if (ref.isFlattened()) {
                    // OK, it's a string context
                } else {
                    return false;
                }

            } else {
                // it must be saxon:assign
                return false;
            }
        }
        return true;
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
     * Iterate over the result of the expression to return a sequence of items
     */

    public SequenceIterator iterate(XPathContext context) throws XPathException {
        // minimize stack consumption by evaluating nested LET expressions iteratively
        LetExpression let = this;
        while (true) {
            ValueRepresentation val = let.eval(context);
            context.setLocalVariable(let.getLocalSlotNumber(), val);
            if (let.action instanceof LetExpression) {
                let = (LetExpression) let.action;
            } else {
                break;
            }
        }
        return let.action.iterate(context);
    }

    /**
     * Iterate over the result of the expression to return a sequence of events
     */

    public EventIterator iterateEvents(XPathContext context) throws XPathException {
        // minimize stack consumption by evaluating nested LET expressions iteratively
        LetExpression let = this;
        while (true) {
            ValueRepresentation val = let.eval(context);
            context.setLocalVariable(let.getLocalSlotNumber(), val);
            if (let.action instanceof LetExpression) {
                let = (LetExpression) let.action;
            } else {
                break;
            }
        }
        return let.action.iterateEvents(context);
    }


    /**
     * Evaluate the variable.
     * @param context the dynamic evaluation context
     * @return the result of evaluating the expression that is bound to the variable
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
            context.setLocalVariable(let.getLocalSlotNumber(), val);
            if (let.action instanceof LetExpression) {
                let = (LetExpression) let.action;
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
            context.setLocalVariable(let.getLocalSlotNumber(), val);
            if (let.action instanceof LetExpression) {
                let = (LetExpression) let.action;
            } else {
                break;
            }
        }
        let.action.process(context);
    }


    /**
     * Determine the data type of the items returned by the expression, if possible
     *
     * @param th the type hierarchy cache
     * @return one of the values Type.STRING, Type.BOOLEAN, Type.NUMBER, Type.NODE,
     *         or Type.ITEM (meaning not known in advance)
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

    public int markTailFunctionCalls(StructuredQName qName, int arity) {
        return ExpressionTool.markTailFunctionCalls(action, qName, arity);
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
            Expression seq2 = doPromotion(sequence, offer);
            if (seq2 != sequence) {
                // if we've extracted a global variable, it may need to be marked indexable
                if (seq2 instanceof VariableReference) {
                    Binding b = ((VariableReference)seq2).getBinding();
                    if (b instanceof GlobalVariable) {
                        ((GlobalVariable)b).setReferenceCount(refCount < 10 ? 10 : refCount);
                    }
                }
                sequence = seq2;
            }
            if (offer.action == PromotionOffer.INLINE_VARIABLE_REFERENCES ||
                    offer.action == PromotionOffer.UNORDERED ||
                    offer.action == PromotionOffer.REPLACE_CURRENT ||
                    offer.action == PromotionOffer.EXTRACT_GLOBAL_VARIABLES) {
                action = doPromotion(action, offer);
            } else if (offer.action == PromotionOffer.RANGE_INDEPENDENT ||
                    offer.action == PromotionOffer.FOCUS_INDEPENDENT) {
                // Pass the offer to the action expression after adding the variable bound by this let expression,
                // so that a subexpression must depend on neither variable if it is to be promoted
                Binding[] savedBindingList = offer.bindingList;
                offer.bindingList = extendBindingList(offer.bindingList);
                action = doPromotion(action, offer);
                offer.bindingList = savedBindingList;
            }
            // if this results in the expression (let $x := $y return Z), replace all references to
            // to $x by references to $y in the Z part, and eliminate this LetExpression by
            // returning the action part.
            if (sequence instanceof VariableReference) {
                Binding b = ((VariableReference)sequence).getBinding();
                if (b != null && !b.isAssignable()) {
                    replaceVariable(offer.getOptimizer(), sequence);
                    return action;
                }
            }
            // similarly, for (let $x := lazy($y) return Z)
            if (sequence instanceof LazyExpression &&
                    ((LazyExpression) sequence).getBaseExpression() instanceof VariableReference &&
                    !((VariableReference)((LazyExpression) sequence).getBaseExpression()).getBinding().isAssignable()) {
                replaceVariable(offer.getOptimizer(), ((LazyExpression) sequence).getBaseExpression());
                return action;
            }

            return this;
        }
    }


    /**
     * Copy an expression. This makes a deep copy.
     * @return the copy of the original expression
     */

    public Expression copy() {
        LetExpression let = new LetExpression();
        let.setVariableQName(variableName);
        let.setRequiredType(requiredType);
        let.setSequence(sequence.copy());
        Expression newAction = action.copy();
        let.setAction(newAction);
        ExpressionTool.rebindVariableReferences(newAction, this, let);
        return let;
    }

    /**
     * Replace all references to the variable bound by this let expression,
     * that occur within the action expression, with the given expression
     *
     * @param opt The optimizer
     * @param seq the expression
     * @throws XPathException
     */

    private void replaceVariable(Optimizer opt, Expression seq) throws XPathException {
        PromotionOffer offer2 = new PromotionOffer(opt);
        offer2.action = PromotionOffer.INLINE_VARIABLE_REFERENCES;
        offer2.bindingList = new Binding[] {this};
        offer2.containingExpression = seq;
        action = doPromotion(action, offer2);
        if (offer2.accepted) {
            // there might be further references to the variable
            offer2.accepted = false;
            replaceVariable(opt, seq);
        }
        if (isIndexedVariable()) {
            if (seq instanceof VariableReference) {
                Binding newBinding = ((VariableReference) seq).getBinding();
                if (newBinding instanceof LetExpression) {
                    ((LetExpression) newBinding).setIndexedVariable();
                }
            } else {
                // can happen as a result of other rewrites
                refCount = 10;
            }
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
            context.setLocalVariable(let.getLocalSlotNumber(), val);
            if (let.action instanceof LetExpression) {
                let = (LetExpression) let.action;
            } else {
                break;
            }
        }
        if (let.action instanceof TailCallReturner) {
            return ((TailCallReturner) let.action).processLeavingTail(context);
        } else {
            let.action.process(context);
            return null;
        }
    }


    /**
     * Evaluate an updating expression, adding the results to a Pending Update List.
     * The default implementation of this method, which is used for non-updating expressions,
     * throws an UnsupportedOperationException
     *
     * @param context the XPath dynamic evaluation context
     * @param pul     the pending update list to which the results should be written
     */

    public void evaluatePendingUpdates(XPathContext context, PendingUpdateList pul) throws XPathException {
        // minimize stack consumption by evaluating nested LET expressions iteratively
        LetExpression let = this;
        while (true) {
            ValueRepresentation val = let.eval(context);
            context.setLocalVariable(let.getLocalSlotNumber(), val);
            if (let.action instanceof LetExpression) {
                let = (LetExpression) let.action;
            } else {
                break;
            }
        }
        let.action.evaluatePendingUpdates(context, pul);
    }

    /**
     * Diagnostic print of expression structure. The abstract expression tree
     * is written to the supplied output destination.
     */

    public void explain(ExpressionPresenter out) {
        out.startElement("let");
        out.emitAttribute("variable", getVariableName());
        out.emitAttribute("as", sequence.getItemType(out.getTypeHierarchy()).toString(out.getNamePool()) +
                Cardinality.getOccurrenceIndicator(sequence.getCardinality()));
        if (isIndexedVariable()) {
            out.emitAttribute("indexable", "true");
        }
        out.startSubsidiaryElement("be");
        sequence.explain(out);
        out.endSubsidiaryElement();
        out.startSubsidiaryElement("return");
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
