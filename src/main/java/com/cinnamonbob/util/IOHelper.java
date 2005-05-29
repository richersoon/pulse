package com.cinnamonbob.util;

import java.io.*;
import java.net.Socket;
import java.net.ServerSocket;
import java.util.Properties;

/**
 * @author Daniel Ostermeier
 */
public class IOHelper
{
    public static void close(Socket s)
    {
        try
        {
            if (s != null)
            {
                s.close();
            }
        } catch (IOException e)
        {
            // nop
        }
    }

    public static Properties read(File f) throws IOException
    {
        return read(new FileInputStream(f));
    }

    public static Properties read(InputStream input) throws IOException
    {
        try
        {
            Properties properties = new Properties();
            properties.load(input);
            return properties;
        } 
        finally
        {
            IOHelper.close(input);
        }
    }
    
    public static void close(InputStream i)
    {
        try
        {
            if (i != null)
            {
                i.close();
            }
        } catch (IOException e)
        {
            //nop
        }
    }

    public static void close(Reader r)
    {
        try
        {
            if (r != null)
            {
                r.close();
            }
        } catch (IOException e)
        {
            // nop
        }
    }

    public static void close(ServerSocket s)
    {
        try
        {
            if (s != null)
            {
                s.close();
            }
        } catch (IOException e)
        {
            // nop
        }
    }

    public static void close(OutputStream outStream)
    {
        try
        {
            if (outStream != null)
            {
                outStream.close();
            }
        } 
        catch (IOException e)
        {
            //noop    
        }
    }
    
    public static void close(Writer w)
    {
        try
        {
            if (w != null)
            {
                w.close();
            }
        } 
        catch (IOException e)
        {
            //noop    
        }
    }
    
    public static void joinStreams(InputStream input, OutputStream output) throws IOException
    {
        byte[] buffer = new byte[1024];
        int n;

        while ((n = input.read(buffer)) > 0)
        {
            output.write(buffer, 0, n);
        }
    }

    public static void joinReaderToWriter(Reader reader, Writer writer) throws IOException
    {
        char[] buffer = new char[1024];
        int n;

        while ((n = reader.read(buffer)) > 0)
        {
            writer.write(buffer, 0, n);
        }
    }

    public static void copyFile(File fromFile, File toFile) throws IOException
    {
        FileInputStream inStream = null;
        FileOutputStream outStream = null;

        try
        {
            inStream = new FileInputStream(fromFile);
            outStream = new FileOutputStream(toFile);
            joinStreams(inStream, outStream);
        } 
        finally
        {
            IOHelper.close(inStream);
            IOHelper.close(outStream);
        }
    }
}
