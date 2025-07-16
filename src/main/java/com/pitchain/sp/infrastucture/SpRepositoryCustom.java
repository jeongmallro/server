package com.pitchain.sp.infrastucture;

import com.pitchain.common.constant.MainCategory;
import com.pitchain.sp.infrastucture.dto.QSpWithLikeDto;
import com.pitchain.sp.infrastucture.dto.SpWithLikeDto;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.pitchain.bm.domain.QBm.bm;
import static com.pitchain.sp.domain.QSp.sp;
import static com.pitchain.splike.domain.QSpLike.spLike;

@RequiredArgsConstructor
@Repository
public class SpRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    public List<SpWithLikeDto> getSpWithLikeDtoFilteredCategory(Long memberId, MainCategory category, Long lastSpId, int size) {
        return queryFactory
                .select(new QSpWithLikeDto(
                        sp,
                        spLike.isNotNull()
                ))
                .from(sp)
                .leftJoin(sp.bm, bm)
                .leftJoin(spLike).on(spLike.sp.id.eq(sp.id).and(spLike.member.id.eq(memberId)))
                .where(
                        eqMainCategory(category),
                        ltSpId(lastSpId)
                )
                .orderBy(sp.id.desc())
                .limit(size + 1)
                .distinct()
                .fetch();
    }

    private static BooleanExpression eqMainCategory(MainCategory category) {
        return bm.mainCategory.eq(category);
    }

    private BooleanExpression ltSpId(Long lastSpId) {
        return lastSpId == null ? null : sp.id.lt(lastSpId);
    }

    @Transactional
    public long updateSpView(Long spId, Long views) {
         return queryFactory
                .update(sp)
                .set(sp.views, sp.views.add(views))
                .where(sp.id.eq(spId))
                .execute();
    }
}
