package org.orbeon.saxon.event;

import org.orbeon.saxon.expr.ExpressionLocation;
import org.orbeon.saxon.expr.RoleLocator;
import org.orbeon.saxon.expr.Token;
import org.orbeon.saxon.om.Item;
import org.orbeon.saxon.om.NamePool;
import org.orbeon.saxon.pattern.CombinedNodeTest;
import org.orbeon.saxon.pattern.ContentTypeTest;
import org.orbeon.saxon.pattern.NameTest;
import org.orbeon.saxon.pattern.NodeKindTest;
import org.orbeon.saxon.trans.DynamicError;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.type.ItemType;
import org.orbeon.saxon.type.Type;
import org.orbeon.saxon.value.Cardinality;
import org.orbeon.saxon.value.Value;

import java.util.HashSet;

/**
 * A filter on the push pipeline that performs type checking, both of the item type and the
 * cardinality.
 * <p>
 * Note that the TypeCheckingFilter cannot currently check document node tests of the form
 * document-node(element(X,Y)), so it is not invoked in such cases. This isn't a big problem, because most
 * instructions that return document nodes materialize them anyway.
 */

public class TypeCheckingFilter extends ProxyReceiver {

    private ItemType itemType;
    private int cardinality;
    private RoleLocator role;
    private int count = 0;
    private int level = 0;
    private HashSet checkedElements = new HashSet(10);
        // used to avoid repeated checking when a template creates large numbers of elements of the same type

    public void setRequiredType(ItemType type, int cardinality, RoleLocator role) {
        this.itemType = type;
        this.cardinality = cardinality;
        this.role = role;
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
        if (level == 0) {
            if (++count == 2) {
                checkAllowsMany(locationId);
            }
            ItemType type = new CombinedNodeTest(
                    new NameTest(Type.ATTRIBUTE, nameCode, getNamePool()),
                    Token.INTERSECT,
                    new ContentTypeTest(Type.ATTRIBUTE, getConfiguration().getSchemaType(typeCode), getConfiguration()));
            checkItemType(type, locationId);
        }
        nextReceiver.attribute(nameCode, typeCode, value, locationId, properties);
    }

    /**
     * Character data
     */

    public void characters(CharSequence chars, int locationId, int properties) throws XPathException {
        if (level == 0) {
            if (++count == 2) {
                checkAllowsMany(locationId);
            }
            ItemType type = NodeKindTest.TEXT;
            checkItemType(type, locationId);
        }
        nextReceiver.characters(chars, locationId, properties);
    }

    /**
     * Output a comment
     */

    public void comment(CharSequence chars, int locationId, int properties) throws XPathException {
        if (level == 0) {
            if (++count == 2) {
                checkAllowsMany(locationId);
            }
            ItemType type = NodeKindTest.COMMENT;
            checkItemType(type, locationId);
        }
        nextReceiver.comment(chars, locationId, properties);    //To change body of overridden methods use File | Settings | File Templates.
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
        if (level == 0) {
            if (++count == 2) {
                checkAllowsMany(0);
            }
            ItemType type = NodeKindTest.NAMESPACE;
            checkItemType(type, 0);
        }
        nextReceiver.namespace(namespaceCode, properties);    //To change body of overridden methods use File | Settings | File Templates.
    }

    /**
     * Processing Instruction
     */

    public void processingInstruction(String target, CharSequence data, int locationId, int properties) throws XPathException {
        if (level == 0) {
            if (++count == 2) {
                checkAllowsMany(locationId);
            }
            ItemType type = NodeKindTest.PROCESSING_INSTRUCTION;
            checkItemType(type, locationId);
        }
        nextReceiver.processingInstruction(target, data, locationId, properties);
    }

    /**
     * Start of a document node.
     */

    public void startDocument(int properties) throws XPathException {
        if (level == 0) {
            if (++count == 2) {
                checkAllowsMany(0);
            }
            ItemType type = NodeKindTest.DOCUMENT;
            checkItemType(type, 0);
        }
        level++;
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
        if (level == 0) {
            if (++count == 1) {
                // don't bother with any caching on the first item, it will often be the only one
                ItemType type = new CombinedNodeTest(
                        new NameTest(Type.ELEMENT, nameCode, getNamePool()),
                        Token.INTERSECT,
                        new ContentTypeTest(Type.ELEMENT, getConfiguration().getSchemaType(typeCode), getConfiguration()));
                checkItemType(type, locationId);
            } else {
                if (count == 2) {
                    checkAllowsMany(locationId);
                }
                Long key = new Long(((long)(nameCode&NamePool.FP_MASK))<<32 | (long)(typeCode&NamePool.FP_MASK));
                if (!checkedElements.contains(key)) {
                    ItemType type = new CombinedNodeTest(
                            new NameTest(Type.ELEMENT, nameCode, getNamePool()),
                            Token.INTERSECT,
                            new ContentTypeTest(Type.ELEMENT, getConfiguration().getSchemaType(typeCode), getConfiguration()));
                    checkItemType(type, locationId);
                    checkedElements.add(key);
                }
            }
        }
        level++;
        nextReceiver.startElement(nameCode, typeCode, locationId, properties);
    }

    /**
     * Notify the end of a document node
     */

    public void endDocument() throws XPathException {
        level--;
        nextReceiver.endDocument();
    }

    /**
     * End of element
     */

    public void endElement() throws XPathException {
        level--;
        nextReceiver.endElement();
    }

    /**
     * End of event stream
     */

    public void close() throws XPathException {
        if (count == 0 && !Cardinality.allowsZero(cardinality)) {
            DynamicError err = new DynamicError(
                        "An empty sequence is not allowed as the " +
                        role.getMessage());
            String errorCode = role.getErrorCode();
            err.setErrorCode(errorCode);
            if (!"XPDY0050".equals(errorCode)) {
                err.setIsTypeError(true);
            }
            throw err;
        }
        // don't pass on the close event
    }

    /**
     * Output an item (atomic value or node) to the sequence
     */

    public void append(Item item, int locationId, int copyNamespaces) throws XPathException {
        if (level == 0) {
            if (++count == 2) {
                checkAllowsMany(locationId);
            }
            checkItemType(Value.asValue(item).getItemType(getConfiguration().getTypeHierarchy()), locationId);
        }
        if (nextReceiver instanceof SequenceReceiver) {
            ((SequenceReceiver)nextReceiver).append(item, locationId, copyNamespaces);
        } else {
            super.append(item, locationId, copyNamespaces);
        }
    }

    private void checkItemType(ItemType type, int locationId) throws DynamicError {
        if (!getConfiguration().getTypeHierarchy().isSubType(type, itemType)) {
            String message = role.composeErrorMessage(itemType, type, getNamePool());
            String errorCode = role.getErrorCode();
            DynamicError err = new DynamicError(message);
            err.setErrorCode(errorCode);
            if (!"XPDY0050".equals(errorCode)) {
                err.setIsTypeError(true);
            }
            err.setLocator(ExpressionLocation.getSourceLocator(locationId,
                    getPipelineConfiguration().getLocationProvider()));
            throw err;
        }
    }

    private void checkAllowsMany(int locationId) throws XPathException {
        if (!Cardinality.allowsMany(cardinality)) {
            DynamicError err = new DynamicError(
                        "A sequence of more than one item is not allowed as the " +
                        role.getMessage());
            String errorCode = role.getErrorCode();
            err.setErrorCode(errorCode);
            if (!"XPDY0050".equals(errorCode)) {
                err.setIsTypeError(true);
            }
            err.setLocator(ExpressionLocation.getSourceLocator(locationId,
                    getPipelineConfiguration().getLocationProvider()));
            throw err;
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
