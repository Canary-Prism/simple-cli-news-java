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

package canaryprism.simpleclinews.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static canaryprism.simpleclinews.Main.mapper;

public class NewsConfig {
    
    private String apikey;
    private String language;
    
    public String getApikey() {
        return apikey;
    }
    
    public NewsConfig setApikey(String apikey) {
        this.apikey = apikey;
        return this;
    }
    
    public String getLanguage() {
        return language;
    }
    
    public NewsConfig setLanguage(String language) {
        this.language = language;
        return this;
    }
    
    public NewsConfig(String apikey, String language) {
        this.apikey = apikey;
        this.language = language;
    }
    
    public NewsConfig() {
        this("", "en");
    }
    
    public static Optional<NewsConfig> read(Path path) {
        try {
            var config = mapper.readValue(Files.readString(path), NewsConfig.class);
            return Optional.of(config);
        } catch (IOException e) {
            return Optional.empty();
        }
    }

    
    public void write(Path path) throws IOException {
        Files.createDirectories(path.getParent());
        Files.writeString(path, mapper.writeValueAsString(this));
    }
    
}
