package org.orbeon.saxon.instruct;

import org.orbeon.saxon.Controller;
import org.orbeon.saxon.value.Value;
import org.orbeon.saxon.expr.*;
import org.orbeon.saxon.om.Item;
import org.orbeon.saxon.om.NamePool;
import org.orbeon.saxon.om.SequenceIterator;
import org.orbeon.saxon.pattern.Pattern;
import org.orbeon.saxon.sort.*;
import org.orbeon.saxon.style.StandardNames;
import org.orbeon.saxon.trace.TraceListener;
import org.orbeon.saxon.type.ItemType;
import org.orbeon.saxon.xpath.XPathException;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;

/**
 * Handler for xsl:for-each-group elements in stylesheet. This is a new instruction
 * defined in XSLT 2.0
 */

public class ForEachGroup extends Instruction implements MappingFunction {

    public static final int GROUP_BY = 0;
    public static final int GROUP_ADJACENT = 1;
    public static final int GROUP_STARTING = 2;
    public static final int GROUP_ENDING = 3;


    private Expression select;
    private Expression action;
    private byte algorithm;
    private Object key;     // either an expression or pattern, depending on algorithm
    private Comparator collator = null;
    private SortKeyDefinition[] sortKeys = null;

    // TODO: need to treat predicates within the patterns as legitimate subexpressions

    public ForEachGroup(Expression select,
                        Expression action,
                        byte algorithm,
                        Object key,
                        Comparator collator,
                        SortKeyDefinition[] sortKeys) {
        this.select = select;
        this.action = action;
        this.algorithm = algorithm;
        this.key = key;
        this.collator = collator;
        this.sortKeys = sortKeys;
    }

    /**
     * Get the name of this instruction for diagnostic and tracing purposes
     */

    public int getInstructionNameCode() {
        return StandardNames.XSL_FOR_EACH_GROUP;
    }

    /**
     * Simplify an expression. This performs any static optimization (by rewriting the expression
     * as a different expression).
     *
     * @return the simplified expression
     * @throws XPathException if an error is discovered during expression
     *                        rewriting
     */

    public Expression simplify(StaticContext env) throws XPathException {
        select = select.simplify(env);
        action = action.simplify(env);
        if (key instanceof Expression) {
            key = ((Expression)key).simplify(env);
        }
        return this;
    }

    /**
     * Perform static analysis of an expression and its subexpressions.
     * <p/>
     * <p>This checks statically that the operands of the expression have
     * the correct type; if necessary it generates code to do run-time type checking or type
     * conversion. A static type error is reported only if execution cannot possibly succeed, that
     * is, if a run-time type error is inevitable. The call may return a modified form of the expression.</p>
     * <p/>
     * <p>This method is called after all references to functions and variables have been resolved
     * to the declaration of the function or variable. However, the types of such functions and
     * variables will only be accurately known if they have been explicitly declared.</p>
     *
     * @param env the static context of the expression
     * @return the original expression, rewritten to perform necessary
     *         run-time type checks, and to perform other type-related
     *         optimizations
     * @throws XPathException if an error is discovered during this phase
     *                        (typically a type error)
     */

    public Expression analyze(StaticContext env, ItemType contextItemType) throws XPathException {
        select = select.analyze(env, contextItemType);
        action = action.analyze(env, select.getItemType());
        if (key instanceof Expression) {
            key = ((Expression)key).analyze(env, select.getItemType());
        }
        return this;
    }

    /**
     * Get the item type of the items returned by evaluating this instruction
     *
     * @return the static item type of the instruction
     */

    public ItemType getItemType() {
        return action.getItemType();
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
        // some of the dependencies in the "action" part and in the grouping and sort keys aren't relevant,
        // because they don't depend on values set outside the for-each-group expression
        int dependencies = 0;
        dependencies |= select.getDependencies();
        if (key instanceof Expression) {
            dependencies |= (((Expression)key).getDependencies()
                    &~ StaticProperty.DEPENDS_ON_FOCUS );
        }
        dependencies |= (action.getDependencies()
                    &~ (StaticProperty.DEPENDS_ON_FOCUS | StaticProperty.DEPENDS_ON_CURRENT_GROUP));
        if (sortKeys != null) {
            for (int i = 0; i < sortKeys.length; i++) {
                dependencies |= (sortKeys[i].getSortKey().getDependencies() &~ StaticProperty.DEPENDS_ON_FOCUS);
                Expression e = sortKeys[i].getCaseOrder();
                if (e != null && !(e instanceof Value)) {
                    dependencies |= (e.getDependencies());
                }
                e = sortKeys[i].getDataTypeExpression();
                if (e != null && !(e instanceof Value)) {
                    dependencies |= (e.getDependencies());
                }
                e = sortKeys[i].getLanguage();
                if (e != null && !(e instanceof Value)) {
                    dependencies |= (e.getDependencies());
                }
            }
        }
        return dependencies;
    }

    /**
     * Handle promotion offers, that is, non-local tree rewrites.
     *
     * @param offer The type of rewrite being offered
     * @throws XPathException
     */

    protected void promoteInst(PromotionOffer offer) throws XPathException {
        select = select.promote(offer);
        action = action.promote(offer);
        if (key instanceof Expression) {
            key = ((Expression)key).promote(offer);
        }
    }

    /**
     * Get all the XPath expressions associated with this instruction
     * (in XSLT terms, the expression present on attributes of the instruction,
     * as distinct from the child instructions in a sequence construction)
     */

    public Iterator iterateSubExpressions() {
        ArrayList list = new ArrayList(3);
        list.add(select);
        list.add(action);
        if (key instanceof Expression) {
            list.add(key);
        }
        if (sortKeys != null) {
            for (int i = 0; i < sortKeys.length; i++) {
                list.add(sortKeys[i].getSortKey());
                Expression e = sortKeys[i].getOrder();
                if (e != null && !(e instanceof Value)) {
                    list.add(e);
                }
                e = sortKeys[i].getCaseOrder();
                if (e != null && !(e instanceof Value)) {
                    list.add(e);
                }
                e = sortKeys[i].getDataTypeExpression();
                if (e != null && !(e instanceof Value)) {
                    list.add(e);
                }
                e = sortKeys[i].getLanguage();
                if (e != null && !(e instanceof Value)) {
                    list.add(e);
                }
            }
        }
        return list.iterator();
    }

    public TailCall processLeavingTail(XPathContext context) throws XPathException {
        Controller controller = context.getController();

        GroupIterator groupIterator = getGroupIterator(context);

        XPathContextMajor c2 = context.newContext();
        c2.setOrigin(this);
        c2.setCurrentIterator(groupIterator);
        c2.setCurrentGroupIterator(groupIterator);
        c2.setCurrentTemplate(null);

        if (controller.isTracing()) {
            TraceListener listener = controller.getTraceListener();
            while (true) {
                Item item = groupIterator.next();
                if (item == null) {
                    break;
                }
                listener.startCurrentItem(item);
                action.process(c2);
                listener.endCurrentItem(item);
            }
        } else {
            while (true) {
                Item item = groupIterator.next();
                if (item == null) {
                    break;
                }
                action.process(c2);
            }
        }

        return null;
    }

    private GroupIterator getGroupIterator(XPathContext context) throws XPathException {
        SequenceIterator population = select.iterate(context);

        // get an iterator over the groups in "order of first appearance"

        GroupIterator groupIterator;
        switch (algorithm) {
            case GROUP_BY:
                {
                    XPathContext c2 = context.newMinorContext();
                    c2.setOrigin(this);
                    c2.setCurrentIterator(population);
                    groupIterator = new GroupByIterator(population,
                            (Expression)key,
                            c2,
                            collator);
                    break;
                }
            case GROUP_ADJACENT:
                {
                    XPathContext c2 = context.newMinorContext();
                    c2.setOrigin(this);
                    c2.setCurrentIterator(population);
                    groupIterator = new GroupAdjacentIterator(population,
                            (Expression)key,
                            c2,
                            collator);
                    break;
                }
            case GROUP_STARTING:
                groupIterator = new GroupStartingIterator(population,
                        (Pattern)key,
                        context);
                break;
            case GROUP_ENDING:
                groupIterator = new GroupEndingIterator(population,
                        (Pattern)key,
                        context);
                break;
            default:
                throw new AssertionError("Unknown grouping algorithm");
        }


        // now iterate over the leading nodes of the groups

        if (sortKeys != null) {
            // TODO: avoid reducing the sort keys if they have no dependencies
            FixedSortKeyDefinition[] reducedSortKeys = new FixedSortKeyDefinition[sortKeys.length];
            XPathContext xpc = context.newMinorContext();
            for (int s = 0; s < sortKeys.length; s++) {
                reducedSortKeys[s] = sortKeys[s].reduce(xpc);
            }
            groupIterator = new SortedGroupIterator(xpc,
                    groupIterator,
                    reducedSortKeys,
                    this);
        }
        return groupIterator;
    }

    /**
     * Return an Iterator to iterate over the values of a sequence. The value of every
     * expression can be regarded as a sequence, so this method is supported for all
     * expressions. This default implementation relies on the process() method: it
     * "pushes" the results of the instruction to a sequence in memory, and then
     * iterates over this in-memory sequence.
     * <p/>
     * In principle instructions should implement a pipelined iterate() method that
     * avoids the overhead of intermediate storage.
     *
     * @param context supplies the context for evaluation
     * @return a SequenceIterator that can be used to iterate over the result
     *         of the expression
     * @throws XPathException if any dynamic error occurs evaluating the
     *                        expression
     */

    public SequenceIterator iterate(XPathContext context) throws XPathException {
        GroupIterator master = getGroupIterator(context);
        XPathContextMajor c2 = context.newContext();
        c2.setOrigin(this);
        c2.setCurrentIterator(master);
        c2.setCurrentGroupIterator(master);
        c2.setCurrentTemplate(null);
        return new MappingIterator(master, this, c2, null);
    }

    /**
     * Map one item to a sequence.
     *
     * @param item    The item to be mapped.
     *                If context is supplied, this must be the same as context.currentItem().
     * @param context The processing context. This is supplied only for mapping constructs that
     *                set the context node, position, and size. Otherwise it is null.
     * @param info    Arbitrary information supplied by the creator of the MappingIterator. It must be
     *                read-only and immutable for the duration of the iteration.
     * @return either (a) a SequenceIterator over the sequence of items that the supplied input
     *         item maps to, or (b) an Item if it maps to a single item, or (c) null if it maps to an empty
     *         sequence.
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
        out.println(ExpressionTool.indent(level) + "for-each-group");
        out.println(ExpressionTool.indent(level) + "select");
        select.display(level + 1, pool, out);
        out.println(ExpressionTool.indent(level) + "return");
        action.display(level + 1, pool, out);
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
