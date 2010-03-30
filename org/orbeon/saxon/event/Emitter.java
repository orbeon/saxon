package org.orbeon.saxon.event;
import org.orbeon.saxon.Configuration;
import org.orbeon.saxon.charcode.CharacterSet;
import org.orbeon.saxon.charcode.CharacterSetFactory;
import org.orbeon.saxon.charcode.PluggableCharacterSet;
import org.orbeon.saxon.charcode.UnicodeCharacterSet;
import org.orbeon.saxon.om.NamePool;
import org.orbeon.saxon.trans.XPathException;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.TransformerException;
import javax.xml.transform.stream.StreamResult;
import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Properties;


/**
  * Emitter: This abstract class defines methods that must be implemented by
  * components that format SAXON output. There is one emitter for XML,
  * one for HTML, and so on. Additional methods are concerned with
  * setting options and providing a Writer.<p>
  *
  * The interface is deliberately designed to be as close as possible to the
  * standard SAX2 ContentHandler interface, however, it allows additional
  * information to be made available.
  *
  * An Emitter is a Receiver, specifically it is a Receiver that can direct output
  * to a Writer or OutputStream, using serialization properties defined in a Properties
  * object.
  */

public abstract class Emitter implements Result, Receiver
{
    protected PipelineConfiguration pipelineConfig;
    protected NamePool namePool;
    protected String systemId;
    protected StreamResult streamResult;
    protected Writer writer;
    protected OutputStream outputStream;
    protected Properties outputProperties;
    protected CharacterSet characterSet = null;
    protected boolean allCharactersEncodable = false;

	/**
	* Set the pipelineConfiguration
	*/

	public void setPipelineConfiguration(PipelineConfiguration pipe) {
        pipelineConfig = pipe;
        namePool = pipe.getConfiguration().getNamePool();
	}

	/**
	* Get the pipeline configuration used for this document
	*/

	public PipelineConfiguration getPipelineConfiguration() {
		return pipelineConfig;
	}

	/**
	 * Get the configuration used for this document
     * @return the configuration
	*/

	public Configuration getConfiguration() {
		return pipelineConfig.getConfiguration();
	}

	/**
	 * Set the System ID
     * @param systemId the system identifier (=base URI)
	*/

	public void setSystemId(String systemId) {
	    this.systemId = systemId;
	}

	/**
	* Get the System ID
	*/

	public String getSystemId() {
	    return systemId;
	}

    /**
     * Set output properties
     * @param details the output serialization properties
    */

    public void setOutputProperties(Properties details) throws XPathException {
        if (characterSet==null) {
            characterSet = CharacterSetFactory.getCharacterSet(details, getPipelineConfiguration());
            allCharactersEncodable = (characterSet instanceof UnicodeCharacterSet);
        }
        outputProperties = details;
    }

    /**
     * Get the output properties
     * @return the output serialization properties
     */

    public Properties getOutputProperties() {
        return outputProperties;
    }

    /**
     * Set the StreamResult acting as the output destination of the Emitter
     * @param result the output destination
     */

    public void setStreamResult(StreamResult result) throws XPathException {
        streamResult = result;
        if (systemId == null) {
            systemId = result.getSystemId();
        }
    }

    /**
     * Make a Writer for this Emitter to use, given a StreamResult.
     */

    protected void makeWriter() throws XPathException {
        if (writer != null) {
            return;
        }
        if (streamResult == null) {
            throw new IllegalStateException("Emitter must have either a Writer or a StreamResult to write to");
        }
        writer = streamResult.getWriter();
        if (writer == null) {
            OutputStream os = streamResult.getOutputStream();
            if (os != null) {
                setOutputStream(os);
            }
        }
        if (writer == null) {
            String uriString = streamResult.getSystemId();
            if (uriString == null) {
                throw new XPathException("No system ID supplied for result file");
            }

            try {
                URI uri = new URI(uriString);
                if (!uri.isAbsolute()) {
                    try {
                        uri = new File(uriString).getAbsoluteFile().toURI();
                    } catch (Exception e) {
                        // if we fail, we'll get another exception
                    }
                }
                File file = new File(uri);
                setOutputStream(new FileOutputStream(file));
                // Set the outputstream in the StreamResult object so that the
                // call on OutputURIResolver.close() can close it
                streamResult.setOutputStream(outputStream);
            } catch (FileNotFoundException fnf) {
                throw new XPathException(fnf);
            } catch (URISyntaxException use) {
                throw new XPathException(use);
            } catch (IllegalArgumentException iae) {
                // for example, the system ID doesn't use the file: scheme
                throw new XPathException(iae);
            }
        }
    }
    /**
     * Determine whether the Emitter wants a Writer for character output or
     * an OutputStream for binary output. The standard Emitters all use a Writer, so
     * this returns true; but a subclass can override this if it wants to use an OutputStream
     * @return true if a Writer is needed, as distinct from an OutputStream
    */

    public boolean usesWriter() {
        return true;
    }

    /**
     * Set the output destination as a character stream
     * @param writer the Writer to use as an output destination
    */

    public void setWriter(Writer writer) throws XPathException {
        this.writer = writer;

        // If the writer uses a known encoding, change the encoding in the XML declaration
        // to match. Any encoding actually specified in xsl:output is ignored, because encoding
        // is being done by the user-supplied Writer, and not by Saxon itself.

        if (writer instanceof OutputStreamWriter && outputProperties != null) {
            String enc = ((OutputStreamWriter)writer).getEncoding();
            //System.err.println("Java encoding: " + enc);
            outputProperties.setProperty(OutputKeys.ENCODING, enc);
            characterSet = CharacterSetFactory.getCharacterSet(outputProperties, getPipelineConfiguration());
            allCharactersEncodable = (characterSet instanceof UnicodeCharacterSet);
        }
    }

    /**
     * Get the output writer
     * @return the Writer being used as an output destination, if any
    */

    public Writer getWriter() {
        return writer;
    }

    /**
     * Set the output destination as a byte stream.
     * <p>Note that if a specific encoding (other than the default, UTF-8) is required, then
     * {@link #setOutputProperties(java.util.Properties)} must be called <i>before</i> calling
     * this method.</p>
     * @param stream the OutputStream being used as an output destination
    */

    public void setOutputStream(OutputStream stream) throws XPathException {
        outputStream = stream;

        // If the user supplied an OutputStream, but the Emitter is written to
        // use a Writer (this is the most common case), then we create a Writer
        // to wrap the supplied OutputStream; the complications are to ensure that
        // the character encoding is correct.

        if (usesWriter()) {

            if (outputProperties == null) {
                outputProperties = new Properties();
            }

            String encoding = outputProperties.getProperty(OutputKeys.ENCODING);
            if (encoding==null) {
                encoding = "UTF8";
                allCharactersEncodable = true;
            } else if (encoding.equalsIgnoreCase("UTF-8")) {
                encoding = "UTF8";
                allCharactersEncodable = true;
            } else if (encoding.equalsIgnoreCase("UTF-16")) {
                encoding = "UTF16";
            }

            String byteOrderMark = outputProperties.getProperty(SaxonOutputKeys.BYTE_ORDER_MARK);
            if ("no".equals(byteOrderMark) && "UTF16".equals(encoding)) {
                // Java always writes a bom for UTF-16, so if the user doesn't want one, use utf16-be
                encoding = "UTF-16BE";
            }

            if (characterSet instanceof PluggableCharacterSet) {
	        	encoding = ((PluggableCharacterSet)characterSet).getEncodingName();
	        }

            while (true) {
                try {
                    String javaEncoding = encoding;
                    if (encoding.equalsIgnoreCase("iso-646") || encoding.equalsIgnoreCase("iso646")) {
                        javaEncoding = "US-ASCII";
                    }
                    writer = new BufferedWriter(
                                    new OutputStreamWriter(
                                        outputStream, javaEncoding));
                    break;
                } catch (Exception err) {
                    if (encoding.equalsIgnoreCase("UTF8")) {
                        throw new XPathException("Failed to create a UTF8 output writer");
                    }
                    XPathException de = new XPathException("Encoding " + encoding + " is not supported: using UTF8");
                    de.setErrorCode("SESU0007");
                    try {
                        getPipelineConfiguration().getErrorListener().error(de);
                    } catch (TransformerException e) {
                        throw XPathException.makeXPathException(e);
                    }
                    encoding = "UTF8";
                    characterSet = UnicodeCharacterSet.getInstance();
                    allCharactersEncodable = true;
                    outputProperties.setProperty(OutputKeys.ENCODING, "UTF-8");
                }
            }
        }

    }

    /**
     * Get the output stream
     * @return the OutputStream being used as an output destination, if any
    */

    public OutputStream getOutputStream() {
        return outputStream;
    }

    /**
     * Set unparsed entity URI. Needed to satisfy the Receiver interface, but not used,
     * because unparsed entities can occur only in input documents, not in output documents.
     * @param name the entity name
     * @param uri the entity system ID
     * @param publicId the entity public ID
    */

    public void setUnparsedEntity(String name, String uri, String publicId) throws XPathException {}


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
