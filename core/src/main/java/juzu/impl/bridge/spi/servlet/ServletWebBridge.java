/*
 * Copyright 2013 eXo Platform SAS
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package juzu.impl.bridge.spi.servlet;

import juzu.Method;
import juzu.asset.AssetLocation;
import juzu.impl.bridge.spi.web.WebBridge;
import juzu.impl.common.Logger;
import juzu.impl.common.Tools;
import juzu.impl.inject.ScopedContext;
import juzu.request.ClientContext;
import juzu.request.HttpContext;
import juzu.request.UserContext;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Writer;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;

/** @author <a href="mailto:julien.viet@exoplatform.com">Julien Viet</a> */
public class ServletWebBridge extends WebBridge implements HttpContext, ClientContext, UserContext {

  /** . */
  private final HttpServletRequest req;

  /** . */
  private final HttpServletResponse resp;

  /** . */
  private final String requestPath;

  /** . */
  private final String path;

  /** . */
  private final Method method;

  /** . */
  private final Logger log;

  public ServletWebBridge(HttpServletRequest req, HttpServletResponse resp, String path, Logger log) {
    this.req = req;
    this.resp = resp;
    this.requestPath = req.getRequestURI().substring(req.getContextPath().length());
    this.path = path;
    this.method = Method.valueOf(req.getMethod());
    this.log = log;
  }

  public HttpServletRequest getRequest() {
    return req;
  }

  public HttpServletResponse getResponse() {
    return resp;
  }

  // HttpBridge implementation

  public Map<String, String[]> getParameters() {
    return req.getParameterMap();
  }

  public String getRequestPath() {
    return requestPath;
  }

  public String getPath() {
    return path;
  }

  public String getRequestURI() {
    return req.getRequestURI();
  }

  public void renderRequestURL(Appendable appendable) throws IOException {
    appendable.append(req.getScheme());
    appendable.append("://");
    appendable.append(req.getServerName());
    int port = req.getServerPort();
    if (port != 80) {
      appendable.append(':').append(Integer.toString(port));
    }
    appendable.append(req.getContextPath());
    appendable.append(path);
  }

  public void renderAssetURL(AssetLocation location, String uri, Appendable appendable) throws IOException {
    switch (location) {
      case SERVER:
        if (!uri.startsWith("/")) {
          appendable.append(req.getContextPath());
          appendable.append('/');
        }
        appendable.append(uri);
        break;
      case APPLICATION:
        appendable.append(req.getContextPath()).append("/assets");
        if (!uri.startsWith("/")) {
          appendable.append('/');
        }
        appendable.append(uri);
        break;
      case URL:
        appendable.append(uri);
        break;
      default:
        throw new AssertionError();
    }
  }

  public ClientContext getClientContext() {
    return this;
  }

  public HttpContext getHttpContext() {
    return this;
  }

  public UserContext getUserContext() {
    return this;
  }

  public ScopedContext getRequestScope(boolean create) {
    ScopedContext context = (ScopedContext)req.getAttribute("juzu.request_scope");
    if (context == null && create) {
      req.setAttribute("juzu.request_scope", context = new ScopedContext(log));
    }
    return context;
  }

  public ScopedContext getFlashScope(boolean create) {
    ScopedContext context = null;
    HttpSession session = req.getSession(create);
    if (session != null) {
      context = (ScopedContext)session.getAttribute("juzu.flash_scope");
      if (context == null && create) {
        session.setAttribute("juzu.flash_scope", context = new ScopedContext(log));
      }
    }
    return context;
  }

  public ScopedContext getSessionScope(boolean create) {
    ScopedContext context = null;
    HttpSession session = req.getSession(create);
    if (session != null) {
      context = (ScopedContext)session.getAttribute("juzu.session_scope");
      if (context == null && create) {
        session.setAttribute("juzu.session_scope", context = new ScopedContext(log));
      }
    }
    return context;
  }

  public void purgeSession() {
    HttpSession session = req.getSession(false);
    if (session != null) {
      for (String key : Tools.list(session.getAttributeNames())) {
        session.removeAttribute(key);
      }
    }
  }

  public void setHeader(String name, String value) {
    resp.setHeader(name, value);
  }

  public void sendRedirect(String location) throws IOException {
    resp.sendRedirect(location);
  }

  public void setContentType(String contentType) {
    resp.setContentType(contentType);
  }

  public void setStatus(int status) {
    resp.setStatus(status);
  }

  public Writer getWriter() throws IOException {
    return resp.getWriter();
  }

  public OutputStream getOutputStream() throws IOException {
    return resp.getOutputStream();
  }

// ClientContext implementation

  public String getContentType() {
    return req.getContentType();
  }

  public String getCharacterEncoding() {
    return req.getCharacterEncoding();
  }

  public int getContentLenth() {
    return req.getContentLength();
  }

  public InputStream getInputStream() throws IOException {
    return req.getInputStream();
  }

  // HttpContext implementation

  public Method getMethod() {
    return method;
  }

  public Cookie[] getCookies() {
    return req.getCookies();
  }

  public String getScheme() {
    return req.getScheme();
  }

  public int getServerPort() {
    return req.getServerPort();
  }

  public String getServerName() {
    return req.getServerName();
  }

  public String getContextPath() {
    return req.getContextPath();
  }

  // UserContext implementation

  public Locale getLocale() {
    return req.getLocale();
  }

  public Iterable<Locale> getLocales() {
    return new Iterable<Locale>() {
      public Iterator<Locale> iterator() {
        return new Iterator<Locale>() {
          Enumeration<Locale> e = req.getLocales();
          public boolean hasNext() {
            return e.hasMoreElements();
          }
          public Locale next() {
            return e.nextElement();
          }
          public void remove() {
            throw new UnsupportedOperationException();
          }
        };
      }
    };
  }
}
