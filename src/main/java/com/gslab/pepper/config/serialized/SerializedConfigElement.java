package com.gslab.pepper.config.serialized;

import com.gslab.pepper.loadgen.BaseLoadGenerator;
import com.gslab.pepper.loadgen.impl.SerializedLoadGenerator;
import com.gslab.pepper.model.FieldExpressionMapping;
import com.gslab.pepper.util.PropsKeys;
import org.apache.jmeter.config.ConfigTestElement;
import org.apache.jmeter.engine.event.LoopIterationEvent;
import org.apache.jmeter.engine.event.LoopIterationListener;
import org.apache.jmeter.testbeans.TestBean;
import org.apache.jmeter.threads.JMeterContextService;
import org.apache.jmeter.threads.JMeterVariables;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

/**
 * The SerializedConfigElement custom jmeter config element. This class acts as serialized object feeder to jmeter java sampler, it includes serialized load generator which takes input class and its property expressions and generates messages.
 *
 * @Author Satish Bhor<satish.bhor@gslab.com>, Nachiket Kate <nachiket.kate@gslab.com>
 * @Version 1.0
 * @since 28/02/2017
 */
public class SerializedConfigElement extends ConfigTestElement implements TestBean, LoopIterationListener {

    //clase whose objects will be generated by load generator
    private String className;

    //Class fields and their mapping with functions
    private List<FieldExpressionMapping> objProperties;

    //Message placeholder key
    private String placeHolder;

    //Serialized object load generator
    private BaseLoadGenerator generator = null;

    /**
     * For every JMeter sample, iterationStart method gets invoked, it initializes load generator and for each iteration sets new message as JMeter variable
     *
     * @param loopIterationEvent
     */
    @Override
    public void iterationStart(LoopIterationEvent loopIterationEvent) {

        try {
            //Check if load generator is instantiated
            if (generator == null) {

                //instantiate serialized load generator
                generator = new SerializedLoadGenerator(className, objProperties);

            }

            //For ever iteration put message in jmeter variables
            JMeterVariables variables = JMeterContextService.getContext().getVariables();
            variables.putObject(placeHolder, generator.nextMessage());
        } catch (Exception e) {
            LOGGER.error("Failed to create PlaintTextLoadGenerator instance", e);
        }
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public List<FieldExpressionMapping> getObjProperties() {
        return objProperties;
    }

    public void setObjProperties(List<FieldExpressionMapping> objProperties) {
        this.objProperties = objProperties;
    }


    public String getPlaceHolder() {
        return placeHolder;
    }

    public void setPlaceHolder(String placeHolder) {
        this.placeHolder = placeHolder;
    }

    private static final Logger LOGGER = LogManager.getLogger(SerializedConfigElement.class);

}
