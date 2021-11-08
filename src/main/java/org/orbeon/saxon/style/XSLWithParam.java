package org.orbeon.saxon.style;
import org.orbeon.saxon.expr.Expression;
import org.orbeon.saxon.instruct.Executable;
import org.orbeon.saxon.instruct.WithParam;
import org.orbeon.saxon.om.Axis;
import org.orbeon.saxon.om.AxisIterator;
import org.orbeon.saxon.om.Item;
import org.orbeon.saxon.trans.XPathException;

/**
* An xsl:with-param element in the stylesheet. <br>
* The xsl:with-param element has mandatory attribute name and optional attribute select
*/

public class XSLWithParam extends XSLGeneralVariable {

    protected boolean allowsAsAttribute() {
        return true;
    }

    protected boolean allowsTunnelAttribute() {
        return true;
    }

    public void validate() throws XPathException {
        super.validate();

        // Check for duplicate parameter names

        AxisIterator iter = iterateAxis(Axis.PRECEDING_SIBLING);
        while (true) {
            Item prev = iter.next();
            if (prev == null) {
                break;
            }
            if (prev instanceof XSLWithParam) {
                if (this.getVariableQName().equals(((XSLWithParam)prev).getVariableQName())) {
                    compileError("Duplicate parameter name", "XTSE0670");
                }
            }
        }

    }

    public Expression compile(Executable exec) throws XPathException {
        WithParam inst = new WithParam();
        inst.adoptChildExpression(select);
        inst.setParameterId(
                        getPrincipalStylesheet().allocateUniqueParameterNumber(getVariableQName()));
        initializeInstruction(exec, inst);
        return inst;
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
