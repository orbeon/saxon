package org.orbeon.saxon.instruct;
import org.orbeon.saxon.expr.XPathContext;
import org.orbeon.saxon.expr.XPathContextMajor;
import org.orbeon.saxon.xpath.XPathException;
import org.orbeon.saxon.trace.InstructionInfoProvider;
import org.orbeon.saxon.trace.InstructionInfo;
import org.orbeon.saxon.trace.Location;
import org.orbeon.saxon.style.StandardNames;

/**
* The compiled form of an xsl:attribute-set element in the stylesheet.
*/

// Note, there is no run-time check for circularity. This is checked at compile time.

public class AttributeSet extends Procedure implements InstructionInfoProvider {

    int nameCode;

    private AttributeSet[] useAttributeSets;

    public AttributeSet() {}

    public void setNameCode(int nameCode) {
        this.nameCode = nameCode;
    }

    public int getNameCode() {
        return nameCode;
    }

    public void setUseAttributeSets(AttributeSet[] useAttributeSets) {
        this.useAttributeSets = useAttributeSets;
    }

    public void setStackFrameMap(SlotManager stackFrameMap) {
        if (stackFrameMap != null && stackFrameMap.getNumberOfVariables() > 0) {
            super.setStackFrameMap(stackFrameMap);
        }
    }

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

    public InstructionInfo getInstructionInfo() {
        InstructionDetails details = new InstructionDetails();
        details.setConstructType(StandardNames.XSL_ATTRIBUTE_SET);
        details.setSystemId(getSystemId());
        details.setLineNumber(getLineNumber());
        details.setProperty("attribute-set", this);
        return details;
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
