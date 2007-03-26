package org.orbeon.saxon.om;

import org.orbeon.saxon.style.StandardNames;
import org.orbeon.saxon.trans.DynamicError;

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;

/**
 * An object representing a collection of XML names, each containing a Namespace URI,
 * a Namespace prefix, and a local name; plus a collection of namespaces, each
 * consisting of a prefix/URI pair. <br>
 * <p/>
 * <p>The equivalence betweem names depends only on the URI and the local name.
 * The prefix is retained for documentary purposes only: it is useful when
 * reconstructing a document to use prefixes that the user is familiar with.</p>
 * <p/>
 * <p>The NamePool eliminates duplicate names if they have the same prefix, uri,
 * and local part. It retains duplicates if they have different prefixes</p>
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
    // a 10-bit prefix index. This distinguishes the prefix used, among all the
    // prefixes that have been used with this namespace URI. If the prefix index is
    // zero, the prefix is null. Otherwise, it indexes an array of
    // prefix Strings associated with the namespace URI. Note that the data structures
    // and algorithms are optimized for the case where URIs usually use the same prefix.

    /**
     * FP_MASK is a mask used to obtain a fingerprint from a nameCode. Given a
     * nameCode nc, the fingerprint is <code>nc & NamePool.FP_MASK</code>.
     * (In practice, Saxon code often uses the literal constant 0xfffff.)
     * The difference between a nameCode is that a nameCode contains information
     * about the prefix of a name, the fingerprint depends only on the namespace
     * URI and local name. Note that the "null" nameCode (-1) does not produce
     * the "null" fingerprint (also -1) when this mask is applied.
     */

    public static final int FP_MASK = 0xfffff;

    // Since fingerprints in the range 0-1023 belong to predefined names, user-defined names
    // will always have a fingerprint above this range, which can be tested by a mask.

    public static final int USER_DEFINED_MASK = 0xffc00;

    // Limit: maximum number of prefixes allowed for one URI

    public static final int MAX_PREFIXES_PER_URI = 1023;


    // The default singular instance, used unless the user deliberately wants to
    // manage name pools himself

    private static NamePool defaultNamePool = new NamePool();

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
    String[][] prefixesForUri = new String[100][0];
    short urisUsed = 0;

    // General purpose cache for data held by clients of the namePool

    private HashMap clientData;

    public NamePool() {

        prefixes[NamespaceConstant.NULL_CODE] = "";
        uris[NamespaceConstant.NULL_CODE] = NamespaceConstant.NULL;
        String[] nullp = {""};
        prefixesForUri[NamespaceConstant.NULL_CODE] = nullp;

        prefixes[NamespaceConstant.XML_CODE] = "xml";
        uris[NamespaceConstant.XML_CODE] = NamespaceConstant.XML;
        String[] xmlp = {"xml"};
        prefixesForUri[NamespaceConstant.XML_CODE] = xmlp;

        prefixes[NamespaceConstant.XSLT_CODE] = "xsl";
        uris[NamespaceConstant.XSLT_CODE] = NamespaceConstant.XSLT;
        String[] xsltp = {"xslt"};
        prefixesForUri[NamespaceConstant.XSLT_CODE] = xsltp;

        prefixes[NamespaceConstant.SAXON_CODE] = "saxon";
        uris[NamespaceConstant.SAXON_CODE] = NamespaceConstant.SAXON;
        String[] saxonp = {"saxon"};
        prefixesForUri[NamespaceConstant.SAXON_CODE] = saxonp;

        prefixes[NamespaceConstant.SCHEMA_CODE] = "xs";
        uris[NamespaceConstant.SCHEMA_CODE] = NamespaceConstant.SCHEMA;
        String[] schemap = {"xs"};
        prefixesForUri[NamespaceConstant.SCHEMA_CODE] = schemap;

        prefixes[NamespaceConstant.XDT_CODE] = "xdt";
        uris[NamespaceConstant.XDT_CODE] = NamespaceConstant.XDT;
        String[] xdtp = {"xdt"};
        prefixesForUri[NamespaceConstant.XDT_CODE] = xdtp;

        prefixes[NamespaceConstant.XSI_CODE] = "xsi";
        uris[NamespaceConstant.XSI_CODE] = NamespaceConstant.SCHEMA_INSTANCE;
        String[] xsip = {"xsi"};
        prefixesForUri[NamespaceConstant.XSI_CODE] = xsip;

        prefixesUsed = 7;
        urisUsed = 7;

    }

    /**
     * Get a name entry corresponding to a given name code
     *
     * @return null if there is none.
     */

    private NameEntry getNameEntry(int nameCode) {
        int hash = nameCode & 0x3ff;
        int depth = (nameCode >> 10) & 0x3ff;
        NameEntry entry = hashslots[hash];

        for (int i = 1; i < depth; i++) {
            if (entry == null) {
                return null;
            }
            entry = entry.nextEntry;
        }
        return entry;
    }

    /**
     * Allocate the namespace code for a namespace prefix/URI pair.
     * Create it if not already present
     *
     * @param prefix the namespace prefix
     * @param uri    the namespace URI
     * @return an integer code identifying the namespace. The namespace code
     *         identifies both the prefix and the URI.
     */

    public synchronized int allocateNamespaceCode(String prefix, String uri) {
        // System.err.println("allocate nscode for " + prefix + " = " + uri);

        int prefixCode = allocateCodeForPrefix(prefix);
        int uriCode = allocateCodeForURI(uri);

        if (prefixCode != 0) {
            // ensure the prefix is in the list of prefixes used with this URI
            final String[] prefixes = prefixesForUri[uriCode];
            if (prefixes.length == 0 ||
                    (!prefixes[0].equals(prefix) && Arrays.asList(prefixes).indexOf(prefix) < 0)) {
                if (prefixes.length == MAX_PREFIXES_PER_URI) {
                    throw new NamePoolLimitException("NamePool limit exceeded: max " +
                                    MAX_PREFIXES_PER_URI + " prefixes per URI");
                }
                String[] p2 = new String[prefixes.length + 1];
                System.arraycopy(prefixes, 0, p2, 0, prefixes.length);
                p2[prefixes.length] = prefix;
                prefixesForUri[uriCode] = p2;
            }
        }

        return (prefixCode << 16) + uriCode;
    }

    /**
     * Get the existing namespace code for a namespace prefix/URI pair.
     *
     * @return -1 if there is none present
     */

    public int getNamespaceCode(String prefix, String uri) {
        //System.err.println("get nscode for " + prefix + " = " + uri);
        int prefixCode = getCodeForPrefix(prefix);
        if (prefixCode < 0) {
            return -1;
        }
        int uriCode = getCodeForURI(uri);
        if (uriCode < 0) {
            return -1;
        }

        if (prefixCode != 0) {
            // ensure the prefix is in the list of prefixes used with this URI
            final String[] prefixes = prefixesForUri[uriCode];
            if (prefixes.length == 0 ||
                    (!prefixes[0].equals(prefix) && Arrays.asList(prefixes).indexOf(prefix) < 0)) {
                return -1;
            }
        }

        return (prefixCode << 16) + uriCode;
    }

    /**
     * Allocate a namespace code for a given namecode
     *
     * @param namecode a code identifying an expanded QName, e.g. of an element or attribute
     * @return a code identifying the namespace used in the given name. The namespace code
     *         identifies both the prefix and the URI. Return -1 if no namespace code has been allocated
     *         (in this case the caller should call allocateNamespaceCode() to get one).
     */

    public int getNamespaceCode(int namecode) {
        short uriCode;
        int fp = namecode & FP_MASK;
        if ((fp & USER_DEFINED_MASK) == 0) {
            uriCode = StandardNames.getURICode(fp);
        } else {
            NameEntry entry = getNameEntry(namecode);
            if (entry == null) {
                return -1;
            } else {
                uriCode = entry.uriCode;
            }
        }
        int prefixIndex = getPrefixIndex(namecode);
        String prefix = getPrefixWithIndex(uriCode, prefixIndex);
        if (prefix == null) {
            return -1;
        }
        int prefixCode = getCodeForPrefix(prefix);
        if (prefixCode == -1) {
            return -1;
        }
        return (prefixCode << 16) + uriCode;
    }

    /**
     * Get the prefix index from a namecode
     * @param nameCode the name code
     * @return the prefix index. A value of zero means the name is unprefixed (which in the case of an
     * attribute, means that it is in the null namespace)
     */

    public static int getPrefixIndex(int nameCode) {
        return (nameCode >> 20) & 0x3ff;
    }


    /**
     * Allocate the uri code for a given URI;
     * create one if not found
     */

    public synchronized short allocateCodeForURI(String uri) {
        for (short j = 0; j < urisUsed; j++) {
            if (uris[j].equals(uri)) {
                return j;
            }
        }
        if (urisUsed >= uris.length) {
            if (urisUsed > 32000) {
                throw new NamePoolLimitException("Too many namespace URIs");
            }
            String[][] p = new String[urisUsed * 2][0];
            String[] u = new String[urisUsed * 2];
            System.arraycopy(prefixesForUri, 0, p, 0, urisUsed);
            System.arraycopy(uris, 0, u, 0, urisUsed);
            prefixesForUri = p;
            uris = u;
        }
        uris[urisUsed] = uri;
        //prefixesForUri[urisUsed] = "";
        return urisUsed++;
    }


    /**
     * Get the uri code for a given URI
     *
     * @return -1 if not present in the name pool
     */

    public short getCodeForURI(String uri) {
        for (short j = 0; j < urisUsed; j++) {
            if (uris[j].equals(uri)) {
                return j;
            }
        }
        return -1;
    }

    /**
     * Allocate the prefix code for a given Prefix; create one if not found
     *
     * @param prefix the namespace prefix whose code is to be allocated or returned
     * @return the numeric code for this prefix
     */

    private short allocateCodeForPrefix(String prefix) {
        // Not synchronized, because it's always called from a synchronized method

        // exploit knowledge of the standard prefixes to shorten the search
        short start = 1;
        if (prefix.equals("")) {
            return NamespaceConstant.NULL_CODE;
        }
        if (prefix.charAt(0) != 'x') {
            if (prefix.equals("saxon")) {
                return NamespaceConstant.SAXON_CODE;
            }
            start = NamespaceConstant.XSI_CODE + 1;
        }

        for (short i=start; i < prefixesUsed; i++) {
            if (prefixes[i].equals(prefix)) {
                return i;
            }
        }
        if (prefixesUsed >= prefixes.length) {
            if (prefixesUsed > 32000) {
                throw new NamePoolLimitException("Too many namespace prefixes");
            }
            String[] p = new String[prefixesUsed * 2];
            System.arraycopy(prefixes, 0, p, 0, prefixesUsed);
            prefixes = p;
        }
        prefixes[prefixesUsed] = prefix;
        return prefixesUsed++;
    }


    /**
     * Get the prefix code for a given Prefix
     *
     * @return -1 if not found
     */

    public short getCodeForPrefix(String prefix) {
        for (short i = 0; i < prefixesUsed; i++) {
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
        if (uriCode == -1) {
            return null;
        }
        if (prefixesForUri[uriCode].length >= 1) {
            return prefixesForUri[uriCode][0];
        }
        return null;
    }

    /**
     * Get a prefix among all the prefixes used with a given URI, given its index
     * @return the prefix with the given index. If the index is 0, the prefix is always "".
     */

    public String getPrefixWithIndex(short uriCode, int index) {
        if (index == 0) {
            return "";
        }
        return prefixesForUri[uriCode][index-1];
    }

    /**
     * Allocate a name from the pool, or a new Name if there is not a matching one there
     *
     * @param prefix
     * @param uri       - the namespace URI. The null URI is represented as an empty string.
     * @param localName
     * @return an integer (the "namecode") identifying the name within the namepool.
     *         The Name itself may be retrieved using the getName(int) method
     */

    public synchronized int allocate(String prefix, String uri, String localName) {
        if (NamespaceConstant.isReserved(uri) || uri.equals(NamespaceConstant.SAXON)) {
            int fp = StandardNames.getFingerprint(uri, localName);
            if (fp != -1) {
                short uriCode = StandardNames.getURICode(fp);
                int pindex;
                if (prefix.equals("")) {
                    pindex = 0;
                } else {
                    final String[] prefixes = prefixesForUri[uriCode];
                    int prefixPosition = Arrays.asList(prefixes).indexOf(prefix);
                    if (prefixPosition < 0) {
                        if (prefixes.length == MAX_PREFIXES_PER_URI) {
                            throw new NamePoolLimitException("NamePool limit exceeded: max " +
                                    MAX_PREFIXES_PER_URI + " prefixes per URI");
                        }
                        String[] p2 = new String[prefixes.length + 1];
                        System.arraycopy(prefixes, 0, p2, 0, prefixes.length);
                        p2[prefixes.length] = prefix;
                        prefixesForUri[uriCode] = p2;
                        prefixPosition = prefixes.length;
                    }
                    pindex = prefixPosition + 1;
                }
                return (pindex << 20) + fp;
            }
        }
        // otherwise register the name in this NamePool
        short uriCode = allocateCodeForURI(uri);
        return allocateInternal(prefix, uriCode, localName);
    }

    /**
     * Allocate a name from the pool, or a new Name if there is not a matching one there
     *
     * @param prefix    - the namespace prefix
     * @param uriCode   - the code of the URI
     * @param localName - the local part of the QName
     * @return an integer (the "namecode") identifying the name within the namepool.
     */

    public synchronized int allocate(String prefix, short uriCode, String localName) {
        // System.err.println("Allocate " + prefix + " : " + uriCode + " : " + localName);
        if (NamespaceConstant.isSpecialURICode(uriCode)) {
            return allocate(prefix, getURIFromURICode(uriCode), localName);
        } else {
            return allocateInternal(prefix, uriCode, localName);
        }
    }

    private int allocateInternal(String prefix, short uriCode, String localName) {
        int hash = (localName.hashCode() & 0x7fffffff) % 1023;
        int depth = 1;

        final String[] prefixes = prefixesForUri[uriCode];
        int prefixIndex;
        if (prefix.equals("")) {
            prefixIndex = 0;
        } else {
            int prefixPosition = Arrays.asList(prefixes).indexOf(prefix);
            if (prefixPosition < 0) {
                if (prefixes.length == MAX_PREFIXES_PER_URI) {
                    throw new NamePoolLimitException("NamePool limit exceeded: max " +
                                    MAX_PREFIXES_PER_URI + " prefixes per URI");
                }
                String[] p2 = new String[prefixes.length + 1];
                System.arraycopy(prefixes, 0, p2, 0, prefixes.length);
                p2[prefixes.length] = prefix;
                prefixesForUri[uriCode] = p2;
                prefixPosition = prefixes.length;
            }
            prefixIndex = prefixPosition + 1;
        }

        NameEntry entry;

        if (hashslots[hash] == null) {
            entry = new NameEntry(uriCode, localName);
            hashslots[hash] = entry;
        } else {
            entry = hashslots[hash];
            while (true) {
                boolean sameLocalName = (entry.localName.equals(localName));
                boolean sameURI = (entry.uriCode == uriCode);

                if (sameLocalName && sameURI) {
                    // may need to add a new prefix to the entry
                    break;
                } else {
                    NameEntry next = entry.nextEntry;
                    depth++;
                    if (depth >= 1024) {
                        throw new NamePoolLimitException("Saxon name pool is full");
                    }
                    if (next == null) {
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
        return ((prefixIndex << 20) + (depth << 10) + hash);
    }

    /**
     * Allocate a namespace code for the prefix/URI of a given namecode
     *
     * @param namecode a code identifying an expanded QName, e.g. of an element or attribute
     * @return a code identifying the namespace used in the given name. The namespace code
     *         identifies both the prefix and the URI.
     */

    public synchronized int allocateNamespaceCode(int namecode) {
        short uriCode;
        int fp = namecode & FP_MASK;
        if ((fp & USER_DEFINED_MASK) == 0) {
            uriCode = StandardNames.getURICode(fp);
        } else {
            NameEntry entry = getNameEntry(namecode);
            if (entry == null) {
                unknownNameCode(namecode);
                return -1;  // to keep the compiler happy
            } else {
                uriCode = entry.uriCode;
            }
        }
        int prefixIndex = getPrefixIndex(namecode);
        String prefix = getPrefixWithIndex(uriCode, prefixIndex);
        int prefixCode = allocateCodeForPrefix(prefix);
        return (prefixCode << 16) + uriCode;
    }

    /**
     * Get the namespace-URI of a name, given its name code or fingerprint
     * @return the namespace URI corresponding to this name code. Returns "" for the
     * null namespace.
     * @throws IllegalArgumentException if the nameCode is not known to the NamePool.
     */

    public String getURI(int nameCode) {
        if ((nameCode & USER_DEFINED_MASK) == 0) {
            return StandardNames.getURI(nameCode & FP_MASK);
        }
        NameEntry entry = getNameEntry(nameCode);
        if (entry == null) {
            unknownNameCode(nameCode);
            return null;    // to keep the compiler happy
        }
        return uris[entry.uriCode];
    }

    /**
     * Get the URI code of a name, given its name code or fingerprint
     */

    public short getURICode(int nameCode) {
        if ((nameCode & USER_DEFINED_MASK) == 0) {
            return StandardNames.getURICode(nameCode & FP_MASK);
        }
        NameEntry entry = getNameEntry(nameCode);
        if (entry == null) {
            unknownNameCode(nameCode);
            return -1;
        }
        return entry.uriCode;
    }

    /**
     * Get the local part of a name, given its name code or fingerprint
     */

    public String getLocalName(int nameCode) {
        if ((nameCode & USER_DEFINED_MASK) == 0) {
            return StandardNames.getLocalName(nameCode & FP_MASK);
        }
        NameEntry entry = getNameEntry(nameCode);
        if (entry == null) {
            unknownNameCode(nameCode);
            return null;
        }
        return entry.localName;
    }

    /**
     * Get the prefix part of a name, given its name code or fingerprint
     */

    public String getPrefix(int nameCode) {
        if ((nameCode & USER_DEFINED_MASK) == 0) {
            return StandardNames.getPrefix(nameCode & FP_MASK);
        }
        short uriCode = getURICode(nameCode);
        int prefixIndex = getPrefixIndex(nameCode);
        return getPrefixWithIndex(uriCode, prefixIndex);
    }

    /**
     * Get the display form of a name (the QName), given its name code or fingerprint
     */

    public String getDisplayName(int nameCode) {
        if ((nameCode & USER_DEFINED_MASK) == 0) {
            // This indicates a standard name known to the system (but it might have a non-standard prefix)
            int prefixIndex = getPrefixIndex(nameCode);
            short uriCode = getURICode(nameCode);
            String prefix = getPrefixWithIndex(uriCode, prefixIndex);
            if (prefix.equals("")) {
                return StandardNames.getLocalName(nameCode & FP_MASK);
            } else {
                return prefix + ':' + StandardNames.getLocalName(nameCode & FP_MASK);
            }
        }

        NameEntry entry = getNameEntry(nameCode);
        if (entry == null) {
            unknownNameCode(nameCode);
            return null;
        }
        int prefixIndex = getPrefixIndex(nameCode);
        String prefix = getPrefixWithIndex(entry.uriCode, prefixIndex);
        if (prefix == null || prefix.equals("")) {
            return entry.localName;
        } else {
            return prefix + ':' + entry.localName;
        }
    }

    /**
     * Get the Clark form of a name, given its name code or fingerprint
     *
     * @return the local name if the name is in the null namespace, or "{uri}local"
     *         otherwise. The name is always interned.
     */

    public String getClarkName(int nameCode) {
        if ((nameCode & USER_DEFINED_MASK) == 0) {
            return StandardNames.getClarkName(nameCode & FP_MASK);
        }
        NameEntry entry = getNameEntry(nameCode);
        if (entry == null) {
            unknownNameCode(nameCode);
            return null;
        }
        if (entry.uriCode == 0) {
            return entry.localName;
        } else {
            String n = '{' + getURIFromURICode(entry.uriCode) + '}' + entry.localName;
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
     * Parse a Clark-format expanded name, returning the URI and local name
     */

    public static String[] parseClarkName(String expandedName) {
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
        String[] result = {namespace, localName};
        return result;
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
     *
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
            for (short j = 0; j < urisUsed; j++) {
                if (uris[j].equals(uri)) {
                    uriCode = j;
                    break;
                }
            }
            if (uriCode == -1) {
                return -1;
            }
        }

        int hash = (localName.hashCode() & 0x7fffffff) % 1023;
        int depth = 1;

        NameEntry entry;

        if (hashslots[hash] == null) {
            return -1;
        }

        entry = hashslots[hash];
        while (true) {
            if (entry.uriCode == uriCode && entry.localName.equals(localName)) {
                break;
            } else {
                NameEntry next = entry.nextEntry;
                depth++;
                if (next == null) {
                    return -1;
                } else {
                    entry = next;
                }
            }
        }
        return (depth << 10) + hash;
    }

    /**
     * Get the namespace URI from a namespace code.
     */

    public String getURIFromNamespaceCode(int code) {
        return uris[code & 0xffff];
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
        return prefixes[code >> 16];
    }

    /**
     * Get the nameCode for a lexical QName, given a namespace resolver.
     * @param qname the lexical QName.
     * @param useDefault if true, an absent prefix is resolved by the NamespaceResolver
     * to the namespace URI assigned to the prefix "". If false, an absent prefix is
     * interpreted as meaning the name is in no namespace.
     * @param checker NameChecker used to check names against the XML 1.0 or 1.1 specification
     * @return the corresponding nameCode
     * @throws org.orbeon.saxon.trans.DynamicError if the string is not a valid lexical QName or
     * if the namespace prefix has not been declared*
     */

    public int allocateLexicalQName(CharSequence qname, boolean useDefault,
                                    NamespaceResolver resolver, NameChecker checker)
    throws DynamicError {
        try {
            String[] parts = checker.getQNameParts(qname);
            String uri = resolver.getURIForPrefix(parts[0], useDefault);
            if (uri == null) {
                throw new DynamicError("Namespace prefix '" + parts[0] + "' has not been declared");
            }
            return allocate(parts[0], uri, parts[1]);
        } catch (QNameException e) {
            throw new DynamicError(e.getMessage());
        }
    }
    /**
     * Get fingerprint for expanded name in {uri}local format
     */

    public int getFingerprintForExpandedName(String expandedName) {

        String localName;
        String namespace;

        if (expandedName.charAt(0) == '{') {
            int closeBrace = expandedName.indexOf('}');
            if (closeBrace < 0) {
                throw new IllegalArgumentException("No closing '}' in parameter name");
            }
            namespace = expandedName.substring(1, closeBrace);
            if (closeBrace == expandedName.length()) {
                throw new IllegalArgumentException("Missing local part in parameter name");
            }
            localName = expandedName.substring(closeBrace + 1);
        } else {
            namespace = "";
            localName = expandedName;
        }

        return allocate("", namespace, localName);
    }

    /**
     * Save client data on behalf of a user of the namepool
     */

    public void setClientData(Class key, Object value) {
        if (clientData == null) {
            clientData = new HashMap(10);
        }
        clientData.put(key, value);
    }

    /**
     * Retrieve client data on behalf of a user of the namepool
     */

    public Object getClientData(Class key) {
        if (clientData == null) {
            return null;
        }
        return clientData.get(key);
    }

    /**
     * Diagnostic print of the namepool contents.
     */

    public synchronized void diagnosticDump() {
        System.err.println("Contents of NamePool " + this);
        for (int i = 0; i < 1024; i++) {
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

        for (int p = 0; p < prefixesUsed; p++) {
            System.err.println("Prefix " + p + " = " + prefixes[p]);
        }
        for (int u = 0; u < urisUsed; u++) {
            System.err.println("URI " + u + " = " + uris[u]);
            System.err.println("Prefixes for URI " + u + " = " + prefixesForUri[u]);
        }
    }

    /**
     * Statistics summarizing the namepool contents.
     * This method outputs summary statistical information to System.err
     */

    public synchronized void statistics() {
        int slots = 0;
        int entries = 0;
        for (int i = 0; i < 1024; i++) {
            NameEntry entry = hashslots[i];
            if (entry != null) slots++;
            while (entry != null) {
                entry = entry.nextEntry;
                entries++;
            }
        }
        System.err.println("NamePool contents: " + entries + " entries in " + slots + " chains. " +
                 + prefixesUsed + " prefixes, " + urisUsed + " URIs");
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
