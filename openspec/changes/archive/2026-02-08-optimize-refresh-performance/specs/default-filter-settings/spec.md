## ADDED Requirements

### Requirement: Compact filter selection UI
The system SHALL provide a compact single-row or two-row layout for default filter selection, replacing the previous vertical radio button layout.

#### Scenario: SegmentedButton layout
- **WHEN** user opens settings screen
- **THEN** system displays three filter options in horizontal SegmentedButton
- **AND** options are labeled "全部" "未读" "已读"
- **AND** UI occupies maximum 2 rows of vertical space
- **AND** selected option is highlighted with primary color

#### Scenario: Compact dropdown menu
- **WHEN** user taps compact filter selector
- **THEN** system displays dropdown menu with three options
- **AND** menu shows "全部" "未读" "已读"
- **AND** currently selected option is displayed in closed state

## MODIFIED Requirements

### Requirement: Default filter setting display
The system SHALL display the default filter setting in a space-efficient manner using Material 3 SegmentedButton or ExposedDropdownMenu component.

#### Scenario: Space-efficient layout
- **WHEN** user views settings screen
- **THEN** filter selection UI occupies maximum 80dp vertical space
- **AND** layout is horizontal (single row) or 2x2 grid (two rows)
- **AND** no descriptive text is shown below each option
- **AND** option labels are self-explanatory

#### Scenario: Touch target size
- **WHEN** user interacts with filter selector
- **THEN** each option has minimum touch target of 48x48dp
- **AND** options are easily selectable without scrolling

### Requirement: Filter option selection feedback
The system SHALL provide immediate visual feedback when user changes default filter setting, but with simplified confirmation message.

#### Scenario: Selection confirmation
- **WHEN** user taps different filter option
- **THEN** system updates selected state immediately
- **AND** system displays Toast: "默认筛选已设置为: {选项名}"
- **AND** Toast displays for 1.5 seconds (shorter than previous 2 seconds)

## REMOVED Requirements

### Requirement: Verbose radio button layout
**Reason**: Replaced by compact layout to reduce vertical space usage and improve settings page aesthetics.

**Migration**: Remove vertical Column containing RadioButton + Text + description for each filter option. Replace with SegmentedButton row.
