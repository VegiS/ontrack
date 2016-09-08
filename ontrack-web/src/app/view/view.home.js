angular.module('ot.view.home', [
        'ui.router',
        'ot.service.structure',
        'ot.service.core',
        'ot.service.user'
    ])
    .config(function ($stateProvider) {
        $stateProvider.state('home', {
            url: '/home',
            templateUrl: 'app/view/view.home.tpl.html',
            controller: 'HomeCtrl'
        });
    })
    .controller('HomeCtrl', function ($rootScope, $location, $log, $scope, $http, ot, otStructureService, otNotificationService, otUserService) {
        var search = $location.search();
        var code = search.code;
        var url = search.url;
        $rootScope.view = {
            // Title
            title: 'Home',
            // Commands
            commands: []
        };
        // No initial filter
        $scope.projectFilter = {
            name: ''
        };
        // Loading the project list
        function loadProjects() {
            $scope.loadingProjects = true;
            ot.pageCall($http.get('structure/projects')).then(function (projectResources) {
                $scope.projectResources = projectResources;
                // Commands
                $rootScope.view.commands = [
                    {
                        id: 'createProject',
                        name: 'Create project',
                        cls: 'ot-command-project-new',
                        condition: function () {
                            return projectResources._create;
                        },
                        action: $scope.createProject
                    }, {
                        id: 'showDisabled',
                        name: "Show all hidden items",
                        cls: 'ot-command-show-disabled',
                        condition: function () {
                            return !$scope.showDisabled;
                        },
                        action: function () {
                            $scope.showDisabled = true;
                        }
                    }, {
                        id: 'hideDisabled',
                        name: "Hide disabled items",
                        cls: 'ot-command-hide-disabled',
                        condition: function () {
                            return $scope.showDisabled;
                        },
                        action: function () {
                            $scope.showDisabled = false;
                        }
                    }, {
                        name: "API",
                        cls: 'ot-command-api',
                        link: '/api-doc'
                    }
                ];
            });
            // Detailed views
            ot.pageCall($http.get('structure/projects/favourites')).then(function (projectStatusViewResources) {
                $scope.projectStatusViewResources = projectStatusViewResources;
                $scope.projectStatusViews = projectStatusViewResources.resources;
                // All branches disabled status computation
                $scope.projectStatusViews.forEach(function (projectStatusView) {
                    projectStatusView.allBranchesDisabled = projectStatusView.branchStatusViews.length > 0 &&
                        projectStatusView.branchStatusViews.every(function (branchStatusView) {
                            return branchStatusView.branch.disabled || branchStatusView.branch.type == 'TEMPLATE_DEFINITION';
                        });
                });
            }).finally(function () {
                $scope.loadingProjects = false;
            });
        }

        // Creating a project
        $scope.createProject = function () {
            otStructureService.createProject($scope.projectResources._create).then(loadProjects);
        };

        // Sets a project as favourite
        $scope.projectFavourite = function (project) {
            if (project._favourite) {
                ot.pageCall($http.put(project._favourite)).then(loadProjects);
            }
        };

        // Unsets a project as favourite
        $scope.projectUnfavourite = function (project) {
            if (project._unfavourite) {
                ot.pageCall($http.put(project._unfavourite)).then(loadProjects);
            }
        };

        // Login procedure
        $scope.accessStatus = 'undefined';
        if (code && code == 403) {
            $log.debug('[403] received');
            if (otUserService.logged()) {
                $scope.accessStatus = 'unauthorised';
                $log.debug('[403] user already logged - error notification');
            } else {
                $scope.accessStatus = 'login-requested';
                $log.debug('[403] user not logged - login redirection - callback URL = ' + url);
                otUserService.login().then(function () {
                    $scope.accessStatus = 'ok';
                    if (url) {
                        // Callback URL
                        $log.debug('[403] reloading ' + url + 'after signing in.');
                        location.href = url;
                        location.reload();
                    } else {
                        // Reloads current page
                        $log.debug('[403] reloading after signing in.');
                        location.reload();
                    }
                }, function () {
                    $scope.accessStatus = 'login-failed';
                    $log.debug('[403] user cannot be logged - error notification');
                });
            }
        } else if ($rootScope.user && $rootScope.user.authenticationRequired && !$rootScope.user.logged) {
            $scope.accessStatus = 'login-requested';
            $log.debug('user not logged and authentication is required');
            otUserService.login().then(function () {
                $scope.accessStatus = 'ok';
                $log.debug('[403] reloading after signing in.');
                location.reload();
            }, function () {
                $scope.accessStatus = 'login-failed';
                $log.debug('[403] user cannot be logged - error notification');
            });
        } else {
            $scope.accessStatus = 'ok';
            // Loading the list of projects
            loadProjects();
        }

    })
;