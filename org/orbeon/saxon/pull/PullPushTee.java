package org.orbeon.saxon.pull;

import org.orbeon.saxon.event.Receiver;
import org.orbeon.saxon.om.AttributeCollection;
import org.orbeon.saxon.om.NamespaceDeclarations;
import org.orbeon.saxon.trans.XPathException;

/**
 * PullPushTee is a pass-through filter class that links one PullProvider to another PullProvider
 * in a pipeline, copying all events that are read into a push pipeline, supplied in the form
 * of a Receiver.
 *
 * <p>This class can be used to insert a schema validator into a pull pipeline, since Saxon's schema
 * validation is push-based. It could also be used to insert a serializer into the pipeline, allowing
 * the XML document being "pulled" to be displayed for diagnostic purposes.</p>
*/

public class PullPushTee extends PullFilter {

    private Receiver branch;

    /**
     * Create a PullPushTee
     * @param base the PullProvider to which requests are to be passed
     * @param branch the Receiver to which all events are to be copied, as "push" events
     */

    public PullPushTee(PullProvider base, Receiver branch) {
        super(base);
        this.branch = branch;
    }

    /**
     * Get the Receiver to which events are being tee'd.
     */

    public Receiver getReceiver() {
        return branch;
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
        copyEvent(currentEvent);
        return currentEvent;
    }


    /**
     * Copy a pull event to a Receiver
     */

    private void copyEvent(int event) throws XPathException {
        PullProvider in = getUnderlyingProvider();
        Receiver out = branch;
        switch (event) {
            case START_DOCUMENT:
                out.startDocument(0);
                break;

            case START_ELEMENT:
                out.startElement(in.getNameCode(), in.getTypeAnnotation(), 0, 0);

                NamespaceDeclarations decl = in.getNamespaceDeclarations();
                for (int i=0; i<decl.getLength(); i++) {
                    out.namespace(decl.getNamespaceCode(i), 0);
                }

                AttributeCollection atts = in.getAttributes();
                for (int i=0; i<atts.getLength(); i++) {
                    out.attribute(atts.getNameCode(i), atts.getTypeAnnotation(i),
                            atts.getValue(i), 0, atts.getProperties(i));
                }

                out.startContent();
                break;

            case TEXT:

                out.characters(in.getStringValue(), 0, 0);
                break;

            case COMMENT:

                out.comment(in.getStringValue(), 0, 0);
                break;

            case PROCESSING_INSTRUCTION:

                out.processingInstruction(
                        in.getPipelineConfiguration().getConfiguration().getNamePool().getLocalName(in.getNameCode()),
                        in.getStringValue(), 0, 0);
                break;

            case END_ELEMENT:

                out.endElement();
                break;

            case END_DOCUMENT:

                out.endDocument();
                break;

            case END_OF_INPUT:
                in.close();
                out.close();
                break;
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
