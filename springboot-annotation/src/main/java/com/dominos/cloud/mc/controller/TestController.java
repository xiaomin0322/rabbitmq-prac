package com.dominos.cloud.mc.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 */
@RestController
@RequestMapping("/api/test")
public class TestController {

	/**
	 * http://localhost:8080/api/test/send?source=aaa
	 * @param source
	 * @return
	 */
	@GetMapping(value = "/send")
	public String send(@RequestParam(required = true, defaultValue = "") String source) {
		return source;
	}

}
