package net.sf.saxon.sort;
import net.sf.saxon.Configuration;
import net.sf.saxon.expr.CardinalityChecker;
import net.sf.saxon.expr.RoleLocator;
import net.sf.saxon.expr.StaticProperty;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.trans.DynamicError;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.value.EmptySequence;
import net.sf.saxon.value.StringValue;

import java.text.Collator;
import java.util.Comparator;
import java.util.Locale;

/**
* A FixedSortKeyDefinition is a SortKeyDefinition in which all aspects of the
* sort key definition (sort order, data type, etc,) are known.
* A SortKeyDefinition defines one component of a sort key. <BR>
*
*/


public class FixedSortKeyDefinition extends SortKeyDefinition {

    public FixedSortKeyDefinition() {}

    private transient Comparator comparer = null;
    // Note, the "collation" defines the collating sequence for the sort key. The
    // "comparer" is what is actually used to do comparisons, after taking into account
    // ascending/descending, caseOrder, etc.

    public SortKeyDefinition simplify() throws XPathException {
        return this;
    }

    /**
    * Eliminate dependencies of the sort key definition on the context. For the sort key select
    * expression, this means things that don't depend on the individual node: specifically, variables
    * and current-group(). For the AVTs used to select data type, case order, language, it means
    * all dependencies: after reduction, these values will be constants.
    */

    public FixedSortKeyDefinition reduce(XPathContext context) throws XPathException {
            return this;
    }

    /**
    * Allocate a resusable Comparer to implement this sort key comparison
    */

    public void bindComparer() throws XPathException {

        String orderX = ((StringValue)order).getStringValue();
        String caseOrderX = ((StringValue)caseOrder).getStringValue();
        String languageX = ((StringValue)language).getStringValue();

        Comparator comp;
        if (collation != null) {
           comp = collation;
        } else {
            Collator base;
            if (languageX.equals("")) {
                // get Java collator for the default locale
                base = Collator.getInstance();
            } else {
                Locale locale = Configuration.getLocale(languageX);
                base = Collator.getInstance(locale);
            }
            comp = getCaseOrderComparer(base, caseOrderX);
        }

        if (dataTypeExpression==null || dataTypeExpression instanceof EmptySequence) {
            RoleLocator role =
                new RoleLocator(RoleLocator.INSTRUCTION, "xsl:sort/sort-key", 0, null);
            sortKey = new CardinalityChecker(sortKey, StaticProperty.ALLOWS_ZERO_OR_ONE, role);
            comp = new AtomicSortComparer(comp);
        } else {
            String dataType = ((StringValue)dataTypeExpression).getStringValue();
            if (dataType.equals("text")) {
                comp = new TextComparer(comp);
            } else if (dataType.equals("number")) {
                comp = new NumericComparer();
            } else {
                DynamicError err = new DynamicError("data-type on xsl:sort must be 'text' or 'number'");
                err.setErrorCode("XT0030");
                throw err;
            }
        }

        comparer = getOrderedComparer(comp, orderX);
    }

    private Comparator getOrderedComparer(Comparator base, String order)
    throws XPathException {
        if (order.equals("ascending")) {
            return base;
        } else if (order.equals("descending")) {
            return new DescendingComparer(base);
        } else {
            DynamicError err = new DynamicError("order must be 'ascending' or 'descending'");
            err.setErrorCode("XT0030");
            throw err;
        }
    }

    private Comparator getCaseOrderComparer(Collator base, String caseOrder)
    throws XPathException {
        if (caseOrder.equals("#default")) {
            return base;
        } else if (caseOrder.equals("lower-first")) {
            return new LowercaseFirstComparer(base);
        } else if (caseOrder.equals("upper-first")) {
            return new UppercaseFirstComparer(base);
        } else {
            DynamicError err = new DynamicError("case-order must be 'lower-first' or 'upper-first'");
            err.setErrorCode("XT0030");
            throw err;
        }
    }

    /**
    * Get the comparer which is used to compare two values according to this sort key.
    */

    public Comparator getComparer(XPathContext context) throws XPathException {
        return comparer;
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
