package com.konrad.library.membership.service;

/** Shaped to match the Member schema in api/openapi.yaml. */
public record MemberView(Long id, String name, String email) {}
