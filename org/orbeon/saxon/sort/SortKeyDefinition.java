package org.orbeon.saxon.sort;
import org.orbeon.saxon.expr.Expression;
import org.orbeon.saxon.expr.XPathContext;
import org.orbeon.saxon.value.EmptySequence;
import org.orbeon.saxon.value.StringValue;
import org.orbeon.saxon.xpath.XPathException;

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
    protected Comparator collation;   // usually a Collation, but not always
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

    public SortKeyDefinition simplify() throws XPathException {

        if (order instanceof StringValue &&
                (dataTypeExpression == null || dataTypeExpression instanceof StringValue) &&
                caseOrder instanceof StringValue &&
                language instanceof StringValue) {

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
            //return fskd.simplify();
        } else {
            return this;
        }
    }

//    public int getDependencies() {
//
//        // Not all dependencies in the sort key expression matter, because the context node, etc,
//        // are not dependent on the outer context
//        int dep = sortKey.getDependencies() & StaticProperty.DEPENDS_ON_XSLT_CONTEXT;
//        dep |= order.getDependencies();
//        if (dataTypeExpression != null) {
//            dep |= dataTypeExpression.getDependencies();
//        }
//        dep |= caseOrder.getDependencies();
//        dep |= language.getDependencies();
//        return dep;
//    }

    /**
     * Evaluate any aspects of the sort definition that were specified as AVTs, for example
     * ascending/descending, language, case-order, data-type. This is done at the start of each
     * sort. A FixedSortKeyDefinition is a SortKeyDefinition in which these properties are all
     * known values.
    */

    public FixedSortKeyDefinition reduce(XPathContext context) throws XPathException {

        // System.err.println("SortKeyDefinition.reduce()"); sortKey.display(10);

        FixedSortKeyDefinition sknew = new FixedSortKeyDefinition();

        Expression sortKey2 = sortKey;

        sknew.setSortKey(sortKey2);
        sknew.setOrder((StringValue)order.evaluateItem(context));
        sknew.setDataTypeExpression((StringValue)dataTypeExpression.evaluateItem(context));
        sknew.setCaseOrder((StringValue)caseOrder.evaluateItem(context));
        sknew.setLanguage((StringValue)language.evaluateItem(context));
        sknew.setCollation(collation);
        sknew.setEmptyFirst(emptyFirst);
        sknew.bindComparer();
        return sknew;
        //return (FixedSortKeyDefinition)sknew.simplify();
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
