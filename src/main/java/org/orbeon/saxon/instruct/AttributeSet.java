package org.orbeon.saxon.instruct;
import org.orbeon.saxon.Configuration;
import org.orbeon.saxon.expr.StaticProperty;
import org.orbeon.saxon.expr.XPathContext;
import org.orbeon.saxon.expr.XPathContextMajor;
import org.orbeon.saxon.om.StandardNames;
import org.orbeon.saxon.om.StructuredQName;
import org.orbeon.saxon.trans.XPathException;

/**
* The compiled form of an xsl:attribute-set element in the stylesheet.
*/

// Note, there is no run-time check for circularity. This is checked at compile time.

public class AttributeSet extends Procedure {

    StructuredQName attributeSetName;

    private AttributeSet[] useAttributeSets;

    /**
     * Create an empty attribute set
     */

    public AttributeSet() {
        setHostLanguage(Configuration.XSLT);
    }

    /**
     * Set the name of the attribute-set
     * @param attributeSetName the name of the attribute-set
     */

    public void setName(StructuredQName attributeSetName) {
        this.attributeSetName = attributeSetName;
    }

    /**
     * Set the attribute sets used by this attribute set
     * @param useAttributeSets the set of attribute sets used by this attribute set
     */

    public void setUseAttributeSets(AttributeSet[] useAttributeSets) {
        this.useAttributeSets = useAttributeSets;
    }

    /**
     * Set the stack frame map which allocates slots to variables declared in this attribute set
     * @param stackFrameMap the stack frame map
     */

    public void setStackFrameMap(SlotManager stackFrameMap) {
        if (stackFrameMap != null && stackFrameMap.getNumberOfVariables() > 0) {
            super.setStackFrameMap(stackFrameMap);
        }
    }

    /**
     * Determine whether the attribute set has any dependencies on the focus
     * @return the dependencies
     */

    public int getFocusDependencies() {
        int d = 0;
        if (body != null) {
            d |= body.getDependencies() & StaticProperty.DEPENDS_ON_FOCUS;
        }
        if (useAttributeSets != null) {
            for (int i=0; i<useAttributeSets.length; i++) {
                d |= useAttributeSets[i].getFocusDependencies();
            }
        }
        return d;
    }

    /**
     * Evaluate an attribute set
     * @param context the dynamic context
     * @throws XPathException if any failure occurs
     */

    public void expand(XPathContext context) throws XPathException {
        // apply the content of any attribute sets mentioned in use-attribute-sets

        if (useAttributeSets != null) {
            AttributeSet.expand(useAttributeSets, context);
        }

        if (getStackFrameMap() != null) {
            XPathContextMajor c2 = context.newContext();
            c2.setOrigin(this);
            c2.openStackFrame(getStackFrameMap());
            getBody().process(c2);
        } else {
            getBody().process(context);
        }
    }

    /**
     * Get the InstructionInfo details about the construct. This information isn't used for tracing,
     * but it is available when inspecting the context stack.
     */

//    public InstructionInfo getInstructionInfo() {
//        InstructionDetails details = new InstructionDetails();
//        details.setConstructType(StandardNames.XSL_ATTRIBUTE_SET);
//        details.setObjectName(attributeSetName);
//        details.setSystemId(getSystemId());
//        details.setLineAndColumn(getLineNumber());
//        details.setProperty("attribute-set", this);
//        return details;
//    }


    /**
     * Get the type of construct. This will either be the fingerprint of a standard XSLT instruction name
     * (values in {@link org.orbeon.saxon.om.StandardNames}: all less than 1024)
     * or it will be a constant in class {@link org.orbeon.saxon.trace.Location}.
     */

    public int getConstructType() {
        return StandardNames.XSL_ATTRIBUTE_SET;
    }

    /**
     * Get a name identifying the object of the expression, for example a function name, template name,
     * variable name, key name, element name, etc. This is used only where the name is known statically.
     *
     */

    public StructuredQName getObjectName() {
        return attributeSetName;
    }

    /**
     * Expand an array of attribute sets
     * @param asets the attribute sets to be expanded
     * @param context the run-time context to use
     * @throws XPathException
     */

    protected static void expand(AttributeSet[] asets, XPathContext context) throws XPathException {
        for (int i=0; i<asets.length; i++) {
            asets[i].expand(context);
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
