package com.example.PDFMerger;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.io.RandomAccessReadBuffer;
import org.apache.pdfbox.multipdf.PDFMergerUtility;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "https://pdf-merger-galaxy-merger.vercel.app/")
public class PdfMerger {

    private static final String MAX_FILE_SIZE = "100MB";
    private static final String MAX_REQUEST_SIZE = "250MB";

    @PostMapping("/merge")
    public ResponseEntity<?> mergePdfFile(@RequestParam("files") MultipartFile[] img) {
        Path tempMergedFile = null;
        List<Path> tempInputFiles = new ArrayList<>(); // Store all temp input files

        try {
            long totalSize = 0;
            for (MultipartFile file : img) {
                totalSize += file.getSize();
                if (file.getSize() > 100 * 1024 * 1024) {
                    Map<String, String> errorResponse = new HashMap<>();
                    errorResponse.put("error", "FILE_SIZE_EXCEEDED");
                    errorResponse.put("message", "File '" + file.getOriginalFilename() + "' exceeds the maximum allowed size of " + MAX_FILE_SIZE);
                    errorResponse.put("maxFileSize", MAX_FILE_SIZE);
                    return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(errorResponse);
                }
            }

            if (totalSize > 250 * 1024 * 1024) {
                Map<String, String> errorResponse = new HashMap<>();
                errorResponse.put("error", "REQUEST_SIZE_EXCEEDED");
                errorResponse.put("message", "Total request size exceeds the maximum allowed size of " + MAX_REQUEST_SIZE);
                errorResponse.put("maxRequestSize", MAX_REQUEST_SIZE);
                errorResponse.put("actualSize", formatFileSize(totalSize));
                return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(errorResponse);
            }

            PDFMergerUtility pdfMergerUtility = new PDFMergerUtility();
            tempMergedFile = Files.createTempFile("merged_", ".pdf");
            pdfMergerUtility.setDestinationFileName(tempMergedFile.toString());

            for (MultipartFile f : img) {
                Path tempInputFile = Files.createTempFile("input_", ".pdf");
                tempInputFiles.add(tempInputFile);

                try (InputStream inputStream = f.getInputStream()) {
                    Files.copy(inputStream, tempInputFile, StandardCopyOption.REPLACE_EXISTING);
                }


                pdfMergerUtility.addSource(tempInputFile.toFile());


            }

            pdfMergerUtility.mergeDocuments(null);
            byte[] compressedPdf = compressPdf(tempMergedFile);

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=merged.pdf")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(compressedPdf);

        } catch (Exception e) {
            e.printStackTrace();
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "PROCESSING_ERROR");
            errorResponse.put("message", "Error occurred during merging: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        } finally {
            // Clean up all temporary files
            if (tempMergedFile != null) {
                try {
                    Files.deleteIfExists(tempMergedFile);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }


            for (Path tempInputFile : tempInputFiles) {
                try {
                    Files.deleteIfExists(tempInputFile);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }


    @ControllerAdvice
    public static class GlobalExceptionHandler {
        @ExceptionHandler(MaxUploadSizeExceededException.class)
        public ResponseEntity<Map<String, String>> handleMaxSizeException(MaxUploadSizeExceededException exc) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "UPLOAD_SIZE_EXCEEDED");
            errorResponse.put("message", "File size exceeds the maximum allowed limit");
            errorResponse.put("maxFileSize", MAX_FILE_SIZE);
            errorResponse.put("maxRequestSize", MAX_REQUEST_SIZE);
            return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(errorResponse);
        }
    }

    private byte[] compressPdf(Path pdfPath) {
        try (PDDocument document = Loader.loadPDF(pdfPath.toFile())) {
            ByteArrayOutputStream compressedStream = new ByteArrayOutputStream();
            document.save(compressedStream);
            return compressedStream.toByteArray();
        } catch (Exception e) {
            e.printStackTrace();
            try {
                return Files.readAllBytes(pdfPath);
            } catch (Exception ex) {
                throw new RuntimeException("Failed to read file after compression error", ex);
            }
        }
    }

    private String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String pre = "KMGTPE".charAt(exp-1) + "";
        return String.format("%.1f %sB", bytes / Math.pow(1024, exp), pre);
    }
}