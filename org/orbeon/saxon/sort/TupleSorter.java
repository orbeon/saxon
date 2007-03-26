package org.orbeon.saxon.sort;

import org.orbeon.saxon.Configuration;
import org.orbeon.saxon.expr.*;
import org.orbeon.saxon.om.Item;
import org.orbeon.saxon.om.SequenceIterator;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.type.AnyItemType;
import org.orbeon.saxon.type.ItemType;
import org.orbeon.saxon.type.Type;
import org.orbeon.saxon.type.TypeHierarchy;
import org.orbeon.saxon.value.EmptySequence;
import org.orbeon.saxon.value.ObjectValue;
import org.orbeon.saxon.value.Value;
import org.orbeon.saxon.value.StringValue;

import java.io.PrintStream;
import java.util.Iterator;
import java.util.Comparator;

/**
 * A TupleSorter is an expression that sorts a stream of tuples. It is used
 * to implement XQuery FLWR expressions.
 */
public class TupleSorter extends ComputedExpression implements MappingFunction {

    private Expression base;
    private SortKeyDefinition[] sortKeys;
    private Comparator[] comparators;

        // Although this class uses the SortKeyDefinition class to define the sort keys,
        // the actual sort key expression in the SortKeyDefinition is not used. This is because
        // the sort key is instead computed as one of the members of the tuple delivered by the
        // TupleSorter. Therefore, the sort key expression is not managed as a child of this expression.

    public TupleSorter(Expression base, SortKeyDefinition[] keys) {
        this.base = base;
        this.sortKeys = keys;
        adoptChildExpression(base);
    }

    public Expression simplify(StaticContext env) throws XPathException {
        base = base.simplify(env);
        return this;
    }

    public Expression typeCheck(StaticContext env, ItemType contextItemType) throws XPathException {
        base = base.typeCheck(env, contextItemType);
        comparators = new Comparator[sortKeys.length];
        final XPathContext context = env.makeEarlyEvaluationContext();
        for (int i=0; i<sortKeys.length; i++) {
            // sort key doesn't get a new context in XQuery
            sortKeys[i].getSortKey().typeCheck(env, contextItemType);
            comparators[i] = sortKeys[i].makeComparator(context);
            // now lose the sort key expression, which we only held on to for its type information
            sortKeys[i].setSortKey(StringValue.EMPTY_STRING);
        }
        return this;
    }

    public Expression optimize(Optimizer opt, StaticContext env, ItemType contextItemType) throws XPathException {
        base = base.optimize(opt, env, contextItemType);
        if (base instanceof EmptySequence) {
            return base;
        }
        return this;
    }

    public ItemType getItemType(TypeHierarchy th) {
        return AnyItemType.getInstance();
            // TODO: we can do better than this, but we need more information
    }

    public int computeCardinality() {
        return StaticProperty.ALLOWS_ZERO_OR_MORE;
    }

    public Iterator iterateSubExpressions() {
        return new MonoIterator(base);
    }

    /**
      * Replace one subexpression by a replacement subexpression
      * @param original the original subexpression
      * @param replacement the replacement subexpression
      * @return true if the original subexpression is found
      */

     public boolean replaceSubExpression(Expression original, Expression replacement) {
         boolean found = false;
         if (base == original) {
             base = replacement;
             found = true;
         }
         return found;
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
            return this;
        }
    }

    public SequenceIterator iterate(XPathContext context) throws XPathException {
        SequenceIterator iter = new SortedTupleIterator(context, base.iterate(context), sortKeys, comparators);
        MappingIterator mapper = new MappingIterator(iter, this);
        return mapper;
    }

    public boolean effectiveBooleanValue(XPathContext context) throws XPathException {
        // so long as the sequence is homogeneous (all atomic values or all nodes), the EBV
        // of the sorted sequence is the same as the EBV of the base sequence. Only if it is
        // heterogeneous do we need to do the sort in order to calculate the EBV.
        final TypeHierarchy th = context.getConfiguration().getTypeHierarchy();
        ItemType type = base.getItemType(th);
        if (type == Type.ITEM_TYPE) {
            return super.effectiveBooleanValue(context);
        } else {
            return base.effectiveBooleanValue(context);
        }
    }

    public void display(int level, PrintStream out, Configuration config) {
        out.println(ExpressionTool.indent(level) + "TupleSorter");
        base.display(level+1, out, config);
    }

    /**
     * Mapping function to map the wrapped objects returned by the SortedTupleIterator
     * into real items. This is done because each tuple may actually represent a sequence
     * of underlying values that share the same sort key.
     */

    public Object map(Item item) throws XPathException {
        ObjectValue tuple = (ObjectValue)item;
        Object o = tuple.getObject();
        if (o == null) {
            return o;
        }
        if (o instanceof Item) {
            return o;
        }
        Value value = (Value)o;
        return value.iterate(null);
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