package net.sf.saxon.style;
import net.sf.saxon.Err;
import net.sf.saxon.expr.*;
import net.sf.saxon.instruct.ApplyTemplates;
import net.sf.saxon.instruct.Executable;
import net.sf.saxon.om.*;
import net.sf.saxon.sort.SortExpression;
import net.sf.saxon.sort.SortKeyDefinition;
import net.sf.saxon.trans.Mode;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.Type;
import net.sf.saxon.value.SequenceType;

import javax.xml.transform.TransformerConfigurationException;


/**
* An xsl:apply-templates element in the stylesheet
*/

public class XSLApplyTemplates extends StyleElement {

    private Expression select;
    private int modeNameCode = -1;            // -1 if no mode specified
    private boolean useCurrentMode = false;
    private boolean useTailRecursion = false;
    private Mode mode;
    private String modeAttribute;

    /**
    * Determine whether this node is an instruction.
    * @return true - it is an instruction
    */

    public boolean isInstruction() {
        return true;
    }


    public void prepareAttributes() throws TransformerConfigurationException {

		AttributeCollection atts = getAttributeList();

		String selectAtt = null;

		for (int a=0; a<atts.getLength(); a++) {
			int nc = atts.getNameCode(a);
			String f = getNamePool().getClarkName(nc);
			if (f==StandardNames.MODE) {
        		modeAttribute = atts.getValue(a).trim();
        	} else if (f==StandardNames.SELECT) {
        		selectAtt = atts.getValue(a);
        	} else {
        		checkUnknownAttribute(nc);
        	}
        }

        if (modeAttribute!=null) {
            if (modeAttribute.equals("#current")) {
                useCurrentMode = true;
            } else if (modeAttribute.equals("#default")) {
                // do nothing;
            } else {
                try {
                    modeNameCode = makeNameCode(modeAttribute.trim());
                } catch (NamespaceException err) {
                    compileError(err.getMessage());
                } catch (XPathException err) {
                    compileError("Mode name " + Err.wrap(modeAttribute) + " is not a valid QName", "XT0280");
                }
            }
        }

        if (selectAtt!=null) {
            select = makeExpression(selectAtt);
        }
    }

    public void validate() throws TransformerConfigurationException {

        checkWithinTemplate();

        // get the Mode object
        if (!useCurrentMode) {
            mode = getPrincipalStylesheet().getRuleManager().getMode(modeNameCode);
        }

        // handle sorting if requested

        AxisIterator kids = iterateAxis(Axis.CHILD);
        while (true) {
            NodeInfo child = (NodeInfo)kids.next();
            if (child == null) {
                break;
            }
            if (child instanceof XSLSort) {
                // no-op
            } else if (child instanceof XSLWithParam) {
                // usesParams = true;
            } else if (child.getNodeKind() == Type.TEXT) {
                    // with xml:space=preserve, white space nodes may still be there
                if (!Navigator.isWhite(child.getStringValueCS())) {
                    compileError("No character data is allowed within xsl:apply-templates", "XT0010");
                }
            } else {
                compileError("Invalid element within xsl:apply-templates", "XT0010");
            }
        }

        if (select==null) {
            select = new AxisExpression(Axis.CHILD, null);
        }

        select = typeCheck("select", select);
        try {
            RoleLocator role =
                new RoleLocator(RoleLocator.INSTRUCTION, "xsl:apply-templates/select", 0, null);
            role.setErrorCode("XT0520");
            select = TypeChecker.staticTypeCheck(select,
                                        SequenceType.NODE_SEQUENCE,
                                        false, role, getStaticContext());
        } catch (XPathException err) {
            compileError(err);
        }

    }

    /**
     * Mark tail-recursive calls on templates and functions.
     * For most instructions, this does nothing.
    */

    public void markTailCalls() {
        useTailRecursion = true;
    }


    public Expression compile(Executable exec) throws TransformerConfigurationException {
        SortKeyDefinition[] sortKeys = makeSortKeys();
        if (sortKeys != null) {
            useTailRecursion = false;
        }
        Expression sortedSequence = select;
        if (sortKeys != null) {
            sortedSequence = new SortExpression(select, sortKeys);
            ExpressionTool.makeParentReferences(sortedSequence);
        }
        ApplyTemplates app = new ApplyTemplates(
                                    sortedSequence,
                                    getWithParamInstructions(exec, false),
                                    getWithParamInstructions(exec, true),
                                    useCurrentMode,
                                    useTailRecursion,
                                    mode,
                                    backwardsCompatibleModeIsEnabled());
        ExpressionTool.makeParentReferences(app);
        return app;
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
