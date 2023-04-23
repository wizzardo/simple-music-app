import Store from "react-ui-basics/store/Store";

export type BlobsState = { [url: string]: string; }

export const store = new Store({} as BlobsState)

export default store;

export const add = (url: string, blobUrl: string) => {
    store.set(state => {
        state[url] = blobUrl
        return state
    });
}