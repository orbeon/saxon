package org.orbeon.saxon.trans;
import org.orbeon.saxon.Configuration;
import org.orbeon.saxon.expr.Binding;
import org.orbeon.saxon.expr.BindingReference;
import org.orbeon.saxon.expr.VariableDeclaration;
import org.orbeon.saxon.expr.XPathContext;
import org.orbeon.saxon.om.ValueRepresentation;
import org.orbeon.saxon.value.EmptySequence;
import org.orbeon.saxon.value.QNameValue;
import org.orbeon.saxon.value.SequenceType;
import org.orbeon.saxon.value.Value;

import java.io.Serializable;


/**
* An object representing an XPath variable for use in the standalone XPath API. The object
* can only be created by calling the declareVariable method of class StandaloneContext.
*/

public final class Variable implements VariableDeclaration, Binding, Serializable {

    private QNameValue name;
    private ValueRepresentation value;
    private Configuration config;
    private int slotNumber;
    private boolean useStack = false;

    /**
    * Private constructor: for use only by the protected factory method make()
    */

    private Variable() {};

    /**
    * Factory method, for use by the declareVariable method of class StandaloneContext
    */

    public static Variable make(QNameValue name, Configuration config) {
        Variable v = new Variable();
        v.name = name;
        v.config = config;
        return v;
    }

    /**
     * Indicate that values of variables are to be found on the stack, not
     * in the Variable object itself
     */

    public void setUseStack(boolean useStack) {
        this.useStack = true;
    }


    /**
     * Indicate whether the binding is local or global. A global binding is one that has a fixed
     * value for the life of a query or transformation; any other binding is local.
     */

    public boolean isGlobal() {
        return true;
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
     * Set the slot number allocated to this variable
     * @param slotNumber
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
     * Get the name of the variable. Used for diagnostic purposes only.
     * @return the name of the variable, as a string (containing the raw QName)
     */

    public String getVariableName() {
        return name.toString();
    }

    /**
     * Establish the nameCode of the name of this variable.
     * @return the nameCode
     */

    public int getNameCode() {
        return name.allocateNameCode(config.getNamePool());
    }

    /**
     * Assign a value to the variable. This value may be changed between successive evaluations of
     * a compiled XPath expression that references the variable.
     * If this method is called, the value will be set directly within the Variable object. This is only
     * workable if compilation and execution happen in the same thread. The preferred approach is to
     * set the variable in the stack frame of the XPathContext object.
     * @param value     the value of the variable, as a Java object. This is converted to the "best fit"
     * XPath data type.
     * @throws org.orbeon.saxon.trans.XPathException if the Java value cannot be converted to an XPath type
     */

    public void setValue(Object value) throws XPathException {
        this.value = Value.convertJavaObjectToXPath(value, SequenceType.ANY_SEQUENCE, config);
        if (this.value==null) {
            this.value = EmptySequence.getInstance();
        }
    }

    /**
     * Assign a value to the variable. This value may be changed between successive evaluations of
     * a compiled XPath expression that references the variable.
     * If this method is called, the value will be set directly within the Variable object. This is only
     * workable if compilation and execution happen in the same thread. The preferred approach is to
     * set the variable in the stack frame of the XPathContext object.
     * @param value     the value of the variable, which must be an instance of a class
     * representing a value in the XPath model.
     */

    public void setXPathValue(ValueRepresentation value) {
        this.value = value;
        if (this.value==null) {
            this.value = EmptySequence.getInstance();
        }
    }

    /**
    * Method called by the XPath expression parser to register a reference to this variable.
    * This method should not be called by users of the API.
    */

    public void registerReference(BindingReference ref) {
        ref.setStaticType(SequenceType.ANY_SEQUENCE, null, 0);
        ref.fixup(this);
    }

    /**
     * Get the value of the variable. This method is used by the XPath execution engine
     * to retrieve the value.
     * @param context    The dynamic evaluation context
     * @return           The value of the variable
     */

    public ValueRepresentation evaluateVariable(XPathContext context) {
        if (useStack) {
            return context.evaluateLocalVariable(slotNumber);
        } else {
            return value;
        }
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
