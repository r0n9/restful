package vip.fanrong.REST;

import java.lang.reflect.Field;
import java.util.Date;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author home
 *
 * $LastChangedDate$
 * $LastChangedRevision$
 * $LastChangedBy$
 */
public class Status {
    public static enum StatusCode{
        
        JOB_EXISTS("job is running already, start new job fail"), 
        NEW_JOB_START("start new job success"), 
        NO_JOB_RUNNING("no job is running"), 
        JOB_COMPLETE("job is completed"), 
        JOB_RUNNING("job is running");
        
        private String msg;
        StatusCode(String msg){
            this.msg = msg;
        }        
        public String getMsg() {
            return msg;
        }
    }
    
    public final AtomicBoolean isCompleted = new AtomicBoolean(false);

    private Date startTime, endTime;
    private String timecost;
    private String errorMsg;
    private Exception e;
    private Object methodResult;

    public void setErrorMsg(String errorMsg, Exception e) {
        this.errorMsg = errorMsg;
        this.e = e;
    }

    public void setMethodResult(Object jobResult) {
        this.methodResult = jobResult;
    }

    public Object getMethodResult() {
        return methodResult;
    }

    public void clear() {
        try {
            for (Field f : Status.class.getDeclaredFields()) {                
                f.setAccessible(true); // You might want to set modifier to public first.                
                if (f.getType()!=AtomicBoolean.class&&f.get(this) != null) {
                    f.set(this, null);
                }
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public void start(){
        this.startTime = new Date();
    }
    
    public void end(){
        this.endTime = new Date();
        
        long millis = this.endTime.getTime()-this.startTime.getTime();
        long second = (millis / 1000) % 60;
        long minute = (millis / (1000 * 60)) % 60;
        long hour = (millis / (1000 * 60 * 60)) % 24;

        timecost = String.format("%02d:%02d:%02d:%d", hour, minute, second, millis);
    }
    
    public static void main(String[] args) throws Exception{
        Status s = new Status();
        s.start();        
        Thread.sleep(1000);
        s.end();
        System.out.println(JsonOutputUtil.toString(s, Status.class));
        s.clear();
        System.out.println(JsonOutputUtil.toString(s, Status.class));
    }
}
