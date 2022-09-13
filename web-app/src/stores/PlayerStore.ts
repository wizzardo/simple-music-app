import Store from "react-ui-basics/store/Store";
import {addEventListener, WINDOW} from "react-ui-basics/Tools";


export type QueuedSong = {
    artistId: number,
    albumId: string,
    songId: string,
}

export type PlayerState = {
    queue: QueuedSong[],
    position: number,
    offset: number,
    volume: number,
    playing: boolean,
}

export const store = new Store({
    queue: [],
    position: 0,
    offset: 0,
    volume: 1,
    playing: false,
} as PlayerState)

export default store;

export const setQueue = (songs: QueuedSong[]) => {
    store.set(state => {
        state.queue = songs
        state.position = 0
        state.offset = 0
    });
}
export const play = (songs: QueuedSong[], position = 0) => {
    store.set(state => {
        state.queue = songs
        state.position = position
        state.playing = true
        state.offset = 0
    });
}

export const appendToQueue = (songs: QueuedSong[]) => {
    store.set(state => {
        state.queue.push(...songs)
    });
}
export const setPlaying = (playing: boolean) => {
    store.set(state => {
        state.playing = playing
    });
    if (!playing)
        saveState()
}
export const setPlayingAndOffset = (playing: boolean, offset: number) => {
    store.set(state => {
        state.playing = playing
        state.offset = offset
    });
    if (!playing)
        saveState()
}
export const setOffset = (offset: number) => {
    store.set(state => {
        state.offset = offset
    });
}
export const setVolume = (volume: number) => {
    store.set(state => {
        state.volume = volume
    });
}

export const next = () => {
    store.set(state => {
        state.position++
        if (state.position >= state.queue.length)
            state.position = 0
    });
}

export const prev = () => {
    store.set(state => {
        state.position--
        if (state.position < 0)
            state.position = state.queue.length - 1
    });
}

const saveState = () => {
    localStorage.setItem('playerState', JSON.stringify(store.get()));
};

addEventListener(WINDOW, 'beforeunload', saveState, {});

(() => {
    let savedState = localStorage.getItem('playerState');
    if (savedState) {
        store.set(() => JSON.parse(savedState))
    }
})()