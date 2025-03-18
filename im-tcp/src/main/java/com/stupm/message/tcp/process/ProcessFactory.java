package com.stupm.message.tcp.process;

public class ProcessFactory {
    static{
        defaultProcess = new BaseProcess() {
            @Override
            public void processBefore() {

            }

            @Override
            public void processAfter() {

            }
        };
    }
    private static BaseProcess defaultProcess;

    public static BaseProcess getMessageProcess(Integer command) {
        return defaultProcess;
    }
}
