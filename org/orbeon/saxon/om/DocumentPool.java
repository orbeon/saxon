package net.sf.saxon.om;
import java.util.HashMap;
import java.util.Iterator;

/**
  * An object representing the collection of documents handled during
  * a single transformation.
  *
  * <p>From Saxon 7.2, the function of allocating document numbers is performed
  * by the NamePool, not by the DocumentPool. This has a
  * number of effects: in particular it allows operations involving multiple
  * documents (such as generateId() and document()) to occur in a free-standing
  * XPath environment.</p>
  */

public final class DocumentPool {

    // The document pool ensures that the document()
    // function, when called twice with the same URI, returns the same document
    // each time. For this purpose we use a hashtable from
    // URI to DocumentInfo object.

    private HashMap documentNameMap = new HashMap(10);

    /**
    * Add a document to the pool
    * @param doc The DocumentInfo for the document in question
    * @param name The name of the document.
    */

    public void add(DocumentInfo doc, String name) {
        if (name!=null) {
            documentNameMap.put(name, doc);
        }
    }

    /**
    * Get the document with a given name
    * @return the DocumentInfo with the given name if it exists,
    * or null if it is not found.
    */

    public DocumentInfo find(String name) {
        return (DocumentInfo)documentNameMap.get(name);
    }

    /**
     * Release a document from the document pool. This means that if the same document is
     * loaded again later, the source will need to be re-parsed, and nodes will get new identities.
     */

    public DocumentInfo discard(DocumentInfo doc) {
        Iterator iter = documentNameMap.keySet().iterator();
        while (iter.hasNext()) {
            Object name = iter.next();
            DocumentInfo entry = (DocumentInfo) documentNameMap.get(name);
            if (entry == doc) {
                documentNameMap.remove(name);
                return doc;
            }
        }
        return doc;
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
