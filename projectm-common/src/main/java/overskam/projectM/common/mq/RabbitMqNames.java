package overskam.projectM.common.mq;

public final class RabbitMqNames {
    private RabbitMqNames() {
    }
    
    public static final String COMPILE_EXCHANGE = "projectm.compile.exchange";
    public static final String SAVE_EXCHANGE = "projectm.save.exchange";
    public static final String RPC_EXCHANGE = "projectm.rpc.exchange";
    
    public static final String COMPILE_TASK_QUEUE = "projectm.compile.tasks";
    public static final String SAVE_TASK_QUEUE = "projectm.save.tasks";
    public static final String RPC_REPLY_QUEUE = "projectm.rpc.replies";
    
    public static final String COMPILE_REQUEST_ROUTING_KEY = "compile.request";
    public static final String SAVE_REQUEST_ROUTING_KEY = "save.request";
    public static final String RPC_REPLY_ROUTING_KEY = "rpc.reply";
}