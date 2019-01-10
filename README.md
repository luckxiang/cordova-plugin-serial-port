# cordova-plugin-serial-port
ionic3 基于cordova编写的安卓串口通信插件  ionic3 serial port plugins for android

# support
 1. read serial data
 2. write serial data
 3. write serial data and wait response 

# how to use
1. mv libs to platforms/android
2. add the plugins to your project
3. change the read protocol in ReadDataThread class
4. then you can use HEX string to send or recive frame data.

# ts example: 
```

import { Injectable } from '@angular/core';
import  { CRC } from './crc';

enum COMMAND {
    setSystemCfg = 0x01,
    setWorkingCfg = 0x02,
    setControl = 0x03,
    setWeight = 0x04,
    getSysData = 0x11,
    getWorkingCfg = 0x12,
    getControl = 0x13,
    getWeight = 0x14,
    getRealTimeData = 0x15
}

enum CONTROL_ACTION {
    start = 0x01,
    pause = 0x02,
    stop = 0x03,
    unloading = 0x04,
    calibration = 0x05,
    netWeight = 0x06
}


declare let cordova: any;

@Injectable()
export class RunControl {
  interval:any = null;
  constructor() {
    cordova.plugins.SerialPortPlugin.openSerialPort([{dev:'/dev/ttyS0',baudrate:115200, flags:0}],result=>alert(result),error=>alert(error));
  }


/** 数据格式
帧头|命令 |长度  |帧内容|校验   |帧尾|
0x68|UINT8|UINT16|N BYTE|异或CRC|0x16|
**/
  createFrame(com:COMMAND, data:number[], isUNIT16:boolean) {
    let frame = '68';
    frame += CRC.padLeft(com.toString(16), 2); //padLeft 补位函数，数据不够用0补位

    let lenStr = ''
    if(isUNIT16) {
        lenStr = (data.length * 2).toString(16);
    } else {
        lenStr = data.length.toString(16);
    }
    lenStr = CRC.padLeft(lenStr, 4);
    frame += lenStr;

    data.forEach(function(value){
        console.log(value);
        if(value === null) {
            value = 0;
        }
        let vaStr = parseInt(value.toString()).toString(16);
        if(isUNIT16) {
            vaStr = CRC.padLeft(vaStr, 4);
          } else {
            vaStr = CRC.padLeft(vaStr, 2);
        }
        frame += vaStr;
    });

    let crc = CRC.ToModbusCRC16(frame, true);
    frame += crc;
    frame += '16';
    console.log(frame);

    return frame;
  }

  sendFrame(com:COMMAND, data:number[], isUNIT16:boolean) {
    let frame = this.createFrame(com, data, isUNIT16);
    cordova.plugins.SerialPortPlugin.sendDataAndWaitResponse(frame,
      res=> {
            console.log(res);
            if(res.length > 7 && (res.charAt(2) === '8' || res.charAt(2) === '9')) { //校验第三个字符是否是8或9，ack为命令字|0x80
                console.log('ACK check 成功');
            } else {
                alert("系统异常,返回数据校验错误");
            }
      },
      error=> {
          alert(error);
      });
  }

  getRealTimeData():any {
    let that =this;
    return  new Promise(function(resolve, reject){
        let frame = that.createFrame(COMMAND.getRealTimeData, [], false);
        cordova.plugins.SerialPortPlugin.sendDataAndWaitResponse(frame,
          res=> {
            console.log("实时数据：" + res);
            if(res.length === 42 && res.charAt(2) === '9') { //校验第三个字符是否是9，ack为命令字|0x80
                console.log('ACK check 成功');
                resolve(that.parseFrame(res));
            } else {
                alert("系统异常,返回数据校验错误");
            }
         },
         error=> {
          alert(error);
        });
    });
  }

  start(s, w) {
    this.sendFrame(COMMAND.setSystemCfg, [s.feedingDelayTime,s.unloadingDelayTime,s.startupDelayTime,s.comparisonTime,s.zeroPositionRange,s.zeroTrackingRange,
    s.automaticUnloading? 1: 0,s.reversingTime,s.mixingTimes,s.turnTime,s.autodrop?1:0,s.dropWater,s.dropCement,s.dropAdditive1,s.dropAdditive2,s.range], true);

    this.sendFrame(COMMAND.setWorkingCfg, [w.formulationUsed.water,w.formulationUsed.cement,w.formulationUsed.additive1,w.formulationUsed.additive2,w.repeat], true);

    this.sendFrame(COMMAND.setControl, [CONTROL_ACTION.start], false);
    console.log("开始");
  }

  readRealTimeData() {
   return this.getRealTimeData();
  }

}

```
