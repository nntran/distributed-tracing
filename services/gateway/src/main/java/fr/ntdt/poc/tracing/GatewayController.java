package fr.ntdt.poc.tracing;

import java.io.IOException;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.opentracing.Scope;
import io.opentracing.Span;
import io.opentracing.Tracer;
import io.opentracing.log.Fields;
import io.opentracing.propagation.Format;
import io.opentracing.tag.Tags;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

@RestController
@RequestMapping("/v1/names")
class GatewayController {

    private OkHttpClient client = new OkHttpClient();

    @Autowired
    private Tracer tracer;
    // private Tracer tracer = GlobalTracer.get();

    @GetMapping(path = "/random")
    public String generateRandomName(HttpServletRequest request) throws Exception {

        if (tracer == null)
            throw new Exception("tracer is null");

        // Build a new span from parent context
        Span serverSpan = tracer.activeSpan();
        Span parentSpan = tracer.buildSpan("callServices").asChildOf(serverSpan.context()).start();

        String name = null;
        try (Scope scope = tracer.scopeManager().activate(parentSpan)) {

            // Create child span to follow the companie WS
            Span companieSpan = tracer.buildSpan("callCompanieService").asChildOf(parentSpan).start();
            String companieName = httpGet("http://company:8090/v1/companies/random");
            companieSpan.finish();

            // Create child span to follow the employee WS
            Span employeeSpan = tracer.buildSpan("callEmployeeService").asChildOf(parentSpan).start();
            String animal = httpGet("http://employee:8090/v1/employees/random");
            employeeSpan.finish();

            name = companieName + " - " + animal;

            parentSpan.setTag("name", name);

        } catch (Exception ex) {
            Tags.ERROR.set(parentSpan, true);
            parentSpan.log(Map.of(Fields.EVENT, "error", Fields.ERROR_OBJECT, ex, Fields.MESSAGE, ex.getMessage()));
        } finally {
            parentSpan.finish();
        }

        return name;
    }

    private String httpGet(String url) throws IOException {
        System.out.println("Call url: " + url);
        // Request request = new Request.Builder()
        // .url(url)
        // .build();

        // try (Response response = client.newCall(request).execute()) {
        // return response.body().string();
        // }

        // Create a request builder
        Request.Builder requestBuilder = new Request.Builder().url(url);

        // Inject context into request hearder
        tracer.inject(tracer.activeSpan().context(), Format.Builtin.HTTP_HEADERS,
                new RequestBuilderCarrier(requestBuilder));

        // Build request
        Request request = requestBuilder.build();

        // Execute the request
        try (Response response = client.newCall(request).execute()) {
            return response.body().string();
        }
    }
}