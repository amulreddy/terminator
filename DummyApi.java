package com.autowares.mongoose.api;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpServerErrorException;

import com.autowares.mongoose.client.DummyClient;
import com.autowares.servicescommon.api.ApiMessage;
import com.autowares.servicescommon.util.PrettyPrint;

@RestController
@RequestMapping("/mongoose/dummy")
@Profile("local")
public class DummyApi {

	private DummyClient dummyClient = new DummyClient();
	private Integer callTime = 50;
	private Executor threadPool = Executors.newFixedThreadPool(50);

	@GetMapping(path = "/timeout")
	public ApiMessage takeSecondsToRespond(@RequestParam(name = "timeInSeconds", required = false) Integer timeInSeconds)
			throws InterruptedException {
		if (timeInSeconds == null) {
			timeInSeconds = callTime;
		}
		Thread.sleep(timeInSeconds);
		
		return new ApiMessage("done");
	}

	@GetMapping(path = "/fiveHundy")
	public String processUnProcessedDocments() {
		throw new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR);
	}

	@GetMapping(path = "/exerciseClient")
	public void exercise() {
		for (int i = 0; i < 11; i++) {
			threadPool.execute(()-> PrettyPrint.print(dummyClient.timeOut())); 
		}
	}

	@PostMapping(path = "/setTimeout")
	public void setTimeout(@RequestParam Integer timeout) {
		this.callTime = timeout;
	}

}
