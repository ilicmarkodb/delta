/*
 * Copyright (2023) The Delta Lake Project Authors.
 *
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
 */
package io.delta.kernel.types;

import io.delta.kernel.annotation.Evolving;

/**
 * The data type representing {@code string} type values.
 *
 * @since 3.0.0
 */
@Evolving
public class StringType extends BasePrimitiveType {
  public static final StringType STRING = new StringType("UTF8_BINARY");

  public StringType(String collationName) {
    super("string");
    this.collationName = collationName;
  }

  public String getCollationName() {
    return collationName;
  }

  private final String collationName;
}
