package net.sf.saxon.style;
import net.sf.saxon.expr.*;
import net.sf.saxon.instruct.Executable;
import net.sf.saxon.instruct.ForEachGroup;
import net.sf.saxon.om.AttributeCollection;
import net.sf.saxon.om.Axis;
import net.sf.saxon.pattern.Pattern;
import net.sf.saxon.pattern.PatternSponsor;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.value.EmptySequence;
import net.sf.saxon.value.SequenceType;

import javax.xml.transform.TransformerConfigurationException;
import java.util.Comparator;

/**
* Handler for xsl:for-each-group elements in stylesheet. This is a new instruction
* defined in XSLT 2.0
*/

public final class XSLForEachGroup extends StyleElement {

    private Expression select = null;
    private Expression groupBy = null;
    private Expression groupAdjacent = null;
    private Pattern starting = null;
    private Pattern ending = null;
    private String collationName;

    /**
    * Determine whether this node is an instruction.
    * @return true - it is an instruction
    */

    public boolean isInstruction() {
        return true;
    }

    /**
     * Specify that xsl:sort is a permitted child
     */

    protected boolean isPermittedChild(StyleElement child) {
        return (child instanceof XSLSort);
    }

    /**
    * Determine whether this type of element is allowed to contain a template-body
    * @return true: yes, it may contain a template-body
    */

    public boolean mayContainSequenceConstructor() {
        return true;
    }

    public void prepareAttributes() throws XPathException {

		AttributeCollection atts = getAttributeList();

		String selectAtt = null;
		String groupByAtt = null;
		String groupAdjacentAtt = null;
		String startingAtt = null;
		String endingAtt = null;

		for (int a=0; a<atts.getLength(); a++) {
			int nc = atts.getNameCode(a);
			String f = getNamePool().getClarkName(nc);
            if (f==StandardNames.SELECT) {
        		selectAtt = atts.getValue(a);
        	} else if (f==StandardNames.GROUP_BY) {
        		groupByAtt = atts.getValue(a);
        	} else if (f==StandardNames.GROUP_ADJACENT) {
        		groupAdjacentAtt = atts.getValue(a);
        	} else if (f==StandardNames.GROUP_STARTING_WITH) {
        		startingAtt = atts.getValue(a);
        	} else if (f==StandardNames.GROUP_ENDING_WITH) {
        		endingAtt = atts.getValue(a);
        	} else if (f==StandardNames.COLLATION) {
        		collationName = atts.getValue(a).trim();
        	} else {
        		checkUnknownAttribute(nc);
        	}
        }

        if (selectAtt==null) {
            reportAbsence("select");
        } else {
            select = makeExpression(selectAtt);
        }

        int c = (groupByAtt==null ? 0 : 1) +
                (groupAdjacentAtt==null ? 0 : 1) +
                (startingAtt==null ? 0 : 1) +
                (endingAtt==null ? 0 : 1);;
        if (c!=1) {
            compileError("Exactly one of the attributes group-by, group-adjacent, group-starting-with, " +
                    "and group-ending-with must be specified", "XTSE1080");
        }

        if (groupByAtt != null) {
            groupBy = makeExpression(groupByAtt);
        }

        if (groupAdjacentAtt != null) {
            groupAdjacent = makeExpression(groupAdjacentAtt);
        }

        if (startingAtt != null) {
            starting = makePattern(startingAtt);
        }

        if (endingAtt != null) {
            ending = makePattern(endingAtt);
        }

        if (collationName!=null && groupBy==null && groupAdjacent==null) {
            compileError("A collation may be specified only if group-by or group-adjacent is specified", "XTSE1090");
        }
    }

    public void validate() throws XPathException {
        checkWithinTemplate();
        checkSortComesFirst(false);
        select = typeCheck("select", select);

        ExpressionLocation locator = new ExpressionLocation(this);
        if (groupBy != null) {
            groupBy = typeCheck("group-by", groupBy);
            try {
                RoleLocator role =
                    new RoleLocator(RoleLocator.INSTRUCTION, "xsl:for-each-group/group-by", 0, null);
                role.setSourceLocator(locator);
                groupBy = TypeChecker.staticTypeCheck(groupBy,
                        SequenceType.ATOMIC_SEQUENCE,
                        false, role, getStaticContext());
            } catch (XPathException err) {
                compileError(err);
            }
        } else if (groupAdjacent != null) {
            groupAdjacent = typeCheck("group-adjacent", groupAdjacent);
            try {
                RoleLocator role =
                    new RoleLocator(RoleLocator.INSTRUCTION, "xsl:for-each-group/group-adjacent", 0, null);
                role.setSourceLocator(locator);
                role.setErrorCode("XTTE1100");
                groupAdjacent = TypeChecker.staticTypeCheck(groupAdjacent,
                        SequenceType.SINGLE_ATOMIC,
                        false, role, getStaticContext());
            } catch (XPathException err) {
                compileError(err);
            }
        }

        starting = typeCheck("starting", starting);
        ending = typeCheck("ending", ending);

        if (starting != null || ending != null) {
            try {
                RoleLocator role =
                    new RoleLocator(RoleLocator.INSTRUCTION, "xsl:for-each-group/select", 0, null);
                role.setSourceLocator(locator);
                role.setErrorCode("XTTE1120");
                select = TypeChecker.staticTypeCheck(select,
                                            SequenceType.NODE_SEQUENCE,
                                            false, role, getStaticContext());
            } catch (XPathException err) {
                compileError(err);
            }
        }
    }

    public Expression compile(Executable exec) throws XPathException {

        Comparator collator = null;
        if (collationName != null) {
            collator = getPrincipalStylesheet().findCollation(collationName);
            if (collator==null) {
                compileError("The collation name '" + collationName + "' has not been defined", "XTDE1110");
            }
        }

        byte algorithm = 0;
        Expression key = null;
        if (groupBy != null) {
            algorithm = ForEachGroup.GROUP_BY;
            key = groupBy;
        } else if (groupAdjacent != null) {
            algorithm = ForEachGroup.GROUP_ADJACENT;
            key = groupAdjacent;
        } else if (starting != null) {
            algorithm = ForEachGroup.GROUP_STARTING;
            key = new PatternSponsor(starting);
        } else if (ending != null) {
            algorithm = ForEachGroup.GROUP_ENDING;
            key = new PatternSponsor(ending);
        }

//        Block action = new Block();
//        compileChildren(exec, action, true);
        Expression action = compileSequenceConstructor(exec, iterateAxis(Axis.CHILD), true);
        if (action == null) {
            // body of for-each is empty: it's a no-op.
            return EmptySequence.getInstance();
        }
        try {
            ForEachGroup inst = new ForEachGroup(
                                        select,
                                        action.simplify(getStaticContext()),
                                        algorithm,
                                        key,
                                        collator,
                                        makeSortKeys() );
            ExpressionTool.makeParentReferences(inst);
            return inst;
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
// The Initial Developer of the Original Code is Michael H. Kay.
//
// Portions created by (your name) are Copyright (C) (your legal entity). All Rights Reserved.
//
// Contributor(s): none.
//
