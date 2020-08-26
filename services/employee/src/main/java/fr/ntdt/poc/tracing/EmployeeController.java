package fr.systel.poc.tracing;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

import io.opentracing.Span;
import io.opentracing.SpanContext;
import io.opentracing.Scope;
import io.opentracing.Tracer;
import io.opentracing.log.Fields;
import io.opentracing.propagation.Format;
import io.opentracing.propagation.TextMapAdapter;
// import io.opentracing.propagation.TextMapExtractAdapter;
import io.opentracing.tag.Tags;

@RestController
@RequestMapping("/v1/employees")
class EmployeeController {

    private final List<String> employeeNames;
    private Random random;

    @Autowired
    private Tracer tracer;

    public EmployeeController() throws IOException {
        InputStream inputStream = new ClassPathResource("/employees.txt").getInputStream();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            employeeNames = reader.lines().collect(Collectors.toList());
        }
        random = new Random();
    }

    @GetMapping(path = "/random")
    public String findRandomEmployee(@RequestHeader HttpHeaders headers) {

        // Get parent span context from request header
        // SpanContext parentSpan = tracer.extract(Format.Builtin.HTTP_HEADERS,
        // new TextMapExtractAdapter(headers.toSingleValueMap()));
        SpanContext parentSpan = tracer.extract(Format.Builtin.HTTP_HEADERS,
                new TextMapAdapter(headers.toSingleValueMap()));

        // Build a new span from parent context
        Span span = null;
        if (parentSpan == null) {
            span = tracer.buildSpan("getRandomEmployee").start();
        } else {
            span = tracer.buildSpan("getRandomEmployee").asChildOf(parentSpan).start();
        }

        String name = null;
        try (Scope scope = tracer.scopeManager().activate(span)) {
            // Process here
            name = employeeNames.get(random.nextInt(employeeNames.size()));
            span.setTag("employee", name);
        } catch (Exception ex) {
            Tags.ERROR.set(span, true);
            span.log(Map.of(Fields.EVENT, "error", Fields.ERROR_OBJECT, ex, Fields.MESSAGE, ex.getMessage()));
        } finally {
            span.finish();
        }

        return name;
    }
}
