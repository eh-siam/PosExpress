# Implementation Plan - Unique Product ID Generation (UUID / Firebase Key)

Replace integer/timestamp-based product IDs with unique string IDs (`UUID.randomUUID().toString()` or Firebase push keys) to prevent ID conflicts when multiple devices add products offline.

## User Review Required

> [!IMPORTANT]
> - Changing `Product.id` from `Int` to `String` requires updating cart quantity maps (`Map<String, Integer>`), repository methods, ViewModel methods, and adapter references.

## Proposed Changes

### Data Model

#### [MODIFY] [Product.kt](file:///home/simec-system-android/AndroidStudioProjects/PosApplicatinJava/app/src/main/java/com/max/posexpress/model/Product.kt)
- Change `var id: Int = 0` to `var id: String = ""`.

### Repository & ViewModel

#### [MODIFY] [PosRepository.java](file:///home/simec-system-android/AndroidStudioProjects/PosApplicatinJava/app/src/main/java/com/max/posexpress/repository/PosRepository.java)
- Change `cartQuantities` from `Map<Integer, Integer>` to `Map<String, Integer>`.
- Update `updateCartQuantity(String productId, int quantity)`.

#### [MODIFY] [PosViewModel.java](file:///home/simec-system-android/AndroidStudioProjects/PosApplicatinJava/app/src/main/java/com/max/posexpress/viewmodel/PosViewModel.java)
- In `addProduct(...)`, generate product ID using `UUID.randomUUID().toString()`.
- Update cart quantity handling methods for `String` product ID.

### UI Adapters & Fragments

#### [MODIFY] [ProductAdapter.java](file:///home/simec-system-android/AndroidStudioProjects/PosApplicatinJava/app/src/main/java/com/max/posexpress/ui/ProductAdapter.java)
- Update `productQuantities` map type to `Map<String, Integer>`.

#### [MODIFY] [PaymentFragment.java](file:///home/simec-system-android/AndroidStudioProjects/PosApplicatinJava/app/src/main/java/com/max/posexpress/ui/PaymentFragment.java)
- Update cart map type to `Map<String, Integer>`.

## Verification Plan

### Automated Tests
- Build project using `./gradlew :app:assembleDebug`.

### Manual Verification
- Deploy app, add new products, update quantities, and process checkout to ensure product ID and cart mapping work correctly with unique string IDs.
