/*
 * Copyright (C) 2012 eXo Platform SAS.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package juzu.impl.template.spi.juzu.dialect.gtmpl;

import juzu.impl.template.spi.EmitContext;
import juzu.impl.template.spi.Template;
import juzu.impl.template.spi.TemplateException;
import juzu.impl.template.spi.TemplateStub;
import juzu.impl.template.spi.juzu.DialectTemplateEmitter;
import juzu.impl.template.spi.juzu.DialectTemplateProvider;
import juzu.impl.template.spi.juzu.ast.ASTNode;
import juzu.impl.template.spi.juzu.compiler.EmitPhase;

import java.io.IOException;

/** @author <a href="mailto:julien.viet@exoplatform.com">Julien Viet</a> */
public class GroovyTemplateProvider extends DialectTemplateProvider {

  @Override
  public Class<? extends TemplateStub> getTemplateStubType() {
    return GroovyTemplateStub.class;
  }

  @Override
  protected DialectTemplateEmitter createEmitter() {
    return new GroovyTemplateEmitter();
  }

  @Override
  public String getSourceExtension() {
    return "gtmpl";
  }

  @Override
  public String getTargetExtension() {
    return "groovy";
  }

  @Override
  public final void emit(EmitContext context, Template<ASTNode.Template> template) throws TemplateException, IOException {
    DialectTemplateEmitter emitter = createEmitter();
    EmitPhase tcc = new EmitPhase(context);
    tcc.emit(emitter, template.getModel());
    context.createResource(template.getPath().getRawName(), "groovy", emitter.toString());
  }
}
