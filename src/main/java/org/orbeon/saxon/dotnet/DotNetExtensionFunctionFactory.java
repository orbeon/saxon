package org.orbeon.saxon.dotnet;
import cli.System.Reflection.MemberInfo;
import org.orbeon.saxon.Configuration;
import org.orbeon.saxon.om.StructuredQName;
import org.orbeon.saxon.functions.ExtensionFunctionFactory;
import org.orbeon.saxon.expr.Expression;

import java.io.Serializable;

/**
* This class acts as a factory for creating expressions that call Java extension functions.
 * A different factory may be registered with the Configuration in order to customize the
 * behaviour. Alternatively, this factory class can be customized by calling setExtensionFunctionClass
 * to nominate a subclass of ExtensionFunctionCall to be used to implement calls on extension functions.
 * <p>Note that this class handles Java extension functions only; a different class, 
 * DotNetExtensionFunctionFactory, is used for .NET extensions.
*/

public class DotNetExtensionFunctionFactory implements ExtensionFunctionFactory, Serializable {

    public DotNetExtensionFunctionFactory(Configuration config) {
        this.config = config;
    }

    private Class extensionFunctionCallClass = DotNetExtensionFunctionCall.class;
    private Configuration config;

    /**
     * Set the class to be used to represent extension function calls. This must be a subclass
     * of {@link org.orbeon.saxon.dotnet.DotNetExtensionFunctionCall}
     * @param subclass the subclass of ExtensionFunctionCall to be used
     */

    public void setExtensionFunctionClass(Class subclass) {
        extensionFunctionCallClass = subclass;
    }

    /**
     * Factory method to create an expression that calls a Java extension function.
     * This is always called at XPath compile time.
     * @param functionName the name of the function
     * @param theClass the Java class containing the extension function
     * @param method The "accessibleObject" representing a constructor, method, or field corresponding
     * to the extension function
     * @param arguments Array containing the expressions supplied as arguments to the function call.
     * @return the constructed ExtensionFunctionCall object (a subclass might return any expression
     * representing the extension function call).
     */

    public Expression makeExtensionFunctionCall(
            StructuredQName functionName, cli.System.Type theClass, MemberInfo method, Expression[] arguments) {
        DotNetExtensionFunctionCall fn;
        try {
            fn = (DotNetExtensionFunctionCall)(extensionFunctionCallClass.newInstance());
        } catch (InstantiationException e) {
            throw new IllegalArgumentException(e.getMessage());
        } catch (IllegalAccessException e) {
            throw new IllegalArgumentException(e.getMessage());
        }
        fn.init(functionName, theClass, method, config);
        fn.setArguments(arguments);
        return fn;
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
// The Initial Developer of the Original Code is Michael H. Kay.
//
// Portions created by (your name) are Copyright (C) (your legal entity). All Rights Reserved.
//
// Contributor(s): Gunther Schadow (changes to allow access to public fields; also wrapping
// of extensions and mapping of null to empty sequence).
//
