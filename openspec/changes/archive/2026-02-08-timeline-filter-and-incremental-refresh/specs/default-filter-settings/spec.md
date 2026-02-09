## ADDED Requirements

### Requirement: Default filter setting persistence
The system SHALL persist user's default filter preference using SharedPreferences and provide API to read/write the setting.

#### Scenario: Save default filter preference
- **WHEN** user selects "未读" (Unread) as default filter in settings
- **THEN** system saves the preference to SharedPreferences with key "default_filter_type"
- **AND** saved value is "UNREAD"
- **AND** preference persists across app restarts

#### Scenario: Read default filter preference
- **WHEN** TimelineViewModel initializes
- **THEN** system reads preference from SharedPreferences
- **AND** if preference is "ALL", system sets filter to FilterType.ALL
- **AND** if preference is "UNREAD", system sets filter to FilterType.UNREAD
- **AND** if preference is "READ", system sets filter to FilterType.READ
- **AND** if preference does not exist, system uses factory default "UNREAD"

#### Scenario: Update existing preference
- **WHEN** user changes default filter from "未读" (Unread) to "全部" (All)
- **THEN** system overwrites existing preference value
- **AND** new value takes effect immediately on next timeline launch
- **AND** current timeline view is not affected until screen refresh

### Requirement: Settings UI for default filter
The system SHALL provide a settings UI component that allows users to select their preferred default filter type.

#### Scenario: Display default filter options
- **WHEN** user opens settings screen
- **THEN** system displays "默认筛选条件" (Default Filter) section
- **AND** shows three radio button options: "全部" (All), "未读" (Unread), "已读" (Read)
- **AND** current selection is highlighted as selected

#### Scenario: Select default filter option
- **WHEN** user taps on "已读" (Read) radio button
- **THEN** system highlights "已读" (Read) as selected
- **AND** saves preference to SharedPreferences
- **AND** displays confirmation message "默认筛选已设置为: 已读"

#### Scenario: Factory default indication
- **WHEN** user has never changed default filter setting
- **THEN** "未读" (Unread) option is shown as selected
- **AND** system displays "(默认)" label next to "未读" (Unread) option
- **AND** when user changes selection, "(默认)" label disappears

### Requirement: Settings management utility
The system SHALL provide a utility class for managing default filter setting with type-safe API.

#### Scenario: Type-safe read API
- **WHEN** code calls `SettingsManager.getDefaultFilterType()`
- **THEN** system returns `FilterType` enum value
- **AND** return value is never null
- **AND** invalid preference values fall back to factory default UNREAD

#### Scenario: Type-safe write API
- **WHEN** code calls `SettingsManager.setDefaultFilterType(FilterType.ALL)`
- **THEN** system saves "ALL" string to SharedPreferences
- **AND** operation completes synchronously
- **AND** preference is immediately available for read operations

#### Scenario: Preference change listener
- **WHEN** default filter preference changes
- **THEN** SettingsManager notifies registered listeners
- **AND** TimelineViewModel receives notification
- **AND** ViewModel updates filter state if currently using default filter

### Requirement: Default filter vs manual filter
The system SHALL distinguish between user's manual filter selection and default filter preference.

#### Scenario: Manual filter overrides default temporarily
- **WHEN** user's default filter setting is "未读" (Unread)
- **AND** user manually switches to "已读" (Read) in timeline
- **THEN** timeline displays read articles
- **AND** default filter setting remains "未读" (Unread)
- **AND** next app launch uses "未读" (Unread) from settings

#### Scenario: Default filter only applies on launch
- **WHEN** user launches application
- **THEN** system applies default filter setting
- **AND** subsequent manual filter changes do not update default setting
- **AND** default setting only changes when user modifies it in settings screen
