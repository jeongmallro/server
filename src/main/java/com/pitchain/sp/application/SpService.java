package com.pitchain.sp.application;

import com.pitchain.common.constant.SpStatus;
import com.pitchain.common.dto.InfinityScrollRes;
import com.pitchain.sp.presentation.req.SpCreateReq;
import com.pitchain.sp.application.res.SpDetailRes;
import com.pitchain.sp.domain.Sp;
import com.pitchain.common.security.MemberDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RequiredArgsConstructor
@Service
public class SpService {
    private final SpCommandService spCommandService;
    private final SpQueryService spQueryService;
    private final SpViewsService spViewsService;

    @Transactional
    public void createSp(MemberDetails memberDetails, Long bmId, SpCreateReq spCreateReq, MultipartFile thumbnailImg) {
        spCommandService.createSp(memberDetails, bmId, spCreateReq, thumbnailImg);
    }

    @Transactional(readOnly = true)
    public List<SpDetailRes> getSpDetails(MemberDetails memberDetails) {
        return spQueryService.getSpDetails(memberDetails);
    }

    @Transactional(readOnly = true)
    public InfinityScrollRes<SpDetailRes> getSpDetailsFilteredCategory(MemberDetails memberDetails, String mainCategoryInKorean, Long lastSpId, int size) {
        return spQueryService.getSpDetailsFilteredCategory(memberDetails, mainCategoryInKorean, lastSpId, size);
    }

    public SpDetailRes getSpDetail(MemberDetails memberDetails, Long bmId, Long spId) {
        spViewsService.updateSpView(spId);
        return spQueryService.getSpDetail(memberDetails, bmId, spId);
    }

    @Transactional
    public void updateSp(MemberDetails memberDetails, Long bmId, Long spId, String name, MultipartFile thumbnailImg) {
        spCommandService.updateSp(memberDetails, bmId, spId, name, thumbnailImg);
    }

    @Transactional
    public void deleteSp(MemberDetails memberDetails, Long bmId, Long spId) {
        spCommandService.deleteSp(memberDetails, bmId, spId);
    }

    @Transactional(readOnly = true)
    public List<Sp> getSpsByBmId(Long bmId) {
        return spQueryService.getSpsByBmId(bmId);
    }

    @Transactional
    public void updateStatus(Long spId, SpStatus spStatus) {
        spCommandService.updateStatus(spId, spStatus);
    }

    @Transactional(readOnly = true)
    public Sp getSp(Long spId) {
        return spQueryService.getSp(spId);
    }
}
