package net.sf.saxon.functions;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.om.Item;
import net.sf.saxon.sort.CodepointCollator;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.value.AtomicValue;
import net.sf.saxon.value.BooleanValue;
import net.sf.saxon.value.StringValue;

import java.text.CollationElementIterator;
import java.text.RuleBasedCollator;
import java.util.Comparator;


/**
* This class implements the contains(), starts-with(), ends-with(),
* substring-before(), and substring-after() functions
*/

public class Contains extends CollatingFunction {

    public static final int CONTAINS = 0;
    public static final int STARTSWITH = 1;
    public static final int ENDSWITH = 2;
    public static final int AFTER = 3;
    public static final int BEFORE = 4;

    /**
    * Evaluate the function
    */

    public Item evaluateItem(XPathContext context) throws XPathException {

        Comparator collator = getCollator(2, context, false);

        AtomicValue arg0 = (AtomicValue)argument[0].evaluateItem(context);
        if (arg0==null) {
            arg0 = StringValue.EMPTY_STRING;
        }

        AtomicValue arg1 = (AtomicValue)argument[1].evaluateItem(context);
        if (arg1==null) {
            arg1 = StringValue.EMPTY_STRING;
        };

        String s0 = arg0.getStringValue();
        String s1 = arg1.getStringValue();

        if (collator instanceof CodepointCollator) {
            // Using unicode code-point matching: use the Java string-matching routines directly
            switch(operation) {
                case CONTAINS:
                    return BooleanValue.get(s0.indexOf(s1) >= 0);
                case STARTSWITH:
                    return BooleanValue.get(s0.startsWith(s1));
                case ENDSWITH:
                    return BooleanValue.get(s0.endsWith(s1));
                case AFTER:
                    int i = s0.indexOf(s1);
                    if (i<0) return StringValue.EMPTY_STRING;
                    return new StringValue(s0.substring(i+s1.length()));
                case BEFORE:
                    int j = s0.indexOf(s1);
                    if (j<0) return StringValue.EMPTY_STRING;
                    return new StringValue(s0.substring(0, j));
                default:
                    throw new UnsupportedOperationException("Unknown operation " + operation);
            }
        } else {

            if (!(collator instanceof RuleBasedCollator)) {
                dynamicError("The collation for " + getDisplayName(context.getController().getNamePool()) +
                        " must be a RuleBaseCollator", context);
                return null;
            }
            RuleBasedCollator rbc = (RuleBasedCollator)collator;
            CollationElementIterator iter0 = rbc.getCollationElementIterator(s0);
            CollationElementIterator iter1 = rbc.getCollationElementIterator(s1);

            switch (operation) {
                case STARTSWITH:
                    return BooleanValue.get( collationStartsWith(iter0, iter1) );
                case CONTAINS:
                case ENDSWITH:
                    return BooleanValue.get( collationContains(iter0, iter1, null) );
                case AFTER:
                    int[] ia = new int[2];
                    boolean ba = collationContains(iter0, iter1, ia);
                    if (ba) {
                        return new StringValue(s0.substring(ia[1]));
                    } else {
                        return StringValue.EMPTY_STRING;
                    }
                case BEFORE:
                    int[] ib = new int[2];
                    boolean bb = collationContains(iter0, iter1, ib);
                    if (bb) {
                        return new StringValue(s0.substring(0, ib[0]));
                    } else {
                        return StringValue.EMPTY_STRING;
                    }
                default:
                    throw new UnsupportedOperationException("Unknown operation " + operation);
            }
        }
    }

    /**
    * Determine whether one string starts with another, under the terms of a given
    * collating sequence.
    * @param s0 iterator over the collation elements of the containing string
    * @param s1 iterator over the collation elements of the contained string
    */

    private boolean collationStartsWith(CollationElementIterator s0,
                                        CollationElementIterator s1) {
        while (true) {
            int e1 = s1.next();
            if (e1 == -1) {
                return true;
            }
            int e0 = s0.next();
            if (e0 != e1) {
                return false;
            }
        }
    }

    /**
    * Determine whether one string contains another, under the terms of a given
    * collating sequence. If operation=ENDSWITH, the match must be at the end of the first
    * string.
    * @param s0 iterator over the collation elements of the containing string
    * @param s1 iterator over the collation elements of the contained string
    * @param offsets may be null, but if it is supplied, it must be an array of two
    * integers which, if the function returns true, will contain the start position of the
    * first matching substring, and the offset of the first character after the first
    * matching substring. This is not available for operation=endswith
    * @return true if the first string contains the second
    */

    private boolean collationContains(CollationElementIterator s0,
                                      CollationElementIterator s1,
                                      int[] offsets ) {
        int e1 = s1.next();
        if (e1 == -1) {
            return true;
        }
        int e0 = -1;
        while (true) {
            // scan the first string to find a matching character
            while (e0 != e1) {
                e0 = s0.next();
                if (e0 == -1) {
                    // hit the end, no match
                    return false;
                }
            }
            // matched first character, note the position of the possible match
            int start = s0.getOffset();
            if (collationStartsWith(s0, s1)) {
                if (operation == ENDSWITH) {
                    if (s0.next() == -1) {
                        // the match is at the end
                        return true;
                    }
                    // else ignore this match and keep looking
                } else {
                    if (offsets != null) {
                        offsets[0] = start-1;
                        offsets[1] = s0.getOffset();
                    }
                    return true;
                }
            }
            // reset the position and try again
            s0.setOffset(start);

            // workaround for a difference between JDK 1.4.0 and JDK 1.4.1
            if (s0.getOffset() != start) {
                // JDK 1.4.0 takes this path
                s0.next();
            }
            s1.reset();
            e0 = -1;
            e1 = s1.next();
            // loop round to try again
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
