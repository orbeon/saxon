package org.orbeon.saxon.functions;

import org.orbeon.saxon.*;
import org.orbeon.saxon.event.Builder;
import org.orbeon.saxon.event.PipelineConfiguration;
import org.orbeon.saxon.event.Stripper;
import org.orbeon.saxon.expr.*;
import org.orbeon.saxon.om.*;
import org.orbeon.saxon.pattern.NodeKindTest;
import org.orbeon.saxon.trans.DynamicError;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.value.AnyURIValue;
import org.orbeon.saxon.value.ObjectValue;
import org.orbeon.saxon.value.Whitespace;
import org.xml.sax.XMLReader;

import javax.xml.transform.ErrorListener;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.stream.StreamSource;
import java.io.File;
import java.io.FilenameFilter;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * This class implements the default collection URI Resolver.
 * <p>
 * This supports two implementations of collections. If the URI supplied uses the "file:/" scheme, and the
 * file that is referenced is a directory, then the collection is the set of files in that directory. Query parameters
 * may be included in the URI:
 * <ul>
 * <li><p>recurse=yes|no controls whether the directory is scanned recursively; </p></li>
 * <li><p>strip-space=yes|no  determines whether whitespace text nodes are stripped from the selected documents; </p></li>
 * <li><p>validation=strict|lax|preserve|strip determines whether schema validation is applied;</p></li>
 * <li><p>select=pattern determines which files in the directory are selected.</p></li>
 * <li><p>on-error=fail|warn|ignore determines the action taken if processing of a file fails</p></li>
 * <li><p>parser=qualified.class.name selects the parser (XMLReader) to be used to read the files</p></li>
 * </ul>
 * <p>
 * Otherwise, the resolver attempts to dereference the URI to obtain a catalog file. This is an XML file
 * containing a list of documents, in the format: </p>
 * <pre>
 * &lt;collection>
 *   &lt;doc href="doc1.xml"/>
 *   &lt;doc href="doc2.xml"/>
 * &lt;/collection>
 * </pre>
 */

public class StandardCollectionURIResolver implements CollectionURIResolver {

    /**
     * Resolve a URI.
     *
     * @param href The relative URI of the collection. This corresponds to the
     *             argument supplied to the collection() function. If the collection() function
     *             was called with no arguments (to get the "default collection") this argument
     *             will be null.
     * @param base The base URI that should be used. This is the base URI of the
     *             static context in which the call to collection() was made, typically the URI
     *             of the stylesheet or query module
    * @return an Iterator over the documents in the collection. The items returned
    * by this iterator must be instances either of xs:anyURI, or of node() (specifically,
     * {@link org.orbeon.saxon.om.NodeInfo}.). If xs:anyURI values are returned, the corresponding
     * document will be retrieved as if by a call to the doc() function: this means that
     * the system first checks to see if the document is already loaded, and if not, calls
     * the registered URIResolver to dereference the URI. This is the recommended approach
     * to ensure that the resulting collection is stable: however, it has the consequence
     * that the documents will by default remain in memory for the duration of the query
     * or transformation.
     */

    public SequenceIterator resolve(String href, String base, XPathContext context) throws XPathException {

        if (href == null) {
            // default collection. This returns empty, we previously threw an error.
            return null;
        }

        if (base == null) {
            base = JavaPlatform.tryToExpand(base);
            if (base == null) {
                DynamicError err = new DynamicError(
                    "Cannot resolve relative URI: no base URI available");
                err.setErrorCode("FODC0002");
                err.setXPathContext(context);
                throw err;
            }
        }

        URI resolvedURI;
        URIQueryParameters params = null;
        try {
            URI relative = new URI(href);
            String query = relative.getQuery();
            if (query != null) {
                params = new URIQueryParameters(query, context.getConfiguration());
                int q = href.indexOf('?');
                href = href.substring(0, q);
            }
            resolvedURI = new URI(base).resolve(href);
        } catch (URISyntaxException e) {
            DynamicError err = new DynamicError(
                    "Invalid URI " + Err.wrap(href) + " passed to collection() function");
            err.setErrorCode("FODC0002");
            err.setXPathContext(context);
            throw err;
        }

        if ("file".equals(resolvedURI.getScheme())) {
            File file = new File(resolvedURI);
            if (!file.exists()) {
                DynamicError err = new DynamicError(
                        "The file or directory " + resolvedURI + " does not exist");
                err.setErrorCode("FODC0004");
                err.setXPathContext(context);
                throw err;
            }
            if (file.isDirectory()) {
                return directoryContents(file, params, context);
            }
        }
        return catalogContents(resolvedURI, params, context);

    }

    private SequenceIterator directoryContents(File directory, URIQueryParameters params, XPathContext context) {

        FilenameFilter filter = null;

        if (params != null) {
            FilenameFilter f = params.getFilenameFilter();
            if (f != null) {
                filter = f;
            }
        }

        File[] files;
        if (filter == null) {
            files = directory.listFiles();
        } else {
            files = directory.listFiles(filter);
        }

        ObjectValue[] fileValues = new ObjectValue[files.length];
        for (int f=0; f<files.length; f++) {
            fileValues[f] = new ObjectValue(files[f]);
        }

        // If the URI requested suppression of errors, or that errors should be treated
        // as warnings, we set up a special ErrorListener to achieve this

        int onError = URIQueryParameters.ON_ERROR_FAIL;
        if (params != null && params.getOnError() != null) {
            onError = params.getOnError().intValue();
        }
        final Controller controller = context.getController();
        final PipelineConfiguration oldPipe = controller.makePipelineConfiguration();
        final PipelineConfiguration newPipe = new PipelineConfiguration(oldPipe);
        final ErrorListener oldErrorListener = controller.getErrorListener();
        if (onError == URIQueryParameters.ON_ERROR_IGNORE) {
            newPipe.setErrorListener(new ErrorListener() {
                public void warning(TransformerException exception) {}
                public void error(TransformerException exception) {}
                public void fatalError(TransformerException exception) {}
            });
        } else if (onError == URIQueryParameters.ON_ERROR_WARNING) {
            newPipe.setErrorListener(new ErrorListener() {
                public void warning(TransformerException exception) throws TransformerException {
                    oldErrorListener.warning(exception);
                }
                public void error(TransformerException exception) throws TransformerException {
                    oldErrorListener.warning(exception);
                    DynamicError supp = new DynamicError("The document will be excluded from the collection");
                    supp.setLocator(exception.getLocator());
                    oldErrorListener.warning(supp);
                }
                public void fatalError(TransformerException exception) throws TransformerException {
                    error(exception);
                }
            });
        }
        FileExpander expander = new FileExpander(params, newPipe);
        SequenceIterator base = new ArrayIterator(fileValues);
        return new MappingIterator(base, expander);
    }

    /**
     * Return a collection defined as a list of URIs in a catalog file
     * @param catalogFile the URI of the catalog file
     * @param params
     * @param context
     * @return
     * @throws XPathException
     */

    private SequenceIterator catalogContents(URI catalogFile, URIQueryParameters params, final XPathContext context)
    throws XPathException {

        boolean stable = true;
        NamePool pool = context.getController().getNamePool();

        DocumentInfo catalog =
                (DocumentInfo) Document.makeDoc(catalogFile.toString(), null, context, null);
        if (catalog==null) {
            // we failed to read the catalogue
            DynamicError err = new DynamicError("Failed to load collection catalogue " + catalogFile);
            err.setErrorCode("FODC0004");
            err.setXPathContext(context);
            throw err;
        }

        // Now return an iterator over the documents that it refers to

        SequenceIterator iter =
                catalog.iterateAxis(Axis.CHILD, NodeKindTest.ELEMENT);
        NodeInfo top;
        while (true) {
            top = (NodeInfo)iter.next();
            if (top == null) break;
            if (!("collection".equals(top.getLocalPart()) &&
                    top.getURI().equals("") )) {
                DynamicError err = new DynamicError("collection catalogue must contain top-level element <collection>");
                err.setErrorCode("FODC0004");
                err.setXPathContext(context);
                throw err;
            }
            break;
        }

        String stableAtt = top.getAttributeValue(pool.allocate("", "", "stable"));
        if (stableAtt != null) {
            if ("true".equals(stableAtt)) {
                stable = true;
            } else if ("false".equals(stableAtt)) {
                stable = false;
            } else {
                DynamicError err = new DynamicError("The 'stable' attribute of element <collection> must be true or false");
                err.setErrorCode("FODC0004");
                err.setXPathContext(context);
                throw err;
            }
        }

        final boolean finalStable = stable;
        SequenceIterator documents =
                top.iterateAxis(Axis.CHILD, NodeKindTest.ELEMENT);

        ItemMappingFunction catalogueMapper = new ItemMappingFunction() {
            public Item map(Item item) throws XPathException {
                NodeInfo element = (NodeInfo)item;
                if (!("doc".equals(element.getLocalPart()) &&
                        element.getURI().equals("") )) {
                    DynamicError err = new DynamicError(
                            "children of <collection> element must be <doc> elements");
                    err.setErrorCode("FODC0004");
                    err.setXPathContext(context);
                    throw err;
                }
                String href = Navigator.getAttributeValue(element, "", "href");
                if (href==null) {
                    DynamicError err = new DynamicError(
                            "\"<doc> element in catalogue has no @href attribute\"");
                    err.setErrorCode("FODC0004");
                    err.setXPathContext(context);
                    throw err;
                }
                String uri;
                try {
                    uri = new URI(element.getBaseURI()).resolve(href).toString();
                } catch (URISyntaxException e) {
                    DynamicError err = new DynamicError("Invalid base URI or href URI in collection catalog: ("
                        + element.getBaseURI() + ", " + href + ")");
                    err.setErrorCode("FODC0004");
                    err.setXPathContext(context);
                    throw err;
                }
                if (finalStable) {
                    return new AnyURIValue(uri);
                } else {
                    // stability not required, bypass the document pool and URI resolver
                    StreamSource source = new StreamSource(uri);
                    PipelineConfiguration pipe = context.getController().makePipelineConfiguration();
                    NodeInfo contextNode = Builder.build(source, null, pipe);
                    return contextNode.getDocumentRoot();
                }
//                NodeInfo target = Document.makeDoc(href, element.getBaseURI(), context, null);
//                return target;
            }
        };

        return new ItemMappingIterator(documents, catalogueMapper);
    }

    /**
     * Mapping function to process the files in a directory. This maps a sequence of external
     * objects representing files to a sequence of DocumentInfo nodes representing the parsed
     * contents of those files.
     */

    private static class FileExpander implements MappingFunction {

        private URIQueryParameters params;
        boolean recurse = false;
        int strip = Whitespace.UNSPECIFIED;
        int validation = Validation.STRIP;
        XMLReader parser = null;
        int onError = URIQueryParameters.ON_ERROR_FAIL;
        FilenameFilter filter = null;
        PipelineConfiguration pipe;

        public FileExpander(URIQueryParameters params, PipelineConfiguration pipe) {
            this.params = params;
            this.pipe = pipe;
            if (params != null) {
                FilenameFilter f = params.getFilenameFilter();
                if (f != null) {
                    filter = f;
                }
                Boolean r = params.getRecurse();
                if (r != null) {
                    recurse = r.booleanValue();
                }
                Integer v = params.getValidationMode();
                if (v != null) {
                    validation = v.intValue();
                }
                strip = params.getStripSpace();
                Integer e = params.getOnError();
                if (e != null) {
                    onError = e.intValue();
                }
                XMLReader p = params.getXMLReader();
                if (p != null) {
                    parser = p;
                }
            }

        }

        /**
         * Map one item to a sequence.
         *
         * @param item    The item to be mapped.
         *                If context is supplied, this must be the same as context.currentItem().
         * @return either (a) a SequenceIterator over the sequence of items that the supplied input
         *         item maps to, or (b) an Item if it maps to a single item, or (c) null if it maps to an empty
         *         sequence.
         */

        public Object map(Item item) throws XPathException {
            File file = (File)((ObjectValue)item).getObject();
            if (file.isDirectory()) {
                if (recurse) {
                   File[] files;
                    if (filter == null) {
                        files = file.listFiles();
                    } else {
                        files = file.listFiles(filter);
                    }

                    ObjectValue[] fileValues = new ObjectValue[files.length];
                    for (int f=0; f<files.length; f++) {
                        fileValues[f] = new ObjectValue(files[f]);
                    }

                    FileExpander expander = new FileExpander(params, pipe);
                    return new MappingIterator(new ArrayIterator(fileValues), expander);
                } else {
                    return null;
                }
            } else {
                try {
                    Source source = new StreamSource(file.toURI().toString());
                    if (validation != Validation.STRIP && validation != Validation.PRESERVE) {
                        source = AugmentedSource.makeAugmentedSource(source);
                        ((AugmentedSource)source).setSchemaValidationMode(validation);
                    }
                    if (parser != null) {
                        source = AugmentedSource.makeAugmentedSource(source);
                        ((AugmentedSource)source).setXMLReader(parser);
                    }

                    Stripper stripper = null;
                    if (params != null) {
                        int stripSpace = params.getStripSpace();
                        switch (strip) {
                            case Whitespace.ALL: {
                                stripper = AllElementStripper.getInstance();
                                stripper.setStripAll();
                                source = AugmentedSource.makeAugmentedSource(source);
                                ((AugmentedSource)source).addFilter(stripper);
                                break;
                            }
                            case Whitespace.IGNORABLE:
                            case Whitespace.NONE:
                                source = AugmentedSource.makeAugmentedSource(source);
                                ((AugmentedSource)source).setStripSpace(stripSpace);
                        }
                    }
                    NodeInfo contextNode = Builder.build(source, null, pipe);
                    return contextNode.getDocumentRoot();
                } catch (XPathException err) {
                    if (onError == URIQueryParameters.ON_ERROR_IGNORE) {
                        return null;
                    } else if (onError == URIQueryParameters.ON_ERROR_WARNING) {
                        try {
                            if (!err.hasBeenReported()) {
                                pipe.getErrorListener().warning(err);
                                DynamicError supp = new DynamicError("The document will be excluded from the collection");
                                supp.setLocator(err.getLocator());
                                pipe.getErrorListener().warning(supp);
                            }
                        } catch (TransformerException err2) {
                            //
                        }
                        return null;
                    } else {
                        throw err;
                    }
                }
            }
        }
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

