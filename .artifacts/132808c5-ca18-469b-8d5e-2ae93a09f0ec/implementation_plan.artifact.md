# Implementation Plan - Improve bKash QR Scanner Logic

The user wants to know if their bKash QR code scanner works perfectly. The current implementation is basic and might fail for certain types of bKash QR codes (e.g., those containing URLs or country codes).

## Analysis
Current `extractNumber` logic:
- Uses `\\d{11}` to find a sequence of 11 digits.
- Returns the whole raw string if no match is found.

Potential Issues:
1.  **Country Code**: bKash numbers often appear as `8801XXXXXXXXX` (13 digits). The current regex would only extract the first 11 digits (`8801XXXXXXX`).
2.  **Merchant URLs**: bKash merchant QRs are often URLs like `https://www.bkash.com/app/s/?s=01XXXXXXXXX`. While `\\d{11}` might work here, it's safer to look for the specific parameter `s`.
3.  **Formatting**: The raw value might contain spaces or dashes.

## Proposed Changes

### [Component Name] QrScannerActivity

#### [MODIFY] [QrScannerActivity.java](file:///home/simec-system-android/AndroidStudioProjects/PosApplicatinJava/app/src/main/java/com/example/posexpress/ui/QrScannerActivity.java)
- Improve `extractNumber` logic:
    - Check if the raw string is a URL containing `?s=` or `&s=`.
    - If it's a URL, extract the value of the `s` parameter.
    - Clean the string by removing all non-digit characters.
    - Handle 13-digit numbers starting with `880` by stripping the `880`.
    - Ensure the final result is 11 digits starting with `01`.

## Verification Plan

### Automated Tests
- Build the project to ensure no syntax errors.

### Manual Verification
- Test with different bKash QR code formats:
    - Personal QR (e.g., `01712345678`)
    - URL QR (e.g., `https://www.bkash.com/app/s/?s=01712345678`)
    - Country Code QR (e.g., `8801712345678`)
