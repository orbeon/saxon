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
 * $Id: AtomicType.java,v 1.2 2005/03/18 20:42:20 dsmall Exp $
 */

package org.orbeon.saxon.type;

import org.orbeon.saxon.Err;
import org.orbeon.saxon.om.*;
import org.orbeon.saxon.style.StandardNames;
import org.orbeon.saxon.value.AtomicValue;
import org.orbeon.saxon.value.DerivedAtomicValue;
import org.orbeon.saxon.value.QNameValue;
import org.orbeon.saxon.value.StringValue;
import org.orbeon.saxon.xpath.XPathException;

import java.util.Iterator;

/**
 * An object belonging to this class represents an atomic type: either a built-in
 * atomic type, or a user-defined atomic type. There is at most one AtomicType
 * object for each distinct type in the schema (so equality can safely be tested
 * using the == operator).
 * @author <a href="mailto:berry@intalio.com">Arnaud Berry</a>
 * @version $Revision:
**/
public class AtomicType extends SimpleType implements ItemType
{

    private boolean builtIn = false;

    public AtomicType() {}

    /**
     * Mark this as a built-in type
     */

    public void setIsBuiltIn(boolean yesOrNo) {
        builtIn = yesOrNo;
    }

    /**
     * Test whether this is a built-in type
     */

    public boolean isBuiltIn() {
        return builtIn;
    }

   /**
     * Checks the validity of this SimpleType definition.
     *
     * @throws SchemaException when this SimpleType definition
     * is invalid.
    **/
    public boolean validate(SchemaMarker schema) throws SchemaException {
       if (!super.validate(schema)) {
           return false;
       }
       int prim = getPrimitiveType();
       String[] allowed = {};
       // See XML Schema Part 2 section 4.1.5
       switch (prim) {
           case Type.STRING:
           case Type.HEX_BINARY:
           case Type.BASE64_BINARY:
           case Type.ANY_URI:
           case Type.QNAME:
           case Type.NOTATION:
               String[] f1 = {Facet.LENGTH, Facet.MIN_LENGTH, Facet.MAX_LENGTH, Facet.PATTERN,
                              Facet.ENUMERATION, Facet.WHITESPACE};
               allowed = f1;
               break;
           case Type.BOOLEAN:
               String[] f2 = {Facet.PATTERN, Facet.WHITESPACE};
               allowed = f2;
               break;
           case Type.FLOAT:
           case Type.DOUBLE:
           case Type.DURATION:
           case Type.DATE_TIME:
           case Type.TIME:
           case Type.DATE:
           case Type.G_YEAR_MONTH:
           case Type.G_YEAR:
           case Type.G_MONTH:
           case Type.G_MONTH_DAY:
           case Type.G_DAY:
               String[] f3 = {Facet.PATTERN, Facet.ENUMERATION, Facet.WHITESPACE,
                              Facet.MIN_INCLUSIVE, Facet.MAX_INCLUSIVE, Facet.MIN_EXCLUSIVE, Facet.MAX_EXCLUSIVE };
               allowed = f3;
               break;
           case Type.DECIMAL:
           case Type.INTEGER:
               String[] f4 = {Facet.TOTALDIGITS, Facet.FRACTIONDIGITS,
                              Facet.PATTERN, Facet.ENUMERATION, Facet.WHITESPACE,
                              Facet.MIN_INCLUSIVE, Facet.MAX_INCLUSIVE, Facet.MIN_EXCLUSIVE, Facet.MAX_EXCLUSIVE };
               allowed = f4;
               break;
       }
       Iterator fit = getFacets();
       while (fit.hasNext()) {
           Facet fac = (Facet)fit.next();
           boolean found = false;
           String name = fac.getName();
           if (name.equals("enumerationSet")) {
               name = Facet.ENUMERATION;
           }
           for (int i=0; i<allowed.length; i++) {
               if (name.equals(allowed[i])) {
                   found = true;
                   break;
               }
           }
           if (!found) {
               schema.error("The " + fac.getName() + " facet is not applicable to types derived from " +
                       getNamePool().getDisplayName(prim), this);
               return false;
           }
       }

       return true;
   }

    /**
     * Test whether a given item conforms to this type
     * @param item The item to be tested
     * @return true if the item is an instance of this type; false otherwise
    */

    public boolean matchesItem(Item item) {
        if (item instanceof AtomicValue) {
            AtomicValue value = (AtomicValue)item;
            AtomicType type = (AtomicType)value.getItemType();
            if (type.getFingerprint()==this.getFingerprint()) {
                // note, with compiled stylesheets one can have two objects representing
                // the same type, so comparing identity is not safe
                return true;
            }
            return Type.isSubType(type, this);
        } else {
            return false;
        }
    }

    /**
     * Check whether a given input string is valid according to this SimpleType
     * @param value the input string to be checked
     * @param nsResolver a namespace resolver used to resolve namespace prefixes if the type
     * is namespace sensitive. The value supplied may be null; in this case any namespace-sensitive
     * content will throw an UnsupportedOperationException.
     * @throws ValidationException if the content is invalid
     * @throws UnsupportedOperationException if the type is namespace-sensitive and no namespace
     * resolver is supplied
     */

    public void validateContent(CharSequence value, NamespaceResolver nsResolver)
    throws ValidationException {
        int f = getFingerprint();
        if (f==StandardNames.XS_STRING ||
                f==StandardNames.XS_ANY_SIMPLE_TYPE ||
                f==StandardNames.XDT_UNTYPED_ATOMIC) {
            return;
        }
        if (isNamespaceSensitive()) {
            if (nsResolver == null) {
                throw new UnsupportedOperationException("Cannot validate a QName without a namespace resolver");
            }
            QNameValue qname;
            try {
                String[] parts = Name.getQNameParts(value.toString());
                String uri = nsResolver.getURIForPrefix(parts[0], true);
                if (uri == null) {
                    throw new ValidationException("Namespace prefix " + Err.wrap(parts[0]) +
                            " has not been declared");
                }
                qname = new QNameValue(parts[0], uri, parts[1]);
                if (!isBuiltIn()) {
                    String lexicalValue = value.toString();
                    DerivedAtomicValue.makeValue(qname, lexicalValue, this, true);
                }
            } catch (QNameException err) {
                throw new ValidationException("Invalid lexical QName " + Err.wrap(value));
            } catch (XPathException err) {
                throw new ValidationException(err.getMessage());
            }
        } else {
            try {
                new StringValue(value).convert(this, null);
            } catch (XPathException err) {
                throw new ValidationException(err.getMessage());
            }
        }
    }

    /**
     * Get the typed value corresponding to a given string value, assuming it is
     * valid against this type
     * @param value the string value
     * @param resolver
     * @return an iterator over the atomic sequence comprising the typed value
     */

    public SequenceIterator getTypedValue(CharSequence value, NamespaceResolver resolver)
            throws ValidationException {
        if (isNamespaceSensitive()) {
            try {
                String[] parts = Name.getQNameParts(value.toString());
                String uri = resolver.getURIForPrefix(parts[0], true);
                if (uri == null) {
                    throw new ValidationException("No namespace binding for prefix in QName value " + Err.wrap(value));
                }
                QNameValue qname = new QNameValue(parts[0], uri, parts[1]);
                return SingletonIterator.makeIterator(qname);

            } catch (QNameException err) {
                throw new ValidationException("Malformed QName in content: " + Err.wrap(value));
            } catch (XPathException err) {
                throw new ValidationException("Malformed QName in content: " + Err.wrap(value));
            }
        }
        try {
            return SingletonIterator.makeIterator(new StringValue(value).convert(this, null));
        } catch (XPathException err) {
            throw new ValidationException(err.getMessage());
        }
    }

    /**
     * Get the type from which this item type is derived by restriction. This
     * is the supertype in the XPath type heirarchy, as distinct from the Schema
     * base type: this means that the supertype of xs:boolean is xdt:anyAtomicType,
     * whose supertype is item() (rather than xs:anySimpleType).
     * @return the supertype, or null if this type is item()
     */

    public ItemType getSuperType() {
        if (this == Type.ANY_ATOMIC_TYPE || getFingerprint() == Type.ANY_SIMPLE_TYPE) {
                                // the latter case arises only with the schema for schemas
            return AnyItemType.getInstance();
        } else {
            SchemaType base = getBaseType();
            return (ItemType)base;
        }
    }

    /**
      * Get the primitive type from which this type is derived. For the definition
      * of primitive types, see {@link Type#isPrimitiveType}
      * @return the type code of the primitive type
      */
    
     public ItemType getPrimitiveItemType() {
        if (Type.isPrimitiveType(getFingerprint())) {
             return this;
         } else {
             ItemType s = getSuperType();
             if (s instanceof AtomicType) {
                 return s.getPrimitiveItemType();
             } else {
                 return this;
             }
         }
     }


    /**
     * Get the primitive type from which this type is derived. For the definition
     * of primitive types, see {@link Type#isPrimitiveType}
     * @return the type code of the primitive type
     */
    public int getPrimitiveType() {
        int x = getFingerprint();
        if (Type.isPrimitiveType(x)) {
            return x;
        } else {
            ItemType s = getSuperType();
            if (s instanceof AtomicType) {
                return s.getPrimitiveType();
            } else {
                return this.getFingerprint();
            }
        }
    }

    /**
     * Get the item type of the atomic values that will be produced when an item
     * of this type is atomized
     */

    public AtomicType getAtomizedItemType() {
        return this;
    }

    /**
     * Produce string representation for use in diagnostic output
     */

    public String toString() {
        int f = getFingerprint();
        if (f < 0) {
            throw new IllegalArgumentException("fingerprint < 0");
        }
        if (f < 1023) {
            return StandardNames.getDisplayName(f);
        } else {
            return getNamePool().getDisplayName(f);
        }
    }

    /**
     * Display the type descriptor for diagnostics
     */

    public String toString(NamePool pool) {
        return toString();
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

    public int relationshipTo(SchemaType other) {
        if (other instanceof AnyType) {
            return Type.SUBSUMED_BY;
        } else if (other instanceof AtomicType) {
            return Type.relationship(this, (AtomicType)other);
        } else if (other instanceof SimpleType) {
            return Type.inverseRelationship(other.relationshipTo(this));
        } else {
            return Type.DISJOINT;
        }
    }


    /**
     * Returns a hash code value for the object.
     */

    public int hashCode() {
        return getFingerprint();
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