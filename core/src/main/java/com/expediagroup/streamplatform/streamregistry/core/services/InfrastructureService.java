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
package com.expediagroup.streamplatform.streamregistry.core.services;

import static com.expediagroup.streamplatform.streamregistry.core.events.EventType.CREATE;
import static com.expediagroup.streamplatform.streamregistry.core.events.EventType.UPDATE;
import static java.util.stream.Collectors.toList;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Component;

import com.expediagroup.streamplatform.streamregistry.core.events.EventType;
import com.expediagroup.streamplatform.streamregistry.core.events.NotificationEventEmitter;
import com.expediagroup.streamplatform.streamregistry.core.handlers.HandlerService;
import com.expediagroup.streamplatform.streamregistry.core.validators.InfrastructureValidator;
import com.expediagroup.streamplatform.streamregistry.core.validators.ValidationException;
import com.expediagroup.streamplatform.streamregistry.model.Infrastructure;
import com.expediagroup.streamplatform.streamregistry.model.keys.InfrastructureKey;
import com.expediagroup.streamplatform.streamregistry.repository.InfrastructureRepository;

@Component
@RequiredArgsConstructor
public class InfrastructureService {
  private final HandlerService handlerService;
  private final InfrastructureValidator infrastructureValidator;
  private final InfrastructureRepository infrastructureRepository;
  private final NotificationEventEmitter<Infrastructure> infrastructureServiceEventEmitter;

  public Optional<Infrastructure> create(Infrastructure infrastructure) throws ValidationException {
    if (read(infrastructure.getKey()).isPresent()) {
      throw new ValidationException("Can't create because it already exists");
    }
    infrastructureValidator.validateForCreate(infrastructure);
    infrastructure.setSpecification(handlerService.handleInsert(infrastructure));
    return save(infrastructure, CREATE);
  }

  public Optional<Infrastructure> update(Infrastructure infrastructure) throws ValidationException {
    var existing = read(infrastructure.getKey());
    if (!existing.isPresent()) {
      throw new ValidationException("Can't update " + infrastructure.getKey().getName() + " because it doesn't exist");
    }
    infrastructureValidator.validateForUpdate(infrastructure, existing.get());
    infrastructure.setSpecification(handlerService.handleUpdate(infrastructure, existing.get()));
    return save(infrastructure, UPDATE);
  }

  private Optional<Infrastructure> save(Infrastructure infrastructure, EventType eventType) {
    infrastructure = infrastructureRepository.save(infrastructure);
    infrastructureServiceEventEmitter.emitEventOnProcessedEntity(eventType, infrastructure);
    return Optional.ofNullable(infrastructure);
  }

  public Optional<Infrastructure> upsert(Infrastructure infrastructure) throws ValidationException {
    return !read(infrastructure.getKey()).isPresent() ? create(infrastructure) : update(infrastructure);
  }

  public Optional<Infrastructure> read(InfrastructureKey key) {
    return infrastructureRepository.findById(key);
  }

  public List<Infrastructure> findAll(Predicate<Infrastructure> filter) {
    return infrastructureRepository.findAll().stream().filter(filter).collect(toList());
  }

  public void delete(Infrastructure infrastructure) {
    throw new UnsupportedOperationException();
  }

  public boolean exists(InfrastructureKey key) {
    return read(key).isPresent();
  }
}
