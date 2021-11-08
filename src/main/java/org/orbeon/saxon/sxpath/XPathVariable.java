package org.orbeon.saxon.sxpath;
import org.orbeon.saxon.expr.Binding;
import org.orbeon.saxon.expr.BindingReference;
import org.orbeon.saxon.expr.VariableDeclaration;
import org.orbeon.saxon.expr.XPathContext;
import org.orbeon.saxon.om.StructuredQName;
import org.orbeon.saxon.om.ValueRepresentation;
import org.orbeon.saxon.value.SequenceType;

import java.io.Serializable;


/**
 * An object representing an XPath variable for use in the standalone XPath API. The object
 * can only be created by calling the declareVariable method of class {@link IndependentContext}.
 * Note that once declared, this object is thread-safe: it does not hold the actual variable
 * value, which means it can be used with any number of evaluations of a given XPath expression,
 * in series or in parallel.
 *
 * <p>A variable can be given a value by calling
 * {@link XPathDynamicContext#setVariable(XPathVariable, org.orbeon.saxon.om.ValueRepresentation)}.
 * Note that the value of the variable is not held in the XPathVariable object, but in the
 * XPathDynamicContext, which means that the XPathVariable itself can be used in multiple threads.
*/

public final class XPathVariable implements VariableDeclaration, Binding, Serializable {

    private StructuredQName name;
    private SequenceType requiredType = SequenceType.ANY_SEQUENCE;
    private int slotNumber;

    /**
    * Private constructor: for use only by the protected factory method make()
    */

    private XPathVariable() {}

    /**
     * Factory method, for use by the declareVariable method of class IndependentContext
     * @param name the name of the variable to create
     * @return the constructed XPathVariable
    */

    protected static XPathVariable make(StructuredQName name) {
        XPathVariable v = new XPathVariable();
        v.name = name;
        return v;
    }

    /**
     * Ask whether the binding is local or global. A global binding is one that has a fixed
     * value for the life of a query or transformation; any other binding is local. An XPath
     * variable is treated as a local variable (largely because it is held on the stack frame)
     * @return false (always)
     */

    public boolean isGlobal() {
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
     * Set the required type of this variable. If no required type is specified,
     * the type <code>item()*</code> is assumed.
     * @param requiredType the required type
     */

    public void setRequiredType(SequenceType requiredType) {
        this.requiredType = requiredType;
    }

    /**
     * Get the required type of this variable. If no required type has been specified,
     * the type <code>item()*</code> is returned.
     * @return the required type of the variable
     */

    public SequenceType getRequiredType() {
        return requiredType;
    }

    /**
     * Set the slot number allocated to this variable. This method is for internal use.
     * @param slotNumber the slot number to be allocated
     */

    public void setSlotNumber(int slotNumber) {
        this.slotNumber = slotNumber;
    }

    /**
     * If this is a local variable held on the local stack frame, return the corresponding slot number.
     * In other cases, return -1.
     */

    public int getLocalSlotNumber() {
        return slotNumber;
    }

    /**
     * Get the name of the variable as a QNameValue.
     * @return the name of the variable, as a QNameValue
     */

    public StructuredQName getVariableQName() {
        return name;
    }

    /**
    * Method called by the XPath expression parser to register a reference to this variable.
    * This method should not be called by users of the API.
    */

    public void registerReference(BindingReference ref) {
        ref.setStaticType(requiredType, null, 0);
        ref.fixup(this);
    }

    /**
     * Get the value of the variable. This method is used by the XPath execution engine
     * to retrieve the value. Note that the value is not held within the variable itself,
     * but within the dunamic context.
     * @param context    The dynamic evaluation context
     * @return           The value of the variable
     */

    public ValueRepresentation evaluateVariable(XPathContext context) {
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
// Contributor(s): none.
//
