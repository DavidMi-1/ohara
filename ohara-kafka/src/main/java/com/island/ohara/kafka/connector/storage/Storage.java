/*
 * Copyright 2019 is-land
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

package com.island.ohara.kafka.connector.storage;

import com.island.ohara.common.exception.OharaException;
import com.island.ohara.common.exception.OharaFileAlreadyExistsException;
import com.island.ohara.common.util.Releasable;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.Iterator;

/**
 * Interface to distributed storage.
 *
 * <p>Depending on the storage implementation, an object corresponds to a file or a directory in a
 * distributed filesystem or an object in an object store. Similarly, a path corresponds to an
 * actual path in a distributed filesystem or a lookup key in an object store.
 */
public interface Storage extends Releasable {

  /**
   * Returns whether an object exists.
   *
   * @param path the path to the object.
   * @return true if object exists, false otherwise.
   */
  boolean exists(String path);

  /**
   * List the contents of the storage at a given path.
   *
   * <p>WARNING: We only want to use {@link Path} to assist us in representing a system dependent
   * file path. Please avoid using methods related to file operations. e.g. {@link Path#toFile()}
   *
   * @throws OharaException if the path does not exist.
   * @param path the path.
   * @return the listing of the contents.
   */
  Iterator<Path> list(String path);

  /**
   * Creates a new object in the given path.
   *
   * @param path the path of the object to be created.
   * @throws OharaFileAlreadyExistsException if a object of that path already exists.
   * @throws OharaException if the parent container does not exist.
   * @return an output stream associated with the new object.
   */
  OutputStream create(String path);

  /**
   * Append data to an existing object at the given path (optional operation).
   *
   * @param path the path of the object to be appended.
   * @throws OharaException if the path does not exist.
   * @return an output stream associated with the existing object.
   */
  OutputStream append(String path);

  /**
   * Open for reading an object at the given path.
   *
   * @param path the path of the object to be read.
   * @return an input stream with the requested object.
   */
  InputStream open(String path);

  /**
   * Delete the given object or container.
   *
   * <p>NOTED: Do nothing if the path does not exist.
   *
   * @param path path the path to the object or container to delete.
   */
  void delete(String path);

  /**
   * Move or rename a object from source path to target path.
   *
   * @param sourcePath the path to the object to move
   * @param targetPath the path to the target object
   * @return true if object have moved to target path , false otherwise.
   */
  boolean move(String sourcePath, String targetPath);

  /**
   * creates container, including any necessary but nonexistent parent containers
   *
   * @param path container path
   */
  void mkdirs(String path);

  /** Stop using this storage. */
  void close();
}
