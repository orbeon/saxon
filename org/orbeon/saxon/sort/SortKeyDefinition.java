package net.sf.saxon.sort;
import net.sf.saxon.expr.Expression;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.instruct.Executable;
import net.sf.saxon.trans.DynamicError;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.value.EmptySequence;
import net.sf.saxon.value.StringValue;

import java.io.Serializable;
import java.util.Comparator;

/**
* A SortKeyDefinition defines one component of a sort key. <BR>
*
* Note that most attributes defining the sort key can be attribute value templates,
* and can therefore vary from one invocation to another. We hold them as expressions. As
* soon as they are all known (which in general is only at run-time), the SortKeyDefinition
* is replaced by a FixedSortKeyDefinition in which all these values are fixed.
*/

// TODO: optimise also for the case where the attributes depend only on global variables
// or parameters, in which case the same Comparator can be used for the duration of a
// transformation.

// TODO: at present the SortKeyDefinition is evaluated to obtain a Comparator, which can
// be used to compare two sort keys. It would be more efficient to use a Collator to
// obtain collation keys for all the items to be sorted, as these can be compared more
// efficiently.


public class SortKeyDefinition implements Serializable {

    private static StringValue defaultOrder = new StringValue("ascending");
    private static StringValue defaultCaseOrder = new StringValue("#default");
    private static StringValue defaultLanguage = StringValue.EMPTY_STRING;

    protected Expression sortKey;
    protected Expression order = defaultOrder;
    protected Expression dataTypeExpression = EmptySequence.getInstance();
                                        // used when the type is not known till run-time
    protected Expression caseOrder = defaultCaseOrder;
    protected Expression language = defaultLanguage;
    protected Expression collationName = null;
    protected Comparator collation;   // usually a Collator, but not always
    protected boolean emptyFirst = true;
                            // used only in XQuery at present

    // Note, the "collation" defines the collating sequence for the sort key. The
    // "comparer" is what is actually used to do comparisons, after taking into account
    // ascending/descending, caseOrder, etc.

    // The comparer is transient because a RuleBasedCollator is not serializable. This means that
    // when a stylesheet is compiled, the comparer is discarded, which means a new comparer will be
    // constructed for each sort at run-time.

    /**
    * Set the expression used as the sort key
    */

    public void setSortKey(Expression exp) {
        sortKey = exp;
    }

    /**
    * Get the expression used as the sort key
    */

    public Expression getSortKey() {
        return sortKey;
    }


    /**
    * Set the order. This is supplied as an expression which must evaluate to "ascending"
    * or "descending". If the order is fixed, supply e.g. new StringValue("ascending").
    * Default is "ascending".
    */

    public void setOrder(Expression exp) {
        order = exp;
    }

    public Expression getOrder() {
        return order;
    }

    /**
    * Set the data type. This is supplied as an expression which must evaluate to "text",
    * "number", or a QName. If the data type is fixed, the valus should be supplied using
    * setDataType() and not via this method.
    */

    public void setDataTypeExpression(Expression exp) {
        dataTypeExpression = exp;
    }

    public Expression getDataTypeExpression() {
        return dataTypeExpression;
    }
    /**
    * Set the case order. This is supplied as an expression which must evaluate to "upper-first"
    * or "lower-first" or "#default". If the order is fixed, supply e.g. new StringValue("lower-first").
    * Default is "#default".
    */

    public void setCaseOrder(Expression exp) {
        caseOrder = exp;
    }

    public Expression getCaseOrder() {
        return caseOrder;
    }

    /**
    * Set the language. This is supplied as an expression which evaluates to the language name.
    * If the order is fixed, supply e.g. new StringValue("de").
    */

    public void setLanguage(Expression exp) {
        language = exp;
    }

    public Expression getLanguage() {
        return language;
    }

    /**
    * Set the collation.
    */

    public void setCollationName(Expression collationName) {
        this.collationName = collationName;
    }

    public Expression getCollationName() {
        return collationName;
    }

    public void setCollation(Comparator collation) {
        this.collation = collation;
    }

    public Comparator getCollation() {
        return collation;
    }

    /**
     * Set whether empty sequence comes before other values or after them
     * @param emptyFirst true if () is considered lower than any other value
     */

    public void setEmptyFirst(boolean emptyFirst) {
        this.emptyFirst = emptyFirst;
    }

    public boolean getEmptyFirst() {
        return emptyFirst;
    }

    public SortKeyDefinition simplify(Executable exec) throws XPathException {

        if (order instanceof StringValue &&
                (dataTypeExpression == null || dataTypeExpression instanceof StringValue) &&
                caseOrder instanceof StringValue &&
                language instanceof StringValue &&
                collation != null) {

            FixedSortKeyDefinition fskd = new FixedSortKeyDefinition();
            fskd.setSortKey(sortKey);
            fskd.setOrder(order);
            fskd.setDataTypeExpression(dataTypeExpression);
            fskd.setCaseOrder(caseOrder);
            fskd.setLanguage(language);
            fskd.setEmptyFirst(emptyFirst);
            fskd.collation = collation;
            fskd.bindComparer();
            return fskd;
        } else {
            return this;
        }
    }

    /**
     * Evaluate any aspects of the sort definition that were specified as AVTs, for example
     * ascending/descending, language, case-order, data-type. This is done at the start of each
     * sort. A FixedSortKeyDefinition is a SortKeyDefinition in which these properties are all
     * known values.
    */

    public FixedSortKeyDefinition reduce(XPathContext context) throws XPathException {

        FixedSortKeyDefinition sknew = new FixedSortKeyDefinition();

        Expression sortKey2 = sortKey;

        sknew.setSortKey(sortKey2);
        sknew.setOrder((StringValue)order.evaluateItem(context));
        sknew.setDataTypeExpression((StringValue)dataTypeExpression.evaluateItem(context));
        sknew.setCaseOrder((StringValue)caseOrder.evaluateItem(context));
        sknew.setLanguage((StringValue)language.evaluateItem(context));
        if (collation == null && collationName != null) {
            String cname = collationName.evaluateItem(context).getStringValue();
            Comparator comp = context.getCollation(cname);
            if (comp == null) {
                throw new DynamicError("Collation " + cname + " is not recognized");
            }
            sknew.setCollation(comp);
        }
        if (collation != null) {
            sknew.setCollation(collation);
        }
        sknew.setEmptyFirst(emptyFirst);
        sknew.bindComparer();
        return sknew;
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
