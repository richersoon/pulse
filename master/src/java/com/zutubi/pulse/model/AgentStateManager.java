package com.zutubi.pulse.model;

import java.util.List;

/**
 * <class-comment/>
 */
public interface AgentStateManager extends EntityManager<AgentState>
{
    List<AgentState> getAll();

    AgentState getAgentState(long id);

    void delete(long id);

}
