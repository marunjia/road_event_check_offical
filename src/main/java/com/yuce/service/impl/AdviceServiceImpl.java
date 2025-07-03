package com.yuce.service.impl;

import com.yuce.service.AdviceService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * @ClassName AdviceServiceImpl
 * @Description TODO
 * @Author jacksparrow
 * @Email 18310124408@163.com
 * @Date 2025/5/27 16:58
 * @Version 1.0
 */

@Slf4j
@Service
public class AdviceServiceImpl implements AdviceService {

    public int getAdviceFlag(int checkFlag, String eventType){
        int adviceFlag = 0;
        if(eventType.equals("抛洒物")){

        }else if(eventType.equals("行人") || eventType.equals("停驶")){
            //todo 暂未接入行人以及停驶事件
            adviceFlag = 2; //尽快确认
        }else {
            //todo 其他事件checkFlag默认为正检，对应处置建议默认为尽快确认
            adviceFlag = 2; //尽快确认
        }
        return adviceFlag;
    }
}