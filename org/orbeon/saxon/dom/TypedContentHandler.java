package net.sf.saxon.dom;

import net.sf.saxon.event.ContentHandlerProxy;
import net.sf.saxon.event.ReceiverOptions;
import net.sf.saxon.om.NamespaceConstant;
import net.sf.saxon.style.StandardNames;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.AnySimpleType;
import net.sf.saxon.type.AnyType;
import net.sf.saxon.type.SchemaType;
import org.w3c.dom.TypeInfo;

import javax.xml.validation.TypeInfoProvider;

/**
 * This class is an extension of ContentHandlerProxy that provides access to type
 * information, using the DOM Level 3 TypeInfo interfaces.
 * The ContentHandlerProxy also acts as a TypeInfoProvider, providing information
 * about the type of the current element or attribute.
 */

public class TypedContentHandler extends ContentHandlerProxy {
    private int pendingElementTypeCode;

    /**
     * Get a TypeInfoProvider to provide type information for the current element or attribute
     * event.
     */

    public TypeInfoProvider getTypeInfoProvider() {
        return new TypeInfoProviderImpl();
    }


    /**
     * Notify the start of an element
     */

    public void startElement(int nameCode, int typeCode, int locationId, int properties) throws XPathException {
        pendingElementTypeCode = typeCode;
        super.startElement(nameCode, typeCode, locationId, properties);
    }


    ///////////////////////////////////////////////////////////////////////////////////////////
    // TypeInfoProvider
    ///////////////////////////////////////////////////////////////////////////////////////////

    public class TypeInfoProviderImpl extends TypeInfoProvider {

        /**
         * Returns the immutable {@link org.w3c.dom.TypeInfo} object for the current element.
         *
         * @return An immutable {@link org.w3c.dom.TypeInfo} object that represents the
         *         type of the current element.
         *         Note that the caller can keep references to the obtained
         *         {@link org.w3c.dom.TypeInfo} longer than the callback scope.
         *         <p/>
         *         Otherwise, this method returns null if the validator is unable to
         *         determine the type of the current element for some reason
         */

        public TypeInfo getElementTypeInfo() {
            if (pendingElementTypeCode == -1) {
                return new TypeInfoImpl(getConfiguration(), AnyType.getInstance());
            } else {
                return new TypeInfoImpl(getConfiguration(),
                        getConfiguration().getSchemaType(pendingElementTypeCode));
            }
        }

        /**
         * Returns the immutable {@link org.w3c.dom.TypeInfo} object for the specified
         * attribute of the current element.
         * <p/>
         * The method may only be called by the startElement event of
         * the {@link org.xml.sax.ContentHandler} that the application sets to the
         * {@link javax.xml.validation.ValidatorHandler}.
         *
         * @param index The index of the attribute. The same index for
         *              the {@link org.xml.sax.Attributes} object passed to the
         *              <tt>startElement</tt> callback.
         * @return An immutable {@link org.w3c.dom.TypeInfo} object that represents the
         *         type of the specified attribute.
         *         Note that the caller can keep references to the obtained
         *         {@link org.w3c.dom.TypeInfo} longer than the callback scope.
         *         <p/>
         *         Otherwise, this method returns null if the validator is unable to
         *         determine the type.
         * @throws IndexOutOfBoundsException If the index is invalid.
         * @throws IllegalStateException     If this method is called from other {@link org.xml.sax.ContentHandler}
         *                                   methods.
         */
        public TypeInfo getAttributeTypeInfo(int index) {
            if (index < 0 || index > pendingAttributes.getLength()) {
                throw new IndexOutOfBoundsException(""+index);
            }
            int type = pendingAttributes.getTypeAnnotation(index);
            if (type == -1) {
                return new TypeInfoImpl(getConfiguration(), AnySimpleType.getInstance());
            } else {
                return new TypeInfoImpl(getConfiguration(),
                        getConfiguration().getSchemaType(type));
            }
        }

        /**
         * Returns <tt>true</tt> if the specified attribute is determined
         * to be an ID.
         * @param index The index of the attribute. The same index for
         *              the {@link org.xml.sax.Attributes} object passed to the
         *              <tt>startElement</tt> callback.
         * @return true
         *         if the type of the specified attribute is ID.
         */
        public boolean isIdAttribute(int index) {
            int type = pendingAttributes.getTypeAnnotation(index);
            return (type == StandardNames.XS_ID ||
                    getAttributeTypeInfo(index).isDerivedFrom(
                            NamespaceConstant.SCHEMA, "ID", SchemaType.DERIVATION_RESTRICTION));
        }

        /**
         * Returns <tt>false</tt> if the attribute was added by the validator.
         *
         * @param index The index of the attribute. The same index for
         *              the {@link org.xml.sax.Attributes} object passed to the
         *              <tt>startElement</tt> callback.
         * @return <tt>true</tt> if the attribute was present before the validator
         *         processes input. <tt>false</tt> if the attribute was added
         *         by the validator.
         */

        public boolean isSpecified(int index) {
            return (pendingAttributes.getProperties(index) & ReceiverOptions.DEFAULTED_ATTRIBUTE) == 0;
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
