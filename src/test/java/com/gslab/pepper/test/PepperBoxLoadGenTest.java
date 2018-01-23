package com.gslab.pepper.test;

import com.gslab.pepper.PepperBoxLoadGenerator;
import kafka.admin.AdminUtils;
import kafka.admin.RackAwareMode;
import kafka.server.KafkaConfig;
import kafka.server.KafkaServer;
import kafka.utils.MockTime;
import kafka.utils.TestUtils;
import kafka.utils.ZKStringSerializer$;
import kafka.utils.ZkUtils;
import kafka.zk.EmbeddedZookeeper;
import org.I0Itec.zkclient.ZkClient;
import org.apache.jmeter.threads.JMeterContext;
import org.apache.jmeter.threads.JMeterContextService;
import org.apache.jmeter.threads.JMeterVariables;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.utils.Time;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Created by satish on 5/3/17.
 */
public class PepperBoxLoadGenTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(PepperBoxLoadGenTest.class);

    private static final String ZKHOST = "127.0.0.1";
    private static final String BROKERHOST = "127.0.0.1";
    private static final String BROKERPORT = "9093";
    private static final String TOPIC = "test";

    private EmbeddedZookeeper zkServer = null;

    private KafkaServer kafkaServer = null;

    private ZkClient zkClient = null;

    @Before
    public void setup() throws IOException {

        // setup Zookeeper
        zkServer = new EmbeddedZookeeper();

        String zkConnect = ZKHOST + ":" + zkServer.port();
        zkClient = new ZkClient(zkConnect, 30000, 30000, ZKStringSerializer$.MODULE$);
        ZkUtils zkUtils = ZkUtils.apply(zkClient, false);

        // setup Broker
        Path kafkaLogDirs = Files.createTempDirectory("kafka-");
        kafkaLogDirs.toFile().deleteOnExit();

        Properties brokerProps = new Properties();
        brokerProps.setProperty("zookeeper.connect", zkConnect);
        brokerProps.setProperty("broker.id", "0");
        brokerProps.setProperty("log.dirs", kafkaLogDirs.toAbsolutePath().toString());
        brokerProps.setProperty("listeners", "PLAINTEXT://" + BROKERHOST +":" + BROKERPORT);
        brokerProps.setProperty("port", BROKERPORT);
        brokerProps.setProperty("offsets.topic.replication.factor" , "1");
        KafkaConfig config = new KafkaConfig(brokerProps);
        Time mock = new MockTime();
        kafkaServer = TestUtils.createServer(config, mock);

        // create topic
        AdminUtils.createTopic(zkUtils, TOPIC, 1, 1, new Properties(), RackAwareMode.Disabled$.MODULE$);

        List<KafkaServer> servers = new ArrayList<KafkaServer>();
        servers.add(kafkaServer);
        TestUtils.waitUntilMetadataIsPropagated(scala.collection.JavaConversions.asScalaBuffer(servers), TOPIC, 0, 5000);

        JMeterContext jmcx = JMeterContextService.getContext();
        jmcx.setVariables(new JMeterVariables());
    }

    @Test
    public void consoleLoadGenTest() throws IOException {
        File schemaFile = File.createTempFile("json", ".schema");
        schemaFile.deleteOnExit();
        FileWriter schemaWriter = new FileWriter(schemaFile);
        schemaWriter.write(TestInputUtils.testSchema);
        schemaWriter.close();

        // setup producer
        File producerFile = File.createTempFile("producer", ".properties");
        producerFile.deleteOnExit();
        FileWriter producerPropsWriter = new FileWriter(producerFile);
        producerPropsWriter.write(String.format(TestInputUtils.producerProps, BROKERHOST, BROKERPORT, ZKHOST, zkServer.port()));
        producerPropsWriter.close();

        // send message
        String vargs []  = new String[]{"--schema-file", schemaFile.getAbsolutePath(), "--producer-config-file", producerFile.getAbsolutePath(), "--throughput-per-producer", "10", "--test-duration", "1", "--num-producers", "1"};
        PepperBoxLoadGenerator.main(vargs);

        // setup consumer
        Properties consumerProps = new Properties();
        consumerProps.setProperty("bootstrap.servers", BROKERHOST + ":" + BROKERPORT);
        consumerProps.setProperty("group.id", "group0");
        consumerProps.setProperty("client.id", "consumer0");
        consumerProps.setProperty("key.deserializer","org.apache.kafka.common.serialization.StringDeserializer");
        consumerProps.setProperty("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        consumerProps.put("auto.offset.reset", "earliest");  // to make sure the consumer starts from the beginning of the topic
        KafkaConsumer<String, String> consumer = new KafkaConsumer<>(consumerProps);
        consumer.subscribe(Arrays.asList(TOPIC));

        // starting consumer
        ConsumerRecords<String, String> records = consumer.poll(50000000);
        Assert.assertTrue("PepperBoxLoadGenerator validation failed", records.count() > 0);
        Iterator<ConsumerRecord<String, String>> recordIterator = records.iterator();
        ConsumerRecord<String, String> record = recordIterator.next();
        LOGGER.info("offset = " + record.offset() + ", key = " + record.key() + ", value = " + record.value());
    }

    @After
    public void teardown(){
        kafkaServer.shutdown();
        zkClient.close();
        try {
            zkServer.shutdown();
        } catch(Exception e) {
            // Ignore exception, as deleting file is failing due to https://issues.apache.org/jira/browse/KAFKA-6291
        }
    }
}
