package org.orbeon.saxon.pull;

import org.orbeon.saxon.event.Receiver;
import org.orbeon.saxon.trans.XPathException;

/**
 * This class copies a document by using the pull interface to read the input document,
 * and the push interface to write the output document.
 */
public class PullPushCopier {

    private PullProvider in;
    private Receiver out;

    /**
     * Create a PullPushCopier
     * @param in a PullProvider from which events will be read
     * @param out a Receiver to which copies of the same events will be written
     */

    public PullPushCopier(PullProvider in, Receiver out) {
        this.out = out;
        this.in = in;
    }

    /**
     * Copy the input to the output. This method will open the output Receiver before appending to
     * it, and will close it afterwards.
     * @throws XPathException
     */

    public void copy() throws XPathException {
        out.open();
        PullPushTee tee = new PullPushTee(in, out);
        new PullConsumer(tee).consume();
        out.close();
    }

    /**
     * Copy the input to the output. This method relies on the caller to open the output Receiver before
     * use and to close it afterwards.
     * @throws XPathException
     */

    public void append() throws XPathException {
        PullPushTee tee = new PullPushTee(in, out);
        new PullConsumer(tee).consume();
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
