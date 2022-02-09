Changes in Tchap 1.99.5 (2022-02-09)
====================================

Improvements 🙌
--------------
 - [Login] Handle correctly M_LIMIT_EXCEEDED error code ([#322](https://github.com/tchapgouv/tchap-android-v2/issues/322))
 - [Workaround] Hide the text input in a DM left by the other member ([#405](https://github.com/tchapgouv/tchap-android-v2/issues/405))

Bugfixes 🐛
----------
 - Fix potential missing rooms caused by the use of Spaces or low priority tags ([#389](https://github.com/tchapgouv/tchap-android-v2/issues/389))
 - [Invitation by email] Handle correctly Id server consent ([#392](https://github.com/tchapgouv/tchap-android-v2/issues/392))
 - [Workaround] Do not select a DM left by the other user ([#401](https://github.com/tchapgouv/tchap-android-v2/issues/401))
 - Fix the push notifications which were not working for some devices ([#402](https://github.com/tchapgouv/tchap-android-v2/issues/402))


Changes in Tchap 1.99.4 (2022-01-26)
====================================

Features ✨
----------

- The room admin is able to open the room to the external users ([#337](https://github.com/tchapgouv/tchap-android-v2/issues/337))

Improvements 🙌
--------------

- [Account Creation] Translate in French the weak password errors ([#234](https://github.com/tchapgouv/tchap-android-v2/issues/234))
- [Direct Message] Enhance invite by email and discovery result section ([#247](https://github.com/tchapgouv/tchap-android-v2/issues/247))
- Wrong icon displayed for an infected file ([#306](https://github.com/tchapgouv/tchap-android-v2/issues/306))
- [Migration] Automatically give user consent to identity server policy ([#313](https://github.com/tchapgouv/tchap-android-v2/issues/313))
- Hide the voice message option ([#323](https://github.com/tchapgouv/tchap-android-v2/issues/323))
- [Room Settings] Remove the "guest_access" option available in developer mode ([#340](https://github.com/tchapgouv/tchap-android-v2/issues/340))
- Prompt the last admin before letting them leave a room ([#345](https://github.com/tchapgouv/tchap-android-v2/issues/345))
- Adjust F-Droid parameters ([#352](https://github.com/tchapgouv/tchap-android-v2/issues/352))
- Remove the variant named Tchap Secure ([#358](https://github.com/tchapgouv/tchap-android-v2/issues/358))
- Hide the developer mode from Tchap ([#359](https://github.com/tchapgouv/tchap-android-v2/issues/359))
- Rename the unsigned APKs ([#360](https://github.com/tchapgouv/tchap-android-v2/issues/360))
- [Room Avatar] Remove the hexagonal shape on the room avatar ([#372](https://github.com/tchapgouv/tchap-android-v2/issues/372))
- Add a flag to disable message edition ([#373](https://github.com/tchapgouv/tchap-android-v2/issues/373))
- Add a flag to disable message reaction ([#374](https://github.com/tchapgouv/tchap-android-v2/issues/374))
- Updated search in the home screen, filtering is now kept after a rotation. The search widget in the toolbar now use the default android
  design. ([#378](https://github.com/tchapgouv/tchap-android-v2/issues/378))
- Hide Labs menu item in Tchap ([#380](https://github.com/tchapgouv/tchap-android-v2/issues/380))
- [Home] Restore the options menu from Element ([#384](https://github.com/tchapgouv/tchap-android-v2/issues/384))
- Remove the optional screenshot from Security&Privacy ([#386](https://github.com/tchapgouv/tchap-android-v2/issues/386))

Bugfixes 🐛
----------

- [Room settings] Actions must be disabled on infected attachment in the attachments list ([#304](https://github.com/tchapgouv/tchap-android-v2/issues/304))
- Application tabs are not updated correctly ([#307](https://github.com/tchapgouv/tchap-android-v2/issues/307))
- [Login] Do not display last login error on new attempt ([#320](https://github.com/tchapgouv/tchap-android-v2/issues/320))
- Fix malformed link when we try to open TAC with Tchap ([#321](https://github.com/tchapgouv/tchap-android-v2/issues/321))
- [Room creation] the HS name is wrong when the forum type is selected ([#341](https://github.com/tchapgouv/tchap-android-v2/issues/341))
- Wrong landing page on logout ([#346](https://github.com/tchapgouv/tchap-android-v2/issues/346))
- [Dark mode] Improve the contrast in the incoming verification request ([#347](https://github.com/tchapgouv/tchap-android-v2/issues/347))
- Unexpected white area in the verification bottom sheet ([#348](https://github.com/tchapgouv/tchap-android-v2/issues/348))
- Fix crash on screen rotation ([#370](https://github.com/tchapgouv/tchap-android-v2/issues/370))

Changes in Tchap 1.99.3 (2021-12-10)
===================================================

Bugfix 🐛:

- [Login Screen] An unexpected error on the password field #276

Improvements 🙌:

- Rebase/element android v1.3.8 PR #286
- Antivirus: Add MediaScan in the attachments handling #204
- Remove the tab contacts #290
- Remove irrelevant Matrix Id PR #294
- [Pinning] Finalize the configuration about pinning #287
- Hide encryption trust shields PR #296
- Hide share icon in room member profile detail ActionBar #273
- [Settings] Hide "Push Rules" option in Advanced settings>Notifications #271
- Tchap V2 - Update migration dialog #302
- [Direct chat] Disable the click on the room name in the room settings
- [Direct Message] Adapt the Id server consents #246
- [Settings] adjust Security & Privacy section #270
- [Settings] hide "Voice & Video" section when the voip is not available #269
- [Settings] Adjust Preferences #268
- [Settings] Hide General/Discovery settings #251
- [Settings] adjust Help & About section #308

Changes in Tchap 1.99.2 (2021-11-08)
===================================================

Bugfix 🐛:

- Remove unsupported filltype attribute in vector drawable

Changes in Tchap 1.99.1 (2021-11-05)
===================================================

Improvements 🙌:

- Prepare the Tchap release/pre-release #253

Changes in Tchap 1.99.0 (2021-11-05)
===================================================

Features ✨:

- Set up the sign in process #3
- Set up the account creation #6
- Set up Tchap color and light theme #4
- [Home screen] Apply the room cells design #48
- Set up the Home screen - Rooms and Contacts lists #5
- Support the room access rules state event #90
- [Home screen] Toolbar + Search mode #52
- [Room Creation] Set up the new room creation screen #121
- [Expired Account] Support expired account by prompting the user to renew it #125
- [Room Settings] Support the access by link in the private room #229

Improvements 🙌:

- Replace Element references with the actual Tchap information #1
- Set up Tchap RestClient in element-android-sdk #15
- Set up the splash screen #80
- Resolve matrixId to get Tchap user #91
- Update the home bottom bar icons #83
- Simplify the notifications modes #77
- Update the selected room menu #78
- Design Tchap invite cells #79
- Change the invite description #104
- Be able to select the best direct chat related to a user id #88
- [Home screen] cancel the search when the user selects an item from the search result #107
- Implement the users/info endpoint to get users status #87
- Design Tchap contact cells #98
- [Contacts list] open the DM (if any) for the selected user #129
- Set up the application icon (Btchap/Tchap/Tchap secure) #102
- [Home screen] Set up the (+) button on the rooms list #109
- Set up the variant Tchap without voip #161
- Adjust the design in the room details screen #168

=======================================================
+        TEMPLATE WHEN PREPARING A NEW RELEASE        +
=======================================================


Changes in Tchap X.X.X (2021-XX-XX)
===================================================

Features ✨:
-

Improvements 🙌:
-

Bugfix 🐛:
-

Translations 🗣:
-

SDK API changes ⚠️:
- 

Build 🧱:
-

Test:
-

Other changes:
-
