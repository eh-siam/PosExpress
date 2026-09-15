# Add Edit Button to Catalog Product Cards Implementation Plan

This plan outlines the changes required to add an Edit button next to the Add/Quantity controls on each product card in the catalog screen, enabling merchants to easily edit product details.

## User Review Required

> [!NOTE]
> We will expose the existing `btnMore` (turning it into a distinct Edit icon button) on each product card in `product_item.xml`, connect it via `ProductAdapter.OnProductActionListener`, and open the edit product dialog in `CatalogFragment`.

## Proposed Changes

### UI & Adapter

#### [MODIFY] [product_item.xml](file:///home/simec-system-android/AndroidStudioProjects/PosApplicatinJava/app/src/main/res/layout/product_item.xml)
- Update `btnMore` to use an edit icon (`android.R.drawable.ic_menu_edit` or a styled button) and ensure proper margin/padding next to the add button / quantity controls.

#### [MODIFY] [ProductAdapter.java](file:///home/simec-system-android/AndroidStudioProjects/PosApplicatinJava/app/src/main/java/com/max/posexpress/ui/ProductAdapter.java)
- Add `onEditProduct(Product product)` to `OnProductActionListener` interface.
- Make `btnMore` visible (`View.VISIBLE`) in `onBindViewHolder`.
- Set click listener on `btnMore` to call `listener.onEditProduct(product)`.

#### [MODIFY] [CatalogFragment.java](file:///home/simec-system-android/AndroidStudioProjects/PosApplicatinJava/app/src/main/java/com/max/posexpress/ui/CatalogFragment.java)
- Implement `onEditProduct(Product product)` in `CatalogFragment`, calling `showProductDialog(product)`.

## Verification Plan

### Automated Tests
- Build app with `app:assembleDebug` to ensure compilation.

### Manual Verification
- Deploy to emulator/device, open Catalog screen, tap the Edit button on any product card, and verify that the edit product dialog opens with pre-filled product details.
