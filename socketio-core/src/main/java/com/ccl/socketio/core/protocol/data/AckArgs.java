package com.ccl.socketio.core.protocol.data;

import java.util.List;

public class AckArgs {

    private int ackId;
    private List<Object> data;

    public AckArgs() {
    }

    public AckArgs(int ackId, List<Object> data) {
        this.ackId = ackId;
        this.data = data;
    }

    public int getAckId() {
        return ackId;
    }

    public void setAckId(int ackId) {
        this.ackId = ackId;
    }

    public List<Object> getData() {
        return data;
    }

    public void setData(List<Object> data) {
        this.data = data;
    }
}