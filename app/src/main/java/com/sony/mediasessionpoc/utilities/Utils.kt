package com.sony.mediasessionpoc.utilities

fun getListOfAvailableMediaTypes() : List<MediaType> {
    return listOf(MediaType.VodWithoutAds(), MediaType.VodWithAds(), MediaType.LiveWithDaiAds())
}