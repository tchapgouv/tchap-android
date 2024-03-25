/*
 * Copyright (c) 2024 New Vector Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package im.vector.app.features.location

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.util.Size
import android.widget.ImageView
import androidx.annotation.UiThread
import com.bumptech.glide.load.resource.bitmap.BitmapTransformation
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.RequestOptions
import com.mapbox.mapboxsdk.camera.CameraPosition
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.mapboxsdk.snapshotter.MapSnapshotter
import im.vector.app.core.glide.GlideApp
import im.vector.app.features.home.room.detail.timeline.action.LocationUiData
import org.matrix.android.sdk.api.extensions.tryOrNull
import java.io.File
import javax.inject.Inject

class MapRenderer @Inject constructor() {

    companion object {
        private val styleBuilder = Style.Builder().fromUri(MAP_BASE_URL)
    }

    @UiThread
    fun render(
            locationUiData: LocationUiData,
            imageView: ImageView
    ) {
        val mapSnapshotter = getMapSnapshotter(imageView.context, locationUiData.locationData, locationUiData.mapZoom, locationUiData.mapSize)
        mapSnapshotter.start(
                {
                    GlideApp.with(imageView)
                            .load(it.bitmap)
                            .apply(RequestOptions.centerCropTransform())
                            .into(imageView)
                }, null
        )
    }

    @UiThread
    fun render(
            locationData: LocationData,
            zoom: Double,
            size: Size,
            imageView: ImageView,
            imageCornerTransformation: BitmapTransformation,
            listener: RequestListener<Drawable>,
            errorHandler: MapSnapshotter.ErrorHandler? = null,
    ) {
        val context = imageView.context
        val mapSnapshotFile = File(
                context.cacheDir.absolutePath,
                "${locationData.latitude}${locationData.longitude}${zoom}${size.width}${size.height}"
        )

        if (mapSnapshotFile.exists()) {
            loadMap(imageView, mapSnapshotFile, imageCornerTransformation, listener)
        } else {
            val mapSnapshotter = getMapSnapshotter(context, locationData, zoom, size)
            mapSnapshotter.start(
                    { mapSnapshot ->
                        mapSnapshotFile.outputStream().use {
                            mapSnapshot.bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
                        }
                        loadMap(imageView, mapSnapshotFile, imageCornerTransformation, listener)
                    }, errorHandler
            )
        }
    }

    fun clear(mapView: ImageView, pinView: ImageView) {
        // It can be called after recycler view is destroyed, just silently catch
        tryOrNull {
            GlideApp.with(mapView).clear(mapView)
            GlideApp.with(pinView).clear(pinView)
        }
    }

    private fun getMapSnapshotter(
            context: Context,
            locationData: LocationData,
            zoom: Double,
            size: Size
    ) = MapSnapshotter.Options(size.width, size.height)
            .withStyleBuilder(styleBuilder)
            .withCameraPosition(
                    CameraPosition.Builder()
                            .zoom(zoom)
                            .target(LatLng(locationData.latitude, locationData.longitude))
                            .build()
            ).let { MapSnapshotter(context, it) }

    private fun loadMap(
            imageView: ImageView,
            mapSnapshotFile: File,
            imageCornerTransformation: BitmapTransformation,
            listener: RequestListener<Drawable>,
    ) {
        GlideApp.with(imageView)
                .load(mapSnapshotFile)
                .apply(RequestOptions.centerCropTransform())
                .placeholder(imageView.drawable)
                .listener(listener)
                .transform(imageCornerTransformation)
                .into(imageView)
    }
}
