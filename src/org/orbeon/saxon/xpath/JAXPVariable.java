package org.orbeon.saxon.xpath;
import org.orbeon.saxon.expr.*;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.value.*;
import org.orbeon.saxon.om.ValueRepresentation;
import org.orbeon.saxon.om.StructuredQName;
import org.orbeon.saxon.Configuration;

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

    private StructuredQName name;
    private XPathVariableResolver resolver;

    /**
     * Private constructor: for use only be the protected factory method make()
     * @param name the name of the variable
     * @param resolver the resolver used in conjunction with this variable
    */

    protected JAXPVariable(StructuredQName name, XPathVariableResolver resolver) {
        this.name = name;
        this.resolver = resolver;
    }


    public SequenceType getRequiredType() {
        return SequenceType.ANY_SEQUENCE;
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
     * If this is a local variable held on the local stack frame, return the corresponding slot number.
     * In other cases, return -1.
     */

    public int getLocalSlotNumber() {
        return -1;
    }

    /**
     * Get the name of the variable as a structured QName
     */

    public StructuredQName getVariableQName() {
        return name;
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
        Configuration config = context.getConfiguration();
        Object value = resolver.resolveVariable((QName)name.makeQName(config));
        if (value == null) {
            return EmptySequence.getInstance();
        }
        JPConverter converter = JPConverter.allocate(value.getClass(), config);
        return converter.convert(value, context);
        //return Value.convertJavaObjectToXPath(value, SequenceType.ANY_SEQUENCE, context);
    }

    /**
     * Construct a JAXP QName from a Saxon QNameValue
     * @param in the Saxon QNameValue
     * @return the JAXP QName
     */

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
