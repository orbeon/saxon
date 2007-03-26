package org.orbeon.saxon.event;

import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.value.Whitespace;
import org.orbeon.saxon.om.FastStringBuffer;

/**
 * A filter that can be inserted into a Receiver pipeline to trace the events that pass through
 */
public class TracingFilter extends ProxyReceiver {

    private static int id = 0;
    private String indent = "";

    public TracingFilter() {
        id++;
    }

    public TracingFilter(Receiver r) {
        id++;
        setUnderlyingReceiver(r);
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
        System.err.println("RCVR " + id + indent + " ATTRIBUTE " + getNamePool().getDisplayName(nameCode));
        nextReceiver.attribute(nameCode, typeCode, value, locationId, properties);
    }

    /**
     * Character data
     */

    public void characters(CharSequence chars, int locationId, int properties) throws XPathException {
        System.err.println("RCVR " + id + indent + " CHARACTERS " + (Whitespace.isWhite(chars) ? "(whitespace)" : ""));
        FastStringBuffer sb = new FastStringBuffer(chars.length() * 3);
        for (int i=0; i<chars.length(); i++) {
            sb.append((int)chars.charAt(i) + " ");
        }
        System.err.println("    \"" + sb + '\"');
        nextReceiver.characters(chars, locationId, properties);
    }

    /**
     * End of document
     */

    public void close() throws XPathException {
        System.err.println("RCVR " + id + indent + " CLOSE");
        nextReceiver.close();
    }

    /**
     * Output a comment
     */

    public void comment(CharSequence chars, int locationId, int properties) throws XPathException {
        System.err.println("RCVR " + id + indent + " COMMENT");
        nextReceiver.comment(chars, locationId, properties);
    }

    /**
     * Notify the end of a document node
     */

    public void endDocument() throws XPathException {
        System.err.println("RCVR " + id + indent + " END DOCUMENT");
        nextReceiver.endDocument();
    }

    /**
     * End of element
     */

    public void endElement() throws XPathException {
        indent = indent.substring(2);
        System.err.println("RCVR " + id + indent + " END ELEMENT");
        nextReceiver.endElement();
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
        System.err.println("RCVR " + id + indent + " NAMESPACE " +
                getNamePool().getPrefixFromNamespaceCode(namespaceCode) + "=" +
                getNamePool().getURIFromNamespaceCode(namespaceCode));
        nextReceiver.namespace(namespaceCode, properties);
    }

    /**
     * Start of event stream
     */

    public void open() throws XPathException {
        System.err.println("RCVR " + id + indent + " OPEN");
        nextReceiver.open();
    }

    /**
     * Processing Instruction
     */

    public void processingInstruction(String target, CharSequence data, int locationId, int properties) throws XPathException {
        System.err.println("RCVR " + id + indent + " PROCESSING INSTRUCTION");
        nextReceiver.processingInstruction(target, data, locationId, properties);
    }

    /**
     * Notify the start of the content, that is, the completion of all attributes and namespaces.
     * Note that the initial receiver of output from XSLT instructions will not receive this event,
     * it has to detect it itself. Note that this event is reported for every element even if it has
     * no attributes, no namespaces, and no content.
     */


    public void startContent() throws XPathException {
        System.err.println("RCVR " + id + indent + " START CONTENT");
        nextReceiver.startContent();
    }

    /**
     * Start of a document node.
     */

    public void startDocument(int properties) throws XPathException {
        System.err.println("RCVR " + id + indent + " START DOCUMENT");
        nextReceiver.startDocument(properties);
    }

    /**
     * Notify the start of an element
     *
     * @param nameCode   integer code identifying the name of the element within the name pool.
     * @param typeCode   integer code identifying the element's type within the name pool.
     * @param properties properties of the element node
     */

    public void startElement(int nameCode, int typeCode, int locationId, int properties) throws XPathException {
        System.err.println("RCVR " + id + indent + " START ELEMENT " + getNamePool().getDisplayName(nameCode));
        indent = indent + "  ";
        nextReceiver.startElement(nameCode, typeCode, locationId, properties);
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
// Contributor(s):
//
