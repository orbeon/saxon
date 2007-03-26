package org.orbeon.saxon.sort;
import org.orbeon.saxon.Configuration;
import org.orbeon.saxon.expr.Container;
import org.orbeon.saxon.expr.Expression;
import org.orbeon.saxon.expr.StaticContext;
import org.orbeon.saxon.expr.XPathContext;
import org.orbeon.saxon.instruct.Executable;
import org.orbeon.saxon.trans.DynamicError;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.type.TypeHierarchy;
import org.orbeon.saxon.value.EmptySequence;
import org.orbeon.saxon.value.StringValue;

import java.io.Serializable;
import java.util.Comparator;
import java.util.Properties;
import java.net.URI;
import java.net.URISyntaxException;

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
    protected Expression dataTypeExpression = null;
                                        // used when the type is not known till run-time
    protected Expression caseOrder = defaultCaseOrder;
    protected Expression language = defaultLanguage;
    protected Expression collationName = null;
    protected Expression stable = null; // not actually used, but present so it can be validated
    protected transient Comparator collation;     // usually a Collator, but not always
    protected String baseURI;           // needed in case collation URI is relative
    protected boolean emptyLeast = true;
                            // used only in XQuery at present
    protected boolean backwardsCompatible = false;
    protected Container parentExpression;

    protected transient Comparator comparer = null;
    // Note, the "collation" defines the collating sequence for the sort key. The
    // "comparer" is what is actually used to do comparisons, after taking into account
    // ascending/descending, caseOrder, etc.

    // The comparer is transient because a RuleBasedCollator is not serializable. This means that
    // when a stylesheet is compiled, the comparer is discarded, which means a new comparer will be
    // constructed for each sort at run-time.

    public void setParentExpression(Container container) {
        parentExpression = container;
    }

    public Container getParentExpression() {
        return parentExpression;
    }

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

    public void setBaseURI(String baseURI) {
        this.baseURI = baseURI;
    }

    public String getBaseURI() {
        return baseURI;
    }

    public void setStable(Expression stable) {
        this.stable = stable;
    }

    public Expression getStable() {
        return stable;
    }

    public void setBackwardsCompatible(boolean compatible) {
        backwardsCompatible = compatible;
    }

    public boolean isBackwardsCompatible() {
        return backwardsCompatible;
    }


    /**
     * Set whether empty sequence comes before other values or after them
     * @param emptyLeast true if () is considered lower than any other value
     */

    public void setEmptyLeast(boolean emptyLeast) {
        this.emptyLeast = emptyLeast;
    }

    public boolean getEmptyLeast() {
        return emptyLeast;
    }

    /**
     * Determine whether the sort key definition is fixed, that is, whether all the information needed
     * to create a Comparator is known statically
     */

    public boolean isFixed() {
        return (order instanceof StringValue &&
                (dataTypeExpression == null ||
                    dataTypeExpression instanceof EmptySequence ||
                    dataTypeExpression instanceof StringValue) &&
                caseOrder instanceof StringValue &&
                language instanceof StringValue &&
                (stable == null || stable instanceof StringValue) &&
                (collationName == null || collationName instanceof StringValue));
    }

    public SortKeyDefinition simplify(StaticContext env, Executable exec) throws XPathException {
        sortKey = sortKey.simplify(env);
        order = order.simplify(env);
        if (dataTypeExpression != null) {
            dataTypeExpression = dataTypeExpression.simplify(env);
        }
        caseOrder = caseOrder.simplify(env);
        language = language.simplify(env);
        if (stable != null) {
            stable = stable.simplify(env);
        }
        return this;
    }

    /**
    * Allocate a Comparator to perform the comparisons described by this sort key component
    */

    public Comparator makeComparator(XPathContext context) throws XPathException {

        String orderX = order.evaluateAsString(context);

        final Configuration config = context.getConfiguration();
        final TypeHierarchy th = config.getTypeHierarchy();

        Comparator comp;
        if (collation != null) {
            comp = collation;
        } else if (collationName != null) {
            String cname = collationName.evaluateAsString(context);
            URI collationURI;
            try {
                collationURI = new URI(cname);
                if (!collationURI.isAbsolute()) {
                    if (baseURI == null) {
                        throw new DynamicError("Collation URI is relative, and base URI is unknown");
                    } else {
                        URI base = new URI(baseURI);
                        collationURI = base.resolve(collationURI);
                    }
                }
            } catch (URISyntaxException err) {
                throw new DynamicError("Collation name " + cname + " is not a valid URI: " + err);
            }
            try {
                comp = context.getCollation(collationURI.toString());
            } catch (XPathException e) {
                if ("FOCH0002".equals(e.getErrorCodeLocalPart())) {
                    e.setErrorCode("XTDE1035");
                }
                throw e;
            }
        } else {
            String caseOrderX = caseOrder.evaluateAsString(context);
            String languageX = language.evaluateAsString(context);
            Properties props = new Properties();
            if (!languageX.equals("")) {
                props.setProperty("lang", languageX);
            }
            if (!caseOrderX.equals("#default")) {
                props.setProperty("case-order", caseOrderX);
            }
            comp = config.getPlatform().makeCollation(config, props);
        }

        if (dataTypeExpression==null) {
            int type = sortKey.getItemType(th).getAtomizedItemType().getPrimitiveType();
            comp = AtomicSortComparer.makeSortComparer(comp, type, context);
            if (!emptyLeast) {
                comp = new EmptyGreatestComparer((AtomicComparer)comp);
            }
        } else {
            String dataType = dataTypeExpression.evaluateAsString(context);
            if (dataType.equals("text")) {
                comp = new TextComparer(comp);
            } else if (dataType.equals("number")) {
                comp = NumericComparer.getInstance();
            } else {
                DynamicError err = new DynamicError("data-type on xsl:sort must be 'text' or 'number'");
                err.setErrorCode("XTDE0030");
                throw err;
            }
        }

        if (stable != null) {
            StringValue stableVal = (StringValue)stable.evaluateItem(context);
            String s = stableVal.getStringValue().trim();
            if (s.equals("yes") || s.equals("no")) {
                // no action
            } else {
                DynamicError err = new DynamicError("Value of 'stable' on xsl:sort must be 'yes' or 'no'");
                err.setErrorCode("XTDE0030");
                throw err;
            }
        }

        if (orderX.equals("ascending")) {
            return comp;
        } else if (orderX.equals("descending")) {
            return new DescendingComparer(comp);
        } else {
            DynamicError err1 = new DynamicError("order must be 'ascending' or 'descending'");
            err1.setErrorCode("XTDE0030");
            throw err1;
        }


    }

    /**
    * Set the comparer which is used to compare two values according to this sort key.
    */

    public void setComparer(Comparator comp) {
        comparer = comp;
    }

    /**
    * Get the comparer which is used to compare two values according to this sort key.
    */

    public Comparator getComparer() {
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
