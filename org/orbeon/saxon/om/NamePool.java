package net.sf.saxon.om;

import net.sf.saxon.style.StandardNames;

import java.io.Serializable;
import java.util.StringTokenizer;
import java.util.WeakHashMap;

/**
  * An object representing a collection of XML names, each containing a Namespace URI,
  * a Namespace prefix, and a local name; plus a collection of namespaces, each
  * consisting of a prefix/URI pair. <br>
  *
  * <p>The equivalence betweem names depends only on the URI and the local name.
  * The prefix is retained for documentary purposes only: it is useful when
  * reconstructing a document to use prefixes that the user is familiar with.</p>
  *
  * <p>The NamePool eliminates duplicate names if they have the same prefix, uri,
  * and local part. It retains duplicates if they have different prefixes</p>
  *
  *
  * @author Michael H. Kay
  */

public class NamePool implements Serializable {

	// The NamePool holds two kinds of entry: name entries, representing
	// expanded names (local name + prefix + URI), identified by a name code,
	// and namespace entries (prefix + URI) identified by a namespace code.
	//
	// The data structure of the name table is as follows.
	//
	// There is a fixed size hash table; names are allocated to slots in this
	// table by hashing on the local name. Each entry in the table is the head of
	// a chain of NameEntry objects representing names that have the same hash code.
	//
	// Each NameEntry represents a distinct name (same URI and local name). It contains
	// The local name as a string, plus a short integer representing the URI (as an
	// offset into the array uris[]).
	//
	// The fingerprint of a name consists of the hash slot number (in the bottom 10 bits)
    // concatenated with the depth of the entry down the chain of hash synonyms (in the
    // next 10 bits). Fingerprints with depth 0 (i.e., in the range 0-1023) are reserved
    // for predefined names (names of XSLT elements and attributes, and of built-in types).
    // These names are not stored in the name pool, but are accessible as if they were.
	//
	// A nameCode contains the fingerprint in the bottom 20 bits. It also contains
	// an 8-bit prefix index. This distinguishes the prefix used, among all the
	// prefixes that have been used with this namespace URI. If the prefix index is
	// zero, the prefix is null. Otherwise, it indexes an space-separated list of
	// prefix Strings associated with the namespace URI.


	// The default singular instance, used unless the user deliberately wants to
	// manage name pools himself

	private static NamePool defaultNamePool = new NamePool();

	// The documentNumberMap associates a unique number with each loaded document. It is defined
	// as a WeakHashMap so that it does not lock the documents in memory. Until Saxon 7.1, this
	// information was held in the DocumentPool. It has been moved into the NamePool to allow
	// multiple documents to be handled in a free-standing XPath environment, that is, in the
	// absence of a Controller. Because a Document has a link to its NamePool, functions such
	// as generate-id() can now operate in the absence of a Controller.

    private transient WeakHashMap documentNumberMap = new WeakHashMap(10);
    private int numberOfDocuments = 0;

	/**
	* Get the singular default NamePool
	*/

	public static NamePool getDefaultNamePool() {
		return defaultNamePool;
	}

	/**
	* Set the default NamePool
	* (used after loading a compiled stylesheet)
	*/

	public static void setDefaultNamePool(NamePool pool) {
	    defaultNamePool = pool;
	}


	private static class NameEntry implements Serializable {
		String localName;
		short uriCode;
		NameEntry nextEntry;	// next NameEntry with the same hashcode

		public NameEntry(short uriCode, String localName) {
			this.uriCode = uriCode;
			this.localName = localName.intern();
			this.nextEntry = null;
		}

	}

    NameEntry[] hashslots = new NameEntry[1024];

    String[] prefixes = new String[100];
    short prefixesUsed = 0;
    String[] uris = new String[100];
    String[] prefixesForUri = new String[100];
    short urisUsed = 0;

    public NamePool() {

    	prefixes[NamespaceConstant.NULL_CODE] = "";
    	uris[NamespaceConstant.NULL_CODE] = NamespaceConstant.NULL;
    	prefixesForUri[NamespaceConstant.NULL_CODE] = "";

    	prefixes[NamespaceConstant.XML_CODE] = "xml";
    	uris[NamespaceConstant.XML_CODE] = NamespaceConstant.XML;
    	prefixesForUri[NamespaceConstant.XML_CODE] = "xml ";

    	prefixes[NamespaceConstant.XSLT_CODE] = "xsl";
    	uris[NamespaceConstant.XSLT_CODE] = NamespaceConstant.XSLT;
    	prefixesForUri[NamespaceConstant.XSLT_CODE] = "xsl ";

    	prefixes[NamespaceConstant.SAXON_CODE] = "saxon";
    	uris[NamespaceConstant.SAXON_CODE] = NamespaceConstant.SAXON;
    	prefixesForUri[NamespaceConstant.SAXON_CODE] = "saxon ";

    	prefixes[NamespaceConstant.SCHEMA_CODE] = "xs";
    	uris[NamespaceConstant.SCHEMA_CODE] = NamespaceConstant.SCHEMA;
    	prefixesForUri[NamespaceConstant.SCHEMA_CODE] = "xs ";

    	prefixes[NamespaceConstant.XDT_CODE] = "xdt";
    	uris[NamespaceConstant.XDT_CODE] = NamespaceConstant.XDT;
    	prefixesForUri[NamespaceConstant.XDT_CODE] = "xdt ";

        prefixes[NamespaceConstant.XSI_CODE] = "xsi";
        uris[NamespaceConstant.XSI_CODE] = NamespaceConstant.SCHEMA_INSTANCE;
        prefixesForUri[NamespaceConstant.XSI_CODE] = "xsi ";

    	prefixesUsed = 7;
    	urisUsed = 7;

    }

    /**
    * Add a tree to the pool, and allocate a document number
    * @param doc The NodeInfo for the root of the tree in question
    * (this is not necessarily a document node)
    * @return the document number, unique within this document pool
    */

    public synchronized int allocateDocumentNumber(NodeInfo doc) {
        if (documentNumberMap == null) {
            // this can happen after deserialization
            documentNumberMap = new WeakHashMap(10);
        }

        Integer nr = (Integer)documentNumberMap.get(doc);
        if (nr!=null) {
            return nr.intValue();
        } else {
            int next = numberOfDocuments++;
            documentNumberMap.put(doc, new Integer(next));
            return next;
        }
    }

	/**
	* Get a name entry corresponding to a given name code
	* @return null if there is none.
	*/

	private NameEntry getNameEntry(int nameCode) {
		int hash = nameCode & 0x3ff;
		int depth = (nameCode >> 10) & 0x3ff;
		NameEntry entry = hashslots[hash];

		for (int i=1; i<depth; i++) {
			if (entry==null) return null;
			entry = entry.nextEntry;
		}
		return entry;
	}

    /**
    * Allocate the namespace code for a namespace prefix/URI pair.
    * Create it if not already present
    * @param prefix the namespace prefix
    * @param uri the namespace URI
    * @return an integer code identifying the namespace. The namespace code
     * identifies both the prefix and the URI.
    */

    public synchronized int allocateNamespaceCode(String prefix, String uri) {
    			// System.err.println("allocate nscode for " + prefix + " = " + uri);

    	int prefixCode = allocateCodeForPrefix(prefix);
    	int uriCode = allocateCodeForURI(uri);

    	if (prefixCode!=0) {
    		// ensure the prefix is in the list of prefixes used with this URI
    		String key = prefix + ' ';
    		if (prefixesForUri[uriCode].indexOf(key) < 0) {
    			prefixesForUri[uriCode] += key;
    		}
		}

    	return (prefixCode<<16) + uriCode;
    }

    /**
    * Get the existing namespace code for a namespace prefix/URI pair.
    * @return -1 if there is none present
    */

    public int getNamespaceCode(String prefix, String uri) {
    			//System.err.println("get nscode for " + prefix + " = " + uri);
    	int prefixCode = getCodeForPrefix(prefix);
    	if (prefixCode<0) return -1;
    	int uriCode = getCodeForURI(uri);
    	if (uriCode<0) return -1;

    	if (prefixCode!=0) {
    		// ensure the prefix is in the list of prefixes used with this URI
    		String key = prefix + ' ';
    		if (prefixesForUri[uriCode].indexOf(key) < 0) {
    			return -1;
    		}
		}

    	return (prefixCode<<16) + uriCode;
    }

	/**
	* Allocate the uri code for a given URI;
	* create one if not found
	*/

	public synchronized short allocateCodeForURI(String uri) {
                    //System.err.println("allocate code for URI " + uri);
    	for (short j=0; j<urisUsed; j++) {
    		if (uris[j].equals(uri)) {
    			return j;
    		}
    	}
		if (urisUsed >= uris.length) {
			if (urisUsed>32000) {
				throw new NamePoolLimitException("Too many namespace URIs");
			}
			String[] p = new String[urisUsed*2];
			String[] u = new String[urisUsed*2];
			System.arraycopy(prefixesForUri, 0, p, 0, urisUsed);
			System.arraycopy(uris, 0, u, 0, urisUsed);
			prefixesForUri = p;
			uris = u;
		}
		uris[urisUsed] = uri;
		prefixesForUri[urisUsed] = "";
		return urisUsed++;
    }



	/**
	* Get the uri code for a given URI
	* @return -1 if not present in the name pool
	*/

	public short getCodeForURI(String uri) {
    	for (short j=0; j<urisUsed; j++) {
    		if (uris[j].equals(uri)) {
    			return j;
    		}
    	}
		return -1;
    }

	/**
	* Allocate the prefix code for a given Prefix; create one if not found
    * @param prefix the namespace prefix whose code is to be allocated or returned
    * @return the numeric code for this prefix
	*/

	public synchronized short allocateCodeForPrefix(String prefix) {
        // TODO: this search can be quite lengthy (typically 9 entries) - use some better approach.
    	for (short i=0; i<prefixesUsed; i++) {
    		if (prefixes[i].equals(prefix)) {
    			return i;
    		}
    	}
		if (prefixesUsed >= prefixes.length) {
			if (prefixesUsed>32000) {
				throw new NamePoolLimitException("Too many namespace prefixes");
			}
			String[] p = new String[prefixesUsed*2];
			System.arraycopy(prefixes, 0, p, 0, prefixesUsed);
			prefixes = p;
		}
		prefixes[prefixesUsed] = prefix;
		return prefixesUsed++;
    }


	/**
	* Get the prefix code for a given Prefix
	* @return -1 if not found
	*/

	public short getCodeForPrefix(String prefix) {
    	for (short i=0; i<prefixesUsed; i++) {
    		if (prefixes[i].equals(prefix)) {
    			return i;
    		}
    	}
		return -1;
    }

    /**
    * Suggest a prefix for a given URI. If there are several, it's undefined which one is returned.
    * If there are no prefixes registered for this URI, return null.
    */

    public String suggestPrefixForURI(String URI) {
        short uriCode = getCodeForURI(URI);
        if (uriCode==-1) return null;
    	StringTokenizer tok = new StringTokenizer(prefixesForUri[uriCode]);
    	if (tok.hasMoreElements()) {
    		return (String)tok.nextElement();
    	}
    	return null;
    }

    /**
    * Get the index of a prefix among all the prefixes used with a given URI
    * @return -1 if not found
    */

    private int getPrefixIndex(short uriCode, String prefix) {

    	// look for quick wins
    	if (prefix.equals("")) return 0;
    	if (prefixesForUri[uriCode].equals(prefix+' ')) return 1;

    	// search for the prefix in the list
    	int i = 1;
    	StringTokenizer tok = new StringTokenizer(prefixesForUri[uriCode]);
    	while (tok.hasMoreElements()) {
    		if (prefix.equals(tok.nextElement())) {
    			return i;
    		}
    		if (i++==255) {
    			throw new NamePoolLimitException("Too many prefixes for one namespace URI");
    		}
    	}
    	return -1;
    }

    /**
    * Get a prefix among all the prefixes used with a given URI, given its index
    * @return null if not found
    */

    public String getPrefixWithIndex(short uriCode, int index) {
    	if (index==0) return "";
    	StringTokenizer tok = new StringTokenizer(prefixesForUri[uriCode]);
    	int i=1;
    	while (tok.hasMoreElements()) {
    		String prefix = (String)tok.nextElement();
    		if (i++ == index) {
    			return prefix;
    		}
    	}
    	return null;
    }

    /**
    * Allocate a name from the pool, or a new Name if there is not a matching one there
    * @param prefix
    * @param uri - the namespace URI. The null URI is represented as an empty string.
    * @param localName
    * @return an integer (the "namecode") identifying the name within the namepool.
    * The Name itself may be retrieved using the getName(int) method
    */

    public synchronized int allocate(String prefix, String uri, String localName) {
        if (NamespaceConstant.isReserved(uri) || uri.equals(NamespaceConstant.SAXON)) {
            int fp = StandardNames.getFingerprint(uri, localName);
            if (fp != -1) {
                short uriCode = StandardNames.getURICode(fp);
                int prefixIndex = getPrefixIndex(uriCode, prefix);

                if (prefixIndex<0) {
                    prefixesForUri[uriCode] += (prefix + ' ');
                    prefixIndex = getPrefixIndex(uriCode, prefix);
                }

                return (prefixIndex<<20) + fp;
            }
        }
        // otherwise register the name in this NamePool
    	short uriCode = allocateCodeForURI(uri);
    	return allocate(prefix, uriCode, localName);
    }

    /**
    * Allocate a name from the pool, or a new Name if there is not a matching one there
    * @param prefix - the namespace prefix
    * @param uriCode - the code of the URI
    * @param localName - the local part of the QName
    * @return an integer (the "namecode") identifying the name within the namepool.
    */

    public synchronized int allocate(String prefix, short uriCode, String localName) {
    	        // System.err.println("Allocate " + prefix + " : " + uriCode + " : " + localName);
        int hash = (localName.hashCode() & 0x7fffffff) % 1023;
        int depth = 1;
        int prefixIndex = getPrefixIndex(uriCode, prefix);

        if (prefixIndex<0) {
        	prefixesForUri[uriCode] += (prefix + ' ');
        	prefixIndex = getPrefixIndex(uriCode, prefix);
        }
        NameEntry entry;

        if (hashslots[hash]==null) {
			entry = new NameEntry(uriCode, localName);
			hashslots[hash] = entry;
		} else {
			entry = hashslots[hash];
			while (true) {
				boolean sameLocalName = (entry.localName.equals(localName));
				boolean sameURI = (entry.uriCode==uriCode);

				if (sameLocalName && sameURI) {
							// may need to add a new prefix to the entry
					break;
				} else {
					NameEntry next = entry.nextEntry;
					depth++;
					if (depth >= 1024) {
						throw new NamePoolLimitException("Saxon name pool is full");
					}
					if (next==null) {
						NameEntry newentry = new NameEntry(uriCode, localName);
						entry.nextEntry = newentry;
						break;
					} else {
						entry = next;
					}
				}
			}
		}
		// System.err.println("name code = " + prefixIndex + "/" + depth + "/" + hash);
		return ((prefixIndex<<20) + (depth<<10) + hash);
	}

    /**
    * Allocate a namespace code for the prefix/URI of a given namecode
    * @param namecode a code identifying an expanded QName, e.g. of an element or attribute
    * @return a code identifying the namespace used in the given name. The namespace code
    * identifies both the prefix and the URI.
    */

    public synchronized int allocateNamespaceCode(int namecode) {
        short uriCode;
        int fp = namecode & 0xfffff;
        if ((fp & 0xffc00) == 0) {
            uriCode = StandardNames.getURICode(fp);
        } else {
            NameEntry entry = getNameEntry(namecode);
            if (entry==null) {
                unknownNameCode(namecode);
                return -1;  // to keep the compiler happy
            } else {
                uriCode = entry.uriCode;
            }
        }
        int prefixIndex = (namecode >> 20) & 0xff;
        String prefix = getPrefixWithIndex(uriCode, prefixIndex);
        int prefixCode = allocateCodeForPrefix(prefix);
    	return (prefixCode<<16) + uriCode;
    }

	/**
	* Get the namespace-URI of a name, given its name code or fingerprint
	*/

	public String getURI(int nameCode) {
        if ((nameCode & 0xffc00) == 0) {
            return StandardNames.getURI(nameCode & 0xfffff);
        }
		NameEntry entry = getNameEntry(nameCode);
		if (entry==null) {
			unknownNameCode(nameCode);
            return null;    // to keep the compiler happy
		}
		return uris[entry.uriCode];
	}

	/**
	* Get the URI code of a name, given its name code or fingerprint
	*/

	public short getURICode(int nameCode) {
        if ((nameCode & 0xffc00) == 0) {
            return StandardNames.getURICode(nameCode & 0xfffff);
        }
		NameEntry entry = getNameEntry(nameCode);
		if (entry==null) {
			unknownNameCode(nameCode);
            return -1;
		}
		return entry.uriCode;
	}

	/**
	* Get the local part of a name, given its name code or fingerprint
	*/

	public String getLocalName(int nameCode) {
        if ((nameCode & 0xffc00) == 0) {
            return StandardNames.getLocalName(nameCode & 0xfffff);
        }
		NameEntry entry = getNameEntry(nameCode);
		if (entry==null) {
			unknownNameCode(nameCode);
            return null;
		}
		return entry.localName;
	}

	/**
	* Get the prefix part of a name, given its name code or fingerprint
	*/

	public String getPrefix(int nameCode) {
        if ((nameCode & 0xffc00) == 0) {
            return StandardNames.getPrefix(nameCode & 0xfffff);
        }
        short uriCode = getURICode(nameCode);
		int prefixIndex = (nameCode >> 20) & 0xff;
		return getPrefixWithIndex(uriCode, prefixIndex);
	}

	/**
	* Get the display form of a name (the QName), given its name code or fingerprint
	*/

	public String getDisplayName(int nameCode) {
        if ((nameCode & 0xffc00) == 0) {
            // This indicates a standard name known to the system (but it might have a non-standard prefix)
            int prefixIndex = (nameCode >> 20) & 0xff;
            short uriCode = getURICode(nameCode);
            String prefix = getPrefixWithIndex(uriCode, prefixIndex);
            if (prefix.equals("")) {
                return StandardNames.getLocalName(nameCode & 0xfffff);
            } else {
                return prefix + ':' + StandardNames.getLocalName(nameCode & 0xfffff);
            }
        }

		NameEntry entry = getNameEntry(nameCode);
		if (entry==null) {
			unknownNameCode(nameCode);
            return null;
		}
		int prefixIndex = (nameCode >> 20) & 0xff;
        String prefix = getPrefixWithIndex(entry.uriCode, prefixIndex);
        if (prefix==null || prefix.equals("")) {
            return entry.localName;
        } else {
            return prefix + ':' + entry.localName;
        }
	}

    /**
     * Get the Clark form of a name, given its name code or fingerprint
     * @return the local name if the name is in the null namespace, or "{uri}local"
     * otherwise. The name is always interned.
     */

     public String getClarkName(int nameCode) {
        if ((nameCode & 0xffc00) == 0) {
            return StandardNames.getClarkName(nameCode & 0xfffff);
        }
		NameEntry entry = getNameEntry(nameCode);
		if (entry==null) {
			unknownNameCode(nameCode);
            return null;
		}
        if (entry.uriCode == 0) {
            return entry.localName;
        } else {
            String n =
                '{' + getURIFromURICode(entry.uriCode) + '}' + entry.localName;
            return n.intern();
        }
	}

    /**
     * Allocate a fingerprint given a Clark Name
     */

    public int allocateClarkName(String expandedName) {
        String namespace;
        String localName;
        if (expandedName.charAt(0) == '{') {
            int closeBrace = expandedName.indexOf('}');
            if (closeBrace < 0) {
                throw new IllegalArgumentException("No closing '}' in Clark name");
            }
            namespace = expandedName.substring(1, closeBrace);
            if (closeBrace == expandedName.length()) {
                throw new IllegalArgumentException("Missing local part in Clark name");
            }
            localName = expandedName.substring(closeBrace + 1);
        } else {
            namespace = "";
            localName = expandedName;
        }

        return allocate("", namespace, localName);
    }



	/**
	* Internal error: name not found in namepool
	* (Usual cause is allocating a name code from one name pool and trying to
	* find it in another)
	*/

	private void unknownNameCode(int nameCode) {
		//System.err.println("Unknown name code " + nameCode);
		//diagnosticDump();
		//(new IllegalArgumentException("Unknown name")).printStackTrace();
		throw new IllegalArgumentException("Unknown name code " + nameCode);
	}

	/**
	* Get a fingerprint for the name with a given uri and local name.
	* These must be present in the NamePool.
	* The fingerprint has the property that if two fingerprint are the same, the names
	* are the same (ie. same local name and same URI).
	* @return -1 if not found
	*/

	public int getFingerprint(String uri, String localName) {
		// A read-only version of allocate()

        short uriCode;
        if (uri.equals("")) {
            uriCode = 0;
        } else {
            if (NamespaceConstant.isReserved(uri) || uri.equals(NamespaceConstant.SAXON)) {
                int fp = StandardNames.getFingerprint(uri, localName);
                if (fp != -1) {
                    return fp;
                    // otherwise, look for the name in this namepool
                }
            }
            uriCode = -1;
            for (short j=0; j<urisUsed; j++) {
                if (uris[j].equals(uri)) {
                    uriCode = j;
                    break;
                }
            }
            if (uriCode==-1) return -1;
        }

        int hash = (localName.hashCode() & 0x7fffffff) % 1023;
        int depth = 1;

        NameEntry entry;

        if (hashslots[hash]==null) {
			return -1;
		}

        entry = hashslots[hash];
        while (true) {
            if (entry.uriCode==uriCode && entry.localName.equals(localName)) {
                break;
            } else {
                NameEntry next = entry.nextEntry;
                depth++;
                if (next==null) {
                    return -1;
                } else {
                    entry = next;
                }
            }
        }
		return (depth<<10) + hash;
	}

	/**
	* Get the namespace URI from a namespace code.
	*/

	public String getURIFromNamespaceCode(int code) {
		return uris[code&0xffff];
	}

	/**
	* Get the namespace URI from a URI code.
	*/

	public String getURIFromURICode(short code) {
		return uris[code];
	}

	/**
	* Get the namespace prefix from a namespace code.
	*/

	public String getPrefixFromNamespaceCode(int code) {
			// System.err.println("get prefix for " + code);
		return prefixes[code>>16];
	}

    /**
    * Get fingerprint for expanded name in {uri}local format
    */

    public int getFingerprintForExpandedName(String expandedName) {

        String localName;
        String namespace;

        if (expandedName.charAt(0)=='{') {
            int closeBrace = expandedName.indexOf('}');
            if (closeBrace < 0) {
                throw new IllegalArgumentException("No closing '}' in parameter name");
            }
            namespace = expandedName.substring(1, closeBrace);
            if (closeBrace == expandedName.length()) {
                throw new IllegalArgumentException("Missing local part in parameter name");
            }
            localName = expandedName.substring(closeBrace+1);
        } else {
            namespace = "";
            localName = expandedName;
        }

        return allocate("", namespace, localName);
    }


    /**
    * Diagnostic print of the namepool contents.
    */

    public synchronized void diagnosticDump() {
    	System.err.println("Contents of NamePool " + this);
		for (int i=0; i<1024; i++) {
			NameEntry entry = hashslots[i];
			int depth = 0;
			while (entry != null) {
				System.err.println("Fingerprint " + depth + '/' + i);
				System.err.println("  local name = " + entry.localName +
									 " uri code = " + entry.uriCode);
				entry = entry.nextEntry;
				depth++;
			}
		}

		for (int p=0; p<prefixesUsed; p++) {
			System.err.println("Prefix " + p + " = " + prefixes[p]);
		}
		for (int u=0; u<urisUsed; u++) {
			System.err.println("URI " + u + " = " + uris[u]);
			System.err.println("Prefixes for URI " + u + " = " + prefixesForUri[u]);
		}
	}


    public static class NamePoolLimitException extends RuntimeException {

        public NamePoolLimitException(String message) {
            super(message);
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
