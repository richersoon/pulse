package com.zutubi.pulse.web.ajax;

/**
 */
public class SimpleResult
{
    private boolean success;
    private String detail;

    public SimpleResult(boolean success, String detail)
    {
        this.success = success;
        this.detail = detail;
    }

    public boolean isSuccess()
    {
        return success;
    }

    public String getDetail()
    {
        return detail;
    }
}
