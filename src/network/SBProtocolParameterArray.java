package network;

import util.SBLogger;

import java.util.Arrays;
import java.util.Vector;
import java.util.logging.Level;

/**
 * Representation of a parameter array in a sb protocol message
 * Created by milan on 27.3.15.
 */
public class SBProtocolParameterArray extends SBProtocolParameter {

    private static final SBLogger L = new SBLogger(SBProtocolParameterArray.class.getName(), util.SBLogger.LOG_LEVEL);

    private Vector<SBProtocolParameter> parameters = new Vector<SBProtocolParameter>();

    /**
     * Create an empty parameter array.
     */
    public SBProtocolParameterArray() {
        super();
        this.parameters = new Vector<SBProtocolParameter>();
    }

    /**
     * Create a new parameter array with one or more parameters.
     * @param parameters The parameters to put into the parameter array.
     */
    public SBProtocolParameterArray(SBProtocolParameter... parameters) {
        super();
        this.parameters.addAll(Arrays.asList(parameters));
    }
    /**
     * Create a new parameter array with one or more parameters from strings.
     * @param parameters The strings to put into the parameter array.
     */
    public SBProtocolParameterArray(String... parameters) {
        super();
        for(String parameter: parameters) this.parameters.add(new SBProtocolParameter(parameter));
    }

    /**
     * Get the parameter at index i.
     * @param i The index of the parameter to return.
     * @return The parameter at index i.
     */
    public SBProtocolParameter getParameter(int i) {
        return parameters.get(i);
    }

    /**
     * Get the size of the parameter array.
     * @return The size of the parameter array.
     */
    public int size() {
        return parameters.size();
    }

    /**
     * Append parameter to parameter array.
     * @param parameter The parameter to add.
     */
    public void addParameter(SBProtocolParameter parameter) {
        if(parameter != null) parameters.add(parameter);
    }

    /**
     * Insert parameter in parameter array at specified index.
     * @param index The index to insert the parameter at.
     * @param parameter The parameter to add.
     */
    public void addParameter(int index, SBProtocolParameter parameter) {
        parameters.add(index, parameter);
    }

    /**
     * Overwrite the escape method from SBProtocolParameter
     * @return The escaped parameter array.
     */
    public String escape() {
        return toString();
    }

    /**
     * Find out if a SBProtocolParameter is a SBProtocolParameterArray
     * @return True because this is an array.
     */
    public boolean isArray() {
        return true;
    }

    public SBProtocolParameterArray toArray() {
        return this;
    }

    /**
     * Get the string representation of this parameter array (with escaped parameters).
     * @return The string representation of this parameter array. E.g. "["param1", "param2", ["param3", "param4"]]"
     */
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        for(SBProtocolParameter parameter: parameters) {
            sb.append(parameter).append(", ");
        }
        if(sb.length() > 1) { // there was at least one parameter added
            sb.replace(sb.length() - 2, sb.length(), "]");
        } else { // array is empty, only add closing bracket
            sb.append("]");
        }
        return sb.toString();
    }

    /**
     * Get the string representation of this parameter array.
     * @return The string representation of this parameter array. E.g. "["param1", "param2", ["param3", "param4"]]"
     */
    public String toStringUnescaped() {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        for(SBProtocolParameter parameter: parameters) {
            sb.append(unescape(parameter.toString())).append(", ");
        }
        if(sb.length() > 1) { // there was at least one parameter added
            sb.replace(sb.length() - 2, sb.length(), "]");
        } else { // array is empty, only add closing bracket
            sb.append("]");
        }
        return sb.toString();
    }

    /**
     * Create a new SBProtocolParameterArray from an ESCAPED string. (see SBProtocolParameter.escape())
     * NOTE: This method will fail if parameter strings contain unescaped spaces, ", [, or ].
     * @param p The ESCAPED string to create a parameter array from.
     * @return The created parameter array. Null if there was an error creating the parameter array.
     */
    public static SBProtocolParameterArray paraception(String p) {
        // prepare return array
        SBProtocolParameterArray returnArray = new SBProtocolParameterArray();
        // remove whitespace in this string
        p = p.replaceAll("\\s+","");
        if(p.startsWith("[") && p.endsWith("]") || p.startsWith("\"") && p.endsWith("\"")) { // p starts and ends correctly
            // strip [ and ] from string if there
            if(p.startsWith("[") && p.endsWith("]")) p = p.substring(1, p.length() - 1);
            // return empty array if string is empty after stripping
            if(p.equals("")) return new SBProtocolParameterArray();
            // prepare counter
            int c = 0;
            // loop the whole parameter
            while(c < p.length()-1) {
                // prepare subparameter string builder
                StringBuilder subparameter = new StringBuilder();
                if (p.charAt(c) == '"') { // subparameter is simple parameter
                    // skip first quote
                    c++;
                    // get simple subparameter
                    while (p.charAt(c) != '"') { // loop until a closing quote appears
                        if(c >= p.length()-1) {
                            L.log(Level.WARNING, "Tried to parse malformed parameter into parameter array. Definetly not doing that!");
                            return null;
                        }
                        // add character to subparameter stringbuilder and count c up
                        subparameter.append(p.charAt(c));
                        c++;
                    }
                    // skip closing quote
                    c++;
                    // add collected parameter string as parameter
                    returnArray.addParameter(new SBProtocolParameter(SBProtocolParameter.unescape(subparameter.toString())));
                } else if(p.charAt(c) == '[') { // subparameter is array
                    // skip first bracket
                    c++;
                    // get subparameter array
                    int level = 0; // how deep the loop is in the array (to find the correct closing bracket)
                    while (p.charAt(c) != ']' && level == 0) { // loop until a closing quote at level 0 appears
                        if(c >= p.length()-1) {
                            L.log(Level.WARNING, "Tried to parse malformed parameter into parameter array. Definetly not doing that!");
                            return null;
                        }
                        // increase level if an opening bracket appears and decrease level if a closing bracket appears
                        if(p.charAt(c) == '[') level++;
                        if(p.charAt(c) == ']') level--;
                        // add character to subparameter stringbuilder and count c up
                        subparameter.append(p.charAt(c));
                        c++;
                    }
                    // skip last closing bracket
                    c++;
                    // recursively get the parameter array of this subparameter array string
                    SBProtocolParameterArray subArray = paraception(subparameter.toString());
                    // add collected parameter string as parameter
                    returnArray.addParameter(subArray);
                } else { // character at beginning of next subparameter is not [ nor "
                    L.log(Level.WARNING, "Tried to parse malformed parameter into parameter array. Definetly not doing that!");
                    return null;
                }
                // check after last parameter
                if(c >= p.length()-1) { // reached end of array
                    break;
                } else if(p.charAt(c) != ',') { // the next character after a parameter must be the end of the array or a comma
                    L.log(Level.WARNING, "Tried to parse malformed parameter into parameter array. Definetly not doing that!");
                    return null;
                } else c++; // skip comma
            }
        } else {
            if(p.equals("")) return new SBProtocolParameterArray(); // p was empty, which is allowed
            else {
                L.log(Level.WARNING, "Tried to parse malformed parameter into parameter array. Definetly not doing that!");
                return null;
            }
        }
        return returnArray;
    }

}
