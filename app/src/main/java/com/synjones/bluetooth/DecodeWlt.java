package com.synjones.bluetooth;

public class DecodeWlt {
	public native int Wlt2Bmp(String wltPath, String bmpPath);
	static{
		System.loadLibrary("DecodeWlt");
	}
}
