package ru.blackbull.eatogether

import ru.blackbull.eatogether.classesForParsingPlaceDetails.PlaceDetail
import ru.blackbull.eatogether.classesForParsingPlaces.BasicLocation
import com.google.android.gms.maps.model.LatLng
import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonParser
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.lang.reflect.Type
import java.net.URL
import java.net.URLEncoder


class PlaceDataParser {
    private val apiKey = "AIzaSyDCvP07ssvmpkykrJ3QN5_BzGifwa4Weqo"

    companion object {
        private const val baseUrl = "https://maps.googleapis.com/maps/api"
    }

    suspend fun getPlaceDetail(placeID: String): PlaceDetail {
        val fields = arrayListOf(
            "name", "rating", "formatted_phone_number",
            "review", "photos", "formatted_address", "opening_hours"
        )
        val language = "ru"
        val url = baseUrl + "/place/details/json?" +
                "place_id=$placeID" +
                "&language=$language" +
                "&fields=${fields.joinToString(",")}" +
                "&key=$apiKey"
        val jsonResp = getJsonElement(asyncDownloadURL(url)).asJsonObject
        return Gson().fromJson(
            jsonResp.get("result").asJsonObject,
            PlaceDetail::class.java
        )
    }

    suspend fun getPlaceByName(query: String): ArrayList<BasicLocation> {
        val fields = arrayListOf(
            "name", "geometry", "place_id",
        )
        val url = baseUrl + "/place/textsearch/json?" +
                "language=ru" +
                "&query=${URLEncoder.encode(
                    query,
                    "utf-8"
                )}" + // TODO: спросить что за предупреждение
//                "&inputtype=textquery" +
//                "&fields=${fields.joinToString(",")}" +
                "&key=$apiKey"
        val jsonResp = getJsonElement(asyncDownloadURL(url)).asJsonObject
        val basicLocationListType: Type = object : TypeToken<ArrayList<BasicLocation>?>() {}.type
        return Gson().fromJson(
            jsonResp.get("results").asJsonArray,
            basicLocationListType
        )
    }

    fun getPhotoUrl(photoReference: String, width: Int, height: Int): String {
        val url = baseUrl + "/place/photo?" +
                "maxwidth=" + width +
                "&maxheight=" + height +
                "&photoreference=" + photoReference +
                "&key=" + apiKey
        return url
    }

    suspend fun getNearByPlaces(latLng: LatLng): List<BasicLocation> {
        val url = baseUrl + "/place/nearbysearch/json?" +
                "language=ru" +
                "&location=" + latLng.latitude + "," + latLng.longitude +
                "&radius=1000" +
                "&type=restaurant" +
                "&key=" + apiKey
        val jsonResp = getJsonElement(asyncDownloadURL(url)).asJsonObject
        val basicLocationListType: Type = object : TypeToken<ArrayList<BasicLocation>?>() {}.type
        return Gson().fromJson(
            jsonResp.get("results").asJsonArray,
            basicLocationListType
        )
    }

    private suspend fun asyncDownloadURL(string: String): String {
        return withContext(Dispatchers.IO) {
            return@withContext URL(string).readText()
        }
    }

    private fun getJsonElement(response: String): JsonElement {
        val jsonResp = JsonParser.parseString(response).asJsonObject
        val status = jsonResp.get("status").asString
        if (status == "OK") {
            return jsonResp
        }
        throw RuntimeException("Get invalid status: $status")
    }
}