package net.sf.saxon.instruct;

import net.sf.saxon.expr.Binding;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.om.ValueRepresentation;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.value.SequenceType;

import java.io.Serializable;

/**
 * Run-time object representing a formal argument to a user-defined function
 */
public class UserFunctionParameter implements Binding, Serializable {

    private SequenceType requiredType;
    private int slotNumber;
    private int referenceCount = 999;

    /**
     * Indicate whether the binding is local or global. A global binding is one that has a fixed
     * value for the life of a query or transformation; any other binding is local.
     */

    public boolean isGlobal() {
        return false;
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