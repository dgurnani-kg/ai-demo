package com.konrad.library.membership.web.dto;

import com.konrad.library.membership.service.MemberView;

/** Wire shape for Member. Must stay identical to the Member schema in api/openapi.yaml. */
public record MemberResponse(Long id, String name, String email) {

    public static MemberResponse from(MemberView view) {
        return new MemberResponse(view.id(), view.name(), view.email());
    }
}
