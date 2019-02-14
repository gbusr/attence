package com.swf.attence.service.impl;

import com.baomidou.mybatisplus.mapper.EntityWrapper;

import com.swf.attence.entity.AttenceMsg;
import com.swf.attence.entity.CameraMsg;
import com.swf.attence.entity.ICommand;
import com.swf.attence.entity.TimeControl;
import com.swf.attence.mapper.ICommandMapper;
import com.swf.attence.service.IAttenceMsgService;
import com.swf.attence.service.ICameraMsgService;
import com.swf.attence.service.IEveryTaskService;
import com.swf.attence.service.ITimeControlService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Date;

/**
 * 每日建表服务类 建立的表名规则：icommandYYYYMMDD
 */
@Service
public class EveryTaskServiceImpl implements IEveryTaskService {
    @Autowired
    private ITimeControlService iTimeControlService;
    @Autowired
    private ICameraMsgService iCameraMsgService;
    @Autowired
    private IAttenceMsgService iAttenceMsgService;
    @Autowired
    private ICommandMapper iCommandMapper;

    @Value(value = "${spring.datasource.driver-class-name}")
    private String driver;
    @Value(value = "${spring.datasource.url}")
    private String url;
    @Value(value = "${spring.datasource.username}")
    private String username;
    @Value(value = "${spring.datasource.password}")
    private String password;
    /**
     * 表明前缀prefix
     */
    public String prefix = "icommand";

    @Override
    public Boolean creatEveryDayTable(String day) {
        Boolean todayTableExist = false;
        try {
            System.out.println(driver + url + username + password);
            Class.forName(driver);
            Connection connection = DriverManager.getConnection(url, username, password);
            Statement statement = connection.createStatement();
            String todayTable = prefix + day;
            ResultSet tables = connection.getMetaData().getTables(null, null, todayTable, null);
            if (tables.next()) {
                return todayTableExist;
            } else {
                statement.executeUpdate("DROP TABLE IF EXISTS " + todayTable + ";");
                statement.executeUpdate("    CREATE TABLE `attence`.`" + todayTable + "`  (\n" +
                        "  `id` varchar(255) NOT NULL ,\n" +
                        "  `icommand_time` varchar(255) NOT NULL COMMENT '报警时间',\n" +
                        "  `icommand_cameraid` varchar(255) NOT NULL COMMENT '报警来源：摄像头ip',\n" +
                        "  `icommand_userid` varchar(255) NOT NULL COMMENT '用户工号 ',\n" +
                        "  `icommand_username` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '用户姓名',\n" +
                        "  PRIMARY KEY (`id`)\n" +
                        ") ENGINE = InnoDB CHARACTER SET = utf8 COLLATE = utf8_general_ci;");
                statement.close();
                connection.close();
                todayTableExist = true;
                return todayTableExist;
            }
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return todayTableExist;
    }

    @Override
    public Boolean insertEveryICommandIntoDateBase(ICommand iCommand) throws ClassNotFoundException, SQLException {
        Boolean insertEveryICommandIntoDateBase = false;
        Class.forName(driver);
        Connection connection = DriverManager.getConnection(url, username, password);
        Statement statement = connection.createStatement();
        /**
         * 获取当前表名
         */
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
        String day = dateFormat.format(new Date());
        String todayTable = prefix + day;
        ResultSet tables = connection.getMetaData().getTables(null, null, todayTable, null);
        if (!tables.next()) {
            statement.executeUpdate("DROP TABLE IF EXISTS " + todayTable + ";");
            statement.executeUpdate("    CREATE TABLE `attence`.`" + todayTable + "`  (\n" +
                    "  `id` varchar(255) NOT NULL ,\n" +
                    "  `icommand_time` varchar(255) NOT NULL COMMENT '报警时间',\n" +
                    "  `icommand_cameraid` varchar(255) NOT NULL COMMENT '报警来源：摄像头ip',\n" +
                    "  `icommand_userid` varchar(255) NOT NULL COMMENT '用户工号 ',\n" +
                    "  `icommand_username` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '用户姓名',\n" +
                    "  PRIMARY KEY (`id`)\n" +
                    ") ENGINE = InnoDB CHARACTER SET = utf8 COLLATE = utf8_general_ci;");
        }
        String sql = "INSERT INTO " + todayTable + "(" +
                "`id`, `icommand_time`, `icommand_cameraid`, `icommand_userid`, `icommand_username`) VALUES (?,?,?,?,?);";
        PreparedStatement preparedStatement = connection.prepareStatement(sql);
        preparedStatement.setString(1, iCommand.getId());
        preparedStatement.setString(2, iCommand.getIcommandTime());
        preparedStatement.setString(3, iCommand.getIcommandCameraid());
        preparedStatement.setString(4, iCommand.getIcommandUserid());
        preparedStatement.setString(5, iCommand.getIcommandUsername());
        preparedStatement.executeUpdate();
        System.out.println("插入成功");


        statement.close();
        connection.close();
        insertEveryICommandIntoDateBase = true;
        return insertEveryICommandIntoDateBase;
    }
    @Override
    public Boolean insertIntoDatabase(String todayTable, ICommand iCommand) {
        iCommandMapper.insertIntoDatabase(todayTable,iCommand);
        return true;
    }

    @Override
    public void everyDataAnalsis(String day, ArrayList<ICommand> inCommandList, ArrayList<ICommand> outCommandList) {
        /**
         * 构造两个新list用来存放  除去那些符合条件的 对象数据
         */
        ArrayList<ICommand> inICommands = new ArrayList<>(inCommandList);
        ArrayList<ICommand> outICommands = new ArrayList<>(outCommandList);
        /**
         * 查询进/出时间
         */
        List<TimeControl> timeControls = iTimeControlService.selectList(new EntityWrapper<TimeControl>().eq("things_name", "考勤"));
        System.out.println(timeControls);
        String inFormat = null;
        String outFormat = null;
        for (TimeControl t : timeControls
                ) {
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("HH:mm:ss");
            /**
             * 当天应该'进 出'的时间
             */
            inFormat = day + simpleDateFormat.format(t.getStartTime());
            outFormat = day + simpleDateFormat.format(t.getEndTime());
        }
        /**
         * 比'进'规定时间小的所有List
         */
        ArrayList<ICommand> newInCommandList1 = new ArrayList<>(inCommandList);
        /**
         * 比'进'规定时间大的所有List
         */
        ArrayList<ICommand> newInCommandList2 = new ArrayList<>();
        /**
         * 如果时间大于规定时间 则从“比进规定时间小的集合中”移除该对象，这部分对象存入2
         */
        for (ICommand ic : inCommandList
                ) {
            String replace = ic.getIcommandTime().replace("-", "").replace(" ", "");
            int i = replace.compareTo(inFormat);
            if (i>=0){
                newInCommandList1.remove(ic);
                newInCommandList2.add(ic);
            }
        }
        /**
         * 比'出'规定时间小的所有List
         */
        ArrayList<ICommand> newOutCommandList1 = new ArrayList<>(outCommandList);
        /**
         * 比'出'规定时间大的所有List
         */
        ArrayList<ICommand> newOutCommandList2 = new ArrayList<>();

        for (ICommand ic : outCommandList
                ) {
            String outReplace = ic.getIcommandTime().replace("-", "").replace(" ", "");
            int compare = outReplace.compareTo(outFormat);
            if (compare <= 0) {
                newOutCommandList1.remove(ic);
                newOutCommandList2.add(ic);
            }
        }
        /**
         * 符合规定的进出list存入attence_msg 状态为1 即为考勤成功
         * newInList1 小于等于 进
         * newInList2  大于 进
         * newOutList1 大于 出
         * newOutList2 小于 出
         *
         * 这一部分处理的是  规定时间  出
         *
         */
        for (int a = 0; a < newOutCommandList1.size(); a++) {
            ICommand outICommand = newOutCommandList1.get(a);
            /**
             * 这个for处理的是 进出均在规定时间的正常考勤数据
             */
            for (int b = 0; b < newInCommandList1.size(); b++) {
                ICommand inICommand = newInCommandList1.get(b);
                if (inICommand.getIcommandUserid().equals(outICommand.getIcommandUserid())) {
                    AttenceMsg completeAttenceMsg = new AttenceMsg();
                    completeAttenceMsg.setUserid(inICommand.getIcommandUserid());
                    completeAttenceMsg.setCheckInTime(inICommand.getIcommandTime());
                    completeAttenceMsg.setCameraidIn(inICommand.getIcommandCameraid());
                    completeAttenceMsg.setCameraidOut(outICommand.getIcommandCameraid());
                    completeAttenceMsg.setCheckOutTime(outICommand.getIcommandTime());
                    completeAttenceMsg.setCheckState(1);
                    iAttenceMsgService.insert(completeAttenceMsg);
                    System.out.println("工号: " + inICommand.getIcommandUserid() + "考勤成功");
                }
            }
            /**
             * 这个for处理的是 迟到  但是按照规定时间下班的人
             */
            for (int c=0;c<newInCommandList2.size();c++){
                ICommand inICommand = newInCommandList1.get(c);
                if (inICommand.getIcommandUserid().equals(outICommand.getIcommandUserid())) {
                    AttenceMsg completeAttenceMsg = new AttenceMsg();
                    completeAttenceMsg.setUserid(inICommand.getIcommandUserid());
                    completeAttenceMsg.setCheckInTime(inICommand.getIcommandTime());
                    completeAttenceMsg.setCameraidIn(inICommand.getIcommandCameraid());
                    completeAttenceMsg.setCameraidOut(outICommand.getIcommandCameraid());
                    completeAttenceMsg.setCheckOutTime(outICommand.getIcommandTime());
                    completeAttenceMsg.setCheckState(2);
                    iAttenceMsgService.insert(completeAttenceMsg);
                    System.out.println("工号: " + inICommand.getIcommandUserid() + "迟到");
                }
            }
        }

        /**
         * 这一部分处理的是 早退   也就是规定时间 进入 的员工
         */
        for (int d=0;d<newInCommandList1.size();d++){
            ICommand iCommand = newInCommandList1.get(d);
            for(int e=0;e<newOutCommandList2.size();e++){
                ICommand iCommand1 = newOutCommandList2.get(e);
                if (iCommand1.getIcommandUserid().equals(iCommand.getIcommandUserid())){
                    AttenceMsg completeAttenceMsg = new AttenceMsg();
                    completeAttenceMsg.setUserid(iCommand1.getIcommandUserid());
                    completeAttenceMsg.setCheckInTime(iCommand1.getIcommandTime());
                    completeAttenceMsg.setCameraidIn(iCommand1.getIcommandCameraid());
                    completeAttenceMsg.setCameraidOut(iCommand.getIcommandCameraid());
                    completeAttenceMsg.setCheckOutTime(iCommand.getIcommandTime());
                    completeAttenceMsg.setCheckState(3);
                    iAttenceMsgService.insert(completeAttenceMsg);
                    System.out.println("工号: " + iCommand1.getIcommandUserid() + "早退");
                }
            }
        }

        /**
         * 这一部分处理的是  不在规定时间 进 出 的人
         */

        for (int f=0;f<newOutCommandList2.size();f++){
            ICommand iCommand = newOutCommandList2.get(f);
            for (int g=0;g<newInCommandList2.size();g++){
                ICommand iCommand1 = newInCommandList2.get(g);
                if (iCommand1.getIcommandUserid().equals(iCommand.getIcommandUserid())){
                    AttenceMsg completeAttenceMsg = new AttenceMsg();
                    completeAttenceMsg.setUserid(iCommand1.getIcommandUserid());
                    completeAttenceMsg.setCheckInTime(iCommand1.getIcommandTime());
                    completeAttenceMsg.setCameraidIn(iCommand1.getIcommandCameraid());
                    completeAttenceMsg.setCameraidOut(iCommand.getIcommandCameraid());
                    completeAttenceMsg.setCheckOutTime(iCommand.getIcommandTime());
                    completeAttenceMsg.setCheckState(4);
                    iAttenceMsgService.insert(completeAttenceMsg);
                    System.out.println("工号: " + iCommand1.getIcommandUserid() + "迟到  早退");
                }
            }
        }








        /**
         * List 的并集去掉正常考勤的部分放入attence_msg
         */
     /*       inICommands.retainAll(outICommands);
            inICommands.removeAll(inCommandList);
            outICommands.retainAll(inICommands);
            outICommands.removeAll(outCommandList);
            for (int a = 0; a < outICommands.size(); a++) {
                ICommand outCommand = outICommands.get(a);
                for (int b=0;b<inICommands.size();b++){
                    ICommand inCommand = inICommands.get(b);
                    if (inCommand.getIcommandUserid().equals(outCommand.getIcommandUserid())){
                        AttenceMsg failedAttence = new AttenceMsg();
                        failedAttence.setUserid(inCommand.getIcommandUserid());
                        failedAttence.setCheckInTime(inCommand.getIcommandTime());
                        failedAttence.setCameraidIn(inCommand.getIcommandCameraid());
                        failedAttence.setCheckOutTime(outCommand.getIcommandTime());
                        failedAttence.setCameraidOut(outCommand.getIcommandCameraid());
                        failedAttence.setCheckState(0);
                        System.out.println("工号: " + inCommand.getIcommandUserid() + "考勤失败");
                        return true;
                    }
                }
            }*/

        }



    @Override
    public ArrayList<ICommand> getInICommand(String day) throws ClassNotFoundException, SQLException {

        List<CameraMsg> inCameraMsgs = iCameraMsgService.selectList(new EntityWrapper<CameraMsg>().eq("camera_position", "进"));
        String inCameraid = "";
        for (CameraMsg c : inCameraMsgs
                ) {
            inCameraid = c.getCameraid();
        }
        /**
         * JDBC操作
         */
        Class.forName(driver);
        Connection connection = DriverManager.getConnection(url, username, password);
        Statement statement = connection.createStatement();
        /**
         * 获取数据库表所有数据
         */
        String todayTable = prefix + day;
        ResultSet tables = connection.getMetaData().getTables(null, null, todayTable, null);
        /**
         * "进"口sql
         */
        String inSql = "SELECT id,icommand_time,icommand_cameraid,icommand_username,icommand_userid FROM `icommand20190131` where icommand_cameraid=?";

        PreparedStatement inPreparedStatement = connection.prepareStatement(inSql);
        if (inCameraid != null) {
            inPreparedStatement.setString(1, inCameraid);
        } else {
            System.out.println("没有指定'进'口设备");
        }
        /**
         * '进'口结果集
         */
        ResultSet inResultSet = inPreparedStatement.executeQuery();
        /**
         * '进'口设备捕捉到的所有报警对象
         */
        ArrayList<ICommand> inICommands = new ArrayList<>();
        if (tables.next()) {
            ICommand inICommand = new ICommand();
            inICommand.setIcommandTime(inResultSet.getString(1));
            inICommand.setIcommandUsername(inResultSet.getString(3));
            inICommand.setIcommandUserid(inResultSet.getString(4));
            inICommands.add(inICommand);
        }
        statement.close();
        connection.close();
        /**
         * 去重之后的进数据
         */
        ArrayList<ICommand> inICommandsList = removeDuplicateICommand(inICommands);
        return inICommandsList;
    }

    @Override
    public ArrayList<ICommand> getOutICommand(String day) throws ClassNotFoundException, SQLException {

        List<CameraMsg> outCameraMsgs = iCameraMsgService.selectList(new EntityWrapper<CameraMsg>().eq("camera_position", "出"));

        String outCameraid = "";

        for (CameraMsg c : outCameraMsgs
                ) {
            outCameraid = c.getCameraid();
        }
        /**
         * JDBC操作
         */
        Class.forName(driver);
        Connection connection = DriverManager.getConnection(url, username, password);
        Statement statement = connection.createStatement();
        /**
         * 获取数据库表所有数据
         */
        String todayTable = prefix + day;
        ResultSet tables = connection.getMetaData().getTables(null, null, todayTable, null);

        /**
         * "出"口sql
         */
        String outSql = "SELECT id,icommand_time,icommand_cameraid,icommand_username,icommand_userid FROM `icommand20190131` where icommand_cameraid=?";

        PreparedStatement outPreparedStatement = connection.prepareStatement(outSql);

        if (outCameraid != null) {
            outPreparedStatement.setString(1, outCameraid);
        } else {
            System.out.println("没有指定'出'口设备");
        }
        /**
         * '出'口结果集
         */

        ResultSet outResultSet = outPreparedStatement.executeQuery();

        /**
         * '出'口设备捕捉到的所有报警对象
         */
        ArrayList<ICommand> outICommands = new ArrayList<>();
        if (tables.next()) {
            ICommand outICommand = new ICommand();
            outICommand.setIcommandTime(outResultSet.getString(1));
            outICommand.setIcommandUsername(outResultSet.getString(3));
            outICommand.setIcommandUserid(outResultSet.getString(4));
            outICommands.add(outICommand);
        }
        statement.close();
        connection.close();
        /**
         * 去重之后的进出数据
         */
        ArrayList<ICommand> outICommandsList = removeDuplicateICommand(outICommands);

        return outICommandsList;
    }

    /**
     * 去重
     *
     * @param iCommand
     * @return
     */
    public static ArrayList<ICommand> removeDuplicateICommand(List<ICommand> iCommand) {
        TreeSet<ICommand> iCommands = new TreeSet<>(new Comparator<ICommand>() {
            @Override
            public int compare(ICommand o1, ICommand o2) {
                return o1.getIcommandUserid().compareTo(o2.getIcommandUserid());
            }
        });
        iCommands.addAll(iCommand);
        return new ArrayList<ICommand>(iCommands);
    }

}
