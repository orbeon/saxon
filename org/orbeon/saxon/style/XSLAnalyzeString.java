package net.sf.saxon.style;
import net.sf.saxon.expr.Expression;
import net.sf.saxon.expr.ExpressionTool;
import net.sf.saxon.expr.RoleLocator;
import net.sf.saxon.expr.TypeChecker;
import net.sf.saxon.functions.Matches;
import net.sf.saxon.instruct.AnalyzeString;
import net.sf.saxon.instruct.Executable;
import net.sf.saxon.om.AttributeCollection;
import net.sf.saxon.om.Axis;
import net.sf.saxon.om.AxisIterator;
import net.sf.saxon.om.NodeInfo;
import net.sf.saxon.type.ItemType;
import net.sf.saxon.type.RegexTranslator;
import net.sf.saxon.value.SequenceType;
import net.sf.saxon.value.StringValue;
import net.sf.saxon.xpath.XPathException;

import javax.xml.transform.TransformerConfigurationException;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
* An xsl:analyze-string elements in the stylesheet. New at XSLT 2.0<BR>
*/

public class XSLAnalyzeString extends StyleElement {

    private Expression select;
    private Expression regex;
    private Expression flags;
    private StyleElement matching;
    private StyleElement nonMatching;
    private Pattern pattern;

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

     public void prepareAttributes() throws TransformerConfigurationException {
		String selectAtt = null;
		String regexAtt = null;
		String flagsAtt = null;

		AttributeCollection atts = getAttributeList();

		for (int a=0; a<atts.getLength(); a++) {
			int nc = atts.getNameCode(a);
			String f = getNamePool().getClarkName(nc);
			if (f==StandardNames.REGEX) {
        		regexAtt = atts.getValue(a);
			} else if (f==StandardNames.SELECT) {
        		selectAtt = atts.getValue(a);
			} else if (f==StandardNames.FLAGS) {
        		flagsAtt = atts.getValue(a).trim();
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

        if (regex instanceof StringValue && flags instanceof StringValue) {
            int jflags = 0;
            try {
                jflags = Matches.setFlags(((StringValue)flags).getStringValue());
            } catch (XPathException err) {
                compileError("Invalid value of flags attribute: " + err, "XT1145");
            }
            try {
                String javaRegex = RegexTranslator.translate(
                        ((StringValue)regex).getStringValue(), true);
                pattern = Pattern.compile(javaRegex, jflags);
                if (pattern.matcher("").matches()) {
                    compileError("The regular expression must not be one that matches a zero-length string", "XT1150");
                }
            } catch (RegexTranslator.RegexSyntaxException err) {
                compileError("Error in regular expression: " + err, "XT1140");
            } catch (PatternSyntaxException err) {
                compileError("Error in regular expression: " + err, "XT1140");
            }
        }

    }

    public void validate() throws TransformerConfigurationException {
        checkWithinTemplate();

        AxisIterator kids = iterateAxis(Axis.CHILD);
        while(true) {
            NodeInfo curr = (NodeInfo)kids.next();
            if (curr == null) {
                break;
            }
            if (curr instanceof XSLMatchingSubstring) {
                boolean b = curr.getLocalPart().equals("matching-substring");
                if (b) {
                    if (matching!=null) {
                        compileError("xsl:matching-substring element must only appear once", "XT0010");
                    }
                    matching = (StyleElement)curr;
                } else {
                    if (nonMatching!=null) {
                        compileError("xsl:non-matching-substring element must only appear once", "XT0010");
                    }
                    nonMatching = (StyleElement)curr;
                }
            } else {
                compileError("Only xsl:matching-substring and xsl:non-matching-substring are allowed here", "XT0010");
            }
        }

        if (matching==null && nonMatching==null) {
            compileError("At least one xsl:matching-substring or xsl:non-matching-substring element must be present",
                    "XT1130");
        }

        select = typeCheck("select", select);
        regex = typeCheck("regex", regex);
        flags = typeCheck("flags", flags);

        try {
            RoleLocator role =
                new RoleLocator(RoleLocator.INSTRUCTION, "xsl:analyze-string/select", 0, null);
            select = TypeChecker.staticTypeCheck(select, SequenceType.SINGLE_STRING, false, role, getStaticContext());

            role =
                new RoleLocator(RoleLocator.INSTRUCTION, "xsl:analyze-string/regex", 0, null);
            regex = TypeChecker.staticTypeCheck(regex, SequenceType.SINGLE_STRING, false, role, getStaticContext());

            role =
                new RoleLocator(RoleLocator.INSTRUCTION, "xsl:analyze-string/flags", 0, null);
            flags = TypeChecker.staticTypeCheck(flags, SequenceType.SINGLE_STRING, false, role, getStaticContext());
        } catch (XPathException err) {
            compileError(err);
        }

    }

    public Expression compile(Executable exec) throws TransformerConfigurationException {
        Expression matchingBlock = null;
        if (matching != null) {
            matchingBlock = matching.compileSequenceConstructor(exec, matching.iterateAxis(Axis.CHILD), false);
        }

        Expression nonMatchingBlock = null;
        if (nonMatching != null) {
            nonMatchingBlock = nonMatching.compileSequenceConstructor(exec, nonMatching.iterateAxis(Axis.CHILD), false);
        }

        try {
            AnalyzeString anal = new AnalyzeString(
                                     select,
                                     regex,
                                     flags,
                                     (matching==null ? null : matchingBlock.simplify(matching.getStaticContext())),
                                     (nonMatching==null ? null : nonMatchingBlock.simplify(nonMatching.getStaticContext())),
                                     pattern );
            ExpressionTool.makeParentReferences(anal);
            return anal;
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
// Portions marked "e.g." are from Edwin Glaser (edwin@pannenleiter.de)
//
