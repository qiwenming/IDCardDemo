# IDCardDemo
身份证二代扫描

http://blog.csdn.net/qiwenmingshiwo/article/details/50698927


[TOC]
#android串口通信——身份证识别器
本文主要解决的问题：
1.身份证识别器硬件的使用
2.读取到数据的解析
##一、身份证识别器基础
###1.调用身份证识别器的步骤
调用身份证识别器的步骤分为三步：
- 1.寻找身份证信息
- 2.选取身份证信息
- 3.读取身份证信息

###2.波特率
身份证识别器的默认波特率是115200。

###3.基本指令
- 1、寻找身份证信息：
寻卡命令： AA AA AA 96 69 00 03 20 01 22
返 回 值： AA AA AA 96 69 00 08 00 00 9F 00 00 00 00 97

- 2、选取身份证信息：
选卡命令： AA AA AA 96 69 00 03 20 02 21
返 回 值： AA AA AA 96 69 00 0C 00 00 90 00 00 00 00 00 00 00 00 9C

- 3、读取身份证信息(文字+照片信息)：
读卡命令： AA AA AA 96 69 00 03 30 01 32 
返 回 值： 1295 字节数据身份证信息

- 4 、 读取身份证信息(文字+照片+指纹特征点信息) 
读卡命令： AA AA AA 96 69 00 03 30 10 23
返回 值： 2321 或 1809 或 1297 字节数据身份证信息

###4. 身份证信息结构
- 身份证信息(文字+照片)结构：
AA AA AA 96 69 05 08 00 00 90 01 00 04 00 +（ 256 字节文字信息 ） +（ 1024 字节
照片信息） +（ 1 字节 CRC）

- 身份证信息(文字+照片+指纹)结构：
AA AA AA 96 69 09 0A 00 00 90 01 00 04 00 04 00 +（ 256 字节文字信息） +
（ 1024 字节图片信息） +（ 1024 或 512 或 0 字节指纹信息） +1 字节校验位 指
纹数据的具体大小由第十五和第十六字节判断 (04 00)=4\*16\*16=1024
(02 00)=2\*16\*16=512

###5.文字结构说明
文字信息采用 GB 13000 的 UCS-2 进行存储， 各项目分配如下：

| 项目        | 长度（字节）           | 说明  |
| ------------- |:-------------:| -----:|
| 姓名           | 30            | 汉字     |
| 性别           | 2             | 代码     |
| 民族           | 4             | 代码     |
| 出生           | 16            | 年月日： YYYYMMDD     |
| 住址           | 70            | 汉字和数字     |
| 公民身份号码    | 36            | 数字和字母X(x)     |
| 签发机关        | 30            | 汉字     |
| 有效期起始日期  | 16            | 年月日： YYYYMMDD     |
| 有效期截止日期  | 16            | 年月日： YYYYMMDD 有效期为长期时存储  “长期”     |
| 备用            | 36            | 汉字     |


###6.民族代码对照表

|编号|名族|编号|名族|编号|名族|编号|名族
| ------------- |:-------------:| -----:|
|01|汉|15|土家|29|柯尔克孜|43|乌孜别克|
|02|蒙古|16|哈尼|30|土|44|俄罗斯|
|03|回|17|哈萨克|31|达斡尔|45|鄂温克|
|04|藏|18|傣|32|仫佬|46|德昂|
|05|维吾尔|19|黎|33|羌|47|保安|
|06|苗|20|傈僳|34|布朗|48|裕固|
|07|彝|21|佤|35|撒拉|49|京|
|08|壮|22|畲|36|毛南|50|塔塔尔|
|09|布依|23|高山|37|仡佬|51|独龙|
|10|朝鲜|24|拉祜|38|锡伯|52|鄂伦春|
|11|满|25|水|39|阿昌|53|赫哲|
|12|侗|26|东乡|40|普米|54|门巴|
|13|瑶|27|纳西|41|塔吉克|55|珞巴|
|14|白|28|景颇|42|怒|56|基诺|
|97|其他|98|外国血统中国籍人士|

###7.性别代码对照表        
  
| 编号                | 说明  |
| ------------- |:-------------:|
| 0            | 未知           |
| 1            | 男          |
| 2            | 女          |
| 9            | 未说明           |


---
##二、身份证的读取
首先需要知道身份证的波特率和硬件连接地址，身份证的波特率默认是**115200**
###1.读取的方法调用
```java
    /**
     * 读取 身份证信息
     *
     * @param view
     */
    public void readIdCard(View view) {
        //1.硬件地址判断
        String adress = addressTv.getText().toString().trim();
        if ("".equals(adress)) {
            Toast.makeText(this, "请选择硬件地址", Toast.LENGTH_SHORT).show();
            return;
        }
        //2.波特率判断
        String bauteStr = bauteRateTv.getText().toString().trim();
        if ("".equals(bauteStr)) {
            Toast.makeText(this, "请选择波特率", Toast.LENGTH_SHORT).show();
            return;
        }
        new IDCardReadUtils(this).queryIdCardInfo(adress, Integer.parseInt(bauteStr), new IDCardReadUtils.IDCardListener() {
            @Override
            public void onInfo(IdCardBean bean) {
                //输出身份证信息
                infoTv.setText(bean.word.toMyString());
                //头像的处理，先不处理
//                headIv.setImageBitmap(bytes2Bimap(bean.headImage));
            }
        });
    }
```

###2.身份证的工具类（IDCardReadUtils）
这个类主要处理
1.读取身份证信息
 ①寻找身份证信息
 ②选取身份证信息
 ③读取身份证信息
2.把读取到信息进行处理，封装bean

```java
package com.qwm.idcarddemo.utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.Log;
import android.widget.Toast;


import com.qwm.idcarddemo.bean.IdCardBean;
import com.qwm.idcarddemo.bean.SerialPortSendData;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;

/**
 * @author qiwenming
 * @date 2016/1/19 0019 下午 6:02
 * @ClassName: IDCardReadUtils
 * @ProjectName: 
 * @PackageName: com.qwm.idcarddemo.utils
 * @Description:  读取的工具类
 *
 */
public class IDCardReadUtils {
    private final Context context;

    private IDCardListener listener;

    public IDCardReadUtils(Context context){
        this.context = context;

    }


    private IDCardDevicesUtils device;
    private boolean isIdCardOk;



    /**
     * 获取错误的字符
     * @return
     */
    private String getFialStr(){
        StringBuilder sb = new StringBuilder();
        sb.append("(");
        sb.append("000010|");
        sb.append("000011|");
        sb.append("000010|");
        sb.append("000021|");
        sb.append("000023|");
        sb.append("000024|");
//        sb.append("31|");
//        sb.append("32|");
        sb.append("000033|");
        sb.append("40|");
        sb.append("41|");
        sb.append("47|");
        sb.append("000060|");
        sb.append("000066|");
        sb.append("000080|");
        sb.append("000081|");
        sb.append("000091");
        sb.append(")");
        return sb.toString();
    }

    private int ii=0;
    public String getSSSS(){
        String ss = Integer.toHexString(ii);
        ii++;
        if(ss.length()<2)
            ss = "0"+ss;
        return ss;
    }


    /**
     * 第一步：寻找身份证信息
     * @param devicessAddress  设备地址
     * @param bauteRate  波特率
     * @param listener  回调
     * 获取身份证信息
     * 分为三步走：
     * 1.寻找身份证信息
     *   AA AA AA 96 69 00 03 20 01 22
     *   返回 AA AA AA 96 69 00 08 00 00 9F 00 00 00 00 97
     * 2.选取身份证信息
     *   AA AA AA 96 69 00 03 20 02 21
     *   返回AA AA AA 96 69 00 0C 00 00 90 00 00 00 00 00 00 00 00 9C
     * 3.获取
     AA AA AA 96 69 00 03 30 01 32
     返回1295  字节数据身份证信息
     */
    public void queryIdCardInfo(String devicessAddress,int bauteRate,IDCardListener listener) {
        this.listener = listener;
        Log.i("queryIdCardInfo", "queryIdCardInfo--------");
        device = new IDCardDevicesUtils();
        SerialPortSendData sendData = new SerialPortSendData("/dev/ttyS3",115200,  "AAAAAA96690003200122",
                "9f", getFialStr(),"97", true);
        device.toSend(context, sendData, new IDCardDevicesUtils.ReciverListener() {
            @Override
            public void onReceived(String receviceStr) {
                choiceIdCardInfo();
            }

            @Override
            public void onFail(String fialStr) {
                //mHandler.sendEmptyMessageDelayed(GETIDCARD, 1000);
                try{
                    Toast.makeText(context, "请放入或 重放身份证", Toast.LENGTH_SHORT).show();
                }catch(Exception e){
                    e.printStackTrace();

                }

            }
            @Override
            public void onErr(Exception e) {
                try{
//                    MyToast.shortShow(getActivity(), "请放入或 重放身份证");
                }catch(Exception e2){
                    e2.printStackTrace();
                }
                //    mHandler.sendEmptyMessageDelayed(GETIDCARD, 1000);
            }

//            @Override
//            public void onFinished(Exception e) {
//
//            }
        });
    }

    /**
     * 第二步：选取身份证
     */
    public void choiceIdCardInfo(){
        device = new IDCardDevicesUtils();
        Log.i("choiceIdCardInfo", "choiceIdCardInfo--------");
        SerialPortSendData sendData = new SerialPortSendData("/dev/ttyS3",115200,  "AAAAAA96690003200221","90", getFialStr(),"9c", true);
        device.toSend(context, sendData, new IDCardDevicesUtils.ReciverListener() {
            @Override
            public void onReceived(String receviceStr) {
                getIdCardInfo();
            }

            @Override
            public void onFail(String fialStr) {
                //mHandler.sendEmptyMessageDelayed(GETIDCARD, 1000);
//                MyToast.shortShow(getActivity(), "选取失败");
            }

            @Override
            public void onErr(Exception e) {
//                MyToast.shortShow(getActivity(), "选取失败");
                //    mHandler.sendEmptyMessageDelayed(GETIDCARD, 1000);
            }

//            @Override
//            public void onFinished(Exception e) {
//
//            }
        });
    }



    /**
     * 第三步：获取身份证信息
     */
    public void getIdCardInfo(){
        Log.i("getIdCardInfo", "getIdCardInfo--------");
        device = new IDCardDevicesUtils();
        SerialPortSendData sendData = new SerialPortSendData("/dev/ttyS3",115200,"AAAAAA96690003300132",1295);
        device.toSend(context, sendData, new IDCardDevicesUtils.ReciverListener() {
            @Override
            public void onReceived(String receviceStr) {
                IdCardBean bean = getIdCardDataBean(receviceStr);
                listener.onInfo(bean);
            }

            @Override
            public void onFail(String fialStr) {
//                MyToast.shortShow(getActivity(), "身份证读取失败");
            }

            @Override
            public void onErr(Exception e) {
//                MyToast.shortShow(getActivity(), "身份证读取失败");
            }

//            @Override
//            public void onFinished(Exception e) {
//
//            }
        });
    }

    /**
     * 身份证信息处理
     * 身份证信息(文字+照片)结构：
     * AA AA AA 96 69 05 08 00 00 90 01 00 04 00 +（256  字节文字信息 ）+（1024 字节  照片信息）+（1  字节 CRC）
     * @param dataStr
     */
    @SuppressLint("NewApi")
    public IdCardBean getIdCardDataBean(String dataStr){
        Log.i("idcard_str",dataStr);
        IdCardBean idCard = new IdCardBean();
        byte[] data = StringUtils.hexStringToBytes(dataStr);
        Log.i("--------------dataStr-------------",dataStr.length()+"");
        if(data.length>=1295){
            //1.文字信息处理
            byte[] idWordbytes = Arrays.copyOfRange(data, 14, 270);
            //2.头像处理
           String headStr = dataStr.substring(540,2588);
            idCard.headImage = hex2byte(headStr);//Arrays.copyOfRange(data,270, 1294);
            try {
                idCard.word.name = new String(Arrays.copyOfRange(idWordbytes,0, 30),"UTF-16LE").trim().trim();
                idCard.word.gender = new String(Arrays.copyOfRange(idWordbytes,30, 32),"UTF-16LE").trim();
                idCard.word.nation = new String(Arrays.copyOfRange(idWordbytes,32, 36),"UTF-16LE").trim();
                idCard.word.birthday = new String(Arrays.copyOfRange(idWordbytes,36, 52),"UTF-16LE").trim();
                idCard.word.address = new String(Arrays.copyOfRange(idWordbytes,52, 122),"UTF-16LE").trim();
                idCard.word.idCard = new String(Arrays.copyOfRange(idWordbytes,122, 158),"UTF-16LE").trim();
                idCard.word.issuingAuthority = new String(Arrays.copyOfRange(idWordbytes,158, 188),"UTF-16LE").trim();
                idCard.word.startTime = new String(Arrays.copyOfRange(idWordbytes,188, 204),"UTF-16LE").trim();
                idCard.word.startopTime = new String(Arrays.copyOfRange(idWordbytes,204, 220),"UTF-16LE").trim();
                //名族的特殊处理
                idCard.word.nation = NationUtils.getNationNameById(idCard.word.nation);

            } catch (UnsupportedEncodingException e) {
            }
        }
//        if(data.length>=1294){
//            //照片信息处理
//            idCard.headImage = Arrays.copyOfRange(data,270, 1295);
//        }
        return idCard;
    }

    /**
     *    项目  长度（字节）  说明
     姓名  30              汉字
     性别  2              代码
     民族  4              代码
     出生  16             年月日：YYYYMMDD
     住址  70              汉字和数字
     公民身份号码 36  数字
     签发机关 30  汉字
     有效期起始日期 16 年月日：YYYYMMDD
     有效期截止日期 16 年月日：YYYYMMDD 有效期为长期时存储 “长期”
     备用 36
     */

    /**
     * 读取成功以后，身份证信息的回调接口
     */
    public interface IDCardListener{
        void onInfo(IdCardBean bean);
    }


    public static byte[] hex2byte(String hex) {
        int len = (hex.length() / 2);
        byte[] result = new byte[len];
        char[] achar = hex.toCharArray();
        for (int i = 0; i < len; i++) {
            int pos = i * 2;
            result[i] = (byte) (toByte(achar[pos]) << 4 | toByte(achar[pos + 1]));
        }
        Log.i("===========result--length===========",result.length+"");
        return result;
    }

    private static byte toByte(char c) {
        byte b = (byte) "0123456789ABCDEF".indexOf(c);
        return b;
    }

//    public static byte[] hex2byte(String str) { // 字符串转二进制
//        if (str == null)
//            return null;
//        str = str.trim();
//        int len = str.length();
//        if (len == 0 || len % 2 == 1)
//            return null;
//        byte[] b = new byte[len / 2];
//        try {
//            for (int i = 0; i < str.length(); i += 2) {
//                b[i / 2] = (byte) Integer.decode("0X" + str.substring(i, i + 2)).intValue();
//            }
//            return b;
//        } catch (Exception e) {
//            return null;
//        }
//    }
}
```

##三、硬件读取类（IDCardDevicesUtils）
```java
package com.qwm.idcarddemo.utils;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import com.qwm.idcarddemo.MainActivity;
import com.qwm.idcarddemo.bean.SerialPortSendData;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.InvalidParameterException;

import android_serialport_api.SerialPort;
import android_serialport_api.SerialPortFinder;

/**
 * @author qiwenming
 * @date 2016/1/19 0019 下午 5:27
 * @ClassName: IDCardDevicesUtils
 * @ProjectName: 
 * @PackageName: com.qwm.idcarddemo.utils
 * @Description:  身份证识别 硬件调用
 */
public class IDCardDevicesUtils {
    protected OutputStream mOutputStream;
    private InputStream mInputStream;
    private ReadThread mReadThread;
    public SerialPortFinder mSerialPortFinder = new SerialPortFinder();
    private SerialPort mSerialPort;
    private Context context;

    /**
     * @author qiwenming
     * @creation 2015-6-18 下午4:38:54
     * @instruction 串口读取类
     */
    private class ReadThread extends Thread {
        private ReciverListener listener;
        private SerialPortSendData sendData;
        public boolean isReadData = false;
        public boolean isOK = true;

        public ReadThread(SerialPortSendData sendData, ReciverListener listener) {
            this.listener = listener;
            this.sendData = sendData;
        }

        @Override
        public void run() {
            StringBuffer sb = new StringBuffer();
            StringBuffer sb2 = new StringBuffer();
            super.run();
            while (!isInterrupted()) {
                int size;
                try {
                    byte[] buffer = new byte[1024];
                    if (mInputStream == null)
                        return;
                    size = mInputStream.read(buffer);
                    Log.i("--------------", "---------------mInputStream---------------" + mInputStream.available());
                    if (size > 0) { // 读取数据 数据c
                        String str = StringUtils.bytesToHexString(buffer, size).trim().toLowerCase();
                        sb2.append(str);
                        if (sendData.isOnlyLenght) {//这里我们只按照长度读取
                            sb.append(str);

                            if(sb2.toString().matches("\\w+"+sendData.failStr+"\\w+")){
                                closeDevice();
                                if (null == context)
                                    return;
                                ((MainActivity) context)
                                        .runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                listener.onFail(sendData.failStr);
                                            }
                                        });
                            } else if(sb.toString().length()>=(2*sendData.readByteCount)){
                                final String data = sb.toString();
                                closeDevice();
                                if (null == context)
                                    return;
                                ((MainActivity) context)
                                        .runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                listener.onReceived(data);
                                            }
                                        });
                            }
                        } else {//下面的处理是不安长度读取的
                            Log.i("onDataReceived", str);
                                if (sb2.toString().contains(sendData.stopStr)) {
                                    // 根据结束标志获取字符消息
                                    String[] strs = str.split(sendData.stopStr);
                                    if (strs.length > 1)
                                        for (int i = 0; i < strs.length - 1; i++)
                                            sb.append(strs[i]);

                                    final String data = sb.toString();
                                    sb = new StringBuffer();
                                    Log.i("onDataReceived_stop", data);
                                    Log.i("onDataReceived_stop_ascii",  StringUtils.convertHexToString(data));
                                    isReadData = false;
                                    closeDevice();
                                    if (null == context)
                                        return;
                                    ((MainActivity) context)
                                            .runOnUiThread(new Runnable() {
                                                @Override
                                                public void run() {
                                                    if (isOK)
                                                        listener.onReceived(data);
                                                    else
                                                        listener.onFail(sendData.failStr);
                                                }
                                            });
                                }
                                if (isReadData) {
                                    sb.append(str);
                                }
                                if (sb2.toString().contains(sendData.okStr)) {
                                    isReadData = true;
                                    isOK = true;
                                    String[] datas = str.split(sendData.okStr);
                                    for (int i = 1; i < datas.length; i++) {
                                        sb.append(str);
                                    }
                                }
                                if (sb2.toString().matches("\\w+"+sendData.failStr+"\\w+")) {
//                                    sb = new StringBuffer();
                                    isReadData = false;
                                    isOK = false;
                                    closeDevice();
                                    if (null == context)
                                        return;
                                    ((MainActivity) context)
                                            .runOnUiThread(new Runnable() {
                                                @Override
                                                public void run() {
                                                    listener.onFail(sendData.failStr);
                                                }
                                            });
                                }

                        }

                    }
                } catch (Exception e) {
                    listener.onErr(e);
                    return;
                }
            }
        }
    }

    /**
     * 发送数据
     *
     * @param context
     * @param sendData
     * @param listener
     */
    public void toSend(Context context, SerialPortSendData sendData,
                       ReciverListener listener) {
        this.context = context;
        if ("".equals(sendData.path) || "/dev/tty".equals(sendData.path)) {
            Toast.makeText(context, "设备地址不能为空", 0).show();
            return;
            // devStr = "/dev/ttyS1";
        }
        if ("".equals(sendData.commandStr)) {
            Toast.makeText(context, "指令不能为空", 0).show();
            return;
        }
        try {
            mSerialPort = getSerialPort(sendData.path, sendData.baudRate);
            mOutputStream = mSerialPort.getOutputStream();
            mInputStream = mSerialPort.getInputStream();

            /* Create a receiving thread */
            mReadThread = new ReadThread(sendData, listener);
            mReadThread.start();
        } catch (SecurityException e) {
            // DisplayError(R.string.error_security);
        } catch (IOException e) {
            // DisplayError(R.string.error_unknown);
        } catch (InvalidParameterException e) {
            // DisplayError(R.string.error_configuration);
        }

        // 上面是获取设置而已 下面这个才是发送指令
        byte[] text = StringUtils.hexStringToBytes(sendData.commandStr);
        try {
            mOutputStream.write(text);
            //mOutputStream.write('\n');
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 获取到串口通信的一个示例
     *
     * @param path
     * @param baudrate
     * @return
     * @throws SecurityException
     * @throws IOException
     * @throws InvalidParameterException
     */
    public SerialPort getSerialPort(String path, int baudrate)
            throws SecurityException, IOException, InvalidParameterException {
        // if (mSerialPort == null) {
        /* Check parameters */
        if ((path.length() == 0) || (baudrate == -1)) {
            throw new InvalidParameterException();
        }
        /* Open the serial port */
        mSerialPort = new SerialPort(new File(path), baudrate, 0);// 打开这个串口
        // }

        return mSerialPort;
    }

    public void closeDevice() {
        if (mReadThread != null)
            mReadThread.interrupt();
        // mApplication.closeSerialPort();
        closeSerialPort();
        // mSerialPort = null;
    }

    public void closeSerialPort() {// 关闭窗口
        if (mSerialPort != null) {
            mSerialPort.close();
            mSerialPort = null;
        }
    }

    /**
     * @author qiwenming
     * @creation 2015-7-20 上午10:16:54
     * @instruction 接受回调类
     */
    public interface ReciverListener {

        /**
         * 接受以后的处理方法
         *
         * @param string
         * @param headBytes
         */
        public abstract void onReceived(String receviceStr);

        /**
         * 出错
         *
         * @param string
         */
        public abstract void onFail(String fialStr);

        /**
         * 出现异常
         *
         * @param e
         */
        public abstract void onErr(Exception e);

    }

    /**
     * @author qiwenming
     * @creation 2015-7-20 下午2:34:28
     * @instruction 这个是我们用于存储读取的数据
     */
    public class RecevedData {
        public ReturnType returnType;
        /**
         * 数据
         */
        public String receviedData;
    }

    /**
     * @author qiwenming
     * @creation 2015-7-20 下午2:36:21
     * @instruction 使用辨识返回的数据的
     */
    public enum ReturnType {
        ERR, // 错误
        OK, // OK
        Exception
    }
}

```

##四、图示
![默认](http://img.blog.csdn.net/20160219162607901)
![这里写图片描述](http://img.blog.csdn.net/20160219162633423)

##五、源码下载
 https://github.com/qiwenming/IDCardDemo  
