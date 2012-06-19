package org.orbeon.saxon.style;
import org.orbeon.saxon.trans.Err;
import org.orbeon.saxon.expr.*;
import org.orbeon.saxon.instruct.ApplyTemplates;
import org.orbeon.saxon.instruct.Executable;
import org.orbeon.saxon.om.*;
import org.orbeon.saxon.sort.SortExpression;
import org.orbeon.saxon.sort.SortKeyDefinition;
import org.orbeon.saxon.trans.Mode;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.type.Type;
import org.orbeon.saxon.value.SequenceType;
import org.orbeon.saxon.value.Whitespace;


/**
* An xsl:apply-templates element in the stylesheet
*/

public class XSLApplyTemplates extends StyleElement {

    private Expression select;
    private StructuredQName modeName;   // null if no name specified or if conventional values such as #current used
    private boolean useCurrentMode = false;
    private boolean useTailRecursion = false;
    private Mode mode;
    private String modeAttribute;
    private boolean implicitSelect = false;

    /**
    * Determine whether this node is an instruction.
    * @return true - it is an instruction
    */

    public boolean isInstruction() {
        return true;
    }


    public void prepareAttributes() throws XPathException {

		AttributeCollection atts = getAttributeList();

		String selectAtt = null;

		for (int a=0; a<atts.getLength(); a++) {
			int nc = atts.getNameCode(a);
			String f = getNamePool().getClarkName(nc);
			if (f.equals(StandardNames.MODE)) {
        		modeAttribute = Whitespace.trim(atts.getValue(a));
        	} else if (f.equals(StandardNames.SELECT)) {
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
                    modeName = makeQName(modeAttribute);
                } catch (NamespaceException err) {
                    compileError(err.getMessage(), "XTSE0280");
                    modeName = null;
                } catch (XPathException err) {
                    compileError("Mode name " + Err.wrap(modeAttribute) + " is not a valid QName",
                            err.getErrorCodeLocalPart());
                    modeName = null;
                }
            }
        }

        if (selectAtt!=null) {
            select = makeExpression(selectAtt);
        }
    }

    public void validate() throws XPathException {

        //checkWithinTemplate();

        // get the Mode object
        if (!useCurrentMode) {
            mode = getPrincipalStylesheet().getRuleManager().getMode(modeName, true);
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
                if (!Whitespace.isWhite(child.getStringValueCS())) {
                    compileError("No character data is allowed within xsl:apply-templates", "XTSE0010");
                }
            } else {
                compileError("Invalid element within xsl:apply-templates", "XTSE0010");
            }
        }

        if (select==null) {
            select = new AxisExpression(Axis.CHILD, null);
            implicitSelect = true;
        }

        select = typeCheck("select", select);
        try {
            RoleLocator role =
                new RoleLocator(RoleLocator.INSTRUCTION, "xsl:apply-templates/select", 0);
            //role.setSourceLocator(new ExpressionLocation(this));
            role.setErrorCode("XTTE0520");
            select = TypeChecker.staticTypeCheck(select,
                                        SequenceType.NODE_SEQUENCE,
                                        false, role, makeExpressionVisitor());
        } catch (XPathException err) {
            compileError(err);
        }

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
        SortKeyDefinition[] sortKeys = makeSortKeys();
        if (sortKeys != null) {
            useTailRecursion = false;
        }
        Expression sortedSequence = select;
        if (sortKeys != null) {
            sortedSequence = new SortExpression(select, sortKeys);
        }
        compileSequenceConstructor(exec, iterateAxis(Axis.CHILD), true);
        ApplyTemplates app = new ApplyTemplates(
                                    sortedSequence,
                                    useCurrentMode,
                                    useTailRecursion,
                                    mode,
                                    backwardsCompatibleModeIsEnabled(),
                                    implicitSelect);
        app.setActualParameters(getWithParamInstructions(exec, false, app),
                                 getWithParamInstructions(exec, true, app));
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
