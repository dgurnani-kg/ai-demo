package com.konrad.library.lending.domain;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    List<Reservation> findByMemberIdOrderByReservedAtDesc(Long memberId);

    List<Reservation> findAllByOrderByReservedAtDesc();

    List<Reservation> findByBookIdAndCancelledAtIsNullOrderByReservedAtAscIdAsc(Long bookId);
}
