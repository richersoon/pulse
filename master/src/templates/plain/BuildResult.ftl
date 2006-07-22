[#ftl]
=====================================================================
:: ${result.project.name} ::
=====================================================================
Build ${result.number?c} has completed with status '${result.state.prettyString}'.

You can view the full build result at:

${baseUrl}/viewBuild.action?id=${result.id?c}

[#if result.reason?exists]
Build reason: ${result.reason.summary}.

[/#if]
Build stages:
[#list result.root.children as child]
  * ${child.stage} :: ${child.result.recipeNameSafe}@${child.hostSafe} :: ${result.state.prettyString}
[/#list]

[#if changelists?exists]
    [#if changelists?size &gt; 0]
New changes in this build:
        [#list changelists as change]
            [#assign revision = change.revision]
  * ${revision.revisionString} by ${revision.author}:
    ${renderer.wrapString(renderer.trimmedString(revision.comment, 180), "    ")}
        [/#list]
    [#else]
There were no new changes in this build.
    [/#if]
[/#if]

[@buildMessages result=result level=errorLevel/]

[@buildMessages result=result level=warningLevel/]

[@buildFailedTests result=result/]
