package org.orbeon.saxon.trans;

import org.orbeon.saxon.om.StructuredQName;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * A set of xsl:key definitions in a stylesheet that share the same name
 */

public class KeyDefinitionSet implements Serializable {

    StructuredQName keyName;
    int keySetNumber;               // unique among the KeyDefinitionSets within a KeyManager
    List keyDefinitions;
    String collationName;
    boolean backwardsCompatible;    // true if any of the keys is backwards compatible

    /**
     * Create a key definition set for keys sharing a given name
     * @param keyName the name of the key definitions in this set
     * @param keySetNumber a unique number identifying this key definition set
     */

    public KeyDefinitionSet(StructuredQName keyName, int keySetNumber) {
        this.keyName = keyName;
        this.keySetNumber = keySetNumber;
        keyDefinitions = new ArrayList(3);
    }

    /**
     * Add a key definition to this set of key definitions. The caller is responsible for ensuring that
     * all key definitions in a key definition set have the same name
     * @param keyDef the key definition to be added
     * @throws XPathException if the key definition uses a different collation from others in the set
     */

    public void addKeyDefinition(KeyDefinition keyDef) throws XPathException {
        if (keyDefinitions.isEmpty()) {
            collationName = keyDef.getCollationName();
        } else {
            if ((collationName == null && keyDef.getCollationName() != null) ||
                    (collationName != null && !collationName.equals(keyDef.getCollationName()))) {
                XPathException err = new XPathException("All keys with the same name must use the same collation");
                err.setErrorCode("XTSE1220");
                throw err;
            }
            // ignore this key definition if it is a duplicate of another already present
            List v = getKeyDefinitions();
            for (int i=0; i<v.size(); i++) {
                KeyDefinition other = (KeyDefinition)v.get(i);
                if (keyDef.getMatch().equals(other.getMatch()) &&
                        keyDef.getBody().equals(other.getBody())) {
                    return;
                }
            }
        }
        if (keyDef.isBackwardsCompatible()) {
            backwardsCompatible = true;
        }
        keyDefinitions.add(keyDef);
    }

    /**
     * Get the name of the key definitions in this set (they all share the same name)
     * @return the name of these key definitions
     */

    public StructuredQName getKeyName() {
        return keyName;
    }

    /**
     * Get the KeySet number. This uniquely identifies the KeyDefinitionSet within a KeyManager
     * @return the unique number
     */

    public int getKeySetNumber() {
        return keySetNumber;
    }

    /**
     * Get the key definitions in this set
     * @return the key definitions in this set
     */

    public List getKeyDefinitions() {
        return keyDefinitions;
    }

    /**
     * Determine if the keys are to be evaluated in backwards compatible mode
     * @return true if backwards compatibility is in force for at least one of the keys in the set
     */

    public boolean isBackwardsCompatible() {
        return backwardsCompatible;
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
// The Original Code is: all this file
//
// The Initial Developer of the Original Code is Michael H. Kay.
//
// Contributor(s):
//

