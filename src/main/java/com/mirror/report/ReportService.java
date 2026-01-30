package com.mirror.report;

import com.mirror.model.DiffResult;

public interface ReportService {
    void generate(DiffResult result);

    void generate(com.mirror.model.SemanticComparisonResult result);
}
