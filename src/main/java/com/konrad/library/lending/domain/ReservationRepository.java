package com.konrad.library.lending.domain;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    List<Reservation> findAllByOrderByReservedAtDesc();

    List<Reservation> findByMemberIdOrderByReservedAtDesc(Long memberId);

    List<Reservation> findByStatusOrderByReservedAtDesc(ReservationStatus status);

    List<Reservation> findByMemberIdAndStatusOrderByReservedAtDesc(Long memberId, ReservationStatus status);

    long countByBookIdAndStatus(Long bookId, ReservationStatus status);

    boolean existsByBookIdAndMemberIdAndStatusIn(Long bookId, Long memberId, List<ReservationStatus> statuses);

    Optional<Reservation> findFirstByBookIdAndStatusOrderByReservedAtAsc(Long bookId, ReservationStatus status);

    Optional<Reservation> findFirstByBookIdAndMemberIdAndStatusOrderByReservedAtAsc(
            Long bookId, Long memberId, ReservationStatus status);
}
