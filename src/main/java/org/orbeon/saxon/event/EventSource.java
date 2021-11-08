package org.orbeon.saxon.event;

import org.orbeon.saxon.trans.XPathException;

import javax.xml.transform.Source;

/**
 * An implementation of the JAXP Source class that supplies a document in the form of a stream
 * of push events sent to a Receiver
 * @since 9.1
 */
public abstract class EventSource implements Source {

    private String systemId;

    /**
     * Set the system identifier for this Source.
     * <p/>
     * <p>The system identifier is optional if the source does not
     * get its data from a URL, but it may still be useful to provide one.
     * The application can use a system identifier, for example, to resolve
     * relative URIs and to include in error messages and warnings.</p>
     * @param systemId The system identifier as a URL string.
     */
    public void setSystemId(String systemId) {
        this.systemId = systemId;
    }

    /**
     * Get the system identifier that was set with setSystemId.
     * @return The system identifier that was set with setSystemId, or null
     *         if setSystemId was not called.
     */
    public String getSystemId() {
        return systemId;
    }

    /**
     * Supply events to a Receiver.
     * @param out the Receiver to which events will be sent. It is the caller's responsibility
     * to initialize the receiver with a PipelineConfiguration, and to call the open() and close()
     * methods on the receiver before and after calling this send() method.
     */

    public abstract void send(Receiver out) throws XPathException;
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
// Contributor(s): none.
//

