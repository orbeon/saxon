package org.orbeon.saxon.event;

import org.orbeon.saxon.trans.DynamicError;
import org.orbeon.saxon.Configuration;
import org.orbeon.saxon.type.Type;

/**
* Exception indicating that an attribute or namespace node has been written when
* there is no open element to write it to
*/

public class NoOpenStartTagException extends DynamicError {

    public static NoOpenStartTagException makeNoOpenStartTagException(
            int nodeKind, String name, int hostLanguage, boolean parentIsDocument, boolean isSerializing) {
        String message;
        String errorCode;
        if (parentIsDocument) {
            if (isSerializing) {
                String kind = (nodeKind == Type.ATTRIBUTE ? "attribute" : "namespace");
                String article = (nodeKind == Type.ATTRIBUTE ? "an " : "a ");
                if (hostLanguage == Configuration.XSLT ) {
                    message = "Cannot have " + article + kind + " node (" + name + ") whose parent is a document node";
                    errorCode = "XTDE0420";
                } else {
                    message = "Cannot serialize a free-standing " + kind + " node (" + name + ')';
                    errorCode = "SENR0001";
                }
            } else {
                String kind = (nodeKind == Type.ATTRIBUTE ? "an attribute" : "a namespace");
                message = "Cannot create " + kind + " node (" + name + ") whose parent is a document node";
                errorCode = (hostLanguage == Configuration.XSLT ? "XTDE0420" : "XPTY0004");
            }
        } else {
            String kind = (nodeKind == Type.ATTRIBUTE ? "An attribute" : "A namespace");
            message = kind + " node (" + name + ") cannot be created after the children of the containing element";
            errorCode = (hostLanguage == Configuration.XSLT ? "XTDE0410" : "XQTY0024");
        }
        NoOpenStartTagException err = new NoOpenStartTagException(message);
        err.setErrorCode(errorCode);
        return err;
    }

    public NoOpenStartTagException(String message) {
        super(message);
    }

//    public NoOpenStartTagException(int nodeKind, String name, int hostLanguage, boolean topLevel, boolean isSerializing) {
//        // The contorted conditional here is because super() has to be at the start of the method
//        super((topLevel ?
//                (isSerializing ?
//                   "Cannot serialize ")
//                ("Cannot create " +
//                    (nodeKind==Type.ATTRIBUTE ? "an attribute" : "a namespace") +
//                    " node (" + name + ") whose parent is a document node")
//                :
//                (nodeKind==org.orbeon.saxon.type.Type.ATTRIBUTE ? "An attribute" : "A namespace") +
//                    " node (" + name + ") cannot be created after the children of the containing element"
//                ));
//        if (hostLanguage == Configuration.XSLT) {
//            setErrorCode(topLevel ? "XTDE0420" : "XTDE0410");
//        } else {
//            setErrorCode(topLevel ? "XPTY0004" : "XQTY0024");
//        }
//    }

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