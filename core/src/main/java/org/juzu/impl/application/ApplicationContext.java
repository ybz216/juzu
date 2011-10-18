package org.juzu.impl.application;

import org.juzu.AmbiguousResolutionException;
import org.juzu.MimeScoped;
import org.juzu.Path;
import org.juzu.application.ApplicationDescriptor;
import org.juzu.impl.cdi.Export;
import org.juzu.impl.cdi.ScopeController;
import org.juzu.impl.request.ControllerParameter;
import org.juzu.impl.request.MimeContext;
import org.juzu.impl.spi.cdi.Container;
import org.juzu.impl.request.ControllerMethod;
import org.juzu.impl.request.RequestContext;
import org.juzu.impl.spi.template.TemplateStub;
import org.juzu.template.Template;
import org.juzu.text.Printer;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.inject.Singleton;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** @author <a href="mailto:julien.viet@exoplatform.com">Julien Viet</a> */
@Export
@Singleton
public class ApplicationContext
{

   public static RequestContext getCurrentRequest()
   {
      return current.get();
   }

   /** . */
   private final ApplicationDescriptor descriptor;

   /** . */
   private final Container container;

   /** . */
   private final ControllerResolver controllerResolver;

   /** . */
   private static final ThreadLocal<RequestContext> current = new ThreadLocal<RequestContext>();

   public ApplicationContext()
   {
      Bootstrap bootstrap = Bootstrap.foo.get();

      //
      this.descriptor = bootstrap.descriptor;
      this.container = bootstrap.container;
      this.controllerResolver = new ControllerResolver(bootstrap.descriptor);
   }

   public ApplicationDescriptor getDescriptor()
   {
      return descriptor;
   }

   public void invoke(RequestContext context)
   {
      ClassLoader oldCL = Thread.currentThread().getContextClassLoader();
      try
      {
         Thread.currentThread().setContextClassLoader(context.getClassLoader());
         current.set(context);
         ScopeController.begin(context);
         doInvoke(context);
      }
      finally
      {
         current.set(null);
         ScopeController.end();
         Thread.currentThread().setContextClassLoader(oldCL);
      }
   }

   public Object resolveBean(String name)
   {
      BeanManager mgr = container.getManager();
      Set<Bean<?>> beans = mgr.getBeans(name);
      switch (beans.size())
      {
         case 0:
            return null;
         case 1:
            Bean<?> bean = beans.iterator().next();
            CreationalContext<?> cc = mgr.createCreationalContext(bean);
            return mgr.getReference(bean, bean.getBeanClass(), cc);
         default:
            throw new AmbiguousResolutionException();
      }
   }

   private void doInvoke(RequestContext<?> context)
   {
      ControllerMethod method = controllerResolver.resolve(context.getPhase(), context.getParameters());

      //
      if (method == null)
      {
         StringBuilder sb = new StringBuilder("handle me gracefully : no method could be resolved for " +
            "phase=" + context.getPhase() + " and parameters={");
         int index = 0;
         for (Map.Entry<String, String[]> entry : context.getParameters().entrySet())
         {
            if (index++ > 0)
            {
               sb.append(',');
            }
            sb.append(entry.getKey()).append('=').append(Arrays.asList(entry.getValue()));
         }
         sb.append("}");
         throw new UnsupportedOperationException(sb.toString());
      }
      else
      {
         Class<?> type = method.getType();
         BeanManager mgr = container.getManager();
         Set<? extends Bean> beans = mgr.getBeans(type);

         if (beans.size() == 1)
         {
            CreationalContext<?> cc = null;
            try
            {
               // Get the bean
               Bean bean = beans.iterator().next();
               cc = mgr.createCreationalContext(bean);

               // Get a reference
               Object o = mgr.getReference(bean, type, cc);

               // Prepare method parameters
               List<ControllerParameter> params = method.getArgumentParameters();
               Object[] args = new Object[params.size()];
               for (int i = 0;i < args.length;i++)
               {
                  String[] values = context.getParameters().get(params.get(i).getName());
                  args[i] = (values != null && values.length > 0) ? values[0] : null;
               }

               //
               method.getMethod().invoke(o, args);
            }
            catch (Exception e)
            {
               throw new UnsupportedOperationException("handle me gracefully", e);
            }
            finally
            {
               if (cc != null)
               {
                  cc.release();
               }
            }
         }
      }
   }

   @Produces
   @MimeScoped
   public Printer getPrinter()
   {
      RequestContext context = current.get();
      if (context instanceof MimeContext)
      {
         return ((MimeContext)context).getPrinter();
      }
      else
      {
         throw new AssertionError("does not make sense");
      }
   }

   public TemplateStub resolveTemplateStub(String path)
   {
      try
      {
         StringBuilder id = new StringBuilder(descriptor.getTemplatesPackageName());
         if (id.length() > 0)
         {
            id.append('.');
         }
         id.append(path, 0, path.indexOf('.'));
         ClassLoader cl = Thread.currentThread().getContextClassLoader();
         Class<?> stubClass = cl.loadClass(id.toString());
         return(TemplateStub)stubClass.newInstance();
      }
      catch (Exception e)
      {
         throw new UnsupportedOperationException("handle me gracefully");
      }
   }

   @Produces
   public Template resolveTemplate(InjectionPoint point)
   {
      Path path = point.getAnnotated().getAnnotation(Path.class);
      return new Template(this, path.value());
   }
}