package net.sf.saxon.xpath;
import net.sf.saxon.expr.Binding;
import net.sf.saxon.expr.BindingReference;
import net.sf.saxon.expr.VariableDeclaration;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.value.EmptySequence;
import net.sf.saxon.value.SequenceType;
import net.sf.saxon.value.Value;


/**
* An object representing an XPath variable for use in the standalone XPath API. The object
* can only be created by calling the declareVariable method of class StandaloneContext.
*/

public final class Variable implements VariableDeclaration, Binding {

    private String name;
    private Value value;

    /**
    * Private constructor: for use only be the protected factory method make()
    */

    private Variable() {};

    /**
    * Protected factory method, for use by the declareVariable method of class StandaloneContext
    */

    public static Variable make(String name) {
        Variable v = new Variable();
        v.name = name;
        return v;
    }

    /**
     * Get the name of the variable. Used for diagnostic purposes only.
     * @return the name of the variable, as a string (containing the raw QName)
     */

    public String getVariableName() {
        return name;
    }

    /**
     * Establish the fingerprint of the name of this variable.
     * Dummy implementation, not used.
     * @return -1, always
     */

    public int getNameCode() {
        return -1;
    }

    /**
     * Assign a value to the variable. This value may be changed between successive evaluations of
     * a compiled XPath expression that references the variable.
     * @param value     the value of the variable
     * @throws XPathException if the Java value cannot be converted to an XPath type
     */

    public void setValue(Object value) throws XPathException {
        this.value = Value.convertJavaObjectToXPath(value, SequenceType.ANY_SEQUENCE, null);
        if (this.value==null) {
            this.value = EmptySequence.getInstance();
        }
    }

    /**
     * Assign a value to the variable. This value may be changed between successive evaluations of
     * a compiled XPath expression that references the variable.
     * @param value     the value of the variable, which must be an instance of a class
     * representing a value in the XPath model.
     */

    public void setXPathValue(Value value) {
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

    public Value evaluateVariable(XPathContext context) {
        return value;
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
