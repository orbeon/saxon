package net.sf.saxon.xpath;
import net.sf.saxon.expr.Binding;
import net.sf.saxon.expr.BindingReference;
import net.sf.saxon.expr.VariableDeclaration;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.value.EmptySequence;
import net.sf.saxon.value.QNameValue;
import net.sf.saxon.value.SequenceType;
import net.sf.saxon.value.Value;
import net.sf.saxon.om.ValueRepresentation;

import javax.xml.namespace.QName;
import javax.xml.xpath.XPathVariableResolver;


/**
* An object representing an XPath variable for use in the JAXP XPath API. The object
 * is created at compile time when the parser tries to bind a variable reference; the
 * value is fetched at run-time from the XPathVariableResolver. With this interface,
 * there is no way of reporting a static error if the variable has not been declared.
 * <p>
 * In Saxon terms, this class is both a VariableDeclaration and a Binding. Unlike
 * a normal VariableDeclaration, it isn't created in advance, but is created on demand
 * when the parser encounters a variable reference. This actually means that if the
 * XPath expression contains two references to the same variable, two VariableDeclarations
 * will be created; however, they will be indistinguishable to the VariableResolver.
 * Acting as a VariableDeclaration, the object goes through the motions of fixing up
 * a binding to a variable reference (in practice, of course, there is exactly one
 * reference to the variable). Acting as a run-time binding, it then evaluates the
 * variable by calling the XPathVariableResolver supplied by the API caller. If no
 * XPathVariableResolver was supplied, an error is reported when a variable is encountered;
 * but if the variable resolver doesn't recognize the variable name, it returns null,
 * which is treated as an empty sequence.
 * </p>
*/

public final class JAXPVariable implements VariableDeclaration, Binding {

    private QNameValue name;
    private XPathVariableResolver resolver;

    /**
    * Private constructor: for use only be the protected factory method make()
    */

    public JAXPVariable(QNameValue name, XPathVariableResolver resolver) {
        this.name = name;
        this.resolver = resolver;
    };

    /**
     * Indicate whether the binding is local or global. A global binding is one that has a fixed
     * value for the life of a query or transformation; any other binding is local.
     */

    public boolean isGlobal() {
        return true;
    }

    /**
     * Get the name of the variable. Used for diagnostic purposes only.
     * @return the name of the variable, as a string (containing the raw QName)
     */

    public String getVariableName() {
        return name.getStringValue();
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

    public ValueRepresentation evaluateVariable(XPathContext context) throws XPathException {
        Object value = resolver.resolveVariable(
                (QName)name.makeQName(
                        context.getController().getConfiguration()));
        if (value == null) {
            return EmptySequence.getInstance();
        }
        return Value.convertJavaObjectToXPath(
                value, SequenceType.ANY_SEQUENCE, context.getController().getConfiguration());
    }

    QName makeQName(QNameValue in) {
        return new QName(in.getNamespaceURI(), in.getLocalName(), in.getPrefix());
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
