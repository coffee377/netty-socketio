package com.ccl.socketio.core.protocol.data;

import java.util.List;

public class Event {

    private String name;
    private List<Object> args;

    public Event() {
    }

    public Event(String name, List<Object> args) {
        this.name = name;
        this.args = args;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<Object> getArgs() {
        return args;
    }

    public void setArgs(List<Object> args) {
        this.args = args;
    }

    public Object getFirstArg() {
        return args != null && !args.isEmpty() ? args.get(0) : null;
    }
}