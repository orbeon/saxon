package org.orbeon.saxon.om;

import java.io.Serializable;

/**
 * This class (which has one instance per Configuration) is used to allocate unique document
 * numbers. It's a separate class so that it can act as a monitor for synchronization
 */
public class DocumentNumberAllocator implements Serializable {

    private int nextDocumentNumber = 0;

    /**
     * Allocate a unique document number
     * @return a unique document number
     */

    public synchronized int allocateDocumentNumber() {
        return nextDocumentNumber++;
    }
}

//
// The contents of this file are subject to the Mozilla Public License Version
// 1.0 (the "License");
// you may not use this file except in compliance with the License. You may
// obtain a copy of the
// License at http://www.mozilla.org/MPL/
//
// Software distributed under the License is distributed on an "AS IS" basis,
// WITHOUT WARRANTY OF ANY KIND, either express or implied.
// See the License for the specific language governing rights and limitations
// under the License.
//
// The Original Code is: all this file.
//
// The Initial Developer of the Original Code is Michael Kay, with extensive
// rewriting by Wolfgang Hoschek
//
// Portions created by (your name) are Copyright (C) (your legal entity). All
// Rights Reserved.
//
// Contributor(s): none.
//
