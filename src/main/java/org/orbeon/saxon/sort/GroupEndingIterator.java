package org.orbeon.saxon.sort;

import org.orbeon.saxon.expr.XPathContext;
import org.orbeon.saxon.om.*;
import org.orbeon.saxon.pattern.Pattern;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.value.AtomicValue;

import java.util.ArrayList;
import java.util.List;

/**
 * A GroupEndingIterator iterates over a sequence of groups defined by
 * xsl:for-each-group group-ending-with="x". The groups are returned in
 * order of first appearance.
 */

public class GroupEndingIterator implements GroupIterator, LookaheadIterator {

    private SequenceIterator population;
    private Pattern endPattern;
    private XPathContext baseContext;
    private XPathContext runningContext;
    private List currentMembers;
    private Item next;
    private Item current = null;
    private int position = 0;

    public GroupEndingIterator(SequenceIterator population, Pattern endPattern,
                               XPathContext context)
            throws XPathException {
        this.population = population;
        this.endPattern = endPattern;
        baseContext = context;
        runningContext = context.newMinorContext();
        runningContext.setCurrentIterator(population);
        // the first item in the population always starts a new group
        next = population.next();
    }

    private void advance() throws XPathException {
        currentMembers = new ArrayList(20);
        currentMembers.add(current);

        next = current;
        while (next != null) {
            if (endPattern.matches((NodeInfo)next, runningContext)) {
                next = population.next();
                if (next != null) {
                    break;
                }
            } else {
                next = population.next();
                if (next != null) {
                    currentMembers.add(next);
                }
            }
        }
    }

    public AtomicValue getCurrentGroupingKey() {
        return null;
    }

    public SequenceIterator iterateCurrentGroup() {
        return new ListIterator(currentMembers);
    }

    public boolean hasNext() {
        return next != null;
    }

    public Item next() throws XPathException {
        if (next != null) {
            current = next;
            position++;
            advance();
            return current;
        } else {
            current = null;
            position = -1;
            return null;
        }
    }

    public Item current() {
        return current;
    }

    public int position() {
        return position;
    }

    public void close() {
        population.close();
    }

    public SequenceIterator getAnother() throws XPathException {
        return new GroupEndingIterator(population.getAnother(), endPattern, baseContext);
    }

    /**
     * Get properties of this iterator, as a bit-significant integer.
     * @return the properties of this iterator. This will be some combination of
     *         properties such as {@link #GROUNDED}, {@link #LAST_POSITION_FINDER},
     *         and {@link #LOOKAHEAD}. It is always
     *         acceptable to return the value zero, indicating that there are no known special properties.
     *         It is acceptable for the properties of the iterator to change depending on its state.
     */

    public int getProperties() {
        return LOOKAHEAD;
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