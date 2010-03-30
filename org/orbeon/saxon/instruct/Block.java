package org.orbeon.saxon.instruct;
import org.orbeon.saxon.evpull.BlockEventIterator;
import org.orbeon.saxon.evpull.EmptyEventIterator;
import org.orbeon.saxon.evpull.EventIterator;
import org.orbeon.saxon.expr.*;
import org.orbeon.saxon.om.EmptyIterator;
import org.orbeon.saxon.om.Item;
import org.orbeon.saxon.om.SequenceIterator;
import org.orbeon.saxon.om.Axis;
import org.orbeon.saxon.pattern.EmptySequenceTest;
import org.orbeon.saxon.trace.ExpressionPresenter;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.type.*;
import org.orbeon.saxon.value.AtomicValue;
import org.orbeon.saxon.value.Cardinality;
import org.orbeon.saxon.value.SequenceExtent;

import java.util.*;


/**
* Implements an imaginary xsl:block instruction which simply evaluates
* its contents. Used for top-level templates, xsl:otherwise, etc.
*/

public class Block extends Instruction {

    // TODO: allow the last expression in a Block to be a tail-call of a function, at least in push mode

    private Expression[] children;

    /**
     * Create an empty block
     */

    public Block() {
    }

    /**
     * Static factory method to create a block. If one of the arguments is already a block,
     * the contents will be merged into a new composite block
     * @param e1 the first subexpression (child) of the block
     * @param e2 the second subexpression (child) of the block
     * @return a Block containing the two subexpressions, and if either of them is a block, it will
     * have been collapsed to create a flattened sequence
     */

    public static Expression makeBlock(Expression e1, Expression e2) {
        if (e1==null || Literal.isEmptySequence(e1)) {
            return e2;
        }
        if (e2==null || Literal.isEmptySequence(e2)) {
            return e1;
        }
        if (e1 instanceof Block || e2 instanceof Block) {
            Iterator it1 = (e1 instanceof Block ? e1.iterateSubExpressions() : new MonoIterator(e1));
            Iterator it2 = (e2 instanceof Block ? e2.iterateSubExpressions() : new MonoIterator(e2));
            List list = new ArrayList(10);
            while (it1.hasNext()) {
                list.add(it1.next());
            }
            while (it2.hasNext()) {
                list.add(it2.next());
            }
            Expression[] exps = new Expression[list.size()];
            exps = (Expression[])list.toArray(exps);
            Block b = new Block();
            b.setChildren(exps);
            return b;
        } else {
            Expression[] exps = {e1, e2};
            Block b = new Block();
            b.setChildren(exps);
            return b;
        }
    }

    /**
     * Static factory method to create a block from a list of expressions
     * @param list the list of expressions making up this block. The members of the List must
     * be instances of Expression
     * @return a Block containing the two subexpressions, and if either of them is a block, it will
     * have been collapsed to create a flattened sequence
     */

    public static Expression makeBlock(List list) {
        Expression[] exps = new Expression[list.size()];
        exps = (Expression[])list.toArray(exps);
        Block b = new Block();
        b.setChildren(exps);
        return b;
    }


    /**
    * Set the children of this instruction
    * @param children The instructions that are children of this instruction
    */

    public void setChildren(Expression[] children) {
        if (children==null || children.length==0) {
            this.children = null;
        } else {
            this.children = children;
            for (int c=0; c<children.length; c++) {
                adoptChildExpression(children[c]);
            }
        }
    }

    /**
    * Get the children of this instruction
    * @return the children of this instruction, as an array of Instruction objects. May return either
     * a zero-length array or null if there are no children
    */

    public Expression[] getChildren() {
        return children;
    }


    public int computeSpecialProperties() {
        if (children == null || children.length == 0) {
            // An empty sequence has all special properties except "has side effects".
            return StaticProperty.SPECIAL_PROPERTY_MASK &~ StaticProperty.HAS_SIDE_EFFECTS;
        }
        int p = super.computeSpecialProperties();
        // if all the expressions are axis expressions, we have a same-document node-set
        boolean allAxisExpressions = true;
        boolean allChildAxis = true;
        boolean allSubtreeAxis = true;
        for (int i=0; i<children.length; i++) {
            if (!(children[i] instanceof AxisExpression)) {
                allAxisExpressions = false;
                allChildAxis = false;
                allSubtreeAxis = false;
                break;
            }
            byte axis = ((AxisExpression)children[i]).getAxis();
            if (axis != Axis.CHILD) {
                allChildAxis = false;
            }
            if (!Axis.isSubtreeAxis[axis]) {
                allSubtreeAxis = false;
            }
        }
        if (allAxisExpressions) {
            p |= StaticProperty.CONTEXT_DOCUMENT_NODESET |
               StaticProperty.SINGLE_DOCUMENT_NODESET |
               StaticProperty.NON_CREATIVE;
            // if they all use the child axis, then we have a peer node-set
            if (allChildAxis) {
                p |= StaticProperty.PEER_NODESET;
            }
            if (allSubtreeAxis) {
                p |= StaticProperty.SUBTREE_NODESET;
            }
            // special case: selecting attributes then children, node-set is sorted
            if (children.length == 2 &&
                    ((AxisExpression)children[0]).getAxis() == Axis.ATTRIBUTE &&
                    ((AxisExpression)children[1]).getAxis() == Axis.CHILD) {
                p |= StaticProperty.ORDERED_NODESET;
            }
        }
        return p;
    }

    /**
     * Merge any adjacent instructions that create literal text nodes
     * @return the expression after merging literal text instructions
     */

    public Expression mergeAdjacentTextInstructions() {
        boolean[] isLiteralText = new boolean[children.length];
        boolean hasAdjacentTextNodes = false;
        for (int i=0; i<children.length; i++) {
            isLiteralText[i] = children[i] instanceof ValueOf &&
                    ((ValueOf)children[i]).getSelect() instanceof StringLiteral &&
                    !((ValueOf)children[i]).isDisableOutputEscaping();

            if (i > 0 && isLiteralText[i] && isLiteralText[i-1]) {
                hasAdjacentTextNodes = true;
            }
        }
        if (hasAdjacentTextNodes) {
            List content = new ArrayList(children.length);
            String pendingText = null;
            for (int i=0; i<children.length; i++) {
                if (isLiteralText[i]) {
                    pendingText = (pendingText == null ? "" : pendingText) +
                                    ((StringLiteral)((ValueOf)children[i]).getSelect()).getStringValue();
                } else {
                    if (pendingText != null) {
                        ValueOf inst = new ValueOf(new StringLiteral(pendingText), false, false);
                        content.add(inst);
                        pendingText = null;
                    }
                    content.add(children[i]);
                }
            }
            if (pendingText != null) {
                ValueOf inst = new ValueOf(new StringLiteral(pendingText), false, false);
                content.add(inst);
            }
            return makeBlock(content);
        } else {
            return this;
        }
    }

    public Iterator iterateSubExpressions() {
        if (children == null) {
            return Collections.EMPTY_LIST.iterator();
        } else {
            return Arrays.asList(children).iterator();
        }
    }

    /**
     * Test whether the Block includes a LocalParam instruction (which will be true only if it is the
     * body of an XSLT template)
     * @return true if the Block contains a LocalParam instruction
     */

    public boolean containsLocalParam() {
        return children != null && children.length > 0 && children[0] instanceof LocalParam;
    }    

    /**
     * Replace one subexpression by a replacement subexpression
     * @param original the original subexpression
     * @param replacement the replacement subexpression
     * @return true if the original subexpression is found
     */

    public boolean replaceSubExpression(Expression original, Expression replacement) {
        boolean found = false;
        for (int c=0; c<children.length; c++) {
            if (children[c] == original) {
                children[c] = replacement;
                found = true;
            }
        }
        return found;
    }


    /**
     * Copy an expression. This makes a deep copy.
     *
     * @return the copy of the original expression
     */

    public Expression copy() {
        Expression[] c2 = new Expression[children.length];
        for (int c=0; c<children.length; c++) {
            c2[c] = children[c].copy();
        }
        Block b2 = new Block();
        b2.children = c2;
        return b2;
    }

    /**
     * Determine the data type of the items returned by this expression
     * @return the data type
     * @param th the type hierarchy cache
     */

    public final ItemType getItemType(TypeHierarchy th) {
        if (children==null || children.length==0) {
            return EmptySequenceTest.getInstance();
        }
        ItemType t1 = children[0].getItemType(th);
        for (int i=1; i<children.length; i++) {
            t1 = Type.getCommonSuperType(t1, children[i].getItemType(th), th);
            if (t1 instanceof AnyItemType) {
                return t1;  // no point going any further
            }
        }
        return t1;
    }

    /**
     * Determine the cardinality of the expression
     */

    public final int getCardinality() {
        if (children==null || children.length==0) {
            return StaticProperty.EMPTY;
        }
        int c1 = children[0].getCardinality();
        for (int i=1; i<children.length; i++) {
            c1 = Cardinality.sum(c1, children[i].getCardinality());
            if (c1 == StaticProperty.ALLOWS_ZERO_OR_MORE) {
                break;
            }
        }
        return c1;
    }

    /**
     * Determine whether this instruction creates new nodes.
     * This implementation returns true if any child instruction
     * returns true.
     */

    public final boolean createsNewNodes() {
        if (children==null) {
            return false;
        }
        for (int i=0; i<children.length; i++) {
            int props = children[i].getSpecialProperties();
            if ((props & StaticProperty.NON_CREATIVE) == 0) {
                return true;
            }
        }
        return false;
    }


    /**
     * Check to ensure that this expression does not contain any updating subexpressions.
     * This check is overridden for those expressions that permit updating subexpressions.
     *
     * @throws org.orbeon.saxon.trans.XPathException
     *          if the expression has a non-permitted updateing subexpression
     */

    public void checkForUpdatingSubexpressions() throws XPathException {
        if (children==null || children.length < 2) {
            return;
        }
        boolean updating = false;
        boolean nonUpdating = false;
        for (int i=0; i<children.length; i++) {
            Expression act = children[i];
            if (!ExpressionTool.isAllowedInUpdatingContext(act)) {
                if (updating) {
                    XPathException err = new XPathException(
                            "If any subexpression is updating, then all must be updating", "XUST0001");
                    err.setLocator(children[i]);
                    throw err;
                }
                nonUpdating = true;
            }
            if (act.isUpdatingExpression()) {
                if (nonUpdating) {
                    XPathException err = new XPathException(
                            "If any subexpression is updating, then all must be updating", "XUST0001");
                    err.setLocator(children[i]);
                    throw err;
                }
                updating = true;
            }
        }
    }

    /**
     * Determine whether this is an updating expression as defined in the XQuery update specification
     *
     * @return true if this is an updating expression
     */

    public boolean isUpdatingExpression() {
        return children != null &&
                children.length != 0 &&
                children[0].isUpdatingExpression();
    }

    /**
     * Simplify an expression. This performs any static optimization (by rewriting the expression
     * as a different expression). The default implementation does nothing.
     *
     * @exception XPathException if an error is discovered during expression
     *     rewriting
     * @return the simplified expression
     * @param visitor an expression visitor
     */

    public Expression simplify(ExpressionVisitor visitor) throws XPathException {
        boolean allAtomic = true;
        boolean nested = false;
        if (children != null) {
            for (int c=0; c<children.length; c++) {
                children[c] = visitor.simplify(children[c]);
                if (!Literal.isAtomic(children[c])) {
                    allAtomic = false;
                }
                if (children[c] instanceof Block) {
                    nested = true;
                } else if (Literal.isEmptySequence(children[c])) {
                    nested = true;
                }
            }
            if (children.length == 1) {
                return getChildren()[0];
            }
            if (children.length == 0) {
                Expression result = Literal.makeEmptySequence();
                ExpressionTool.copyLocationInfo(this, result);
                return result;
            }
            if (nested) {
                List list = new ArrayList(children.length*2);
                flatten(list);
                children = new Expression[list.size()];
                for (int i=0; i<children.length; i++) {
                    children[i] = (Expression)list.get(i);
                    adoptChildExpression(children[i]);
                }
            }
            if (allAtomic) {
                AtomicValue[] values = new AtomicValue[children.length];
                for (int c=0; c<children.length; c++) {
                    values[c] = (AtomicValue)((Literal)children[c]).getValue();
                }
                Expression result = Literal.makeLiteral(new SequenceExtent(values));
                ExpressionTool.copyLocationInfo(this, result);
                return result;
            }
        } else {
            Expression result = Literal.makeEmptySequence();
            ExpressionTool.copyLocationInfo(this, result);
            return result;
        }

        return this;
    }

    private void flatten(List list) {
        for (int i=0; i<children.length; i++) {
            if (children[i] instanceof Block) {
                ((Block)children[i]).flatten(list);
            } else if (Literal.isEmptySequence(children[i])) {
                // no-op
            } else {
                list.add(children[i]);
            }
        }
    }

    public Expression typeCheck(ExpressionVisitor visitor, ItemType contextItemType) throws XPathException {
        boolean nested = false;
        if (children != null) {
            for (int c=0; c<children.length; c++) {
                children[c] = visitor.typeCheck(children[c], contextItemType);
                adoptChildExpression(children[c]);
                if (children[c] instanceof Block) {
                    nested = true;
                } else if (Literal.isEmptySequence(children[c])) {
                    nested = true;
                }
            }
        }
        if (nested) {
            List list = new ArrayList(children.length*2);
            flatten(list);
            children = new Expression[list.size()];
            for (int i=0; i<children.length; i++) {
                children[i] = (Expression)list.get(i);
                adoptChildExpression(children[i]);
            }
        }
        if (children == null || children.length == 0) {
            return Literal.makeEmptySequence();
        } else if (children.length == 1) {
            return children[0];
        } else {
            return this;
        }
    }

    public Expression optimize(ExpressionVisitor visitor, ItemType contextItemType) throws XPathException {
        boolean allAtomic = true;
        if (children != null) {
            for (int c=0; c<children.length; c++) {
                children[c] = visitor.optimize(children[c], contextItemType);
                adoptChildExpression(children[c]);
                if (!Literal.isAtomic(children[c])) {
                    allAtomic = false;
                }
            }
            if (allAtomic) {
                Item[] items = new Item[children.length];
                for (int c=0; c<children.length; c++) {
                    items[c] = (AtomicValue)((Literal)children[c]).getValue();
                }
                return new Literal(new SequenceExtent(items));
            }
        }
        return this;
    }



    /**
     * Handle promotion offers, that is, non-local tree rewrites.
     * @param offer The type of rewrite being offered
     * @throws XPathException
     */

    protected void promoteInst(PromotionOffer offer) throws XPathException {
        if (children != null) {
            for (int c=0; c<children.length; c++) {
                children[c] = doPromotion(children[c], offer);
//                if (offer.accepted) {
//                    break;  // can only handle one promotion at a time
//                }
            }
        }
    }

    /**
     * Check that any elements and attributes constructed or returned by this expression are acceptable
     * in the content model of a given complex type. It's always OK to say yes, since the check will be
     * repeated at run-time. The process of checking element and attribute constructors against the content
     * model of a complex type also registers the type of content expected of those constructors, so the
     * static validation can continue recursively.
     */

    public void checkPermittedContents(SchemaType parentType, StaticContext env, boolean whole) throws XPathException {
        if (children != null) {
            for (int c=0; c<children.length; c++) {
                children[c].checkPermittedContents(parentType, env, false);
            }
        }
    }

    /**
     * Diagnostic print of expression structure. The abstract expression tree
     * is written to the supplied output destination.
     */

    public void explain(ExpressionPresenter out) {
        out.startElement("sequence");
        if (children != null) {
            for (int c=0; c<children.length; c++) {
                children[c].explain(out);
            }
        }
        out.endElement();
    }


    public TailCall processLeavingTail(XPathContext context) throws XPathException {

        if (children==null) {
            return null;
        }

        TailCall tc = null;
        for (int i=0; i<children.length; i++) {
            try {
                if (children[i] instanceof TailCallReturner) {
                    tc = ((TailCallReturner)children[i]).processLeavingTail(context);
                } else {
                    children[i].process(context);
                    tc = null;
                }
            } catch (XPathException e) {
                e.maybeSetLocation(children[i]);
                e.maybeSetContext(context);
                throw e;
            }
        }
    	return tc;
    }

    /**
     * Process any local parameter declarations within the block. This is used within a saxon:finally
     * instruction within saxon:iterate to make sure that the loop parameters are properly set
     * @param context dynamic evaluation context
     * @throws XPathException
     */

    public void processLocalParams(XPathContext context) throws XPathException {

        if (children==null) {
            return;
        }

        for (int i=0; i<children.length; i++) {
            try {
                if (children[i] instanceof LocalParam) {
                    children[i].process(context);
                } else {
                    return;
                }
            } catch (XPathException e) {
                e.maybeSetLocation(children[i]);
                e.maybeSetContext(context);
                throw e;
            }
        }
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
     * Iterate over the results of all the child expressions
     */

    public SequenceIterator iterate(XPathContext context) throws XPathException {
        if (children==null || children.length == 0) {
            return EmptyIterator.getInstance();
        } else if (children.length == 1) {
            return children[0].iterate(context);
        } else {
            return new BlockIterator(children, context);
        }
    }

    /**
     * Get an EventIterator over the results of all the child expressions
     * @param context the XPath dynamic context
     * @return an EventIterator
     */

    public EventIterator iterateEvents(XPathContext context) throws XPathException {
        if (children==null || children.length == 0) {
            return EmptyEventIterator.getInstance();
        } else if (children.length == 1) {
            return children[0].iterateEvents(context);
        } else {
            return new BlockEventIterator(children, context);
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
        if (children==null) {
            return;
        }
        for (int i=0; i<children.length; i++) {
            children[i].evaluatePendingUpdates(context, pul);
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
// The Initial Developer of the Original Code is Michael H. Kay.
//
// Portions created by (your name) are Copyright (C) (your legal entity). All Rights Reserved.
//
// Contributor(s): none.
//
