package net.sf.saxon.dom;
import org.w3c.dom.DOMException;

/**
 *  DOM operations only raise exceptions in "exceptional" circumstances,
 * i.e., when an operation is impossible to perform (either for logical
 * reasons, because data is lost, or  because the implementation has become
 * unstable). In general, DOM methods return specific error values in ordinary
 *  processing situations, such as out-of-bound errors when using
 * <code>NodeList</code> .
 * <p> Implementations may raise other exceptions under other circumstances.
 * For example, implementations may raise an implementation-dependent
 * exception if a <code>null</code> argument is passed.
 * <p>See also the <a href='http://www.w3.org/TR/2000/CR-DOM-Level-2-20000510'>Document Object Model (DOM) Level 2 Specification</a>.
 */
public class DOMExceptionImpl extends DOMException {

    public DOMExceptionImpl (short code, String message) {
       super(code, message);
       //this.code = code;
    }
    public short   code;
    // ExceptionCode
//    public static final short INDEX_SIZE_ERR            = 1;
//    public static final short DOMSTRING_SIZE_ERR        = 2;
//    public static final short HIERARCHY_REQUEST_ERR     = 3;
//    public static final short WRONG_DOCUMENT_ERR        = 4;
//    public static final short INVALID_CHARACTER_ERR     = 5;
//    public static final short NO_DATA_ALLOWED_ERR       = 6;
//    public static final short NO_MODIFICATION_ALLOWED_ERR = 7;
//    public static final short NOT_FOUND_ERR             = 8;
//    public static final short NOT_SUPPORTED_ERR         = 9;
//    public static final short INUSE_ATTRIBUTE_ERR       = 10;
    /**
     * @since DOM Level 2
     */
    public static final short INVALID_STATE_ERR         = 11;
    /**
     * @since DOM Level 2
     */
    public static final short SYNTAX_ERR                = 12;
    /**
     * @since DOM Level 2
     */
    public static final short INVALID_MODIFICATION_ERR  = 13;
    /**
     * @since DOM Level 2
     */
    public static final short NAMESPACE_ERR             = 14;
    /**
     * @since DOM Level 2
     */
    public static final short INVALID_ACCESS_ERR        = 15;

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
// Contributor(s): none
//