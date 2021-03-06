/*
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
package juzu.plugin.webjars;

/**
 * Specify a webjar by its coordinates (artifactId, version), for example <code>@WebJar("jquery", "2.0.0")</code>.
 *
 * @author Julien Viet
 */
public @interface WebJar {

  /**
   * @return the webjar artifact id
   */
  String value();

  /**
   * @return the optional version override
   */
  String version() default "";

  /**
   * @return true if the version of the webjar asset should be erased
   */
  boolean stripVersion() default false;

}
