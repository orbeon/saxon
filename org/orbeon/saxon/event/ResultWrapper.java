package net.sf.saxon.event;

import net.sf.saxon.Controller;
import net.sf.saxon.om.ExternalObjectModel;
import net.sf.saxon.trans.DynamicError;
import net.sf.saxon.trans.XPathException;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.stream.StreamResult;
import java.util.List;
import java.util.Properties;

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

    public static Receiver getReceiver(Result result,
                                       PipelineConfiguration pipe,
                                       Properties props)
                                    throws XPathException {
        if (result instanceof Emitter) {
            ((Emitter)result).setOutputProperties(props);
            return (Emitter)result;
        } else if (result instanceof Receiver) {
            Receiver builder = (Receiver)result;
            builder.setSystemId(result.getSystemId());
            builder.setPipelineConfiguration(pipe);
            builder.open();
            builder.startDocument(0);
            return builder;
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
            String useMaps = props.getProperty(SaxonOutputKeys.USE_CHARACTER_MAPS);
            if (useMaps != null) {
                Controller controller = pipe.getController();
                if (controller == null) {
                    throw new DynamicError("Cannot use character maps in an environment with no Controller");
                }
                characterMapExpander = controller.makeCharacterMapExpander(useMaps);
            }

            String method = props.getProperty(OutputKeys.METHOD);
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
                Receiver userReceiver = Emitter.makeEmitter(localName, pipe.getController());
                userReceiver.setPipelineConfiguration(pipe);
                target = userReceiver;
                if (userReceiver instanceof Emitter) {
                    emitter = (Emitter)userReceiver;
                } else {
                    return userReceiver;
                }
            }
            emitter.setOutputProperties(props);
            StreamResult sr = (StreamResult)result;
            emitter.setStreamResult(sr);
            return target;

        } else {
            // try to find an external object model that knows this kind of Result
            List externalObjectModels = pipe.getConfiguration().getExternalObjectModels();
            for (int m=0; m<externalObjectModels.size(); m++) {
                ExternalObjectModel model = (ExternalObjectModel)externalObjectModels.get(m);
                Receiver builder = model.getDocumentBuilder(result);
                if (builder != null) {
                    builder.setSystemId(result.getSystemId());
                    builder.setPipelineConfiguration(pipe);
                    return builder;
                }
            }

        }

//        // Redesign creating a Saxon tree as the result
//        if (result instanceof DOMResult) {
//
//                    // we've been given an empty wrapper for a Saxon tree
//                    TinyBuilder builder = new TinyBuilder();
//                    builder.setSystemId(result.getSystemId());
//                    builder.setPipelineConfiguration(pipe);
//                    builder.open();
//                    builder.startDocument(0);
//                    DocumentInfo resultDoc = (DocumentInfo)builder.getCurrentRoot();
//
//                    return builder;
//                }
//                    } else {
//                        throw new DynamicError("Cannot add to an existing Saxon document");
//                    }
//                }
//            } else {
//                // no result node supplied; we must create our own
//                TinyBuilder builder = new TinyBuilder();
//                builder.setSystemId(result.getSystemId());
//                builder.setPipelineConfiguration(pipe);
//                builder.open();
//                builder.startDocument(0);
//                DocumentInfo resultDoc = (DocumentInfo)builder.getCurrentRoot();
//                ((DOMResult)result).setNode(NodeOverNodeInfo.wrap(resultDoc));
//                return builder;
//            }
//        } else {
//            throw new IllegalArgumentException("Unknown type of result: " + result.getClass());
//        }
        throw new IllegalArgumentException("Unknown type of result: " + result.getClass());
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