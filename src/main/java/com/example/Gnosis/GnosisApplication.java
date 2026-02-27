package com.example.Gnosis;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@SpringBootApplication
public class GnosisApplication {

	public static void main(String[] args) {
		SpringApplication.run(GnosisApplication.class, args);
	}

	@GetMapping("/landingPage")
	public String landing(Model model) {

		return "Landing.html";
	}

	// Auth endpoints are handled by com.example.Gnosis.auth.AuthController.

}
