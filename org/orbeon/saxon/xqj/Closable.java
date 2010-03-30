package org.orbeon.saxon.xqj;

import javax.xml.xquery.XQException;

/**
 * This class represents the common ability of many XQJ classes to be closed. Note that closing an object
 * serves no useful purpose in the Saxon implementation; this complex machinery is provided merely to satisfy
 * the XQJ interface, which is designed to accommodate a client-server implementation.
 */
public abstract class Closable {

    private Closable container = null;
    private boolean closed = false;

    /**
     * Set the container of this closable object. Closing the container causes this object to be
     * treated as closed itself
     * @param container the container of this closable object
     */

    public final void setClosableContainer(Closable container) {
        this.container = container;
    }

    /**
     * Close this object
     */

    public final void close() {
        closed = true;
    }

    /**
     * Ask whether this object has been closed.
     * @return true if either the object itself or its container has been closed
     */

    public final boolean isClosed() {
        if (container != null && container.isClosed()) {
            close();
        }
        return closed;
    }

    /**
     * Check whether this object has been closed (either directly, or by closing its container)
     * @throws XQException if the object has been closed
     */

    final void checkNotClosed() throws XQException {
        if (isClosed()) {
            throw new XQException("The XQJ object has been closed");
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
// The Original Code is: all this file
//
// The Initial Developer of the Original Code is Michael H. Kay.
//
// Contributor(s):
//

