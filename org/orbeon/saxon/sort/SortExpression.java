package net.sf.saxon.sort;
import net.sf.saxon.expr.*;
import net.sf.saxon.om.EmptyIterator;
import net.sf.saxon.om.NamePool;
import net.sf.saxon.om.SequenceIterator;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.ItemType;
import net.sf.saxon.value.Cardinality;
import net.sf.saxon.value.Value;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
* Expression equivalent to the imaginary syntax
 * expr sortby (sort-key)+
*/

public class SortExpression extends ComputedExpression {

    private Expression select = null;
    private SortKeyDefinition[] sortKeys = null;
    private FixedSortKeyDefinition[] fixedSortKeys = null;

    public SortExpression( Expression select,
                           SortKeyDefinition[] sortKeys ) {
        this.select = select;
        this.sortKeys = sortKeys;
        boolean fixed = true;
        for (int i = 0; i < sortKeys.length; i++) {
            if (!(sortKeys[i] instanceof FixedSortKeyDefinition)) {
                fixed = false;
                break;
            };
        }
        if (fixed) {
            fixedSortKeys = new FixedSortKeyDefinition[sortKeys.length];
            System.arraycopy(sortKeys, 0, fixedSortKeys, 0, sortKeys.length);
        }
    }

    /**
     * Get the immediate sub-expressions of this expression. Default implementation
     * returns a zero-length array, appropriate for an expression that has no
     * sub-expressions.
     * @return an iterator containing the sub-expressions of this expression
     */

    public Iterator iterateSubExpressions() {
        List list = new ArrayList(8);
        list.add(select);
        for (int i=0; i<sortKeys.length; i++) {
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
    * Simplify an expression
    */

     public Expression simplify(StaticContext env) throws XPathException {
        select = select.simplify(env);
        return this;
    }

    /**
    * Type-check the expression
    */

    public Expression analyze(StaticContext env, ItemType contextItemType) throws XPathException {
        select = select.analyze(env, contextItemType);
        if (Cardinality.allowsMany(select.getCardinality())) {
            return this;
        } else {
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
     * @throws net.sf.saxon.trans.XPathException
     *          if any error is detected
     */

    public Expression promote(PromotionOffer offer) throws XPathException {
        Expression exp = offer.accept(this);
        if (exp!=null) {
            return exp;
        } else {
            select = select.promote(offer);
            for (int i=0; i<sortKeys.length; i++) {
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
        for (int i=0; i<sortKeys.length; i++) {
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
    * @return a value such as Type.STRING, Type.BOOLEAN, Type.NUMBER, Type.NODE,
    * or Type.ITEM (meaning not known in advance)
    */

	public ItemType getItemType() {
	    return select.getItemType();
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

        FixedSortKeyDefinition[] reducedSortKeys;
        if (fixedSortKeys != null) {
            reducedSortKeys = fixedSortKeys;
        } else {
            reducedSortKeys = new FixedSortKeyDefinition[sortKeys.length];
            for (int s=0; s<sortKeys.length; s++) {
                reducedSortKeys[s] = sortKeys[s].reduce(xpc);
            }
        }
        iter = new SortedIterator(  xpc,
                                    iter,
                                    reducedSortKeys);
        return iter;
    }

    public void display(int level, NamePool pool, PrintStream out) {
        out.println(ExpressionTool.indent(level) + "sort");
        select.display(level+1, pool, out);
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
