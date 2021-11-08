package org.orbeon.saxon.functions;

/**
 * This is a marker interface representing an abstract superclass of JavaExtensionFunctionFactory
 * and DotNetExtensionFunctionFactory. These play equivalent roles in the system: that is, they
 * are responsible for determining how the QNames of extension functions are bound to concrete
 * implementation classes; but they do not share the same interface.
 *
 * <p>This interface was introduced in Saxon 8.9. Prior to that, <code>ExtensionFunctionFactory</code>
 * was a concrete class - the class now named <code>JavaExtensionFunctionFactory</code>.
 */

public interface ExtensionFunctionFactory {
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

