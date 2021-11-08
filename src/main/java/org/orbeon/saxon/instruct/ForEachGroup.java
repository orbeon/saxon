package org.orbeon.saxon.instruct;

import org.orbeon.saxon.Controller;
import org.orbeon.saxon.trans.Err;
import org.orbeon.saxon.expr.*;
import org.orbeon.saxon.om.Item;
import org.orbeon.saxon.om.SequenceIterator;
import org.orbeon.saxon.om.StandardNames;
import org.orbeon.saxon.pattern.PatternSponsor;
import org.orbeon.saxon.sort.*;
import org.orbeon.saxon.trace.ExpressionPresenter;
import org.orbeon.saxon.trace.TraceListener;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.type.ItemType;
import org.orbeon.saxon.type.SchemaType;
import org.orbeon.saxon.type.TypeHierarchy;
import org.orbeon.saxon.value.StringValue;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * Handler for xsl:for-each-group elements in stylesheet. This is a new instruction
 * defined in XSLT 2.0
 */

public class ForEachGroup extends Instruction
        implements ContextMappingFunction, SortKeyEvaluator {

    public static final int GROUP_BY = 0;
    public static final int GROUP_ADJACENT = 1;
    public static final int GROUP_STARTING = 2;
    public static final int GROUP_ENDING = 3;


    private Expression select;
    private Expression action;
    private byte algorithm;
    private Expression key;     // for group-starting and group-ending, this is a PatternSponsor
    private Expression collationNameExpression;
    private String baseURI;
    private StringCollator collator = null;             // collation used for the grouping comparisons
    private SortKeyDefinition[] sortKeys = null;
    private transient AtomicComparer[] sortComparators = null;    // comparators used for sorting the groups

    /**
     * Create a for-each-group instruction
     * @param select the select expression (selects the population to be grouped)
     * @param action the body of the for-each-group (applied to each group in turn)
     * @param algorithm one of group-by, group-adjacent, group-starting-with, group-ending-with
     * @param key expression to evaluate the grouping key
     * @param collator user for comparing strings
     * @param collationNameExpression expression that yields the name of the collation to be used
     * @param baseURI static base URI of the expression
     * @param sortKeys list of xsl:sort keys for sorting the groups
     */

    public ForEachGroup(Expression select,
                        Expression action,
                        byte algorithm,
                        Expression key,
                        StringCollator collator,
                        Expression collationNameExpression,
                        String baseURI,
                        SortKeyDefinition[] sortKeys) {
        this.select = select;
        this.action = action;
        this.algorithm = algorithm;
        this.key = key;
        this.collator = collator;
        this.collationNameExpression = collationNameExpression;
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
     * @return the name of the instruction
     */

    public int getInstructionNameCode() {
        return StandardNames.XSL_FOR_EACH_GROUP;
    }

    /**
     * Get the action expression (the content of the for-each-group)
     * @return the body of the xsl:for-each-group instruction
     */

    public Expression getActionExpression() {
        return action;
    }

    /**
     * Get the grouping key expression expression (the group-by or group-adjacent expression, or a
     * PatternSponsor containing the group-starting-with or group-ending-with expression)
     * @return the expression used to calculate grouping keys
     */

    public Expression getGroupingKey() {
        return key;
    }

    /**
     * Simplify an expression. This performs any static optimization (by rewriting the expression
     * as a different expression).
     *
     * @return the simplified expression
     * @throws XPathException if an error is discovered during expression
     *                        rewriting
     * @param visitor an expression visitor
     */

    public Expression simplify(ExpressionVisitor visitor) throws XPathException {
        select = visitor.simplify(select);
        action = visitor.simplify(action);
        key = visitor.simplify(key);
        return this;
    }

    public Expression typeCheck(ExpressionVisitor visitor, ItemType contextItemType) throws XPathException {
        final TypeHierarchy th = visitor.getConfiguration().getTypeHierarchy();
        select = visitor.typeCheck(select, contextItemType);
        ItemType selectedItemType = select.getItemType(th);
        action = visitor.typeCheck(action, selectedItemType);
        key = visitor.typeCheck(key, selectedItemType);
        if (Literal.isEmptySequence(select)) {
            return select;
        }
        if (Literal.isEmptySequence(action)) {
            return action;
        }
        if (sortKeys != null) {

            boolean allFixed = true;
            for (int i=0; i<sortKeys.length; i++) {
                Expression sortKey = sortKeys[i].getSortKey();
                sortKey = visitor.typeCheck(sortKey, selectedItemType);
                if (visitor.getStaticContext().isInBackwardsCompatibleMode()) {
                    sortKey = new FirstItemExpression(sortKey);
                } else {
                    RoleLocator role =
                        new RoleLocator(RoleLocator.INSTRUCTION, "xsl:sort/select", 0);
                    role.setErrorCode("XTTE1020");
                    sortKey = CardinalityChecker.makeCardinalityChecker(sortKey, StaticProperty.ALLOWS_ZERO_OR_ONE, role);
                }
                sortKeys[i].setSortKey(sortKey);

                if (sortKeys[i].isFixed()) {
                    AtomicComparer comp = sortKeys[i].makeComparator(
                            visitor.getStaticContext().makeEarlyEvaluationContext());
                    sortKeys[i].setFinalComparator(comp);
                } else {
                    allFixed = false;
                }
            }
            if (allFixed) {
                sortComparators = new AtomicComparer[sortKeys.length];
                for (int i=0; i<sortKeys.length; i++) {
                    sortComparators[i] = sortKeys[i].getFinalComparator();
                }
            }
        }
        return this;
    }

    public Expression optimize(ExpressionVisitor visitor, ItemType contextItemType) throws XPathException {
        final TypeHierarchy th = visitor.getConfiguration().getTypeHierarchy();
        select = visitor.optimize(select, contextItemType);
        action = action.optimize(visitor, select.getItemType(th));
        key = key.optimize(visitor, select.getItemType(th));
        adoptChildExpression(select);
        adoptChildExpression(action);
        adoptChildExpression(key);
        if (Literal.isEmptySequence(select)) {
            return select;
        }
        if (Literal.isEmptySequence(action)) {
            return action;
        }
        // Optimize the sort key definitions
        ItemType selectedItemType = select.getItemType(th);
        if (sortKeys != null) {
            for (int i=0; i<sortKeys.length; i++) {
                Expression sortKey = sortKeys[i].getSortKey();
                sortKey = visitor.optimize(sortKey, selectedItemType);
                sortKeys[i].setSortKey(sortKey);
            }
        }
        if (collator == null && (collationNameExpression instanceof StringLiteral)) {
            String collation = ((StringLiteral)collationNameExpression).getStringValue();
            URI collationURI;
            try {
                collationURI = new URI(collation);
                if (!collationURI.isAbsolute()) {
                    URI base = new URI(baseURI);
                    collationURI = base.resolve(collationURI);
                    final String collationNameString = collationURI.toString();
                    collationNameExpression = new StringLiteral(collationNameString);
                    collator = visitor.getStaticContext().getCollation(collationNameString);
                    if (collator == null) {
                        XPathException err = new XPathException("Unknown collation " + Err.wrap(collationURI.toString(), Err.URI));
                        err.setErrorCode("XTDE1110");
                        err.setLocator(this);
                        throw err;
                    }
                }
            } catch (URISyntaxException err) {
                XPathException e = new XPathException("Collation name '" + collationNameExpression + "' is not a valid URI");
                e.setErrorCode("XTDE1110");
                e.setLocator(this);
                throw e;
            }
        }
        return this;
    }

    /**
     * Copy an expression. This makes a deep copy.
     *
     * @return the copy of the original expression
     */

    public Expression copy() {
        throw new UnsupportedOperationException("copy");
    }
    

    /**
     * Get the item type of the items returned by evaluating this instruction
     *
     * @return the static item type of the instruction
     * @param th the type hierarchy cache
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
                if (e != null && !(e instanceof Literal)) {
                    dependencies |= (e.getDependencies());
                }
                e = sortKeys[i].getDataTypeExpression();
                if (e != null && !(e instanceof Literal)) {
                    dependencies |= (e.getDependencies());
                }
                e = sortKeys[i].getLanguage();
                if (e != null && !(e instanceof Literal)) {
                    dependencies |= (e.getDependencies());
                }
            }
        }
        if (collationNameExpression != null) {
            dependencies |= collationNameExpression.getDependencies();
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
        if (collationNameExpression != null) {
            list.add(collationNameExpression);
        }
        if (sortKeys != null) {
            for (int i = 0; i < sortKeys.length; i++) {
                list.add(sortKeys[i].getSortKey());
                Expression e = sortKeys[i].getOrder();
                if (e != null) {
                    list.add(e);
                }
                e = sortKeys[i].getCaseOrder();
                if (e != null) {
                    list.add(e);
                }
                e = sortKeys[i].getDataTypeExpression();
                if (e != null) {
                    list.add(e);
                }
                e = sortKeys[i].getLanguage();
                if (e != null) {
                    list.add(e);
                }
                e = sortKeys[i].getCollationNameExpression();
                if (e != null) {
                    list.add(e);
                }
            }
        }
        return list.iterator();
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
     * @param pathMapNodeSet
     * @return the pathMapNode representing the focus established by this expression, in the case where this
     *         expression is the first operand of a path expression or filter expression. For an expression that does
     *         navigation, it represents the end of the arc in the path map that describes the navigation route. For other
     *         expressions, it is the same as the input pathMapNode.
     */

    public PathMap.PathMapNodeSet addToPathMap(PathMap pathMap, PathMap.PathMapNodeSet pathMapNodeSet) {
        PathMap.PathMapNodeSet target = select.addToPathMap(pathMap, pathMapNodeSet);
        if (collationNameExpression != null) {
            collationNameExpression.addToPathMap(pathMap, pathMapNodeSet);
        }
        if (sortKeys != null) {
            for (int i = 0; i < sortKeys.length; i++) {
                sortKeys[i].getSortKey().addToPathMap(pathMap, target);
                Expression e = sortKeys[i].getOrder();
                if (e != null) {
                    e.addToPathMap(pathMap, pathMapNodeSet);
                }
                e = sortKeys[i].getCaseOrder();
                if (e != null) {
                    e.addToPathMap(pathMap, pathMapNodeSet);
                }
                e = sortKeys[i].getDataTypeExpression();
                if (e != null) {
                    e.addToPathMap(pathMap, pathMapNodeSet);
                }
                e = sortKeys[i].getLanguage();
                if (e != null) {
                    e.addToPathMap(pathMap, pathMapNodeSet);
                }
                e = sortKeys[i].getCollationNameExpression();
                if (e != null) {
                    e.addToPathMap(pathMap, pathMapNodeSet);
                }
            }
        }
        return action.addToPathMap(pathMap, target);
    }

    /**
     * Given an expression that is an immediate child of this expression, test whether
     * the evaluation of the parent expression causes the child expression to be
     * evaluated repeatedly
     * @param child the immediate subexpression
     * @return true if the child expression is evaluated repeatedly
     */

    public boolean hasLoopingSubexpression(Expression child) {
        return child == action || child == key;
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
        if (collationNameExpression == original) {
            collationNameExpression = replacement;
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
                }
                if (sortKeys[i].getCaseOrder() == original) {
                    sortKeys[i].setCaseOrder(replacement);
                    found = true;
                }
                if (sortKeys[i].getDataTypeExpression() == original) {
                    sortKeys[i].setDataTypeExpression(replacement);
                    found = true;
                }
                if (sortKeys[i].getLanguage() == original) {
                    sortKeys[i].setLanguage(replacement);
                    found = true;
                }
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
     * @param context XPath dynamic context
     * @return a StringCollator suitable for comparing the values of grouping keys
     * @throws XPathException
     */

    private StringCollator getCollator(XPathContext context) throws XPathException {
        if (collationNameExpression != null) {
            StringValue collationValue = (StringValue)collationNameExpression.evaluateItem(context);
            String cname = collationValue.getStringValue();
            URI collationURI;
            try {
                collationURI = new URI(cname);
                if (!collationURI.isAbsolute()) {
                    if (baseURI == null) {
                        XPathException err = new XPathException("Cannot resolve relative collation URI '" + cname +
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
                XPathException err = new XPathException("Collation name '" + cname + "' is not a valid URI");
                err.setErrorCode("XTDE1110");
                err.setXPathContext(context);
                err.setLocator(this);
                throw err;
            }
            return context.getCollation(cname);
        } else {
            StringCollator collator = context.getDefaultCollation();
            return (collator==null ? CodepointCollator.getInstance() : collator);
        }
    }

    private GroupIterator getGroupIterator(XPathContext context) throws XPathException {
        SequenceIterator population = select.iterate(context);

        // get an iterator over the groups in "order of first appearance"

        GroupIterator groupIterator;
        switch (algorithm) {
            case GROUP_BY: {
                StringCollator coll = collator;
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
                StringCollator coll = collator;
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
            AtomicComparer[] comps = sortComparators;
            XPathContext xpc = context.newMinorContext();
            if (comps == null) {
                comps = new AtomicComparer[sortKeys.length];
                for (int s = 0; s < sortKeys.length; s++) {
                    comps[s] = sortKeys[s].makeComparator(xpc);
                }
            }
            groupIterator = new SortedGroupIterator(xpc, groupIterator, this, comps, this);
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
     * Callback for evaluating the sort keys
     */

    public Item evaluateSortKey(int n, XPathContext c) throws XPathException {
        return sortKeys[n].getSortKey().evaluateItem(c);
    }

    /**
     * Diagnostic print of expression structure. The abstract expression tree
     * is written to the supplied output destination.
     */

    public void explain(ExpressionPresenter out) {
        out.startElement("forEachGroup");
        out.emitAttribute("algorithm", getAlgorithmName(algorithm));
        out.startSubsidiaryElement("select");
        select.explain(out);
        out.endSubsidiaryElement();
        out.startSubsidiaryElement("key");
        key.explain(out);
        out.endSubsidiaryElement();
        out.startSubsidiaryElement("return");
        action.explain(out);
        out.endSubsidiaryElement();
        out.endElement();
    }

    private String getAlgorithmName(byte algorithm) {
        switch (algorithm) {
            case GROUP_BY:
                return "group-by";
            case GROUP_ADJACENT:
                return "group-adjacent";
            case GROUP_STARTING:
                return "group-starting-with";
            case GROUP_ENDING:
                return "group-ending-with";
            default:
                return "** unknown algorithm **";
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
