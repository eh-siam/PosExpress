# Implementation Plan - Aesthetic Login Card Refinement

I will remove the redundant "Merchant Login" title and refine the internal layout of the login card to make it look more professional and visually appealing.

## Proposed Changes

### UI Components

#### [MODIFY] [activity_login.xml](file:///home/simec-system-android/AndroidStudioProjects/PosApplicatinJava/app/src/main/res/layout/activity_login.xml)
- **Remove Redundant Title**: Delete the `TextView` displaying `@string/title_login` inside the card.
- **Add "Forgot Password?"**: Add a subtle text button below the password field to fill the functional gap and improve the "real-app" look.
- **Add "OR" Divider**: Implement a modern horizontal divider with "OR" text to cleanly separate the standard login from the Google login option.
- **Spacing Refinement**: Adjust margins between input fields and buttons for better vertical rhythm.

## Verification Plan

### Manual Verification
1. Open the Login screen.
2. Confirm the card looks cleaner without the "Merchant Login" title.
3. Verify the "Forgot Password?" link is correctly aligned.
4. Check the visual appeal of the new "OR" divider.
5. Ensure all interactive elements (inputs and buttons) are still fully functional.
