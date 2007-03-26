package org.orbeon.saxon.type;

/**
 * This exception occurs when an attempt is made to dereference a reference from one
 * schema component to another, if the target of the reference cannot be found. Note that
 * an unresolved reference is not necessarily an error: a schema containing unresolved
 * references may be used for validation, provided the components containing the
 * unresolved references are not actually used.
 */

public abstract class UnresolvedReferenceException extends RuntimeException {

    public UnresolvedReferenceException(String ref) {
        super(ref);
    }
}


