// dependency: ./namespace.js
// dependency: ext/package.js
// dependency: zutubi/table/namespace.js

/**
 * A box that highlights the primary status of something, e.g. the health of a project, the result
 * of a build.  This component it intended to signal to the user quickly if things are good or bad,
 * and present key details.
 *
 * @cfg {String} id            Id to use for the box.
 * @cfg {String} titleTemplate Template applied to the data to yield the title to display at the top
 *                             of the box.
 * @cfg {Array}  fields        An array of Zutubi.KeyValue configs defining fields to show in the
 *                             box.
 * @cfg {Object} data          Data object used to populate the box.  Must contain properties to
 *                             matching the title template and fields, along with a health property
 *                             with a value from {unknown, inprogress, broken, warnings, ok}.
 */
Zutubi.pulse.project.StatusBox = Ext.extend(Ext.BoxComponent, {
    template: new Ext.XTemplate(
        '<table class="status-box {health}-box" id="{id}">' +
            '<tr><th colspan="2"></th></tr>' +
        '</table>'),

    rowTemplate: new Ext.XTemplate(
        '<tr class="' + Zutubi.table.CLASS_DYNAMIC + '">' +
            '<td class="status-key {extraCls}">{key}</td>' +
            '<td id="{id}" class="status-value {extraCls}">{value}</td>' +
        '</tr>'),
    
    initComponent: function() {
        this.titleTemplate = new Ext.XTemplate(this.titleTemplate);

        var fieldConfigs = this.fields;
        this.fields = [];
        for (var i = 0; i < fieldConfigs.length; i++)
        {
            this.fields.push(new Zutubi.KeyValue(fieldConfigs[i]));
        }
    },
    
    onRender: function(container, position) {
        var args = {
            id: this.id,
            health: this.data.health
        };
        
        if (position)
        {
            this.el = this.template.insertBefore(position, args, true);    
        }
        else
        {
            this.el = this.template.append(container, args, true);
        }
        
        this.tbodyEl = this.el.down('tbody');
        this.titleEl = this.tbodyEl.down('tr').down('th');
        
        this.renderData();
        
        Zutubi.pulse.project.StatusBox.superclass.onRender.apply(this, arguments);
    },
    
    renderData: function(/*data*/) {
        this.titleTemplate.overwrite(this.titleEl, this.data);
        for (var i = 0, l = this.fields.length; i < l; i++)
        {
            var field = this.fields[i];
            var args = {
                id: this.id + '-' + field.name,
                key: field.key,
                value: field.getRenderedValue(this.data),
                extraCls: i == l - 1 ? 'status-last' : ''
            };
            
            this.rowTemplate.append(this.tbodyEl, args);
        }
    },
    
    update: function(data) {
        this.data = data;
        if (this.rendered)
        {
            this.clearDataRows();
            if (data)
            {
                this.renderData();
            }
        }
    },
    
    clearDataRows: function(/*data*/) {
         var els = this.tbodyEl.select('.' + Zutubi.table.CLASS_DYNAMIC);
         els.remove();
    }
});

Ext.reg('xzstatusbox', Zutubi.pulse.project.StatusBox);