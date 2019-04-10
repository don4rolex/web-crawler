package com.andrew.webcrawler;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.andrew.webcrawler.Constants.NUMBER_OF_PAGES;
import static com.andrew.webcrawler.Constants.TIMEOUT;
import static com.andrew.webcrawler.WebCrawlerUtil.getDataFromGoogle;
import static com.andrew.webcrawler.WebCrawlerUtil.getJavaScriptLibraries;
import static com.andrew.webcrawler.WebCrawlerUtil.shutDownExecutor;
import static java.lang.Long.compare;
import static java.util.stream.Collectors.counting;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;

/**
 * @author andrew
 */
public class Main {

  public static void main(String[] args) throws ExecutionException, InterruptedException {
    if (args.length < 1) {
      throw new IllegalArgumentException("No search query entered");
    }

    final String searchQuery = args[0];
    final CompletableFuture<Set<String>> dateFromGoogleFuture = getDataFromGoogle(searchQuery, NUMBER_OF_PAGES, TIMEOUT);
    final Set<String> pageUrls = dateFromGoogleFuture.get();

    final List<CompletableFuture<Set<String>>> javaScriptLibraries = pageUrls.stream()
        .map(url -> getJavaScriptLibraries(url, TIMEOUT))
        .collect(toList());

    final CompletableFuture<Void> allJavaScriptLibrariesFutures = CompletableFuture.allOf(
        javaScriptLibraries.toArray(new CompletableFuture[javaScriptLibraries.size()])
    );

    final CompletableFuture<List<Set<String>>> javaScriptLibrariesFuture = allJavaScriptLibrariesFutures
        .thenApply(e -> javaScriptLibraries.stream()
        .map(CompletableFuture::join)
        .collect(Collectors.toList()));

    final CompletableFuture<Map<String, Long>> countFuture = javaScriptLibrariesFuture.thenApply(libraries ->
        libraries.stream()
            .flatMap(Set::stream)
            .collect(groupingBy(Function.identity(), counting())));

    countFuture.get().entrySet().stream()
        .sorted((t1, t2) -> compare(t2.getValue(), t1.getValue()))
        .limit(5)
        .forEach(System.out::println);

    shutDownExecutor();
  }
}
