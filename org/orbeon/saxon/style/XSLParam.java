package net.sf.saxon.style;
import net.sf.saxon.expr.*;
import net.sf.saxon.instruct.*;
import net.sf.saxon.om.Axis;
import net.sf.saxon.om.AxisIterator;
import net.sf.saxon.om.Navigator;
import net.sf.saxon.om.NodeInfo;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.value.SequenceType;

/**
* An xsl:param element in the stylesheet. <br>
* The xsl:param element has mandatory attribute name and optional attributes
 *  select, required, as, ...
*/

public class XSLParam extends XSLVariableDeclaration {

    Expression conversion = null;

    protected boolean allowsValue() {
        return !(getParent() instanceof XSLFunction);
        // function parameters cannot take a default value
    }

    protected boolean allowsRequired() {
        return !(getParent() instanceof XSLFunction);
        // function parameters cannot take the "required" attribute
    }

    protected boolean allowsTunnelAttribute() {
        return true;
    }

    public void validate() throws XPathException {

        NodeInfo parent = getParent();
        boolean local = (parent instanceof XSLTemplate || parent instanceof XSLFunction);
        global = (parent instanceof XSLStylesheet);

        if (!local && !global) {
            compileError("xsl:param must be immediately within a template, function or stylesheet", "XTSE0010");
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
                        compileError("The name of the parameter is not unique", "XTSE0580");
                    }
                } else if (node instanceof StyleElement) {
                    compileError("xsl:param must be the first element within a template or function", "XTSE0010");
                } else {
                    // it must be a text node; allow it if all whitespace
                    if (!Navigator.isWhite(node.getStringValueCS())) {
                        compileError("xsl:param must not be preceded by text", "XTSE0010");
                    }
                }
            }

            SlotManager p = getContainingSlotManager();
            if (p==null) {
                compileError("Local variable must be declared within a template or function", "XTSE0010");
            } else {
                setSlotNumber(p.allocateSlotNumber(getVariableFingerprint()));
            }

        }

        if (requiredParam) {
            if (select != null) {
                // NB, we do this test before setting the default select attribute
                compileError("The select attribute should be omitted when required='yes'", "XTSE0010");
            }
            if (hasChildNodes()) {
                compileError("A parameter specifying required='yes' must have empty content", "XTSE0010");
            }
        }

        super.validate();
    }

    /**
    * Compile: this ensures space is available for local variables declared within
    * this global variable
    */

    public Expression compile(Executable exec) throws XPathException {

        if (getParent() instanceof XSLFunction) {
            // Do nothing. We did everything necessary while compiling the XSLFunction element.
            return null;
        } else {
            int slot = getSlotNumber();
            if (requiredType != null) {
                //try {
                    SuppliedParameterReference pref = new SuppliedParameterReference(slot);
                    pref.setLocationId(staticContext.getLocationMap().allocateLocationId(getSystemId(), getLineNumber()));
                    RoleLocator role = new RoleLocator(RoleLocator.VARIABLE, getVariableName(), 0, null);
                    role.setSourceLocator(new ExpressionLocation(this));
                    conversion = TypeChecker.staticTypeCheck(
                            pref,
                            requiredType,
                            false,
                            role, getStaticContext());
//                } catch (XPathException z) {
//                    throw new TransformerConfigurationException(z);
//                }
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
