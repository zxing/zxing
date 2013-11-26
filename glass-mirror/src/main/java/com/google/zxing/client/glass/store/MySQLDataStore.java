/*
 * Copyright 2013 ZXing authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.zxing.client.glass.store;

import com.google.api.client.auth.oauth2.StoredCredential;
import com.google.api.client.util.store.AbstractDataStore;
import com.google.api.client.util.store.DataStore;
import com.google.api.client.util.store.DataStoreFactory;

import java.io.IOException;
import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;

final class MySQLDataStore<V extends Serializable> extends AbstractDataStore<V> {

  MySQLDataStore(DataStoreFactory dataStoreFactory, String id) {
    super(dataStoreFactory, id);
  }

  @Override
  public Set<String> keySet() {
    return Collections.singleton(StoredCredential.DEFAULT_DATA_STORE_ID);
  }

  @Override
  public Collection<V> values() throws IOException {
    return null; // TODO
  }

  @Override
  public V get(String key) throws IOException {
    return null; // TODO
  }

  @Override
  public DataStore<V> set(String key, V value) throws IOException {
    return null; // TODO
  }

  @Override
  public DataStore<V> clear() throws IOException {
    return null; // TODO
  }

  @Override
  public DataStore<V> delete(String key) throws IOException {
    return null; // TODO
  }

}
