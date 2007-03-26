package org.orbeon.saxon.trans;
import org.orbeon.saxon.Configuration;
import org.orbeon.saxon.Platform;
import org.orbeon.saxon.expr.*;
import org.orbeon.saxon.functions.SystemFunction;
import org.orbeon.saxon.functions.Tokenize;
import org.orbeon.saxon.instruct.SlotManager;
import org.orbeon.saxon.om.*;
import org.orbeon.saxon.om.ListIterator;
import org.orbeon.saxon.pattern.IdrefTest;
import org.orbeon.saxon.pattern.NodeTestPattern;
import org.orbeon.saxon.pattern.Pattern;
import org.orbeon.saxon.pattern.UnionPattern;
import org.orbeon.saxon.sort.IntHashMap;
import org.orbeon.saxon.sort.LocalOrderComparer;
import org.orbeon.saxon.sort.IntIterator;
import org.orbeon.saxon.style.StandardNames;
import org.orbeon.saxon.type.Type;
import org.orbeon.saxon.value.AtomicValue;
import org.orbeon.saxon.value.NumericValue;
import org.orbeon.saxon.value.StringValue;

import java.io.Serializable;
import java.lang.ref.WeakReference;
import java.util.*;

/**
  * KeyManager manages the set of key definitions in a stylesheet, and the indexes
  * associated with these key definitions. It handles xsl:sort-key as well as xsl:key
  * definitions.
  *
  * <p>The memory management in this class is subtle, with extensive use of weak references.
  * The idea is that an index should continue to exist in memory so long as both the compiled
  * stylesheet and the source document exist in memory: if either is removed, the index should
  * go too. The document itself holds no reference to the index. The compiled stylesheet (which
  * owns the KeyManager) holds a weak reference to the index. The index, of course, holds strong
  * references to the nodes in the document. The Controller holds a strong reference to the
  * list of indexes used for each document, so that indexes remain in memory for the duration
  * of a transformation even if the documents themselves are garbage collected.</p>
  *
  * <p>Potentially there is a need for more than one index for a given key name, depending
  * on the primitive type of the value provided to the key() function. An index is built
  * corresponding to the type of the requested value; if subsequently the key() function is
  * called with the same name and a different type of value, then a new index is built.</p>
  *
  * @author Michael H. Kay
  */

public class KeyManager implements Serializable {

    private IntHashMap keyList;      // one entry for each named key; the entry contains
                                     // a list of key definitions with that name
    private transient WeakHashMap docIndexes;
                                     // one entry for each document that is in memory;
                                     // the entry contains a HashMap mapping the fingerprint of
                                     // the key name plus the primitive item type
                                     // to the HashMap that is the actual index
                                     // of key/value pairs.

    /**
    * create a KeyManager and initialise variables
    */

    public KeyManager(Configuration config) {
        keyList = new IntHashMap(10);
        docIndexes = new WeakHashMap(10);
        // Create a key definition for the idref() function
        registerIdrefKey(config);
    }

    /**
     * An internal key definition is used to support the idref() function. The key definition
     * is equivalent to xsl:key match="element(*, xs:IDREF) | element(*, IDREFS) |
     * attribute(*, xs:IDREF) | attribute(*, IDREFS)" use=".". This method creates this
     * key definition.
     * @param config The configuration. This is needed because the patterns that are
     * generated need access to schema information.
     */

    private void registerIdrefKey(Configuration config) {
        Pattern idrefAtt = new NodeTestPattern(new IdrefTest(Type.ATTRIBUTE));
        Pattern idrefElem = new NodeTestPattern(new IdrefTest(Type.ELEMENT));
        Pattern all = new UnionPattern(idrefAtt, idrefElem);
        Expression eval = new Atomizer(new ContextItemExpression(), config);
        Tokenize use = (Tokenize)SystemFunction.makeSystemFunction("tokenize", 2, config.getNamePool());
        StringValue regex = new StringValue("\\s");
        Expression[] params = {eval, regex};
        use.setArguments(params);
        KeyDefinition key = new KeyDefinition(all, use, null, null);
        try {
            addKeyDefinition(StandardNames.XS_IDREFS, key, config);
        } catch (XPathException err) {
            throw new AssertionError(err); // shouldn't happen
        }
    }

    /**
    * Register a key definition. Note that multiple key definitions with the same name are
    * allowed
    * @param fingerprint Integer representing the name of the key
     * @param keydef The details of the key's definition
     * @param config The configuration
     */

    public void addKeyDefinition(int fingerprint, KeyDefinition keydef, Configuration config)
    throws XPathException {
        //Integer keykey = new Integer(fingerprint);
        ArrayList v = (ArrayList)keyList.get(fingerprint);
        if (v==null) {
            v = new ArrayList(3);
            keyList.put(fingerprint, v);
        } else {
            // check the consistency of the key definitions
            String collation = keydef.getCollationName();
            if (collation == null) {
                for (int i=0; i<v.size(); i++) {
                    if (((KeyDefinition)v.get(i)).getCollationName() != null) {
                        StaticError err = new StaticError("All keys with the same name must use the same collation");
                        err.setErrorCode("XTSE1220");
                        throw err;
                    }
                }
            } else {
                for (int i=0; i<v.size(); i++) {
                    if (!collation.equals(((KeyDefinition)v.get(i)).getCollationName())) {
                        StaticError err = new StaticError("All keys with the same name must use the same collation");
                        err.setErrorCode("XTSE1220");
                        throw err;
                    }
                }
            }

        }
        v.add(keydef);
        boolean backwardsCompatible = false;
        for (int i=0; i<v.size(); i++) {
            if (((KeyDefinition)v.get(i)).isBackwardsCompatible()) {
                backwardsCompatible = true;
                break;
            }
        }
        if (backwardsCompatible) {
            // In backwards compatibility mode, convert all the use-expression results to sequences of strings
            for (int i=0; i<v.size(); i++) {
                KeyDefinition kd = (KeyDefinition)v.get(i);
                kd.setBackwardsCompatible(true);
                if (kd.getBody().getItemType(config.getTypeHierarchy()) != Type.STRING_TYPE) {
                    Expression exp = new AtomicSequenceConverter(kd.getBody(), Type.STRING_TYPE);
                    kd.setBody(exp);
                }
            }
        }

    }

    /**
    * Get all the key definitions that match a particular fingerprint
    * @param fingerprint The fingerprint of the name of the required key
    * @return The key definition of the named key if there is one, or null otherwise.
    */

    public List getKeyDefinitions(int fingerprint) {
        return (List)keyList.get(fingerprint);
    }

    /**
    * Build the index for a particular document for a named key
    * @param fingerprint The fingerprint of the name of the required key
    * @param doc The source document in question
    * @param context The dynamic context
    * @return the index in question, as a HashMap mapping a key value onto a ArrayList of nodes
    */

    private synchronized HashMap buildIndex(int fingerprint,
                                            int itemType,
                                            DocumentInfo doc,
                                            XPathContext context) throws XPathException {

        List definitions = getKeyDefinitions(fingerprint);
        if (definitions==null) {
            DynamicError de = new DynamicError("Key " +
            		context.getNamePool().getDisplayName(fingerprint) +
            							" has not been defined");
            de.setXPathContext(context);
            de.setErrorCode("XTDE1260");
            throw de;
        }

        HashMap index = new HashMap(100);

        // There may be multiple xsl:key definitions with the same name. Index them all.
        for (int k=0; k<definitions.size(); k++) {
            constructIndex( doc,
                            index,
                            (KeyDefinition)definitions.get(k),
                            itemType,
                            context,
                            k==0);
        }

        return index;

    }

    /**
    * Process one key definition to add entries to an index
    */

    private void constructIndex(    DocumentInfo doc,
                                    HashMap index,
                                    KeyDefinition keydef,
                                    int soughtItemType,
                                    XPathContext context,
                                    boolean isFirst) throws XPathException {

        Pattern match = keydef.getMatch();
        Expression use = keydef.getUse();
        Comparator collator = keydef.getCollation();
        boolean strict = keydef.isStrictComparison();

        NodeInfo curr;
        XPathContextMajor xc = context.newContext();
        xc.setOrigin(keydef);

        // The use expression (or sequence constructor) may contain local variables.
        SlotManager map = keydef.getStackFrameMap();
        if (map != null) {
            xc.openStackFrame(map);
        }

        int nodeType = match.getNodeKind();

        if (nodeType==Type.ATTRIBUTE || nodeType==Type.NODE || nodeType==Type.DOCUMENT) {
            // If the match pattern allows attributes to appear, we must visit them.
            // We also take this path in the pathological case where the pattern can match
            // document nodes.
            SequenceIterator all = doc.iterateAxis(Axis.DESCENDANT_OR_SELF);
            while(true) {
                curr = (NodeInfo)all.next();
                if (curr==null) {
                    break;
                }
                if (curr.getNodeKind()==Type.ELEMENT) {
                    SequenceIterator atts = curr.iterateAxis(Axis.ATTRIBUTE);
                    while (true) {
                        NodeInfo att = (NodeInfo)atts.next();
                        if (att == null) {
                            break;
                        }

                        if (match.matches(att, xc)) {
                            processKeyNode(att, use, soughtItemType, strict,
                                            collator, index, xc, isFirst);
                        }
                    }
                    if (nodeType==Type.NODE) {
                        // index the element as well as its attributes
                        if (match.matches(curr, xc)) {
                            processKeyNode(curr, use, soughtItemType, strict,
                                        collator, index, xc, isFirst);
                        }
                    }
                } else {
                    if (match.matches(curr, xc)) {
                        processKeyNode(curr, use, soughtItemType, strict,
                                     collator, index, xc, isFirst);
                    }
                }
            }

        } else {
            SequenceIterator all =
                doc.iterateAxis( Axis.DESCENDANT,
                                    match.getNodeTest());
            // If the match is a nodetest, we avoid testing it again
            while(true) {
                curr = (NodeInfo)all.next();
                if (curr == null) {
                    break;
                }
                if (match instanceof NodeTestPattern || match.matches(curr, xc)) {
                    processKeyNode(curr, use, soughtItemType, strict,
                                collator, index, xc, isFirst);
                }
            }
        }
    }

    /**
    * Process one matching node, adding entries to the index if appropriate
     * @param curr the node being processed
     * @param use the expression used to compute the key values for this node
     * @param soughtItemType the primitive item type of the argument to the key() function that triggered
     * this index to be built
     * @param strict true if strict comparison semantics (ValueComparison) are required
     * @param collation the collation defined in the key definition
     * @param index the index being constructed
     * @param xc the context for evaluating expressions
     * @param isFirst indicates whether this is the first key definition with a given key name (which means
     * no sort of the resulting key entries is required)
    */

    private void processKeyNode(    NodeInfo curr,
                                    Expression use,
                                    int soughtItemType,
                                    boolean strict,
                                    Comparator collation,
                                    HashMap index,
                                    XPathContext xc,
                                    boolean isFirst) throws XPathException {


        // Make the node we are testing the context node,
        // with context position and context size set to 1

        AxisIterator si = SingletonIterator.makeIterator(curr);
        si.next();    // need to position iterator at first node

        xc.setCurrentIterator(si);

        Platform platform = null;
        if (collation != null) {
            platform = xc.getConfiguration().getPlatform();
        }

        // Evaluate the "use" expression against this context node

        SequenceIterator useval = use.iterate(xc);
        while (true) {
            AtomicValue item = (AtomicValue)useval.next();
            if (item == null) {
                break;
            }
            int actualItemType = item.getItemType(null).getPrimitiveType();
            if (!Type.isComparable(actualItemType, soughtItemType, false)) {
                // the types aren't comparable
                if (strict) {
                    DynamicError de = new DynamicError("Cannot compare " + xc.getNamePool().getDisplayName(soughtItemType) +
                                              " to " + xc.getNamePool().getDisplayName(actualItemType)
                                              + " using 'eq'");
                    de.setErrorCode("XPTY0004");
                    throw de;
                } else {
                    // simply ignore this key value
                    break;
                }
            }
            Object val;

            if (soughtItemType==Type.UNTYPED_ATOMIC) {
                // if the supplied key value is untyped atomic, we build an index using the
                // actual type returned by the use expression
                if (collation==null) {
                    val = item.getStringValue();
                } else {
                    val = platform.getCollationKey(collation, item.getStringValue());
                }
            } else if (soughtItemType==Type.STRING) {
                // if the supplied key value is a string, there is no match unless the use expression
                // returns a string or an untyped atomic value
                if (collation==null) {
                    val = item.getStringValue();
                } else {
                    val = platform.getCollationKey(collation, item.getStringValue());
                }
            } else {
                // Ignore NaN values
                if (item instanceof NumericValue && ((NumericValue)item).isNaN()) {
                    break;
                }
                try {
                    val = item.convert(soughtItemType, xc);
                } catch (XPathException err) {
                    // ignore values that can't be converted to the required type
                    break;
                }
            }

            ArrayList nodes = (ArrayList)index.get(val);
            if (nodes==null) {
                // this is the first node with this key value
                nodes = new ArrayList(4);
                index.put(val, nodes);
                nodes.add(curr);
            } else {
                // this is not the first node with this key value.
                // add the node to the list of nodes for this key,
                // unless it's already there
                if (isFirst) {
                    // if this is the first index definition that we're processing,
                    // then this node must be after all existing nodes in document
                    // order, or the same node as the last existing node
                    if (nodes.get(nodes.size()-1)!=curr) {
                        nodes.add(curr);
                    }
                } else {
                    // otherwise, we need to insert the node at the correct
                    // position in document order. This code does an insertion sort:
                    // not ideal for performance, but it's very unusual to have more than
                    // one key definition for a key.
                    LocalOrderComparer comparer = LocalOrderComparer.getInstance();
                    for (int i=0; i<nodes.size(); i++) {
                        int d = comparer.compare(curr, (NodeInfo)nodes.get(i));
                        if (d<=0) {
                            if (d==0) {
                                // node already in list; do nothing
                            } else {
                                // add the node at this position
                                nodes.add(i, curr);
                            }
                            return;
                        }
                        // else continue round the loop
                    }
                    // if we're still here, add the new node at the end
                    nodes.add(curr);
                }
            }
        }

    }

    /**
    * Get the nodes with a given key value
    * @param fingerprint The fingerprint of the name of the required key
    * @param doc The source document in question
    * @param value The required key value
    * @param context The dynamic context, needed only the first time when the key is being built
    * @return an enumeration of nodes, always in document order
    */

    public SequenceIterator selectByKey(
                                int fingerprint,
                                DocumentInfo doc,
                                AtomicValue value,
                                XPathContext context) throws XPathException {


        List definitions = getKeyDefinitions(fingerprint);
        if (definitions == null) {
            DynamicError de = new DynamicError("Key " +
            		context.getNamePool().getDisplayName(fingerprint) +
            							" has not been defined");
            de.setXPathContext(context);
            de.setErrorCode("XTDE1260");
            throw de;
        }
        KeyDefinition definition = (KeyDefinition)definitions.get(0);
               // the itemType and collation and BC mode will be the same for all keys with the same name
        Comparator collation = definition.getCollation();
        Platform platform = null;
        if (collation != null) {
            platform = context.getConfiguration().getPlatform();
        }
        boolean backwardsCompatible = definition.isBackwardsCompatible();

        if (backwardsCompatible) {
            value = value.convert(Type.STRING, context);
        }

        // If the key value is numeric, promote it to a double
        // TODO: this could result in two decimals comparing equal because they convert to the same double

        int itemType = value.getItemType(null).getPrimitiveType();
        if (itemType == StandardNames.XS_INTEGER ||
                itemType == StandardNames.XS_DECIMAL ||
                itemType == StandardNames.XS_FLOAT) {
            itemType = StandardNames.XS_DOUBLE;
            value = value.convert(itemType, context);
        }

        // No special action needed for anyURI to string promotion (it just seems to work: tests idky44, 45)

        Object indexObject = getIndex(doc, fingerprint, itemType);
        if (indexObject instanceof String) {
            // index is under construction
            DynamicError de = new DynamicError("Key definition is circular");
            de.setXPathContext(context);
            de.setErrorCode("XTDE0640");
            throw de;
        }
        HashMap index = (HashMap)indexObject;

        // If the index does not yet exist, then create it.
        if (index==null) {
            // Mark the index as being under construction, in case the definition is circular
            putIndex(doc, fingerprint, itemType, "Under Construction", context);
            index = buildIndex(fingerprint, itemType, doc, context);
            putIndex(doc, fingerprint, itemType, index, context);
        }

        Object val;
        if (itemType==Type.STRING || itemType==Type.UNTYPED_ATOMIC) {
            if (collation==null) {
                val = value.getStringValue();
            } else {
                val = platform.getCollationKey(collation, value.getStringValue());
            }
        } else {
            val = value;
        }

        ArrayList nodes = (ArrayList)index.get(val);
        if (nodes==null) {
            return EmptyIterator.getInstance();
        } else {
            return new ListIterator(nodes);
        }
    }

    /**
    * Save the index associated with a particular key, a particular item type,
    * and a particular document. This
    * needs to be done in such a way that the index is discarded by the garbage collector
    * if the document is discarded. We therefore use a WeakHashMap indexed on the DocumentInfo,
    * which returns HashMap giving the index for each key fingerprint. This index is itself another
    * HashMap.
    * The methods need to be synchronized because several concurrent transformations (which share
    * the same KeyManager) may be creating indexes for the same or different documents at the same
    * time.
    */

    private synchronized void putIndex(DocumentInfo doc, int keyFingerprint,
                                       int itemType, Object index, XPathContext context) {
        if (docIndexes==null) {
            // it's transient, so it will be null when reloading a compiled stylesheet
            docIndexes = new WeakHashMap(10);
        }
        WeakReference indexRef = (WeakReference)docIndexes.get(doc);
        HashMap indexList;
        if (indexRef==null || indexRef.get()==null) {
            indexList = new HashMap(10);
            // ensure there is a firm reference to the indexList for the duration of a transformation
            context.getController().setUserData(doc, "key-index-list", indexList);
            docIndexes.put(doc, new WeakReference(indexList));
        } else {
            indexList = (HashMap)indexRef.get();
        }
        indexList.put(new Long(((long)keyFingerprint)<<32 | itemType), index);
    }

    /**
    * Get the index associated with a particular key, a particular source document,
     * and a particular primitive item type
    */

    private synchronized Object getIndex(DocumentInfo doc, int keyFingerprint, int itemType) {
        if (docIndexes==null) {
            // it's transient, so it will be null when reloading a compiled stylesheet
            docIndexes = new WeakHashMap(10);
        }
        WeakReference ref = (WeakReference)docIndexes.get(doc);
        if (ref==null) return null;
        HashMap indexList = (HashMap)ref.get();
        if (indexList==null) return null;
        return indexList.get(new Long(((long)keyFingerprint)<<32 | itemType));
    }

    /**
     * Diagnostic output explaining the keys
     */

    public void explainKeys(Configuration config) {
        if (keyList.size() < 2) {
            // don't bother with IDREFS if it's the only index
            return;
        }
        System.err.println("============ Indexes ======================");
        IntIterator keyIter = keyList.keyIterator();
        while (keyIter.hasNext()) {
            int fp = keyIter.next();
            List list = (List)keyList.get(fp);
            for (int i=0; i<list.size(); i++) {
                KeyDefinition kd = (KeyDefinition)list.get(i);
                System.err.println(" Index " + config.getNamePool().getDisplayName(fp));
                System.err.println("   match = " + kd.getMatch().toString());
                System.err.println("   use = ");
                kd.getUse().display(10, System.err, config);
            }
        }
        System.err.println("===========================================");

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
