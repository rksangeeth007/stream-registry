/**
 * Copyright (C) 2018-2020 Expedia, Inc.
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
package com.expediagroup.streamplatform.streamregistry.repository.postgres;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;

import java.util.List;
import java.util.Optional;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Example;
import org.springframework.stereotype.Component;

import com.expediagroup.streamplatform.streamregistry.model.Producer;
import com.expediagroup.streamplatform.streamregistry.model.keys.ProducerKey;
import com.expediagroup.streamplatform.streamregistry.repository.postgres.data.DataToModel;
import com.expediagroup.streamplatform.streamregistry.repository.postgres.data.ModelToData;
import com.expediagroup.streamplatform.streamregistry.repository.postgres.jpa.ProducerJpaRepository;

@Component
@RequiredArgsConstructor
public class ProducerRepository implements com.expediagroup.streamplatform.streamregistry.repository.ProducerRepository {
  private final ModelToData modelToData;
  private final DataToModel dataToModel;
  private final ProducerJpaRepository delegate;

  @Override
  public Producer save(Producer entity) {
    return Optional.of(entity)
        .map(modelToData::convertToData)
        .map(delegate::save)
        .map(dataToModel::convertToModel)
        .orElse(null);
  }

  @Override
  public Optional<Producer> findById(ProducerKey key) {
    return Optional.of(key)
        .map(modelToData::convertToData)
        .flatMap(delegate::findById)
        .map(dataToModel::convertToModel);
  }

  @Override
  public List<Producer> findAll() {
    return delegate.findAll().stream()
        .map(dataToModel::convertToModel)
        .collect(toList());
  }

  @Override
  public List<Producer> findAll(Producer example) {
    return Optional.of(example)
        .map(modelToData::convertToData)
        .map(Example::of)
        .map(delegate::findAll)
        .orElse(emptyList())
        .stream()
        .map(dataToModel::convertToModel)
        .collect(toList());
  }
}
