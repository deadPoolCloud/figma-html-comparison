package com.mirror.orchestrator;

import com.mirror.model.DiffResult;
import com.mirror.model.Viewport;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * REST Controller for visual comparison API
 */
@RestController
@RequestMapping("/api")
public class CompareController {

    private final ComparisonOrchestrator orchestrator = new ComparisonOrchestrator();

    @PostMapping("/compare")
    public Map<String, Object> compare(@RequestParam String url,
                                       @RequestParam String figmaFile,
                                       @RequestParam String figmaFrame,
                                       @RequestParam(required = false, defaultValue = "DESKTOP") String viewport) {

        Viewport viewportEnum = Viewport.valueOf(viewport.toUpperCase());
        DiffResult result = orchestrator.compare(url, figmaFile, figmaFrame, viewportEnum);

        Map<String, Object> response = new HashMap<>();
        response.put("mismatchPercent", result.getMismatchPercent());
        response.put("severity", result.getSeverity().getLabel());
        response.put("regions", result.getRegions().size());
        response.put("observations", result.getObservations());
        response.put("reportPath", "reports/report_*.html");

        return response;
    }
}
