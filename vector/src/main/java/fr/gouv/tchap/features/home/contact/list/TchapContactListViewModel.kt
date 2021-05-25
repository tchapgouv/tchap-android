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

package fr.gouv.tchap.features.home.contact.list

import androidx.lifecycle.viewModelScope
import com.airbnb.mvrx.ActivityViewModelContext
import com.airbnb.mvrx.FragmentViewModelContext
import com.airbnb.mvrx.Loading
import com.airbnb.mvrx.MvRxViewModelFactory
import com.airbnb.mvrx.Success
import com.airbnb.mvrx.ViewModelContext
import com.jakewharton.rxrelay2.BehaviorRelay
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import im.vector.app.core.contacts.ContactsDataSource
import im.vector.app.core.contacts.MappedContact
import im.vector.app.core.extensions.exhaustive
import im.vector.app.core.platform.VectorViewModel
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.matrix.android.sdk.api.MatrixPatterns
import org.matrix.android.sdk.api.query.ActiveSpaceFilter
import org.matrix.android.sdk.api.query.RoomCategoryFilter
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.identity.IdentityServiceError
import org.matrix.android.sdk.api.session.identity.ThreePid
import org.matrix.android.sdk.api.session.profile.ProfileService
import org.matrix.android.sdk.api.session.room.RoomSortOrder
import org.matrix.android.sdk.api.session.room.model.Membership
import org.matrix.android.sdk.api.session.room.roomSummaryQueryParams
import org.matrix.android.sdk.api.session.user.model.User
import org.matrix.android.sdk.api.util.Optional
import org.matrix.android.sdk.api.util.toMatrixItem
import org.matrix.android.sdk.api.util.toOptional
import org.matrix.android.sdk.rx.asObservable
import org.matrix.android.sdk.rx.rx
import timber.log.Timber
import java.util.concurrent.TimeUnit

//private typealias KnownUsersSearch = String
private typealias DirectoryUsersSearch = String

class TchapContactListViewModel @AssistedInject constructor(@Assisted initialState: TchapContactListViewState,
                                                            private val contactsDataSource: ContactsDataSource,
                                                            private val session: Session)
    : VectorViewModel<TchapContactListViewState, TchapContactListAction, TchapContactListViewEvents>(initialState) {

//    private val knownUsersSearch = BehaviorRelay.create<KnownUsersSearch>()
    private val directoryUsersSearch = BehaviorRelay.create<DirectoryUsersSearch>()

    private var allContacts: List<MappedContact> = emptyList()
    private var mappedContacts: List<MappedContact> = emptyList()

    @AssistedFactory
    interface Factory {
        fun create(initialState: TchapContactListViewState): TchapContactListViewModel
    }

    companion object : MvRxViewModelFactory<TchapContactListViewModel, TchapContactListViewState> {

        override fun create(viewModelContext: ViewModelContext, state: TchapContactListViewState): TchapContactListViewModel? {
            val factory = when (viewModelContext) {
                is FragmentViewModelContext -> viewModelContext.fragment as? Factory
                is ActivityViewModelContext -> viewModelContext.activity as? Factory
            }
            return factory?.create(state) ?: error("You should let your activity/fragment implements Factory interface")
        }
    }

    init {
        observeUsers()
        observeDMs()

        selectSubscribe(TchapContactListViewState::searchTerm) { _ ->
            updateFilteredContacts()
        }
    }

    private fun loadContacts() {
        setState {
            copy(
                    mappedContacts = Loading(),
                    identityServerUrl = session.identityService().getCurrentIdentityServerUrl(),
                    userConsent = session.identityService().getUserConsent()
            )
        }

        viewModelScope.launch(Dispatchers.IO) {
            allContacts = contactsDataSource.getContacts(
                    withEmails = true,
                    // Do not handle phone numbers for the moment
                    withMsisdn = false
            )
            mappedContacts = allContacts

            setState {
                copy(
                        mappedContacts = Success(allContacts)
                )
            }

            performLookup(allContacts)
            updateFilteredContacts()
        }
    }

    private fun performLookup(contacts: List<MappedContact>) {
        if (!session.identityService().getUserConsent()) {
            return
        }
        viewModelScope.launch {
            val threePids = contacts.flatMap { contact ->
                contact.emails.map { ThreePid.Email(it.email) } +
                        contact.msisdns.map { ThreePid.Msisdn(it.phoneNumber) }
            }

            val data = try {
                session.identityService().lookUp(threePids)
            } catch (failure: Throwable) {
                Timber.w(failure, "Unable to perform the lookup")

                // Should not happen, but just to be sure
                if (failure is IdentityServiceError.UserConsentNotProvided) {
                    setState {
                        copy(userConsent = false)
                    }
                }
                return@launch
            }

            mappedContacts = allContacts.map { contactModel ->
                contactModel.copy(
                        emails = contactModel.emails.map { email ->
                            email.copy(
                                    matrixId = data
                                            .firstOrNull { foundThreePid -> foundThreePid.threePid.value == email.email }
                                            ?.matrixId
                            )
                        },
                        msisdns = contactModel.msisdns.map { msisdn ->
                            msisdn.copy(
                                    matrixId = data
                                            .firstOrNull { foundThreePid -> foundThreePid.threePid.value == msisdn.phoneNumber }
                                            ?.matrixId
                            )
                        }
                )
            }

            setState {
                copy(
                        isBoundRetrieved = true
                )
            }

            updateFilteredContacts()
        }
    }

    private fun updateFilteredContacts() = withState { state ->
        val filteredMappedContacts = mappedContacts
                .filter { it.displayName.contains(state.searchTerm, true) }
                .filter { contactModel -> contactModel.emails.any { it.matrixId != null } || contactModel.msisdns.any { it.matrixId != null }
                }

        val filteredRoomSummaries = state.roomSummaries.invoke()
                ?.filter { it.displayName.contains(state.searchTerm, true) }
                ?.filter { user -> user.directUserId != null }
                .orEmpty()

        setState {
            copy(
                    filteredLocalContacts = filteredMappedContacts,
                    filteredRoomSummaries = filteredRoomSummaries
            )
        }
    }

    override fun handle(action: TchapContactListAction) {
        when (action) {
            is TchapContactListAction.SearchUsers                -> handleSearchUsers(action.value)
            is TchapContactListAction.ClearSearchUsers           -> handleClearSearchUsers()
            TchapContactListAction.ComputeMatrixToLinkForSharing -> handleShareMyMatrixToLink()
            TchapContactListAction.LoadContacts                  -> loadContacts()
        }.exhaustive
    }

    private fun handleSearchUsers(searchTerm: String) {
        setState {
            copy(searchTerm = searchTerm)
        }
//        knownUsersSearch.accept(searchTerm)
        directoryUsersSearch.accept(searchTerm)
    }

    private fun handleShareMyMatrixToLink() {
        session.permalinkService().createPermalink(session.myUserId)?.let {
            _viewEvents.post(TchapContactListViewEvents.OpenShareMatrixToLink(it))
        }
    }

    private fun handleClearSearchUsers() {
//        knownUsersSearch.accept("")
        directoryUsersSearch.accept("")
        setState {
            copy(searchTerm = "")
        }
    }

    private fun observeUsers() = withState { state ->
//        knownUsersSearch
//                .throttleLast(300, TimeUnit.MILLISECONDS)
//                .observeOn(AndroidSchedulers.mainThread())
//                .switchMap {
//                    session.rx().livePagedUsers(it, state.excludedUserIds)
//                }
//                .execute { async ->
//                    copy(knownUsers = async)
//                }

        directoryUsersSearch
                .debounce(300, TimeUnit.MILLISECONDS)
                .switchMapSingle { search ->
                    val stream = if (search.isBlank()) {
                        Single.just(emptyList<User>())
                    } else {
                        val searchObservable = session.rx()
                                .searchUsersDirectory(search, 50, state.excludedUserIds.orEmpty())
                                .map { users ->
                                    users.sortedBy { it.toMatrixItem().firstLetterOfDisplayName() }
                                }
                        // If it's a valid user id try to use Profile API
                        // because directory only returns users that are in public rooms or share a room with you, where as
                        // profile will work other federations
                        if (!MatrixPatterns.isUserId(search)) {
                            searchObservable
                        } else {
                            val profileObservable = session.rx().getProfileInfo(search)
                                    .map { json ->
                                        User(
                                                userId = search,
                                                displayName = json[ProfileService.DISPLAY_NAME_KEY] as? String,
                                                avatarUrl = json[ProfileService.AVATAR_URL_KEY] as? String
                                        ).toOptional()
                                    }
                                    .onErrorReturn { Optional.empty() }

                            Single.zip(
                                    searchObservable,
                                    profileObservable,
                                    { searchResults, optionalProfile ->
                                        val profile = optionalProfile.getOrNull() ?: return@zip searchResults
                                        val searchContainsProfile = searchResults.any { it.userId == profile.userId }
                                        if (searchContainsProfile) {
                                            searchResults
                                        } else {
                                            listOf(profile) + searchResults
                                        }
                                    }
                            )
                        }
                    }
                    stream.toAsync {
                        copy(directoryUsers = it)
                    }
                }
                .subscribe()
                .disposeOnClear()
    }

    private fun observeDMs() {
        session.getPagedRoomSummariesLive(
                roomSummaryQueryParams {
                    this.memberships = listOf(Membership.JOIN)
                    this.activeSpaceFilter = ActiveSpaceFilter.ActiveSpace(null)
                    this.roomCategoryFilter = RoomCategoryFilter.ONLY_DM
                }, sortOrder = RoomSortOrder.NONE
        ).asObservable()
                .throttleFirst(300, TimeUnit.MILLISECONDS)
                .observeOn(Schedulers.computation())
                .execute {
                    copy(
                            roomSummaries = it,
                            filteredRoomSummaries = it.invoke().orEmpty()
                    )
                }
    }
}
