/*
 *    Copyright 2024 Canary Prism <canaryprsn@gmail.com>
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package canaryprism.simpleclinews.request;

import com.fasterxml.jackson.core.JsonProcessingException;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static canaryprism.simpleclinews.Main.mapper;

public record NewsRequest(Endpoint endpoint, String apikey, Optional<String> sources, Optional<String> q, int pageSize, String language) {
    
    static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("y-d-M");
    
    @Override
    public String toString() {
        var link = new StringBuilder("https://newsapi.org/v2/");
        
        switch (this.endpoint) {
            case Endpoint.Everything(var from) -> {
                link.append("everything?");
                
                from.ifPresent((instant) -> link
                        .append("from=")
                        .append(instant.atOffset(ZoneOffset.UTC).format(formatter))
                        .append('&'));
            }
            case Endpoint.Headlines(var country, var category) -> {
                link.append("top-headlines?");
                
                country.ifPresent((e) -> link
                        .append("country=")
                        .append(e)
                        .append('&'));
                
                category.ifPresent((e) -> link
                        .append("category=")
                        .append(e)
                        .append('&'));
            }
        }
        
        this.q.ifPresent((e) -> link
                .append("q=")
                .append(e)
                .append('&'));
        
        this.sources.ifPresent((e) -> link
                .append("sources=")
                .append(e)
                .append('&'));
        
        link
                .append("language=")
                .append(language)
                .append("&pageSize=")
                .append(pageSize)
                .append("&apikey=")
                .append(apikey);
        
        return link.toString();
    }
    
    static final HttpClient client = HttpClient.newHttpClient();
    
    public CompletableFuture<News> send() {
        var request = HttpRequest.newBuilder(URI.create(this.toString()))
                .build();
        
        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply((response) -> {
                    if (response.statusCode() == 401)
                        throw new RuntimeException("Apikey is invalid. Please set a valid apikey with 'simple-cli-news -a [apikey from newsapi.org]");
                    
                    try {
                        return mapper.readValue(response.body(), News.class);
                    } catch (JsonProcessingException e) {
                        throw new RuntimeException(e);
                    }
                });
    }
}
