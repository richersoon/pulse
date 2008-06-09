package com.zutubi.pulse.agent;

import com.mockobjects.dynamic.C;
import com.mockobjects.dynamic.Mock;
import com.zutubi.pulse.services.SlaveService;
import com.zutubi.pulse.services.SlaveStatus;
import com.zutubi.pulse.test.PulseTestCase;

/**
 */
public class AgentPingTest extends PulseTestCase
{
    private String token = "test token";
    private String masterLocation = "test location";
    private int buildNumber = 10;
    private Mock mockAgent;
    private Mock mockService;

    public void setUp() throws Exception
    {
        super.setUp();
        mockAgent = new Mock(Agent.class);
        mockService = new Mock(SlaveService.class);
    }

    public void testSuccessfulPing() throws Exception
    {
        SlaveStatus status = new SlaveStatus(Status.IDLE, 12, true);
        mockService.expectAndReturn("ping", buildNumber);
        mockService.expectAndReturn("getStatus", C.args(C.eq(token), C.eq(masterLocation)), status);

        AgentPing ping = new AgentPing((Agent) mockAgent.proxy(), (SlaveService) mockService.proxy(), buildNumber, masterLocation, token);
        assertSame(status, ping.call());
        verify();
    }

    public void testFailedPing() throws Exception
    {
        mockService.expectAndThrow("ping", new RuntimeException("bang"));
        mockAgent.expectAndReturn("getName", "slaaave");

        AgentPing ping = new AgentPing((Agent) mockAgent.proxy(), (SlaveService) mockService.proxy(), buildNumber, masterLocation, token);
        assertEquals(new SlaveStatus(Status.OFFLINE, "Exception: 'java.lang.RuntimeException'. Reason: bang"), ping.call());
        verify();
    }

    public void testFailedGetStatus() throws Exception
    {
        mockService.expectAndReturn("ping", buildNumber);
        mockService.expectAndThrow("getStatus", C.args(C.eq(token), C.eq(masterLocation)), new RuntimeException("oops"));
        mockAgent.expectAndReturn("getName", "slaaave");
        
        AgentPing ping = new AgentPing((Agent) mockAgent.proxy(), (SlaveService) mockService.proxy(), buildNumber, masterLocation, token);
        assertEquals(new SlaveStatus(Status.OFFLINE, "Exception: 'java.lang.RuntimeException'. Reason: oops"), ping.call());
        verify();
    }

    public void testVersionMismatch() throws Exception
    {
        mockService.expectAndReturn("ping", buildNumber - 1);

        AgentPing ping = new AgentPing((Agent) mockAgent.proxy(), (SlaveService) mockService.proxy(), buildNumber, masterLocation, token);
        assertEquals(new SlaveStatus(Status.VERSION_MISMATCH), ping.call());
        verify();
    }

    private void verify()
    {
        mockAgent.verify();
        mockService.verify();
    }
}
