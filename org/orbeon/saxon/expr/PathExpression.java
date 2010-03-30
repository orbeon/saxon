package org.orbeon.saxon.expr;

import org.orbeon.saxon.functions.SystemFunction;
import org.orbeon.saxon.om.Axis;
import org.orbeon.saxon.om.SequenceIterator;
import org.orbeon.saxon.pattern.AnyNodeTest;
import org.orbeon.saxon.pattern.NodeKindTest;
import org.orbeon.saxon.pattern.NodeTest;
import org.orbeon.saxon.sort.DocumentSorter;
import org.orbeon.saxon.trace.ExpressionPresenter;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.type.ItemType;
import org.orbeon.saxon.type.Type;
import org.orbeon.saxon.type.TypeHierarchy;
import org.orbeon.saxon.value.Cardinality;
import org.orbeon.saxon.value.SequenceType;

/**
 * An expression that establishes a set of nodes by following relationships between nodes
 * in the document. Specifically, it consists of a start expression which defines a set of
 * nodes, and a step which defines a relationship to be followed from those nodes to create
 * a new set of nodes.
 *
 * <p>This class inherits from SlashExpression; it is used in the common case where the SlashExpression
 * is known to return nodes rather than atomic values.</p>
 *
 * <p>This class is not responsible for sorting the results into document order or removing duplicates.
 * That is done by a DocumentSorter expression which is wrapped around the path expression. However, this
 * class does contain the logic for deciding statically whether the DocumentSorter is needed or not.</p>
 */

public final class PathExpression extends SlashExpression implements ContextMappingFunction {

    private transient int state = 0;    // 0 = raw, 1 = simplified, 2 = analyzed, 3 = optimized

    /**
     * Constructor
     * @param start A node-set expression denoting the absolute or relative set of nodes from which the
     * navigation path should start.
     * @param step The step to be followed from each node in the start expression to yield a new
     * node-set
     */

    public PathExpression(Expression start, Expression step) {
        super(start, step);

        // If start is a path expression such as a, and step is b/c, then
        // instead of a/(b/c) we construct (a/b)/c. This is because it often avoids
        // a sort.

        // The "/" operator in XPath 2.0 is not always associative. Problems
        // can occur if position() and last() are used on the rhs, or if node-constructors
        // appear, e.g. //b/../<d/>. So we only do this rewrite if the step is a path
        // expression in which both operands are axis expressions optionally with predicates

        if (step instanceof PathExpression) {
            PathExpression stepPath = (PathExpression) step;
            if (isFilteredAxisPath(stepPath.getStartExpression()) && isFilteredAxisPath(stepPath.getStepExpression())) {
                setStartExpression(new PathExpression(start, stepPath.start));
                setStepExpression(stepPath.step);
            }
        }
    }

    public boolean isHybrid() {
        return false;
    }

    /**
     * Add a document sorting node to the expression tree, if needed
     */

    Expression addDocumentSorter() {
        int props = getSpecialProperties();
        if ((props & StaticProperty.ORDERED_NODESET) != 0) {
            return this;
        } else if ((props & StaticProperty.REVERSE_DOCUMENT_ORDER) != 0) {
            return SystemFunction.makeSystemFunction("reverse", new Expression[]{this});
        } else {
            return new DocumentSorter(this);
        }
    }

    /**
     * Determine whether an expression is an
     * axis step with optional filter predicates.
     * @param exp the expression to be examined
     * @return true if the supplied expression is an AxisExpression, or an AxisExpression wrapped by one
     * or more filter expressions
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
     * Simplify an expression
     * @return the simplified expression
     * @param visitor the expression visitor
     */

     public Expression simplify(ExpressionVisitor visitor) throws XPathException {
        if (state > 0) {
            return this;
        }
        state = 1;

        Expression e2 = super.simplify(visitor);
        if (e2 != this) {
            return e2;
        }

        // Remove a redundant "." from the path
        // TODO: can we move this logic to the superclass? Does it destroy type-checking?

        if (start instanceof ContextItemExpression) {
            return step;
        }

        if (step instanceof ContextItemExpression) {
            return start;
        }

        return this;
    }

    // Simplify an expression of the form a//b, where b has no positional filters.
    // This comes out of the contructor above as (a/descendent-or-self::node())/child::b,
    // but it is equivalent to a/descendant::b; and the latter is better as it
    // doesn't require sorting. Note that we can't do this until type information is available,
    // as we need to know whether any filters are positional or not.

    private PathExpression simplifyDescendantPath(StaticContext env) {

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
            if (((FilterExpression) underlyingStep).isPositional(env.getConfiguration().getTypeHierarchy())) {
                return null;
            }
            underlyingStep = ((FilterExpression) underlyingStep).getBaseExpression();
        }

        if (!(underlyingStep instanceof AxisExpression)) {
            return null;
        }

        AxisExpression underlyingAxis = (AxisExpression) underlyingStep;
        if (underlyingAxis.getAxis() == Axis.CHILD) {

            Expression newStep =
                    new AxisExpression(Axis.DESCENDANT,
                            ((AxisExpression) underlyingStep).getNodeTest());
            ExpressionTool.copyLocationInfo(this, newStep);

            underlyingStep = step;
            while (underlyingStep instanceof FilterExpression) {
                // Add any filters to the new expression. We know they aren't
                // positional, so the order of the filters doesn't matter.
                newStep = new FilterExpression(newStep,
                        ((FilterExpression) underlyingStep).getFilter());
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

            Expression newStep =
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
     * Perform type analysis
     */

    public Expression typeCheck(ExpressionVisitor visitor, ItemType contextItemType) throws XPathException {

        final TypeHierarchy th = visitor.getConfiguration().getTypeHierarchy();
        if (state >= 2) {
            // we've already done the main analysis, and we don't want to do it again because
            // decisions on sorting get upset. But we have new information, namely the contextItemType,
            // so we use that to check that it's a node
            setStartExpression(visitor.typeCheck(start, contextItemType));
            setStepExpression(visitor.typeCheck(step, start.getItemType(th)));
            return this;
        }
        state = 2;

        setStartExpression(visitor.typeCheck(start, contextItemType));

        // The first operand must be of type node()*

        RoleLocator role0 = new RoleLocator(RoleLocator.BINARY_EXPR, "/", 0);
        //role0.setSourceLocator(this);
        role0.setErrorCode("XPTY0019");
        setStartExpression(
                TypeChecker.staticTypeCheck(start, SequenceType.NODE_SEQUENCE, false, role0, visitor));

        // Now check the second operand

        setStepExpression(visitor.typeCheck(step, start.getItemType(th)));

        if ((step.getSpecialProperties() & StaticProperty.NON_CREATIVE) != 0) {

            // A traditional path expression

            // We don't need the operands to be sorted; any sorting that's needed
            // will be done at the top level

            Optimizer opt = visitor.getConfiguration().getOptimizer();
            setStartExpression(ExpressionTool.unsorted(opt, start, false));
            setStepExpression(ExpressionTool.unsorted(opt, step, false));

            // Try to simplify expressions such as a//b
            PathExpression p = simplifyDescendantPath(visitor.getStaticContext());
            if (p != null) {
                ExpressionTool.copyLocationInfo(this, p);
                return visitor.typeCheck(visitor.simplify(p), contextItemType);
            } else {
                // a failed attempt to simplify the expression may corrupt the parent pointers
                adoptChildExpression(start);
                adoptChildExpression(step);
            }
        }
        return this;
    }

    /**
     * Optimize the expression and perform type analysis
     */

    public Expression optimize(ExpressionVisitor visitor, ItemType contextItemType) throws XPathException {

        Optimizer opt = visitor.getConfiguration().getOptimizer();

        // TODO: recognize explosive path expressions such as ..//../..//.. : eliminate duplicates early to contain the size
        // Mainly for benchmarks, but one sees following-sibling::p/preceding-sibling::h2. We could define an expression as
        // explosive if it contains two adjacent steps with opposite directions (except where both are singletons).

        final TypeHierarchy th = visitor.getConfiguration().getTypeHierarchy();
        if (state >= 3) {
            // we've already done the main analysis, and we don't want to do it again because
            // decisions on sorting get upset. But we have new information, namely the contextItemType,
            // so we use that to check that it's a node
            setStartExpression(visitor.optimize(start, contextItemType));
            setStepExpression(step.optimize(visitor, start.getItemType(th)));
            return this;
        }
        state = 3;

        // Rewrite a/b[filter] as (a/b)[filter] to improve the chance of indexing

        Expression lastStep = getLastStep();
        if (lastStep instanceof FilterExpression && !((FilterExpression)lastStep).isPositional(th)) {
            Expression leading = getLeadingSteps();
            Expression p2 = new PathExpression(leading, ((FilterExpression)lastStep).getBaseExpression());
            Expression f2 = new FilterExpression(p2, ((FilterExpression)lastStep).getFilter());
            return f2.optimize(visitor, contextItemType);
        }

        Expression k = opt.convertPathExpressionToKey(this, visitor);
        if (k != null) {
            return k.typeCheck(visitor, contextItemType).optimize(visitor, contextItemType);
        }

        setStartExpression(visitor.optimize(start, contextItemType));
        setStepExpression(step.optimize(visitor, start.getItemType(th)));

        // If any subexpressions within the step are not dependent on the focus,
        // and if they are not "creative" expressions (expressions that can create new nodes), then
        // promote them: this causes them to be evaluated once, outside the path expression

        return promoteFocusIndependentSubexpressions(visitor, contextItemType);

    }


    /**
     * Promote this expression if possible
     */

    public Expression promote(PromotionOffer offer) throws XPathException {
        Expression p = this;
        if (offer.action == PromotionOffer.RANGE_INDEPENDENT) {
            // try converting the expression first from a/b/c[pred] to (a/b/c)[pred] so that a/b/c can be promoted

            final Optimizer optimizer = offer.getOptimizer();
            FilterExpression p2 = optimizer.convertToFilterExpression(
                    this, optimizer.getConfiguration().getTypeHierarchy());
            if (p2 != null) {
                return p2.promote(offer);
            }
        }
        Expression exp = offer.accept(p);
        if (exp != null) {
            return exp;
        } else {
            setStartExpression(doPromotion(start, offer));
            if (offer.action == PromotionOffer.INLINE_VARIABLE_REFERENCES ||
                    offer.action == PromotionOffer.REPLACE_CURRENT) {
                // Don't pass on other requests. We could pass them on, but only after augmenting
                // them to say we are interested in subexpressions that don't depend on either the
                // outer context or the inner context.
                setStepExpression(doPromotion(step, offer));
            }
            return this;
        }
    }




    /**
     * Copy an expression. This makes a deep copy.
     *
     * @return the copy of the original expression
     */

    public Expression copy() {
        return new PathExpression(start.copy(), step.copy());
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
     * @param startProperties the properties of the left-hand expression
     * @param stepProperties the properties of the right-hand expression
     * @return true if the natural nested-loop evaluation strategy for the expression
     * is known to deliver results with no duplicates and in document order, that is,
     * if no additional sort is required
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
            //if ((stepProperties & StaticProperty.ORDERED_NODESET) != 0) {
                return true;
            //}
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

        return ((startProperties & StaticProperty.PEER_NODESET) != 0) &&
                ((stepProperties & StaticProperty.SUBTREE_NODESET) != 0);

    }

    /**
     * Determine if the path expression naturally returns nodes in reverse document order
     * @return true if the natural nested-loop evaluation strategy returns nodes in reverse
     * document order
     */

    private boolean testNaturallyReverseSorted() {

        // Some examples of path expressions that are naturally reverse sorted:
        //     ancestor::*/@x
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

        return !Cardinality.allowsMany(step.getCardinality()) &&
                (start instanceof AxisExpression) &&
                !Axis.isForwards[((AxisExpression)start).getAxis()];

    }

    /**
     * Get the first step in this expression. A path expression A/B/C is represented as (A/B)/C, but
     * the first step is A
     * @return the first step in the expression, after expanding any nested path expressions
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
     * @return a path expression containing all steps in this path expression other than the first,
     * after expanding any nested path expressions
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
     * Get the last step of the path expression
     * @return the last step in the expression, after expanding any nested path expressions
     */

    public Expression getLastStep() {
        if (step instanceof PathExpression) {
            return ((PathExpression)step).getLastStep();
        } else {
            return step;
        }
    }

    /**
     * Get a path expression consisting of all steps except the last
     * @return a path expression containing all steps in this path expression other than the last,
     * after expanding any nested path expressions
     */

    public Expression getLeadingSteps() {
        if (step instanceof PathExpression) {
            PathExpression rem =
                    new PathExpression(start, ((PathExpression) step).getLeadingSteps());
            ExpressionTool.copyLocationInfo(start, rem);
            return rem;
        } else {
            return start;
        }
    }


    /**
     * Test whether a path expression is an absolute path - that is, a path whose first step selects a
     * document node
     * @param th the type hierarchy cache
     * @return true if the first step in this path expression selects a document node
     */

    public boolean isAbsolute(TypeHierarchy th) {
        Expression first = getFirstStep();
        if (first.getItemType(th).getPrimitiveType() == Type.DOCUMENT) {
            return true;
        }
        // This second test allows keys to be built. See XMark q9.
//        if (first instanceof AxisExpression && ((AxisExpression)first).getContextItemType().getPrimitiveType() == Type.DOCUMENT) {
//            return true;
//        };
        return false;
    }

    /**
     * Test whether a path expression is an absolute path - that is, a path whose first step selects a
     * document node; if not, see if it can be converted to an absolute path. This is possible in cases where
     * the path expression has the form a/b/c and it is known that the context item is a document node; in this
     * case it is safe to change the path expression to /a/b/c
     * @param th the type hierarchy cache
     * @return the path expression if it is absolute; the converted path expression if it can be made absolute;
     * or null if neither condition applies.
     */

    public PathExpression tryToMakeAbsolute(TypeHierarchy th) {
        Expression first = getFirstStep();
        if (first.getItemType(th).getPrimitiveType() == Type.DOCUMENT) {
            return this;
        }
        // This second test allows keys to be built. See XMark q9.
        if (first instanceof AxisExpression && ((AxisExpression)first).getContextItemType().getPrimitiveType() == Type.DOCUMENT) {
            RootExpression root = new RootExpression();
            ExpressionTool.copyLocationInfo(this, root);
            PathExpression path = new PathExpression(root, this);
            ExpressionTool.copyLocationInfo(this, path);
            return path;
        }
        return null;
    }


    /**
     * Add a representation of this expression to a PathMap. The PathMap captures a map of the nodes visited
     * by an expression in a source tree.
     *
     * @param pathMap     the PathMap to which the expression should be added
     * @param pathMapNodeSet
     * @return the pathMapNode representing the focus established by this expression, in the case where this
     *         expression is the first operand of a path expression or filter expression
     */

    public PathMap.PathMapNodeSet addToPathMap(PathMap pathMap, PathMap.PathMapNodeSet pathMapNodeSet) {
        // TODO: not clear why this differs from the superclass method
        PathMap.PathMapNodeSet target = start.addToPathMap(pathMap, pathMapNodeSet);
        return step.addToPathMap(pathMap, target);
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
        context2.setOrigin(this);
        //context2.setOriginatingConstructType(Location.PATH_EXPRESSION);

        return new ContextMappingIterator(this, context2);

    }

    /**
     * The toString() method for an expression attempts to give a representation of the expression
     * in an XPath-like form, but there is no guarantee that the syntax will actually be true XPath.
     * In the case of XSLT instructions, the toString() method gives an abstracted view of the syntax
     */

    public String toString() {
        return "(" + start.toString() + "/" + step.toString() + ")";
    }

    /**
     * Diagnostic print of expression structure. The abstract expression tree
     * is written to the supplied output destination.
     */

    public void explain(ExpressionPresenter destination) {
        destination.startElement("path");
        start.explain(destination);
        step.explain(destination);
        destination.endElement();
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
// The Original Code is: all this file
//
// The Initial Developer of the Original Code is Michael H. Kay.
//
// Contributor(s):
//

