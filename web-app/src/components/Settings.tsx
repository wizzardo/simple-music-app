import React from "react";
import {useStore} from "react-ui-basics/store/Store";
import * as SettingsStore from "../stores/SettingsStore";
import {classNames} from "react-ui-basics/Tools";
import {SCROLLBAR_MODE_HIDDEN} from "react-ui-basics/Scrollable";
import AutocompleteSelect from "react-ui-basics/AutocompleteSelect";

const Settings = ({}) => {
    const {format, bitrate} = useStore(SettingsStore.store)

    return <div>
        <h2>Settings</h2>

        <AutocompleteSelect
            className={classNames()}
            scroll={SCROLLBAR_MODE_HIDDEN}
            value={format}
            withArrow={false}
            withFilter={false}
            selectedMode={'inline'}
            onSelect={SettingsStore.setFormat}
            data={[
                'MP3', 'OGG', 'AAC', 'FLAC', 'OPUS'
            ]}
        />

        <AutocompleteSelect
            className={classNames()}
            scroll={SCROLLBAR_MODE_HIDDEN}
            value={bitrate}
            withArrow={false}
            withFilter={false}
            selectedMode={'inline'}
            onSelect={SettingsStore.setBitrate}
            data={[
                '128', '160', '192', '224', '256', '320'
            ]}
        />
    </div>
}

export default Settings