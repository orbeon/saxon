package org.orbeon.saxon.instruct;

import org.orbeon.saxon.expr.Binding;
import org.orbeon.saxon.expr.XPathContext;
import org.orbeon.saxon.om.ValueRepresentation;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.value.SequenceType;

import java.io.Serializable;

/**
 * Run-time object representing a formal argument to a user-defined function
 */
public class UserFunctionParameter implements Binding, Serializable {

    private SequenceType requiredType;
    private int slotNumber;
    private int referenceCount = 999;
        // The initial value is deliberately set to indicate "many" so that it will be assumed a parameter
        // is referenced repeatedly until proved otherwise

    /**
     * Indicate whether the binding is local or global. A global binding is one that has a fixed
     * value for the life of a query or transformation; any other binding is local.
     */

    public final boolean isGlobal() {
        return false;
    }

    /**
    * Test whether it is permitted to assign to the variable using the saxon:assign
    * extension element. This will only be for an XSLT global variable where the extra
    * attribute saxon:assignable="yes" is present.
    */

    public final boolean isAssignable() {
        return false;
    }

    /**
     * If this is a local variable held on the local stack frame, return the corresponding slot number.
     * In other cases, return -1.
     */

    public int getLocalSlotNumber() {
        return slotNumber;
    }

    public void setRequiredType(SequenceType type) {
        requiredType = type;
    }

    public SequenceType getRequiredType() {
        return requiredType;
    }

    public void setReferenceCount(int count) {
        referenceCount = count;
    }

    public int getReferenceCount() {
        return referenceCount;
    }

    public void setSlotNumber(int slot) {
        slotNumber = slot;
    }

    public ValueRepresentation evaluateVariable(XPathContext context) throws XPathException {
        return context.evaluateLocalVariable(slotNumber);
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