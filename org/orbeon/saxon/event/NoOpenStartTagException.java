package net.sf.saxon.event;

import net.sf.saxon.trans.DynamicError;

/**
* Exception indicating that an attribute or namespace node has been written when
* there is no open element to write it to
*/

public class NoOpenStartTagException extends DynamicError {
    
    public NoOpenStartTagException(int nodeKind, boolean topLevel) {
        super((topLevel ?
                ("Cannot create " +
                    (nodeKind==net.sf.saxon.type.Type.ATTRIBUTE ? "an attribute" : "a namespace") +
                    " node whose parent is a document node")
                :
                (nodeKind==net.sf.saxon.type.Type.ATTRIBUTE ? "Attribute" : "Namespace") +
                    " nodes must be created before the children of an element node"
                ));
        setErrorCode(topLevel ? "XT0410" : "XT0420");
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