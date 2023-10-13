import Store from "react-ui-basics/store/Store";
import {GenericState} from "./Stores";
import {ArtistDto} from "../services/NetworkService";
import {isDifferent} from "react-ui-basics/Tools"

export type SongsState = GenericState<ArtistDto>

export const store = new Store({
    ids: [],
    map: {},
} as SongsState)

export default store;

export const setAll = (items: Array<ArtistDto>) => {
    store.set(state => {
        state.ids = items.map(it => it.id)
        state.map = items.reduce((m, it) => (m[it.id] = it, m), {})
        return state
    });
}

export const set = (item: ArtistDto) => {
    store.set(state => {
        if (!state.ids.includes(item.id))
            state.ids.push(item.id)
        if (isDifferent(item, state.map[item.id]))
            state.map[item.id] = item
        return state
    });
}