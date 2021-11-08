package org.orbeon.saxon.pull;

import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.om.NamePool;
import org.orbeon.saxon.om.FastStringBuffer;

/**
 * PullTracer is a PullFilter that can be inserted into a pull pipeline for diagnostic purposes. It traces
 * all the events as they are read, writing details to System.err
*/

public class PullTracer extends PullFilter {

    private NamePool pool;

    /**
     * Create a PullTracer
     * @param base the PullProvider to which requests are to be passed
     */

    public PullTracer(PullProvider base) {
        super(base);
    }

    /**
     * Get the next event. This implementation gets the next event from the underlying PullProvider,
     * copies it to the branch Receiver, and then returns the event to the caller.
     *
     * @return an integer code indicating the type of event. The code
     *         {@link #END_OF_INPUT} is returned at the end of the sequence.
     */

    public int next() throws XPathException {
        currentEvent = super.next();
        traceEvent(currentEvent);
        return currentEvent;
    }


    /**
     * Copy a pull event to a Receiver
     */

    private void traceEvent(int event) {
        if (pool == null) {
            pool = getPipelineConfiguration().getConfiguration().getNamePool();
        }
        PullProvider in = getUnderlyingProvider();
        switch (event) {
            case START_DOCUMENT:
                System.err.println("START_DOCUMENT");
                break;

            case START_ELEMENT:
                System.err.println("START_ELEMENT " + pool.getDisplayName(in.getNameCode()));
                break;

            case TEXT:
                System.err.println("TEXT");
                try {
                    CharSequence cs = this.getStringValue();
                    FastStringBuffer sb = new FastStringBuffer(cs.length() * 5);
                    sb.append('(');
                    for (int i=0; i<cs.length(); i++) {
                        sb.append((int)cs.charAt(i) + " ");
                    }
                    sb.append(')');
                    System.err.println(sb);
                } catch (XPathException err) {
                    // no-op
                }
                break;

            case COMMENT:
                System.err.println("COMMENT");
                break;

            case PROCESSING_INSTRUCTION:
                System.err.println("PROCESSING_INSTRUCTION");
                break;

            case END_ELEMENT:
                int nc = in.getNameCode();
                System.err.println("END_ELEMENT " + (nc == -1 ? "" : pool.getDisplayName(nc)));
                break;

            case END_DOCUMENT:
                System.err.println("END_DOCUMENT");
                break;

            case END_OF_INPUT:
                System.err.println("END_OF_INPUT");
                break;

            case ATOMIC_VALUE:
                try {
                    System.err.println("ATOMIC VALUE: " + in.getStringValue());
                } catch (XPathException e) {
                    //
                }
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
