package org.orbeon.saxon.om;

/**
 * An implementation of the NamespaceDeclarations interface,
 * based on encapsulating an array of namespace codes.
 */

public class NamespaceDeclarationsImpl implements NamespaceDeclarations {

    private NamePool namePool;
    private int[] namespaceCodes;
    private int used;

    private static final int[] emptyArray = new int[0];

    /**
     * Create an uninitialized instance
     */
 
    public NamespaceDeclarationsImpl() {}

    /**
     * Construct a set of namespace declarations
     * @param pool the name pool
     * @param codes an integer array holding the namespace codes. These
     * codes are allocated by the name pool, and can be used to look up
     * a prefix and uri in the name pool. If the array contains the integer
     * -1, this acts as a terminator for the list. This is the format
     * returned by the method {@link NodeInfo#getDeclaredNamespaces(int[])}.
     * A value of null is equivalent to supplying an empty array.
     */

    public NamespaceDeclarationsImpl(NamePool pool, int[] codes) {
        namePool = pool;
        setNamespaceCodes(codes);
    }

    /**
     * Set the name pool
     * @param pool the NamePool
     */

    public void setNamePool(NamePool pool) {
        namePool = pool;
    }

    /**
     * Set the namespace codes.
     * @param codes an integer array holding the namespace codes. These
     * codes are allocated by the name pool, and can be used to look up
     * a prefix and uri in the name pool. If the array contains the integer
     * -1, this acts as a terminator for the list. This is the format
     * returned by the method {@link NodeInfo#getDeclaredNamespaces(int[])}.
     * A value of null is equivalent to supplying an empty array.
     */

    public void setNamespaceCodes(int[] codes) {
        if (codes == null) {
            codes = emptyArray;
        }
        namespaceCodes = codes;
        used = codes.length;
        for (int i=0; i<codes.length; i++) {
            if (codes[i] == -1) {
                used = i;
                break;
            }
        }
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
        return namespaceCodes;
    }

    /**
     * Get the number of declarations (and undeclarations) in this list.
     */

    public int getNumberOfNamespaces() {
        return used;
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
        return namePool.getPrefixFromNamespaceCode(namespaceCodes[index]);
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
        return namePool.getURIFromNamespaceCode(namespaceCodes[index]);
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
        return namespaceCodes[index];
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
