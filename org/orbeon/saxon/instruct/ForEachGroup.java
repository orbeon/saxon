package org.orbeon.saxon.instruct;

import org.orbeon.saxon.Configuration;
import org.orbeon.saxon.Controller;
import org.orbeon.saxon.Err;
import org.orbeon.saxon.expr.*;
import org.orbeon.saxon.om.Item;
import org.orbeon.saxon.om.SequenceIterator;
import org.orbeon.saxon.pattern.PatternSponsor;
import org.orbeon.saxon.sort.*;
import org.orbeon.saxon.style.StandardNames;
import org.orbeon.saxon.trace.TraceListener;
import org.orbeon.saxon.trans.DynamicError;
import org.orbeon.saxon.trans.StaticError;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.type.ItemType;
import org.orbeon.saxon.type.SchemaType;
import org.orbeon.saxon.type.TypeHierarchy;
import org.orbeon.saxon.value.EmptySequence;
import org.orbeon.saxon.value.StringValue;
import org.orbeon.saxon.value.Value;

import java.io.PrintStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;

/**
 * Handler for xsl:for-each-group elements in stylesheet. This is a new instruction
 * defined in XSLT 2.0
 */

public class ForEachGroup extends Instruction implements ContextMappingFunction {

    public static final int GROUP_BY = 0;
    public static final int GROUP_ADJACENT = 1;
    public static final int GROUP_STARTING = 2;
    public static final int GROUP_ENDING = 3;


    private Expression select;
    private Expression action;
    private byte algorithm;
    private Expression key;     // for group-starting and group-ending, this is a PatternSponsor
    private Expression collationName;
    private String baseURI;
    private Comparator collator = null;             // comparator used for the grouping comparisons
    private SortKeyDefinition[] sortKeys = null;
    private transient Comparator[] sortComparators = null;    // comparators used for sorting the groups

    public ForEachGroup(Expression select,
                        Expression action,
                        byte algorithm,
                        Expression key,
                        Comparator collator,
                        Expression collationName,
                        String baseURI,
                        SortKeyDefinition[] sortKeys) {
        this.select = select;
        this.action = action;
        this.algorithm = algorithm;
        this.key = key;
        this.collator = collator;
        this.collationName = collationName;
        this.baseURI = baseURI;
        this.sortKeys = sortKeys;
        Iterator kids = iterateSubExpressions();
        while (kids.hasNext()) {
            Expression child = (Expression)kids.next();
            adoptChildExpression(child);
        }
    }

    /**
     * Get the name of this instruction for diagnostic and tracing purposes
     */

    public int getInstructionNameCode() {
        return StandardNames.XSL_FOR_EACH_GROUP;
    }

    /**
     * Get the action expression (the content of the for-each)
     */

    public Expression getActionExpression() {
        return action;
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
        key = key.simplify(env);
        return this;
    }

    public Expression typeCheck(StaticContext env, ItemType contextItemType) throws XPathException {
        final TypeHierarchy th = env.getConfiguration().getTypeHierarchy();
        select = select.typeCheck(env, contextItemType);
        ItemType selectedItemType = select.getItemType(th);
        action = action.typeCheck(env, selectedItemType);
        key = key.typeCheck(env, selectedItemType);
//        adoptChildExpression(select);
//        adoptChildExpression(action);
//        adoptChildExpression(key);
        if (select instanceof EmptySequence) {
            return EmptySequence.getInstance();
        }
        if (action instanceof EmptySequence) {
            return EmptySequence.getInstance();
        }
        if (sortKeys != null) {

            boolean allFixed = true;
            for (int i=0; i<sortKeys.length; i++) {
                Expression sortKey = sortKeys[i].getSortKey();
                sortKey = sortKey.typeCheck(env, selectedItemType);
                if (env.isInBackwardsCompatibleMode()) {
                    sortKey = new FirstItemExpression(sortKey);
                } else {
                    RoleLocator role =
                        new RoleLocator(RoleLocator.INSTRUCTION, "xsl:sort/select", 0, null);
                    role.setErrorCode("XTTE1020");
                    sortKey = CardinalityChecker.makeCardinalityChecker(sortKey, StaticProperty.ALLOWS_ZERO_OR_ONE, role);
                }
                sortKeys[i].setSortKey(sortKey);

                if (sortKeys[i].isFixed()) {
                    Comparator comp = sortKeys[i].makeComparator(env.makeEarlyEvaluationContext());
                    sortKeys[i].setComparer(comp);
                } else {
                    allFixed = false;
                }
            }
            if (allFixed) {
                sortComparators = new Comparator[sortKeys.length];
                for (int i=0; i<sortKeys.length; i++) {
                    sortComparators[i] = sortKeys[i].getComparer();
                }
            }
        }
        return this;
    }

    public Expression optimize(Optimizer opt, StaticContext env, ItemType contextItemType) throws XPathException {
        final TypeHierarchy th = env.getConfiguration().getTypeHierarchy();
        select = select.optimize(opt, env, contextItemType);
        action = action.optimize(opt, env, select.getItemType(th));
        key = key.optimize(opt, env, select.getItemType(th));
        adoptChildExpression(select);
        adoptChildExpression(action);
        adoptChildExpression(key);
        if (select instanceof EmptySequence) {
            return EmptySequence.getInstance();
        }
        if (action instanceof EmptySequence) {
            return EmptySequence.getInstance();
        }
        // TODO: optimize the sort key definitions
        if (collator == null && (collationName instanceof StringValue)) {
            String collation = ((StringValue)collationName).getStringValue();
            URI collationURI;
            try {
                collationURI = new URI(collation);
                if (!collationURI.isAbsolute()) {
                    URI base = new URI(baseURI);
                    collationURI = base.resolve(collationURI);
                    collationName = new StringValue(collationURI.toString());
                    collator = env.getCollation(collationURI.toString());
                    if (collator == null) {
                        StaticError err = new StaticError("Unknown collation " + Err.wrap(collationURI.toString(), Err.URI));
                        err.setErrorCode("XTDE1110");
                        err.setLocator(this);
                        throw err;
                    }
                }
            } catch (URISyntaxException err) {
                StaticError e = new StaticError("Collation name '" + collationName + "' is not a valid URI");
                e.setErrorCode("XTDE1110");
                e.setLocator(this);
                throw e;
            }
        }
        return this;
    }

    /**
     * Get the item type of the items returned by evaluating this instruction
     *
     * @return the static item type of the instruction
     * @param th
     */

    public ItemType getItemType(TypeHierarchy th) {
        return action.getItemType(th);
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
        dependencies |= key.getDependencies() & ~StaticProperty.DEPENDS_ON_FOCUS;
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
        if (collationName != null) {
            dependencies |= collationName.getDependencies();
        }
        return dependencies;
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
     * Handle promotion offers, that is, non-local tree rewrites.
     *
     * @param offer The type of rewrite being offered
     * @throws XPathException
     */

    protected void promoteInst(PromotionOffer offer) throws XPathException {
        select = doPromotion(select, offer);
        action = doPromotion(action, offer);
        key = doPromotion(key, offer);
        // TODO: promote expressions in the sort key definitions
    }

    /**
     * Get all the XPath expressions associated with this instruction
     * (in XSLT terms, the expression present on attributes of the instruction,
     * as distinct from the child instructions in a sequence construction)
     */

    public Iterator iterateSubExpressions() {
        ArrayList list = new ArrayList(8);
        list.add(select);
        list.add(action);
        list.add(key);
        if (collationName != null) {
            list.add(collationName);
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
        if (collationName == original) {
            collationName = replacement;
            found = true;
        }
        if (key == original) {
            key = replacement;
            found = true;
        }
        if (sortKeys != null) {
            for (int i = 0; i < sortKeys.length; i++) {
                if (sortKeys[i].getSortKey() == original) {
                    sortKeys[i].setSortKey(replacement);
                    found = true;
                }
                if (sortKeys[i].getOrder() == original) {
                    sortKeys[i].setOrder(replacement);
                    found = true;
                };
                if (sortKeys[i].getCaseOrder() == original) {
                    sortKeys[i].setCaseOrder(replacement);
                    found = true;
                };
                if (sortKeys[i].getDataTypeExpression() == original) {
                    sortKeys[i].setDataTypeExpression(replacement);
                    found = true;
                };
                if (sortKeys[i].getLanguage() == original) {
                    sortKeys[i].setLanguage(replacement);
                    found = true;
                };
            }
        }
        return found;
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

        GroupIterator groupIterator = getGroupIterator(context);

        XPathContextMajor c2 = context.newContext();
        c2.setOrigin(this);
        c2.setCurrentIterator(groupIterator);
        c2.setCurrentGroupIterator(groupIterator);
        c2.setCurrentTemplateRule(null);

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

    /**
     * Get (and if necessary, create) the comparator used for comparing grouping key values
     * @param context
     * @return
     * @throws XPathException
     */

    private Comparator getCollator(XPathContext context) throws XPathException {
        if (collationName != null) {
            StringValue collationValue = (StringValue)collationName.evaluateItem(context);
            String cname = collationValue.getStringValue();
            URI collationURI;
            try {
                collationURI = new URI(cname);
                if (!collationURI.isAbsolute()) {
                    if (baseURI == null) {
                        DynamicError err = new DynamicError(
                                "Cannot resolve relative collation URI '" + cname +
                                "': unknown or invalid base URI");
                        err.setErrorCode("XTDE1110");
                        err.setXPathContext(context);
                        err.setLocator(this);
                        throw err;
                    }
                    collationURI = new URI(baseURI).resolve(collationURI);
                    cname = collationURI.toString();
                }
            } catch (URISyntaxException e) {
                DynamicError err = new DynamicError("Collation name '" + cname + "' is not a valid URI");
                err.setErrorCode("XTDE1110");
                err.setXPathContext(context);
                err.setLocator(this);
                throw err;
            }
            return context.getCollation(cname);
        } else {
            Comparator collator = context.getDefaultCollation();
            return (collator==null ? CodepointCollator.getInstance() : collator);
        }
    }

    private GroupIterator getGroupIterator(XPathContext context) throws XPathException {
        SequenceIterator population = select.iterate(context);

        // get an iterator over the groups in "order of first appearance"

        GroupIterator groupIterator;
        switch (algorithm) {
            case GROUP_BY: {
                Comparator coll = collator;
                if (coll==null) {
                    // The collation is determined at run-time
                    coll = getCollator(context);
                }
                XPathContext c2 = context.newMinorContext();
                c2.setOrigin(this);
                c2.setCurrentIterator(population);
                groupIterator = new GroupByIterator(population, key, c2, coll);
                break;
            }
            case GROUP_ADJACENT: {
                Comparator coll = collator;
                if (coll==null) {
                    // The collation is determined at run-time
                    coll = getCollator(context);
                }
                groupIterator = new GroupAdjacentIterator(population, key, context, coll);
                break;
            }
            case GROUP_STARTING:
                groupIterator = new GroupStartingIterator(population,
                        ((PatternSponsor)key).getPattern(),
                        context);
                break;
            case GROUP_ENDING:
                groupIterator = new GroupEndingIterator(population,
                        ((PatternSponsor)key).getPattern(),
                        context);
                break;
            default:
                throw new AssertionError("Unknown grouping algorithm");
        }


        // now iterate over the leading nodes of the groups

        if (sortKeys != null) {
            Comparator[] comps = sortComparators;
            XPathContext xpc = context.newMinorContext();
            if (comps == null) {
                comps = new Comparator[sortKeys.length];
                for (int s = 0; s < sortKeys.length; s++) {
                    comps[s] = sortKeys[s].makeComparator(xpc);
                }
            }
            groupIterator = new SortedGroupIterator(xpc, groupIterator, sortKeys, comps, this);
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
        c2.setCurrentTemplateRule(null);
        return new ContextMappingIterator(this, c2);
    }

    /**
     * Map one item to a sequence.
     *
     * @param context The processing context. This is supplied only for mapping constructs that
     *                set the context node, position, and size. Otherwise it is null.
     * @return either (a) a SequenceIterator over the sequence of items that the supplied input
     *         item maps to, or (b) an Item if it maps to a single item, or (c) null if it maps to an empty
     *         sequence.
     */

    public SequenceIterator map(XPathContext context) throws XPathException {
        return action.iterate(context);
    }

    /**
     * Diagnostic print of expression structure. The expression is written to the System.err
     * output stream
     *
     * @param level indentation level for this expression
     @param out
     @param config
     */

    public void display(int level, PrintStream out, Configuration config) {
        out.println(ExpressionTool.indent(level) + "for-each-group");
        out.println(ExpressionTool.indent(level) + "select");
        select.display(level + 1, out, config);
        out.println(ExpressionTool.indent(level) + "return");
        action.display(level + 1, out, config);
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
