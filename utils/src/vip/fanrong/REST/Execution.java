package vip.fanrong.REST;

import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.commons.collections.map.LinkedMap;
import org.apache.commons.lang.exception.ExceptionUtils;

public class Execution {
    private Map<String, Object> stepReport = new LinkedHashMap<String, Object>();
    private LinkedMap stepTrace = new LinkedMap();
    private Map<String, Long> stepTimeReport = new LinkedHashMap<String, Long>();
    private Exception error = null;

    public final void startStep(String stepName) {
        stepTrace.put(stepName, System.currentTimeMillis());
    }

    public final void completeStep() {
        completeStep(null);
    }

    public final void completeStep(Exception e) {
        if (stepTrace.isEmpty()) {// a defensive check, it should never come to here
            System.err.println("current step trace has no step in tracing");
            return;
        }

        String currentStep = (String) stepTrace.lastKey();
        Long start = (Long) stepTrace.remove(currentStep);

        long timecost = System.currentTimeMillis() - start;
        stepReport.put(currentStep, timecost);
        stepTimeReport.put(currentStep, timecost);
        if (e != null) {
            String errorMsg = ExceptionUtils.getStackTrace(e);
            stepReport.put(currentStep + ".error", errorMsg);
            error = e;
        }

    }

    public void record(String key, Object value) {
        stepReport.put(key, value);
    }

    public String getMessage() {
        StringBuilder buf = new StringBuilder();
        for (String step : stepReport.keySet()) {
            buf.append(step).append(":");
            buf.append(stepReport.get(step));
            buf.append("\r\n");
        }
        return buf.toString();
    }

    public final boolean hasError() {
        return error != null;
    }

    public Map<String, Object> getReport() {
        return stepReport;
    }

    public Map<String, Long> getTimeReport() {
        return stepTimeReport;
    }

}
