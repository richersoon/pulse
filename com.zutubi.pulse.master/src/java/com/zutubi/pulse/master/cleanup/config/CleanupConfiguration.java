package com.zutubi.pulse.master.cleanup.config;

import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import com.zutubi.pulse.core.engine.api.ResultState;
import com.zutubi.pulse.master.model.BuildResult;
import com.zutubi.pulse.master.model.Project;
import com.zutubi.pulse.master.model.persistence.BuildResultDao;
import com.zutubi.tove.annotations.ControllingCheckbox;
import com.zutubi.tove.annotations.Form;
import com.zutubi.tove.annotations.Format;
import com.zutubi.tove.annotations.SymbolicName;
import com.zutubi.util.Constants;
import com.zutubi.util.StringUtils;
import com.zutubi.validation.annotations.Required;

import java.util.LinkedList;
import java.util.List;

/**
 * The cleanup configuration defines how builds are cleaned up for a project.  This includes
 * both when cleanup occurs, and what parts of the builds are cleaned up.
 */
@SymbolicName("zutubi.cleanupConfig")
@Form(fieldOrder = {"name", "cleanupAll", "what", "retain", "unit", "states", "statuses"})
public class CleanupConfiguration extends AbstractCleanupConfiguration
{
    @ControllingCheckbox(uncheckedFields = {"what"})
    private boolean cleanupAll = true;

    @Required
    @Format("CleanupWhatColumnFormatter")
    private List<CleanupWhat> what;

    public CleanupConfiguration(CleanupWhat what, List<ResultState> states, int count, CleanupUnit unit)
    {
        this.what = new LinkedList<CleanupWhat>();
        if (what != null)
        {
            this.what.add(what);
            this.cleanupAll = false;
        }
        else
        {
            this.cleanupAll = true;
        }
        this.states = states;
        this.retain = count;
        this.unit = unit;
    }

    public CleanupConfiguration()
    {
    }

    public List<CleanupWhat> getWhat()
    {
        return what;
    }

    public void setWhat(List<CleanupWhat> what)
    {
        this.what = what;
    }

    public boolean isCleanupAll()
    {
        return cleanupAll;
    }

    public void setCleanupAll(boolean cleanupAll)
    {
        this.cleanupAll = cleanupAll;
    }

    @Override
    public List<BuildResult> getMatchingResults(Project project, BuildResultDao dao)
    {
        ResultState[] allowedStates = resolveAllowedStates();
        String[] allowedStatuses = resolveAllowedStatuses();

        List<BuildResult> results = new LinkedList<BuildResult>();
        if(unit == CleanupUnit.BUILDS)
        {
            // See if there are too many builds of our states.  We assume here
            // we are called from within the build manager (so these two dao
            // calls are within the same transaction).
            int total = dao.getBuildCount(project, allowedStates, allowedStatuses, false);
            if(total > retain)
            {
                // Clean out the difference
                results.addAll(dao.queryBuilds(new Project[]{project}, allowedStates, allowedStatuses, 0, 0, 0, total - retain, false, false));
            }
        }
        else if (unit == CleanupUnit.DAYS)
        {
            long startTime = System.currentTimeMillis() - retain * Constants.DAY;
            results.addAll(dao.queryBuilds(new Project[]{project}, allowedStates, allowedStatuses, 0, startTime, -1, -1, false, false));
        }

        return results;
    }

    @Override
    public String summarise()
    {
        String whatString;
        if (cleanupAll)
        {
            whatString = "everything";
        }
        else
        {
            final CleanupWhatColumnFormatter whatFormatter = new CleanupWhatColumnFormatter();
            whatString = StringUtils.join(", ", Iterables.transform(what, new Function<CleanupWhat, String>()
            {
                public String apply(CleanupWhat input)
                {
                    return whatFormatter.format(input);
                }
            }));
        }

        String unit = getUnit().toString().toLowerCase();
        if (getRetain() == 1)
        {
            unit = StringUtils.stripSuffix(unit, "s");
        }

        return "remove " + whatString + " for " + summariseFilter() + " after " + getRetain() + " " + unit;
    }
}
