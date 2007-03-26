package org.orbeon.saxon.expr;
import org.orbeon.saxon.Configuration;
import org.orbeon.saxon.functions.SystemFunction;
import org.orbeon.saxon.om.Item;
import org.orbeon.saxon.om.SequenceIterator;
import org.orbeon.saxon.om.ValueRepresentation;
import org.orbeon.saxon.trace.Location;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.type.ItemType;
import org.orbeon.saxon.type.SchemaType;
import org.orbeon.saxon.type.TypeHierarchy;
import org.orbeon.saxon.value.Cardinality;
import org.orbeon.saxon.value.EmptySequence;
import org.orbeon.saxon.value.IntegerValue;
import org.orbeon.saxon.value.SequenceType;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

/**
* A ForExpression maps an expression over a sequence.
* This version works with range variables, it doesn't change the context information
*/

public class ForExpression extends Assignation {

    private transient RangeVariableDeclaration positionVariable = null;
    private PositionBinding positionBinding = null;


    public ForExpression() {
    }

    /**
     * Set the reference to the position variable (XQuery only)
     */

    public void setPositionVariable (RangeVariableDeclaration decl) {
        positionVariable = decl;
        if (decl != null) {
            positionBinding = new PositionBinding(decl.getNameCode());
        }
    }

    public int getPositionVariableNameCode() {
        if (positionBinding == null) {
            return -1;
        } else {
            return positionBinding.getNameCode();
        }
    }

    public void setAction(Expression action) {
        super.setAction(action);
        if (positionVariable != null) {
            positionVariable.fixupReferences(positionBinding);
        }
    }

    /**
    * Set the slot number for the range variable
    */

    public void setSlotNumber(int nr) {
        super.setSlotNumber(nr);
        if (positionBinding != null) {
            positionBinding.setSlotNumber(nr+1);
        }
    }

    /**
     * Get the number of slots required. Normally 1, except for a FOR expression with an AT clause, where it is 2.
     */

    public int getRequiredSlots() {
        return (positionBinding == null ? 1 : 2);
    }

    /**
    * Type-check the expression
    */

    public Expression typeCheck(StaticContext env, ItemType contextItemType) throws XPathException {

        // The order of events is critical here. First we ensure that the type of the
        // sequence expression is established. This is used to establish the type of the variable,
        // which in turn is required when type-checking the action part.

        sequence = sequence.typeCheck(env, contextItemType);
        if (sequence instanceof EmptySequence) {
            return EmptySequence.getInstance();
        }

        if (declaration != null) {
            // if declaration is null, we've already done the type checking in a previous pass
            final TypeHierarchy th = env.getConfiguration().getTypeHierarchy();
            SequenceType decl = declaration.getRequiredType();
            SequenceType sequenceType = SequenceType.makeSequenceType(
                    decl.getPrimaryType(), StaticProperty.ALLOWS_ZERO_OR_MORE);
            RoleLocator role = new RoleLocator(RoleLocator.VARIABLE, new Integer(nameCode), 0, env.getNamePool());
            role.setSourceLocator(this);
            sequence = TypeChecker.strictTypeCheck(
                                    sequence, sequenceType, role, env);
            ItemType actualItemType = sequence.getItemType(th);
            declaration.refineTypeInformation(actualItemType,
                    StaticProperty.EXACTLY_ONE,
                    null,
                    sequence.getSpecialProperties(), env);
        }

        action = action.typeCheck(env, contextItemType);
        if (action instanceof EmptySequence) {
            return EmptySequence.getInstance();
        }

        return this;
    }

    /**
    * Optimize the expression
    */

    public Expression optimize(Optimizer opt, StaticContext env, ItemType contextItemType) throws XPathException {

        // Try to promote any WHERE clause appearing immediately within the FOR expression

        Expression p = promoteWhereClause(positionBinding);
        if (p != null) {
            return p.optimize(opt, env, contextItemType);
        }

        // See if there is a simple "where" condition that can be turned into a predicate

        Expression pred = convertWhereToPredicate(opt, env, contextItemType);
        if (pred != null && pred != this) {
            return pred.optimize(opt, env, contextItemType);
        }

        Expression seq2 = sequence.optimize(opt, env, contextItemType);
        if (seq2 != sequence) {
            sequence = seq2;
            adoptChildExpression(sequence);
            resetStaticProperties();
            return optimize(opt, env, contextItemType);
        }

        if (sequence instanceof EmptySequence) {
            return EmptySequence.getInstance();
        }

        Expression act2 = action.optimize(opt, env, contextItemType);
        if (act2 != action) {
            action = act2;
            adoptChildExpression(action);
            resetStaticProperties();
            // it's now worth re-attempting the "where" clause optimizations
            return optimize(opt, env, contextItemType);
        }

        if (action instanceof EmptySequence) {
            return EmptySequence.getInstance();
        }

        Expression e2 = extractLoopInvariants(opt, env, contextItemType);
        if (e2 != null && e2 != this) {
            return e2.optimize(opt, env, contextItemType);
        }

        // Simplify an expression of the form "for $b in a/b/c return $b/d".
        // (XQuery users seem to write these a lot!)

        if (declaration != null && positionVariable==null &&
                sequence instanceof PathExpression && action instanceof PathExpression) {
            int count = declaration.getReferenceCount(this, env);
            PathExpression path2 = (PathExpression)action;
            Expression s2 = path2.getStartExpression();
            if (count == 1 && s2 instanceof VariableReference && ((VariableReference)s2).getBinding() == this) {
                PathExpression newPath = new PathExpression(sequence, path2.getStepExpression());
                if ((newPath.getSpecialProperties() & StaticProperty.ORDERED_NODESET) != 0) {
                    // see test qxmp299, where this condition isn't satisfied
                    newPath.setParentExpression(getParentExpression());
                    newPath.setLocationId(getLocationId());
                    return newPath.simplify(env).typeCheck(env, contextItemType).optimize(opt, env, contextItemType);
                }
            }
        }

        // Simplify an expression of the form "for $x in EXPR return $x". These sometimes
        // arise as a result of previous optimization steps.

        if (action instanceof VariableReference && ((VariableReference)action).getBinding() == this) {
            ComputedExpression.setParentExpression(sequence, getParentExpression());
            return sequence;
        }

        declaration = null;     // let the garbage collector take it
        return this;
    }


    /**
     * Extract subexpressions in the action part that don't depend on the range variable
     */

    private Expression extractLoopInvariants(Optimizer opt, StaticContext env, ItemType contextItemType) throws XPathException {
        // Extract subexpressions that don't depend on the range variable.
        // We don't do this if there is a position variable. Ideally we would
        // extract subexpressions so long as they don't depend on either variable,
        // but we don't have the machinery to do that yet.
        // TODO: add this optimisation: we now have the mechanism in ExpressionTool.dependsOnVariable()
        // If a subexpression is (or might be) creative, this is, if it creates new nodes, we don't
        // extract it from the loop, but we do extract its non-creative subexpressions

        if (positionVariable == null) {
            PromotionOffer offer = new PromotionOffer(opt);
            offer.containingExpression = this;
            offer.action = PromotionOffer.RANGE_INDEPENDENT;
            Binding[] bindingList = {this};
            offer.bindingList = bindingList;
            Container container = getParentExpression();
            action = doPromotion(action, offer);
            if (offer.containingExpression instanceof LetExpression) {
                // a subexpression has been promoted
                ((ComputedExpression)offer.containingExpression).setParentExpression(container);
                // try again: there may be further subexpressions to promote
                offer.containingExpression = offer.containingExpression
                        //.simplify(env)
                        //.typeCheck(env, contextItemType)
                        .optimize(opt, env, contextItemType);
            }
            return offer.containingExpression;
        }
        return null;

    }

    /**
     * Convert where clause, if possible, to a predicate. Returns the converted expression if modified,
     * or null otherwise
     */

    public Expression convertWhereToPredicate(Optimizer opt, StaticContext env, ItemType contextItemType) throws XPathException {
        if (action instanceof IfExpression && ((IfExpression)action).getElseExpression() instanceof EmptySequence) {
            final TypeHierarchy th = env.getConfiguration().getTypeHierarchy();
            Expression head = null;
            Expression selection = sequence;
            ItemType selectionContextItemType = contextItemType;
            if (sequence instanceof PathExpression && ((PathExpression)sequence).isAbsolute(th)) {
                head = ((PathExpression)sequence).getFirstStep();
                selection = ((PathExpression)sequence).getRemainingSteps();
                selectionContextItemType = head.getItemType(th);
            }

            boolean changed = false;
            IfExpression condAction = (IfExpression)action;
            List list = new ArrayList(4);
            BooleanExpression.listAndComponents(condAction.getCondition(), list);
            for (int t=list.size()-1; t>=0; t--) {
                // Process each term in the where clause independently
                Expression term = (Expression)list.get(t);

                if (term instanceof ValueComparison || term instanceof SingletonComparison) {
                    BinaryExpression comp = (BinaryExpression)term;
                    Expression[] operands = comp.getOperands();
                    for (int op=0; op<2; op++) {

                        // If the where clause is a simple test on the position variable, for example
                        //    for $x at $p in EXPR where $p = 5 return A
                        // then absorb the where condition into a predicate, rewriting it as
                        //    for $x in EXPR[position() = 5] return A
                        // This takes advantage of the optimizations applied to positional filter expressions
                        // Only do this if the sequence expression has not yet been changed, because
                        // the position in a predicate after the first is different.

                        if (positionVariable != null && positionVariable.getReferenceList().size() == 1 && !changed) {
                            if (operands[op] instanceof VariableReference &&
                                    ((VariableReference)operands[op]).getBinding() == positionBinding &&
                                    (operands[1-op].getDependencies() & StaticProperty.DEPENDS_ON_FOCUS) == 0) {
                                FunctionCall position =
                                        SystemFunction.makeSystemFunction("position", 1, env.getNamePool());
                                position.setArguments(SimpleExpression.NO_ARGUMENTS);
                                Expression predicate;
                                if (term instanceof ValueComparison) {
                                    if (op==0) {
                                        predicate = new ValueComparison(position, comp.getOperator(), operands[1]);
                                    } else {
                                        predicate = new ValueComparison(operands[0], comp.getOperator(), position);
                                    }
                                } else { // term instanceof SingletonComparison
                                    if (op==0) {
                                        predicate = new SingletonComparison(position, comp.getOperator(), operands[1]);
                                    } else {
                                        predicate = new SingletonComparison(operands[0], comp.getOperator(), position);
                                    }
                                }
                                selection = new FilterExpression(selection, predicate);
                                selection = selection.typeCheck(env, selectionContextItemType);
                                //action = condAction.getThenExpression();
                                positionVariable = null;
                                positionBinding = null;
                                list.remove(t);
                                changed = true;
                                break;
                                //return simplify(env).typeCheck(env, contextItemType).optimize(opt, env, contextItemType);
                            }
                        }

                        // If the where clause is a simple test on the value of the range variable, or a path
                        // expression starting with the range variable, then rewrite it as a predicate.
                        // For example, rewrite
                        //    for $x in EXPR where $x/a/b eq "z" return A
                        // as
                        //    for $x in EXPR[a/b eq "z"] return A

                        Binding[] thisVar = {this};
                        if ( positionVariable == null &&
                                ExpressionTool.isVariableReplaceableByDot(term, thisVar) &&
                                (term.getDependencies() & StaticProperty.DEPENDS_ON_FOCUS) == 0 &&
                                ExpressionTool.dependsOnVariable(operands[op], thisVar) &&
                                !ExpressionTool.dependsOnVariable(operands[1-op], thisVar)) {
                            PromotionOffer offer = new PromotionOffer(opt);
                            offer.action = PromotionOffer.INLINE_VARIABLE_REFERENCES;
                            offer.bindingList = thisVar;
                            offer.containingExpression = new ContextItemExpression();
                            Expression newOperand = operands[op].promote(offer);
                            if (newOperand != null && offer.accepted) {
                                Expression predicate;
                                if (op==0) {
                                    predicate = new ValueComparison(newOperand, comp.getOperator(), operands[1]);
                                } else {
                                    predicate = new ValueComparison(operands[0], comp.getOperator(), newOperand);
                                }
                                predicate = predicate.typeCheck(env, sequence.getItemType(th));
                                selection = new FilterExpression(selection, predicate);
                                selection = selection.typeCheck(env, selectionContextItemType);
                                changed = true;
                                positionVariable = null;
                                positionBinding = null;
                                list.remove(t);
                            }
                        }
                    }
                } else if (term instanceof GeneralComparison) {
                    GeneralComparison comp = (GeneralComparison)term;
                    Expression[] operands = comp.getOperands();
                    for (int op=0; op<2; op++) {

                        // If the where clause is a simple test on the value of the range variable, or a path
                        // expression starting with the range variable, then rewrite it as a predicate.
                        // For example, rewrite
                        //    for $x in EXPR where $x/a/b = "z" return A
                        // as
                        //    for $x in EXPR[a/b = "z"] return A

                        Binding[] thisVar = {this};
                        if (positionVariable == null &&
                                ExpressionTool.isVariableReplaceableByDot(term, thisVar) &&
                                (term.getDependencies() & StaticProperty.DEPENDS_ON_FOCUS) == 0 &&
                                ExpressionTool.dependsOnVariable(operands[op], thisVar) &&
                                !ExpressionTool.dependsOnVariable(operands[1-op], thisVar)) {
                            PromotionOffer offer = new PromotionOffer(opt);
                            offer.action = PromotionOffer.INLINE_VARIABLE_REFERENCES;
                            offer.bindingList = thisVar;
                            offer.containingExpression = new ContextItemExpression();
                            Expression newOperand = operands[op].promote(offer);
                            if (newOperand != null && !ExpressionTool.dependsOnVariable(newOperand, thisVar)) {
                                if (newOperand instanceof ComputedExpression) {
                                    ((ComputedExpression)newOperand).resetStaticProperties();
                                }
                                Expression predicate;
                                //newOperand = new Atomizer(newOperand, env.getConfiguration());
                                if (op==0) {
                                    // TODO: make GeneralComparisonSA where appropriate
                                    predicate = new GeneralComparison(newOperand, comp.getOperator(), operands[1]);
                                } else {
                                    predicate = new GeneralComparison(operands[0], comp.getOperator(), newOperand);
                                }
                                selection = new FilterExpression(selection, predicate);
                                selection = selection.typeCheck(env, selectionContextItemType);
                                selection = selection.optimize(opt, env, selectionContextItemType);
                                resetStaticProperties();
                                //action = condAction.getThenExpression();
                                positionVariable = null;
                                positionBinding = null;
                                //return simplify(env).typeCheck(env, contextItemType).optimize(opt, env, contextItemType);
                                list.remove(t);
                                changed = true;
                                break;
                            }
                        }
                    }
                }
            }
            if (changed) {
                if (list.isEmpty()) {
                    action = condAction.getThenExpression();
                    adoptChildExpression(action);
                } else {
                    Expression term = (Expression)list.get(0);
                    for (int t=1; t<list.size(); t++) {
                        term = new BooleanExpression(term, Token.AND, (Expression)list.get(t));
                    }
                    condAction.setCondition(term);
                }
                if (head == null) {
                    sequence = selection;
                } else {
                    PathExpression path = new PathExpression(head, selection);
                    path.setParentExpression(this);
                    Expression k = opt.convertPathExpressionToKey(path, env);
                    if (k == null) {
                        sequence = path;
                    } else {
                        sequence = k;
                    }
                    sequence = sequence.simplify(env).typeCheck(env, contextItemType).optimize(opt, env, contextItemType);
                    adoptChildExpression(sequence);
                }
                return this;
            }
        }
        return null;
    }

    /**
     * Mark tail function calls: only possible if the for expression iterates zero or one times.
     * (This arises in XSLT/XPath, which does not have a LET expression, so FOR gets used instead)
     */

    public boolean markTailFunctionCalls(int nameCode, int arity) {
        if (!Cardinality.allowsMany(sequence.getCardinality())) {
            return ExpressionTool.markTailFunctionCalls(action, nameCode, arity);
        } else {
            return false;
        }
    }

    /**
     * Extend an array of variable bindings to include the binding(s) defined in this expression
     */

    protected Binding[] extendBindingList(Binding[] in) {
        if (positionBinding == null) {
            return super.extendBindingList(in);
        }
        Binding[] newBindingList = new Binding[in.length+2];
        System.arraycopy(in, 0, newBindingList, 0, in.length);
        newBindingList[in.length] = this;
        newBindingList[in.length+1] = positionBinding;
        return newBindingList;
    }

    /**
     * An implementation of Expression must provide at least one of the methods evaluateItem(), iterate(), or process().
     * This method indicates which of these methods is provided. This implementation provides both iterate() and
     * process() methods natively.
     */

    public int getImplementationMethod() {
        return ITERATE_METHOD | PROCESS_METHOD;
    }

    /**
     * Check that any elements and attributes constructed or returned by this expression are acceptable
     * in the content model of a given complex type. It's always OK to say yes, since the check will be
     * repeated at run-time. The process of checking element and attribute constructors against the content
     * model of a complex type also registers the type of content expected of those constructors, so the
     * static validation can continue recursively.
     */

    public void checkPermittedContents(SchemaType parentType, StaticContext env, boolean whole) throws XPathException {
        action.checkPermittedContents(parentType, env, false);
    }

    /**
    * Iterate over the sequence of values
    */

    public SequenceIterator iterate(XPathContext context) throws XPathException {

        // First create an iteration of the base sequence.

        // Then create a MappingIterator which applies a mapping function to each
        // item in the base sequence. The mapping function is essentially the "return"
        // expression, wrapped in a MappingAction object that is responsible also for
        // setting the range variable at each step.

        SequenceIterator base = sequence.iterate(context);
                                                            // TODO:PERF treat "for" over singleton specially
        MappingFunction map = new MappingAction(context, slotNumber, positionBinding, action);
        return new MappingIterator(base, map);
    }

    /**
     * Process this expression as an instruction, writing results to the current
     * outputter
     */

    public void process(XPathContext context) throws XPathException {
        SequenceIterator iter = sequence.iterate(context);
        int position = 1;
        while (true) {
            Item item = iter.next();
            if (item == null) break;
            context.setLocalVariable(slotNumber, item);
            if (positionBinding != null) {
                positionBinding.setPosition(position++, context);
            }
            action.process(context);
        }
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
        int c1 = sequence.getCardinality();
        int c2 = action.getCardinality();
        return Cardinality.multiply(c1, c2);
	}

    /**
    * Diagnostic print of expression structure
    */

    public void display(int level, PrintStream out, Configuration config) {
        out.println(ExpressionTool.indent(level) +
                "for $" + getVariableName(config.getNamePool()) +
                " as " + sequence.getItemType(config.getTypeHierarchy()).toString(config.getNamePool()) +
                (positionVariable == null ? "" : " at $?") +
                " in");
        sequence.display(level+1, out, config);
        out.println(ExpressionTool.indent(level) + "return");
        action.display(level+1, out, config);
    }

    /**
     * The MappingAction represents the action to be taken for each item in the
     * source sequence. It acts as the MappingFunction for the mapping iterator, and
     * also as the Binding of the position variable (at $n) in XQuery, if used.
     */

    private static class MappingAction implements MappingFunction {

        private XPathContext context;
        private int slotNumber;
        private Expression action;
        private PositionBinding positionBinding;
        private int position = 1;

        public MappingAction(XPathContext context,
                                int slotNumber,
                                PositionBinding positionBinding,
                                Expression action) {
            this.context = context;
            this.slotNumber = slotNumber;
            this.positionBinding = positionBinding;
            this.action = action;
        }

        public Object map(Item item) throws XPathException {
            context.setLocalVariable(slotNumber, item);
            if (positionBinding != null) {
                positionBinding.setPosition(position++, context);
            }
            return action.iterate(context);
        }
    }

    /**
     * Get the type of this expression for use in tracing and diagnostics
     * @return the type of expression, as enumerated in class {@link org.orbeon.saxon.trace.Location}
     */

    protected int getConstructType() {
        return Location.FOR_EXPRESSION;
    }

    /**
     * This class represents the binding of the position variable ("at $p") in an XQuery FOR clause.
     * The variable is held in a slot on the stackframe: in 8.4 and earlier it was held as a property
     * of the iterator, but that let to problems with lazy evaluation because the value wasn't saved as
     * part of a Closure.
     */

    private static class PositionBinding implements Binding {

        private int slotNumber;
        private int nameCode;

        public PositionBinding(int nameCode) {
            this.nameCode = nameCode;
        }

        private void setSlotNumber(int slot) {
            this.slotNumber = slot;
        }

        private void setPosition(int position, XPathContext context) {
            context.setLocalVariable(slotNumber, new IntegerValue(position));
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
         * Get the name of the positional variable
         * @return the namecode of the positional variable
         */

        public int getNameCode() {
            return nameCode;
        }

        public ValueRepresentation evaluateVariable(XPathContext context) throws XPathException {
            return context.evaluateLocalVariable(slotNumber);
        }

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
