package org.orbeon.saxon.expr;

import org.orbeon.saxon.om.ValueRepresentation;
import org.orbeon.saxon.trans.XPathException;

/**
 * Variable reference: a reference to a local variable. This subclass of VariableReference
 * bypasses the Binding object to get the value directly from the relevant slot in the local
 * stackframe.
 */

public class LocalVariableReference extends VariableReference {

    int slotNumber = -999;

    /**
     * Create a local variable reference. The binding and slot number will be supplied later
     */

    public LocalVariableReference() {
    }

    /**
     * Create a LocalVariableReference bound to a given Binding
     * @param binding the binding (that is, the declaration of this local variable)
     */

    public LocalVariableReference(Binding binding) {
        super(binding);
    }

    /**
     * Create a clone copy of this VariableReference
     * @return the cloned copy
     */

    public Expression copy() {
        if (binding == null) {
            throw new UnsupportedOperationException("Cannot copy a variable reference whose binding is unknown");
        }
        LocalVariableReference ref = new LocalVariableReference();
        ref.binding = binding;
        ref.staticType = staticType;
        ref.slotNumber = slotNumber;
        ref.constantValue = constantValue;
        ref.displayName = displayName;
        ExpressionTool.copyLocationInfo(this, ref);
        return ref;
    }

    /**
     * Set the slot number for this local variable, that is, its position in the local stack frame
     * @param slotNumber the slot number to be used
     */

    public void setSlotNumber(int slotNumber) {
        this.slotNumber = slotNumber;
    }

    /**
     * Get the slot number allocated to this local variable
     * @return the slot number
     */

    public int getSlotNumber() {
        return slotNumber;
    }

    /**
     * Return the value of the variable
     * @param c the XPath dynamic context
     * @return the value of the variable
     * @throws XPathException if any dynamic error occurs while evaluating the variable
     */

    public ValueRepresentation evaluateVariable(XPathContext c) throws XPathException {
        try {
            return c.getStackFrame().slots[slotNumber];
        } catch (ArrayIndexOutOfBoundsException err) {
            if (slotNumber == -999) {
                throw new ArrayIndexOutOfBoundsException("Local variable has not been allocated a stack frame slot");
            }
            throw err;
        }
    }

    /**
     * Replace this VariableReference where appropriate by a more efficient implementation. This
     * can only be done after all slot numbers are allocated. The efficiency is gained by binding the
     * VariableReference directly to a local or global slot, rather than going via the Binding object
     *
     * @param parent the parent expression of this variable reference
     */

//    public void refineVariableReference(Expression parent) {
//        // no-op
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
// Contributor(s):
//
