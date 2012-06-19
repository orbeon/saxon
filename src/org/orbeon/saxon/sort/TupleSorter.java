package org.orbeon.saxon.sort;

import org.orbeon.saxon.expr.*;
import org.orbeon.saxon.om.Item;
import org.orbeon.saxon.om.SequenceIterator;
import org.orbeon.saxon.om.SingletonIterator;
import org.orbeon.saxon.trace.ExpressionPresenter;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.type.AnyItemType;
import org.orbeon.saxon.type.ItemType;
import org.orbeon.saxon.type.TypeHierarchy;
import org.orbeon.saxon.value.ObjectValue;
import org.orbeon.saxon.value.StringValue;
import org.orbeon.saxon.value.Value;

import java.util.Iterator;

/**
 * A TupleSorter is an expression that sorts a stream of tuples. It is used
 * to implement XQuery FLWR expressions.
 */
public class TupleSorter extends Expression {

    private Expression select;
    private SortKeyDefinition[] sortKeyDefinitions;
    private AtomicComparer[] comparators;

    /**
     * Create a TupleSorter
     * @param base The base expression returns the sequence of tuples to be sorted. Each tuple is
     * represented by an ObjectValue which wraps a Value (that is, in general, a sequence)
     * @param keys Although this class uses the SortKeyDefinition class to define the sort keys,
     * the actual sort key expression in the SortKeyDefinition is not used. This is because
     * the sort key is instead computed as one of the members of the tuple delivered by the
     * TupleSorter. Therefore, the sort key expression is not managed as a child of this expression.
     * Moreover, in xquery the other aspects of a sort key are always fixed statically, so we
     * don't treat those as subexpressions either.
     */

    public TupleSorter(Expression base, SortKeyDefinition[] keys) {
        select = base;
        sortKeyDefinitions = keys;
        adoptChildExpression(base);
    }

    /**
     * Get the array of AtomicComparer objects, one per sort key, that are used to compare values in the sequence
     * @return an array of AtomicComparer objects, one per sort key
     */

    public AtomicComparer[] getComparators() {
        return comparators;
    }

    /**
     * Get the base expression, the expression that computes the sequence to be sorted
     * @return the base expression
     */

    public Expression getBaseExpression() {
        return select;
    }

    public Expression simplify(ExpressionVisitor visitor) throws XPathException {
        select = visitor.simplify(select);
        return this;
    }

    public Expression typeCheck(ExpressionVisitor visitor, ItemType contextItemType) throws XPathException {
        select = visitor.typeCheck(select, contextItemType);
        if (comparators == null) {
            comparators = new AtomicComparer[sortKeyDefinitions.length];
            final XPathContext context = visitor.getStaticContext().makeEarlyEvaluationContext();
            for (int i=0; i<sortKeyDefinitions.length; i++) {
                // sort key doesn't get a new context in XQuery
                visitor.typeCheck(sortKeyDefinitions[i].getSortKey(), contextItemType);
                comparators[i] = sortKeyDefinitions[i].makeComparator(context);
                // now lose the sort key expression, which we only held on to for its type information
                sortKeyDefinitions[i].setSortKey(new StringLiteral(StringValue.EMPTY_STRING));
            }
        }
        return this;
    }

    public Expression optimize(ExpressionVisitor visitor, ItemType contextItemType) throws XPathException {
        select = visitor.optimize(select, contextItemType);
        if (Literal.isEmptySequence(select)) {
            return select;
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

    public ItemType getItemType(TypeHierarchy th) {
        return AnyItemType.getInstance();
            // TODO: we can do better than this, but we need more information
    }

    public int computeCardinality() {
        return StaticProperty.ALLOWS_ZERO_OR_MORE;
    }

    public Iterator iterateSubExpressions() {
        return new MonoIterator(select);
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
            select = select.promote(offer);
            return this;
        }
    }

    public SequenceIterator iterate(XPathContext context) throws XPathException {
        SequenceIterator iter = new SortedTupleIterator(context, select.iterate(context), comparators);
        return new MappingIterator(iter, TupleUnwrapper.getInstance());
    }

//   TODO: reinstate this, but correct it (and do it statically): it actually returns ExternalObjectType
//    public boolean effectiveBooleanValue(XPathContext context) throws XPathException {
//        // so long as the sequence is homogeneous (all atomic values or all nodes), the EBV
//        // of the sorted sequence is the same as the EBV of the base sequence. Only if it is
//        // heterogeneous do we need to do the sort in order to calculate the EBV.
//        final TypeHierarchy th = context.getConfiguration().getTypeHierarchy();
//        ItemType type = select.getItemType(th);
//        if (type == Type.ITEM_TYPE) {
//            return super.effectiveBooleanValue(context);
//        } else {
//            return select.effectiveBooleanValue(context);
//        }
//    }

    /**
     * Diagnostic print of expression structure. The abstract expression tree
     * is written to the supplied output destination.
     */

    public void explain(ExpressionPresenter out) {
        out.startElement("tupleSorter");
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

    /**
     * Mapping function to map the wrapped objects returned by the SortedTupleIterator
     * into real items. This is done because each tuple may actually represent a sequence
     * of underlying values that share the same sort key.
     */

    public static class TupleUnwrapper implements MappingFunction {

        private TupleUnwrapper(){}

        private static TupleUnwrapper THE_INSTANCE = new TupleUnwrapper();

        /**
         * Get the singular instance of this class
         * @return the singular instance
         */

        public static TupleUnwrapper getInstance() {
            return THE_INSTANCE;
        }

        public SequenceIterator map(Item item) throws XPathException {
            ObjectValue tuple = (ObjectValue)item;
            Object o = tuple.getObject();
            if (o == null) {
                return null;
            }
            if (o instanceof Item) {
                return SingletonIterator.makeIterator((Item)o);
            }
            Value value = (Value)o;
            return value.iterate();
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
// The Initial Developer of the Original Code is Michael H. Kay
//
// Portions created by (your name) are Copyright (C) (your legal entity). All Rights Reserved.
//
// Contributor(s): none
//