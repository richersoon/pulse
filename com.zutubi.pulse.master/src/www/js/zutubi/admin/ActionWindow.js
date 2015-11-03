// dependency: ./namespace.js
// dependency: ./WorkflowWindow.js
// dependency: ./Form.js

(function($)
{
    var WorkflowWindow = Zutubi.admin.WorkflowWindow,
        CLONE = "clone",
        CLONE_KEY = "cloneKey",
        CLONE_KEY_PREFIX = "cloneKey_",
        DEFAULT_ACTIONS = [CLONE, "pullUp", "pushDown"];

    Zutubi.admin.ActionWindow = WorkflowWindow.extend({
        init: function (options)
        {
            var that = this,
                actionPart;

            that.options = jQuery.extend({}, that.options, options);

            if (DEFAULT_ACTIONS.indexOf(options.action.action) >= 0)
            {
                actionPart = options.action.action;
            }
            else
            {
                actionPart = "single/" + options.action.action;
            }

            that.url = "/api/action/" + actionPart + "/" + Zutubi.admin.encodePath(options.path);

            WorkflowWindow.fn.init.call(that, {
                url: that.url,
                title: options.action.label,
                continueLabel: options.action.label,
                width: 600,
                render: jQuery.proxy(that._render, that),
                success: jQuery.proxy(that._execute, that)
            });
        },

        _render: function(data, el)
        {
            var that = this,
                wrapper = $("<div></div>");

            that.action = data;

            that.form = wrapper.kendoZaForm({
                parentPath: Zutubi.admin.parentPath(that.options.path),
                baseName: Zutubi.admin.baseName(that.options.path),
                structure: data.form,
                values: data.formDefaults || [],
                submits: []
            }).data("kendoZaForm");

            that.form.bind("enterPressed", jQuery.proxy(that.complete, that));
            el.append(wrapper);
        },

        _translateProperties: function()
        {
            var properties,
                fields,
                field,
                name,
                i;

            // Some actions need to transform from the form to a more direct representation.
            // FIXME kendo this is perhaps where we also need to coerce? Could generic actions
            // have a type to allow this?
            if (this.action.action === CLONE)
            {
                properties = {};
                properties[Zutubi.admin.baseName(this.options.path)] = this.form.getFieldNamed(CLONE_KEY).getValue();
                fields = this.form.getFields();
                for (i = 0; i < fields.length; i++)
                {
                    field = fields[i];
                    name = field.getFieldName();
                    if (name.indexOf(CLONE_KEY_PREFIX) === 0 && field.isEnabled())
                    {
                        properties[name.substring(CLONE_KEY_PREFIX.length)] = field.getValue();
                    }
                }
            }
            else
            {
                properties = this.form.getValues();
            }

            return properties;
        },

        _translateErrors: function(errorDetails)
        {
            var fieldErrors,
                baseName,
                field,
                translated;

            if (this.action.action === CLONE)
            {
                fieldErrors = errorDetails.fieldErrors;
                if (fieldErrors)
                {
                    baseName = Zutubi.admin.baseName(this.options.path);
                    translated = {};
                    for (field in fieldErrors)
                    {
                        if (fieldErrors.hasOwnProperty(field))
                        {
                            if (field === baseName)
                            {
                                translated[CLONE_KEY] = fieldErrors[field];
                            }
                            else
                            {
                                translated[CLONE_KEY_PREFIX + field] = fieldErrors[field];
                            }
                        }
                    }

                    errorDetails.fieldErrors = translated;
                }
            }

            return errorDetails;
        },

        _execute: function()
        {
            var that = this,
                properties = that._translateProperties();

            that.form.clearValidationErrors();

            that.mask(true);

            Zutubi.admin.ajax({
                type: "POST",
                url: that.url,
                data: {
                    kind: "composite",
                    properties: properties
                },
                success: function (data)
                {
                    that.mask(false);
                    that.close();
                    that.options.executed(data);
                },
                error: function (jqXHR)
                {
                    var details;

                    that.mask(false);
                    if (jqXHR.status === 422)
                    {
                        try
                        {
                            details = JSON.parse(jqXHR.responseText);
                            if (details.type === "com.zutubi.pulse.master.rest.errors.ValidationException")
                            {
                                that.form.showValidationErrors(that._translateErrors(details));
                                return;
                            }
                        }
                        catch(e)
                        {
                            // Do nothing.
                        }
                    }

                    Zutubi.admin.reportError("Could not perform action: " + Zutubi.admin.ajaxError(jqXHR));
                }
            });
        }
    });
}(jQuery));
