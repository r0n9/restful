/**
 * @file	ExecutionCollector.java
 * @author	xiangl
 * @date	2015-6-11
 * Copyright (c) 2015 Telenav
 */
package vip.fanrong.REST;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * @author xiangl
 *
 * $LastChangedDate$
 * $LastChangedRevision$
 * $LastChangedBy$
 */
public class ExecutionMonitor extends Timer{
    private int total = 0;
    private ConcurrentLinkedQueue<Execution> list = new ConcurrentLinkedQueue<Execution>();
    public void submit(Execution tRACE){
        if(tRACE!=null)
            list.add(tRACE);
    }
    
    /**
     * @return the total
     */
    public int getTotal() {
        return total;
    }
    
    public void start(){
        this.schedule(new TimerTask() {
        
            
            @Override
            public void run() {
                Object[] array = list.toArray();                
                List<Object> collect = Arrays.asList(array);                
                list.removeAll(collect);
                final int count = array.length;
                total += count;
                
                LinkedHashMap<String, Long> timeSum = new LinkedHashMap<String, Long>();
                LinkedHashMap<String, Integer> countSum = new LinkedHashMap<String, Integer>();
                LinkedHashMap<String, Long> counts = new LinkedHashMap<String, Long>();
//                LinkedHashMap<String, Long> counts = new LinkedHashMap<String, Long>();
             
                for(Object obj:array){
                    Map<String, Long> timeReport = ((Execution)obj).getTimeReport();
                    for(Entry<String, Long> entry:timeReport.entrySet()){
                        Long newV = entry.getValue();
                        Long oldV = timeSum.get(entry.getKey());
                        if(oldV!=null){
                            newV += oldV;                            
                        }
                        timeSum.put(entry.getKey(), newV);
                        
                        Long newC = 1L;
                        Long oldC = counts.get(entry.getKey());
                        if(oldC!=null)
                            newC += oldC;
                        counts.put(entry.getKey(), newC);
                    }
                    
                    Map<String, Object> report = ((Execution)obj).getReport();
                    for(Entry<String, Object> entry:report.entrySet()){
                        Object cV = entry.getValue();
                        if(!(cV instanceof Integer)) continue;
                        Integer newV = (Integer)cV;
                        Integer oldV = countSum.get(entry.getKey());
                        if(oldV!=null){
                            newV += oldV;                            
                        }
                        countSum.put(entry.getKey(), newV);
                        
                        Long newC = 1L;
                        Long oldC = counts.get(entry.getKey());
                        if(oldC!=null)
                            newC += oldC;
                        counts.put(entry.getKey(), newC);
                    }
                }
                
                StringBuilder buf = new StringBuilder();
                
                SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm");                
                buf.append("report execution:"+format.format(Calendar.getInstance().getTime())).append("\r\n");
                buf.append("speed: " + (array.length)).append("per min, total: " + total).append("\r\n");
                if(count>0){
                    for(Entry<String, Long> entry:timeSum.entrySet()){
                        long entryCount = counts.get(entry.getKey());
                        buf.append(entry.getKey()).append(".time:").append(entry.getValue()/entryCount).append(" ms").append("\r\n");
                        buf.append(entry.getKey()).append(".occur:").append(entryCount).append("\r\n");
                    }
                    for(Entry<String, Integer> entry:countSum.entrySet()){
                        long entryCount = counts.get(entry.getKey());
                        buf.append(entry.getKey()).append(".count:").append(entry.getValue()/entryCount).append("\r\n");
                        buf.append(entry.getKey()).append(".occur:").append(entryCount).append("\r\n");
                    }
                }
                System.out.println(buf);
            }
		}, 0L, 60000L);
	}

}
