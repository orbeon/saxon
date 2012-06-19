package org.orbeon.saxon.instruct;
import org.orbeon.saxon.Controller;
import org.orbeon.saxon.expr.*;
import org.orbeon.saxon.om.Item;
import org.orbeon.saxon.om.SequenceIterator;
import org.orbeon.saxon.om.StandardNames;
import org.orbeon.saxon.trace.ExpressionPresenter;
import org.orbeon.saxon.trace.TraceListener;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.type.ItemType;
import org.orbeon.saxon.type.SchemaType;
import org.orbeon.saxon.type.TypeHierarchy;

import java.util.Iterator;


/**
* Handler for xsl:for-each elements in a stylesheet.
*/

public class ForEach extends Instruction implements ContextMappingFunction {

    private Expression select;
    private Expression action;
    private boolean containsTailCall;

    /**
     * Create an xsl:for-each instruction
     * @param select the select expression
     * @param action the body of the xsl:for-each loop
     */

    public ForEach(Expression select, Expression action, boolean containsTailCall) {
        this.select = select;
        this.action = action;
        this.containsTailCall = containsTailCall && action instanceof TailCallReturner;
        adoptChildExpression(select);
        adoptChildExpression(action);
    }

    /**
     * Get the name of this instruction for diagnostic and tracing purposes
     * @return the code for name xsl:for-each
    */

    public int getInstructionNameCode() {
        return StandardNames.XSL_FOR_EACH;
    }

    /**
     * Get the action expression (the content of the for-each)
     * @return the body of the for-each loop
     */

    public Expression getActionExpression() {
        return action;
    }

    /**
     * Determine the data type of the items returned by this expression
     * @return the data type
     * @param th the type hierarchy cache
     */

    public final ItemType getItemType(TypeHierarchy th) {
        return action.getItemType(th);
    }

    /**
     * Determine whether this instruction creates new nodes.
     * This implementation returns true if the "action" creates new nodes.
     * (Nodes created by the condition can't contribute to the result).
     */

    public final boolean createsNewNodes() {
        int props = action.getSpecialProperties();
        return ((props & StaticProperty.NON_CREATIVE) == 0);
    }

    /**
     * Simplify an expression. This performs any static optimization (by rewriting the expression
     * as a different expression).
     *
     * @exception XPathException if an error is discovered during expression
     *     rewriting
     * @return the simplified expression
     * @param visitor the expression visitor
     */

    public Expression simplify(ExpressionVisitor visitor) throws XPathException {
        select = visitor.simplify(select);
        action = visitor.simplify(action);
        return this;
    }

    public Expression typeCheck(ExpressionVisitor visitor, ItemType contextItemType) throws XPathException {
        final TypeHierarchy th = visitor.getConfiguration().getTypeHierarchy();
        select = visitor.typeCheck(select, contextItemType);
        adoptChildExpression(select);
        action = visitor.typeCheck(action, select.getItemType(th));
        adoptChildExpression(action);
        if (Literal.isEmptySequence(select)) {
            return select;
        }
        if (Literal.isEmptySequence(action)) {
            return action;
        }
        return this;
    }

    public Expression optimize(ExpressionVisitor visitor, ItemType contextItemType) throws XPathException {
        final TypeHierarchy th = visitor.getConfiguration().getTypeHierarchy();
        select = visitor.optimize(select, contextItemType);
        adoptChildExpression(select);
        action = action.optimize(visitor, select.getItemType(th));
        adoptChildExpression(action);
        if (Literal.isEmptySequence(select)) {
            return select;
        }
        if (Literal.isEmptySequence(action)) {
            return action;
        }

        // If any subexpressions within the body of the for-each are not dependent on the focus,
        // promote them: this causes them to be evaluated once, outside the for-each loop

        PromotionOffer offer = new PromotionOffer(visitor.getConfiguration().getOptimizer());
        offer.action = PromotionOffer.FOCUS_INDEPENDENT;
        offer.promoteDocumentDependent = (select.getSpecialProperties() & StaticProperty.CONTEXT_DOCUMENT_NODESET) != 0;
        offer.promoteXSLTFunctions = false;
        offer.containingExpression = this;
        action = doPromotion(action, offer);

        if (offer.containingExpression instanceof LetExpression) {
            offer.containingExpression =
                    visitor.optimize(offer.containingExpression, contextItemType);
        }
        return offer.containingExpression;
    }


    /**
     * Add a representation of this expression to a PathMap. The PathMap captures a map of the nodes visited
     * by an expression in a source tree.
     * <p/>
     * <p>The default implementation of this method assumes that an expression does no navigation other than
     * the navigation done by evaluating its subexpressions, and that the subexpressions are evaluated in the
     * same context as the containing expression. The method must be overridden for any expression
     * where these assumptions do not hold. For example, implementations exist for AxisExpression, ParentExpression,
     * and RootExpression (because they perform navigation), and for the doc(), document(), and collection()
     * functions because they create a new navigation root. Implementations also exist for PathExpression and
     * FilterExpression because they have subexpressions that are evaluated in a different context from the
     * calling expression.</p>
     *
     * @param pathMap     the PathMap to which the expression should be added
     * @param pathMapNodeSet the set of nodes in the path map that are affected
     * @return the pathMapNode representing the focus established by this expression, in the case where this
     *         expression is the first operand of a path expression or filter expression. For an expression that does
     *         navigation, it represents the end of the arc in the path map that describes the navigation route. For other
     *         expressions, it is the same as the input pathMapNode.
     */

    public PathMap.PathMapNodeSet addToPathMap(PathMap pathMap, PathMap.PathMapNodeSet pathMapNodeSet) {
        PathMap.PathMapNodeSet target = select.addToPathMap(pathMap, pathMapNodeSet);
        return action.addToPathMap(pathMap, target);
    }

    /**
     * Copy an expression. This makes a deep copy.
     *
     * @return the copy of the original expression
     */

    public Expression copy() {
        return new ForEach(select.copy(), action.copy(), containsTailCall);
    }

    /**
     * Compute the dependencies of an expression, as the union of the
     * dependencies of its subexpressions. (This is overridden for path expressions
     * and filter expressions, where the dependencies of a subexpression are not all
     * propogated). This method should be called only once, to compute the dependencies;
     * after that, getDependencies should be used.
     *
     * @return the depencies, as a bit-mask
     */

    public int computeDependencies() {
        // Some of the dependencies aren't relevant. Note that the sort keys are absorbed into the select
        // expression.
        int dependencies = 0;
        dependencies |= select.getDependencies();
        dependencies |= (action.getDependencies() & ~StaticProperty.DEPENDS_ON_FOCUS);
        return dependencies;
    }


    /**
     * Handle promotion offers, that is, non-local tree rewrites.
     * @param offer The type of rewrite being offered
     * @throws XPathException
     */

    protected void promoteInst(PromotionOffer offer) throws XPathException {
        select = doPromotion(select, offer);
        action = doPromotion(action, offer);
    }

    /**
     * Get all the XPath expressions associated with this instruction
     * (in XSLT terms, the expression present on attributes of the instruction,
     * as distinct from the child instructions in a sequence construction)
     */

    public Iterator iterateSubExpressions() {
        return new PairIterator(select, action);
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
     * Replace one subexpression by a replacement subexpression
     * @param original the original subexpression
     * @param replacement the replacement subexpression
     * @return true if the original subexpression is found
     */

    public boolean replaceSubExpression(Expression original, Expression replacement) {
        boolean found = false;
        if (select == original) {
            select = replacement;
            found = true;
        }
        if (action == original) {
            action = replacement;
            found = true;
        }
        return found;
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

    public TailCall processLeavingTail(XPathContext context) throws XPathException {
        Controller controller = context.getController();
        SequenceIterator iter = select.iterate(context);

        XPathContextMajor c2 = context.newContext();
        c2.setOrigin(this);
        c2.setCurrentIterator(iter);
        c2.setCurrentTemplateRule(null);

        if (containsTailCall) {
            if (controller.isTracing()) {
                TraceListener listener = controller.getTraceListener();
                Item item = iter.next();
                if (item == null) {
                    return null;
                }
                listener.startCurrentItem(item);
                TailCall tc = ((TailCallReturner)action).processLeavingTail(c2);
                listener.endCurrentItem(item);
                return tc;
            } else {
                Item item = iter.next();
                if (item == null) {
                    return null;
                }
                return ((TailCallReturner)action).processLeavingTail(c2);
            }
        } else {
            if (controller.isTracing()) {
                TraceListener listener = controller.getTraceListener();
                while(true) {
                    Item item = iter.next();
                    if (item == null) {
                        break;
                    }
                    listener.startCurrentItem(item);
                    action.process(c2);
                    listener.endCurrentItem(item);
                }
            } else {
                while(true) {
                    Item item = iter.next();
                    if (item == null) {
                        break;
                    }
                    action.process(c2);
                }
            }
        }
        return null;
    }

    /**
     * Return an Iterator to iterate over the values of the sequence. 
     *
     * @exception XPathException if any dynamic error occurs evaluating the
     *     expression
     * @param context supplies the context for evaluation
     * @return a SequenceIterator that can be used to iterate over the result
     *     of the expression
     */

    public SequenceIterator iterate(XPathContext context) throws XPathException {
        SequenceIterator master = select.iterate(context);
        XPathContextMajor c2 = context.newContext();
        c2.setOrigin(this);
        c2.setCurrentTemplateRule(null);
        c2.setCurrentIterator(master);
        master = new ContextMappingIterator(this, c2);
        return master;
    }

    /**
     * Map one item to a sequence.
     * @param context The processing context. This is supplied only for mapping constructs that
     * set the context node, position, and size. Otherwise it is null.
     * @return either (a) a SequenceIterator over the sequence of items that the supplied input
     * item maps to, or (b) an Item if it maps to a single item, or (c) null if it maps to an empty
     * sequence.
     */

    public SequenceIterator map(XPathContext context) throws XPathException {
        return action.iterate(context);
    }

    /**
     * Diagnostic print of expression structure. The abstract expression tree
     * is written to the supplied output destination.
     */

    public void explain(ExpressionPresenter out) {
        out.startElement("forEach");
        select.explain(out);
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
// The Initial Developer of the Original Code is Michael H. Kay.
//
// Portions created by (your name) are Copyright (C) (your legal entity). All Rights Reserved.
//
// Contributor(s): none.
//
