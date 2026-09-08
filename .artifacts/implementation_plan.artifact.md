# Transaction History Details Implementation

This plan adds the ability to view detailed transaction receipts by clicking on items in the order history list.

## User Review Required

> [!NOTE]
> I will be reusing the existing `ReceiptFragment` to show historical details. The "New Transaction" button on that screen will still function as a way to return to the catalog and start a fresh sale.

## Proposed Changes

### Core Logic & Consistency
#### [MODIFY] [PosViewModel.java](file:///home/simec-system-android/AndroidStudioProjects/PosApplicatinJava/app/src/main/java/com/max/posexpress/viewmodel/PosViewModel.java)
- Add `setSelectedOrder(Order order)` to convert a historical order into JSON format for the receipt view.
- Standardize the item string separator to `|` in `placeOrder` to match the receipt parser.

#### [MODIFY] [ReceiptFragment.java](file:///home/simec-system-android/AndroidStudioProjects/PosApplicatinJava/app/src/main/java/com/max/posexpress/ui/ReceiptFragment.java)
- Update date display logic to prioritize the timestamp stored in the transaction data over the current system time.

### UI Enhancements
#### [MODIFY] [OrderHistoryAdapter.java](file:///home/simec-system-android/AndroidStudioProjects/PosApplicatinJava/app/src/main/java/com/max/posexpress/ui/OrderHistoryAdapter.java)
- Implement a click listener interface to detect when a user taps on a transaction card.

#### [MODIFY] [OrderHistoryFragment.java](file:///home/simec-system-android/AndroidStudioProjects/PosApplicatinJava/app/src/main/java/com/max/posexpress/ui/OrderHistoryFragment.java)
- Connect the adapter click listener to the navigation logic.
- Pass the selected order to the ViewModel before navigating.

### Navigation
#### [MODIFY] [nav_graph.xml](file:///home/simec-system-android/AndroidStudioProjects/PosApplicatinJava/app/src/main/res/navigation/nav_graph.xml)
- Define the navigation action between `OrderHistoryFragment` and `ReceiptFragment`.

## Verification Plan

### Manual Verification
1. Navigate to **Order History**.
2. Tap on any entry in the list.
3. Confirm that the **Receipt** screen opens and correctly displays:
    - The original Transaction ID.
    - The correct historical date and time.
    - All items purchased in that transaction.
    - All pricing details (subtotal, tax, discount, total).
4. Tap "Back" to ensure you return to the history list.
