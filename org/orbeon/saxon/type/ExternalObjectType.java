package net.sf.saxon.type;

import net.sf.saxon.om.Item;
import net.sf.saxon.om.NamespaceResolver;
import net.sf.saxon.style.StandardNames;
import net.sf.saxon.value.ObjectValue;

/**
 * This class represents the type of an external Java object returned by
 * an extension function.
 */
public class ExternalObjectType extends BuiltInAtomicType {

    private Class javaClass;

    public ExternalObjectType(Class javaClass) {
        this.javaClass = javaClass;
        this.fingerprint = StandardNames.SAXON_JAVA_LANG_OBJECT;
    }

    public Class getJavaClass() {
        return javaClass;
    }

    public boolean isBuiltIn() {
        return true;
    }

    public boolean matchesItem(Item item) {
        if (item instanceof ObjectValue) {
            Object obj = ((ObjectValue)item).getObject();
            return javaClass.isAssignableFrom(obj.getClass());
        }
        return false;
    }

    /**
     * Check whether a given input string is valid according to this SimpleType
     * @param value the input string to be checked
     * @param nsResolver a namespace resolver used to resolve namespace prefixes if the type
     * is namespace sensitive.
     * @throws ValidationException if the content is invalid. This implementation of the method always throws a
     * ValidationException, because external objects cannot be stored in nodes and cannot be validated
     */

    public void validateContent(CharSequence value, NamespaceResolver nsResolver) throws ValidationException {
        throw new ValidationException("Cannot use an external object type for validation");
    }

    public ItemType getSuperType() {
        // TODO: reflect the Java class hierarchy, to give better type checking
        return Type.ANY_ATOMIC_TYPE;
    }

    public int getFingerprint() {
        return StandardNames.SAXON_JAVA_LANG_OBJECT;
    }

    public String toString() {
        String name = javaClass.getName();
        name = name.replace('$', '-');
        return "java:" + name;
    }

    public String getDisplayName() {
        return toString();
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