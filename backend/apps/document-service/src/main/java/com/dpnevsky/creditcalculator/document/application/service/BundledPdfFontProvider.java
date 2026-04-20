package com.dpnevsky.creditcalculator.document.application.service;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;

@Component
public class BundledPdfFontProvider {

    private static final String FONT_RESOURCE_PATH = "fonts/Roboto-Regular.ttf";
    private static final String FONT_FAMILY = "PdfRoboto";

    public void configure(PdfRendererBuilder builder) {
        builder.useFont(this::openFontStream, FONT_FAMILY);
    }

    InputStream openFontStream() {
        try {
            return new ClassPathResource(FONT_RESOURCE_PATH).getInputStream();
        } catch (IOException exception) {
            throw new IllegalStateException("Bundled PDF font not found: " + FONT_RESOURCE_PATH, exception);
        }
    }
}
