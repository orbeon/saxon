package org.orbeon.saxon.xqj;

import org.orbeon.saxon.Configuration;
import org.orbeon.saxon.javax.xml.xquery.XQException;
import org.orbeon.saxon.javax.xml.xquery.XQItemType;
import org.orbeon.saxon.javax.xml.xquery.XQSequenceType;
import org.orbeon.saxon.om.NamePool;
import org.orbeon.saxon.om.NodeInfo;
import org.orbeon.saxon.pattern.DocumentNodeTest;
import org.orbeon.saxon.pattern.NodeTest;
import org.orbeon.saxon.sort.IntHashSet;
import org.orbeon.saxon.type.*;
import org.orbeon.saxon.value.SingletonNode;

import javax.xml.namespace.QName;
import java.net.URI;

/**
 */
public class SaxonXQItemType implements XQItemType {

    private ItemType itemType;
    private Configuration config;

    protected SaxonXQItemType(ItemType itemType, Configuration config) {
        this.itemType = itemType;
        this.config = config;
    }

    protected SaxonXQItemType(NodeInfo node) {
        this.config = node.getConfiguration();
        this.itemType = new SingletonNode(node).getItemType(config.getTypeHierarchy());
    }

    public int getBaseType() {
        int fp;
        if (itemType instanceof AtomicType) {
            if (itemType instanceof BuiltInAtomicType) {
                fp = ((AtomicType)itemType).getFingerprint();
            } else {
                fp = itemType.getPrimitiveType();
                // ideally, we would get the lowest built-in base type
            }
            return SaxonXQDataFactory.mapSaxonTypeToXQJ(fp);
        } else {
            // TODO: it's not clear what we're supposed to return here
            return -1;
        }
    }

    public int getItemKind() {
        if (itemType instanceof AtomicType) {
            return XQITEMKIND_ATOMIC;
        } else if (itemType instanceof NodeTest) {
            if (itemType instanceof DocumentNodeTest) {
                return XQITEMKIND_DOCUMENT_ELEMENT;
            }
            int x = ((NodeTest)itemType).getPrimitiveType();
            switch (x) {
                case Type.DOCUMENT:
                    return XQITEMKIND_DOCUMENT;
                case Type.ELEMENT:
                    return XQITEMKIND_ELEMENT;
                case Type.ATTRIBUTE:
                    return XQITEMKIND_ATTRIBUTE;
                case Type.TEXT:
                    return XQITEMKIND_TEXT;
                case Type.COMMENT:
                    return XQITEMKIND_COMMENT;
                case Type.PROCESSING_INSTRUCTION:
                    return XQITEMKIND_PI;
                case Type.NODE:
                    return XQITEMKIND_NODE;
            }
        }
        return XQITEMKIND_ITEM;
    }

    public int getItemOccurrence() {
        return XQSequenceType.OCC_EXACTLY_ONE;
    }

    public QName getNodeName() throws XQException {
        ItemType type = itemType;
        if (type instanceof DocumentNodeTest) {
            type = ((DocumentNodeTest)type).getElementTest();
        }
        if (type instanceof NodeTest) {
            IntHashSet set = ((NodeTest)itemType).getRequiredNodeNames();
            if (set.size() == 1) {
                int fp = set.getFirst(-1);
                NamePool pool = config.getNamePool();
                String uri = pool.getURI(fp);
                String local = pool.getLocalName(fp);
                return new QName(uri, local);
            }
        }
        return null;
        // TODO: distinguish cases where an exception should be thrown rather than null being returned
    }

    public URI getSchemaURI() {
        return null;  // No idea what this method is supposed to return, but null is apparently OK
    }

    public String getString() {
        return ((AtomicType)itemType).toString(config.getNamePool());
    }

    public String toString() {
        return getString();
    }

    public QName getTypeName() throws XQException {
        ItemType type = itemType;
        if (type instanceof DocumentNodeTest) {
            type = ((DocumentNodeTest)type).getElementTest();
        }
        if (type instanceof NodeTest) {
            SchemaType t = ((NodeTest)type).getContentType();
            if (t != null) {
                int fp = ((NodeTest)type).getContentType().getFingerprint();
                NamePool pool = config.getNamePool();
                String uri = pool.getURI(fp);
                String local = pool.getLocalName(fp);
                return new QName(uri, local);
            }
        }
        throw new XQException("getTypeName() failed: itemType is not a documet, element, or attribute test");
    }

    public boolean isAnonymousType() {
        ItemType type = itemType;
        if (type instanceof DocumentNodeTest) {
            type = ((DocumentNodeTest)type).getElementTest();
        }
        if (type instanceof NodeTest) {
            SchemaType t = ((NodeTest)type).getContentType();
            if (t != null) {
                return t.isAnonymousType();
            }
        }
        return false;
    }

    public boolean isElementNillable() {
        return (itemType instanceof NodeTest) && ((NodeTest)itemType).isNillable();
    }

    public boolean isSchemaElement() {
        return false;  // TODO: implement this (if we can find out what it means)
    }

    public XQItemType getItemType() {
        return this;
    }

    AtomicType getAtomicType() {
        if (itemType instanceof AtomicType) {
            return (AtomicType)itemType;
        } else {
            return null;
        }
    }

    ItemType getSaxonItemType() {
        return itemType;
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
// Contributor(s):
//