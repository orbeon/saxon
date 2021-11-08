package org.orbeon.saxon.s9api;

import org.orbeon.saxon.expr.StaticContext;
import org.orbeon.saxon.sxpath.IndependentContext;
import org.orbeon.saxon.sxpath.XPathExpression;
import org.orbeon.saxon.sxpath.XPathVariable;

import java.util.ArrayList;

/**
 * An XPathExecutable represents the compiled form of an XPath expression.
 * To evaluate the expression, it must first be loaded to form an {@link XPathSelector}.
 *
 * <p>An XPathExecutable is immutable, and therefore thread-safe. It is simplest to load
 * a new XPathSelector each time the expression is to be evaluated. However, the XPathSelector
 * is serially reusable within a single thread.</p>
 *
 * <p>An XPathExecutable is created by using the {@link XPathCompiler#compile} method
 * on the {@link XPathCompiler} class.</p>
 */

public class XPathExecutable {

    private XPathExpression exp;
    private Processor processor;
    private IndependentContext env;
    private ArrayList<XPathVariable> declaredVariables;

    // protected constructor

    protected XPathExecutable(XPathExpression exp, Processor processor,
            IndependentContext env, ArrayList<XPathVariable> declaredVariables) {
        this.exp = exp;
        this.processor = processor;
        this.env = env;
        this.declaredVariables = declaredVariables;
    }

    /**
     * Load the compiled XPath expression to prepare it for execution.
     * @return An XPathSelector. The returned XPathSelector can be used to set up the
     * dynamic context, and then to evaluate the expression.
     */

    public XPathSelector load() {
        return new XPathSelector(exp, declaredVariables);
    }

    /**
     * Get the ItemType of the items in the result of the expression, as determined by static analysis. This
     * is the most precise ItemType that the processor is able to determine from static examination of the
     * expression; the actual items in the expression result are guaranteed to belong to this ItemType or to a subtype
     * of this ItemType.
     * @return the statically-determined ItemType of the result of the expression
     * @since 9.1
     */

    public ItemType getResultItemType() {
        org.orbeon.saxon.type.ItemType it =
                exp.getInternalExpression().getItemType(processor.getUnderlyingConfiguration().getTypeHierarchy());
        return new ItemType(it, processor);
    }

    /**
     * Get the statically-determined cardinality of the result of the expression. This is the most precise cardinality
     * that the processor is able to determine from static examination of the expression.
     * @return the statically-determined cardinality of the result of the expression
     * @since 9.1
     */

    public OccurrenceIndicator getResultCardinality() {
        int card = exp.getInternalExpression().getCardinality();
        return OccurrenceIndicator.getOccurrenceIndicator(card);
    }


    /**
     * Get the underlying implementation object representing the compiled XPath expression.
     * This method provides access to lower-level Saxon classes and methods which may be subject to change
     * from one release to the next.
     * @return the underlying compiled XPath expression.
     */

    public XPathExpression getUnderlyingExpression() {
        return exp;
    }

    /**
     * Get the underlying implementation object representing the static context of the compiled
     * XPath expression. This method provides access to lower-level Saxon classes and methods which may be
     * subject to change from one release to the next.
     * @return the underlying static context.
     */

    public StaticContext getUnderlyingStaticContext() {
        return env;
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

