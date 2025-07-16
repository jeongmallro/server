package com.pitchain.service;

import com.pitchain.bm.domain.Bm;
import com.pitchain.common.constant.MemberRole;
import com.pitchain.common.redis.RedisHashRepository;
import com.pitchain.common.security.MemberDetails;
import com.pitchain.company.domain.Company;
import com.pitchain.member.domain.Member;
import com.pitchain.sp.application.SpService;
import com.pitchain.sp.application.SpViewsService;
import com.pitchain.sp.domain.Sp;
import com.pitchain.sp.infrastucture.SpRepository;
import com.pitchain.util.EntitySaver;
import org.assertj.core.api.Assertions;
import org.awaitility.Awaitility;
import org.awaitility.Durations;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.verify;

@SpringBootTest
class SpViewsServiceTest {

    @Autowired
    private EntitySaver entitySaver;
    @Autowired
    private RedisHashRepository redisHashRepository;
    @Autowired
    private SpRepository spRepository;
    @Autowired
    private SpService spService;
    @SpyBean
    private SpViewsService spViewsService;

    private static final String SP_VIEW_REDIS_KEY = "spView";

    private Member individualMember;
    private MemberDetails individualMemberDetails;
    private Bm bm;
    private Company company;

    @BeforeEach
    void setUp() {
        Member companyMember = entitySaver.saveCompanyMember();
        company = entitySaver.saveCompany(companyMember);
        individualMember = entitySaver.saveIndividualMember();
        individualMemberDetails = new MemberDetails(individualMember.getId(), MemberRole.INDIVIDUAL);
        bm = entitySaver.saveBm(company);
    }

    @AfterEach
    void tearDown() {
        redisHashRepository.getAndDeleteAll(SP_VIEW_REDIS_KEY);
        spRepository.deleteAll();
    }

    @Test
    void SP_조회_동시성() throws InterruptedException {
        //given
        Sp sp = entitySaver.saveSp(bm);

        //when
        int threadCount = 100;
        ExecutorService executorService = Executors.newFixedThreadPool(32);
        CountDownLatch latch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    spService.getSpDetail(individualMemberDetails, bm.getId(), sp.getId());
                } finally {
                    latch.countDown();
                }
            });
        }
        latch.await();

        //then
        Map<String, String> spView = redisHashRepository.findAll(SP_VIEW_REDIS_KEY);
        String views = spView.get(String.valueOf(sp.getId()));
        assertEquals(100, Integer.parseInt(views));
    }

    @Test
    @DisplayName("Sp 조회수 DB 업데이트 중 Sp 조회 동시성 테스트")
    @Transactional(propagation = Propagation.NEVER)
    void SP_조회수_업데이트_동시성() throws InterruptedException, ExecutionException {
        //given
        int spSize = 100;
        List<Long> spIds = new ArrayList<>();
        for (int i = 0; i < spSize; i++) {
            Sp sp = entitySaver.saveSp(bm);
            spIds.add(sp.getId());
        }

        //when
        //1) updateSpView 멀티스레드 실행
        //updateSpView: Redis에 Sp 조회수 증가
        int threadCount = 100;
        ExecutorService executorService = Executors.newFixedThreadPool(32);

        //updateSpView에 대한 CountDownLatch 설정
        CountDownLatch latch = new CountDownLatch(spSize * threadCount);

        //updateSpView 스레드 실행
        for (Long spId : spIds) {
            for (int i = 0; i < threadCount; i++) {
                executorService.execute(() -> {
                    try {
                        spViewsService.updateSpView(spId);
                    } finally {
                        latch.countDown();
                    }
                });
            }
        }

        //2) updateSpViews 싱글스레드 반복 실행
        //updateSpViews: Redis -> DB 조회수 업데이트

        //updateSpViews 스레드 종료를 위한 플래그
        AtomicBoolean isRunning = new AtomicBoolean(true);

        //updateSpViews 스레드 실행
        ExecutorService updateExecutorService = Executors.newSingleThreadExecutor();
        Future<?> updateFuture = updateExecutorService.submit(() -> {
            try {
                //조회수 DB에 업데이트
                while (isRunning.get()) {
                    spViewsService.updateSpViews();
                    Thread.sleep(60000);
                }

                //마지막으로 남은 데이터 처리
                spViewsService.updateSpViews();

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        latch.await();  //updateSpView 모든 스레드 작업 끝날 때까지 대기
        executorService.shutdown();

        isRunning.set(false);  //updateSpViews 종료 유도

        updateExecutorService.shutdown();
        updateFuture.get();  //updateSpViews 완전히 종료될 때까지 대기

        //then
        Map<String, String> spViewList = redisHashRepository.findAll(SP_VIEW_REDIS_KEY);
        List<Sp> spList = spRepository.findAll();
        long totalCount = spList.stream().mapToLong(Sp::getViews).sum();

        Assertions.assertThat(spViewList).isEmpty();
        Assertions.assertThat(totalCount).isEqualTo(spSize * threadCount);
    }

    @Test
    void SP_조회수_업데이트_스케줄링() {
        //given
        Sp sp = entitySaver.saveSp(bm);
        Long spId = sp.getId();
        redisHashRepository.increment(SP_VIEW_REDIS_KEY, String.valueOf(spId), 1L);

        //then
        Awaitility.await()
                .atMost(Durations.ONE_MINUTE)
                .untilAsserted(() -> {
                    verify(spViewsService, atLeast(1)).runUpdateSpViews();

                    Sp foundSp = spRepository.findById(spId).orElseThrow();
                    Assertions.assertThat(foundSp.getViews()).isEqualTo(1L);
                });
    }
}