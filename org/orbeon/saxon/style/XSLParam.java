package net.sf.saxon.style;
import net.sf.saxon.instruct.*;
import net.sf.saxon.om.NodeInfo;
import net.sf.saxon.om.Axis;
import net.sf.saxon.om.AxisIterator;
import net.sf.saxon.om.Navigator;
import net.sf.saxon.value.SequenceType;
import net.sf.saxon.expr.*;
import net.sf.saxon.xpath.XPathException;

import javax.xml.transform.TransformerConfigurationException;

/**
* An xsl:param element in the stylesheet. <br>
* The xsl:param element has mandatory attribute name and optional attributes
 *  select, required, as, ...
*/

public class XSLParam extends XSLVariableDeclaration {

    Expression conversion = null;

    protected boolean allowsValue() {
        return !(getParentNode() instanceof XSLFunction);
        // function parameters cannot take a default value
    }

    protected boolean allowsRequired() {
        return !(getParentNode() instanceof XSLFunction);
        // function parameters cannot take the "required" attribute
    }

    protected boolean allowsTunnelAttribute() {
        return true;
    }

    public void validate() throws TransformerConfigurationException {

        NodeInfo parent = (NodeInfo)getParentNode();
        boolean local = (parent instanceof XSLTemplate || parent instanceof XSLFunction);
        global = (parent instanceof XSLStylesheet);

        if (!local && !global) {
            compileError("xsl:param must be immediately within a template, function or stylesheet");
        }

        if (!global) {
            AxisIterator preceding = iterateAxis(Axis.PRECEDING_SIBLING);
            while (true) {
                NodeInfo node = (NodeInfo)preceding.next();
                if (node == null) {
                    break;
                }
                if (node instanceof XSLParam) {
                    if (this.getVariableFingerprint() == ((XSLParam)node).getVariableFingerprint()) {
                        compileError("The name of the parameter is not unique", "XT0580");
                    }
                } else if (node instanceof StyleElement) {
                    compileError("xsl:param must be the first element within a template or function");
                } else {
                    // it must be a text node; allow it if all whitespace
                    if (!Navigator.isWhite(node.getStringValue())) {
                        compileError("xsl:param must not be preceded by text");
                    }
                }
            }
        }

        if (requiredParam) {
            if (select != null) {
                // NB, we do this test before setting the default select attribute
                compileError("The select attribute should be omitted when required='yes'");
            }
            if (hasChildNodes()) {
                compileError("A parameter specifying required='yes' must have empty content");
            }
        }

        super.validate();
    }

    /**
    * Compile: this ensures space is available for local variables declared within
    * this global variable
    */

    public Expression compile(Executable exec) throws TransformerConfigurationException {

        // TODO: deal specially with params that aren't referenced. (These aren't a problem
        // for stylesheet function parameters, the UserFunctionParam object is created but
        // gets garbage collected if there are no variables that refer to it.)

        if (getParent() instanceof XSLFunction) {
            // For Function arguments, the UserFunctionParameter is more efficient than
            // the general-purpose Param object
            UserFunctionParameter arg = new UserFunctionParameter();
            arg.setRequiredType(getRequiredType());
            arg.setSlotNumber(getSlotNumber());
            arg.setVariableName(getVariableName());
            fixupBinding(arg);
            return null;
                // no need to return an instruction in this case, the parameter definition
                // is not executable.
        } else {
            int slot = getSlotNumber();
            if (requiredType != null) {
                try {
                    SuppliedParameterReference pref = new SuppliedParameterReference(slot);
                    pref.setLocationId(staticContext.getLocationMap().allocateLocationId(getSystemId(), getLineNumber()));
                    conversion = TypeChecker.staticTypeCheck(
                            pref,
                            requiredType,
                            false,
                            new RoleLocator(RoleLocator.VARIABLE, getVariableName(), 0), getStaticContext());
                } catch (XPathException z) {
                    throw new TransformerConfigurationException(z);
                }
            }

            GeneralVariable inst;
            if (global) {
                inst = new GlobalParam();
                ((GlobalParam)inst).setExecutable(getExecutable());
                if (select instanceof ComputedExpression) {
                    ((ComputedExpression)select).setParentExpression(inst);
                }
            } else {
                inst = new LocalParam();
                ((LocalParam)inst).setConversion(conversion);
            }
            initializeInstruction(exec, inst);
            inst.setVariableName(getVariableName());
            inst.setSlotNumber(slot);
            inst.setRequiredType(getRequiredType());
            ExpressionTool.makeParentReferences(inst);
            fixupBinding(inst);
            return inst;
        }
    }


    /**
    * Get the static type of the parameter. This is the declared type, because we cannot know
    * the actual value in advance.
    */

    public SequenceType getRequiredType() {
        if (requiredType!=null) {
            return requiredType;
        } else {
            return SequenceType.ANY_SEQUENCE;
        }
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
