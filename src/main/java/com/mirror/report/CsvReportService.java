package com.mirror.report;

import com.mirror.model.DiffRegion;
import com.mirror.model.DiffResult;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;

import java.io.FileWriter;
import java.io.IOException;

public class CsvReportService implements ReportService {

    private static final String OUTPUT_DIR = "src/main/resources/static/results/";

    @Override
    public void generate(DiffResult result) {

        try {
            // Save diff mask image
            Mat diff = result.getDiffMask();
            Imgcodecs.imwrite(OUTPUT_DIR + "diff.png", diff);

            // Write CSV
            FileWriter writer = new FileWriter(OUTPUT_DIR + "report.csv");
            CSVPrinter csv = new CSVPrinter(writer,
                    CSVFormat.DEFAULT.withHeader("X","Y","Width","Height","Impact%"));

            for (DiffRegion r : result.getRegions()) {
                csv.printRecord(r.getX(), r.getY(),
                        r.getWidth(), r.getHeight(),
                        r.getImpactPercent());
            }

            csv.flush();
            csv.close();

        } catch (IOException e) {
            throw new RuntimeException("Failed to generate report", e);
        }
    }
}
