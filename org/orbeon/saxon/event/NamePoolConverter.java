package org.orbeon.saxon.event;
import org.orbeon.saxon.om.NamePool;
import org.orbeon.saxon.trans.XPathException;

/**
* This class is a filter that passes all Receiver events through unchanged,
* except that it changes namecodes to allow for the source and the destination
* using different NamePools. This is necessary when a stylesheet has been constructed
* as a general document (e.g. as the result of a transformation) and is passed to
* newTemplates() to be compiled as a stylesheet.
*
* @author Michael Kay
*/


public class NamePoolConverter extends ProxyReceiver {

    NamePool oldPool;
    NamePool newPool;

    /**
    * Constructor
    */

    public NamePoolConverter(NamePool oldPool, NamePool newPool) {
        this.oldPool = oldPool;
        this.newPool = newPool;
    }

    /**
    * Set the underlying emitter. This call is mandatory before using the Emitter.
    * This version is modified from that of the parent class to avoid setting the namePool
    * of the destination Receiver.
    */

    public void setUnderlyingReceiver(Receiver receiver) {
        nextReceiver = receiver;
    }

    /**
    * Output element start tag
    */

    public void startElement(int nameCode, int typeCode, int locationId, int properties) throws XPathException {
        int nc = newPool.allocate(oldPool.getPrefix(nameCode), oldPool.getURI(nameCode), oldPool.getLocalName(nameCode));
        nextReceiver.startElement(nc, typeCode, locationId, properties);
    }

    /**
    * Handle a namespace
    */

    public void namespace(int namespaceCode, int properties) throws XPathException {
        int nc = newPool.allocateNamespaceCode(oldPool.getPrefixFromNamespaceCode(namespaceCode),
                                                oldPool.getURIFromNamespaceCode(namespaceCode));
        nextReceiver.namespace(nc, properties);
    }

    /**
    * Handle an attribute
    */

    public void attribute(int nameCode, int typeCode, CharSequence value, int locationId, int properties)
    throws XPathException {
        int nc = newPool.allocate(oldPool.getPrefix(nameCode), oldPool.getURI(nameCode), oldPool.getLocalName(nameCode));
        nextReceiver.attribute(nc, typeCode, value, locationId, properties);
    }

};

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

