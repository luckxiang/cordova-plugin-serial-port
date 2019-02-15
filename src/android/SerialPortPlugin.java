package com.plugin.SerialPortPlugin;

import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import com.example.x6.serial.SerialPort;
import java.util.concurrent.locks.*;
/**
 * This class echoes a string called from JavaScript.
 */
public class SerialPortPlugin extends CordovaPlugin {
    private SerialPort serialPort;
    private InputStream inputStream;
    private OutputStream outputStream;
    private ReadDataThread readThread;
    private boolean dataModel;

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        if (action.equals("openDevice")) {
            String message = args.getString(0);
            this.openDevice(message, callbackContext);
            return true;
        }
        else if (action.equals("closeDevice")) {
            this.closeDevice(callbackContext);
            return true;
        }
        else if (action.equals("read")) {
            this.readSerialData(callbackContext);
            return true;
        }
        else if (action.equals("write")) {
            String message = args.getString(0);
            this.writeSerialData(message, callbackContext);
            return true;
        }
        else if (action.equals("sendDataAndWaitResponse")) {
            String message = args.getString(0);
            int timeout = args.getInt(1);
            System.out.println("timeout:" + timeout);
            this.sendDataAndWaitResponse(message, timeout, callbackContext);
            return true;
        }
        else if (action.equals("setHex")) {
            String message = args.getString(0);
            this.setHex(message);
            return true;
        }

        return false;
    }

    private void openDevice(String message, CallbackContext callbackContext) {
        JSONArray jsonArray = null;
        JSONObject arg = null;
        String devName = null;
        int baudrate = 0;
        int flags = 0;
        if (message != null && message.length() > 0) {
            try {
                try {
                    jsonArray = new JSONArray(message);
                }
                catch(Exception e){
                    callbackContext.error("get json array exception");
                    return;
                }
                try {
                    arg = jsonArray.getJSONObject(0);
                }
                catch(Exception e){
                    callbackContext.error("get arg exception");
                    return;
                }
                try {
                    devName = arg.getString("dev");
                }
                catch(Exception e){
                    callbackContext.error("get dev exception");
                    return;
                }
                try {
                    baudrate =  arg.getInt("baudrate");
                }
                catch(Exception e){
                    callbackContext.error("get baudrate exception");
                    return;
                }
                try {
                    flags = arg.getInt("flags");
                }
                catch(Exception e){
                    callbackContext.error("get flags exception");
                    return;
                }
                try {
                    this.dataModel = arg.getBoolean("isHex");
                }
                catch(Exception e){
                    this.dataModel = false;
                }
                System.out.println("dataModel:" + this.dataModel);

                serialPort = new SerialPort(new File(devName), baudrate, flags);
                inputStream = serialPort.getInputStream();
                outputStream = serialPort.getOutputStream();
                readThread = new ReadDataThread( "Thread-Read", inputStream, this.dataModel);
                readThread.start();
                callbackContext.success("open device success");
            } catch (IOException e) {
                e.printStackTrace();
                callbackContext.error("open device exception");
            }
        } else {
            callbackContext.error("open device fail");
        }
    }

    private void closeDevice(CallbackContext callbackContext) {
            try {
                readThread.stop();
                outputStream.close();
                serialPort.close();
                callbackContext.success("close device success");
            } catch (Throwable e) {
                e.printStackTrace();
                callbackContext.error("close device exception");
        }
    }

    private void readSerialData(CallbackContext callbackContext) {
        String data = readThread.getData();
        if(data == null){
            callbackContext.error("null");
        }else {
            callbackContext.success(data);
        }
    }

    private void writeSerialData(String message, CallbackContext callbackContext) {
        if (message != null && message.length() > 0) {
            try {
                byte[] byteArray;
                if(this.dataModel == true) {
                    byteArray =FormatUtil.hexString2Bytes(message);
                } else {
                    byteArray = message.getBytes();
                }
                outputStream.write(byteArray);
                System.out.println("writestr:" + message);
                callbackContext.success("write data success");
            } catch (IOException e) {
                e.printStackTrace();
                callbackContext.error("write data exception");
            }
        } else {
            callbackContext.error("write data fail");
        }
    }

    private void sendDataAndWaitResponse(String message, int timeout, CallbackContext callbackContext) {
        if (message != null && message.length() > 0) {
            try {
                byte[] byteArray;
                if(this.dataModel == true) {
                    byteArray =FormatUtil.hexString2Bytes(message);
                } else {
                    byteArray = message.getBytes();
                }
                outputStream.write(byteArray);
                String data = readThread.getData();
                int i = 0;
                while(data == null && i < (int)(timeout/10)) {
                    try {
                        Thread.sleep(10);
                    } catch(Exception e) {
                    }
                    i++;
                    data = readThread.getData();
                }
                if(data == null) {
                    callbackContext.error("read data timeout");
                } else {
                    callbackContext.success(data);
                }
            } catch (IOException e) {
                e.printStackTrace();
                callbackContext.error("write data exception");
            }
        } else {
            callbackContext.error("write data fail");
        }
    }

    private void setHex(String message) {
        if (message != null && message.length() > 0) {
            this.dataModel = Boolean.parseBoolean(message);
        } else {
            this.dataModel = false;
        }
        readThread.setHex(this.dataModel);
    }
}

class ReadDataThread implements Runnable {
   private Thread t;
   private String threadName;
   private InputStream input;
   private int readLen = 0;
   private String readData = "";
   private  Lock lock=new ReentrantLock();
   private boolean dataModel;
   private boolean running = true;

   ReadDataThread( String name, InputStream inputStream, boolean model) {
      threadName = name;
      input = inputStream;
      dataModel = model;
      System.out.println("Creating " +  threadName );
   }

   public void run() {
	 int readSize = -1;
     while(running) {
         try {
			 if((readSize = input.available()) <= 0) {  //get the buffer length before read. if you do not, the read will block
				try {
					Thread.sleep(1);
				} catch(Exception e) {
				}
				continue;
			 }
          } catch (IOException e) {
                e.printStackTrace();
      			System.out.println("Thread" +  threadName + " break exiting..");
				break;
          }
          System.out.println("readSize:" + readSize);
          byte[] byteArray = new byte[readSize];
          try {
                readLen = input.read(byteArray);
          } catch (IOException e) {
                e.printStackTrace();
      			System.out.println("Thread" +  threadName + " break exiting..");
				break;
          }
          lock.lock(); // must lock to copy readData
          if(this.dataModel == true) {
			 readData += FormatUtil.bytes2HexString(byteArray, readLen);
          } else {
			readData += new String(byteArray);
          }
          lock.unlock();
          System.out.println("readstr:" + readData);
      }

	  if(running) {
		 try {
			input.close();
		 } catch (IOException e) {
			e.printStackTrace();
		 }
	  }
      System.out.println("Thread" +  threadName + " success exiting..");
   }

   public void setHex(boolean model) {
      this.dataModel = model;
      System.out.println("dataModel:" + this.dataModel);
   }

   public String getData() {
        String data = null;
        lock.lock();
        if(readData != "") {
            data = new String(readData);
            readData = "";
        }
        lock.unlock();
        return data;
   }

   public void start() {
      System.out.println("Starting " +  threadName );
      if (t == null) {
         t = new Thread (this, threadName);
         t.start ();
      }
   }

   public void stop() {
	 this.running = false;
     try {
		input.close();
     } catch (IOException e) {
        e.printStackTrace();
     }
  }
}
