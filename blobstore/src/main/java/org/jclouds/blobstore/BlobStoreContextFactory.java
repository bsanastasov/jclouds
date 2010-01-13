/**
 *
 * Copyright (C) 2009 Cloud Conscious, LLC. <info@cloudconscious.com>
 *
 * ====================================================================
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ====================================================================
 */
package org.jclouds.blobstore;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.IOException;
import java.net.URI;
import java.util.Properties;

import javax.inject.Inject;

import org.jclouds.domain.Credentials;
import org.jclouds.http.HttpPropertiesBuilder;

import com.google.common.io.Resources;
import com.google.inject.Module;

/**
 * 
 * @author Adrian Cole
 */
public class BlobStoreContextFactory {
   private final Properties properties;

   public BlobStoreContextFactory() throws IOException {
      this(init());
   }

   static Properties init() throws IOException {
      Properties properties = new Properties();
      properties.load(Resources.newInputStreamSupplier(
               Resources.getResource("blobstore.properties")).getInput());
      return properties;
   }

   @Inject
   public BlobStoreContextFactory(Properties properties) {
      this.properties = properties;
   }

   public BlobStoreContext<?, ?> createContext(URI blobStore, Module... modules) {
      return createContext(blobStore, Credentials.parse(blobStore), modules);
   }

   public BlobStoreContext<?, ?> createContext(URI blobStore, Credentials creds, Module... modules) {
      return createContext(checkNotNull(blobStore.getHost(), "host"), checkNotNull(creds.account,
               "account"), creds.key, modules);
   }

   @SuppressWarnings("unchecked")
   public BlobStoreContext<?, ?> createContext(String hint, String account, String key,
            Module... modules) {
      checkNotNull(hint, "hint");
      checkNotNull(account, "account");
      String propertiesBuilderKey = String.format("%s.propertiesbuilder", hint);
      String propertiesBuilderClassName = checkNotNull(
               properties.getProperty(propertiesBuilderKey), hint + " service not supported");

      String contextBuilderKey = String.format("%s.contextbuilder", hint);
      String contextBuilderClassName = checkNotNull(properties.getProperty(contextBuilderKey),
               contextBuilderKey);

      try {
         Class<HttpPropertiesBuilder> propertiesBuilderClass = (Class<HttpPropertiesBuilder>) Class
                  .forName(propertiesBuilderClassName);
         Class<BlobStoreContextBuilder<?, ?>> contextBuilderClass = (Class<BlobStoreContextBuilder<?, ?>>) Class
                  .forName(contextBuilderClassName);

         HttpPropertiesBuilder builder = propertiesBuilderClass.getConstructor(String.class,
                  String.class).newInstance(account, key);
         return contextBuilderClass.getConstructor(Properties.class).newInstance(builder.build())
                  .withModules(modules).buildContext();
      } catch (Exception e) {
         throw new RuntimeException("error instantiating " + contextBuilderClassName, e);
      }
   }
}
