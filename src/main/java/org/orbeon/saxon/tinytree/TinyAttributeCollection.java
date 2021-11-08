package org.orbeon.saxon.tinytree;

import org.orbeon.saxon.event.LocationProvider;
import org.orbeon.saxon.om.AttributeCollection;
import org.orbeon.saxon.om.NamePool;
import org.orbeon.saxon.om.StandardNames;

/**
 * An implementation of the AttributeCollection interface based directly on the
 * TinyTree data structure.
 */

public class TinyAttributeCollection implements AttributeCollection {

    int element;
    TinyTree tree;
    int firstAttribute;

    public TinyAttributeCollection(TinyTree tree, int element) {
        this.tree = tree;
        this.element = element;
        firstAttribute = tree.alpha[element];
    }

    /**
     * Set the location provider. This must be set if the methods getSystemId() and getLineNumber()
     * are to be used to get location information for an attribute.
     */

    public void setLocationProvider(LocationProvider provider) {
        //
    }

    /**
     * Return the number of attributes in the list.
     *
     * @return The number of attributes in the list.
     */

    public int getLength() {
        int i = firstAttribute;
        while (i < tree.numberOfAttributes && tree.attParent[i] == element) {
            i++;
        }
        return i - firstAttribute;
    }

    /**
     * Get the namecode of an attribute (by position).
     *
     * @param index The position of the attribute in the list.
     * @return The display name of the attribute as a string, or null if there
     *         is no attribute at that position.
     */

    public int getNameCode(int index) {
        return tree.attCode[firstAttribute + index];
    }

    /**
     * Get the type annotation of an attribute (by position).
     *
     * @param index The position of the attribute in the list.
     * @return The type annotation of the attribute as the fingerprint of the type name.
     * The bit {@link org.orbeon.saxon.om.NodeInfo#IS_DTD_TYPE} represents a DTD-derived type.
     */

    public int getTypeAnnotation(int index) {
        if (tree.attTypeCode == null) {
            return StandardNames.XS_UNTYPED_ATOMIC;
        }
        return tree.attTypeCode[firstAttribute + index];
    }

    /**
     * Get the locationID of an attribute (by position)
     *
     * @param index The position of the attribute in the list.
     * @return The location identifier of the attribute. This can be supplied
     *         to a {@link org.orbeon.saxon.event.LocationProvider} in order to obtain the
     *         actual system identifier and line number of the relevant location
     */

    public int getLocationId(int index) {
        return 0;
    }

    /**
     * Get the systemId part of the location of an attribute, at a given index.
     * <p/>
     * <p>Attribute location information is not available from a SAX parser, so this method
     * is not useful for getting the location of an attribute in a source document. However,
     * in a Saxon result document, the location information represents the location in the
     * stylesheet of the instruction used to generate this attribute, which is useful for
     * debugging.</p>
     *
     * @param index the required attribute
     * @return the systemId of the location of the attribute
     */

    public String getSystemId(int index) {
        return tree.getSystemId(element);
    }

    /**
     * Get the line number part of the location of an attribute, at a given index.
     * <p/>
     * <p>Attribute location information is not available from a SAX parser, so this method
     * is not useful for getting the location of an attribute in a source document. However,
     * in a Saxon result document, the location information represents the location in the
     * stylesheet of the instruction used to generate this attribute, which is useful for
     * debugging.</p>
     *
     * @param index the required attribute
     * @return the line number of the location of the attribute
     */

    public int getLineNumber(int index) {
        return -1;
    }

    /**
     * Get the properties of an attribute (by position)
     *
     * @param index The position of the attribute in the list.
     * @return The properties of the attribute. This is a set
     *         of bit-settings defined in class {@link org.orbeon.saxon.event.ReceiverOptions}. The
     *         most interesting of these is {{@link org.orbeon.saxon.event.ReceiverOptions#DEFAULTED_ATTRIBUTE},
     *         which indicates an attribute that was added to an element as a result of schema validation.
     */

    public int getProperties(int index) {
        return 0;
    }

    /**
     * Get the prefix of the name of an attribute (by position).
     *
     * @param index The position of the attribute in the list.
     * @return The prefix of the attribute name as a string, or null if there
     *         is no attribute at that position. Returns "" for an attribute that
     *         has no prefix.
     */

    public String getPrefix(int index) {
        return tree.getNamePool().getPrefix(getNameCode(index));
    }

    /**
     * Get the lexical QName of an attribute (by position).
     *
     * @param index The position of the attribute in the list.
     * @return The lexical QName of the attribute as a string, or null if there
     *         is no attribute at that position.
     */

    public String getQName(int index) {
        return tree.getNamePool().getDisplayName(getNameCode(index));
    }

    /**
     * Get the local name of an attribute (by position).
     *
     * @param index The position of the attribute in the list.
     * @return The local name of the attribute as a string, or null if there
     *         is no attribute at that position.
     */

    public String getLocalName(int index) {
        return tree.getNamePool().getLocalName(getNameCode(index));
    }

    /**
     * Get the namespace URI of an attribute (by position).
     *
     * @param index The position of the attribute in the list.
     * @return The local name of the attribute as a string, or null if there
     *         is no attribute at that position.
     */

    public String getURI(int index) {
        return tree.getNamePool().getURI(getNameCode(index));
    }

    /**
     * Get the index of an attribute (by name).
     *
     * @param uri       The namespace uri of the attribute.
     * @param localname The local name of the attribute.
     * @return The index position of the attribute, or -1 if there is no attribute with this name
     */

    public int getIndex(String uri, String localname) {
        int fingerprint = tree.getNamePool().getFingerprint(uri, localname);
        return getIndexByFingerprint(fingerprint);
    }

    /**
     * Get the index, given the fingerprint
     * @param fingerprint the NamePool fingerprint of the required attribute name
     * @return The index position of the attribute, or -1 if there is no attribute with this name
     */

    public int getIndexByFingerprint(int fingerprint) {
        int i = firstAttribute;
        while (tree.attParent[i] == element) {
            if ((tree.attCode[i] & NamePool.FP_MASK) == fingerprint) {
                 return i - firstAttribute;
            }
            i++;
        }
        return -1;
    }

    /**
     * Get the attribute value using its fingerprint
     */

    public String getValueByFingerprint(int fingerprint) {
        return getValue(getIndexByFingerprint(fingerprint));
    }

    /**
     * Get the value of an attribute (by name).
     *
     * @param uri       The namespace uri of the attribute.
     * @param localname The local name of the attribute.
     * @return The index position of the attribute
     */

    public String getValue(String uri, String localname) {
        return getValue(getIndex(uri, localname));
    }

    /**
     * Get the value of an attribute (by position).
     *
     * @param index The position of the attribute in the list.
     * @return The attribute value as a string, or null if
     *         there is no attribute at that position.
     */

    public String getValue(int index) {
        CharSequence cs = tree.attValue[firstAttribute + index];
        return (cs==null ? null : cs.toString());
    }

    /**
     * Determine whether a given attribute has the is-ID property set
     */

    public boolean isId(int index) {
        return ((getTypeAnnotation(index) & NamePool.FP_MASK) == StandardNames.XS_ID) ||
                ((getNameCode(index) & NamePool.FP_MASK) == StandardNames.XML_ID);
    }

    /**
     * Determine whether a given attribute has the is-idref property set
     */

    public boolean isIdref(int index) {
        return ((getTypeAnnotation(index) & NamePool.FP_MASK) == StandardNames.XS_IDREF) ||
                ((getTypeAnnotation(index) & NamePool.FP_MASK) == StandardNames.XS_IDREFS);
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
