package com.cinnamonbob.core.util;

/**
 */
public class StringUtils
{
    /**
     * Returns the given string, trimmed if necessary to the given maximum
     * length.  Upon trimming, the last 3 characters in the returned string
     * will be "...": and the returned string will be exactly length
     * characters long including these dots.  If 3 characters cannot fit
     * under the limit, only length dots will be returned.
     *
     * @param s      the string to trim
     * @param length the maximum length of the returned string
     * @return the given string, trimmed if necessary
     */
    public static String trimmedString(String s, int length)
    {
        if (s.length() > length)
        {
            if (length >= 3)
            {
                return s.substring(0, length - 3) + "...";
            }
            else
            {
                String result = "";
                for (int i = 0; i < length; i++)
                {
                    result += ".";
                }

                return result;
            }
        }

        return s;
    }

    /**
     * Returns a version of the given string wrapped if necessary to ensure
     * it does not exceed the line length satisfied.  The string will be
     * wrapped at whitespace if possible but if not will be broken whereever
     * necessary to ensure the line length is not exceeded.  Wrapping is done
     * by replacing a space with a newline, or inserting a newline (when no
     * space is found to replace).  Existing newlines are ignored.
     *
     * @param s          the string to wrap
     * @param lineLength the maximum length of lines returned in the result
     * @param prefix     An optional prefix to add after each inserted newline
     *                   character.  The returned lines will still not exceed
     *                   lineLength.  Thus the prefix length must be less than
     *                   lineLength - 1.  If null is passed, no prefix is used.
     * @return a version of the given string with newlines inserted to
     *         wrap lines at the given length
     */
    public static String wrapString(String s, int lineLength, String prefix)
    {
        if (prefix != null && prefix.length() >= lineLength - 1)
        {
            throw new IllegalArgumentException("prefic length must be less than line length -1");
        }

        // Short circuit a common case
        if (s.length() < lineLength)
        {
            return s;
        }

        int length = s.length();
        int effectiveLineLength = lineLength;
        StringBuilder result = new StringBuilder(length + length * 2 / lineLength);

        for (int i = 0; i < length;)
        {
            if (length - i <= effectiveLineLength)
            {
                // Last bit
                result.append(s.substring(i));
                break;
            }

            // Check for existing newlines in this span
            int j;
            boolean alreadySplit = false;
            for(j = i + effectiveLineLength; j >= i; j--)
            {
                if(s.charAt(j) == '\n')
                {
                    // Already split at this point, continue from the split
                    alreadySplit = true;
                    result.append(s.substring(i, j + 1));
                    if(prefix != null)
                    {
                        result.append(prefix);
                    }
                    i = j + 1;
                    break;
                }
            }

            if(!alreadySplit)
            {
                // Need to find a place to trim, starting at i + effectiveLineLength
                int candidate = i + effectiveLineLength;
                for (j = candidate; j > i; j--)
                {
                    if (s.charAt(j) == ' ')
                    {
                        // OK, found a spot to split
                        result.append(s.substring(i, j));
                        result.append('\n');
                        if (prefix != null)
                        {
                            result.append(prefix);
                        }

                        i = j + 1;
                        break;
                    }
                }

                if (j == i)
                {
                    // No space found
                    result.append(s.substring(i, candidate));
                    result.append('\n');
                    if (prefix != null)
                    {
                        result.append(prefix);
                    }
                    i = candidate;
                }
            }

            if (prefix != null)
            {
                effectiveLineLength = lineLength - prefix.length();
            }
        }

        return result.toString();
    }
}
