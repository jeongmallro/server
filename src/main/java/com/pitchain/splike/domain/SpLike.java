package com.pitchain.splike.domain;

import com.pitchain.member.domain.Member;
import com.pitchain.sp.domain.Sp;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"member_id", "sp_id"}))
public class SpLike {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "sp_like_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sp_id", nullable = false)
    private Sp sp;

    public SpLike(Member member, Sp sp) {
        this.member = member;
        this.sp = sp;
    }
}
