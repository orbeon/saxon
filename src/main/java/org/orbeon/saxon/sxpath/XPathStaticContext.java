package org.orbeon.saxon.sxpath;

import org.orbeon.saxon.expr.Container;
import org.orbeon.saxon.expr.StaticContext;
import org.orbeon.saxon.instruct.Executable;
import org.orbeon.saxon.instruct.SlotManager;
import org.orbeon.saxon.om.NamespaceResolver;
import org.orbeon.saxon.value.QNameValue;

/**
 * This interface defines methods that must be provided when Saxon's free-standing XPath API is used.
 * The default implementation of this interface is {@link org.orbeon.saxon.sxpath.IndependentContext}, and
 * that implementation should be adequate for most purposes; but for extra customization, a user-written
 * implementation of this interface may be used instead.
 */
public interface XPathStaticContext extends StaticContext, Container {

    /**
     * Get the executable associated with this static context. The Executable generally holds details
     * of function libraries and collations. For freestanding XPath expressions, there will generally
     * be a single executable corresponding one-to-one with the static context object, and which can be
     * created as soon as the Configuration is known.
     * @return the Executable
     */

    public Executable getExecutable();

    /**
     * Set the default namespace for elements and types
     * @param uri The namespace to be used to qualify unprefixed element names and type names appearing
     * in the XPath expression.
     */

    public void setDefaultElementNamespace(String uri);

    /**
     * Set an external namespace resolver. If this is set, then all resolution of namespace
     * prefixes is delegated to the external namespace resolver, and namespaces declared
     * individually on this IndependentContext object are ignored.
     * @param resolver the external namespace resolver
     */

    public void setNamespaceResolver(NamespaceResolver resolver);

    /**
     * Declare a variable. A variable must be declared before an expression referring
     * to it is compiled. The initial value of the variable will be the empty sequence.
     * This method backs up the {@link XPathEvaluator#declareVariable} method.
     * @param qname The name of the variable
     * @return a Variable object representing information about the variable that has been
     * declared.
    */

    public XPathVariable declareVariable(QNameValue qname);

    /**
     * Declare a variable. A variable must be declared before an expression referring
     * to it is compiled. The initial value of the variable will be the empty sequence.
     * This method backs up the {@link XPathEvaluator#declareVariable} method.
     * @param namespaceURI The namespace URI of the name of the variable. Supply "" to represent
     * names in no namespace (null is also accepted)
     * @param localName The local part of the name of the variable (an NCName)
     * @return an XPathVariable object representing information about the variable that has been
     * declared.
    */

    public XPathVariable declareVariable(String namespaceURI, String localName);

    /**
     * Get a Stack Frame Map containing definitions of all the declared variables. This will return a newly
     * created object that the caller is free to modify by adding additional variables, without affecting
     * the static context itself.
     * @return a SlotManager object holding details of the allocation of variables on the stack frame.
     */

    public SlotManager getStackFrameMap();


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
// The Original Code is: all this file
//
// The Initial Developer of the Original Code is Michael H. Kay.
//
// Contributor(s):
//

