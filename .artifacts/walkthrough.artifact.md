# Walkthrough - Firebase `.info/connected` Network Status Indicator

Implemented real-time network connectivity monitoring using Firebase Realtime Database `.info/connected` to display an offline warning banner whenever the device loses internet connection.

## Changes

### Repository & ViewModel

#### [PosRepository.java](file:///home/simec-system-android/AndroidStudioProjects/PosApplicatinJava/app/src/main/java/com/max/posexpress/repository/PosRepository.java)
- Added `observeNetworkStatus(NetworkStatusCallback callback)` listening to `FirebaseDatabase.getInstance().getReference(".info/connected")`.

#### [PosViewModel.java](file:///home/simec-system-android/AndroidStudioProjects/PosApplicatinJava/app/src/main/java/com/max/posexpress/viewmodel/PosViewModel.java)
- Added `isConnected` LiveData (`MutableLiveData<Boolean>`) and initialized observation in constructor.
- Exposed `getIsConnected()` getter method.

### UI & Activity

#### [activity_host.xml](file:///home/simec-system-android/AndroidStudioProjects/PosApplicatinJava/app/src/main/res/layout/activity_host.xml)
- Added a top offline warning banner (`tvOfflineBanner`) with a red background and white text ("No internet connection. Working offline.").

#### [HostActivity.java](file:///home/simec-system-android/AndroidStudioProjects/PosApplicatinJava/app/src/main/java/com/max/posexpress/HostActivity.java)
- Observed `viewModel.getIsConnected()` to automatically show or hide the offline banner in real-time.

## Verification Results

### Automated Tests
- `./gradlew :app:assembleDebug` -> **BUILD SUCCESSFUL**
