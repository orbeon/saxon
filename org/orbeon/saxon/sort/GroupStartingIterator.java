package net.sf.saxon.sort;

import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.om.Item;
import net.sf.saxon.om.ListIterator;
import net.sf.saxon.om.NodeInfo;
import net.sf.saxon.om.SequenceIterator;
import net.sf.saxon.pattern.Pattern;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.value.AtomicValue;

import java.util.ArrayList;
import java.util.List;

/**
 * A GroupStartingIterator iterates over a sequence of groups defined by
 * xsl:for-each-group group-starting-with="x". The groups are returned in
 * order of first appearance.
 */

public class GroupStartingIterator implements GroupIterator {

    private SequenceIterator population;
    private Pattern startPattern;
    private XPathContext baseContext;
    private XPathContext runningContext;
    private List currentMembers;
    private Item next;
    private Item current = null;
    private int position = 0;

    public GroupStartingIterator(SequenceIterator population, Pattern startPattern,
                                 XPathContext context)
    throws XPathException {
        this.population = population;
        this.startPattern = startPattern;
        baseContext = context;
        runningContext = context.newMinorContext();
        runningContext.setCurrentIterator(population);
        // the first item in the population always starts a new group
        next = population.next();
     }

     private void advance() throws XPathException {
         currentMembers = new ArrayList(10);
         currentMembers.add(current);
         while (true) {
             NodeInfo nextCandidate = (NodeInfo)population.next();
             if (nextCandidate == null) {
                 break;
             }
             if (startPattern.matches(nextCandidate, runningContext)) {
                 next = nextCandidate;
                 return;
             } else {
                 currentMembers.add(nextCandidate);
             }
         }
         next = null;
     }

     public AtomicValue getCurrentGroupingKey() {
         return null;
     }

     public SequenceIterator iterateCurrentGroup() {
         return new ListIterator(currentMembers);
     }

     public Item next() throws XPathException {
         if (next != null) {
             current = next;
             position++;
             advance();
             return current;
         } else {
             return null;
         }
     }

     public Item current() {
         return current;
     }

     public int position() {
         return position;
     }

    public SequenceIterator getAnother() throws XPathException {
        return new GroupStartingIterator(population.getAnother(), startPattern, baseContext);
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