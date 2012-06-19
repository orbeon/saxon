package org.orbeon.saxon.type;

import org.orbeon.saxon.sort.IntHashSet;


/**
 * A complex type as defined in XML Schema: either a user-defined complex type, or xs:anyType, or xs:untyped.
 * In the non-schema-aware version of the Saxon product, the only complex type encountered is xs:untyped.
 */

public interface ComplexType extends SchemaType {

    /**
     * Test whether this complex type has been marked as abstract. This corresponds to
     * the {abstract} property in the schema component model.
     *
     * @return true if this complex type is abstract.
     */

    public boolean isAbstract();

    /**
	 * Test whether this complex type has complex content. This represents one aspect of the
     * {content type} property in the schema component model.
     *
	 * @return true if this complex type has a complex content model, false if it has a simple content model
	 */

    public boolean isComplexContent();

    /**
	 * Test whether this complexType has simple content. This represents one aspect of the
     * {content type} property in the schema component model.
     *
	 * @return true if this complex type has a simple content model, false if it has a complex content model
	 */

    public boolean isSimpleContent();

    /**
     * Test whether this complex type has "all" content, that is, a content model
     * using an xs:all compositor
     * @return true if the type has an "all" content model
     */

    public boolean isAllContent();

    /**
     * Get the simple content type. This represents one aspect of the
     * {content type} property in the schema component model.
     *
     * @return For a complex type with simple content, returns the simple type of the content.
     * Otherwise, returns null.
     */

    public SimpleType getSimpleContentType();

    /**
	 * Test whether this complex type is derived by restriction. This corresponds to one
     * aspect of the {derivation method} property in the schema component model.
     *
	 * @return true if this complex type is derived by restriction
	 */

    public boolean isRestricted();

    /**
     * Test whether the content model of this complex type is empty. This represents one aspect of the
     * {content type} property in the schema component model.
     *
     * @return true if the content model is defined as empty
     */

    public boolean isEmptyContent();

    /**
     * Test whether the content model of this complex type allows empty content. This property applies only if
     * this is a complex type with complex content.
     *
     * @return true if empty content is valid
     */

    public boolean isEmptiable() throws SchemaException;

    /**
     * Test whether this complex type allows mixed content. This represents one aspect of the
     * {content type} property in the schema component model. This property applies only if
     * this is a complex type with complex content.
     *
     * @return true if mixed content is allowed
     */

    public boolean isMixedContent();

    /**
     * Test whether this complex type subsumes another complex type. The algorithm
     * used is as published by Thompson and Tobin, XML Europe 2003.
     * @param sub the other type (the type that is derived by restriction, validly or otherwise)
     * @param compiler used for error reporting
     * @return null indicating that this type does indeed subsume the other; or a string indicating
     * why it doesn't.
     */

    //public String subsumes(ComplexType sub, ISchemaCompiler compiler) throws SchemaException;

    /**
     * Find an element particle within this complex type definition having a given element name
     * (identified by fingerprint), and return the schema type associated with that element particle.
     * If there is no such particle, return null. If the fingerprint matches an element wildcard,
     * return the type of the global element declaration with the given name if one exists, or AnyType
     * if none exists and lax validation is permitted by the wildcard.
     * @param fingerprint Identifies the name of the child element within this content model
     * @param considerExtensions
     * @return the schema type associated with the child element particle with the given name.
     * If there is no such particle, return null.
     */

    public SchemaType getElementParticleType(int fingerprint, boolean considerExtensions) throws SchemaException, ValidationException;

    /**
     * Find an element particle within this complex type definition having a given element name
     * (identified by fingerprint), and return the cardinality associated with that element particle,
     * that is, the number of times the element can occur within this complex type. The value is one of
     * {@link org.orbeon.saxon.expr.StaticProperty#EXACTLY_ONE}, {@link org.orbeon.saxon.expr.StaticProperty#ALLOWS_ZERO_OR_ONE},
     * {@link org.orbeon.saxon.expr.StaticProperty#ALLOWS_ZERO_OR_MORE}, {@link org.orbeon.saxon.expr.StaticProperty#ALLOWS_ONE_OR_MORE},
     * If there is no such particle, return {@link org.orbeon.saxon.expr.StaticProperty#EMPTY}.
     * @param fingerprint Identifies the name of the child element within this content model
     * @param searchExtensionTypes
     * @return the cardinality associated with the child element particle with the given name.
     * If there is no such particle, return {@link org.orbeon.saxon.expr.StaticProperty#EMPTY}.
     */

    public int getElementParticleCardinality(int fingerprint, boolean searchExtensionTypes) throws SchemaException, ValidationException;

    /**
     * Find an attribute use within this complex type definition having a given attribute name
     * (identified by fingerprint), and return the schema type associated with that attribute.
     * If there is no such attribute use, return null. If the fingerprint matches an attribute wildcard,
     * return the type of the global attribute declaration with the given name if one exists, or AnySimpleType
     * if none exists and lax validation is permitted by the wildcard.
     * <p>
     * If there are types derived from this type by extension, search those too.
     * @param fingerprint Identifies the name of the child element within this content model
     * @return the schema type associated with the attribute use identified by the fingerprint.
     * If there is no such attribute use, return null.
     */

    public SchemaType getAttributeUseType(int fingerprint) throws SchemaException, ValidationException;

    /**
     * Return true if this type (or any known type derived from it by extension) allows the element
     * to have one or more attributes.
     * @return true if attributes are allowed
     */

    public boolean allowsAttributes();

   /**
     * Get a list of all the names of elements that can appear as children of an element having this
     * complex type, as integer fingerprints. If the list is unbounded (because of wildcards or the use
     * of xs:anyType), return null.
     * @param children an integer set, initially empty, which on return will hold the fingerprints of all permitted
     * child elements; if the result contains the value -1, this indicates that it is not possible to enumerate
     * all the children, typically because of wildcards. In this case the other contents of the set should
     * be ignored.
     */

    public void gatherAllPermittedChildren(IntHashSet children) throws SchemaException;

   /**
     * Get a list of all the names of elements that can appear as descendants of an element having this
     * complex type, as integer fingerprints. If the list is unbounded (because of wildcards or the use
     * of xs:anyType), include a -1 in the result.
     * @param descendants an integer set, initially empty, which on return will hold the fingerprints of all permitted
     * descendant elements; if the result contains the value -1, this indicates that it is not possible to enumerate
     * all the descendants, typically because of wildcards. In this case the other contents of the set should
     * be ignored.
     */

    public void gatherAllPermittedDescendants(IntHashSet descendants) throws SchemaException;

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