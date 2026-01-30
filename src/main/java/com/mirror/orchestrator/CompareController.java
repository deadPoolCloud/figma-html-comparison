package com.mirror.orchestrator;

import com.mirror.model.SemanticComparisonResult;
import com.mirror.model.Viewport;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST Controller for visual comparison API
 */
@RestController
@RequestMapping("/api")
public class CompareController {

    private final ComparisonOrchestrator orchestrator = new ComparisonOrchestrator();

    @PostMapping("/compare")
    public SemanticComparisonResult compare(@RequestParam String url,
            @RequestParam String figmaFile,
            @RequestParam String figmaFrame,
            @RequestParam(required = false, defaultValue = "DESKTOP") String viewport,
            @RequestParam(required = false, defaultValue = "false") boolean semanticOnly) {

        Viewport viewportEnum = Viewport.valueOf(viewport.toUpperCase());
        // If semanticOnly is true, includePixelComparison should be false
        return orchestrator.compareSemantic(url, figmaFile, figmaFrame, viewportEnum, !semanticOnly);
    }
}
