package com.cinnamonbob.filesystem;

import com.cinnamonbob.core.BobException;

/**
 * <class-comment/>
 */
public class FileSystemException extends BobException
{
    public FileSystemException(String errorMessage)
    {
        super(errorMessage);
    }

    public FileSystemException()
    {
    }

    public FileSystemException(Throwable cause)
    {
        super(cause);
    }

    public FileSystemException(String errorMessage, Throwable cause)
    {
        super(errorMessage, cause);
    }
}
