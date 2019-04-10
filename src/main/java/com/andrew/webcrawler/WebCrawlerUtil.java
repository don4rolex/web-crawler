package com.andrew.webcrawler;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static com.andrew.webcrawler.Constants.THREAD_POOL_COUNT;
import static java.util.stream.Collectors.toSet;

/**
 * @author andrew
 */
public class WebCrawlerUtil {

  private static final ExecutorService executor = Executors.newFixedThreadPool(THREAD_POOL_COUNT);
  private static final Set<String> javaScriptLibraries = Stream.of("angular", "aurelia", "backbone", "ember", "express",
      "jquery", "meteor", "node", "polymer", "react", "socket", "vue")
      .collect(toSet());

  private static final Pattern patternDomainName;

  static {
    patternDomainName = Pattern.compile("([a-zA-Z0-9]([a-zA-Z0-9\\-]{0,61}[a-zA-Z0-9])?\\.)+[a-zA-Z]{2,6}");
  }

  private WebCrawlerUtil() {

  }

  public static CompletableFuture<Set<String>> getDataFromGoogle(String searchQuery, int numOfPages, int timeout) {
    final Supplier<Set<String>> task = () -> {
      final Set<String> result = new HashSet<>();

      try {
        final Document doc = Jsoup
            .connect("https://www.google.com/search?q=" + searchQuery + "&num=" + numOfPages)
            .userAgent("Mozilla/5.0 (compatible; Googlebot/2.1; +http://www.google.com/bot.html)")
            .timeout(timeout)
            .get();

        final Elements links = doc.select("a[href]");
        for (Element link : links) {
          final String temp = link.attr("href");
          if (temp.startsWith("/url?q=")) {
            System.out.println(getDomainName(temp));
            result.add(getDomainName(temp));
          }
        }
      } catch (IOException e) {
        e.printStackTrace();
      }

      return result;
    };

    return CompletableFuture.supplyAsync(task, executor);
  }

  public static CompletableFuture<Set<String>> getJavaScriptLibraries(String url, int timeout) {
    final Supplier<Set<String>> task = () -> {
      final Set<String> result = new HashSet<>();
      try {
        final Document doc = Jsoup
            .connect(url)
            .userAgent("Mozilla/5.0 (compatible; Googlebot/2.1; +http://www.google.com/bot.html)")
            .timeout(timeout)
            .get();

        final Elements scripts = doc.getElementsByTag("script");
        for (Element script : scripts) {
          if (script.hasAttr("src")) {
            result.add(getJavascriptLibrary(script.attr("src")));
          }
        }
      } catch (IOException e) {
        e.printStackTrace();
      }

      return result;
    };

    return CompletableFuture.supplyAsync(task, executor);
  }

  public static void shutDownExecutor() {
    executor.shutdown();
  }

  private static String getDomainName(String url) {
    final Matcher matcher = patternDomainName.matcher(url);

    if (matcher.find()) {
      final String domainName = matcher.group(0).toLowerCase().trim();

      if (url.startsWith("/url?q=https://")) {
        return "https://" + domainName;
      }

      if (url.startsWith("/url?q=http://")) {
        return "http://" + domainName;
      }
    }

    return "";
  }

  private static String getJavascriptLibrary(String url) {
    return javaScriptLibraries.stream()
        .filter(javaScriptLibrary -> url.toLowerCase().contains(javaScriptLibrary))
        .findFirst()
        .orElse("others");

  }
}
