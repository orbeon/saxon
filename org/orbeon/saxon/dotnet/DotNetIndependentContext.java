package org.orbeon.saxon.dotnet;

import org.orbeon.saxon.Configuration;
import org.orbeon.saxon.trans.IndependentContext;
import org.orbeon.saxon.trans.Variable;
import org.orbeon.saxon.value.QNameValue;

/**
 * This is a variant on the IndependentContext used by the free-standing XPath API in the
 * .NET product. It differs from its parent in the way variables are handled. Instead of
 * the Variable object being used both at compile time and at run-time, it is immutable once
 * compiled; run-time values are held instead in the dynamic context stack-frame, allowing
 * compiled XPathExpressions to be executed safely in multiple threads. This change should
 * probably be migrated into the superclass at some stage, but may affect existing interfaces
 * for example for debuggers.
 */
public class DotNetIndependentContext extends IndependentContext {

    /**
	* Create an IndependentContext using a specific Configuration
	*/

	public DotNetIndependentContext(Configuration config) {
        super(config);
    }

    /**
    * Declare a variable. A variable must be declared before an expression referring
    * to it is compiled. The initial value of the variable will be the empty sequence
     * @param qname The name of the variable
    */

    public Variable declareVariable(QNameValue qname) {
        Variable var = super.declareVariable(qname);
        var.setUseStack(true);
        return var;
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
// Contributor(s): none.
//
