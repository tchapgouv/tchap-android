/*
 * Copyright 2019-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.home.room.list

import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.airbnb.epoxy.EpoxyController
import com.airbnb.epoxy.OnModelBuildFinishedListener
import com.airbnb.mvrx.args
import com.airbnb.mvrx.fragmentViewModel
import com.airbnb.mvrx.withState
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import fr.gouv.tchap.core.utils.TchapUtils
import im.vector.app.R
import im.vector.app.core.epoxy.LayoutManagerStateRestorer
import im.vector.app.core.extensions.cleanup
import im.vector.app.core.platform.OnBackPressed
import im.vector.app.core.platform.StateView
import im.vector.app.core.platform.VectorBaseFragment
import im.vector.app.core.resources.UserPreferencesProvider
import im.vector.app.databinding.FragmentRoomListBinding
import im.vector.app.features.analytics.plan.MobileScreen
import im.vector.app.features.analytics.plan.ViewRoom
import im.vector.app.features.home.HomeSharedActionViewModel
import im.vector.app.features.home.RoomListDisplayMode
import im.vector.app.features.home.room.filtered.FilteredRoomFooterItem
import im.vector.app.features.home.room.list.actions.RoomListQuickActionsBottomSheet
import im.vector.app.features.home.room.list.actions.RoomListQuickActionsSharedAction
import im.vector.app.features.home.room.list.actions.RoomListQuickActionsSharedActionViewModel
import im.vector.app.features.home.room.list.widget.NotifsFabMenuView
import im.vector.app.features.home.room.list.widget.TchapRoomsFabMenuView
import im.vector.app.features.matrixto.OriginOfMatrixTo
import im.vector.app.features.notifications.NotificationDrawerManager
import im.vector.lib.strings.CommonStrings
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import org.matrix.android.sdk.api.extensions.orTrue
import org.matrix.android.sdk.api.session.room.model.RoomSummary
import org.matrix.android.sdk.api.session.room.model.SpaceChildInfo
import org.matrix.android.sdk.api.session.room.model.tag.RoomTag
import org.matrix.android.sdk.api.session.room.notification.RoomNotificationState
import javax.inject.Inject

@Parcelize
data class RoomListParams(
        val displayMode: RoomListDisplayMode
) : Parcelable

@AndroidEntryPoint
class RoomListFragment :
        VectorBaseFragment<FragmentRoomListBinding>(),
        RoomListListener,
        OnBackPressed,
        FilteredRoomFooterItem.Listener,
        NotifsFabMenuView.Listener,
        TchapRoomsFabMenuView.Listener {

    @Inject lateinit var pagedControllerFactory: RoomSummaryPagedControllerFactory
    @Inject lateinit var notificationDrawerManager: NotificationDrawerManager
    @Inject lateinit var footerController: RoomListFooterController
    @Inject lateinit var userPreferencesProvider: UserPreferencesProvider

    private var modelBuildListener: OnModelBuildFinishedListener? = null
    private lateinit var sharedActionViewModel: RoomListQuickActionsSharedActionViewModel
    private lateinit var homeSharedActionViewModel: HomeSharedActionViewModel
    private val roomListParams: RoomListParams by args()
    private val roomListViewModel: RoomListViewModel by fragmentViewModel()
    private lateinit var stateRestorer: LayoutManagerStateRestorer

    override fun getBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentRoomListBinding {
        return FragmentRoomListBinding.inflate(inflater, container, false)
    }

    data class SectionKey(
            val name: String,
            val isExpanded: Boolean,
            val notifyOfLocalEcho: Boolean
    )

    data class SectionAdapterInfo(
            var section: SectionKey,
            val sectionHeaderAdapter: SectionHeaderAdapter,
            val contentEpoxyController: EpoxyController
    )

    private val adapterInfosList = mutableListOf<SectionAdapterInfo>()
    private var concatAdapter: ConcatAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        analyticsScreenName = when (roomListParams.displayMode) {
            RoomListDisplayMode.PEOPLE -> MobileScreen.ScreenName.People
            RoomListDisplayMode.ROOMS -> MobileScreen.ScreenName.Rooms
            else -> null
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        views.stateView.contentView = views.roomListView
        views.stateView.state = StateView.State.Loading
        setupCreateRoomButton()
        setupRecyclerView()
        sharedActionViewModel = activityViewModelProvider.get(RoomListQuickActionsSharedActionViewModel::class.java)
        homeSharedActionViewModel = activityViewModelProvider.get(HomeSharedActionViewModel::class.java)
        roomListViewModel.observeViewEvents {
            when (it) {
                is RoomListViewEvents.Loading -> showLoading(it.message)
                is RoomListViewEvents.Failure -> showFailure(it.throwable)
                is RoomListViewEvents.SelectRoom -> handleSelectRoom(it, it.isInviteAlreadyAccepted)
                is RoomListViewEvents.Done -> Unit
                is RoomListViewEvents.NavigateToMxToBottomSheet -> handleShowMxToLink(it.link)
                RoomListViewEvents.CreateDirectChat -> handleCreateDirectChat()
                is RoomListViewEvents.CreateRoom -> handleCreateRoom(it.initialName)
                is RoomListViewEvents.OpenRoomDirectory -> handleOpenRoomDirectory(it.filter)
            }
        }

        views.createChatFabMenu.listener = this
        views.tchapCreateRoomFabMenu.listener = this

        sharedActionViewModel
                .stream()
                .onEach { handleQuickActions(it) }
                .launchIn(viewLifecycleOwner.lifecycleScope)

        roomListViewModel.onEach(RoomListViewState::roomMembershipChanges) { ms ->
            // it's for invites local echo
            adapterInfosList.filter { it.section.notifyOfLocalEcho }
                    .onEach {
                        (it.contentEpoxyController as? RoomSummaryPagedController)?.roomChangeMembershipStates = ms
                    }
        }
    }

    override fun onStart() {
        super.onStart()

        // Local rooms should not exist anymore when the room list is shown
        roomListViewModel.handle(RoomListAction.DeleteAllLocalRoom)
    }

    private fun refreshCollapseStates() {
        val sectionsCount = adapterInfosList.count { !it.sectionHeaderAdapter.roomsSectionData.isHidden }
        roomListViewModel.sections.forEachIndexed { index, roomsSection ->
            val actualBlock = adapterInfosList[index]
            val isRoomSectionCollapsable = sectionsCount > 1
            val isRoomSectionExpanded = roomsSection.isExpanded.value.orTrue()
            if (actualBlock.section.isExpanded && !isRoomSectionExpanded) {
                // mark controller as collapsed
                actualBlock.contentEpoxyController.setCollapsed(true)
            } else if (!actualBlock.section.isExpanded && isRoomSectionExpanded) {
                // we must expand!
                actualBlock.contentEpoxyController.setCollapsed(false)
            }
            actualBlock.section = actualBlock.section.copy(isExpanded = isRoomSectionExpanded)
            actualBlock.sectionHeaderAdapter.updateSection {
                it.copy(
                        isExpanded = isRoomSectionExpanded,
                        isCollapsable = isRoomSectionCollapsable
                )
            }

            if (!isRoomSectionExpanded && !isRoomSectionCollapsable) {
                // force expand if the section is not collapsable
                roomListViewModel.handle(RoomListAction.ToggleSection(roomsSection))
            }
        }
    }

    override fun showFailure(throwable: Throwable) {
        showErrorInSnackbar(throwable)
    }

    private fun handleShowMxToLink(link: String) {
        navigator.openMatrixToBottomSheet(requireActivity(), link, OriginOfMatrixTo.ROOM_LIST)
    }

    override fun onDestroyView() {
        adapterInfosList.onEach { it.contentEpoxyController.removeModelBuildListener(modelBuildListener) }
        adapterInfosList.clear()
        modelBuildListener = null
        views.roomListView.cleanup()
        footerController.listener = null
        // TODO Cleanup listener on the ConcatAdapter's adapters?
        stateRestorer.clear()
        views.tchapCreateRoomFabMenu.listener = null
        views.createChatFabMenu.listener = null
        concatAdapter = null
        super.onDestroyView()
    }

    private fun handleSelectRoom(event: RoomListViewEvents.SelectRoom, isInviteAlreadyAccepted: Boolean) {
        navigator.openRoom(
                context = requireActivity(),
                roomId = event.roomSummary.roomId,
                isInviteAlreadyAccepted = isInviteAlreadyAccepted,
                trigger = ViewRoom.Trigger.RoomList
        )
    }

    private fun handleCreateDirectChat() {
        navigator.openCreateDirectRoom(requireActivity())
    }

    private fun handleCreateRoom(name: String) {
        navigator.openCreateRoom(requireActivity(), name)
    }

    private fun handleOpenRoomDirectory(filter: String) {
        navigator.openRoomDirectory(requireActivity(), filter)
    }

    private fun setupCreateRoomButton() {
        val showFab = !TchapUtils.isExternalTchapUser(roomListViewModel.session.myUserId)
        when (roomListParams.displayMode) {
            RoomListDisplayMode.NOTIFICATIONS -> views.createChatFabMenu.isVisible = showFab
            RoomListDisplayMode.PEOPLE -> views.createChatRoomButton.isVisible = showFab
            RoomListDisplayMode.ROOMS -> views.tchapCreateRoomFabMenu.isVisible = showFab
            RoomListDisplayMode.FILTERED -> Unit // No button in this mode
        }

        views.createChatRoomButton.debouncedClicks {
            fabCreateDirectChat()
        }

        // Hide FAB when list is scrolling
        views.roomListView.addOnScrollListener(
                object : RecyclerView.OnScrollListener() {
                    override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                        if (!showFab) return // do nothing

                        views.createChatFabMenu.removeCallbacks(showFabRunnable)
                        views.tchapCreateRoomFabMenu.removeCallbacks(showFabRunnable)

                        when (newState) {
                            RecyclerView.SCROLL_STATE_IDLE -> {
                                views.createChatFabMenu.postDelayed(showFabRunnable, 250)
                                views.tchapCreateRoomFabMenu.postDelayed(showFabRunnable, 250)
                            }
                            RecyclerView.SCROLL_STATE_DRAGGING,
                            RecyclerView.SCROLL_STATE_SETTLING -> {
                                when (roomListParams.displayMode) {
                                    RoomListDisplayMode.NOTIFICATIONS -> views.createChatFabMenu.hide()
                                    RoomListDisplayMode.PEOPLE -> views.createChatRoomButton.hide()
                                    RoomListDisplayMode.ROOMS -> views.tchapCreateRoomFabMenu.hide()
                                    RoomListDisplayMode.FILTERED -> Unit
                                }
                            }
                        }
                    }
                })
    }

    fun filterRoomsWith(filter: String) {
        // Scroll the list to top
        views.roomListView.scrollToPosition(0)

        roomListViewModel.handle(RoomListAction.FilterWith(filter))
    }

    // FilteredRoomFooterItem.Listener
    override fun createRoom(initialName: String) {
        roomListViewModel.handle(RoomListAction.CreateRoom(initialName))
    }

    override fun createDirectChat() {
        roomListViewModel.handle(RoomListAction.CreateDirectChat)
    }

    override fun openRoomDirectory(initialFilter: String) {
        roomListViewModel.handle(RoomListAction.OpenRoomDirectory(initialFilter))
    }

    // NotifsFabMenuView.Listener, TchapRoomFabMenuView.Listener
    override fun fabCreateDirectChat() {
        roomListViewModel.handle(RoomListAction.CreateDirectChat)
    }

    override fun fabOpenRoomDirectory() {
        roomListViewModel.handle(RoomListAction.OpenRoomDirectory(""))
    }

    override fun fabCreateRoom() {
        roomListViewModel.handle(RoomListAction.CreateRoom())
    }

    private fun setupRecyclerView() {
        val layoutManager = LinearLayoutManager(requireContext())
        stateRestorer = LayoutManagerStateRestorer(layoutManager).register()
        views.roomListView.layoutManager = layoutManager
        views.roomListView.itemAnimator = RoomListAnimator()
        layoutManager.recycleChildrenOnDetach = true

        modelBuildListener = OnModelBuildFinishedListener { it.dispatchTo(stateRestorer) }

        val concatAdapter = ConcatAdapter()

        roomListViewModel.sections.forEachIndexed { index, section ->
            val sectionAdapter = SectionHeaderAdapter(SectionHeaderAdapter.RoomsSectionData(section.sectionName)) {
                if (adapterInfosList[index].sectionHeaderAdapter.roomsSectionData.isCollapsable) {
                    roomListViewModel.handle(RoomListAction.ToggleSection(section))
                }
            }
            val contentAdapter =
                    when {
                        section.livePages != null -> {
                            pagedControllerFactory.createRoomSummaryPagedController(roomListParams.displayMode)
                                    .also { controller ->
                                        section.livePages.observe(viewLifecycleOwner) { pl ->
                                            controller.submitList(pl)
                                            sectionAdapter.updateSection {
                                                it.copy(
                                                        isHidden = pl.isEmpty(),
                                                        isLoading = false
                                                )
                                            }
                                            refreshCollapseStates()
                                            checkEmptyState()
                                        }
                                        observeItemCount(section, sectionAdapter)
                                        section.notificationCount.observe(viewLifecycleOwner) { counts ->
                                            sectionAdapter.updateSection {
                                                it.copy(
                                                        notificationCount = counts.totalCount,
                                                        isHighlighted = counts.isHighlight,
                                                )
                                            }
                                        }
                                        section.isExpanded.observe(viewLifecycleOwner) {
                                            refreshCollapseStates()
                                        }
                                        controller.listener = this
                                    }
                        }
                        section.liveSuggested != null -> {
                            pagedControllerFactory.createSuggestedRoomListController()
                                    .also { controller ->
                                        section.liveSuggested.observe(viewLifecycleOwner) { info ->
                                            controller.setData(info)
                                            sectionAdapter.updateSection {
                                                it.copy(
                                                        isHidden = info.rooms.isEmpty(),
                                                        isLoading = false
                                                )
                                            }
                                            refreshCollapseStates()
                                            checkEmptyState()
                                        }
                                        observeItemCount(section, sectionAdapter)
                                        section.isExpanded.observe(viewLifecycleOwner) {
                                            refreshCollapseStates()
                                        }
                                        controller.listener = this
                                    }
                        }
                        else -> {
                            pagedControllerFactory.createRoomSummaryListController(roomListParams.displayMode)
                                    .also { controller ->
                                        section.liveList?.observe(viewLifecycleOwner) { list ->
                                            controller.setData(list)
                                            sectionAdapter.updateSection {
                                                it.copy(
                                                        isHidden = list.isEmpty(),
                                                        isLoading = false,
                                                )
                                            }
                                            refreshCollapseStates()
                                            checkEmptyState()
                                        }
                                        observeItemCount(section, sectionAdapter)
                                        section.notificationCount.observe(viewLifecycleOwner) { counts ->
                                            sectionAdapter.updateSection {
                                                it.copy(
                                                        notificationCount = counts.totalCount,
                                                        isHighlighted = counts.isHighlight
                                                )
                                            }
                                        }
                                        section.isExpanded.observe(viewLifecycleOwner) {
                                            refreshCollapseStates()
                                        }
                                        controller.listener = this
                                    }
                        }
                    }
            adapterInfosList.add(
                    SectionAdapterInfo(
                            SectionKey(
                                    name = section.sectionName,
                                    isExpanded = section.isExpanded.value.orTrue(),
                                    notifyOfLocalEcho = section.notifyOfLocalEcho
                            ),
                            sectionAdapter,
                            contentAdapter
                    )
            )
            concatAdapter.addAdapter(sectionAdapter)
            concatAdapter.addAdapter(contentAdapter.adapter)
        }

        // Add the footer controller
        footerController.listener = this
        concatAdapter.addAdapter(footerController.adapter)

        this.concatAdapter = concatAdapter
        views.roomListView.adapter = concatAdapter
    }

    private val showFabRunnable = Runnable {
        if (isAdded) {
            when (roomListParams.displayMode) {
                RoomListDisplayMode.NOTIFICATIONS -> views.createChatFabMenu.show()
                RoomListDisplayMode.PEOPLE -> views.createChatRoomButton.show()
                RoomListDisplayMode.ROOMS -> views.tchapCreateRoomFabMenu.show()
                RoomListDisplayMode.FILTERED -> Unit
            }
        }
    }

    private fun observeItemCount(section: RoomsSection, sectionAdapter: SectionHeaderAdapter) {
        lifecycleScope.launch {
            section.itemCount
                    .flowWithLifecycle(lifecycle, Lifecycle.State.STARTED)
                    .filter { it > 0 }
                    .collect { count ->
                        sectionAdapter.updateSection {
                            it.copy(itemCount = count)
                        }
                    }
        }
    }

    private fun handleQuickActions(quickAction: RoomListQuickActionsSharedAction) {
        when (quickAction) {
            is RoomListQuickActionsSharedAction.NotificationsAllNoisy -> {
                roomListViewModel.handle(RoomListAction.ChangeRoomNotificationState(quickAction.roomId, RoomNotificationState.ALL_MESSAGES_NOISY))
            }
            is RoomListQuickActionsSharedAction.NotificationsAll -> {
                roomListViewModel.handle(RoomListAction.ChangeRoomNotificationState(quickAction.roomId, RoomNotificationState.ALL_MESSAGES))
            }
            is RoomListQuickActionsSharedAction.NotificationsMentionsOnly -> {
                roomListViewModel.handle(RoomListAction.ChangeRoomNotificationState(quickAction.roomId, RoomNotificationState.MENTIONS_ONLY))
            }
            is RoomListQuickActionsSharedAction.NotificationsMute -> {
                roomListViewModel.handle(RoomListAction.ChangeRoomNotificationState(quickAction.roomId, RoomNotificationState.MUTE))
            }
            is RoomListQuickActionsSharedAction.Settings -> {
                navigator.openRoomProfile(requireActivity(), quickAction.roomId)
            }
            is RoomListQuickActionsSharedAction.Favorite -> {
                roomListViewModel.handle(RoomListAction.ToggleTag(quickAction.roomId, RoomTag.ROOM_TAG_FAVOURITE))
            }
            is RoomListQuickActionsSharedAction.LowPriority -> {
                // TCHAP Not used in Tchap
//                roomListViewModel.handle(RoomListAction.ToggleTag(quickAction.roomId, RoomTag.ROOM_TAG_LOW_PRIORITY))
            }
            is RoomListQuickActionsSharedAction.Leave -> {
                promptLeaveRoom(quickAction.roomId)
            }
        }
    }

    private fun promptLeaveRoom(roomId: String) {
        val isPublicRoom = roomListViewModel.isPublicRoom(roomId)
        val message = buildString {
            append(getString(CommonStrings.room_participants_leave_prompt_msg))
            if (!isPublicRoom) {
                append("\n\n")
                append(getString(CommonStrings.room_participants_leave_private_warning))
            }
        }
        MaterialAlertDialogBuilder(
                requireContext(),
                if (isPublicRoom) 0 else im.vector.lib.ui.styles.R.style.ThemeOverlay_Vector_MaterialAlertDialog_Destructive
        )
                .setTitle(CommonStrings.room_participants_leave_prompt_title)
                .setMessage(message)
                .setPositiveButton(CommonStrings.action_leave) { _, _ ->
                    roomListViewModel.handle(RoomListAction.LeaveRoom(roomId))
                }
                .setNegativeButton(CommonStrings.action_cancel, null)
                .show()
    }

    override fun invalidate() = withState(roomListViewModel) { state ->
        footerController.setData(state)
    }

    private fun checkEmptyState() {
        val shouldShowEmpty = adapterInfosList.all { it.sectionHeaderAdapter.roomsSectionData.isHidden } &&
                !adapterInfosList.any { it.sectionHeaderAdapter.roomsSectionData.isLoading }
        if (shouldShowEmpty) {
            val emptyState = when (roomListParams.displayMode) {
                RoomListDisplayMode.NOTIFICATIONS -> {
                    StateView.State.Empty(
                            title = getString(CommonStrings.room_list_catchup_empty_title),
                            image = ContextCompat.getDrawable(requireContext(), R.drawable.ic_noun_party_popper),
                            message = getString(CommonStrings.room_list_catchup_empty_body)
                    )
                }
                RoomListDisplayMode.PEOPLE ->
                    StateView.State.Empty(
                            title = getString(CommonStrings.room_list_people_empty_title),
                            image = ContextCompat.getDrawable(requireContext(), R.drawable.empty_state_dm),
                            isBigImage = true,
                            message = getString(CommonStrings.room_list_people_empty_body)
                    )
                RoomListDisplayMode.ROOMS ->
                    StateView.State.Empty(
                            title = getString(CommonStrings.room_list_rooms_empty_title),
                            image = ContextCompat.getDrawable(requireContext(), R.drawable.empty_state_room),
                            isBigImage = true,
                            message = getString(CommonStrings.room_list_rooms_empty_body)
                    )
                RoomListDisplayMode.FILTERED ->
                    // Always display the content in this mode, because if the footer
                    StateView.State.Content
            }
            views.stateView.state = emptyState
        } else {
            // is there something to show already?
            if (adapterInfosList.any { !it.sectionHeaderAdapter.roomsSectionData.isHidden }) {
                views.stateView.state = StateView.State.Content
            } else {
                views.stateView.state = StateView.State.Loading
            }
        }
    }

    override fun onBackPressed(toolbarButton: Boolean): Boolean {
        if (views.createChatFabMenu.onBackPressed() || views.tchapCreateRoomFabMenu.onBackPressed()) {
            return true
        }
        return false
    }

    // RoomSummaryController.Callback **************************************************************

    override fun onRoomClicked(room: RoomSummary) {
        roomListViewModel.handle(RoomListAction.SelectRoom(room))
    }

    override fun onRoomLongClicked(room: RoomSummary): Boolean {
        userPreferencesProvider.neverShowLongClickOnRoomHelpAgain()
        withState(roomListViewModel) {
            // refresh footer
            footerController.setData(it)
        }
        RoomListQuickActionsBottomSheet
                .newInstance(room.roomId)
                .show(childFragmentManager, "ROOM_LIST_QUICK_ACTIONS")
        return true
    }

    override fun onAcceptRoomInvitation(room: RoomSummary) {
        notificationDrawerManager.updateEvents { it.clearMemberShipNotificationForRoom(room.roomId) }
        roomListViewModel.handle(RoomListAction.AcceptInvitation(room))
    }

    override fun onJoinSuggestedRoom(room: SpaceChildInfo) {
        roomListViewModel.handle(RoomListAction.JoinSuggestedRoom(room.childRoomId, room.viaServers))
    }

    override fun onSuggestedRoomClicked(room: SpaceChildInfo) {
        roomListViewModel.handle(RoomListAction.ShowRoomDetails(room.childRoomId, room.viaServers))
    }

    override fun onRejectRoomInvitation(room: RoomSummary) {
        notificationDrawerManager.updateEvents { it.clearMemberShipNotificationForRoom(room.roomId) }
        roomListViewModel.handle(RoomListAction.RejectInvitation(room))
    }
}
