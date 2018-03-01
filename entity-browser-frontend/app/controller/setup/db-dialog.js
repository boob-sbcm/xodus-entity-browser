angular.module('xodus').controller('DBDialogController', [
    '$scope',
    '$http',
    '$uibModalInstance',
    'databaseService',
    function ($scope, $http, $modalInstance, databaseService) {
        var dbDialogCtrl = this;

        dbDialogCtrl.isChaCha = true;
        dbDialogCtrl.error = null;


        dbDialogCtrl.toggleProvider = function () {
            dbDialogCtrl.isChaCha = !dbDialogCtrl.isChaCha;
        };
        dbDialogCtrl.cancel = function () {
            $modalInstance.dismiss('cancel');
        };

        dbDialogCtrl.getMessage = function (name) {
            var field = $scope.database[name];
            if (('undefined' === typeof(field)) || field.$valid) {
                return undefined;
            }
            var message = '';
            if (field.$error['required']) {
                message += ' - is required';
            } else {
                message += ' - is invalid';
            }
            return message;
        };

        dbDialogCtrl.db = {
            location: angular.isDefined(dbDialogCtrl.location) ? dbDialogCtrl.location : null,
            key: angular.isDefined(dbDialogCtrl.key) ? dbDialogCtrl.key : null,
            opened: true,
            encrypted: false
        };

        dbDialogCtrl.saveDB = function () {
            if ($scope.database.$valid) {
                dbDialogCtrl.db.encryptionProvider = (dbDialogCtrl.isChaCha ? 'SALSA' : 'CHACHA');
                databaseService.add(dbDialogCtrl.db).then(function (db) {
                    return $modalInstance.close(db);
                });
            }
        };

        var hubKey = 'jetPassServerDb';
        var youtrackKey = 'teamsysstore';


        dbDialogCtrl.predefinedKeys = [
            {name: 'Hub', key: hubKey},
            {name: 'YouTrack', key: youtrackKey}
        ];

        dbDialogCtrl.applyKey = function (item) {
            dbDialogCtrl.db.key = item.key;
        };
    }]);