package net.sf.saxon.event;
import net.sf.saxon.Configuration;
import net.sf.saxon.om.Item;
import net.sf.saxon.om.NamePool;
import net.sf.saxon.xpath.XPathException;

/**
  * SequenceReceiver: this extension of the Receiver interface is used when processing
  * a sequence constructor. It differs from the Receiver in allowing items (atomic values or
  * nodes) to be added to the sequence, not just tree-building events.
  */

public abstract class SequenceReceiver implements Receiver {

    protected boolean previousAtomic = false;

    protected PipelineConfiguration pipelineConfiguration;

    public SequenceReceiver(){}

    public PipelineConfiguration getPipelineConfiguration() {
        return pipelineConfiguration;
    }

    public void setPipelineConfiguration(PipelineConfiguration pipelineConfiguration) {
        this.pipelineConfiguration = pipelineConfiguration;
    }

    public Configuration getConfiguration() {
        return pipelineConfiguration.getConfiguration();
    }

    public void setSystemId(String systemId) {}

    public String getSystemId() {
        return null;
    }

    public void setUnparsedEntity(String name, String systemId, String publicId) throws XPathException {}

    /**
    * Start the output process
    */

    public void open() throws XPathException {
        previousAtomic = false;
    }

    /**
    * Output an item (atomic value or node) to the sequence
    */
    
    public abstract void append(Item item, int locationId) throws XPathException;

    /**
    * Get the name pool
    * @return the Name Pool that was supplied using the setConfiguration() method
    */

    public NamePool getNamePool() {
        return getConfiguration().getNamePool();
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
