package org.orbeon.saxon.style;
import org.orbeon.saxon.expr.Expression;
import org.orbeon.saxon.instruct.*;
import org.orbeon.saxon.om.*;
import org.orbeon.saxon.xpath.XPathException;

import javax.xml.transform.TransformerConfigurationException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
* An xsl:attribute-set element in the stylesheet. <br>
*/

public class XSLAttributeSet extends StyleElement implements StylesheetProcedure {

    //private int attributeSetNameCode;
                // the name of this attribute set, as a Namepool fingerprint

    private String nameAtt;
                // the name of the attribute set as written

    private String useAtt;
                // the value of the use-attribute-sets attribute, as supplied

    private SlotManager stackFrameMap;
                // needed if variables are used

    private List attributeSetElements = null;
                // list of XSLAttributeSet objects referenced by this one

    private AttributeSet[] useAttributeSets = null;
                // compiled instructions for the attribute sets used by this one

    private AttributeSet procedure = new AttributeSet();
                // the instruction representing this attribute set

    private int referenceCount = 0;
                // number of references to this attribute set

    private boolean validated = false;

    public int getAttributeSetFingerprint() {
        return getObjectFingerprint();
    }

    public AttributeSet getInstruction() {
        return procedure;
    }

    public void incrementReferenceCount() {
        referenceCount++;
    }

    public void prepareAttributes() throws TransformerConfigurationException {


		useAtt = null;

		AttributeCollection atts = getAttributeList();

		for (int a=0; a<atts.getLength(); a++) {
			int nc = atts.getNameCode(a);
			String f = getNamePool().getClarkName(nc);
			if (f==StandardNames.NAME) {
        		nameAtt = atts.getValue(a).trim();
        	} else if (f==StandardNames.USE_ATTRIBUTE_SETS) {
        		useAtt = atts.getValue(a);
        	} else {
        		checkUnknownAttribute(nc);
        	}
        }

        if (nameAtt==null) {
            reportAbsence("name");
            return;
        }

        try {
            setObjectNameCode(makeNameCode(nameAtt.trim()));
        } catch (NamespaceException err) {
            compileError(err.getMessage(), "XT0280");
        } catch (XPathException err) {
            compileError(err.getMessage());
        }

    }

    public void validate() throws TransformerConfigurationException {

        if (validated) return;

        checkTopLevel(null);

        stackFrameMap = getConfiguration().makeSlotManager();

        AxisIterator kids = iterateAxis(Axis.CHILD);
        while (true) {
            Item child = kids.next();
            if (child == null) {
                break;
            }
            if (!(child instanceof XSLAttribute)) {
                compileError("Only xsl:attribute is allowed within xsl:attribute-set", "XT0010");
            }
        }

        if (useAtt!=null) {
            // identify any attribute sets that this one refers to

            attributeSetElements = new ArrayList(5);
            useAttributeSets = getAttributeSets(useAtt, attributeSetElements);

            // check for circularity

            for (Iterator it=attributeSetElements.iterator(); it.hasNext();) {
                ((XSLAttributeSet)it.next()).checkCircularity(this);
            }
        }

        validated = true;
    }

    /**
    * Check for circularity: specifically, check that this attribute set does not contain
    * a direct or indirect reference to the one supplied as a parameter
    */

    public void checkCircularity(XSLAttributeSet origin) throws TransformerConfigurationException {
        if (this==origin) {
            compileError("The definition of the attribute set is circular", "XT0720");
        } else {
            if (!validated) {
                // if this attribute set isn't validated yet, we don't check it.
                // The circularity will be detected when the last attribute set in the cycle
                // gets validated
                return;
            }
            if (attributeSetElements != null) {
                for (Iterator it=attributeSetElements.iterator(); it.hasNext();) {
                    ((XSLAttributeSet)it.next()).checkCircularity(origin);
                }
            }
        }
    }

    /**
    * Get details of stack frame
    */

    public SlotManager getSlotManager() {
        return stackFrameMap;
    }

    public Expression compile(Executable exec) throws TransformerConfigurationException {
        if (referenceCount > 0 ) {
            Expression body = compileSequenceConstructor(exec, iterateAxis(Axis.CHILD), true);

            if (getConfiguration().getTraceListener() != null) {
                TraceWrapper trace = new TraceInstruction(body, this);
                trace.setLocationId(allocateLocationId(getSystemId(), getLineNumber()));
                trace.setParentExpression(procedure);
                body = trace;
            }

            try {
                procedure.setUseAttributeSets(useAttributeSets);
                procedure.setNameCode(getObjectNameCode());
                procedure.setBody(body.simplify(getStaticContext()));
                procedure.setStackFrameMap(stackFrameMap);
                procedure.setSystemId(getSystemId());
                procedure.setLineNumber(getLineNumber());
                procedure.setExecutable(exec);
            } catch (XPathException e) {
                compileError(e);
            }
        }
        return null;
    }

    /**
     * Get the type of construct. This will be a constant in
     * class {@link org.orbeon.saxon.trace.Location}. This method is part of the {@link org.orbeon.saxon.trace.InstructionInfo} interface
     */

    public int getConstructType() {
        return StandardNames.XSL_ATTRIBUTE_SET;
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
