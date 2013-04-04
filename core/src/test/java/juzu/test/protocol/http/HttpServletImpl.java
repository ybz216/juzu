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

package juzu.test.protocol.http;

import juzu.impl.bridge.spi.servlet.ServletLogger;
import juzu.impl.common.Logger;
import juzu.impl.plugin.application.ApplicationLifeCycle;
import juzu.impl.asset.AssetManager;
import juzu.impl.asset.AssetServer;
import juzu.impl.common.MethodHandle;
import juzu.impl.plugin.controller.ControllerPlugin;
import juzu.impl.request.Method;
import juzu.request.Phase;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/** @author <a href="mailto:julien.viet@exoplatform.com">Julien Viet</a> */
public class HttpServletImpl extends HttpServlet {


  /** . */
  private ApplicationLifeCycle<?, ?> application;

  /** . */
  private AssetServer assetServer;

  /** . */
  AssetManager scriptManager;

  /** . */
  AssetManager stylesheetManager;

  /** . */
  Logger log = Logger.SYSTEM;

  private RequestBridgeImpl create(HttpServletRequest req, HttpServletResponse resp) {
    Phase phase = Phase.VIEW;
    Map<String, String[]> parameters = new HashMap<String, String[]>();

    //
    String methodId = null;
    for (Map.Entry<String, String[]> entry : ((Map<String, String[]>)req.getParameterMap()).entrySet()) {
      String name = entry.getKey();
      String[] value = entry.getValue();
      if (name.equals("juzu.phase")) {
        phase = Phase.valueOf(value[0]);
      }
      else if (name.equals("juzu.op")) {
        methodId = value[0];
      }
      else {
        parameters.put(name, value);
      }
    }

    //
    MethodHandle method = methodId != null ? application.getPlugin(ControllerPlugin.class).getDescriptor().getMethodById(methodId).getHandle() : null;

    //
    if (method == null) {
      Method descriptor = application.getPlugin(ControllerPlugin.class).getDescriptor().getResolver().resolve(Phase.VIEW, Collections.<String>emptySet());
      method = descriptor != null ? descriptor.getHandle() : null;
    }

    //
    if (phase == Phase.ACTION) {
      return new ActionBridgeImpl(log, application, req, resp, method, parameters);
    } else if (phase == Phase.VIEW) {
      return new RenderBridgeImpl(log, this, application, req, resp, method, parameters);
    } else if (phase == Phase.RESOURCE) {
      return new ResourceBridgeImpl(log, application, req, resp, method, parameters);
    } else {
      throw new AssertionError();
    }
  }

  @Override
  public void init() throws ServletException {
    try {
      ApplicationLifeCycle<?, ?> application = AbstractHttpTestCase.getCurrentApplication();

      //
      application.refresh();

      // Bind the asset managers
      assetServer = new AssetServer();
      scriptManager = application.getScriptManager();
      stylesheetManager = application.getStylesheetManager();

      // Manually register here : a bit ugly
      assetServer.register(application);

      //
      this.application = application;
      this.log = new ServletLogger(getServletConfig());
    }
    catch (Exception e) {
      throw new ServletException(e);
    }
  }

  @Override
  public void destroy() {
    assetServer.unregister(application);
  }

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
    String path = req.getRequestURI().substring(req.getContextPath().length());
    String contentType;
    if (path.endsWith(".js")) {
      contentType = "text/javascript";
    }
    else if (path.endsWith(".css")) {
      contentType = "text/css";
    }
    else {
      contentType = null;
    }
    if (contentType != null) {
      if (!assetServer.doGet(path, getServletContext(), resp)) {
        resp.sendError(404, "Path " + path + " could not be resolved");
      }
    }
    else {
      RequestBridgeImpl requestBridge = create(req, resp);
      try {
        application.getPlugin(ControllerPlugin.class).invoke(requestBridge);
      }
      finally {
        requestBridge.close();
      }
    }
  }
}
