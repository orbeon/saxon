package org.orbeon.saxon.s9api;

import org.orbeon.saxon.Configuration;
import org.orbeon.saxon.event.Receiver;
import org.orbeon.saxon.event.TeeOutputter;

/**
 * A TeeDestination allows writing to two destinations at once. For example the output of a transformation
 * can be written simultaneously to a Serializer and to a second Transformation. By chaining together a number
 * of TeeDestinations it is possible to write to any number of destinations at once.
 * @since 9.1
 */

public class TeeDestination implements Destination {

    private Destination dest0;
    private Destination dest1;

    /**
     * Create a TeeDestination: a destination which copies everything that is sent to it to two
     * separate destinations
     * @param destination0 the first destination
     * @param destination1 the second destination
     */

    public TeeDestination(Destination destination0, Destination destination1) {
        dest0 = destination0;
        dest1 = destination1;
    }

    /**
     * Return a Receiver. Saxon calls this method to obtain a Receiver, to which it then sends
     * a sequence of events representing the content of an XML document.
     * @param config The Saxon configuration. This is supplied so that the destination can
     *               use information from the configuration (for example, a reference to the name pool)
     *               to construct or configure the returned Receiver.
     * @return the Receiver to which events are to be sent. It is the caller's responsibility to
     *         initialize this Receiver with a {@link org.orbeon.saxon.event.PipelineConfiguration} before calling
     *         its <code>open()</code> method.
     * @throws org.orbeon.saxon.s9api.SaxonApiException
     *          if the Receiver cannot be created
     */

    public Receiver getReceiver(Configuration config) throws SaxonApiException {
        return new TeeOutputter(dest0.getReceiver(config), dest1.getReceiver(config));
    }
}

//
// The contents of this file are subject to the Mozilla Public License Version
// 1.0 (the "License");
// you may not use this file except in compliance with the License. You may
// obtain a copy of the License at http://www.mozilla.org/MPL/
//
// Software distributed under the License is distributed on an "AS IS" basis,
// WITHOUT WARRANTY OF ANY KIND, either express or implied.
// See the License for the specific language governing rights and limitations
// under the License.
//
// The Original Code is: all this file.
//
// The Initial Developer of the Original Code is Michael Kay,
//
// Portions created by (your name) are Copyright (C) (your legal entity). All
// Rights Reserved.
//
// Contributor(s): none.
//



