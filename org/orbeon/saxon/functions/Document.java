package net.sf.saxon.functions;
import net.sf.saxon.Controller;
import net.sf.saxon.event.Builder;
import net.sf.saxon.event.Sender;
import net.sf.saxon.event.Stripper;
import net.sf.saxon.event.Receiver;
import net.sf.saxon.expr.*;
import net.sf.saxon.om.*;
import net.sf.saxon.sort.DocumentOrderIterator;
import net.sf.saxon.sort.GlobalOrderComparer;
import net.sf.saxon.trans.DynamicError;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.value.Cardinality;

import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.URIResolver;
import javax.xml.transform.dom.DOMSource;
import java.net.URI;
import java.net.URISyntaxException;


/**
 * Implements the XSLT document() function
 */

public class Document extends SystemFunction implements MappingFunction, XSLTFunction {

    private String expressionBaseURI = null;

    public void checkArguments(StaticContext env) throws XPathException {
        if (expressionBaseURI == null) {
            // only do this once. The second call supplies an env pointing to the containing
            // xsl:template, which has a different base URI (and in a simplified stylesheet, has no base URI)
            super.checkArguments(env);
            expressionBaseURI = env.getBaseURI();
            argument[0] = ExpressionTool.unsorted(argument[0], false);
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
    * Information passed through the mapping iterator
    */

    private static class DocumentMappingInfo {
        public String baseURI;
        public String stylesheetURI;
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

        DocumentMappingInfo info = new DocumentMappingInfo();
        info.baseURI = baseURI;
        info.stylesheetURI = expressionBaseURI;

        MappingIterator map = new MappingIterator(
                                    hrefSequence,
                                    this,
                                    context,
                                    info);

        Expression expression = argument[0];
        if (Cardinality.allowsMany(expression.getCardinality())) {
            return new DocumentOrderIterator(map, GlobalOrderComparer.getInstance());
            // this is to make sure we eliminate duplicates: two href's might be the same
        } else {
            return map;
        }
    }

    /**
    * Implement the MappingFunction interface: called from the MappingIterator
    */

    public Object map(Item item, XPathContext context, Object dinfo) throws XPathException {

        DocumentMappingInfo info = (DocumentMappingInfo)dinfo;
        String baseURI = info.baseURI;

        if (baseURI==null) {
            if (item instanceof NodeInfo) {
                baseURI = ((NodeInfo)item).getBaseURI();
            } else {
                baseURI = info.stylesheetURI;
            }
        }
        NodeInfo doc = makeDoc(item.getStringValue(), baseURI, context);
        return doc;
    }


    /**
    * Supporting routine to load one external document given a URI (href) and a baseURI
    */

    public static NodeInfo makeDoc(String href, String baseURL, XPathContext c) throws XPathException {

        // If the href contains a fragment identifier, strip it out now

        int hash = href.indexOf('#');

        String fragmentId = null;
        if (hash>=0) {
            if (hash==href.length()-1) {
                // # sign at end - just ignore it
                href = href.substring(0, hash);
            } else {
                fragmentId = href.substring(hash+1);
                href = href.substring(0, hash);
            }
        }


        // Resolve relative URI

        String documentKey;
        if (baseURL==null) {    // no base URI available
            try {
                // the href might be an absolute URL
                documentKey = (new URI(href)).toString();
            } catch (URISyntaxException err) {
                // it isn't; but the URI resolver might know how to cope
                documentKey = baseURL + '/' + href;
                baseURL = "";
            }
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
        if (doc != null) {
            return getFragment(doc, fragmentId, c);
        }

        try {
            // Get a Source from the URIResolver

            URIResolver r = controller.getURIResolver();
            Source source = r.resolve(href, baseURL);

            // if a user URI resolver returns null, try the standard one
            // (Note, the standard URI resolver never returns null)
            if (source==null) {
                r = controller.getStandardURIResolver();
                source = r.resolve(href, baseURL);
            }

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
            }
            controller.registerDocument(newdoc, documentKey);
            return getFragment(newdoc, fragmentId, c);

        } catch (TransformerException err) {
            DynamicError xerr = DynamicError.makeDynamicError(err);
            try {
                controller.recoverableError(xerr);
            } catch (XPathException err2) {
                throw new DynamicError(err);
            }
            return null;
        }
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
        if (!XMLChar.isValidNCName(fragmentId)) {
            DynamicError err = new DynamicError("Invalid fragment identifier in URI");
            err.setXPathContext(context);
            err.setErrorCode("XT1160");
            try {
                context.getController().recoverableError(err);
            } catch (DynamicError dynamicError) {
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
