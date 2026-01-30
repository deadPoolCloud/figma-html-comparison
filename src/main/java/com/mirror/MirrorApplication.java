package com.mirror;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class MirrorApplication {

    public static void main(String[] args) {
        boolean serverMode = false;
        for (String arg : args) {
            if ("--server".equalsIgnoreCase(arg)) {
                serverMode = true;
                break;
            }
        }

        if (serverMode) {
            SpringApplication.run(MirrorApplication.class, args);
        } else {
            // Default to CLI mode (handles both interactive and direct arguments)
            com.mirror.cli.VisualComparisonCLI.main(args);
        }
    }
}
