/*
 * Copyright (c) 2021 New Vector Ltd
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

import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isGone
import androidx.fragment.app.setFragmentResultListener
import androidx.lifecycle.lifecycleScope
import com.airbnb.mvrx.fragmentViewModel
import com.airbnb.mvrx.withState
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.mapbox.mapboxsdk.maps.MapView
import im.vector.app.R
import im.vector.app.core.platform.VectorBaseBottomSheetDialogFragment
import im.vector.app.core.platform.VectorBaseFragment
import im.vector.app.core.utils.PERMISSIONS_FOR_FOREGROUND_LOCATION_SHARING
import im.vector.app.core.utils.checkPermissions
import im.vector.app.core.utils.onPermissionDeniedDialog
import im.vector.app.core.utils.registerForPermissionsResult
import im.vector.app.databinding.FragmentLocationSharingBinding
import im.vector.app.features.home.AvatarRenderer
import im.vector.app.features.home.room.detail.timeline.helper.MatrixItemColorProvider
import im.vector.app.features.location.live.LiveLocationLabsFlagPromotionBottomSheet
import im.vector.app.features.location.live.duration.ChooseLiveDurationBottomSheet
import im.vector.app.features.location.option.LocationSharingOption
import im.vector.app.features.settings.VectorPreferences
import org.matrix.android.sdk.api.util.MatrixItem
import java.lang.ref.WeakReference
import javax.inject.Inject

/**
 * We should consider using SupportMapFragment for a out of the box lifecycle handling.
 */
class LocationSharingFragment @Inject constructor(
        private val urlMapProvider: UrlMapProvider,
        private val avatarRenderer: AvatarRenderer,
        private val matrixItemColorProvider: MatrixItemColorProvider,
        private val vectorPreferences: VectorPreferences,
) : VectorBaseFragment<FragmentLocationSharingBinding>(),
        LocationTargetChangeListener,
        VectorBaseBottomSheetDialogFragment.ResultListener {

    private val viewModel: LocationSharingViewModel by fragmentViewModel()

    private val locationSharingNavigator: LocationSharingNavigator by lazy { DefaultLocationSharingNavigator(activity) }

    // Keep a ref to handle properly the onDestroy callback
    private var mapView: WeakReference<MapView>? = null

    private var hasRenderedUserAvatar = false

    override fun getBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentLocationSharingBinding {
        return FragmentLocationSharingBinding.inflate(inflater, container, false)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setFragmentResultListener(LiveLocationLabsFlagPromotionBottomSheet.REQUEST_KEY) { _, bundle ->
            val isApproved = bundle.getBoolean(LiveLocationLabsFlagPromotionBottomSheet.BUNDLE_KEY_LABS_APPROVAL)
            handleLiveLocationLabsFlagPromotionResult(isApproved)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mapView = WeakReference(views.mapView)
        views.mapView.onCreate(savedInstanceState)

        lifecycleScope.launchWhenCreated {
            views.mapView.initialize(
                    url = urlMapProvider.getMapUrl(),
                    locationTargetChangeListener = this@LocationSharingFragment
            )
        }

        initLocateButton()
        initOptionsPicker()

        viewModel.observeViewEvents {
            when (it) {
                LocationSharingViewEvents.Close -> locationSharingNavigator.quit()
                LocationSharingViewEvents.LocationNotAvailableError -> handleLocationNotAvailableError()
                is LocationSharingViewEvents.ZoomToUserLocation -> handleZoomToUserLocationEvent(it)
                is LocationSharingViewEvents.StartLiveLocationService -> handleStartLiveLocationService(it)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        views.mapView.onResume()
    }

    override fun onPause() {
        views.mapView.onPause()
        super.onPause()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        views.mapView.onSaveInstanceState(outState)
    }

    override fun onStart() {
        super.onStart()
        views.mapView.onStart()
    }

    override fun onStop() {
        views.mapView.onStop()
        super.onStop()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        views.mapView.onLowMemory()
    }

    override fun onDestroy() {
        mapView?.get()?.onDestroy()
        mapView?.clear()
        super.onDestroy()
    }

    override fun onLocationTargetChange(target: LocationData) {
        viewModel.handle(LocationSharingAction.LocationTargetChange(target))
    }

    override fun invalidate() = withState(viewModel) { state ->
        updateMap(state)
        updateUserAvatar(state.userItem)
        if (state.locationTargetDrawable != null) {
            updateLocationTargetPin(state.locationTargetDrawable)
        }
        views.shareLocationGpsLoading.isGone = state.lastKnownUserLocation != null
    }

    private fun handleLocationNotAvailableError() {
        MaterialAlertDialogBuilder(requireActivity())
                .setTitle(R.string.location_not_available_dialog_title)
                .setMessage(R.string.location_not_available_dialog_content)
                .setPositiveButton(R.string.ok) { _, _ ->
                    locationSharingNavigator.quit()
                }
                .setCancelable(false)
                .show()
    }

    private fun initLocateButton() {
        views.mapView.locateButton.setOnClickListener {
            viewModel.handle(LocationSharingAction.ZoomToUserLocation)
        }
    }

    private fun handleZoomToUserLocationEvent(event: LocationSharingViewEvents.ZoomToUserLocation) {
        views.mapView.zoomToLocation(event.userLocation)
    }

    private fun handleStartLiveLocationService(event: LocationSharingViewEvents.StartLiveLocationService) {
        val args = LocationSharingService.RoomArgs(event.sessionId, event.roomId, event.durationMillis)

        Intent(requireContext(), LocationSharingService::class.java)
                .putExtra(LocationSharingService.EXTRA_ROOM_ARGS, args)
                .also {
                    ContextCompat.startForegroundService(requireContext(), it)
                }

        vectorBaseActivity.finish()
    }

    private fun initOptionsPicker() {
        // set no option at start
        views.shareLocationOptionsPicker.render()
        views.shareLocationOptionsPicker.optionPinned.debouncedClicks {
            val targetLocation = views.mapView.getLocationOfMapCenter()
            viewModel.handle(LocationSharingAction.PinnedLocationSharing(targetLocation))
        }
        views.shareLocationOptionsPicker.optionUserCurrent.debouncedClicks {
            viewModel.handle(LocationSharingAction.CurrentUserLocationSharing)
        }
        views.shareLocationOptionsPicker.optionUserLive.debouncedClicks {
            tryStartLiveLocationSharing()
        }
    }

    private fun handleLiveLocationLabsFlagPromotionResult(isApproved: Boolean) {
        if (isApproved) {
            vectorPreferences.setLiveLocationLabsEnabled(isEnabled = true)
            startLiveLocationSharing()
        }
    }

    private fun tryStartLiveLocationSharing() {
        if (vectorPreferences.labsEnableLiveLocation()) {
            startLiveLocationSharing()
        } else {
            LiveLocationLabsFlagPromotionBottomSheet.newInstance()
                    .show(requireActivity().supportFragmentManager, "DISPLAY_LIVE_LOCATION_LABS_FLAG_PROMOTION")
        }
    }

    private val foregroundLocationResultLauncher = registerForPermissionsResult { allGranted, deniedPermanently ->
        if (allGranted) {
            startLiveLocationSharing()
        } else if (deniedPermanently) {
            activity?.onPermissionDeniedDialog(R.string.denied_permission_generic)
        }
    }

    private fun startLiveLocationSharing() {
        // we need to re-check foreground location to be sure it has not changed after landing on this screen
        if (checkPermissions(PERMISSIONS_FOR_FOREGROUND_LOCATION_SHARING, requireActivity(), foregroundLocationResultLauncher)) {
            ChooseLiveDurationBottomSheet.newInstance(this)
                    .show(requireActivity().supportFragmentManager, "DISPLAY_CHOOSE_DURATION_OPTIONS")
        }
    }

    override fun onBottomSheetResult(resultCode: Int, data: Any?) {
        if (resultCode == VectorBaseBottomSheetDialogFragment.ResultListener.RESULT_OK) {
            (data as? Long)?.let { viewModel.handle(LocationSharingAction.StartLiveLocationSharing(it)) }
        }
    }

    private fun updateMap(state: LocationSharingViewState) {
        // first, update the options view
        val options: Set<LocationSharingOption> = when (state.areTargetAndUserLocationEqual) {
            true -> setOf(LocationSharingOption.USER_CURRENT, LocationSharingOption.USER_LIVE)
            false -> setOf(LocationSharingOption.PINNED)
            else -> emptySet()
        }
        views.shareLocationOptionsPicker.render(options)

        // then, update the map using the height of the options view after it has been rendered
        views.shareLocationOptionsPicker.post {
            val mapState = state
                    .toMapState()
                    .copy(logoMarginBottom = views.shareLocationOptionsPicker.height)
            views.mapView.render(mapState)
        }
    }

    private fun updateUserAvatar(userItem: MatrixItem.UserItem?) {
        userItem?.takeUnless { hasRenderedUserAvatar }
                ?.let {
                    hasRenderedUserAvatar = true
                    avatarRenderer.render(it, views.shareLocationOptionsPicker.optionUserCurrent.iconView)
                    val tintColor = matrixItemColorProvider.getColor(it)
                    views.shareLocationOptionsPicker.optionUserCurrent.setIconBackgroundTint(tintColor)
                }
    }

    private fun updateLocationTargetPin(drawable: Drawable) {
        views.shareLocationPin.setImageDrawable(drawable)
    }
}
