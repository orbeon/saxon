package net.sf.saxon.sort;

import net.sf.saxon.expr.Expression;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.om.ListIterator;
import net.sf.saxon.om.Item;
import net.sf.saxon.om.SequenceIterator;
import net.sf.saxon.om.LookaheadIterator;
import net.sf.saxon.value.AtomicValue;
import net.sf.saxon.xpath.XPathException;
import net.sf.saxon.xpath.DynamicError;

import java.util.ArrayList;
import java.util.List;
import java.util.Comparator;

/**
 * A GroupAdjacentIterator iterates over a sequence of groups defined by
 * xsl:for-each-group group-adjacent="x". The groups are returned in
 * order of first appearance.
 */

public class GroupAdjacentIterator implements GroupIterator, LookaheadIterator {

    private SequenceIterator population;
    private Expression keyExpression;
    private Comparator collator;
    private AtomicSortComparer comparer;
    private AtomicSortComparer.ComparisonKey currentComparisonKey;
    private XPathContext keyContext;
    private AtomicValue currentKey = null;
    private List currentMembers;
    private AtomicValue nextKey = null;
    private Item next;
    private Item current = null;
    private int position = 0;

    public GroupAdjacentIterator(SequenceIterator population, Expression keyExpression,
                                 XPathContext keyContext, Comparator collator)
    throws XPathException {
        this.population = population;
        this.keyExpression = keyExpression;
        this.keyContext = keyContext;
        this.collator = collator;
        this.comparer = new AtomicSortComparer(collator);
        next = population.next();
        if (next != null) {
            nextKey = (AtomicValue)keyExpression.evaluateItem(keyContext);
        }
    }

    private void advance() throws XPathException {
        currentMembers = new ArrayList();
        currentMembers.add(current);
        while (true) {
            Item nextCandidate = population.next();
            if (nextCandidate == null) {
                break;
            }
            AtomicValue candidateKey =
                    (AtomicValue)keyExpression.evaluateItem(keyContext);
            try {
                if (currentComparisonKey.equals(comparer.getComparisonKey(candidateKey))) {
                    currentMembers.add(nextCandidate);
                } else {
                    next = nextCandidate;
                    nextKey = candidateKey;
                    return;
                }
            } catch (ClassCastException e) {
                DynamicError err = new DynamicError("Grouping key values are of non-comparable types (" +
                        currentKey.getItemType() +
                        " and " +
                        candidateKey.getItemType() + ")");
                err.setIsTypeError(true);
                err.setXPathContext(keyContext);
                throw err;
            }
        }
        next = null;
        nextKey = null;
    }

    public AtomicValue getCurrentGroupingKey() {
        return currentKey;
    }

    public SequenceIterator iterateCurrentGroup() {
        return new ListIterator(currentMembers);
    }

    public boolean hasNext() {
        return next != null;
    }

    public Item next() throws XPathException {
        if (next == null) {
            return null;
        }
        current = next;
        currentKey = nextKey;
        currentComparisonKey = comparer.getComparisonKey(currentKey);
        position++;
        advance();
        return current;
    }

    public Item current() {
        return current;
    }

    public int position() {
        return position;
    }

    public SequenceIterator getAnother() throws XPathException {
        XPathContext c2 = keyContext.newMinorContext();
        c2.setOrigin(keyContext.getOrigin());
        return new GroupAdjacentIterator(population, keyExpression, c2, collator);
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