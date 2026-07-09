 package io.rosecloud.starter.security.auth.jwt;
 
  import jakarta.servlet.http.HttpServletRequest;
  import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
  import org.springframework.security.web.util.matcher.OrRequestMatcher;
  import org.springframework.security.web.util.matcher.RequestMatcher;
  import org.springframework.util.Assert;

  import java.util.ArrayList;
  import java.util.List;
  import java.util.stream.Collectors;

  public class SkipPathRequestMatcher implements RequestMatcher {

      private final OrRequestMatcher matchers;
      private final RequestMatcher processingMatcher;

      public SkipPathRequestMatcher(List<String> pathsToSkip, String processingPath) {
          this(pathsToSkip, List.of(), processingPath);
      }

      public SkipPathRequestMatcher(List<String> pathsToSkip, List<RequestMatcher> additionalSkips, String processingPath) {
          Assert.notNull(pathsToSkip, "List of paths to skip is required.");
         List<RequestMatcher> m = new ArrayList<>(pathsToSkip.stream()
                 .map(PathPatternRequestMatcher::pathPattern)
                 .collect(Collectors.toList()));
         m.addAll(additionalSkips);
         matchers = new OrRequestMatcher(m);
         processingMatcher = PathPatternRequestMatcher.pathPattern(processingPath);
      }

      @Override
      public boolean matches(HttpServletRequest request) {
          if (matchers.matches(request)) {
              return false;
          }
          return processingMatcher.matches(request);
      }
  }
