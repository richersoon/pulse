<table id="config.table" width="60%">
<#assign tablewidth = table.columns?size + 1/>
    <tr>
        <th class="heading" colspan="${tablewidth}">${table.heading?html}</th>
    </tr>
    <tr>
<#list table.columns as column>
<#assign header = "${column.name}.label"/>
        <th class="content">${header?i18n}</th>
</#list>
        <th class="content">${"actions.label"?i18n}</th>
    </tr>
<#if data?exists && data?size &gt; 0>
    <#list data as item>
        <#assign rawId = "item:${baseName(item)}"/>
    <tr id="${rawId?id}">
        <#list table.columns as column>
            <#assign value = column.getValue(item)/>
        <td class="content">${value?string?html}</td>
        </#list>
        <td class="content" width="5%">
        <#assign first = true/>
        <#list table.getActions(item) as action>
            <#assign actionlabel = "${action}.label"/>
            <#assign actionId = "${action}:${baseName(item)}"/>
            <#if !first>
    |
            </#if>
    <a id="${actionId?id}"
            <#if action == "view">
                <#if embedded>
                    <#assign clickAction = "edit"/>
                <#else>
                    <#assign clickAction = "select"/>
                </#if>
        onclick="${clickAction}Path('${item.configurationPath}'); return false">${"view.label"?i18n}<#t>
            <#elseif action == "delete">
        onclick="deletePath('${item.configurationPath}'); return false;">${"delete.label"?i18n}<#t>
            <#else>
        onclick="actionPath('${item.configurationPath}?${action}'); return false;">${actionlabel?i18n}<#t>
            </#if>
            </a><#lt>
            <#assign first = false/>
        </#list>
        </td>
    </tr>
    </#list>
<#else>
    <tr>
        <td class="content" colspan="${tablewidth}">${"no.data.available"?i18n}</td>
    </tr>
</#if>
<#if table.addAllowed>
    <tr>
        <td class="content" colspan="${tablewidth}"><a id="map:add" onclick="addToPath('${path}'); return false;">${"add.label"?i18n}</a></td>
    </tr>
</#if>
</table>