package com.lpt4s.be;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import jakarta.annotation.PostConstruct;

import java.io.IOException;

@SpringBootApplication
public class BeApplication {

    @Autowired
    private InputParser inputParser;

    public static void main(String[] args) {
        SpringApplication.run(BeApplication.class, args);
    }

    @Autowired
    public void consumeInput() {
        try {
            inputParser.consume();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

	@PostConstruct
    public void openCRUDGeneratorPage() {
        try {
            String url = "input/eingabeMaske.html"; // Replace with the actual file path or URL of your HTML page
            
            String os = System.getProperty("os.name").toLowerCase();

            ProcessBuilder processBuilder;
            if (os.contains("win")) {
                // For Windows
                processBuilder = new ProcessBuilder("cmd", "/c", "start", url);
            } else if (os.contains("mac")) {
                // For Mac
                processBuilder = new ProcessBuilder("open", url);
            } else if (os.contains("nix") || os.contains("nux") || os.contains("bsd")) {
                // For Linux/Unix
                processBuilder = new ProcessBuilder("xdg-open", url);
            } else {
                throw new UnsupportedOperationException("Unsupported operating system");
            }

            processBuilder.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
	
}


