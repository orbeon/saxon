package net.sf.saxon.event;

import net.sf.saxon.Configuration;
import net.sf.saxon.dom.DOMEmitter;
import net.sf.saxon.om.DocumentInfo;
import net.sf.saxon.om.NamePool;
import net.sf.saxon.om.NodeInfo;
import net.sf.saxon.tinytree.TinyBuilder;
import net.sf.saxon.tree.DocumentImpl;
import net.sf.saxon.tree.TreeBuilder;
import net.sf.saxon.xpath.DynamicError;
import net.sf.saxon.xpath.XPathException;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.stream.StreamResult;
import java.util.*;

/**
* Helper class to construct a serialization pipeline for a given result destination
* and a given set of output properties. The pipeline is represented by a Receiver object
* to which result tree events are sent
*/

public class ResultWrapper {

    // Class is never instantiated
    private ResultWrapper() {
    }

    /**
    * Get a Receiver that wraps a given Result object
    */

    public static Receiver getReceiver( Result result,
                                        PipelineConfiguration pipe,
                                        Properties props,
                                        HashMap characterMapIndex)
                                    throws XPathException {
        Configuration config = pipe.getConfiguration();
        NamePool namePool = config.getNamePool();
        if (result instanceof Emitter) {
            ((Emitter)result).setOutputProperties(props);
            return (Emitter)result;
        } else if (result instanceof Receiver) {
            return (Receiver)result;
        } else if (result instanceof SAXResult) {
            ContentHandlerProxy proxy = new ContentHandlerProxy();
            proxy.setUnderlyingContentHandler(((SAXResult)result).getHandler());
            proxy.setPipelineConfiguration(pipe);
            return proxy;
        } else if (result instanceof StreamResult) {

            // The "target" is the start of the output pipeline, the Receiver that
            // instructions will actually write to (except that other things like a
            // NamespaceReducer may get added in front of it). The "emitter" is the
            // last thing in the output pipeline, the Receiver that actually generates
            // characters or bytes that are written to the StreamResult.

            Receiver target;
            Emitter emitter;
            CharacterMapExpander characterMapExpander = null;
            String method = props.getProperty(OutputKeys.METHOD);

            String useMaps = props.getProperty(SaxonOutputKeys.USE_CHARACTER_MAPS);
            if (useMaps != null && characterMapIndex != null) {
                List characterMaps = new ArrayList(5);
                StringTokenizer st = new StringTokenizer(useMaps);
                while (st.hasMoreTokens()) {
                    String expandedName = st.nextToken();
                    int f = namePool.getFingerprintForExpandedName(expandedName);
                    HashMap map = (HashMap)characterMapIndex.get(new Integer(f));
                    if (map==null) {
                        throw new DynamicError("Character map '" + expandedName + "' has not been defined");
                    }
                    characterMaps.add(map);
                }
                if (characterMaps.size() > 0) {
                    characterMapExpander = new CharacterMapExpander();
                    characterMapExpander.setCharacterMaps(characterMaps);
                }
            }
            if (method==null) {
            	emitter = new UncommittedEmitter();
            	emitter.setPipelineConfiguration(pipe);
            	target = emitter;
                if (characterMapExpander != null) {
                    characterMapExpander.setUnderlyingReceiver(target);
                    target = characterMapExpander;
                }
            } else if ("html".equals(method)) {
                emitter = new HTMLEmitter();
                emitter.setPipelineConfiguration(pipe);
                target = emitter;
                if (!"no".equals(props.getProperty(OutputKeys.INDENT))) {
                    HTMLIndenter in = new HTMLIndenter();
                    in.setUnderlyingReceiver(target);
                    in.setPipelineConfiguration(pipe);
                    in.setOutputProperties(props);
                    target=in;
                }
                // TODO: should URI-escape URI-valued HTML attributes without
                // doing character mapping (test charmap009)
                if (characterMapExpander != null) {
                    characterMapExpander.setUnderlyingReceiver(target);
                    target = characterMapExpander;
                }
            } else if ("xml".equals(method)) {
                emitter = new XMLEmitter();
                emitter.setPipelineConfiguration(pipe);
                target = emitter;
                if ("yes".equals(props.getProperty(OutputKeys.INDENT))) {
                    XMLIndenter in = new XMLIndenter();
                    in.setUnderlyingReceiver(target);
                    in.setPipelineConfiguration(pipe);
                    in.setOutputProperties(props);
                    target=in;
                }
                if (characterMapExpander != null) {
                    characterMapExpander.setUnderlyingReceiver(target);
                    target = characterMapExpander;
                }
                String cdataElements = props.getProperty(OutputKeys.CDATA_SECTION_ELEMENTS);
                if (cdataElements!=null && cdataElements.length()>0) {
                    CDATAFilter filter = new CDATAFilter();
                    filter.setUnderlyingReceiver(target);
                    filter.setPipelineConfiguration(pipe);
                    filter.setOutputProperties(props);
                    target = filter;
                }
            } else if ("xhtml".equals(method)) {
                emitter = new XHTMLEmitter();
                emitter.setPipelineConfiguration(pipe);
                target = emitter;
                if ("yes".equals(props.getProperty(OutputKeys.INDENT))) {
                    HTMLIndenter in = new HTMLIndenter();
                    in.setUnderlyingReceiver(target);
                    in.setPipelineConfiguration(pipe);
                    in.setOutputProperties(props);
                    target=in;
                }
                if (characterMapExpander != null) {
                    characterMapExpander.setUnderlyingReceiver(target);
                    target = characterMapExpander;
                }
                String cdataElements = props.getProperty(OutputKeys.CDATA_SECTION_ELEMENTS);
                if (cdataElements!=null && cdataElements.length()>0) {
                    CDATAFilter filter = new CDATAFilter();
                    filter.setUnderlyingReceiver(target);
                    filter.setPipelineConfiguration(pipe);
                    filter.setOutputProperties(props);
                    target = filter;
                }
            } else if ("text".equals(method)) {
                emitter = new TEXTEmitter();
                emitter.setPipelineConfiguration(pipe);
                target = emitter;
                if (characterMapExpander != null) {
                    characterMapExpander.setUnderlyingReceiver(target);
                    characterMapExpander.setUseNullMarkers(false);
                    target = characterMapExpander;
                }
            } else {
                // TODO: externally supplied properties must be validated
                int brace = method.indexOf('}');
                String localName = method.substring(brace+1);
                int colon = localName.indexOf(':');
                localName = localName.substring(colon+1);
                emitter = Emitter.makeEmitter(localName);
                emitter.setPipelineConfiguration(pipe);
                target = emitter;
            }
            emitter.setOutputProperties(props);
            StreamResult sr = (StreamResult)result;
            emitter.setStreamResult(sr);
            return target;

        } else if (result instanceof DOMResult) {
            Node resultNode = ((DOMResult)result).getNode();
            if (resultNode!=null) {
                if (resultNode instanceof NodeInfo) {
                    // Writing to a SAXON tree is handled specially
                    if (resultNode instanceof DocumentInfo) {
                        DocumentInfo doc = (DocumentInfo)resultNode;
                        if (resultNode.getFirstChild() != null) {
                            throw new DynamicError("Target document must be empty");
                        } else {
                            Builder builder;
                            if (doc instanceof DocumentImpl) {
                                builder = new TreeBuilder();
                            } else {
                                builder = new TinyBuilder();
                            }
                            builder.setRootNode(doc);
                            builder.setSystemId(result.getSystemId());
                            builder.setPipelineConfiguration(pipe);
                            return builder;
                        }
                    } else {
                        throw new DynamicError("Cannot add to an existing Saxon document");
                    }
                } else {
                    // Non-Saxon DOM
                    DOMEmitter emitter = new DOMEmitter();
                    emitter.setSystemId(result.getSystemId());
                    emitter.setPipelineConfiguration(pipe);
                    emitter.setNode(resultNode);
                    return emitter;
                }
            } else {
                // no result node supplied; we must create our own
                TinyBuilder builder = new TinyBuilder();
                builder.setSystemId(result.getSystemId());
                builder.setPipelineConfiguration(pipe);
                builder.open();
                builder.startDocument(0);
                Document resultDoc = (Document)builder.getCurrentRoot();
                ((DOMResult)result).setNode(resultDoc);
                return builder;
            }
        } else {
            throw new IllegalArgumentException("Unknown type of result: " + result.getClass());
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