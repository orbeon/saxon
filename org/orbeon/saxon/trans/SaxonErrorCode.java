package net.sf.saxon.trans;

/**
 * The class acts as a register of Saxon-specific error codes.
 * <p>
 * Technically, these codes should be in their own namespace. At present, however, they share the
 * same namespace as system-defined error codes.
 */
public class SaxonErrorCode {

    /**
     * SXLM0001: stylesheet or query appears to be looping/recursing indefinitely
     */

    public static final String SXLM0001 = "SXLM0001";

    /**
     * SXCH0002: cannot supply output to ContentHandler because it is not well-formed
     */

    public static final String SXCH0002 = "SXCH0002";

    /**
     * SXXP0003: error reported by XML parser while parsing source document
     */

    public static final String SXXP0003 = "SXXP0003";

    /**
     * SXIN0004: input document is not a stylesheet
     */

    public static final String SXIN0004 = "SXIN0004";
        // There should really be a standard XSLT error code for this, but there isn't...

    /**
     * SXWN9001: a variable declaration with no following siblings has no effect
     */

    public static final String SXWN9001 = "SXWN9001";

    /**
     * SXWN9002: saxon:indent-spaces must be a positive integer
     */

    public static final String SXWN9002 = "SXWN9002";    

    /**
     * SXWN9003: saxon:require-well-formed must be a "yes" or "no"
     */

    public static final String SXWN9003 = "SXWN9003";

    /**
     * SXWN9004: saxon:next-in-chain cannot be specified dynamically
     */

    public static final String SXWN9004 = "SXWN9004";
}
