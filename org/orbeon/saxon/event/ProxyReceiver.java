package net.sf.saxon.event;
import net.sf.saxon.Configuration;
import net.sf.saxon.om.NamePool;
import net.sf.saxon.trans.XPathException;

    /**
    * A ProxyReceiver is an Receiver that filters data before passing it to another
    * underlying Receiver.
    */

public abstract class ProxyReceiver implements Receiver
{
    protected Receiver baseReceiver;
    protected PipelineConfiguration pipelineConfig;
    protected String systemId;
    //protected LocationProvider locationProvider;

    public PipelineConfiguration getPipelineConfiguration() {
        return pipelineConfig;
    }

    public void setSystemId(String systemId) {
        if (systemId != this.systemId) {
            this.systemId = systemId;
            if (baseReceiver!=null) {
                baseReceiver.setSystemId(systemId);
            }
        }
    }

    public String getSystemId() {
        return systemId;
    }

    /**
     * Set the underlying receiver. This call is mandatory before using the Receiver.
     */

    public void setUnderlyingReceiver(Receiver receiver) {
        if (receiver != baseReceiver) {
            baseReceiver = receiver;
            if (pipelineConfig != null) {
                baseReceiver.setPipelineConfiguration(pipelineConfig);
            }
        }
    }

    /**
     * Get the underlying Receiver (that is, the next one in the pipeline)
     */

    public Receiver getUnderlyingReceiver() {
        return baseReceiver;
    }




	public void setPipelineConfiguration(PipelineConfiguration config) {
        if (this.pipelineConfig != config) {
            this.pipelineConfig = config;
            if (baseReceiver!=null) {
                baseReceiver.setPipelineConfiguration(config);
            }
        }
	}

    public Configuration getConfiguration() {
        return pipelineConfig.getConfiguration();
    }

    /**
     * Get the namepool for this configuration
     */

    public NamePool getNamePool() {
        return getConfiguration().getNamePool();
    }

    /**
    * Start of event stream
    */

    public void open() throws XPathException {
        if (baseReceiver==null) {
            throw new IllegalStateException("ProxyReceiver.open(): no underlying emitter provided");
        }
        baseReceiver.open();
    }

    /**
    * End of document
    */

    public void close() throws XPathException {
        if (baseReceiver!=null) {
            baseReceiver.close();
        }
    }

    /**
     * Start of a document node.
    */

    public void startDocument(int properties) throws XPathException {
        if (baseReceiver!=null) {
            baseReceiver.startDocument(properties);
        }
    }

    /**
     * Notify the end of a document node
     */

    public void endDocument() throws XPathException {
        if (baseReceiver!=null) {
            baseReceiver.endDocument();
        }
    }

    /**
    * Notify the start of an element
    * @param nameCode integer code identifying the name of the element within the name pool.
    * @param typeCode integer code identifying the element's type within the name pool.
    * @param properties properties of the element node
    */

    public void startElement(int nameCode, int typeCode, int locationId, int properties) throws XPathException {
        if (baseReceiver!=null) {
            baseReceiver.startElement(nameCode, typeCode, locationId, properties);
        }
    }

    /**
    * Notify a namespace. Namespaces are notified <b>after</b> the startElement event, and before
    * any children for the element. The namespaces that are reported are only required
    * to include those that are different from the parent element; however, duplicates may be reported.
    * A namespace must not conflict with any namespaces already used for element or attribute names.
    * @param namespaceCode an integer: the top half is a prefix code, the bottom half a URI code.
    * These may be translated into an actual prefix and URI using the name pool. A prefix code of
    * zero represents the empty prefix (that is, the default namespace). A URI code of zero represents
    * a URI of "", that is, a namespace undeclaration.
    * @throws IllegalStateException: attempt to output a namespace when there is no open element
    * start tag
    */

    public void namespace(int namespaceCode, int properties) throws XPathException {
        if (baseReceiver!=null) {
            baseReceiver.namespace(namespaceCode, properties);
        }
    }

    /**
    * Notify an attribute. Attributes are notified after the startElement event, and before any
    * children. Namespaces and attributes may be intermingled.
    * @param nameCode The name of the attribute, as held in the name pool
    * @param typeCode The type of the attribute, as held in the name pool
    * @param properties Bit significant value. The following bits are defined:
    *        <dd>DISABLE_ESCAPING</dd>    <dt>Disable escaping for this attribute</dt>
    *        <dd>NO_SPECIAL_CHARACTERS</dd>      <dt>Attribute value contains no special characters</dt>
    * @throws IllegalStateException: attempt to output an attribute when there is no open element
    * start tag
    */

    public void attribute(int nameCode, int typeCode, CharSequence value, int locationId, int properties)
    throws XPathException {
        if (baseReceiver!=null) {
            baseReceiver.attribute(nameCode, typeCode, value, locationId, properties);
        }
    }

    /**
    * Notify the start of the content, that is, the completion of all attributes and namespaces.
    * Note that the initial receiver of output from XSLT instructions will not receive this event,
    * it has to detect it itself. Note that this event is reported for every element even if it has
    * no attributes, no namespaces, and no content.
    */


    public void startContent() throws XPathException {
        if (baseReceiver!=null) {
            baseReceiver.startContent();
        }
    }

    /**
    * End of element
    */

    public void endElement() throws XPathException {
        if (baseReceiver!=null) {
            baseReceiver.endElement();
        }
    }

    /**
    * Character data
    */

    public void characters(CharSequence chars, int locationId, int properties) throws XPathException {
        if (baseReceiver!=null) {
            baseReceiver.characters(chars, locationId, properties);
        }
    }


    /**
    * Processing Instruction
    */

    public void processingInstruction(String target, CharSequence data, int locationId, int properties) throws XPathException {
        if (baseReceiver!=null) {
            baseReceiver.processingInstruction(target, data, locationId, properties);
        }
    }

    /**
    * Output a comment
    */

    public void comment (CharSequence chars, int locationId, int properties) throws XPathException {
        if (baseReceiver!=null) {
            baseReceiver.comment(chars, locationId, properties);
        }
    }


    /**
    * Set the URI for an unparsed entity in the document.
    */

    public void setUnparsedEntity(String name, String uri, String publicId) throws XPathException {
        if (baseReceiver!=null) {
            baseReceiver.setUnparsedEntity(name, uri, publicId);
        }
	}

    /**
     * Get the Document Locator
     */

    public LocationProvider getDocumentLocator() {
        return pipelineConfig.getLocationProvider();
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
// The Initial Developer of the Original Code is Michael H. Kay
//
// Portions created by (your name) are Copyright (C) (your legal entity). All Rights Reserved.
//
// Contributor(s): none.
//
