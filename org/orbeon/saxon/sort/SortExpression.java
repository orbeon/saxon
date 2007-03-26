package org.orbeon.saxon.sort;

import org.orbeon.saxon.Configuration;
import org.orbeon.saxon.expr.*;
import org.orbeon.saxon.om.EmptyIterator;
import org.orbeon.saxon.om.SequenceIterator;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.type.ItemType;
import org.orbeon.saxon.type.TypeHierarchy;
import org.orbeon.saxon.value.Cardinality;
import org.orbeon.saxon.value.Value;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Comparator;

/**
 * Expression equivalent to the imaginary syntax
 * expr sortby (sort-key)+
 */

public class SortExpression extends ComputedExpression {

    private Expression select = null;
    private SortKeyDefinition[] sortKeys = null;
    private transient Comparator[] comparators = null;
        // created early if all comparators can be created statically
        // transient because Java RuleBasedCollator is not serializable


    public SortExpression(Expression select, SortKeyDefinition[] sortKeys) {
        this.select = select;
        this.sortKeys = sortKeys;

        Iterator children = iterateSubExpressions();
        while (children.hasNext()) {
            Expression exp = (Expression) children.next();
            adoptChildExpression(exp);
        }
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
        for (int i = 0; i < sortKeys.length; i++) {
            list.add(sortKeys[i].getSortKey());
            Expression e = sortKeys[i].order;
            if (e != null && !(e instanceof Value)) {
                list.add(e);
            }
            e = sortKeys[i].caseOrder;
            if (e != null && !(e instanceof Value)) {
                list.add(e);
            }
            e = sortKeys[i].dataTypeExpression;
            if (e != null && !(e instanceof Value)) {
                list.add(e);
            }
            e = sortKeys[i].language;
            if (e != null && !(e instanceof Value)) {
                list.add(e);
            }
            e = sortKeys[i].collationName;
            if (e != null && !(e instanceof Value)) {
                list.add(e);
            }
        }
        return list.iterator();
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
        return found;
    }

    /**
     * Simplify an expression
     */

    public Expression simplify(StaticContext env) throws XPathException {
        select = select.simplify(env);
        return this;
    }

    /**
     * Type-check the expression
     */

    public Expression typeCheck(StaticContext env, ItemType contextItemType) throws XPathException {
        Expression select2 = select.typeCheck(env, contextItemType);
        if (select2 != select) {
            adoptChildExpression(select2);
            select = select2;
        }
        ItemType sortedItemType = select.getItemType(env.getConfiguration().getTypeHierarchy());

        boolean allKeysFixed = true;
        for (int i = 0; i < sortKeys.length; i++) {
            sortKeys[i].setParentExpression(this);
            if (!(sortKeys[i].isFixed())) {
                allKeysFixed = false;
            }
        }

        if (allKeysFixed) {
            comparators = new Comparator[sortKeys.length];
        }

        for (int i = 0; i < sortKeys.length; i++) {
            Expression sortKey = sortKeys[i].getSortKey();
            sortKey = sortKey.typeCheck(env, sortedItemType);
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
                if (allKeysFixed) {
                    comparators[i] = comp;
                }
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
     * @param opt             the optimizer in use. This provides access to supporting functions; it also allows
     *                        different optimization strategies to be used in different circumstances.
     * @param env             the static context of the expression
     * @param contextItemType the static type of "." at the point where this expression is invoked.
     *                        The parameter is set to null if it is known statically that the context item will be undefined.
     *                        If the type of the context item is not known statically, the argument is set to
     *                        {@link org.orbeon.saxon.type.Type#ITEM_TYPE}
     * @return the original expression, rewritten if appropriate to optimize execution
     * @throws org.orbeon.saxon.trans.StaticError if an error is discovered during this phase
     *                                        (typically a type error)
     */

    public Expression optimize(Optimizer opt, StaticContext env, ItemType contextItemType) throws XPathException {
        Expression select2 = select.optimize(opt, env, contextItemType);
        if (select2 != select) {
            adoptChildExpression(select2);
            select = select2;
        }
        ItemType sortedItemType = select.getItemType(env.getConfiguration().getTypeHierarchy());
        // TODO: optimize the sort keys etc.
        if (Cardinality.allowsMany(select.getCardinality())) {
            return this;
        } else {
            ComputedExpression.setParentExpression(select, getParentExpression());
            return select;
        }
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
            for (int i = 0; i < sortKeys.length; i++) {
                sortKeys[i].setSortKey((sortKeys[i].getSortKey().promote(offer)));
                if (sortKeys[i].caseOrder != null) {
                    sortKeys[i].caseOrder = sortKeys[i].caseOrder.promote(offer);
                }
                if (sortKeys[i].dataTypeExpression != null) {
                    sortKeys[i].dataTypeExpression = sortKeys[i].dataTypeExpression.promote(offer);
                }
                if (sortKeys[i].language != null) {
                    sortKeys[i].language = sortKeys[i].language.promote(offer);
                }
                if (sortKeys[i].collationName != null) {
                    sortKeys[i].collationName = sortKeys[i].collationName.promote(offer);
                }
            }
            return this;
        }
    }

    /**
     * Test whether a given expression is one of the sort keys
     */

    public boolean isSortKey(Expression child) {
        for (int i = 0; i < sortKeys.length; i++) {
            Expression exp = sortKeys[i].getSortKey();
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
     * @param th
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

        Comparator[] comps = comparators;
        if (comparators == null) {
            comps = new Comparator[sortKeys.length];
            for (int s = 0; s < sortKeys.length; s++) {
                Comparator comp = sortKeys[s].getComparer();
                if (comp == null) {
                    comp = sortKeys[s].makeComparator(xpc);
                }
                comps[s] = comp;
            }
        }
        iter = new SortedIterator(xpc, iter, sortKeys, comps);
        ((SortedIterator) iter).setHostLanguage(getHostLanguage());
        return iter;
    }

    public void display(int level, PrintStream out, Configuration config) {
        out.println(ExpressionTool.indent(level) + "sort");
        select.display(level + 1, out, config);
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
