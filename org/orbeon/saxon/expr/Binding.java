package org.orbeon.saxon.expr;
import org.orbeon.saxon.om.ValueRepresentation;
import org.orbeon.saxon.om.StructuredQName;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.value.SequenceType;

/**
* Binding is a interface used to represent the run-time properties and methods
* associated with a variable: specifically, a method to get the value
* of the variable.
*/

public interface Binding  {

    /**
     * Get the declared type of the variable
     * @return the declared type
     */

    public SequenceType getRequiredType();

    /**
     * Evaluate the variable
     * @param context the XPath dynamic evaluation context
     * @return the result of evaluating the variable
    */

    public ValueRepresentation evaluateVariable(XPathContext context) throws XPathException;

    /**
     * Indicate whether the binding is local or global. A global binding is one that has a fixed
     * value for the life of a query or transformation; any other binding is local.
     * @return true if the binding is global
     */

    public boolean isGlobal();

    /**
     * Test whether it is permitted to assign to the variable using the saxon:assign
     * extension element. This will only be for an XSLT global variable where the extra
     * attribute saxon:assignable="yes" is present.
     * @return true if the binding is assignable
    */

    public boolean isAssignable();

    /**
     * If this is a local variable held on the local stack frame, return the corresponding slot number.
     * In other cases, return -1.
     * @return the slot number on the local stack frame
     */

    public int getLocalSlotNumber();

    /**
     * Get the name of the variable
     * @return the name of the variable, as a structured QName
     */

    public StructuredQName getVariableQName();

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
