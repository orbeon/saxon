package net.sf.saxon.event;
import net.sf.saxon.Configuration;
import net.sf.saxon.Err;
import net.sf.saxon.om.Name;
import net.sf.saxon.om.NamePool;
import net.sf.saxon.om.NamespaceConstant;
import net.sf.saxon.om.QNameException;
import net.sf.saxon.type.ValidationException;
import net.sf.saxon.xpath.XPathException;

import java.util.StringTokenizer;

/**
  * NamespaceReducer is a ProxyReceiver responsible for removing duplicate namespace
  * declarations. It also ensures that an xmlns="" undeclaration is output when
  * necessary. Used on its own, the NamespaceReducer simply eliminates unwanted
  * namespace declarations. It can also be subclassed, in which case the subclass
  * can use the services of the NamespaceReducer to resolve QNames.
  * <p>
  * The NamespaceReducer also validates namespace-sensitive content.
  */

public class NamespaceReducer extends ProxyReceiver
{
    private int nscodeXML;
    private int nscodeNull;

    // We keep track of namespaces to avoid outputting duplicate declarations. The namespaces
    // vector holds a list of all namespaces currently declared (organised as pairs of entries,
    // prefix followed by URI). The stack contains an entry for each element currently open; the
    // value on the stack is an Integer giving the number of namespaces added to the main
    // namespace stack by that element.

    private int[] namespaces = new int[50];          // all namespace codes currently declared
    private int namespacesSize = 0;                  // all namespaces currently declared
    private int[] countStack = new int[50];
    private int depth = 0;

    // Creating an element does not automatically inherit the namespaces of the containing element.
    // When the DISINHERIT property is set on startElement(), this indicates that the namespaces
    // on that element are not to be automatically inherited by its children. So startElement()
    // stacks a boolean flag indicating whether the children are to disinherit the parent's namespaces.

    private boolean[] disinheritStack = new boolean[50];

    private int[] pendingUndeclarations = null;

	/**
	* Set the name pool to be used for all name codes
	*/

	public void setConfiguration(Configuration config) {
        super.setConfiguration(config);
		NamePool namePool = config.getNamePool();
		nscodeXML = namePool.getNamespaceCode("xml", NamespaceConstant.XML);
		nscodeNull = namePool.getNamespaceCode("", "");
		super.setConfiguration(config);
	}


    /**
    * startElement. This call removes redundant namespace declarations, and
    * possibly adds an xmlns="" undeclaration.
    */

    public void startElement(int nameCode, int typeCode, int locationId, int properties) throws XPathException {

        super.startElement(nameCode, typeCode, locationId, properties);

        // If the parent element specified inherit=no, keep a list of namespaces that need to be
        // undeclared

        if (depth>0 && disinheritStack[depth-1]) {
            pendingUndeclarations = new int[namespacesSize];
            System.arraycopy(namespaces, 0, pendingUndeclarations, 0, namespacesSize);
        } else {
            pendingUndeclarations = null;
        }

        // Record the current height of the namespace list so it can be reset at endElement time

        countStack[depth] = 0;
        disinheritStack[depth] = (properties & ReceiverOptions.DISINHERIT_NAMESPACES) != 0;
        if (++depth >= countStack.length) {
            int[] newstack = new int[depth*2];
            System.arraycopy(countStack, 0, newstack, 0, depth);
            boolean[] disStack2 = new boolean[depth*2];
            System.arraycopy(disinheritStack, 0, disStack2, 0, depth);
            countStack = newstack;
            disinheritStack = disStack2;
        }


        // Ensure that the element namespace is output, unless this is done
        // automatically by the caller (which is true, for example, for a literal
        // result element).

        if ((properties & ReceiverOptions.NAMESPACE_OK) == 0) {
            namespace(getNamePool().allocateNamespaceCode(nameCode), 0);
        }

    }

    public void namespace(int namespaceCode, int properties) throws XPathException {

        // Keep the namespace only if it is actually needed

        if (isNeeded(namespaceCode)) {
            addToStack(namespaceCode);
            countStack[depth - 1]++;
            super.namespace(namespaceCode, properties);
        }

    }

    /**
     * Handle an attribute
     */

    public void attribute(int nameCode, int typeCode, CharSequence value, int locationId, int properties)
            throws XPathException {
        if ((properties & ReceiverOptions.NEEDS_PREFIX_CHECK) != 0) {
            // TODO: handle lists that mix QNames with other types
            String val = value.toString();
            if (val.indexOf(' ') < 0) {
                checkQNamePrefix(val);
            } else {
                StringTokenizer tok = new StringTokenizer(val);
                while (tok.hasMoreTokens()) {
                    String token = tok.nextToken();
                    checkQNamePrefix(token);
                }
            }
            properties &= (~ReceiverOptions.NEEDS_PREFIX_CHECK);
        }
        super.attribute(nameCode, typeCode, value, locationId, properties);
    }

    /**
     * Check the prefix of a value to be used in element or attribute content against
     * the namespace context in the result tree
     * @param token a QName or list of QNames to be checked
     * @throws ValidationException if a prefix has not been declared
     */

    private void checkQNamePrefix(String token) throws ValidationException {
        try {
            String[] parts = Name.getQNameParts(token);
            if (parts[0].equals("") || parts[0].equals("xml")) {
                return;
            }
            int prefixCode = getConfiguration().getNamePool().getCodeForPrefix(parts[0]);
            if (prefixCode == -1) {
                throw new ValidationException("Unknown prefix in QName content " + Err.wrap(token));
            }
            for (int i=namespacesSize-1; i>=0; i--) {
                if (namespaces[i]>>16==prefixCode) {
                    return;
                }
                // TODO: handle undeclared namespaces
            }
        } catch (QNameException err) {
            throw new ValidationException("Invalid QName in content " + Err.wrap(token));
        }
        throw new ValidationException("Undeclared prefix in QName content " + Err.wrap(token));
    }

    /**
    * Determine whether a namespace declaration is needed
    */

    private boolean isNeeded(int nscode) {
        if (nscode==nscodeXML) {
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

        for (int i=namespacesSize-1; i>=0; i--) {
        	if (namespaces[i]==nscode) {
        		// it's a duplicate so we don't need it
        		return false;
        	}
        	if ((namespaces[i]>>16) == (nscode>>16)) {
        		// same prefix, different URI, so we do need it
        		return true;
            }
        }

        // we need it unless it's a redundant xmlns=""
        return (nscode != nscodeNull);
    }

    /**
    * Add a namespace declaration to the stack
    */

    private void addToStack(int nscode) {
		// expand the stack if necessary
        if (namespacesSize+1 >= namespaces.length) {
            int[] newlist = new int[namespacesSize*2];
            System.arraycopy(namespaces, 0, newlist, 0, namespacesSize);
            namespaces = newlist;
        }
        namespaces[namespacesSize++] = nscode;
    }

    /**
     * startContent: Add any namespace undeclarations needed to stop
     * namespaces being inherited from parent elements
     */

    public void startContent() throws XPathException {

        if (pendingUndeclarations != null) {
            for (int i=0; i<pendingUndeclarations.length; i++) {
                int nscode = pendingUndeclarations[i];
                if (nscode != -1) {
                    namespace(nscode & 0xffff0000, 0);
                    // relies on the namespace() method to prevent duplicate undeclarations
                }
            }
        }
        pendingUndeclarations = null;
        super.startContent();
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
        namespacesSize -= nscount;

        super.endElement();

    }

    /**
     * Get the URI code corresponding to a given prefix code, by searching the
     * in-scope namespaces. This is a service provided to subclasses.
     * @param prefixCode the 16-bit prefix code required
     * @return the 16-bit URI code, or -1 if the prefix is not found
     */

    protected int getURICode(short prefixCode) {
        for (int i=namespacesSize-1; i>=0; i--) {
        	if ((namespaces[i]>>16) == (prefixCode)) {
        		return namespaces[i]&0xffff;
            }
        }
        if (prefixCode == 0) {
            return 0;   // by default, no prefix means no namespace URI
        } else {
            return -1;
        }
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
