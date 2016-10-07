/**
 * @file	RESTResult.java
 * @author	xiangl
 * @date	2016-4-7
 * Copyright (c) 2016 Telenav
 */
package vip.fanrong.REST;

import vip.fanrong.REST.Status.StatusCode;

/**
 * @author xiangl
 *
 * $LastChangedDate$
 * $LastChangedRevision$
 * $LastChangedBy$
 */
public class RESTResult {
    
    public static class Header{
        //request parameter
        private String classSelected;
        private String methodInvoked;
        private String query;
        private String asycKey;
        private boolean isAsyncMethod = false;
    }

    private Header header = new Header();
    //response
    private StatusCode asyncStatusCode;
    private Status status;    
    
    public Object getMethodResult(){
        return status!=null?status.getMethodResult():null;
    }

    public void setClassSelected(String classSelected) {
        header.classSelected = classSelected;
    }
    
    public void setMethodInvoked(String methodInvoked) {
        header.methodInvoked = methodInvoked;
    }

    public void setQuery(String query) {
        header.query = query;
    }
    
    public void setStatus(Status status) {
        this.status = status;
    }
    
    public void setIsAsyncMethod(boolean isAsyncMethod) {
        this.header.isAsyncMethod = isAsyncMethod;
    }
    
    public void setAsycKey(String asycKey) {
        this.header.asycKey = asycKey;
    }
    
    public void setAsyncStatusCode(StatusCode asyncStatusCode) {
        this.asyncStatusCode = asyncStatusCode;
    }

    public StatusCode getAsyncStatusCode() {
        return asyncStatusCode;
    }
    
    
}
