package net.sf.saxon.om;

/**
 * A NamespaceException represents an error condition whereby a QName (for example a variable
 * name or template name) uses a namespace prefix that is not declared
 */

public class NamespaceException extends Exception {

    String prefix;

    public NamespaceException (String prefix) {
       this.prefix = prefix;
    }

    public String getMessage() {
        return "Namespace prefix " + prefix + " has not been declared";
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
// Contributor(s): none
//