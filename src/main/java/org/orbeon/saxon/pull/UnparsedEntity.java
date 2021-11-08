package org.orbeon.saxon.pull;

/**
 * This class is used to represent unparsed entities in the PullProvider interface
 */

public class UnparsedEntity {

    private String name;
    private String systemId;
    private String publicId;
    private String baseURI;

    /**
     * Get the name of the unparsed entity
     * @return the name of the unparsed entity
     */

    public String getName() {
        return name;
    }

   /**
     * Set the name of the unparsed entity
     * @param name the name of the unparsed entity
     */

    public void setName(String name) {
        this.name = name;
    }

    /**
     * Get the system identifier of the unparsed entity
     * @return the system identifier of the unparsed entity
     */

    public String getSystemId() {
        return systemId;
    }

    /**
      * Set the system identifier of the unparsed entity
      * @param systemId the system identifier of the unparsed entity
      */

    public void setSystemId(String systemId) {
        this.systemId = systemId;
    }

    /**
     * Get the public identifier of the unparsed entity
     * @return the public identifier of the unparsed entity
     */

    public String getPublicId() {
        return publicId;
    }

    /**
      * Set the public identifier of the unparsed entity
      * @param publicId the public identifier of the unparsed entity
      */

    public void setPublicId(String publicId) {
        this.publicId = publicId;
    }

    /**
     * Get the base URI of the unparsed entity
     * @return the base URI  of the unparsed entity
     */

    public String getBaseURI() {
        return baseURI;
    }

    /**
      * Set the base URI of the unparsed entity
      * @param baseURI the base URI  of the unparsed entity
      */

    public void setBaseURI(String baseURI) {
        this.baseURI = baseURI;
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

