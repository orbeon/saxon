package org.orbeon.saxon.event;

import org.orbeon.saxon.om.*;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.value.AtomicValue;
import org.orbeon.saxon.type.Type;

/**
 * A TreeReceiver acts as a bridge between a SequenceReceiver, which can receive
 * events for constructing any kind of sequence, and an ordinary Receiver, which
 * only handles events relating to the building of trees. To do this, it has to
 * process any items added to the sequence using the append() interface; all other
 * events are passed through unchanged.
 *
 * <p>If atomic items are appended to the sequence, then adjacent atomic items are
 * turned in to a text node by converting them to strings and adding a single space
 * as a separator.</p>
 *
 * <p>If a document node is appended to the sequence, then the document node is ignored
 * and its children are appended to the sequence.</p>
 *
 * <p>If any other node is appended to the sequence, then it is pushed to the result
 * as a sequence of Receiver events, which may involve walking recursively through the
 * contents of a tree.</p>
 */

public class TreeReceiver extends SequenceReceiver {
    private Receiver nextReceiver;
    private int level = 0;
    private boolean[] isDocumentLevel = new boolean[20];
        // The sequence of events can include startElement/endElement pairs or startDocument/endDocument
        // pairs at any level. A startDocument/endDocument pair is essentially ignored except at the
        // outermost level, except that a namespace or attribute node cannot be sent when we're at a
        // document level. See for example schema90963-err.xsl
    private boolean inStartTag = false;

    /**
     * Create a TreeReceiver
     * @param nextInChain the receiver to which events will be directed, after
     * expanding append events into more primitive tree-based events
     */

    public TreeReceiver(Receiver nextInChain) {
        nextReceiver = nextInChain;
        previousAtomic = false;
        setPipelineConfiguration(nextInChain.getPipelineConfiguration());
    }

    public void setSystemId(String systemId) {
        if (systemId != null && !systemId.equals(this.systemId)) {
            this.systemId = systemId;
            if (nextReceiver != null) {
                nextReceiver.setSystemId(systemId);
            }
        }
    }

    public void setPipelineConfiguration(PipelineConfiguration pipe) {
        if (pipelineConfiguration != pipe) {
            pipelineConfiguration = pipe;
            if (nextReceiver != null) {
                nextReceiver.setPipelineConfiguration(pipe);
            }
        }
    }

    /**
     * Get the underlying Receiver (that is, the next one in the pipeline)
     * @return the underlying Receiver
     */

    public Receiver getUnderlyingReceiver() {
        return nextReceiver;
    }

    /**
     * Start of event sequence
     */

    public void open() throws XPathException {
        if (nextReceiver == null) {
            throw new IllegalStateException("TreeReceiver.open(): no underlying receiver provided");
        }
        nextReceiver.open();
        previousAtomic = false;
    }

    /**
     * End of event sequence
     */

    public void close() throws XPathException {
        if (nextReceiver != null) {
            nextReceiver.close();
        }
        previousAtomic = false;
    }

    /**
     * Start of a document node.
    */

    public void startDocument(int properties) throws XPathException {
        if (level == 0) {
            nextReceiver.startDocument(properties);
        }
        if (isDocumentLevel.length - 1 < level) {
            boolean[] d2 = new boolean[level*2];
            System.arraycopy(isDocumentLevel, 0, d2, 0, level);
            isDocumentLevel = d2;
        }
        isDocumentLevel[level++] = true;
    }

    /**
     * Notify the end of a document node
     */

    public void endDocument() throws XPathException {
        level--;
        if (level == 0) {
            nextReceiver.endDocument();
        }
    }

    /**
     * Notify the start of an element
     * @param nameCode integer code identifying the name of the element within the name pool.
     * @param typeCode integer code identifying the element's type within the name pool.
     * @param properties bit-significant properties of the element node
     */

    public void startElement(int nameCode, int typeCode, int locationId, int properties) throws XPathException {
        if (inStartTag) {
            startContent();
        }
        inStartTag = true;
        nextReceiver.startElement(nameCode, typeCode, locationId, properties);
        previousAtomic = false;
        if (isDocumentLevel.length - 1 < level) {
            boolean[] d2 = new boolean[level*2];
            System.arraycopy(isDocumentLevel, 0, d2, 0, level);
            isDocumentLevel = d2;
        }
        isDocumentLevel[level++] = false;
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
        boolean documentLevel = level==0 || isDocumentLevel[level-1];
        if (documentLevel || !inStartTag) {
            throw NoOpenStartTagException.makeNoOpenStartTagException(
                    Type.NAMESPACE, getNamePool().getPrefixFromNamespaceCode(namespaceCode),
                    getPipelineConfiguration().getHostLanguage(),
                    documentLevel, getPipelineConfiguration().isSerializing());
        }
        nextReceiver.namespace(namespaceCode, properties);
        previousAtomic = false;
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
        boolean documentLevel = level==0 || isDocumentLevel[level-1];
        if (documentLevel || !inStartTag) {
            throw NoOpenStartTagException.makeNoOpenStartTagException(
                    Type.ATTRIBUTE, getNamePool().getDisplayName(nameCode),
                    getPipelineConfiguration().getHostLanguage(),
                    documentLevel, getPipelineConfiguration().isSerializing());
        }
        nextReceiver.attribute(nameCode, typeCode, value, locationId, properties);
        previousAtomic = false;
    }

    /**
     * Notify the start of the content, that is, the completion of all attributes and namespaces.
     * Note that the initial receiver of output from XSLT instructions will not receive this event,
     * it has to detect it itself. Note that this event is reported for every element even if it has
     * no attributes, no namespaces, and no content.
     */


    public void startContent() throws XPathException {
        inStartTag = false;
        nextReceiver.startContent();
        previousAtomic = false;
    }

    /**
     * End of element
     */

    public void endElement() throws XPathException {
        if (inStartTag) {
            startContent();
        }
        nextReceiver.endElement();
        previousAtomic = false;
        level--;
    }

    /**
     * Character data
     */

    public void characters(CharSequence chars, int locationId, int properties) throws XPathException {
        if (chars.length() > 0) {
            if (inStartTag) {
                startContent();
            }
            nextReceiver.characters(chars, locationId, properties);
        }
        previousAtomic = false;
    }


    /**
     * Processing Instruction
     */

    public void processingInstruction(String target, CharSequence data, int locationId, int properties) throws XPathException {
        if (inStartTag) {
            startContent();
        }
        nextReceiver.processingInstruction(target, data, locationId, properties);
        previousAtomic = false;
    }

    /**
     * Output a comment
     */

    public void comment(CharSequence chars, int locationId, int properties) throws XPathException {
        if (inStartTag) {
            startContent();
        }
        nextReceiver.comment(chars, locationId, properties);
        previousAtomic = false;
    }


    /**
     * Set the URI for an unparsed entity in the document.
     */

    public void setUnparsedEntity(String name, String uri, String publicId) throws XPathException {
        nextReceiver.setUnparsedEntity(name, uri, publicId);
    }

    /**
     * Append an arbitrary item (node or atomic value) to the output
     */

    public void append(Item item, int locationId, int copyNamespaces) throws XPathException {
        if (item instanceof AtomicValue) {
            if (previousAtomic) {
                characters(" ", locationId, 0);
            }
            characters(item.getStringValueCS(), locationId, 0);
            previousAtomic = true;
        } else if (((NodeInfo)item).getNodeKind() == Type.DOCUMENT) {
            startDocument(0); // needed to ensure that illegal namespaces or attributes in the content are caught
            SequenceIterator iter = ((NodeInfo)item).iterateAxis(Axis.CHILD);
            while (true) {
                Item it = iter.next();
                if (it == null) break;
                append(it, locationId, copyNamespaces);
            }
            previousAtomic = false;
            endDocument();
        } else {
            ((NodeInfo)item).copy(this, copyNamespaces, true, locationId);
            previousAtomic = false;
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
// The Initial Developer of the Original Code is Michael H. Kay
//
// Portions created by (your name) are Copyright (C) (your legal entity). All Rights Reserved.
//
// Contributor(s): none.
//
