package net.sf.saxon.style;
import net.sf.saxon.expr.Expression;
import net.sf.saxon.expr.ExpressionTool;
import net.sf.saxon.instruct.CallTemplate;
import net.sf.saxon.instruct.Executable;
import net.sf.saxon.instruct.Template;
import net.sf.saxon.om.*;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.AnyItemType;
import net.sf.saxon.type.ItemType;
import net.sf.saxon.type.Type;
import net.sf.saxon.value.SequenceType;
import net.sf.saxon.Err;

import javax.xml.transform.TransformerConfigurationException;
import java.util.List;

/**
* An xsl:call-template element in the stylesheet
*/

public class XSLCallTemplate extends StyleElement {

    private int calledTemplateFingerprint = -1;   // the fingerprint of the called template
    private XSLTemplate template = null;
    private boolean useTailRecursion = false;
    private String calledTemplateName = null;       // used only for diagnostics
    private Expression calledTemplateExpression;    // allows name to be an AVT

    /**
     * Determine whether the called template can be specified as an AVT
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

    public void prepareAttributes() throws TransformerConfigurationException {

		AttributeCollection atts = getAttributeList();

        String nameAttribute = null;

		for (int a=0; a<atts.getLength(); a++) {
			int nc = atts.getNameCode(a);
			String f = getNamePool().getClarkName(nc);
			if (f==StandardNames.NAME) {
        		nameAttribute = atts.getValue(a).trim();
        	} else {
        		checkUnknownAttribute(nc);
        	}
        }

        if (nameAttribute==null) {
            reportAbsence("name");
            return;
        }

        if (allowAVT() && nameAttribute.indexOf('{')>=0) {
            calledTemplateExpression = makeAttributeValueTemplate(nameAttribute);
        } else {
            calledTemplateName = nameAttribute;
            try {
                calledTemplateFingerprint =
            	    makeNameCode(nameAttribute.trim()) & 0xfffff;
            } catch (NamespaceException err) {
                compileError(err.getMessage(), "XT0280");
            } catch (XPathException err) {
                compileError(err.getMessage());
            }
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
            } else if (child instanceof XSLFallback && mayContainFallback()) {
                // xsl:fallback is not allowed on xsl:call-template, but is allowed on saxon:call-template (cheat!)
            } else if (child.getNodeKind() == Type.TEXT) {
                    // with xml:space=preserve, white space nodes may still be there
                if (!Navigator.isWhite(child.getStringValueCS())) {
                    compileError("No character data is allowed within xsl:call-template", "XT0010");
                }
            } else {
                compileError("Child element " + Err.wrap(child.getDisplayName(), Err.ELEMENT) +
                        " is not allowed within xsl:call-template", "XT0010");
            }
        }        
        if (calledTemplateExpression==null) {
            template = findTemplate(calledTemplateFingerprint);
            if (template==null) {
                return;
            }
        }
        calledTemplateExpression = typeCheck("name", calledTemplateExpression);
    }

    public void postValidate() throws TransformerConfigurationException {
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
                                ((XSLWithParam)withParam).getVariableFingerprint() ==
                                    ((XSLParam)param).getVariableFingerprint()) {
                            ok = true;
                            break;
                        }
                    }
                    if (!ok) {
                        compileError("No value supplied for required parameter " +
                                Err.wrap(((XSLParam)param).getVariableName(), Err.VARIABLE), "XT0690");
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
                                ((XSLParam)param).getVariableFingerprint() ==
                                    withParam.getVariableFingerprint()) {
                            ok = true;
                            SequenceType required = ((XSLParam)param).getRequiredType();
                            withParam.checkAgainstRequiredType(required);
                            break;
                        }
                    }
                    if (!ok) {
                        if (!backwardsCompatibleModeIsEnabled()) {
                            compileError("Parameter " +
                                    withParam.getVariableName() +
                                    " is not declared in the called template", "XT0680");
                        }
                    }
                }
            }
        }
    }

    private XSLTemplate findTemplate(int fingerprint)
    throws TransformerConfigurationException {

        XSLStylesheet stylesheet = getPrincipalStylesheet();
        List toplevel = stylesheet.getTopLevel();

        // search for a matching template name, starting at the end in case of duplicates.
        // this also ensures we get the one with highest import precedence.

        for (int i=toplevel.size()-1; i>=0; i--) {
            if (toplevel.get(i) instanceof XSLTemplate) {
                XSLTemplate t = (XSLTemplate)toplevel.get(i);
                if (t.getTemplateFingerprint() == fingerprint) {
                    return t;
                }
            }
        }
        compileError("No template exists named " + calledTemplateName, "XT0650");
        return null;
    }

    /**
     * Mark tail-recursive calls on templates and functions.
     * For most instructions, this does nothing.
    */

    public void markTailCalls() {
        useTailRecursion = true;
    }


    public Expression compile(Executable exec) throws TransformerConfigurationException {
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
                                    getWithParamInstructions(exec, false),
                                    getWithParamInstructions(exec, true),
                                    useTailRecursion,
                                    calledTemplateExpression,
                                    nsContext );
        ExpressionTool.makeParentReferences(call);
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
