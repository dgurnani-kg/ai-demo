package com.konrad.library.membership.web;

import com.konrad.library.membership.service.MemberService;
import com.konrad.library.membership.web.dto.MemberResponse;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/members")
public class MemberController {

    private final MemberService members;

    public MemberController(MemberService members) {
        this.members = members;
    }

    @GetMapping
    public List<MemberResponse> listMembers() {
        return members.list().stream().map(MemberResponse::from).toList();
    }
}
