package org.orbeon.saxon.style;

import org.orbeon.saxon.trans.Err;
import org.orbeon.saxon.expr.ContinueInstr;
import org.orbeon.saxon.expr.Expression;
import org.orbeon.saxon.instruct.Executable;
import org.orbeon.saxon.om.Axis;
import org.orbeon.saxon.om.AxisIterator;
import org.orbeon.saxon.om.NodeInfo;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.type.Type;
import org.orbeon.saxon.value.SequenceType;
import org.orbeon.saxon.value.Whitespace;

/**
* A saxon:continue element in the stylesheet
*/

public class SaxonContinue extends SaxonBreak {



    public void validate() throws XPathException {
        validatePosition();
        if (saxonIterate == null) {
            compileError("saxon:continue must be a descendant of a saxon:iterate instruction");
        }
        AxisIterator kids = iterateAxis(Axis.CHILD);
        while (true) {
            NodeInfo child = (NodeInfo)kids.next();
            if (child == null) {
                break;
            }
            if (child instanceof XSLWithParam) {
                // OK;
            } else if (child instanceof XSLFallback && mayContainFallback()) {
                // xsl:fallback is not allowed on xsl:call-template, but is allowed on saxon:call-template (cheat!)
            } else if (child.getNodeKind() == Type.TEXT) {
                    // with xml:space=preserve, white space nodes may still be there
                if (!Whitespace.isWhite(child.getStringValueCS())) {
                    compileError("No character data is allowed within saxon:continue", "XTSE0010");
                }
            } else {
                compileError("Child element " + Err.wrap(child.getDisplayName(), Err.ELEMENT) +
                        " is not allowed within saxon:continue", "XTSE0010");
            }
        }

    }

    public void postValidate() throws XPathException {
        // check that a parameter is supplied for each required parameter
        // of the containing saxon:iterate instruction

        if (saxonIterate != null) {
            AxisIterator declaredParams = saxonIterate.iterateAxis(Axis.CHILD);
            while(true) {
                NodeInfo param = (NodeInfo)declaredParams.next();
                if (param == null) {
                    break;
                }
                if (param instanceof XSLParam && ((XSLParam)param).isRequiredParam()) {
                    AxisIterator actualParams = iterateAxis(Axis.CHILD);
                    boolean ok = false;
                    while(true) {
                        NodeInfo withParam = (NodeInfo)actualParams.next();
                        if (withParam == null) {
                            break;
                        }
                        if (withParam instanceof XSLWithParam &&
                                ((XSLWithParam)withParam).getVariableQName().equals(
                                    ((XSLParam)param).getVariableQName())) {
                            ok = true;
                            break;
                        }
                    }
                    if (!ok) {
                        compileError("No value supplied for required parameter " +
                                Err.wrap(((XSLParam)param).getVariableDisplayName(), Err.VARIABLE), "XTSE0690");
                    }
                }
            }


            // check that every supplied parameter is declared in the saxon:iterate instruction

            AxisIterator actualParams = iterateAxis(Axis.CHILD);
            while(true) {
                NodeInfo w = (NodeInfo)actualParams.next();
                if (w == null) {
                    break;
                }
                if (w instanceof XSLWithParam && !((XSLWithParam)w).isTunnelParam()) {
                    XSLWithParam withParam = (XSLWithParam)w;
                    AxisIterator formalParams = saxonIterate.iterateAxis(Axis.CHILD);
                    boolean ok = false;
                    while(true) {
                        NodeInfo param = (NodeInfo)formalParams.next();
                        if (param == null) {
                            break;
                        }
                        if (param instanceof XSLParam &&
                                ((XSLParam)param).getVariableQName().equals(withParam.getVariableQName())) {
                            ok = true;
                            SequenceType required = ((XSLParam)param).getRequiredType();
                            withParam.checkAgainstRequiredType(required);
                            break;
                        }
                    }
                    if (!ok) {
                        if (!backwardsCompatibleModeIsEnabled()) {
                            compileError("Parameter " +
                                    withParam.getVariableDisplayName() +
                                    " is not declared in the containing saxon:iterate instruction", "XTSE0680");
                        }
                    }
                }
            }
        }
    }


    public Expression compile(Executable exec) throws XPathException {
        ContinueInstr call = new ContinueInstr (null);
        call.setParameters(getWithParamInstructions(exec, false, call));
        return call;
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
// The Original Code is: all this file
//
// The Initial Developer of the Original Code is Michael H. Kay.
//
// Contributor(s):
//

