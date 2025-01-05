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
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static canaryprism.simpleclinews.Main.mapper;

public record SourcesRequest(String apikey, Optional<String> country) {
    
    @Override
    public String toString() {
        var link = new StringBuilder("https://newsapi.org/v2/top-headlines/sources?");
        
        link
                .append("apikey=")
                .append(apikey);
        
        country.ifPresent((e) -> link
                .append("&country=")
                .append(e));
        
        return link.toString();
    }
    
    static final HttpClient client = HttpClient.newHttpClient();
    
    public CompletableFuture<Sources> send() {
        var request = HttpRequest.newBuilder(URI.create(this.toString()))
                .build();
        
        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply((response) -> {
                    if (response.statusCode() == 401)
                        throw new RuntimeException("Apikey is invalid. Please set a valid apikey with 'simple-cli-news -a [apikey from newsapi.org]");
                    
                    try {
                        return mapper.readValue(response.body(), Sources.class);
                    } catch (JsonProcessingException e) {
                        throw new RuntimeException(e);
                    }
                });
    }
}
