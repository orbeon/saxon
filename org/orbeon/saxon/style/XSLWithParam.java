package net.sf.saxon.style;
import net.sf.saxon.expr.Expression;
import net.sf.saxon.expr.ExpressionTool;
import net.sf.saxon.instruct.Executable;
import net.sf.saxon.instruct.WithParam;
import net.sf.saxon.om.Axis;
import net.sf.saxon.om.AxisIterator;
import net.sf.saxon.om.Item;
import net.sf.saxon.om.NodeInfo;

import javax.xml.transform.TransformerConfigurationException;

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

    public void validate() throws TransformerConfigurationException {
        super.validate();

        NodeInfo parent = getParent();
        if (!((parent instanceof XSLApplyTemplates) ||
                 (parent instanceof XSLCallTemplate) ||
                 (parent instanceof XSLApplyImports) ||
                 (parent instanceof XSLNextMatch))) {
            compileError("xsl:with-param cannot appear as a child of " + parent.getDisplayName(), "XT0010");
        }

        // Check for duplicate parameter names

        AxisIterator iter = iterateAxis(Axis.PRECEDING_SIBLING);
        while (true) {
            Item prev = iter.next();
            if (prev == null) {
                break;
            }
            if (prev instanceof XSLWithParam) {
                if (this.getVariableFingerprint() == ((XSLWithParam)prev).getVariableFingerprint()) {
                    compileError("Duplicate parameter name", "XT0670");
                }
            }
        }

    }

    public Expression compile(Executable exec) throws TransformerConfigurationException {
        WithParam inst = new WithParam();
        initializeInstruction(exec, inst);
        ExpressionTool.makeParentReferences(inst);
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
