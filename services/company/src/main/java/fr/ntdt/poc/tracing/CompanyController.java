package fr.systel.poc.tracing;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.opentracing.Span;
import io.opentracing.SpanContext;
import io.opentracing.log.Fields;
import io.opentracing.propagation.Format;
import io.opentracing.propagation.TextMapAdapter;
// import io.opentracing.propagation.TextMapExtractAdapter;
import io.opentracing.tag.Tags;

@RestController
@RequestMapping("/v1/companies")
public class CompanyController {

    private final List<String> companiesNames;
    private Random random;

    @Autowired
    private io.opentracing.Tracer tracer;

    public CompanyController() throws IOException {
        InputStream inputStream = new ClassPathResource("/companies.txt").getInputStream();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            companiesNames = reader.lines().collect(Collectors.toList());
        }
        random = new Random();
    }

    @GetMapping(path = "/random")
    public String findRandomCompany(@RequestHeader HttpHeaders headers) {

        // // Get parent span context from request header
        // SpanContext parentSpan = tracer.extract(Format.Builtin.HTTP_HEADERS,
        // new TextMapExtractAdapter(headers.toSingleValueMap()));
        SpanContext parentSpan = tracer.extract(Format.Builtin.HTTP_HEADERS,
                new TextMapAdapter(headers.toSingleValueMap()));

        // Build a new span from parent context
        Span span = null;
        if (parentSpan == null) {
            span = tracer.buildSpan("getRandomCompany").start();
        } else {
            span = tracer.buildSpan("getRandomCompany").asChildOf(parentSpan).start();
        }

        String name = null;
        try (io.opentracing.Scope scope = tracer.scopeManager().activate(span)) {
            // return random name
            name = companiesNames.get(random.nextInt(companiesNames.size()));
            span.setTag("company", name);
        } catch (Exception ex) {
            Tags.ERROR.set(span, true);
            span.log(Map.of(Fields.EVENT, "error", Fields.ERROR_OBJECT, ex, Fields.MESSAGE, ex.getMessage()));
        } finally {
            span.finish();
        }

        return name;
    }
}