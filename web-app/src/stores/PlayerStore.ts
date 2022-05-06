import Store from "react-ui-basics/store/Store";


export type QueuedSong = {
    artistId: number,
    albumId: string,
    songId: string,
}

export type PlayerState = {
    queue: QueuedSong[],
    position: number,
    offset: number,
    playing: boolean,
}

export const store = new Store({
    queue: [],
    position: 0,
    offset: 0,
    playing: false,
} as PlayerState)

export default store;

export const setQueue = (songs: QueuedSong[]) => {
    store.set(state => {
        state.queue = songs
        state.position = 0
    });
}
export const play = (songs: QueuedSong[]) => {
    store.set(state => {
        state.queue = songs
        state.position = 0
        state.playing = true
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
}
export const setOffset = (offset: number) => {
    store.set(state => {
        state.offset = offset
    });
}

export const next = () => {
    store.set(state => {
        state.position++
        if (state.position >= state.queue.length)
            state.position = 0
    });
}

window.onbeforeunload = () => {
    localStorage.setItem('playerState', JSON.stringify(store.get()));
}
(() => {
    let savedState = localStorage.getItem('playerState');
    if (savedState) {
        store.set(() => JSON.parse(savedState))
    }
})()
