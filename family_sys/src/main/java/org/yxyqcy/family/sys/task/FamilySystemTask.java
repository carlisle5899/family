package org.yxyqcy.family.sys.task;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.yxyqcy.family.common.config.Global;
import org.yxyqcy.family.sys.util.SystemUtils;

/**
 * Created with family.
 * author: cy
 * Date: 17/2/26
 * Time: 下午8:52
 * description: 系统任务
 */
@Component
public class FamilySystemTask {

    /**
     * 备份family db
     *
         按顺序依次为
         秒（0~59）
         分钟（0~59）
         小时（0~23）
         天（月）（0~31，但是你需要考虑你月的天数）
         月（0~11）
         天（星期）（1~7 1=SUN 或 SUN，MON，TUE，WED，THU，FRI，SAT）
         7.年份（1970－2099）

         其中每个元素可以是一个值(如6),一个连续区间(9-12),一个间隔时间(8-18/4)
         秒	 	0-59	 	, - * /
         分	 	0-59	 	, - * /
         小时	 	0-23	 	, - * /
         日期	 	1-31	 	, - * ? / L W C
         月份	 	1-12 或者 JAN-DEC	 	, - * /
         星期	 	1-7 或者 SUN-SAT	 	, - * ? / L C #
         年（可选）	 	留空, 1970-2099	 	, - * /

         其中每个元素可以是一个值(如6),一个连续区间(9-12),一个间隔时间(8-18/4)(/表示每隔4小时),一个列表(1,3,5),通配符。由于"月份中的日期"和"星期中的日期"这两个元素互斥的,必须要对其中一个设置?.
         0 0 10,14,16 * * ? 每天上午10点，下午2点，4点
         0 0/30 9-17 * * ?   朝九晚五工作时间内每半小时
         0 0 12 ? * WED 表示每个星期三中午12点
         "0 0 12 * * ?" 每天中午12点触发
         "0 15 10 ? * *" 每天上午10:15触发
         "0 15 10 * * ?" 每天上午10:15触发
         "0 15 10 * * ? *" 每天上午10:15触发
         "0 15 10 * * ? 2005" 2005年的每天上午10:15触发
         "0 * 14 * * ?" 在每天下午2点到下午2:59期间的每1分钟触发
         "0 0/5 14 * * ?" 在每天下午2点到下午2:55期间的每5分钟触发
         "0 0/5 14,18 * * ?" 在每天下午2点到2:55期间和下午6点到6:55期间的每5分钟触发
         "0 0-5 14 * * ?" 在每天下午2点到下午2:05期间的每1分钟触发
         "0 10,44 14 ? 3 WED" 每年三月的星期三的下午2:10和2:44触发
         "0 15 10 ? * MON-FRI" 周一至周五的上午10:15触发
         "0 15 10 15 * ?" 每月15日上午10:15触发
         "0 15 10 L * ?" 每月最后一日的上午10:15触发
         "0 15 10 ? * 6L" 每月的最后一个星期五上午10:15触发
         "0 15 10 ? * 6L 2002-2005" 2002年至2005年的每月的最后一个星期五上午10:15触发
         "0 15 10 ? * 6#3" 每月的第三个星期五上午10:15触发



     */
    @Scheduled(cron = "0 0 11 * * ?")
    public void backupFamilyDb(){
        //主机备份
        if("1".equals(Global.getConfig("isMaster")))
            SystemUtils.backupDb(Global.getConfig("backup_dbName"));
    }
}