package com.example.Gnosis.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PageController {
	@GetMapping({"/", "/landingPage"})
	public String landingPage() {
		return "Landing.html";
	}
}

