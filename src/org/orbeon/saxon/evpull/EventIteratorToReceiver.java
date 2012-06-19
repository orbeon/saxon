package org.orbeon.saxon.evpull;

import org.orbeon.saxon.event.ReceiverOptions;
import org.orbeon.saxon.event.SequenceReceiver;
import org.orbeon.saxon.om.*;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.type.Type;

import java.util.Iterator;

/**
 * Class to read pull events from an EventIterator and write them to a Receiver
 */
public class EventIteratorToReceiver {

    /**
     * Private constructor: this class holds static methods only
     */

    private EventIteratorToReceiver() {}

    /**
     * Read the data obtained from an EventIterator and write the same data to a SequenceReceiver
     * @param in the input EventIterator
     * @param out the output Receiver
     * @throws XPathException
     */

    public static void copy(EventIterator in, SequenceReceiver out) throws XPathException {
        in = EventStackIterator.flatten(in);
        int level = 0;
        out.open();
        while (true) {
            PullEvent event = in.next();
            if (event == null) {
                break;
            }
            if (event instanceof Orphan && ((Orphan)event).getNodeKind() == Type.TEXT) {
                out.characters(((Orphan)event).getStringValueCS(), 0, 0);
            } else if (event instanceof DocumentInfo && level > 0) {
                AxisIterator kids = ((DocumentInfo)event).iterateAxis(Axis.CHILD);
                while (true) {
                    NodeInfo node = (NodeInfo)kids.next();
                    if (node == null) {
                        break;
                    }
                    out.append(node, 0, 0);
                }
            } else if (event instanceof Item) {
                out.append((Item)event, 0, NodeInfo.ALL_NAMESPACES);
            } else if (event instanceof StartElementEvent) {
                StartElementEvent see = (StartElementEvent)event;
                level++;
                out.startElement(see.getNameCode(), see.getTypeCode(), 0, ReceiverOptions.NAMESPACE_OK);
                int[] localNamespaces = see.getLocalNamespaces();
                for (int n=0; n<localNamespaces.length; n++) {
                    int ns = localNamespaces[n];
                    if (ns == -1) {
                        break;
                    }
                    out.namespace(ns, 0);
                }
                if (see.hasAttributes()) {
                    for (Iterator ai=see.iterateAttributes(); ai.hasNext();) {
                        NodeInfo att = (NodeInfo)ai.next();
                        out.attribute(att.getNameCode(), att.getTypeAnnotation(), att.getStringValueCS(), 0, 0);
                    }
                }
                out.startContent();
            } else if (event instanceof EndElementEvent) {
                level--;
                out.endElement();
            } else if (event instanceof StartDocumentEvent) {
                if (level == 0) {
                    out.startDocument(0);
                } else {
                    // output a zero-length text node to prevent whitespace being added between atomic values
                    out.characters("", 0, 0);
                }
                level++;
            } else if (event instanceof EndDocumentEvent) {
                level--;
                if (level == 0) {
                    out.endDocument();
                } else {
                    // output a zero-length text node to prevent whitespace being added between atomic values
                    out.characters("", 0, 0);
                }
            } else {
                throw new AssertionError("Unknown event class " + event.getClass());
            }

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
// The Original Code is: all this file
//
// The Initial Developer of the Original Code is Michael H. Kay.
//
// Contributor(s):
//

