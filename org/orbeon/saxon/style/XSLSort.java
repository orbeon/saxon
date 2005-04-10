package net.sf.saxon.style;
import net.sf.saxon.expr.*;
import net.sf.saxon.instruct.Executable;
import net.sf.saxon.om.AttributeCollection;
import net.sf.saxon.om.Axis;
import net.sf.saxon.sort.SortKeyDefinition;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.ItemType;
import net.sf.saxon.value.EmptySequence;
import net.sf.saxon.value.SequenceType;
import net.sf.saxon.value.StringValue;

import java.text.Collator;
import java.util.Comparator;
import java.util.Locale;

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
            dataType = EmptySequence.getInstance();
        } else {
            dataType = makeAttributeValueTemplate(dataTypeAtt);
        }

        if (caseOrderAtt == null) {
            caseOrder = new StringValue("#default");
        } else {
            caseOrder = makeAttributeValueTemplate(caseOrderAtt);
        }

        if (langAtt == null) {
            lang = new StringValue(Locale.getDefault().getLanguage());
        } else {
            lang = makeAttributeValueTemplate(langAtt);
        }

        if (collationAtt != null) {
            collationName = makeAttributeValueTemplate(collationAtt);
        }

    }

    public void validate() throws XPathException {
        if (select != null && hasChildNodes()) {
            compileError("An xsl:sort element with a select attribute must be empty");
        }
        if (select == null && !hasChildNodes()) {
            select = new ContextItemExpression();
        }

        // Get the named or default collation

        Comparator collator = null;
        if (collationName instanceof StringValue) {
            String uri = ((StringValue)collationName).getStringValue();
            collator = getPrincipalStylesheet().findCollation(uri);
            if (collator==null) {
                compileError("Collation " + uri + " has not been defined");
                collator = Collator.getInstance();     // for recovery paths
            }
        }

        select      = typeCheck("select", select);
        order       = typeCheck("order", order);
        caseOrder   = typeCheck("case-order", caseOrder);
        lang        = typeCheck("lang", lang);
        dataType    = typeCheck("data-type", dataType);
        collationName = typeCheck("collation", collationName);

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
            if (b == null) {
                b = EmptySequence.getInstance();
            }
            try {
                StaticContext env = getStaticContext();
                sortKeyDefinition.setSortKey(
                        new Atomizer(b.simplify(env), env.getConfiguration()));
            } catch (XPathException e) {
                compileError(e);
            }
        }
        // Simplify the sort key definition - this is especially important in the case where
        // all aspects of the sort key are known statically.
        sortKeyDefinition = sortKeyDefinition.simplify(exec);
        // not an executable instruction
        return null;
    }

    public SortKeyDefinition getSortKeyDefinition() {
        return sortKeyDefinition;
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
