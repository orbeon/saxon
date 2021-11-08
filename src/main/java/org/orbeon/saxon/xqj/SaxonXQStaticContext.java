package org.orbeon.saxon.xqj;

import org.orbeon.saxon.Configuration;
import org.orbeon.saxon.query.StaticQueryContext;
import org.orbeon.saxon.om.NamespaceConstant;
import org.orbeon.saxon.om.Validation;

import javax.xml.xquery.XQConstants;
import javax.xml.xquery.XQException;
import javax.xml.xquery.XQItemType;
import javax.xml.xquery.XQStaticContext;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Saxon implementation of the XQJ XQStaticContext interface
 */
public class SaxonXQStaticContext implements XQStaticContext {

    private Configuration config;
    private int bindingMode = XQConstants.BINDING_MODE_IMMEDIATE;
    private int holdability = XQConstants.HOLDTYPE_HOLD_CURSORS_OVER_COMMIT;
    private int scrollability = XQConstants.SCROLLTYPE_FORWARD_ONLY;
    private Map namespaces = new HashMap();
    private String baseURI = "";
    boolean preserveBoundarySpace = false;
    boolean constructionModeIsPreserve = false;
    boolean inheritNamespaces = true;
    boolean preserveNamespaces = true;
    boolean emptyLeast = true;
    boolean isOrdered = true;
    SaxonXQItemType contextItemStaticType = null;
    String defaultCollationName = NamespaceConstant.CODEPOINT_COLLATION_URI;
    String defaultElementNamespace = "";
    String defaultFunctionNamespace = NamespaceConstant.FN;
    //private StaticQueryContext sqc;

    /**
     * Create a SaxonXQStaticContext object, the Saxon implementation of XQStaticContext in XQJ
     * @param config the Saxon configuration
     */

    public SaxonXQStaticContext(Configuration config) {
        this.config = config;
    }

    /**
     * Get a new Saxon StaticQueryContext object holding the information held in this
     * XQStaticContext
     * @return a newly constructed StaticQueryContext object
     */

    protected StaticQueryContext getSaxonStaticQueryContext() {
        StaticQueryContext sqc = new StaticQueryContext(config);
        sqc.setBaseURI(baseURI);
        sqc.setConstructionMode(constructionModeIsPreserve ? Validation.PRESERVE : Validation.STRIP);
        sqc.setDefaultElementNamespace(defaultElementNamespace);
        sqc.setDefaultFunctionNamespace(defaultFunctionNamespace);
        sqc.setEmptyLeast(emptyLeast);
        sqc.setInheritNamespaces(inheritNamespaces);
        sqc.setPreserveBoundarySpace(preserveBoundarySpace);
        sqc.setPreserveNamespaces(preserveNamespaces);
        if (contextItemStaticType != null) {
            sqc.setRequiredContextItemType(contextItemStaticType.getSaxonItemType());
        }
        for (Iterator iter = namespaces.keySet().iterator(); iter.hasNext();) {
            String prefix = (String)iter.next();
            String uri = (String)namespaces.get(prefix);
            sqc.declareNamespace(prefix, uri);
        }
        return sqc;
    }


    public void declareNamespace(String prefix, String uri) throws XQException {
        checkNotNull(prefix);
        checkNotNull(uri);
        if (uri.length() == 0) {
            namespaces.remove(prefix);
        } else {
            namespaces.put(prefix, uri);
        }
    }

    public String getBaseURI() {
        return baseURI;
    }


    public int getBindingMode() {
        return bindingMode;
    }

    public int getBoundarySpacePolicy() {
        return preserveBoundarySpace
                ? XQConstants.BOUNDARY_SPACE_PRESERVE
                : XQConstants.BOUNDARY_SPACE_STRIP;
    }

    public int getConstructionMode() {
        return constructionModeIsPreserve
                ? XQConstants.CONSTRUCTION_MODE_PRESERVE
                : XQConstants.CONSTRUCTION_MODE_STRIP;
    }


    public XQItemType getContextItemStaticType() {
        return contextItemStaticType;
    }

    public int getCopyNamespacesModeInherit()  {
        return inheritNamespaces ?
            XQConstants.COPY_NAMESPACES_MODE_INHERIT :
            XQConstants.COPY_NAMESPACES_MODE_NO_INHERIT;
    }

    public int getCopyNamespacesModePreserve() {
        return preserveNamespaces ?
            XQConstants.COPY_NAMESPACES_MODE_PRESERVE :
            XQConstants.COPY_NAMESPACES_MODE_NO_PRESERVE;
    }

    public String getDefaultCollation() {
        return defaultCollationName;
    }

    public String getDefaultElementTypeNamespace()  {
        return defaultElementNamespace;
    }

    public String getDefaultFunctionNamespace() {
        return defaultFunctionNamespace;
    }

    public int getDefaultOrderForEmptySequences() {
       return emptyLeast ?
           XQConstants.DEFAULT_ORDER_FOR_EMPTY_SEQUENCES_LEAST :
           XQConstants.DEFAULT_ORDER_FOR_EMPTY_SEQUENCES_GREATEST;
    }

    public String[] getNamespacePrefixes() {
        String[] result = new String[namespaces.size()];
        Iterator iter = namespaces.keySet().iterator();
        for (int i=0; i<result.length; i++) {
            iter.hasNext();
            result[i] = (String)iter.next();
        }
        return result;
    }

    public String getNamespaceURI(String prefix) throws XQException {
        checkNotNull(prefix);
        String uri = (String)namespaces.get(prefix);
        if (uri == null) {
            throw new XQException("Unknown prefix");
        }
        return uri;
    }

    public int getOrderingMode() {
        return isOrdered
                ? XQConstants.ORDERING_MODE_ORDERED
                : XQConstants.ORDERING_MODE_UNORDERED;
    }

    public int getHoldability() {
        return holdability;
    }

    public int getQueryLanguageTypeAndVersion() {
        return XQConstants.LANGTYPE_XQUERY;
    }

    public int getQueryTimeout() {
        return 0;
    }

    public int getScrollability() {
        return scrollability;
    }


    public void setBaseURI(String baseUri) throws XQException {
        checkNotNull(baseUri);
        this.baseURI = baseUri;
    }

    public void setBindingMode(int bindingMode) throws XQException {
        switch (bindingMode) {
            case XQConstants.BINDING_MODE_IMMEDIATE:
            case XQConstants.BINDING_MODE_DEFERRED:
                this.bindingMode = bindingMode;
                break;
            default:
                throw new XQException("Invalid value for binding mode - " + bindingMode);
        }
    }

    public void setBoundarySpacePolicy(int policy) throws XQException {
        switch (policy) {
            case XQConstants.BOUNDARY_SPACE_PRESERVE:
                preserveBoundarySpace = true;
                break;
            case XQConstants.BOUNDARY_SPACE_STRIP:
                preserveBoundarySpace = false;
                break;
            default:
                throw new XQException("Invalid value for boundary space policy - " + policy);
        }
    }

    public void setConstructionMode(int mode) throws XQException {
        switch (mode) {
            case XQConstants.CONSTRUCTION_MODE_PRESERVE:
                constructionModeIsPreserve = true;
                break;
            case XQConstants.CONSTRUCTION_MODE_STRIP:
                constructionModeIsPreserve = false;
                break;
            default:
                throw new XQException("Invalid value for construction mode - " + mode);
        }
    }

    public void setContextItemStaticType(XQItemType contextItemType) {
        this.contextItemStaticType = (SaxonXQItemType)contextItemType;
    }

    public void setCopyNamespacesModeInherit(int mode) throws XQException {
        switch (mode) {
            case XQConstants.COPY_NAMESPACES_MODE_INHERIT:
                inheritNamespaces = true;
                break;
            case XQConstants.COPY_NAMESPACES_MODE_NO_INHERIT:
                inheritNamespaces = false;
                break;
            default:
                throw new XQException("Invalid value for namespaces inherit mode - " + mode);
        }
    }

    public void setCopyNamespacesModePreserve(int mode) throws XQException {
        switch (mode) {
            case XQConstants.COPY_NAMESPACES_MODE_PRESERVE:
                preserveNamespaces = true;
                break;
            case XQConstants.COPY_NAMESPACES_MODE_NO_PRESERVE:
                preserveNamespaces = false;
                break;
            default:
                throw new XQException("Invalid value for namespaces preserve mode - " + mode);
        }
    }

    public void setDefaultCollation(String uri) throws XQException {
        checkNotNull(uri);
        defaultCollationName = uri;
    }

    public void setDefaultElementTypeNamespace(String uri) throws XQException {
        checkNotNull(uri);
        defaultElementNamespace = uri;
    }

    public void setDefaultFunctionNamespace(String uri) throws XQException {
        checkNotNull(uri);
        defaultFunctionNamespace = uri;
    }

    public void setDefaultOrderForEmptySequences(int order) throws XQException {
        switch (order) {
            case XQConstants.DEFAULT_ORDER_FOR_EMPTY_SEQUENCES_GREATEST:
                emptyLeast = false;
                break;
            case XQConstants.DEFAULT_ORDER_FOR_EMPTY_SEQUENCES_LEAST:
                emptyLeast = true;
                break;
            default:
                throw new XQException("Invalid value for default order for empty sequences - " + order);
        }
    }

    public void setOrderingMode(int mode) throws XQException {
        switch (mode) {
            case XQConstants.ORDERING_MODE_ORDERED:
                isOrdered = true;
                break;
            case XQConstants.ORDERING_MODE_UNORDERED:
                isOrdered = false;
                break;
            default:
                throw new XQException("Invalid ordering mode - " + mode);
        }
    }

    public void setQueryTimeout(int seconds) throws XQException {
        if (seconds < 0) {
            throw new XQException("Query timeout must not be negative");
        }
        // no-op
    }

    public void setHoldability(int holdability) throws XQException {
        switch (holdability) {
            case XQConstants.HOLDTYPE_HOLD_CURSORS_OVER_COMMIT:
            case XQConstants.HOLDTYPE_CLOSE_CURSORS_AT_COMMIT:
                this.holdability = holdability;
                break;
            default:
                throw new XQException("Invalid holdability value - " + holdability);
        }
    }

    public void setQueryLanguageTypeAndVersion(int langtype) throws XQException {
        if (langtype != XQConstants.LANGTYPE_XQUERY) {
            throw new XQException("XQueryX is not supported");
        }
    }

    public void setScrollability(int scrollability) throws XQException {
        switch (scrollability) {
            case XQConstants.SCROLLTYPE_FORWARD_ONLY:
            case XQConstants.SCROLLTYPE_SCROLLABLE:
                this.scrollability = scrollability;
                break;
            default:
                throw new XQException("Invalid scrollability value - " + scrollability);
        }
    }

    protected void checkNotNull(Object arg) throws XQException {
        if (arg == null) {
            throw new XQException("Argument is null");
        }
    }

//    private XQException newXQException(Exception err) {
//        XQException xqe = new XQException(err.getMessage());
//        xqe.initCause(err);
//        return xqe;
//    }

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

