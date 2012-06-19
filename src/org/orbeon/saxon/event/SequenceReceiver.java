package org.orbeon.saxon.event;
import org.orbeon.saxon.Configuration;
import org.orbeon.saxon.om.Item;
import org.orbeon.saxon.om.NamePool;
import org.orbeon.saxon.trans.XPathException;

/**
  * SequenceReceiver: this extension of the Receiver interface is used when processing
  * a sequence constructor. It differs from the Receiver in allowing items (atomic values or
  * nodes) to be added to the sequence, not just tree-building events.
  */

public abstract class SequenceReceiver implements Receiver {

    protected boolean previousAtomic = false;
    protected PipelineConfiguration pipelineConfiguration;
    protected String systemId = null;

    /**
     * Create a SequenceReceiver
     */

    public SequenceReceiver(){}

    public PipelineConfiguration getPipelineConfiguration() {
        return pipelineConfiguration;
    }

    public void setPipelineConfiguration(PipelineConfiguration pipelineConfiguration) {
        this.pipelineConfiguration = pipelineConfiguration;
    }

    /**
     * Get the Saxon Configuration
     * @return the Configuration
     */

    public Configuration getConfiguration() {
        return pipelineConfiguration.getConfiguration();
    }

    /**
    * Set the system ID
    * @param systemId the URI used to identify the tree being passed across this interface
    */

    public void setSystemId(String systemId) {
        this.systemId = systemId;
    }

    /**
    * Get the system ID
    * @return the system ID that was supplied using the setSystemId() method
    */

    public String getSystemId() {
        return systemId;
    }

    /**
    * Notify an unparsed entity URI.
    * @param name The name of the unparsed entity
    * @param systemID The system identifier of the unparsed entity
    * @param publicID The public identifier of the unparsed entity
    */

    public void setUnparsedEntity(String name, String systemID, String publicID) throws XPathException {}

    /**
    * Start the output process
    */

    public void open() throws XPathException {
        previousAtomic = false;
    }

    /**
    * Append an arbitrary item (node or atomic value) to the output
     * @param item the item to be appended
     * @param locationId the location of the calling instruction, for diagnostics
     * @param copyNamespaces if the item is an element node, this indicates whether its namespaces
* need to be copied. Values are {@link org.orbeon.saxon.om.NodeInfo#ALL_NAMESPACES},
* {@link org.orbeon.saxon.om.NodeInfo#LOCAL_NAMESPACES}, {@link org.orbeon.saxon.om.NodeInfo#NO_NAMESPACES}
     */

    public abstract void append(Item item, int locationId, int copyNamespaces) throws XPathException;

    /**
    * Get the name pool
    * @return the Name Pool that was supplied using the setConfiguration() method
    */

    public NamePool getNamePool() {
        return pipelineConfiguration.getConfiguration().getNamePool();
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
// The Initial Developer of the Original Code is Michael H. Kay
//
// Portions created by (your name) are Copyright (C) (your legal entity). All Rights Reserved.
//
// Contributor(s): none.
//
