package com.criss.wang.controller;

import java.util.Arrays;

import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.Consumed;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.KTable;
import org.apache.kafka.streams.kstream.Produced;

public class KafkaStreamTest3 {

	public static void main(String[] args) {
		// Serializers/deserializers (serde) for String and Long types
		final Serde<String> stringSerde = Serdes.String();
		final Serde<Long> longSerde = Serdes.Long();

		// Construct a `KStream` from the input topic "streams-plaintext-input", where message values
		// represent lines of text (for the sake of this example, we ignore whatever may be stored
		// in the message keys).
		StreamsBuilder builder = new StreamsBuilder();
		KStream<String, String> textLines = builder.stream(
		      "streams-plaintext-input",
		      Consumed.with(stringSerde, stringSerde)
		    );

		KTable<String, Long> wordCounts = textLines
		    // Split each text line, by whitespace, into words.
		    .flatMapValues(value -> Arrays.asList(value.toLowerCase().split("\\W+")))

		    // Group the text words as message keys
		    .groupBy((key, value) -> value)

		    // Count the occurrences of each word (message key).
		    .count();

		// Store the running counts as a changelog stream to the output topic.
		wordCounts.toStream().to("streams-wordcount-output", Produced.with(Serdes.String(), Serdes.Long()));
	}

}
