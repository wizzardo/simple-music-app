
export interface GenericState<T>{
    ids: Array<number>,
    map: { [id: number]: T; }
}