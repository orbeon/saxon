package net.sf.saxon.pull;

import net.sf.saxon.trans.XPathException;

/**
 * This is a filter that can be added to a pull pipeline to remember element names so that
 * they are available immediately after the END_ELEMENT event is notified
 */
public class ElementNameTracker extends PullFilter {

    private int[] namestack = new int[20];
    int used = 0;
    int elementJustEnded = -1;

    public ElementNameTracker(PullProvider base) {
        super(base);
    }

    /**
     * Get the next event.
     * <p/>
     * <p>Note that a subclass that overrides this method is responsible for ensuring
     * that current() works properly. This can be achieved by setting the field
     * currentEvent to the event returned by any call on next().</p>
     *
     * @return an integer code indicating the type of event. The code
     *         {@link #END_OF_INPUT} is returned at the end of the sequence.
     */

    public int next() throws XPathException {
        currentEvent = super.next();
        if (currentEvent == START_ELEMENT) {
            int nc = getNameCode();
            if (used >= namestack.length) {
                int[] n2 = new int[used*2];
                System.arraycopy(namestack, 0, n2, 0, used);
                namestack = n2;
            }
            namestack[used++] = nc;
        } else if (currentEvent == END_ELEMENT) {
            elementJustEnded = namestack[--used];
        }
        return currentEvent;
    }

    /**
     * Get the nameCode identifying the name of the current node. This method
     * can be used after the {@link #START_ELEMENT}, {@link #PROCESSING_INSTRUCTION},
     * {@link #ATTRIBUTE}, or {@link #NAMESPACE} events. With some PullProvider implementations,
     * including this one, it can also be used after {@link #END_ELEMENT}: in fact, that is the
     * main purpose of this class.
     * If called at other times, the result is undefined and may result in an IllegalStateException.
     * If called when the current node is an unnamed namespace node (a node representing the default namespace)
     * the returned value is -1.
     *
     * @return the nameCode. The nameCode can be used to obtain the prefix, local name,
     *         and namespace URI from the name pool.
     */

    public int getNameCode() {
        if (currentEvent == END_ELEMENT) {
            return elementJustEnded;
        } else {
            return super.getNameCode();
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
