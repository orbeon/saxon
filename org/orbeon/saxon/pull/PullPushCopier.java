package net.sf.saxon.pull;

import net.sf.saxon.event.Receiver;
import net.sf.saxon.trans.XPathException;

/**
 * This class copies a document by using the pull interface to read the first document,
 * and the push interface to write the second.
 */
public class PullPushCopier {

    private PullProvider in;
    private Receiver out;

    public PullPushCopier(PullProvider in, Receiver out) {
        this.out = out;
        this.in = in;
    }

    /**
     * Copy the input to the output
     * @throws XPathException
     */

    public void copy() throws XPathException {
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
