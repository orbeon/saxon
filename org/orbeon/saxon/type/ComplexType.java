package net.sf.saxon.type;

/**
 * A complex type as defined in XML Schema: either a user-defined complex type, or xs:anyType.
 * In the non-schema-aware version of the Saxon product, the only complex type encountered is xs:anyType.
 */

public interface ComplexType {

    /**
     * The base type that this type is derived from.
     * @return the base type
     */
    public SchemaType getBaseType();

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

    public SimpleType getSimpleContentType();

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

    public boolean isEmptiable() throws SchemaException;

    /**
     * Test whether this complex type allows mixed content
     * @return true if mixed content is allowed
     */

    public boolean isMixedContent();

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