package org.orbeon.saxon.instruct;

import org.orbeon.saxon.expr.Binding;
import org.orbeon.saxon.expr.XPathContext;
import org.orbeon.saxon.value.SequenceType;
import org.orbeon.saxon.value.Value;
import org.orbeon.saxon.xpath.XPathException;

import java.io.Serializable;

/**
 * Run-time object representing a formal argument to a user-defined function
 */
public class UserFunctionParameter implements Binding, Serializable {

    private SequenceType requiredType;
    private String variableName;
    private int slotNumber;

    public void setRequiredType(SequenceType type) {
        requiredType = type;
    }

    public SequenceType getRequiredType() {
        return requiredType;
    }

    public void setSlotNumber(int slot) {
        slotNumber = slot;
    }

    public Value evaluateVariable(XPathContext context) throws XPathException {
        return context.evaluateLocalVariable(slotNumber);
        //return context.getController().getBindery().getLocalVariable(slotNumber);
    }

    public void setVariableName(String name) {
        variableName = name;
    }

    public String getVariableName() {
        return variableName;
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