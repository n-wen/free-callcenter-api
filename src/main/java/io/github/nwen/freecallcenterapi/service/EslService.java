package io.github.nwen.freecallcenterapi.service;

public interface EslService {

    boolean isConnected();

    void connect();

    void disconnect();

    String sendCommand(String command);
}
