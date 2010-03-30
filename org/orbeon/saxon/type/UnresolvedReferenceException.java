package org.orbeon.saxon.type;

/**
 * This exception occurs when an attempt is made to dereference a reference from one
 * schema component to another, if the target of the reference cannot be found. Note that
 * an unresolved reference is not necessarily an error: a schema containing unresolved
 * references may be used for validation, provided the components containing the
 * unresolved references are not actually used.
 */

public abstract class UnresolvedReferenceException extends RuntimeException {

    public UnresolvedReferenceException(String ref) {
        super(ref);
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
