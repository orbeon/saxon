package org.orbeon.saxon.om;

import org.orbeon.saxon.Configuration;
import org.orbeon.saxon.event.LocationProvider;
import org.xml.sax.Attributes;


/**
 * AttributeCollectionImpl is an implementation of both the SAX2 interface Attributes
 * and the Saxon equivalent AttributeCollection.
 *
 * <p>As well as providing the information required by the SAX2 interface, an
 * AttributeCollection can hold type information (as needed to support the JAXP 1.3
 * {@link javax.xml.validation.ValidatorHandler} interface), and location information
 * for debugging. The location information is used in the case of attributes on a result
 * tree to identify the location in the query or stylesheet from which they were
 * generated.
 */

public final class AttributeCollectionImpl implements Attributes, AttributeCollection {

    // Attribute values are maintained as an array of Strings. Everything else is maintained
    // in the form of integers.

    private Configuration config;
    private LocationProvider locationProvider;
    private String[] values = null;
    private int[] codes = null;
    private int used = 0;

    // Empty attribute collection. The caller is trusted not to try and modify it.

    public static final AttributeCollection EMPTY_ATTRIBUTE_COLLECTION =
            new AttributeCollectionImpl(null);

    // Layout of the integer array. There are RECSIZE integers for each attribute.

    private static final int RECSIZE = 4;

    //private static final int NAMECODE = 0;
    private static final int TYPECODE = 1;
    private static final int LOCATIONID = 2;
    private static final int PROPERTIES = 3;

    /**
     * Create an empty attribute list.
     * @param config the Saxon Configuration
     */

    public AttributeCollectionImpl(Configuration config) {
        this.config = config;
        used = 0;
    }

    /**
     * Set the location provider. This must be set if the methods getSystemId() and getLineNumber()
     * are to be used to get location information for an attribute.
     * @param provider the location provider
     */

    public void setLocationProvider(LocationProvider provider) {
        locationProvider = provider;
    }

    /**
     * Add an attribute to an attribute list. The parameters correspond
     * to the parameters of the {@link org.orbeon.saxon.event.Receiver#attribute(int,int,CharSequence,int,int)}
     * method. There is no check that the name of the attribute is distinct from other attributes
     * already in the collection: this check must be made by the caller.
     *
     * @param nameCode Integer representing the attribute name.
     * @param typeCode  The attribute type code
     * @param value    The attribute value (must not be null)
     * @param locationId Identifies the attribtue location.
     * @param properties Attribute properties
     */

    public void addAttribute(int nameCode, int typeCode, String value, long locationId, int properties) {
        if (values == null) {
            values = new String[5];
            codes = new int[5 * RECSIZE];
            used = 0;
        }
        if (values.length == used) {
            int newsize = (used == 0 ? 5 : used * 2);
            String[] v2 = new String[newsize];
            int[] c2 = new int[newsize * RECSIZE];
            System.arraycopy(values, 0, v2, 0, used);
            System.arraycopy(codes, 0, c2, 0, used*RECSIZE);
            values = v2;
            codes = c2;
        }
        int n = used*RECSIZE;
        codes[n] = nameCode;
        codes[n+TYPECODE] = typeCode;
        codes[n+LOCATIONID] = (int)locationId;
        codes[n+PROPERTIES] = properties;
        values[used++] = value;
    }

    /**
     * Set (overwrite) an attribute in the attribute list. The parameters correspond
     * to the parameters of the {@link org.orbeon.saxon.event.Receiver#attribute(int,int,CharSequence,int,int)}
     * method.
     * @param index Identifies the entry to be replaced
     * @param nameCode Integer representing the attribute name.
     * @param typeCode  The attribute type code
     * @param value    The attribute value (must not be null)
     * @param locationId Identifies the attribtue location.
     * @param properties Attribute properties
     */

    public void setAttribute(int index, int nameCode, int typeCode, String value, long locationId, int properties) {
        int n = index*RECSIZE;
        codes[n] = nameCode;
        codes[n+TYPECODE] = typeCode;
        codes[n+LOCATIONID] = (int)locationId;
        codes[n+PROPERTIES] = properties;
        values[index] = value;
    }


    /**
     * Clear the attribute list. This removes the values but doesn't free the memory used.
     * free the memory, use clear() then compact().
     */

    public void clear() {
        used = 0;
    }

    /**
     * Compact the attribute list to avoid wasting memory
     */

    public void compact() {
        if (used == 0) {
            codes = null;
            values = null;
        } else if (values.length > used) {
            String[] v2 = new String[used];
            int[] c2 = new int[used * RECSIZE];
            System.arraycopy(values, 0, v2, 0, used);
            System.arraycopy(codes, 0, c2, 0, used*RECSIZE);
            values = v2;
            codes = c2;
        }
    }

    /**
     * Return the number of attributes in the list.
     *
     * @return The number of attributes in the list.
     */

    public int getLength() {
        return (values == null ? 0 : used);
    }

    /**
     * Get the namecode of an attribute (by position).
     *
     * @param index The position of the attribute in the list.
     * @return The display name of the attribute as a string, or null if there
     *         is no attribute at that position.
     */

    public int getNameCode(int index) {
        if (codes == null) {
            return -1;
        }
        if (index < 0 || index >= used) {
            return -1;
        }

        return codes[(index * RECSIZE)];
    }

    /**
     * Get the namecode of an attribute (by position).
     *
     * @param index The position of the attribute in the list.
     * @return The type annotation, as the fingerprint of the type name.
     * The bit {@link org.orbeon.saxon.om.NodeInfo#IS_DTD_TYPE} represents a DTD-derived type.
     */

    public int getTypeAnnotation(int index) {
        if (codes == null) {
            return StandardNames.XS_UNTYPED_ATOMIC;
        }
        if (index < 0 || index >= used) {
            return StandardNames.XS_UNTYPED_ATOMIC;
        }

        return codes[index * RECSIZE + TYPECODE];
    }

    /**
     * Get the locationID of an attribute (by position)
     * @param index The position of the attribute in the list.
     * @return The location identifier of the attribute. This can be supplied
     * to a {@link org.orbeon.saxon.event.LocationProvider} in order to obtain the
     * actual system identifier and line number of the relevant location
     */

    public int getLocationId(int index) {
        if (codes == null) {
            return -1;
        }
        if (index < 0 || index >= used) {
            return -1;
        }

        return codes[index * RECSIZE + LOCATIONID];
    }

    /**
     * Get the systemId part of the location of an attribute, at a given index.
     *
     * <p>Attribute location information is not available from a SAX parser, so this method
     * is not useful for getting the location of an attribute in a source document. However,
     * in a Saxon result document, the location information represents the location in the
     * stylesheet of the instruction used to generate this attribute, which is useful for
     * debugging.</p>
     * @param index the required attribute
     * @return the systemId of the location of the attribute
     */

    public String getSystemId(int index) {
        return locationProvider.getSystemId(getLocationId(index));
    }

    /**
     * Get the line number part of the location of an attribute, at a given index.
     *
     * <p>Attribute location information is not available from a SAX parser, so this method
     * is not useful for getting the location of an attribute in a source document. However,
     * in a Saxon result document, the location information represents the location in the
     * stylesheet of the instruction used to generate this attribute, which is useful for
     * debugging.</p>
     * @param index the required attribute
     * @return the line number of the location of the attribute
     */

     public int getLineNumber(int index) {
         return locationProvider.getLineNumber(getLocationId(index));
     }

    /**
     * Get the properties of an attribute (by position)
     * @param index The position of the attribute in the list.
     * @return The properties of the attribute. This is a set
     * of bit-settings defined in class {@link org.orbeon.saxon.event.ReceiverOptions}. The
     * most interesting of these is {{@link org.orbeon.saxon.event.ReceiverOptions#DEFAULTED_ATTRIBUTE},
     * which indicates an attribute that was added to an element as a result of schema validation.
     */

    public int getProperties(int index) {
        if (codes == null) {
            return -1;
        }
        if (index < 0 || index >= used) {
            return -1;
        }

        return codes[index * RECSIZE + PROPERTIES];
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
        if (codes == null) {
            return null;
        }
        if (index < 0 || index >= used) {
            return null;
        }
        return config.getNamePool().getPrefix(getNameCode(index));
    }

    /**
     * Get the lexical QName of an attribute (by position).
     *
     * @param index The position of the attribute in the list.
     * @return The lexical QName of the attribute as a string, or null if there
     *         is no attribute at that position.
     */

    public String getQName(int index) {
        if (codes == null) {
            return null;
        }
        if (index < 0 || index >= used) {
            return null;
        }
        return config.getNamePool().getDisplayName(getNameCode(index));
    }

    /**
     * Get the local name of an attribute (by position).
     *
     * @param index The position of the attribute in the list.
     * @return The local name of the attribute as a string, or null if there
     *         is no attribute at that position.
     */

    public String getLocalName(int index) {
        if (codes == null) {
            return null;
        }
        if (index < 0 || index >= used) {
            return null;
        }
        return config.getNamePool().getLocalName(getNameCode(index));
    }

    /**
     * Get the namespace URI of an attribute (by position).
     *
     * @param index The position of the attribute in the list.
     * @return The local name of the attribute as a string, or null if there
     *         is no attribute at that position.
     */

    public String getURI(int index) {
        if (codes == null) {
            return null;
        }
        if (index < 0 || index >= used) {
            return null;
        }
        return config.getNamePool().getURI(getNameCode(index));
    }


    /**
     * Get the type of an attribute (by position). This is a SAX2 method,
     * so it gets the type name as a DTD attribute type, mapped from the
     * schema type code.
     *
     * @param index The position of the attribute in the list.
     * @return The attribute type as a string ("NMTOKEN" for an
     *         enumeration, and "CDATA" if no declaration was
     *         read), or null if there is no attribute at
     *         that position.
     */

    public String getType(int index) {
        int typeCode = getTypeAnnotation(index) & NamePool.FP_MASK;
        switch (typeCode) {
            case StandardNames.XS_ID: return "ID";
            case StandardNames.XS_IDREF: return "IDREF";
            case StandardNames.XS_NMTOKEN: return "NMTOKEN";
            case StandardNames.XS_ENTITY: return "ENTITY";
            case StandardNames.XS_IDREFS: return "IDREFS";
            case StandardNames.XS_NMTOKENS: return "NMTOKENS";
            case StandardNames.XS_ENTITIES: return "ENTITIES";
            default: return "CDATA";
        }
    }

    /**
     * Get the type of an attribute (by name).
     *
     * @param uri       The namespace uri of the attribute.
     * @param localname The local name of the attribute.
     * @return The index position of the attribute
     */

    public String getType(String uri, String localname) {
        int index = findByName(uri, localname);
        return (index < 0 ? null : getType(index));
    }

    /**
     * Get the value of an attribute (by position).
     *
     * @param index The position of the attribute in the list.
     * @return The attribute value as a string, or null if
     *         there is no attribute at that position.
     */

    public String getValue(int index) {
        if (values == null) {
            return null;
        }
        if (index < 0 || index >= used) {
            return null;
        }
        return values[index];
    }

    /**
     * Get the value of an attribute (by name).
     *
     * @param uri       The namespace uri of the attribute.
     * @param localname The local name of the attribute.
     * @return The index position of the attribute
     */

    public String getValue(String uri, String localname) {
        int index = findByName(uri, localname);
        return (index < 0 ? null : getValue(index));
    }

    /**
     * Get the attribute value using its fingerprint
     */

    public String getValueByFingerprint(int fingerprint) {
        int index = findByFingerprint(fingerprint);
        return (index < 0 ? null : getValue(index));
    }

    /**
     * Get the index of an attribute, from its lexical QName
     *
     * @param qname The lexical QName of the attribute. The prefix must match.
     * @return The index position of the attribute
     */

    public int getIndex(String qname) {
        if (codes == null) {
            return -1;
        }
        if (qname.indexOf(':') < 0) {
            return findByName("", qname);
        }
        // Searching using prefix+localname is not recommended, but SAX allows it...
        String[] parts;
        try {
            parts = Name11Checker.getInstance().getQNameParts(qname);
        } catch (QNameException err) {
            return -1;
        }
        String prefix = parts[0];
        if (prefix.length() == 0) {
            return findByName("", qname);
        } else {
            String localName = parts[1];
            for (int i = 0; i < used; i++) {
                String lname = config.getNamePool().getLocalName(getNameCode(i));
                String ppref = config.getNamePool().getPrefix(getNameCode(i));
                if (localName.equals(lname) && prefix.equals(ppref)) {
                    return i;
                }
            }
            return -1;
        }
    }

    /**
     * Get the index of an attribute (by name).
     *
     * @param uri       The namespace uri of the attribute.
     * @param localname The local name of the attribute.
     * @return The index position of the attribute
     */

    public int getIndex(String uri, String localname) {
        return findByName(uri, localname);
    }

    /**
     * Get the index, given the fingerprint.
     * Return -1 if not found.
     */

    public int getIndexByFingerprint(int fingerprint) {
        return findByFingerprint(fingerprint);
    }

    /**
     * Get the type of an attribute (by lexical QName).
     *
     * @param name The lexical QName of the attribute.
     * @return The attribute type as a string (e.g. "NMTOKEN", or
     *  "CDATA" if no declaration was read).
     */

    public String getType(String name) {
        int index = getIndex(name);
        return getType(index);
    }


    /**
     * Get the value of an attribute (by lexical QName).
     *
     * @param name The attribute name (a lexical QName).
     * The prefix must match the prefix originally used. This method is defined in SAX, but is
     * not recommended except where the prefix is null.
     */

    public String getValue(String name) {
        int index = getIndex(name);
        return getValue(index);
    }

    /**
     * Find an attribute by expanded name
     * @param uri the namespace uri
     * @param localName the local name
     * @return the index of the attribute, or -1 if absent
     */

    private int findByName(String uri, String localName) {
        if (config == null) {
            return -1;		// indicates an empty attribute set
        }
        NamePool namePool = config.getNamePool();
        int f = namePool.getFingerprint(uri, localName);
        if (f == -1) {
            return -1;
        }
        return findByFingerprint(f);
    }

    /**
     * Find an attribute by fingerprint
     * @param fingerprint the fingerprint representing the name of the required attribute
     * @return the index of the attribute, or -1 if absent
     */

    private int findByFingerprint(int fingerprint) {
        if (codes == null) {
            return -1;
        }
        for (int i = 0; i < used; i++) {
            if (fingerprint == (codes[(i * RECSIZE)] & NamePool.FP_MASK)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Determine whether a given attribute has the is-ID property set
     */

    public boolean isId(int index) {
        return (codes[index * RECSIZE] & NamePool.FP_MASK) == StandardNames.XML_ID
                || config.getTypeHierarchy().isIdCode(getTypeAnnotation(index));
        //        return getType(index).equals("ID") ||
//                ((getNameCode(index) & NamePool.FP_MASK) == StandardNames.XML_ID);
    }

    /**
     * Determine whether a given attribute has the is-idref property set
     */

    public boolean isIdref(int index) {
        return config.getTypeHierarchy().isIdrefsCode(getTypeAnnotation(index));
    }

    /**
     * Delete the attribute with a given fingerprint
     * @param fingerprint The fingerprint of the attribute to be removed
     */

    public void removeAttribute(int fingerprint) {
        int index = findByFingerprint(fingerprint);
        if (index == -1) {
            // no action
        } else if (index == used-1) {
            used--;
        } else {
            System.arraycopy(values, index+1, values, index, used-index-1);
            System.arraycopy(codes, (index+1)*RECSIZE, codes, index*RECSIZE, (used-index-1)*RECSIZE);
            used--;
        }
    }

    /**
     * Rename an attribute
     * @param oldName the namecode of the existing name
     * @param newName the namecode of the new name
     */

    public void renameAttribute(int oldName, int newName) {
        int index = findByFingerprint(oldName & NamePool.FP_MASK);
        if (index == -1) {
            // no action
        } else {
            codes[index*RECSIZE] = newName;
        }
    }

    /**
     * Replace the value of an attribute
     * @param nameCode the name code of the attribute name
     * @param newValue the new string value of the attribute
     */

    public void replaceAttribute(int nameCode, CharSequence newValue) {
        int index = findByFingerprint(nameCode & NamePool.FP_MASK);
        if (index == -1) {
            // no action
        } else {
            values[index] = newValue.toString();
        }
    }

    /**
     * Set the type annotation of an attribute
     * @param nameCode the name code of the attribute name
     * @param typeCode the new type code for the attribute
     */

    public void setTypeAnnotation(int nameCode, int typeCode) {
        int index = findByFingerprint(nameCode & NamePool.FP_MASK);
        if (index == -1) {
            // no action
        } else {
            codes[index*RECSIZE + TYPECODE] = typeCode;
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
