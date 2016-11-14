package de.usu.research.hobbit.gui.rabbitmq;

public class RabbitMQConnectionSingleton {

    private static volatile RabbitMQConnection theConnection = null;

    public static RabbitMQConnection getConnection() throws Exception {
        if (theConnection == null) {
            synchronized (RabbitMQConnectionSingleton.class) {
                if (theConnection == null) {
                    theConnection = new RabbitMQConnection();
                    theConnection.open();
                }
            }
        }
        return theConnection;
    }

    public static void shutdown() {
        try {
            if (theConnection != null) {
                theConnection.close();
                theConnection = null;
            }
        } catch (Exception e) {
        }
    }
}
