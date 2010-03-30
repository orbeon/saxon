package org.orbeon.saxon.style;
import org.orbeon.saxon.expr.*;
import org.orbeon.saxon.instruct.Executable;
import org.orbeon.saxon.instruct.ForEachGroup;
import org.orbeon.saxon.om.AttributeCollection;
import org.orbeon.saxon.om.Axis;
import org.orbeon.saxon.om.NamespaceConstant;
import org.orbeon.saxon.om.StandardNames;
import org.orbeon.saxon.pattern.Pattern;
import org.orbeon.saxon.pattern.PatternSponsor;
import org.orbeon.saxon.sort.StringCollator;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.trans.SaxonErrorCode;
import org.orbeon.saxon.value.EmptySequence;
import org.orbeon.saxon.value.SequenceType;
import org.orbeon.saxon.value.Whitespace;

import java.net.URI;
import java.net.URISyntaxException;

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
    private Expression collationName;

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
        String collationAtt = null;

		for (int a=0; a<atts.getLength(); a++) {
			int nc = atts.getNameCode(a);
			String f = getNamePool().getClarkName(nc);
            if (f.equals(StandardNames.SELECT)) {
        		selectAtt = atts.getValue(a);
        	} else if (f.equals(StandardNames.GROUP_BY)) {
        		groupByAtt = atts.getValue(a);
        	} else if (f.equals(StandardNames.GROUP_ADJACENT)) {
        		groupAdjacentAtt = atts.getValue(a);
        	} else if (f.equals(StandardNames.GROUP_STARTING_WITH)) {
        		startingAtt = atts.getValue(a);
        	} else if (f.equals(StandardNames.GROUP_ENDING_WITH)) {
        		endingAtt = atts.getValue(a);
        	} else if (f.equals(StandardNames.COLLATION)) {
        		collationAtt = Whitespace.trim(atts.getValue(a));
        	} else {
        		checkUnknownAttribute(nc);
        	}
        }

        if (selectAtt==null) {
            reportAbsence("select");
            select = new Literal(EmptySequence.getInstance()); // for error recovery
        } else {
            select = makeExpression(selectAtt);
        }

        int c = (groupByAtt==null ? 0 : 1) +
                (groupAdjacentAtt==null ? 0 : 1) +
                (startingAtt==null ? 0 : 1) +
                (endingAtt==null ? 0 : 1);
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

        if (collationAtt != null) {
            if (groupBy==null && groupAdjacent==null) {
                compileError("A collation may be specified only if group-by or group-adjacent is specified", "XTSE1090");
            } else {
                collationName = makeAttributeValueTemplate(collationAtt);
                if (collationName instanceof StringLiteral) {
                    String collation = ((StringLiteral)collationName).getStringValue();
                    URI collationURI;
                    try {
                        collationURI = new URI(collation);
                        if (!collationURI.isAbsolute()) {
                            URI base = new URI(getBaseURI());
                            collationURI = base.resolve(collationURI);
                            collationName = new StringLiteral(collationURI.toString());
                        }
                    } catch (URISyntaxException err) {
                        compileError("Collation name '" + collationName + "' is not a valid URI", "XTDE1110");
                        collationName = new StringLiteral(NamespaceConstant.CODEPOINT_COLLATION_URI);
                    }
                }
            }
        } else {
            String defaultCollation = getDefaultCollationName();
            if (defaultCollation != null) {
                collationName = new StringLiteral(defaultCollation);
            }
        }
    }

    public void validate() throws XPathException {
        checkSortComesFirst(false);
        select = typeCheck("select", select);

        ExpressionLocation locator = new ExpressionLocation(this);
        ExpressionVisitor visitor = makeExpressionVisitor();
        if (groupBy != null) {
            groupBy = typeCheck("group-by", groupBy);
            try {
                RoleLocator role =
                    new RoleLocator(RoleLocator.INSTRUCTION, "xsl:for-each-group/group-by", 0);
                //role.setSourceLocator(locator);
                groupBy = TypeChecker.staticTypeCheck(groupBy,
                        SequenceType.ATOMIC_SEQUENCE,
                        false, role, visitor);
            } catch (XPathException err) {
                compileError(err);
            }
        } else if (groupAdjacent != null) {
            groupAdjacent = typeCheck("group-adjacent", groupAdjacent);
            try {
                RoleLocator role =
                    new RoleLocator(RoleLocator.INSTRUCTION, "xsl:for-each-group/group-adjacent", 0);
                //role.setSourceLocator(locator);
                role.setErrorCode("XTTE1100");
                groupAdjacent = TypeChecker.staticTypeCheck(groupAdjacent,
                        SequenceType.SINGLE_ATOMIC,
                        false, role, visitor);
            } catch (XPathException err) {
                compileError(err);
            }
        }

        starting = typeCheck("starting", starting);
        ending = typeCheck("ending", ending);

        if (starting != null || ending != null) {
            try {
                RoleLocator role =
                    new RoleLocator(RoleLocator.INSTRUCTION, "xsl:for-each-group/select", 0);
                //role.setSourceLocator(locator);
                role.setErrorCode("XTTE1120");
                select = TypeChecker.staticTypeCheck(select,
                                            SequenceType.NODE_SEQUENCE,
                                            false, role, visitor);
            } catch (XPathException err) {
                String prefix = (starting != null ?
                        "With group-starting-with attribute: " :
                        "With group-ending-with attribute: ");
                compileError(prefix + err.getMessage(), err.getErrorCodeLocalPart());
            }
        }
        if (!hasChildNodes()) {
            compileWarning("An empty xsl:for-each-group instruction has no effect", SaxonErrorCode.SXWN9009);
        }
    }

    public Expression compile(Executable exec) throws XPathException {

        StringCollator collator = null;
        if (collationName instanceof StringLiteral) {
            // if the collation name is constant, then we've already resolved it against the base URI
            final String uri = ((StringLiteral)collationName).getStringValue();
            collator = getPrincipalStylesheet().findCollation(uri);
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
            return new Literal(EmptySequence.getInstance());
        }
        try {
            return new ForEachGroup(    select,
                                        makeExpressionVisitor().simplify(action),
                                        algorithm,
                                        key,
                                        collator,
                                        collationName,
                                        getBaseURI(),
                                        makeSortKeys() );
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
