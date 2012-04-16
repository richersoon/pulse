package com.zutubi.pulse.acceptance;

import com.zutubi.pulse.acceptance.pages.browse.BuildChangesPage;
import com.zutubi.pulse.acceptance.utils.BuildRunner;
import com.zutubi.pulse.acceptance.utils.TriviAntProject;
import com.zutubi.pulse.core.scm.api.Changelist;
import static com.zutubi.util.CollectionUtils.asPair;
import com.zutubi.util.adt.Pair;
import static java.util.Arrays.asList;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Acceptance tests for interactions between dependencies and changes between builds.
 */
public class DependenciesWithChangesAcceptanceTest extends AcceptanceTestBase
{
    private BuildRunner buildRunner;

    @Override
    protected void setUp() throws Exception
    {
        super.setUp();
        rpcClient.loginAsAdmin();
        buildRunner = new BuildRunner(rpcClient.RemoteApi);
    }

    @Override
    protected void tearDown() throws Exception
    {
        rpcClient.logout();
        super.tearDown();
    }

    public void testChangeToUpstreamBuild() throws Exception
    {
        TriviAntProject upstream = projectConfigurations.createTrivialAntProject(random + "-upstream");
        configurationHelper.insertProject(upstream.getConfig(), false);
        TriviAntProject downstream = projectConfigurations.createTrivialAntProject(random + "-downstream");
        downstream.addDependency(upstream);
        configurationHelper.insertProject(downstream.getConfig(), false);

        // Run two upstream builds with a change between.  Wait for the second triggered downstream
        // build to complete.
        buildRunner.triggerSuccessfulBuild(upstream);
        upstream.editAndCommitBuildFile("a change", random);
        buildRunner.triggerSuccessfulBuild(upstream);
        rpcClient.RemoteApi.waitForBuildToComplete(downstream.getName(), 2);

        // Now check the upstream change appears.
        getBrowser().loginAsAdmin();
        BuildChangesPage changesPage = getBrowser().openAndWaitFor(BuildChangesPage.class, downstream.getName(), 2L);
        assertEquals(1, changesPage.getUpstreamChangeCount());
        Changelist changelist = changesPage.getUpstreamChangelists().get(0);
        assertEquals("a change", changelist.getComment());
        assertEquals(1, changelist.getChanges().size());
        assertEquals("/accept/trunk/triviant/build.xml", changelist.getChanges().get(0).getPath());

        assertEquals(asList(asList(asPair(upstream.getName(), 2L))), changesPage.getUpstreamChangeVia(1));
    }

    public void testSameChangeToMultipleUpstreamBuilds() throws Exception
    {
        // util <-+
        //  ^      \ 
        //  |      app
        //  |      /
        // lib  <-+
        TriviAntProject util = projectConfigurations.createTrivialAntProject(random + "-util");
        configurationHelper.insertProject(util.getConfig(), false);
        TriviAntProject lib = projectConfigurations.createTrivialAntProject(random + "-lib");
        lib.addDependency(util);
        configurationHelper.insertProject(lib.getConfig(), false);
        TriviAntProject app = projectConfigurations.createTrivialAntProject(random + "-app");
        app.addDependency(util);
        app.addDependency(lib);
        configurationHelper.insertProject(app.getConfig(), false);

        // Run two util builds with a change between.  Wait for the second triggered app build to
        // complete.  Note the projects all use the same repo, so to ensure they all see this change
        // we need to wait out the first build off app before making the change.
        buildRunner.triggerSuccessfulBuild(util);
        rpcClient.RemoteApi.waitForBuildToComplete(app.getName(), 1);
        util.editAndCommitBuildFile("multi ball", random);
        buildRunner.triggerSuccessfulBuild(util);
        rpcClient.RemoteApi.waitForBuildToComplete(app.getName(), 2);

        // Now check the upstream change appears via multiple paths.
        getBrowser().loginAsAdmin();
        BuildChangesPage changesPage = getBrowser().openAndWaitFor(BuildChangesPage.class, app.getName(), 2L);
        assertEquals(1, changesPage.getUpstreamChangeCount());
        Changelist changelist = changesPage.getUpstreamChangelists().get(0);
        assertEquals("multi ball", changelist.getComment());

        List<List<Pair<String,Long>>> vias = changesPage.getUpstreamChangeVia(1);
        // Sort with longer vias last, those of equal length sorted by project name.
        Collections.sort(vias, new Comparator<List<Pair<String, Long>>>()
        {
            public int compare(List<Pair<String, Long>> o1, List<Pair<String, Long>> o2)
            {
                if (o1.size() != o2.size())
                {
                    return o1.size() - o2.size();
                }
                
                for (int i = 0; i < o1.size(); i++)
                {
                    int nameComp = o1.get(i).first.compareTo(o2.get(i).first);
                    if (nameComp != 0)
                    {
                        return nameComp;
                    }
                }
                
                return 0;
            }
        });
        
        assertEquals(asList(
                asList(asPair(lib.getName(), 2L)),
                asList(asPair(util.getName(), 2L)),
                asList(asPair(lib.getName(), 2L), asPair(util.getName(), 2L))
        ), vias);
    }
}