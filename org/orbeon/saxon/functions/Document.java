package org.orbeon.saxon.functions;
import org.orbeon.saxon.*;
import org.orbeon.saxon.event.Builder;
import org.orbeon.saxon.event.Receiver;
import org.orbeon.saxon.event.Sender;
import org.orbeon.saxon.event.PipelineConfiguration;
import org.orbeon.saxon.expr.*;
import org.orbeon.saxon.om.*;
import org.orbeon.saxon.sort.DocumentOrderIterator;
import org.orbeon.saxon.sort.GlobalOrderComparer;
import org.orbeon.saxon.trans.DynamicError;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.value.Cardinality;

import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.URIResolver;
import javax.xml.transform.SourceLocator;
import javax.xml.transform.dom.DOMSource;
import java.net.URI;
import java.net.URISyntaxException;


/**
 * Implements the XSLT document() function
 */

public class Document extends SystemFunction implements XSLTFunction {

    private String expressionBaseURI = null;

    public void checkArguments(StaticContext env) throws XPathException {
        if (expressionBaseURI == null) {
            // only do this once. The second call supplies an env pointing to the containing
            // xsl:template, which has a different base URI (and in a simplified stylesheet, has no base URI)
            super.checkArguments(env);
            expressionBaseURI = env.getBaseURI();
            Optimizer opt = env.getConfiguration().getOptimizer();
            argument[0] = ExpressionTool.unsorted(opt, argument[0], false);
        }
    }

    /**
    * Determine the static cardinality
    */

    public int computeCardinality() {
        Expression expression = argument[0];
        if (Cardinality.allowsMany(expression.getCardinality())) {
            return StaticProperty.ALLOWS_ZERO_OR_MORE;
        } else {
            return StaticProperty.ALLOWS_ZERO_OR_ONE;
        }
        // may have to revise this if the argument can be a list-valued element or attribute
    }

    /**
    * Get the static properties of this expression (other than its type). The result is
    * bit-signficant. These properties are used for optimizations. In general, if
    * property bit is set, it is true, but if it is unset, the value is unknown.
    */

    public int computeSpecialProperties() {
        return StaticProperty.ORDERED_NODESET |
                StaticProperty.PEER_NODESET |
                StaticProperty.NON_CREATIVE;
        // Declaring it as a peer node-set expression avoids sorting of expressions such as
        // document(XXX)/a/b/c
        // The document() function might appear to be creative: but it isn't, because multiple calls
        // with the same arguments will produce identical results.
    }

    /**
    * preEvaluate: this method suppresses compile-time evaluation by doing nothing
    */

    public Expression preEvaluate(StaticContext env) {
        return this;
    }


    /**
    * iterate() handles evaluation of the function:
    * it returns a sequence of Document nodes
    */

    public SequenceIterator iterate(XPathContext context) throws XPathException {
        int numArgs = argument.length;

        SequenceIterator hrefSequence = argument[0].iterate(context);
        String baseURI = null;
        if (numArgs==2) {
            // we can trust the type checking: it must be a node
            NodeInfo base = (NodeInfo)argument[1].evaluateItem(context);
            baseURI = base.getBaseURI();
        }

        DocumentMappingFunction map = new DocumentMappingFunction(context);
        map.baseURI = baseURI;
        map.stylesheetURI = expressionBaseURI;
        map.locator = this;

        ItemMappingIterator iter = new ItemMappingIterator(hrefSequence, map);

        Expression expression = argument[0];
        if (Cardinality.allowsMany(expression.getCardinality())) {
            return new DocumentOrderIterator(iter, GlobalOrderComparer.getInstance());
            // this is to make sure we eliminate duplicates: two href's might be the same
        } else {
            return iter;
        }
    }

    private static class DocumentMappingFunction implements ItemMappingFunction {

        public String baseURI;
        public String stylesheetURI;
        public SourceLocator locator;
        public XPathContext context;

        public DocumentMappingFunction(XPathContext context) {
            this.context = context;
        }

        public Item map(Item item) throws XPathException {

            if (baseURI==null) {
                if (item instanceof NodeInfo) {
                    baseURI = ((NodeInfo)item).getBaseURI();
                } else {
                    baseURI = stylesheetURI;
                }
            }
            NodeInfo doc = makeDoc(item.getStringValue(), baseURI, context, locator);
            return doc;
        }
    }


    /**
    * Supporting routine to load one external document given a URI (href) and a baseURI
    */

    public static NodeInfo makeDoc(String href, String baseURL, XPathContext c, SourceLocator locator)
            throws XPathException {

        // If the href contains a fragment identifier, strip it out now
        //System.err.println("Entering makeDoc " + href);
        int hash = href.indexOf('#');

        String fragmentId = null;
        if (hash>=0) {
            if (hash==href.length()-1) {
                // # sign at end - just ignore it
                href = href.substring(0, hash);
            } else {
                fragmentId = href.substring(hash+1);
                href = href.substring(0, hash);
                if (!c.getConfiguration().getNameChecker().isValidNCName(fragmentId)) {
                    DynamicError de = new DynamicError(
                            "The fragment identifier " + Err.wrap(fragmentId) + " is not a valid NCName");
                    de.setErrorCode("XTRE1160");
                    de.setXPathContext(c);
                    throw de;
                }
            }
        }

        Controller controller = c.getController();

        // Resolve relative URI
        String documentKey;
        URIResolver resolver = controller.getURIResolver();
        if (resolver instanceof RelativeURIResolver) {
            try {
                documentKey = ((RelativeURIResolver)resolver).makeAbsolute(href, baseURL);
            } catch (TransformerException e) {
                documentKey = '/' + href;
                baseURL = "";
            }
        } else {
            if (baseURL==null) {    // no base URI available
                try {
                    // the href might be an absolute URL
                    documentKey = (new URI(href)).toString();
                } catch (URISyntaxException err) {
                    // it isn't; but the URI resolver might know how to cope
                    documentKey = '/' + href;
                    baseURL = "";
                }
            } else if (href.equals("")) {
                // common case in XSLT, which java.net.URI#resolve() does not handle correctly
                documentKey = baseURL;
            } else {
                try {
                    URI url = new URI(baseURL).resolve(href);
                    documentKey = url.toString();
                } catch (URISyntaxException err) {
                    documentKey = baseURL + "/../" + href;
                } catch (IllegalArgumentException err) {
                    documentKey = baseURL + "/../" + href;
                }
            }
        }

        // see if the document is already loaded

        DocumentInfo doc = controller.getDocumentPool().find(documentKey);
        if (doc != null) {
            return getFragment(doc, fragmentId, c);
        }

        // check that the document was not written by this transformation

        if (!controller.checkUniqueOutputDestination(documentKey)) {
            DynamicError err = new DynamicError(
                    "Cannot read a document that was written during the same transformation: " + documentKey);
            err.setXPathContext(c);
            err.setErrorCode("XTRE1500");
            throw err;
        }

        try {
            // Get a Source from the URIResolver

            URIResolver r = controller.getURIResolver();
            Source source = null;
            if (r != null) {
                try {
                    source = r.resolve(href, baseURL);
                } catch (Exception ex) {
                    DynamicError de = new DynamicError("Exception thrown by URIResolver", ex);
                    throw de;
                }
            }

            // if a user URI resolver returns null, try the standard one
            // (Note, the standard URI resolver never returns null)
            if (source==null && !(r instanceof NonDelegatingURIResolver)) {
                r = controller.getStandardURIResolver();
                source = r.resolve(href, baseURL);
            }
            //System.err.println("URI resolver returned " + source.getClass() + " " + source.getSystemId());
            Configuration config = controller.getConfiguration();
            source = config.getSourceResolver().resolveSource(source, config);
            //System.err.println("Resolved source " + source.getClass() + " " + source.getSystemId());

            DocumentInfo newdoc;
            if (source instanceof NodeInfo || source instanceof DOMSource) {
                NodeInfo startNode = controller.prepareInputTree(source);
                newdoc = startNode.getDocumentRoot();
            } else {
                Builder b = controller.makeBuilder();
                Receiver s = controller.makeStripper(b);
                if (controller.getExecutable().stripsInputTypeAnnotations()) {
                    s = controller.getConfiguration().getAnnotationStripper(s);
                }
                new Sender(controller.makePipelineConfiguration()).send(source, s);
                newdoc = (DocumentInfo)b.getCurrentRoot();
                if (source instanceof AugmentedSource && ((AugmentedSource)source).isPleaseCloseAfterUse()) {
                    ((AugmentedSource)source).close();
                }
            }
            controller.registerDocument(newdoc, documentKey);
            controller.addUnavailableOutputDestination(documentKey);
            return getFragment(newdoc, fragmentId, c);

        } catch (TransformerException err) {
            DynamicError xerr = DynamicError.makeDynamicError(err);
            xerr.setLocator(locator);
            xerr.setErrorCode("FODC0005");
            try {
                controller.recoverableError(xerr);
            } catch (XPathException err2) {
                throw new DynamicError(err);
            }
            return null;
        }
    }

    /**
     * Copy the documents identified by this expression to a given Receiver. This method is used only when it is
     * known that the documents are being copied, because there is then no problem about node identity.
     */

    public void sendDocuments(XPathContext context, Receiver out) throws XPathException {
        SequenceIterator hrefSequence = argument[0].iterate(context);
        String explicitBaseURI = null;
        if (argument.length==2) {
            // we can trust the type checking: it must be a node
            NodeInfo base = (NodeInfo)argument[1].evaluateItem(context);
            explicitBaseURI = base.getBaseURI();
        }
        while (true) {
            Item href = hrefSequence.next();
            if (href == null) {
                break;
            }
            String base;
            if (explicitBaseURI == null) {
                if (href instanceof NodeInfo) {
                    base = ((NodeInfo)href).getBaseURI();
                } else {
                    base = expressionBaseURI;
                }
            } else {
                base = explicitBaseURI;
            }
            sendDoc(href.getStringValue(), base, context, this, out);
        }
    }

    /**
     * Supporting routine to push one external document given a URI (href) and a baseURI to a given Receiver.
     * This method cannot handle fragment identifiers
    */

    public static void sendDoc(String href, String baseURL, XPathContext c,
                               SourceLocator locator, Receiver out) throws XPathException {

        PipelineConfiguration pipe = out.getPipelineConfiguration();
        if (pipe == null) {
            pipe = c.getController().makePipelineConfiguration();
        }

        // Resolve relative URI

        String documentKey;
        if (baseURL==null) {    // no base URI available
            try {
                // the href might be an absolute URL
                documentKey = (new URI(href)).toString();
            } catch (URISyntaxException err) {
                // it isn't; but the URI resolver might know how to cope
                documentKey = '/' + href;
                baseURL = "";
            }
        } else if (href.equals("")) {
            // common case in XSLT, which java.net.URI#resolve() does not handle correctly
            documentKey = baseURL;
        } else {
            try {
                URI url = new URI(baseURL).resolve(href);
                documentKey = url.toString();
            } catch (URISyntaxException err) {
                documentKey = baseURL + "/../" + href;
            } catch (IllegalArgumentException err) {
                documentKey = baseURL + "/../" + href;
            }
        }

        Controller controller = c.getController();

        // see if the document is already loaded

        DocumentInfo doc = controller.getDocumentPool().find(documentKey);
        Source source = null;
        if (doc != null) {
            source = doc;
        } else {

            try {
                // Get a Source from the URIResolver

                URIResolver r = controller.getURIResolver();
                if (r != null) {
                    source = r.resolve(href, baseURL);
                }

                // if a user URI resolver returns null, try the standard one
                // (Note, the standard URI resolver never returns null)
                if (source==null) {
                    r = controller.getStandardURIResolver();
                    source = r.resolve(href, baseURL);
                }
                if (source instanceof NodeInfo || source instanceof DOMSource) {
                    NodeInfo startNode = controller.prepareInputTree(source);
                    source = startNode.getDocumentRoot();
                }
            } catch (TransformerException err) {
                DynamicError xerr = DynamicError.makeDynamicError(err);
                xerr.setLocator(locator);
                xerr.setErrorCode("FODC0005");
                throw xerr;
            }
        }
        out = controller.makeStripper(out);
        out.setPipelineConfiguration(pipe);
        if (controller.getExecutable().stripsInputTypeAnnotations()) {
            out = controller.getConfiguration().getAnnotationStripper(out);
            out.setPipelineConfiguration(pipe);
        }
        new Sender(pipe).send(source, out);
    }


    /**
    * Resolve the fragment identifier within a URI Reference.
    * Only "bare names" XPointers are recognized, that is, a fragment identifier
    * that matches an ID attribute value within the target document.
    * @return the element within the supplied document that matches the
    * given id value; or null if no such element is found.
    */

    private static NodeInfo getFragment(DocumentInfo doc, String fragmentId, XPathContext context)
    throws XPathException {
        // TODO: we only support one kind of fragment identifier. The rules say
        // that the interpretation of the fragment identifier depends on media type,
        // but we aren't getting the media type from the URIResolver.
        if (fragmentId==null) {
            return doc;
        }
        if (!context.getConfiguration().getNameChecker().isValidNCName(fragmentId)) {
            DynamicError err = new DynamicError("Invalid fragment identifier in URI");
            err.setXPathContext(context);
            err.setErrorCode("XTRE1160");
            try {
                context.getController().recoverableError(err);
            } catch (DynamicError dynamicError) {
                throw err;
            }
            return doc;
        }
        return doc.selectID(fragmentId);
    }


    public PathMap.PathMapNode addToPathMap(PathMap pathMap, PathMap.PathMapNode pathMapNode) {
        return addDocToPathMap(pathMap, pathMapNode);
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
