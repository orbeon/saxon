package org.orbeon.saxon.instruct;
import org.orbeon.saxon.Controller;
import org.orbeon.saxon.expr.*;
import org.orbeon.saxon.om.Item;
import org.orbeon.saxon.om.NamePool;
import org.orbeon.saxon.om.SequenceIterator;
import org.orbeon.saxon.style.StandardNames;
import org.orbeon.saxon.trace.TraceListener;
import org.orbeon.saxon.type.ItemType;
import org.orbeon.saxon.type.SchemaType;
import org.orbeon.saxon.value.EmptySequence;
import org.orbeon.saxon.xpath.XPathException;

import java.io.PrintStream;
import java.util.Iterator;


/**
* Handler for xsl:for-each elements in a stylesheet.
*/

public class ForEach extends Instruction implements MappingFunction {

    private Expression select;
    private Expression action;

    public ForEach(Expression select, Expression action) {
        this.select = select;
        this.action = action;
    }

    /**
    * Get the name of this instruction for diagnostic and tracing purposes
    */

    public int getInstructionNameCode() {
        return StandardNames.XSL_FOR_EACH;
    }

    /**
     * Get the action expression (the content of the for-each)
     */

    public Expression getActionExpression() {
        return action;
    }

    /**
    * Determine the data type of the items returned by this expression
    * @return the data type
    */

    public final ItemType getItemType() {
        return action.getItemType();
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
     */

    public Expression simplify(StaticContext env) throws XPathException {
        select = select.simplify(env);
        action = action.simplify(env);
        return this;
    }

    /**
     * Perform static analysis of an expression and its subexpressions.
     *
     * <p>This checks statically that the operands of the expression have
     * the correct type; if necessary it generates code to do run-time type checking or type
     * conversion. A static type error is reported only if execution cannot possibly succeed, that
     * is, if a run-time type error is inevitable. The call may return a modified form of the expression.</p>
     *
     * <p>This method is called after all references to functions and variables have been resolved
     * to the declaration of the function or variable. However, the types of such functions and
     * variables will only be accurately known if they have been explicitly declared.</p>
     *
     * @param env the static context of the expression
     * @exception XPathException if an error is discovered during this phase
     *     (typically a type error)
     * @return the original expression, rewritten to perform necessary
     *     run-time type checks, and to perform other type-related
     *     optimizations
     */

    public Expression analyze(StaticContext env, ItemType contextItemType) throws XPathException {
        select = select.analyze(env, contextItemType);
        action = action.analyze(env, select.getItemType());
        if (select instanceof EmptySequence) {
            return EmptySequence.getInstance();
        }
        if (action instanceof EmptySequence) {
            return EmptySequence.getInstance();
        }

        // If any subexpressions within the body of the for-each are not dependent on the focus,
        // promote them: this causes them to be evaluated once, outside the for-each loop

        PromotionOffer offer = new PromotionOffer();
        offer.action = PromotionOffer.FOCUS_INDEPENDENT;
        offer.promoteDocumentDependent = (select.getSpecialProperties() & StaticProperty.CONTEXT_DOCUMENT_NODESET) != 0;
        offer.promoteXSLTFunctions = false;
        offer.containingExpression = this;
        action = action.promote(offer);

        if (offer.containingExpression instanceof LetExpression) {
            // TODO: get all the extractable sub-expressions in one pass, to avoid unnecessary repetition.
            offer.containingExpression = offer.containingExpression.analyze(env, contextItemType);
        }

        return offer.containingExpression;
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
        select = select.promote(offer);
        action = action.promote(offer);
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
        c2.setCurrentTemplate(null);

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
        return null;
    }

    /**
     * Return an Iterator to iterate over the values of a sequence. The value of every
     * expression can be regarded as a sequence, so this method is supported for all
     * expressions. This default implementation relies on the process() method: it
     * "pushes" the results of the instruction to a sequence in memory, and then
     * iterates over this in-memory sequence.
     *
     * In principle instructions should implement a pipelined iterate() method that
     * avoids the overhead of intermediate storage.
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
        c2.setCurrentTemplate(null);
        c2.setCurrentIterator(master);
        master = new MappingIterator(master, this, c2, null);
        return master;
    }

    /**
     * Map one item to a sequence.
     * @param item The item to be mapped.
     * If context is supplied, this must be the same as context.currentItem().
     * @param context The processing context. This is supplied only for mapping constructs that
     * set the context node, position, and size. Otherwise it is null.
     * @param info Arbitrary information supplied by the creator of the MappingIterator. It must be
     * read-only and immutable for the duration of the iteration.
     * @return either (a) a SequenceIterator over the sequence of items that the supplied input
     * item maps to, or (b) an Item if it maps to a single item, or (c) null if it maps to an empty
     * sequence.
     */

    public Object map(Item item, XPathContext context, Object info) throws XPathException {
        return action.iterate(context);
    }

    /**
     * Diagnostic print of expression structure. The expression is written to the System.err
     * output stream
     *
     * @param level indentation level for this expression
     * @param out
     */

    public void display(int level, NamePool pool, PrintStream out) {
        out.println(ExpressionTool.indent(level) + "for-each");
        select.display(level+1, pool, out);
        out.println(ExpressionTool.indent(level) + "return");
        action.display(level+1, pool, out);
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
