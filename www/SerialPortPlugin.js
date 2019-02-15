var exec = require('cordova/exec');


var SerialPort = {

    openDevice: function (settings, success, error) {
        exec(success, error, "SerialPortPlugin", "openDevice", [settings]);
    },
    closeDevice: function (success, error) {
        exec(success, error, "SerialPortPlugin", "closeDevice", []);
    },
    read: function (success, error) {
        exec(success, error, "SerialPortPlugin", "read", []);
    },
    write: function (data, success, error) {
        exec(success, error, "SerialPortPlugin", "write", [data]);
    },
    sendDataAndWaitResponse: function (data, timeoutMs, success, error) {
        exec(success, error, "SerialPortPlugin", "sendDataAndWaitResponse", [data, timeoutMs]);
    },
    setHex: function (isTrue) {
        exec(null, null, "SerialPortPlugin", "setHex", [isTrue]);
    }
};

module.exports = SerialPort;
