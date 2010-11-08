// dependency: ./namespace.js
// dependency: ext/package.js
// dependency: zutubi/ActivePanel.js
// dependency: zutubi/table/LinkTable.js
// dependency: zutubi/table/PropertyTable.js
// dependency: zutubi/pulse/SectionHeading.js
// dependency: zutubi/pulse/project/BuildSummaryTable.js
// dependency: zutubi/pulse/project/ResponsibilityBox.js
// dependency: zutubi/pulse/project/StatusBox.js

/**
 * The content of the project home page.
 */
Zutubi.pulse.project.browse.ProjectHomePanel = Ext.extend(Zutubi.ActivePanel, {
    layout: 'border',
    border: false,
    
    dataKeys: ['responsibility', 'status', 'activity', 'latest', 'recent', 'changes', 'actions', 'links'],
    
    initComponent: function(container, position) {
        var panel = this;
        Ext.apply(this, {
            defaults: {
                layout: 'fit',
                border: false,
                autoScroll: true
            },
            contentEl: 'center',
            items: [{
                region: 'center',
                id: this.id + '-main',
                split: false,
                layout: 'vtable',
                items: [{
                    xtype: 'xzresponsibilitybox',
                    id: this.id + '-responsibility',
                    projectId: this.projectId,
                    style: 'margin: 0 17px'
                }, {
                    xtype: 'container',
                    layout: 'htable',
                    items: [{
                        xtype: 'xzstatusbox',
                        id: this.id + '-status',
                        titleTemplate: '{name:htmlEncode}',
                        fields: [
                            {name: 'health'},
                            {name: 'state', renderer: Zutubi.pulse.project.renderers.projectState},
                            {name: 'successRate', key: 'success rate', renderer: Zutubi.pulse.project.renderers.projectSuccessRate},
                            {name: 'statistics', renderer: Zutubi.pulse.project.renderers.projectStatistics}
                        ]
                    }, {
                        xtype: 'box'
                    },
                    {
                        xtype: 'xzsummarytable',
                        id: this.id + '-activity',
                        title: 'current activity',
                        columns: [
                            Zutubi.pulse.project.configs.build.number,
                            Zutubi.pulse.project.configs.build.status,
                            Zutubi.pulse.project.configs.build.reason,
                            Zutubi.pulse.project.configs.build.revision
                        ],
                        emptyMessage: 'no current build activity'
                    }]
                }, {
                    xtype: 'xzsectionheading',
                    text: 'builds'
                }, {
                    xtype: 'container',
                    layout: 'htable',
                    items: [{
                        xtype: 'xzpropertytable',
                        id: this.id + '-latest',
                        title: 'latest completed build',
                        rows: [
                            Zutubi.pulse.project.configs.build.number,
                            Zutubi.pulse.project.configs.build.status,
                            Zutubi.pulse.project.configs.build.reason,
                            Zutubi.pulse.project.configs.build.revision,
                            Zutubi.pulse.project.configs.build.tests,
                            Zutubi.pulse.project.configs.build.errors,
                            Zutubi.pulse.project.configs.build.warnings,
                            Zutubi.pulse.project.configs.build.when,
                            Zutubi.pulse.project.configs.build.elapsed,
                            Zutubi.pulse.project.configs.build.stages
                        ],
                        emptyMessage: 'no completed builds found'
                    }, {
                        xtype: 'box'
                    }, {
                        xtype: 'xzbuildsummarytable',
                        id: this.id + '-recent',
                        title: 'recently completed builds',
                        selectedColumns: this.recentColumns,
                        emptyMessage: 'no historic builds found'
                    }]
                }, {
                    xtype: 'xzsectionheading',
                    text: 'changes'
                }, {
                    xtype: 'xzsummarytable',
                    id: this.id + '-changes',
                    title: 'latest changes',
                    cellCls: 'hpad',
                    columns: [
                        Zutubi.pulse.project.configs.changelist.rev,
                        'who',
                        Zutubi.pulse.project.configs.changelist.when,
                        Zutubi.pulse.project.configs.changelist.comment,
                        Zutubi.pulse.project.configs.changelist.actions
                    ],
                    emptyMessage: 'no changes found'
                }]
            }, {
                region: 'east',
                id: this.id + '-right',
                bodyStyle: 'padding: 0 17px',
                split: true,
                collapsible: true,
                collapseMode: 'mini',
                hideCollapseTool: true,
                width: 300,
                layout: 'vtable',
                items: [{
                    xtype: 'xzlinktable',
                    id: this.id + '-actions',
                    title: 'actions',
                    handlers: {
                        clean: this.markForClean.createDelegate(this),
                        clearResponsibility: clearResponsibility.createDelegate(window, [this.projectId]),
                        takeResponsibility: takeResponsibility.createDelegate(window, [this.projectId]),
                        trigger: window.baseUrl + '/triggerBuild.action?projectId=' + this.projectId,
                        rebuild: window.baseUrl + '/triggerBuild.action?rebuild=true&projectId=' + this.projectId,
                        viewSource: Zutubi.fs.viewWorkingCopy.createDelegate(window, [this.projectId])
                    }
                }, {
                    xtype: 'xzlinktable',
                    id: this.id + '-links',
                    title: 'links',
                    iconTemplate: 'images/config/links/{icon}.gif',
                    listeners: {
                        afterrender: function() {
                            panel.updateRows();
                        }
                    }
                }]
            }]
        });
        
        Zutubi.pulse.project.browse.ProjectHomePanel.superclass.initComponent.apply(this, arguments);
    },
        
    update: function(data) {
        Zutubi.pulse.project.browse.ProjectHomePanel.superclass.update.apply(this, arguments);
        if (this.rendered)
        {
            this.updateRows();
        }
    },

    updateRows: function() {
        Ext.getCmp('project-home-main').getLayout().checkRows();
        Ext.getCmp('project-home-right').getLayout().checkRows();
    },
    
    handleMarkForCleanResponse: function(options, success, response)
    {
        if (success)
        {
            var result = Ext.util.JSON.decode(response.responseText);
            if (result.success)
            {
                if (result.status)
                {
                    showStatus(result.status.message, result.status.type);
                }
            }
            else
            {
                showStatus(Ext.util.Format.htmlEncode(result.detail), 'failure');
            }
        }
        else
        {
            showStatus('Cannot contact server', 'failure');
        }
    },
    
    markForClean: function()
    {
        showStatus('Cleaning up build directories...', 'working');
        Ext.Ajax.request({
            url: window.baseUrl + '/ajax/config/projects/' + encodeURIComponent(this.data.status.name) + '?clean=clean',
            callback: this.handleMarkForCleanResponse,
            scope: this
        });
    }
});
