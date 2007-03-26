package org.orbeon.saxon.dom;

import org.orbeon.saxon.om.NodeInfo;
import org.orbeon.saxon.pattern.ContentTypeTest;
import org.orbeon.saxon.style.StandardNames;
import org.orbeon.saxon.type.BuiltInSchemaFactory;
import org.orbeon.saxon.type.SchemaType;
import org.orbeon.saxon.type.Type;
import org.w3c.dom.Attr;
import org.w3c.dom.DOMException;
import org.w3c.dom.Element;
import org.w3c.dom.TypeInfo;

/**
 * This class is an implementation of the DOM Attr class that wraps a Saxon NodeInfo
 * representation of an attribute or namespace node.
 */

public class AttrOverNodeInfo extends NodeOverNodeInfo implements Attr {

     /**
    * Get the name of an attribute node (the lexical QName) (DOM method)
    */

    public String getName() {
        if (node.getNodeKind() == Type.NAMESPACE) {
            String local = node.getLocalPart();
            if (local.equals("")) {
                return "xmlns";
            } else {
                return "xmlns:" + local;
            }
        }
        return node.getDisplayName();
    }

    /**
    * Return the character value of an attribute node (DOM method)
    * @return the attribute value
    */

    public String getValue() {
        return node.getStringValue();
    }

    /**
     * If this attribute was explicitly given a value in the original
     * document, this is <code>true</code> ; otherwise, it is
     * <code>false</code>. (DOM method)
     * @return Always true in this implementation.
     */

    public boolean getSpecified() {
        return true;
    }

    /**
    * Set the value of an attribute node. (DOM method).
    * Always fails (because tree is readonly)
    */

    public void setValue(String value) throws DOMException {
        disallowUpdate();
    }

    /**
     * Determine whether this (attribute) node is an ID. This method is introduced
     * in DOM Level 3.
     */

    public boolean isId() {
        SchemaType id = BuiltInSchemaFactory.getSchemaType(StandardNames.XS_ID);
        ContentTypeTest idTest = new ContentTypeTest(Type.ATTRIBUTE, id, node.getConfiguration());
        idTest.setMatchDTDTypes(true);
        return idTest.matches(node);
    }


    /**
     * The <code>Element</code> node this attribute is attached to or
     * <code>null</code> if this attribute is not in use.
     * @since DOM Level 2
     */

    public Element getOwnerElement() {
        if (node.getNodeKind() == Type.ATTRIBUTE || node.getNodeKind() == Type.NAMESPACE) {
            return (Element)wrap(node.getParent());
        } else {
            throw new UnsupportedOperationException(
                        "This method is defined only on attribute and namespace nodes");
        }
    }
    /**
     * Get the schema type information for this node. Returns null for an untyped node.
     */

    public TypeInfo getSchemaTypeInfo() {
        int annotation = node.getTypeAnnotation();
        if (annotation == -1 || ((annotation & NodeInfo.IS_DTD_TYPE) != 0)) {
            return null;
        }
        return new TypeInfoImpl(node.getConfiguration(),
                node.getConfiguration().getSchemaType(annotation));
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