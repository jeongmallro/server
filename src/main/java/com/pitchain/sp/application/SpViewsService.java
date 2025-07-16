package com.pitchain.sp.application;

import com.pitchain.common.redis.RedisHashRepository;
import com.pitchain.sp.infrastucture.SpRepositoryCustom;
import com.pitchain.sp.infrastucture.dto.SpViewsDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SpViewsService {

    private final SpRepositoryCustom spRepositoryCustom;
    private final RedisHashRepository redisHashRepository;

    private static final String SP_VIEW_REDIS_KEY = "spView";

    /**
     * Redis에 Sp 조회수 증가
     * @param spId
     */
    public void updateSpView(Long spId) {
        redisHashRepository.increment(SP_VIEW_REDIS_KEY, String.valueOf(spId), 1L);
    }

    /**
     * Redis에서 DB로 Sp 조회수 업데이트
     */
    @Transactional
    public void updateSpViews() {
        List<String> spViewsResult = redisHashRepository.getAndDeleteAll(SP_VIEW_REDIS_KEY);
        List<SpViewsDto> spViewsDtoList = parseResult(spViewsResult);

        for (SpViewsDto spViewsDto : spViewsDtoList) {
            spRepositoryCustom.updateSpView(spViewsDto.spId(), spViewsDto.views());
        }
    }

    /**
     * 1분마다 Redis에서 DB로 Sp 조회수 업데이트하는 작업 수행
     */
    @Scheduled(cron = "0 */1 * * * *")
    public void runUpdateSpViews() {
        try {
            updateSpViews();
        } catch (Exception e) {
            log.error("Scheduling task [runUpdateSpViews] failed", e);
        }
    }

    /**
     * Redis에서 가져온 Sp 조회수 String 리스트를 Dto 리스트로 파싱
     * @param List<String>
     * @return List<SpViewsDto>
     */
    private List<SpViewsDto> parseResult(List<String> spViewsStringList) {
        if (spViewsStringList.size() % 2 != 0){
            log.error("spViewsStringList 개수가 올바르지 않습니다.");
            throw new IllegalArgumentException("spViewsStringList 개수가 올바르지 않습니다.");
        }

        List<SpViewsDto> spViewsDtoList = new ArrayList<>();
        for (int i = 0; i < spViewsStringList.size(); i += 2) {
            Long spId = Long.parseLong(spViewsStringList.get(i));
            Long views = Long.parseLong(spViewsStringList.get(i + 1));

            SpViewsDto spViewsDto = new SpViewsDto(spId, views);
            spViewsDtoList.add(spViewsDto);
        }

        return spViewsDtoList;
    }
}
