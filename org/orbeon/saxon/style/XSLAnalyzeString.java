package org.orbeon.saxon.style;
import org.orbeon.saxon.Configuration;
import org.orbeon.saxon.Platform;
import org.orbeon.saxon.expr.*;
import org.orbeon.saxon.instruct.AnalyzeString;
import org.orbeon.saxon.instruct.Executable;
import org.orbeon.saxon.om.*;
import org.orbeon.saxon.regex.RegularExpression;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.type.ItemType;
import org.orbeon.saxon.value.SequenceType;

/**
* An xsl:analyze-string elements in the stylesheet. New at XSLT 2.0<BR>
*/

public class XSLAnalyzeString extends StyleElement {

    private Expression select;
    private Expression regex;
    private Expression flags;
    private StyleElement matching;
    private StyleElement nonMatching;
    private RegularExpression pattern;

    /**
    * Determine whether this node is an instruction.
    * @return true - it is an instruction
    */

    public boolean isInstruction() {
        return true;
    }

    /**
    * Determine whether this type of element is allowed to contain an xsl:fallback
    * instruction
    */

    public boolean mayContainFallback() {
        return true;
    }

    /**
     * Determine the type of item returned by this instruction (only relevant if
     * it is an instruction).
     * @return the item type returned
     */

    protected ItemType getReturnedItemType() {
        return getCommonChildItemType();
    }

     public void prepareAttributes() throws XPathException {
		String selectAtt = null;
		String regexAtt = null;
		String flagsAtt = null;

		AttributeCollection atts = getAttributeList();

		for (int a=0; a<atts.getLength(); a++) {
			int nc = atts.getNameCode(a);
			String f = getNamePool().getClarkName(nc);
			if (f.equals(StandardNames.REGEX)) {
        		regexAtt = atts.getValue(a);
			} else if (f.equals(StandardNames.SELECT)) {
        		selectAtt = atts.getValue(a);
			} else if (f.equals(StandardNames.FLAGS)) {
        		flagsAtt = atts.getValue(a); // not trimmed, see bugzilla 4315
        	} else {
        		checkUnknownAttribute(nc);
        	}
        }

        if (selectAtt==null) {
            reportAbsence("select");
            selectAtt = ".";    // for error recovery
        }
        select = makeExpression(selectAtt);

        if (regexAtt==null) {
            reportAbsence("regex");
            regexAtt = "xxx";      // for error recovery
        }
        regex = makeAttributeValueTemplate(regexAtt);

        if (flagsAtt==null) {
            flagsAtt = "";
        }
        flags = makeAttributeValueTemplate(flagsAtt);

        if (regex instanceof StringLiteral && flags instanceof StringLiteral) {
            try {
                final Platform platform = Configuration.getPlatform();
                final CharSequence regex = ((StringLiteral)this.regex).getStringValue();
                final CharSequence flagstr = ((StringLiteral)flags).getStringValue();
                final int xmlVersion = getConfiguration().getXMLVersion();
                pattern = platform.compileRegularExpression(
                        regex, xmlVersion, RegularExpression.XPATH_SYNTAX, flagstr);

                if (pattern.matches("")) {
                    invalidRegex("The regular expression must not be one that matches a zero-length string", "XTDE1150");
                }
            } catch (XPathException err) {
                if ("FORX0001".equals(err.getErrorCodeLocalPart())) {
                    invalidRegex("Error in regular expression flags: " + err, "XTDE1145");
                } else {
                    invalidRegex("Error in regular expression: " + err, "XTDE1140");
                }
            }
        }

    }

    private void invalidRegex(String message, String errorCode) throws XPathException {
        compileError(message, errorCode);
        pattern = null;
    }

    public void validate() throws XPathException {
        //checkWithinTemplate();

        AxisIterator kids = iterateAxis(Axis.CHILD);
        while(true) {
            NodeInfo curr = (NodeInfo)kids.next();
            if (curr == null) {
                break;
            }
            if (curr instanceof XSLFallback) {
                // no-op
            } else if (curr instanceof XSLMatchingSubstring) {
                boolean b = curr.getLocalPart().equals("matching-substring");
                if (b) {
                    if (matching!=null) {
                        compileError("xsl:matching-substring element must only appear once", "XTSE0010");
                    }
                    matching = (StyleElement)curr;
                } else {
                    if (nonMatching!=null) {
                        compileError("xsl:non-matching-substring element must only appear once", "XTSE0010");
                    }
                    nonMatching = (StyleElement)curr;
                }
            } else {
                compileError("Only xsl:matching-substring and xsl:non-matching-substring are allowed here", "XTSE0010");
            }
        }

        if (matching==null && nonMatching==null) {
            compileError("At least one xsl:matching-substring or xsl:non-matching-substring element must be present",
                    "XTSE1130");
        }

        select = typeCheck("select", select);
        regex = typeCheck("regex", regex);
        flags = typeCheck("flags", flags);

        try {
            ExpressionVisitor visitor = makeExpressionVisitor();

            RoleLocator role =
                new RoleLocator(RoleLocator.INSTRUCTION, "xsl:analyze-string/select", 0);
            ExpressionLocation locator = new ExpressionLocation(this);
            //role.setSourceLocator(locator);
            select = TypeChecker.staticTypeCheck(select, SequenceType.SINGLE_STRING, false, role, 
                    visitor);

            role =
                new RoleLocator(RoleLocator.INSTRUCTION, "xsl:analyze-string/regex", 0);
            //role.setSourceLocator(locator);
            regex = TypeChecker.staticTypeCheck(regex, SequenceType.SINGLE_STRING, false, role, visitor);

            role =
                new RoleLocator(RoleLocator.INSTRUCTION, "xsl:analyze-string/flags", 0);
            //role.setSourceLocator(locator);
            flags = TypeChecker.staticTypeCheck(flags, SequenceType.SINGLE_STRING, false, role, visitor);
        } catch (XPathException err) {
            compileError(err);
        }

    }

    public Expression compile(Executable exec) throws XPathException {
        Expression matchingBlock = null;
        if (matching != null) {
            matchingBlock = matching.compileSequenceConstructor(exec, matching.iterateAxis(Axis.CHILD), false);
        }

        Expression nonMatchingBlock = null;
        if (nonMatching != null) {
            nonMatchingBlock = nonMatching.compileSequenceConstructor(exec, nonMatching.iterateAxis(Axis.CHILD), false);
        }

        try {
            ExpressionVisitor visitor = makeExpressionVisitor();
            return new AnalyzeString(select,
                                     regex,
                                     flags,
                                     (matchingBlock==null ? null : matchingBlock.simplify(visitor)),
                                     (nonMatchingBlock==null ? null : nonMatchingBlock.simplify(visitor)),
                                     pattern );
        } catch (XPathException e) {
            compileError(e);
            return null;
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
// The Initial Developer of the Original Code is Michael H. Kay
//
// Portions created by (your name) are Copyright (C) (your legal entity). All Rights Reserved.
//
// Contributor(s):
//
