package org.orbeon.saxon.s9api;

import org.orbeon.saxon.query.XQueryExpression;

/**
 * An XQueryExecutable represents the compiled form of a query.
 * To execute the query, it must first be loaded to form an {@link org.orbeon.saxon.s9api.XQueryEvaluator}.
 *
 * <p>An XQueryExecutable is immutable, and therefore thread-safe.
 *  It is simplest to load a new XsltTransformer each time the stylesheet is to be run.
 *  However, the XsltTransformer is serially reusable within a single thread. </p>
 *
 * <p>An XQueryExecutable is created by using one of the <code>compile</code> methods on the
 * {@link org.orbeon.saxon.s9api.XQueryCompiler} class.</p>
 *
 * @since 9.0
 */
public class XQueryExecutable {

    Processor processor;
    XQueryExpression exp;

    protected XQueryExecutable(Processor processor, XQueryExpression exp) {
        this.processor = processor;
        this.exp = exp;
    }

    /**
     * Load the stylesheet to prepare it for execution.
     * @return  An XsltTransformer. The returned XsltTransformer can be used to set up the
     * dynamic context for stylesheet evaluation, and to run the stylesheet.
     */

    public XQueryEvaluator load() {
        return new XQueryEvaluator(processor, exp);
    }

    /**
     * Get the ItemType of the items in the result of the query, as determined by static analysis. This
     * is the most precise ItemType that the processor is able to determine from static examination of the
     * query; the actual items in the query result are guaranteed to belong to this ItemType or to a subtype
     * of this ItemType.
     * @return the statically-determined ItemType of the result of the query
     * @since 9.1
     */

    public ItemType getResultItemType() {
        org.orbeon.saxon.type.ItemType it =
                exp.getExpression().getItemType(processor.getUnderlyingConfiguration().getTypeHierarchy());
        return new ItemType(it, processor);
    }

    /**
     * Get the statically-determined cardinality of the result of the query. This is the most precise cardinality
     * that the processor is able to determine from static examination of the query.
     * @return the statically-determined cardinality of the result of the query
     * @since 9.1
     */

    public OccurrenceIndicator getResultCardinality() {
        int card = exp.getExpression().getCardinality();
        return OccurrenceIndicator.getOccurrenceIndicator(card);
    }

    /**
     * Ask whether the query is an updating query: that is, whether it returns a Pending Update List
     * rather than a Value. Note that queries using the XUpdate copy-modify syntax are not considered
     * to be updating queries.
     * @return true if the query is an updating query, false if not
     * @since 9.1
     */

    public boolean isUpdateQuery() {
        return exp.isUpdateQuery();
    }

    /**
     * Get the underlying implementation object representing the compiled stylesheet. This provides
     * an escape hatch into lower-level APIs. The object returned by this method may change from release
     * to release.
     * @return the underlying implementation of the compiled stylesheet
     */

    public XQueryExpression getUnderlyingCompiledQuery() {
        return exp;
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
// The Original Code is: all this file
//
// The Initial Developer of the Original Code is Michael H. Kay.
//
// Contributor(s):
//

