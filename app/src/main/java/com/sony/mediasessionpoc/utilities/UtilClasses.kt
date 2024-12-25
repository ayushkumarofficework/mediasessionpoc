package com.sony.mediasessionpoc.utilities

sealed class MediaType(val mediaName : String) {
    class VodWithoutAds(val name : String = "Vod Without Ads") : MediaType(name)
    class VodWithAds(val name : String = "Vod With Ads") : MediaType(name)
    class LiveWithDaiAds(val name : String = "Live With dai Ads") : MediaType(name)
}