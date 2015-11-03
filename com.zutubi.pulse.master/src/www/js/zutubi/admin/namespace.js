// dependency: zutubi/namespace.js

if (window.Zutubi.admin === undefined)
{
    window.Zutubi.admin = (function($)
    {
        var app = {},
            baseUrl = window.baseUrl;

        function _createNotificationWidget()
        {
            var notificationElement = $("#notification");
            return notificationElement.kendoNotification({
                autoHideAfter: 7000,
                allowHideAfter: 1000,
                button: true,
                hideOnClick: false,
                position: {
                    top: 50
                },
                stacking: "down"
            }).data("kendoNotification");
        }

        function _showScope(scope, name)
        {
            if (app.configPanel)
            {
                app.configPanel.destroy();
                delete app.configPanel;
            }

            if (!app.scopePanel)
            {
                app.scopePanel = new Zutubi.admin.ScopePanel("#config-view");
                app.scopePanel.bind("select", function(e)
                {
                    var url = "/hierarchy/" + e.scope;
                    if (e.name.length > 0)
                    {
                        url += "/" + encodeURIComponent(e.name);
                    }

                    app.router.navigate(url, true);
                });
            }

            app.scopePanel.setScope(scope, name);
        }

        function _showConfig(path, templated)
        {
            var rootIndex = templated ? 2 : 1,
                rootPath = Zutubi.admin.subPath(path, 0, rootIndex),
                configPath = Zutubi.admin.subPath(path, rootIndex);

            if (app.scopePanel)
            {
                app.scopePanel.destroy();
                delete app.scopePanel;
            }

            if (!app.configPanel)
            {
                app.configPanel = new Zutubi.admin.ConfigPanel("#config-view");
                app.configPanel.bind("pathselect", function(e)
                {
                    console.log("PATH SELECT '" + e.path + '"');
                    app.router.navigate("/config/" + Zutubi.admin.encodePath(e.path), true);
                });
            }

            app.configPanel.setPaths(rootPath, configPath);
        }

        function _createRouter()
        {
            var router = new kendo.Router({
                root: baseUrl + "/admin",
                pushState: true,
                routeMissing: function(e)
                {
                    app.notificationWidget.error("Unknown admin path '" + e.url + "', redirecting.");
                    router.navigate("/");
                }
            });

            router.route("/", function()
            {
                router.navigate("/hierarchy/projects");
            });

            router.route("/hierarchy/projects(/)(:name)(/)", function(name)
            {
                app.navbar.selectScope("projects");
                _showScope("projects", name);
            });

            router.route("/config/projects(/)*path", function(path)
            {
                var normalisedPath = Zutubi.admin.normalisedPath(path);
                app.navbar.selectScope("projects", normalisedPath);
                _showConfig("projects/" + normalisedPath, true);
            });

            router.route("/hierarchy/agents(/)(:name)(/)", function(name)
            {
                app.navbar.selectScope("agents");
                _showScope("agents", name);
            });

            router.route("/config/agents(/)*path", function(path)
            {
                var normalisedPath = Zutubi.admin.normalisedPath(path);
                app.navbar.selectScope("agents", normalisedPath);
                if (path)
                {
                    _showConfig("agents/" + normalisedPath, true);
                }
            });

            router.route("/config/settings(/)*path", function(path)
            {
                app.navbar.selectScope("settings");
                _showConfig("settings/" + Zutubi.admin.normalisedPath(path), false);
            });

            router.route("/config/users(/)*path", function(path)
            {
                app.navbar.selectScope("users");
                _showConfig("users/" + Zutubi.admin.normalisedPath(path), false);
            });

            router.route("/config/groups(/)*path", function(path)
            {
                app.navbar.selectScope("groups");
                _showConfig("groups/" + Zutubi.admin.normalisedPath(path), false);
            });

            router.route("/plugins(/)(:id)", function(id)
            {
                app.navbar.selectScope("plugins");
            });

            return router;
        }

        function _showAddWizard(scope)
        {
            var path, item, label, window;

            if (app.scopePanel)
            {
                path = scope;
                item = app.scopePanel.getItem();
                if (item)
                {
                    path += "/" + item;
                }
            }
            else if (app.configPanel)
            {
                path = app.configPanel.getRootPath();
            }

            label = Zutubi.admin.subPath(path, 0, 1);
            label = label.substring(0, label.length - 1);

            window = new Zutubi.admin.WizardWindow({
                path: path,
                label: label,
                success: function(delta)
                {
                    app.router.navigate("/config/" + Zutubi.admin.encodePath(delta.addedPaths[0]), false);
                }
            });

            window.show();
        }

        function _createNavbar(options)
        {
            var navbar = $("#navbar").kendoZaNavbar(options).data("kendoZaNavbar");
            navbar.bind("scope-selected", function(e)
            {
                if (e.scope === "projects" || e.scope === "agents")
                {
                    app.router.navigate("/hierarchy/" + e.scope);
                }
                else if (e.scope === "plugins")
                {
                    app.router.navigate("/plugins");
                }
                else
                {
                    app.router.navigate("/config/" + e.scope);
                }
            });

            navbar.bind("item-selected", function(e)
            {
                var rootPath = app.configPanel.getRootPath(),
                    configPath = app.configPanel.getConfigPath();

                rootPath = Zutubi.admin.subPath(rootPath, 0, 1) + "/" + e.name;
                app.router.navigate(Zutubi.admin.encodePath("/config/" + rootPath + "/" + configPath), true);
                // Lazy setPaths will take care of choosing the longest valid config path.
                app.configPanel.setPaths(rootPath, configPath, true);
            });

            navbar.bind("add", function(e)
            {
                _showAddWizard(e.scope);
            });

            return navbar;
        }

        function _coerceInt(properties, name)
        {
            var value, newValue;
            if (properties.hasOwnProperty(name))
            {
                value = properties[name];
                if (value === "")
                {
                    newValue = null;
                }
                else
                {
                    newValue = Number(value);
                }

                properties[name] = newValue;
            }
        }

        return {
            app: app,

            ACTION_ICONS: {
                "add": "plus-circle",
                "addComment": "comment",
                "changePassword": "key",
                "clean": "eraser",
                "clearResponsibility": "user-times",
                "clone": "clone",
                "convertToCustom": "code",
                "convertToVersioned": "file-code-o",
                "delete": "trash",
                "disable": "toggle-off",
                "enable": "toggle-on",
                "fire": "bolt",
                "hide": "trash-o",
                "initialise": "refresh",
                "kill": "exclamation-circle",
                "pause": "pause",
                "pin": "thumb-tack",
                "ping": "bullseye",
                "pullUp": "angle-double-up",
                "pushDown": "angle-double-down",
                "rebuild": "bolt",
                "reload": "repeat",
                "rename": "pencil",
                "restore": "plus-square-o",
                "resume": "play",
                "setPassword": "key",
                "takeResponsibility": "wrench",
                "trigger": "bolt",
                "unpin": "minus",
                "view": "arrow-circle-right",
                "write": "pencil-square-o"
            },

            LINK_ICONS: {
                "config": "pencil",
                "dependencies": "sitemap",
                "home": "home",
                "homepage": "external-link",
                "history": "clock-o",
                "info": "info-circle",
                "log": "file-text",
                "messages": "files-o",
                "reports": "bar-chart",
                "rss": "rss",
                "statistics": "pie-chart",
                "status": "heartbeat"
            },

            init: function(isAdmin, projectCreateAllowed, agentCreateAllowed)
            {
                app.notificationWidget = _createNotificationWidget();
                app.router = _createRouter();
                app.navbar = _createNavbar({
                    isAdmin: isAdmin,
                    projectCreateAllowed: projectCreateAllowed,
                    agentCreateAllowed: agentCreateAllowed
                });
            },

            start: function()
            {
                app.router.start();
            },

            normalisedPath: function(path)
            {
                if (!path)
                {
                    return "";
                }

                if (path.length > 0 && path[0] === "/")
                {
                    path = path.substring(1);
                }
                if (path.length > 0 && path[path.length - 1] === "/")
                {
                    path = path.substring(0, path.length - 1);
                }

                return path;
            },

            subPath: function(path, begin, end)
            {
                var elements = path.split("/");

                if (typeof end === "undefined")
                {
                    end = elements.length;
                }

                elements = elements.slice(begin, end);
                return elements.join("/");
            },

            parentPath: function(path)
            {
                var i = path.lastIndexOf("/");
                if (i >= 0)
                {
                    return path.substring(0, i);
                }

                return null;
            },

            baseName: function(path)
            {
                var i = path.lastIndexOf("/");
                if (i >= 0)
                {
                    return path.substring(i + 1);
                }

                return path;
            },

            encodePath: function(path)
            {
                var pieces, encodedPath, i;

                pieces = path.split('/');
                encodedPath = '';

                for (i = 0; i < pieces.length; i++)
                {
                    if (encodedPath.length > 0)
                    {
                        encodedPath += '/';
                    }

                    encodedPath += encodeURIComponent(pieces[i]);
                }

                return encodedPath;
            },

            reportSuccess: function(message)
            {
                app.notificationWidget.success(message);
            },

            reportError: function(message)
            {
                app.notificationWidget.error(message);
            },

            reportWarning: function(message)
            {
                app.notificationWidget.warning(message);
            },

            openConfigPath: function(newPath)
            {
                app.router.navigate("/config/" + Zutubi.admin.encodePath(newPath), false);
            },

            replaceConfigPath: function(newPath)
            {
                app.router.replace("/config/" + Zutubi.admin.encodePath(newPath), true);
            },

            coerceProperties: function(properties, propertyTypes)
            {
                var i,
                    propertyType;

                if (propertyTypes)
                {
                    for (i = 0; i < propertyTypes.length; i++)
                    {
                        propertyType = propertyTypes[i];
                        if (propertyType.shortType === "int")
                        {
                            _coerceInt(properties, propertyType.name);
                        }
                    }
                }
            },

            hasCollapsedCollection: function(data)
            {
                // Simple (<10 field) composite with a single nested collection will be collapsed.
                return data.kind === "composite" &&
                    data.nested && data.nested.length === 1 && data.nested[0].kind === "collection" &&
                    (!data.type.simpleProperties || data.type.simpleProperties.length < 10);
            },

            labelCompare: function(a1, a2)
            {
                return a1.label.localeCompare(a2.label);
            }
        };
    }(jQuery));
}
