package network;

/**
 * One parameter in the SafariBowl protocol.
 */
public class SBProtocolParameter {

    private String content;

    /**
     * Create parameter from string.
     * @param parameter The string to create the parameter from.
     */
    public SBProtocolParameter(String parameter) {
        this.content = parameter;
    }

    /**
     * Empty constructor.
     */
    public SBProtocolParameter() {
        this.content = "";
    }

    /**
     * Escape and return the content of this parameter.
     * @return The escaped content of this parameter.
     */
    public String escape() {
        // replace the set [&, ", space, [, ], \t, \n] with the set [&amp;, &quot;, &space;, &brackl;, &brackr;, &tab;, &newl;] and return
        return content.replace("&", "&amp;").replace("\"", "&quot;").replace(" ", "&space;").replace("[", "&brackl;").replace("]", "&brackr;").replace("\t", "&tab;").replace("\n", "&newl;");
    }

    /**
     * Unescape and return the content of this parameter.
     * @return The unescaped content of this parameter.
     */
    public String unescape() {
        // replace the set [&amp;, &quot;, &space;, &brackl;, &brackr;, &tab;, &newl;] with the set [&, ", space, [, ], \t, \n] and return
        return content.replace("&newl;", "\n").replace("&tab;", "\t").replace("&brackr;", "]").replace("&brackl;", "[").replace("&space;", " ").replace("&quot;", "\"").replace("&amp;", "&");
    }

    /**
     * Escape and return the given content.
     * @param content The content to escape.
     * @return The escaped content.
     */
    public static String escape(String content) {
        // replace the set [&, ", space, [, ], \t, \n] with the set [&amp;, &quot;, &space;, &brackl;, &brackr;, &tab;, &newl;] and return
        return content.replace("&", "&amp;").replace("\"", "&quot;").replace(" ", "&space;").replace("[", "&brackl;").replace("]", "&brackr;").replace("\t", "&tab;").replace("\n", "&newl;");
    }

    /**
     * Unescape and return the given content.
     * @param content The content to unescape.
     * @return The unescaped content.
     */
    public static String unescape(String content) {
        // replace the set [&amp;, &quot;, &space;, &brackl;, &brackr;, &tab;, &newl;] with the set [&, ", space, [, ], \t, \n] and return
        return content.replace("&newl;", "\n").replace("&tab;", "\t").replace("&brackr;", "]").replace("&brackl;", "[").replace("&space;", " ").replace("&quot;", "\"").replace("&amp;", "&");
    }

    /**
     * Get the string representation of this parameter.
     * @return The string representation of this parameter.
     */
    public String toString() {
        return "\""+escape()+"\"";
    }

    /**
     * Find out if a SBProtocolParameter is a SBProtocolParameterArray
     * @return False because this is not an array.
     */
    public boolean isArray() {
        return false;
    }

    /**
     * Create a new SBProtocolParameterArray with the content of this parameter.
     * @return The created parameter array.
     */
    public SBProtocolParameterArray toArray() {
        return new SBProtocolParameterArray(this);
    }

    /**
     * Get the content of this message.
     * @return The content of this message.
     */
    public String getContent() {
        return content;
    }

}

