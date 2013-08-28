package org.bsonspec.me;

/***
Copyright (c) 2010 Ufuk Kayserilioglu (ufuk@paralaus.com)

Licensed under the Apache License, Version 2.0 (the "License"); you may
not use this file except in compliance with the License. You may obtain
a copy of the License at
	http://www.apache.org/licenses/LICENSE-2.0
Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

import java.io.IOException;
import java.io.Writer;
import java.util.Enumeration;
import java.util.Hashtable;


/**
 * A BSONDocument is an unordered collection of name/value pairs. The internal form
 * is an object having <code>get</code> and <code>opt</code> methods for
 * accessing the values by name, and <code>put</code> methods for adding or
 * replacing values by name. The values can be any of these types:
 * <code>Boolean</code>, <code>BSONArray</code>, <code>BSONDocument</code>,
 * <code>Number</code>, <code>String</code>, or the <code>BSONDocument.NULL</code>
 * object. A BSONDocument constructor can be used to convert an external form
 * BSON data into an internal form whose values can be retrieved with the
 * <code>get</code> and <code>opt</code> methods, or to convert values into a
 * JSON text using the <code>put</code> and <code>toString</code> methods.
 * A <code>get</code> method returns a value if one can be found, and throws an
 * exception if one cannot be found. An <code>opt</code> method returns a
 * default value instead of throwing an exception, and so is useful for
 * obtaining optional values.
 * <p>
 * The generic <code>get()</code> and <code>opt()</code> methods return an
 * object, which you can cast or query for type. There are also typed
 * <code>get</code> and <code>opt</code> methods that do type checking and type
 * coersion for you.
 * <p>
 * The <code>put</code> methods adds values to an object. For example, <pre>
 *     myString = new BSONDocument().put("BSON", "Hello, World!").toString();</pre>
 * produces the string <code>{"BSON": "Hello, World"}</code>.
 * <p>
 * The texts produced by the <code>toString</code> methods strictly conform to
 * the JSON syntax rules.
 * @author Ufuk Kayserilioglu
 * @version 1
 */
public class BSONDocument {

    /**
     * The hash map where the BSONDocument's properties are kept.
     */
    private Hashtable myHashMap;

    /**
     * Construct an empty BSONDocument.
     */
    public BSONDocument() {
        this.myHashMap = new Hashtable();
    }

    /**
     * Construct a BSONDocument from a JSONTokener.
     * @param x A JSONTokener object containing the source string.
     * @throws BSONException If there is a syntax error in the source string.
     */
    public BSONDocument(BSONTokener x) throws BSONException {
        this();
        
        int length = x.nextInt();
        if (x.size() != length) {
            throw x.syntaxError("The length of the BSONDocument does not match with the length of given data.");
        }
        
        for (;;) {
        	BSONElement e = x.nextElement();
        	
        	if (e == null) {
        		return;
        	}
        	
            put(e.getKey(), e);
        }
    }  
    
    /**
     * Construct a BSONDocument from a string.
     * This is the most commonly used BSONDocument constructor.
//     * @param string    A string beginning
//     *  with <code>{</code>&nbsp;<small>(left brace)</small> and ending
//     *  with <code>}</code>&nbsp;<small>(right brace)</small>.
     * @exception BSONException If there is a syntax error in the source string.
     */
    public BSONDocument(byte[] data) throws BSONException {
    	this(data, 0, data.length);
    }

    public BSONDocument(byte[] data, int offset, int length) throws BSONException {
        this(new BSONTokener(data, offset, length));    	
    }


    /**
     * Produce a string from a double. The string "null" will be returned if
     * the number is not finite.
     * @param  d A double.
     * @return A String.
     */
    static public String doubleToString(double d) {
        if (Double.isInfinite(d) || Double.isNaN(d)) {
        	return "null";
        }

        // Shave off trailing zeros and decimal point, if possible.

        String s = Double.toString(d);
        if (s.indexOf('.') > 0 && s.indexOf('e') < 0 && s.indexOf('E') < 0) {
            while (s.endsWith("0")) {
                s = s.substring(0, s.length() - 1);
            }
            if (s.endsWith(".")) {
                s = s.substring(0, s.length() - 1);
            }
        }
        return s;
    }

    /**
     * Get the value object associated with a key.
     * If the key doesn't exist, then return null.
     * If the value object is NULL, return null.
     * @param key   A key string.
     * @return      The object associated with the key.
     */
    public Object get(String key) {
    	return opt(key).getValue();
    }
    
    
    /**
     * Get the value object associated with a key.
     *
     * @param key   A key string.
     * @return      The object associated with the key.
     * @throws   BSONException if the key is not found.
     */
    protected BSONElement getElement(String key) throws BSONException {
    	BSONElement o = opt(key);
        if (o.isNull()) {
            throw new BSONException("BSONDocument[" + quote(key) +
                    "] not found.");
        }
        return o;
    }


    /**
     * Get the boolean value associated with a key.
     *
     * @param key   A key string.
     * @return      The truth.
     * @throws   BSONException
     *  if the value is not a Boolean or the String "true" or "false".
     */
    public boolean getBoolean(String key) throws BSONException {
        BSONElement o = getElement(key);
        return o.getBoolean();
    }

    /**
     * Get the double value associated with a key.
     * @param key   A key string.
     * @return      The numeric value.
     * @throws BSONException if the key is not found or
     *  if the value is not a Number object and cannot be converted to a number.
     */
    public double getDouble(String key) throws BSONException {
        BSONElement o = getElement(key);
        return o.getDouble();
    }


    /**
     * Get the int value associated with a key. If the number value is too
     * large for an int, it will be clipped.
     *
     * @param key   A key string.
     * @return      The integer value.
     * @throws   BSONException if the key is not found or if the value cannot
     *  be converted to an integer.
     */
    public int getInt(String key) throws BSONException {
        BSONElement o = getElement(key);
        return o.getInt();
    }


    /**
     * Get the BSONArray value associated with a key.
     *
     * @param key   A key string.
     * @return      A BSONArray which is the value.
     * @throws   BSONException if the key is not found or
     *  if the value is not a BSONArray.
     */
    public BSONArray getBSONArray(String key) throws BSONException {
    	BSONElement o = getElement(key);
    	return o.getBSONArray();
    }

    /**
     * Get the BSONDocument value associated with a key.
     *
     * @param key   A key string.
     * @return      A BSONDocument which is the value.
     * @throws   BSONException if the key is not found or
     *  if the value is not a BSONDocument.
     */
    public BSONDocument getBSONDocument(String key) throws BSONException {
        BSONElement o = getElement(key);
        return o.getBSONDocument();
    }

    /**
     * Get the long value associated with a key. If the number value is too
     * long for a long, it will be clipped.
     *
     * @param key   A key string.
     * @return      The long value.
     * @throws   BSONException if the key is not found or if the value cannot
     *  be converted to a long.
     */
    public long getLong(String key) throws BSONException {
    	BSONElement o = getElement(key);
    	return o.getLong();
    }

    /**
     * Get the string associated with a key.
     *
     * @param key   A key string.
     * @return      A string which is the value.
     * @throws   BSONException if the key is not found.
     */
    public String getString(String key) throws BSONException {
        return getElement(key).toString();
    }

    /**
     * Determine if the BSONDocument contains a specific key.
     * @param key   A key string.
     * @return      true if the key exists in the BSONDocument.
     */
    public boolean has(String key) {
        return this.myHashMap.containsKey(key);
    }


    /**
     * Determine if the value associated with the key is null or if there is
     *  no value.
     * @param key   A key string.
     * @return      true if there is no value associated with the key or if
     *  the value is the BSONDocument.NULL object.
     */
    public boolean isNull(String key) {
        return opt(key).isNull();
    }


    /**
     * Get an enumeration of the keys of the BSONDocument.
     *
     * @return An iterator of the keys.
     */
    public Enumeration keys() {
        return this.myHashMap.keys();
    }


    /**
     * Get the number of keys stored in the BSONDocument.
     *
     * @return The number of keys in the BSONDocument.
     */
    public int length() {
        return this.myHashMap.size();
    }


    /**
     * Produce a BSONArray containing the names of the elements of this
     * BSONDocument.
     * @return A BSONArray containing the key strings, or null if the BSONDocument
     * is empty.
     * @throws BSONException 
     */
    public BSONArray names() throws BSONException {
        BSONArray array = new BSONArray();
        Enumeration keys = keys();
        while (keys.hasMoreElements()) {
            array.put((String) keys.nextElement());
        }
        return array.length() == 0 ? null : array;
    }

    
    /**
     * Shave off trailing zeros and decimal point, if possible.
     */
    static public String trimNumber(String s) {
        if (s.indexOf('.') > 0 && s.indexOf('e') < 0 && s.indexOf('E') < 0) {
            while (s.endsWith("0")) {
                s = s.substring(0, s.length() - 1);
            }
            if (s.endsWith(".")) {
                s = s.substring(0, s.length() - 1);
            }
        }
        return s;
    }

    /**
     * Produce a string from a Number.
     * @param  n A Number
     * @return A String.
     * @throws BSONException If n is a non-finite number.
     */
    static public String numberToString(Object n)
            throws BSONException {
        if (n == null) {
            throw new BSONException("Null pointer");
        }
        testValidity(n);
        return trimNumber(n.toString());
    }

    /**
     * Get an optional value associated with a key.
     * @param key   A key string.
     * @return      An object which is the value, or null if there is no value.
     */
    public BSONElement opt(String key) {
    	if (myHashMap.containsKey(key))
    		return (BSONElement) this.myHashMap.get(key);
    	return BSONElement.NULL;
    }


    /**
     * Get an optional boolean associated with a key.
     * It returns false if there is no such key, or if the value is not
     * Boolean.TRUE or the String "true".
     *
     * @param key   A key string.
     * @return      The truth.
     */
    public boolean optBoolean(String key) {
        return optBoolean(key, false);
    }


    /**
     * Get an optional boolean associated with a key.
     * It returns the defaultValue if there is no such key, or if it is not
     * a Boolean or the String "true" or "false" (case insensitive).
     *
     * @param key              A key string.
     * @param defaultValue     The default.
     * @return      The truth.
     */
    public boolean optBoolean(String key, boolean defaultValue) {
        try {
            return getBoolean(key);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    
    /**
     * Get an optional double associated with a key,
     * or NaN if there is no such key or if its value is not a number.
     * If the value is a string, an attempt will be made to evaluate it as
     * a number.
     *
     * @param key   A string which is the key.
     * @return      An object which is the value.
     */
    public double optDouble(String key) {
        return optDouble(key, Double.NaN);
    }
    
     /**
     * Get an optional double associated with a key, or the
     * defaultValue if there is no such key or if its value is not a number.
     * If the value is a string, an attempt will be made to evaluate it as
     * a number.
     *
     * @param key   A key string.
     * @param defaultValue     The default.
     * @return      An object which is the value.
     */
    public double optDouble(String key, double defaultValue) {
        try {
            BSONElement o = opt(key);
            return Double.parseDouble(o.getValue().toString());
        } catch (Exception e) {
            return defaultValue;
        }
    }

    /**
     * Get an optional int value associated with a key,
     * or zero if there is no such key or if the value is not a number.
     * If the value is a string, an attempt will be made to evaluate it as
     * a number.
     *
     * @param key   A key string.
     * @return      An object which is the value.
     */
    public int optInt(String key) {
        return optInt(key, 0);
    }


    /**
     * Get an optional int value associated with a key,
     * or the default if there is no such key or if the value is not a number.
     * If the value is a string, an attempt will be made to evaluate it as
     * a number.
     *
     * @param key   A key string.
     * @param defaultValue     The default.
     * @return      An object which is the value.
     */
    public int optInt(String key, int defaultValue) {
        try {
            return getInt(key);
        } catch (Exception e) {
            return defaultValue;
        }
    }


    /**
     * Get an optional BSONArray associated with a key.
     * It returns null if there is no such key, or if its value is not a
     * BSONArray.
     *
     * @param key   A key string.
     * @return      A BSONArray which is the value.
     */
    public BSONArray optBSONArray(String key) {
        try {
			return opt(key).getBSONArray();
		} catch (BSONException e) {
			return null;
		}
    }


    /**
     * Get an optional BSONDocument associated with a key.
     * It returns null if there is no such key, or if its value is not a
     * BSONDocument.
     *
     * @param key   A key string.
     * @return      A BSONObject which is the value.
     */
    public BSONDocument optBSONObject(String key) {
        try {
			return opt(key).getBSONDocument();
		} catch (BSONException e) {
			return null;
		}
    }


    /**
     * Get an optional long value associated with a key,
     * or zero if there is no such key or if the value is not a number.
     * If the value is a string, an attempt will be made to evaluate it as
     * a number.
     *
     * @param key   A key string.
     * @return      An object which is the value.
     */
    public long optLong(String key) {
        return optLong(key, 0);
    }


    /**
     * Get an optional long value associated with a key,
     * or the default if there is no such key or if the value is not a number.
     * If the value is a string, an attempt will be made to evaluate it as
     * a number.
     *
     * @param key   A key string.
     * @param defaultValue     The default.
     * @return      An object which is the value.
     */
    public long optLong(String key, long defaultValue) {
        try {
            return getLong(key);
        } catch (Exception e) {
            return defaultValue;
        }
    }


    /**
     * Get an optional string associated with a key.
     * It returns an empty string if there is no such key. If the value is not
     * a string and is not null, then it is coverted to a string.
     *
     * @param key   A key string.
     * @return      A string which is the value.
     */
    public String optString(String key) {
        return optString(key, "");
    }


    /**
     * Get an optional string associated with a key.
     * It returns the defaultValue if there is no such key.
     *
     * @param key   A key string.
     * @param defaultValue     The default.
     * @return      A string which is the value.
     */
    public String optString(String key, String defaultValue) {
    	try {
			return opt(key).getString();
		} catch (BSONException e) {
			return defaultValue;
		}
    }


    /**
     * Put a key/boolean pair in the BSONDocument.
     *
     * @param key   A key string.
     * @param value A boolean which is the value.
     * @return this.
     * @throws BSONException If the key is null.
     */
    public BSONDocument put(String key, boolean value) throws BSONException {
        put(key, new BSONElement(key, value ? Boolean.TRUE : Boolean.FALSE, BSONElement.TYPE_BOOLEAN));
        return this;
    }

    /**
     * Put a key/int pair in the BSONDocument.
     *
     * @param key   A key string.
     * @param value An int which is the value.
     * @return this.
     * @throws BSONException If the key is null.
     */
    public BSONDocument put(String key, int value) throws BSONException {
        put(key, new BSONElement(key, new Integer(value), BSONElement.TYPE_INT32));
        return this;
    }


    /**
     * Put a key/long pair in the BSONDocument.
     *
     * @param key   A key string.
     * @param value A long which is the value.
     * @return this.
     * @throws BSONException If the key is null.
     */
    public BSONDocument put(String key, long value) throws BSONException {
        put(key, new BSONElement(key, new Long(value), BSONElement.TYPE_INT64));
        return this;
    }
  
    /**
     * Put a key/String pair in the BSONDocument.
     *
     * @param key   A key string.
     * @param value A String which is the value.
     * @return this.
     * @throws BSONException If the key is null.
     */
	public BSONDocument put(String key, String value) throws BSONException {
        put(key, new BSONElement(key, value, BSONElement.TYPE_STRING));
        return this;
	}
	  
    /**
     * Put a key/BSONDocument pair in the BSONDocument.
     *
     * @param key   A key string.
     * @param value A String which is the value.
     * @return this.
     * @throws BSONException If the key is null.
     */
	public BSONDocument put(String key, BSONDocument value) throws BSONException {
        put(key, new BSONElement(key, value, BSONElement.TYPE_DOCUMENT));
        return this;
	}
	  
    /**
     * Put a key/BSONArray pair in the BSONDocument.
     *
     * @param key   A key string.
     * @param value A String which is the value.
     * @return this.
     * @throws BSONException If the key is null.
     */
	public BSONDocument put(String key, BSONArray value) throws BSONException {
        put(key, new BSONElement(key, value, BSONElement.TYPE_ARRAY));
        return this;
	}
        
    /**
     * Put a key/value pair in the BSONDocument. If the value is null,
     * then the key will be removed from the BSONDocument if it is present.
     * @param key   A key string.
     * @param value An object which is the value. It should be of one of these
     *  types: Boolean, Double, Integer, BSONArray, BSONDocument, Long, String,
     *  or the BSONDocument.NULL object.
     * @return this.
     * @throws BSONException If the value is non-finite number
     *  or if the key is null.
     */
    public BSONDocument put(String key, BSONElement value) throws BSONException {
        if (value != null) {
            testValidity(value);
            this.myHashMap.put(key, value);
        } else {
            remove(key);
        }
        return this;
    }

    /**
     * Put a key/value pair in the BSONDocument, but only if the
     * key and the value are both non-null.
     * @param key   A key string.
     * @param value An object which is the value. It should be of one of these
     *  types: Boolean, Double, Integer, BSONArray, BSONDocument, Long, String,
     *  or the BSONDocument.NULL object.
     * @return this.
     * @throws BSONException If the value is a non-finite number.
     */
    public BSONDocument putOpt(String key, BSONElement value) throws BSONException {
        if (key != null && value != null) {
            put(key, value);
        }
        return this;
    }

    /**
     * Produce a string in double quotes with backslash sequences in all the
     * right places. A backslash will be inserted within </, allowing JSON
     * text to be delivered in HTML. In JSON text, a string cannot contain a
     * control character or an unescaped quote or backslash.
     * @param string A String
     * @return  A String correctly formatted for insertion in a JSON text.
     */
    public static String quote(String string) {
        if (string == null || string.length() == 0) {
            return "\"\"";
        }

        char         b;
        char         c = 0;
        int          i;
        int          len = string.length();
        StringBuffer sb = new StringBuffer(len + 4);
        String       t;

        sb.append('"');
        for (i = 0; i < len; i += 1) {
            b = c;
            c = string.charAt(i);
            switch (c) {
            case '\\':
            case '"':
                sb.append('\\');
                sb.append(c);
                break;
            case '/':
                if (b == '<') {
                    sb.append('\\');
                }
                sb.append(c);
                break;
            case '\b':
                sb.append("\\b");
                break;
            case '\t':
                sb.append("\\t");
                break;
            case '\n':
                sb.append("\\n");
                break;
            case '\f':
                sb.append("\\f");
                break;
            case '\r':
                sb.append("\\r");
                break;
            default:
                if (c < ' ') {
                    t = "000" + Integer.toHexString(c);
                    sb.append("\\u" + t.substring(t.length() - 4));
                } else {
                    sb.append(c);
                }
            }
        }
        sb.append('"');
        return sb.toString();
    }

    /**
     * Remove a name and its value, if present.
     * @param key The name to be removed.
     * @return The value that was associated with the name,
     * or null if there was no value.
     */
    public Object remove(String key) {
        return this.myHashMap.remove(key);
    }

    /**
     * Throw an exception if the object is an NaN or infinite number.
     * @param o The object to test.
     * @throws BSONException If o is a non-finite number.
     */
    static void testValidity(Object o) throws BSONException {
        if (o != null) {
            if (o instanceof Double) {
                if (((Double)o).isInfinite() || ((Double)o).isNaN()) {
                    throw new BSONException(
                        "JSON does not allow non-finite numbers");
                }
            } else if (o instanceof Float) {
                if (((Float)o).isInfinite() || ((Float)o).isNaN()) {
                    throw new BSONException(
                        "JSON does not allow non-finite numbers.");
                }
            }
        }
    }

    /**
     * Produce a BSONArray containing the values of the members of this
     * BSONDocument.
     * @param names A BSONArray containing a list of key strings. This
     * determines the sequence of the values in the result.
     * @return A BSONArray of values.
     * @throws BSONException If any of the values are non-finite numbers.
     */
    public BSONArray toBSONArray(BSONArray names) throws BSONException {
        if (names == null || names.length() == 0) {
            return null;
        }
        BSONArray ja = new BSONArray();
        for (int i = 0; i < names.length(); i += 1) {
            ja.put(this.opt(names.getString(i)));
        }
        return ja;
    }

    /**
     * Make a JSON text of this BSONDocument. For compactness, no whitespace
     * is added. If this would not result in a syntactically correct JSON text,
     * then null will be returned instead.
     * <p>
     * Warning: This method assumes that the data structure is acyclical.
     *
     * @return a printable, displayable, portable, transmittable
     *  representation of the object, beginning
     *  with <code>{</code>&nbsp;<small>(left brace)</small> and ending
     *  with <code>}</code>&nbsp;<small>(right brace)</small>.
     */
    public String toString() {
        try {
            Enumeration keys = keys();
            StringBuffer sb = new StringBuffer("{");

            while (keys.hasMoreElements()) {
                if (sb.length() > 1) {
                    sb.append(',');
                }
                Object o = keys.nextElement();
                sb.append(quote(o.toString()));
                sb.append(':');
                sb.append(valueToString(this.myHashMap.get(o)));
            }
            sb.append('}');
            return sb.toString();
        } catch (Exception e) {
            return null;
        }
    }


    /**
     * Make a prettyprinted JSON text of this BSONDocument.
     * <p>
     * Warning: This method assumes that the data structure is acyclical.
     * @param indentFactor The number of spaces to add to each level of
     *  indentation.
     * @return a printable, displayable, portable, transmittable
     *  representation of the object, beginning
     *  with <code>{</code>&nbsp;<small>(left brace)</small> and ending
     *  with <code>}</code>&nbsp;<small>(right brace)</small>.
     * @throws BSONException If the object contains an invalid number.
     */
    public String toString(int indentFactor) throws BSONException {
        return toString(indentFactor, 0);
    }


    /**
     * Make a prettyprinted JSON text of this BSONDocument.
     * <p>
     * Warning: This method assumes that the data structure is acyclical.
     * @param indentFactor The number of spaces to add to each level of
     *  indentation.
     * @param indent The indentation of the top level.
     * @return a printable, displayable, transmittable
     *  representation of the object, beginning
     *  with <code>{</code>&nbsp;<small>(left brace)</small> and ending
     *  with <code>}</code>&nbsp;<small>(right brace)</small>.
     * @throws BSONException If the object contains an invalid number.
     */
    String toString(int indentFactor, int indent) throws BSONException {
        int          i;
        int          n = length();
        if (n == 0) {
            return "{}";
        }
        Enumeration keys = keys();
        StringBuffer sb = new StringBuffer("{");
        int          newindent = indent + indentFactor;
        Object       o;
        if (n == 1) {
            o = keys.nextElement();
            sb.append(quote(o.toString()));
            sb.append(": ");
            sb.append(valueToString(this.myHashMap.get(o), indentFactor,
                    indent));
        } else {
            while (keys.hasMoreElements()) {
                o = keys.nextElement();
                if (sb.length() > 1) {
                    sb.append(",\n");
                } else {
                    sb.append('\n');
                }
                for (i = 0; i < newindent; i += 1) {
                    sb.append(' ');
                }
                sb.append(quote(o.toString()));
                sb.append(": ");
                sb.append(valueToString(this.myHashMap.get(o), indentFactor,
                        newindent));
            }
            if (sb.length() > 1) {
                sb.append('\n');
                for (i = 0; i < indent; i += 1) {
                    sb.append(' ');
                }
            }
        }
        sb.append('}');
        return sb.toString();
    }


    /**
     * Make a JSON text of an Object value. If the object has an
     * value.toJSONString() method, then that method will be used to produce
     * the JSON text. The method is required to produce a strictly
     * conforming text. If the object does not contain a toJSONString
     * method (which is the most common case), then a text will be
     * produced by the rules.
     * <p>
     * Warning: This method assumes that the data structure is acyclical.
     * @param value The value to be serialized.
     * @return a printable, displayable, transmittable
     *  representation of the object, beginning
     *  with <code>{</code>&nbsp;<small>(left brace)</small> and ending
     *  with <code>}</code>&nbsp;<small>(right brace)</small>.
     * @throws BSONException If the value is or contains an invalid number.
     */
    static String valueToString(Object value) throws BSONException {
        if (value == null || value.equals(null)) {
            return "null";
        }
        if (value instanceof JSONString) {
        	Object o;
        	try {
            	o = ((JSONString)value).toJSONString();
            } catch (Exception e) {
            	throw new BSONException(e);
            }
            if (o instanceof String) {
	        	return (String)o;
	        }
            throw new BSONException("Bad value from toJSONString: " + o);
        }
        if (value instanceof Float || value instanceof Double ||
            value instanceof Byte || value instanceof Short || 
            value instanceof Integer || value instanceof Long) {
            return numberToString(value);
        }
        if (value instanceof Boolean || value instanceof BSONDocument ||
                value instanceof BSONArray) {
            return value.toString();
        }
        if (value instanceof BSONElement) {
        	/**
        	 * @author Diego
        	 */
        	BSONElement el = (BSONElement) value;
        	if (el.isType(BSONElement.TYPE_DOCUMENT) || el.isType(BSONElement.TYPE_ARRAY) ||
        		el.isType(BSONElement.TYPE_NULL) ||	el.isType(BSONElement.TYPE_BOOLEAN) ||
       			el.isType(BSONElement.TYPE_INT32) || el.isType(BSONElement.TYPE_INT64) ||
        		el.isType(BSONElement.TYPE_DATETIME) || el.isType(BSONElement.TYPE_TIMESTAMP)) {
        		return el.toString();
        	}
        }
        return quote(value.toString());
    }


    /**
     * Make a prettyprinted JSON text of an object value.
     * <p>
     * Warning: This method assumes that the data structure is acyclical.
     * @param value The value to be serialized.
     * @param indentFactor The number of spaces to add to each level of
     *  indentation.
     * @param indent The indentation of the top level.
     * @return a printable, displayable, transmittable
     *  representation of the object, beginning
     *  with <code>{</code>&nbsp;<small>(left brace)</small> and ending
     *  with <code>}</code>&nbsp;<small>(right brace)</small>.
     * @throws BSONException If the object contains an invalid number.
     */
     static String valueToString(Object value, int indentFactor, int indent)
            throws BSONException {
        if (value == null || value.equals(null)) {
            return "null";
        }
        try {
	        if (value instanceof JSONString) {
		        Object o = ((JSONString)value).toJSONString();
		        if (o instanceof String) {
		        	return (String)o;
		        }
	        }
        } catch (Exception e) {
        	/* forget about it */
        }
        if (value instanceof Float || value instanceof Double ||
            value instanceof Byte || value instanceof Short || 
            value instanceof Integer || value instanceof Long) {
            return numberToString(value);
        }
        if (value instanceof Boolean) {
            return value.toString();
        }
        if (value instanceof BSONDocument) {
            return ((BSONDocument)value).toString(indentFactor, indent);
        }
        if (value instanceof BSONArray) {
            return ((BSONArray)value).toString(indentFactor, indent);
        }
        if (value instanceof BSONElement) {
        	/**
        	 * @author Diego
        	 */
        	BSONElement el = (BSONElement) value;
        	if (el.isType(BSONElement.TYPE_DOCUMENT) || el.isType(BSONElement.TYPE_ARRAY) ||
           		el.isType(BSONElement.TYPE_NULL) ||	el.isType(BSONElement.TYPE_BOOLEAN) ||
          		el.isType(BSONElement.TYPE_INT32) || el.isType(BSONElement.TYPE_INT64) ||
           		el.isType(BSONElement.TYPE_DATETIME) || el.isType(BSONElement.TYPE_TIMESTAMP)) {
           		return el.toString();
        	}
        }
        return quote(value.toString());
    }


     /**
      * Write the contents of the BSONDocument as JSON text to a writer.
      * For compactness, no whitespace is added.
      * <p>
      * Warning: This method assumes that the data structure is acyclical.
      *
      * @return The writer.
      * @throws BSONException
      */
     public Writer write(Writer writer) throws BSONException {
        try {
            boolean  b = false;
            Enumeration keys = keys();
            writer.write('{');

            while (keys.hasMoreElements()) {
                if (b) {
                    writer.write(',');
                }
                Object k = keys.nextElement();
                writer.write(quote(k.toString()));
                writer.write(':');
                Object v = this.myHashMap.get(k);
                if (v instanceof BSONDocument) {
                    ((BSONDocument)v).write(writer);
                } else if (v instanceof BSONArray) {
                    ((BSONArray)v).write(writer);
                } else {
                    writer.write(valueToString(v));
                }
                b = true;
            }
            writer.write('}');
            return writer;
        } catch (IOException e) {
            throw new BSONException(e);
        }
     }

}