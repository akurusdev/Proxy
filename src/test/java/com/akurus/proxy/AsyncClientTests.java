package com.akurus.proxy;

import com.akurus.proxy.configuration.AsyncConfiguration;
import com.akurus.proxy.model.DataNotification;
import com.akurus.proxy.service.AsyncRequestSenderService;
import com.akurus.proxy.service.CacheServiceImpl;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

@ExtendWith(SpringExtension.class)
@SpringBootTest( webEnvironment= SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import({AsyncRequestSenderService.class, CacheServiceImpl.class})
class AsyncClientTests {

	@Autowired
	AsyncRequestSenderService asyncRequestSenderService;
	@Autowired
	private TestRestTemplate restTemplate;

	@LocalServerPort
	int randomServerPort;

	@Test
	void testAsyncClient() throws Exception {
		CountDownLatch countDownLatch = new CountDownLatch(1);
		Server server = createServer(countDownLatch);
		List<DataNotification> listDataNotification = new ArrayList<DataNotification>();
		listDataNotification.add(new DataNotification(
				Instant.now().toEpochMilli(),
				"192.168.1.1",
				"data"
		));
		asyncRequestSenderService.sendNotificationsData(listDataNotification);
		countDownLatch.await(20000, TimeUnit.MILLISECONDS);
		server.stop();
	}

	//Test ByCount && ByTime
	@ParameterizedTest
	@ValueSource(ints = {10, 9})
	void testSetNotification(int events) throws Exception {
		CountDownLatch countDownLatch = new CountDownLatch(events);
		Server server = createServer(countDownLatch);
		final String baseUrl = "http://localhost:" + randomServerPort+"/sensor/notifications";
		URI uri = new URI(baseUrl);
		DataNotification data = new DataNotification(
				Instant.now().toEpochMilli(),
				"192.168.1.1",
				"data"
		);
		HttpHeaders headers = new HttpHeaders();
		HttpEntity<DataNotification> request = new HttpEntity<>(data, headers);
		for(int i = 0; i < events; i++){
			ResponseEntity result = restTemplate.postForEntity(
					uri,
					request,
					Object.class
			);
			Assertions.assertEquals(200, result.getStatusCodeValue());
		}
		countDownLatch.await(20000, TimeUnit.MILLISECONDS);
		server.stop();
	}


	@Test
	void testSetMultiNotification() throws Exception {
		CountDownLatch countDownLatch = new CountDownLatch(100);
		Server server = createServer(countDownLatch);
		final String baseUrl = "http://localhost:" + randomServerPort+"/sensor/notifications";
		URI uri = new URI(baseUrl);
		DataNotification data = new DataNotification(
				Instant.now().toEpochMilli(),
				"192.168.1.1",
				"data"
		);
		HttpHeaders headers = new HttpHeaders();
		HttpEntity<DataNotification> request = new HttpEntity<>(data, headers);
		for(int i = 0; i < 10; i++){
			new Thread(() -> {
				for(int j = 0; j < 10; j++){
					ResponseEntity result = restTemplate.postForEntity(
							uri,
							request,
							Object.class
					);
				}
			}).start();
		}
		assertThat(countDownLatch.await(20000, TimeUnit.MILLISECONDS)).isTrue();
		server.stop();
	}

	private Server createServer(CountDownLatch countDownLatch) throws Exception {
		Server server = new Server(22222);
		ServletContextHandler context = new ServletContextHandler();
		context.setContextPath("/");
		ServletHolder asyncHolder = new ServletHolder(new TestServlet(countDownLatch));
		context.addServlet(asyncHolder, "/test");
		asyncHolder.setAsyncSupported(true);
		server.setHandler(context);
		server.start();
		while (!server.isStarted()) {
			Thread.sleep(10);
		}
		return server;
	}

}
