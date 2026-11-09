package com.example.legacy.web;

import com.example.legacy.service.AccountAccessService;
import com.example.legacy.service.dto.AccountDto;
import com.example.legacy.util.MessageKeys;
import com.example.legacy.util.MessagesUtil;
import java.io.IOException;
import java.security.MessageDigest;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * Basic login and CSRF enforcement, intentionally separate from legacy business code.
 *
 * @author Chuck Emmons
 */
public final class SecurityFilter implements Filter {
  private final AccountAccessService accountAccessService;

  /** Creates lazy production account-access services for the container-managed filter. */
  public SecurityFilter() {
    this(new AccountAccessService());
  }

  /**
   * Accepts the account-access collaborator explicitly for filter tests.
   *
   * @param accountAccessService the account access service collaborator
   * @throws NullPointerException if {@code accountAccessService} is {@code null}
   */
  public SecurityFilter(AccountAccessService accountAccessService) {
    this.accountAccessService =
        java.util.Objects.requireNonNull(accountAccessService, "accountAccessService");
  }

  /**
   * Accepts servlet-container initialization; no additional filter setup is required.
   *
   * @param configuration the configuration to update
   */
  @Override
  public void init(FilterConfig configuration) {}

  /** Handles filter shutdown; this filter owns no resources requiring cleanup. */
  @Override
  public void destroy() {}

  /**
   * Adds security headers and a CSRF token, redirects unauthenticated users, refreshes
   * active-account roles, and rejects invalid tokens or non-POST mutations. Multipart upload tokens
   * are checked by DocumentAction.
   *
   * @param servletRequest the incoming servlet request
   * @param servletResponse the outgoing servlet response
   * @param chain the remaining servlet filter chain
   * @throws IOException if an I/O operation fails
   * @throws ServletException if servlet initialization or request processing fails
   * @throws com.example.legacy.service.exception.ConcurrentUpdateException if submitted data is
   *     stale or Hibernate detects an optimistic-lock conflict
   * @throws IllegalArgumentException if a message pattern or formatting argument is invalid
   * @throws NullPointerException if {@code chain} is {@code null}
   * @throws RuntimeException if transaction callback execution, transaction completion, or session
   *     cleanup fails
   * @throws java.util.MissingResourceException if a required message bundle or message key cannot
   *     be found
   * @throws org.hibernate.HibernateException if a database query, persistence operation, or
   *     Hibernate session or transaction operation fails
   */
  @Override
  public void doFilter(
      ServletRequest servletRequest, ServletResponse servletResponse, FilterChain chain)
      throws IOException, ServletException {
    HttpServletRequest request = (HttpServletRequest) servletRequest;
    HttpServletResponse response = (HttpServletResponse) servletResponse;
    response.setHeader(HttpConstants.CONTENT_TYPE_OPTIONS, HttpConstants.NO_SNIFF);
    response.setHeader(HttpConstants.FRAME_OPTIONS, HttpConstants.DENY);
    response.setHeader(HttpConstants.CACHE_CONTROL, HttpConstants.NO_STORE);
    HttpSession session = request.getSession(true);
    if (session.getAttribute(WebKeys.CSRF) == null) {
      session.setAttribute(WebKeys.CSRF, WebUtil.generateCsrfToken());
    }
    String url = request.getServletPath();
    boolean publicPath = Routes.LOGIN.equals(url) || Routes.AUTHENTICATE.equals(url);
    if (!publicPath && session.getAttribute(WebKeys.ACTOR) == null) {
      response.sendRedirect(request.getContextPath() + Routes.LOGIN);
      return;
    }
    if (!publicPath) {
      try {
        final String username = (String) session.getAttribute(WebKeys.ACTOR);
        AccountDto account = accountAccessService.loadActiveAccount(username);
        session.setAttribute(WebKeys.ROLE, account.getRole());
        session.setAttribute(WebKeys.DISPLAY_NAME, account.getDisplayName());
      } catch (com.example.legacy.service.exception.AccessDeniedException failure) {
        session.invalidate();
        response.sendRedirect(request.getContextPath() + Routes.LOGIN);
        return;
      }
    }
    if (HttpConstants.POST.equalsIgnoreCase(request.getMethod())
        && !Routes.CASE_UPLOAD.equals(url)) {
      String submitted = request.getParameter(WebKeys.CSRF);
      Object original = session.getAttribute(WebKeys.CSRF);
      if (submitted == null
          || original == null
          || !MessageDigest.isEqual(
              submitted.getBytes(java.nio.charset.StandardCharsets.UTF_8.name()),
              original.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8.name()))) {
        response.sendError(
            HttpServletResponse.SC_FORBIDDEN,
            MessagesUtil.formatMessage(request.getLocale(), MessageKeys.SECURITY_INVALID_CSRF));
        return;
      }
    }
    // Do not permit GET to perform mutations.
    if ((Routes.AUTHENTICATE.equals(url)
            || Routes.LOGOUT.equals(url)
            || Routes.CASE_CREATE.equals(url)
            || Routes.CASE_UPDATE.equals(url)
            || Routes.CASE_ASSIGN.equals(url)
            || Routes.CASE_TRANSITION.equals(url)
            || Routes.CASE_COMMENT.equals(url)
            || Routes.CASE_ARCHIVE.equals(url)
            || Routes.CASE_UPLOAD.equals(url)
            || Routes.USER_CREATE.equals(url)
            || Routes.USER_ROLE.equals(url)
            || Routes.USER_DISABLE.equals(url)
            || Routes.NOTIFICATION_READ.equals(url))
        && !HttpConstants.POST.equalsIgnoreCase(request.getMethod())) {
      response.sendError(
          HttpServletResponse.SC_METHOD_NOT_ALLOWED,
          MessagesUtil.formatMessage(request.getLocale(), MessageKeys.SECURITY_POST_REQUIRED));
      return;
    }
    chain.doFilter(servletRequest, servletResponse);
  }
}
