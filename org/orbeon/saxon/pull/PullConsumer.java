package org.orbeon.saxon.pull;

import org.orbeon.saxon.trans.XPathException;

/**
 * A PullConsumer consumes all the events supplied by a PullProvider, doing nothing
 * with them. The class exists so that PullFilters on the pipeline can produce side-effects.
 * For example, this class can be used to validate a document, where the side effects are
 * error messages.
 */

public class PullConsumer {

    private PullProvider in;

    public PullConsumer(PullProvider in) {
        this.in = in;
    }

    /**
     * Consume the input
     * @throws org.orbeon.saxon.trans.XPathException
     */

    public void consume() throws XPathException {
        while (true) {
            if (in.next() == PullProvider.END_OF_INPUT) {
                return;
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
