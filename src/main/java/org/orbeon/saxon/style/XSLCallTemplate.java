package org.orbeon.saxon.style;
import org.orbeon.saxon.trans.Err;
import org.orbeon.saxon.expr.Expression;
import org.orbeon.saxon.instruct.CallTemplate;
import org.orbeon.saxon.instruct.Executable;
import org.orbeon.saxon.instruct.Template;
import org.orbeon.saxon.om.*;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.type.AnyItemType;
import org.orbeon.saxon.type.ItemType;
import org.orbeon.saxon.type.Type;
import org.orbeon.saxon.value.SequenceType;
import org.orbeon.saxon.value.Whitespace;

import java.util.List;

/**
* An xsl:call-template element in the stylesheet
*/

public class XSLCallTemplate extends StyleElement {

    private StructuredQName calledTemplateName;   // the name of the called template
    private XSLTemplate template = null;
    private boolean useTailRecursion = false;
    private Expression calledTemplateExpression;    // allows name to be an AVT

    /**
     * Determine whether the called template can be specified as an AVT
     * @return true if the template name can be specified at run-time, that is, if this is a saxon:call-template
     * instruction
     */

    protected boolean allowAVT() {
        return false;
    }

    /**
    * Determine whether this node is an instruction.
    * @return true - it is an instruction
    */

    public boolean isInstruction() {
        return true;
    }

    /**
     * Determine the type of item returned by this instruction (only relevant if
     * it is an instruction).
     * @return the item type returned
     */

    protected ItemType getReturnedItemType() {
        if (template==null) {
            return AnyItemType.getInstance();
        } else {
            return template.getReturnedItemType();
        }
    }

    public void prepareAttributes() throws XPathException {

		AttributeCollection atts = getAttributeList();

        String nameAttribute = null;

		for (int a=0; a<atts.getLength(); a++) {
			int nc = atts.getNameCode(a);
			String f = getNamePool().getClarkName(nc);
			if (f.equals(StandardNames.NAME)) {
        		nameAttribute = Whitespace.trim(atts.getValue(a));
        	} else {
        		checkUnknownAttribute(nc);
        	}
        }

        if (nameAttribute==null) {
            calledTemplateName = new StructuredQName("saxon", NamespaceConstant.SAXON, "error-template");
            reportAbsence("name");
            return;
        }

        if (allowAVT() && nameAttribute.indexOf('{')>=0) {
            calledTemplateExpression = makeAttributeValueTemplate(nameAttribute);
        } else {
            try {
                calledTemplateName = makeQName(nameAttribute);
            } catch (NamespaceException err) {
                calledTemplateName = new StructuredQName("saxon", NamespaceConstant.SAXON, "error-template");
                compileError(err.getMessage(), "XTSE0280");
            } catch (XPathException err) {
                calledTemplateName = new StructuredQName("saxon", NamespaceConstant.SAXON, "error-template");
                compileError(err.getMessage(), err.getErrorCodeLocalPart());
            }
        }
    }

    public void validate() throws XPathException {
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
                    compileError("No character data is allowed within xsl:call-template", "XTSE0010");
                }
            } else {
                compileError("Child element " + Err.wrap(child.getDisplayName(), Err.ELEMENT) +
                        " is not allowed within xsl:call-template", "XTSE0010");
            }
        }
        if (calledTemplateExpression==null &&
                !(calledTemplateName.getNamespaceURI().equals(NamespaceConstant.SAXON) &&
                    calledTemplateName.getLocalName().equals("error-template"))) {
            template = findTemplate(calledTemplateName);
            if (template==null) {
                return;
            }
        }
        calledTemplateExpression = typeCheck("name", calledTemplateExpression);
    }

    public void postValidate() throws XPathException {
        // check that a parameter is supplied for each required parameter
        // of the called template

        if (template != null) {
            AxisIterator declaredParams = template.iterateAxis(Axis.CHILD);
            while(true) {
                NodeInfo param = (NodeInfo)declaredParams.next();
                if (param == null) {
                    break;
                }
                if (param instanceof XSLParam && ((XSLParam)param).isRequiredParam()
                                              && !((XSLParam)param).isTunnelParam()) {
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


            // check that every supplied parameter is declared in the called
            // template

            AxisIterator actualParams = iterateAxis(Axis.CHILD);
            while(true) {
                NodeInfo w = (NodeInfo)actualParams.next();
                if (w == null) {
                    break;
                }
                if (w instanceof XSLWithParam && !((XSLWithParam)w).isTunnelParam()) {
                    XSLWithParam withParam = (XSLWithParam)w;
                    AxisIterator formalParams = template.iterateAxis(Axis.CHILD);
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
                                    " is not declared in the called template", "XTSE0680");
                        }
                    }
                }
            }
        }
    }

    private XSLTemplate findTemplate(StructuredQName templateName)
    throws XPathException {

        XSLStylesheet stylesheet = getPrincipalStylesheet();
        List toplevel = stylesheet.getTopLevel();

        // search for a matching template name, starting at the end in case of duplicates.
        // this also ensures we get the one with highest import precedence.

        for (int i=toplevel.size()-1; i>=0; i--) {
            if (toplevel.get(i) instanceof XSLTemplate) {
                XSLTemplate t = (XSLTemplate)toplevel.get(i);
                if (templateName.equals(t.getTemplateName())) {
                    return t;
                }
            }
        }
        compileError("No template exists named " + calledTemplateName, "XTSE0650");
        return null;
    }

    /**
     * Mark tail-recursive calls on templates and functions.
     * For most instructions, this does nothing.
    */

    public boolean markTailCalls() {
        useTailRecursion = true;
        return true;
    }


    public Expression compile(Executable exec) throws XPathException {
        Template target = null;
        NamespaceResolver nsContext = null;

        if (calledTemplateExpression==null) {
            if (template==null) {
                return null;   // error already reported
            }
            target = template.getCompiledTemplate();
        } else {
            //getPrincipalStyleSheet().setRequireRuntimeTemplateMap(true);
            nsContext = makeNamespaceContext();
        }

        CallTemplate call = new CallTemplate (
                                    target,
                                    useTailRecursion,
                                    calledTemplateExpression,
                                    nsContext );
        call.setActualParameters(getWithParamInstructions(exec, false, call),
                                 getWithParamInstructions(exec, true, call));
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
// The Original Code is: all this file.
//
// The Initial Developer of the Original Code is Michael H. Kay.
//
// Portions created by (your name) are Copyright (C) (your legal entity). All Rights Reserved.
//
// Contributor(s): none.
//
