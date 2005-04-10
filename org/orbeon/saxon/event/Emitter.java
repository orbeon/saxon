package net.sf.saxon.event;
import net.sf.saxon.Configuration;
import net.sf.saxon.Controller;
import net.sf.saxon.charcode.CharacterSet;
import net.sf.saxon.charcode.CharacterSetFactory;
import net.sf.saxon.charcode.PluggableCharacterSet;
import net.sf.saxon.charcode.UnicodeCharacterSet;
import net.sf.saxon.om.NamePool;
import net.sf.saxon.trans.DynamicError;
import net.sf.saxon.trans.XPathException;
import org.xml.sax.ContentHandler;

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

	/**
	* Set the namePool in which all name codes can be found
	*/

	public void setPipelineConfiguration(PipelineConfiguration config) {
	    this.pipelineConfig = config;
        this.namePool = config.getConfiguration().getNamePool();
	}

	/**
	* Get the pipeline configuration used for this document
	*/

	public PipelineConfiguration getPipelineConfiguration() {
		return pipelineConfig;
	}

	/**
	* Get the configuration used for this document
	*/

	public Configuration getConfiguration() {
		return pipelineConfig.getConfiguration();
	}

	/**
	* Set the System ID
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
    */

    public void setOutputProperties(Properties details) throws XPathException {
        if (characterSet==null) {
            characterSet = CharacterSetFactory.getCharacterSet(details, getPipelineConfiguration().getController());
        }
        outputProperties = details;
    }

    /**
    * Get the output properties
    */

    public Properties getOutputProperties() {
        return outputProperties;
    }

    /**
     * Set the StreamResult acting as the output destination of the Emitter
     */

    public void setStreamResult(StreamResult result) {
        this.streamResult = result;
    }

    /**
     * Make a Writer for this Emitter to use, given a StreamResult
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
            String uri = streamResult.getSystemId();
            try {
                File file = new File(new URI(uri));
                setOutputStream(new FileOutputStream(file));
                // Set the outputstream in the StreamResult object so that the
                // call on OutputURIResolver.close() can close it
                streamResult.setOutputStream(outputStream);
            } catch (FileNotFoundException fnf) {
                throw new DynamicError(fnf);
            } catch (URISyntaxException use) {
                throw new DynamicError(use);
            }
        }
    }
    /**
    * Determine whether the Emitter wants a Writer for character output or
    * an OutputStream for binary output. The standard Emitters all use a Writer, so
    * this returns true; but a subclass can override this if it wants to use an OutputStream
    */

    public boolean usesWriter() {
        return true;
    }

    /**
    * Set the output destination as a character stream
    */

    public void setWriter(Writer writer) {
        this.writer = writer;

        // If the writer uses a known encoding, change the encoding in the XML declaration
        // to match. Any encoding actually specified in xsl:output is ignored, because encoding
        // is being done by the user-supplied Writer, and not by Saxon itself.

        if (writer instanceof OutputStreamWriter && outputProperties != null) {
            String enc = ((OutputStreamWriter)writer).getEncoding();
            outputProperties.put(OutputKeys.ENCODING, enc);
        }
    }

    /**
    * Get the output writer
    */

    public Writer getWriter() {
        return writer;
    }

    /**
    * Set the output destination as a byte stream
    */

    public void setOutputStream(OutputStream stream) throws XPathException {
        this.outputStream = stream;

        // If the user supplied an OutputStream, but the Emitter is written to
        // use a Writer (this is the most common case), then we create a Writer
        // to wrap the supplied OutputStream; the complications are to ensure that
        // the character encoding is correct.

        if (usesWriter()) {

            //CharacterSet charSet = CharacterSetFactory.getCharacterSet(outputProperties);

            String encoding = outputProperties.getProperty(OutputKeys.ENCODING);
            if (encoding==null) encoding = "UTF8";
            if (encoding.equalsIgnoreCase("UTF-8")) encoding = "UTF8";
                 // needed for Microsoft Java VM

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
                        throw new DynamicError("Failed to create a UTF8 output writer");
                    }
                    DynamicError de = new DynamicError("Encoding " + encoding + " is not supported: using UTF8");
                    de.setErrorCode("SE0007");
                    try {
                        getPipelineConfiguration().getErrorListener().error(de);
                    } catch (TransformerException e) {
                        throw DynamicError.makeDynamicError(e);
                    }
                    encoding = "UTF8";
                    characterSet = UnicodeCharacterSet.getInstance();
                    outputProperties.put(OutputKeys.ENCODING, "UTF-8");
                }
            }
        }

    }

    /**
    * Get the output stream
    */

    public OutputStream getOutputStream() {
        return outputStream;
    }

    /**
    * Set unparsed entity URI. Needed to satisfy the Receiver interface, but not used,
    * because unparsed entities can occur only in input documents, not in output documents.
    */

    public void setUnparsedEntity(String name, String uri, String publicId) throws XPathException {}

    /**
    * Load a named output emitter or SAX2 ContentHandler and check it is OK.
    */

    public static Receiver makeEmitter (String className, Controller controller) throws XPathException
    {
        Object handler;
        try {
            handler = controller.getConfiguration().getInstance(className, controller.getClassLoader());
        } catch (XPathException e) {
            throw new DynamicError("Cannot load user-supplied output method " + className);
        }

        if (handler instanceof Receiver) {
            return (Receiver)handler;
        } else if (handler instanceof ContentHandler) {
            ContentHandlerProxy emitter = new ContentHandlerProxy();
            emitter.setUnderlyingContentHandler((ContentHandler)handler);
            return emitter;
        } else {
            throw new DynamicError("Failed to load " + className +
                        ": it is neither a Receiver nor a SAX2 ContentHandler");
        }

    }

    /**
    * Close output stream or writer. Called by subclasses at endDocument time
    */
/*
    public void close() {
        if (closeAfterUse) {
            try {
                if (usesWriter) {
                    writer.close();
                } else {
                    outputStream.close();
                }
            } catch (Exception err) {}
        }
    }
*/
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
