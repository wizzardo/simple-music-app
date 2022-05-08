import Store from "react-ui-basics/store/Store";


export type SettingsState = {
    format: string
    bitrate: number,
}

export const store = new Store({
    format: 'MP3',
    bitrate: 192,
} as SettingsState)

export default store;


export const setFormat = (format: string) => {
    store.set(state => {
        state.format = format
    });
}

export const setBitrate = (bitrate: number) => {
    store.set(state => {
        state.bitrate = bitrate
    });
}

window.onbeforeunload = () => {
    localStorage.setItem('settings', JSON.stringify(store.get()));
}
(() => {
    let savedState = localStorage.getItem('settings');
    if (savedState) {
        store.set(() => JSON.parse(savedState))
    }
})()
