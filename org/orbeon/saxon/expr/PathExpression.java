package net.sf.saxon.expr;

import net.sf.saxon.om.Axis;
import net.sf.saxon.om.Item;
import net.sf.saxon.om.NamePool;
import net.sf.saxon.om.SequenceIterator;
import net.sf.saxon.pattern.AnyNodeTest;
import net.sf.saxon.pattern.NodeKindTest;
import net.sf.saxon.pattern.NodeTest;
import net.sf.saxon.sort.DocumentSorter;
import net.sf.saxon.sort.Reverser;
import net.sf.saxon.trace.Location;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.ItemType;
import net.sf.saxon.type.Type;
import net.sf.saxon.value.Cardinality;
import net.sf.saxon.value.EmptySequence;
import net.sf.saxon.value.SequenceType;

import java.io.PrintStream;
import java.util.Iterator;

/**
 * An expression that establishes a set of nodes by following relationships between nodes
 * in the document. Specifically, it consists of a start expression which defines a set of
 * nodes, and a Step which defines a relationship to be followed from those nodes to create
 * a new set of nodes.
 */

public final class PathExpression extends ComputedExpression implements MappingFunction {

    private Expression start;
    private Expression step;
    private transient int state = 0;    // 0 = raw, 1 = simplified, 2 = analyzed

    /**
     * Constructor
     * @param start A node-set expression denoting the absolute or relative set of nodes from which the
     * navigation path should start.
     * @param step The step to be followed from each node in the start expression to yield a new
     * node-set
     */

    public PathExpression(Expression start, Expression step) {
        this.start = start;
        this.step = step;
        adoptChildExpression(start);
        adoptChildExpression(step);

        // If start is a path expression such as a, and step is b/c, then
        // instead of a/(b/c) we construct (a/b)/c. This is because it often avoids
        // a sort.

        // The "/" operator in XPath 2.0 is not always associative. Problems
        // can occur if position() and last() are used on the rhs, or if node-constructors
        // appear, e.g. //b/../<d/>. So we only do this rewrite if the step is a path
        // expression in which both operands are axis expressions optionally with predicates

        if (step instanceof PathExpression) {
            PathExpression stepPath = (PathExpression) step;
            if (isFilteredAxisPath(stepPath.start) && isFilteredAxisPath(stepPath.step)) {
                this.start = new PathExpression(start, stepPath.start);
                ExpressionTool.copyLocationInfo(start, this.start);
                this.step = stepPath.step;
                resetStaticProperties();
            }
        }
    }

    /**
     * Get the start expression (the left-hand operand)
     */

    public Expression getStartExpression() {
        return start;
    }

    /**
     * Get the step expression (the right-hand operand)
     */

    public Expression getStepExpression() {
        return step;
    }

    /**
     * Determine whether an operand of a PathExpression is an
     * axis step with optional filter predicates.
     */

    private static boolean isFilteredAxisPath(Expression exp) {
        if (exp instanceof AxisExpression) {
            return true;
        } else {
            while (exp instanceof FilterExpression) {
                exp = ((FilterExpression) exp).getBaseExpression();
            }
            return exp instanceof AxisExpression;
        }
    }

    /**
     * Determine the data type of the items returned by this exprssion
     * @return the type of the step
     */

    public final ItemType getItemType() {
        return step.getItemType();
    }

    /**
     * Simplify an expression
     * @return the simplified expression
     */

     public Expression simplify(StaticContext env) throws XPathException {
        if (state > 0) return this;
        state = 1;
        start = start.simplify(env);
        step = step.simplify(env);
        resetStaticProperties();

        // if the start expression is an empty sequence, then the whole PathExpression is empty
        if (start instanceof EmptySequence) {
            return start;
        }

        // if the simplified Step is an empty sequence, then the whole PathExpression is empty
        if (step instanceof EmptySequence) {
            return step;
        }

        // Remove a redundant "." from the path
        // Note: we are careful not to do this unless the other operand is an ordered node-set.
        // In other cases, ./E (or E/.) is not a no-op, because it forces sorting.

        if (start instanceof ContextItemExpression) {
            if (step instanceof PathExpression || (step.getSpecialProperties() & StaticProperty.ORDERED_NODESET) != 0) {
                return step;
            }
        }

        if (step instanceof ContextItemExpression &&
                (start instanceof PathExpression || (start.getSpecialProperties() & StaticProperty.ORDERED_NODESET) != 0)) {
            return start;
        }

        // Remove a redundant "." in the middle of a path expression

        if (step instanceof PathExpression && ((PathExpression)step).start instanceof ContextItemExpression) {
            return new PathExpression(start, ((PathExpression)step).step);
        }

        if (start instanceof PathExpression && ((PathExpression)start).step instanceof ContextItemExpression) {
            return new PathExpression(((PathExpression)start).start, step);
        }

        // the expression /.. is sometimes used to represent the empty node-set

        if (start instanceof RootExpression && step instanceof ParentNodeExpression) {
            return EmptySequence.getInstance();
        }

        return this;
    }

    // Simplify an expression of the form a//b, where b has no positional filters.
    // This comes out of the contructor above as (a/descendent-or-self::node())/child::b,
    // but it is equivalent to a/descendant::b; and the latter is better as it
    // doesn't require sorting

    private PathExpression simplifyDescendantPath(StaticContext env) throws XPathException {

        Expression st = start;

        // detect .//x as a special case; this will appear as descendant-or-self::node()/x

        if (start instanceof AxisExpression) {
            AxisExpression stax = (AxisExpression) start;
            if (stax.getAxis() != Axis.DESCENDANT_OR_SELF) {
                return null;
            }
            ContextItemExpression cie = new ContextItemExpression();
            ExpressionTool.copyLocationInfo(this, cie);
            st = new PathExpression(cie, stax);
            ExpressionTool.copyLocationInfo(this, st);
        }

        if (!(st instanceof PathExpression)) {
            return null;
        }

        PathExpression startPath = (PathExpression) st;
        if (!(startPath.step instanceof AxisExpression)) {
            return null;
        }

        AxisExpression mid = (AxisExpression) startPath.step;
        if (mid.getAxis() != Axis.DESCENDANT_OR_SELF) {
            return null;
        }


        NodeTest test = mid.getNodeTest();
        if (!(test == null || test instanceof AnyNodeTest)) {
            return null;
        }

        Expression underlyingStep = step;
        while (underlyingStep instanceof FilterExpression) {
            if (((FilterExpression) underlyingStep).isPositional()) {
                return null;
            }
            underlyingStep = ((FilterExpression) underlyingStep).getBaseExpression();
        }

        if (!(underlyingStep instanceof AxisExpression)) {
            return null;
        }

        AxisExpression underlyingAxis = (AxisExpression) underlyingStep;
        if (underlyingAxis.getAxis() == Axis.CHILD) {

            ComputedExpression newStep =
                    new AxisExpression(Axis.DESCENDANT,
                            ((AxisExpression) underlyingStep).getNodeTest());
            ExpressionTool.copyLocationInfo(this, newStep);

            underlyingStep = step;
            while (underlyingStep instanceof FilterExpression) {
                // Add any filters to the new expression. We know they aren't
                // positional, so the order of the filters doesn't matter.
                newStep = new FilterExpression(newStep,
                        ((FilterExpression) underlyingStep).getFilter(), env);
                ExpressionTool.copyLocationInfo(underlyingStep, newStep);
                underlyingStep = ((FilterExpression) underlyingStep).getBaseExpression();
            }

            //System.err.println("Simplified this:");
            //    display(10);
            //System.err.println("as this:");
            //    new PathExpression(startPath.start, newStep).display(10);

            PathExpression newPath = new PathExpression(startPath.start, newStep);
            ExpressionTool.copyLocationInfo(this, newPath);
            return newPath;
        }

        if (underlyingAxis.getAxis() == Axis.ATTRIBUTE) {

            // turn the expression a//@b into a/descendant-or-self::*/@b

            ComputedExpression newStep =
                    new AxisExpression(Axis.DESCENDANT_OR_SELF, NodeKindTest.ELEMENT);
            ExpressionTool.copyLocationInfo(this, newStep);

            PathExpression newPath = new PathExpression(
                    new PathExpression(startPath.start, newStep),
                    step);
            ExpressionTool.copyLocationInfo(this, newPath);
            return newPath;
        }

        return null;
    }

    /**
     * Type-check the expression
     */

    public Expression analyze(StaticContext env, ItemType contextItemType) throws XPathException {
        if (state > 1) {
            // we've already done the main analysis, and we don't want to do it again because
            // decisions on sorting get upset. But we have new information, namely the contextItemType,
            // so we use that to check that it's a node
            Expression start2 = start.analyze(env, contextItemType);
            if (start2 != start) {
                adoptChildExpression(start2);
                start = start2;
            }
            Expression step2 = step.analyze(env, start.getItemType());
            if (step2 != step) {
                adoptChildExpression(step2);
                step = step2;
            }
            return this;
        };
        state = 2;

        start = start.analyze(env, contextItemType);
        Expression start2 = start.analyze(env, contextItemType);
        if (start2 != start) {
            adoptChildExpression(start2);
            start = start2;
        }
        Expression step2 = step.analyze(env, start.getItemType());
        if (step2 != step) {
            adoptChildExpression(step2);
            step = step2;
        }

        // The first operand must be of type node()*

        RoleLocator role0 =
                new RoleLocator(RoleLocator.BINARY_EXPR, "/", 0, null);
        role0.setErrorCode("XP0019");
        start2 = TypeChecker.staticTypeCheck(start,
                SequenceType.NODE_SEQUENCE,
                false, role0, env);
        if (start2 != start) {
            adoptChildExpression(start2);
            start = start2;
        }

        // If any subexpressions within the step are not dependent on the focus,
        // and if they are not "creative" expressions (expressions that can create new nodes), then
        // promote them: this causes them to be evaluated once, outside the path expression


        PromotionOffer offer = new PromotionOffer();
        offer.action = PromotionOffer.FOCUS_INDEPENDENT;
        offer.promoteDocumentDependent = (start.getSpecialProperties() & StaticProperty.CONTEXT_DOCUMENT_NODESET) != 0;
        offer.containingExpression = this;
        
        //if ((step.getSpecialProperties() & StaticProperty.NON_CREATIVE) != 0) {
            step2 = step.promote(offer);
            if (step2 != step) {
                adoptChildExpression(step2);
                step = step2;
            }
            resetStaticProperties();
            if (offer.containingExpression != this) {
                state = 0;  // allow reanalysis (see test axes286)
                offer.containingExpression = offer.containingExpression.analyze(env, contextItemType);
                return offer.containingExpression;
            }
        //}

        // We distinguish three cases for the second operand: either it is known statically to deliver
        // nodes only (a traditional path expression), or it is known statically to deliver atomic values
        // only (a simple mapping expression), or we don't yet know.

        ItemType stepType = step.getItemType();
        if (Type.isSubType(stepType, Type.NODE_TYPE)) {

            if ((step.getSpecialProperties() & StaticProperty.NON_CREATIVE) != 0) {

                // A traditional path expression

                // We don't need the operands to be sorted; any sorting that's needed
                // will be done at the top level

                start2 = ExpressionTool.unsorted(start, false);
                if (start2 != start) {
                    adoptChildExpression(start2);
                    start = start2;
                }
                step2 = ExpressionTool.unsorted(step, false);
                if (step2 != step) {
                    adoptChildExpression(step2);
                    step = step2;
                }


//            *** this check is unnecessary since we've already established that the step delivers nodes ***
//            RoleLocator role1 =
//                    new RoleLocator(RoleLocator.BINARY_EXPR, "/", 1, null);
//            role1.setErrorCode("XP0019");
//            step = TypeChecker.staticTypeCheck(step,
//                    SequenceType.NODE_SEQUENCE,
//                    false, role1, env);
//            resetStaticProperties();

            // Try to simplify expressions such as a//b
                PathExpression p = simplifyDescendantPath(env);
                if (p != null) {
                    return p.simplify(env).analyze(env, contextItemType);
                }
            }

            // Decide whether the result needs to be wrapped in a sorting
            // expression to deliver the results in document order

            //if (offer.containingExpression instanceof PathExpression) {
                PathExpression path = (PathExpression) offer.containingExpression;
                int props = path.getSpecialProperties();

                if ((props & StaticProperty.ORDERED_NODESET) != 0) {
                    return path;
                } else if ((props & StaticProperty.REVERSE_DOCUMENT_ORDER) != 0) {
                    return new Reverser(path);
                } else {
                    return new DocumentSorter(path);
                }
            //} else {
            //    return offer.containingExpression;
            //}

        } else if (Type.isSubType(stepType, Type.ANY_ATOMIC_TYPE)) {
            // This is a simple mapping expression: a/b where b returns atomic values
            return new SimpleMappingExpression(start, step, false).simplify(env).analyze(env, contextItemType);
        } else {
            // This is a hybrid mapping expression, one where we don't know the type of the step
            // (and therefore, we don't know whether sorting into document order is required) until run-time
            return new SimpleMappingExpression(start, step, true).simplify(env).analyze(env, contextItemType);
        }
    }

    /**
     * Promote this expression if possible
     */

    public Expression promote(PromotionOffer offer) throws XPathException {
        Expression exp = offer.accept(this);
        if (exp != null) {
            return exp;
        } else {
            Expression start2 = start.promote(offer);
            Expression step2 = step;
            if (offer.action == PromotionOffer.INLINE_VARIABLE_REFERENCES) {
                // Don't pass on other requests. We could pass them on, but only after augmenting
                // them to say we are interested in subexpressions that don't depend on either the
                // outer context or the inner context.
                step2 = step.promote(offer);
            }
            if (start2 != start || step2 != step) {
                resetStaticProperties();
                start = start2;
                step = step2;
            }
            return this;
        }
    }

    /**
     * Get the immediate subexpressions of this expression
     */

    public Iterator iterateSubExpressions() {
        return new PairIterator(start, step);
    }
    /**
     * Determine which aspects of the context the expression depends on. The result is
     * a bitwise-or'ed value composed from constants such as XPathContext.VARIABLES and
     * XPathContext.CURRENT_NODE
     */

    public int computeDependencies() {
        return start.getDependencies() |
                // not all dependencies in the step matter, because the context node, etc,
                // are not those of the outer expression
                (step.getDependencies() &
                (StaticProperty.DEPENDS_ON_XSLT_CONTEXT));
    }

    /**
     * Get the static properties of this expression (other than its type). The result is
     * bit-signficant. These properties are used for optimizations. In general, if
     * property bit is set, it is true, but if it is unset, the value is unknown.
     */

    public int computeSpecialProperties() {
        int startProperties = start.getSpecialProperties();
        int stepProperties = step.getSpecialProperties();

        int p = 0;
        if (!Cardinality.allowsMany(start.getCardinality())) {
            startProperties |= StaticProperty.ORDERED_NODESET | StaticProperty.PEER_NODESET;
        }
        if (!Cardinality.allowsMany(step.getCardinality())) {
            stepProperties |= StaticProperty.ORDERED_NODESET | StaticProperty.PEER_NODESET;
        }


        if ((startProperties & stepProperties & StaticProperty.CONTEXT_DOCUMENT_NODESET) != 0) {
            p |= StaticProperty.CONTEXT_DOCUMENT_NODESET;
        }
        if (((startProperties & StaticProperty.SINGLE_DOCUMENT_NODESET) != 0) &&
            ((stepProperties & StaticProperty.CONTEXT_DOCUMENT_NODESET) != 0)) {
            p |= StaticProperty.SINGLE_DOCUMENT_NODESET;
        }
        if ((startProperties & stepProperties & StaticProperty.PEER_NODESET) != 0) {
            p |= StaticProperty.PEER_NODESET;
        }
        if ((startProperties & stepProperties & StaticProperty.SUBTREE_NODESET) != 0) {
            p |= StaticProperty.SUBTREE_NODESET;
        }

        if (testNaturallySorted(startProperties, stepProperties)) {
            p |= StaticProperty.ORDERED_NODESET;
        }

        if (testNaturallyReverseSorted()) {
            p |= StaticProperty.REVERSE_DOCUMENT_ORDER;
        }

        if ((startProperties & stepProperties & StaticProperty.NON_CREATIVE) != 0) {
            p |= StaticProperty.NON_CREATIVE;
        }

        return p;
    }

    /**
     * Determine if we can guarantee that the nodes are delivered in document order.
     * This is true if the start nodes are sorted peer nodes
     * and the step is based on an Axis within the subtree rooted at each node.
     * It is also true if the start is a singleton node and the axis is sorted.
     */

    private boolean testNaturallySorted(int startProperties, int stepProperties) {

        // System.err.println("**** Testing pathExpression.isNaturallySorted()");
        // display(20);
        // System.err.println("Start is ordered node-set? " + start.isOrderedNodeSet());
        // System.err.println("Start is naturally sorted? " + start.isNaturallySorted());
        // System.err.println("Start is singleton? " + start.isSingleton());

        if ((stepProperties & StaticProperty.ORDERED_NODESET) == 0) {
            return false;
        }
        if (Cardinality.allowsMany(start.getCardinality())) {
            if ((startProperties & StaticProperty.ORDERED_NODESET) == 0) {
                return false;
            }
        } else {
            if ((stepProperties & StaticProperty.ORDERED_NODESET) != 0) {
                return true;
            }
        }

        // We know now that both the start and the step are sorted. But this does
        // not necessarily mean that the combination is sorted.

        // The result is sorted if the start is sorted and the step selects attributes
        // or namespaces

        if ((stepProperties & StaticProperty.ATTRIBUTE_NS_NODESET) != 0) {
            return true;
        }

        // The result is sorted if the start selects "peer nodes" (that is, a node-set in which
        // no node is an ancestor of another) and the step selects within the subtree rooted
        // at the context node

        if (((startProperties & StaticProperty.PEER_NODESET) != 0) && ((stepProperties & StaticProperty.SUBTREE_NODESET) != 0)) {
            return true;
        }

        return false;
    }

    /**
     * Determine if the path expression naturally returns nodes in reverse document order
     */

    private boolean testNaturallyReverseSorted() {

        // Some examples of expressions that are naturally reverse sorted:
        //     ../@x
        //     ancestor::*[@lang]
        //     ../preceding-sibling::x
        //     $x[1]/preceding-sibling::node()

        // This information is used to do a simple reversal of the nodes
        // instead of a full sort, which is significantly cheaper, especially
        // when using tree models (such as DOM and JDOM) in which comparing
        // nodes in document order is an expensive operation.


        if (!Cardinality.allowsMany(start.getCardinality()) &&
                (step instanceof AxisExpression)) {
            return !Axis.isForwards[((AxisExpression) step).getAxis()];
        }

        if (!(start instanceof AxisExpression)) {
            return false;
        }

        if (Axis.isForwards[((AxisExpression) start).getAxis()]) {
            return false;
        }

//        if (step instanceof AttributeReference) {
//            return true;
//        }

        return false;
    }

    /**
     * Determine the static cardinality of the expression
     */

    public int computeCardinality() {
        int c1 = start.getCardinality();
        int c2 = step.getCardinality();
        return Cardinality.multiply(c1, c2);
    }

    /**
     * Is this expression the same as another expression?
     */

    public boolean equals(Object other) {
        if (!(other instanceof PathExpression)) {
            return false;
        }
        PathExpression p = (PathExpression) other;
        return (start.equals(p.start) && step.equals(p.step));
    }

    /**
     * get HashCode for comparing two expressions
     */

    public int hashCode() {
        return "PathExpression".hashCode() + start.hashCode() + step.hashCode();
    }

    /**
     * Get the first step in this expression. A path expression A/B/C is represented as (A/B)/C, but
     * the first step is A
     */

    public Expression getFirstStep() {
        if (start instanceof PathExpression) {
            return ((PathExpression) start).getFirstStep();
        } else {
            return start;
        }
    }

    /**
     * Get all steps after the first.
     * This is complicated by the fact that A/B/C is represented as ((A/B)/C; we are required
     * to return B/C
     */

    public Expression getRemainingSteps() {
        if (start instanceof PathExpression) {
            PathExpression rem =
                    new PathExpression(((PathExpression) start).getRemainingSteps(), step);
            ExpressionTool.copyLocationInfo(start, rem);
            return rem;
        } else {
            return step;
        }
    }

    /**
     * Iterate the path-expression in a given context
     * @param context the evaluation context
     */

    public SequenceIterator iterate(XPathContext context) throws XPathException {

        // This class delivers the result of the path expression in unsorted order,
        // without removal of duplicates. If sorting and deduplication are needed,
        // this is achieved by wrapping the path expression in a DocumentSorter

        SequenceIterator master = start.iterate(context);
        XPathContext context2 = context.newMinorContext();
        context2.setCurrentIterator(master);
        context2.setOriginatingConstructType(Location.PATH_EXPRESSION);

        master = new MappingIterator(master, this, context2, null);
        return master;

    }

    /**
     * Mapping function, from a node returned by the start iteration, to a sequence
     * returned by the child.
     */

    public Object map(Item item, XPathContext context, Object info) throws XPathException {
        return step.iterate(context);
    }

    /**
     * Diagnostic print of expression structure
     */

    public void display(int level, NamePool pool, PrintStream out) {
        out.println(ExpressionTool.indent(level) + "path /");
        start.display(level + 1, pool, out);
        step.display(level + 1, pool, out);
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
// The Initial Developer of the Original Code is Michael H. Kay.
//
// Portions created by (your name) are Copyright (C) (your legal entity). All Rights Reserved.
//
// Contributor(s): none.
//
