package com.favoriteplace.app.dto.travel;

import lombok.*;

import java.util.List;


public class RallyDto {
    @Builder
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RallyDetailResponseDto {
        String name;
        Integer pilgrimageNumber;
        Integer myPilgrimageNumber;
        String image;
        String description;
        Integer achieveNumber;
        String itemImage;
        Boolean isLike;
    }
    @Builder
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RallyAddressListDto{
        String name;
        Integer pilgrimageNumber;
        Integer myPilgrimageNumber;
        String image;
        List<RallyAddressDto> rally;
    }
    @Builder
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RallyAddressDto{
        String address;
        List<RallyAddressPilgrimageDto> pilgrimage;
    }
    @Builder
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RallyAddressPilgrimageDto{
        Long id;
        String pilgrimageAddress;
        String image;
        Boolean isVisited;
    }
}