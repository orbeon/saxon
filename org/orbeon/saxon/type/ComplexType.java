package net.sf.saxon.type;

/**
 * A complex type as defined in XML Schema: either a user-defined complex type, or xs:anyType.
 * In the non-schema-aware version of the Saxon product, the only complex type encountered is xs:anyType.
 */

public interface ComplexType extends SchemaType {

    /**
     * Test whether this complex type has been marked as abstract.
     * @return true if this complex type is abstract.
    **/

    public boolean isAbstract();

    /**
	 * Test whether this complex type has complex content
	 * @return true if this complex type has a complex content model, false if it has a simple content model
	 */

    public boolean isComplexContent();

    /**
	 * Test whether this complexType has simple content
	 * @return true if this complex type has a simple content model, false if it has a complex content model
	 */

    public boolean isSimpleContent();

    /**
     * Test whether this complex type has "all" content, that is, a content model
     * using an xs:all compositor
     */

    public boolean isAllContent();

    /**
     * Get the simple content type
     * @return For a complex type with simple content, returns the simple type of the content.
     * Otherwise, returns null.
     */

    public SimpleType getSimpleContentType() throws ValidationException;

    /**
	 * Test whether this complex type is derived by restriction
	 * @return true if this complex type is derived by restriction
	 */

    public boolean isRestricted();

    /**
     * Test whether the content model of this complex type is empty
     * @return true if the content model is defined as empty
     */

    public boolean isEmptyContent();

    /**
     * Test whether the content model of this complex type allows empty content
     * @return true if empty content is valid
     */

    public boolean isEmptiable() throws SchemaException, ValidationException;

    /**
     * Test whether this complex type allows mixed content
     * @return true if mixed content is allowed
     */

    public boolean isMixedContent();

    /**
     * Test whether this complex type subsumes another complex type. The algorithm
     * used is as published by Thompson and Tobin, XML Europe 2003.
     * @param sub the other type (the type that is derived by restriction, validly or otherwise)
     * @return null indicating that this type does indeed subsume the other; or a string indicating
     * why it doesn't.
     */

    public String subsumes(ComplexType sub) throws ValidationException;

    /**
     * Find an element particle within this complex type definition having a given element name
     * (identified by fingerprint), and return the schema type associated with that element particle.
     * If there is no such particle, return null. If the fingerprint matches an element wildcard,
     * return the type of the global element declaration with the given name if one exists, or AnyType
     * if none exists and lax validation is permitted by the wildcard.
     * @param fingerprint Identifies the name of the child element within this content model
     */

    public SchemaType getElementParticleType(int fingerprint) throws SchemaException, ValidationException;

    /**
     * Find an element particle within this complex type definition having a given element name
     * (identified by fingerprint), and return the cardinality associated with that element particle,
     * that is, the number of times the element can occur within this complex type. The value is one of
     * {@link net.sf.saxon.expr.StaticProperty.EXACTLY_ONE}, {@link net.sf.saxon.expr.StaticProperty.ALLOWS_ZERO_OR_ONE},
     * {@link net.sf.saxon.expr.StaticProperty.ALLOWS_ZERO_OR_MORE}, {@link net.sf.saxon.expr.StaticProperty.ALLOWS_ONE_OR_MORE},
     * If there is no such particle, return zero.
     * @param fingerprint Identifies the name of the child element within this content model
     */

    public int getElementParticleCardinality(int fingerprint) throws SchemaException, ValidationException;    

    /**
     * Find an attribute use within this complex type definition having a given attribute name
     * (identified by fingerprint), and return the schema type associated with that attribute.
     * If there is no such attribute use, return null. If the fingerprint matches an attribute wildcard,
     * return the type of the global attribute declaration with the given name if one exists, or AnySimpleType
     * if none exists and lax validation is permitted by the wildcard.
     * @param fingerprint Identifies the name of the child element within this content model
     */

    public SchemaType getAttributeUseType(int fingerprint) throws SchemaException, ValidationException;

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
// The Initial Developer of the Original Code is Saxonica Limited
//
// Portions created by (your name) are Copyright (C) (your legal entity). All Rights Reserved.
//
// Contributor(s): none
//