/**
 * Redistribution and use of this software and associated documentation
 * ("Software"), with or without modification, are permitted provided
 * that the following conditions are met:
 *
 * 1. Redistributions of source code must retain copyright
 *    statements and notices.  Redistributions must also contain a
 *    copy of this document.
 *
 * 2. Redistributions in binary form must reproduce the
 *    above copyright notice, this list of conditions and the
 *    following disclaimer in the documentation and/or other
 *    materials provided with the distribution.
 *
 * 3. The name "Exolab" must not be used to endorse or promote
 *    products derived from this Software without prior written
 *    permission of Intalio, Inc.  For written permission,
 *    please contact info@exolab.org.
 *
 * 4. Products derived from this Software may not be called "Exolab"
 *    nor may "Exolab" appear in their names without prior written
 *    permission of Intalio, Inc. Exolab is a registered
 *    trademark of Intalio, Inc.
 *
 * 5. Due credit should be given to the Exolab Project
 *    (http://www.exolab.org/).
 *
 * THIS SOFTWARE IS PROVIDED BY INTALIO, INC. AND CONTRIBUTORS
 * ``AS IS'' AND ANY EXPRESSED OR IMPLIED WARRANTIES, INCLUDING, BUT
 * NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL
 * INTALIO, INC. OR ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * Copyright 1999-2000 (C) Intalio Inc. All Rights Reserved.
 *
 * $Id: SchemaType.java,v 1.2 2005/03/18 20:42:20 dsmall Exp $
 */

package org.orbeon.saxon.type;

import org.orbeon.saxon.om.*;
import org.orbeon.saxon.xpath.XPathException;

import javax.xml.transform.SourceLocator;
import java.io.Serializable;

/**
 * This class represents a simple type or complex type as defined in XML Schema.
 * In the non-schema-aware version of Saxon it is used to represent built-in schema types only.
 * In the schema-aware version it is subclassed to represent user-defined types.
 */

public abstract class SchemaType implements TypeInfo, Serializable, SourceLocator
{
    /**
     * Flags used to implement the "final" and "block" attributes
     */
    protected int finalProhibitions = 0;
    // TODO: First four are identical to the definitions in interface TypeInfo; remove them
    public static final int DERIVATION_RESTRICTION = 1;
    public static final int DERIVATION_EXTENSION = 2;
    public static final int DERIVATION_UNION = 4;
    public static final int DERIVATION_LIST = 8;
    public static final int DERIVE_BY_SUBSTITUTION = 16;

    /**
     * The local name of this schema type
     */
    private String localName = null;

    /**
     * The NamePool fingerprint of the name of the base type. Used for
     * resolving forwards references.
     */

    private int baseTypeFingerprint = -1;

   /**
     * The base schema type
     */
    private SchemaType baseType = null;

    /**
     * The fingerprint of the element or attribute declaration containing
     * this anonymous type definition
     */

    private int containingDeclarationName = -1;

    /**
     * Flag to indicate whether the containing declaration of an anonymous type
     * is an element or attribute
     */

    private boolean containingDeclarationIsElement = true;

    /**
     * The name pool containing this type
    **/
    private NamePool namePool = null;

    /**
     * The derivation method (if any), for example DERIVED_BY_RESTRICTION
     */
    private int derivation = 0;

    /**
     * The fingerprint of this type in the Saxon NamePool
     */

    private int fingerprint = -1;

    /**
     * Loctation information
     */

    private String systemId = null;
    private int lineNumber = -1;

    /**
     * Flag used to check for cycles during validation
     */

    protected int validationPhase = 0;
    public static final int UNVALIDATED = 0;
    public static final int VALIDATING = 1;
    public static final int VALIDATED = 2;
    public static final int INVALID = 3;

    /**
     * Default constructor. For internal use only.
    **/
    public SchemaType() {}

    /**
     * Get the fingerprint of the name of this type
     * @return the fingerprint. Returns -1 for an anonymous type.
     */

    public int getFingerprint() {
        return fingerprint;
    }

    /**
     * Set the fingerprint of the name of this type. For internal use only.
     * @param fingerprint the fingerprint allocated in the namepool for this schema
     */

    public void setFingerprint(int fingerprint) {
        this.fingerprint = fingerprint;
    }

    /**
     * Get the fingerprint of the name of the base type of this type
     * @return the fingerprint. Return -1 for an anonymous type.
     */
    public int getBaseTypeFingerprint() {
        if (baseTypeFingerprint == -1 && baseType != null) {
            baseTypeFingerprint = baseType.getFingerprint();
        }
        return baseTypeFingerprint;
    }

    /**
     * Set the fingerprint of the name of the base type of this type.
     * For internal use only.
     * @param fingerprint the fingerprint allocated in the namepool
     */

    public void setBaseTypeFingerprint(int fingerprint) {
        this.baseTypeFingerprint = fingerprint;
    }

    /**
     * Get the local name of this type
     * @return the local part of the name, or null if the type is anonymous
     */
    public String getLocalName() {
        return localName;
    }

    /**
     * Set the local name of this type
     * @param localName the local name of the type
    **/
    public void setLocalName(String localName) {
        // MHK: we do now allow the name to be changed, as part of <redefine>, and
        // it is the caller's responsibility to update all indexes.
        this.localName = localName;
    }

    /**
     * Get the display name of the type: that is, a lexical QName with an arbitrary prefix
     * @return a lexical QName identifying the type
     */

    public String getDisplayName() {
        return namePool.getDisplayName(fingerprint);
    }

    /**
     * Get the local name of the type (a system-allocated name if anonymous). Needed to implement the
     * DOM level 3 TypeInfo interface.
     */

    public String getTypeName() {
        return namePool.getLocalName(fingerprint);
    }

    /**
     * Get the namespace name of the type (a system-allocated name if anonymous). Needed to implement the
     * DOM level 3 TypeInfo interface.
     */

    public String getTypeNamespace() {
        return namePool.getURI(fingerprint);
    }

    /**
     * Test whether this SchemaType is a complex type
     * @return true if this SchemaType is a complex type
     */

    public final boolean isComplexType() {
        return !isSimpleType();
    }

    /**
     * Test whether this SchemaType is a simple type
     * @return true if this SchemaType is a simple type
     */

    public abstract boolean isSimpleType();

	/**
	 * Returns the value of the 'block' attribute for this type, as a bit-signnificant
     * integer with fields such as {@link #DERIVATION_LIST} and {@link #DERIVATION_EXTENSION}
     * @return the value of the 'block' attribute for this type
	 */

	public int getBlock() {
		return 0;
	}

    /**
     * Get the NamePool in which the name of this type is defined
     */

    public NamePool getNamePool() {
        return namePool;
    }

    /**
     * Set the NamePool in which the type is defined.
     * For internal use only.
     * @param pool the NamePool containing the names in this schema
     */

    public void setNamePool(NamePool pool) {
        namePool = pool;
    }

    /**
     * Returns the base type that this type inherits from.
     * If this type is a Simpletype that is a built in primitive type then null is returned.
     * @return the base type.
    */

    public SchemaType getBaseType() {
        return baseType;
    }

    /**
     * Sets the base type for this datatype. For internal use only.
     * @param baseType the base type which this type inherits from
     */

    public void setBaseType(SchemaType baseType) throws SchemaException {
        this.baseType = baseType;
    }

    /**
     * Gets the integer code of the derivation method used to derive this type from its
     * parent. Returns zero for primitive types.
     * @return a numeric code representing the derivation method, for example {@link #DERIVATION_RESTRICTION}
     */

    public int getDerivationMethod() {
        return derivation;
    }

    /**
     * Determines whether derivation (of a particular kind)
     * from this type is allowed, based on the "final" property
     * @param derivation the kind of derivation, for example {@link #DERIVATION_LIST}
     * @return true if this kind of derivation is allowed
     */

    public boolean allowsDerivation(int derivation) {
        return (finalProhibitions & derivation) == 0;
    }

    /**
     * Set the derivation method name. For internal use only.
     * @param method the derivation method, for example "restriction"
     * @throws SchemaException for an unknown derivation method
     */

    public void setDerivationMethodName(String method) throws SchemaException {
        if (method.equals("restriction")) {
            derivation = DERIVATION_RESTRICTION;
        } else if (method.equals("union")) {
            derivation = DERIVATION_UNION;
        } else if (method.equals("list")) {
            derivation = DERIVATION_LIST;
        } else if (method.equals("extension")) {
            derivation = DERIVATION_EXTENSION;
        } else {
            throw new SchemaException("Unknown derivation method: " + method);
        }
    }

    /**
     * Sets the derivation method code (without validating it)
     * @param method the derivation method as an integer code, for example {@link #DERIVATION_UNION}
     */
    public void setDerivationMethod(int method) {
        derivation = method;
    }

    /**
     * Sets the value of the 'final' property, indicating which
     * types of derivation are not allowed. For internal use only.
     * @param finalValue the value of the final property, as a bit-significant integer
    **/

     public void setFinalProhibitions(int finalValue) {
        finalProhibitions = finalValue;
    }

    /**
     * Check that this type is validly derived from a given type
     * @param type the type from which this type is derived
     * @param block the derivations that are blocked by the relevant element declaration
     * @throws SchemaException if the derivation is not allowed
     */

    public void checkDerivation(SchemaType type, int block) throws SchemaException {
        int derivations = 0;
        SchemaType t = this;
        while (true) {
            if (t == null) {
                if (type instanceof AnyType) {
                    return;
                } else {
                    throw new SchemaException(
                        "The requested type " +
                        getDescription() +
                        " is not derived from the declared type " +
                        type.getDescription());
                }
            }
            if ((derivations & t.getBlock()) != 0) {
                throw new SchemaException(
                        "Derivation of the requested type " +
                        getDescription() +
                        " is blocked by the base type " +
                        t.getDescription());
            }
            if (t == type) {
                break;
            }
            derivations |= t.getDerivationMethod();
            t = t.getBaseType();
        }
        if ((derivations & block) != 0) {
            throw new SchemaException(
                    "Derivation of the requested type " +
                    getDescription() +
                    " is blocked by the element declaration");
        }
    }

    /**
     * This method returns true if there is a derivation between the reference type definition, that is the TypeInfo
     * on which the method is being called, and the other type definition, that is the one passed as parameters.
     * This method implements the DOM Level 3 TypeInfo interface.
     * @param typeNamespaceArg the namespace of the "other" type
     * @param typeNameArg the local name of the "other" type
     * @param derivationMethod the derivation method: zero or more of {@link TypeInfo#DERIVATION_RESTRICTION},
     * {@link TypeInfo#DERIVATION_EXTENSION}, {@link TypeInfo#DERIVATION_LIST}, or {@link TypeInfo#DERIVATION_UNION}.
     * Zero means derived by any possible route.
     */

    public boolean isDerivedFrom(String typeNamespaceArg,
                                 String typeNameArg,
                                 int derivationMethod) {
        SchemaType base = getBaseType();
        if (derivationMethod==0 || (derivationMethod & getDerivationMethod()) != 0) {
            if (base.getTypeName().equals(typeNameArg) &&
                    base.getTypeNamespace().equals(typeNamespaceArg)) {
                return true;
            } else if (base instanceof AnyType) {
                return false;
            } else {
                return base.isDerivedFrom(typeNamespaceArg, typeNameArg, derivationMethod);
            }
        }
        return false;
        // TODO: if derivationMethod is RESTRICTION, this interpretation requires every step to be derived
        // by restriction. An alternative interpretation is that at least one step must be derived by restriction.
    }



    /**
     * Get the typed value of a node that is annotated with this schema type
     * @param node the node whose typed value is required
     * @return a SequenceIterator over the atomic values making up the typed value of the specified
     * node. The objects returned by this iterator are of type {@link org.orbeon.saxon.value.AtomicValue}
     */

    public abstract SequenceIterator getTypedValue(NodeInfo node)
            throws XPathException;


    /**
     * Set the name of the containing declaration (for diagnostics)
     * @param fingerprint The fingerprint of the element or attribute declaration "owning" this anonymous
     * type definition
     * @param isElement True if the owning declaration is an element declaration, false if it is an
     * attribute declaration
     */

    public void setContainingDeclaration(int fingerprint, boolean isElement) {
        containingDeclarationName = fingerprint;
        containingDeclarationIsElement = isElement;
    }

    /**
     * Set location information for diagnostics
     * @param locator information about the location of the type definition within a schema document
     */

    public void setLocator(SourceLocator locator) {
        systemId = locator.getSystemId();
        lineNumber = locator.getLineNumber();
    }

    /**
     * Set the system identifier (URI) of the schema document in which this type is defined
     * @param systemId the URI of the schema document
     */

    public void setSystemId(String systemId) {
        this.systemId = systemId;
    }

    /**
     * Set the line number at which the type is defined within a schema document
     * @param lineNumber the line where the definition appears
     */

    public void setLineNumber(int lineNumber) {
        this.lineNumber = lineNumber;
    }

    /**
     * Get the system ID (URI) of the schema document where the type was defined
     * @return the system ID (or URI) of the containing schema document
     */

    public String getSystemId() {
        return systemId;
    }

    /**
     * Get the line number of the location where the type was defined
     * @return the line number
     */

    public int getLineNumber() {
        return lineNumber;
    }

    /**
     * Get the public ID of the location where the type was defined.
     * This is always null.
     * @return null
     */

    public String getPublicId() {
        return null;
    }

    /**
     * Get the column number of the location where the type was defined.
     * This is always -1, meaning unknown.
     * @return -1
     */

    public int getColumnNumber() {
        return -1;
    }

    /**
     * Determine the relationship of this schema type to another schema type.
     * @param other the other schema type
     * @return {@link Type#SAME_TYPE} if the types are the same; {@link Type#SUBSUMES} if the first
     * type subsumes the second (that is, all instances of the second type are also instances
     * of the first); {@link Type#SUBSUMED_BY} if the second type subsumes the first;
     * {@link Type#OVERLAPS} if the two types overlap (have a non-empty intersection);
     * {@link Type#DISJOINT} if the two types are disjoint (have an empty intersection)
     */

    public abstract int relationshipTo(SchemaType other);


    /**
     * Get a description of this type for use in diagnostics. In the case of a named type, this is the
     * same as the display name. In the case of a type known to be defined immediately within an element
     * or attribute declaration, it is a phrase that identifies the containing declaration. In other cases,
     * it is a phrase of the form "defined at line L of URI". The description is designed to be inserted
     * in a context such as "the type X is ..."
     */

    public String getDescription() {
        int fp = getFingerprint();
        NamePool pool = getNamePool();
        if (fp != -1 && pool.getURI(fp) != NamespaceConstant.ANONYMOUS) {
            return pool.getDisplayName(fp);
        }
        if (containingDeclarationName != -1) {
            return "of " +
                    (containingDeclarationIsElement ? "element " : "attribute ") +
                    pool.getDisplayName(containingDeclarationName);
        }
        return "defined at line " + getLineNumber() + " of " +
                getSystemId();
    }

    /**
     * Get the name of the containing element or attribute declaration, if any (and if known)
     * @return the name of the containing declaration, as a NamePool fingerprint.
     */

    public int getContainingDeclarationName() {
        return containingDeclarationName;
    }

    /**
     * Determine whether the containing declaration is an element declaration or an attribute declaration
     * @return true if the containin declaration is an element declaration, false if it is an attribute
     */

    public boolean containingDeclarationIsElement() {
        return containingDeclarationIsElement;
    }

    /**
     * Test whether this is the same type as another type. They are considered to be the same type
     * if they are derived from the same type definition in the original XML representation (which
     * can happen when there are multiple includes of the same file)
     */

    public boolean isSameType(SchemaType other) {
        if (this == other) return true;
        return getFingerprint() == other.getFingerprint() &&
                getLineNumber() == other.getLineNumber() &&
                getColumnNumber() == other.getColumnNumber() &&
                getSystemId().equals(other.getSystemId());

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
// The Initial Developer of the Original Code is Saxonica Limited
//
// Portions created by (your name) are Copyright (C) (your legal entity). All Rights Reserved.
//
// Contributor(s): none
//