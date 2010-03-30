package org.orbeon.saxon.s9api;

import org.orbeon.saxon.Controller;
import org.orbeon.saxon.PreparedStylesheet;
import org.orbeon.saxon.Configuration;
import org.orbeon.saxon.trace.ExpressionPresenter;

/**
 * An XsltExecutable represents the compiled form of a stylesheet.
 * To execute the stylesheet, it must first be loaded to form an {@link XsltTransformer}.
 *
 * <p>An XsltExecutable is immutable, and therefore thread-safe.
 *  It is simplest to load a new XsltTransformer each time the stylesheet is to be run.
 *  However, the XsltTransformer is serially reusable within a single thread. </p>
 *
 * <p>An XsltExecutable is created by using one of the <code>compile</code> methods on the
 * {@link XsltCompiler} class.</p>
 */
public class XsltExecutable {

    Processor processor;
    PreparedStylesheet pss;

    protected XsltExecutable(Processor processor, PreparedStylesheet pss) {
        this.processor = processor;
        this.pss = pss;
    }

    /**
     * Load the stylesheet to prepare it for execution.
     * @return  An XsltTransformer. The returned XsltTransformer can be used to set up the
     * dynamic context for stylesheet evaluation, and to run the stylesheet.
     */

    public XsltTransformer load() {
        return new XsltTransformer(processor, (Controller)pss.newTransformer());
    }

    /**
     * Produce a diagnostic representation of the compiled stylesheet, in XML form.
     * <p><i>The detailed form of this representation is not stable (or even documented).<i></p>
     * @param destination the destination for the XML document containing the diagnostic representation
     * of the compiled stylesheet
     * @since 9.1
     */

    public void explain(Destination destination) throws SaxonApiException {
        Configuration config = processor.getUnderlyingConfiguration();
        pss.explain(new ExpressionPresenter(config, destination.getReceiver(config)));
    }

    /**
     * Get the underlying implementation object representing the compiled stylesheet. This provides
     * an escape hatch into lower-level APIs. The object returned by this method may change from release
     * to release.
     * @return the underlying implementation of the compiled stylesheet
     */

    public PreparedStylesheet getUnderlyingCompiledStylesheet() {
        return pss;
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

