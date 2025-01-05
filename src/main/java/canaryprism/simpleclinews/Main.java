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

package canaryprism.simpleclinews;

import canaryprism.simpleclinews.config.NewsConfig;
import canaryprism.simpleclinews.request.Endpoint;
import canaryprism.simpleclinews.request.NewsRequest;
import canaryprism.simpleclinews.request.SourcesRequest;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import dev.dirs.ProjectDirectories;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Help.Ansi;
import picocli.CommandLine.Option;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

@Command(name = "simple-cli-news-java", subcommands = Main.List.class, version = Main.VERSION)
public class Main implements Runnable {
    
    public static final String VERSION = "1.0.0";
    
    private static final ProjectDirectories dirs = ProjectDirectories.from("", "canaryprism", "simple-cli-news-java");
    
    public static final ObjectMapper mapper = new ObjectMapper()
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .registerModule(new Jdk8Module());
    
    private static final Path config_path = Path.of(dirs.configDir, "config.json");
    private static final NewsConfig config = NewsConfig.read(config_path).orElse(new NewsConfig());
    
    
    @Option(
            names = { "--apikey", "-a", "--key" },
            description = "Update the config with an api key from NewsApi"
    )
    private Optional<String> apikey;
    
    @Option(
            names = { "--query", "-q", "--search" },
            description = "Search news from the past 14 days"
    )
    private Optional<String> query;
    
    @Option(
            names = { "--source", "-s" },
            description = "Get news form a certain source with its ID"
    )
    private Optional<String> source;
    
    @Option(
            names = { "--page-size", "-p", "--pagesize", "--pgsize", "--pg-size", "--page_size" },
            description = "Set how many articles should be displayed"
    )
    private Optional<Integer> page_size;
    
    @Option(
            names = { "--language", "-l", "--lang" },
            description = "Set the default language"
    )
    private Optional<String> language;
    
    
    
    @Override
    public void run() {

        if (apikey.isPresent()) {
            try {
                config.setApikey(apikey.get())
                        .write(config_path);
            } catch (IOException e) {
                throw new RuntimeException("Couldn't write apikey", e);
            }
            return;
        }
        if (language.isPresent()) {
            try {
                config.setLanguage(language.get())
                        .write(config_path);
            } catch (IOException e) {
                throw new RuntimeException("Couldn't write apikey", e);
            }
            return;
        }
        
        Endpoint endpoint;
        if (query.isPresent() || source.isPresent())
            endpoint = new Endpoint.Everything(Optional.of(Instant.now().minus(Duration.ofDays(10))));
        else
            endpoint = new Endpoint.Headlines(Optional.empty(), Optional.empty());
        
        var request = new NewsRequest(
                endpoint,
                config.getApikey(),
                source,
                query,
                page_size.orElse(20),
                config.getLanguage()
        );
        
        var articles = request.send().join().articles();
        
        if (!articles.isEmpty()) {
            for (var article : articles) {
                System.out.println();
                System.out.println(Ansi.AUTO.string(String.format("@|yellow %s|@", article.title())));
                System.out.println(Ansi.AUTO.string(String.format("@|blue >>> %s|@", article.url())));
            }
        } else {
            System.out.println(Ansi.AUTO.string("@|red Failed to find any articles|@"));
        }
    }
    
    @Command(name = "list", description = "List possible args for various commands", subcommands = List.Sources.class)
    static class List {
    
        @Command(name = "sources", description = "List possible sources")
        static class Sources implements Runnable {
            
            @Option(
                    names = { "--country", "-c", "--location" },
                    description = "List sources from a country using its 2-Digit ISO code"
            )
            private Optional<String> country;
            
            @Override
            public void run() {
                var sources = new SourcesRequest(config.getApikey(), country)
                        .send()
                        .join()
                        .sources();
                
                if (!sources.isEmpty()) {
                    for (var source : sources) {
                        System.out.println(Ansi.AUTO.string(String.format("@|green,bold %s|@", source.name())));
                        System.out.println(Ansi.AUTO.string(String.format("@|yellow %s|@", source.description())));
                        System.out.println(Ansi.AUTO.string(String.format("@|yellow %s|@", source.id())));
                        System.out.println();
                    }
                } else {
                    System.out.println(Ansi.AUTO.string("@|red Failed to find any sources|@"));
                }
            }
        }
    }
    
    
    @Option(
            names = { "--version", "-v" },
            description = "Prints version information and then exits",
            versionHelp = true
    )
    private boolean version;
    
    @Option(
            names = { "--help", "-h" },
            description = "Prints usage information and then exits",
            usageHelp = true
    )
    private boolean usage;
    
    public static void main(String[] args) {
        System.exit(new CommandLine(new Main()).execute(args));
    }
}