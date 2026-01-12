package com.hazelcast.guide;

import com.hazelcast.config.Config;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static com.hazelcast.spring.session.HazelcastSessionConfiguration.applySerializationConfig;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HazelcastSpringSessionApplicationTests {

	static final String COOKIE_NAME = "SESSION";

	private ConfigurableApplicationContext app1;
	private ConfigurableApplicationContext app2;
	private HazelcastInstance hazelcast;

	@AfterEach
	void shutdown() {
		if (app1 != null) {
			app1.close();
		}
		if (app2 != null) {
			app2.close();
		}
		if (hazelcast != null) {
			hazelcast.shutdown();
		}
	}

	@ParameterizedTest(name = "useClientServer={0}")
	@ValueSource(booleans =  {true, false})
	void contextLoads(boolean useClientServer) {
		// given
		if (useClientServer) {
			Config config = new Config();
			config.getNetworkConfig().getJoin().getTcpIpConfig()
				  .setEnabled(true)
				  .addMember("127.0.0.1");

			hazelcast = Hazelcast.newHazelcastInstance(applySerializationConfig(config));
		}
		app1 = startApplication(useClientServer);
		app2 = startApplication(useClientServer);
        String port1 = app1.getEnvironment().getProperty("local.server.port");
		String port2 = app2.getEnvironment().getProperty("local.server.port");
		Map<String, String> principalMap = Collections.singletonMap("principal", "hazelcast2020");

		// when
		ResponseEntity<?> response1 = makeRequest(port1, null, principalMap);
		String sessionCookie1 = extractSessionCookie(response1);

		// then
		ResponseEntity<?> response2 = makeRequest(port2, sessionCookie1, principalMap);
        assert response2.getBody() != null;
        assertTrue(response2.getBody().toString().contains("Session already exists"));
	}

	private static ConfigurableApplicationContext startApplication(boolean useClientServer) {
		return new SpringApplicationBuilder(HazelcastSpringSessionApplication.class)
				.properties("server.port=0",
							"server.servlet.session.cookie.name=" + COOKIE_NAME,
							"guide.useClientServer=" + useClientServer)
				.run();
	}

	private String extractSessionCookie(ResponseEntity<?> response) {
		List<String> cookies = response.getHeaders().get("Set-Cookie");
		assert cookies != null;
		return cookies.stream()
					  .filter(s -> s.contains(COOKIE_NAME))
					  .map(s -> s.substring(COOKIE_NAME.length() + 1))
					  .findFirst().orElse(null);
	}

	private static ResponseEntity<?> makeRequest(String port, String sessionCookie, Map<String, String> parameters) {
		String url = "http://localhost:" + port + "/create";

		// Header
		HttpHeaders headers = new HttpHeaders();
		if (sessionCookie != null) {
			headers.add("Cookie", COOKIE_NAME + "=" + sessionCookie);
		}

		// Query parameters
		UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(url);
		parameters.forEach(builder::queryParam);

		// Request
		RestTemplate restTemplate = new RestTemplate();
		return restTemplate.exchange(builder.toUriString(), HttpMethod.GET, new HttpEntity<>(headers), String.class);
	}

}
