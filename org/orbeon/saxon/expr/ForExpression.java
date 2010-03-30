package org.orbeon.saxon.expr;
import org.orbeon.saxon.evpull.EventIterator;
import org.orbeon.saxon.evpull.EventMappingFunction;
import org.orbeon.saxon.evpull.EventMappingIterator;
import org.orbeon.saxon.functions.KeyFn;
import org.orbeon.saxon.functions.SystemFunction;
import org.orbeon.saxon.instruct.Choose;
import org.orbeon.saxon.om.Item;
import org.orbeon.saxon.om.SequenceIterator;
import org.orbeon.saxon.om.StructuredQName;
import org.orbeon.saxon.om.ValueRepresentation;
import org.orbeon.saxon.trace.ExpressionPresenter;
import org.orbeon.saxon.trace.Location;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.type.ItemType;
import org.orbeon.saxon.type.SchemaType;
import org.orbeon.saxon.type.TypeHierarchy;
import org.orbeon.saxon.value.Cardinality;
import org.orbeon.saxon.value.Int64Value;
import org.orbeon.saxon.value.SequenceType;

import java.util.ArrayList;
import java.util.List;

/**
* A ForExpression maps an expression over a sequence.
* This version works with range variables, it doesn't change the context information
*/

public class ForExpression extends Assignation {

    private PositionVariable positionVariable = null;

    /**
     * Create a "for" expression (for $x at $p in SEQUENCE return ACTION)
     */

    public ForExpression() {
    }

    /**
     * Set the reference to the position variable (XQuery only)
     * @param decl the range variable declaration for the position variable
     */

    public void setPositionVariable (PositionVariable decl) {
        positionVariable = decl;
    }

    /**
     * Get the name of the position variable
     * @return the name of the position variable ("at $p") if there is one, or null if not
     */

    public StructuredQName getPositionVariableName() {
        if (positionVariable == null) {
            return null;
        } else {
            return positionVariable.getVariableQName();
        }
    }

    /**
     * Set the slot number for the range variable
     * @param nr the slot number allocated to the range variable on the local stack frame.
     * This implicitly allocates the next slot number to the position variable if there is one.
    */

    public void setSlotNumber(int nr) {
        super.setSlotNumber(nr);
        if (positionVariable != null) {
            positionVariable.setSlotNumber(nr+1);
        }
    }

    /**
     * Get the number of slots required.
     * @return normally 1, except for a FOR expression with an AT clause, where it is 2.
     */

    public int getRequiredSlots() {
        return (positionVariable == null ? 1 : 2);
    }

    /**
    * Type-check the expression
    */

    public Expression typeCheck(ExpressionVisitor visitor, ItemType contextItemType) throws XPathException {

        // The order of events is critical here. First we ensure that the type of the
        // sequence expression is established. This is used to establish the type of the variable,
        // which in turn is required when type-checking the action part.

        sequence = visitor.typeCheck(sequence, contextItemType);
        if (Literal.isEmptySequence(sequence)) {
            return sequence;
        }

        if (requiredType != null) {
            // if declaration is null, we've already done the type checking in a previous pass
            final TypeHierarchy th = visitor.getConfiguration().getTypeHierarchy();
            SequenceType decl = requiredType;
            SequenceType sequenceType = SequenceType.makeSequenceType(
                    decl.getPrimaryType(), StaticProperty.ALLOWS_ZERO_OR_MORE);
            RoleLocator role = new RoleLocator(RoleLocator.VARIABLE, variableName, 0
            );
            //role.setSourceLocator(this);
            sequence = TypeChecker.strictTypeCheck(
                                    sequence, sequenceType, role, visitor.getStaticContext());
            ItemType actualItemType = sequence.getItemType(th);
            refineTypeInformation(actualItemType,
                    StaticProperty.EXACTLY_ONE,
                    null,
                    sequence.getSpecialProperties(), visitor, this);
        }

        action = visitor.typeCheck(action, contextItemType);
        if (Literal.isEmptySequence(action)) {
            return action;
        }

        return this;
    }

    /**
    * Optimize the expression
    */

    public Expression optimize(ExpressionVisitor visitor, ItemType contextItemType) throws XPathException {
        Optimizer opt = visitor.getConfiguration().getOptimizer();
        boolean debug = opt.getConfiguration().isOptimizerTracing();

        // Try to promote any WHERE clause appearing immediately within the FOR expression

        Expression p = promoteWhereClause(positionVariable);
        if (p != null) {
            if (debug) {
                opt.trace("Promoted where clause in for $" + getVariableName(), p);
            }
            return visitor.optimize(p, contextItemType);
        }

        // See if there is a simple "where" condition that can be turned into a predicate

        Expression pred = convertWhereToPredicate(visitor, contextItemType);
        if (pred != null && pred != this) {
            if (debug) {
                opt.trace("Converted where clause in for $" + getVariableName() + " to predicate", pred);
            }
            return visitor.optimize(pred, contextItemType);
        }

        Expression seq2 = visitor.optimize(sequence, contextItemType);
        if (seq2 != sequence) {
            sequence = seq2;
            adoptChildExpression(sequence);
            visitor.resetStaticProperties();
            return optimize(visitor, contextItemType);
        }

        if (Literal.isEmptySequence(sequence)) {
            return sequence;
        }

        Expression act2 = visitor.optimize(action, contextItemType);
        if (act2 != action) {
            action = act2;
            adoptChildExpression(action);
            visitor.resetStaticProperties();
            // it's now worth re-attempting the "where" clause optimizations
            return optimize(visitor, contextItemType);
        }

        if (Literal.isEmptySequence(action)) {
            return action;
        }

        Expression e2 = extractLoopInvariants(visitor, contextItemType);
        if (e2 != null && e2 != this) {
            if (debug) {
                opt.trace("Extracted invariant in 'for $" + getVariableName() + "' loop", e2);
            }
            return visitor.optimize(e2, contextItemType);
        }

        // Simplify an expression of the form "for $b in a/b/c return $b/d".
        // (XQuery users seem to write these a lot!)

        if (positionVariable==null &&
                sequence instanceof SlashExpression && action instanceof SlashExpression) {
            int count = ExpressionTool.getReferenceCount(action, this, false);
            SlashExpression path2 = (SlashExpression)action;
            Expression s2 = path2.getStartExpression();
            Expression step2 = path2.getStepExpression();
            if (count == 1 && s2 instanceof VariableReference && ((VariableReference)s2).getBinding() == this &&
                    ((step2.getDependencies() & (StaticProperty.DEPENDS_ON_POSITION | StaticProperty.DEPENDS_ON_LAST)) == 0)) {
                Expression newPath = new SlashExpression(sequence, path2.getStepExpression());
                ExpressionTool.copyLocationInfo(this, newPath);
                newPath = visitor.typeCheck(visitor.simplify(newPath), contextItemType);
                if (newPath instanceof SlashExpression) {
                    // if not, it has been wrapped in a DocumentSorter or Reverser, which makes it ineligible.
                    // see test qxmp299, where this condition isn't satisfied
                    if (debug) {
                        opt.trace("Collapsed return clause of for $" + getVariableName() +
                                " into path expression", newPath);
                    }
                    return visitor.optimize(newPath, contextItemType);
                }
            }
        }

        // Simplify an expression of the form "for $x in EXPR return $x". These sometimes
        // arise as a result of previous optimization steps.

        if (action instanceof VariableReference && ((VariableReference)action).getBinding() == this) {
            if (debug) {
                opt.trace("Collapsed redundant for expression $" + getVariableName(), sequence);
            }
            return sequence;
        }

        // Rewrite an expression of the form "for $x at $p in EXPR return $p" as "1 to count(EXPR)"

        if (action instanceof VariableReference && ((VariableReference)action).getBinding() == positionVariable) {
            FunctionCall count = SystemFunction.makeSystemFunction("count", new Expression[]{sequence});
            RangeExpression range = new RangeExpression(new Literal(Int64Value.PLUS_ONE), Token.TO, count);
            if (debug) {
                opt.trace("Replaced 'for $x at $p in EXP return $p' by '1 to count(EXP)'", range);
            }
            return range.optimize(visitor, contextItemType);
        }

        // If the cardinality of the sequence is exactly one, rewrite as a LET expression

        if (sequence.getCardinality() == StaticProperty.EXACTLY_ONE && positionVariable == null) {
            LetExpression let = new LetExpression();
            let.setVariableQName(variableName);
            let.setRequiredType(SequenceType.makeSequenceType(
                    sequence.getItemType(visitor.getConfiguration().getTypeHierarchy()),
                    StaticProperty.EXACTLY_ONE));
            let.setSequence(sequence);
            let.setAction(action);
            let.setSlotNumber(slotNumber);
            ExpressionTool.rebindVariableReferences(action, this, let);
            return let.optimize(visitor, contextItemType);
        }

        //declaration = null;     // let the garbage collector take it
        return this;
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
     * Extract subexpressions in the action part that don't depend on the range variable
     * @param visitor the expression visitor
     * @param contextItemType the item type of the context item
     * @return the optimized expression if it has changed, or null if no optimization was possible
     */

    private Expression extractLoopInvariants(ExpressionVisitor visitor, ItemType contextItemType) throws XPathException {
        // Extract subexpressions that don't depend on the range variable or the position variable
        // If a subexpression is (or might be) creative, this is, if it creates new nodes, we don't
        // extract it from the loop, but we do extract its non-creative subexpressions

        //if (positionVariable == null) {
            PromotionOffer offer = new PromotionOffer(visitor.getConfiguration().getOptimizer());
            offer.containingExpression = this;
            offer.action = PromotionOffer.RANGE_INDEPENDENT;
            if (positionVariable == null) {
                offer.bindingList = new Binding[] {this};
            } else {
                offer.bindingList = new Binding[] {this, positionVariable};
            }
            action = doPromotion(action, offer);
            if (offer.containingExpression instanceof LetExpression) {
                // a subexpression has been promoted
                //offer.containingExpression.setParentExpression(container);
                // try again: there may be further subexpressions to promote
                offer.containingExpression = visitor.optimize(offer.containingExpression, contextItemType);
            }
            return offer.containingExpression;
        //}
        //return null;

    }

    /**
     * Convert where clause, if possible, to a predicate.
     * @param visitor the expression visitor
     * @param contextItemType the item type of the context item
     * @return the converted expression if modified, or null otherwise
     */

    public Expression convertWhereToPredicate(ExpressionVisitor visitor, ItemType contextItemType) throws XPathException {
        if (Choose.isSingleBranchChoice(action)) {
            final TypeHierarchy th = visitor.getConfiguration().getTypeHierarchy();
            final Optimizer opt = visitor.getConfiguration().getOptimizer();
            Expression head = null;
            Expression selection = sequence;
            ItemType selectionContextItemType = contextItemType;
            if (sequence instanceof PathExpression) {
                if (((PathExpression)sequence).isAbsolute(th)) {
                    head = ((PathExpression)sequence).getFirstStep();
                    selection = ((PathExpression)sequence).getRemainingSteps();
                    selectionContextItemType = head.getItemType(th);
                } else {
                    PathExpression p = ((PathExpression)sequence).tryToMakeAbsolute(th);
                    if (p != null) {
                        sequence = p;
                        adoptChildExpression(p);
                        head = ((PathExpression)sequence).getFirstStep();
                        selection = ((PathExpression)sequence).getRemainingSteps();
                        selectionContextItemType = head.getItemType(th);
                    }
                }
            }

            boolean changed = false;
            Expression condition = ((Choose)action).getConditions()[0];
            List list = new ArrayList(4);
            BooleanExpression.listAndComponents(condition, list);
            for (int t=list.size()-1; t>=0; t--) {
                // Process each term in the where clause independently
                Expression term = (Expression)list.get(t);

                // TODO: following code should be generalized so it works on any kind of expression.

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
                        Binding[] thisVar = {this};
                        if (positionVariable != null && operands[op] instanceof VariableReference && !changed) {
                            List varRefs = new ArrayList();
                            ExpressionTool.gatherVariableReferences(action, positionVariable, varRefs);
                            if (varRefs.size() == 1 && varRefs.get(0) == operands[op] &&
                                    (operands[1-op].getDependencies() & StaticProperty.DEPENDS_ON_FOCUS) == 0 &&
                                    !ExpressionTool.dependsOnVariable(operands[1-op], thisVar)) {
                                FunctionCall position =
                                        SystemFunction.makeSystemFunction("position", SimpleExpression.NO_ARGUMENTS);
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
                                ExpressionTool.copyLocationInfo(this, selection);
                                selection = visitor.typeCheck(selection, selectionContextItemType);
                                //action = condAction.getThenExpression();
                                positionVariable = null;
                                //positionBinding = null;
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

                        if ( positionVariable == null &&
                                opt.isVariableReplaceableByDot(term, thisVar) &&
                                (term.getDependencies() & StaticProperty.DEPENDS_ON_FOCUS) == 0 &&
                                ExpressionTool.dependsOnVariable(operands[op], thisVar) &&
                                !ExpressionTool.dependsOnVariable(operands[1-op], thisVar)) {
                            PromotionOffer offer = new PromotionOffer(visitor.getConfiguration().getOptimizer());
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
                                predicate = visitor.typeCheck(predicate, sequence.getItemType(th));
                                selection = new FilterExpression(selection, predicate);
                                ExpressionTool.copyLocationInfo(this, selection);
                                selection = visitor.typeCheck(selection, selectionContextItemType);
                                changed = true;
                                positionVariable = null;
                                //positionBinding = null;
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
                                opt.isVariableReplaceableByDot(term, thisVar) &&
                                (term.getDependencies() & StaticProperty.DEPENDS_ON_FOCUS) == 0 &&
                                ExpressionTool.dependsOnVariable(operands[op], thisVar) &&
                                !ExpressionTool.dependsOnVariable(operands[1-op], thisVar)) {
                            PromotionOffer offer = new PromotionOffer(visitor.getConfiguration().getOptimizer());
                            offer.action = PromotionOffer.INLINE_VARIABLE_REFERENCES;
                            offer.bindingList = thisVar;
                            offer.containingExpression = new ContextItemExpression();
                            Expression newOperand = operands[op].promote(offer);
                            if (newOperand != null && !ExpressionTool.dependsOnVariable(newOperand, thisVar)) {
                                //newOperand.resetStaticProperties();
                                Expression predicate;
                                //newOperand = new Atomizer(newOperand, env.getConfiguration());
                                if (op==0) {
                                    // TODO: make GeneralComparisonSA where appropriate
                                    predicate = new GeneralComparison(newOperand, comp.getOperator(), operands[1]);
                                } else {
                                    predicate = new GeneralComparison(operands[0], comp.getOperator(), newOperand);
                                }
                                selection = new FilterExpression(selection, predicate);
                                ExpressionTool.copyLocationInfo(this, selection);
                                selection = visitor.typeCheck(selection, selectionContextItemType);
                                selection = selection.optimize(visitor, selectionContextItemType);
                                visitor.resetStaticProperties();
                                //action = condAction.getThenExpression();
                                positionVariable = null;
                                //positionBinding = null;
                                //return simplify(env).typeCheck(env, contextItemType).optimize(opt, env, contextItemType);
                                list.remove(t);
                                changed = true;
                                break;
                            }
                        }
                    }
                } else if (term instanceof QuantifiedExpression) {
                    QuantifiedExpression q0 = (QuantifiedExpression)term;
                    Expression sequence = q0.getSequence();
                    Expression action = q0.getAction();
                    Binding[] thisVar = {this};
                    if (positionVariable == null &&
                            opt.isVariableReplaceableByDot(term, thisVar) &&
                            (term.getDependencies() & StaticProperty.DEPENDS_ON_FOCUS) == 0 &&
                            ExpressionTool.dependsOnVariable(sequence, thisVar) &&
                            !ExpressionTool.dependsOnVariable(action, thisVar)) {
                        PromotionOffer offer = new PromotionOffer(visitor.getConfiguration().getOptimizer());
                        offer.action = PromotionOffer.INLINE_VARIABLE_REFERENCES;
                        offer.bindingList = thisVar;
                        offer.containingExpression = new ContextItemExpression();
                        Expression newSequence = sequence.promote(offer);
                        if (newSequence != null) {
                            if (ExpressionTool.dependsOnVariable(newSequence, thisVar)) {
                                throw new IllegalStateException("We have a problem...");
                                // we've partially rewritten the expression but we haven't got rid of all
                                // dependencies on the variable. We can't go back and we can't go forward...
                            }
                            q0.setSequence(newSequence);
                            selection = new FilterExpression(selection, q0);
                            ExpressionTool.copyLocationInfo(this, selection);
                            selection = visitor.typeCheck(selection, selectionContextItemType);
                            selection = selection.optimize(visitor, selectionContextItemType);
                            visitor.resetStaticProperties();
                            positionVariable = null;
                            list.remove(t);
                            changed = true;
                            break;
                        }
                    }

                }
            }
            if (changed) {
                if (list.isEmpty()) {
                    action = ((Choose)action).getActions()[0];
                    adoptChildExpression(action);
                } else {
                    Expression term = (Expression)list.get(0);
                    for (int t=1; t<list.size(); t++) {
                        term = new BooleanExpression(term, Token.AND, (Expression)list.get(t));
                    }
                    ((Choose)action).getConditions()[0] = term;
                }
                if (head == null) {
                    sequence = selection;
                } else if (head instanceof RootExpression && selection instanceof KeyFn) {
                    sequence = selection;
                } else {
                    PathExpression path = new PathExpression(head, selection);
                    ExpressionTool.copyLocationInfo(this, path);
                    Expression k = visitor.getConfiguration().getOptimizer().convertPathExpressionToKey(path, visitor);
                    if (k == null) {
                        sequence = path;
                    } else {
                        sequence = k;
                    }
                    sequence = visitor.optimize(visitor.typeCheck(visitor.simplify(sequence), contextItemType), contextItemType);
                    adoptChildExpression(sequence);
                }
                return this;
            }
        }
        return null;
    }

    /**
     * Copy an expression. This makes a deep copy.
     * @return the copy of the original expression
     */

    public Expression copy() {
        if (positionVariable != null) {
            throw new UnsupportedOperationException("copy");
        }

        ForExpression forExp = new ForExpression();
        forExp.setRequiredType(requiredType);
        forExp.setVariableQName(variableName);
        forExp.setSequence(sequence.copy());
        Expression newAction = action.copy();
        forExp.setAction(newAction);
        forExp.variableName = variableName;
        ExpressionTool.rebindVariableReferences(newAction, this, forExp);
        return forExp;
    }

    /**
     * Mark tail function calls: only possible if the for expression iterates zero or one times.
     * (This arises in XSLT/XPath, which does not have a LET expression, so FOR gets used instead)
     */

    public int markTailFunctionCalls(StructuredQName qName, int arity) {
        if (!Cardinality.allowsMany(sequence.getCardinality())) {
            return ExpressionTool.markTailFunctionCalls(action, qName, arity);
        } else {
            return 0;
        }
    }

    /**
     * Extend an array of variable bindings to include the binding(s) defined in this expression
     */

    protected Binding[] extendBindingList(Binding[] in) {
        if (positionVariable == null) {
            return super.extendBindingList(in);
        }
        Binding[] newBindingList = new Binding[in.length+2];
        System.arraycopy(in, 0, newBindingList, 0, in.length);
        newBindingList[in.length] = this;
        newBindingList[in.length+1] = positionVariable;
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
        int pslot = (positionVariable == null ? -1 : positionVariable.getLocalSlotNumber());
        MappingFunction map = new MappingAction(context, getLocalSlotNumber(), pslot, action);
        return new MappingIterator(base, map);
    }

    /**
     * Deliver the result of the expression as a sequence of events.
     * @param context The dynamic evaluation context
     * @return the result of the expression as an iterator over a sequence of PullEvent objects
     * @throws XPathException if a dynamic error occurs during expression evaluation
     */
    
    public EventIterator iterateEvents(XPathContext context) throws XPathException {

        // First create an iteration of the base sequence.

        // Then create an EventMappingIterator which applies a mapping function to each
        // item in the base sequence. The mapping function is essentially the "return"
        // expression, wrapped in an EventMappingAction object that is responsible also for
        // setting the range variable at each step.

        SequenceIterator base = sequence.iterate(context);
        EventMappingFunction map = new EventMappingAction(context, getLocalSlotNumber(), positionVariable, action);
        return new EventMappingIterator(base, map);
    }

    /**
     * Process this expression as an instruction, writing results to the current
     * outputter
     */

    public void process(XPathContext context) throws XPathException {
        SequenceIterator iter = sequence.iterate(context);
        int position = 1;
        int slot = getLocalSlotNumber();
        int pslot = -1;
        if (positionVariable != null) {
            pslot = positionVariable.getLocalSlotNumber();
        }
        while (true) {
            Item item = iter.next();
            if (item == null) break;
            context.setLocalVariable(slot, item);
            if (pslot >= 0) {
                context.setLocalVariable(pslot, Int64Value.makeIntegerValue(position++));
            }
            action.process(context);
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
        SequenceIterator iter = sequence.iterate(context);
        int position = 1;
        int slot = getLocalSlotNumber();
        int pslot = -1;
        if (positionVariable != null) {
            pslot = positionVariable.getLocalSlotNumber();
        }
        while (true) {
            Item item = iter.next();
            if (item == null) break;
            context.setLocalVariable(slot, item);
            if (pslot >= 0) {
                context.setLocalVariable(pslot, Int64Value.makeIntegerValue(position++));
            }
            action.evaluatePendingUpdates(context, pul);
        }
    }

    /**
     * Determine the data type of the items returned by the expression, if possible
     * @return one of the values Type.STRING, Type.BOOLEAN, Type.NUMBER, Type.NODE,
     * or Type.ITEM (meaning not known in advance)
     * @param th the type hierarchy cache
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
     * Diagnostic print of expression structure. The abstract expression tree
     * is written to the supplied output destination.
     */

    public void explain(ExpressionPresenter out) {
        out.startElement("for");
        out.emitAttribute("variable", getVariableName());
        out.emitAttribute("as", sequence.getItemType(out.getTypeHierarchy()).toString(out.getNamePool()));
        if (positionVariable != null) {
            out.emitAttribute("at", positionVariable.getVariableQName().getDisplayName());
        }
        out.startSubsidiaryElement("in");
        sequence.explain(out);
        out.endSubsidiaryElement();
        out.startSubsidiaryElement("return");
        action.explain(out);
        out.endSubsidiaryElement();
        out.endElement();
    }

    /**
     * The MappingAction represents the action to be taken for each item in the
     * source sequence. It acts as the MappingFunction for the mapping iterator, and
     * also as the Binding of the position variable (at $n) in XQuery, if used.
     */

    private static class MappingAction implements StatefulMappingFunction {

        private XPathContext context;
        private int slotNumber;
        private Expression action;
        private int pslot = -1;
        private int position = 1;

        public MappingAction(XPathContext context,
                                int slotNumber,
                                int pslot,
                                Expression action) {
            this.context = context;
            this.slotNumber = slotNumber;
            this.pslot = pslot;
            this.action = action;
        }

        public SequenceIterator map(Item item) throws XPathException {
            context.setLocalVariable(slotNumber, item);
            if (pslot >= 0) {
                context.setLocalVariable(pslot, Int64Value.makeIntegerValue(position++));
            }
            return action.iterate(context);
        }

        public StatefulMappingFunction getAnother() {
            // Create a copy of the stack frame, so that changes made to local variables by the cloned
            // iterator are not seen by the original iterator
            XPathContextMajor c2 = context.newContext();
            StackFrame oldstack = context.getStackFrame();
            ValueRepresentation[] vars = oldstack.getStackFrameValues();
            ValueRepresentation[] newvars = new ValueRepresentation[vars.length];
            System.arraycopy(vars, 0, newvars, 0, vars.length);
            c2.setStackFrame(oldstack.getStackFrameMap(), newvars);
            return new MappingAction(c2, slotNumber, pslot, action);
        }
    }

    /**
     * The EventMappingAction represents the action to be taken for each item in the
     * source sequence. It acts as the EventMappingFunction for the mapping iterator, and
     * also provides the Binding of the position variable (at $n) in XQuery, if used.
     */

    private static class EventMappingAction implements EventMappingFunction {

        private XPathContext context;
        private int slotNumber;
        private Expression action;
        private int position = 1;
        private int pslot = -1;

        public EventMappingAction(XPathContext context,
                                int slotNumber,
                                PositionVariable positionBinding,
                                Expression action) {
            this.context = context;
            this.slotNumber = slotNumber;
            if (positionBinding != null) {
                pslot = positionBinding.getLocalSlotNumber();
            }
            this.action = action;
        }

        public EventIterator map(Item item) throws XPathException {
            context.setLocalVariable(slotNumber, item);
            if (pslot >= 0) {
                context.setLocalVariable(pslot, Int64Value.makeIntegerValue(position++));
            }
            return action.iterateEvents(context);
        }

    }


    /**
     * Get the type of this expression for use in tracing and diagnostics
     * @return the type of expression, as enumerated in class {@link org.orbeon.saxon.trace.Location}
     */

    public int getConstructType() {
        return Location.FOR_EXPRESSION;
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
