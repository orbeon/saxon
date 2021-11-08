package org.orbeon.saxon.trans;
import org.orbeon.saxon.Configuration;
import org.orbeon.saxon.expr.Expression;
import org.orbeon.saxon.expr.SequenceIterable;
import org.orbeon.saxon.instruct.Procedure;
import org.orbeon.saxon.instruct.SlotManager;
import org.orbeon.saxon.om.StructuredQName;
import org.orbeon.saxon.om.StandardNames;
import org.orbeon.saxon.pattern.PatternFinder;
import org.orbeon.saxon.sort.StringCollator;
import org.orbeon.saxon.type.BuiltInAtomicType;

import java.io.Serializable;

/**
  * Corresponds to a single xsl:key declaration.<P>
  * @author Michael H. Kay
  */

public class KeyDefinition extends Procedure implements Serializable {

    private PatternFinder match;          // the match pattern
    private SequenceIterable use;
    private BuiltInAtomicType useType;    // the type of the values returned by the atomized use expression
    private StringCollator collation;     // the collating sequence, when type=string
    private String collationName;         // the collation URI
    private boolean backwardsCompatible = false;
    private boolean strictComparison = false;
    private boolean convertUntypedToOther = false;

    /**
    * Constructor to create a key definition
     * @param match the pattern in the xsl:key match attribute
     * @param use the expression in the xsl:key use attribute, or the expression that results from compiling
     * the xsl:key contained instructions. Note that a KeyDefinition constructed by the XSLT or XQuery parser will
     * always use an Expression here; however, a KeyDefinition constructed at run-time by a compiled stylesheet
     * or XQuery might use a simple ExpressionEvaluator that lacks all the compile-time information associated
     * with an Expression
     * @param collationName the name of the collation being used
     * @param collation the actual collation. This must be one that supports generation of collation keys.
    */

    public KeyDefinition(PatternFinder match, SequenceIterable use, String collationName, StringCollator collation) {
        setHostLanguage(Configuration.XSLT);
        this.match = match;
        this.use = use;
        if (use instanceof Expression) {
            setBody((Expression)use);
        }
        this.collation = collation;
        this.collationName = collationName;
    }

    /**
     * Set the primitive item type of the values returned by the use expression
     * @param itemType the primitive type of the indexed values
     */

    public void setIndexedItemType(BuiltInAtomicType itemType) {
        useType = itemType;
    }

    /**
     * Get the primitive item type of the values returned by the use expression
     * @return the primitive item type of the indexed values
     */

    public BuiltInAtomicType getIndexedItemType() {
        if (useType == null) {
            return BuiltInAtomicType.ANY_ATOMIC;
        } else {
            return useType;
        }
    }

    /**
     * Set backwards compatibility mode. The key definition is backwards compatible if ANY of the xsl:key
     * declarations has version="1.0" in scope.
     * @param bc set to true if running in XSLT 2.0 backwards compatibility mode
     */

    public void setBackwardsCompatible(boolean bc) {
        backwardsCompatible = bc;
    }

    /**
     * Test backwards compatibility mode
     * @return true if running in XSLT backwards compatibility mode
     */

    public boolean isBackwardsCompatible() {
        return backwardsCompatible;
    }

    /**
     * Set whether strict comparison is needed. Strict comparison treats non-comparable values as an
     * error rather than a no-match. This is used for internal keys that support value comparisons in
     * Saxon-SA, it is not used for user-defined XSLT keys.
     * @param strict true if strict comparison is needed
     */

    public void setStrictComparison(boolean strict) {
        strictComparison = strict;
    }

    /**
     * Get whether strict comparison is needed. Strict comparison treats non-comparable values as an
     * error rather than a no-match. This is used for internal keys that support value comparisons in
     * Saxon-SA, it is not used for user-defined XSLT keys.
     * @return true if strict comparison is needed.
     */

    public boolean isStrictComparison() {
        return strictComparison;
    }

    /**
     * Indicate that untypedAtomic values should be converted to the type of the other operand,
     * rather than to strings. This is used for indexes constructed internally by Saxon-SA to
     * support filter expressions that use the "=" operator, as distinct from "eq".
     * @param convertToOther true if comparisons follow the semantics of the "=" operator rather than
     * the "eq" operator
     */

    public void setConvertUntypedToOther(boolean convertToOther) {
        convertUntypedToOther = convertToOther;
    }

    /**
     * Determine whether untypedAtomic values are converted to the type of the other operand.
     * @return true if comparisons follow the semantics of the "=" operator rather than
     * the "eq" operator
     */

    public boolean isConvertUntypedToOther() {
        return convertUntypedToOther;
    }

    /**
     * Set the map of local variables needed while evaluating the "use" expression
     */

    public void setStackFrameMap(SlotManager map) {
        if (map != null && map.getNumberOfVariables() > 0) {
            super.setStackFrameMap(map);
        }
    }

    /**
     * Set the system Id and line number of the source xsl:key definition
     * @param systemId the URI of the module containing the key definition
     * @param lineNumber the line number of the key definition
     */

    public void setLocation(String systemId, int lineNumber) {
        setSystemId(systemId);
        setLineNumber(lineNumber);
    }

    /**
    * Get the match pattern for the key definition
     * @return the pattern specified in the "match" attribute of the xsl:key declaration
    */

    public PatternFinder getMatch() {
        return match;
    }

    /**
     * Set the body of the key (the use expression). This is held redundantly as an Expression and
     * as a SequenceIterable (not sure why!)
     * @param body the use expression of the key
     */

    public void setBody(Expression body) {
        super.setBody(body);
        use = body;
    }

    /**
    * Get the use expression for the key definition
     * @return the expression specified in the "use" attribute of the xsl:key declaration
    */

    public SequenceIterable getUse() {
        return use;
    }

    /**
    * Get the collation name for this key definition.
    * @return the collation name (the collation URI)
    */

    public String getCollationName() {
        return collationName;
    }

    /**
    * Get the collation.
     * @return the collation
    */

    public StringCollator getCollation() {
        return collation;
    }

    /**
     * Get the type of construct. This will either be the fingerprint of a standard XSLT instruction name
     * (values in {@link org.orbeon.saxon.om.StandardNames}: all less than 1024)
     * or it will be a constant in class {@link org.orbeon.saxon.trace.Location}.
     */

    public int getConstructType() {
        return StandardNames.XSL_KEY;
    }

    /**
     * Get a name identifying the object of the expression, for example a function name, template name,
     * variable name, key name, element name, etc. This is used only where the name is known statically.
     *
     */

    public StructuredQName getObjectName() {
        return null; 
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
