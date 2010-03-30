package org.orbeon.saxon.style;
import org.orbeon.saxon.expr.Expression;
import org.orbeon.saxon.instruct.Assign;
import org.orbeon.saxon.instruct.Executable;
import org.orbeon.saxon.trans.XPathException;

/**
* saxon:assign element in stylesheet.
* The saxon:assign element has mandatory attribute name and optional attribute expr.
* It also allows xsl:extension-element-prefixes etc.
*/

public class SaxonAssign extends XSLGeneralVariable  {

    private Assign instruction = new Assign();

    /**
    * Determine whether this node is an instruction.
    * @return true - it is an instruction
    */

    public boolean isInstruction() {
        return true;
    }

    public boolean isAssignable() {     // this variable is assignable by definition
        return true;
    }

    protected boolean allowsAsAttribute() {
        return false;
    }

    public void validate() throws XPathException {
        super.validate();
        XSLVariableDeclaration declaration;
        try {
            declaration = bindVariable(getVariableQName());
            declaration.registerReference(instruction);
            requiredType = declaration.getRequiredType();
        } catch (XPathException err) {
            // variable not declared
            compileError(err.getMessage());
            return;
        }
        if (!declaration.isAssignable()) {
            compileError("Variable " + getVariableDisplayName() + " is not marked as assignable");
        }
        if (!declaration.isGlobal()) {
            compileError("saxon:assign now works only with global variables");
        }
    }

    public Expression compile(Executable exec) throws XPathException {
        initializeInstruction(exec, instruction);
        return instruction;
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
