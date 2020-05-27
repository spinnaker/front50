/*
 * Copyright 2020 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package com.netflix.spinnaker.front50;

import com.netflix.spinnaker.kork.annotations.NonnullByDefault;
import java.lang.reflect.Field;
import javax.annotation.Nullable;
import lombok.SneakyThrows;

/** Helper class for dealing with untyped data / Groovy migration. */
@NonnullByDefault
public class UntypedUtils {

  @SneakyThrows
  public static Object getProperty(Object obj, String propertyName) {
    Field f = obj.getClass().getDeclaredField(propertyName);
    f.setAccessible(true);
    return f.get(obj);
  }

  @SneakyThrows
  public static void setProperty(Object obj, String propertyName, @Nullable Object value) {
    Field f = obj.getClass().getDeclaredField(propertyName);
    f.setAccessible(true);
    f.set(obj, value);
  }
}
