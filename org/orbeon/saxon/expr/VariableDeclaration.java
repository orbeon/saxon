package org.orbeon.saxon.expr;

import org.orbeon.saxon.om.StructuredQName;


/**
* Generic interface representing a variable declaration in the static context of an XPath expression.
* The declaration may be internal or external to the XPath expression itself. An external
* VariableDeclaration is identified (perhaps created) by the bindVariable() method in the StaticContext.
*/

public interface VariableDeclaration {

    /**
     * Method called by a BindingReference to register the variable reference for
     * subsequent fixup.
     * This method is called by the XPath parser when
     * each reference to the variable is encountered. At some time after parsing and before execution of the
     * expression, the VariableDeclaration is responsible for calling the two methods setStaticType()
     * and fixup() on each BindingReference that has been registered with it.<br>
     * @param ref the variable reference
    */

    public void registerReference(BindingReference ref);

    /**
     * Get the name of the variable as a structured QName
     * @return the variable name
    */

    public StructuredQName getVariableQName();

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
