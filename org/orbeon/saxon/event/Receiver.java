package org.orbeon.saxon.event;
import org.orbeon.saxon.trans.XPathException;

import javax.xml.transform.Result;
/**
  * Receiver: This interface represents a recipient of XML tree-walking (push) events. It is
  * based on SAX2's ContentHandler, but adapted to handle additional events, and
  * to use Saxon's name pool. Namespaces and Attributes are handled by separate events
  * following the startElement event. Schema types can be defined for elements and attributes.
  * <p>
  * The Receiver interface is an important internal interface within Saxon, and provides a powerful
  * mechanism for integrating Saxon with other applications. It has been designed with extensibility
  * and stability in mind. However, it should be considered as an interface designed primarily for
  * internal use, and not as a completely stable part of the public Saxon API.
  * <p>
  * @author Michael H. Kay
  */

public interface Receiver extends Result {

    /**
    * Set the pipeline configuration
    */

    public void setPipelineConfiguration(PipelineConfiguration config);

    /**
    * Get the pipeline configuration
    */

    public PipelineConfiguration getPipelineConfiguration();

	/**
	* Set the System ID of the tree represented by this event stream
	*/

	public void setSystemId(String systemId);

    /**
    * Notify the start of the event stream
    */

    public void open() throws XPathException;

    /**
     * Notify the start of a document node
     */

    public void startDocument(int properties) throws XPathException;

    /**
     * Notify the end of a document node
     */

    public void endDocument() throws XPathException;

    /**
    * Notify an unparsed entity URI.
    * @param name The name of the unparsed entity
    * @param systemID The system identifier of the unparsed entity
    * @param publicID The public identifier of the unparsed entity
    */

    public void setUnparsedEntity(String name, String systemID, String publicID) throws XPathException;

    /**
     * Notify the start of an element
     * @param nameCode integer code identifying the name of the element within the name pool.
     * @param typeCode integer code identifying the element's type within the name pool. The value -1
     * indicates the default type, xdt:untyped.
     * @param locationId an integer which can be interpreted using a {@link LocationProvider} to return
     * information such as line number and system ID. If no location information is available,
     * the value zero is supplied.
     * @param properties bit-significant properties of the element node. If there are no revelant
     * properties, zero is supplied.
    */

    public void startElement(int nameCode, int typeCode, int locationId, int properties)
            throws XPathException;

    /**
     * Notify a namespace. Namespaces are notified <b>after</b> the startElement event, and before
     * any children for the element. The namespaces that are reported are only required
     * to include those that are different from the parent element. The events represent namespace
     * declarations and undeclarations rather than in-scope namespace nodes: an undeclaration is represented
     * by a namespace code of zero. If the sequence of namespace events contains two
     * A namespace must not conflict with any namespaces already used for element or attribute names.
     * @param namespaceCode an integer: the top half is a prefix code, the bottom half a URI code.
     * These may be translated into an actual prefix and URI using the name pool. A prefix code of
     * zero represents the empty prefix (that is, the default namespace). A URI code of zero represents
     * a URI of "", that is, a namespace undeclaration.
     * @param properties The most important property is REJECT_DUPLICATES. If this property is set, the
     * namespace declaration will be rejected if it conflicts with a previous declaration of the same
     * prefix. If the property is not set, the namespace declaration will be ignored if it conflicts
     * with a previous declaration. This reflects the fact that when copying a tree, namespaces for child
     * elements are emitted before the namespaces of their parent element. Unfortunately this conflicts
     * with the XSLT rule for complex content construction, where the recovery action in the event of
     * conflicts is to take the namespace that comes last. XSLT therefore doesn't recover from this error:
     * it sets the REJECT_DUPLICATES flag, and this is treated as a hard error.
    */

    public void namespace(int namespaceCode, int properties) throws XPathException;

    /**
     * Notify an attribute. Attributes are notified after the startElement event, and before any
     * children. Namespaces and attributes may be intermingled.
     * @param nameCode The name of the attribute, as held in the name pool
     * @param typeCode The type of the attribute, as held in the name pool. The additional bit
     * NodeInfo.IS_DTD_TYPE may be set to indicate a DTD-derived type.
     * @param locationId an integer which can be interpreted using a {@link LocationProvider} to return
     * information such as line number and system ID. If no location information is available,
     * the value zero is supplied.
     * @param properties Bit significant value. The following bits are defined:
     *        <dt>DISABLE_ESCAPING</dt>    <dd>Disable escaping for this attribute</dd>
     *        <dt>NO_SPECIAL_CHARACTERS</dt>      <dd>Attribute value contains no special characters</dd>
     * @throws IllegalStateException: attempt to output an attribute when there is no open element
     * start tag
    */

    public void attribute(int nameCode, int typeCode, CharSequence value, int locationId, int properties)
            throws XPathException;

    /**
    * Notify the start of the content, that is, the completion of all attributes and namespaces.
    * Note that the initial receiver of output from XSLT instructions will not receive this event,
    * it has to detect it itself. Note that this event is reported for every element even if it has
    * no attributes, no namespaces, and no content.
    */

    public void startContent() throws XPathException;

    /**
    * Notify the end of an element. The receiver must maintain a stack if it needs to know which
    * element is ending.
    */

    public void endElement() throws XPathException;

    /**
     * Notify character data. Note that some receivers may require the character data to be
     * sent in a single event, but in general this is not a requirement.
     * @param chars The characters
     * @param locationId an integer which can be interpreted using a {@link LocationProvider} to return
     * information such as line number and system ID. If no location information is available,
     * the value zero is supplied.
     * @param properties Bit significant value. The following bits are defined:
     *        <dt>DISABLE_ESCAPING</dt>           <dd>Disable escaping for this text node</dd>
     *        <dt>USE_CDATA</dt>                  <dd>Output as a CDATA section</dd>
     *        <dt>NO_SPECIAL_CHARACTERS</dt>      <dd>Value contains no special characters</dd>
     *        <dt>WHITESPACE</dt>                 <dd>Text is all whitespace</dd>
     */

    public void characters(CharSequence chars, int locationId, int properties)
            throws XPathException;

    /**
     * Output a processing instruction
     * @param name The PI name. This must be a legal name (it will not be checked).
     * @param data The data portion of the processing instruction
     * @param locationId an integer which can be interpreted using a {@link LocationProvider} to return
     * information such as line number and system ID. If no location information is available,
     * the value zero is supplied.
     * @param properties Additional information about the PI. The following bits are
     * defined:
     *        <dt>CHECKED</dt>    <dd>Data is known to be legal (e.g. doesn't contain "?>")</dd>
     * @throws IllegalArgumentException: the content is invalid for an XML processing instruction
     */

    public void processingInstruction(String name, CharSequence data, int locationId, int properties)
            throws XPathException;

    /**
     * Notify a comment. Comments are only notified if they are outside the DTD.
     * @param content The content of the comment
     * @param locationId an integer which can be interpreted using a {@link LocationProvider} to return
     * information such as line number and system ID. If no location information is available,
     * the value zero is supplied.
     * @param properties Additional information about the comment. The following bits are
     * defined:
     *        <dt>CHECKED</dt>    <dd>Comment is known to be legal (e.g. doesn't contain "--")</dd>
     * @throws IllegalArgumentException: the content is invalid for an XML comment
     */

    public void comment(CharSequence content, int locationId, int properties) throws XPathException;

    /**
    * Notify the end of the event stream
    */

    public void close() throws XPathException;



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
