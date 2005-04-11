package org.orbeon.saxon.pull;
import org.orbeon.saxon.om.*;
import org.orbeon.saxon.trans.XPathException;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * PullNamespaceReducer is a PullFilter responsible for removing duplicate namespace
 * declarations. It also performs namespace fixup: that is, it ensures that the
 * namespaces used in element and attribute names are all declared.
 *
 * <p>This class is derived from, and contains much common code with, the NamespaceReducer
 * in the push pipeline. (In the push version, however, namespace fixup is not
 * performed by the NamespaceReducer, but by the ComplexContentOutputter).</p>
 * @see net.sf.saxon.event.NamespaceReducer
 */

public class PullNamespaceReducer extends PullFilter implements NamespaceResolver
{

    // We keep track of namespaces to avoid outputting duplicate declarations. The namespaces
    // vector holds a list of all namespaces currently declared (organised as integer namespace codes).
    // The countStack contains an entry for each element currently open; the
    // value on the countStack is an Integer giving the number of namespaces added to the main
    // namespace stack by that element.

    private int[] allNamespaces = new int[50];          // all namespace codes currently declared
    private int allNamespacesSize = 0;                  // all namespaces currently declared
    private int[] countStack = new int[50];
    private int depth = 0;
    private int[] localNamespaces;
    private int localNamespacesSize = 0;
    private int nameCode;                               // the namecode of the current element

    private NamespaceDeclarations declaredNamespaces;
    private AttributeCollection attributeCollection;

    // Creating an element does not automatically inherit the namespaces of the containing element.
    // TODO: disinheriting namespaces is not yet supported by the pull pipeline
    //private boolean[] disinheritStack = new boolean[50];

    private int[] pendingUndeclarations = null;

    public PullNamespaceReducer(PullProvider base) {
        super(base);
    }

    /**
    * next(): handle next event.
    * The START_ELEMENT event removes redundant namespace declarations, and
    * possibly adds an xmlns="" undeclaration.
    */

    public int next() throws XPathException {

        int event = super.next();

        switch (event) {
            case START_ELEMENT:
                startElement();
                break;
            case END_ELEMENT:
                endElement();
                break;
            case PROCESSING_INSTRUCTION:
            case ATTRIBUTE:
            case NAMESPACE:
                nameCode = super.getNameCode();
                break;
            default:
                nameCode = -1;
        }
        return event;
    }

    private void startElement() throws XPathException {

        // If the parent element specified inherit=no, keep a list of namespaces that need to be
        // undeclared

//        if (depth>0 && disinheritStack[depth-1]) {
//            pendingUndeclarations = new int[namespacesSize];
//            System.arraycopy(namespaces, 0, pendingUndeclarations, 0, namespacesSize);
//        } else {
//            pendingUndeclarations = null;
//        }



        // Record the current height of the namespace list so it can be reset at endElement time

        countStack[depth] = 0;
        //disinheritStack[depth] = (properties & ReceiverOptions.DISINHERIT_NAMESPACES) != 0;
        if (++depth >= countStack.length) {
            int[] newstack = new int[depth*2];
            System.arraycopy(countStack, 0, newstack, 0, depth);
            //boolean[] disStack2 = new boolean[depth*2];
            //System.arraycopy(disinheritStack, 0, disStack2, 0, depth);
            countStack = newstack;
            //disinheritStack = disStack2;
        }

        // Get the list of namespaces associated with this element

        NamespaceDeclarations declarations = super.getNamespaceDeclarations();
        localNamespaces = declarations.getNamespaceCodes(nsBuffer);
        localNamespacesSize = 0;
        for (int i=0; i<localNamespaces.length; i++) {
            if (localNamespaces[i] == -1) {
                break;
            } else {
                if (isNeeded(localNamespaces[i])) {
                    addGlobalNamespace(localNamespaces[i]);
                    countStack[depth - 1]++;
                    localNamespaces[localNamespacesSize++] = localNamespaces[i];
                }
            }
        }

        // Namespace fixup: ensure that the element namespace is output


       nameCode = checkProposedPrefix(super.getNameCode(), 0);

//        int elementNS = getNamePool().allocateNamespaceCode(getNameCode());
//        if (isNeeded(elementNS)) {
//            appendNamespace(elementNS);
//        }

        // Namespace fixup: ensure that all namespaces used in attribute names are declared

        attributeCollection = super.getAttributes();
        boolean modified = false;
        for (int i=0; i<attributeCollection.getLength(); i++) {
            int nc = attributeCollection.getNameCode(i);
            if ((nc & ~NamePool.FP_MASK) != 0) {
                // Only need to do checking for an attribute that's namespaced
                int newnc = checkProposedPrefix(nc, i+1);
                if (nc != newnc) {
                    if (!modified) {
                        attributeCollection = copyAttributeCollection(attributeCollection);
                        modified = true;
                    }
                    ((AttributeCollectionImpl)attributeCollection).setAttribute(i, newnc,
                            attributeCollection.getTypeAnnotation(i),
                            attributeCollection.getValue(i),
                            attributeCollection.getLocationId(i),
                            attributeCollection.getProperties(i));
                }
            }
        }


        if (localNamespacesSize < localNamespaces.length) {
            localNamespaces[localNamespacesSize] = -1;     // add a terminator
        }

        declaredNamespaces = new NamespaceDeclarationsImpl(getNamePool(), localNamespaces);
        countStack[depth-1] = localNamespacesSize;
    }

    private int[] nsBuffer = new int[20];

    private void addLocalNamespace(int nc) {
        if (localNamespacesSize < localNamespaces.length) {
            localNamespaces[localNamespacesSize++] = nc;
        } else {
            if (localNamespacesSize == 0) {
                localNamespaces = new int[10];
            } else {
                int[] nc2 = new int[localNamespacesSize*2];
                System.arraycopy (localNamespaces, 0, nc2, 0, localNamespacesSize);
                localNamespaces = nc2;
                localNamespaces[localNamespacesSize++] = nc;
            }
        }
        addGlobalNamespace(nc);
    }

    /**
    * Determine whether a namespace declaration is needed
    */

    private boolean isNeeded(int nscode) {
        if (nscode==NamespaceConstant.XML_NAMESPACE_CODE) {
        		// Ignore the XML namespace
            return false;
        }

        // First cancel any pending undeclaration of this namespace prefix (there may be more than one)

        if (pendingUndeclarations != null) {
            for (int p=0; p<pendingUndeclarations.length; p++) {
                if ((nscode>>16) == (pendingUndeclarations[p]>>16)) {
                    pendingUndeclarations[p] = -1;
                    //break;
                }
            }
        }

        for (int i=allNamespacesSize-1; i>=0; i--) {
        	if (allNamespaces[i]==nscode) {
        		// it's a duplicate so we don't need it
        		return false;
        	}
        	if ((allNamespaces[i]>>16) == (nscode>>16)) {
        		// same prefix, different URI, so we do need it
        		return true;
            }
        }

        // we need it unless it's a redundant xmlns=""
        return (nscode != NamespaceConstant.NULL_NAMESPACE_CODE);


        // startContent: Add any namespace undeclarations needed to stop
        // namespaces being inherited from parent elements

//        if (pendingUndeclarations != null) {
//            for (int i=0; i<pendingUndeclarations.length; i++) {
//                int nscode1 = pendingUndeclarations[i];
//                if (nscode1 != -1) {
//                    namespace(nscode1 & 0xffff0000, 0);
//                    // relies on the namespace() method to prevent duplicate undeclarations
//                }
//            }
//        }
        //pendingUndeclarations = null;
    }

    /**
    * Check that the prefix for an element or attribute is acceptable, allocating a substitute
    * prefix if not. The prefix is acceptable unless a namespace declaration has been
    * written that assignes this prefix to a different namespace URI. This method
    * also checks that the element or attribute namespace has been declared, and declares it
    * if not.
    */

    private int checkProposedPrefix(int nameCode, int seq) {
        NamePool namePool = getNamePool();
        int nscode = namePool.getNamespaceCode(nameCode);
        if (nscode == -1) {
            // avoid calling allocate where possible, because it's synchronized
            nscode = namePool.allocateNamespaceCode(nameCode);
        }
        int nsprefix = nscode>>16;

        for (int i=allNamespacesSize-1; i>=0; i--) {
            if (nsprefix == (allNamespaces[i]>>16)) {
                // same prefix
                if ((nscode & 0xffff) == (allNamespaces[i] & 0xffff)) {
                    // same URI
                    return nameCode;	// all is well
                } else {
                    // same prefix is bound to a different URI. Action depends on whether the declaration
                    // is local to this element or at an outer level
                    if (i + localNamespacesSize >= allNamespacesSize) {
                        // the prefix is already defined locally, so allocate a new one
                        String prefix = getSubstitutePrefix(nscode, seq);

                        int newNameCode = namePool.allocate(
                                            prefix,
                                            namePool.getURI(nameCode),
                                            namePool.getLocalName(nameCode));
                        int newNSCode = namePool.allocateNamespaceCode(newNameCode);
                        addLocalNamespace(newNSCode);
                        return newNameCode;
                    } else {
                        // the prefix has been used on an outer level, but we can reuse it here
                        addLocalNamespace(nscode);
                        return nameCode;
                    }
                }
            }
        }
        // there is no declaration of this prefix: declare it now
        if (nscode != NamespaceConstant.NULL_NAMESPACE_CODE) {
            addLocalNamespace(nscode);
        }
        return nameCode;
    }

    /**
    * It is possible for a single output element to use the same prefix to refer to different
    * namespaces. In this case we have to generate an alternative prefix for uniqueness. The
    * one we generate is based on the sequential position of the element/attribute: this is
    * designed to ensure both uniqueness (with a high probability) and repeatability
    */

    private String getSubstitutePrefix(int nscode, int seq) {
    	String prefix = getNamePool().getPrefixFromNamespaceCode(nscode);
        return prefix + '_' + seq;
    }

    /**
    * Add a namespace declaration to the stack
    */

    private void addGlobalNamespace(int nscode) {
		// expand the stack if necessary
        if (allNamespacesSize+1 >= allNamespaces.length) {
            int[] newlist = new int[allNamespacesSize*2];
            System.arraycopy(allNamespaces, 0, newlist, 0, allNamespacesSize);
            allNamespaces = newlist;
        }
        allNamespaces[allNamespacesSize++] = nscode;
    }

    /**
     * Get the nameCode identifying the name of the current node. This method
     * can be used after the {@link #START_ELEMENT}, {@link #PROCESSING_INSTRUCTION},
     * {@link #ATTRIBUTE}, or {@link #NAMESPACE} events. With some PullProvider implementations,
     * <b>but not this one</b>, it can also be used after {@link #END_ELEMENT}: a client that
     * requires the information at that point (for example, to do serialization) should insert an
     * {@link ElementNameTracker} into the pipeline.
     * If called at other times, the result is undefined and may result in an IllegalStateException.
     * If called when the current node is an unnamed namespace node (a node representing the default namespace)
     * the returned value is -1.
     *
     * @return the nameCode. The nameCode can be used to obtain the prefix, local name,
     *         and namespace URI from the name pool.
     */

    public int getNameCode() {
        return nameCode;
    }

    /**
     * Get the attributes associated with the current element. This method must
     * be called only after a START_ELEMENT event has been notified. The contents
     * of the returned AttributeCollection are guaranteed to remain unchanged
     * until the next START_ELEMENT event, but may be modified thereafter. The object
     * should not be modified by the client.
     * <p/>
     * <p>Attributes may be read before or after reading the namespaces of an element,
     * but must not be read after the first child node has been read, or after calling
     * one of the methods skipToEnd(), getStringValue(), or getTypedValue().</p>
     *
     * @return an AttributeCollection representing the attributes of the element
     *         that has just been notified.
     */

    public AttributeCollection getAttributes() throws XPathException {
        return attributeCollection;
    }

    private AttributeCollectionImpl copyAttributeCollection(AttributeCollection in) {
        AttributeCollectionImpl out = new AttributeCollectionImpl(getNamePool());
        for (int i=0; i<in.getLength(); i++) {
            out.addAttribute(in.getNameCode(i),
                    in.getTypeAnnotation(i),
                    in.getValue(i),
                    in.getLocationId(i),
                    in.getProperties(i));
        }
        return out;
    }

    /**
     * Get the namespace declarations associated with the current element. This method must
     * be called only after a START_ELEMENT event has been notified. In the case of a top-level
     * START_ELEMENT event (that is, an element that either has no parent node, or whose parent
     * is not included in the sequence being read), the NamespaceDeclarations object returned
     * will contain a namespace declaration for each namespace that is in-scope for this element
     * node. In the case of a non-top-level element, the NamespaceDeclarations will contain
     * a set of namespace declarations and undeclarations, representing the differences between
     * this element and its parent.
     * <p/>
     * <p>It is permissible for this method to return namespace declarations that are redundant.</p>
     * <p/>
     * <p>The NamespaceDeclarations object is guaranteed to remain unchanged until the next START_ELEMENT
     * event, but may then be overwritten. The object should not be modified by the client.</p>
     * <p/>
     * <p>Namespaces may be read before or after reading the attributes of an element,
     * but must not be read after the first child node has been read, or after calling
     * one of the methods skipToEnd(), getStringValue(), or getTypedValue().</p>*
     */

    public NamespaceDeclarations getNamespaceDeclarations() throws XPathException {
        return declaredNamespaces;
    }

    /**
    * endElement: Discard the namespaces declared on this element.
    */


    public void endElement () throws XPathException
    {
        if (depth-- == 0) {
            throw new IllegalStateException("Attempt to output end tag with no matching start tag");
        }

        int nscount = countStack[depth];
        allNamespacesSize -= nscount;
    }

    /**
     * Get the URI code corresponding to a given prefix code, by searching the
     * in-scope namespaces. This is a service provided to subclasses.
     * @param prefixCode the 16-bit prefix code required
     * @return the 16-bit URI code, or -1 if the prefix is not found
     */

    protected short getURICode(short prefixCode) {
        for (int i=allNamespacesSize-1; i>=0; i--) {
        	if ((allNamespaces[i]>>16) == (prefixCode)) {
        		return (short)(allNamespaces[i]&0xffff);
            }
        }
        if (prefixCode == 0) {
            return 0;   // by default, no prefix means no namespace URI
        } else {
            return -1;
        }
    }

    /**
     * Get the namespace URI corresponding to a given prefix. Return null
     * if the prefix is not in scope.
     *
     * @param prefix     the namespace prefix
     * @param useDefault true if the default namespace is to be used when the
     *                   prefix is ""
     * @return the uri for the namespace, or null if the prefix is not in scope
     */

    public String getURIForPrefix(String prefix, boolean useDefault) {
        NamePool pool = getNamePool();
        if ("".equals(prefix) && !useDefault) {
            return "";
        } else if ("xml".equals(prefix)) {
            return NamespaceConstant.XML;
        } else {
            short prefixCode = pool.getCodeForPrefix(prefix);
            short uriCode = getURICode(prefixCode);
            if (uriCode == -1) {
                return null;
            }
            return pool.getURIFromURICode(uriCode);
        }
    }

    /**
     * Get an iterator over all the prefixes declared in this namespace context. This will include
     * the default namespace (prefix="") and the XML namespace where appropriate
     */

    public Iterator iteratePrefixes() {
        NamePool pool = getNamePool();
        List prefixes = new ArrayList(allNamespacesSize);
        for (int i=allNamespacesSize-1; i>=0; i--) {
            String prefix = pool.getPrefixFromNamespaceCode(allNamespaces[i]);
            if (!prefixes.contains(prefix)) {
                prefixes.add(prefix);
            }
        }
        prefixes.add("xml");
        return prefixes.iterator();
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
