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
import com.expediagroup.streamplatform.streamregistry.core.validators.ConsumerValidator;
import com.expediagroup.streamplatform.streamregistry.core.validators.ValidationException;
import com.expediagroup.streamplatform.streamregistry.model.Consumer;
import com.expediagroup.streamplatform.streamregistry.model.keys.ConsumerKey;
import com.expediagroup.streamplatform.streamregistry.repository.ConsumerRepository;

@Component
@RequiredArgsConstructor
public class ConsumerService {
  private final HandlerService handlerService;
  private final ConsumerValidator consumerValidator;
  private final ConsumerRepository consumerRepository;
  private final NotificationEventEmitter<Consumer> consumerServiceEventEmitter;

  public Optional<Consumer> create(Consumer consumer) throws ValidationException {
    if (read(consumer.getKey()).isPresent()) {
      throw new ValidationException("Can't create because it already exists");
    }
    consumerValidator.validateForCreate(consumer);
    consumer.setSpecification(handlerService.handleInsert(consumer));
    return save(consumer, CREATE);
  }

  public Optional<Consumer> update(Consumer consumer) throws ValidationException {
    var existing = read(consumer.getKey());
    if (!existing.isPresent()) {
      throw new ValidationException("Can't update " + consumer.getKey().getName() + " because it doesn't exist");
    }
    consumerValidator.validateForUpdate(consumer, existing.get());
    consumer.setSpecification(handlerService.handleUpdate(consumer, existing.get()));
    return save(consumer, UPDATE);
  }

  private Optional<Consumer> save(Consumer consumer, EventType eventType) {
    consumer = consumerRepository.save(consumer);
    consumerServiceEventEmitter.emitEventOnProcessedEntity(eventType, consumer);
    return Optional.ofNullable(consumer);
  }

  public Optional<Consumer> upsert(Consumer consumer) throws ValidationException {
    return !read(consumer.getKey()).isPresent() ? create(consumer) : update(consumer);
  }

  public Optional<Consumer> read(ConsumerKey key) {
    return consumerRepository.findById(key);
  }

  public List<Consumer> findAll(Predicate<Consumer> filter) {
    return consumerRepository.findAll().stream().filter(filter).collect(toList());
  }

  public void delete(Consumer consumer) {
    throw new UnsupportedOperationException();
  }

  public boolean exists(ConsumerKey key) {
    return read(key).isPresent();
  }
}
