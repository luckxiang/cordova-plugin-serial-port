# cordova-plugin-serial-port
ionic3 基于cordova编写的安卓串口通信插件  ionic3 serial port plugins for android

# support
 1. read string & hexString
 2. write string & hexString
 3. write data and wait response 
 4. read is not block, you must check read data in you application. include response data.
 5. if you recive 2 or more message and do not read in time. you will get all in next read.
 6. UART device does not check data is complete & right. you should have a protocol for exchanging data between two devices.

# how to use
1. add the plugins to your project 'ionic cordova plugins add ./cordova-plugin-serial-port'
2. 'declare let cordova: any;'before use api. this way not  good. can anybody help me?
3. enjoy

# API

#### openDevice
param: 
- config
- success callback 
- error callback

config:
- dev: serial port device node
- baudrate: serial port baud rate
- flags:  srdial port flags
- isHex: if you want to use hexString you should set it to true.

example:
```
cordova.plugins.SerialPortPlugin.openDevice([{dev:'/dev/ttyS0',baudrate:115200, flags:0, isHex:false}],
    result=>alert(result),
    error=>alert(error));
}
```

#### closeDevice
example:
```
cordova.plugins.SerialPortPlugin.closeDevice(
    result=>alert(result),
    error=>alert(error)
);

```

#### read
example:
```
cordova.plugins.SerialPortPlugin.read(
  res=> {
        console.log(res);
        alert(res);
  },
  error=> {
      alert(error);
  });
```

#### write
example:
```
cordova.plugins.SerialPortPlugin.write('12345678900000000000000000000000123',
  res=> {
        console.log(res);
        alert(res);
  },
  error=> {
      alert(error);
  });
```

#### sendDataAndWaitResponse
response data maybe not complete,you can use read api to get the rest.

param: 
- arg1: data
- arg2: timoutMs

example:
```
cordova.plugins.SerialPortPlugin.sendDataAndWaitResponse('12345678900000000000000000000000123',1000,
  res=> {
        console.log(res);
        alert(res);
  },
  error=> {
      alert(error);
  });

```

#### setHex
you can change the hexString | string  after openDevice

example:
```
cordova.plugins.SerialPortPlugin.setHex(true);
```



