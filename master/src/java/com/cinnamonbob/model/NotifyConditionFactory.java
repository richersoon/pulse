package com.cinnamonbob.model;

import java.util.Map;
import java.util.HashMap;

/**
 * <class-comment/>
 */
public class NotifyConditionFactory
{

    public static final String ALL_BUILDS = "all builds";
    public static final String ALL_CHANGED = "all changed";
    public static final String ALL_FAILED = "all failed";
    public static final String ALL_CHANGED_OR_FAILED = "all changed or failed";

    private final static Map<String, NotifyCondition> instanceMap = new HashMap<String, NotifyCondition>();
    static {
        instanceMap.put(ALL_BUILDS, new TrueNotifyCondition());
        instanceMap.put(ALL_CHANGED, new ChangedNotifyCondition());
        instanceMap.put(ALL_FAILED, new FailedNotifyCondition());
        instanceMap.put(ALL_CHANGED_OR_FAILED, new CompoundNotifyCondition(new ChangedNotifyCondition(), new FailedNotifyCondition(), true));
    }

    public static NotifyCondition getInstance(String key)
    {
        return instanceMap.get(key);
    }
}
