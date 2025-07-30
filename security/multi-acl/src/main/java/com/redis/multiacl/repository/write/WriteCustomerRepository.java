package com.redis.multiacl.repository.write;

import com.redis.om.spring.repository.RedisDocumentRepository;
import com.redis.multiacl.model.Customer;

public interface WriteCustomerRepository extends RedisDocumentRepository<Customer, String> {
}