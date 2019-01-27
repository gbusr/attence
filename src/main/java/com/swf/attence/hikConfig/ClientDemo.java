package com.swf.attence.hikConfig;

import com.sun.jna.NativeLong;
import com.sun.jna.Pointer;
import org.dom4j.DocumentException;

import java.io.UnsupportedEncodingException;

public class ClientDemo {
    static HCNetSDK hCNetSDK = HCNetSDK.INSTANCE;
    HCNetSDK.NET_DVR_DEVICEINFO_V30 m_strDeviceInfo;//设备信息
    public NativeLong lUserID;//用户句柄
    public NativeLong lAlarmHandle;//报警布防句柄
    public NativeLong lListenHandle;//报警监听句柄
    public FMSGCallBack fMSFCallBack;//报警回调函数实现
    public FMSGCallBack_V31 fMSFCallBack_V31;//报警回调函数实现
    public String m_sDeviceIP;//已登录设备的IP地址
    public String username; //设备用户名
    public String password;//设备登录密码
    public int iPort;//设备端口号
    public static String userpicPath="F:\\Attence相关\\userpic\\";
    public static String userdataPath="F:\\Attence相关\\userdata\\";


    public ClientDemo() {
        lUserID = new NativeLong(-1);
        lAlarmHandle = new NativeLong(-1);
        lListenHandle = new NativeLong(-1);
        fMSFCallBack = null;
    }
    public String CameraInit(){
        //初始化
        boolean initSuc = hCNetSDK.NET_DVR_Init();
        if (initSuc != true){
            System.out.println("初始化失败");
            return "初始化失败";
        }else{
            System.out.println("初始化成功");
            return "初始化成功";
        }
    }
    //注册
    public boolean register(String username, String password, String m_sDeviceIP){
        //注册之前先注销已注册的用户,预览情况下不可注销
        if (lUserID.longValue() > -1) {
            //先注销
            hCNetSDK.NET_DVR_Logout(lUserID);
            lUserID = new NativeLong(-1);
        }

        //注册
        m_strDeviceInfo = new HCNetSDK.NET_DVR_DEVICEINFO_V30();
        int iPort = 8000;
        System.out.println("注册，设备IP："+m_sDeviceIP);
        lUserID = hCNetSDK.NET_DVR_Login_V30(m_sDeviceIP,(short) iPort, username, password, m_strDeviceInfo);

        long userID = lUserID.longValue();
        if (userID == -1) {
            System.out.println("注册失败");
            return false;
        } else {
            System.out.println("注册成功,lUserID:"+userID);
            return true;
        }

    }
    //注销
    public void Logout(){
        //报警撤防
        if (lAlarmHandle.intValue() > -1)
        {
            if(!hCNetSDK.NET_DVR_CloseAlarmChan_V30(lAlarmHandle))
            {
                System.out.println("撤防失败");
            }else{
                lAlarmHandle = new NativeLong(-1);
                System.out.println("撤防成功");
            }
        }

        //注销
        if (lUserID.longValue() > -1) {
            if(hCNetSDK.NET_DVR_Logout(lUserID))
            {
                System.out.println("注销成功");
                lUserID = new NativeLong(-1);
            }
        }
    }

    //布防
    public String SetupAlarmChan() {
        if (lUserID.intValue() == -1)
        {
            System.out.println("请先注册");
            return "请先注册";
        }
        if (lAlarmHandle.intValue() < 0)//尚未布防,需要布防
        {
            if (fMSFCallBack_V31 == null)
            {
                fMSFCallBack_V31 = new FMSGCallBack_V31();
                Pointer pUser = null;
                if (!hCNetSDK.NET_DVR_SetDVRMessageCallBack_V31(fMSFCallBack_V31, pUser))
                {
                    System.out.println("设置回调函数失败!");
                    return "设置回调函数失败!";
                }
            }
            HCNetSDK.NET_DVR_SETUPALARM_PARAM m_strAlarmInfo = new HCNetSDK.NET_DVR_SETUPALARM_PARAM();
            m_strAlarmInfo.dwSize=m_strAlarmInfo.size();
            m_strAlarmInfo.byLevel=1;
            m_strAlarmInfo.byAlarmInfoType=1;
            m_strAlarmInfo.write();
            lAlarmHandle = hCNetSDK.NET_DVR_SetupAlarmChan_V41(lUserID, m_strAlarmInfo);

            if (lAlarmHandle.intValue() == -1)
            {
                System.out.println("布防失败");
                System.out.println("错误代码："+hCNetSDK.NET_DVR_GetLastError());
                return "布防失败: "+"错误代码："+hCNetSDK.NET_DVR_GetLastError();

            }else{
                System.out.println("布防成功");
                return "布防成功";
            }
        }else{
            System.out.println("已经布防，不要重复操作");
            return "已经布防，不要重复操作";
        }
    }
    //撤防
    public void CloseAlarmChan() {
        //报警撤防
        if (lAlarmHandle.intValue() > -1)
        {
            if(hCNetSDK.NET_DVR_CloseAlarmChan_V30(lAlarmHandle))
            {
                System.out.println("撤防成功");
                lAlarmHandle = new NativeLong(-1);
            }
        }
    }

    //开始监听
    public void StartAlarmListen() {
        int iListenPort = 8000;
        Pointer pUser = null;

        if (fMSFCallBack == null)
        {
            fMSFCallBack = new FMSGCallBack();
        }
        lListenHandle = hCNetSDK.NET_DVR_StartListen_V30(m_sDeviceIP, (short)iListenPort,fMSFCallBack, pUser);
        if(lListenHandle.intValue() < 0)
        {
            System.out.println("启动监听失败");
        }else{
            System.out.println("启动监听成功");
        }
    }

    //停止监听
    public void StopAlarmListen() {
        if(lListenHandle.intValue() < 0)
        {
            return;
        }

        if(!hCNetSDK.NET_DVR_StopListen_V30(lListenHandle))
        {
            System.out.println("停止监听失败");
            System.out.println("停止监听失败，错误："+hCNetSDK.NET_DVR_GetLastError());
        }else{

            System.out.println("停止监听成功");
        }
    }

    public static void main(String[] args) throws UnsupportedEncodingException, DocumentException, InterruptedException {
        ClientDemo clientDemo = new ClientDemo();
        clientDemo.CameraInit();

        clientDemo.register("admin","admin123456","192.168.0.101");

        FDLibBox fdLibBox = new FDLibBox(clientDemo);

        /*String faceName="测试人脸库";
        fdLibBox.CreateFDLib(faceName);*/
        /*boolean b = fdLibBox.GetFaceCapabilities();
        System.out.println(b);*/
       /* fdLibBox.UploadFaceLinData(1);*/
        clientDemo.SetupAlarmChan();
        Thread.sleep(100000);

        /**
         * 调用SearchFDLib方法
         */
       /* HashMap<Object, Object> stringObjectHashMap = new HashMap<>();
        if(fdLibBox.SearchFDLib()){*/
           /* for (int i=0;i<fdLibBox.m_FDLibList.size();i++){
                stringObjectHashMap.put("id"+i,fdLibBox.m_FDLibList.get(i).dwID);
                stringObjectHashMap.put("name"+i,fdLibBox.m_FDLibList.get(i).szFDName);
                stringObjectHashMap.put("ffid"+i,fdLibBox.m_FDLibList.get(i).szFDID);
            }
            System.out.println(stringObjectHashMap);
             fdLibBox.DeleteFDLib(2);*/
         /*  String realUserpicPath=userpicPath+"1101.png";
           String realUserdataPaht=userdataPath+"1101.xml";
           fdLibBox.UploadFaceLinData(1,realUserpicPath,realUserdataPaht);*/
         /*fdLibBox.DeleteFaceAppendData(1,"2");*/

        }



    }


