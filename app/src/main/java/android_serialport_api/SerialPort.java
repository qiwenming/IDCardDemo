/*
 * Copyright 2009 Cedric Priscal
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License. 
 */

package android_serialport_api;

import android.util.Log;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * 串口类
 * @author qiwenming
 *
 */
public class SerialPort {

	private static final String TAG = "SerialPort";

	/*
	 * Do not remove or rename the field mFd: it is used by native method close();
	 */
	private FileDescriptor mFd;//文件描述
	private FileInputStream mFileInputStream;
	private FileOutputStream mFileOutputStream;
	
	/**
	 *获得一个窗口 
	 * @param device 设备
	 * @param baudrate 波特率
	 * @param flags 标志
	 * @throws SecurityException
	 * @throws IOException
	 */
	public SerialPort(File device, int baudrate, int flags) throws SecurityException, IOException {

		/* Check access permission */ //检查权限
		if (!device.canRead() || !device.canWrite()) {
			try {
				//如果丢失权限，就再获取权限
				/* Missing read/write permission, trying to chmod the file */
				Process su;
				su = Runtime.getRuntime().exec("/system/bin/su");
				String cmd = "chmod 666 " + device.getAbsolutePath() + "\n"
						+ "exit\n";
				//写命令
				su.getOutputStream().write(cmd.getBytes());
				if ((su.waitFor() != 0) || !device.canRead()
						|| !device.canWrite()) {
					throw new SecurityException();
				}
			} catch (Exception e) {
				e.printStackTrace();
				throw new SecurityException();
			}
		}
		//打开设备，这里面调用jni 的open方法
		mFd = open(device.getAbsolutePath(), baudrate, flags);
		if (mFd == null) {
			Log.e(TAG, "native open returns null");
			throw new IOException();
		}
		mFileInputStream = new FileInputStream(mFd);
		mFileOutputStream = new FileOutputStream(mFd);
	}

	// Getters and setters
	public FileInputStream getInputStream() {
		return mFileInputStream;
	}

	public FileOutputStream getOutputStream() {
		return mFileOutputStream;
	}

	//============== JNI=========================================
	/**
	 * 打开串口设备的方法
	 * @param path 设备的绝对路径
	 * @param baudrate 波特率
	 * @param flags 标志
	 * @return
	 */
	private native static FileDescriptor open(String path, int baudrate, int flags);
	//关闭设备
	public native void close();
	//加载库文件
	static {
		System.loadLibrary("serial_port");
	}
}
