package org.orbeon.saxon.sort;

import org.orbeon.saxon.trace.ExpressionPresenter;
import org.orbeon.saxon.expr.*;
import org.orbeon.saxon.om.EmptyIterator;
import org.orbeon.saxon.om.Item;
import org.orbeon.saxon.om.SequenceIterator;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.type.ItemType;
import org.orbeon.saxon.type.TypeHierarchy;
import org.orbeon.saxon.value.Cardinality;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Expression equivalent to the imaginary syntax
 * expr sortby (sort-key)+
 */

public class SortExpression extends Expression
        implements SortKeyEvaluator {

    private Expression select = null;
    private SortKeyDefinition[] sortKeyDefinitions = null;
    private transient AtomicComparer[] comparators = null;
        // created early if all comparators can be created statically
        // transient because Java RuleBasedCollator is not serializable

    /**
     * Create a sort expression
     * @param select the expression whose result is to be sorted
     * @param sortKeys the set of sort key definitions to be used, in major to minor order
     */

    public SortExpression(Expression select, SortKeyDefinition[] sortKeys) {
        this.select = select;
        sortKeyDefinitions = sortKeys;
        Iterator children = iterateSubExpressions();
        while (children.hasNext()) {
            Expression exp = (Expression) children.next();
            adoptChildExpression(exp);
        }
    }

    /**
     * Get the expression defining the sequence being sorted
     * @return the expression whose result is to be sorted
     */

    public Expression getBaseExpression() {
        return select;
    }

    /**
     * Get the immediate sub-expressions of this expression. Default implementation
     * returns a zero-length array, appropriate for an expression that has no
     * sub-expressions.
     *
     * @return an iterator containing the sub-expressions of this expression
     */

    public Iterator iterateSubExpressions() {
        List list = new ArrayList(8);
        list.add(select);
        for (int i = 0; i < sortKeyDefinitions.length; i++) {
            list.add(sortKeyDefinitions[i].getSortKey());
            Expression e = sortKeyDefinitions[i].order;
            if (e != null) {
                list.add(e);
            }
            e = sortKeyDefinitions[i].caseOrder;
            if (e != null) {
                list.add(e);
            }
            e = sortKeyDefinitions[i].dataTypeExpression;
            if (e != null) {
                list.add(e);
            }
            e = sortKeyDefinitions[i].language;
            if (e != null) {
                list.add(e);
            }
            e = sortKeyDefinitions[i].collationName;
            if (e != null) {
                list.add(e);
            }
            e = sortKeyDefinitions[i].stable;
            if (e != null) {
                list.add(e);
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
        if (sortKeyDefinitions != null) {
            for (int i = 0; i < sortKeyDefinitions.length; i++) {
                sortKeyDefinitions[i].getSortKey().addToPathMap(pathMap, target);
                Expression e = sortKeyDefinitions[i].getOrder();
                if (e != null) {
                    e.addToPathMap(pathMap, pathMapNodeSet);
                }
                e = sortKeyDefinitions[i].getCaseOrder();
                if (e != null) {
                    e.addToPathMap(pathMap, pathMapNodeSet);
                }
                e = sortKeyDefinitions[i].getDataTypeExpression();
                if (e != null) {
                    e.addToPathMap(pathMap, pathMapNodeSet);
                }
                e = sortKeyDefinitions[i].getLanguage();
                if (e != null) {
                    e.addToPathMap(pathMap, pathMapNodeSet);
                }
                e = sortKeyDefinitions[i].getCollationNameExpression();
                if (e != null) {
                    e.addToPathMap(pathMap, pathMapNodeSet);
                }
            }
        }
        return target;
    }

    /**
     * Given an expression that is an immediate child of this expression, test whether
     * the evaluation of the parent expression causes the child expression to be
     * evaluated repeatedly
     * @param child the immediate subexpression
     * @return true if the child expression is evaluated repeatedly
     */

    public boolean hasLoopingSubexpression(Expression child) {
        return isSortKey(child);
    }

    /**
     * Replace one subexpression by a replacement subexpression
     *
     * @param original    the original subexpression
     * @param replacement the replacement subexpression
     * @return true if the original subexpression is found
     */

    public boolean replaceSubExpression(Expression original, Expression replacement) {
        boolean found = false;
        if (select == original) {
            select = replacement;
            found = true;
        }
        for (int i = 0; i < sortKeyDefinitions.length; i++) {
            if (sortKeyDefinitions[i].getSortKey() == original) {
                sortKeyDefinitions[i].setSortKey(replacement);
                found = true;
            }
            if (sortKeyDefinitions[i].getOrder() == original) {
                sortKeyDefinitions[i].setOrder(replacement);
                found = true;
            }
            if (sortKeyDefinitions[i].getCaseOrder() == original) {
                sortKeyDefinitions[i].setCaseOrder(replacement);
                found = true;
            }
            if (sortKeyDefinitions[i].getDataTypeExpression() == original) {
                sortKeyDefinitions[i].setDataTypeExpression(replacement);
                found = true;
            }
            if (sortKeyDefinitions[i].getLanguage() == original) {
                sortKeyDefinitions[i].setLanguage(replacement);
                found = true;
            }
            if (sortKeyDefinitions[i].collationName == original) {
                sortKeyDefinitions[i].collationName = replacement;
                found = true;
            }
            if (sortKeyDefinitions[i].stable == original) {
                sortKeyDefinitions[i].stable = replacement;
                found = true;
            }            
        }
        return found;
    }

    /**
     * Simplify an expression
     * @param visitor an expression visitor
     */

    public Expression simplify(ExpressionVisitor visitor) throws XPathException {
        select = visitor.simplify(select);
        return this;
    }

    /**
     * Type-check the expression
     */

    public Expression typeCheck(ExpressionVisitor visitor, ItemType contextItemType) throws XPathException {
        Expression select2 = visitor.typeCheck(select, contextItemType);
        if (select2 != select) {
            adoptChildExpression(select2);
            select = select2;
        }
        ItemType sortedItemType = select.getItemType(visitor.getConfiguration().getTypeHierarchy());

        boolean allKeysFixed = true;
        for (int i = 0; i < sortKeyDefinitions.length; i++) {
            if (!(sortKeyDefinitions[i].isFixed())) {
                allKeysFixed = false;
            }
        }

        if (allKeysFixed) {
            comparators = new AtomicComparer[sortKeyDefinitions.length];
        }

        for (int i = 0; i < sortKeyDefinitions.length; i++) {
            Expression sortKey = sortKeyDefinitions[i].getSortKey();
            sortKey = visitor.typeCheck(sortKey, sortedItemType);
            if (visitor.getStaticContext().isInBackwardsCompatibleMode()) {
                sortKey = new FirstItemExpression(sortKey);
            } else {
                RoleLocator role =
                        new RoleLocator(RoleLocator.INSTRUCTION, "xsl:sort/select", 0);
                role.setErrorCode("XTTE1020");
                sortKey = CardinalityChecker.makeCardinalityChecker(sortKey, StaticProperty.ALLOWS_ZERO_OR_ONE, role);
            }
            sortKeyDefinitions[i].setSortKey(sortKey);
            if (sortKeyDefinitions[i].isFixed()) {
                AtomicComparer comp = sortKeyDefinitions[i].makeComparator(
                        visitor.getStaticContext().makeEarlyEvaluationContext());
                sortKeyDefinitions[i].setFinalComparator(comp);
                if (allKeysFixed) {
                    comparators[i] = comp;
                }
            }
            if ((sortKey.getDependencies() & StaticProperty.DEPENDS_ON_FOCUS) == 0) {
                visitor.getStaticContext().issueWarning(
                        "Sort key will have no effect because its value does not depend on the context item",
                        sortKey);
            }

        }
        return this;
    }

    /**
     * Perform optimisation of an expression and its subexpressions.
     * <p/>
     * <p>This method is called after all references to functions and variables have been resolved
     * to the declaration of the function or variable, and after all type checking has been done.</p>
     *
     * @param visitor an expression visitor
     * @param contextItemType the static type of "." at the point where this expression is invoked.
     *                        The parameter is set to null if it is known statically that the context item will be undefined.
     *                        If the type of the context item is not known statically, the argument is set to
     *                        {@link org.orbeon.saxon.type.Type#ITEM_TYPE}
     * @return the original expression, rewritten if appropriate to optimize execution
     * @throws XPathException if an error is discovered during this phase
     *                                        (typically a type error)
     */

    public Expression optimize(ExpressionVisitor visitor, ItemType contextItemType) throws XPathException {
        Expression select2 = visitor.optimize(select, contextItemType);
        if (select2 != select) {
            adoptChildExpression(select2);
            select = select2;
        }
        // optimize the sort keys
        ItemType sortedItemType = select.getItemType(visitor.getConfiguration().getTypeHierarchy());
        for (int i = 0; i < sortKeyDefinitions.length; i++) {
            Expression sortKey = sortKeyDefinitions[i].getSortKey();
            sortKey = visitor.optimize(sortKey, sortedItemType);
            sortKeyDefinitions[i].setSortKey(sortKey);
        }
        if (Cardinality.allowsMany(select.getCardinality())) {
            return this;
        } else {
            return select;
        }
    }

    /**
     * Copy an expression. This makes a deep copy.
     * @return the copy of the original expression
     */

    public Expression copy() {
        throw new UnsupportedOperationException("copy");
    }

    /**
     * Offer promotion for this subexpression. The offer will be accepted if the subexpression
     * is not dependent on the factors (e.g. the context item) identified in the PromotionOffer.
     * By default the offer is not accepted - this is appropriate in the case of simple expressions
     * such as constant values and variable references where promotion would give no performance
     * advantage. This method is always called at compile time.
     *
     * @param offer details of the offer, for example the offer to move
     *              expressions that don't depend on the context to an outer level in
     *              the containing expression
     * @return if the offer is not accepted, return this expression unchanged.
     *         Otherwise return the result of rewriting the expression to promote
     *         this subexpression
     * @throws org.orbeon.saxon.trans.XPathException
     *          if any error is detected
     */

    public Expression promote(PromotionOffer offer) throws XPathException {
        Expression exp = offer.accept(this);
        if (exp != null) {
            return exp;
        } else {
            select = doPromotion(select, offer);
            for (int i = 0; i < sortKeyDefinitions.length; i++) {
                final Expression sk2 = sortKeyDefinitions[i].getSortKey().promote(offer);
                sortKeyDefinitions[i].setSortKey(sk2);
                if (sortKeyDefinitions[i].order != null) {
                    sortKeyDefinitions[i].order = sortKeyDefinitions[i].order.promote(offer);
                }
                if (sortKeyDefinitions[i].stable != null) {
                    sortKeyDefinitions[i].stable = sortKeyDefinitions[i].stable.promote(offer);
                }
                if (sortKeyDefinitions[i].caseOrder != null) {
                    sortKeyDefinitions[i].caseOrder = sortKeyDefinitions[i].caseOrder.promote(offer);
                }
                if (sortKeyDefinitions[i].dataTypeExpression != null) {
                    sortKeyDefinitions[i].dataTypeExpression = sortKeyDefinitions[i].dataTypeExpression.promote(offer);
                }
                if (sortKeyDefinitions[i].language != null) {
                    sortKeyDefinitions[i].language = sortKeyDefinitions[i].language.promote(offer);
                }
                if (sortKeyDefinitions[i].collationName != null) {
                    sortKeyDefinitions[i].collationName = sortKeyDefinitions[i].collationName.promote(offer);
                }
            }
            return this;
        }
    }

    /**
     * Test whether a given expression is one of the sort keys
     * @param child the given expression
     * @return true if the given expression is one of the sort keys
     */

    public boolean isSortKey(Expression child) {
        for (int i = 0; i < sortKeyDefinitions.length; i++) {
            Expression exp = sortKeyDefinitions[i].getSortKey();
            if (exp == child) {
                return true;
            }
        }
        return false;
    }

    /**
     * Determine the static cardinality
     */

    public int computeCardinality() {
        return select.getCardinality();
    }

    /**
     * Determine the data type of the items returned by the expression, if possible
     *
     * @param th the type hierarchy cache
     * @return a value such as Type.STRING, Type.BOOLEAN, Type.NUMBER, Type.NODE,
     *         or Type.ITEM (meaning not known in advance)
     */

    public ItemType getItemType(TypeHierarchy th) {
        return select.getItemType(th);
    }

    /**
     * Get the static properties of this expression (other than its type). The result is
     * bit-significant. These properties are used for optimizations. In general, if
     * property bit is set, it is true, but if it is unset, the value is unknown.
     */

    public int computeSpecialProperties() {
        int props = 0;
        if ((select.getSpecialProperties() & StaticProperty.CONTEXT_DOCUMENT_NODESET) != 0) {
            props |= StaticProperty.CONTEXT_DOCUMENT_NODESET;
        }
        if ((select.getSpecialProperties() & StaticProperty.SINGLE_DOCUMENT_NODESET) != 0) {
            props |= StaticProperty.SINGLE_DOCUMENT_NODESET;
        }
        if ((select.getSpecialProperties() & StaticProperty.NON_CREATIVE) != 0) {
            props |= StaticProperty.NON_CREATIVE;
        }
        return props;
    }

    /**
     * Enumerate the results of the expression
     */

    public SequenceIterator iterate(XPathContext context) throws XPathException {

        SequenceIterator iter = select.iterate(context);
        if (iter instanceof EmptyIterator) {
            return iter;
        }
        XPathContext xpc = context.newMinorContext();
        xpc.setOrigin(this);

        AtomicComparer[] comps = comparators;
        if (comparators == null) {
            comps = new AtomicComparer[sortKeyDefinitions.length];
            for (int s = 0; s < sortKeyDefinitions.length; s++) {
                AtomicComparer comp = sortKeyDefinitions[s].getFinalComparator();
                if (comp == null) {
                    comp = sortKeyDefinitions[s].makeComparator(xpc);
                }
                comps[s] = comp;
            }
        }
        iter = new SortedIterator(xpc, iter, this, comps);
        ((SortedIterator) iter).setHostLanguage(getHostLanguage());
        return iter;
    }

    /**
     * Callback for evaluating the sort keys
     */

    public Item evaluateSortKey(int n, XPathContext c) throws XPathException {
        return sortKeyDefinitions[n].getSortKey().evaluateItem(c);
    }

    /**
     * Diagnostic print of expression structure. The abstract expression tree
     * is written to the supplied output destination.
     */

    public void explain(ExpressionPresenter out) {
        out.startElement("sort");
        out.startSubsidiaryElement("select");
        select.explain(out);
        out.endSubsidiaryElement();
        for (int s = 0; s < sortKeyDefinitions.length; s++) {
            out.startSubsidiaryElement("by");
            sortKeyDefinitions[s].getSortKey().explain(out);
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
// The Original Code is: all this file.
//
// The Initial Developer of the Original Code is Michael H. Kay
//
// Portions created by (your name) are Copyright (C) (your legal entity). All Rights Reserved.
//
// Contributor(s): none.
//
