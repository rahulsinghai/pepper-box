package com.gslab.pepper.input.serialized;

import org.apache.kafka.common.serialization.Deserializer;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.Map;

/**
 * The ObjectDeserializer is custom Object deserializer for kafka consumer. This class takes byte array as input and returns object.
 *
 * @Author Satish Bhor<satish.bhor@gslab.com>, Nachiket Kate <nachiket.kate@gslab.com>
 * @Version 1.0
 * @since 01/03/2017
 */
public class ObjectDeserializer implements Deserializer {

    private static final Logger LOGGER = LogManager.getLogger(ObjectDeserializer.class);

    @Override
    public void configure(Map map, boolean b) {
        //TODO
    }

    @Override
    public Object deserialize(String s, byte[] bytes) {
        Object receivedObj = null;

        try (
                ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(bytes);
                ObjectInputStream ois = new ObjectInputStream(byteArrayInputStream);
        ) {
            receivedObj = ois.readObject();
        } catch (ClassNotFoundException | IOException e) {
            LOGGER.log(Level.ERROR, "Failed to deserialize object", e);
        }
        return receivedObj;
    }

    @Override
    public void close() {
        //TODO
    }
}
