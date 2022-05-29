import Store from "react-ui-basics/store/Store";
import {addEventListener, WINDOW} from "react-ui-basics/Tools";


export type SettingsState = {
    format: string
    bitrate: number,
}

export const store = new Store({
    format: 'MP3',
    bitrate: 192,
} as SettingsState)

export default store;

const saveSettings = () => {
    localStorage.setItem('settings', JSON.stringify(store.get()));
};

export const setFormat = (format: string) => {
    store.set(state => {
        state.format = format
    });
    saveSettings()
}

export const setBitrate = (bitrate: number) => {
    store.set(state => {
        state.bitrate = bitrate
    });
    saveSettings()
}

(() => {
    let savedState = localStorage.getItem('settings');
    if (savedState) {
        store.set(() => JSON.parse(savedState))
    }
})()
