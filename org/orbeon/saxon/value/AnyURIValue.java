package org.orbeon.saxon.value;
import org.orbeon.saxon.expr.XPathContext;
import org.orbeon.saxon.functions.EscapeURI;
import org.orbeon.saxon.om.StandardNames;
import org.orbeon.saxon.sort.LRUCache;
import org.orbeon.saxon.type.AtomicType;
import org.orbeon.saxon.type.BuiltInAtomicType;
import org.orbeon.saxon.type.ConversionResult;
import org.orbeon.saxon.type.ValidationFailure;

import java.net.URI;
import java.net.URISyntaxException;


/**
* An XPath value of type xs:anyURI.
 *
 * <p>This is implemented as a subtype of StringValue even though xs:anyURI is not a subtype of
 * xs:string in the XPath type hierarchy. This enables type promotion from URI to String to happen
 * automatically in most cases where it is appropriate.</p>
 *
 * <p>This implementation of xs:anyURI allows any string to be contained in the value space. It is possible
 * to validate that the string is a "valid URI" in the sense of XML Schema Part 2 (which refers to the XLink
 * specification and to RFC 2396); however, this validation is optional, and is not carried out by default.
 * In particular, there is no constraint that namespace URIs, collation URIs, and the like should be valid
 * URIs. However, casting from strings to xs:anyURI does invoke validation.</p>
*/

public final class AnyURIValue extends StringValue {

    public static final AnyURIValue EMPTY_URI = new AnyURIValue("");

    /**
     * To prevent repeated validation of commonly used URIs (especially namespaces)
     * we keep a small cache. This is especially useful in the case of URIs that are
     * valid only after escaping, as otherwise an exception occurs during the validation process
     */

    private static ThreadLocal caches = new ThreadLocal();
    //private static LRUCache cache = new LRUCache(20);

    /**
    * Constructor
    * @param value the String value. Null is taken as equivalent to "". This constructor
    * does not check that the value is a valid anyURI instance.
    */

    public AnyURIValue(CharSequence value) {
        this.value = (value==null ? "" : Whitespace.collapseWhitespace(value).toString());
        typeLabel = BuiltInAtomicType.ANY_URI;
    }

    /**
     * Constructor for a user-defined subtype of anyURI
     * @param value the String value. Null is taken as equivalent to "".
     * @param type a user-defined subtype of anyURI. It is the caller's responsibility
     * to ensure that this is actually a subtype of anyURI, and that the value conforms
     * to the definition of this type.
     */

     public AnyURIValue(CharSequence value, AtomicType type) {
         this.value = (value==null ? "" : Whitespace.collapseWhitespace(value).toString());
         typeLabel = type;
     }


    /**
     * Create a copy of this atomic value, with a different type label
     *
     * @param typeLabel the type label of the new copy. The caller is responsible for checking that
     *                  the value actually conforms to this type.
     */

    public AtomicValue copyAsSubType(AtomicType typeLabel) {
        AnyURIValue v = new AnyURIValue(value);
        v.length = length;
        v.typeLabel = typeLabel;
        return v;
    }

    /**
     * Check whether a string consititutes a valid URI
     * @param value the string to be tested
     * @return true if the string is a valid URI
     */

    public static boolean isValidURI(CharSequence value) {

        LRUCache cache = (LRUCache)caches.get();
        if (cache == null) {
            cache = new LRUCache(10);
            caches.set(cache);
        }

        if (cache.get(value) != null) {
            return true;
        }

        String sv = Whitespace.trim(value);

        // Allow zero-length strings (RFC2396 is ambivalent on this point)
        if (sv.length() == 0) {
            return true;
        }

        // Allow a string if the java.net.URI class accepts it
        try {
            new URI(sv);
            cache.put(value, value);
            return true;
        } catch (URISyntaxException e) {
            // keep trying
            // Note: it's expensive to throw exceptions on a success path, so we keep a cache.
        }

        // Allow a string if it can be escaped into a form that java.net.URI accepts
        sv = EscapeURI.iriToUri(sv).toString();
        try {
            new URI(sv);
            cache.put(value, value);
            return true;
        } catch (URISyntaxException e) {
            return false;
        }
    }

    public BuiltInAtomicType getPrimitiveType() {
        return BuiltInAtomicType.ANY_URI;
    }

    /**
    * Convert to target data type
     * @param requiredType integer code representing the item type required
     * @param context the XPath dynamic evaluation context
     * @return the result of the conversion, or an ErrorValue
    */

    public ConversionResult convertPrimitive(BuiltInAtomicType requiredType, boolean validate, XPathContext context) {
        int req = requiredType.getPrimitiveType();
        switch(req) {
        case StandardNames.XS_ANY_ATOMIC_TYPE:
        case StandardNames.XS_ANY_URI:
            return this;
        case StandardNames.XS_UNTYPED_ATOMIC:
            return new UntypedAtomicValue(value);
        case StandardNames.XS_STRING:
            return new StringValue(value);
        case StandardNames.XS_NORMALIZED_STRING:
        case StandardNames.XS_TOKEN:
        case StandardNames.XS_LANGUAGE:
        case StandardNames.XS_NAME:
        case StandardNames.XS_NCNAME:
        case StandardNames.XS_ID:
        case StandardNames.XS_IDREF:
        case StandardNames.XS_ENTITY:
        case StandardNames.XS_NMTOKEN:
            return makeRestrictedString(value, requiredType,
                    (validate ? context.getConfiguration().getNameChecker() : null));

        default:
            ValidationFailure err = new ValidationFailure("Cannot convert anyURI to " +
                                     requiredType.getDisplayName());
            err.setErrorCode("XPTY0004");
            return err;
        }
    }

//    public static void main(String[] args) {
//        ExecutorService executor = Executors.newFixedThreadPool(10);
//        for (int i = 0; i < 100; i++) {
//            executor.execute(new Runnable() {
//                public void run() {
//                    for (int i=0; i<1000000; i++) {
//                        String uri = "http://a.com/aaa" + i;
//                        boolean b = AnyURIValue.isValidURI(uri);
//                        if (i % 1000 == 0) {
//                            System.err.println("Memory: " + (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()));
//                        }
//                    }
//                }
//            });
//        }
//    }

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

