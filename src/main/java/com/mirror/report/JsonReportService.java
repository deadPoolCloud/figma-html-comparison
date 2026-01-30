package com.mirror.report;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.mirror.model.DiffResult;
import com.mirror.model.SemanticComparisonResult;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Generates automated JSON reports for machine-level integration (CI/CD,
 * dashboards).
 */
public class JsonReportService implements ReportService {

    private static final String OUTPUT_DIR = "reports/";
    private final ObjectMapper mapper;

    public JsonReportService() {
        this.mapper = new ObjectMapper();
        this.mapper.enable(SerializationFeature.INDENT_OUTPUT);
    }

    @Override
    public void generate(SemanticComparisonResult result) {
        saveJson(result, "semantic_report");
    }

    @Override
    public void generate(DiffResult result) {
        // Visual diff results are usually heavy due to images,
        // we summary the metadata for JSON.
        saveJson(result, "visual_report");
    }

    private void saveJson(Object data, String prefix) {
        try {
            Files.createDirectories(Paths.get(OUTPUT_DIR));
            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            String path = OUTPUT_DIR + prefix + "_" + timestamp + ".json";

            mapper.writeValue(new File(path), data);
            System.out.println("JSON Report generated: " + path);
        } catch (IOException e) {
            throw new RuntimeException("Failed to generate JSON report", e);
        }
    }
}
