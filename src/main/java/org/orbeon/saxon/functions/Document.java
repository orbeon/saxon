package org.orbeon.saxon.functions;
import org.orbeon.saxon.*;
import org.orbeon.saxon.event.Builder;
import org.orbeon.saxon.event.PipelineConfiguration;
import org.orbeon.saxon.event.Receiver;
import org.orbeon.saxon.event.Sender;
import org.orbeon.saxon.expr.*;
import org.orbeon.saxon.om.DocumentInfo;
import org.orbeon.saxon.om.Item;
import org.orbeon.saxon.om.NodeInfo;
import org.orbeon.saxon.om.SequenceIterator;
import org.orbeon.saxon.sort.DocumentOrderIterator;
import org.orbeon.saxon.sort.GlobalOrderComparer;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.trans.Err;
import org.orbeon.saxon.value.Cardinality;
import org.orbeon.saxon.value.Whitespace;
import org.orbeon.saxon.value.AtomicValue;
import org.orbeon.saxon.value.SingletonNode;

import javax.xml.transform.Source;
import javax.xml.transform.SourceLocator;
import javax.xml.transform.TransformerException;
import javax.xml.transform.URIResolver;
import javax.xml.transform.dom.DOMSource;
import java.net.URI;
import java.net.URISyntaxException;


/**
 * Implements the XSLT document() function
 */

public class Document extends SystemFunction implements XSLTFunction {

    private String expressionBaseURI = null;
    private boolean readOnce = false;   // TODO: implement this. The idea is that if streaming copy cannot be
    // used, then document projection will be used instead, for this specific call on the document() function.

    /**
     * Indicate that the document(s) will be read once only (or that they should be treated as if they
     * are read once only. This means (a) the document will not be held in memory after all references
     * to it go out of scope, and (b) if the query or transformation tries to read it again, it will get a new
     * copy, with different node identities, and potentially with different content. It also means that the
     * document is eligible for document projection.
     * @param once true if this document is to be treated as being read once only
     */

    public void setReadOnce(boolean once) {
        readOnce = once;
    }

    /**
     * Ask whether this document has been marked as being read once only.
     * @return true if the document has been marked as being read once only
     */

    public boolean isReadOnce() {
        return readOnce;
    }


    public void checkArguments(ExpressionVisitor visitor) throws XPathException {
        if (expressionBaseURI == null) {
            // only do this once. The second call supplies an env pointing to the containing
            // xsl:template, which has a different base URI (and in a simplified stylesheet, has no base URI)
            super.checkArguments(visitor);
            expressionBaseURI = visitor.getStaticContext().getBaseURI();
            Optimizer opt = visitor.getConfiguration().getOptimizer();
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
     * Get the base URI from the static context
     * @return the base URI
     */

    public String getStaticBaseURI() {
        return expressionBaseURI;
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
    * preEvaluate: the document() function can be evaluated at compile time if (a) the argument
     * is a string literal, and (b) the option {@link FeatureKeys#PRE_EVALUATE_DOC_FUNCTION} is set.
     * @param visitor an expression visitor
     */

    public Expression preEvaluate(ExpressionVisitor visitor) {
        Configuration config = visitor.getConfiguration();
        if (getNumberOfArguments() == 1 &&
                ((Boolean)config.getConfigurationProperty(FeatureKeys.PRE_EVALUATE_DOC_FUNCTION)).booleanValue()) {
            try {
                AtomicValue hrefVal = (AtomicValue)argument[0].evaluateItem(null);
                if (hrefVal==null) {
                    return null;
                }
                String href = hrefVal.getStringValue();
                if (href.indexOf('#') >= 0) {
                    return this;
                }
                NodeInfo item = Document.preLoadDoc(href, expressionBaseURI, config, this);
                if (item!=null) {
                    return new Literal(new SingletonNode(item));
                }
            } catch (Exception err) {
                // ignore the exception and try again at run-time
                return this;
            }
        }
        return this;
    }

    /**
     * Add a representation of this expression to a PathMap. The PathMap captures a map of the nodes visited
     * by an expression in a source tree.
     *
     * @param pathMap     the PathMap to which the expression should be added
     * @param pathMapNodeSet the set of nodes in the path map that are affected
     * @return the pathMapNode representing the focus established by this expression, in the case where this
     *         expression is the first operand of a path expression or filter expression
     */

    public PathMap.PathMapNodeSet addToPathMap(PathMap pathMap, PathMap.PathMapNodeSet pathMapNodeSet) {
        return addDocToPathMap(pathMap, pathMapNodeSet);
    }
    

    /**
     * Copy an expression. This makes a deep copy.
     * @return the copy of the original expression
     */

    public Expression copy() {
        Document d = (Document)super.copy();
        d.expressionBaseURI = expressionBaseURI;
        d.readOnce = readOnce;
        return d;
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
            String b = baseURI;
            if (b==null) {
                if (item instanceof NodeInfo) {
                    b = ((NodeInfo)item).getBaseURI();
                } else {
                    b = stylesheetURI;
                }
            }
            return makeDoc(item.getStringValue(), b, context, locator);
        }
    }

    /**
     * Supporting routine to load one external document given a URI (href) and a baseURI. This is used
     * in the normal case when a document is loaded at run-time (that is, when a Controller is available)
     * @param href the relative URI
     * @param baseURI the base URI
     * @param c the dynamic XPath context
     * @param locator used to identify the location of the instruction in event of error
     * @return the root of the constructed document, or the selected element within the document
     * if a fragment identifier was supplied
    */

    public static NodeInfo makeDoc(String href, String baseURI, XPathContext c, SourceLocator locator)
            throws XPathException {

        Configuration config = c.getConfiguration();

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
                if (!config.getNameChecker().isValidNCName(fragmentId)) {
                    XPathException de = new XPathException("The fragment identifier " + Err.wrap(fragmentId) + " is not a valid NCName");
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
        if (resolver == null) {
            resolver = controller.getStandardURIResolver();
        }
        if (resolver instanceof RelativeURIResolver) {
            // If this is the case, the URIResolver is responsible for absolutization as well as dereferencing
            try {
                documentKey = ((RelativeURIResolver)resolver).makeAbsolute(href, baseURI);
            } catch (TransformerException e) {
                documentKey = '/' + href;
                baseURI = "";
            }
        } else {
            // Saxon takes charge of absolutization, leaving the user URIResolver to handle dereferencing only
            if (baseURI==null) {    // no base URI available
                try {
                    // the href might be an absolute URL
                    documentKey = (new URI(href)).toString();
                } catch (URISyntaxException err) {
                    // it isn't; but the URI resolver might know how to cope
                    documentKey = '/' + href;
                    baseURI = "";
                }
            } else if (href.length() == 0) {
                // common case in XSLT, which java.net.URI#resolve() does not handle correctly
                documentKey = baseURI;
            } else {
                try {
                    URI uri = new URI(baseURI).resolve(href);
                    documentKey = uri.toString();
                } catch (URISyntaxException err) {
                    documentKey = baseURI + "/../" + href;
                } catch (IllegalArgumentException err) {
                    documentKey = baseURI + "/../" + href;
                }
            }
        }

        // see if the document is already loaded

        DocumentInfo doc = config.getGlobalDocumentPool().find(documentKey);
        if (doc != null) {
            return doc;
        }

        doc = controller.getDocumentPool().find(documentKey);
        if (doc != null) {
            return getFragment(doc, fragmentId, c);
        }

        // check that the document was not written by this transformation

        if (!controller.checkUniqueOutputDestination(documentKey)) {
            XPathException err = new XPathException(
                    "Cannot read a document that was written during the same transformation: " + documentKey);
            err.setXPathContext(c);
            err.setErrorCode("XTRE1500");
            throw err;
        } 

        try {
            // Get a Source from the URIResolver

            Source source;
            if (resolver instanceof RelativeURIResolver) {
                try {
                    source = ((RelativeURIResolver)resolver).dereference(documentKey);
                } catch (Exception ex) {
                    XPathException de = new XPathException("Exception thrown by URIResolver", ex);
                    if (controller.getConfiguration().isTraceExternalFunctions()) {
                        ex.printStackTrace();
                    }
                    de.setLocator(locator);
                    throw de;
                }
            } else {
                try {
                    source = resolver.resolve(href, baseURI);
                } catch (Exception ex) {
                    XPathException de = new XPathException("Exception thrown by URIResolver", ex);
                    if (controller.getConfiguration().isTraceExternalFunctions()) {
                        ex.printStackTrace();
                    }
                    de.setLocator(locator);
                    throw de;
                }
            }

            // if a user URI resolver returns null, try the standard one
            // (Note, the standard URI resolver never returns null)
            if (source==null && !(resolver instanceof NonDelegatingURIResolver)) {
                resolver = controller.getStandardURIResolver();
                if (resolver instanceof RelativeURIResolver) {
                    source = ((RelativeURIResolver)resolver).dereference(documentKey);
                } else {
                    source = resolver.resolve(href, baseURI);
                }
            }

            //System.err.println("URI resolver returned " + source.getClass() + " " + source.getSystemId());
            source = config.getSourceResolver().resolveSource(source, config);
            //System.err.println("Resolved source " + source.getClass() + " " + source.getSystemId());

            DocumentInfo newdoc;
            if (source instanceof NodeInfo || source instanceof DOMSource) {
                NodeInfo startNode = controller.prepareInputTree(source);
                newdoc = startNode.getDocumentRoot();
            } else {
                Builder b = controller.makeBuilder();
                Receiver s = b;
                source = AugmentedSource.makeAugmentedSource(source);
                ((AugmentedSource)source).setStripSpace(Whitespace.XSLT);
                if (controller.getExecutable().stripsInputTypeAnnotations()) {
                    s = controller.getConfiguration().getAnnotationStripper(s);
                }
                PathMap map = controller.getPathMapForDocumentProjection();
                if (map != null) {
                    PathMap.PathMapRoot pathRoot = map.getRootForDocument(documentKey);
                    if (pathRoot != null && !pathRoot.isReturnable() && !pathRoot.hasUnknownDependencies()) {
                        ((AugmentedSource)source).addFilter(config.makeDocumentProjector(pathRoot));
                    }
                }
                new Sender(b.getPipelineConfiguration()).send(source, s);
                newdoc = (DocumentInfo)b.getCurrentRoot();
                b.reset();
                if (source instanceof AugmentedSource && ((AugmentedSource)source).isPleaseCloseAfterUse()) {
                    ((AugmentedSource)source).close();
                }
            }
            controller.registerDocument(newdoc, documentKey);
            controller.addUnavailableOutputDestination(documentKey);
            return getFragment(newdoc, fragmentId, c);

        } catch (TransformerException err) {
            XPathException xerr = XPathException.makeXPathException(err);
            xerr.setLocator(locator);
            xerr.setErrorCode("FODC0002");
            try {
                controller.recoverableError(xerr);
            } catch (XPathException err2) {
                throw xerr;
            }
            return null;
        }
    }

    /**
     * Supporting routine to load one external document given a URI (href) and a baseURI. This is used
     * when the document is pre-loaded at compile time.
     * @param href the relative URI. This must not contain a fragment identifier
     * @param baseURI the base URI
     * @param config the Saxon configuration
     * @param locator used to identify the location of the instruction in event of error. May be null.
     * @return the root of the constructed document, or the selected element within the document
     * if a fragment identifier was supplied
    */

    public static NodeInfo preLoadDoc(String href, String baseURI, Configuration config, SourceLocator locator)
            throws XPathException {

        int hash = href.indexOf('#');
        if (hash>=0) {
            throw new XPathException("Fragment identifier not supported for preloaded documents");
        }

        // Resolve relative URI
        String documentKey;
        URIResolver resolver = config.getURIResolver();
        if (resolver instanceof RelativeURIResolver) {
            try {
                documentKey = ((RelativeURIResolver)resolver).makeAbsolute(href, baseURI);
            } catch (TransformerException e) {
                documentKey = '/' + href;
                baseURI = "";
            }
        } else {
            if (baseURI==null) {    // no base URI available
                try {
                    // the href might be an absolute URL
                    documentKey = (new URI(href)).toString();
                } catch (URISyntaxException err) {
                    // it isn't; but the URI resolver might know how to cope
                    documentKey = '/' + href;
                    baseURI = "";
                }
            } else if (href.length() == 0) {
                // common case in XSLT, which java.net.URI#resolve() does not handle correctly
                documentKey = baseURI;
            } else {
                try {
                    URI uri = new URI(baseURI).resolve(href);
                    documentKey = uri.toString();
                } catch (URISyntaxException err) {
                    documentKey = baseURI + "/../" + href;
                } catch (IllegalArgumentException err) {
                    documentKey = baseURI + "/../" + href;
                }
            }
        }

        // see if the document is already loaded

        DocumentInfo doc = config.getGlobalDocumentPool().find(documentKey);
        if (doc != null) {
            return doc;
        }

        try {
            // Get a Source from the URIResolver

            URIResolver r = resolver;
            Source source = null;
            if (r != null) {
                try {
                    source = r.resolve(href, baseURI);
                } catch (Exception ex) {
                    XPathException de = new XPathException("Exception thrown by URIResolver", ex);
                    if (config.isTraceExternalFunctions()) {
                        ex.printStackTrace();
                    }
                    de.setLocator(locator);
                    throw de;
                }
            }

            // if a user URI resolver returns null, try the standard one
            // (Note, the standard URI resolver never returns null)
            if (source==null && !(r instanceof NonDelegatingURIResolver)) {
                r = config.getSystemURIResolver();
                source = r.resolve(href, baseURI);
            }
            //System.err.println("URI resolver returned " + source.getClass() + " " + source.getSystemId());
            source = config.getSourceResolver().resolveSource(source, config);
            //System.err.println("Resolved source " + source.getClass() + " " + source.getSystemId());

            DocumentInfo newdoc = config.buildDocument(source);
            config.getGlobalDocumentPool().add(newdoc, documentKey);
            return newdoc;

        } catch (TransformerException err) {
            XPathException xerr = XPathException.makeXPathException(err);
            xerr.setLocator(locator);
            xerr.setErrorCode("FODC0002");
            throw new XPathException(err);
        }
    }


    /**
     * Copy the documents identified by this expression to a given Receiver. This method is used only when it is
     * known that the documents are being copied, because there is then no problem about node identity.
     * @param context the XPath dynamic context
     * @param out the destination to which the documents will be sent
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
     * @param href the relative URI
     * @param baseURL the base URI
     * @param c the XPath dynamic context
     * @param locator used to identify the lcoation of the instruction in case of error
     * @param out the destination where the document is to be sent
    */

    public static void sendDoc(String href, String baseURL, XPathContext c,
                               SourceLocator locator, Receiver out) throws XPathException {

        PipelineConfiguration pipe = out.getPipelineConfiguration();
        if (pipe == null) {
            pipe = c.getController().makePipelineConfiguration();
            out.setPipelineConfiguration(pipe);
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
        } else if (href.length() == 0) {
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
                XPathException xerr = XPathException.makeXPathException(err);
                xerr.setLocator(locator);
                if (xerr.getErrorCodeLocalPart() == null) {
                    xerr.setErrorCode("FODC0005");
                }
                throw xerr;
            }
        }
        //out = controller.makeStripper(out);
        source = AugmentedSource.makeAugmentedSource(source);
        ((AugmentedSource)source).setStripSpace(Whitespace.XSLT);

        if (controller.getExecutable().stripsInputTypeAnnotations()) {
            out = controller.getConfiguration().getAnnotationStripper(out);
        }
        try {
            new Sender(pipe).send(source, out);
        } catch (XPathException e) {
            e.maybeSetLocation(locator);
            if (e.getErrorCodeLocalPart() == null) {
                e.setErrorCode("FODC0005");
            }
            throw e;
        }
    }


    /**
     * Resolve the fragment identifier within a URI Reference.
     * Only "bare names" XPointers are recognized, that is, a fragment identifier
     * that matches an ID attribute value within the target document.
     * @param doc the document node
     * @param fragmentId the fragment identifier (an ID value within the document)
     * @param context the XPath dynamic context
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
            XPathException err = new XPathException("Invalid fragment identifier in URI");
            err.setXPathContext(context);
            err.setErrorCode("XTRE1160");
            try {
                context.getController().recoverableError(err);
            } catch (XPathException dynamicError) {
                throw err;
            }
            return doc;
        }
        return doc.selectID(fragmentId);
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
