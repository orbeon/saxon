package net.sf.saxon.expr;
import net.sf.saxon.om.Item;
import net.sf.saxon.om.NamePool;
import net.sf.saxon.om.SequenceIterator;
import net.sf.saxon.om.ValueRepresentation;
import net.sf.saxon.trace.Location;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.ItemType;
import net.sf.saxon.type.SchemaType;
import net.sf.saxon.value.*;
import net.sf.saxon.functions.SystemFunction;

import java.io.PrintStream;

/**
* A ForExpression maps an expression over a sequence.
* This version works with range variables, it doesn't change the context information
*/

public class ForExpression extends Assignation {

    private transient RangeVariableDeclaration positionVariable = null;
    private PositionBinding positionBinding = null;

    /**
     * Set the reference to the position variable (XQuery only)
     */

    public void setPositionVariable (RangeVariableDeclaration decl) {
        positionVariable = decl;
        positionBinding = new PositionBinding();
    }

    public void setAction(Expression action) {
        super.setAction(action);
        if (positionVariable != null) {
            positionVariable.fixupReferences(positionBinding);
        }
    }

    /**
    * Type-check the expression
    */

    public Expression analyze(StaticContext env, ItemType contextItemType) throws XPathException {

        if (declaration==null) {
            // We've already done the type checking, no need to do it again.
            // But there may be new information about the context item type.
            sequence = sequence.analyze(env, contextItemType);
            action = action.analyze(env, contextItemType);
            return this;
        }

        // The order of events is critical here. First we ensure that the type of the
        // sequence expression is established. This is used to establish the type of the variable,
        // which in turn is required when type-checking the action part.

        sequence = sequence.analyze(env, contextItemType);
        if (sequence instanceof EmptySequence) {
            return EmptySequence.getInstance();
        }

        SequenceType decl = declaration.getRequiredType();
        SequenceType sequenceType = SequenceType.makeSequenceType(
                decl.getPrimaryType(), StaticProperty.ALLOWS_ZERO_OR_MORE);
        RoleLocator role = new RoleLocator(RoleLocator.VARIABLE, new Integer(nameCode), 0, env.getNamePool());
        sequence = TypeChecker.strictTypeCheck(
                                sequence, sequenceType, role, env);
        ItemType actualItemType = sequence.getItemType();
        declaration.refineTypeInformation(actualItemType,
                StaticProperty.EXACTLY_ONE,
                null,
                sequence.getSpecialProperties());

        action = action.analyze(env, contextItemType);

        // See if there is a simple "where" condition that can be turned into a predicate

        if (action instanceof IfExpression && ((IfExpression)action).getElseExpression() instanceof EmptySequence) {
            IfExpression condAction = (IfExpression)action;
            if (condAction.getCondition() instanceof ValueComparison) {
                ValueComparison comp = (ValueComparison)condAction.getCondition();
                Expression[] operands = comp.getOperands();
                for (int op=0; op<2; op++) {

                    // If the where clause is a simple test on the position variable, for example
                    //    for $x at $p in EXPR where $p = 5 return A
                    // then absorb the where condition into a predicate, rewriting it as
                    //    for $x in EXPR[position() = 5] return A
                    // This takes advantage of the optimizations applied to positional filter expressions

                    if (positionVariable != null && positionVariable.getReferenceList().size() == 1) {
                        if (operands[op] instanceof VariableReference &&
                                ((VariableReference)operands[op]).getBinding() == positionBinding &&
                                (operands[1-op].getDependencies() & StaticProperty.DEPENDS_ON_FOCUS) == 0) {
                            FunctionCall position =
                                    SystemFunction.makeSystemFunction("position", 1, env.getNamePool());
                            position.setArguments(SimpleExpression.NO_ARGUMENTS);
                            Expression predicate;
                            if (op==0) {
                                predicate = new ValueComparison(position, comp.getOperator(), operands[1]);
                            } else {
                                predicate = new ValueComparison(operands[0], comp.getOperator(), position);
                            }
                            sequence = new FilterExpression(sequence, predicate, env);
                            action = condAction.getThenExpression();
                            positionVariable = null;
                            positionBinding = null;
                            return simplify(env).analyze(env, contextItemType);
                        }
                    }

                    // If the where clause is a simple test on the value of the range variable, or a path
                    // expression starting with the range variable, then rewrite it as a predicate.
                    // For example, rewrite
                    //    for $x in EXPR where $x/a/b eq "z" return A
                    // as
                    //    for $x in EXPR[a/b eq "z"] return A

                    // TODO: this optimization rarely kicks in, because (a) it doesn't cater for
                    // general comparisons, and (b) the path expression is often wrapped in expressions
                    // that do atomization and type conversion on the result. See for example qxmp214.
                    // (It's not a big time-saver at the moment, but would be if we used indexing for
                    // filter predicates). Need a more general mechanism to replace a variable reference
                    // with a reference to the context item wherever it appears in an expression, so long
                    // as the context is the same as the outermost context.

                    Expression start = operands[op];
                    Expression rest = new ContextItemExpression();
                    if (start instanceof PathExpression) {
                        start = ((PathExpression)start).getFirstStep();
                        rest = ((PathExpression)start).getRemainingSteps();
                    }
                    if (start instanceof VariableReference &&
                            ((VariableReference)start).getBinding() == this &&
                            (operands[1-op].getDependencies() & StaticProperty.DEPENDS_ON_FOCUS) == 0) {
                        Expression predicate;
                            if (op==0) {
                                predicate = new ValueComparison(rest, comp.getOperator(), operands[1]);
                            } else {
                                predicate = new ValueComparison(operands[0], comp.getOperator(), rest);
                            }
                        sequence = new FilterExpression(sequence, predicate, env);
                        action = condAction.getThenExpression();
                        positionVariable = null;
                        positionBinding = null;
                        return simplify(env).analyze(env, contextItemType);
                    }
                }
            }
        }

        // Simplify an expression of the form "for $b in a/b/c return $b/d".
        // (XQuery users seem to write these a lot!)

        if (positionVariable==null && sequence instanceof PathExpression && action instanceof PathExpression) {
            int count = declaration.getReferenceCount(this);
            PathExpression path2 = (PathExpression)action;
            Expression s2 = path2.getStartExpression();
            if (count == 1 && s2 instanceof VariableReference && ((VariableReference)s2).getBinding() == this) {
                PathExpression newPath = new PathExpression(sequence, path2.getStepExpression());
                return newPath.simplify(env).analyze(env, contextItemType);
            }
        }

        // Simplify an expression of the form "for $x in EXPR return $x". These sometimes
        // arise as a result of previous optimization steps.

        if (action instanceof VariableReference && ((VariableReference)action).getBinding() == this) {
            return sequence;
        }

        declaration = null;     // let the garbage collector take it

        // Try to promote any WHERE clause appearing within the FOR expression

        Expression p = promoteWhereClause(positionBinding);
        if (p != null) {
            return p;
        }

        // Extract subexpressions that don't depend on the range variable.
        // We don't do this if there is a position variable. Ideally we would
        // extract subexpressions so long as they don't depend on either variable,
        // but we don't have the machinery to do that yet.
        // TODO: add this optimisation
        // If a subexpression is (or might be) creative, this is, if it creates new nodes, we don't
        // extract it from the loop, but we do extract its non-creative subexpressions

        if (positionVariable == null) {
            PromotionOffer offer = new PromotionOffer();
            offer.containingExpression = this;
            offer.action = PromotionOffer.RANGE_INDEPENDENT;
            Binding[] bindingList = {this};
            offer.bindingList = bindingList;
            Container container = getParentExpression();
            action = action.promote(offer);
            if (offer.containingExpression instanceof LetExpression) {
                // a subexpression has been promoted
                ((ComputedExpression)offer.containingExpression).setParentExpression(container);
                // try again: there may be further subexpressions to promote
                offer.containingExpression = offer.containingExpression.analyze(env, contextItemType);
            }
            return offer.containingExpression;
        }



        return this;
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

        MappingFunction map = new MappingAction(context, slotNumber, positionBinding, action);
        return new MappingIterator(base, map, null, null);
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
                positionBinding.setPosition(position++);
            }
            action.process(context);
        }
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
        int c1 = sequence.getCardinality();
        int c2 = action.getCardinality();
        return Cardinality.multiply(c1, c2);
	}

    /**
    * Diagnostic print of expression structure
    */

    public void display(int level, NamePool pool, PrintStream out) {
        out.println(ExpressionTool.indent(level) +
                "for $" + getVariableName(pool) +
                (positionVariable == null ? "" : " at $?") +
                " in");
        sequence.display(level+1, pool, out);
        out.println(ExpressionTool.indent(level) + "return");
        action.display(level+1, pool, out);
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

        public Object map(Item item, XPathContext c, Object info) throws XPathException {
            context.setLocalVariable(slotNumber, item);
            if (positionBinding != null) {
                positionBinding.setPosition(position++);
            }
            return action.iterate(context);
        }
    }

    /**
     * Get the type of this expression for use in tracing and diagnostics
     * @return the type of expression, as enumerated in class {@link net.sf.saxon.trace.Location}
     */

    protected int getConstructType() {
        return Location.FOR_EXPRESSION;
    }

    /**
     * This class represents the binding of the position variable ("at $p") in an XQuery FOR clause.
     * Note that this variable does not have a slot number. No space is allocated to it in the local
     * stack frame, rather it is maintained as a property of the iterator supporting the containing
     * FOR expression.
     */

    // TODO: does this create a problem with doing lazy evaluation of an expression that depends on the
    // position variable?

    private static class PositionBinding implements Binding {

        private int position;

        private void setPosition(int position) {
            this.position = position;
        }

        /**
         * Indicate whether the binding is local or global. A global binding is one that has a fixed
         * value for the life of a query or transformation; any other binding is local.
         */

        public boolean isGlobal() {
            return false;
        }

        public ValueRepresentation evaluateVariable(XPathContext context) throws XPathException {
            return new IntegerValue(position);
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
