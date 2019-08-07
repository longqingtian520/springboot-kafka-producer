package com.criss.wang.controller;

import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Date;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.Materialized;
import org.apache.kafka.streams.kstream.TimeWindows;
import org.apache.kafka.streams.kstream.Windowed;

public class TumblingTimeWindows {

	private static final String TEST_TOPIC = "criss-test";

	private static final int TIME_WINDOW_SECONDS = 5;

	public static void main(String[] arg) throws Exception {
		Properties props = new Properties();
		props.put(StreamsConfig.APPLICATION_ID_CONFIG, "wordcount-application");
		props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "office-server:9092");
		props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass());
		props.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass());

		new TumblingTimeWindows().test(props);
	}

	public void test(Properties props) throws Exception {

		StreamsBuilder builder = new StreamsBuilder();
		KStream<String, String> data = builder.stream(TEST_TOPIC);

		Instant initTime = Instant.now();

		data.groupByKey().windowedBy(TimeWindows.of(TimeUnit.SECONDS.toMillis(TIME_WINDOW_SECONDS)))
				.count(Materialized.with(Serdes.String(), Serdes.Long())).toStream()
				.filterNot(((windowedKey, value) -> this.isOldWindow(windowedKey, value, initTime))) // 剔除太旧的时间窗口，程序二次启动时，会重新读取历史数据进行整套流处理，为了不影响观察，这里过滤掉历史数据
				.foreach(this::dealWithTimeWindowAggrValue);

		Topology topology = builder.build();
		KafkaStreams streams = new KafkaStreams(topology, props);
		streams.start();

		Thread.currentThread().join();
	}

	private boolean isOldWindow(Windowed<String> windowKey, Long value, Instant initTime) {
		Date windowEnd = new Date(windowKey.window().end());
		return windowEnd.before(new Date(initTime.toEpochMilli()));
	}

	private void dealWithTimeWindowAggrValue(Windowed<String> key, Long value) {
		Windowed<String> windowed = getReadableWindowed(key);
		System.out.println("处理聚合结果：key=" + windowed + ",value=" + value);
	}

	private Windowed<String> getReadableWindowed(Windowed<String> key) {
		return new Windowed<String>(key.key(), key.window()) {
			@Override
			public String toString() {
				String startTimeStr = toLocalTimeStr(Instant.ofEpochMilli(window().start()));
	            String endTimeStr = toLocalTimeStr(Instant.ofEpochMilli(window().end()));
				return "[" + key() + "@" + startTimeStr + "/" + endTimeStr + "]";
			}
		};
	}

	private static String toLocalTimeStr(Instant i) {
	    return i.atZone(ZoneId.systemDefault()).toLocalDateTime().toString();
	}

	public static void main1(String[] s) {
		Instant init = Instant.now();
		System.out.println(init.toEpochMilli());
		System.out.println(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(init.toEpochMilli())));
	}

}
