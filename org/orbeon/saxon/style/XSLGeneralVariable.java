package net.sf.saxon.style;
import net.sf.saxon.expr.ErrorExpression;
import net.sf.saxon.expr.Expression;
import net.sf.saxon.expr.RoleLocator;
import net.sf.saxon.expr.TypeChecker;
import net.sf.saxon.instruct.*;
import net.sf.saxon.om.*;
import net.sf.saxon.pattern.NodeKindTest;
import net.sf.saxon.type.ItemType;
import net.sf.saxon.type.Type;
import net.sf.saxon.value.Cardinality;
import net.sf.saxon.value.EmptySequence;
import net.sf.saxon.value.SequenceType;
import net.sf.saxon.value.StringValue;
import net.sf.saxon.xpath.XPathException;

import javax.xml.transform.TransformerConfigurationException;

/**
* This class defines common behaviour across xsl:variable, xsl:param, and xsl:with-param
*/

public abstract class XSLGeneralVariable extends StyleElement {

    protected Expression select = null;
    protected SequenceType requiredType = null;
    protected String constantText = null;
    protected boolean global;
    protected SlotManager slotManager = null;  // used only for global variable declarations
    protected boolean assignable = false;
    protected boolean redundant = false;
    protected boolean requiredParam = false;
    protected boolean tunnel = false;
    private boolean textonly;

    /**
     * Determine the type of item returned by this instruction (only relevant if
     * it is an instruction).
     * @return the item type returned. This is null for a variable: we are not
     * interested in the type of the variable, but in what the xsl:variable constributes
     * to the result of the sequence constructor it is part of.
     */

    protected ItemType getReturnedItemType() {
        return null;
    }
    /**
    * Determine whether this type of element is allowed to contain a template-body
    * @return true: yes, it may contain a template-body
    */

    public boolean mayContainSequenceConstructor() {
        return true;
    }

    protected boolean allowsAsAttribute() {
        return true;
    }

    protected boolean allowsTunnelAttribute() {
        return false;
    }

    protected boolean allowsValue() {
        return true;
    }

    protected boolean allowsRequired() {
        return false;
    }

    /**
    * Test whether it is permitted to assign to the variable using the saxon:assign
    * extension element. This will only be true if the extra attribute saxon:assignable="yes"
    * is present.
    */

    public boolean isAssignable() {
        return assignable;
    }

    public boolean isTunnelParam() {
        return tunnel;
    }

    public boolean isRequiredParam() {
        return requiredParam;
    }

    public boolean isGlobal() {
        return global;
    }

    /**
    * Get the display name of the variable.
    */

    public String getVariableName() {
    	return getAttributeValue(StandardNames.NAME);
    }

    /**
    * Mark this global variable as redundant. This is done before prepareAttributes is called.
    */

    public void setRedundant() {
        redundant = true;
    }

    /**
    * Get the fingerprint of the variable name
    */

    public int getVariableFingerprint() {

        // if an expression has a forwards reference to this variable, getNameCode() can be
        // called before prepareAttributes() is called. We need to allow for this. But we'll
        // deal with any errors when we come round to processing this attribute, to avoid
        // duplicate error messages

        // TODO: this won't establish the requiredType in time to optimize an expression containing
        // a forwards reference to the variable

        if (getObjectNameCode()==-1) {
            String nameAttribute = getAttributeValue(StandardNames.NAME);
            if (nameAttribute==null) {
                return -1;              // we'll report the error later
            }
            try {
                setObjectNameCode(makeNameCode(nameAttribute.trim()));
            } catch (NamespaceException err) {
                setObjectNameCode(-1);
            } catch (XPathException err) {
                setObjectNameCode(-1);;
            }
        }
        return getObjectFingerprint();
    }

    public void prepareAttributes() throws TransformerConfigurationException {

        getVariableFingerprint();

		AttributeCollection atts = getAttributeList();

		String selectAtt = null;
        String assignAtt = null;
        String nameAtt = null;
        String asAtt = null;
        String requiredAtt = null;
        String tunnelAtt = null;

		for (int a=0; a<atts.getLength(); a++) {
			int nc = atts.getNameCode(a);
			String f = getNamePool().getClarkName(nc);
			if (f==StandardNames.NAME) {
        		nameAtt = atts.getValue(a).trim();
        	} else if (f==StandardNames.SELECT) {
        		selectAtt = atts.getValue(a);
        	} else if (f==StandardNames.AS && allowsAsAttribute()) {
        		asAtt = atts.getValue(a);
        	} else if (f==StandardNames.REQUIRED && allowsRequired()) {
        		requiredAtt = atts.getValue(a).trim();
            } else if (f==StandardNames.TUNNEL && allowsTunnelAttribute()) {
        		tunnelAtt = atts.getValue(a).trim();
        	} else if (f==StandardNames.SAXON_ASSIGNABLE && this instanceof XSLVariableDeclaration) {
        		assignAtt = atts.getValue(a).trim();
        	} else {
        		checkUnknownAttribute(nc);
        	}
        }

        if (nameAtt==null) {
            reportAbsence("name");
        } else {
            try {
                setObjectNameCode(makeNameCode(nameAtt.trim()));
            } catch (NamespaceException err) {
                compileError(err.getMessage());
            } catch (XPathException err) {
                compileError(err.getMessage());
            }
        }

        if (selectAtt!=null) {
            if (!allowsValue()) {
                compileError("Function parameters cannot have a default value", "XT0760");
            }
            select = makeExpression(selectAtt);
        }

        if (assignAtt!=null && assignAtt.equals("yes")) {
            assignable=true;
        }

        if (requiredAtt!=null) {
            if (requiredAtt.equals("yes")) {
                requiredParam = true;
            } else if (requiredAtt.equals("no")) {
                requiredParam = false;
            } else {
                compileError("The attribute 'required' must be set to 'yes' or 'no'", "XT0020");
            }
        }

        if (tunnelAtt!=null) {
            if (tunnelAtt.equals("yes")) {
                tunnel = true;
            } else if (tunnelAtt.equals("no")) {
                tunnel = false;
            } else {
                compileError("The attribute 'tunnel' must be set to 'yes' or 'no'", "XT0020");
            }
        }

        if (asAtt!=null) {
            requiredType = makeSequenceType(asAtt);
        }
    }

    public void validate() throws TransformerConfigurationException {
        global = (getParentNode() instanceof XSLStylesheet);

        if (global) {
            slotManager = getConfiguration().makeSlotManager();
        }
        if (select!=null && hasChildNodes()) {
            compileError("An " + getDisplayName() + " element with a select attribute must be empty", "XT0620");
        }

        if (assignable && !global) {
            compileError("saxon:assignable='yes' is no longer permitted for local variables");
        }

        checkAgainstRequiredType(requiredType);

        if (select==null && allowsValue()) {
            textonly = true;
            AxisIterator kids = iterateAxis(Axis.CHILD);
            NodeInfo first = (NodeInfo)kids.next();
            if (first == null) {
                if (requiredType == null) {
                    select = StringValue.EMPTY_STRING;
                } else {
                    if (this instanceof XSLParam) {
                        if (!requiredParam) {
                            if (Cardinality.allowsZero(requiredType.getCardinality())) {
                                select = EmptySequence.getInstance();
                            } else {
                                // The implicit default value () is not valid for the required type, so
                                // it is treated as if there is no default
                                requiredParam = true;
                            }
                        }
                    } else {
                        if (Cardinality.allowsZero(requiredType.getCardinality())) {
                            select = EmptySequence.getInstance();
                        } else {
                            compileError("The implicit value () is not valid for the declared type", "XT0570");
                        }
                    }
                }
            } else {
                if (kids.next() == null) {
                    // there is exactly one child node
                    if (first.getNodeKind() == Type.TEXT) {
                        // it is a text node: optimize for this case
                        constantText = first.getStringValue();
                    }
                }

                // Determine if the temporary tree can only contain text nodes
                textonly = (getCommonChildItemType() == NodeKindTest.TEXT);
            }
        }
        select = typeCheck("select", select);
    }

    /**
     * Method called for parameters of call-template to check the type of the actual
     * parameter against the type of the required parameter
     * @param required The type required by the signature of the called template
     */

    protected void checkAgainstRequiredType(SequenceType required)
    throws TransformerConfigurationException {
        try {
            RoleLocator role = new RoleLocator(RoleLocator.VARIABLE, getVariableName(), 0, null);
            role.setErrorCode("XT0570");
            if (required!=null) {
                // check that the expression is consistent with the required type
                if (select != null) {
                    select = TypeChecker.staticTypeCheck(select, required, false, role, getStaticContext());
                } else {
                    // do the check later
                }
            }
        } catch (XPathException err) {
            err.setLocator(this);   // because the expression wasn't yet linked into the module
            compileError(err);
            select = new ErrorExpression(err);
        }
    }

    /**
    * Initialize - common code called from the compile() method of all subclasses
    */

    protected void initializeInstruction(Executable exec, GeneralVariable var)
    throws TransformerConfigurationException {

        var.init(select, getObjectNameCode());
        var.setAssignable(assignable);
        var.setRequiredParam(requiredParam);
        var.setRequiredType(requiredType);
        var.setTunnel(tunnel);

        // handle the "temporary tree" case by creating a Document sub-instruction
        // to construct and return a document node.
        if (hasChildNodes()) {
            if (requiredType==null) {
                DocumentInstr doc = new DocumentInstr(textonly, constantText, getBaseURI());
                Expression b = compileSequenceConstructor(exec, iterateAxis(Axis.CHILD), true);
                if (b == null) {
                    b = EmptySequence.getInstance();
                }
                doc.setContent(b);
                select = doc;
                var.setSelectExpression(doc);
            } else {
                select = compileSequenceConstructor(exec, iterateAxis(Axis.CHILD), true);
                if (select == null) {
                    select = EmptySequence.getInstance();
                }
                try {
                    if (requiredType != null) {
                        RoleLocator role =
                                new RoleLocator(RoleLocator.VARIABLE, getVariableName(), 0, null);
                        role.setErrorCode("XT0570");
                        select = select.simplify(getStaticContext());
                        select = TypeChecker.staticTypeCheck(select, requiredType, false, role, getStaticContext());
                    }
                } catch (XPathException err) {
                    err.setLocator(this);
                    compileError(err);
                    select = new ErrorExpression(err);
                }
                var.setSelectExpression(select);
            }
        }
        if (global) {
            Expression exp2 = select;
            if (exp2 != null) {
                try {
                    exp2 = select.simplify(staticContext).analyze(staticContext, Type.NODE_TYPE);
                } catch (XPathException err) {
                    compileError(err);
                }

                if (getConfiguration().getTraceListener() != null) {
                    TraceWrapper trace = new TraceInstruction(exp2, this);
                    trace.setLocationId(allocateLocationId(getSystemId(), getLineNumber()));
                    exp2 = trace;
                }

                allocateSlots(exp2);
            }
            if (slotManager != null && slotManager.getNumberOfVariables() > 0) {
                ((GlobalVariable)var).setContainsLocals(slotManager);
            }
            exec.registerGlobalVariable(var);
            if (exp2 != select) {
                var.setSelectExpression(exp2);
            }
        }
    }

    /**
     * Get the type of construct. This will be a constant in
     * class {@link net.sf.saxon.trace.Location}. This method is part of the
     * {@link net.sf.saxon.trace.InstructionInfo} interface
     */

    public int getConstructType() {
        return StandardNames.XSL_VARIABLE;
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
