package org.orbeon.saxon.sort;

import org.orbeon.saxon.expr.*;
import org.orbeon.saxon.om.Item;
import org.orbeon.saxon.om.NamePool;
import org.orbeon.saxon.om.SequenceIterator;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.type.AnyItemType;
import org.orbeon.saxon.type.ItemType;
import org.orbeon.saxon.value.ObjectValue;
import org.orbeon.saxon.value.Value;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * A TupleSorter is an expression that sorts a stream of tuples. It is used
 * to implement XQuery FLWR expressions.
 */
public class TupleSorter extends ComputedExpression implements MappingFunction {

    private Expression base;
    private FixedSortKeyDefinition[] sortKeys;

    public TupleSorter(Expression base, FixedSortKeyDefinition[] keys) {
        this.base = base;
        this.sortKeys = keys;
    }

     public Expression simplify(StaticContext env) throws XPathException {
        base = base.simplify(env);
        return this;
    }

    public Expression analyze(StaticContext env, ItemType contextItemType) throws XPathException {
        base = base.analyze(env, contextItemType);
        return this;
    }

    public ItemType getItemType() {
        return AnyItemType.getInstance();
            // TODO: we can do better than this, but we need more information
    }

    public int computeCardinality() {
        return StaticProperty.ALLOWS_ZERO_OR_MORE;
    }

    public Iterator iterateSubExpressions() {
        ArrayList list = new ArrayList(sortKeys.length + 1);
        list.add(base);
        for (int i=0; i<sortKeys.length; i++) {
            list.add(sortKeys[i].getSortKey());
        }
        return list.iterator();
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
    * Promote this expression if possible
    */

    public Expression promote(PromotionOffer offer) throws XPathException {
        Expression exp = offer.accept(this);
        if (exp != null) {
            return exp;
        } else {
            base = base.promote(offer);
            for (int i=0; i<sortKeys.length; i++) {
                sortKeys[i].setSortKey(sortKeys[i].getSortKey().promote(offer));
            }
            return this;
        }
    }

    public SequenceIterator iterate(XPathContext context) throws XPathException {
        SequenceIterator iter = new SortedTupleIterator(context, base.iterate(context), sortKeys);
        MappingIterator mapper = new MappingIterator(iter, this, context, null);
        return mapper;
    }

    public boolean effectiveBooleanValue(XPathContext context) throws XPathException {
        // TODO: is this still correct? EBV now depends on the order of the sequence
        return base.effectiveBooleanValue(context);
    }

    public void display(int level, NamePool pool, PrintStream out) {
        out.println(ExpressionTool.indent(level) + "TupleSorter");
        base.display(level+1, pool, out);
    }

    /**
     * Mapping function to map the wrapped objects returned by the SortedTupleIterator
     * into real items. This is done because each tuple may actually represent a sequence
     * of underlying values that share the same sort key.
     */

    public Object map(Item item, XPathContext context, Object info) throws XPathException {
        ObjectValue tuple = (ObjectValue)item;
        Object o = tuple.getObject();
        if (o == null) {
            return o;
        }
        if (o instanceof Item) {
            return o;
        }
        Value value = (Value)tuple.getObject();
        return value.iterate(context);
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
// Contributor(s): none
//