package org.orbeon.saxon.style;
import org.orbeon.saxon.expr.Expression;
import org.orbeon.saxon.expr.ExpressionTool;
import org.orbeon.saxon.instruct.ApplyImports;
import org.orbeon.saxon.instruct.Executable;
import org.orbeon.saxon.om.*;
import org.orbeon.saxon.type.Type;

import javax.xml.transform.TransformerConfigurationException;

/**
* An xsl:apply-imports element in the stylesheet
*/

public class XSLApplyImports extends StyleElement {


    /**
    * Determine whether this node is an instruction.
    * @return true - it is an instruction
    */

    public boolean isInstruction() {
        return true;
    }

    public void prepareAttributes() throws TransformerConfigurationException {

		AttributeCollection atts = getAttributeList();

		for (int a=0; a<atts.getLength(); a++) {
			int nc = atts.getNameCode(a);
        	checkUnknownAttribute(nc);
        }
    }

    public void validate() throws TransformerConfigurationException {
        checkWithinTemplate();
        AxisIterator kids = iterateAxis(Axis.CHILD);
        while (true) {
            NodeInfo child = (NodeInfo)kids.next();
            if (child == null) {
                break;
            }
            if (child instanceof XSLWithParam) {
                // OK;
            } else if (child.getNodeKind() == Type.TEXT) {
                    // with xml:space=preserve, white space nodes may still be there
                if (!Navigator.isWhite(child.getStringValueCS())) {
                    compileError("No character data is allowed within xsl:apply-imports", "XT0010");
                }
            } else {
                compileError("Child element " + child.getDisplayName() +
                        " is not allowed within xsl:apply-imports", "XT0010");
            }
        }
    }

    public Expression compile(Executable exec) throws TransformerConfigurationException {
        ApplyImports inst = new ApplyImports(backwardsCompatibleModeIsEnabled());
        inst.setActualParameters(getWithParamInstructions(exec, false),
                                 getWithParamInstructions(exec, true));
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
