package com.gslab.pepper.input.serialized;

import org.apache.kafka.common.serialization.Serializer;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.util.Map;

/**
 * The ObjectSerializer is custom Object serializer for kafka producer. This class takes object as input and returns byte array.
 *
 * @Author Satish Bhor<satish.bhor@gslab.com>, Nachiket Kate <nachiket.kate@gslab.com>
 * @Version 1.0
 * @since 01/03/2017
 */
public class ObjectSerializer implements Serializer {

    private static final Logger LOGGER = LogManager.getLogger(ObjectSerializer.class);

    @Override
    public void configure(Map map, boolean b) {
        //TODO
    }

    @Override
    public byte[] serialize(String s, Object o) {

        byte[] retVal = null;

        try (
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                ObjectOutput out = new ObjectOutputStream(bos);
        ) {

            out.writeObject(o);
            out.flush();
            retVal = bos.toByteArray();
        } catch (IOException e) {
            LOGGER.log(Level.ERROR, "Failed to serialize object", e);
        }
        return retVal;
    }

    @Override
    public void close() {
        //TODO
    }
}
