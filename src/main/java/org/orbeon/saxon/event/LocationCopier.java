package org.orbeon.saxon.event;

import org.orbeon.saxon.om.NodeInfo;

/**
 * A Receiver that can be inserted into an event pipeline to copy location information.
 * The class acts as a LocationProvider, so it supports getSystemId() and getLineNumber() methods;
 * the location returned can vary for each node, and is set by the class generating the events.
 * The class is used when it is necessary to copy a subtree along with its location information;
 * for example, when copying an inline schema within a stylesheet to a separate schema document.
 */

public class LocationCopier extends ProxyReceiver implements CopyInformee, LocationProvider {

    private int lineNumber;

    public LocationCopier() {
    }

    public LocationCopier(Receiver nextReceiver) {
        setUnderlyingReceiver(nextReceiver);
    }

    public void setPipelineConfiguration(PipelineConfiguration pipe) {
        PipelineConfiguration pipe2 = new PipelineConfiguration(pipe);
        pipe2.setLocationProvider(this);
        super.setPipelineConfiguration(pipe2);
    }


    /**
     * Provide information about the node being copied. This method is called immediately before
     * the startElement call for the element node in question.
     *
     * @param element the node being copied, which must be an element node
     */

    public void notifyElementNode(NodeInfo element) {
        setSystemId(element.getBaseURI());
        setLineNumber(element.getLineNumber());
    }

    /**
     * Set the line number
     * @param lineNumber the line number
     */

    private void setLineNumber(int lineNumber) {
        this.lineNumber = lineNumber;
    }

    /**
     * Get the line number
     * @return the line number most recently set
     */

    public int getLineNumber() {
		return lineNumber;
	}

    public String getSystemId(long locationId) {
        return getSystemId();
    }

    public int getLineNumber(long locationId) {
        return getLineNumber();
    }

    public int getColumnNumber(long locationId) {
        return -1;
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
// Contributor(s): None
//
