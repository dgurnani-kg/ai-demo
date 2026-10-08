package com.konrad.library.membership.service;

import com.konrad.library.common.NotFoundException;
import com.konrad.library.membership.domain.Member;
import com.konrad.library.membership.domain.MemberRepository;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class MemberService {

    private final MemberRepository members;

    public MemberService(MemberRepository members) {
        this.members = members;
    }

    public List<MemberView> list() {
        return members.findAll().stream()
                .map(this::toView)
                .sorted(Comparator.comparing(MemberView::name))
                .toList();
    }

    public MemberView get(long memberId) {
        return members.findById(memberId)
                .map(this::toView)
                .orElseThrow(() -> new NotFoundException("No member with id " + memberId));
    }

    private MemberView toView(Member member) {
        return new MemberView(member.getId(), member.getName(), member.getEmail());
    }
}
