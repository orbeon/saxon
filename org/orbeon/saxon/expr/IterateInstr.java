package org.orbeon.saxon.expr;

import org.orbeon.saxon.instruct.Block;
import org.orbeon.saxon.instruct.Instruction;
import org.orbeon.saxon.instruct.TailCall;
import org.orbeon.saxon.instruct.UserFunction;
import org.orbeon.saxon.om.Item;
import org.orbeon.saxon.om.SequenceIterator;
import org.orbeon.saxon.trace.ExpressionPresenter;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.type.ItemType;
import org.orbeon.saxon.type.SchemaType;
import org.orbeon.saxon.type.Type;
import org.orbeon.saxon.type.TypeHierarchy;
import org.orbeon.saxon.value.EmptySequence;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
* A TailCallLoop wraps the body of a function that contains tail-recursive function calls. On completion
 * of the "real" body of the function it tests whether the function has executed a tail call, and if so,
 * iterates to evaluate the tail call.
*/

public final class IterateInstr extends Instruction {

    private Expression select;
    private Expression action;
    private Expression finallyExp;

    /**
     * Create a saxon:iterate instruction
     * @param select the select expression
     * @param action the body of the saxon:iterate loop
     * @param finallyExp the expression to be evaluated before final completion, may be null
     */

    public IterateInstr(Expression select, Expression action, Expression finallyExp) {
        this.select = select;
        this.action = action;
        this.finallyExp = (finallyExp == null ? new Literal(EmptySequence.getInstance()) : finallyExp);
        adoptChildExpression(select);
        adoptChildExpression(action);
        adoptChildExpression(finallyExp);
    }

    /**
     * Get the action expression (the content of the for-each)
     * @return the body of the for-each loop
     */

    public Expression getActionExpression() {
        return action;
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
        finallyExp = visitor.simplify(finallyExp);
        return this;
    }

    /**
    * Type-check the expression
    */

    public Expression typeCheck(ExpressionVisitor visitor, ItemType contextItemType) throws XPathException {
        final TypeHierarchy th = visitor.getConfiguration().getTypeHierarchy();
        select = visitor.typeCheck(select, contextItemType);
        adoptChildExpression(select);
        action = visitor.typeCheck(action, select.getItemType(th));
        adoptChildExpression(action);
        finallyExp = visitor.typeCheck(finallyExp, null);
        adoptChildExpression(finallyExp);
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
        finallyExp = finallyExp.optimize(visitor, select.getItemType(th));
        adoptChildExpression(finallyExp);
        if (Literal.isEmptySequence(select)) {
            return select;
        }
        if (Literal.isEmptySequence(action)) {
            return action;
        }

        return this;
    }


    /**
     * Determine the data type of the items returned by this expression
     * @return the data type
     * @param th the type hierarchy cache
     */

    public final ItemType getItemType(TypeHierarchy th) {
        if (Literal.isEmptySequence(finallyExp)) {
            return action.getItemType(th);
        } else {
            return Type.getCommonSuperType(action.getItemType(th), finallyExp.getItemType(th), th);
        }
    }

    /**
     * Determine whether this instruction creates new nodes.
     * This implementation returns true if the "action" creates new nodes.
     * (Nodes created by the condition can't contribute to the result).
     */

    public final boolean createsNewNodes() {
        return (action.getSpecialProperties() & 
                finallyExp.getSpecialProperties() &
                StaticProperty.NON_CREATIVE) == 0;
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
        dependencies |= (finallyExp.getDependencies() & ~StaticProperty.DEPENDS_ON_FOCUS);
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
        finallyExp = doPromotion(finallyExp, offer);
    }

    /**
     * Get all the XPath expressions associated with this instruction
     * (in XSLT terms, the expression present on attributes of the instruction,
     * as distinct from the child instructions in a sequence construction)
     */

    public Iterator iterateSubExpressions() {
        List sub = new ArrayList(8);
        sub.add(select);
        sub.add(action);
        sub.add(finallyExp);
        return sub.iterator();
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
        if (finallyExp == original) {
            finallyExp = replacement;
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
        return PROCESS_METHOD;
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
     * Copy an expression. This makes a deep copy.
     *
     * @return the copy of the original expression
     */

    public Expression copy() {
        throw new UnsupportedOperationException("copy");
    }


    public TailCall processLeavingTail(XPathContext context) throws XPathException {
        SequenceIterator iter = select.iterate(context);

        XPathContextMajor c2 = context.newContext();
        c2.setOrigin(this);
        c2.setCurrentIterator(iter);
        c2.setCurrentTemplateRule(null);

        // TODO: add tracing as in xsl:for-each
        while (true) {
            Item item = iter.next();
            if (item == null) {
                if (action instanceof Block) {
                    c2.setCurrentIterator(null);
                    ((Block)action).processLocalParams(c2);
                }
                finallyExp.process(context);
                break;
            }
            action.process(c2);
            UserFunction fn = c2.getTailCallFunction();
            if (fn == null) {
                // no saxon:continue or saxon:break was encountered; just loop around
            } else if (fn.getFunctionName().equals(BreakInstr.SAXON_BREAK)) {
                // indicates a saxon:break instruction was encountered: break the loop
                iter.close();
                return null;
            } else {
                // a saxon:continue instruction was encountered.
                // It will have reset the parameters to the loop; we just need to loop round
            }
        }
        return null;
    }



    /**
     * Diagnostic print of expression structure. The abstract expression tree
     * is written to the supplied output destination.
     */

    public void explain(ExpressionPresenter out) {
        out.startElement("saxonIterate");
        select.explain(out);
        out.startSubsidiaryElement("return");
        action.explain(out);
        out.endSubsidiaryElement();
        if (!Literal.isEmptySequence(finallyExp)) {
            out.startSubsidiaryElement("saxonFinally");
            finallyExp.explain(out);
            out.endSubsidiaryElement();
        }
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
// The Original Code is: all this file
//
// The Initial Developer of the Original Code is Michael H. Kay.
//
// Contributor(s):
//

