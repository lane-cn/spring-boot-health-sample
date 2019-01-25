package com.mydomain.app.health;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Health.Builder;
import org.springframework.boot.actuate.health.HealthIndicator;

public class MyHealthIndicator implements HealthIndicator {

	protected final static Logger LOG = LoggerFactory.getLogger(MyHealthIndicator.class);
	private String command;

	public MyHealthIndicator(String command) {
		this.command = command;
	}

	@Override
	public Health health() {
		Builder builder = null;

		try {
			Process process = Runtime.getRuntime().exec(command);
			process.waitFor();
			
			List<String> stdout = readLines(process.getInputStream());
			List<String> stderr = readLines(process.getErrorStream());
			int exitValue = process.exitValue();
			LOG.debug("exitValue: {}", exitValue);

			if (exitValue == 0) {
				builder = Health.up();
			} else {
				builder = Health.down();
			}

			builder.withDetail("exitValue", exitValue).withDetail("stdout", stdout).withDetail("stderr", stderr);
		} catch (Exception e) {
			builder = Health.down(e);
		}

		builder.withDetail("command", command);
		return builder.build();
	}

	public static List<String> readLines(InputStream input) throws IOException {
		return readLines(input, Charset.defaultCharset().name());
	}

	public static List<String> readLines(InputStream input, String encoding) throws IOException {
		InputStreamReader reader = new InputStreamReader(input, encoding);
		return readLines(reader);
	}

	public static List<String> readLines(Reader input) throws IOException {
		BufferedReader reader = toBufferedReader(input);
		List<String> list = new ArrayList<String>();
		String line = reader.readLine();
		while (line != null) {
			list.add(line);
			line = reader.readLine();
		}
		return list;
	}

	public static BufferedReader toBufferedReader(Reader reader) {
		return reader instanceof BufferedReader ? (BufferedReader) reader : new BufferedReader(reader);
	}

}
