package org.orbeon.saxon.instruct;

import org.orbeon.saxon.expr.Binding;
import org.orbeon.saxon.expr.XPathContext;
import org.orbeon.saxon.om.ValueRepresentation;
import org.orbeon.saxon.om.StructuredQName;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.value.SequenceType;

import java.io.Serializable;

/**
 * Run-time object representing a formal argument to a user-defined function
 */
public class UserFunctionParameter implements Binding, Serializable {

    private SequenceType requiredType;
    private StructuredQName variableQName;
    private int slotNumber;
    private int referenceCount = 999;
        // The initial value is deliberately set to indicate "many" so that it will be assumed a parameter
        // is referenced repeatedly until proved otherwise
    private boolean isIndexed = false;

    /**
     * Create a UserFunctionParameter
     */

    public UserFunctionParameter(){}

    /**
     * Indicate whether the binding is local or global. A global binding is one that has a fixed
     * value for the life of a query or transformation; any other binding is local.
     * @return false (always)
     */

    public final boolean isGlobal() {
        return false;
    }

    /**
     * Test whether it is permitted to assign to the variable using the saxon:assign
     * extension element. This will only be for an XSLT global variable where the extra
     * attribute saxon:assignable="yes" is present.
     * @return false (always)
    */

    public final boolean isAssignable() {
        return false;
    }

    /**
     * Set the slot number to be used by this parameter
     * @param slot the slot number, that is, the position of the parameter value within the local stack frame
     */

    public void setSlotNumber(int slot) {
        slotNumber = slot;
    }

    /**
     * If this is a local variable held on the local stack frame, return the corresponding slot number.
     * In other cases, return -1.
     * @return the slot number, indicating the position of the parameter on the local stack frame
     */

    public int getLocalSlotNumber() {
        return slotNumber;
    }

    /**
     * Set the required type of this function parameter
     * @param type the declared type of the parameter
     */

    public void setRequiredType(SequenceType type) {
        requiredType = type;
    }

    /**
     * Get the required type of this function parameter
     * @return the declared type of the parameter
     */

    public SequenceType getRequiredType() {
        return requiredType;
    }

    /**
     * Set the name of this parameter
     * @param name the name of the parameter
     */

    public void setVariableQName(StructuredQName name) {
        variableQName = name;
    }

    /**
     * Get the name of this parameter
     * @return the name of this parameter
     */

    public StructuredQName getVariableQName() {
        return variableQName;
    }

    /**
     * Set the (nominal) number of references within the function body to this parameter, where a reference
     * inside a loop is counted as multiple references
     * @param count the nominal number of references
     */

    public void setReferenceCount(int count) {
        referenceCount = count;
    }

    /**
     * Get the (nominal) number of references within the function body to this parameter, where a reference
     * inside a loop is counted as multiple references
     * @return the nominal number of references
     */

    public int getReferenceCount() {
        return referenceCount;
    }

    /**
     * Indicate that this parameter requires (or does not require) support for indexing
     * @param indexed true if support for indexing is required. This will be set if the parameter
     * is used in a filter expression such as $param[@a = 17]
     */

    public void setIndexedVariable(boolean indexed) {
        isIndexed = indexed;
    }

    /**
     * Ask whether this parameter requires support for indexing
     * @return true if support for indexing is required. This will be set if the parameter
     * is used in a filter expression such as $param[@a = 17]
     */

    public boolean isIndexedVariable() {
        return isIndexed;
    }

    /**
     * Evaluate this function parameter
     * @param context the XPath dynamic context
     * @return the value of the parameter
     * @throws XPathException if an error occurs
     */

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