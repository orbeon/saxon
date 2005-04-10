package net.sf.saxon.event;

import net.sf.saxon.om.SequenceIterator;
import net.sf.saxon.om.Item;
import net.sf.saxon.om.NodeInfo;
import net.sf.saxon.trans.XPathException;

/**
 * Copies a sequence, supplied as a SequenceIterator, to a push pipeline, represented by
 * a SequenceReceiver
 */
public class SequenceCopier {

    private SequenceCopier() {
    }

    public static void copySequence(SequenceIterator in, SequenceReceiver out) throws XPathException {
        out.open();
        while (true) {
            Item item = in.next();
            if (item == null) {
                break;
            }
            out.append(item, 0, NodeInfo.ALL_NAMESPACES);
        }
        out.close();
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
