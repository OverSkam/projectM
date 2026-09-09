package overskam.projectM.common.mq;

public final class RabbitMqNames {
    private RabbitMqNames() {
    }
    
    public static final String COMPILE_EXCHANGE = "projectm.compile.exchange";
    public static final String CLEANUP_EXCHANGE = "projectm.cleanup.exchange";

    public static final String COMPILE_TASK_QUEUE = "projectm.compile.tasks";
    public static final String CLEANUP_QUEUE = "projectm.cleanup.tasks";

    public static final String COMPILE_REQUEST_ROUTING_KEY = "compile.request";
    public static final String PROJECT_DELETED_ROUTING_KEY = "project.deleted";

    public static final String COMPILE_DLX = "projectm.compile.dlx";
    public static final String COMPILE_DLQ = "projectm.compile.tasks.dlq";
    public static final String COMPILE_DLQ_ROUTING_KEY = "compile.dead";

    public static final String CLEANUP_DLX = "projectm.cleanup.dlx";
    public static final String CLEANUP_DLQ = "projectm.cleanup.tasks.dlq";
    public static final String CLEANUP_DLQ_ROUTING_KEY = "cleanup.dead";
}