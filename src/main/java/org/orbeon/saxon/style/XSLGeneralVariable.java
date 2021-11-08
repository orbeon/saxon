package org.orbeon.saxon.style;
import org.orbeon.saxon.expr.*;
import org.orbeon.saxon.instruct.*;
import org.orbeon.saxon.om.*;
import org.orbeon.saxon.pattern.NodeKindTest;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.type.ItemType;
import org.orbeon.saxon.type.Type;
import org.orbeon.saxon.value.Cardinality;
import org.orbeon.saxon.value.SequenceType;
import org.orbeon.saxon.value.StringValue;
import org.orbeon.saxon.value.Whitespace;

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
    protected boolean implicitlyRequiredParam = false;
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
     * extension element.
     * @return true if the extra attribute saxon:assignable="yes"
     * is present.
     */

    public boolean isAssignable() {
        return assignable;
    }

    /**
     * Test whether this is a tunnel parameter (tunnel="yes")
     * @return true if this is a tunnel parameter
     */

    public boolean isTunnelParam() {
        return tunnel;
    }

    /**
     * Test whether this is a required parameter (required="yes")
     * @return true if this is a required parameter
     */

    public boolean isRequiredParam() {
        return requiredParam;
    }

    /**
     * Test whether this is a global variable or parameter
     * @return true if this is global
     */

    public boolean isGlobal() {
        return isTopLevel();
            // might be called before the "global" field is initialized
    }

    /**
     * Get the display name of the variable.
     * @return the lexical QName
    */

    public String getVariableDisplayName() {
    	return getAttributeValue(StandardNames.NAME);
    }

    /**
    * Mark this global variable as redundant. This is done before prepareAttributes is called.
    */

    public void setRedundant() {
        redundant = true;
    }

    /**
     * Get the QName of the variable
     * @return the name as a structured QName, or a dummy name if the variable has no name attribute
     * or has an invalid name attribute
     */

    public StructuredQName getVariableQName() {
        // if an expression has a forwards reference to this variable, getVariableQName() can be
        // called before prepareAttributes() is called. We need to allow for this. But we'll
        // deal with any errors when we come round to processing this attribute, to avoid
        // duplicate error messages

        if (getObjectName() == null) {
            String nameAttribute = getAttributeValue(StandardNames.NAME);
            if (nameAttribute == null) {
                return new StructuredQName("saxon", NamespaceConstant.SAXON, "error-variable-name");
            }
            try {
                setObjectName(makeQName(nameAttribute));

            } catch (NamespaceException err) {
                setObjectName(new StructuredQName("saxon", NamespaceConstant.SAXON, "error-variable-name"));
            } catch (XPathException err) {
                setObjectName(new StructuredQName("saxon", NamespaceConstant.SAXON, "error-variable-name"));
            }
        }
        return getObjectName();
    }

    public void prepareAttributes() throws XPathException {

        getVariableQName();

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
			if (f.equals(StandardNames.NAME)) {
        		nameAtt = Whitespace.trim(atts.getValue(a)) ;
        	} else if (f.equals(StandardNames.SELECT)) {
        		selectAtt = atts.getValue(a);
        	} else if (f.equals(StandardNames.AS) && allowsAsAttribute()) {
        		asAtt = atts.getValue(a);
        	} else if (f.equals(StandardNames.REQUIRED) && allowsRequired()) {
        		requiredAtt = Whitespace.trim(atts.getValue(a)) ;
            } else if (f.equals(StandardNames.TUNNEL) && allowsTunnelAttribute()) {
        		tunnelAtt = Whitespace.trim(atts.getValue(a)) ;
        	} else if (f.equals(StandardNames.SAXON_ASSIGNABLE) && this instanceof XSLVariableDeclaration) {
        		assignAtt = Whitespace.trim(atts.getValue(a)) ;
        	} else {
        		checkUnknownAttribute(nc);
        	}
        }

        if (nameAtt==null) {
            reportAbsence("name");
        } else {
            // the name might have already been read, but errors weren't reported
            try {
                setObjectName(makeQName(nameAtt));
            } catch (NamespaceException e) {
                compileError("Prefix in variable name has not been declared: " + nameAtt, "XTSE0280");
            } catch (XPathException e) {
                String expl = (nameAtt.startsWith("$") ? " (must not start with '$')" : "");
                compileError("Variable name is not a valid QName: " + nameAtt + expl, "XTSE0020");
            }
        }

        if (selectAtt!=null) {
            if (!allowsValue()) {
                compileError("Function parameters cannot have a default value", "XTSE0760");
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
                compileError("The attribute 'required' must be set to 'yes' or 'no'", "XTSE0020");
            }
        }

        if (tunnelAtt!=null) {
            if (tunnelAtt.equals("yes")) {
                tunnel = true;
                if (this instanceof XSLParam && !(getParent() instanceof XSLTemplate)) {
                    compileError("For attribute 'tunnel' within an " + getParent().getDisplayName() +
                            " parameter, the only permitted value is 'no'", "XTSE0020");
                }
            } else if (tunnelAtt.equals("no")) {
                tunnel = false;
            } else {
                compileError("The attribute 'tunnel' must be set to 'yes' or 'no'", "XTSE0020");
            }
        }

        if (asAtt!=null) {
            requiredType = makeSequenceType(asAtt);
        }
    }

    public void validate() throws XPathException {
        global = isTopLevel();

        if (global) {
            slotManager = getConfiguration().makeSlotManager();
        }
        if (select!=null && hasChildNodes()) {
            compileError("An " + getDisplayName() + " element with a select attribute must be empty", "XTSE0620");
        }
        if (hasChildNodes() && !allowsValue()) {
            compileError("Function parameters cannot have a default value", "XTSE0760");
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
                    select = new StringLiteral(StringValue.EMPTY_STRING);
                } else {
                    if (this instanceof XSLParam) {
                        if (!requiredParam) {
                            if (Cardinality.allowsZero(requiredType.getCardinality())) {
                                select = Literal.makeEmptySequence();
                            } else {
                                // The implicit default value () is not valid for the required type, so
                                // it is treated as if there is no default
                                implicitlyRequiredParam = true;
                            }
                        }
                    } else {
                        if (Cardinality.allowsZero(requiredType.getCardinality())) {
                            select = Literal.makeEmptySequence();
                        } else {
                            compileError("The implicit value () is not valid for the declared type", "XTTE0570");
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
     * Check the supplied select expression against the required type.
     * @param required The type required by the variable declaration, or in the case
     * of xsl:with-param, the signature of the called template
     */

    protected void checkAgainstRequiredType(SequenceType required)
    throws XPathException {
        try {

            if (required!=null) {
                // check that the expression is consistent with the required type
                if (select != null) {
                    int category = RoleLocator.VARIABLE;
                    String errorCode = "XTTE0570";
                    if (this instanceof XSLParam) {
                        category = RoleLocator.PARAM;
                        errorCode = "XTTE0600";
                    } else if (this instanceof XSLWithParam) {
                        category = RoleLocator.PARAM;
                    }
                    RoleLocator role = new RoleLocator(category, getVariableDisplayName(), 0);
                    //role.setSourceLocator(new ExpressionLocation(this));
                    role.setErrorCode(errorCode);
                    select = TypeChecker.staticTypeCheck(select, required, false, role, makeExpressionVisitor());
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
     * @param exec the executable
     * @param var the representation of the variable declaration in the compiled executable
    */

    protected void initializeInstruction(Executable exec, GeneralVariable var)
    throws XPathException {

        var.init(select, getVariableQName());
        var.setAssignable(assignable);
        var.setRequiredParam(requiredParam);
        var.setImplicitlyRequiredParam(implicitlyRequiredParam);
        var.setRequiredType(requiredType);
        var.setTunnel(tunnel);

        // handle the "temporary tree" case by creating a Document sub-instruction
        // to construct and return a document node.
        if (hasChildNodes()) {
            if (requiredType==null) {
                DocumentInstr doc = new DocumentInstr(textonly, constantText, getBaseURI());
                var.adoptChildExpression(doc);
                Expression b = compileSequenceConstructor(exec, iterateAxis(Axis.CHILD), true);
                if (b == null) {
                    b = Literal.makeEmptySequence();
                }
                doc.setContentExpression(b);
                select = doc;
                var.setSelectExpression(doc);
            } else {
                select = compileSequenceConstructor(exec, iterateAxis(Axis.CHILD), true);
                var.adoptChildExpression(select);
                if (select == null) {
                    select = Literal.makeEmptySequence();
                }
                try {
                    if (requiredType != null) {
                        var.setContainer(this);  //temporarily
                        select.setContainer(this);
                        RoleLocator role =
                                new RoleLocator(RoleLocator.VARIABLE, getVariableDisplayName(), 0);
                        role.setErrorCode("XTTE0570");
                        //role.setSourceLocator(new ExpressionLocation(this));
                        select = makeExpressionVisitor().simplify(select);
                        select = TypeChecker.staticTypeCheck(select, requiredType, false, role, makeExpressionVisitor());
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
            final GlobalVariable gvar = (GlobalVariable)var;
            var.setContainer(gvar);
            Expression exp2 = select;
            if (exp2 != null) {
                try {
                    ExpressionVisitor visitor = makeExpressionVisitor();
                    exp2.setContainer(gvar);
                    exp2 = visitor.typeCheck(visitor.simplify(select), Type.NODE_TYPE);
                    exp2 = exp2.optimize(visitor, Type.NODE_TYPE);
                } catch (XPathException err) {
                    compileError(err);
                }

                if (getConfiguration().isCompileWithTracing()) {
                    TraceWrapper trace = new TraceInstruction(exp2, this);
                    trace.setLocationId(allocateLocationId(getSystemId(), getLineNumber()));
                    trace.setContainer(gvar);
                    exp2 = trace;
                }

                allocateSlots(exp2);
            }
            if (slotManager != null && slotManager.getNumberOfVariables() > 0) {
                gvar.setContainsLocals(slotManager);
            }
            exec.registerGlobalVariable(gvar);
            setReferenceCount(gvar);

            if (exp2 != select) {
                gvar.setSelectExpression(exp2);
            }
        }
    }

    protected void setReferenceCount(GeneralVariable var) {
        // overridden in subclass
    }

    /**
     * Get the type of construct. This will be a constant in
     * class {@link org.orbeon.saxon.trace.Location}. This method is part of the
     * {@link org.orbeon.saxon.trace.InstructionInfo} interface
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
