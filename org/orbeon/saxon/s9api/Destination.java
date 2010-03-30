package org.orbeon.saxon.s9api;

import org.orbeon.saxon.Configuration;
import org.orbeon.saxon.event.Receiver;

/**
 * A Destination represents a place where XML can be sent. It is used, for example,
 * to define the output of a transformation or query.
 *
 * <p>The interface <code>Destination</code> has some similarities with the JAXP
 * {@link javax.xml.transform.Result} class. It differs, however, in that implementations
 * of this interface can be written by users or third parties to define new kinds of
 * destination, and any such implementation can be supplied to the Saxon methods that
 * take a <code>Destination</code> as an argument.</p>
 *
 * <p>Implementing a new <code>Destination</code>, however, will generally require access
 * to implementation-level classes and methods within the Saxon product. The only method that
 * needs to be supplied is {@link #getReceiver}, which returns an instance of
 * {@link org.orbeon.saxon.event.Receiver}, and unless you use an existing implementation of
 * <code>Receiver</code>, you will need to handle internal Saxon concepts such as name codes
 * and name pools.</p>
 */
public interface Destination {

    /**
     * Return a Receiver. Saxon calls this method to obtain a Receiver, to which it then sends
     * a sequence of events representing the content of an XML document.
     * @param config The Saxon configuration. This is supplied so that the destination can
     * use information from the configuration (for example, a reference to the name pool)
     * to construct or configure the returned Receiver.
     * @return the Receiver to which events are to be sent. It is the caller's responsibility to
     * initialize this Receiver with a {@link org.orbeon.saxon.event.PipelineConfiguration} before calling
     * its <code>open()</code> method.
     * @throws SaxonApiException if the Receiver cannot be created
     */

    public Receiver getReceiver(Configuration config) throws SaxonApiException;
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

