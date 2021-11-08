package org.orbeon.saxon.om;

/**
 * A class representing an empty set of namespace declarations
 */
public class EmptyNamespaceDeclarationList implements NamespaceDeclarations {

    private static final int[] emptyArray = new int[0];
    private static EmptyNamespaceDeclarationList THE_INSTANCE = new EmptyNamespaceDeclarationList();

    private EmptyNamespaceDeclarationList(){};

    /**
     * Get the singular instance of this class
     * @return the singular instance of this class
     */

    public static EmptyNamespaceDeclarationList getInstance() {
        return THE_INSTANCE;
    }

    /**
     * Get the n'th declaration in the list in the form of a namespace code. Namespace
     * codes can be translated into a prefix and URI by means of methods in the
     * NamePool
     *
     * @param index the index identifying which declaration is required.
     * @return the namespace code. This is an integer whose upper half indicates
     *         the prefix (0 represents the default namespace), and whose lower half indicates
     *         the URI (0 represents an undeclaration).
     * @throws IndexOutOfBoundsException if the index is out of range.
     * @see NamePool#getPrefixFromNamespaceCode(int)
     * @see NamePool#getURIFromNamespaceCode(int)
     */

    public int getNamespaceCode(int index) {
        throw new IndexOutOfBoundsException(index+"");
    }

    /**
     * Get all the namespace codes, as an array.
     *
     * @param buffer a sacrificial array that the method is free to use to contain the result.
     *               May be null.
     * @return an integer array containing namespace codes. The array may be filled completely
     *         with namespace codes, or it may be incompletely filled, in which case a -1 integer acts
     *         as a terminator.
     */

    public int[] getNamespaceCodes(int[] buffer) {
        return emptyArray;
    }

    /**
     * Get the number of declarations (and undeclarations) in this list.
     */

    public int getNumberOfNamespaces() {
        return 0;
    }

    /**
     * Get the prefix of the n'th declaration (or undeclaration) in the list,
     * counting from zero.
     *
     * @param index the index identifying which declaration is required.
     * @return the namespace prefix. For a declaration or undeclaration of the
     *         default namespace, this is the zero-length string.
     * @throws IndexOutOfBoundsException if the index is out of range.
     */

    public String getPrefix(int index) {
        throw new IndexOutOfBoundsException(index+"");
    }

    /**
     * Get the namespace URI of the n'th declaration (or undeclaration) in the list,
     * counting from zero.
     *
     * @param index the index identifying which declaration is required.
     * @return the namespace URI. For a namespace undeclaration, this is the
     *         zero-length string.
     * @throws IndexOutOfBoundsException if the index is out of range.
     */

    public String getURI(int index) {
        throw new IndexOutOfBoundsException(index+"");
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

