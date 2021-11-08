package org.orbeon.saxon.xqj;

import org.orbeon.saxon.Configuration;
import org.orbeon.saxon.om.NamePool;
import org.orbeon.saxon.om.NodeInfo;
import org.orbeon.saxon.om.StandardNames;
import org.orbeon.saxon.pattern.DocumentNodeTest;
import org.orbeon.saxon.pattern.NameTest;
import org.orbeon.saxon.pattern.NodeKindTest;
import org.orbeon.saxon.pattern.NodeTest;
import org.orbeon.saxon.sort.IntHashSet;
import org.orbeon.saxon.type.*;
import org.orbeon.saxon.value.SingletonNode;

import javax.xml.namespace.QName;
import javax.xml.xquery.XQException;
import javax.xml.xquery.XQItemType;
import javax.xml.xquery.XQSequenceType;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * Saxon implementation of the XQJ XQItemType interface
 */
public class SaxonXQItemType implements XQItemType {

    private ItemType itemType;
    private Configuration config;

    protected SaxonXQItemType(ItemType itemType, Configuration config) {
        this.itemType = itemType;
        this.config = config;
    }

    protected SaxonXQItemType(NodeInfo node) {
        config = node.getConfiguration();
        itemType = new SingletonNode(node).getItemType(config.getTypeHierarchy());
    }

    public int getBaseType() throws XQException {
        if (itemType instanceof AtomicType) {
            AtomicType at = (AtomicType)itemType;
            while (!at.isBuiltInType()) {
                at = (AtomicType)at.getBaseType();
            }
            return SaxonXQDataFactory.mapSaxonTypeToXQJ(at.getFingerprint());
        } else if (itemType instanceof NodeTest) {
            NodeTest it = (NodeTest)itemType;
            if (it instanceof DocumentNodeTest) {
                it = ((DocumentNodeTest)it).getElementTest();
            }
            if ((it.getNodeKindMask() &
                    (1<<Type.DOCUMENT | 1<<Type.TEXT | 1<<Type.COMMENT | 1<<Type.PROCESSING_INSTRUCTION | 1<<Type.NAMESPACE)) != 0) {
                throw new XQException("Wrong node kind for getBaseType()");
            }
            SchemaType contentType = it.getContentType();
            if (contentType.isAtomicType()) {
                AtomicType at = (AtomicType)contentType;
                while (!at.isBuiltInType()) {
                    at = (AtomicType)at.getBaseType();
                }
                return SaxonXQDataFactory.mapSaxonTypeToXQJ(at.getFingerprint());
            } else if (contentType.isSimpleType()) {
                if (((SimpleType)contentType).isListType()) {
                    int fp = contentType.getFingerprint();
                    if (fp == StandardNames.XS_NMTOKENS) {
                        return XQBASETYPE_NMTOKENS;
                    } else if (fp == StandardNames.XS_ENTITIES) {
                        return XQBASETYPE_ENTITIES;
                    } else if (fp == StandardNames.XS_IDREFS) {
                        return XQBASETYPE_IDREFS;
                    }
                }
                return XQBASETYPE_ANYSIMPLETYPE;
            } else if (contentType == Untyped.getInstance()) {
                return XQBASETYPE_UNTYPED;
            } else {
                return XQBASETYPE_ANYTYPE;
            }

        } else {
            throw new XQException("Wrong item type for getBaseType()");
        }
    }

    public int getItemKind() {
        if (itemType instanceof AtomicType) {
            return XQITEMKIND_ATOMIC;
        } else if (itemType instanceof NodeTest) {
            if (itemType instanceof DocumentNodeTest) {
                return XQITEMKIND_DOCUMENT_ELEMENT;
            }
            int x = itemType.getPrimitiveType();
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
            if ((((NodeTest)type).getNodeKindMask() &
                    (1<<Type.DOCUMENT | 1<<Type.TEXT | 1<<Type.COMMENT | 1<<Type.PROCESSING_INSTRUCTION | 1<<Type.NAMESPACE)) != 0) {
                throw new XQException("Wrong node kind for getNodeName()");
            }
            IntHashSet set = ((NodeTest)type).getRequiredNodeNames();
            if (set != null && set.size() == 1) {
                int fp = set.getFirst(-1);
                NamePool pool = config.getNamePool();
                String uri = pool.getURI(fp);
                String local = pool.getLocalName(fp);
                return new QName(uri, local);
            } else {
                return null;
            }
        }
        throw new XQException("getNodeName() is not defined for this kind of item type");
    }

    public String getPIName() throws XQException {
        if (itemType instanceof NameTest && itemType.getPrimitiveType() == Type.PROCESSING_INSTRUCTION) {
            NamePool pool = config.getNamePool();
            return pool.getLocalName(((NameTest)itemType).getFingerprint());
        } else if (itemType instanceof NodeKindTest && itemType.getPrimitiveType() == Type.PROCESSING_INSTRUCTION) {
            return null;
        } else {
            throw new XQException("Item kind is not a processing instruction");
        }
    }

    public URI getSchemaURI() {
        try {
            if (itemType instanceof NodeTest) {
                SchemaType content = ((NodeTest)itemType).getContentType();
                if (content == null) {
                    return null;
                }
                String systemId = content.getSystemId();
                if (systemId == null) {
                    return null;
                }
                return new URI(systemId);
            } else if (itemType instanceof AtomicType) {
                String systemId = ((AtomicType)itemType).getSystemId();
                return (systemId == null ? null : new URI(systemId));
            } else {
                return null;
            }
        } catch (URISyntaxException e) {
            return null;
        }
    }

    public String toString() {
        return itemType.toString(config.getNamePool());
    }

    public QName getTypeName() throws XQException {
        ItemType type = itemType;
        if (type instanceof AtomicType) {
            int fp = ((AtomicType)type).getFingerprint();
            NamePool pool = config.getNamePool();
            String uri = pool.getURI(fp);
            String local = pool.getLocalName(fp);
            return new QName(uri, local);
        }
        if (type instanceof DocumentNodeTest) {
            type = ((DocumentNodeTest)type).getElementTest();
        }
        if (type instanceof NodeTest) {
            if ((((NodeTest)type).getNodeKindMask() &
                    (1<<Type.DOCUMENT | 1<<Type.TEXT | 1<<Type.COMMENT | 1<<Type.PROCESSING_INSTRUCTION | 1<<Type.NAMESPACE)) != 0) {
                throw new XQException("getTypeName() failed: itemType is not a document, element, or attribute test");
            }
            SchemaType t = ((NodeTest)type).getContentType();
            if (t != null) {
                int fp = ((NodeTest)type).getContentType().getFingerprint();
                NamePool pool = config.getNamePool();
                String uri = pool.getURI(fp);
                String local = pool.getLocalName(fp);
                return new QName(uri, local);
            }
        }
        throw new XQException("getTypeName() failed: itemType is not a document, element, or attribute test");
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

    public boolean equals(Object obj) {
        return obj instanceof SaxonXQItemType &&
                itemType.equals(((SaxonXQItemType)obj).itemType);
    }

    public int hashCode()  {
        return itemType.hashCode();
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