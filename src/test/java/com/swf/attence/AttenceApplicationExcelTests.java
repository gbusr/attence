package com.swf.attence;

import com.baomidou.mybatisplus.mapper.EntityWrapper;
import com.swf.attence.entity.AttenceMsg;
import com.swf.attence.entity.ICommand;
import com.swf.attence.entity.LeaveMsg;
import com.swf.attence.mapper.AttenceMsgMapper;
import com.swf.attence.mapper.ICommandMapper;
import com.swf.attence.service.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

@RunWith(SpringRunner.class)
@SpringBootTest
public class AttenceApplicationExcelTests {
    @Autowired
   private IAttenceMsgService iAttenceMsgService;
    @Autowired
    private AttenceMsgMapper attenceMsgMapper;
    @Autowired
    private IUserMsgService iUserMsgService;
    @Autowired
    private ILeaveMsgService iLeaveMsgService;
    @Autowired
    private IStateControlService iStateControlService;
    @Test
    public void contextLoads() throws IOException {
        iStateControlService.dateDiff(1);
    }

}

