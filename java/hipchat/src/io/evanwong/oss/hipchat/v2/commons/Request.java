package io.evanwong.oss.hipchat.v2.commons;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

public abstract class Request<T> {

    protected static final String BASE_URL = "https://api.hipchat.com/v2";

    private static final Logger LOGGER = LoggerFactory.getLogger(Request.class);

    private final ObjectMapper objectMapper = new ObjectMapper();

    protected final ObjectWriter objectWriter = objectMapper.writer();
    protected final ObjectReader objectReader = objectMapper.reader(getParameterClass());

    protected ExecutorService executorService;
    protected String accessToken;
    protected HttpClient httpClient;

    protected Request(String accessToken, HttpClient httpClient, ExecutorService executorService) {
        this.executorService = executorService;
        this.accessToken = accessToken;
        this.httpClient = httpClient;
    }

    protected abstract Map<String, Object> toQueryMap();

    protected abstract HttpResponse request() throws IOException;

    protected abstract String getPath();

    public Future<T> execute() {
        return executorService.submit(() -> {
            HttpResponse response = request();
            int status = response.getStatusLine().getStatusCode();
            HttpEntity entity = response.getEntity();
            String content = entity != null ? EntityUtils.toString(entity) : null;
            if (status >= 200 && status < 300) {
                if (content == null) {
                    //should be NoContent
                    return getParameterClass().newInstance();
                }
                return objectReader.readValue(content);
            } else {
                LOGGER.error("Invalid response status: {}, content: {}", status, content);
                return null;
            }
        });
    }

    protected Class<T> getParameterClass() {
        return (Class<T>) (((ParameterizedType) this.getClass().getGenericSuperclass()).getActualTypeArguments()[0]);
    }
}
