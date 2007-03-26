package org.orbeon.saxon.style;
import org.orbeon.saxon.expr.*;
import org.orbeon.saxon.instruct.Executable;
import org.orbeon.saxon.om.AttributeCollection;
import org.orbeon.saxon.om.Axis;
import org.orbeon.saxon.om.NamespaceConstant;
import org.orbeon.saxon.sort.SortKeyDefinition;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.type.ItemType;
import org.orbeon.saxon.value.EmptySequence;
import org.orbeon.saxon.value.SequenceType;
import org.orbeon.saxon.value.StringValue;

import java.net.URI;
import java.net.URISyntaxException;
import java.text.Collator;
import java.util.Comparator;

/**
* An xsl:sort element in the stylesheet. <br>
*/

public class XSLSort extends StyleElement {

    private SortKeyDefinition sortKeyDefinition;
    private Expression select;
    private Expression order;
    private Expression dataType = null;
    private Expression caseOrder;
    private Expression lang;
    private Expression collationName;
    private Expression stable;
    private boolean useDefaultCollation = true;

    /**
      * Determine whether this type of element is allowed to contain a sequence constructor
      * @return true: yes, it may contain a sequence constructor
      */

    public boolean mayContainSequenceConstructor() {
        return true;
    }

    public void prepareAttributes() throws XPathException {

		AttributeCollection atts = getAttributeList();

		String selectAtt = null;
        String orderAtt = null;
        String dataTypeAtt = null;
        String caseOrderAtt = null;
        String langAtt = null;
        String collationAtt = null;
        String stableAtt = null;

		for (int a=0; a<atts.getLength(); a++) {
			int nc = atts.getNameCode(a);
			String f = getNamePool().getClarkName(nc);
			if (f==StandardNames.SELECT) {
        		selectAtt = atts.getValue(a);
        	} else if (f==StandardNames.ORDER) {
        		orderAtt = atts.getValue(a).trim();
        	} else if (f==StandardNames.DATA_TYPE) {
        		dataTypeAtt = atts.getValue(a).trim();
        	} else if (f==StandardNames.CASE_ORDER) {
        		caseOrderAtt = atts.getValue(a).trim();
        	} else if (f==StandardNames.LANG) {
        		langAtt = atts.getValue(a).trim();
        	} else if (f==StandardNames.COLLATION) {
        		collationAtt = atts.getValue(a).trim();
            } else if (f==StandardNames.STABLE) {
        		stableAtt = atts.getValue(a).trim();
        	} else {
        		checkUnknownAttribute(nc);
        	}
        }

        if (selectAtt==null) {
            //select = new ContextItemExpression();
        } else {
            select = makeExpression(selectAtt);
        }

        if (orderAtt == null) {
            order = new StringValue("ascending");
        } else {
            order = makeAttributeValueTemplate(orderAtt);
        }

        if (dataTypeAtt == null) {
            dataType = null;
        } else {
            dataType = makeAttributeValueTemplate(dataTypeAtt);
        }

        if (caseOrderAtt == null) {
            caseOrder = new StringValue("#default");
        } else {
            caseOrder = makeAttributeValueTemplate(caseOrderAtt);
            useDefaultCollation = false;
        }

        if (langAtt == null) {
            lang = StringValue.EMPTY_STRING;
        } else if (langAtt.equals("")) {
            compileError("The lang attribute must be a valid language code", "XTDE0030");
        } else {
            lang = makeAttributeValueTemplate(langAtt);
            useDefaultCollation = false;
        }

        if (stableAtt == null) {
            stable = null;
        } else {
            stable = makeAttributeValueTemplate(stableAtt);
        }

        if (collationAtt != null) {
            collationName = makeAttributeValueTemplate(collationAtt);
            useDefaultCollation = false;
        }

    }

    public void validate() throws XPathException {
        if (select != null && hasChildNodes()) {
            compileError("An xsl:sort element with a select attribute must be empty", "XTSE1015");
        }
        if (select == null && !hasChildNodes()) {
            select = new ContextItemExpression();
        }

        // Get the named or default collation

        if (useDefaultCollation) {
            collationName = new StringValue(getDefaultCollationName());
        }

        Comparator collator = null;
        if (collationName instanceof StringValue) {
            String collationString = ((StringValue)collationName).getStringValue();
            try {
                URI collationURI = new URI(collationString);
                if (!collationURI.isAbsolute()) {
                    URI base = new URI(getBaseURI());
                    collationURI = base.resolve(collationURI);
                    collationString = collationURI.toString();
                }
            } catch (URISyntaxException err) {
                compileError("Collation name '" + collationString + "' is not a valid URI");
                collationString = NamespaceConstant.CODEPOINT_COLLATION_URI;
            }
            collator = getPrincipalStylesheet().findCollation(collationString);
            if (collator==null) {
                compileError("Collation " + collationString + " has not been defined", "XTDE1035");
                collator = Collator.getInstance();     // for recovery paths
            }
        }

        select      = typeCheck("select", select);
        order       = typeCheck("order", order);
        caseOrder   = typeCheck("case-order", caseOrder);
        lang        = typeCheck("lang", lang);
        dataType    = typeCheck("data-type", dataType);
        collationName = typeCheck("collation", collationName);
        stable      = typeCheck("stable", stable);

        if (select != null) {
            try {
                RoleLocator role =
                    new RoleLocator(RoleLocator.INSTRUCTION, "xsl:sort/select", 0, null);
                role.setSourceLocator(new ExpressionLocation(this));
                select = TypeChecker.staticTypeCheck(select,
                                SequenceType.ATOMIC_SEQUENCE,
                                false, role, getStaticContext());
            } catch (XPathException err) {
                compileError(err);
            }
        }

        sortKeyDefinition = new SortKeyDefinition();
        sortKeyDefinition.setOrder(order);
        sortKeyDefinition.setCaseOrder(caseOrder);
        sortKeyDefinition.setLanguage(lang);
        sortKeyDefinition.setSortKey(select);
        sortKeyDefinition.setDataTypeExpression(dataType);
        sortKeyDefinition.setCollationName(collationName);
        sortKeyDefinition.setCollation(collator);
        sortKeyDefinition.setBaseURI(getBaseURI());
        sortKeyDefinition.setStable(stable);
        sortKeyDefinition.setBackwardsCompatible(backwardsCompatibleModeIsEnabled());
    }

    /**
     * Determine the type of item returned by this instruction (only relevant if
     * it is an instruction). Default implementation returns Type.ITEM, indicating
     * that we don't know, it might be anything. Returns null in the case of an element
     * such as xsl:sort or xsl:variable that can appear in a sequence constructor but
     * contributes nothing to the result sequence.
     * @return the item type returned
     */

    protected ItemType getReturnedItemType() {
        return null;
    }

    public Expression compile(Executable exec) throws XPathException {
        if (select == null) {
            Expression b = compileSequenceConstructor(exec, iterateAxis(Axis.CHILD), true);
            ComputedExpression.setParentExpression(b, this);
            if (b == null) {
                b = EmptySequence.getInstance();
            }
            try {
                StaticContext env = getStaticContext();
                Atomizer atomizedSortKey = new Atomizer(b.simplify(env), env.getConfiguration());
                atomizedSortKey.setParentExpression(sortKeyDefinition.getParentExpression());
                sortKeyDefinition.setSortKey(atomizedSortKey);
            } catch (XPathException e) {
                compileError(e);
            }
        }
        // Simplify the sort key definition - this is especially important in the case where
        // all aspects of the sort key are known statically.
        sortKeyDefinition = sortKeyDefinition.simplify(getStaticContext(), exec);
        // not an executable instruction
        return null;
    }

    public SortKeyDefinition getSortKeyDefinition() {
        return sortKeyDefinition;
    }

    public Expression getStable() {
        return stable;
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
