package com.redis.multiacl.repository.read;

import com.redis.om.spring.repository.RedisDocumentRepository;
import com.redis.multiacl.model.Customer;

public interface ReadCustomerRepository extends RedisDocumentRepository<Customer, String> {
}