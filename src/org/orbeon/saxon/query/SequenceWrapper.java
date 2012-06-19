package org.orbeon.saxon.query;

import org.orbeon.saxon.event.Receiver;
import org.orbeon.saxon.event.SequenceReceiver;
import org.orbeon.saxon.om.*;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.type.AtomicType;
import org.orbeon.saxon.value.AtomicValue;

/**
 * This class can be used in a push pipeline: it accepts any sequence as input, and generates
 * a document in which the items of the sequence are wrapped by elements containing information about
 * the types of the items in the input sequence.
 */

public class SequenceWrapper extends SequenceReceiver {

    public static final String RESULT_NS = QueryResult.RESULT_NS;

    private Receiver out;
    private int depth = 0;

    //@SuppressWarnings({"FieldCanBeLocal"})
    private int resultSequence;
    private int resultDocument;
    private int resultElement;
    private int resultAttribute;
    private int resultText;
    private int resultComment;
    private int resultPI;
    private int resultNamespace;
    private int resultAtomicValue;
    private int xsiType;

    /**
     * Create a sequence wrapper. This creates an XML representation of the items sent to destination
     * in which the types of all items are made explicit
     * @param destination the sequence being wrapped
     */

    public SequenceWrapper(Receiver destination) {
        out = destination;
        // out = new TracingFilter(out);
        setPipelineConfiguration(destination.getPipelineConfiguration());
    }

    public void open() throws XPathException {

        NamePool pool = getNamePool();

        resultSequence = pool.allocate("result", RESULT_NS, "sequence");
        resultDocument = pool.allocate("result", RESULT_NS, "document");
        resultElement = pool.allocate("result", RESULT_NS, "element");
        resultAttribute = pool.allocate("result", RESULT_NS, "attribute");
        resultText = pool.allocate("result", RESULT_NS, "text");
        resultComment = pool.allocate("result", RESULT_NS, "comment");
        resultPI = pool.allocate("result", RESULT_NS, "processing-instruction");
        resultNamespace = pool.allocate("result", RESULT_NS, "namespace");
        resultAtomicValue = pool.allocate("result", RESULT_NS, "atomic-value");
        xsiType = pool.allocate("xsi", NamespaceConstant.SCHEMA_INSTANCE, "type");

        out.open();
        out.startDocument(0);

        out.startElement(resultSequence, StandardNames.XS_UNTYPED, 0, 0);
        out.namespace(pool.allocateNamespaceCode("result", RESULT_NS), 0);
        out.namespace(pool.allocateNamespaceCode("xs", NamespaceConstant.SCHEMA), 0);
        out.namespace(pool.allocateNamespaceCode("xsi", NamespaceConstant.SCHEMA_INSTANCE), 0);
        out.startContent();

    }

    /**
     * Start of a document node.
     */

    public void startDocument(int properties) throws XPathException {
        out.startElement(resultDocument, StandardNames.XS_UNTYPED, 0, 0);
        out.startContent();
        depth++;
    }

    /**
     * Notify the end of a document node
     */

    public void endDocument() throws XPathException {
        out.endElement();
        depth--;
    }

    /**
     * Notify the start of an element
     *
     * @param nameCode   integer code identifying the name of the element within the name pool.
     * @param typeCode   integer code identifying the element's type within the name pool.
     * @param properties properties of the element node
     */

    public void startElement(int nameCode, int typeCode, int locationId, int properties) throws XPathException {
        if (depth++ == 0) {
            out.startElement(resultElement, StandardNames.XS_UNTYPED, 0, 0);
            out.startContent();
        }
        out.startElement(nameCode, typeCode, locationId, properties);
    }

    /**
     * End of element
     */

    public void endElement() throws XPathException {
        out.endElement();
        if (--depth == 0) {
            out.endElement();   // close the wrapping element
        }
    }

    /**
     * Notify an attribute. Attributes are notified after the startElement event, and before any
     * children. Namespaces and attributes may be intermingled.
     *
     * @param nameCode   The name of the attribute, as held in the name pool
     * @param typeCode   The type of the attribute, as held in the name pool
     * @param properties Bit significant value. The following bits are defined:
     *                   <dd>DISABLE_ESCAPING</dd>    <dt>Disable escaping for this attribute</dt>
     *                   <dd>NO_SPECIAL_CHARACTERS</dd>      <dt>Attribute value contains no special characters</dt>
     * @throws IllegalStateException: attempt to output an attribute when there is no open element
     *                                start tag
     */

    public void attribute(int nameCode, int typeCode, CharSequence value, int locationId, int properties) throws XPathException {
        if (depth==0) {
            out.startElement(resultAttribute, StandardNames.XS_UNTYPED, 0, 0);
            if ((nameCode &~ NamePool.FP_MASK) != 0) {
                out.namespace(getNamePool().allocateNamespaceCode(nameCode), 0);
            }
            out.attribute(nameCode, typeCode, value, locationId, properties);
            out.startContent();
            out.endElement();
        } else {
            out.attribute(nameCode, typeCode, value, locationId, properties);
        }
    }

    /**
     * Notify a namespace. Namespaces are notified <b>after</b> the startElement event, and before
     * any children for the element. The namespaces that are reported are only required
     * to include those that are different from the parent element; however, duplicates may be reported.
     * A namespace must not conflict with any namespaces already used for element or attribute names.
     *
     * @param namespaceCode an integer: the top half is a prefix code, the bottom half a URI code.
     *                      These may be translated into an actual prefix and URI using the name pool. A prefix code of
     *                      zero represents the empty prefix (that is, the default namespace). A URI code of zero represents
     *                      a URI of "", that is, a namespace undeclaration.
     * @throws IllegalStateException: attempt to output a namespace when there is no open element
     *                                start tag
     */

    public void namespace(int namespaceCode, int properties) throws XPathException {
        if (depth == 0) {
            out.startElement(resultNamespace, StandardNames.XS_UNTYPED, 0, 0);
            out.namespace(namespaceCode, properties);
            out.startContent();
            out.endElement();
        } else {
            out.namespace(namespaceCode, properties);
        }
    }

    /**
     * Character data
     */

    public void characters(CharSequence chars, int locationId, int properties) throws XPathException {
        if (depth==0) {
            out.startElement(resultText, StandardNames.XS_UNTYPED, 0, 0);
            out.startContent();
            out.characters(chars, locationId, properties);
            out.endElement();
        } else {
            out.characters(chars, locationId, properties);
        }
    }

    /**
     * Output a comment
     */

    public void comment(CharSequence chars, int locationId, int properties) throws XPathException {
        if (depth==0) {
            out.startElement(resultComment, StandardNames.XS_UNTYPED, 0, 0);
            out.startContent();
            out.comment(chars, locationId, properties);
            out.endElement();
        } else {
            out.comment(chars, locationId, properties);
        }
    }

    /**
     * Processing Instruction
     */

    public void processingInstruction(String target, CharSequence data, int locationId, int properties) throws XPathException {
        if (depth==0) {
            out.startElement(resultPI, StandardNames.XS_UNTYPED, 0, 0);
            out.startContent();
            out.processingInstruction(target, data, locationId, properties);
            out.endElement();
        } else {
            out.processingInstruction(target, data, locationId, properties);
        }
    }

    /**
     * Output an item (atomic value or node) to the sequence
     */

    public void append(Item item, int locationId, int copyNamespaces) throws XPathException {
        if (item instanceof AtomicValue) {
            final NamePool pool = getNamePool();
            out.startElement(resultAtomicValue, StandardNames.XS_UNTYPED, 0, 0);
            AtomicType type = (AtomicType)((AtomicValue)item).getItemType(getConfiguration().getTypeHierarchy());
            int nameCode = type.getNameCode();
            String prefix = pool.getPrefix(nameCode);
            String localName = pool.getLocalName(nameCode);
            String uri = pool.getURI(nameCode);
            if (prefix.length() == 0) {
                prefix = pool.suggestPrefixForURI(uri);
                if (prefix == null) {
                    prefix = "p" + uri.hashCode();
                }
            }
            int nscode = pool.allocateNamespaceCode(prefix, uri);
            String displayName = prefix + ':' + localName;
            out.namespace(nscode, 0);
            out.attribute(xsiType, StandardNames.XS_UNTYPED_ATOMIC, displayName, 0, 0);
            out.startContent();
            out.characters(item.getStringValue(), 0, 0);
            out.endElement();
        } else {
            ((NodeInfo)item).copy(this, NodeInfo.ALL_NAMESPACES, true, locationId);
        }
    }

    /**
     * Notify the start of the content, that is, the completion of all attributes and namespaces.
     * Note that the initial receiver of output from XSLT instructions will not receive this event,
     * it has to detect it itself. Note that this event is reported for every element even if it has
     * no attributes, no namespaces, and no content.
     */

    public void startContent() throws XPathException {
        out.startContent();
    }

    /**
     * Notify the end of the event stream
     */

    public void close() throws XPathException {
        out.endElement();   // close the result:sequence element
        out.endDocument();
        out.close();
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
