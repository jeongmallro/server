package com.pitchain.sp.application.res;

import com.pitchain.common.annotation.S3Url;
import com.pitchain.sp.infrastucture.dto.SpWithLikeDto;
import com.pitchain.bm.domain.Bm;
import com.pitchain.company.domain.Company;
import com.pitchain.member.domain.Member;
import com.pitchain.sp.domain.Sp;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.util.List;

@Builder
public record SpDetailRes(
        @NotNull
        Long bmId,
        @NotBlank
        String bmName,
        @NotBlank
        @S3Url
        String companyProfileImgURL,
        @NotBlank
        String companyName,
        @NotBlank
        String companyAddress,
        @NotBlank
        String spURL,
        @NotBlank
        @S3Url
        String thumbnailImgURL,
        @NotNull
        Long views,
        @NotBlank
        String name,
        @NotBlank
        String mainCategory,
        @NotNull
        List<String> subCategories,
        @NotNull
        Boolean isLiked,
        @NotNull
        Long likeCnt
) {
    public static SpDetailRes createRes(SpWithLikeDto spWithLikeDto, Long likeCnt, List<String> subCategories) {
        Sp sp = spWithLikeDto.getSp();
        Bm bm = sp.getBm();
        Company company = bm.getCompany();
        Member member = company.getMember();
        return SpDetailRes.builder()
                .bmId(sp.getBm().getId())
                .bmName(bm.getName())
                .companyProfileImgURL(member.getProfileImgKey())  //추후에 JSON 직렬화 처리됨
                .companyName(member.getName())
                .companyAddress(company.getAddress())
                .spURL(sp.getSpKey())
                .thumbnailImgURL(sp.getThumbnailImgKey())  //추후에 JSON 직렬화 처리됨
                .views(sp.getViews())
                .name(sp.getName())
                .mainCategory(bm.getMainCategory().getKoreanName())
                .subCategories(subCategories)
                .isLiked(spWithLikeDto.isLiked())
                .likeCnt(likeCnt)
                .build();
    }
}
